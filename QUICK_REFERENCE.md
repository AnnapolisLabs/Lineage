# Quick Reference - Common Operations

Quick commands for day-to-day operations with the new pipeline.

## 🚀 Deployment Commands

### Deploy to Production
```bash
# On production server
cd /opt/lineage
podman login registry.ftco.ca
podman-compose -f docker-compose.prod.yml pull
podman-compose -f docker-compose.prod.yml up -d
```

### Check Status
```bash
# View running containers
podman ps

# View logs
podman-compose logs -f

# Health check
curl http://localhost:8080/actuator/health
```

### Restart Services
```bash
# Restart all
podman-compose restart

# Restart app only
podman-compose restart app

# Restart database only
podman-compose restart postgres
```

---

## 🔄 Pipeline Operations

### Trigger Pipeline
```bash
# Commit and push
git add .
git commit -m "Your changes"
git push origin main
```

### View Pipeline Status
GitLab → CI/CD → Pipelines → Click latest pipeline

### Download Artifacts
GitLab → CI/CD → Pipelines → Job → Browse → Download

---

## 🔍 Debugging

### View Container Logs
```bash
# All logs
podman-compose logs

# Tail last 100 lines
podman-compose logs --tail=100 app

# Follow logs in real-time
podman-compose logs -f app

# Only errors
podman-compose logs app | grep -i error
```

### Inspect Container
```bash
# Container details
podman inspect lineage-prod-app

# Resource usage
podman stats lineage-prod-app

# Enter container
podman exec -it lineage-prod-app /bin/sh
```

### Database Access
```bash
# Connect to database
podman exec -it lineage-prod-db psql -U lineage -d lineage

# Backup database
podman exec lineage-prod-db pg_dump -U lineage lineage > backup.sql

# Restore database
cat backup.sql | podman exec -i lineage-prod-db psql -U lineage -d lineage
```

---

## 🏗️ Multi-Instance Operations

### Create New Instance
```bash
# Create directory
cd /opt/lineage/instances
mkdir customer-name
cd customer-name

# Copy compose file
cp ../../docker-compose.prod.yml ./docker-compose.yml

# Create .env
cat > .env << EOF
CONTAINER_NAME=customer-name
APP_PORT=8081
DB_NAME=customer_name_db
DB_USERNAME=customer_name
DB_PASSWORD=$(openssl rand -base64 32)
DOMAIN=customer-name.example.com
EOF

# Start
podman-compose up -d
```

### Update All Instances
```bash
for dir in /opt/lineage/instances/*/; do
    cd "$dir"
    echo "Updating $(basename $dir)..."
    podman-compose pull
    podman-compose up -d
done
```

### List All Instances
```bash
podman ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

---

## 🔐 Registry Operations

### Login
```bash
echo "YOUR_TOKEN" | podman login -u username --password-stdin registry.ftco.ca
```

### Pull Specific Tag
```bash
podman pull registry.ftco.ca/mfraser/lineage:production
podman pull registry.ftco.ca/mfraser/lineage:latest
podman pull registry.ftco.ca/mfraser/lineage:develop
```

### List Tags
GitLab → Packages & Registries → Container Registry

---

## 🧪 Testing

### Test Frontend Build
```bash
cd frontend
npm install
npm run build
ls -la dist/  # Should have index.html
```

### Test Backend Build
```bash
./gradlew clean build -x test
ls -la build/libs/  # Should have *.jar
```

### Test Docker Build Locally
```bash
docker build -t lineage-test .
docker run -p 8080:8080 lineage-test
```

### Test Compose Stack
```bash
podman-compose -f docker-compose.prod.yml up
# Ctrl+C to stop
podman-compose -f docker-compose.prod.yml down -v
```

---

## 📊 Monitoring

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
podman exec lineage-prod-db pg_isready -U lineage

# Container health
podman inspect lineage-prod-app | grep -A 10 Health
```

### Resource Usage
```bash
# Real-time stats
podman stats

# Disk usage
podman system df

# View logs size
du -sh /var/lib/containers/storage/volumes/
```

---

## 🔧 Maintenance

### Cleanup Old Images
```bash
# Remove unused images
podman image prune -a

# Remove stopped containers
podman container prune

# Full cleanup
podman system prune -a
```

### Update Base Images
```bash
# Pull latest postgres
podman pull postgres:15-alpine

# Recreate with new image
podman-compose up -d --force-recreate postgres
```

### Rotate Logs
```bash
# Set log rotation
podman-compose -f docker-compose.prod.yml logs --no-log-prefix > archived-logs-$(date +%Y%m%d).log
podman-compose restart app
```

---

## 🚨 Emergency Procedures

### Complete Restart
```bash
cd /opt/lineage
podman-compose down
podman-compose up -d
```

### Rollback to Previous Version
```bash
# Edit .env
IMAGE_TAG=previous-tag-name

# Restart
podman-compose up -d
```

### Emergency Stop
```bash
podman-compose down
```

### Recover from Failed Migration
```bash
# Stop app
podman-compose stop app

# Connect to database
podman exec -it lineage-prod-db psql -U lineage -d lineage

# Check Flyway history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;

# Fix manually or restore backup

# Restart
podman-compose start app
```

---

## 📝 Configuration

### Update Environment Variables
```bash
# Edit .env
nano .env

# Restart to apply
podman-compose up -d
```

### Change Port
```bash
# Edit .env
APP_PORT=8081

# Restart
podman-compose up -d
```

### Change Database Password
```bash
# Stop services
podman-compose down

# Edit .env
DB_PASSWORD=new-secure-password

# Start services (will update postgres)
podman-compose up -d
```

---

## 🎯 Common Scenarios

### "Pipeline Failed"
1. Go to GitLab → CI/CD → Pipelines
2. Click failed job
3. Read error message
4. Fix code
5. Push again

### "Frontend Not Loading"
```bash
# Check if app is running
podman ps

# Check logs for errors
podman logs lineage-prod-app | grep -i error

# Verify static files in JAR
podman exec lineage-prod-app unzip -l app.jar | grep static

# Test from inside container
podman exec lineage-prod-app curl http://localhost:8080/
```

### "Database Connection Failed"
```bash
# Check postgres is running
podman ps | grep postgres

# Check postgres logs
podman logs lineage-prod-db

# Test connection
podman exec lineage-prod-app ping postgres

# Verify credentials in .env
cat .env | grep DB_
```

### "Out of Disk Space"
```bash
# Check usage
df -h

# Clean up Docker/Podman
podman system prune -a

# Clean up old logs
podman-compose logs --no-log-prefix > archive.log
podman-compose restart

# Clean up old volumes
podman volume prune
```

---

## 📞 Help

### Where to Look
1. Container logs: `podman logs <container-name>`
2. Pipeline logs: GitLab → CI/CD → Pipelines
3. Health check: `curl http://localhost:8080/actuator/health`
4. Database logs: `podman logs lineage-prod-db`

### Common Issues
- **502 Bad Gateway**: App not started yet, wait 30-60s
- **Connection refused**: Wrong port or firewall blocking
- **404 on frontend**: Static files not built, rebuild image
- **Database errors**: Password mismatch, check .env

### Full Guides
- Deployment: [`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md:1)
- Architecture: [`DEPLOYMENT_ARCHITECTURE_PLAN.md`](DEPLOYMENT_ARCHITECTURE_PLAN.md:1)
- Testing: [`PIPELINE_TESTING.md`](PIPELINE_TESTING.md:1)
- Changes: [`CHANGES_SUMMARY.md`](CHANGES_SUMMARY.md:1)