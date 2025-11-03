import { vi } from 'vitest'

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn((key: string) => {
    return localStorageMock.store[key] || null
  }),
  setItem: vi.fn((key: string, value: string) => {
    localStorageMock.store[key] = value
  }),
  removeItem: vi.fn((key: string) => {
    delete localStorageMock.store[key]
  }),
  clear: vi.fn(() => {
    localStorageMock.store = {}
  }),
  store: {} as Record<string, string>
}

global.localStorage = localStorageMock as any

// Mock fetch
global.fetch = vi.fn()

// Reset mocks before each test
beforeEach(() => {
  localStorageMock.clear()
  vi.clearAllMocks()
})
