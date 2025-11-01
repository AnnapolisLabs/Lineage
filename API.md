# API Documentation

## Base URL
```
http://localhost:8080/api
```

## Authentication

All endpoints except `/auth/login` require JWT authentication.

**Header:**
```
Authorization: Bearer <token>
```

---

## Endpoints

### Authentication

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "admin@lineage.local",
  "password": "admin123"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "email": "admin@lineage.local",
  "name": "Admin User",
  "role": "ADMIN"
}
```

#### Get Current User
```http
GET /auth/me
Authorization: Bearer <token>
```

**Response (200):**
```json
{
  "id": "uuid",
  "email": "admin@lineage.local",
  "name": "Admin User",
  "role": "ADMIN",
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-01T00:00:00"
}
```

---

### Projects

#### List All Projects
```http
GET /projects
Authorization: Bearer <token>
```

**Response (200):**
```json
[
  {
    "id": "uuid",
    "name": "My Project",
    "description": "Project description",
    "projectKey": "MYPROJ",
    "createdByName": "Admin User",
    "createdByEmail": "admin@lineage.local",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
]
```

#### Create Project
```http
POST /projects
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "New Project",
  "description": "Description here",
  "projectKey": "NEWPROJ"
}
```

**Validation:**
- `name`: Required, max 255 chars
- `projectKey`: Required, 2-10 uppercase letters, unique
- `description`: Optional, max 5000 chars

**Response (201):** Project object

#### Get Project
```http
GET /projects/{projectId}
Authorization: Bearer <token>
```

**Response (200):** Project object

#### Update Project
```http
PUT /projects/{projectId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Updated Name",
  "description": "Updated description",
  "projectKey": "PROJ"
}
```

**Note:** Project key cannot be changed after creation

**Response (200):** Project object

#### Delete Project
```http
DELETE /projects/{projectId}
Authorization: Bearer <token>
```

**Permissions:** Requires ADMIN role on project

**Response (204):** No content

---

### Requirements

#### List Project Requirements
```http
GET /projects/{projectId}/requirements
Authorization: Bearer <token>
```

**Response (200):**
```json
[
  {
    "id": "uuid",
    "reqId": "PROJ-001",
    "title": "Requirement Title",
    "description": "Description in markdown",
    "status": "DRAFT",
    "priority": "MEDIUM",
    "parentId": null,
    "parentReqId": null,
    "customFields": {},
    "createdByName": "User Name",
    "createdByEmail": "user@example.com",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
]
```

#### Create Requirement
```http
POST /projects/{projectId}/requirements
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "New Requirement",
  "description": "Description in **markdown**",
  "status": "DRAFT",
  "priority": "MEDIUM",
  "parentId": null,
  "customFields": {
    "sprint": "Sprint 1",
    "component": "Backend"
  }
}
```

**Validation:**
- `title`: Required, max 500 chars
- `description`: Optional, max 50000 chars
- `status`: DRAFT | REVIEW | APPROVED | DEPRECATED
- `priority`: LOW | MEDIUM | HIGH | CRITICAL
- `parentId`: Optional UUID of parent requirement

**Permissions:** Requires EDITOR or ADMIN role

**Response (201):** Requirement object

#### Get Requirement
```http
GET /requirements/{requirementId}
Authorization: Bearer <token>
```

**Response (200):** Requirement object

#### Update Requirement
```http
PUT /requirements/{requirementId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Updated Title",
  "description": "Updated description",
  "status": "APPROVED",
  "priority": "HIGH",
  "parentId": null,
  "customFields": {}
}
```

**Permissions:** Requires EDITOR or ADMIN role

**Response (200):** Requirement object

#### Delete Requirement
```http
DELETE /requirements/{requirementId}
Authorization: Bearer <token>
```

**Permissions:** Requires EDITOR or ADMIN role

**Response (204):** No content

#### Get Requirement History
```http
GET /requirements/{requirementId}/history
Authorization: Bearer <token>
```

**Response (200):**
```json
[
  {
    "id": "uuid",
    "changeType": "UPDATED",
    "changedBy": "user@example.com",
    "changedAt": "2025-01-01T00:00:00",
    "oldValue": {
      "title": "Old Title",
      "status": "DRAFT"
    },
    "newValue": {
      "title": "New Title",
      "status": "APPROVED"
    }
  }
]
```

**Change Types:** CREATED | UPDATED | DELETED

---

### Requirement Links

#### Create Link
```http
POST /requirements/{requirementId}/links
Authorization: Bearer <token>
Content-Type: application/json

{
  "toRequirementId": "target-uuid"
}
```

**Permissions:** Requires EDITOR or ADMIN role

**Response (201):**
```json
{
  "id": "uuid",
  "from": { /* requirement object */ },
  "to": { /* requirement object */ },
  "createdAt": "2025-01-01T00:00:00"
}
```

#### Get All Links for Requirement
```http
GET /requirements/{requirementId}/links
Authorization: Bearer <token>
```

**Response (200):**
```json
[
  {
    "id": "uuid",
    "direction": "outgoing",
    "requirement": { /* linked requirement */ },
    "createdAt": "2025-01-01T00:00:00"
  },
  {
    "id": "uuid",
    "direction": "incoming",
    "requirement": { /* linked requirement */ },
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

#### Delete Link
```http
DELETE /links/{linkId}
Authorization: Bearer <token>
```

**Permissions:** Requires EDITOR or ADMIN role

**Response (204):** No content

---

### Search

#### Search Requirements
```http
GET /projects/{projectId}/search?q=keyword&status=DRAFT&priority=HIGH
Authorization: Bearer <token>
```

**Query Parameters:**
- `q` (optional): Full-text search query
- `status` (optional): Filter by status
- `priority` (optional): Filter by priority

**Response (200):** Array of requirement objects

**Search Features:**
- Full-text search on title and description (PostgreSQL)
- Combine search with filters
- Case-insensitive matching

---

### Export

#### Export as CSV
```http
GET /projects/{projectId}/export/csv
Authorization: Bearer <token>
```

**Response (200):** CSV file
**Headers:** `Content-Disposition: attachment; filename=requirements.csv`

**Format:**
```csv
REQ_ID,Title,Description,Status,Priority,Parent,Created By,Created At
PROJ-001,Title,Description,DRAFT,MEDIUM,,user@example.com,2025-01-01
```

#### Export as JSON
```http
GET /projects/{projectId}/export/json
Authorization: Bearer <token>
```

**Response (200):** JSON file
```json
{
  "project": {
    "name": "Project Name",
    "key": "PROJ",
    "description": "Description"
  },
  "requirements": [
    { /* full requirement objects */ }
  ]
}
```

#### Export as Markdown
```http
GET /projects/{projectId}/export/markdown
Authorization: Bearer <token>
```

**Response (200):** Markdown file

**Format:**
```markdown
# Project Name

## PROJ-001: Requirement Title

**Status:** DRAFT
**Priority:** MEDIUM

Requirement description...

---

### PROJ-002: Child Requirement

**Status:** REVIEW
**Priority:** HIGH

...
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-01-01T00:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2025-01-01T00:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required"
}
```

### 403 Forbidden
```json
{
  "message": "Access denied"
}
```

### 404 Not Found
```json
{
  "message": "Project not found"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-01-01T00:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An error occurred"
}
```

---

## Rate Limiting

Currently no rate limiting implemented. Consider adding for production use.

---

## CORS

Configured for local development:
- `http://localhost:3000`
- `http://localhost:5173`

Update `SecurityConfig.java` for production domains.

---

## OpenAPI/Swagger

Interactive API documentation available at:
```
http://localhost:8080/swagger-ui.html
```

Test all endpoints directly from the browser!
