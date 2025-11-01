# Getting Started with Lineage

This guide will help you get Lineage up and running in under 5 minutes.

## What You Just Built

- ✅ **Backend API** - Spring Boot with JWT authentication, PostgreSQL, full REST API
- ✅ **Frontend UI** - Vue 3 with Tailwind CSS, responsive design
- ✅ **Project Management** - Create, edit, delete projects
- ✅ **Requirements CRUD** - Full create, read, update, delete for requirements
- ✅ **Search & Filter** - Full-text search with status/priority filtering
- ✅ **Export** - CSV, JSON, and Markdown export
- ✅ **Version History** - Automatic change tracking
- ✅ **Linking** - Bi-directional requirement linking (API ready)

## Quick Start

### Step 1: Start PostgreSQL

You have two options:

**Option A - Docker (Recommended):**
```bash
docker compose -f docker-compose.dev.yml up -d
```

**Option B - Local PostgreSQL:**
Create a database:
```sql
CREATE DATABASE lineage;
CREATE USER lineage WITH PASSWORD 'devpassword';
GRANT ALL PRIVILEGES ON DATABASE lineage TO lineage;
GRANT ALL ON SCHEMA public TO lineage;
```

### Step 2: Start the Backend

From the project root:
```bash
./gradlew bootRun
```

The backend will start on **http://localhost:8080**

You'll see output like:
```
Started LineageApplication in X.XXX seconds
```

### Step 3: Start the Frontend

In a new terminal:
```bash
cd frontend
npm install  # First time only
npm run dev
```

The frontend will start on **http://localhost:5173**

### Step 4: Login

1. Open your browser to **http://localhost:5173**
2. Login with:
   - **Email:** `admin@lineage.local`
   - **Password:** `admin123`

## What Can You Do Now?

### 1. Create a Project
- Click "New Project"
- Enter a name (e.g., "My Software Project")
- Enter a project key (e.g., "MSP") - 2-10 uppercase letters
- Add a description
- Click "Create"

### 2. Create Requirements
- Click on your project
- Click "New Requirement"
- Add title, description, status, priority
- Click "Create"

### 3. Search Requirements
- Use the search box to find requirements by text
- Filter by status (Draft, Review, Approved, Deprecated)
- Filter by priority (Low, Medium, High, Critical)

### 4. Export Data
- Click "Export" dropdown
- Choose CSV, JSON, or Markdown format
- File downloads automatically

### 5. View API Documentation
- Open **http://localhost:8080/swagger-ui.html**
- Explore all available endpoints
- Test API calls directly from Swagger UI

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with email/password
- `GET /api/auth/me` - Get current user info

### Projects
- `GET /api/projects` - List all projects (user has access to)
- `POST /api/projects` - Create new project
- `GET /api/projects/{id}` - Get project details
- `PUT /api/projects/{id}` - Update project
- `DELETE /api/projects/{id}` - Delete project

### Requirements
- `GET /api/projects/{projectId}/requirements` - List all requirements in project
- `POST /api/projects/{projectId}/requirements` - Create requirement
- `GET /api/requirements/{id}` - Get single requirement
- `PUT /api/requirements/{id}` - Update requirement
- `DELETE /api/requirements/{id}` - Delete requirement
- `GET /api/requirements/{id}/history` - Get version history

### Links
- `POST /api/requirements/{id}/links` - Create link to another requirement
- `GET /api/requirements/{id}/links` - Get all linked requirements
- `DELETE /api/links/{id}` - Remove link

### Search
- `GET /api/projects/{projectId}/search?q={query}&status={status}&priority={priority}`

### Export
- `GET /api/projects/{projectId}/export/csv` - Export as CSV
- `GET /api/projects/{projectId}/export/json` - Export as JSON
- `GET /api/projects/{projectId}/export/markdown` - Export as Markdown

## Architecture

### Backend (Port 8080)
```
src/main/java/com/annapolislabs/lineage/
├── config/          # Spring Security, CORS
├── controller/      # REST controllers (Auth, Projects, Requirements, Links, Search, Export)
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities (User, Project, Requirement, RequirementLink, etc.)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT utilities, filters, UserDetailsService
└── service/         # Business logic (Auth, Project, Requirement, Link, Export)
```

### Frontend (Port 5173)
```
frontend/src/
├── components/      # Reusable Vue components
├── services/        # API service layer (auth, projects, requirements)
├── stores/          # Pinia state management (auth, projects)
├── router/          # Vue Router configuration
└── views/           # Page components (Login, Projects, ProjectDetail)
```

### Database
PostgreSQL with Flyway migrations:
- `V1__initial_schema.sql` - Creates all tables
- `V2__seed_data.sql` - Creates default admin user

## Common Issues

### Backend won't start
- Check PostgreSQL is running: `docker ps` or check local PostgreSQL service
- Verify database credentials in `application.yml`
- Check Java version: `java -version` (should be 21+)

### Frontend won't start
- Check Node version: `node --version` (should be 18+)
- Delete `node_modules` and run `npm install` again
- Check if port 5173 is in use

### Can't login
- Verify backend is running on port 8080
- Check browser console for errors
- Try clearing browser cache
- Default credentials: `admin@lineage.local` / `admin123`

### Database connection errors
- Check PostgreSQL is running
- Verify credentials in `application.yml`
- Check database exists: `psql -U lineage -d lineage -c "\dt"`

## Next Steps

### Features to Explore
1. **Create hierarchical requirements** - Set parent requirements
2. **Link requirements together** - Use the API to create relationships
3. **Track changes** - View requirement history via API
4. **Export your data** - Try all three export formats

### Future Enhancements (You can build these!)
- [ ] Requirement linking UI
- [ ] Graph visualization of requirement relationships
- [ ] Drag-and-drop tree view for hierarchy
- [ ] Rich markdown editor with preview
- [ ] Real-time collaboration
- [ ] Comments and discussions
- [ ] PDF export
- [ ] ReqIF import/export
- [ ] Email notifications
- [ ] Advanced role permissions

## Development

### Hot Reload
Both backend and frontend support hot reload:
- **Backend**: Spring Boot DevTools automatically recompiles on file changes
- **Frontend**: Vite HMR updates instantly on save

### Running Tests
```bash
# Backend tests
./gradlew test

# Frontend tests (if configured)
cd frontend && npm test
```

### Building for Production
```bash
# Backend JAR
./gradlew build
# Output: build/libs/Lineage-0.0.1-SNAPSHOT.jar

# Frontend production build
cd frontend && npm run build
# Output: frontend/dist/
```

## License

MIT License - Free to use, modify, and distribute!

## Support

- **Issues**: Create an issue on GitHub
- **Documentation**: See README.md
- **API Docs**: http://localhost:8080/swagger-ui.html

---

**Happy Requirement Managing! 🚀**
