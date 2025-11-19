<template>
  <div class="min-h-screen bg-gradient-to-br from-annapolis-navy via-annapolis-charcoal to-annapolis-teal-dark">
    <!-- Header -->
    <header class="bg-annapolis-charcoal/80 backdrop-blur-sm shadow-lg border-b border-annapolis-teal/20">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <router-link to="/" class="text-3xl font-bold text-white">Lineage</router-link>
          <div class="flex items-center space-x-4">
            <span class="text-sm text-annapolis-gray-300">
              {{ profileStore.displayName || profileStore.profile?.email }}
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
        <h2 class="text-3xl font-bold text-white">Profile Settings</h2>
        <p class="text-annapolis-gray-300 mt-2">Manage your account information and preferences</p>
      </div>

      <div class="max-w-4xl mx-auto">
        <!-- Profile Card -->
        <ProfileCard
          :profile="profileStore.profile"
          @upload-avatar="showAvatarUpload = true"
          @delete-avatar="handleDeleteAvatar"
          @edit-profile="showEditForm = true"
          @change-password="showPasswordChange = true"
        />

        <!-- Edit Profile Form -->
        <div v-if="showEditForm" class="form-section">
          <div class="form-header">
            <h2>Edit Profile</h2>
            <button @click="showEditForm = false" class="close-button">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <ProfileForm
            :profile="profileStore.profile"
            :loading="profileStore.loading"
            @save="handleProfileUpdate"
            @cancel="showEditForm = false"
          />
        </div>

        <!-- Password Change Section -->
        <div v-if="showPasswordChange" class="form-section">
          <div class="form-header">
            <h2>Change Password</h2>
            <button @click="showPasswordChange = false" class="close-button">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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

            <div class="form-actions">
              <button type="button" @click="showPasswordChange = false" class="btn-secondary">
                Cancel
              </button>
              <button type="submit" class="btn-primary" :disabled="profileStore.loading">
                {{ profileStore.loading ? 'Changing...' : 'Change Password' }}
              </button>
            </div>
          </form>
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

    <!-- Avatar Upload Modal -->
    <AvatarUpload
      v-if="showAvatarUpload"
      :current-avatar="profileStore.profile?.avatarUrl"
      :user-name="profileStore.displayName"
      :user-email="profileStore.profile?.email"
      @close="showAvatarUpload = false"
      @upload="handleAvatarUpload"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useProfileStore } from '@/stores/profile'
import ProfileCard from '@/components/profile/ProfileCard.vue'
import ProfileForm from '@/components/profile/ProfileForm.vue'
import AvatarUpload from '@/components/profile/AvatarUpload.vue'
import type { ProfileUpdateRequest } from '@/services/profileService'

const profileStore = useProfileStore()

const showEditForm = ref(false)
const showPasswordChange = ref(false)
const showAvatarUpload = ref(false)
const successMessage = ref<string>('')

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

onMounted(async () => {
  await profileStore.fetchProfile()
})

async function handleProfileUpdate(profileData: ProfileUpdateRequest) {
  const success = await profileStore.updateProfile(profileData)
  if (success) {
    showEditForm.value = false
    successMessage.value = 'Profile updated successfully!'
    setTimeout(() => successMessage.value = '', 3000)
  }
}

async function handlePasswordChange() {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    profileStore.error = 'Passwords do not match'
    return
  }

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
}

async function handleAvatarUpload(file: File) {
  const avatarUrl = await profileStore.uploadAvatar(file)
  if (avatarUrl) {
    successMessage.value = 'Avatar uploaded successfully!'
    setTimeout(() => successMessage.value = '', 3000)
  }
}

async function handleDeleteAvatar() {
  const success = await profileStore.deleteAvatar()
  if (success) {
    successMessage.value = 'Avatar removed successfully!'
    setTimeout(() => successMessage.value = '', 3000)
  }
}
</script>

<style scoped>
/* Dark theme styles matching the Projects page */
.form-section {
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(8px);
  border-radius: 12px;
  padding: 2rem;
  border: 1px solid rgba(14, 165, 233, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(14, 165, 233, 0.2);
}

.form-header h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: white;
}

.close-button {
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  padding: 0.5rem;
  border-radius: 6px;
  transition: all 0.2s;
}

.close-button:hover {
  color: white;
  background: rgba(14, 165, 233, 0.1);
}

.password-form {
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding-top: 1rem;
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

@media (max-width: 768px) {
  .form-section {
    padding: 1.5rem;
  }

  .form-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>