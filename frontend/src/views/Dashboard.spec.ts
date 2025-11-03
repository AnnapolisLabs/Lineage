import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Dashboard from './Dashboard.vue'

describe('Dashboard.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render dashboard', () => {
    const wrapper = mount(Dashboard, {
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.exists()).toBe(true)
  })
})
