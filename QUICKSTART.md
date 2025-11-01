# Lineage - Quick Start

> Open-source requirements management tool with Spring Boot + Vue 3

## Setup (< 5 minutes)

### 1. Database
```bash
docker compose -f docker-compose.dev.yml up -d
```

### 2. Backend (Terminal 1)
```bash
./gradlew bootRun
```
Runs on **http://localhost:8080**

### 3. Frontend (Terminal 2)
```bash
cd frontend && npm install && npm run dev
```
Runs on **http://localhost:5173**

### 4. Login
- URL: **http://localhost:5173**
- Email: `admin@lineage.local`
- Password: `admin123`

---

## Core Features

| Feature | Status | Location |
|---------|--------|----------|
| JWT Authentication | ✅ | `/api/auth/login` |
| Project Management | ✅ | `/api/projects` |
| Requirements CRUD | ✅ | `/api/requirements` |
| Hierarchical Structure | ✅ | Parent-child support |
| Bi-directional Links | ✅ | `/api/requirements/{id}/links` |
| Full-text Search | ✅ | `/api/projects/{id}/search` |
| Version History | ✅ | `/api/requirements/{id}/history` |
| Export (CSV/JSON/MD) | ✅ | `/api/projects/{id}/export/{format}` |
| Role-based Access | ✅ | VIEWER/EDITOR/ADMIN |

---

## API Quick Reference

### Auth
```bash
# Login
POST /api/auth/login
{"email": "admin@lineage.local", "password": "admin123"}

# Get current user
GET /api/auth/me
```

### Projects
```bash
# List projects
GET /api/projects

# Create project
POST /api/projects
{"name": "My Project", "projectKey": "MYPROJ", "description": "..."}

# Get project
GET /api/projects/{id}
```

### Requirements
```bash
# List requirements
GET /api/projects/{projectId}/requirements

# Create requirement
POST /api/projects/{projectId}/requirements
{"title": "...", "description": "...", "status": "DRAFT", "priority": "MEDIUM"}

# Update requirement
PUT /api/requirements/{id}

# Delete requirement
DELETE /api/requirements/{id}

# Get history
GET /api/requirements/{id}/history
```

### Search
```bash
# Search with filters
GET /api/projects/{projectId}/search?q=login&status=DRAFT&priority=HIGH
```

### Export
```bash
# Export formats
GET /api/projects/{projectId}/export/csv
GET /api/projects/{projectId}/export/json
GET /api/projects/{projectId}/export/markdown
```

---

## Project Structure

```
Lineage/
├── src/main/java/              # Backend (Spring Boot)
│   ├── controller/              # REST endpoints (6 controllers)
│   ├── service/                 # Business logic (5 services)
│   ├── repository/              # Data access (6 repositories)
│   ├── entity/                  # Database models (7 entities)
│   └── security/                # JWT auth
├── frontend/src/                # Frontend (Vue 3)
│   ├── views/                   # Pages (Login, Projects, ProjectDetail)
│   ├── services/                # API clients
│   ├── stores/                  # Pinia state
│   └── router/                  # Vue Router
└── src/main/resources/
    └── db/migration/            # Flyway SQL scripts
```

---

## Tech Stack

**Backend:**
- Java 21 + Spring Boot 3.5.7
- PostgreSQL 15+ with JSONB
- Spring Security + JWT
- Flyway migrations

**Frontend:**
- Vue 3 + TypeScript
- Vite + Tailwind CSS
- Pinia + Vue Router
- Axios

---

## Testing

```bash
# Run all tests (27 tests)
./gradlew test

# View test report
open build/reports/tests/test/index.html
```

**Test Coverage:**
- ✅ Service layer (ProjectService, RequirementService)
- ✅ Security (JWT token validation)
- ✅ Controllers (Auth endpoints)
- ✅ Repositories (User queries)

---

## Documentation

- **README.md** - Full project documentation
- **GETTING_STARTED.md** - Detailed setup guide
- **TESTS.md** - Test documentation
- **API Docs** - http://localhost:8080/swagger-ui.html

---

## Common Tasks

### Create a Project
1. Login to http://localhost:5173
2. Click "+ New Project"
3. Enter name, key (2-10 uppercase), description
4. Click "Create"

### Add Requirements
1. Click on project
2. Click "+ New Requirement"
3. Fill in title, description, status, priority
4. Click "Create"

### Search Requirements
1. Enter search query in search box
2. Filter by status/priority dropdowns
3. Results update automatically

### Export Data
1. Click "Export" button
2. Choose format (CSV/JSON/Markdown)
3. File downloads automatically

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Backend won't start | Check PostgreSQL is running: `docker ps` |
| Can't login | Verify backend is on port 8080 |
| Frontend build fails | Delete `node_modules`, run `npm install` |
| Database error | Check `application.yml` credentials |

---

## Next Steps

1. ✅ Core CRUD working
2. 🚧 Add linking UI in frontend
3. 🚧 Build tree view with drag-and-drop
4. 🚧 Add markdown editor with preview
5. 🚧 Implement graph visualization

---

## License

MIT - Free and open source forever!

**Repository:** https://github.com/annapolislabs/lineage
**Issues:** https://github.com/annapolislabs/lineage/issues
