import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import RequirementCard from './RequirementCard.vue'

describe('RequirementCard.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render requirement card', () => {
    const requirement = {
      id: '1',
      reqId: 'REQ-1',
      title: 'Test Requirement',
      description: 'Test Description',
      status: 'DRAFT',
      priority: 'MEDIUM'
    }

    const wrapper = mount(RequirementCard, {
      props: { requirement },
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.text()).toContain('Test Requirement')
  })

  it('should display requirement ID', () => {
    const requirement = {
      id: '1',
      reqId: 'REQ-1',
      title: 'Test Requirement',
      description: 'Test Description',
      status: 'DRAFT',
      priority: 'MEDIUM'
    }

    const wrapper = mount(RequirementCard, {
      props: { requirement },
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.text()).toContain('REQ-1')
  })
})
