#!/bin/bash
# AI Launcher Worker - GCP VM Setup Script
# Run this on your LOCAL machine to create and configure the VM

set -e

echo "🚀 AI Launcher Worker - GCP VM Setup"
echo "======================================"
echo ""

# Configuration
PROJECT_ID="${GCP_PROJECT_ID:-your-project-id}"
VM_NAME="${VM_NAME:-launcher-worker}"
ZONE="${ZONE:-us-west1-b}"
MACHINE_TYPE="e2-micro"  # Free tier eligible

echo "Configuration:"
echo "  Project ID: $PROJECT_ID"
echo "  VM Name: $VM_NAME"
echo "  Zone: $ZONE"
echo "  Machine Type: $MACHINE_TYPE (free tier)"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo "❌ gcloud CLI not found. Please install: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

echo "✅ gcloud CLI found"
echo ""

# Create VM
echo "📦 Creating VM instance..."
gcloud compute instances create "$VM_NAME" \
    --project="$PROJECT_ID" \
    --zone="$ZONE" \
    --machine-type="$MACHINE_TYPE" \
    --boot-disk-size=30GB \
    --boot-disk-type=pd-standard \
    --image-family=ubuntu-2204-lts \
    --image-project=ubuntu-os-cloud \
    --tags=http-server,https-server,launcher-worker \
    --metadata=startup-script='#!/bin/bash
# This runs on first boot
apt-get update
' || echo "VM may already exist"

echo ""
echo "✅ VM created (or already exists)"
echo ""

# Create firewall rule for worker port
echo "🔥 Creating firewall rule for port 3456..."
gcloud compute firewall-rules create allow-launcher-worker \
    --project="$PROJECT_ID" \
    --allow=tcp:3456 \
    --target-tags=launcher-worker \
    --description="Allow access to launcher worker on port 3456" || echo "Firewall rule may already exist"

echo ""
echo "✅ Firewall rule created (or already exists)"
echo ""

# Get VM external IP
EXTERNAL_IP=$(gcloud compute instances describe "$VM_NAME" \
    --project="$PROJECT_ID" \
    --zone="$ZONE" \
    --format='get(networkInterfaces[0].accessConfigs[0].natIP)')

echo "📍 VM External IP: $EXTERNAL_IP"
echo ""

echo "✅ VM Setup Complete!"
echo ""
echo "Next steps:"
echo "1. SSH into VM: gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE"
echo "2. Run the install script (I'll show you how)"
echo ""
echo "Your worker will be accessible at: http://$EXTERNAL_IP:3456"
echo ""
