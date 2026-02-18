#!/bin/bash
# AI Launcher Worker - VM Installation Script
# Run this INSIDE the GCP VM after SSH'ing in

set -e

echo "🤖 Installing AI Launcher Worker on GCP VM"
echo "==========================================="
echo ""

# Update system
echo "📦 Updating system packages..."
sudo apt-get update -qq
sudo apt-get upgrade -y -qq

# Install Node.js 20
echo "📦 Installing Node.js 20..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

echo "✅ Node.js installed: $(node --version)"
echo "✅ npm installed: $(npm --version)"
echo ""

# Install Git (if not already installed)
echo "📦 Installing Git..."
sudo apt-get install -y git

# Install PM2 for process management
echo "📦 Installing PM2..."
sudo npm install -g pm2

echo "✅ PM2 installed: $(pm2 --version)"
echo ""

# Install Claude CLI
echo "🤖 Installing Claude CLI..."
sudo npm install -g @anthropic-ai/claude-code

echo "✅ Claude CLI installed"
echo ""

# Clone the repository (you'll need to do this manually or provide repo URL)
echo "📥 Repository Setup"
echo "==================="
echo ""
echo "You have two options:"
echo ""
echo "Option 1: Clone from GitHub (if you've pushed the code)"
echo "  git clone https://github.com/tsushanth/ai-launcher.git"
echo "  cd ai-launcher/launcher-worker"
echo ""
echo "Option 2: Copy files manually"
echo "  On your local machine, run:"
echo "  gcloud compute scp --recurse ~/Documents/GitHub/ai-launcher/launcher-worker \\"
echo "    launcher-worker:/home/\$USER/ --zone=us-west1-b"
echo ""
echo "After copying files:"
echo "  cd ~/launcher-worker"
echo "  npm install"
echo "  cp .env.example .env"
echo "  nano .env  # Edit WORKER_SECRET"
echo ""
echo "Then authenticate Claude CLI:"
echo "  claude auth"
echo ""
echo "Finally, start the worker with PM2:"
echo "  pm2 start server.js --name launcher-worker"
echo "  pm2 startup  # Follow the instructions"
echo "  pm2 save"
echo ""
echo "Check status:"
echo "  pm2 status"
echo "  pm2 logs launcher-worker"
echo ""
echo "Test health endpoint:"
echo "  curl http://localhost:3456/health"
echo ""
echo "✅ Installation complete! Follow the instructions above to finish setup."
