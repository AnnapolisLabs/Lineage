import { describe, it, expect, beforeEach, vi } from 'vitest'
import { requirementService } from './requirementService'
import api from './api'

vi.mock('./api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('requirementService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getByProject', () => {
    it('should get requirements by project id', async () => {
      const mockRequirements = [
        { id: '1', reqId: 'REQ-1', title: 'Req 1', description: 'Desc 1' },
        { id: '2', reqId: 'REQ-2', title: 'Req 2', description: 'Desc 2' }
      ]
      vi.mocked(api.get).mockResolvedValue({ data: mockRequirements } as any)

      const result = await requirementService.getByProject('project-1')

      expect(api.get).toHaveBeenCalledWith('/projects/project-1/requirements')
      expect(result).toEqual(mockRequirements)
    })
  })

  describe('getById', () => {
    it('should get requirement by id', async () => {
      const mockRequirement = { id: '1', reqId: 'REQ-1', title: 'Req 1', description: 'Desc 1' }
      vi.mocked(api.get).mockResolvedValue({ data: mockRequirement } as any)

      const result = await requirementService.getById('1')

      expect(api.get).toHaveBeenCalledWith('/requirements/1')
      expect(result).toEqual(mockRequirement)
    })
  })

  describe('create', () => {
    it('should create requirement', async () => {
      const mockRequirement = { id: '1', reqId: 'REQ-1', title: 'New Req', description: 'New Desc' }
      const createData = { title: 'New Req', description: 'New Desc', status: 'DRAFT', priority: 'MEDIUM' }
      vi.mocked(api.post).mockResolvedValue({ data: mockRequirement } as any)

      const result = await requirementService.create('project-1', createData)

      expect(api.post).toHaveBeenCalledWith('/projects/project-1/requirements', createData)
      expect(result).toEqual(mockRequirement)
    })
  })

  describe('update', () => {
    it('should update requirement', async () => {
      const mockRequirement = { id: '1', reqId: 'REQ-1', title: 'Updated Req', description: 'Updated Desc' }
      const updateData = { title: 'Updated Req', description: 'Updated Desc', status: 'APPROVED', priority: 'HIGH' }
      vi.mocked(api.put).mockResolvedValue({ data: mockRequirement } as any)

      const result = await requirementService.update('1', updateData)

      expect(api.put).toHaveBeenCalledWith('/requirements/1', updateData)
      expect(result).toEqual(mockRequirement)
    })
  })

  describe('delete', () => {
    it('should delete requirement', async () => {
      vi.mocked(api.delete).mockResolvedValue({} as any)

      await requirementService.delete('1')

      expect(api.delete).toHaveBeenCalledWith('/requirements/1')
    })
  })

  describe('getHistory', () => {
    it('should get requirement history', async () => {
      const mockHistory = [
        { id: '1', changes: 'Created requirement' },
        { id: '2', changes: 'Updated title' }
      ]
      vi.mocked(api.get).mockResolvedValue({ data: mockHistory } as any)

      const result = await requirementService.getHistory('1')

      expect(api.get).toHaveBeenCalledWith('/requirements/1/history')
      expect(result).toEqual(mockHistory)
    })
  })

  describe('search', () => {
    it('should search requirements', async () => {
      const mockRequirements = [{ id: '1', reqId: 'REQ-1', title: 'Found Req' }]
      vi.mocked(api.get).mockResolvedValue({ data: mockRequirements } as any)
      const params = { q: 'search', status: 'APPROVED', priority: 'HIGH' }

      const result = await requirementService.search('project-1', params)

      expect(api.get).toHaveBeenCalledWith('/projects/project-1/search', { params })
      expect(result).toEqual(mockRequirements)
    })
  })

  describe('getLinks', () => {
    it('should get requirement links', async () => {
      const mockLinks = [
        { id: '1', direction: 'incoming', requirement: { id: '2', reqId: 'REQ-2' } }
      ]
      vi.mocked(api.get).mockResolvedValue({ data: mockLinks } as any)

      const result = await requirementService.getLinks('1')

      expect(api.get).toHaveBeenCalledWith('/requirements/1/links')
      expect(result).toEqual(mockLinks)
    })
  })

  describe('createLink', () => {
    it('should create requirement link', async () => {
      vi.mocked(api.post).mockResolvedValue({} as any)

      await requirementService.createLink('1', '2')

      expect(api.post).toHaveBeenCalledWith('/requirements/1/links', { toRequirementId: '2' })
    })
  })

  describe('deleteLink', () => {
    it('should delete requirement link', async () => {
      vi.mocked(api.delete).mockResolvedValue({} as any)

      await requirementService.deleteLink('1')

      expect(api.delete).toHaveBeenCalledWith('/links/1')
    })
  })
})
