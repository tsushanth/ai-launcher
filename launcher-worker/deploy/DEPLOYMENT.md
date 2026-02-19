# AI Launcher Worker - GCP Deployment Guide

Complete guide to deploy the launcher worker on Google Cloud Platform (free tier).

## Prerequisites

- ✅ GCP account with billing enabled (for free tier)
- ✅ `gcloud` CLI installed and authenticated
- ✅ GCP project created

## Quick Start

### Step 1: Set Your GCP Project ID

```bash
export GCP_PROJECT_ID="your-project-id"
export VM_NAME="launcher-worker"
export ZONE="us-west1-b"
```

### Step 2: Create the VM

From your **local machine**, run:

```bash
cd ~/Documents/GitHub/ai-launcher/launcher-worker/deploy
./setup-vm.sh
```

This will:
- Create an e2-micro VM (free tier eligible)
- Configure firewall rules for port 3456
- Show you the external IP address

**Expected output:**
```
✅ VM created
✅ Firewall rule created
📍 VM External IP: 34.x.x.x
```

### Step 3: SSH into the VM

```bash
gcloud compute ssh launcher-worker --project=$GCP_PROJECT_ID --zone=$ZONE
```

### Step 4: Install Dependencies on VM

Once inside the VM, download and run the install script:

```bash
# Download the install script
curl -o install.sh https://raw.githubusercontent.com/tsushanth/ai-launcher/main/launcher-worker/deploy/install-on-vm.sh

# Or if not pushed to GitHub yet, we'll copy files manually (see below)

# Make it executable
chmod +x install.sh

# Run it
./install.sh
```

**Or copy files manually from your local machine:**

From your **local machine** (in a new terminal):
```bash
# Copy the entire launcher-worker directory to VM
gcloud compute scp --recurse ~/Documents/GitHub/ai-launcher/launcher-worker \
  launcher-worker:/home/$USER/ \
  --project=$GCP_PROJECT_ID \
  --zone=$ZONE
```

Then back in the **VM terminal**:
```bash
cd ~/launcher-worker/deploy
chmod +x install-on-vm.sh
./install-on-vm.sh
```

### Step 5: Set Up the Worker

Still inside the VM:

```bash
cd ~/launcher-worker

# Install Node.js dependencies
npm install

# Configure environment
cp .env.example .env
nano .env  # Set WORKER_SECRET to a secure random string
```

**Important:** Update `.env`:
```bash
WORKER_PORT=3456
WORKER_SECRET=your-secure-random-secret-here-change-this
NODE_ENV=production
```

### Step 6: Authenticate Claude CLI on the Headless VM

On a headless Linux VM, `claude auth` cannot open a browser. Instead, Claude CLI on Linux reads credentials from a **plaintext JSON file** at `~/.claude/.credentials.json` (it does not use the OS keychain).

**The easiest approach: copy credentials from your local Mac.**

On your **local Mac** (in a new terminal), export the credentials:

```bash
# On your local Mac — read OAuth credentials from macOS keychain
security find-generic-password -s "claude.ai" -w 2>/dev/null | python3 -c "
import sys, json
raw = sys.stdin.read().strip()
# The keychain value is the JSON credentials blob
print(raw)
"
```

If that doesn't produce output, run `claude auth status` locally and grab the credentials from:
```bash
cat ~/.claude/.credentials.json   # may exist on Mac too
```

Then on the **VM**, write the credentials file:

```bash
mkdir -p ~/.claude
cat > ~/.claude/.credentials.json << 'EOF'
{"claudeAiOauth":{"accessToken":"sk-ant-oat01-...","refreshToken":"sk-ant-ort01-...","expiresAt":1234567890000,"scopes":["user:inference","user:profile"],"subscriptionType":"max"},"organizationUuid":"..."}
EOF
```

Replace the values with what you copied from your Mac. Then verify:

```bash
claude auth status
# Should print: loggedIn: true
```

> **Why this works:** On Linux, Claude CLI skips the OS keychain and reads `~/.claude/.credentials.json` directly. On macOS it reads from Keychain, but the file path still works as a fallback on both platforms.

### Step 7: Test the Worker

```bash
# Test locally on VM
curl http://localhost:3456/health
```

**Expected response:**
```json
{
  "status": "healthy",
  "service": "launcher-worker",
  "claudePath": "/usr/local/bin/claude",
  "sessionsDir": "/home/username/.launcher-worker/sessions",
  "timestamp": "2026-02-16T..."
}
```

### Step 8: Start with PM2

```bash
# Start the worker
pm2 start server.js --name launcher-worker

# Check status
pm2 status

# View logs
pm2 logs launcher-worker

# Make it auto-start on reboot
pm2 startup
# Follow the instructions (run the command it shows)

# Save the PM2 process list
pm2 save
```

### Step 9: Test from Your Computer

From your **local machine**, test the external endpoint:

```bash
# Get the external IP
EXTERNAL_IP=$(gcloud compute instances describe launcher-worker \
  --project=$GCP_PROJECT_ID \
  --zone=$ZONE \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)')

echo "Worker URL: http://$EXTERNAL_IP:3456"

# Test health endpoint
curl http://$EXTERNAL_IP:3456/health
```

### Step 10: Update Backend Configuration

Back on your **local machine**, update the backend .env:

```bash
cd ~/Documents/GitHub/ai-launcher/launcher-backend
nano .env
```

Set:
```
LAUNCHER_WORKER_URL=http://YOUR_VM_EXTERNAL_IP:3456
WORKER_SECRET=same-secret-as-vm-env-file
```

---

## Verification Checklist

- [ ] VM created and running
- [ ] Firewall allows port 3456
- [ ] Node.js 20 installed
- [ ] Claude CLI installed and authenticated
- [ ] Worker dependencies installed (`npm install`)
- [ ] `.env` file configured
- [ ] Worker starts successfully
- [ ] Health endpoint returns `200 OK`
- [ ] PM2 configured for auto-restart
- [ ] Backend `.env` updated with VM IP

---

## Troubleshooting

### Claude CLI Not Found

```bash
# Check if installed
which claude
claude --version

# If not found, reinstall
sudo npm install -g @anthropic-ai/claude-code
```

### Claude Auth Failed / `loggedIn: false` on VM

On Linux VMs `claude auth` opens a browser URL but doesn't write credentials automatically. The fix is to write `~/.claude/.credentials.json` directly (see Step 6 above).

```bash
# Verify credentials are in place
cat ~/.claude/.credentials.json   # should show your token JSON

# Check auth status
claude auth status   # should print loggedIn: true

# If the access token has expired, re-run claude auth on your LOCAL Mac
# and copy the fresh credentials file to the VM
```

### Worker Won't Start

```bash
# Check logs
pm2 logs launcher-worker

# Common issues:
# - Port 3456 already in use: sudo lsof -i :3456
# - Missing .env file: cp .env.example .env
# - Wrong Node version: node --version (should be 20+)
```

### Can't Access from Internet

```bash
# Check firewall rule
gcloud compute firewall-rules describe allow-launcher-worker --project=$GCP_PROJECT_ID

# Check VM is running
gcloud compute instances list --project=$GCP_PROJECT_ID

# Check worker is listening
sudo netstat -tulpn | grep 3456
```

### Worker Crashes

```bash
# View crash logs
pm2 logs launcher-worker --lines 100

# Restart worker
pm2 restart launcher-worker

# Check PM2 status
pm2 status
```

---

## Updating the Worker

To update the worker code:

```bash
# SSH into VM
gcloud compute ssh launcher-worker --project=$GCP_PROJECT_ID --zone=$ZONE

# Pull latest code (if using git)
cd ~/launcher-worker
git pull

# Or copy files from local
# (from local machine):
# gcloud compute scp server.js launcher-worker:~/launcher-worker/ --zone=$ZONE

# Restart
pm2 restart launcher-worker
```

---

## Monitoring

### Check Worker Status

```bash
pm2 status
pm2 logs launcher-worker --lines 50
```

### Check Resource Usage

```bash
pm2 monit  # Interactive monitor
top        # System resources
df -h      # Disk space
```

### View Sessions

```bash
ls -lah ~/.launcher-worker/sessions/
du -sh ~/.launcher-worker/sessions/*
```

---

## Cost Estimate

**e2-micro VM (free tier eligible):**
- First 744 hours/month: **FREE**
- 30GB disk: **~$1.50/month**
- Egress (first 1GB free, then ~$0.12/GB)

**Total:** ~$2-5/month depending on usage

---

## Next Steps

After deployment:
1. ✅ Test the worker from your local backend
2. ✅ Update Android app to use VM IP
3. ✅ Test end-to-end AI chat flow
4. ✅ Monitor logs for any issues

## Support

Issues? Check:
- Worker logs: `pm2 logs launcher-worker`
- Health endpoint: `curl http://localhost:3456/health`
- Firewall rules: `gcloud compute firewall-rules list`
