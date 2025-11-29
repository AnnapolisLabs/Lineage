import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useRbacStore } from './rbac'
import { rbacService } from '@/services/rbacService'

vi.mock('@/services/rbacService')

describe('useRbacStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    // Clear any cached timeouts that might interfere with tests
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should initialize with default state', () => {
    const store = useRbacStore()
    
    expect(store.permissions).toEqual({})
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  describe('hasCachedPermission', () => {
    it('should return null when no cache exists', () => {
      const store = useRbacStore()
      
      const result = store.hasCachedPermission('read', 'resource-1')
      
      expect(result).toBeNull()
    })

    it('should return cached permission when cache exists and is not expired', () => {
      const store = useRbacStore()
      store.permissionCache = {
        'resource-1': {
          permissions: { read: true, write: false },
          timestamp: Date.now()
        }
      }
      
      const result = store.hasCachedPermission('read', 'resource-1')
      
      expect(result).toBe(true)
    })

    it('should return null when permission is not in cache', () => {
      const store = useRbacStore()
      store.permissionCache = {
        'resource-1': {
          permissions: { read: true },
          timestamp: Date.now()
        }
      }
      
      const result = store.hasCachedPermission('write', 'resource-1')
      
      expect(result).toBeNull()
    })

    it('should return null and clear expired cache', () => {
      const store = useRbacStore()
      store.permissionCache = {
        'resource-1': {
          permissions: { read: true },
          timestamp: Date.now() - (6 * 60 * 1000) // 6 minutes ago (expired)
        }
      }
      
      const result = store.hasCachedPermission('read', 'resource-1')
      
      expect(result).toBeNull()
      expect(store.permissionCache['resource-1']).toBeUndefined()
    })
  })

  describe('setCachedPermission', () => {
    it('should set cached permission', () => {
      const store = useRbacStore()
      
      store.setCachedPermission('read', 'resource-1', true)
      
      expect(store.permissionCache['resource-1']).toBeDefined()
      expect(store.permissionCache['resource-1'].permissions.read).toBe(true)
      expect(store.permissionCache['resource-1'].timestamp).toBeDefined()
    })

    it('should update existing cached permission', () => {
      const store = useRbacStore()
      store.permissionCache = {
        'resource-1': {
          permissions: { read: false },
          timestamp: Date.now() - 1000
        }
      }
      
      store.setCachedPermission('read', 'resource-1', true)
      
      expect(store.permissionCache['resource-1'].permissions.read).toBe(true)
      expect(store.permissionCache['resource-1'].timestamp).toBeGreaterThan(Date.now() - 1000)
    })
  })

  describe('checkPermission', () => {
    it('should return cached permission when available', async () => {
      const store = useRbacStore()
      store.permissionCache = {
        'resource-1': {
          permissions: { read: true },
          timestamp: Date.now()
        }
      }
      
      const result = await store.checkPermission('read', 'resource-1')
      
      expect(result).toBe(true)
      expect(rbacService.checkPermission).not.toHaveBeenCalled()
    })

    it('should check permission and cache result', async () => {
      const store = useRbacStore()
      const mockResult = { authorized: true, timestamp: Date.now() }
      vi.mocked(rbacService.checkPermission).mockResolvedValue(mockResult as any)
      
      const result = await store.checkPermission('read', 'resource-1')
      
      expect(result).toBe(true)
      expect(rbacService.checkPermission).toHaveBeenCalledWith({
        permission: 'read',
        resource_id: 'resource-1'
      })
      expect(store.permissionCache['resource-1'].permissions.read).toBe(true)
      expect(store.permissions['resource-1']).toHaveLength(1)
    })

    it('should handle permission check error', async () => {
      const store = useRbacStore()
      const mockError = { response: { data: { message: 'Permission check failed' } } }
      vi.mocked(rbacService.checkPermission).mockRejectedValue(mockError)
      
      const result = await store.checkPermission('read', 'resource-1')
      
      expect(result).toBe(false)
      expect(store.error).toBe('Permission check failed')
      expect(store.loading).toBe(false)
    })

    it('should update existing permission in permissions map', async () => {
      const store = useRbacStore()
      store.permissions = {
        'resource-1': [
          {
            permission: 'read',
            resource_id: 'resource-1',
            authorized: false,
            timestamp: Date.now() - 1000
          }
        ]
      }
      const mockResult = { authorized: true, timestamp: Date.now() }
      vi.mocked(rbacService.checkPermission).mockResolvedValue(mockResult as any)
      
      await store.checkPermission('read', 'resource-1')
      
      expect(store.permissions['resource-1'][0].authorized).toBe(true)
    })
  })

  describe('batchCheckPermissions', () => {
    it('should return all cached permissions when all are cached', async () => {
      const store = useRbacStore()
      store.permissionCache = {
        'resource-1': {
          permissions: { read: true, write: false, delete: true },
          timestamp: Date.now()
        }
      }
      
      const result = await store.batchCheckPermissions(['read', 'write', 'delete'], 'resource-1')
      
      expect(result).toEqual({ read: true, write: false, delete: true })
      expect(rbacService.batchCheckPermissions).not.toHaveBeenCalled()
    })

    it('should mix cached and uncached permissions', async () => {
      const store = useRbacStore()
      store.permissionCache = {
        'resource-1': {
          permissions: { read: true },
          timestamp: Date.now()
        }
      }
      const mockResults = [
        { permission: 'write', resource_id: 'resource-1', authorized: false, timestamp: Date.now() },
        { permission: 'delete', resource_id: 'resource-1', authorized: true, timestamp: Date.now() }
      ]
      vi.mocked(rbacService.batchCheckPermissions).mockResolvedValue(mockResults as any)
      
      const result = await store.batchCheckPermissions(['read', 'write', 'delete'], 'resource-1')
      
      expect(result).toEqual({ read: true, write: false, delete: true })
      expect(rbacService.batchCheckPermissions).toHaveBeenCalledWith({
        permissions: ['write', 'delete'],
        resource_id: 'resource-1'
      })
    })

    it('should handle batch check error', async () => {
      const store = useRbacStore()
      const mockError = { response: { data: { message: 'Batch permission check failed' } } }
      vi.mocked(rbacService.batchCheckPermissions).mockRejectedValue(mockError)
      
      const result = await store.batchCheckPermissions(['read', 'write'], 'resource-1')
      
      expect(result).toEqual({ read: false, write: false })
      expect(store.error).toBe('Batch permission check failed')
      expect(store.loading).toBe(false)
    })

    it('should cache all batch results', async () => {
      const store = useRbacStore()
      const mockResults = [
        { permission: 'read', resource_id: 'resource-1', authorized: true, timestamp: Date.now() },
        { permission: 'write', resource_id: 'resource-1', authorized: false, timestamp: Date.now() }
      ]
      vi.mocked(rbacService.batchCheckPermissions).mockResolvedValue(mockResults as any)
      
      await store.batchCheckPermissions(['read', 'write'], 'resource-1')
      
      expect(store.permissionCache['resource-1'].permissions.read).toBe(true)
      expect(store.permissionCache['resource-1'].permissions.write).toBe(false)
    })
  })

  describe('getUserPermissions', () => {
    it('should get user permissions successfully', async () => {
      const store = useRbacStore()
      const mockPermissions = [
        { permission: 'read', resource_id: 'resource-1', authorized: true, timestamp: Date.now() },
        { permission: 'write', resource_id: 'resource-1', authorized: false, timestamp: Date.now() }
      ]
      ;(rbacService.getUserPermissions as any).mockResolvedValue(mockPermissions)
      
      const result = await store.getUserPermissions('user-1', 'resource-1')
      
      expect(result).toEqual(mockPermissions)
      expect(store.permissions['resource-1']).toEqual(mockPermissions)
      expect(store.permissionCache['resource-1'].permissions.read).toBe(true)
      expect(store.permissionCache['resource-1'].permissions.write).toBe(false)
      expect(store.loading).toBe(false)
    })

    it('should handle get permissions error', async () => {
      const store = useRbacStore()
      const mockError = { response: { data: { message: 'Failed to get user permissions' } } }
      ;(rbacService.getUserPermissions as any).mockRejectedValue(mockError)
      
      const result = await store.getUserPermissions('user-1', 'resource-1')
      
      expect(result).toEqual([])
      expect(store.error).toBe('Failed to get user permissions')
      expect(store.loading).toBe(false)
    })
  })

  describe('checkRoleHierarchy', () => {
    it('should check role hierarchy successfully', async () => {
      const store = useRbacStore()
      ;(rbacService.checkRoleHierarchy as any).mockResolvedValue(true)
      
      const result = await store.checkRoleHierarchy('ADMIN', 'user-1')
      
      expect(result).toBe(true)
      expect(rbacService.checkRoleHierarchy).toHaveBeenCalledWith('ADMIN', 'user-1')
      expect(store.loading).toBe(false)
    })

    it('should handle role hierarchy check error', async () => {
      const store = useRbacStore()
      const mockError = { response: { data: { message: 'Role hierarchy check failed' } } }
      ;(rbacService.checkRoleHierarchy as any).mockRejectedValue(mockError)
      
      const result = await store.checkRoleHierarchy('ADMIN', 'user-1')
      
      expect(result).toBe(false)
      expect(store.error).toBe('Role hierarchy check failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('clearCache', () => {
    it('should clear all cached permissions', () => {
      const store = useRbacStore()
      store.permissionCache = {
        'resource-1': { permissions: { read: true }, timestamp: Date.now() },
        'resource-2': { permissions: { write: false }, timestamp: Date.now() }
      }
      
      store.clearCache()
      
      expect(store.permissionCache).toEqual({})
    })
  })

  describe('clearResourceCache', () => {
    it('should clear cache for specific resource', () => {
      const store = useRbacStore()
      store.permissionCache = {
        'resource-1': { permissions: { read: true }, timestamp: Date.now() },
        'resource-2': { permissions: { write: false }, timestamp: Date.now() }
      }
      
      store.clearResourceCache('resource-1')
      
      expect(store.permissionCache['resource-1']).toBeUndefined()
      expect(store.permissionCache['resource-2']).toBeDefined()
    })
  })

  describe('clearError', () => {
    it('should clear error', () => {
      const store = useRbacStore()
      store.error = 'Test error'
      
      store.clearError()
      
      expect(store.error).toBeNull()
    })
  })

  describe('getPermissionsForResource', () => {
    it('should return permissions for resource', () => {
      const store = useRbacStore()
      store.permissions = {
        'resource-1': [
          { permission: 'read', resource_id: 'resource-1', authorized: true, timestamp: Date.now() }
        ]
      }
      
      const result = store.getPermissionsForResource('resource-1')
      
      expect(result).toHaveLength(1)
      expect(result[0].permission).toBe('read')
    })

    it('should return empty array when no permissions for resource', () => {
      const store = useRbacStore()
      store.permissions = {}
      
      const result = store.getPermissionsForResource('resource-1')
      
      expect(result).toEqual([])
    })
  })

  describe('hasAllPermissions', () => {
    it('should return true when user has all permissions', async () => {
      const store = useRbacStore()
      ;(rbacService.batchCheckPermissions as any).mockResolvedValue([
        { permission: 'read', resource_id: 'resource-1', authorized: true, timestamp: Date.now() },
        { permission: 'write', resource_id: 'resource-1', authorized: true, timestamp: Date.now() }
      ])
      
      const result = await store.hasAllPermissions(['read', 'write'], 'resource-1')
      
      expect(result).toBe(true)
    })

    it('should return false when user missing any permission', async () => {
      const store = useRbacStore()
      ;(rbacService.batchCheckPermissions as any).mockResolvedValue([
        { permission: 'read', resource_id: 'resource-1', authorized: true, timestamp: Date.now() },
        { permission: 'write', resource_id: 'resource-1', authorized: false, timestamp: Date.now() }
      ])
      
      const result = await store.hasAllPermissions(['read', 'write'], 'resource-1')
      
      expect(result).toBe(false)
    })
  })

  describe('hasAnyPermission', () => {
    it('should return true when user has at least one permission', async () => {
      const store = useRbacStore()
      ;(rbacService.batchCheckPermissions as any).mockResolvedValue([
        { permission: 'read', resource_id: 'resource-1', authorized: true, timestamp: Date.now() },
        { permission: 'write', resource_id: 'resource-1', authorized: false, timestamp: Date.now() }
      ])
      
      const result = await store.hasAnyPermission(['read', 'write'], 'resource-1')
      
      expect(result).toBe(true)
    })

    it('should return false when user has no permissions', async () => {
      const store = useRbacStore()
      ;(rbacService.batchCheckPermissions as any).mockResolvedValue([
        { permission: 'read', resource_id: 'resource-1', authorized: false, timestamp: Date.now() },
        { permission: 'write', resource_id: 'resource-1', authorized: false, timestamp: Date.now() }
      ])
      
      const result = await store.hasAnyPermission(['read', 'write'], 'resource-1')
      
      expect(result).toBe(false)
    })
  })
})