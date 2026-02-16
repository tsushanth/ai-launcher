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

// Create sessions directory
fs.mkdirSync(SESSIONS_DIR, { recursive: true });

app.use(express.json({ limit: '10mb' }));

// Request logging
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

/**
 * Find Claude CLI binary
 * Reused pattern from riddle-verse/game-worker/server.js
 */
function findClaudeCLI() {
    // Try 'which claude' first
    try {
        const result = execSync('which claude', { encoding: 'utf-8', stdio: ['pipe', 'pipe', 'pipe'] });
        if (result.trim()) {
            console.log(`Found Claude CLI via which: ${result.trim()}`);
            return result.trim();
        }
    } catch (e) {
        // Continue searching
    }

    // Search VSCode extension directory
    const vscodeExtDir = path.join(os.homedir(), '.vscode', 'extensions');
    if (fs.existsSync(vscodeExtDir)) {
        const dirs = fs.readdirSync(vscodeExtDir)
            .filter(d => d.startsWith('anthropic.claude-code-'))
            .sort()
            .reverse();

        for (const dir of dirs) {
            const binaryPath = path.join(vscodeExtDir, dir, 'resources', 'native-binary', 'claude');
            if (fs.existsSync(binaryPath)) {
                console.log(`Found Claude CLI in VSCode extension: ${binaryPath}`);
                return binaryPath;
            }
        }
    }

    // Try common paths
    const commonPaths = [
        '/opt/homebrew/bin/claude',
        '/usr/local/bin/claude',
        path.join(os.homedir(), '.local', 'bin', 'claude')
    ];

    for (const p of commonPaths) {
        if (fs.existsSync(p)) {
            console.log(`Found Claude CLI at: ${p}`);
            return p;
        }
    }

    throw new Error('Claude CLI not found. Please install Claude Code CLI.');
}

/**
 * Build context prompt for Claude
 */
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

    prompt += `\nProvide concise, helpful responses. When appropriate, suggest launcher actions.\n`;
    prompt += `Keep responses brief and conversational.`;

    return prompt;
}

// Health check
app.get('/health', (req, res) => {
    try {
        const claudePath = findClaudeCLI();
        res.json({
            status: 'healthy',
            service: 'launcher-worker',
            claudePath,
            sessionsDir: SESSIONS_DIR,
            timestamp: new Date().toISOString()
        });
    } catch (e) {
        res.status(500).json({
            status: 'unhealthy',
            error: e.message,
            timestamp: new Date().toISOString()
        });
    }
});

// Main chat endpoint
app.post('/chat', async (req, res) => {
    const secret = req.headers['x-worker-secret'];
    if (secret !== WORKER_SECRET) {
        return res.status(401).json({ error: 'Unauthorized' });
    }

    const { userId, context, message, sessionId } = req.body;

    if (!message) {
        return res.status(400).json({ error: 'Message is required' });
    }

    // Set SSE headers
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.setHeader('X-Accel-Buffering', 'no'); // Disable Nginx buffering

    try {
        // Find Claude CLI
        const claudeCli = findClaudeCLI();

        // Build context prompt
        const contextPrompt = buildContextPrompt(context);
        const fullPrompt = `${contextPrompt}\n\nUser: ${message}`;

        // Create session directory
        const sessionDir = path.join(SESSIONS_DIR, userId || 'default', sessionId || 'main');
        fs.mkdirSync(sessionDir, { recursive: true });

        console.log(`Spawning Claude CLI for user ${userId}, session ${sessionId}`);
        console.log(`Prompt: ${fullPrompt.substring(0, 100)}...`);

        // Spawn Claude CLI process
        const claudeProcess = spawn(claudeCli, ['--session', sessionDir], {
            cwd: sessionDir,
            stdio: ['pipe', 'pipe', 'pipe']
        });

        // Send prompt to Claude
        claudeProcess.stdin.write(fullPrompt + '\n');
        claudeProcess.stdin.end();

        let buffer = '';

        // Stream output as SSE
        claudeProcess.stdout.on('data', (data) => {
            const text = data.toString();
            buffer += text;

            // Send chunks as SSE
            res.write(`data: ${JSON.stringify({ type: 'chunk', text })}\n\n`);
        });

        // Handle errors from Claude (quota detection pattern from riddle-verse)
        claudeProcess.stderr.on('data', (data) => {
            const errorText = data.toString();
            console.error(`Claude stderr: ${errorText}`);

            // Detect quota errors
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

        // Handle process completion
        claudeProcess.on('close', (code) => {
            console.log(`Claude process exited with code ${code}`);
            res.write(`data: ${JSON.stringify({ type: 'done', code, response: buffer })}\n\n`);
            res.end();
        });

        // Handle client disconnect
        req.on('close', () => {
            console.log('Client disconnected, killing Claude process');
            claudeProcess.kill();
        });

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

// Start server
app.listen(PORT, () => {
    console.log(`🤖 Launcher Worker running on port ${PORT}`);
    console.log(`📁 Sessions directory: ${SESSIONS_DIR}`);

    try {
        const claudePath = findClaudeCLI();
        console.log(`✅ Claude CLI found: ${claudePath}`);
    } catch (e) {
        console.error(`❌ Claude CLI not found: ${e.message}`);
        console.error(`   Install Claude Code CLI to use this worker`);
    }
});
