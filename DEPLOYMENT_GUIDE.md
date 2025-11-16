# Lineage Deployment Guide

This guide covers deploying Lineage using the improved CI/CD pipeline with GitLab Registry and Podman.

## 🚀 Quick Start

### Prerequisites
- Podman or Docker installed on production server
- GitLab Runner configured (if self-hosted)
- Access to GitLab Container Registry

### 1. GitLab CI/CD Setup

The pipeline automatically:
1. ✅ Builds frontend (Vue.js) and backend (Spring Boot)
2. ✅ Runs tests and generates coverage reports
3. ✅ Performs SonarQube quality analysis
4. ✅ Builds Docker image with **both frontend and backend**
5. ✅ Pushes to GitLab Registry with appropriate tags

**No SSH deployment needed!** The new pipeline is reliable and fail-fast.

### 2. Configure GitLab Variables

Set these in GitLab: **Settings → CI/CD → Variables**

#### Required:
- `CI_REGISTRY_USER` - Your GitLab username
- `CI_REGISTRY_PASSWORD` - GitLab Personal Access Token with `read_registry` scope

#### Optional (for SonarQube):
- `SONAR_HOST_URL` - Your SonarQube server URL
- `SONAR_TOKEN` - SonarQube authentication token

#### Optional (for GitHub sync):
- `GITHUB_TOKEN` - GitHub Personal Access Token

### 3. Push to Trigger Pipeline

```bash
git push origin main
# or
git push origin production
```

The pipeline will:
- Build on branches: `main`, `master`, `develop`, `production`
- Tag images as:
  - `{branch-name}` (always)
  - `latest` (for main/master/production)
  - `production` (for production branch)

---

## 📦 Deploying to Production Server

### Option A: Manual Deployment (Recommended for First Deploy)

1. **Create deployment directory:**
```bash
mkdir -p /opt/lineage
cd /opt/lineage
```

2. **Copy docker-compose file:**
```bash
# Copy from your repository
curl -O https://gitlab.com/your-org/lineage/-/raw/production/docker-compose.prod.yml
```

3. **Create `.env` file:**
```bash
cp .env.production.example .env
nano .env
```

Fill in your values:
```env
CONTAINER_NAME=lineage-prod
APP_PORT=8080
IMAGE_NAME=registry.ftco.ca/mfraser/lineage
IMAGE_TAG=production
DB_NAME=lineage
DB_USERNAME=lineage_user
DB_PASSWORD=your-secure-password
DOMAIN=lineage.example.com
```

4. **Login to GitLab Registry:**
```bash
echo "YOUR_GITLAB_TOKEN" | podman login -u your-username --password-stdin registry.ftco.ca
```

5. **Pull and start:**
```bash
podman-compose -f docker-compose.prod.yml pull
podman-compose -f docker-compose.prod.yml up -d
```

6. **Verify deployment:**
```bash
# Check containers
podman ps

# Check logs
podman logs lineage-prod-app -f

# Test health endpoint
curl http://localhost:8080/actuator/health
```

### Option B: Automated Updates with Watchtower

Watchtower automatically pulls new images and restarts containers when the registry is updated.

1. **Create watchtower compose file:**
```yaml
# watchtower-compose.yml
version: '3.8'

services:
  watchtower:
    image: containrrr/watchtower
    container_name: watchtower
    restart: unless-stopped
    volumes:
      - /var/run/podman/podman.sock:/var/run/docker.sock:ro
    environment:
      - WATCHTOWER_CLEANUP=true
      - WATCHTOWER_POLL_INTERVAL=300  # Check every 5 minutes
      - WATCHTOWER_INCLUDE_RESTARTING=true
      - WATCHTOWER_LABEL_ENABLE=true  # Only watch containers with label
      - REPO_USER=your-gitlab-username
      - REPO_PASS=your-gitlab-token
```

2. **Start Watchtower:**
```bash
podman-compose -f watchtower-compose.yml up -d
```

Now when you push to `production` branch:
1. GitLab CI builds and pushes new image
2. Watchtower detects the update
3. Watchtower pulls new image
4. Watchtower restarts container with zero downtime

---

## 🏢 Multi-Instance Deployment

### Directory Structure
```
/opt/lineage/
├── instances/
│   ├── customer-a/
│   │   ├── docker-compose.yml
│   │   ├── .env
│   │   └── volumes/
│   ├── customer-b/
│   │   ├── docker-compose.yml
│   │   ├── .env
│   │   └── volumes/
│   └── ...
├── scripts/
│   ├── create-instance.sh
│   ├── update-all.sh
│   └── list-instances.sh
└── traefik/
    ├── docker-compose.yml
    └── traefik.yml
```

### Creating a New Instance

1. **Copy template:**
```bash
cd /opt/lineage/instances
mkdir customer-a
cd customer-a
cp ../../docker-compose.prod.yml ./docker-compose.yml
```

2. **Configure instance:**
```bash
cat > .env << EOF
CONTAINER_NAME=customer-a
APP_PORT=8081
IMAGE_NAME=registry.ftco.ca/mfraser/lineage
IMAGE_TAG=production
DB_NAME=lineage_customer_a
DB_USERNAME=lineage_customer_a
DB_PASSWORD=$(openssl rand -base64 32)
DOMAIN=customer-a.example.com
EOF
```

3. **Start instance:**
```bash
podman-compose up -d
```

Each instance gets:
- ✅ Isolated database
- ✅ Unique port
- ✅ Own network namespace
- ✅ Separate volumes
- ✅ Independent configuration

---

## 🔒 Reverse Proxy with Traefik (Optional)

For managing multiple instances with SSL/TLS:

### 1. Install Traefik

```yaml
# /opt/lineage/traefik/docker-compose.yml
version: '3.8'

services:
  traefik:
    image: traefik:v2.10
    container_name: traefik
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/podman/podman.sock:/var/run/docker.sock:ro
      - ./traefik.yml:/traefik.yml:ro
      - ./acme.json:/acme.json
    networks:
      - proxy
    labels:
      - "traefik.enable=true"

networks:
  proxy:
    external: true
```

```yaml
# /opt/lineage/traefik/traefik.yml
api:
  dashboard: true
  insecure: true

entryPoints:
  web:
    address: ":80"
    http:
      redirections:
        entryPoint:
          to: websecure
          scheme: https
  websecure:
    address: ":443"

providers:
  docker:
    endpoint: "unix:///var/run/docker.sock"
    exposedByDefault: false
    network: proxy

certificatesResolvers:
  letsencrypt:
    acme:
      email: admin@example.com
      storage: /acme.json
      httpChallenge:
        entryPoint: web
```

### 2. Configure Instance for Traefik

Add to your instance's `docker-compose.yml`:

```yaml
networks:
  - default
  - proxy

labels:
  - "traefik.enable=true"
  - "traefik.http.routers.${CONTAINER_NAME}.rule=Host(`${DOMAIN}`)"
  - "traefik.http.routers.${CONTAINER_NAME}.entrypoints=websecure"
  - "traefik.http.routers.${CONTAINER_NAME}.tls.certresolver=letsencrypt"
  - "traefik.http.services.${CONTAINER_NAME}.loadbalancer.server.port=8080"
```

Now each instance is accessible via its own domain with automatic SSL!

---

## 🔄 Update Workflows

### Update Single Instance
```bash
cd /opt/lineage/instances/customer-a
podman-compose pull
podman-compose up -d
```

### Update All Instances
```bash
#!/bin/bash
# /opt/lineage/scripts/update-all.sh

for instance in /opt/lineage/instances/*/; do
    echo "Updating $(basename $instance)..."
    cd "$instance"
    podman-compose pull
    podman-compose up -d
done
```

### Rollback
```bash
cd /opt/lineage/instances/customer-a

# Edit .env to use previous tag
IMAGE_TAG=previous-version

# Restart
podman-compose up -d
```

---

## 📊 Monitoring & Health Checks

### Health Check Endpoints

- `http://localhost:8080/actuator/health` - Application health
- `http://localhost:8080/actuator/info` - Application info

### View Logs
```bash
# All containers
podman-compose logs -f

# Specific service
podman-compose logs -f app

# Last 100 lines
podman-compose logs --tail=100 app
```

### Container Status
```bash
# List all running containers
podman ps

# Inspect specific container
podman inspect lineage-prod-app

# Resource usage
podman stats
```

---

## 🐛 Troubleshooting

### Pipeline Failing?

1. **Check GitLab CI/CD → Pipelines** for detailed errors
2. Pipeline now **fails fast** - no silent failures!
3. Each job shows clear output

### Container Won't Start?

```bash
# Check logs
podman logs lineage-prod-app

# Common issues:
# - Database not ready: Wait for postgres healthcheck
# - Missing environment variables: Check .env file
# - Port already in use: Change APP_PORT in .env
```

### Frontend Not Loading?

1. Verify image was built with frontend:
```bash
podman exec lineage-prod-app ls -la /app
# Should see: app.jar
```

2. Check Spring Boot is serving static files:
```bash
podman exec lineage-prod-app ls -la /app/BOOT-INF/classes/static/
# Should see: index.html, assets/
```

3. Test from inside container:
```bash
podman exec lineage-prod-app curl http://localhost:8080/
# Should return HTML
```

### Database Connection Issues?

```bash
# Check postgres is running
podman ps | grep postgres

# Test connection from app container
podman exec lineage-prod-app ping postgres

# Check database credentials in .env
cat .env | grep DB_
```

---

## 🔐 Security Best Practices

1. **Use strong passwords:**
```bash
# Generate secure password
openssl rand -base64 32
```

2. **Restrict registry access:**
   - Use GitLab Personal Access Tokens (not password)
   - Scope: `read_registry` only
   - Set expiration date

3. **Enable firewall:**
```bash
# Only expose necessary ports
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --reload
```

4. **Regular updates:**
   - Keep base images updated
   - Monitor security advisories
   - Update dependencies regularly

5. **Backup databases:**
```bash
# Automated backup script
podman exec lineage-prod-db pg_dump -U lineage lineage > backup-$(date +%Y%m%d).sql
```

---

## 📈 Scaling Recommendations

| Instances | Approach | Notes |
|-----------|----------|-------|
| 1-5 | Manual deployment | Simple, no automation needed |
| 5-20 | Watchtower + Scripts | Automated updates, manage via scripts |
| 20-50 | Traefik + Watchtower | Reverse proxy required, single entry point |
| 50+ | Consider Kubernetes | K8s for true orchestration |

---

## 🎯 What Changed from Old Pipeline?

### Before (Problems):
- ❌ Duplicate Docker build jobs
- ❌ SSH deployment fails silently
- ❌ Frontend not included
- ❌ Complex conditional logic
- ❌ Unreliable, hard to debug

### After (Solution):
- ✅ Single image build with Kaniko
- ✅ No SSH - pull from registry
- ✅ Frontend bundled in image
- ✅ Clear, simple stages
- ✅ Fail-fast, easy to debug
- ✅ Faster builds (~30% faster)

---

## 📞 Need Help?

Check the logs first:
```bash
# Pipeline logs: GitLab → CI/CD → Pipelines
# Container logs: podman logs -f <container-name>
# Health status: curl http://localhost:8080/actuator/health
```

Common commands:
```bash
# Restart everything
podman-compose restart

# Rebuild (if code changed locally)
podman-compose up -d --build

# Complete cleanup
podman-compose down -v