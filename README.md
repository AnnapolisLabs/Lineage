# Lineage - Open Source Requirements Management Tool

A modern, AI-powered requirements management system built with Spring Boot and Vue.js.

## 🚀 Quick Start

### Development
```bash
# Backend
./gradlew bootRun

# Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

Visit: http://localhost:5173

### Production Deployment

**New Simplified Deployment!** We've completely redesigned the CI/CD pipeline for reliability and ease of use.

```bash
# On production server
cd /opt/lineage
podman-compose -f docker-compose.prod.yml pull
podman-compose -f docker-compose.prod.yml up -d
```

That's it! Frontend + Backend + Database all deployed in a single image.

📚 **See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for complete instructions**

---

## 📖 Documentation

### Getting Started
- **[QUICKSTART.md](QUICKSTART.md)** - Quick development setup
- **[API.md](API.md)** - API documentation
- **[MCP_INTEGRATION.md](MCP_INTEGRATION.md)** - Model Context Protocol integration

### Deployment & Operations
- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Complete deployment guide (START HERE)
- **[DEPLOYMENT_ARCHITECTURE_PLAN.md](DEPLOYMENT_ARCHITECTURE_PLAN.md)** - Architecture design & scaling strategy
- **[PIPELINE_TESTING.md](PIPELINE_TESTING.md)** - Testing your deployment
- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Common commands & operations
- **[CHANGES_SUMMARY.md](CHANGES_SUMMARY.md)** - Recent CI/CD improvements

---

## 🎯 What's New?

### CI/CD Pipeline Overhaul (Latest)

We've completely redesigned the GitLab CI/CD pipeline:

**Previous Issues:**
- ❌ Silent failures (6+ hour debugging sessions)
- ❌ Frontend not deploying
- ❌ Fragile SSH deployment
- ❌ Complex, unreliable pipeline

**New Pipeline:**
- ✅ **Reliable** - Fail-fast with clear error messages
- ✅ **Complete** - Frontend + Backend bundled in single image
- ✅ **Simple** - 4 clear stages, no SSH needed
- ✅ **Fast** - ~12 minutes (was 20+ minutes)
- ✅ **Multi-Instance Ready** - Deploy 5-10+ customer instances easily

See **[CHANGES_SUMMARY.md](CHANGES_SUMMARY.md)** for details.

---

## 🏗️ Architecture

### Technology Stack
- **Backend:** Spring Boot 3.5, Java 21, PostgreSQL, Flyway
- **Frontend:** Vue 3, TypeScript, Vite, TailwindCSS
- **Deployment:** Docker/Podman, GitLab CI/CD, GitLab Registry
- **AI Integration:** MCP (Model Context Protocol) compatible

### Single Image Deployment
```
┌─────────────────────────────────────┐
│     Docker Image (450MB)            │
│  ┌──────────────────────────────┐   │
│  │   Vue.js Frontend (Built)    │   │
│  │   → Static files in JAR      │   │
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │   Spring Boot Backend        │   │
│  │   → Serves frontend at /     │   │
│  │   → API endpoints at /api/*  │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

Both frontend and backend are bundled into a single deployable image, served by Spring Boot.

---

## 🚢 CI/CD Pipeline

### Branch Strategy
- **`main`/`master`** - Main development branch → Tags: `main`, `latest`
- **`develop`** - Active development → Tags: `develop`
- **`production`** - Production deployments → Tags: `production`, `latest`

### Pipeline Stages
1. **Build** - Compile frontend (npm) + backend (gradle)
2. **Test** - Run test suites + coverage reports
3. **Quality** - SonarQube analysis
4. **Package** - Build Docker image with Kaniko + Push to registry

### Automatic Deployment
Images pushed to GitLab Registry can be:
- Pulled manually: `podman-compose pull && podman-compose up -d`
- Auto-updated: Use Watchtower for automatic updates

---

## 🏢 Multi-Instance Deployment

Perfect for SaaS with multiple customers:

```bash
# Create new customer instance (takes ~5 minutes)
cd /opt/lineage/instances
mkdir customer-a && cd customer-a
cp ../../docker-compose.prod.yml ./docker-compose.yml

# Configure
cat > .env << EOF
CONTAINER_NAME=customer-a
APP_PORT=8081
DB_NAME=customer_a_db
DB_PASSWORD=$(openssl rand -base64 32)
DOMAIN=customer-a.example.com
EOF

# Deploy
podman-compose up -d
```

Each instance:
- ✅ Isolated database
- ✅ Unique port/domain
- ✅ Own configuration
- ✅ Auto-updates from registry
- ✅ ~5 minute setup

**Scales to 50+ instances** - See [DEPLOYMENT_ARCHITECTURE_PLAN.md](DEPLOYMENT_ARCHITECTURE_PLAN.md) for details.

---

## 🔧 Configuration

### Environment Variables

Production configuration via `.env` file:

```env
# Container
CONTAINER_NAME=lineage-prod
APP_PORT=8080

# Database
DB_NAME=lineage
DB_USERNAME=lineage_user
DB_PASSWORD=secure-password

# Image
IMAGE_NAME=registry.ftco.ca/mfraser/lineage
IMAGE_TAG=production

# Optional: Domain for reverse proxy
DOMAIN=lineage.example.com
```

See [`.env.production.example`](.env.production.example) for complete template.

---

## 🧪 Testing

### Unit Tests
```bash
# Backend
./gradlew test

# Frontend
cd frontend
npm run test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### Coverage Reports
```bash
# Backend
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html

# Frontend
cd frontend
npm run test:coverage
# Report: frontend/coverage/lcov-report/index.html
```

---

## 🔍 Development

### Project Structure
```
lineage/
├── src/main/java/               # Backend source
│   └── com/annapolislabs/lineage/
│       ├── config/              # Configuration
│       ├── controller/          # REST controllers
│       ├── service/             # Business logic
│       ├── mcp/                 # MCP integration
│       └── security/            # Authentication
├── src/main/resources/
│   ├── application.properties   # Dev config
│   ├── application-prod.properties  # Prod config
│   └── db/migration/           # Flyway migrations
├── frontend/                    # Vue.js frontend
│   ├── src/
│   │   ├── components/         # Vue components
│   │   ├── views/              # Page views
│   │   ├── services/           # API clients
│   │   └── stores/             # Pinia stores
│   └── dist/                   # Built static files
├── Dockerfile                   # Multi-stage build
├── .gitlab-ci.yml              # CI/CD pipeline
└── docker-compose.prod.yml     # Production stack
```

### Local Development

1. **Start PostgreSQL:**
```bash
podman-compose -f docker-compose.dev.yml up -d
```

2. **Run Backend:**
```bash
./gradlew bootRun
# Runs on http://localhost:8080
```

3. **Run Frontend:**
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
# Auto-proxies API calls to :8080
```

---

## 🛠️ Common Operations

### Deploy to Production
```bash
podman-compose -f docker-compose.prod.yml pull
podman-compose -f docker-compose.prod.yml up -d
```

### View Logs
```bash
podman-compose logs -f
```

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Database Backup
```bash
podman exec lineage-prod-db pg_dump -U lineage lineage > backup.sql
```

See **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** for more commands.

---

## 🔒 Security

- Non-root user in containers
- JWT-based authentication
- HTTPS support via Traefik
- Environment-based secrets
- SQL injection protection (JPA)
- CORS configured

---

## 📊 Monitoring

### Health Endpoints
- `/actuator/health` - Application health
- `/actuator/info` - Application info

### Metrics (if enabled)
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `./gradlew test && cd frontend && npm test`
5. Push to your fork
6. Create a Pull Request

---

## 📄 License

This project is licensed under the terms specified in [LICENSE](LICENSE).

---

## 🆘 Support

### Documentation
- [Deployment Guide](DEPLOYMENT_GUIDE.md) - Complete deployment instructions
- [Testing Guide](PIPELINE_TESTING.md) - How to test changes
- [Quick Reference](QUICK_REFERENCE.md) - Common commands
- [Architecture Plan](DEPLOYMENT_ARCHITECTURE_PLAN.md) - System design

### Troubleshooting
1. Check container logs: `podman logs <container-name>`
2. Verify health: `curl http://localhost:8080/actuator/health`
3. Review pipeline: GitLab → CI/CD → Pipelines
4. See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) troubleshooting section

---

## ⭐ Features

- ✅ Requirements management with versioning
- ✅ Hierarchical requirement structure
- ✅ Requirement linking and traceability
- ✅ AI-powered assistant (MCP compatible)
- ✅ Export to multiple formats
- ✅ User authentication & authorization
- ✅ RESTful API
- ✅ Modern Vue.js UI
- ✅ PostgreSQL database
- ✅ Docker/Podman deployment
- ✅ Multi-instance support
- ✅ Automatic CI/CD pipeline

---

**Built with ❤️ by Annapolis Labs**
