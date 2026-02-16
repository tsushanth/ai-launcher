# AI Launcher Worker

Worker service that integrates with Claude Code CLI for AI-powered launcher features.

## Requirements

- **Claude Code CLI** installed and authenticated
  - Install: `npm install -g @anthropic-ai/claude-code`
  - Or use VSCode extension binary
- **Node.js** 18+

## Setup

### 1. Install Dependencies

```bash
cd launcher-worker
npm install
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env:
# - Set WORKER_SECRET (must match backend)
# - Set WORKER_PORT (default: 3456)
```

### 3. Authenticate Claude CLI

```bash
claude auth
```

Follow the OAuth flow to authenticate.

### 4. Run Worker

```bash
npm run dev
```

Worker runs on `http://localhost:3456`

## API Endpoints

### Health Check
```
GET /health

Response:
{
  "status": "healthy",
  "claudePath": "/path/to/claude",
  "sessionsDir": "/Users/you/.launcher-worker/sessions"
}
```

### Chat (SSE Streaming)
```
POST /chat
Headers:
  x-worker-secret: your-secret
Body:
{
  "userId": "user123",
  "sessionId": "session1",
  "message": "What's the weather?",
  "context": {
    "currentApp": "com.android.chrome",
    "clipboard": "https://example.com"
  }
}

Response: Server-Sent Events stream
data: {"type":"chunk","text":"The weather..."}
data: {"type":"done","code":0}
```

## How It Works

1. Backend forwards AI queries to worker
2. Worker spawns Claude CLI in isolated session
3. Claude processes query with context
4. Response streams back via SSE
5. Extensions can inject responses before Claude

## Sessions

Sessions are stored in `~/.launcher-worker/sessions/`:
```
sessions/
  ├── user123/
  │   ├── main/
  │   └── secondary/
  └── user456/
      └── main/
```

Each session maintains conversation history with Claude.

## Quota Management

Worker detects Claude quota errors and returns:
```json
{
  "type": "error",
  "error": "QUOTA_EXCEEDED",
  "message": "AI usage limit reached..."
}
```

## Production Deployment

Deploy to GCP e2-micro VM (free tier):
```bash
# SSH into VM
ssh your-vm

# Install Node.js
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Clone repo
git clone <repo-url>
cd launcher-worker

# Install dependencies
npm install

# Install Claude CLI
npm install -g @anthropic-ai/claude-code
claude auth

# Run with PM2
npm install -g pm2
pm2 start server.js --name launcher-worker
pm2 startup
pm2 save
```

## Security

- Worker requires `x-worker-secret` header
- Only accessible from backend (not public)
- Sessions isolated per user
- No data persistence beyond sessions
