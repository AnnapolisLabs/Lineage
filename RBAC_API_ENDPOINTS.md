# RBAC Collaboration API Endpoints

## Overview

The Lineage platform now includes a comprehensive RBAC (Role-Based Access Control) collaboration system that enables team-based project management, task assignment, peer reviews, and granular permission control. This document outlines all new endpoints available for AI model integration.

## Authentication

All endpoints require JWT Bearer token authentication:
```
Authorization: Bearer <jwt_token>
```

## Endpoint Categories

### 1. Permission Evaluation (`/api/v1/rbac`)

#### Check User Permission
```http
POST /api/v1/rbac/permissions/check
```
**Purpose**: Check if current user has specific permission
**Request**:
```json
{
  "permission": "project.manage",
  "resource_id": "uuid-string"
}
```
**Response**:
```json
{
  "user_id": "uuid",
  "permission": "project.manage", 
  "resource_id": "uuid",
  "authorized": true,
  "timestamp": 1234567890
}
```

#### Batch Permission Evaluation
```http
POST /api/v1/rbac/permissions/evaluate
```
**Purpose**: Check multiple permissions at once
**Request**:
```json
{
  "permissions": ["project.read", "team.manage", "task.assign"],
  "resource_id": "uuid-string"
}
```

#### Get User Permissions
```http
GET /api/v1/rbac/permissions/user?user_id=uuid&resource_id=uuid
```
**Purpose**: Get all effective permissions for a user

#### Check Role Hierarchy
```http
POST /api/v1/rbac/roles/check-hierarchy?required_role=ADMIN&user_id=uuid
```
**Purpose**: Check if user has role or higher in hierarchy

### 2. Team Management (`/api/v1/teams`)

#### Create Team
```http
POST /api/v1/teams
```
**Purpose**: Create new team associated with project
**Request**:
```json
{
  "name": "Frontend Development Team",
  "description": "Responsible for frontend development",
  "project_id": "uuid-string",
  "settings": {
    "require_peer_review": true,
    "max_members": 10
  }
}
```

#### Get Teams
```http
GET /api/v1/teams?project_id=uuid&page=0&size=20
```
**Purpose**: List teams with pagination and filtering

#### Get Team Details
```http
GET /api/v1/teams/{team_id}
```

#### Update Team
```http
PUT /api/v1/teams/{team_id}
```
**Purpose**: Update team information and settings

#### Invite User to Team
```http
POST /api/v1/teams/{team_id}/members/invite
```
**Purpose**: Send invitation to user for team membership
**Request**:
```json
{
  "email": "developer@example.com",
  "role": "member",
  "message": "Welcome to the team!"
}
```
**Team Roles**: `OWNER`, `ADMIN`, `MEMBER`, `VIEWER`

#### Accept Team Invitation
```http
POST /api/v1/teams/invitations/{invitation_id}/accept
```

#### Get Team Members
```http
GET /api/v1/teams/{team_id}/members
```

#### Remove Team Member
```http
DELETE /api/v1/teams/{team_id}/members/{user_id}
```

#### Update Member Role
```http
PUT /api/v1/teams/{team_id}/members/{user_id}/role
```
**Request**:
```json
{
  "role": "admin"
}
```

### 3. Task Assignment (`/api/v1/tasks`)

#### Create Task
```http
POST /api/v1/tasks
```
**Purpose**: Create new task assignment
**Request**:
```json
{
  "task_title": "Implement user authentication",
  "task_description": "Create login and registration components",
  "assigned_to": "uuid-string",
  "project_id": "uuid-string",
  "priority": "high",
  "due_date": "2025-12-15T17:00:00Z"
}
```
**Priority Levels**: `low`, `medium`, `high`, `critical`

#### Get Tasks
```http
GET /api/v1/tasks?assigned_to=uuid&project_id=uuid&status=assigned&priority=high&page=0&size=20
```
**Task Status**: `assigned`, `in_progress`, `completed`, `cancelled`

#### Get Task Details
```http
GET /api/v1/tasks/{task_id}
```

#### Update Task
```http
PUT /api/v1/tasks/{task_id}
```

#### Start Task
```http
POST /api/v1/tasks/{task_id}/start
```
**Purpose**: Mark task as in progress

#### Complete Task
```http
POST /api/v1/tasks/{task_id}/complete
```
**Request**:
```json
{
  "completion_notes": "Authentication implementation completed successfully"
}
```

#### Cancel Task
```http
POST /api/v1/tasks/{task_id}/cancel
```
**Request**:
```json
{
  "reason": "Requirements changed"
}
```

#### Reassign Task
```http
POST /api/v1/tasks/{task_id}/reassign
```
**Request**:
```json
{
  "assigned_to": "new-user-uuid"
}
```

#### Add Task Tag
```http
POST /api/v1/tasks/{task_id}/tags
```
**Request**:
```json
{
  "tag": "authentication"
}
```

#### Add Task Blocker
```http
POST /api/v1/tasks/{task_id}/blockers
```
**Request**:
```json
{
  "blocker": "Waiting for API endpoint"
}
```

#### Search Tasks
```http
GET /api/v1/tasks/search?search=auth&status=assigned&page=0&size=20
```

#### Get Overdue Tasks
```http
GET /api/v1/tasks/overdue
```

#### Get Tasks Due Soon
```http
GET /api/v1/tasks/due-soon?days_threshold=7
```

### 4. Peer Review (`/api/v1/reviews`)

#### Create Peer Review
```http
POST /api/v1/reviews
```
**Purpose**: Create new peer review for requirement
**Request**:
```json
{
  "requirement_id": "uuid-string",
  "reviewer_id": "uuid-string",
  "author_id": "uuid-string",
  "review_type": "code",
  "review_deadline": "2025-12-10T17:00:00Z"
}
```
**Review Types**: `code`, `design`, `documentation`, `process`, `requirements`

#### Get Peer Reviews
```http
GET /api/v1/reviews?requirement_id=uuid&reviewer_id=uuid&status=pending&review_type=code&page=0&size=20
```

#### Get Review Details
```http
GET /api/v1/reviews/{review_id}
```

#### Get Reviews by Requirement
```http
GET /api/v1/reviews/requirement/{requirement_id}
```

#### Start Review
```http
POST /api/v1/reviews/{review_id}/start
```

#### Approve Review
```http
POST /api/v1/reviews/{review_id}/approve
```
**Request**:
```json
{
  "comments": "Great implementation, very clean code!"
}
```

#### Reject Review
```http
POST /api/v1/reviews/{review_id}/reject
```
**Request**:
```json
{
  "comments": "Missing error handling"
}
```

#### Request Revision
```http
POST /api/v1/reviews/{review_id}/request-revision
```
**Request**:
```json
{
  "comments": "Please add unit tests"
}
```

#### Set Review Ratings
```http
POST /api/v1/reviews/{review_id}/ratings
```
**Request**:
```json
{
  "effort_rating": 4,
  "quality_rating": 5
}
```
**Rating Range**: 1-5 for both effort and quality

#### Get Pending Reviews
```http
GET /api/v1/reviews/pending
```

#### Get Overdue Reviews
```http
GET /api/v1/reviews/overdue
```

#### Get Reviews Needing Attention
```http
GET /api/v1/reviews/needing-attention
```

#### Search Reviews
```http
GET /api/v1/reviews/search?search=auth&status=pending&page=0&size=20
```

#### Get Review Statistics
```http
GET /api/v1/reviews/statistics
```

## Permission System

### Core Permissions
- `project.read`, `project.create`, `project.update`, `project.delete`, `project.manage`
- `team.read`, `team.create`, `team.update`, `team.delete`, `team.manage`, `team.invite`
- `task.read`, `task.create`, `task.update`, `task.delete`, `task.assign`, `task.complete`
- `review.create`, `review.read`, `review.update`, `review.conduct`, `review.approve`

### Role Hierarchy
1. **OWNER**: Full system access
2. **ADMINISTRATOR**: Project and user management
3. **USER**: Standard collaboration capabilities
4. **VIEWER**: Read-only access

### Team Roles
- **OWNER**: Full team management
- **ADMIN**: Team administration
- **MEMBER**: Standard participation
- **VIEWER**: Read-only access

## AI Model Integration Patterns

### For Task Management AI
1. Use `/api/v1/tasks` endpoints to create, assign, and track tasks
2. Check permissions with `/api/v1/rbac/permissions/check` before operations
3. Use search endpoints for finding related tasks
4. Monitor overdue tasks with `/api/v1/tasks/overdue`

### For Team Collaboration AI
1. Use `/api/v1/teams` endpoints to manage team structures
2. Check team member permissions before collaborative actions
3. Use invitation system for adding new collaborators
4. Monitor team activity through member endpoints

### For Review Workflow AI
1. Use `/api/v1/reviews` endpoints to manage peer reviews
2. Create reviews for requirements that need approval
3. Monitor review status and deadlines
4. Use statistics endpoint for review analytics

### For Permission Management AI
1. Use `/api/v1/rbac/permissions/*` endpoints for access control
2. Check user permissions before suggesting actions
3. Use batch evaluation for efficiency
4. Monitor role hierarchy for delegation decisions

## Error Handling

All endpoints return standard HTTP status codes:
- `200`: Success
- `201`: Created successfully
- `400`: Bad request (validation errors)
- `401`: Unauthorized (invalid/expired token)
- `403`: Forbidden (insufficient permissions)
- `404`: Not found

Error responses include detailed messages:
```json
{
  "error": "User does not have permission to create teams for this project",
  "timestamp": 1234567890
}
```

## Rate Limiting

- Permission checks: 1000 requests/minute
- Team operations: 100 requests/minute
- Task operations: 500 requests/minute
- Review operations: 200 requests/minute

## Performance Considerations

- Permission evaluation uses multi-level caching (< 10ms response time)
- All list endpoints support pagination
- Batch operations available for efficiency
- Use ETags for cache validation where applicable

This API enables comprehensive collaboration features while maintaining security and performance for AI model integration.