# Production Deployment Setup

This guide covers setting up automatic deployment with health verification.

## 📋 Prerequisites

- Production server with Podman installed
- SSH access to production server
- GitLab project with CI/CD enabled

## 🔐 Step 1: Generate SSH Key for GitLab CI

On your local machine or GitLab runner:

```bash
# Generate SSH key (no passphrase for automation)
ssh-keygen -t ed25519 -C "gitlab-ci-deployment" -f ~/.ssh/gitlab_deploy_key -N ""

# This creates:
# - Private key: ~/.ssh/gitlab_deploy_key
# - Public key: ~/.ssh/gitlab_deploy_key.pub
```

## 🖥️ Step 2: Configure Production Server

On your production server:

```bash
# Add GitLab CI's public key to authorized_keys
cat >> ~/.ssh/authorized_keys << 'EOF'
# Paste the content of gitlab_deploy_key.pub here
EOF

chmod 600 ~/.ssh/authorized_keys

# Create deployment directory
mkdir -p ~/lineage
cd ~/lineage

# Download docker-compose file
curl -o docker-compose.yml https://gitlab.com/mfraser/lineage/-/raw/production/docker-compose.simple.yml

# Create .env file
cat > .env << 'EOF'
CONTAINER_NAME=lineage-prod
APP_PORT=8080
IMAGE_NAME=registry.ftco.ca/mfraser/lineage
IMAGE_TAG=production
DB_PASSWORD=YourSecurePassword123
EOF

# Login to GitLab registry
echo "YOUR_DEPLOY_TOKEN" | podman login -u deploy-username --password-stdin registry.ftco.ca
```

## 🔧 Step 3: Configure GitLab CI/CD Variables

In GitLab:

**Go to: Your Project → Settings → CI/CD → Variables**

Add these variables:

### Required Variables:

| Variable | Value | Protected | Masked | Description |
|----------|-------|-----------|--------|-------------|
| `SSH_PRIVATE_KEY` | (private key content) | ✅ Yes | ✅ Yes | Private key from Step 1 |
| `SSH_USER` | `masonfraser` | ✅ Yes | ❌ No | SSH username on production server |
| `PRODUCTION_HOST` | `192.168.2.208` | ✅ Yes | ❌ No | Production server IP/hostname |

### How to Add SSH_PRIVATE_KEY:

```bash
# Display private key
cat ~/.ssh/gitlab_deploy_key

# Copy the entire output including:
# -----BEGIN OPENSSH PRIVATE KEY-----
# ... content ...
# -----END OPENSSH PRIVATE KEY-----
```

In GitLab Variables:
1. Click "Add variable"
2. Key: `SSH_PRIVATE_KEY`
3. Value: Paste the entire private key
4. Type: Variable
5. Environment scope: All
6. Protect variable: ✅ Yes
7. Mask variable: ✅ Yes
8. Expand variable reference: ❌ No

### Existing Variables (Already Set):

These should already be configured:

| Variable | Description |
|----------|-------------|
| `CI_REGISTRY_USER` | GitLab username or deploy token |
| `CI_REGISTRY_PASSWORD` | GitLab token or deploy token password |

## 🚀 Step 4: Test Deployment

### Push to Production Branch

```bash
# Merge your changes to production branch
git checkout production
git merge develop
git push origin production
```

### Monitor Pipeline

1. Go to **GitLab → CI/CD → Pipelines**
2. Watch the stages execute:
   - ✅ Build (3-5 min)
   - ✅ Test (2-4 min)
   - ✅ Quality (optional)
   - ✅ Package (5-8 min)
   - ✅ Deploy (2-3 min) ← **New stage**

### What Deploy Stage Does

1. **SSH into production server**
2. **Pull latest image** from GitLab registry
3. **Restart containers** with new image
4. **Wait 60 seconds** for startup
5. **Verify health** - Retry up to 10 times
   - Checks `http://PRODUCTION_HOST:8080/actuator/health`
   - Verifies `{"status":"UP"}` response
6. **Report success/failure** to GitLab

### Deployment Outcome

**If Successful:**
- ✅ Pipeline shows green checkmark
- ✅ Environment shows as "Active"
- ✅ Can view at `http://PRODUCTION_HOST:8080`

**If Failed:**
- ❌ Pipeline shows red X
- ❌ Logs show health check failures
- ❌ Last 50 lines of container logs displayed
- ❌ Deployment doesn't proceed

## 📊 Monitoring Deployments

### View Active Environments

**GitLab → Deployments → Environments**

You'll see:
- Environment name: `production`
- Status: Active/Inactive
- Last deployment time
- Deployed branch/commit
- URL to access application

### View Deployment History

**GitLab → Deployments → Environments → production → Deployment history**

See all:
- Deployment times
- Success/failure status
- Who triggered deployment
- Commit deployed

### Stop Environment (Manual)

If you need to stop the production environment:

1. Go to **Environments → production**
2. Click **Stop** button
3. Confirms and runs `stop:production` job
4. Executes `podman-compose down` on server

## 🔄 Deployment Workflow

```
Developer → Pushes to 'production' branch
    ↓
GitLab CI/CD Pipeline starts
    ↓
├─ Build stage (creates artifacts)
├─ Test stage (runs tests)
├─ Quality stage (SonarQube)
├─ Package stage (builds & pushes Docker image)
└─ Deploy stage → 
      ├─ SSH to server
      ├─ Pull new image
      ├─ Restart containers
      ├─ Wait for startup
      ├─ Health check (10 retries)
      └─ ✅ Success or ❌ Failure
           ↓
       GitLab shows status
           ↓
       Email notification (if configured)
```

## 🛡️ Security Best Practices

### SSH Key Management

✅ **DO:**
- Use dedicated key for CI/CD (not your personal key)
- Use ed25519 keys (more secure than RSA)
- Restrict key to specific command (advanced)
- Rotate keys every 90 days
- Store private key in GitLab Variables (masked & protected)

❌ **DON'T:**
- Use personal SSH keys
- Share keys between projects
- Store keys in repository
- Use keys with passphrase (automation needs no passphrase)

### Variable Protection

- `SSH_PRIVATE_KEY`: Protected ✅ + Masked ✅
- `PRODUCTION_HOST`: Protected ✅
- `SSH_USER`: Protected ✅

Protected variables only work on protected branches (production).

### Server Security

```bash
# Limit SSH key to specific IP (GitLab runner)
# Edit ~/.ssh/authorized_keys:
from="gitlab-runner-ip" ssh-ed25519 AAAA...

# Disable password auth in SSH
sudo nano /etc/ssh/sshd_config
# Set: PasswordAuthentication no
sudo systemctl restart sshd

# Use firewall
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

## 📝 Troubleshooting

### Pipeline Fails at Deploy Stage

**Error: "Permission denied (publickey)"**

Solution:
```bash
# Verify SSH_PRIVATE_KEY in GitLab variables
# Ensure public key is in ~/.ssh/authorized_keys on server
# Check SSH connection manually:
ssh -i ~/.ssh/gitlab_deploy_key $SSH_USER@$CONTAINER_PROJECT_HOST
```

**Error: "Health check failed"**

Solution:
```bash
# SSH to server and check logs
ssh $SSH_USER@$CONTAINER_PROJECT_HOST
cd ~/lineage
podman-compose logs app

# Check if container is running
podman ps

# Check health manually
curl http://localhost:8080/actuator/health
```

**Error: "podman-compose: command not found"**

Solution:
```bash
# Install podman-compose on server
pip3 install podman-compose
# OR
sudo dnf install podman-compose  # RHEL/Fedora
sudo apt install podman-compose  # Debian/Ubuntu
```

### Container Fails to Start

Check logs in pipeline output, or manually:

```bash
ssh $SSH_USER@$CONTAINER_PROJECT_HOST
cd ~/lineage
podman-compose logs --tail=100 app
```

Common issues:
- Database not ready (healthcheck should prevent this)
- Missing environment variables
- Port already in use
- Image pull failed (registry auth)

### Rollback Deployment

If deployment succeeds but app is broken:

```bash
# SSH to server
ssh $SSH_USER@$CONTAINER_PROJECT_HOST
cd ~/lineage

# Stop current deployment
podman-compose down

# Edit .env to use previous image tag
nano .env
# Change: IMAGE_TAG=previous-commit-sha

# Start with old version
podman-compose pull
podman-compose up -d
```

Or trigger a new deployment from a previous commit:

```bash
# Locally, reset production branch to previous commit
git checkout production
git reset --hard PREVIOUS_COMMIT_SHA
git push origin production --force

# Pipeline will redeploy the old version
```

## 🎯 Next Steps

Now that deployment is automated:

1. **Configure notifications:**
   - GitLab → Settings → Integrations → Slack/Discord
   - Get notified on deployment success/failure

2. **Add smoke tests:**
   - Extend deploy script to test critical functionality
   - Verify login works, API responds, etc.

3. **Set up monitoring:**
   - Prometheus + Grafana
   - Uptime monitoring
   - Alert on health check failures

4. **Implement blue-green deployment:**
   - Run new version on different port
   - Test it first
   - Switch traffic if healthy

5. **Document runbooks:**
   - Common issues and solutions
   - Emergency rollback procedures
   - Contact information

## ✅ Verification Checklist

Before going live:

- [ ] SSH key generated and added to server
- [ ] GitLab variables configured (SSH_PRIVATE_KEY, SSH_USER, PRODUCTION_HOST)
- [ ] Production server has docker-compose.yml and .env
- [ ] Test SSH connection from GitLab runner works
- [ ] Test manual deployment on server
- [ ] Push to production branch and verify pipeline
- [ ] Confirm health check passes
- [ ] Access application at production URL
- [ ] Test rollback procedure
- [ ] Document production credentials (securely)
- [ ] Set up monitoring and alerts

---

**Ready to deploy!** 🚀

When you push to `production` branch, deployment happens automatically with full health verification!