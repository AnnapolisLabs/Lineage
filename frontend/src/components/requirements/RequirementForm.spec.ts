import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import RequirementForm from './RequirementForm.vue'

describe('RequirementForm.vue', () => {
  it('should render requirement form', () => {
    const wrapper = mount(RequirementForm, {
      props: {
        requirement: null,
        projectId: '1',
        modelValue: {}
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should accept props', () => {
    const requirement = { id: '1', title: 'Test' }
    const wrapper = mount(RequirementForm, {
      props: {
        requirement: requirement as any,
        projectId: '1',
        modelValue: {}
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should handle basic rendering', async () => {
    const wrapper = mount(RequirementForm, {
      props: {
        requirement: null,
        projectId: '1',
        modelValue: {}
      }
    })

    expect(wrapper.find('div').exists()).toBe(true)
  })
})
