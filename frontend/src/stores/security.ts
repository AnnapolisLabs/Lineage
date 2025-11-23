import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { securityService, type MfaSetupResponse, type SessionInfo, type SecurityEvent } from '@/services/securityService'

export const useSecurityStore = defineStore('security', () => {
  const mfaSetup = ref<MfaSetupResponse | null>(null)
  const sessions = ref<SessionInfo[]>([])
  const securityEvents = ref<SecurityEvent[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isMfaEnabled = computed(() => mfaSetup.value?.enabled || false)
  const isMfaSetupComplete = computed(() => mfaSetup.value?.setupComplete || false)
  const currentSession = computed(() =>
    sessions.value.find(session => session.current) || null
  )

  async function fetchMfaSetup() {
    loading.value = true
    error.value = null
    try {
      mfaSetup.value = await securityService.getMfaSetup()
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to load MFA setup'
      console.error('MFA setup fetch error:', error.value)
    } finally {
      loading.value = false
    }
  }

  async function enableMfa(verificationCode: string) {
    loading.value = true
    error.value = null
    try {
      const result = await securityService.enableMfa(verificationCode)
      if (result.success) {
        mfaSetup.value = {
          enabled: true,
          setupComplete: true,
          message: result.message
        }
      }
      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to enable MFA'
      console.error('MFA enable error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function disableMfa(verificationCode: string) {
    loading.value = true
    error.value = null
    try {
      const result = await securityService.disableMfa(verificationCode)
      if (result.success) {
        mfaSetup.value = {
          enabled: false,
          setupComplete: false,
          message: result.message
        }
      }
      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to disable MFA'
      console.error('MFA disable error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function validateMfaCode(mfaCode: string) {
    loading.value = true
    error.value = null
    try {
      const result = await securityService.validateMfaCode(mfaCode)
      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to validate MFA code'
      console.error('MFA validation error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchUserSessions() {
    loading.value = true
    error.value = null
    try {
      const result = await securityService.getUserSessions()
      sessions.value = result.sessions
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to load sessions'
      console.error('Sessions fetch error:', error.value)
    } finally {
      loading.value = false
    }
  }

  async function revokeSession(sessionId: string) {
    loading.value = true
    error.value = null
    try {
      const result = await securityService.revokeSession(sessionId)
      // Remove the revoked session from the list
      sessions.value = sessions.value.filter(session => session.id !== sessionId)
      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to revoke session'
      console.error('Session revoke error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchSecurityEvents(page: number = 0, size: number = 20) {
    loading.value = true
    error.value = null
    try {
      const result = await securityService.getSecurityEvents(page, size)
      if (page === 0) {
        securityEvents.value = result.events
      } else {
        securityEvents.value = [...securityEvents.value, ...result.events]
      }
      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to load security events'
      console.error('Security events fetch error:', error.value)
    } finally {
      loading.value = false
    }
  }

  function clearSecurityData() {
    mfaSetup.value = null
    sessions.value = []
    securityEvents.value = []
    error.value = null
  }

  return {
    mfaSetup,
    sessions,
    securityEvents,
    loading,
    error,
    isMfaEnabled,
    isMfaSetupComplete,
    currentSession,
    fetchMfaSetup,
    enableMfa,
    disableMfa,
    validateMfaCode,
    fetchUserSessions,
    revokeSession,
    fetchSecurityEvents,
    clearSecurityData
  }
})