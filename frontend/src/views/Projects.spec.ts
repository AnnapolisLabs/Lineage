import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Projects from './Projects.vue'
import { useProjectStore } from '@/stores/projects'
import { useAuthStore } from '@/stores/auth'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  })
}))

describe('Projects.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render projects view', () => {
    const wrapper = mount(Projects, {
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should fetch projects on mount', async () => {
    const wrapper = mount(Projects, {
      global: {
        plugins: [createPinia()]
      }
    })

    const projectStore = useProjectStore()
    projectStore.fetchProjects = vi.fn().mockResolvedValue(undefined)

    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(projectStore.fetchProjects).toHaveBeenCalled()
  })

  it('should display projects list', async () => {
    const wrapper = mount(Projects, {
      global: {
        plugins: [createPinia()]
      }
    })

    const projectStore = useProjectStore()
    projectStore.projects = [
      { id: '1', name: 'Project 1', description: 'Desc 1', projectKey: 'P1' } as any,
      { id: '2', name: 'Project 2', description: 'Desc 2', projectKey: 'P2' } as any
    ]

    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Project 1')
    expect(wrapper.text()).toContain('Project 2')
  })

  it('should show loading state', async () => {
    const wrapper = mount(Projects, {
      global: {
        plugins: [createPinia()]
      }
    })

    const projectStore = useProjectStore()
    projectStore.loading = true

    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Loading')
  })

  it('should show error message', async () => {
    const wrapper = mount(Projects, {
      global: {
        plugins: [createPinia()]
      }
    })

    const projectStore = useProjectStore()
    projectStore.error = 'Failed to load projects'

    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Failed to load projects')
  })

  it('should handle create project', async () => {
    const wrapper = mount(Projects, {
      global: {
        plugins: [createPinia()]
      }
    })

    const projectStore = useProjectStore()
    projectStore.createProject = vi.fn().mockResolvedValue({ id: '1', name: 'New Project' })

    const createButton = wrapper.findAll('button').find(btn =>
      btn.text().toLowerCase().includes('create') || btn.text().toLowerCase().includes('new')
    )

    if (createButton) {
      await createButton.trigger('click')
      await flushPromises()
      expect(projectStore.createProject).toHaveBeenCalled()
    }
  })
})
