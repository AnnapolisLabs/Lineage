import api from './api'
import type {
  Team,
  CreateTeamRequest,
  TeamMember,
  InviteTeamMemberRequest,
  UpdateMemberRoleRequest,
  PaginatedResponse
} from '@/types/rbac'

export interface TeamListParams {
  project_id?: string
  page?: number
  size?: number
}

export interface TeamMemberListParams {
  page?: number
  size?: number
}

export const teamService = {
  // Team Management
  async createTeam(data: CreateTeamRequest): Promise<Team> {
    const response = await api.post<Team>('/v1/teams', data)
    return response.data
  },

  async getTeams(params?: TeamListParams): Promise<PaginatedResponse<Team>> {
    const response = await api.get<PaginatedResponse<Team>>('/v1/teams', { params })
    return response.data
  },

  async getTeam(teamId: string): Promise<Team> {
    const response = await api.get<Team>(`/v1/teams/${teamId}`)
    return response.data
  },

  async updateTeam(teamId: string, data: Partial<CreateTeamRequest>): Promise<Team> {
    const response = await api.put<Team>(`/v1/teams/${teamId}`, data)
    return response.data
  },

  async deleteTeam(teamId: string): Promise<void> {
    await api.delete(`/v1/teams/${teamId}`)
  },

  // Team Member Management
  async inviteTeamMember(teamId: string, data: InviteTeamMemberRequest): Promise<void> {
    await api.post(`/v1/teams/${teamId}/members/invite`, data)
  },

  async acceptTeamInvitation(invitationId: string): Promise<void> {
    await api.post(`/v1/teams/invitations/${invitationId}/accept`)
  },

  async getTeamMembers(teamId: string, params?: TeamMemberListParams): Promise<TeamMember[]> {
    const response = await api.get<TeamMember[]>(`/v1/teams/${teamId}/members`, { params })
    return response.data
  },

  async removeTeamMember(teamId: string, userId: string): Promise<void> {
    await api.delete(`/v1/teams/${teamId}/members/${userId}`)
  },

  async updateMemberRole(teamId: string, userId: string, data: UpdateMemberRoleRequest): Promise<TeamMember> {
    const response = await api.put<TeamMember>(`/v1/teams/${teamId}/members/${userId}/role`, data)
    return response.data
  },

  // Helper methods
  async getTeamsByProject(projectId: string): Promise<Team[]> {
    const response = await this.getTeams({ project_id: projectId, size: 100 })
    return response.content
  }
}