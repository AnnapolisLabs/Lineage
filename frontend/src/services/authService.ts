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

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', credentials)
    return response.data
  },

  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/auth/me')
    return response.data
  },

  logout() {
    localStorage.removeItem('auth_token')
  }
}
