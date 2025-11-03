import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import RequirementDetail from './RequirementDetail.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    back: vi.fn()
  }),
  useRoute: () => ({
    params: {
      projectId: 'project-1',
      requirementId: 'req-1'
    },
    query: {}
  })
}))

vi.mock('@/services/requirementService', () => ({
  requirementService: {
    getByProject: vi.fn().mockResolvedValue([]),
    getById: vi.fn().mockResolvedValue({ id: 'req-1', reqId: 'REQ-1', title: 'Test' }),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    getHistory: vi.fn().mockResolvedValue([]),
    search: vi.fn(),
    getLinks: vi.fn().mockResolvedValue([]),
    createLink: vi.fn(),
    deleteLink: vi.fn()
  }
}))

describe('RequirementDetail.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render requirement detail view', () => {
    const wrapper = mount(RequirementDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementHistory: true,
          RequirementForm: true
        }
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should have requirement structure', () => {
    const wrapper = mount(RequirementDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementHistory: true,
          RequirementForm: true
        }
      }
    })

    expect(wrapper.vm).toBeDefined()
  })

  it('should render elements', () => {
    const wrapper = mount(RequirementDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementHistory: true,
          RequirementForm: true
        }
      }
    })

    expect(wrapper.find('div').exists()).toBe(true)
  })
})
