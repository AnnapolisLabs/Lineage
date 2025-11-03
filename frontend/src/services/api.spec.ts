import { describe, it, expect, beforeEach, vi } from 'vitest'
import axios from 'axios'
import api from './api'

vi.mock('axios')

describe('api service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('should create axios instance with correct config', () => {
    expect(axios.create).toHaveBeenCalledWith({
      baseURL: '/api',
      headers: {
        'Content-Type': 'application/json'
      }
    })
  })

  it('should add auth token to request headers', async () => {
    const token = 'test-token'
    localStorage.setItem('auth_token', token)

    const config = { headers: {} }
    const requestInterceptor = (api.interceptors.request as any).handlers[0].fulfilled
    const result = requestInterceptor(config)

    expect(result.headers.Authorization).toBe(`Bearer ${token}`)
  })

  it('should not add auth header when no token exists', async () => {
    const config = { headers: {} }
    const requestInterceptor = (api.interceptors.request as any).handlers[0].fulfilled
    const result = requestInterceptor(config)

    expect(result.headers.Authorization).toBeUndefined()
  })

  it('should handle request error', async () => {
    const error = new Error('Request failed')
    const requestInterceptor = (api.interceptors.request as any).handlers[0].rejected

    await expect(requestInterceptor(error)).rejects.toThrow('Request failed')
  })

  it('should return response on successful response', () => {
    const response = { data: 'test' }
    const responseInterceptor = (api.interceptors.response as any).handlers[0].fulfilled
    const result = responseInterceptor(response)

    expect(result).toBe(response)
  })

  it('should handle 401 error and redirect to login', () => {
    const originalHref = globalThis.location.href
    delete (globalThis as any).location
    globalThis.location = { href: '' } as any

    localStorage.setItem('auth_token', 'test-token')
    const error = { response: { status: 401 } }
    const responseInterceptor = (api.interceptors.response as any).handlers[0].rejected

    expect(() => responseInterceptor(error)).rejects.toThrow()
    expect(localStorage.getItem('auth_token')).toBeNull()
    expect(globalThis.location.href).toBe('/login')

    globalThis.location.href = originalHref
  })

  it('should reject non-401 errors', async () => {
    const error = { response: { status: 500 } }
    const responseInterceptor = (api.interceptors.response as any).handlers[0].rejected

    await expect(responseInterceptor(error)).rejects.toEqual(error)
  })
})
