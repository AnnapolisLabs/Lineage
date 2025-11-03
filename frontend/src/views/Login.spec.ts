import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Login from './Login.vue'
import { useAuthStore } from '@/stores/auth'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  })
}))

describe('Login.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockPush.mockClear()
  })

  it('should render login form', () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.find('form').exists()).toBe(true)
    expect(wrapper.find('#email').exists()).toBe(true)
    expect(wrapper.find('#password').exists()).toBe(true)
  })

  it('should have default credentials pre-filled', () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [createPinia()]
      }
    })

    const emailInput = wrapper.find('#email').element as HTMLInputElement
    const passwordInput = wrapper.find('#password').element as HTMLInputElement

    expect(emailInput.value).toBe('admin@lineage.local')
    expect(passwordInput.value).toBe('admin123')
  })

  it('should submit form and redirect on successful login', async () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [createPinia()]
      }
    })

    const authStore = useAuthStore()
    authStore.login = vi.fn().mockResolvedValue(true)

    await wrapper.find('form').trigger('submit.prevent')
    await wrapper.vm.$nextTick()

    expect(authStore.login).toHaveBeenCalledWith({
      email: 'admin@lineage.local',
      password: 'admin123'
    })
    expect(mockPush).toHaveBeenCalledWith('/')
  })

  it('should not redirect on failed login', async () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [createPinia()]
      }
    })

    const authStore = useAuthStore()
    authStore.login = vi.fn().mockResolvedValue(false)

    await wrapper.find('form').trigger('submit.prevent')
    await wrapper.vm.$nextTick()

    expect(mockPush).not.toHaveBeenCalled()
  })

  it('should display error message when login fails', async () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [createPinia()]
      }
    })

    const authStore = useAuthStore()
    authStore.error = 'Invalid credentials'

    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Invalid credentials')
  })

  it('should disable submit button when loading', async () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [createPinia()]
      }
    })

    const authStore = useAuthStore()
    authStore.loading = true

    await wrapper.vm.$nextTick()

    const submitButton = wrapper.find('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()
    expect(submitButton.text()).toContain('Signing in...')
  })
})
