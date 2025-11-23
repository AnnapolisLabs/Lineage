import api from './api'

export interface AdminUser {
  id: string
  email: string
  name: string
  firstName?: string | null
  lastName?: string | null
  globalRole: string
  status: string
  emailVerified: boolean
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
  phoneNumber?: string | null
  avatarUrl?: string | null
}

export interface UserListResponse {
  users: AdminUser[]
  page: number
  size: number
  total: number
  totalPages: number
}

export interface UserUpdateRequest {
  firstName?: string
  lastName?: string
  phoneNumber?: string
  globalRole?: string
  status?: string
}

export interface CreateUserRequest {
  email: string
  firstName: string
  lastName: string
  globalRole: string
  sendInvitation?: boolean
}

export interface SystemStatistics {
  totalUsers: number
  activeUsers: number
  totalProjects: number
  totalRequirements: number
  recentSignups: number
  recentLogins: number
}

export interface AuditLogEntry {
  id: string
  userId?: string
  userEmail?: string
  action: string
  resource: string
  resourceId?: string
  details?: Record<string, any>
  ipAddress: string
  userAgent: string
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL'
  createdAt: string
}

export interface AuditLogResponse {
  logs: AuditLogEntry[]
  page: number
  size: number
  total: number
  totalPages: number
}

export const adminService = {
  // User management
  async getUsers(page: number = 0, size: number = 20, search?: string): Promise<UserListResponse> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString()
    })
    if (search) {
      params.append('search', search)
    }
    const response = await api.get<UserListResponse>(`/admin/users?${params}`)
    return response.data
  },

  async getUserById(userId: string): Promise<AdminUser> {
    const response = await api.get<AdminUser>(`/admin/users/${userId}`)
    return response.data
  },

  async updateUser(userId: string, userData: UserUpdateRequest): Promise<AdminUser> {
    const response = await api.put<AdminUser>(`/admin/users/${userId}`, userData)
    return response.data
  },

  async deleteUser(userId: string): Promise<void> {
    await api.delete(`/admin/users/${userId}`)
  },

  async lockUserAccount(userId: string): Promise<void> {
    await api.post(`/admin/users/${userId}/lock`)
  },

  async unlockUserAccount(userId: string): Promise<void> {
    await api.post(`/admin/users/${userId}/unlock`)
  },

  async createUser(userData: CreateUserRequest): Promise<AdminUser> {
    const response = await api.post<AdminUser>('/admin/create-user', userData)
    return response.data
  },

  // System statistics
  async getSystemStatistics(): Promise<SystemStatistics> {
    const response = await api.get<SystemStatistics>('/admin/statistics')
    return response.data
  },

  // Audit logs
  async getAuditLogs(
    page: number = 0,
    size: number = 20,
    userId?: string,
    action?: string,
    severity?: string,
    startDate?: string,
    endDate?: string
  ): Promise<AuditLogResponse> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString()
    })

    if (userId) params.append('userId', userId)
    if (action) params.append('action', action)
    if (severity) params.append('severity', severity)
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)

    const response = await api.get<AuditLogResponse>(`/admin/audit-logs?${params}`)
    return response.data
  }
}