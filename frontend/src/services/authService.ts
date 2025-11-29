import api from './api'

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  email: string
  refreshToken: string
  user: {
    id: string
    email: string
    name: string
    firstName?: string | null
    lastName?: string | null
    globalRole: string
    status: string
    emailVerified: boolean
    createdAt: string
    updatedAt: string
    lastLoginAt?: string
    phoneNumber?: string | null
    avatarUrl?: string | null
    bio?: string | null
    preferences: Record<string, any>
  }
  message: string
  success: boolean
  userId: string
  expiresAt: string | null
  mfaRequired: boolean
}

export interface User {
  id: string
  email: string
  name: string
  firstName?: string | null
  lastName?: string | null
  globalRole: string
  status: string
  phoneNumber?: string | null
  avatarUrl?: string | null
  bio?: string | null
  preferences: Record<string, any>
  emailVerified: boolean
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
}

// The /auth/me endpoint returns a wrapper object that contains meta
// information alongside the actual user payload under a `user` field.
// We model that explicitly so getCurrentUser can reliably unwrap and
// return the inner User object.
interface MeResponse {
  user: User
  // other meta fields are present but not currently used by the
  // frontend; they can be added here as needed without affecting
  // existing consumers.
  [key: string]: unknown
}

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', credentials)
    return response.data
  },

  async getCurrentUser(): Promise<User> {
    // /auth/me returns a wrapper object with the real user data nested
    // under `user`. Unwrap it here so the rest of the app always works
    // with a plain `User` instance and helpers like isAdmin/isEditor can
    // safely read `user.globalRole`.
    const response = await api.get<MeResponse>('/auth/me')
    return response.data.user
  },

  logout() {
    localStorage.removeItem('auth_token')
  }
}
