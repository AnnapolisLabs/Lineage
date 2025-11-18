import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService, type User, type LoginRequest } from '@/services/authService'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('auth_token'))
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.globalRole === 'ADMIN')
  const isEditor = computed(() => user.value?.globalRole === 'DEVELOPER' || user.value?.globalRole === 'ADMIN')

  async function login(credentials: LoginRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await authService.login(credentials)
      token.value = response.token
      localStorage.setItem('auth_token', response.token)
      
      // Use the user object from the response
      user.value = {
        id: response.user.id,
        email: response.user.email,
        name: response.user.name,
        firstName: response.user.firstName,
        lastName: response.user.lastName,
        globalRole: response.user.globalRole,
        status: response.user.status,
        phoneNumber: response.user.phoneNumber,
        avatarUrl: response.user.avatarUrl,
        bio: response.user.bio,
        preferences: response.user.preferences,
        emailVerified: response.user.emailVerified,
        createdAt: response.user.createdAt,
        updatedAt: response.user.updatedAt,
        lastLoginAt: response.user.lastLoginAt
      }
      
      // Store user ID for AI service
      localStorage.setItem('user_id', response.user.id)
      return true
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Login failed'
      error.value = errorMsg
      console.error('Login error:', errorMsg)
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchCurrentUser() {
    if (!token.value) return
    try {
      user.value = await authService.getCurrentUser()
      if (user.value) {
        localStorage.setItem('user_id', user.value.id)
      }
    } catch (err: any) {
      console.error('Failed to fetch current user:', err.response?.data?.message || err.message)
      logout()
    }
  }

  /**
   * Validate token by calling backend /auth/me
   * If token is invalid, clears auth state
   */
  async function validateToken() {
    if (!token.value) {
      return
    }

    try {
      user.value = await authService.getCurrentUser()
      if (user.value) {
        localStorage.setItem('user_id', user.value.id)
      }
    } catch (err: any) {
      // Token is invalid or expired, clear it
      const errorMsg = err.response?.data?.message || 'Token validation failed'
      console.warn('Token validation failed, logging out:', errorMsg)
      logout()
    }
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('auth_token')
    localStorage.removeItem('user_id')
    authService.logout()
  }

  return {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    isAdmin,
    isEditor,
    login,
    logout,
    fetchCurrentUser,
    validateToken
  }
})
