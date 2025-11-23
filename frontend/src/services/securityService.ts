import api from './api'

export interface MfaSetupResponse {
  enabled: boolean
  setupComplete: boolean
  secretKey?: string
  qrCodeUrl?: string
  backupCodes?: string[]
  message?: string
}

export interface MfaValidationRequest {
  mfaCode: string
}

export interface SessionInfo {
  id: string
  deviceInfo: string
  ipAddress: string
  userAgent: string
  lastActivity: string
  createdAt: string
  current: boolean
}

export interface SecurityEvent {
  id: string
  action: string
  resource: string
  timestamp: string
  ipAddress: string
  userAgent: string
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL'
}

export const securityService = {
  // MFA operations
  async getMfaSetup(): Promise<MfaSetupResponse> {
    const response = await api.get<MfaSetupResponse>('/security/mfa/setup')
    return response.data
  },

  async enableMfa(verificationCode: string): Promise<{ success: boolean; enabled: boolean; enabledAt: string; message: string }> {
    const response = await api.post('/security/mfa/enable', { verificationCode })
    return response.data
  },

  async disableMfa(verificationCode: string): Promise<{ success: boolean; enabled: boolean; message: string }> {
    const response = await api.post('/security/mfa/disable', { verificationCode })
    return response.data
  },

  async validateMfaCode(mfaCode: string): Promise<{ valid: boolean; message: string }> {
    const response = await api.post('/security/mfa/validate', { mfaCode })
    return response.data
  },

  // Session management
  async getUserSessions(): Promise<{ sessions: SessionInfo[]; message?: string }> {
    const response = await api.get('/security/sessions')
    return response.data
  },

  async revokeSession(sessionId: string): Promise<{ success: boolean; message: string }> {
    const response = await api.delete(`/security/sessions/${sessionId}`)
    return response.data
  },

  // Security events/audit log
  async getSecurityEvents(page: number = 0, size: number = 20): Promise<{
    events: SecurityEvent[]
    page: number
    size: number
    total: number
    message?: string
  }> {
    const response = await api.get(`/security/events?page=${page}&size=${size}`)
    return response.data
  },

  // Get last password change date
  async getLastPasswordChange(): Promise<{ lastChanged: string | null; message: string }> {
    const response = await api.get('/security/password/last-changed')
    return response.data
  },

  // Password change (already in profileService, but keeping here for completeness)
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await api.post('/security/change-password', { currentPassword, newPassword })
  }
}