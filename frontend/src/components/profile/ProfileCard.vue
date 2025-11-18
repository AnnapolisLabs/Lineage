<template>
  <div class="profile-card">
    <div class="profile-header">
      <div class="avatar-section">
        <div class="avatar-container">
          <img
            v-if="profile?.avatarUrl"
            :src="profile.avatarUrl"
            :alt="displayName"
            class="avatar-image"
          />
          <div v-else class="avatar-placeholder">
            {{ getInitials() }}
          </div>
        </div>
        <div class="avatar-actions">
          <button
            @click="$emit('upload-avatar')"
            class="btn-secondary btn-small"
          >
            Change Photo
          </button>
          <button
            v-if="profile?.avatarUrl"
            @click="$emit('delete-avatar')"
            class="btn-outline btn-small"
          >
            Remove
          </button>
        </div>
      </div>

      <div class="profile-info">
        <h2 class="profile-name">{{ displayName }}</h2>
        <p class="profile-email">{{ profile?.email }}</p>
        <div class="profile-meta">
          <span class="role-badge" :class="`role-${profile?.globalRole?.toLowerCase()}`">
            {{ formatRole(profile?.globalRole) }}
          </span>
          <span class="status-badge" :class="`status-${profile?.status?.toLowerCase()}`">
            {{ profile?.status }}
          </span>
          <span v-if="profile?.emailVerified" class="verification-badge">
            ✓ Verified
          </span>
        </div>
        <div class="profile-stats">
          <div class="stat">
            <span class="stat-label">Member since</span>
            <span class="stat-value">{{ formatDate(profile?.createdAt) }}</span>
          </div>
          <div v-if="profile?.lastLoginAt" class="stat">
            <span class="stat-label">Last login</span>
            <span class="stat-value">{{ formatDate(profile?.lastLoginAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="profile?.bio" class="profile-bio">
      <h3>About</h3>
      <p>{{ profile.bio }}</p>
    </div>

    <div class="profile-actions">
      <button @click="$emit('edit-profile')" class="btn-primary">
        Edit Profile
      </button>
      <button @click="$emit('change-password')" class="btn-secondary">
        Change Password
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { UserProfile } from '@/services/profileService'

interface Props {
  profile: UserProfile | null
}

const props = defineProps<Props>()

defineEmits<{
  'upload-avatar': []
  'delete-avatar': []
  'edit-profile': []
  'change-password': []
}>()

const displayName = computed(() => {
  if (!props.profile) return ''
  const { firstName, lastName, name } = props.profile
  if (firstName && lastName) {
    return `${firstName} ${lastName}`
  }
  return name || ''
})

function getInitials(): string {
  if (!props.profile) return 'U'
  const { firstName, lastName, name, email } = props.profile

  if (firstName && lastName) {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase()
  }

  if (name) {
    return name.split(' ').map(n => n.charAt(0)).join('').toUpperCase()
  }

  return email.charAt(0).toUpperCase()
}

function formatRole(role?: string): string {
  if (!role) return 'User'
  return role.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
}

function formatDate(dateString?: string): string {
  if (!dateString) return 'Never'
  try {
    return new Date(dateString).toLocaleDateString()
  } catch {
    return 'Unknown'
  }
}
</script>

<style scoped>
.profile-card {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.profile-header {
  display: flex;
  gap: 2rem;
  margin-bottom: 2rem;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.avatar-container {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid #f3f4f6;
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
  font-size: 2.5rem;
  font-weight: bold;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
}

.profile-info {
  flex: 1;
}

.profile-name {
  margin: 0 0 0.5rem 0;
  font-size: 1.75rem;
  font-weight: 600;
  color: #1f2937;
}

.profile-email {
  margin: 0 0 1rem 0;
  color: #6b7280;
  font-size: 1rem;
}

.profile-meta {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.role-badge, .status-badge, .verification-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;
}

.role-admin { background: #fef3c7; color: #92400e; }
.role-project_manager { background: #dbeafe; color: #1e40af; }
.role-developer { background: #d1fae5; color: #065f46; }
.role-viewer { background: #f3f4f6; color: #374151; }

.status-active { background: #d1fae5; color: #065f46; }
.status-suspended { background: #fef3c7; color: #92400e; }
.status-deactivated { background: #fee2e2; color: #991b1b; }

.verification-badge {
  background: #d1fae5;
  color: #065f46;
}

.profile-stats {
  display: flex;
  gap: 2rem;
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 0.875rem;
  color: #6b7280;
}

.stat-value {
  font-size: 0.875rem;
  font-weight: 500;
  color: #1f2937;
}

.profile-bio {
  margin-bottom: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.profile-bio h3 {
  margin: 0 0 0.75rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}

.profile-bio p {
  margin: 0;
  color: #4b5563;
  line-height: 1.6;
}

.profile-actions {
  display: flex;
  gap: 1rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.btn-primary, .btn-secondary, .btn-outline {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-size: 0.875rem;
}

.btn-small {
  padding: 0.5rem 1rem;
  font-size: 0.75rem;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-secondary {
  background: #6b7280;
  color: white;
}

.btn-secondary:hover {
  background: #4b5563;
}

.btn-outline {
  background: transparent;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.btn-outline:hover {
  background: #f9fafb;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
  }

  .profile-stats {
    flex-direction: column;
    gap: 1rem;
  }

  .profile-actions {
    flex-direction: column;
  }
}
</style>