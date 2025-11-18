import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { profileService, type UserProfile, type UserPreferences, type ProfileUpdateRequest } from '@/services/profileService'

export const useProfileStore = defineStore('profile', () => {
  const profile = ref<UserProfile | null>(null)
  const preferences = ref<UserPreferences | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isProfileLoaded = computed(() => profile.value !== null)
  const fullName = computed(() => {
    if (!profile.value) return ''
    const { firstName, lastName, name } = profile.value
    if (firstName && lastName) {
      return `${firstName} ${lastName}`
    }
    return name || ''
  })

  const displayName = computed(() => {
    return fullName.value || profile.value?.email || ''
  })

  async function fetchProfile() {
    loading.value = true
    error.value = null
    try {
      profile.value = await profileService.getProfile()
      preferences.value = profile.value.preferences
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to load profile'
      console.error('Profile fetch error:', error.value)
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(profileData: ProfileUpdateRequest) {
    loading.value = true
    error.value = null
    try {
      const updatedProfile = await profileService.updateProfile(profileData)
      profile.value = updatedProfile
      preferences.value = updatedProfile.preferences
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to update profile'
      console.error('Profile update error:', error.value)
      return false
    } finally {
      loading.value = false
    }
  }

  async function updatePreferences(newPreferences: UserPreferences) {
    loading.value = true
    error.value = null
    try {
      const updatedPreferences = await profileService.updatePreferences(newPreferences)
      preferences.value = updatedPreferences
      if (profile.value) {
        profile.value.preferences = updatedPreferences
      }
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to update preferences'
      console.error('Preferences update error:', error.value)
      return false
    } finally {
      loading.value = false
    }
  }

  async function uploadAvatar(file: File) {
    loading.value = true
    error.value = null
    try {
      const result = await profileService.uploadAvatar(file)
      if (profile.value) {
        profile.value.avatarUrl = result.avatarUrl
      }
      return result.avatarUrl
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to upload avatar'
      console.error('Avatar upload error:', error.value)
      return null
    } finally {
      loading.value = false
    }
  }

  async function changePassword(passwordData: { currentPassword: string; newPassword: string }) {
    loading.value = true
    error.value = null
    try {
      await profileService.changePassword(passwordData)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to change password'
      console.error('Password change error:', error.value)
      return false
    } finally {
      loading.value = false
    }
  }

  async function deleteAvatar() {
    loading.value = true
    error.value = null
    try {
      await profileService.deleteAvatar()
      if (profile.value) {
        profile.value.avatarUrl = null
      }
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to delete avatar'
      console.error('Avatar delete error:', error.value)
      return false
    } finally {
      loading.value = false
    }
  }

  function clearProfile() {
    profile.value = null
    preferences.value = null
    error.value = null
  }

  return {
    profile,
    preferences,
    loading,
    error,
    isProfileLoaded,
    fullName,
    displayName,
    fetchProfile,
    updateProfile,
    updatePreferences,
    changePassword,
    uploadAvatar,
    deleteAvatar,
    clearProfile
  }
})