# Deployment Architecture Plan

## Current Issues Analysis

### Pipeline Problems
1. **Duplicate Docker Builds**: Two separate jobs (`build-docker` and `build-docker-prod`) doing similar work
2. **Inconsistent Tooling**: Mixing Docker-in-Docker and Kaniko unnecessarily
3. **Fragile SSH Deployment**: `sync-to-prod` job fails silently, tries to install Podman remotely over SSH
4. **Missing Frontend**: Frontend is built but not included in Docker image
5. **No Webhook Strategy**: Manual deployment instead of automated registry pulls
6. **Overly Complex**: Too many conditional branches and optional dependencies

### What Works Well
- ✅ Separate build stages for frontend/backend
- ✅ Artifact management between stages
- ✅ SonarQube integration
- ✅ Multi-branch support

---

## Recommended Architecture

### Overview
```
┌─────────────────────────────────────────────────────────┐
│                    GitLab CI/CD                         │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌─────────────┐           │
│  │  Build   │─▶│   Test   │─▶│Push Registry│           │
│  │Frontend/ │  │Frontend/ │  │             │           │
│  │ Backend  │  │ Backend  │  │             │           │
│  └──────────┘  └──────────┘  └──────┬──────┘           │
│                                      │                  │
└──────────────────────────────────────┼──────────────────┘
                                       │
                                       ▼
                            ┌─────────────────┐
                            │ GitLab Registry │
                            │  Registry Push  │
                            │   Triggers      │
                            │   Webhook       │
                            └────────┬────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────┐
│              Production Server (Podman)                │
│                                                         │
│  ┌──────────────┐         ┌──────────────┐            │
│  │   Webhook    │────────▶│   Instance   │            │
│  │   Receiver   │         │   Manager    │            │
│  └──────────────┘         └──────┬───────┘            │
│                                   │                    │
│  ┌───────────────────────────────┴──────────────────┐ │
│  │                                                   │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │ │
│  │  │ Customer A  │  │ Customer B  │  │Customer C│ │ │
│  │  │ - Frontend  │  │ - Frontend  │  │- Frontend│ │ │
│  │  │ - Backend   │  │ - Backend   │  │- Backend │ │ │
│  │  │ - Postgres  │  │ - Postgres  │  │- Postgres│ │ │
│  │  │ Port: 8080  │  │ Port: 8081  │  │Port: 8082│ │ │
│  │  └─────────────┘  └─────────────┘  └──────────┘ │ │
│  │                                                   │ │
│  └──────────────────┬────────────────────────────────┘ │
│                     │                                  │
│              ┌──────▼──────┐                           │
│              │   Traefik   │                           │
│              │   Reverse   │                           │
│              │    Proxy    │                           │
│              └─────────────┘                           │
│                 :80, :443                              │
└────────────────────────────────────────────────────────┘
```

---

## Solution Components

### 1. Unified Multi-Stage Dockerfile

**Goal**: Single Dockerfile that builds both frontend and backend

**Benefits**:
- Single image to manage
- Consistent versioning
- Simplified deployment
- Smaller attack surface

**Structure**:
```dockerfile
# Stage 1: Build Frontend
FROM node:20 AS frontend-builder
# ... build frontend static assets

# Stage 2: Build Backend
FROM gradle:8.10.0-jdk21-jammy AS backend-builder
# ... build Spring Boot JAR

# Stage 3: Final Runtime
FROM eclipse-temurin:21-jre-jammy
COPY --from=backend-builder /app/build/libs/*.jar app.jar
COPY --from=frontend-builder /app/dist /app/static
# Serve frontend via Spring Boot
```

---

### 2. Simplified GitLab CI Pipeline

**Stages**:
1. **build** - Compile frontend & backend
2. **test** - Run tests, generate coverage
3. **quality** - SonarQube analysis
4. **package** - Build and push Docker image to registry
5. **notify** - (Optional) Send webhook notification

**Key Improvements**:
- ✅ Remove duplicate Docker build jobs
- ✅ Use Kaniko exclusively (no Docker-in-Docker issues)
- ✅ Remove SSH-based deployment
- ✅ Build frontend into Docker image
- ✅ Simpler branching logic
- ✅ Fail-fast approach (no silent failures)

**Pipeline Jobs**:
```yaml
stages:
  - build
  - test
  - quality
  - package

build:backend:
  - Build Spring Boot with Gradle
  
build:frontend:
  - Build Vue.js frontend

test:backend:
  - Run Java tests + coverage

test:frontend:
  - Run Vitest + coverage

quality:sonar:
  - SonarQube scan

package:image:
  - Build multi-stage Docker image with Kaniko
  - Push to registry.ftco.ca
  - Tag: branch name + 'latest' for main/production
```

---

### 3. Webhook-Based Deployment

**Instead of SSH deployment, use webhooks**:

1. **GitLab Registry Webhook**: Configure GitLab to send webhook on image push
2. **Webhook Receiver**: Lightweight service on production server
3. **Automatic Pull & Restart**: Service pulls new image and restarts containers

**Benefits**:
- ✅ No SSH credentials in GitLab
- ✅ Faster deployment (no file copying)
- ✅ More reliable (no network issues)
- ✅ Better logging
- ✅ Can be secured with tokens

**Implementation Options**:

#### Option A: Use Watchtower
- Automatically pulls and restarts containers when registry updates
- Simple setup
- Works with Podman

#### Option B: Custom Webhook Service
- More control
- Can integrate with monitoring
- Per-instance control

---

### 4. Multi-Instance Management

**Instance Structure**:
```
/opt/lineage/
├── instances/
│   ├── customer-a/
│   │   ├── docker-compose.yml
│   │   ├── .env
│   │   └── volumes/
│   │       └── postgres/
│   ├── customer-b/
│   │   ├── docker-compose.yml
│   │   ├── .env
│   │   └── volumes/
│   │       └── postgres/
│   └── ...
├── templates/
│   └── docker-compose.template.yml
└── scripts/
    ├── create-instance.sh
    ├── update-instance.sh
    ├── delete-instance.sh
    └── list-instances.sh
```

**Per-Instance Docker Compose**:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: ${INSTANCE_NAME}-db
    volumes:
      - ./volumes/postgres:/var/lib/postgresql/data
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    networks:
      - instance-network

  app:
    image: registry.ftco.ca/mfraser/lineage:production
    container_name: ${INSTANCE_NAME}-app
    depends_on:
      - postgres
    ports:
      - "${APP_PORT}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${DB_NAME}
      DB_USERNAME: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      SERVER_PORT: 8080
    networks:
      - instance-network
    restart: unless-stopped
    labels:
      - "com.centurylinklabs.watchtower.enable=true"

networks:
  instance-network:
    driver: bridge
```

---

### 5. Reverse Proxy with Traefik

**Why Traefik**:
- Works natively with Docker/Podman
- Automatic service discovery
- Built-in SSL/TLS with Let's Encrypt
- Dynamic configuration
- Better than manual Nginx config for multiple instances

**Setup**:
```yaml
# traefik/docker-compose.yml
version: '3.8'

services:
  traefik:
    image: traefik:v2.10
    container_name: traefik
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/podman/podman.sock:/var/run/docker.sock:ro
      - ./traefik.yml:/traefik.yml:ro
      - ./acme.json:/acme.json
    restart: unless-stopped
```

**Instance Configuration**:
Add labels to each instance's docker-compose:
```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.${INSTANCE_NAME}.rule=Host(`${CUSTOMER_DOMAIN}`)"
  - "traefik.http.services.${INSTANCE_NAME}.loadbalancer.server.port=8080"
```

---

## Implementation Plan

### Phase 1: Fix CI/CD Pipeline (Priority: HIGH)
- [ ] Create new multi-stage Dockerfile with frontend
- [ ] Simplify `.gitlab-ci.yml` to 4 stages
- [ ] Remove SSH deployment job
- [ ] Test pipeline end-to-end
- [ ] Verify images pushed to registry

### Phase 2: Setup Webhook Deployment (Priority: HIGH)
- [ ] Install Watchtower OR build custom webhook receiver
- [ ] Configure GitLab registry webhooks
- [ ] Test automatic deployment
- [ ] Add notification system (Slack/Discord/Email)

### Phase 3: Multi-Instance Infrastructure (Priority: MEDIUM)
- [ ] Create instance management scripts
- [ ] Setup Traefik reverse proxy
- [ ] Create template docker-compose files
- [ ] Document instance provisioning process
- [ ] Test with 2-3 instances

### Phase 4: Automation & Monitoring (Priority: LOW)
- [ ] Add health checks
- [ ] Setup monitoring (Prometheus/Grafana)
- [ ] Create backup automation
- [ ] Document rollback procedures
- [ ] Create admin dashboard (optional)

---

## Alternative Approaches

### Approach A: Separate Frontend/Backend Images
**Pros**: 
- Can scale independently
- Smaller individual images
- Clearer separation of concerns

**Cons**:
- More complexity in orchestration
- Two images to manage per instance
- More network configuration

### Approach B: Bundle Frontend in Backend (Recommended)
**Pros**:
- Simpler deployment
- Single image to manage
- One container per instance
- Easier for small-medium scale

**Cons**:
- Larger image size
- Can't scale frontend/backend independently

**Recommendation**: Start with bundled approach (Approach B) for 5-10 instances, migrate to separate images if scaling beyond 50+ instances.

---

## Expected Results

### Before (Current State)
- ❌ Pipeline fails silently
- ❌ Manual deployment via SSH
- ❌ No frontend deployment
- ❌ Complex, hard to debug
- ❌ No multi-instance support

### After (Proposed Solution)
- ✅ Reliable, fail-fast pipeline
- ✅ Automated webhook deployment
- ✅ Frontend included in image
- ✅ Simple, maintainable CI/CD
- ✅ Easy multi-instance management
- ✅ Can provision new customer in ~5 minutes
- ✅ Automatic updates across all instances

---

## Questions to Finalize Design

1. **Frontend Configuration**: Does frontend need different configs per customer? (API endpoints, feature flags, etc.)
2. **SSL/TLS**: Do you have wildcard SSL cert or need Let's Encrypt per domain?
3. **Backup Strategy**: How should we handle database backups per instance?
4. **Monitoring**: Do you need centralized logging/monitoring across instances?
5. **Resource Limits**: Should each instance have CPU/memory limits?

---

## Next Steps

Once you approve this plan, I recommend:
1. **Quick Win**: Fix the CI/CD pipeline first (Phase 1) - this solves your immediate 6-hour problem
2. **Enable Deployment**: Setup webhook deployment (Phase 2) - makes everything automatic
3. **Scale Later**: Multi-instance setup (Phase 3) - when you have your first customer ready

Would you like me to proceed with implementing Phase 1 (fixing the CI/CD pipeline)?