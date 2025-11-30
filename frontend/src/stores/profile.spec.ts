import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProfileStore } from './profile'
import { profileService } from '@/services/profileService'

vi.mock('@/services/profileService')

describe('useProfileStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useProfileStore()
    
    expect(store.profile).toBeNull()
    expect(store.preferences).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('should compute isProfileLoaded correctly', () => {
    const store = useProfileStore()
    expect(store.isProfileLoaded).toBe(false)
    
    store.profile = { 
      id: '1', 
      email: 'test@test.com', 
      name: 'Test User',
      firstName: 'Test',
      lastName: 'User',
      globalRole: 'USER',
      status: 'ACTIVE',
      emailVerified: true,
      createdAt: '2023-01-01',
      updatedAt: '2023-01-01',
      preferences: {}
    } as any
    expect(store.isProfileLoaded).toBe(true)
  })

  describe('fullName', () => {
    it('should return full name when firstName and lastName exist', () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'John',
        lastName: 'Doe',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: {}
      } as any
      
      expect(store.fullName).toBe('John Doe')
    })

    it('should return name when firstName and lastName are not available', () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: undefined,
        lastName: undefined,
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: {}
      } as any
      
      expect(store.fullName).toBe('Test User')
    })

    it('should return empty string when profile is null', () => {
      const store = useProfileStore()
      expect(store.fullName).toBe('')
    })
  })

  describe('displayName', () => {
    it('should return fullName when available', () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'John',
        lastName: 'Doe',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: {}
      } as any
      
      expect(store.displayName).toBe('John Doe')
    })

    it('should return name when fullName is not available', () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: undefined,
        lastName: undefined,
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: {}
      } as any
      
      expect(store.displayName).toBe('Test User')
    })

    it('should return email when no name is available', () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: undefined,
        firstName: undefined,
        lastName: undefined,
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: {}
      } as any
      
      expect(store.displayName).toBe('test@test.com')
    })

    it('should return empty string when profile is null', () => {
      const store = useProfileStore()
      expect(store.displayName).toBe('')
    })
  })

  describe('fetchProfile', () => {
    it('should fetch profile successfully', async () => {
      const store = useProfileStore()
      const mockProfile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'John',
        lastName: 'Doe',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: { theme: 'dark' }
      }
      vi.mocked(profileService.getProfile).mockResolvedValue(mockProfile as any)

      await store.fetchProfile()

      expect(store.profile).toEqual(mockProfile)
      expect(store.preferences).toEqual(mockProfile.preferences)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should handle fetch error', async () => {
      const store = useProfileStore()
      const mockError = { response: { data: { message: 'Failed to load profile' } } }
      vi.mocked(profileService.getProfile).mockRejectedValue(mockError)

      await store.fetchProfile()

      expect(store.profile).toBeNull()
      expect(store.preferences).toBeNull()
      expect(store.error).toBe('Failed to load profile')
      expect(store.loading).toBe(false)
    })
  })

  describe('updateProfile', () => {
    it('should update profile successfully', async () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Old Name',
        firstName: 'Old',
        lastName: 'Name',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: { theme: 'light' }
      } as any
      
      const mockUpdatedProfile = {
        id: '1',
        email: 'test@test.com',
        name: 'New Name',
        firstName: 'New',
        lastName: 'Name',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-02',
        preferences: { theme: 'dark' }
      }
      vi.mocked(profileService.updateProfile).mockResolvedValue(mockUpdatedProfile as any)

      const result = await store.updateProfile({ name: 'New Name', firstName: 'New', lastName: 'Name' })

      expect(result).toBe(true)
      expect(store.profile).toEqual(mockUpdatedProfile)
      expect(store.preferences).toEqual(mockUpdatedProfile.preferences)
      expect(store.loading).toBe(false)
    })

    it('should handle update error', async () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'Test',
        lastName: 'User',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: { theme: 'light' }
      } as any
      
      const mockError = { response: { data: { message: 'Update failed' } } }
      vi.mocked(profileService.updateProfile).mockRejectedValue(mockError)

      const result = await store.updateProfile({ name: 'New Name' })

      expect(result).toBe(false)
      expect(store.error).toBe('Update failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('updatePreferences', () => {
    it('should update preferences successfully', async () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'Test',
        lastName: 'User',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: { theme: 'light' }
      } as any
      
      const newPreferences = { theme: 'dark', language: 'en' }
      vi.mocked(profileService.updatePreferences).mockResolvedValue(newPreferences as any)

      const result = await store.updatePreferences(newPreferences)

      expect(result).toBe(true)
      expect(store.preferences).toEqual(newPreferences)
      expect(store.profile.preferences).toEqual(newPreferences)
      expect(store.loading).toBe(false)
    })

    it('should handle preferences update error', async () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'Test',
        lastName: 'User',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: { theme: 'light' }
      } as any
      
      const mockError = { response: { data: { message: 'Failed to update preferences' } } }
      vi.mocked(profileService.updatePreferences).mockRejectedValue(mockError)

      const result = await store.updatePreferences({ theme: 'dark' })

      expect(result).toBe(false)
      expect(store.error).toBe('Failed to update preferences')
      expect(store.loading).toBe(false)
    })
  })

  describe('uploadAvatar', () => {
    it('should upload avatar successfully', async () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'Test',
        lastName: 'User',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: {},
        avatarUrl: null
      } as any
      
      const mockResult = { avatarUrl: 'https://example.com/avatar.jpg' }
      vi.mocked(profileService.uploadAvatar).mockResolvedValue(mockResult as any)

      const file = new File([''], 'avatar.jpg', { type: 'image/jpeg' })
      const result = await store.uploadAvatar(file)

      expect(result).toBe('https://example.com/avatar.jpg')
      expect(store.profile.avatarUrl).toBe('https://example.com/avatar.jpg')
      expect(store.loading).toBe(false)
    })

    it('should handle upload error', async () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'Test',
        lastName: 'User',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: {},
        avatarUrl: null
      } as any
      
      const mockError = { response: { data: { message: 'Upload failed' } } }
      vi.mocked(profileService.uploadAvatar).mockRejectedValue(mockError)

      const file = new File([''], 'avatar.jpg', { type: 'image/jpeg' })
      const result = await store.uploadAvatar(file)

      expect(result).toBeNull()
      expect(store.error).toBe('Upload failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('changePassword', () => {
    it('should change password successfully', async () => {
      const store = useProfileStore()
      vi.mocked(profileService.changePassword).mockResolvedValue()

      const result = await store.changePassword({
        currentPassword: 'oldPassword',
        newPassword: 'newPassword'
      })

      expect(result).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should handle password change error', async () => {
      const store = useProfileStore()
      const mockError = { response: { data: { message: 'Password change failed' } } }
      vi.mocked(profileService.changePassword).mockRejectedValue(mockError)

      const result = await store.changePassword({
        currentPassword: 'oldPassword',
        newPassword: 'newPassword'
      })

      expect(result).toBe(false)
      expect(store.error).toBe('Password change failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('deleteAvatar', () => {
    it('should delete avatar successfully', async () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'Test',
        lastName: 'User',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: {},
        avatarUrl: 'https://example.com/avatar.jpg'
      } as any
      
      vi.mocked(profileService.deleteAvatar).mockResolvedValue()

      const result = await store.deleteAvatar()

      expect(result).toBe(true)
      expect(store.profile.avatarUrl).toBeNull()
      expect(store.loading).toBe(false)
    })

    it('should handle delete avatar error', async () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'Test',
        lastName: 'User',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: {},
        avatarUrl: 'https://example.com/avatar.jpg'
      } as any
      
      const mockError = { response: { data: { message: 'Delete failed' } } }
      vi.mocked(profileService.deleteAvatar).mockRejectedValue(mockError)

      const result = await store.deleteAvatar()

      expect(result).toBe(false)
      expect(store.error).toBe('Delete failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('clearProfile', () => {
    it('should clear profile data', () => {
      const store = useProfileStore()
      store.profile = {
        id: '1',
        email: 'test@test.com',
        name: 'Test User',
        firstName: 'Test',
        lastName: 'User',
        globalRole: 'USER',
        status: 'ACTIVE',
        emailVerified: true,
        createdAt: '2023-01-01',
        updatedAt: '2023-01-01',
        preferences: { theme: 'dark' }
      } as any
      store.preferences = { theme: 'dark' }
      store.error = 'Test error'

      store.clearProfile()

      expect(store.profile).toBeNull()
      expect(store.preferences).toBeNull()
      expect(store.error).toBeNull()
    })
  })
})