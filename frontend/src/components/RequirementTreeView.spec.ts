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
        projectId: '1',
        expanded: {}
      },
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should accept requirements prop', () => {
    const requirements = [
      { id: '1', reqId: 'REQ-1', title: 'Root Requirement', level: 0 }
    ]

    const wrapper = mount(RequirementTreeView, {
      props: {
        requirements,
        projectId: '1',
        expanded: new Set()
      },
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.props('requirements')).toEqual(requirements)
  })

  it('should handle empty requirements', () => {
    const wrapper = mount(RequirementTreeView, {
      props: {
        requirements: [],
        projectId: '1',
        expanded: {}
      },
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.props('requirements')).toEqual([])
  })
})
