import { ref, computed } from 'vue'
import { rbacService } from '@/services/rbacService'
import { useAuthStore } from '@/stores/auth'

// Permission cache to avoid repeated API calls
const permissionCache = new Map<string, { timestamp: number; authorized: boolean }>()
const CACHE_DURATION = 5 * 60 * 1000 // 5 minutes

interface UsePermissionsReturn {
  hasPermission: (permission: string, resourceId: string) => Promise<boolean>
  hasPermissions: (permissions: string[], resourceId: string) => Promise<Record<string, boolean>>
  loading: ref<boolean>
  error: ref<string | null>
  clearCache: () => void
}

export function usePermissions(): UsePermissionsReturn {
  const authStore = useAuthStore()
  const loading = ref(false)
  const error = ref<string | null>(null)

  function clearCache() {
    permissionCache.clear()
  }

  function isCached(permission: string, resourceId: string): boolean {
    const key = `${permission}:${resourceId}`
    const cached = permissionCache.get(key)
    
    if (!cached) return false
    
    const isExpired = Date.now() - cached.timestamp > CACHE_DURATION
    if (isExpired) {
      permissionCache.delete(key)
      return false
    }
    
    return true
  }

  function getCachedPermission(permission: string, resourceId: string): boolean | null {
    const key = `${permission}:${resourceId}`
    const cached = permissionCache.get(key)
    
    if (!cached) return null
    
    const isExpired = Date.now() - cached.timestamp > CACHE_DURATION
    if (isExpired) {
      permissionCache.delete(key)
      return null
    }
    
    return cached.authorized
  }

  function setCachedPermission(permission: string, resourceId: string, authorized: boolean) {
    const key = `${permission}:${resourceId}`
    permissionCache.set(key, {
      timestamp: Date.now(),
      authorized
    })
  }

  async function hasPermission(permission: string, resourceId: string): Promise<boolean> {
    // Check cache first
    const cached = getCachedPermission(permission, resourceId)
    if (cached !== null) {
      return cached
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
      if (cached !== null) {
        result[permission] = cached
      } else {
        uncachedPermissions.push(permission)
      }
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