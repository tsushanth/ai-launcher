import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Request logging
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'healthy',
        service: 'ai-launcher-backend',
        version: '1.0.0',
        timestamp: new Date().toISOString()
    });
});

// API routes
app.get('/api/launcher', (req, res) => {
    res.json({
        message: 'AI Launcher API',
        version: '1.0.0',
        endpoints: {
            '/api/launcher/chat': 'POST - AI chat with context',
            '/api/launcher/themes': 'GET - Browse themes',
            '/api/launcher/themes/generate': 'POST - Generate AI theme',
            '/api/launcher/extensions': 'GET - Browse extensions marketplace',
            '/api/launcher/sync': 'POST - Sync launcher settings'
        }
    });
});

// Chat endpoint (placeholder - will connect to worker)
app.post('/api/launcher/chat', async (req, res) => {
    const { userId, message, context } = req.body;

    if (!message) {
        return res.status(400).json({ error: 'Message is required' });
    }

    // TODO: Forward to launcher-worker for Claude CLI processing
    res.json({
        response: `Echo: ${message}`,
        context: context,
        timestamp: new Date().toISOString(),
        note: 'Worker integration coming soon'
    });
});

// Themes endpoint (placeholder)
app.get('/api/launcher/themes', async (req, res) => {
    res.json({
        themes: [],
        count: 0,
        note: 'Theme system coming soon'
    });
});

// Extensions marketplace (placeholder)
app.get('/api/launcher/extensions', async (req, res) => {
    res.json({
        extensions: [],
        count: 0,
        note: 'Extension marketplace coming soon'
    });
});

// Error handling
app.use((err, req, res, next) => {
    console.error('Error:', err);
    res.status(500).json({
        error: 'Internal server error',
        message: err.message
    });
});

// 404 handler
app.use((req, res) => {
    res.status(404).json({
        error: 'Not found',
        path: req.path
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`🚀 AI Launcher Backend running on port ${PORT}`);
    console.log(`📡 Health check: http://localhost:${PORT}/health`);
    console.log(`🔌 API docs: http://localhost:${PORT}/api/launcher`);
});
