<template>
  <div class="user-management-view">
    <div class="container">
      <div class="header">
        <h1>User Management</h1>
        <p>Manage user accounts, roles, and permissions</p>
      </div>

      <div class="content">
        <!-- Search and Filters -->
        <div class="filters-section">
          <div class="search-bar">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search users by name or email..."
              class="search-input"
              @keyup.enter="handleSearch"
            />
            <button @click="handleSearch" class="search-button">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/>
                <path d="M21 21l-4.35-4.35"/>
              </svg>
            </button>
          </div>

          <button @click="showCreateModal = true" class="btn-primary">
            + Add User
          </button>
        </div>

        <!-- Loading State -->
        <div v-if="adminStore.isLoading && adminStore.users.length === 0" class="loading-state">
          <div class="spinner"></div>
          <p>Loading users...</p>
        </div>

        <!-- Error State -->
        <div v-else-if="adminStore.hasError" class="error-state">
          <div class="error-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="15" y1="9" x2="9" y2="15"/>
              <line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
          </div>
          <h3>Error Loading Users</h3>
          <p>{{ adminStore.error }}</p>
          <button @click="adminStore.fetchUsers()" class="btn-secondary">
            Try Again
          </button>
        </div>

        <!-- Users Table -->
        <div v-else-if="adminStore.hasUsers" class="users-table-container">
          <table class="users-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Role</th>
                <th>Status</th>
                <th>Last Login</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="user in adminStore.users"
                :key="user.id"
                class="user-row"
                :class="{ 'inactive-user': user.status !== 'ACTIVE' }"
              >
                <td class="user-cell">
                  <div class="user-info">
                    <div class="user-avatar">
                      <img
                        v-if="user.avatarUrl"
                        :src="user.avatarUrl"
                        :alt="user.name"
                        class="avatar-image"
                      />
                      <div v-else class="avatar-placeholder">
                        {{ getInitials(user) }}
                      </div>
                    </div>
                    <div class="user-details">
                      <div class="user-name">{{ user.name }}</div>
                      <div class="user-email">{{ user.email }}</div>
                    </div>
                  </div>
                </td>

                <td class="role-cell">
                  <span class="role-badge" :class="`role-${user.globalRole?.toLowerCase()}`">
                    {{ formatRole(user.globalRole) }}
                  </span>
                </td>

                <td class="status-cell">
                  <span class="status-badge" :class="`status-${user.status?.toLowerCase()}`">
                    {{ user.status }}
                  </span>
                  <span v-if="user.emailVerified" class="verification-icon" title="Email verified">
                    ✓
                  </span>
                </td>

                <td class="date-cell">
                  {{ formatDate(user.lastLoginAt) }}
                </td>

                <td class="date-cell">
                  {{ formatDate(user.createdAt) }}
                </td>

                <td class="actions-cell">
                  <div class="action-buttons">
                    <button
                      @click="viewUserDetails(user)"
                      class="btn-icon"
                      title="View details"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                        <circle cx="12" cy="12" r="3"/>
                      </svg>
                    </button>

                    <button
                      @click="editUser(user)"
                      class="btn-icon"
                      title="Edit user"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                      </svg>
                    </button>

                    <button
                      v-if="user.status === 'ACTIVE'"
                      @click="lockUserAccount(user)"
                      class="btn-icon btn-warning"
                      title="Lock account"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                        <circle cx="12" cy="16" r="1"/>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                      </svg>
                    </button>

                    <button
                      v-else-if="user.status === 'SUSPENDED'"
                      @click="unlockUserAccount(user)"
                      class="btn-icon btn-success"
                      title="Unlock account"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                        <path d="M7 11V7a5 5 0 0 1 9.9-1"/>
                      </svg>
                    </button>

                    <button
                      v-else-if="user.status === 'DEACTIVATED'"
                      @click="reactivateUserAccount(user)"
                      class="btn-icon btn-success"
                      title="Reactivate account"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                        <path d="M7 11V7a5 5 0 0 1 9.9-1"/>
                      </svg>
                    </button>

                    <button
                      @click="openSetPasswordModal(user)"
                      class="btn-icon btn-warning"
                      title="Set password"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="7" cy="17" r="3"/>
                        <path d="M10 17h10"/>
                        <path d="M7 14V7a5 5 0 0 1 10 0v4"/>
                      </svg>
                    </button>

                    <button
                      @click="deleteUser(user)"
                      class="btn-icon btn-danger"
                      title="Delete user"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 6h18"/>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                        <line x1="10" y1="11" x2="10" y2="17"/>
                        <line x1="14" y1="11" x2="14" y2="17"/>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Pagination -->
          <div v-if="adminStore.totalPages > 1" class="pagination">
            <button
              @click="goToPage(adminStore.currentPage - 1)"
              :disabled="adminStore.currentPage === 0"
              class="pagination-btn"
            >
              Previous
            </button>

            <span class="pagination-info">
              Page {{ adminStore.currentPage + 1 }} of {{ adminStore.totalPages }}
              ({{ adminStore.totalUsers }} total users)
            </span>

            <button
              @click="goToPage(adminStore.currentPage + 1)"
              :disabled="adminStore.currentPage >= adminStore.totalPages - 1"
              class="pagination-btn"
            >
              Next
            </button>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else class="empty-state">
          <div class="empty-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </div>
          <h3>No Users Found</h3>
          <p>{{ searchQuery ? 'Try adjusting your search criteria.' : 'No users have been created yet.' }}</p>
          <button @click="showCreateModal = true" class="btn-primary">
            Add First User
          </button>
        </div>
      </div>
    </div>

    <!-- Create User Modal -->
    <div
      v-if="showCreateModal"
      class="modal-overlay"
      @click="showCreateModal = false"
    >
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Create New User</h3>
          <button @click="showCreateModal = false" class="close-button">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <form @submit.prevent="handleCreateUser" class="user-form">
          <div class="form-row">
            <div class="form-group">
              <label for="firstName" class="form-label">First Name *</label>
              <input
                id="firstName"
                v-model="newUser.firstName"
                type="text"
                class="form-input"
                required
              />
            </div>
            <div class="form-group">
              <label for="lastName" class="form-label">Last Name *</label>
              <input
                id="lastName"
                v-model="newUser.lastName"
                type="text"
                class="form-input"
                required
              />
            </div>
          </div>

          <div class="form-group">
            <label for="email" class="form-label">Email Address *</label>
            <input
              id="email"
              v-model="newUser.email"
              type="email"
              class="form-input"
              required
            />
          </div>

          <div class="form-group">
            <label for="globalRole" class="form-label">Role *</label>
            <select
              id="globalRole"
              v-model="newUser.globalRole"
              class="form-select"
              required
            >
              <option value="VIEWER">Viewer</option>
              <option value="DEVELOPER">Developer</option>
              <option value="PROJECT_MANAGER">Project Manager</option>
              <option value="ADMIN">Administrator</option>
            </select>
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input
                v-model="newUser.sendInvitation"
                type="checkbox"
                class="checkbox-input"
              />
              Send invitation email to user
            </label>
          </div>

          <div class="modal-actions">
            <button type="button" @click="showCreateModal = false" class="btn-secondary">
              Cancel
            </button>
            <button type="submit" class="btn-primary" :disabled="adminStore.isLoading">
              {{ adminStore.isLoading ? 'Creating...' : 'Create User' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Set Password Modal -->
    <div
      v-if="showPasswordModal"
      class="modal-overlay"
      @click="closeSetPasswordModal"
    >
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Set Password for {{ passwordTargetUser?.name || passwordTargetUser?.email }}</h3>
          <button @click="closeSetPasswordModal" class="close-button">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <form @submit.prevent="handleSetPassword" class="user-form">
          <div class="form-group">
            <label for="newPassword" class="form-label">New Password *</label>
            <input
              id="newPassword"
              v-model="passwordForm.newPassword"
              type="password"
              class="form-input"
              required
            />
            <p class="helper-text">Must be at least 12 characters and meet password policy.</p>
          </div>

          <div class="form-group">
            <label for="confirmPassword" class="form-label">Confirm Password *</label>
            <input
              id="confirmPassword"
              v-model="passwordForm.confirmPassword"
              type="password"
              class="form-input"
              required
            />
          </div>

          <p v-if="passwordError" class="error-text">{{ passwordError }}</p>
          <p v-if="passwordSuccess" class="success-text">{{ passwordSuccess }}</p>

          <div class="modal-actions">
            <button type="button" @click="closeSetPasswordModal" class="btn-secondary">
              Cancel
            </button>
            <button type="submit" class="btn-primary" :disabled="adminStore.isLoading">
              {{ adminStore.isLoading ? 'Setting...' : 'Set Password' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Edit User Modal -->
    <div
      v-if="showEditModal"
      class="modal-overlay"
      @click="showEditModal = false"
    >
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Edit User: {{ editingUser?.name }}</h3>
          <button @click="showEditModal = false" class="close-button">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <form @submit.prevent="handleUpdateUser" class="user-form">
          <div class="form-row">
            <div class="form-group">
              <label for="editFirstName" class="form-label">First Name</label>
              <input
                id="editFirstName"
                v-model="editForm.firstName"
                type="text"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label for="editLastName" class="form-label">Last Name</label>
              <input
                id="editLastName"
                v-model="editForm.lastName"
                type="text"
                class="form-input"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="editPhone" class="form-label">Phone Number</label>
            <input
              id="editPhone"
              v-model="editForm.phoneNumber"
              type="tel"
              class="form-input"
            />
          </div>

          <div class="form-group">
            <label for="editRole" class="form-label">Role</label>
            <select
              id="editRole"
              v-model="editForm.globalRole"
              class="form-select"
            >
              <option value="VIEWER">Viewer</option>
              <option value="DEVELOPER">Developer</option>
              <option value="PROJECT_MANAGER">Project Manager</option>
              <option value="ADMIN">Administrator</option>
            </select>
          </div>

          <div class="form-group">
            <label for="editStatus" class="form-label">Status</label>
            <select
              id="editStatus"
              v-model="editForm.status"
              class="form-select"
            >
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="DEACTIVATED">Deactivated</option>
            </select>
          </div>

          <div class="modal-actions">
            <button type="button" @click="showEditModal = false" class="btn-secondary">
              Cancel
            </button>
            <button type="submit" class="btn-primary" :disabled="adminStore.isLoading">
              {{ adminStore.isLoading ? 'Updating...' : 'Update User' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import type { AdminUser } from '@/services/adminService'

const adminStore = useAdminStore()

const searchQuery = ref('')
const showCreateModal = ref(false)
const showEditModal = ref(false)
const editingUser = ref<AdminUser | null>(null)
const showPasswordModal = ref(false)
const passwordTargetUser = ref<AdminUser | null>(null)
const passwordForm = ref({
  newPassword: '',
  confirmPassword: ''
})
const passwordError = ref('')
const passwordSuccess = ref('')

const newUser = ref({
  firstName: '',
  lastName: '',
  email: '',
  globalRole: 'VIEWER',
  sendInvitation: true
})

const editForm = ref({
  firstName: '',
  lastName: '',
  phoneNumber: '',
  globalRole: '',
  status: ''
})

onMounted(async () => {
  await adminStore.fetchUsers()
})

function handleSearch() {
  adminStore.fetchUsers(0, 20, searchQuery.value)
}

function getInitials(user: AdminUser): string {
  const name = user.name || ''
  const email = user.email || ''

  if (name) {
    return name.split(' ').map(n => n.charAt(0)).join('').toUpperCase()
  }

  return email.charAt(0).toUpperCase()
}

function formatRole(role?: string): string {
  if (!role) return 'User'
  return role.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
}

function formatDate(dateString?: string): string {
  if (!dateString) return 'Never'
  try {
    return new Date(dateString).toLocaleDateString()
  } catch {
    return 'Unknown'
  }
}

function viewUserDetails(user: AdminUser) {
  // User details view implementation
  console.log('View user details:', user)
}

function editUser(user: AdminUser) {
  editingUser.value = user
  editForm.value = {
    firstName: user.firstName || '',
    lastName: user.lastName || '',
    phoneNumber: user.phoneNumber || '',
    globalRole: user.globalRole || 'VIEWER',
    status: user.status || 'ACTIVE'
  }
  showEditModal.value = true
}

async function handleCreateUser() {
  try {
    await adminStore.createUser(newUser.value)
    showCreateModal.value = false
    newUser.value = {
      firstName: '',
      lastName: '',
      email: '',
      globalRole: 'VIEWER',
      sendInvitation: true
    }
  } catch (error) {
    // Error is handled by the store
    console.error('Failed to create user:', error)
  }
}

async function handleUpdateUser() {
  if (!editingUser.value) return

  try {
    await adminStore.updateUser(editingUser.value.id, editForm.value)
    showEditModal.value = false
    editingUser.value = null
  } catch (error) {
    // Error is handled by the store
    console.error('Failed to update user:', error)
  }
}

function openSetPasswordModal(user: AdminUser) {
  passwordTargetUser.value = user
  passwordForm.value = { newPassword: '', confirmPassword: '' }
  passwordError.value = ''
  passwordSuccess.value = ''
  showPasswordModal.value = true
}

function closeSetPasswordModal() {
  showPasswordModal.value = false
  passwordTargetUser.value = null
  passwordForm.value = { newPassword: '', confirmPassword: '' }
  passwordError.value = ''
  passwordSuccess.value = ''
}

async function handleSetPassword() {
  if (!passwordTargetUser.value) return

  const { newPassword, confirmPassword } = passwordForm.value
  passwordError.value = ''
  passwordSuccess.value = ''

  if (newPassword !== confirmPassword) {
    passwordError.value = 'Passwords do not match.'
    return
  }

  if (newPassword.length < 12) {
    passwordError.value = 'Password must be at least 12 characters long.'
    return
  }

  try {
    await adminStore.setUserPassword(passwordTargetUser.value.id, newPassword)
    passwordSuccess.value = 'Password updated successfully.'
  } catch (error) {
    // Store already logged the error; show generic message
    if (!passwordError.value) {
      passwordError.value = 'Failed to set password. Please check policy requirements.'
    }
  }
}

async function lockUserAccount(user: AdminUser) {
  if (confirm(`Are you sure you want to lock the account for ${user.name}?`)) {
    try {
      await adminStore.lockUserAccount(user.id)
    } catch (error) {
      // Error is handled by the store
      console.error('Failed to lock user account:', error)
    }
  }
}

async function unlockUserAccount(user: AdminUser) {
  try {
    await adminStore.unlockUserAccount(user.id)
  } catch (error) {
    // Error is handled by the store
    console.error('Failed to unlock user account:', error)
  }
}

async function reactivateUserAccount(user: AdminUser) {
  try {
    await adminStore.reactivateUserAccount(user.id)
  } catch (error) {
    // Error is handled by the store
    console.error('Failed to reactivate user account:', error)
  }
}

async function deleteUser(user: AdminUser) {
  if (confirm(`Are you sure you want to permanently delete ${user.name}? This action cannot be undone.`)) {
    try {
      await adminStore.deleteUser(user.id)
    } catch (error) {
      // Error is handled by the store
      console.error('Failed to delete user:', error)
    }
  }
}

function goToPage(page: number) {
  adminStore.fetchUsers(page, 20, searchQuery.value)
}
</script>

<style scoped>
.user-management-view {
  min-height: 100vh;
  background: #f9fafb;
  padding: 2rem 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.header {
  text-align: center;
  margin-bottom: 3rem;
}

.header h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
}

.header p {
  margin: 0;
  color: #6b7280;
  font-size: 1rem;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.filters-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-bar {
  display: flex;
  flex: 1;
  max-width: 400px;
}

.search-input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px 0 0 6px;
  font-size: 0.875rem;
  transition: border-color 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
}

.search-button {
  padding: 0.75rem 1rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 0 6px 6px 0;
  cursor: pointer;
  transition: background 0.2s;
}

.search-button:hover {
  background: #2563eb;
}

.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f4f6;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-icon,
.empty-icon {
  color: #6b7280;
  margin-bottom: 1rem;
}

.error-state h3,
.empty-state h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}

.error-state p,
.empty-state p {
  margin: 0 0 2rem 0;
  color: #6b7280;
}

.users-table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.users-table {
  width: 100%;
  border-collapse: collapse;
}

.users-table th,
.users-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.users-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.user-row:hover {
  background: #f9fafb;
}

.user-row.inactive-user {
  opacity: 0.6;
}

.user-cell {
  min-width: 250px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 0.875rem;
  font-weight: bold;
}

.user-details {
  min-width: 0;
}

.user-name {
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.user-email {
  font-size: 0.875rem;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-cell,
.status-cell {
  min-width: 120px;
}

.role-badge,
.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
}

.role-admin { background: #fef3c7; color: #92400e; }
.role-project_manager { background: #dbeafe; color: #1e40af; }
.role-developer { background: #d1fae5; color: #065f46; }
.role-viewer { background: #f3f4f6; color: #374151; }

.status-active { background: #d1fae5; color: #065f46; }
.status-suspended { background: #fef3c7; color: #92400e; }
.status-deactivated { background: #fee2e2; color: #991b1b; }

.verification-icon {
  margin-left: 0.5rem;
  color: #16a34a;
  font-weight: bold;
}

.date-cell {
  min-width: 120px;
  font-size: 0.875rem;
  color: #6b7280;
}

.actions-cell {
  min-width: 160px;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.btn-icon {
  padding: 0.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  background: transparent;
  color: #6b7280;
}

.btn-icon:hover {
  background: #f3f4f6;
  color: #374151;
}

.btn-warning:hover {
  background: #fef3c7;
  color: #92400e;
}

.btn-success:hover {
  background: #d1fae5;
  color: #065f46;
}

.btn-danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.pagination-info {
  font-size: 0.875rem;
  color: #6b7280;
}

.pagination-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #9ca3af;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}

.close-button {
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  padding: 0.25rem;
  border-radius: 4px;
  transition: color 0.2s;
}

.close-button:hover {
  color: #374151;
}

.user-form {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.form-input,
.form-select {
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #374151;
  cursor: pointer;
}

.checkbox-input {
  margin: 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.btn-primary,
.btn-secondary {
  padding: 0.75rem 1.5rem;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-size: 0.875rem;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-primary:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6b7280;
  color: white;
}

.btn-secondary:hover {
  background: #4b5563;
}

@media (max-width: 768px) {
  .filters-section {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar {
    max-width: none;
  }

  .users-table-container {
    overflow-x: auto;
  }

  .users-table {
    min-width: 800px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .modal-content {
    margin: 1rem;
  }
}
</style>