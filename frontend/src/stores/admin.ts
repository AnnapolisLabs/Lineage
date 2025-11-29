import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { adminService, type AdminUser, type UserListResponse, type SystemStatistics, type AuditLogEntry } from '@/services/adminService'

export const useAdminStore = defineStore('admin', () => {
  const users = ref<AdminUser[]>([])
  const currentPage = ref(0)
  const totalPages = ref(0)
  const totalUsers = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const statistics = ref<SystemStatistics | null>(null)
  const auditLogs = ref<AuditLogEntry[]>([])
  const auditCurrentPage = ref(0)
  const auditTotalPages = ref(0)
  const auditTotalLogs = ref(0)

  const searchQuery = ref('')
  const selectedUser = ref<AdminUser | null>(null)

  const hasUsers = computed(() => users.value.length > 0)
  const isLoading = computed(() => loading.value)
  const hasError = computed(() => error.value !== null)

  async function fetchUsers(page: number = 0, size: number = 20, search: string = '') {
    loading.value = true
    error.value = null
    try {
      const response: UserListResponse = await adminService.getUsers(page, size, search)
      users.value = response.users
      currentPage.value = response.page
      totalPages.value = response.totalPages
      totalUsers.value = response.total
      searchQuery.value = search
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to load users'
      console.error('Users fetch error:', error.value)
    } finally {
      loading.value = false
    }
  }

  async function fetchUserById(userId: string) {
    loading.value = true
    error.value = null
    try {
      const user = await adminService.getUserById(userId)
      selectedUser.value = user
      return user
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to load user'
      console.error('User fetch error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateUser(userId: string, userData: any) {
    loading.value = true
    error.value = null
    try {
      const updatedUser = await adminService.updateUser(userId, userData)

      // Update user in the list if it exists
      const index = users.value.findIndex(u => u.id === userId)
      if (index !== -1) {
        users.value[index] = updatedUser
      }

      // Update selected user if it's the same
      if (selectedUser.value?.id === userId) {
        selectedUser.value = updatedUser
      }

      return updatedUser
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to update user'
      console.error('User update error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteUser(userId: string) {
    loading.value = true
    error.value = null
    try {
      await adminService.deleteUser(userId)

      // Remove user from the list
      users.value = users.value.filter(u => u.id !== userId)

      // Clear selected user if it's the deleted one
      if (selectedUser.value?.id === userId) {
        selectedUser.value = null
      }

      totalUsers.value = Math.max(0, totalUsers.value - 1)
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to delete user'
      console.error('User delete error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function lockUserAccount(userId: string) {
    loading.value = true
    error.value = null
    try {
      await adminService.lockUserAccount(userId)

      // Update user status in the list
      const user = users.value.find(u => u.id === userId)
      if (user) {
        user.status = 'SUSPENDED'
      }

      if (selectedUser.value?.id === userId) {
        selectedUser.value.status = 'SUSPENDED'
      }
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to lock user account'
      console.error('User lock error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function unlockUserAccount(userId: string) {
    loading.value = true
    error.value = null
    try {
      await adminService.unlockUserAccount(userId)
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to unlock user account'
      console.error('User unlock error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function reactivateUserAccount(userId: string) {
    loading.value = true
    error.value = null
    try {
      await adminService.reactivateUserAccount(userId)

      // Update user status in the list
      const user = users.value.find(u => u.id === userId)
      if (user) {
        user.status = 'ACTIVE'
      }

      if (selectedUser.value?.id === userId) {
        selectedUser.value.status = 'ACTIVE'
      }
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to reactivate user account'
      console.error('User reactivate error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createUser(userData: any) {
    loading.value = true
    error.value = null
    try {
      const newUser = await adminService.createUser(userData)

      // Add to the beginning of the list
      users.value.unshift(newUser)
      totalUsers.value += 1

      return newUser
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to create user'
      console.error('User create error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function setUserPassword(userId: string, newPassword: string) {
    loading.value = true
    error.value = null
    try {
      await adminService.setUserPassword(userId, { newPassword })
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to set user password'
      console.error('User password set error:', error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchSystemStatistics() {
    loading.value = true
    error.value = null
    try {
      statistics.value = await adminService.getSystemStatistics()
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to load statistics'
      console.error('Statistics fetch error:', error.value)
    } finally {
      loading.value = false
    }
  }

  async function fetchAuditLogs(
    page: number = 0,
    size: number = 20,
    userId?: string,
    action?: string,
    severity?: string,
    startDate?: string,
    endDate?: string
  ) {
    loading.value = true
    error.value = null
    try {
      const response = await adminService.getAuditLogs(page, size, userId, action, severity, startDate, endDate)
      auditLogs.value = response.logs
      auditCurrentPage.value = response.page
      auditTotalPages.value = response.totalPages
      auditTotalLogs.value = response.total
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to load audit logs'
      console.error('Audit logs fetch error:', error.value)
    } finally {
      loading.value = false
    }
  }

  function clearSelectedUser() {
    selectedUser.value = null
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    users,
    currentPage,
    totalPages,
    totalUsers,
    loading,
    error,
    statistics,
    auditLogs,
    auditCurrentPage,
    auditTotalPages,
    auditTotalLogs,
    searchQuery,
    selectedUser,

    // Computed
    hasUsers,
    isLoading,
    hasError,

    // Actions
    fetchUsers,
    fetchUserById,
    updateUser,
    deleteUser,
    lockUserAccount,
    unlockUserAccount,
    reactivateUserAccount,
    createUser,
    setUserPassword,
    fetchSystemStatistics,
    fetchAuditLogs,
    clearSelectedUser,
    clearError
  }
})