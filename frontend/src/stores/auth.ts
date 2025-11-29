import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService, type User, type LoginRequest } from '@/services/authService'

export const useAuthStore = defineStore('auth', () => {
  // Hydrate user from localStorage if available so that a logged-in user
  // is immediately present on page load/navigation, before `/auth/me` resolves.
  const storedUser = localStorage.getItem('auth_user')
  let initialUser: User | null = null

  if (storedUser) {
    try {
      initialUser = JSON.parse(storedUser) as User
    } catch (e) {
      console.warn('Failed to parse stored auth user, clearing it')
      localStorage.removeItem('auth_user')
    }
  }

  const user = ref<User | null>(initialUser)
  const token = ref<string | null>(localStorage.getItem('auth_token'))
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  // Backend role model (see UserRole enum on the backend):
  // - OWNER is the top "super-user" role (hierarchy level 3)
  // - ADMINISTRATOR is the standard system admin role (level 2)
  // - PROJECT_MANAGER is also an administrative/system role (level 2)
  // All roles with hierarchy level >= 2 are considered administrative on the
  // backend (UserRole.isAdministrative()), so the frontend should treat
  // OWNER, ADMINISTRATOR and PROJECT_MANAGER as admins/editors for global
  // management capabilities such as creating teams.
  const isAdmin = computed(
    () =>
      user.value?.globalRole === 'ADMINISTRATOR' ||
      user.value?.globalRole === 'OWNER' ||
      user.value?.globalRole === 'PROJECT_MANAGER'
  )

  // Editors include developer-level users as well as all administrative roles.
  const isEditor = computed(
    () =>
      user.value?.globalRole === 'DEVELOPER' ||
      user.value?.globalRole === 'ADMINISTRATOR' ||
      user.value?.globalRole === 'OWNER' ||
      user.value?.globalRole === 'PROJECT_MANAGER'
  )

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
      
      // Store user data and ID for persistence/AI service
      localStorage.setItem('auth_user', JSON.stringify(user.value))
      localStorage.setItem('user_id', response.user.id)

      // Touch role-based computed flags once on successful login so any
      // UI that depends on them (e.g. canCreateTeam via isAdmin/isEditor)
      // reacts immediately. This ensures the computed getters run after
      // the user/globalRole are populated, not only when logout clears
      // the user.
      void isAdmin.value
      void isEditor.value
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
        localStorage.setItem('auth_user', JSON.stringify(user.value))
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
        localStorage.setItem('auth_user', JSON.stringify(user.value))
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
    localStorage.removeItem('auth_user')
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
