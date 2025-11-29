import { ref, type Ref } from 'vue'
import { rbacService } from '@/services/rbacService'

type PermissionCacheEntry = {
  timestamp: number
  authorized: boolean
}

// Permission cache to avoid repeated API calls
const permissionCache = new Map<string, PermissionCacheEntry>()
const CACHE_DURATION = 5 * 60 * 1000 // 5 minutes

const buildCacheKey = (permission: string, resourceId: string) => `${permission}:${resourceId}`

function getValidCacheEntry(permission: string, resourceId: string): PermissionCacheEntry | null {
  const key = buildCacheKey(permission, resourceId)
  const cached = permissionCache.get(key)

  if (!cached) {
    return null
  }

  const isExpired = Date.now() - cached.timestamp > CACHE_DURATION
  if (isExpired) {
    permissionCache.delete(key)
    return null
  }

  return cached
}

function clearCache() {
  permissionCache.clear()
}

function isCached(permission: string, resourceId: string): boolean {
  return getValidCacheEntry(permission, resourceId) !== null
}

function getCachedPermission(permission: string, resourceId: string): boolean | null {
  const cached = getValidCacheEntry(permission, resourceId)
  return cached ? cached.authorized : null
}

function setCachedPermission(permission: string, resourceId: string, authorized: boolean) {
  const key = buildCacheKey(permission, resourceId)
  permissionCache.set(key, {
    timestamp: Date.now(),
    authorized
  })
}

interface UsePermissionsReturn {
  hasPermission: (permission: string, resourceId: string) => Promise<boolean>
  hasPermissions: (permissions: string[], resourceId: string) => Promise<Record<string, boolean>>
  loading: Ref<boolean>
  error: Ref<string | null>
  clearCache: () => void
}

export function usePermissions(): UsePermissionsReturn {
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function hasPermission(permission: string, resourceId: string): Promise<boolean> {
    // Check cache first
    if (isCached(permission, resourceId)) {
      const cached = getCachedPermission(permission, resourceId)
      if (cached !== null) {
        return cached
      }
    }

    loading.value = true
    error.value = null

    try {
      const authorized = await rbacService.hasPermission(permission, resourceId)
      setCachedPermission(permission, resourceId, authorized)
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

  async function hasPermissions(permissions: string[], resourceId: string): Promise<Record<string, boolean>> {
    const result: Record<string, boolean> = {}
    const uncachedPermissions: string[] = []

    // Check cache first
    for (const permission of permissions) {
      const cached = getCachedPermission(permission, resourceId)
      if (cached === null) {
        uncachedPermissions.push(permission)
        continue
      }

      result[permission] = cached
    }

    // If all permissions are cached, return early
    if (uncachedPermissions.length === 0) {
      return result
    }

    loading.value = true
    error.value = null

    try {
      const batchResults = await rbacService.hasPermissions(uncachedPermissions, resourceId)
      
      // Cache results and add to final result
      for (const permission of uncachedPermissions) {
        const authorized = batchResults[permission] || false
        setCachedPermission(permission, resourceId, authorized)
        result[permission] = authorized
      }
      
      return result
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Batch permission check failed'
      error.value = errorMsg
      console.error('Batch permission check error:', errorMsg)
      
      // Default to false for uncached permissions
      for (const permission of uncachedPermissions) {
        result[permission] = false
      }
      
      return result
    } finally {
      loading.value = false
    }
  }

  return {
    hasPermission,
    hasPermissions,
    loading,
    error,
    clearCache
  }
}