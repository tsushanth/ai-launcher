# AI Launcher - AI Setup Guide

This guide covers setting up the AI features. For the Android app install, see the [README](README.md).

## How It Works

The **launcher-worker** is a small Node.js server you self-host anywhere. The Android app connects to it directly — no intermediary needed for AI chat.

```
Android App  →  (HTTP/SSE)  →  launcher-worker  →  AI provider of your choice
     ↑                               ↑
  Settings screen             PROVIDER env var
  (paste URL here)
```

When you send a message:
1. Android reads the **Worker URL** you set in Settings → AI Assistant
2. Posts your message + launcher context (current app, clipboard, etc.) to `POST /chat`
3. Worker streams the AI response back word-by-word as Server-Sent Events
4. Android displays the response as it arrives

### Deployment Options

Run the worker wherever makes sense for you:

| Where | Provider | Cost | Best for |
|-------|----------|------|----------|
| Your Mac/PC | `claude-cli` | Free (subscription) | Development |
| Mac mini home server | `claude-cli` | Free (subscription) | Always-on personal use |
| GCP e2-micro VM | `claude-cli` | ~$0/month (free tier) | Shared / production |
| Any server | `anthropic-api` | Pay-per-token | Teams, metered usage |
| Any server | `openai-compatible` | Varies | OpenRouter, LM Studio |
| Your local machine | `ollama` | Free | Privacy-first, local models |

### Choosing a Provider

Set `PROVIDER` in your `launcher-worker/.env`:

| `PROVIDER` | What runs | Requires |
|-----------|-----------|---------|
| `claude-cli` | Claude Code CLI process | `claude auth` (Anthropic subscription) |
| `anthropic-api` | Anthropic Messages API | `API_KEY` from console.anthropic.com |
| `ollama` | Local Ollama server | Ollama installed, `MODEL` set |
| `openai-compatible` | Any OpenAI-compatible endpoint | `API_BASE_URL` + `API_KEY` |

---

## Table of Contents

1. [Run the Worker](#1-run-the-worker)
   - [Option A: Claude CLI (subscription)](#option-a-claude-cli)
   - [Option B: Anthropic API key](#option-b-anthropic-api)
   - [Option C: Ollama (local models)](#option-c-ollama)
   - [Option D: OpenAI-compatible server](#option-d-openai-compatible)
2. [Deploy to GCP VM (always-on)](#2-deploy-to-gcp-vm)
3. [Point the Android App to Your Worker](#3-point-the-android-app-to-your-worker)
4. [Launcher Backend (optional)](#4-launcher-backend-optional)
5. [Troubleshooting](#5-troubleshooting)

---

## 1. Run the Worker

### Prerequisites (all options)

- **Node.js 20+** — https://nodejs.org or `brew install node`

```bash
cd ai-launcher/launcher-worker
npm install
cp .env.example .env
```

Pick a provider below and edit `.env` accordingly.

---

### Option A: Claude CLI

Uses your Claude subscription — no API key billing.

```bash
# Install Claude Code CLI
npm install -g @anthropic-ai/claude-code

# Authenticate
claude auth
```

`.env`:
```
PROVIDER=claude-cli
WORKER_PORT=3456
WORKER_SECRET=your-secret-here
```

```bash
node server.js
# 🤖 Launcher Worker running on port 3456
# 🔌 Provider: claude-cli
# ✅ Claude CLI found: /usr/local/bin/claude
```

---

### Option B: Anthropic API

Use an Anthropic API key. Any server with internet access — no Claude CLI needed.

`.env`:
```
PROVIDER=anthropic-api
MODEL=claude-opus-4-6
API_KEY=sk-ant-your-api-key-here
WORKER_PORT=3456
WORKER_SECRET=your-secret-here
```

```bash
node server.js
# 🔌 Provider: anthropic-api | Model: claude-opus-4-6
```

---

### Option C: Ollama

Run open-source models locally. Completely free, completely private.

```bash
# Install Ollama: https://ollama.ai
ollama pull llama3.2   # or mistral, phi3, gemma3, etc.
```

`.env`:
```
PROVIDER=ollama
MODEL=llama3.2
WORKER_PORT=3456
WORKER_SECRET=your-secret-here
```

```bash
node server.js
# 🔌 Provider: ollama | Model: llama3.2
# 📡 Ollama base URL: http://localhost:11434/v1
```

---

### Option D: OpenAI-compatible server

Works with OpenRouter, LM Studio, OpenAI, or any compatible API.

`.env` for OpenRouter:
```
PROVIDER=openai-compatible
MODEL=anthropic/claude-opus-4-6
API_KEY=sk-or-your-openrouter-key
API_BASE_URL=https://openrouter.ai/api/v1
WORKER_PORT=3456
WORKER_SECRET=your-secret-here
```

`.env` for LM Studio (local):
```
PROVIDER=openai-compatible
MODEL=local-model
API_BASE_URL=http://localhost:1234/v1
WORKER_PORT=3456
WORKER_SECRET=your-secret-here
```

---

### Verify the Worker

```bash
curl http://localhost:3456/health
```

```json
{
  "status": "healthy",
  "service": "launcher-worker",
  "provider": "claude-cli",
  "model": "claude-cli",
  "timestamp": "..."
}
```

### Test AI Chat

```bash
curl -X POST http://localhost:3456/chat \
  -H "Content-Type: application/json" \
  -H "x-worker-secret: your-secret-here" \
  -d '{
    "userId": "test",
    "message": "What can you help me with?",
    "context": { "currentApp": "Home Screen" }
  }'
```

---

## 2. Deploy to GCP VM

Run the worker always-on on a free GCP e2-micro VM. Good for sharing with family or as your personal always-available AI.

**Prerequisites:**
- GCP account with billing enabled
- `gcloud` CLI: https://cloud.google.com/sdk/docs/install

```bash
export GCP_PROJECT_ID="your-project-id"
export VM_NAME="launcher-worker"
export ZONE="us-west1-b"

# Create VM + firewall rule
cd ai-launcher/launcher-worker/deploy
./setup-vm.sh
```

```bash
# SSH in
gcloud compute ssh $VM_NAME --project=$GCP_PROJECT_ID --zone=$ZONE
```

```bash
# Copy code to VM (from local machine in another terminal)
gcloud compute scp --recurse \
  ~/Documents/GitHub/ai-launcher/launcher-worker \
  $VM_NAME:/home/$USER/ \
  --project=$GCP_PROJECT_ID --zone=$ZONE
```

```bash
# On the VM: install dependencies
curl -o install.sh https://raw.githubusercontent.com/tsushanth/ai-launcher/main/launcher-worker/deploy/install-on-vm.sh
chmod +x install.sh && ./install.sh

cd ~/launcher-worker
npm install
cp .env.example .env
nano .env   # Set PROVIDER, WORKER_SECRET, etc.
```

```bash
# Start with PM2 (survives reboots)
pm2 start server.js --name launcher-worker
pm2 startup    # run the command it shows
pm2 save
```

```bash
# Get external IP
EXTERNAL_IP=$(gcloud compute instances describe $VM_NAME \
  --project=$GCP_PROJECT_ID --zone=$ZONE \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)')

echo "Your worker URL: http://$EXTERNAL_IP:3456"
curl http://$EXTERNAL_IP:3456/health
```

---

## 3. Point the Android App to Your Worker

1. Open AI Launcher on your device
2. Tap the gear icon (⚙) → **Settings**
3. Scroll to **AI Assistant**
4. Enter your **AI Worker URL** — e.g. `http://34.x.x.x:3456` or `http://192.168.1.5:3456`
5. Enter your **Worker Secret** (same value as `WORKER_SECRET` in `.env`)
6. Tap **Test Connection**

You should see: `Connected: claude-cli` (or whichever provider you set).

The settings are saved immediately in SharedPreferences — no restart needed.

---

## 4. Launcher Backend (optional)

The `launcher-backend` is optional. It is intended for future features like cloud sync, theme marketplace, and extension management. AI chat goes **directly** from the Android app to the worker.

```bash
cd ai-launcher/launcher-backend
npm install
cp .env.example .env
node server.js
# 🚀 Launcher Backend running on port 3000
```

---

## 5. Troubleshooting

### "Test Connection" fails

- Confirm the worker is running: `curl http://YOUR_URL:3456/health`
- Check the URL has no trailing slash and includes the port
- Check `WORKER_SECRET` in `.env` matches what you entered in Settings
- On a GCP VM: verify the firewall rule exists for port 3456:
  ```bash
  gcloud compute firewall-rules list --project=$GCP_PROJECT_ID
  ```

### Worker won't start

**Port in use:**
```bash
lsof -i :3456 && kill -9 <PID>
```

**Claude CLI not found** (for `claude-cli` provider):
```bash
which claude
npm install -g @anthropic-ai/claude-code
claude auth
```

**Ollama not responding:**
```bash
ollama serve         # start Ollama if not running
ollama list          # check models are downloaded
```

### GCP VM — worker crashes

```bash
pm2 logs launcher-worker --lines 100
pm2 describe launcher-worker
```

### Extensions screen shows "0 installed"

Navigate away from the Extensions screen and back. The 3 built-in extensions (Calculator, Weather, Notes) load from the registry on first view.

---

## Cost Summary

| Deployment | Provider | Cost |
|-----------|----------|------|
| Your Mac/PC | claude-cli | Free (subscription) |
| GCP e2-micro VM | claude-cli | ~$1.50/month (disk only) |
| Any server | anthropic-api | ~$0.01-0.05 per conversation |
| Local machine | ollama | Free |
| OpenRouter | openai-compatible | Varies by model |

---

## Quick Reference

| Item | Value |
|------|-------|
| Worker default port | 3456 |
| Backend default port | 3000 |
| Worker secret pref key | `ai_worker_secret` |
| Worker URL pref key | `ai_worker_url` |
| SharedPreferences file | `launcher_prefs` |

*For GCP-specific details see [launcher-worker/deploy/DEPLOYMENT.md](launcher-worker/deploy/DEPLOYMENT.md)*

*For GCP-specific deployment details, see [launcher-worker/deploy/DEPLOYMENT.md](launcher-worker/deploy/DEPLOYMENT.md)*
