import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from './auth'
import { authService } from '@/services/authService'

vi.mock('@/services/authService')

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should initialize with null user and token from localStorage', () => {
    localStorage.setItem('auth_token', 'test-token')
    setActivePinia(createPinia())
    const store = useAuthStore()

    expect(store.token).toBe('test-token')
    expect(store.user).toBeNull()
  })

  it('should compute isAuthenticated correctly', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)

    store.token = 'test-token'
    expect(store.isAuthenticated).toBe(true)
  })

  it('should compute isAdmin correctly', () => {
    const store = useAuthStore()
    expect(store.isAdmin).toBe(false)

    // Backend uses ADMINISTRATOR for the highest global role; the auth
    // store mirrors that, so the test user must use ADMINISTRATOR too.
    store.user = { id: '1', email: 'test@test.com', name: 'Test', globalRole: 'ADMINISTRATOR', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
    expect(store.isAdmin).toBe(true)
  })

  it('should compute isEditor correctly', () => {
    const store = useAuthStore()
    expect(store.isEditor).toBe(false)

    store.user = { id: '1', email: 'test@test.com', name: 'Test', globalRole: 'DEVELOPER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
    expect(store.isEditor).toBe(true)

    // ADMINISTRATOR should also be treated as editor-level for
    // canCreateTeam and other global management checks.
    store.user = { id: '1', email: 'test@test.com', name: 'Test', globalRole: 'ADMINISTRATOR', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
    expect(store.isEditor).toBe(true)
  })

  describe('login', () => {
    it('should login successfully', async () => {
      const store = useAuthStore()
      const mockResponse = {
        token: 'jwt-token',
        email: 'test@test.com',
        refreshToken: 'refresh-token',
        user: {
          id: 'user-123',
          email: 'test@test.com',
          name: 'Test User',
          globalRole: 'USER',
          status: 'ACTIVE',
          emailVerified: true,
          createdAt: '2023-01-01',
          updatedAt: '2023-01-01',
          preferences: {}
        },
        message: 'Login successful',
        success: true,
        userId: 'user-123',
        expiresAt: null,
        mfaRequired: false
      }
      vi.mocked(authService.login).mockResolvedValue(mockResponse)

      const result = await store.login({ email: 'test@test.com', password: 'password' })

      expect(result).toBe(true)
      expect(store.token).toBe('jwt-token')
      expect(store.user).toEqual({
        id: 'user-123',
        email: 'test@test.com',
        name: 'Test User',
        firstName: undefined,
        lastName: undefined,
        globalRole: 'USER',
        status: 'ACTIVE',
        phoneNumber: undefined,
        avatarUrl: undefined,
        bio: undefined,
        preferences: {},
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        lastLoginAt: undefined
      })
      expect(localStorage.getItem('auth_token')).toBe('jwt-token')
      expect(localStorage.getItem('user_id')).toBe('user-123')
    })

    it('should handle login error', async () => {
      const store = useAuthStore()
      const mockError = {
        response: { data: { message: 'Invalid credentials' } }
      }
      vi.mocked(authService.login).mockRejectedValue(mockError)

      const result = await store.login({ email: 'test@test.com', password: 'wrong' })

      expect(result).toBe(false)
      expect(store.error).toBe('Invalid credentials')
      expect(store.token).toBeNull()
    })
  })

  describe('fetchCurrentUser', () => {
    it('should fetch current user when token exists', async () => {
      const store = useAuthStore()
      store.token = 'test-token'
      const mockUser = { id: 'user-1', email: 'test@test.com', name: 'Test User', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
      vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser)

      await store.fetchCurrentUser()

      expect(store.user).toEqual(mockUser)
      expect(localStorage.getItem('user_id')).toBe('user-1')
    })

    it('should not fetch user when no token', async () => {
      const store = useAuthStore()

      await store.fetchCurrentUser()

      expect(authService.getCurrentUser).not.toHaveBeenCalled()
    })

    it('should logout on fetch error', async () => {
      const store = useAuthStore()
      store.token = 'test-token'
      vi.mocked(authService.getCurrentUser).mockRejectedValue(new Error('Unauthorized'))

      await store.fetchCurrentUser()

      expect(store.token).toBeNull()
      expect(store.user).toBeNull()
    })
  })

  describe('validateToken', () => {
    it('should validate token and set user', async () => {
      const store = useAuthStore()
      store.token = 'test-token'
      const mockUser = { id: 'user-1', email: 'test@test.com', name: 'Test User', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
      vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser)

      await store.validateToken()

      expect(store.user).toEqual(mockUser)
      expect(localStorage.getItem('user_id')).toBe('user-1')
    })

    it('should logout on validation error', async () => {
      const store = useAuthStore()
      store.token = 'invalid-token'
      vi.mocked(authService.getCurrentUser).mockRejectedValue(new Error('Unauthorized'))

      await store.validateToken()

      expect(store.token).toBeNull()
      expect(store.user).toBeNull()
      expect(localStorage.getItem('auth_token')).toBeNull()
    })

    it('should not validate when no token', async () => {
      const store = useAuthStore()

      await store.validateToken()

      expect(authService.getCurrentUser).not.toHaveBeenCalled()
    })
  })

  describe('logout', () => {
    it('should clear user and token', () => {
      const store = useAuthStore()
      store.token = 'test-token'
      store.user = { id: '1', email: 'test@test.com', name: 'Test', globalRole: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2023-01-01', updatedAt: '2023-01-01', preferences: {} }
      localStorage.setItem('auth_token', 'test-token')
      localStorage.setItem('user_id', 'test@test.com')

      store.logout()

      expect(store.token).toBeNull()
      expect(store.user).toBeNull()
      expect(localStorage.getItem('auth_token')).toBeNull()
      expect(localStorage.getItem('user_id')).toBeNull()
      expect(authService.logout).toHaveBeenCalled()
    })
  })
})
