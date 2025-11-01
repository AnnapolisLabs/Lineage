# Requirements Management Tool - Development Specification

## Project Context

### The Problem
There is no truly open-source, usable, modern requirements management tool available:
- **DOORS/Jama**: $5k-10k+ per seat, enterprise complexity
- **ReqView**: Proprietary with restrictive "free for open source" license ($600+/seat commercial)
- **Doorstop**: FOSS but CLI-only, unusable for non-technical stakeholders
- **OSRMT**: Abandoned since 2018

Teams resort to Google Sheets, losing traceability, version history, and collaboration features critical for complex projects.

### The Solution
Build a truly MIT-licensed, web-based, modern requirements management tool that:
- Sets up in under 1 minute (`docker compose up` or precompiled installer)
- Has a clean, fast, modern UI (Linear/GitHub quality)
- Supports real collaboration (not desktop-only)
- Maintains simplicity while allowing flexibility
- Is actually maintained long-term

### Primary Use Case
This tool will be dogfooded on a large-scale quarrying/road construction software project requiring:
- Complex system requirements tracking
- Safety and regulatory traceability
- Multi-stakeholder collaboration (engineers, PMs, QA, compliance)
- Long-term maintenance and version history

---

## Business Model (pgModeler Approach)

### Revenue Strategy
- **Source Code**: MIT licensed, free on GitHub forever
- **Self-Compile**: Users can build from source for free (with good build docs)
- **Precompiled Binaries**: Paid convenience tier ($49-99 one-time or $9-19/month subscription)
  - Windows installer (.exe)
  - macOS installer (.dmg)
  - Linux packages (.deb, .rpm, AppImage)
  - Automatic updates
  - Priority support
- **Hosted Version**: Optional cloud hosting ($29-49/month per team)
  - No infrastructure management
  - Automatic backups
  - SSL certificates
  - Regular updates

### Why This Works
- Developers/hobbyists compile from source (free marketing, community contributions)
- Businesses pay for convenience (time is money)
- No artificial feature restrictions (goodwill from community)
- Similar to: pgModeler, Ardour, GitKraken (free CLI, paid GUI)

### What Gets "Sold"
- NOT the software itself (always MIT licensed)
- Convenience: precompiled, tested, packaged binaries
- Support: priority bug fixes, installation help
- Hosting: managed infrastructure

---

## Architecture Decisions

### Technology Stack

**Backend:**
- **Language**: Java 21+ with Spring Boot 3.2+
- **Framework**: Spring Boot (your strongest skill → ship faster)
- **Database**: PostgreSQL 15+ with JSONB support
- **ORM**: Spring Data JPA with Hibernate
- **API**: REST with Spring Web, OpenAPI documentation
- **Security**: Spring Security with JWT
- **Build**: Maven or Gradle (your preference)

**Performance Considerations for Spring:**
- Use connection pooling (HikariCP - default in Spring Boot)
- Enable HTTP/2 in embedded Tomcat
- Proper JPA lazy loading configuration
- Cache frequently accessed data (Spring Cache with Caffeine)
- Index database properly
- Consider GraalVM native image for v1.0+ (faster startup, lower memory)

**Spring Boot is fine for performance because:**
- This is a web app, not high-frequency trading (startup time doesn't matter)
- Modern JVM is fast for request-response workloads
- Your expertise → faster development → better optimization later
- Can always add caching/optimization after v0.1
- Reference: GitLab (Ruby), GitHub (Ruby/Go), Jira (Java) - all web apps, all fast enough

**Frontend:**
- **Framework**: Vue 3 (Composition API)
- **Build Tool**: Vite (fast dev server, instant HMR)
- **Language**: TypeScript (type safety without complexity)
- **Styling**: Tailwind CSS (utility-first, fast to iterate)
- **State Management**: Pinia (official Vue state library, simpler than Vuex)
- **Router**: Vue Router 4
- **HTTP Client**: Axios (simple, widely used)
- **Markdown Editor**: @toast-ui/vue-editor or similar

**Deployment:**
- **Development**: Docker Compose (Spring Boot + PostgreSQL + Vue dev server)
- **Production Self-Host**: Docker Compose with production builds
- **Precompiled Binary**: 
  - Spring Boot fat JAR + embedded PostgreSQL (H2 for single-user mode)
  - OR Spring Boot + bundled PostgreSQL installer
  - Vue production build served as static files from Spring Boot
  - Native installers via jpackage (Java 21+) or GraalVM

**Development:**
- Monorepo with `/backend` and `/frontend` directories
- Hot reload: Spring Boot DevTools + Vite HMR
- Comprehensive README with setup instructions
- GitHub Actions for CI/CD

---

## Design Principles

### Core Values
1. **Speed**: Sub-second load times, instant search, no loading spinners
2. **Simplicity**: Obvious workflows, no manual required
3. **Clarity**: MIT license, no gotchas, no vendor lock-in
4. **Maintainability**: Clean code, good tests, clear documentation

### UI/UX Principles
- **Keyboard-first**: Cmd/Ctrl+K command palette, shortcuts everywhere
- **Markdown everywhere**: No WYSIWYG complexity
- **Inline editing**: Click to edit, auto-save, no "edit mode"
- **Minimal chrome**: Clean interface, not enterprise button soup
- **Real-time feel**: Optimistic updates, fast feedback

### Anti-Patterns (What NOT to Do)
- ❌ Complex setup requiring configuration files
- ❌ Slow, bloated UI with unnecessary animations
- ❌ Feature bloat (trying to be everything)
- ❌ Poor documentation
- ❌ Desktop-only limitations
- ❌ Fake-free licensing (ReqView model)
- ❌ Overengineered backend (keep Spring Boot config simple)

---

## MVP Scope (v0.1) - 4-6 Weeks

### Must-Have Features

**1. Requirements CRUD**
- Create, read, update, delete requirements
- Fields: ID (auto-generated), title, description (Markdown), status, priority
- Status options: Draft, Review, Approved, Deprecated
- Priority: Low, Medium, High, Critical
- Custom fields stored as JSONB (simple key-value pairs)

**2. Hierarchical Organization**
- Parent-child relationships (tree structure)
- Drag-and-drop to reorganize
- Breadcrumb navigation
- Collapsible tree view in sidebar

**3. Bi-directional Linking**
- Link requirements to each other
- Generic "links to" relationship (no typed relationships yet)
- Show backlinks automatically
- Click to navigate between linked requirements

**4. Search & Filtering**
- Full-text search across title and description
- Filter by status, priority, custom fields
- Search results with highlights
- Fast (sub-100ms response time)

**5. User Authentication & Roles**
- Simple auth: email/password login
- Three roles: Viewer (read-only), Editor (create/edit), Admin (manage users)
- Spring Security with JWT tokens
- No complex RBAC in v0.1

**6. Version History**
- Audit log per requirement (who changed what, when)
- Diff view showing changes
- No branching/merging in v0.1 (just linear history)
- Use Hibernate Envers or manual audit table

**7. Export**
- CSV export (all requirements with fields)
- JSON export (structured data with links)
- Markdown export (human-readable documentation)

**8. Project/Workspace Management**
- Multiple projects per instance
- Users can be assigned to projects
- Project-level permissions

**9. Opinionated Template**
- "Software Project" template with pre-configured:
  - Requirement types: Epic, Feature, Requirement, Test Case
  - Status workflow: Draft → Review → Approved
  - Common custom fields: Component, Sprint, Acceptance Criteria

### Explicitly Out of Scope for v0.1
- ❌ ReqIF import/export (v0.2-0.3)
- ❌ Typed relationships (derives-from, verifies, etc.)
- ❌ Graph visualization (simple lists only)
- ❌ Advanced traceability matrix
- ❌ Git sync/integration
- ❌ PDF/HTML export
- ❌ Comments/discussions
- ❌ Notifications/webhooks
- ❌ API rate limiting
- ❌ SSO/LDAP
- ❌ Native installers (compile from source only for v0.1)

---

## Database Schema (Initial)

### JPA Entities

**User.java**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private UserRole role; // VIEWER, EDITOR, ADMIN
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Project.java**
```java
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Requirement.java**
```java
@Entity
@Table(name = "requirements")
public class Requirement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(unique = true, nullable = false)
    private String reqId; // REQ-001, etc.
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description; // Markdown
    
    private String status;
    private String priority;
    
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Requirement parent;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> customFields;
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**RequirementLink.java**
```java
@Entity
@Table(name = "requirement_links",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"from_requirement_id", "to_requirement_id"}
       ))
public class RequirementLink {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "from_requirement_id", nullable = false)
    private Requirement fromRequirement;
    
    @ManyToOne
    @JoinColumn(name = "to_requirement_id", nullable = false)
    private Requirement toRequirement;
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

**RequirementHistory.java**
```java
@Entity
@Table(name = "requirement_history")
public class RequirementHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;
    
    @ManyToOne
    @JoinColumn(name = "changed_by")
    private User changedBy;
    
    @Enumerated(EnumType.STRING)
    private ChangeType changeType; // CREATED, UPDATED, DELETED
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> oldValue;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> newValue;
    
    @CreationTimestamp
    private LocalDateTime changedAt;
}
```

**ProjectMember.java**
```java
@Entity
@Table(name = "project_members",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"project_id", "user_id"}
       ))
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    private ProjectRole role; // VIEWER, EDITOR, ADMIN
}
```

### Indexes
- Full-text search: Use PostgreSQL full-text search or Hibernate Search
- Composite indexes on frequently queried fields
- Foreign key indexes (automatically created by JPA)

---

## API Endpoints (Spring REST Controllers)

### Authentication (`AuthController.java`)
- `POST /api/auth/login` - Login with email/password → JWT token
- `POST /api/auth/logout` - Logout (invalidate token)
- `GET /api/auth/me` - Get current user (from JWT)

### Projects (`ProjectController.java`)
- `GET /api/projects` - List all projects (user has access to)
- `POST /api/projects` - Create new project
- `GET /api/projects/{id}` - Get project details
- `PUT /api/projects/{id}` - Update project
- `DELETE /api/projects/{id}` - Delete project

### Requirements (`RequirementController.java`)
- `GET /api/projects/{projectId}/requirements` - List all requirements in project
- `POST /api/projects/{projectId}/requirements` - Create requirement
- `GET /api/requirements/{id}` - Get single requirement
- `PUT /api/requirements/{id}` - Update requirement
- `DELETE /api/requirements/{id}` - Delete requirement
- `GET /api/requirements/{id}/history` - Get version history
- `GET /api/requirements/{id}/links` - Get all linked requirements (both directions)

### Links (`RequirementLinkController.java`)
- `POST /api/requirements/{id}/links` - Create link to another requirement
- `DELETE /api/links/{id}` - Remove link

### Search (`SearchController.java`)
- `GET /api/projects/{projectId}/search?q=query&status=&priority=` - Full-text search with filters

### Export (`ExportController.java`)
- `GET /api/projects/{projectId}/export/csv` - Export as CSV
- `GET /api/projects/{projectId}/export/json` - Export as JSON
- `GET /api/projects/{projectId}/export/markdown` - Export as Markdown

### Users (`UserController.java` - Admin only)
- `GET /api/users` - List all users
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

---

## Development Phases

### Phase 1: Backend Foundation (Week 1)
- [ ] Set up Spring Boot project with Maven/Gradle
- [ ] Configure PostgreSQL connection and Hibernate
- [ ] Create JPA entities (User, Project, Requirement, etc.)
- [ ] Set up Flyway or Liquibase for database migrations
- [ ] Implement Spring Security with JWT authentication
- [ ] Basic CRUD repositories and services
- [ ] Configure CORS for local Vue dev server

### Phase 2: Core Backend Features (Week 1-2)
- [ ] Implement all REST controllers
- [ ] Requirements CRUD with hierarchy support
- [ ] Linking between requirements (bi-directional)
- [ ] Full-text search (PostgreSQL or Hibernate Search)
- [ ] Version history tracking (audit logs)
- [ ] Export services (CSV, JSON, Markdown)
- [ ] Unit tests for services
- [ ] Integration tests for controllers

### Phase 3: Frontend Foundation (Week 2-3)
- [ ] Set up Vite + Vue 3 + TypeScript project
- [ ] Configure Tailwind CSS
- [ ] Set up Vue Router and Pinia
- [ ] Create layout components (sidebar, navbar, main content)
- [ ] Implement authentication UI (login/logout)
- [ ] API service layer with Axios
- [ ] JWT token storage and refresh

### Phase 4: Core Frontend Features (Week 3-5)
- [ ] Project list and detail views
- [ ] Requirement CRUD UI
- [ ] Tree view navigation (hierarchical requirements)
- [ ] Markdown editor component with preview
- [ ] Linking UI (search requirements, create links, show backlinks)
- [ ] Search interface with filters
- [ ] Version history viewer with diff display
- [ ] Inline editing with auto-save

### Phase 5: Polish & Documentation (Week 5-6)
- [ ] Export UI (download CSV/JSON/Markdown)
- [ ] User management UI (admin only)
- [ ] Project member management
- [ ] Error handling and loading states
- [ ] Responsive design (mobile-friendly)
- [ ] Keyboard shortcuts (command palette)
- [ ] Comprehensive README
- [ ] API documentation (SpringDoc OpenAPI)
- [ ] User guide (basic usage)
- [ ] Docker Compose setup with one-command deployment

---

## Project Structure

```
requirements-tool/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/yourorg/reqtool/
│   │   │   │   ├── config/          # Spring config (Security, CORS, etc.)
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── dto/             # Data transfer objects
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── repository/      # Spring Data repositories
│   │   │   │   ├── service/         # Business logic
│   │   │   │   ├── security/        # JWT, auth logic
│   │   │   │   └── ReqToolApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/    # Flyway migrations
│   │   └── test/
│   ├── pom.xml (or build.gradle)
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── assets/
│   │   ├── components/
│   │   │   ├── layout/
│   │   │   ├── requirements/
│   │   │   ├── projects/
│   │   │   └── common/
│   │   ├── stores/          # Pinia stores
│   │   ├── services/        # API service layer
│   │   ├── router/          # Vue Router config
│   │   ├── views/           # Page components
│   │   ├── App.vue
│   │   └── main.ts
│   ├── index.html
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── package.json
│   └── Dockerfile
├── docker-compose.yml
├── docker-compose.dev.yml
├── README.md
├── LICENSE (MIT)
└── docs/
    ├── setup.md
    ├── user-guide.md
    └── api.md
```

---

## Docker Compose Setup

### `docker-compose.yml` (Production)
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: reqtool
      POSTGRES_USER: reqtool
      POSTGRES_PASSWORD: changeme
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U reqtool"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/reqtool
      SPRING_DATASOURCE_USERNAME: reqtool
      SPRING_DATASOURCE_PASSWORD: changeme
      JWT_SECRET: your-secret-key-change-in-production
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  postgres-data:
```

### `docker-compose.dev.yml` (Development)
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: reqtool
      POSTGRES_USER: reqtool
      POSTGRES_PASSWORD: devpassword
    volumes:
      - postgres-data-dev:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  # Backend and frontend run locally with hot reload
  # Just use this for the database

volumes:
  postgres-data-dev:
```

---

## Spring Boot Configuration

### `application.yml`
```yaml
spring:
  application:
    name: requirements-tool
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/reqtool}
    username: ${SPRING_DATASOURCE_USERNAME:reqtool}
    password: ${SPRING_DATASOURCE_PASSWORD:devpassword}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for migrations
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  flyway:
    enabled: true
    baseline-on-migrate: true

server:
  port: 8080
  compression:
    enabled: true

jwt:
  secret: ${JWT_SECRET:dev-secret-key-change-in-production}
  expiration: 86400000  # 24 hours

logging:
  level:
    com.yourorg.reqtool: DEBUG
    org.springframework.web: INFO
```

---

## Vue Frontend Configuration

### `vite.config.ts`
```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### `tailwind.config.js`
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

---

## Success Criteria for v0.1

### Functional Requirements
- ✅ Setup from source in under 5 minutes (with Docker)
- ✅ Create and organize 100+ requirements without performance issues
- ✅ Search returns results in under 100ms
- ✅ All CRUD operations work correctly
- ✅ Version history accurately tracks changes
- ✅ Export produces valid CSV/JSON/Markdown

### Non-Functional Requirements
- ✅ Clean, modern UI (Vue + Tailwind aesthetic)
- ✅ Intuitive workflows (no manual needed for basic use)
- ✅ Stable (no crashes or data loss)
- ✅ Well-documented (README, setup guide, basic usage)
- ✅ MIT licensed with clear attribution
- ✅ Spring Boot starts in <10 seconds
- ✅ Page loads feel instant (<200ms perceived)

### User Acceptance
- ✅ Can manage quarrying software requirements effectively
- ✅ Non-technical stakeholders can use it (PMs, QA)
- ✅ Faster than Google Sheets for requirement management
- ✅ Would recommend to others facing same problem

---

## Precompiled Binary Strategy (v1.0+)

### What to Build
1. **Spring Boot Fat JAR** with embedded Tomcat + Vue static files
2. **Native Image** (optional) via GraalVM for faster startup
3. **Installers** via jpackage:
   - Windows: `.exe` installer with bundled JRE
   - macOS: `.dmg` with bundled JRE
   - Linux: `.deb`, `.rpm`, AppImage

### Configuration Options
- **Embedded mode**: H2 database (single-user, portable)
- **Server mode**: PostgreSQL connection (multi-user, recommended)
- **Installer wizard**: Asks user which mode during installation

### Build Process
```bash
# Build backend fat JAR
cd backend
./mvnw clean package -DskipTests

# Build frontend production
cd frontend
npm run build
# Copy dist/ to backend/src/main/resources/static

# Rebuild backend with embedded frontend
cd backend
./mvnw clean package

# Create native installers (jpackage)
jpackage \
  --input target/ \
  --name RequirementsTool \
  --main-jar requirements-tool-1.0.0.jar \
  --type exe \
  --app-version 1.0.0 \
  --vendor "Your Name" \
  --icon icon.ico
```

### Pricing Strategy
- **Source code**: Free (MIT license)
- **Precompiled installer**: $49 one-time OR $9/month
- **Hosted version**: $29/month (team), $49/month (organization)
- **Enterprise support**: Custom pricing

---

## Future Roadmap (Post-MVP)

### v0.2 (Month 2)
- Traceability matrix (table view of links)
- PDF/HTML export for stakeholders
- Baseline/snapshot feature (version entire requirement set)
- Tagging system for categorization
- Comments and discussion threads
- **First precompiled beta builds**

### v0.3 (Month 3-4)
- ReqIF import/export
- Typed relationships (derives-from, verifies, traces-to)
- Simple graph visualization (D3.js or similar)
- Basic API webhooks
- CLI tool for automation (Spring Shell)
- **Official precompiled releases**

### v0.4+ (Future)
- Integration with GitHub/GitLab (sync with issues)
- Integration with Jira (import/export)
- Advanced permissions (field-level, custom roles)
- Git sync (export/import to YAML for version control)
- Notifications (email, in-app)
- Collaborative editing (WebSocket, real-time presence)
- **GraalVM native images** (faster startup, lower memory)

---

## Development Guidelines

### Java/Spring Boot Best Practices
- Use constructor injection (not @Autowired fields)
- Keep controllers thin (delegate to services)
- Use DTOs for API responses (don't expose entities directly)
- Validate input with @Valid and Bean Validation
- Handle exceptions with @ControllerAdvice
- Write unit tests with JUnit 5 + Mockito
- Use Spring Boot Test for integration tests

### Vue Best Practices
- Use Composition API (not Options API)
- Keep components small and focused
- Use TypeScript for type safety
- Props down, events up
- Use Pinia for shared state (not prop drilling)
- Lazy load routes for better performance
- Use Vue DevTools for debugging

### Code Quality
- Write clean, readable code with clear variable names
- Add comments for complex logic
- Follow Java conventions (Google Java Style Guide)
- Use ESLint + Prettier for Vue code
- No premature optimization (make it work, then make it fast)

### Testing
- Unit tests for business logic (services)
- Integration tests for API endpoints (@SpringBootTest)
- E2E tests for critical workflows (optional, Playwright/Cypress)
- Test database migrations
- Test export functionality thoroughly

### Documentation
- Javadoc for public APIs
- JSDoc for Vue components
- OpenAPI documentation (auto-generated via SpringDoc)
- README with:
  - Quick start guide
  - Architecture overview
  - Development setup
  - Deployment instructions
  - License information

### Git Workflow
- Main branch is always deployable
- Feature branches for new work
- Clear commit messages (Conventional Commits style)
- No force pushing to main
- PR reviews (even if solo, for documentation)

---

## Immediate Next Steps

1. **Initialize Projects**
   ```bash
   # Create Spring Boot project
   # Use Spring Initializr: start.spring.io
   # Dependencies: Web, Data JPA, PostgreSQL, Security, Validation, Flyway
   
   # Create Vue project
   npm create vite@latest frontend -- --template vue-ts
   cd frontend
   npm install -D tailwindcss postcss autoprefixer
   npx tailwindcss init -p
   npm install vue-router@4 pinia axios
   ```

2. **Set Up Docker Compose**
   - Create `docker-compose.dev.yml` for PostgreSQL
   - Test connection from Spring Boot
   - Test connection from local psql

3. **Backend Foundation**
   - Create JPA entities
   - Write Flyway migrations
   - Implement JWT authentication
   - Create basic CRUD endpoints

4. **Frontend Foundation**
   - Set up Vue Router
   - Create layout components
   - Implement login UI
   - Connect to backend API

5. **First End-to-End Flow**
   - User can log in
   - User can create a project
   - User can create a requirement
   - User can view requirement
   - Prove the full stack works

---

## Performance Optimization Checklist (v0.1+)

### Backend (Spring Boot)
- [ ] Enable HTTP/2 in application.yml
- [ ] Configure HikariCP connection pool properly
- [ ] Add database indexes on foreign keys and search fields
- [ ] Use @Transactional appropriately (read-only where possible)
- [ ] Implement pagination for large result sets
- [ ] Add caching for frequently accessed data (Spring Cache + Caffeine)
- [ ] Use JPA lazy loading correctly (avoid N+1 queries)
- [ ] Profile with JProfiler or VisualVM if slow

### Frontend (Vue)
- [ ] Lazy load routes with dynamic imports
- [ ] Use `v-show` vs `v-if` appropriately
- [ ] Debounce search input (don't search on every keystroke)
- [ ] Virtual scrolling for large lists (vue-virtual-scroller)
- [ ] Optimize Tailwind build (purge unused classes)
- [ ] Compress assets in production (Vite does this automatically)
- [ ] Use Vue DevTools Performance tab to find bottlenecks

### Database (PostgreSQL)
- [ ] Create indexes on search columns (GIN for full-text search)
- [ ] Analyze query plans with EXPLAIN ANALYZE
- [ ] Tune PostgreSQL config for your hardware
- [ ] Regular VACUUM ANALYZE
- [ ] Monitor slow queries

---

## Questions for Implementation

As you build, consider these decisions:

1. **Maven or Gradle?** (Maven is simpler, Gradle is more flexible)
2. **Flyway or Liquibase?** (Flyway is simpler, Liquibase more powerful)
3. **Auto-save or explicit save?** (Auto-save preferred for modern feel)
4. **REQ-ID format?** (Project prefix + number? e.g., PROJ-001)
5. **Requirement numbering?** (Sequential per project or global?)
6. **H2 embedded mode?** (For single-user portable version?)
7. **GraalVM native image?** (Wait until v1.0+ unless startup time critical)

---

## License & Distribution

- **License**: MIT (full permissions, no restrictions)
- **Copyright**: © 2025 [Your Name/Organization]
- **Repository**: Public GitHub repository from day one
- **Documentation**: Hosted on GitHub Pages
- **Precompiled Binaries**: Gumroad, GitHub Sponsors, or custom site
- **Source Code**: Always free, always available

---

**Remember**: This is v0.1. Ship fast, get feedback, iterate. Use it on your quarrying project ASAP to validate the design. Perfect is the enemy of shipped.

The precompiled binary business model means you can start generating revenue without restricting the software itself. Build trust with the community first, monetize convenience later.