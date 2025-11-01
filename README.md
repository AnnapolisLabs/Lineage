# Lineage - Open Source Requirements Management Tool

A modern, MIT-licensed, web-based requirements management tool built with Spring Boot and Vue 3.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D.svg)](https://vuejs.org/)

## 📚 Documentation

- **[Quick Start Guide](QUICKSTART.md)** - Get up and running in 5 minutes
- **[Getting Started](GETTING_STARTED.md)** - Detailed setup and usage guide
- **[API Documentation](API.md)** - Complete REST API reference
- **[Test Documentation](TESTS.md)** - Unit test coverage and running tests
- **[Project Summary](PROJECT_SUMMARY.md)** - Complete project overview

## Features

- **Requirements CRUD** with Markdown support
- **Hierarchical organization** (parent-child relationships, drag-and-drop)
- **Bi-directional linking** between requirements
- **Full-text search** with filtering
- **User authentication** and role-based access control (Viewer, Editor, Admin)
- **Version history** with diff view
- **Export** to CSV, JSON, and Markdown
- **Multi-project** support with project-level permissions

## Tech Stack

### Backend
- Java 21
- Spring Boot 3.5.7
- PostgreSQL 15+
- Spring Security with JWT
- Flyway for database migrations
- Spring Data JPA with Hibernate

### Frontend
- Vue 3 with Composition API
- TypeScript
- Vite
- Tailwind CSS
- Pinia for state management
- Vue Router
- Axios

## Quick Start

### Prerequisites

- Java 21 or higher
- PostgreSQL 15 or higher
- Node.js 18+ (for frontend)
- Docker (optional, for easier database setup)

### Complete Setup (Backend + Frontend)

#### 1. Start PostgreSQL

**Option A - Using Docker (Recommended):**
```bash
docker compose -f docker-compose.dev.yml up -d
```

**Option B - Using Local PostgreSQL:**
```sql
CREATE DATABASE lineage;
CREATE USER lineage WITH PASSWORD 'devpassword';
GRANT ALL PRIVILEGES ON DATABASE lineage TO lineage;
```

#### 2. Start the Backend
```bash
# From the project root
./gradlew bootRun
```

The backend will be available at `http://localhost:8080`

#### 3. Start the Frontend
```bash
# In a new terminal
cd frontend
npm install   # First time only
npm run dev
```

The frontend will be available at `http://localhost:5173`

#### 4. Login

Navigate to `http://localhost:5173` and login with:
- **Email:** `admin@lineage.local`
- **Password:** `admin123`

⚠️ **IMPORTANT:** Change this password immediately after first login in production!

## API Documentation

Once the application is running, access the OpenAPI documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Development

### Backend Development

The backend uses Spring Boot DevTools for hot reload:

```bash
./gradlew bootRun
```

### Running Tests

```bash
./gradlew test
```

### Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`. They run automatically on application startup.

To manually check migration status:
```bash
./gradlew flywayInfo
```

## Project Structure

```
lineage/
├── src/main/java/com/annapolislabs/lineage/
│   ├── config/          # Spring configuration
│   ├── controller/      # REST controllers
│   ├── dto/             # Data transfer objects
│   ├── entity/          # JPA entities
│   ├── repository/      # Spring Data repositories
│   ├── security/        # JWT and security components
│   └── service/         # Business logic
├── src/main/resources/
│   ├── db/migration/    # Flyway migrations
│   └── application.yml  # Application configuration
├── frontend/            # Vue 3 frontend
│   ├── src/
│   │   ├── components/  # Vue components
│   │   ├── services/    # API service layer
│   │   ├── stores/      # Pinia stores
│   │   ├── router/      # Vue Router config
│   │   └── views/       # Page components
└── docker-compose.dev.yml
```

## Environment Variables

See `.env.example` for all available environment variables:

- `SPRING_DATASOURCE_URL` - Database JDBC URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `JWT_SECRET` - Secret key for JWT token generation (256+ bits)

## Business Model

Lineage follows the **pgModeler approach**:
- ✅ **Source code**: MIT licensed, free on GitHub forever
- ✅ **Self-compile**: Free for anyone to build from source
- 💰 **Precompiled binaries**: Paid convenience tier (planned for v1.0+)
- 💰 **Hosted version**: Optional cloud hosting (planned)

No artificial feature restrictions. Pay for convenience, not the software itself.

## Roadmap

### v0.1 (Current) - MVP
- [x] Backend with Spring Boot
- [x] Database schema and migrations
- [x] JWT authentication
- [x] Frontend with Vue 3
- [x] Login/Logout UI
- [ ] Requirements CRUD UI
- [ ] Search and filtering
- [ ] Version history viewer

### v0.2 (Planned)
- Traceability matrix
- PDF/HTML export
- Comments and discussions
- Baseline/snapshot feature

### v0.3 (Planned)
- ReqIF import/export
- Typed relationships
- Graph visualization
- CLI tool

### v1.0+ (Future)
- Native installers (Windows, macOS, Linux)
- GraalVM native images
- Precompiled binary releases

## Contributing

This is an open-source project under the MIT license. Contributions are welcome!

## License

MIT License - see LICENSE file for details

Copyright © 2025 Annapolis Labs

## Support

- **Issues**: https://github.com/annapolislabs/lineage/issues
- **Documentation**: Coming soon
