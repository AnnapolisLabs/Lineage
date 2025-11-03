import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProjectStore } from './projects'
import { projectService } from '@/services/projectService'

vi.mock('@/services/projectService', () => ({
  projectService: {
    getAll: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  }
}))

describe('useProjectStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('fetchProjects', () => {
    it('should fetch projects successfully', async () => {
      const store = useProjectStore()
      const mockProjects = [
        { id: '1', name: 'Project 1', projectKey: 'P1' },
        { id: '2', name: 'Project 2', projectKey: 'P2' }
      ]
      vi.mocked(projectService.getAll).mockResolvedValue(mockProjects as any)

      await store.fetchProjects()

      expect(store.projects).toEqual(mockProjects)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should handle fetch error', async () => {
      const store = useProjectStore()
      const mockError = { response: { data: { message: 'Fetch failed' } } }
      vi.mocked(projectService.getAll).mockRejectedValue(mockError)

      await store.fetchProjects()

      expect(store.error).toBe('Fetch failed')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchProject', () => {
    it('should fetch single project successfully', async () => {
      const store = useProjectStore()
      const mockProject = { id: '1', name: 'Project 1', projectKey: 'P1' }
      vi.mocked(projectService.getById).mockResolvedValue(mockProject as any)

      const result = await store.fetchProject('1')

      expect(result).toEqual(mockProject)
      expect(store.currentProject).toEqual(mockProject)
      expect(store.loading).toBe(false)
    })

    it('should handle fetch error', async () => {
      const store = useProjectStore()
      const mockError = { response: { data: { message: 'Not found' } } }
      vi.mocked(projectService.getById).mockRejectedValue(mockError)

      const result = await store.fetchProject('1')

      expect(result).toBeNull()
      expect(store.error).toBe('Not found')
    })
  })

  describe('createProject', () => {
    it('should create project successfully', async () => {
      const store = useProjectStore()
      const mockProject = { id: '1', name: 'New Project', projectKey: 'NP' }
      const createData = { name: 'New Project', description: 'Desc', projectKey: 'NP' }
      vi.mocked(projectService.create).mockResolvedValue(mockProject as any)

      const result = await store.createProject(createData)

      expect(result).toEqual(mockProject)
      expect(store.projects.length).toBe(1)
      expect(store.loading).toBe(false)
    })

    it('should handle create error', async () => {
      const store = useProjectStore()
      const mockError = { response: { data: { message: 'Create failed' } } }
      vi.mocked(projectService.create).mockRejectedValue(mockError)

      const result = await store.createProject({ name: 'New', description: 'Desc', projectKey: 'NP' })

      expect(result).toBeNull()
      expect(store.error).toBe('Create failed')
    })
  })

  describe('updateProject', () => {
    it('should update project successfully', async () => {
      const store = useProjectStore()
      store.projects = [{ id: '1', name: 'Old Name', projectKey: 'P1' } as any]
      const mockProject = { id: '1', name: 'Updated Name', projectKey: 'P1' }
      vi.mocked(projectService.update).mockResolvedValue(mockProject as any)

      const result = await store.updateProject('1', { name: 'Updated Name', description: 'Desc', projectKey: 'P1' })

      expect(result).toEqual(mockProject)
      expect(store.projects[0]).toEqual(mockProject)
      expect(store.loading).toBe(false)
    })

    it('should update currentProject if it matches', async () => {
      const store = useProjectStore()
      store.currentProject = { id: '1', name: 'Old Name', projectKey: 'P1' } as any
      const mockProject = { id: '1', name: 'Updated Name', projectKey: 'P1' }
      vi.mocked(projectService.update).mockResolvedValue(mockProject as any)

      await store.updateProject('1', { name: 'Updated Name', description: 'Desc', projectKey: 'P1' })

      expect(store.currentProject).toEqual(mockProject)
    })

    it('should handle update error', async () => {
      const store = useProjectStore()
      const mockError = { response: { data: { message: 'Update failed' } } }
      vi.mocked(projectService.update).mockRejectedValue(mockError)

      const result = await store.updateProject('1', { name: 'Updated', description: 'Desc', projectKey: 'P1' })

      expect(result).toBeNull()
      expect(store.error).toBe('Update failed')
    })
  })

  describe('deleteProject', () => {
    it('should delete project successfully', async () => {
      const store = useProjectStore()
      store.projects = [
        { id: '1', name: 'Project 1', projectKey: 'P1' } as any,
        { id: '2', name: 'Project 2', projectKey: 'P2' } as any
      ]
      vi.mocked(projectService.delete).mockResolvedValue()

      const result = await store.deleteProject('1')

      expect(result).toBe(true)
      expect(store.projects).toHaveLength(1)
      expect(store.projects[0].id).toBe('2')
    })

    it('should clear currentProject if it matches deleted project', async () => {
      const store = useProjectStore()
      store.currentProject = { id: '1', name: 'Project 1', projectKey: 'P1' } as any
      vi.mocked(projectService.delete).mockResolvedValue()

      await store.deleteProject('1')

      expect(store.currentProject).toBeNull()
    })

    it('should handle delete error', async () => {
      const store = useProjectStore()
      const mockError = { response: { data: { message: 'Delete failed' } } }
      vi.mocked(projectService.delete).mockRejectedValue(mockError)

      const result = await store.deleteProject('1')

      expect(result).toBe(false)
      expect(store.error).toBe('Delete failed')
    })
  })
})
