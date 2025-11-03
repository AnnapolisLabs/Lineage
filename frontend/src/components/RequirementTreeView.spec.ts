import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import RequirementTreeView from './RequirementTreeView.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  })
}))

describe('RequirementTreeView.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render tree view', () => {
    const wrapper = mount(RequirementTreeView, {
      props: {
        requirements: [],
        projectId: '1'
      },
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should display requirements in tree structure', () => {
    const requirements = [
      { id: '1', reqId: 'REQ-1', title: 'Root Requirement', level: 0 },
      { id: '2', reqId: 'REQ-2', title: 'Child Requirement', level: 1, parentId: '1' }
    ]

    const wrapper = mount(RequirementTreeView, {
      props: {
        requirements,
        projectId: '1'
      },
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.text()).toContain('Root Requirement')
    expect(wrapper.text()).toContain('Child Requirement')
  })

  it('should handle empty requirements list', () => {
    const wrapper = mount(RequirementTreeView, {
      props: {
        requirements: [],
        projectId: '1'
      },
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.text()).toContain('No requirements')
  })
})
