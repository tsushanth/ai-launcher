import express from 'express';
import { spawn, execSync } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import dotenv from 'dotenv';

dotenv.config();

const app = express();
const PORT = process.env.WORKER_PORT || 3456;
const WORKER_SECRET = process.env.WORKER_SECRET || 'launcher-worker-secret-2026';
const SESSIONS_DIR = path.join(os.homedir(), '.launcher-worker', 'sessions');

// Provider configuration — set via environment variables
const PROVIDER = process.env.PROVIDER || 'claude-cli'; // claude-cli | anthropic-api | openai-compatible | ollama
const MODEL = process.env.MODEL || 'claude-opus-4-6';
const API_KEY = process.env.API_KEY || process.env.ANTHROPIC_API_KEY || process.env.OPENAI_API_KEY || '';
const API_BASE_URL = process.env.API_BASE_URL || (PROVIDER === 'ollama' ? 'http://localhost:11434/v1' : '');

fs.mkdirSync(SESSIONS_DIR, { recursive: true });

app.use(express.json({ limit: '10mb' }));

app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

// ─── Provider: Claude CLI ─────────────────────────────────────────────────────

function findClaudeCLI() {
    try {
        const result = execSync('which claude', { encoding: 'utf-8', stdio: ['pipe', 'pipe', 'pipe'] });
        if (result.trim()) return result.trim();
    } catch (e) {}

    const vscodeExtDir = path.join(os.homedir(), '.vscode', 'extensions');
    if (fs.existsSync(vscodeExtDir)) {
        const dirs = fs.readdirSync(vscodeExtDir)
            .filter(d => d.startsWith('anthropic.claude-code-'))
            .sort()
            .reverse();
        for (const dir of dirs) {
            const binaryPath = path.join(vscodeExtDir, dir, 'resources', 'native-binary', 'claude');
            if (fs.existsSync(binaryPath)) return binaryPath;
        }
    }

    const commonPaths = [
        '/opt/homebrew/bin/claude',
        '/usr/local/bin/claude',
        path.join(os.homedir(), '.local', 'bin', 'claude')
    ];
    for (const p of commonPaths) {
        if (fs.existsSync(p)) return p;
    }

    throw new Error('Claude CLI not found. Install Claude Code CLI or switch PROVIDER.');
}

function chatWithClaudeCLI(fullPrompt, userId, sessionId, res, req) {
    const claudeCli = findClaudeCLI();
    const sessionDir = path.join(SESSIONS_DIR, userId || 'default', sessionId || 'main');
    fs.mkdirSync(sessionDir, { recursive: true });

    console.log(`Spawning Claude CLI: ${claudeCli}`);

    const claudeProcess = spawn(claudeCli, [
        '--print',
        '--dangerously-skip-permissions',
        '--output-format', 'text'
    ], {
        cwd: sessionDir,
        stdio: ['pipe', 'pipe', 'pipe'],
        env: { ...process.env, HOME: os.homedir() }
    });

    claudeProcess.stdin.write(fullPrompt + '\n');
    claudeProcess.stdin.end();

    let buffer = '';

    claudeProcess.stdout.on('data', (data) => {
        const text = data.toString();
        buffer += text;
        res.write(`data: ${JSON.stringify({ type: 'chunk', text })}\n\n`);
    });

    claudeProcess.stderr.on('data', (data) => {
        const errorText = data.toString();
        console.error(`Claude stderr: ${errorText}`);

        const quotaPatterns = [
            "You're out of extra usage",
            'out of extra usage',
            'usage limit',
            'rate limit exceeded',
            'quota exceeded'
        ];
        if (quotaPatterns.some(p => errorText.toLowerCase().includes(p.toLowerCase()))) {
            res.write(`data: ${JSON.stringify({
                type: 'error',
                error: 'QUOTA_EXCEEDED',
                message: 'AI usage limit reached. Please try again later.'
            })}\n\n`);
            res.end();
            claudeProcess.kill();
        }
    });

    claudeProcess.on('close', (code) => {
        res.write(`data: ${JSON.stringify({ type: 'done', code, response: buffer })}\n\n`);
        res.end();
    });

    res.on('close', () => {
        claudeProcess.kill();
    });
}

// ─── Provider: Anthropic API ──────────────────────────────────────────────────

async function chatWithAnthropicAPI(systemPrompt, userMessage, res, req) {
    if (!API_KEY) throw new Error('API_KEY is required for anthropic-api provider');

    const response = await fetch('https://api.anthropic.com/v1/messages', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'x-api-key': API_KEY,
            'anthropic-version': '2023-06-01',
            'anthropic-beta': 'messages-2023-12-15'
        },
        body: JSON.stringify({
            model: MODEL,
            max_tokens: 1024,
            system: systemPrompt,
            messages: [{ role: 'user', content: userMessage }],
            stream: true
        })
    });

    if (!response.ok) {
        const err = await response.text();
        throw new Error(`Anthropic API error ${response.status}: ${err}`);
    }

    let buffer = '';
    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    req.on('close', () => reader.cancel());

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        for (const line of chunk.split('\n')) {
            if (!line.startsWith('data: ')) continue;
            const data = line.slice(6).trim();
            if (data === '[DONE]') continue;
            try {
                const event = JSON.parse(data);
                if (event.type === 'content_block_delta' && event.delta?.text) {
                    buffer += event.delta.text;
                    res.write(`data: ${JSON.stringify({ type: 'chunk', text: event.delta.text })}\n\n`);
                }
            } catch {}
        }
    }

    res.write(`data: ${JSON.stringify({ type: 'done', code: 0, response: buffer })}\n\n`);
    res.end();
}

// ─── Provider: OpenAI-compatible (includes Ollama) ───────────────────────────

async function chatWithOpenAICompatible(systemPrompt, userMessage, res, req) {
    const baseUrl = API_BASE_URL || 'https://api.openai.com/v1';
    const headers = {
        'Content-Type': 'application/json',
        ...(API_KEY ? { 'Authorization': `Bearer ${API_KEY}` } : {})
    };

    const response = await fetch(`${baseUrl}/chat/completions`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
            model: MODEL,
            messages: [
                { role: 'system', content: systemPrompt },
                { role: 'user', content: userMessage }
            ],
            stream: true
        })
    });

    if (!response.ok) {
        const err = await response.text();
        throw new Error(`API error ${response.status}: ${err}`);
    }

    let buffer = '';
    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    req.on('close', () => reader.cancel());

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        for (const line of chunk.split('\n')) {
            if (!line.startsWith('data: ')) continue;
            const data = line.slice(6).trim();
            if (data === '[DONE]') continue;
            try {
                const event = JSON.parse(data);
                const text = event.choices?.[0]?.delta?.content;
                if (text) {
                    buffer += text;
                    res.write(`data: ${JSON.stringify({ type: 'chunk', text })}\n\n`);
                }
            } catch {}
        }
    }

    res.write(`data: ${JSON.stringify({ type: 'done', code: 0, response: buffer })}\n\n`);
    res.end();
}

// ─── Context prompt builder ───────────────────────────────────────────────────

function buildContextPrompt(context) {
    let prompt = `You are an AI assistant integrated into an Android launcher. `;
    prompt += `You can help with app search, calculations, notes, and general tasks.\n\n`;
    prompt += `Current launcher context:\n`;

    if (context?.currentApp) {
        prompt += `- Current app: ${context.currentApp}\n`;
    }
    if (context?.recentNotifications?.length > 0) {
        prompt += `- Recent notifications:\n`;
        context.recentNotifications.slice(0, 5).forEach(notif => {
            prompt += `  • ${notif.appPackage}: ${notif.title || notif.text || 'notification'}\n`;
        });
    }
    if (context?.clipboard) {
        prompt += `- Clipboard: "${context.clipboard}"\n`;
    }
    if (context?.installedApps) {
        prompt += `- ${context.installedApps.length} apps installed\n`;
    }

    prompt += `\nProvide concise, helpful responses. Keep responses brief and conversational.`;
    return prompt;
}

// ─── Health check ─────────────────────────────────────────────────────────────

app.get('/health', (req, res) => {
    const info = {
        status: 'healthy',
        service: 'launcher-worker',
        provider: PROVIDER,
        model: PROVIDER === 'claude-cli' ? 'claude-cli' : MODEL,
        sessionsDir: SESSIONS_DIR,
        timestamp: new Date().toISOString()
    };

    if (PROVIDER === 'claude-cli') {
        try {
            info.claudePath = findClaudeCLI();
        } catch (e) {
            info.status = 'unhealthy';
            info.error = e.message;
            return res.status(500).json(info);
        }
    }

    if (PROVIDER === 'ollama') {
        info.apiBaseUrl = API_BASE_URL;
    }

    if (PROVIDER === 'openai-compatible') {
        info.apiBaseUrl = API_BASE_URL || 'https://api.openai.com/v1';
    }

    res.json(info);
});

// ─── Chat endpoint ────────────────────────────────────────────────────────────

app.post('/chat', async (req, res) => {
    const secret = req.headers['x-worker-secret'];
    if (secret !== WORKER_SECRET) {
        return res.status(401).json({ error: 'Unauthorized' });
    }

    const { userId, context, message, sessionId } = req.body;
    if (!message) {
        return res.status(400).json({ error: 'Message is required' });
    }

    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.setHeader('X-Accel-Buffering', 'no');

    const contextPrompt = buildContextPrompt(context);

    try {
        if (PROVIDER === 'claude-cli') {
            chatWithClaudeCLI(`${contextPrompt}\n\nUser: ${message}`, userId, sessionId, res, req);
        } else if (PROVIDER === 'anthropic-api') {
            await chatWithAnthropicAPI(contextPrompt, message, res, req);
        } else if (PROVIDER === 'openai-compatible' || PROVIDER === 'ollama') {
            await chatWithOpenAICompatible(contextPrompt, message, res, req);
        } else {
            throw new Error(`Unknown PROVIDER: "${PROVIDER}". Use claude-cli, anthropic-api, openai-compatible, or ollama.`);
        }
    } catch (error) {
        console.error('Error in chat endpoint:', error);
        res.write(`data: ${JSON.stringify({
            type: 'error',
            error: 'INTERNAL_ERROR',
            message: error.message
        })}\n\n`);
        res.end();
    }
});

// ─── Start ────────────────────────────────────────────────────────────────────

app.listen(PORT, () => {
    console.log(`🤖 Launcher Worker running on port ${PORT}`);
    console.log(`🔌 Provider: ${PROVIDER}${PROVIDER !== 'claude-cli' ? ` | Model: ${MODEL}` : ''}`);

    if (PROVIDER === 'claude-cli') {
        try {
            const claudePath = findClaudeCLI();
            console.log(`✅ Claude CLI found: ${claudePath}`);
        } catch (e) {
            console.error(`❌ Claude CLI not found: ${e.message}`);
        }
    } else if (PROVIDER === 'ollama') {
        console.log(`📡 Ollama base URL: ${API_BASE_URL}`);
    } else if (PROVIDER === 'openai-compatible') {
        console.log(`📡 API base URL: ${API_BASE_URL || 'https://api.openai.com/v1'}`);
    }

    console.log(`📁 Sessions dir: ${SESSIONS_DIR}`);
});
