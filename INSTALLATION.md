# AI Launcher - Complete Installation Guide

Step-by-step setup for all three components: Android app, backend server, and AI worker.

---

## Table of Contents

1. [Android App](#1-android-app)
2. [Launcher Backend](#2-launcher-backend)
3. [Launcher Worker (Local)](#3-launcher-worker-local)
4. [Launcher Worker (GCP VM)](#4-launcher-worker-gcp-vm)
5. [Connect Everything](#5-connect-everything)
6. [Verify the Full Stack](#6-verify-the-full-stack)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Android App

### Prerequisites

- **Android Studio** Hedgehog (2023.1) or later
  Download: https://developer.android.com/studio
- **JDK 17** (bundled with Android Studio or install separately)
- **Android SDK** API 26+ (Android 8.0 Oreo minimum)
- **Physical Android device** (recommended) or emulator API 26+
- **USB debugging enabled** on your device
  Settings → Developer Options → USB Debugging

### Build & Install

**Option A: Android Studio (Recommended)**

```bash
# 1. Clone the repo
git clone https://github.com/tsushanth/ai-launcher.git
cd ai-launcher

# 2. Open in Android Studio
# File → Open → Select the ai-launcher folder
# Wait for Gradle sync (first time takes 2-3 minutes)

# 3. Connect device via USB, then click Run (▶️) or press Shift+F10
```

**Option B: Command Line**

```bash
# Prerequisites: Android SDK with adb and ANDROID_HOME set
cd ai-launcher

# Create local.properties with your SDK path
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Build the APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or build + install in one step
./gradlew installDebug
```

### Set as Default Launcher

1. Press the **Home button** on your Android device
2. A dialog appears: "Choose a home app" or "Select launcher"
3. Select **AI Launcher**
4. Tap **Always** (to make it permanent) or **Just once** (to test)

To revert to your previous launcher:
- Settings → Apps → Default apps → Home app → Select your previous launcher

### First Launch

When first set as default:
1. The home screen appears with a search bar at the top and dock at the bottom
2. Swipe up to open the **App Drawer** with fuzzy search
3. Tap the gear icon (⚙️) in the top-right for **Settings**
4. In Settings, tap **Manage Extensions** to see the 3 built-in extensions

---

## 2. Launcher Backend

The backend handles AI routing, theme generation, settings sync, and extension marketplace.

### Prerequisites

- **Node.js 20+**
  Install: https://nodejs.org or `brew install node`
- **npm 9+** (included with Node.js)

### Setup

```bash
cd ai-launcher/launcher-backend

# Install dependencies
npm install

# Configure environment
cp .env.example .env
```

Edit `.env`:

```bash
# Server
PORT=3000

# Worker connection (update with your worker URL after setup)
LAUNCHER_WORKER_URL=http://localhost:3456
WORKER_SECRET=change-this-to-a-secure-random-string

# Supabase (optional - for settings sync and themes)
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

### Start the Backend

```bash
# Development
node server.js

# Or with auto-restart on changes
npm install -g nodemon
nodemon server.js
```

**Expected output:**
```
🚀 Launcher Backend running on port 3000
📡 Worker URL: http://localhost:3456
```

### Verify Backend

```bash
curl http://localhost:3000/health
```

**Expected response:**
```json
{
  "status": "healthy",
  "service": "launcher-backend",
  "workerUrl": "http://localhost:3456",
  "timestamp": "2026-02-17T..."
}
```

---

## 3. Launcher Worker (Local)

The worker runs Claude Code CLI to power AI conversations. Run this locally if you have Claude CLI installed, or on a GCP VM (see Section 4).

### Prerequisites

- **Claude Code CLI** installed and authenticated
  ```bash
  # Check if installed
  claude --version

  # Install if needed
  npm install -g @anthropic-ai/claude-code

  # Authenticate (requires Anthropic account with Claude subscription)
  claude auth
  ```

### Setup

```bash
cd ai-launcher/launcher-worker

# Install dependencies
npm install

# Configure environment
cp .env.example .env
```

Edit `.env`:

```bash
WORKER_PORT=3456
WORKER_SECRET=change-this-to-a-secure-random-string  # Must match backend WORKER_SECRET
NODE_ENV=development
```

**Important:** The `WORKER_SECRET` must be the same value in both `launcher-backend/.env` and `launcher-worker/.env`.

### Start the Worker

```bash
node server.js
```

**Expected output:**
```
🤖 Launcher Worker running on port 3456
✅ Claude CLI found: /usr/local/bin/claude (or your path)
📁 Sessions dir: /Users/you/.launcher-worker/sessions
```

### Verify Worker

```bash
curl http://localhost:3456/health
```

**Expected response:**
```json
{
  "status": "healthy",
  "service": "launcher-worker",
  "claudePath": "/usr/local/bin/claude",
  "sessionsDir": "/Users/you/.launcher-worker/sessions",
  "timestamp": "2026-02-17T..."
}
```

### Test AI Chat

```bash
curl -X POST http://localhost:3456/chat \
  -H "Content-Type: application/json" \
  -H "x-worker-secret: your-worker-secret" \
  -d '{
    "userId": "test-user",
    "sessionId": "test-session",
    "message": "Hello! What can you help me with?",
    "context": {
      "currentApp": "Home Screen",
      "installedApps": []
    }
  }'
```

---

## 4. Launcher Worker (GCP VM)

Run the worker on a free GCP e2-micro VM so the AI is always available.

### Prerequisites

- GCP account with billing enabled (required even for free tier)
- `gcloud` CLI installed and authenticated:
  ```bash
  # Install: https://cloud.google.com/sdk/docs/install
  gcloud auth login
  ```

### Step 1: Create the VM

```bash
export GCP_PROJECT_ID="your-gcp-project-id"
export VM_NAME="launcher-worker"
export ZONE="us-west1-b"

cd ai-launcher/launcher-worker/deploy
./setup-vm.sh
```

This creates:
- `e2-micro` VM (free tier: 744 hours/month free)
- 30GB standard disk
- Ubuntu 22.04 LTS
- Firewall rule allowing port 3456

**Expected output:**
```
✅ VM created (or already exists)
✅ Firewall rule created (or already exists)
📍 VM External IP: 34.x.x.x
```

### Step 2: SSH into the VM

```bash
gcloud compute ssh $VM_NAME --project=$GCP_PROJECT_ID --zone=$ZONE
```

### Step 3: Copy Worker Code to VM

From your **local machine** (open a new terminal):

```bash
gcloud compute scp --recurse \
  ~/Documents/GitHub/ai-launcher/launcher-worker \
  $VM_NAME:/home/$USER/ \
  --project=$GCP_PROJECT_ID \
  --zone=$ZONE
```

### Step 4: Install Dependencies on VM

Inside the **VM terminal**:

```bash
# Download and run install script
curl -o install.sh https://raw.githubusercontent.com/tsushanth/ai-launcher/main/launcher-worker/deploy/install-on-vm.sh
chmod +x install.sh
./install.sh
```

This installs:
- Node.js 20
- npm
- Git
- PM2 (process manager)
- Claude Code CLI

### Step 5: Configure Worker on VM

```bash
cd ~/launcher-worker

# Install Node dependencies
npm install

# Configure environment
cp .env.example .env
nano .env  # or vim .env
```

Set in `.env`:
```bash
WORKER_PORT=3456
WORKER_SECRET=your-secure-random-secret-here   # Copy this to backend .env too
NODE_ENV=production
```

### Step 6: Authenticate Claude CLI on VM

```bash
claude auth
```

This will display a URL like:
```
Visit this URL to authenticate:
https://claude.ai/oauth/authorize?...
```

Open this URL on your **local computer**, sign in to your Anthropic account, and authorize the CLI. The VM will detect the authorization automatically.

**Verify authentication:**
```bash
claude --version
# Should show version without errors
```

### Step 7: Start Worker with PM2

```bash
# Start the worker
pm2 start server.js --name launcher-worker

# Check it's running
pm2 status

# View logs
pm2 logs launcher-worker

# Enable auto-start on VM reboot
pm2 startup
# Run the command it shows you (something like: sudo env PATH=...)

# Save PM2 process list
pm2 save
```

### Step 8: Get VM IP and Test

From your **local machine**:

```bash
EXTERNAL_IP=$(gcloud compute instances describe $VM_NAME \
  --project=$GCP_PROJECT_ID \
  --zone=$ZONE \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)')

echo "Worker URL: http://$EXTERNAL_IP:3456"

# Test health
curl http://$EXTERNAL_IP:3456/health
```

---

## 5. Connect Everything

### Update Backend to Point to Worker

If using the GCP VM worker, update `launcher-backend/.env`:

```bash
LAUNCHER_WORKER_URL=http://YOUR_VM_EXTERNAL_IP:3456
WORKER_SECRET=same-secret-as-worker-env-file
```

Restart the backend after changing `.env`.

### Android App Backend URL

Currently the Android app is configured for local development. When deploying the backend, update the base URL in:

```
app/src/main/java/com/launcher/data/LauncherApi.kt
```

Set `BASE_URL` to your backend URL (e.g., Cloud Run URL or `http://your-server-ip:3000`).

### Start Order

Start in this order:
1. **Worker** first (backend depends on it)
2. **Backend** second (Android app talks to it)
3. **Android app** (connects to backend)

---

## 6. Verify the Full Stack

### Check All Services

```bash
# Backend health
curl http://localhost:3000/health

# Worker health (local or VM)
curl http://localhost:3456/health      # local
curl http://YOUR_VM_IP:3456/health     # GCP VM
```

### Test AI Chat via Backend

```bash
curl -X POST http://localhost:3000/api/launcher/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "message": "What apps do I have installed?",
    "context": {
      "currentApp": "Home Screen"
    }
  }'
```

You should see a streaming SSE response with Claude's reply.

### Android App Verification Checklist

- [ ] App installs without errors
- [ ] App drawer opens (swipe up)
- [ ] App search returns results (type an app name)
- [ ] Can set as default launcher
- [ ] Home screen shows with dock
- [ ] Settings opens (gear icon)
- [ ] Extensions screen shows 3 built-in extensions (Calculator, Weather, Notes)

---

## 7. Troubleshooting

### Android Build Errors

**`sdk.dir` not found:**
```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

**Gradle wrapper not executable:**
```bash
chmod +x gradlew
```

**Build fails with Firebase error:**
The Firebase plugin is disabled by default. If you see Firebase errors, check `app/build.gradle.kts` - the `google-services` plugin line should be commented out.

**Manifest namespace error:**
Ensure `AndroidManifest.xml` has `xmlns:tools="http://schemas.android.com/tools"` on the root `<manifest>` element.

---

### Backend Won't Start

**Port already in use:**
```bash
lsof -i :3000
kill -9 <PID>
```

**Missing .env file:**
```bash
cp .env.example .env
# Edit with your values
```

---

### Worker Won't Start

**Claude CLI not found:**
```bash
# Check paths
which claude
ls ~/.local/bin/claude
ls /opt/homebrew/bin/claude

# If on Mac via VSCode extension
ls ~/.vscode/extensions/ | grep anthropic

# Reinstall
npm install -g @anthropic-ai/claude-code
```

**Claude not authenticated:**
```bash
claude auth
# Follow the OAuth flow
```

**Port 3456 in use:**
```bash
lsof -i :3456
kill -9 <PID>
```

---

### GCP VM Issues

**Can't connect to VM worker from internet:**
```bash
# Verify firewall rule exists
gcloud compute firewall-rules describe allow-launcher-worker \
  --project=$GCP_PROJECT_ID

# Check VM is running
gcloud compute instances list --project=$GCP_PROJECT_ID

# Check worker is listening inside VM
sudo netstat -tulpn | grep 3456
```

**PM2 not starting on reboot:**
```bash
pm2 startup
# Run the exact command it outputs
pm2 save
```

**Worker crashes repeatedly:**
```bash
pm2 logs launcher-worker --lines 100
pm2 describe launcher-worker
```

---

### Extension System

**Extensions screen shows "0 installed":**
This is a known state after first install. The 3 built-in extensions (Calculator, Weather, Notes) are loaded from the registry. If they don't appear, navigate away from the Extensions screen and back.

---

## Cost Summary

| Component | Free Tier | Estimated Cost |
|-----------|-----------|----------------|
| GCP e2-micro VM | 744 hours/month free | $0/month |
| GCP 30GB disk | — | ~$1.50/month |
| Supabase (database) | 500MB free | $0/month |
| Firebase (auth) | 50K MAU free | $0/month |
| Claude API | Uses your Claude subscription | $0 additional |
| **Total** | | **~$1.50-5/month** |

---

## Quick Reference

| Service | Default Port | URL |
|---------|-------------|-----|
| Launcher Backend | 3000 | http://localhost:3000 |
| Launcher Worker | 3456 | http://localhost:3456 |
| GCP VM Worker | 3456 | http://YOUR_VM_IP:3456 |

| File | Purpose |
|------|---------|
| `launcher-backend/.env` | Backend config (ports, secrets, DB keys) |
| `launcher-worker/.env` | Worker config (port, secret) |
| `app/local.properties` | Android SDK path |

---

*For GCP-specific deployment details, see [launcher-worker/deploy/DEPLOYMENT.md](launcher-worker/deploy/DEPLOYMENT.md)*
