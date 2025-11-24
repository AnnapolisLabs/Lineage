// RBAC TypeScript Interfaces

export interface Permission {
  id: string
  name: string
  resource: string
  action: string
  description: string
}

export interface User {
  id: string
  email: string
  name: string
  firstName?: string
  lastName?: string
  globalRole: string
  status: string
  phoneNumber?: string
  avatarUrl?: string
  bio?: string
  preferences?: Record<string, any>
  emailVerified: boolean
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
}

// Permission Evaluation Types
export interface PermissionCheckRequest {
  permission: string
  resource_id: string
}

export interface PermissionCheckResponse {
  user_id: string
  permission: string
  resource_id: string
  authorized: boolean
  timestamp: number
}

export interface BatchPermissionCheckRequest {
  permissions: string[]
  resource_id: string
}

export interface RoleHierarchyCheckRequest {
  required_role: string
  user_id: string
}

// Team Management Types
export interface Team {
  id: string
  name: string
  description: string
  projectId: string
  settings: TeamSettings
  createdAt: string
  updatedAt: string
  createdBy: string
  memberCount: number
}

export interface TeamSettings {
  require_peer_review: boolean
  max_members: number
}

export interface CreateTeamRequest {
  name: string
  description: string
  project_id: string
  settings: TeamSettings
}

export interface TeamMember {
  id: string
  teamId: string
  userId: string
  user: User
  role: TeamRole
  joinedAt: string
  invitedBy: string
  status: 'ACTIVE' | 'INVITED' | 'PENDING'
}

export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'

export interface InviteTeamMemberRequest {
  email: string
  role: TeamRole
  message?: string
}

export interface TeamInvitation {
  id: string
  teamId: string
  email: string
  role: TeamRole
  message?: string
  invitedBy: string
  invitedAt: string
  expiresAt: string
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED'
}

export interface UpdateMemberRoleRequest {
  role: TeamRole
}

// Task Assignment Types
export interface Task {
  id: string
  taskTitle: string
  taskDescription: string
  assignedTo: string
  assignedUser?: User
  projectId: string
  requirementId?: string
  requirement?: Requirement
  priority: TaskPriority
  status: TaskStatus
  dueDate?: string
  startedAt?: string
  completedAt?: string
  cancelledAt?: string
  completionNotes?: string
  createdAt: string
  updatedAt: string
  createdBy: string
  tags: string[]
  blockers: string[]
}

export type TaskPriority = 'low' | 'medium' | 'high' | 'critical'
export type TaskStatus = 'assigned' | 'in_progress' | 'completed' | 'cancelled'

export interface CreateTaskRequest {
  task_title: string
  task_description: string
  assigned_to: string
  project_id: string
  priority: TaskPriority
  due_date?: string
}

export interface ReassignTaskRequest {
  assigned_to: string
}

export interface CompleteTaskRequest {
  completion_notes?: string
}

export interface CancelTaskRequest {
  reason: string
}

export interface AddTaskTagRequest {
  tag: string
}

export interface AddTaskBlockerRequest {
  blocker: string
}

// Peer Review Types
export interface PeerReview {
  id: string
  requirementId: string
  requirement?: Requirement
  reviewerId: string
  reviewer?: User
  authorId: string
  author?: User
  reviewType: ReviewType
  reviewDeadline?: string
  status: ReviewStatus
  startedAt?: string
  completedAt?: string
  approvalStatus?: ApprovalStatus
  comments?: string
  effortRating?: number
  qualityRating?: number
  createdAt: string
  updatedAt: string
}

export type ReviewType = 'code' | 'design' | 'documentation' | 'process' | 'requirements'
export type ReviewStatus = 'pending' | 'in_progress' | 'completed' | 'cancelled'
export type ApprovalStatus = 'approved' | 'rejected' | 'revision_requested'

export interface CreatePeerReviewRequest {
  requirement_id: string
  reviewer_id: string
  author_id: string
  review_type: ReviewType
  review_deadline?: string
}

export interface ReviewActionRequest {
  comments?: string
}

export interface SetReviewRatingsRequest {
  effort_rating: number
  quality_rating: number
}

// Requirement type for reviews
export interface Requirement {
  id: string
  reqId: string
  title: string
  description: string
  status: string
  priority: string
  parentId?: string
  parentReqId?: string
  level: number
  section?: string
  customFields: Record<string, any>
  createdByName: string
  createdByEmail: string
  createdAt: string
  updatedAt: string
  inLinkCount?: number
  outLinkCount?: number
}

// API Response Types
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export interface ApiError {
  error: string
  timestamp: number
}