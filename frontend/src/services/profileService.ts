import api from './api'

export interface UserProfile {
  id: string
  email: string
  name: string
  firstName?: string | null
  lastName?: string | null
  phoneNumber?: string | null
  avatarUrl?: string | null
  bio?: string | null
  preferences: UserPreferences
  globalRole: string
  status: string
  emailVerified: boolean
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
}

export interface UserPreferences {
  theme: 'light' | 'dark' | 'system'
  language: string
  notifications: {
    email: boolean
    browser: boolean
    security: boolean
  }
}

export interface ProfileUpdateRequest {
  firstName?: string
  lastName?: string
  phoneNumber?: string
  bio?: string
  preferences?: UserPreferences
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export const profileService = {
  async getProfile(): Promise<UserProfile> {
    const response = await api.get<UserProfile>('/users/profile')
    return response.data
  },

  async updateProfile(profileData: ProfileUpdateRequest): Promise<UserProfile> {
    const response = await api.put<UserProfile>('/users/profile', profileData)
    return response.data
  },

  async uploadAvatar(file: File): Promise<{ avatarUrl: string }> {
    const formData = new FormData()
    formData.append('avatar', file)

    const response = await api.post<{ avatarUrl: string }>('/users/upload-avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  },

  async deleteAvatar(): Promise<void> {
    await api.delete('/users/avatar')
  },

  async changePassword(passwordData: ChangePasswordRequest): Promise<void> {
    await api.post('/security/change-password', passwordData)
  },

  async getPreferences(): Promise<UserPreferences> {
    const response = await api.get<UserPreferences>('/users/preferences')
    return response.data
  },

  async updatePreferences(preferences: UserPreferences): Promise<UserPreferences> {
    const response = await api.put<UserPreferences>('/users/preferences', preferences)
    return response.data
  }
}