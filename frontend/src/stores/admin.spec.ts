import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAdminStore } from './admin'
import { adminService } from '@/services/adminService'

vi.mock('@/services/adminService')

describe('useAdminStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useAdminStore()
    
    expect(store.users).toEqual([])
    expect(store.currentPage).toBe(0)
    expect(store.totalPages).toBe(0)
    expect(store.totalUsers).toBe(0)
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.statistics).toBeNull()
    expect(store.auditLogs).toEqual([])
    expect(store.auditCurrentPage).toBe(0)
    expect(store.auditTotalPages).toBe(0)
    expect(store.auditTotalLogs).toBe(0)
    expect(store.searchQuery).toBe('')
    expect(store.selectedUser).toBeNull()
  })

  it('should compute hasUsers correctly', () => {
    const store = useAdminStore()
    expect(store.hasUsers).toBe(false)
    
    store.users = [{ id: '1', email: 'test@test.com', name: 'Test', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} } as any]
    expect(store.hasUsers).toBe(true)
  })

  it('should compute isLoading correctly', () => {
    const store = useAdminStore()
    expect(store.isLoading).toBe(false)
    
    store.loading = true
    expect(store.isLoading).toBe(true)
  })

  it('should compute hasError correctly', () => {
    const store = useAdminStore()
    expect(store.hasError).toBe(false)
    
    store.error = 'Test error'
    expect(store.hasError).toBe(true)
  })

  describe('fetchUsers', () => {
    it('should fetch users successfully', async () => {
      const store = useAdminStore()
      const mockUsers = [
        { id: '1', email: 'test1@test.com', name: 'Test 1', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} },
        { id: '2', email: 'test2@test.com', name: 'Test 2', globalRole: 'ADMINISTRATOR', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-02', updatedAt: '2023-01-02', preferences: {} }
      ]
      const mockResponse = {
        users: mockUsers,
        page: 0,
        totalPages: 2,
        total: 2
      }
      vi.mocked(adminService.getUsers).mockResolvedValue(mockResponse as any)

      await store.fetchUsers(0, 20, '')

      expect(store.users).toEqual(mockUsers)
      expect(store.currentPage).toBe(0)
      expect(store.totalPages).toBe(2)
      expect(store.totalUsers).toBe(2)
      expect(store.searchQuery).toBe('')
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should handle fetch error', async () => {
      const store = useAdminStore()
      const mockError = { response: { data: { message: 'Failed to load users' } } }
      vi.mocked(adminService.getUsers).mockRejectedValue(mockError)

      await store.fetchUsers()

      expect(store.error).toBe('Failed to load users')
      expect(store.loading).toBe(false)
    })

    it('should update search query', async () => {
      const store = useAdminStore()
      const mockResponse = { users: [], page: 0, totalPages: 0, total: 0 }
      vi.mocked(adminService.getUsers).mockResolvedValue(mockResponse as any)

      await store.fetchUsers(0, 20, 'test search')

      expect(store.searchQuery).toBe('test search')
    })
  })

  describe('fetchUserById', () => {
    it('should fetch user by id successfully', async () => {
      const store = useAdminStore()
      const mockUser = { id: '1', email: 'test@test.com', name: 'Test', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
      vi.mocked(adminService.getUserById).mockResolvedValue(mockUser as any)

      const result = await store.fetchUserById('1')

      expect(result).toEqual(mockUser)
      expect(store.selectedUser).toEqual(mockUser)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should handle fetch error', async () => {
      const store = useAdminStore()
      const mockError = { response: { data: { message: 'User not found' } } }
      vi.mocked(adminService.getUserById).mockRejectedValue(mockError)

      await expect(store.fetchUserById('1')).rejects.toThrow()
      expect(store.error).toBe('User not found')
      expect(store.loading).toBe(false)
    })
  })

  describe('updateUser', () => {
    it('should update user successfully', async () => {
      const store = useAdminStore()
      store.users = [{ id: '1', email: 'old@test.com', name: 'Old', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} } as any]
      const mockUser = { id: '1', email: 'new@test.com', name: 'New', globalRole: 'ADMINISTRATOR', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
      vi.mocked(adminService.updateUser).mockResolvedValue(mockUser as any)

      const result = await store.updateUser('1', { name: 'New' })

      expect(result).toEqual(mockUser)
      expect(store.users[0]).toEqual(mockUser)
      expect(store.loading).toBe(false)
    })

    it('should update selectedUser if it matches', async () => {
      const store = useAdminStore()
      const mockUser = { id: '1', email: 'test@test.com', name: 'Updated', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
      store.selectedUser = { id: '1', email: 'test@test.com', name: 'Old', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} } as any
      vi.mocked(adminService.updateUser).mockResolvedValue(mockUser as any)

      await store.updateUser('1', { name: 'Updated' })

      expect(store.selectedUser).toEqual(mockUser)
    })

    it('should handle update error', async () => {
      const store = useAdminStore()
      const mockError = { response: { data: { message: 'Update failed' } } }
      vi.mocked(adminService.updateUser).mockRejectedValue(mockError)

      await expect(store.updateUser('1', { name: 'New' })).rejects.toThrow()
      expect(store.error).toBe('Update failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('deleteUser', () => {
    it('should delete user successfully', async () => {
      const store = useAdminStore()
      store.users = [
        { id: '1', email: 'test1@test.com', name: 'Test 1', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} } as any,
        { id: '2', email: 'test2@test.com', name: 'Test 2', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-02', updatedAt: '2023-01-02', preferences: {} } as any
      ]
      store.totalUsers = 2
      vi.mocked(adminService.deleteUser).mockResolvedValue()

      await store.deleteUser('1')

      expect(store.users).toHaveLength(1)
      expect(store.users[0]!.id).toBe('2')
      expect(store.totalUsers).toBe(1)
    })

    it('should clear selectedUser if it matches deleted user', async () => {
      const store = useAdminStore()
      store.users = [{ id: '1', email: 'test@test.com', name: 'Test', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} } as any]
      store.selectedUser = { id: '1', email: 'test@test.com', name: 'Test', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} } as any
      vi.mocked(adminService.deleteUser).mockResolvedValue()

      await store.deleteUser('1')

      expect(store.selectedUser).toBeNull()
    })

    it('should handle delete error', async () => {
      const store = useAdminStore()
      const mockError = { response: { data: { message: 'Delete failed' } } }
      vi.mocked(adminService.deleteUser).mockRejectedValue(mockError)

      await expect(store.deleteUser('1')).rejects.toThrow()
      expect(store.error).toBe('Delete failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('lockUserAccount', () => {
    it('should lock user account successfully', async () => {
      const store = useAdminStore()
      store.users = [{ id: '1', email: 'test@test.com', name: 'Test', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} } as any]
      store.selectedUser = { id: '1', email: 'test@test.com', name: 'Test', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} } as any
      vi.mocked(adminService.lockUserAccount).mockResolvedValue()

      await store.lockUserAccount('1')

      expect(store.users[0]!.status).toBe('SUSPENDED')
      expect(store.selectedUser!.status).toBe('SUSPENDED')
      expect(store.loading).toBe(false)
    })

    it('should handle lock error', async () => {
      const store = useAdminStore()
      const mockError = { response: { data: { message: 'Lock failed' } } }
      vi.mocked(adminService.lockUserAccount).mockRejectedValue(mockError)

      await expect(store.lockUserAccount('1')).rejects.toThrow()
      expect(store.error).toBe('Lock failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('createUser', () => {
    it('should create user successfully', async () => {
      const store = useAdminStore()
      const mockUser = { id: '1', email: 'new@test.com', name: 'New User', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
      vi.mocked(adminService.createUser).mockResolvedValue(mockUser as any)

      const result = await store.createUser({ email: 'new@test.com', name: 'New User' })

      expect(result).toEqual(mockUser)
      expect(store.users[0]).toEqual(mockUser)
      expect(store.totalUsers).toBe(1)
      expect(store.loading).toBe(false)
    })

    it('should handle create error', async () => {
      const store = useAdminStore()
      const mockError = { response: { data: { message: 'Create failed' } } }
      vi.mocked(adminService.createUser).mockRejectedValue(mockError)

      await expect(store.createUser({ email: 'new@test.com', name: 'New User' })).rejects.toThrow()
      expect(store.error).toBe('Create failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchSystemStatistics', () => {
    it('should fetch system statistics successfully', async () => {
      const store = useAdminStore()
      const mockStats = {
        totalUsers: 100,
        activeUsers: 80,
        totalProjects: 25,
        totalTeams: 15
      }
      vi.mocked(adminService.getSystemStatistics).mockResolvedValue(mockStats as any)

      await store.fetchSystemStatistics()

      expect(store.statistics).toEqual(mockStats)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should handle statistics fetch error', async () => {
      const store = useAdminStore()
      const mockError = { response: { data: { message: 'Failed to load statistics' } } }
      vi.mocked(adminService.getSystemStatistics).mockRejectedValue(mockError)

      await store.fetchSystemStatistics()

      expect(store.statistics).toBeNull()
      expect(store.error).toBe('Failed to load statistics')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchAuditLogs', () => {
    it('should fetch audit logs successfully', async () => {
      const store = useAdminStore()
      const mockLogs = [
        { id: '1', userId: 'user-1', action: 'LOGIN', timestamp: '2023-01-01T10:00:00Z', details: {} },
        { id: '2', userId: 'user-2', action: 'LOGOUT', timestamp: '2023-01-01T11:00:00Z', details: {} }
      ]
      const mockResponse = {
        logs: mockLogs,
        page: 0,
        totalPages: 1,
        total: 2
      }
      vi.mocked(adminService.getAuditLogs).mockResolvedValue(mockResponse as any)

      await store.fetchAuditLogs(0, 20)

      expect(store.auditLogs).toEqual(mockLogs)
      expect(store.auditCurrentPage).toBe(0)
      expect(store.auditTotalPages).toBe(1)
      expect(store.auditTotalLogs).toBe(2)
      expect(store.loading).toBe(false)
    })

    it('should handle audit logs fetch error', async () => {
      const store = useAdminStore()
      const mockError = { response: { data: { message: 'Failed to load audit logs' } } }
      vi.mocked(adminService.getAuditLogs).mockRejectedValue(mockError)

      await store.fetchAuditLogs()

      expect(store.error).toBe('Failed to load audit logs')
      expect(store.loading).toBe(false)
    })
  })

  describe('clearSelectedUser', () => {
    it('should clear selected user', () => {
      const store = useAdminStore()
      store.selectedUser = { id: '1', email: 'test@test.com', name: 'Test', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} } as any

      store.clearSelectedUser()

      expect(store.selectedUser).toBeNull()
    })
  })

  describe('clearError', () => {
    it('should clear error', () => {
      const store = useAdminStore()
      store.error = 'Test error'

      store.clearError()

      expect(store.error).toBeNull()
    })
  })
})