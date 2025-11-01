import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService, type User, type LoginRequest } from '@/services/authService'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('auth_token'))
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isEditor = computed(() => user.value?.role === 'EDITOR' || user.value?.role === 'ADMIN')

  async function login(credentials: LoginRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await authService.login(credentials)
      token.value = response.token
      localStorage.setItem('auth_token', response.token)
      user.value = {
        id: '',
        email: response.email,
        name: response.name,
        role: response.role
      }
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Login failed'
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchCurrentUser() {
    if (!token.value) return
    try {
      user.value = await authService.getCurrentUser()
    } catch (err) {
      logout()
    }
  }

  function logout() {
    user.value = null
    token.value = null
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
    fetchCurrentUser
  }
})
