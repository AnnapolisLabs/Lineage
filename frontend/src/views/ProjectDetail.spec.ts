import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectDetail from './ProjectDetail.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    currentRoute: { value: { params: { id: '1' } } }
  }),
  useRoute: () => ({
    params: { id: '1' },
    query: {}
  })
}))

vi.mock('@/services/requirementService', () => ({
  requirementService: {
    getByProject: vi.fn().mockResolvedValue([]),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    getHistory: vi.fn(),
    search: vi.fn(),
    getLinks: vi.fn(),
    createLink: vi.fn(),
    deleteLink: vi.fn()
  }
}))

describe('ProjectDetail.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render project detail view', () => {
    const wrapper = mount(ProjectDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementTreeView: true,
          RequirementModal: true
        }
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should have project component structure', () => {
    const wrapper = mount(ProjectDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementTreeView: true,
          RequirementModal: true
        }
      }
    })

    expect(wrapper.vm).toBeDefined()
  })

  it('should render basic elements', () => {
    const wrapper = mount(ProjectDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementTreeView: true,
          RequirementModal: true
        }
      }
    })

    expect(wrapper.find('div').exists()).toBe(true)
  })
})
