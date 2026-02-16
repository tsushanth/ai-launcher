# AI Launcher Backend

Standalone Express.js backend for AI Launcher.

## Features

- AI chat endpoint (proxies to worker)
- Theme marketplace
- Extension marketplace
- User settings sync
- Supabase integration (optional)

## Setup

### 1. Install Dependencies

```bash
cd launcher-backend
npm install
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env with your configuration
```

### 3. Run Development Server

```bash
npm run dev
```

Server runs on `http://localhost:3000`

## API Endpoints

### Health Check
```
GET /health
```

### AI Chat
```
POST /api/launcher/chat
{
  "userId": "user123",
  "message": "What's 2+2?",
  "context": {
    "currentApp": "com.android.chrome",
    "clipboard": "some text",
    "installedApps": ["com.app1", "com.app2"]
  }
}
```

### Themes
```
GET /api/launcher/themes
POST /api/launcher/themes/generate
```

### Extensions
```
GET /api/launcher/extensions
```

## Architecture

```
Android App → Backend → Worker → Claude CLI
```

Backend forwards AI queries to the worker, which runs Claude CLI in isolated sessions.

## Development

```bash
npm run dev  # Auto-reload on changes
npm start    # Production mode
```
