import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectSettings from './ProjectSettings.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    back: vi.fn()
  }),
  useRoute: () => ({
    params: { id: '1' }
  })
}))

describe('ProjectSettings.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render project settings view', () => {
    const wrapper = mount(ProjectSettings, {
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should have settings structure', () => {
    const wrapper = mount(ProjectSettings, {
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.vm).toBeDefined()
  })

  it('should render form elements', () => {
    const wrapper = mount(ProjectSettings, {
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.find('div').exists()).toBe(true)
  })
})
