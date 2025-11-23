# OpenAPI/Swagger API Specifications
## RBAC Architecture - API Documentation

**Version:** 1.0  
**Date:** 2025-11-23  
**Base URL:** `https://api.lineage.example.com/v1`  

---

## OpenAPI 3.0 Specification

```yaml
openapi: 3.0.3
info:
  title: Lineage RBAC API
  description: |
    Comprehensive Role-Based Access Control API for the Lineage system.
    
    This API provides:
    - Role management and hierarchy
    - Permission evaluation and assignment
    - Team collaboration features
    - Task assignment and tracking
    - Peer review system
    
    ## Authentication
    All endpoints require Bearer token authentication.
    
    ## Rate Limiting
    - Standard endpoints: 1000 requests/hour
    - Permission evaluation: 10000 requests/hour
    - Bulk operations: 100 requests/hour
  version: '1.0.0'
  contact:
    name: Lineage API Support
    email: api-support@lineage.example.com
  license:
    name: MIT
    url: https://opensource.org/licenses/MIT

servers:
  - url: https://api.lineage.example.com/v1
    description: Production server
  - url: https://staging-api.lineage.example.com/v1
    description: Staging server
  - url: http://localhost:8080/v1
    description: Development server

security:
  - bearerAuth: []

paths:
  # ===============================================
  # ROLE MANAGEMENT ENDPOINTS
  # ===============================================
  
  /rbac/roles:
    get:
      tags:
        - Role Management
      summary: Get all roles
      description: Retrieve all available roles with their permissions and hierarchy information
      security:
        - bearerAuth: []
      parameters:
        - name: type
          in: query
          description: Filter by role type
          schema:
            type: string
            enum: [GLOBAL, PROJECT]
        - name: hierarchy_level
          in: query
          description: Filter by hierarchy level
          schema:
            type: integer
            minimum: 1
            maximum: 3
        - name: include_permissions
          in: query
          description: Include detailed permission information
          schema:
            type: boolean
            default: false
      responses:
        '200':
          description: List of roles
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/Role'
                  metadata:
                    $ref: '#/components/schemas/PaginationMetadata'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
    
    post:
      tags:
        - Role Management
      summary: Create new role
      description: Create a new role definition with permissions
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateRoleRequest'
      responses:
        '201':
          description: Role created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Role'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '409':
          description: Role name already exists
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'

  /rbac/roles/{roleId}:
    get:
      tags:
        - Role Management
      summary: Get role by ID
      description: Retrieve detailed information about a specific role
      security:
        - bearerAuth: []
      parameters:
        - name: roleId
          in: path
          required: true
          description: Role ID
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Role details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RoleDetail'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'
    
    put:
      tags:
        - Role Management
      summary: Update role
      description: Update role permissions and properties
      security:
        - bearerAuth: []
      parameters:
        - name: roleId
          in: path
          required: true
          description: Role ID
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateRoleRequest'
      responses:
        '200':
          description: Role updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Role'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'
    
    delete:
      tags:
        - Role Management
      summary: Delete role
      description: Delete a role (system roles cannot be deleted)
      security:
        - bearerAuth: []
      parameters:
        - name: roleId
          in: path
          required: true
          description: Role ID
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Role deleted successfully
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'
        '409':
          description: Cannot delete system role or role in use
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'

  # ===============================================
  # PERMISSION EVALUATION ENDPOINTS
  # ===============================================
  
  /rbac/permissions/evaluate:
    post:
      tags:
        - Permission Evaluation
      summary: Evaluate user permissions
      description: Check if a user has specific permissions for a resource
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PermissionEvaluationRequest'
      responses:
        '200':
          description: Permission evaluation result
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PermissionEvaluationResult'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
    
    get:
      tags:
        - Permission Evaluation
      summary: Batch evaluate permissions
      description: Evaluate multiple permissions in a single request
      security:
        - bearerAuth: []
      parameters:
        - name: user_id
          in: query
          required: true
          description: User ID to check
          schema:
            type: string
            format: uuid
        - name: permissions
          in: query
          required: true
          description: Comma-separated list of permission keys
          schema:
            type: string
        - name: resource_id
          in: query
          description: Resource ID for scoped permissions
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Batch evaluation results
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BatchPermissionEvaluationResult'

  /rbac/users/{userId}/permissions:
    get:
      tags:
        - Permission Management
      summary: Get user permissions
      description: Retrieve all permissions for a specific user
      security:
        - bearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
          description: User ID
          schema:
            type: string
            format: uuid
        - name: scope
          in: query
          description: Filter by permission scope
          schema:
            type: string
            enum: [GLOBAL, PROJECT]
        - name: resource_id
          in: query
          description: Filter by resource ID
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: User permissions
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserPermissions'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'

    post:
      tags:
        - Permission Management
      summary: Grant permissions to user
      description: Grant specific permissions to a user
      security:
        - bearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
          description: User ID
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GrantPermissionsRequest'
      responses:
        '200':
          description: Permissions granted successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GrantPermissionsResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'

  /rbac/permissions/definitions:
    get:
      tags:
        - Permission Definitions
      summary: Get all permission definitions
      description: Retrieve all available permission definitions
      security:
        - bearerAuth: []
      parameters:
        - name: category
          in: query
          description: Filter by permission category
          schema:
            type: string
            enum: [project_management, user_management, role_management, content_management, collaboration, task_management, quality_assurance, communication, workflow, system_administration, audit_compliance, integration, data_management]
        - name: resource
          in: query
          description: Filter by resource type
          schema:
            type: string
        - name: risk_level
          in: query
          description: Filter by risk level
          schema:
            type: string
            enum: [LOW, MEDIUM, HIGH, CRITICAL]
      responses:
        '200':
          description: List of permission definitions
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/PermissionDefinition'
                  metadata:
                    $ref: '#/components/schemas/PaginationMetadata'

  # ===============================================
  # TEAM MANAGEMENT ENDPOINTS
  # ===============================================
  
  /teams:
    get:
      tags:
        - Team Management
      summary: Get teams
      description: List teams accessible to the current user
      security:
        - bearerAuth: []
      parameters:
        - name: project_id
          in: query
          description: Filter by project ID
          schema:
            type: string
            format: uuid
        - name: status
          in: query
          description: Filter by team status
          schema:
            type: string
            enum: [active, inactive]
        - name: include_members
          in: query
          description: Include team member information
          schema:
            type: boolean
            default: false
      responses:
        '200':
          description: List of teams
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/Team'
                  metadata:
                    $ref: '#/components/schemas/PaginationMetadata'
    
    post:
      tags:
        - Team Management
      summary: Create team
      description: Create a new team for project collaboration
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateTeamRequest'
      responses:
        '201':
          description: Team created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Team'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'

  /teams/{teamId}:
    get:
      tags:
        - Team Management
      summary: Get team details
      description: Retrieve detailed information about a specific team
      security:
        - bearerAuth: []
      parameters:
        - name: teamId
          in: path
          required: true
          description: Team ID
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Team details with members
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TeamDetail'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'

  /teams/{teamId}/members:
    get:
      tags:
        - Team Management
      summary: Get team members
      description: List all members of a specific team
      security:
        - bearerAuth: []
      parameters:
        - name: teamId
          in: path
          required: true
          description: Team ID
          schema:
            type: string
            format: uuid
        - name: status
          in: query
          description: Filter by member status
          schema:
            type: string
            enum: [active, inactive, pending]
      responses:
        '200':
          description: Team members list
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/TeamMember'
                  metadata:
                    $ref: '#/components/schemas/PaginationMetadata'
    
    post:
      tags:
        - Team Management
      summary: Invite user to team
      description: Invite a user to join the team
      security:
        - bearerAuth: []
      parameters:
        - name: teamId
          in: path
          required: true
          description: Team ID
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/InviteTeamMemberRequest'
      responses:
        '201':
          description: Invitation sent successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TeamInvitation'

  /teams/{teamId}/members/{memberId}:
    delete:
      tags:
        - Team Management
      summary: Remove team member
      description: Remove a user from the team
      security:
        - bearerAuth: []
      parameters:
        - name: teamId
          in: path
          required: true
          description: Team ID
          schema:
            type: string
            format: uuid
        - name: memberId
          in: path
          required: true
          description: Team member ID
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Member removed successfully
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'

  # ===============================================
  # TASK MANAGEMENT ENDPOINTS
  # ===============================================
  
  /tasks:
    get:
      tags:
        - Task Management
      summary: Get tasks
      description: List tasks assigned to the current user or project
      security:
        - bearerAuth: []
      parameters:
        - name: assigned_to
          in: query
          description: Filter by assigned user ID
          schema:
            type: string
            format: uuid
        - name: project_id
          in: query
          description: Filter by project ID
          schema:
            type: string
            format: uuid
        - name: status
          in: query
          description: Filter by task status
          schema:
            type: string
            enum: [assigned, in_progress, completed, cancelled]
        - name: priority
          in: query
          description: Filter by priority level
          schema:
            type: string
            enum: [low, medium, high, critical]
      responses:
        '200':
          description: List of tasks
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/Task'
                  metadata:
                    $ref: '#/components/schemas/PaginationMetadata'
    
    post:
      tags:
        - Task Management
      summary: Create task
      description: Create a new task assignment
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateTaskRequest'
      responses:
        '201':
          description: Task created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Task'

  /tasks/{taskId}:
    get:
      tags:
        - Task Management
      summary: Get task details
      description: Retrieve detailed information about a specific task
      security:
        - bearerAuth: []
      parameters:
        - name: taskId
          in: path
          required: true
          description: Task ID
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Task details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TaskDetail'
    
    put:
      tags:
        - Task Management
      summary: Update task
      description: Update task status or details
      security:
        - bearerAuth: []
      parameters:
        - name: taskId
          in: path
          required: true
          description: Task ID
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateTaskRequest'
      responses:
        '200':
          description: Task updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Task'
    
    delete:
      tags:
        - Task Management
      summary: Delete task
      description: Delete a task assignment
      security:
        - bearerAuth: []
      parameters:
        - name: taskId
          in: path
          required: true
          description: Task ID
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Task deleted successfully

  /tasks/{taskId}/complete:
    post:
      tags:
        - Task Management
      summary: Complete task
      description: Mark a task as completed
      security:
        - bearerAuth: []
      parameters:
        - name: taskId
          in: path
          required: true
          description: Task ID
          schema:
            type: string
            format: uuid
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CompleteTaskRequest'
      responses:
        '200':
          description: Task marked as completed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Task'

  # ===============================================
  # PEER REVIEW ENDPOINTS
  # ===============================================
  
  /reviews:
    get:
      tags:
        - Peer Review
      summary: Get reviews
      description: List peer reviews for requirements
      security:
        - bearerAuth: []
      parameters:
        - name: requirement_id
          in: query
          description: Filter by requirement ID
          schema:
            type: string
            format: uuid
        - name: reviewer_id
          in: query
          description: Filter by reviewer ID
          schema:
            type: string
            format: uuid
        - name: status
          in: query
          description: Filter by review status
          schema:
            type: string
            enum: [pending, approved, rejected, revision_requested]
      responses:
        '200':
          description: List of peer reviews
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/PeerReview'
                  metadata:
                    $ref: '#/components/schemas/PaginationMetadata'
    
    post:
      tags:
        - Peer Review
      summary: Create peer review
      description: Create a new peer review for a requirement
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreatePeerReviewRequest'
      responses:
        '201':
          description: Peer review created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PeerReview'

  /reviews/{reviewId}:
    get:
      tags:
        - Peer Review
      summary: Get review details
      description: Retrieve detailed information about a specific review
      security:
        - bearerAuth: []
      parameters:
        - name: reviewId
          in: path
          required: true
          description: Review ID
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Review details with comments
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PeerReviewDetail'
    
    put:
      tags:
        - Peer Review
      summary: Update review
      description: Update review status, comments, or scores
      security:
        - bearerAuth: []
      parameters:
        - name: reviewId
          in: path
          required: true
          description: Review ID
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdatePeerReviewRequest'
      responses:
        '200':
          description: Review updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PeerReview'

# ===============================================
# SCHEMAS
# ===============================================

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: JWT Bearer token authentication

  schemas:
    # Error schema
    Error:
      type: object
      properties:
        error:
          type: object
          properties:
            code:
              type: string
              example: VALIDATION_ERROR
            message:
              type: string
              example: The request contains invalid parameters
            details:
              type: array
              items:
                type: object
                properties:
                  field:
                    type: string
                  message:
                    type: string
            timestamp:
              type: string
              format: date-time
            path:
              type: string

    # Pagination metadata
    PaginationMetadata:
      type: object
      properties:
        total:
          type: integer
          description: Total number of items
        page:
          type: integer
          description: Current page number
        size:
          type: integer
          description: Number of items per page
        pages:
          type: integer
          description: Total number of pages

    # Role schemas
    Role:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
          example: OWNER
        description:
          type: string
        type:
          type: string
          enum: [GLOBAL, PROJECT]
        hierarchy_level:
          type: integer
          minimum: 1
          maximum: 3
        is_system:
          type: boolean
        is_active:
          type: boolean
        permissions:
          type: array
          items:
            type: string
        created_at:
          type: string
          format: date-time
        updated_at:
          type: string
          format: date-time

    RoleDetail:
      allOf:
        - $ref: '#/components/schemas/Role'
        - type: object
          properties:
            inheritance_path:
              type: array
              items:
                type: string
                format: uuid
            escalation_rules:
              type: object
            collaboration_permissions:
              type: array
              items:
                type: string
            resource_scopes:
              type: array
              items:
                type: object
                properties:
                  type:
                    type: string
                  resource_id:
                    type: string
                    format: uuid
                  granted:
                    type: boolean

    CreateRoleRequest:
      type: object
      required:
        - name
        - type
        - permissions
      properties:
        name:
          type: string
          minLength: 2
          maxLength: 50
          pattern: '^[A-Z_]+$'
        description:
          type: string
          maxLength: 255
        type:
          type: string
          enum: [GLOBAL, PROJECT]
        hierarchy_level:
          type: integer
          minimum: 1
          maximum: 3
          default: 1
        permissions:
          type: array
          items:
            type: string
          minItems: 1
        inheritance_path:
          type: array
          items:
            type: string
            format: uuid
        collaboration_permissions:
          type: array
          items:
            type: string
        resource_scopes:
          type: array
          items:
            type: object
            properties:
              type:
                type: string
              resource_id:
                type: string
                format: uuid
              granted:
                type: boolean

    UpdateRoleRequest:
      type: object
      properties:
        name:
          type: string
          minLength: 2
          maxLength: 50
        description:
          type: string
          maxLength: 255
        permissions:
          type: array
          items:
            type: string
        collaboration_permissions:
          type: array
          items:
            type: string
        resource_scopes:
          type: array
          items:
            type: object
            properties:
              type:
                type: string
              resource_id:
                type: string
                format: uuid
              granted:
                type: boolean
        is_active:
          type: boolean

    # Permission schemas
    PermissionDefinition:
      type: object
      properties:
        id:
          type: string
          format: uuid
        permission_key:
          type: string
          example: project.create
        resource:
          type: string
          example: project
        action:
          type: string
          example: create
        description:
          type: string
        category:
          type: string
          example: project_management
        requires_confirmation:
          type: boolean
        audit_required:
          type: boolean
        is_system:
          type: boolean
        risk_level:
          type: string
          enum: [LOW, MEDIUM, HIGH, CRITICAL]
        created_at:
          type: string
          format: date-time
        updated_at:
          type: string
          format: date-time

    PermissionEvaluationRequest:
      type: object
      required:
        - user_id
        - permission
      properties:
        user_id:
          type: string
          format: uuid
        permission:
          type: string
        resource_id:
          type: string
          format: uuid
        context:
          type: object
          description: Additional context for permission evaluation

    PermissionEvaluationResult:
      type: object
      properties:
        authorized:
          type: boolean
        reason:
          type: string
          enum: [authorized, insufficient_permissions, resource_not_found, access_denied]
        missing_permissions:
          type: array
          items:
            type: string
        user_permissions:
          type: array
          items:
            type: string
        required_permissions:
          type: array
          items:
            type: string
        evaluation_duration_ms:
          type: integer

    BatchPermissionEvaluationResult:
      type: object
      properties:
        user_id:
          type: string
          format: uuid
        results:
          type: array
          items:
            type: object
            properties:
              permission:
                type: string
              authorized:
                type: boolean
              reason:
                type: string
        total_evaluations:
          type: integer
        successful_evaluations:
          type: integer
        evaluation_duration_ms:
          type: integer

    UserPermissions:
      type: object
      properties:
        user_id:
          type: string
          format: uuid
        global_permissions:
          type: array
          items:
            type: string
        project_permissions:
          type: array
          items:
            type: object
            properties:
              project_id:
                type: string
                format: uuid
              project_name:
                type: string
              permissions:
                type: array
                items:
                  type: string
        effective_permissions:
          type: array
          items:
            type: string
        last_updated:
          type: string
          format: date-time

    GrantPermissionsRequest:
      type: object
      required:
        - permissions
      properties:
        permissions:
          type: array
          items:
            type: object
            properties:
              permission_key:
                type: string
              scope:
                type: string
                enum: [GLOBAL, PROJECT]
              resource_id:
                type: string
                format: uuid
              expires_at:
                type: string
                format: date-time
        reason:
          type: string
        temporary:
          type: boolean
          default: false

    GrantPermissionsResponse:
      type: object
      properties:
        granted_permissions:
          type: array
          items:
            type: string
        expires_at:
          type: string
          format: date-time
        approval_required:
          type: boolean
        audit_entry_id:
          type: string
          format: uuid

    # Team schemas
    Team:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        description:
          type: string
        project_id:
          type: string
          format: uuid
        created_by:
          type: string
          format: uuid
        is_active:
          type: boolean
        settings:
          type: object
        member_count:
          type: integer
        created_at:
          type: string
          format: date-time
        updated_at:
          type: string
          format: date-time

    TeamDetail:
      allOf:
        - $ref: '#/components/schemas/Team'
        - type: object
          properties:
            members:
              type: array
              items:
                $ref: '#/components/schemas/TeamMember'
            settings:
              type: object
              properties:
                auto_assign_reviewers:
                  type: boolean
                require_peer_review:
                  type: boolean
                max_members:
                  type: integer

    TeamMember:
      type: object
      properties:
        id:
          type: string
          format: uuid
        user_id:
          type: string
          format: uuid
        user:
          type: object
          properties:
            email:
              type: string
            first_name:
              type: string
            last_name:
              type: string
            avatar_url:
              type: string
        role:
          type: string
          enum: [owner, admin, member, viewer]
        permissions:
          type: array
          items:
            type: string
        status:
          type: string
          enum: [active, inactive, pending]
        joined_at:
          type: string
          format: date-time
        contribution_score:
          type: integer

    CreateTeamRequest:
      type: object
      required:
        - name
        - project_id
      properties:
        name:
          type: string
          minLength: 2
          maxLength: 100
        description:
          type: string
          maxLength: 500
        project_id:
          type: string
          format: uuid
        settings:
          type: object
          properties:
            auto_assign_reviewers:
              type: boolean
              default: false
            require_peer_review:
              type: boolean
              default: true
            max_members:
              type: integer
              minimum: 1
              maximum: 100
              default: 50

    InviteTeamMemberRequest:
      type: object
      required:
        - email
        - role
      properties:
        email:
          type: string
          format: email
        role:
          type: string
          enum: [admin, member, viewer]
        permissions:
          type: array
          items:
            type: string
        message:
          type: string
          maxLength: 500
        expires_in_hours:
          type: integer
          minimum: 1
          maximum: 168
          default: 72

    TeamInvitation:
      type: object
      properties:
        id:
          type: string
          format: uuid
        team_id:
          type: string
          format: uuid
        email:
          type: string
        role:
          type: string
        status:
          type: string
          enum: [pending, accepted, declined, expired]
        token:
          type: string
        expires_at:
          type: string
          format: date-time
        invited_at:
          type: string
          format: date-time

    # Task schemas
    Task:
      type: object
      properties:
        id:
          type: string
          format: uuid
        task_title:
          type: string
        task_description:
          type: string
        assigned_by:
          type: string
          format: uuid
        assigned_to:
          type: string
          format: uuid
        project_id:
          type: string
          format: uuid
        requirement_id:
          type: string
          format: uuid
        status:
          type: string
          enum: [assigned, in_progress, completed, cancelled]
        priority:
          type: string
          enum: [low, medium, high, critical]
        due_date:
          type: string
          format: date-time
        created_at:
          type: string
          format: date-time
        updated_at:
          type: string
          format: date-time

    TaskDetail:
      allOf:
        - $ref: '#/components/schemas/Task'
        - type: object
          properties:
            assigned_by_user:
              type: object
              properties:
                email:
                  type: string
                first_name:
                  type: string
                last_name:
                  type: string
            assigned_to_user:
              type: object
              properties:
                email:
                  type: string
                first_name:
                  type: string
                last_name:
                  type: string
            project:
              type: object
              properties:
                id:
                  type: string
                  format: uuid
                name:
                  type: string
            requirement:
              type: object
              properties:
                id:
                  type: string
                  format: uuid
                title:
                  type: string
            comments:
              type: array
              items:
                $ref: '#/components/schemas/TaskComment'
            completion_notes:
              type: string

    CreateTaskRequest:
      type: object
      required:
        - task_title
        - assigned_to
      properties:
        task_title:
          type: string
          minLength: 1
          maxLength: 255
        task_description:
          type: string
          maxLength: 2000
        assigned_to:
          type: string
          format: uuid
        project_id:
          type: string
          format: uuid
        requirement_id:
          type: string
          format: uuid
        team_id:
          type: string
          format: uuid
        priority:
          type: string
          enum: [low, medium, high, critical]
          default: medium
        due_date:
          type: string
          format: date-time
        estimated_hours:
          type: integer
          minimum: 1
        tags:
          type: array
          items:
            type: string

    UpdateTaskRequest:
      type: object
      properties:
        task_title:
          type: string
          minLength: 1
          maxLength: 255
        task_description:
          type: string
          maxLength: 2000
        status:
          type: string
          enum: [assigned, in_progress, completed, cancelled]
        priority:
          type: string
          enum: [low, medium, high, critical]
        due_date:
          type: string
          format: date-time
        estimated_hours:
          type: integer
          minimum: 1
        actual_hours:
          type: integer
          minimum: 0
        completion_notes:
          type: string
          maxLength: 2000
        blockers:
          type: array
          items:
            type: string
        tags:
          type: array
          items:
            type: string

    CompleteTaskRequest:
      type: object
      properties:
        completion_notes:
          type: string
          maxLength: 2000
        actual_hours:
          type: integer
          minimum: 0

    TaskComment:
      type: object
      properties:
        id:
          type: string
          format: uuid
        user_id:
          type: string
          format: uuid
        user:
          type: object
          properties:
            email:
              type: string
            first_name:
              type: string
            last_name:
              type: string
        comment_text:
          type: string
        is_internal:
          type: boolean
        created_at:
          type: string
          format: date-time
        updated_at:
          type: string
          format: date-time

    # Peer Review schemas
    PeerReview:
      type: object
      properties:
        id:
          type: string
          format: uuid
        requirement_id:
          type: string
          format: uuid
        reviewer_id:
          type: string
          format: uuid
        author_id:
          type: string
          format: uuid
        status:
          type: string
          enum: [pending, approved, rejected, revision_requested]
        review_type:
          type: string
          enum: [code, design, documentation, process]
        comments:
          type: string
        score:
          type: integer
          minimum: 1
          maximum: 5
        reviewed_at:
          type: string
          format: date-time
        created_at:
          type: string
          format: date-time
        updated_at:
          type: string
          format: date-time

    PeerReviewDetail:
      allOf:
        - $ref: '#/components/schemas/PeerReview'
        - type: object
          properties:
            reviewer:
              type: object
              properties:
                email:
                  type: string
                first_name:
                  type: string
                last_name:
                  type: string
            author:
              type: object
              properties:
                email:
                  type: string
                first_name:
                  type: string
                last_name:
                  type: string
            requirement:
              type: object
              properties:
                id:
                  type: string
                  format: uuid
                title:
                  type: string
            review_details:
              type: object
              properties:
                effort_rating:
                  type: integer
                  minimum: 1
                  maximum: 5
                quality_rating:
                  type: integer
                  minimum: 1
                  maximum: 5
                priority_suggestion:
                  type: string
                  enum: [maintain, increase, decrease]
            comments:
              type: array
              items:
                $ref: '#/components/schemas/ReviewComment'

    CreatePeerReviewRequest:
      type: object
      required:
        - requirement_id
        - reviewer_id
      properties:
        requirement_id:
          type: string
          format: uuid
        reviewer_id:
          type: string
          format: uuid
        review_type:
          type: string
          enum: [code, design, documentation, process]
          default: code
        deadline:
          type: string
          format: date-time

    UpdatePeerReviewRequest:
      type: object
      properties:
        status:
          type: string
          enum: [pending, approved, rejected, revision_requested]
        comments:
          type: string
        score:
          type: integer
          minimum: 1
          maximum: 5
        review_details:
          type: object
          properties:
            effort_rating:
              type: integer
              minimum: 1
              maximum: 5
            quality_rating:
              type: integer
              minimum: 1
              maximum: 5
            priority_suggestion:
              type: string
              enum: [maintain, increase, decrease]

    ReviewComment:
      type: object
      properties:
        id:
          type: string
          format: uuid
        user_id:
          type: string
          format: uuid
        user:
          type: object
          properties:
            email:
              type: string
            first_name:
              type: string
            last_name:
              type: string
        comment_text:
          type: string
        comment_type:
          type: string
          enum: [general, suggestion, issue, question, praise]
        line_number:
          type: integer
        file_path:
          type: string
        is_resolved:
          type: boolean
        resolved_by:
          type: string
          format: uuid
        resolved_at:
          type: string
          format: date-time
        parent_comment_id:
          type: string
          format: uuid
        created_at:
          type: string
          format: date-time
        updated_at:
          type: string
          format: date-time

  # Response schemas
  responses:
    BadRequest:
      description: Bad request
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    
    Unauthorized:
      description: Unauthorized
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    
    Forbidden:
      description: Forbidden
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    
    NotFound:
      description: Not found
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'

# ===============================================
# TAGS
# ===============================================

tags:
  - name: Role Management
    description: Role creation, updates, and hierarchy management
  - name: Permission Evaluation
    description: Real-time permission checking and evaluation
  - name: Permission Management
    description: Granting and revoking user permissions
  - name: Permission Definitions
    description: System permission definitions and metadata
  - name: Team Management
    description: Team creation, member management, and collaboration
  - name: Task Management
    description: Task assignment, tracking, and completion
  - name: Peer Review
    description: Peer review system for quality assurance
```

---

## API Usage Examples

### Example 1: Permission Evaluation

```javascript
// Check if user can delete a project
const response = await fetch('/api/v1/rbac/permissions/evaluate', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    user_id: 'user-uuid',
    permission: 'project.delete',
    resource_id: 'project-uuid',
    context: {
      is_owner: false,
      project_role: 'ADMIN'
    }
  })
});

const result = await response.json();
// {
//   "authorized": false,
//   "reason": "insufficient_permissions",
//   "missing_permissions": ["project.delete"],
//   "user_permissions": ["project.read", "project.update"]
// }
```

### Example 2: Team Management

```javascript
// Create a new team
const response = await fetch('/api/v1/teams', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'Frontend Development Team',
    description: 'Responsible for frontend development',
    project_id: 'project-uuid',
    settings: {
      auto_assign_reviewers: true,
      require_peer_review: true,
      max_members: 10
    }
  })
});

const team = await response.json();
```

### Example 3: Task Assignment

```javascript
// Assign a task
const response = await fetch('/api/v1/tasks', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    task_title: 'Implement user authentication',
    task_description: 'Create login and registration components',
    assigned_to: 'user-uuid',
    project_id: 'project-uuid',
    priority: 'high',
    due_date: '2025-12-15T17:00:00Z'
  })
});

const task = await response.json();
```

---

## Rate Limiting

The API implements rate limiting to ensure fair usage:

- **Standard endpoints**: 1000 requests per hour
- **Permission evaluation**: 10000 requests per hour
- **Bulk operations**: 100 requests per hour
- **Admin endpoints**: 500 requests per hour

Rate limit headers are included in all responses:

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1634567890
```

## Error Handling

All errors follow a consistent format:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The request contains invalid parameters",
    "details": [
      {
        "field": "email",
        "message": "Invalid email format"
      }
    ],
    "timestamp": "2025-11-23T04:25:36.952Z",
    "path": "/api/v1/rbac/users/permissions"
  }
}
```

## Authentication

All endpoints require Bearer token authentication:

```
Authorization: Bearer <jwt_token>
```

The JWT token should include the user's role and permissions as specified in the architecture document.

## Pagination

List endpoints support pagination using query parameters:

- `page`: Page number (default: 1)
- `size`: Items per page (default: 20, max: 100)

Response includes metadata:

```json
{
  "data": [...],
  "metadata": {
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```
