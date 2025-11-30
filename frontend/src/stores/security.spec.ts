import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSecurityStore } from './security'
import { securityService } from '@/services/securityService'

vi.mock('@/services/securityService')

describe('useSecurityStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useSecurityStore()
    
    expect(store.mfaSetup).toBeNull()
    expect(store.sessions).toEqual([])
    expect(store.securityEvents).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  describe('isMfaEnabled', () => {
    it('should return false when mfaSetup is null', () => {
      const store = useSecurityStore()
      expect(store.isMfaEnabled).toBe(false)
    })

    it('should return true when mfaSetup is enabled', () => {
      const store = useSecurityStore()
      store.mfaSetup = { enabled: true, setupComplete: true, message: 'MFA enabled' }
      expect(store.isMfaEnabled).toBe(true)
    })

    it('should return false when mfaSetup is not enabled', () => {
      const store = useSecurityStore()
      store.mfaSetup = { enabled: false, setupComplete: false, message: 'MFA disabled' }
      expect(store.isMfaEnabled).toBe(false)
    })
  })

  describe('isMfaSetupComplete', () => {
    it('should return false when mfaSetup is null', () => {
      const store = useSecurityStore()
      expect(store.isMfaSetupComplete).toBe(false)
    })

    it('should return true when mfaSetup is complete', () => {
      const store = useSecurityStore()
      store.mfaSetup = { enabled: true, setupComplete: true, message: 'Setup complete' }
      expect(store.isMfaSetupComplete).toBe(true)
    })

    it('should return false when mfaSetup is not complete', () => {
      const store = useSecurityStore()
      store.mfaSetup = { enabled: true, setupComplete: false, message: 'Setup incomplete' }
      expect(store.isMfaSetupComplete).toBe(false)
    })
  })

  describe('currentSession', () => {
    it('should return current session when found', () => {
      const store = useSecurityStore()
      store.sessions = [
        { id: '1', device: 'Chrome', ipAddress: '192.168.1.1', createdAt: '2023-01-01', lastActivity: '2023-01-01', current: false },
        { id: '2', device: 'Firefox', ipAddress: '192.168.1.2', createdAt: '2023-01-02', lastActivity: '2023-01-02', current: true }
      ]
      
      expect(store.currentSession).toEqual(store.sessions[1])
    })

    it('should return null when no current session', () => {
      const store = useSecurityStore()
      store.sessions = [
        { id: '1', device: 'Chrome', ipAddress: '192.168.1.1', createdAt: '2023-01-01', lastActivity: '2023-01-01', current: false },
        { id: '2', device: 'Firefox', ipAddress: '192.168.1.2', createdAt: '2023-01-02', lastActivity: '2023-01-02', current: false }
      ]
      
      expect(store.currentSession).toBeNull()
    })

    it('should return null when no sessions', () => {
      const store = useSecurityStore()
      expect(store.currentSession).toBeNull()
    })
  })

  describe('fetchMfaSetup', () => {
    it('should fetch MFA setup successfully', async () => {
      const store = useSecurityStore()
      const mockMfaSetup = { enabled: true, setupComplete: true, message: 'MFA is enabled' }
      vi.mocked(securityService.getMfaSetup).mockResolvedValue(mockMfaSetup as any)

      await store.fetchMfaSetup()

      expect(store.mfaSetup).toEqual(mockMfaSetup)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should handle fetch MFA setup error', async () => {
      const store = useSecurityStore()
      const mockError = { response: { data: { message: 'Failed to load MFA setup' } } }
      vi.mocked(securityService.getMfaSetup).mockRejectedValue(mockError)

      await store.fetchMfaSetup()

      expect(store.mfaSetup).toBeNull()
      expect(store.error).toBe('Failed to load MFA setup')
      expect(store.loading).toBe(false)
    })
  })

  describe('enableMfa', () => {
    it('should enable MFA successfully', async () => {
      const store = useSecurityStore()
      const mockResult = { success: true, message: 'MFA enabled successfully' }
      vi.mocked(securityService.enableMfa).mockResolvedValue(mockResult as any)

      const result = await store.enableMfa('123456')

      expect(result).toEqual(mockResult)
      expect(store.mfaSetup).toEqual({
        enabled: true,
        setupComplete: true,
        message: 'MFA enabled successfully'
      })
      expect(store.loading).toBe(false)
    })

    it('should not update MFA setup when enable fails', async () => {
      const store = useSecurityStore()
      store.mfaSetup = { enabled: false, setupComplete: false, message: 'MFA disabled' }
      const mockResult = { success: false, message: 'Invalid code' }
      vi.mocked(securityService.enableMfa).mockResolvedValue(mockResult as any)

      const result = await store.enableMfa('123456')

      expect(result).toEqual(mockResult)
      expect(store.mfaSetup).toEqual({ enabled: false, setupComplete: false, message: 'MFA disabled' })
      expect(store.loading).toBe(false)
    })

    it('should handle enable MFA error', async () => {
      const store = useSecurityStore()
      const mockError = { response: { data: { message: 'Failed to enable MFA' } } }
      vi.mocked(securityService.enableMfa).mockRejectedValue(mockError)

      await expect(store.enableMfa('123456')).rejects.toThrow()
      expect(store.error).toBe('Failed to enable MFA')
      expect(store.loading).toBe(false)
    })
  })

  describe('disableMfa', () => {
    it('should disable MFA successfully', async () => {
      const store = useSecurityStore()
      store.mfaSetup = { enabled: true, setupComplete: true, message: 'MFA enabled' }
      const mockResult = { success: true, message: 'MFA disabled successfully' }
      vi.mocked(securityService.disableMfa).mockResolvedValue(mockResult as any)

      const result = await store.disableMfa('123456')

      expect(result).toEqual(mockResult)
      expect(store.mfaSetup).toEqual({
        enabled: false,
        setupComplete: false,
        message: 'MFA disabled successfully'
      })
      expect(store.loading).toBe(false)
    })

    it('should handle disable MFA error', async () => {
      const store = useSecurityStore()
      const mockError = { response: { data: { message: 'Failed to disable MFA' } } }
      vi.mocked(securityService.disableMfa).mockRejectedValue(mockError)

      await expect(store.disableMfa('123456')).rejects.toThrow()
      expect(store.error).toBe('Failed to disable MFA')
      expect(store.loading).toBe(false)
    })
  })

  describe('validateMfaCode', () => {
    it('should validate MFA code successfully', async () => {
      const store = useSecurityStore()
      const mockResult = { valid: true, message: 'Code is valid' }
      vi.mocked(securityService.validateMfaCode).mockResolvedValue(mockResult as any)

      const result = await store.validateMfaCode('123456')

      expect(result).toEqual(mockResult)
      expect(store.loading).toBe(false)
    })

    it('should handle MFA validation error', async () => {
      const store = useSecurityStore()
      const mockError = { response: { data: { message: 'Failed to validate MFA code' } } }
      vi.mocked(securityService.validateMfaCode).mockRejectedValue(mockError)

      await expect(store.validateMfaCode('123456')).rejects.toThrow()
      expect(store.error).toBe('Failed to validate MFA code')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchUserSessions', () => {
    it('should fetch user sessions successfully', async () => {
      const store = useSecurityStore()
      const mockSessions = [
        { id: '1', device: 'Chrome', ipAddress: '192.168.1.1', createdAt: '2023-01-01', lastActivity: '2023-01-01', current: true },
        { id: '2', device: 'Firefox', ipAddress: '192.168.1.2', createdAt: '2023-01-02', lastActivity: '2023-01-02', current: false }
      ]
      const mockResult = { sessions: mockSessions }
      vi.mocked(securityService.getUserSessions).mockResolvedValue(mockResult as any)

      await store.fetchUserSessions()

      expect(store.sessions).toEqual(mockSessions)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should handle fetch sessions error', async () => {
      const store = useSecurityStore()
      const mockError = { response: { data: { message: 'Failed to load sessions' } } }
      vi.mocked(securityService.getUserSessions).mockRejectedValue(mockError)

      await store.fetchUserSessions()

      expect(store.sessions).toEqual([])
      expect(store.error).toBe('Failed to load sessions')
      expect(store.loading).toBe(false)
    })
  })

  describe('revokeSession', () => {
    it('should revoke session successfully', async () => {
      const store = useSecurityStore()
      store.sessions = [
        { id: '1', device: 'Chrome', ipAddress: '192.168.1.1', createdAt: '2023-01-01', lastActivity: '2023-01-01', current: true },
        { id: '2', device: 'Firefox', ipAddress: '192.168.1.2', createdAt: '2023-01-02', lastActivity: '2023-01-02', current: false }
      ]
      const mockResult = { success: true, message: 'Session revoked' }
      vi.mocked(securityService.revokeSession).mockResolvedValue(mockResult as any)

      const result = await store.revokeSession('2')

      expect(result).toEqual(mockResult)
      expect(store.sessions).toHaveLength(1)
      expect(store.sessions[0].id).toBe('1')
      expect(store.loading).toBe(false)
    })

    it('should handle revoke session error', async () => {
      const store = useSecurityStore()
      const mockError = { response: { data: { message: 'Failed to revoke session' } } }
      vi.mocked(securityService.revokeSession).mockRejectedValue(mockError)

      await expect(store.revokeSession('1')).rejects.toThrow()
      expect(store.error).toBe('Failed to revoke session')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchSecurityEvents', () => {
    it('should fetch security events successfully for first page', async () => {
      const store = useSecurityStore()
      const mockEvents = [
        { id: '1', type: 'LOGIN', description: 'User logged in', timestamp: '2023-01-01T10:00:00Z', severity: 'INFO' },
        { id: '2', type: 'LOGOUT', description: 'User logged out', timestamp: '2023-01-01T11:00:00Z', severity: 'INFO' }
      ]
      const mockResult = { events: mockEvents }
      vi.mocked(securityService.getSecurityEvents).mockResolvedValue(mockResult as any)

      const result = await store.fetchSecurityEvents(0, 20)

      expect(result).toEqual(mockResult)
      expect(store.securityEvents).toEqual(mockEvents)
      expect(store.loading).toBe(false)
    })

    it('should append security events for subsequent pages', async () => {
      const store = useSecurityStore()
      store.securityEvents = [
        { id: '1', type: 'LOGIN', description: 'User logged in', timestamp: '2023-01-01T10:00:00Z', severity: 'INFO' }
      ]
      const mockEvents = [
        { id: '2', type: 'LOGOUT', description: 'User logged out', timestamp: '2023-01-01T11:00:00Z', severity: 'INFO' }
      ]
      const mockResult = { events: mockEvents }
      vi.mocked(securityService.getSecurityEvents).mockResolvedValue(mockResult as any)

      await store.fetchSecurityEvents(1, 20)

      expect(store.securityEvents).toHaveLength(2)
      expect(store.securityEvents[1]).toEqual(mockEvents[0])
    })

    it('should handle fetch security events error', async () => {
      const store = useSecurityStore()
      const mockError = { response: { data: { message: 'Failed to load security events' } } }
      vi.mocked(securityService.getSecurityEvents).mockRejectedValue(mockError)

      await store.fetchSecurityEvents()

      expect(store.error).toBe('Failed to load security events')
      expect(store.loading).toBe(false)
    })
  })

  describe('clearSecurityData', () => {
    it('should clear all security data', () => {
      const store = useSecurityStore()
      store.mfaSetup = { enabled: true, setupComplete: true, message: 'MFA enabled' }
      store.sessions = [{ id: '1', device: 'Chrome', ipAddress: '192.168.1.1', createdAt: '2023-01-01', lastActivity: '2023-01-01', current: true }]
      store.securityEvents = [{ id: '1', type: 'LOGIN', description: 'User logged in', timestamp: '2023-01-01T10:00:00Z', severity: 'INFO' }]
      store.error = 'Test error'

      store.clearSecurityData()

      expect(store.mfaSetup).toBeNull()
      expect(store.sessions).toEqual([])
      expect(store.securityEvents).toEqual([])
      expect(store.error).toBeNull()
    })
  })
})