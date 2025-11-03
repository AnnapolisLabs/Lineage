import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import RequirementDetail from './RequirementDetail.vue'
import { requirementService } from '@/services/requirementService'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    back: vi.fn()
  }),
  useRoute: () => ({
    params: {
      projectId: 'project-1',
      requirementId: 'req-1'
    }
  })
}))

vi.mock('@/services/requirementService')

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

  it('should fetch requirement on mount', async () => {
    vi.mocked(requirementService.getById).mockResolvedValue({
      id: 'req-1',
      reqId: 'REQ-1',
      title: 'Test Requirement',
      description: 'Test Description'
    } as any)

    const wrapper = mount(RequirementDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementHistory: true,
          RequirementForm: true
        }
      }
    })

    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(requirementService.getById).toHaveBeenCalledWith('req-1')
  })

  it('should display requirement title', async () => {
    vi.mocked(requirementService.getById).mockResolvedValue({
      id: 'req-1',
      reqId: 'REQ-1',
      title: 'Test Requirement',
      description: 'Test Description'
    } as any)

    const wrapper = mount(RequirementDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementHistory: true,
          RequirementForm: true
        }
      }
    })

    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(wrapper.text()).toContain('Test Requirement')
  })

  it('should fetch requirement links', async () => {
    vi.mocked(requirementService.getById).mockResolvedValue({
      id: 'req-1',
      reqId: 'REQ-1',
      title: 'Test'
    } as any)
    vi.mocked(requirementService.getLinks).mockResolvedValue([])

    const wrapper = mount(RequirementDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementHistory: true,
          RequirementForm: true
        }
      }
    })

    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(requirementService.getLinks).toHaveBeenCalledWith('req-1')
  })

  it('should fetch requirement history', async () => {
    vi.mocked(requirementService.getById).mockResolvedValue({
      id: 'req-1',
      reqId: 'REQ-1',
      title: 'Test'
    } as any)
    vi.mocked(requirementService.getHistory).mockResolvedValue([])

    const wrapper = mount(RequirementDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementHistory: true,
          RequirementForm: true
        }
      }
    })

    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(requirementService.getHistory).toHaveBeenCalledWith('req-1')
  })
})
