import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectDetail from './ProjectDetail.vue'
import { useProjectStore } from '@/stores/projects'
import { requirementService } from '@/services/requirementService'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    currentRoute: { value: { params: { id: '1' } } }
  }),
  useRoute: () => ({
    params: { id: '1' }
  })
}))

vi.mock('@/services/requirementService')

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

  it('should fetch project on mount', async () => {
    const wrapper = mount(ProjectDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementTreeView: true,
          RequirementModal: true
        }
      }
    })

    const projectStore = useProjectStore()
    projectStore.fetchProject = vi.fn().mockResolvedValue({ id: '1', name: 'Test Project' })

    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(projectStore.fetchProject).toHaveBeenCalledWith('1')
  })

  it('should fetch requirements on mount', async () => {
    vi.mocked(requirementService.getByProject).mockResolvedValue([])

    const wrapper = mount(ProjectDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementTreeView: true,
          RequirementModal: true
        }
      }
    })

    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(requirementService.getByProject).toHaveBeenCalledWith('1')
  })

  it('should display project name', async () => {
    const wrapper = mount(ProjectDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementTreeView: true,
          RequirementModal: true
        }
      }
    })

    const projectStore = useProjectStore()
    projectStore.currentProject = { id: '1', name: 'Test Project', projectKey: 'TP' } as any

    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Test Project')
  })

  it('should display requirements', async () => {
    vi.mocked(requirementService.getByProject).mockResolvedValue([
      { id: '1', reqId: 'REQ-1', title: 'Requirement 1', description: 'Desc 1' } as any,
      { id: '2', reqId: 'REQ-2', title: 'Requirement 2', description: 'Desc 2' } as any
    ])

    const wrapper = mount(ProjectDetail, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RequirementTreeView: true,
          RequirementModal: true
        }
      }
    })

    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(wrapper.text()).toContain('Requirement 1')
  })
})
