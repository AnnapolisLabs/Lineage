import { defineStore } from 'pinia'
import { ref } from 'vue'
import { rbacService } from '@/services/rbacService'
import type { UserPermission } from '@/types/rbac'

interface PermissionCache {
  [resourceId: string]: {
    permissions: Record<string, boolean>
    timestamp: number
  }
}

type BatchPermissionsEvaluator = (
  permissionsList: string[],
  resourceId: string
) => Promise<Record<string, boolean>>

const createHasAllPermissionsEvaluator = (evaluateBatch: BatchPermissionsEvaluator) => {
  return async (permissionsList: string[], resourceId: string): Promise<boolean> => {
    const results = await evaluateBatch(permissionsList, resourceId)
    return permissionsList.every(permission => results[permission] === true)
  }
}

const createHasAnyPermissionEvaluator = (evaluateBatch: BatchPermissionsEvaluator) => {
  return async (permissionsList: string[], resourceId: string): Promise<boolean> => {
    const results = await evaluateBatch(permissionsList, resourceId)
    return permissionsList.some(permission => results[permission] === true)
  }
}

export const useRbacStore = defineStore('rbac', () => {
  // State
  const permissions = ref<Record<string, UserPermission[]>>({})
  const loading = ref(false)
  const error = ref<string | null>(null)
  const permissionCache = ref<PermissionCache>({})

  // Cache duration: 5 minutes
  const CACHE_DURATION = 5 * 60 * 1000

  // Getters
  const hasCachedPermission = (permission: string, resourceId: string): boolean | null => {
    const cache = permissionCache.value[resourceId]
    if (!cache) return null

    const isExpired = Date.now() - cache.timestamp > CACHE_DURATION
    if (isExpired) {
      delete permissionCache.value[resourceId]
      return null
    }

    // Check if the permission exists in the cache
    if (permission in cache.permissions) {
      return cache.permissions[permission]
    }
    return null
  }

  const setCachedPermission = (permission: string, resourceId: string, authorized: boolean) => {
    if (!permissionCache.value[resourceId]) {
      permissionCache.value[resourceId] = {
        permissions: {},
        timestamp: Date.now()
      }
    }
    permissionCache.value[resourceId].permissions[permission] = authorized
    permissionCache.value[resourceId].timestamp = Date.now()
  }

  // Actions
  async function checkPermission(permission: string, resourceId: string): Promise<boolean> {
    // Check cache first
    const cached = hasCachedPermission(permission, resourceId)
    if (cached !== null) {
      return cached
    }

    loading.value = true
    error.value = null

    try {
      const result = await rbacService.checkPermission({ permission, resource_id: resourceId })
      const authorized = result.authorized

      // Cache the result
      setCachedPermission(permission, resourceId, authorized)

      // Store in permissions map
      if (!permissions.value[resourceId]) {
        permissions.value[resourceId] = []
      }

      const existingIndex = permissions.value[resourceId].findIndex(p => p.permission === permission)
      const permissionEntry: UserPermission = {
        permission,
        resource_id: resourceId,
        authorized,
        timestamp: result.timestamp
      }

      if (existingIndex >= 0) {
        permissions.value[resourceId][existingIndex] = permissionEntry
      } else {
        permissions.value[resourceId].push(permissionEntry)
      }

      return authorized
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Permission check failed'
      error.value = errorMsg
      console.error('Permission check error:', errorMsg)
      return false
    } finally {
      loading.value = false
    }
  }

  async function batchCheckPermissions(permissionsList: string[], resourceId: string): Promise<Record<string, boolean>> {
    const results: Record<string, boolean> = {}
    const uncachedPermissions: string[] = []

    // Check cache for each permission
    for (const permission of permissionsList) {
      const cached = hasCachedPermission(permission, resourceId)
      if (cached !== null) {
        results[permission] = cached
      } else {
        uncachedPermissions.push(permission)
      }
    }

    // If all permissions are cached, return early
    if (uncachedPermissions.length === 0) {
      return results
    }

    loading.value = true
    error.value = null

    try {
      const batchResults = await rbacService.batchCheckPermissions({
        permissions: uncachedPermissions,
        resource_id: resourceId
      })

      // Cache results and add to final result
      for (const result of batchResults) {
        setCachedPermission(result.permission, resourceId, result.authorized)
        results[result.permission] = result.authorized

        // Store in permissions map
        if (!permissions.value[resourceId]) {
          permissions.value[resourceId] = []
        }

        const existingIndex = permissions.value[resourceId].findIndex(p => p.permission === result.permission)
        if (existingIndex >= 0) {
          permissions.value[resourceId][existingIndex] = result
        } else {
          permissions.value[resourceId].push(result)
        }
      }

      // Add cached results to final result
      for (const permission of permissionsList) {
        if (results[permission] === undefined) {
          const cached = hasCachedPermission(permission, resourceId)
          if (cached !== null) {
            results[permission] = cached
          }
        }
      }

      return results
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Batch permission check failed'
      error.value = errorMsg
      console.error('Batch permission check error:', errorMsg)
      
      // Default to false for uncached permissions
      for (const permission of uncachedPermissions) {
        results[permission] = false
      }
      
      return results
    } finally {
      loading.value = false
    }
  }

  async function getUserPermissions(userId: string, resourceId: string): Promise<UserPermission[]> {
    loading.value = true
    error.value = null

    try {
      const userPermissions = await rbacService.getUserPermissions(userId, resourceId)
      
      // Cache all permissions
      for (const permission of userPermissions) {
        setCachedPermission(permission.permission, permission.resource_id, permission.authorized)
      }

      permissions.value[resourceId] = userPermissions
      return userPermissions
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Failed to get user permissions'
      error.value = errorMsg
      console.error('Get user permissions error:', errorMsg)
      return []
    } finally {
      loading.value = false
    }
  }

  async function checkRoleHierarchy(requiredRole: string, userId: string): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      return await rbacService.checkRoleHierarchy(requiredRole, userId)
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Role hierarchy check failed'
      error.value = errorMsg
      console.error('Role hierarchy check error:', errorMsg)
      return false
    } finally {
      loading.value = false
    }
  }

  function clearCache() {
    permissionCache.value = {}
  }

  function clearResourceCache(resourceId: string) {
    delete permissionCache.value[resourceId]
  }

  function clearError() {
    error.value = null
  }

  function getPermissionsForResource(resourceId: string): UserPermission[] {
    return permissions.value[resourceId] || []
  }

  // Helper methods to check multiple permissions
  const hasAllPermissions = createHasAllPermissionsEvaluator(batchCheckPermissions)
  const hasAnyPermission = createHasAnyPermissionEvaluator(batchCheckPermissions)

  return {
    // State
    permissions,
    loading,
    error,
    permissionCache,

    // Getters
    hasCachedPermission,
    setCachedPermission,

    // Actions
    checkPermission,
    batchCheckPermissions,
    getUserPermissions,
    checkRoleHierarchy,
    clearCache,
    clearResourceCache,
    clearError,
    getPermissionsForResource,
    hasAllPermissions,
    hasAnyPermission
  }
})