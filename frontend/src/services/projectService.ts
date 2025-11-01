import api from './api'

export interface Project {
  id: string
  name: string
  description: string
  projectKey: string
  levelPrefixes: Record<string, string>
  createdByName: string
  createdByEmail: string
  createdAt: string
  updatedAt: string
}

export interface CreateProjectRequest {
  name: string
  description: string
  projectKey: string
  levelPrefixes?: Record<string, string>
}

export const projectService = {
  async getAll(): Promise<Project[]> {
    const response = await api.get<Project[]>('/projects')
    return response.data
  },

  async getById(id: string): Promise<Project> {
    const response = await api.get<Project>(`/projects/${id}`)
    return response.data
  },

  async create(data: CreateProjectRequest): Promise<Project> {
    const response = await api.post<Project>('/projects', data)
    return response.data
  },

  async update(id: string, data: CreateProjectRequest): Promise<Project> {
    const response = await api.put<Project>(`/projects/${id}`, data)
    return response.data
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/projects/${id}`)
  }
}
