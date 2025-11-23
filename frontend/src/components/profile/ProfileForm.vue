<template>
  <div class="profile-form">
    <form @submit.prevent="handleSubmit" class="form">
      <div class="form-section">
        <h3>Personal Information</h3>

        <div class="form-row">
          <div class="form-group">
            <label for="firstName" class="form-label">First Name</label>
            <input
              id="firstName"
              v-model="formData.firstName"
              type="text"
              class="form-input"
              placeholder="Enter your first name"
            />
          </div>

          <div class="form-group">
            <label for="lastName" class="form-label">Last Name</label>
            <input
              id="lastName"
              v-model="formData.lastName"
              type="text"
              class="form-input"
              placeholder="Enter your last name"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="phoneNumber" class="form-label">Phone Number</label>
          <input
            id="phoneNumber"
            v-model="formData.phoneNumber"
            type="tel"
            class="form-input"
            placeholder="Enter your phone number"
          />
        </div>

        <div class="form-group">
          <label for="bio" class="form-label">Bio</label>
          <textarea
            id="bio"
            v-model="formData.bio"
            class="form-textarea"
            placeholder="Tell us about yourself..."
            rows="4"
            maxlength="500"
          ></textarea>
          <div class="character-count">
            {{ formData.bio?.length || 0 }}/500
          </div>
        </div>
      </div>

      <div class="form-section">
        <h3>Preferences</h3>

        <div class="form-group">
          <label for="theme" class="form-label">Theme</label>
          <div class="radio-group" id="theme">
            <label class="radio-option">
              <input
                v-model="formData.preferences.theme"
                type="radio"
                value="light"
                class="radio-input"
              />
              <span class="radio-label">Light</span>
            </label>
            <label class="radio-option">
              <input
                v-model="formData.preferences.theme"
                type="radio"
                value="dark"
                class="radio-input"
              />
              <span class="radio-label">Dark</span>
            </label>
            <label class="radio-option">
              <input
                v-model="formData.preferences.theme"
                type="radio"
                value="system"
                class="radio-input"
              />
              <span class="radio-label">System</span>
            </label>
          </div>
        </div>

        <div class="form-group">
          <label for="language" class="form-label">Language</label>
          <select
            id="language"
            v-model="formData.preferences.language"
            class="form-select"
          >
            <option value="en">English</option>
            <option value="es">Spanish</option>
            <option value="fr">French</option>
            <option value="de">German</option>
          </select>
        </div>

        <div class="form-group">
          <label for="notifications" class="form-label">Notifications</label>
          <div class="checkbox-group" id="notifications">
            <label class="checkbox-option">
              <input
                v-model="formData.preferences.notifications.email"
                type="checkbox"
                class="checkbox-input"
              />
              <span class="checkbox-label">Email notifications</span>
            </label>
            <label class="checkbox-option">
              <input
                v-model="formData.preferences.notifications.browser"
                type="checkbox"
                class="checkbox-input"
              />
              <span class="checkbox-label">Browser notifications</span>
            </label>
            <label class="checkbox-option">
              <input
                v-model="formData.preferences.notifications.security"
                type="checkbox"
                class="checkbox-input"
              />
              <span class="checkbox-label">Security alerts</span>
            </label>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button
          type="button"
          @click="$emit('cancel')"
          class="btn-secondary"
          :disabled="loading"
        >
          Cancel
        </button>
        <button
          type="submit"
          class="btn-primary"
          :disabled="loading || !hasChanges"
        >
          {{ loading ? 'Saving...' : 'Save Changes' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { UserProfile, ProfileUpdateRequest } from '@/services/profileService'

interface Props {
  profile: UserProfile | null
  loading?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'save': [data: ProfileUpdateRequest]
  'cancel': []
}>()

const formData = ref<ProfileUpdateRequest>({
  firstName: '',
  lastName: '',
  phoneNumber: '',
  bio: '',
  preferences: {
    theme: 'system',
    language: 'en',
    notifications: {
      email: true,
      browser: true,
      security: true
    }
  }
})

const originalData = ref<string>('')

// Initialize form data when profile changes
watch(() => props.profile, (newProfile) => {
  if (newProfile) {
    formData.value = {
      firstName: newProfile.firstName || '',
      lastName: newProfile.lastName || '',
      phoneNumber: newProfile.phoneNumber || '',
      bio: newProfile.bio || '',
      preferences: { ...newProfile.preferences }
    }
    originalData.value = JSON.stringify(formData.value)
  }
}, { immediate: true })

const hasChanges = computed(() => {
  return JSON.stringify(formData.value) !== originalData.value
})

async function handleSubmit() {
  if (!hasChanges.value) return

  emit('save', formData.value)
}
</script>

<style scoped>
.profile-form {
  max-width: 600px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.form-section {
  padding: 1.5rem;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.form-section h3 {
  margin: 0 0 1.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
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
.form-select,
.form-textarea {
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.character-count {
  text-align: right;
  font-size: 0.75rem;
  color: #6b7280;
}

.radio-group {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.radio-input {
  margin: 0;
}

.radio-label {
  font-size: 0.875rem;
  color: #374151;
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.checkbox-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.checkbox-input {
  margin: 0;
}

.checkbox-label {
  font-size: 0.875rem;
  color: #374151;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding-top: 1.5rem;
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

.btn-secondary:hover:not(:disabled) {
  background: #4b5563;
}

.btn-secondary:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .radio-group {
    flex-direction: column;
    gap: 1rem;
  }
}
</style>