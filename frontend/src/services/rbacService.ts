import api from './api'
import type {
  PermissionCheckRequest,
  PermissionCheckResponse,
  BatchPermissionCheckRequest,
  UserPermission
} from '@/types/rbac'

export const rbacService = {
  // Permission Evaluation
  async checkPermission(request: PermissionCheckRequest): Promise<PermissionCheckResponse> {
    const response = await api.post<PermissionCheckResponse>('/v1/rbac/permissions/check', null, {
      params: {
        permission: request.permission,
        resource_id: request.resource_id
      }
    })
    return response.data
  },

  async batchCheckPermissions(request: BatchPermissionCheckRequest): Promise<UserPermission[]> {
    const response = await api.post<UserPermission[]>('/v1/rbac/permissions/evaluate', request)
    return response.data
  },

  async getUserPermissions(userId: string, resourceId: string): Promise<UserPermission[]> {
    const response = await api.get<UserPermission[]>(`/v1/rbac/users/${userId}/permissions`, {
      params: { resource_id: resourceId }
    })
    return response.data
  },

  async checkRoleHierarchy(requiredRole: string, userId: string): Promise<boolean> {
    const response = await api.get<{ authorized: boolean }>(`/v1/rbac/roles/hierarchy/check`, {
      params: { required_role: requiredRole, user_id: userId }
    })
    return response.data.authorized
  },

  // Helper method to check if user has specific permission
  async hasPermission(permission: string, resourceId: string): Promise<boolean> {
    try {
      const result = await this.checkPermission({ permission, resource_id: resourceId })
      return result.authorized
    } catch (error) {
      console.error('Permission check failed:', error)
      return false
    }
  },

  // Helper method to check multiple permissions
  async hasPermissions(permissions: string[], resourceId: string): Promise<Record<string, boolean>> {
    try {
      const results = await this.batchCheckPermissions({ permissions, resource_id: resourceId })
      const permissionMap: Record<string, boolean> = {}

      for (const result of results) {
        permissionMap[result.permission] = result.authorized
      }

      return permissionMap
    } catch (error) {
      console.error('Batch permission check failed:', error)
      // Return false for all permissions if check fails
      const permissionMap: Record<string, boolean> = {}
      for (const permission of permissions) {
        permissionMap[permission] = false
      }
      return permissionMap
    }
  }
}