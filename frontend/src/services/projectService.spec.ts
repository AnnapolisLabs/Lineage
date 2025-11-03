import { describe, it, expect, beforeEach, vi } from 'vitest'
import { projectService } from './projectService'
import api from './api'

vi.mock('./api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('projectService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getAll', () => {
    it('should get all projects', async () => {
      const mockProjects = [
        { id: '1', name: 'Project 1', description: 'Desc 1', projectKey: 'P1' },
        { id: '2', name: 'Project 2', description: 'Desc 2', projectKey: 'P2' }
      ]
      vi.mocked(api.get).mockResolvedValue({ data: mockProjects } as any)

      const result = await projectService.getAll()

      expect(api.get).toHaveBeenCalledWith('/projects')
      expect(result).toEqual(mockProjects)
    })
  })

  describe('getById', () => {
    it('should get project by id', async () => {
      const mockProject = { id: '1', name: 'Project 1', description: 'Desc 1', projectKey: 'P1' }
      vi.mocked(api.get).mockResolvedValue({ data: mockProject } as any)

      const result = await projectService.getById('1')

      expect(api.get).toHaveBeenCalledWith('/projects/1')
      expect(result).toEqual(mockProject)
    })
  })

  describe('create', () => {
    it('should create project', async () => {
      const mockProject = { id: '1', name: 'New Project', description: 'New Desc', projectKey: 'NP' }
      const createData = { name: 'New Project', description: 'New Desc', projectKey: 'NP' }
      vi.mocked(api.post).mockResolvedValue({ data: mockProject } as any)

      const result = await projectService.create(createData)

      expect(api.post).toHaveBeenCalledWith('/projects', createData)
      expect(result).toEqual(mockProject)
    })
  })

  describe('update', () => {
    it('should update project', async () => {
      const mockProject = { id: '1', name: 'Updated Project', description: 'Updated Desc', projectKey: 'UP' }
      const updateData = { name: 'Updated Project', description: 'Updated Desc', projectKey: 'UP' }
      vi.mocked(api.put).mockResolvedValue({ data: mockProject } as any)

      const result = await projectService.update('1', updateData)

      expect(api.put).toHaveBeenCalledWith('/projects/1', updateData)
      expect(result).toEqual(mockProject)
    })
  })

  describe('delete', () => {
    it('should delete project', async () => {
      vi.mocked(api.delete).mockResolvedValue({} as any)

      await projectService.delete('1')

      expect(api.delete).toHaveBeenCalledWith('/projects/1')
    })
  })
})
