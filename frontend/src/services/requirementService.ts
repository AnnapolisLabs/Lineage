import api from './api'

export interface Requirement {
  id: string
  reqId: string
  title: string
  description: string
  status: string
  priority: string
  parentId?: string
  parentReqId?: string
  level: number
  section?: string
  customFields: Record<string, any>
  createdByName: string
  createdByEmail: string
  createdAt: string
  updatedAt: string
}

export interface CreateRequirementRequest {
  title: string
  description: string
  status: string
  priority: string
  parentId?: string
  section?: string
  customFields?: Record<string, any>
}

export const requirementService = {
  async getByProject(projectId: string): Promise<Requirement[]> {
    const response = await api.get<Requirement[]>(`/projects/${projectId}/requirements`)
    return response.data
  },

  async getById(id: string): Promise<Requirement> {
    const response = await api.get<Requirement>(`/requirements/${id}`)
    return response.data
  },

  async create(projectId: string, data: CreateRequirementRequest): Promise<Requirement> {
    const response = await api.post<Requirement>(`/projects/${projectId}/requirements`, data)
    return response.data
  },

  async update(id: string, data: CreateRequirementRequest): Promise<Requirement> {
    const response = await api.put<Requirement>(`/requirements/${id}`, data)
    return response.data
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/requirements/${id}`)
  },

  async getHistory(id: string): Promise<any[]> {
    const response = await api.get<any[]>(`/requirements/${id}/history`)
    return response.data
  },

  async search(projectId: string, params: { q?: string; status?: string; priority?: string }): Promise<Requirement[]> {
    const response = await api.get<Requirement[]>(`/projects/${projectId}/search`, { params })
    return response.data
  }
}
