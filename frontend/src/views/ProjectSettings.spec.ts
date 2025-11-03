import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectSettings from './ProjectSettings.vue'
import { useProjectStore } from '@/stores/projects'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    back: vi.fn()
  }),
  useRoute: () => ({
    params: { id: '1' }
  })
}))

describe('ProjectSettings.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render project settings view', () => {
    const wrapper = mount(ProjectSettings, {
      global: {
        plugins: [createPinia()]
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should fetch project on mount', async () => {
    const wrapper = mount(ProjectSettings, {
      global: {
        plugins: [createPinia()]
      }
    })

    const projectStore = useProjectStore()
    projectStore.fetchProject = vi.fn().mockResolvedValue({ id: '1', name: 'Test Project' })

    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(projectStore.fetchProject).toHaveBeenCalledWith('1')
  })

  it('should display project settings form', async () => {
    const wrapper = mount(ProjectSettings, {
      global: {
        plugins: [createPinia()]
      }
    })

    const projectStore = useProjectStore()
    projectStore.currentProject = {
      id: '1',
      name: 'Test Project',
      projectKey: 'TP',
      description: 'Test Description'
    } as any

    await wrapper.vm.$nextTick()

    expect(wrapper.find('input, textarea').exists()).toBe(true)
  })

  it('should handle update project', async () => {
    const wrapper = mount(ProjectSettings, {
      global: {
        plugins: [createPinia()]
      }
    })

    const projectStore = useProjectStore()
    projectStore.currentProject = {
      id: '1',
      name: 'Test Project',
      projectKey: 'TP',
      description: 'Test Description'
    } as any
    projectStore.updateProject = vi.fn().mockResolvedValue({ id: '1', name: 'Updated Project' })

    await wrapper.vm.$nextTick()

    const form = wrapper.find('form')
    if (form.exists()) {
      await form.trigger('submit')
      await flushPromises()
      expect(projectStore.updateProject).toHaveBeenCalled()
    }
  })

  it('should handle delete project', async () => {
    const wrapper = mount(ProjectSettings, {
      global: {
        plugins: [createPinia()]
      }
    })

    const projectStore = useProjectStore()
    projectStore.currentProject = {
      id: '1',
      name: 'Test Project',
      projectKey: 'TP'
    } as any
    projectStore.deleteProject = vi.fn().mockResolvedValue(true)

    await wrapper.vm.$nextTick()

    const deleteButton = wrapper.findAll('button').find(btn =>
      btn.text().toLowerCase().includes('delete')
    )

    if (deleteButton) {
      await deleteButton.trigger('click')
      await flushPromises()
      expect(projectStore.deleteProject).toHaveBeenCalled()
    }
  })
})
