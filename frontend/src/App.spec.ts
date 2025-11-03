import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import App from './App.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/components/AIAssistant.vue', () => ({
  default: { name: 'AIAssistant', template: '<div>AI Assistant</div>' }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({}),
  useRoute: () => ({}),
  RouterView: { name: 'RouterView', template: '<div>Router View</div>' }
}))

describe('App.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render router view', () => {
    const wrapper = mount(App, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RouterView: { template: '<div>Router View</div>' },
          AIAssistant: { template: '<div>AI Assistant</div>' }
        }
      }
    })

    expect(wrapper.find('#app').exists()).toBe(true)
  })

  it('should show AIAssistant when authenticated', async () => {
    const wrapper = mount(App, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RouterView: { template: '<div>Router View</div>' },
          AIAssistant: { template: '<div class="ai-assistant">AI Assistant</div>' }
        }
      }
    })

    const authStore = useAuthStore()
    authStore.token = 'test-token'
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.ai-assistant').exists()).toBe(true)
  })

  it('should call validateToken on mount', async () => {
    const validateTokenSpy = vi.fn()
    const wrapper = mount(App, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RouterView: { template: '<div>Router View</div>' },
          AIAssistant: { template: '<div>AI Assistant</div>' }
        }
      }
    })

    const authStore = useAuthStore()
    authStore.validateToken = validateTokenSpy

    await wrapper.vm.$nextTick()
    expect(validateTokenSpy).toHaveBeenCalled()
  })
})
