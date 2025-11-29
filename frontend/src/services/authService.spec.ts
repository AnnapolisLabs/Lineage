import { describe, it, expect, beforeEach, vi } from 'vitest'
import { authService } from './authService'
import api from './api'

vi.mock('./api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('authService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('login', () => {
    it('should login successfully', async () => {
      const mockResponse = {
        token: 'jwt-token',
        email: 'test@example.com',
        name: 'Test User',
        role: 'USER'
      }
      vi.mocked(api.post).mockResolvedValue({ data: mockResponse } as any)

      const credentials = { email: 'test@example.com', password: 'password' }
      const result = await authService.login(credentials)

      expect(api.post).toHaveBeenCalledWith('/auth/login', credentials)
      expect(result).toEqual(mockResponse)
    })
  })

  describe('getCurrentUser', () => {
    it('should get current user successfully', async () => {
      const mockUser = {
        id: 'user-1',
        email: 'test@example.com',
        name: 'Test User',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2025-01-01T00:00:00Z',
        updatedAt: '2025-01-01T00:00:00Z',
        preferences: {}
      }

      // /auth/me returns a wrapper object with the real user nested
      // under `user`, which authService.getCurrentUser unwraps.
      vi.mocked(api.get).mockResolvedValue({ data: { user: mockUser } } as any)

      const result = await authService.getCurrentUser()

      expect(api.get).toHaveBeenCalledWith('/auth/me')
      expect(result).toEqual(mockUser)
    })
  })

  describe('logout', () => {
    it('should remove auth token from localStorage', () => {
      localStorage.setItem('auth_token', 'test-token')

      authService.logout()

      expect(localStorage.getItem('auth_token')).toBeNull()
    })
  })
})
