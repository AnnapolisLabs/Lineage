<template>
  <div class="min-h-screen bg-gradient-to-br from-annapolis-navy via-annapolis-charcoal to-annapolis-teal-dark">
    <!-- Header -->
    <header class="bg-annapolis-charcoal/80 backdrop-blur-sm shadow-lg border-b border-annapolis-teal/20">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <router-link to="/" class="text-3xl font-bold text-white">Lineage</router-link>
          <div class="flex items-center space-x-4">
            <span class="text-sm text-annapolis-gray-300">
              {{ authStore.user?.name || authStore.user?.email }}
            </span>
            <router-link
              to="/"
              class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-annapolis-teal transition-colors duration-300"
            >
              Back to Projects
            </router-link>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="w-full px-4 sm:px-6 lg:px-8 py-8">
      <div class="mb-8">
        <h2 class="text-3xl font-bold text-white">Security Settings</h2>
        <p class="text-annapolis-gray-300 mt-2">Manage your account security and authentication preferences</p>
      </div>

      <div class="max-w-4xl mx-auto">
        <!-- Password Section -->
        <div class="security-section">
          <div class="section-header">
            <h2>Password</h2>
            <p>Change your account password regularly to maintain security</p>
          </div>

          <div class="password-info">
            <div class="info-item">
              <span class="label">Last changed:</span>
              <span class="value">{{ formatDate(lastPasswordChange) }}</span>
            </div>
            <button @click="showPasswordChange = true" class="btn-primary">
              Change Password
            </button>
          </div>
        </div>

        <!-- Sessions Section -->
        <div class="security-section">
          <div class="section-header">
            <h2>Active Sessions</h2>
            <p>Manage your active sessions across different devices</p>
          </div>

          <div v-if="securityStore.sessions.length === 0" class="empty-state">
            <p>No active sessions found</p>
            <button @click="securityStore.fetchUserSessions()" class="btn-secondary">
              Refresh Sessions
            </button>
          </div>

          <div v-else class="sessions-list">
            <div
              v-for="session in securityStore.sessions"
              :key="session.id"
              class="session-item"
              :class="{ 'current-session': session.current }"
            >
              <div class="session-info">
                <div class="session-device">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                    <line x1="8" y1="21" x2="16" y2="21"/>
                    <line x1="12" y1="17" x2="12" y2="21"/>
                  </svg>
                  <div>
                    <p class="device-name">{{ session.deviceInfo || 'Unknown Device' }}</p>
                    <p class="session-details">
                      {{ session.ipAddress }} • {{ formatDate(session.lastActivity) }}
                      <span v-if="session.current" class="current-badge">Current Session</span>
                    </p>
                  </div>
                </div>
              </div>

              <div v-if="!session.current" class="session-actions">
                <button
                  @click="revokeSession(session.id)"
                  class="btn-outline btn-small"
                  :disabled="securityStore.loading"
                >
                  Revoke
                </button>
              </div>
            </div>
          </div>

          <div class="section-actions">
            <button @click="securityStore.fetchUserSessions()" class="btn-secondary">
              Refresh Sessions
            </button>
          </div>
        </div>

        <!-- Security Events Section -->
        <div class="security-section">
          <div class="section-header">
            <h2>Recent Security Events</h2>
            <p>View recent security-related activities on your account</p>
          </div>

          <div v-if="securityStore.securityEvents.length === 0" class="empty-state">
            <p>No security events found</p>
            <button @click="securityStore.fetchSecurityEvents()" class="btn-secondary">
              Load Events
            </button>
          </div>

          <div v-else class="events-list">
            <div
              v-for="event in securityStore.securityEvents"
              :key="event.id"
              class="event-item"
              :class="`severity-${event.severity.toLowerCase()}`"
            >
              <div class="event-icon">
                <svg v-if="event.severity === 'CRITICAL'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
                <svg v-else-if="event.severity === 'WARNING'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                </svg>
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M9 12l2 2 4-4"/>
                </svg>
              </div>

              <div class="event-content">
                <p class="event-action">{{ event.action.replace(/_/g, ' ') }}</p>
                <p class="event-details">
                  {{ event.resource ? `${event.resource}` : '' }}
                  {{ event.resourceId ? `(${event.resourceId})` : '' }}
                </p>
                <p class="event-meta">
                  {{ formatDate(event.timestamp) }} • {{ event.ipAddress }}
                </p>
              </div>
            </div>
          </div>

          <div class="section-actions">
            <button @click="loadMoreEvents" class="btn-secondary" :disabled="securityStore.loading">
              {{ securityStore.loading ? 'Loading...' : 'Load More' }}
            </button>
          </div>
        </div>

        <!-- Success/Error Messages -->
        <div v-if="profileStore.error" class="alert alert-error">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="15" y1="9" x2="9" y2="15"></line>
            <line x1="9" y1="9" x2="15" y2="15"></line>
          </svg>
          {{ profileStore.error }}
        </div>

        <div v-if="successMessage" class="alert alert-success">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22,4 12,14.01 9,11.01"></polyline>
          </svg>
          {{ successMessage }}
        </div>
      </div>
    </main>

    <!-- Password Change Modal -->
    <div v-if="showPasswordChange" class="modal-overlay" @click="showPasswordChange = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Change Password</h3>
          <button @click="showPasswordChange = false" class="close-button">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <form @submit.prevent="handlePasswordChange" class="password-form">
          <div class="form-group">
            <label for="currentPassword" class="form-label">Current Password</label>
            <input
              id="currentPassword"
              v-model="passwordForm.currentPassword"
              type="password"
              class="form-input"
              required
            />
          </div>

          <div class="form-group">
            <label for="newPassword" class="form-label">New Password</label>
            <input
              id="newPassword"
              v-model="passwordForm.newPassword"
              type="password"
              class="form-input"
              required
            />
          </div>

          <div class="form-group">
            <label for="confirmPassword" class="form-label">Confirm New Password</label>
            <input
              id="confirmPassword"
              v-model="passwordForm.confirmPassword"
              type="password"
              class="form-input"
              required
            />
          </div>

          <div class="modal-actions">
            <button type="button" @click="showPasswordChange = false" class="btn-secondary">
              Cancel
            </button>
            <button type="submit" class="btn-primary" :disabled="profileStore.loading">
              {{ profileStore.loading ? 'Changing...' : 'Change Password' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useSecurityStore } from '@/stores/security'
import { useProfileStore } from '@/stores/profile'
import { useAuthStore } from '@/stores/auth'
import { securityService } from '@/services/securityService'

const authStore = useAuthStore()

const securityStore = useSecurityStore()
const profileStore = useProfileStore()

const showPasswordChange = ref(false)
const successMessage = ref('')
const lastPasswordChange = ref<string | null>(null)
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

onMounted(async () => {
  await Promise.all([
    securityStore.fetchUserSessions(),
    securityStore.fetchSecurityEvents(),
    fetchLastPasswordChange()
  ])
})

async function handlePasswordChange() {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    profileStore.error = 'Passwords do not match'
    return
  }

  try {
    const success = await profileStore.changePassword({
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword
    })

    if (success) {
      showPasswordChange.value = false
      passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
      successMessage.value = 'Password changed successfully!'
      setTimeout(() => successMessage.value = '', 3000)
    }
  } catch (error) {
    // Error is handled by the store
    console.error('Failed to change password:', error)
  }
}

async function revokeSession(sessionId: string) {
  if (confirm('Are you sure you want to revoke this session?')) {
    try {
      await securityStore.revokeSession(sessionId)
      successMessage.value = 'Session revoked successfully!'
      setTimeout(() => successMessage.value = '', 3000)
    } catch (error) {
      // Error is handled by the store
      console.error('Failed to revoke session:', error)
    }
  }
}

async function loadMoreEvents() {
  const currentPage = Math.floor(securityStore.securityEvents.length / 20)
  await securityStore.fetchSecurityEvents(currentPage + 1)
}

async function fetchLastPasswordChange() {
  try {
    const response = await securityService.getLastPasswordChange()
    lastPasswordChange.value = response.lastChanged
  } catch (error) {
    console.error('Failed to fetch last password change:', error)
    lastPasswordChange.value = null
  }
}

function formatDate(dateString?: string): string {
  if (!dateString) return 'Never'
  try {
    return new Date(dateString).toLocaleString()
  } catch {
    return 'Unknown'
  }
}
</script>

<style scoped>
/* Dark theme styles matching the Projects page */
.security-section {
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(8px);
  border-radius: 12px;
  padding: 2rem;
  border: 1px solid rgba(14, 165, 233, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.section-header h2 {
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: white;
}

.section-header p {
  margin: 0 0 1.5rem 0;
  color: #cbd5e1;
}

.password-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  background: rgba(30, 41, 59, 0.5);
  border-radius: 8px;
  border: 1px solid rgba(14, 165, 233, 0.2);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.label {
  font-size: 0.875rem;
  color: #94a3b8;
}

.value {
  font-size: 0.875rem;
  font-weight: 500;
  color: white;
}

.empty-state {
  text-align: center;
  padding: 3rem 2rem;
  color: #94a3b8;
}

.empty-state p {
  margin: 0 0 1rem 0;
}

.sessions-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border: 1px solid rgba(14, 165, 233, 0.2);
  border-radius: 8px;
  transition: all 0.2s;
  background: rgba(30, 41, 59, 0.3);
}

.session-item:hover {
  border-color: rgba(14, 165, 233, 0.4);
  background: rgba(30, 41, 59, 0.5);
}

.session-item.current-session {
  border-color: #0ea5e9;
  background: rgba(14, 165, 233, 0.1);
}

.session-info {
  flex: 1;
}

.session-device {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.session-device svg {
  color: #94a3b8;
  flex-shrink: 0;
}

.device-name {
  margin: 0 0 0.25rem 0;
  font-weight: 500;
  color: white;
}

.session-details {
  margin: 0;
  font-size: 0.875rem;
  color: #94a3b8;
}

.current-badge {
  color: #0ea5e9;
  font-weight: 500;
}

.session-actions {
  flex-shrink: 0;
}

.events-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.event-item {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid rgba(14, 165, 233, 0.2);
  border-radius: 8px;
  background: rgba(30, 41, 59, 0.3);
}

.event-item.severity-critical {
  border-color: rgba(239, 68, 68, 0.5);
  background: rgba(239, 68, 68, 0.1);
}

.event-item.severity-warning {
  border-color: rgba(245, 158, 11, 0.5);
  background: rgba(245, 158, 11, 0.1);
}

.event-item.severity-error {
  border-color: rgba(239, 68, 68, 0.5);
  background: rgba(239, 68, 68, 0.1);
}

.event-item.severity-info {
  border-color: rgba(14, 165, 233, 0.2);
  background: rgba(30, 41, 59, 0.3);
}

.event-icon {
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.event-icon svg {
  width: 16px;
  height: 16px;
}

.event-item.severity-critical .event-icon svg {
  color: #ef4444;
}

.event-item.severity-warning .event-icon svg {
  color: #f59e0b;
}

.event-item.severity-error .event-icon svg {
  color: #ef4444;
}

.event-item.severity-info .event-icon svg {
  color: #94a3b8;
}

.event-content {
  flex: 1;
}

.event-action {
  margin: 0 0 0.25rem 0;
  font-weight: 500;
  color: white;
  text-transform: capitalize;
}

.event-details {
  margin: 0 0 0.25rem 0;
  font-size: 0.875rem;
  color: #cbd5e1;
}

.event-meta {
  margin: 0;
  font-size: 0.75rem;
  color: #94a3b8;
}

.section-actions {
  margin-top: 1.5rem;
  text-align: center;
}

.alert {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 1rem;
}

.alert-error {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.alert-success {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 12px;
  width: 100%;
  max-width: 400px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 10px 10px -5px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(14, 165, 233, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(14, 165, 233, 0.2);
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: white;
}

.close-button {
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  padding: 0.25rem;
  border-radius: 4px;
  transition: color 0.2s;
}

.close-button:hover {
  color: white;
}

.password-form {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #cbd5e1;
}

.form-input {
  padding: 0.75rem;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(14, 165, 233, 0.3);
  border-radius: 6px;
  color: white;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.1);
}

.form-input::placeholder {
  color: #64748b;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid rgba(14, 165, 233, 0.2);
}

.btn-primary {
  padding: 0.75rem 1.5rem;
  background: #0ea5e9;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.btn-primary:hover:not(:disabled) {
  background: #0284c7;
  transform: translateY(-1px);
}

.btn-primary:disabled {
  background: #64748b;
  cursor: not-allowed;
  transform: none;
}

.btn-secondary {
  padding: 0.75rem 1.5rem;
  background: #64748b;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.btn-secondary:hover {
  background: #475569;
}

.btn-outline {
  background: transparent;
  color: #94a3b8;
  border: 1px solid rgba(14, 165, 233, 0.3);
  padding: 0.5rem 1rem;
  font-size: 0.75rem;
}

.btn-outline:hover {
  background: rgba(14, 165, 233, 0.1);
  color: white;
}

.btn-small {
  padding: 0.5rem 1rem;
  font-size: 0.75rem;
}

@media (max-width: 768px) {
  .security-section {
    padding: 1.5rem;
  }

  .password-info {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .session-item {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .event-item {
    flex-direction: column;
    gap: 0.5rem;
  }
}
</style>