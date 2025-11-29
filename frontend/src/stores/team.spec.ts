import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTeamStore } from './team'
import { teamService } from '@/services/teamService'

vi.mock('@/services/teamService')

describe('useTeamStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useTeamStore()
    
    expect(store.teams).toEqual([])
    expect(store.currentTeam).toBeNull()
    expect(store.teamMembers).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.currentPage).toBe(0)
    expect(store.totalPages).toBe(0)
    expect(store.totalElements).toBe(0)
    expect(store.pageSize).toBe(20)
  })

  it('should compute hasTeam correctly', () => {
    const store = useTeamStore()
    expect(store.hasTeam).toBe(false)
    
    store.teams = [{ id: '1', name: 'Team 1', description: 'Test team', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any]
    expect(store.hasTeam).toBe(true)
  })

  describe('getTeamById', () => {
    it('should find team by id', () => {
      const store = useTeamStore()
      store.teams = [
        { id: '1', name: 'Team 1', description: 'Test team 1', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any,
        { id: '2', name: 'Team 2', description: 'Test team 2', createdBy: 'user-2', memberCount: 3, createdAt: '2023-01-02', updatedAt: '2023-01-02' } as any
      ]
      
      const result = store.getTeamById('2')
      
      expect(result).toEqual(store.teams[1])
    })

    it('should return undefined when team not found', () => {
      const store = useTeamStore()
      store.teams = [{ id: '1', name: 'Team 1', description: 'Test team', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any]
      
      const result = store.getTeamById('999')
      
      expect(result).toBeUndefined()
    })
  })

  describe('getUserTeamRole', () => {
    it('should return role from team members', () => {
      const store = useTeamStore()
      store.teamMembers = [
        { teamId: 'team-1', userId: 'user-1', role: 'ADMIN', status: 'ACTIVE' },
        { teamId: 'team-1', userId: 'user-2', role: 'MEMBER', status: 'ACTIVE' }
      ]
      
      const result = store.getUserTeamRole('team-1', 'user-1')
      
      expect(result).toBe('ADMIN')
    })

    it('should return OWNER when user created the team', () => {
      const store = useTeamStore()
      store.teams = [{ id: 'team-1', name: 'Team 1', description: 'Test team', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any]
      
      const result = store.getUserTeamRole('team-1', 'user-1')
      
      expect(result).toBe('OWNER')
    })

    it('should return MEMBER as default', () => {
      const store = useTeamStore()
      const result = store.getUserTeamRole('team-1', 'user-1')
      
      expect(result).toBe('MEMBER')
    })

    it('should return MEMBER when member status is not ACTIVE', () => {
      const store = useTeamStore()
      store.teamMembers = [{ teamId: 'team-1', userId: 'user-1', role: 'ADMIN', status: 'PENDING' }]
      
      const result = store.getUserTeamRole('team-1', 'user-1')
      
      expect(result).toBe('MEMBER')
    })
  })

  describe('fetchTeams', () => {
    it('should fetch teams successfully', async () => {
      const store = useTeamStore()
      const mockTeams = [
        { id: '1', name: 'Team 1', description: 'Test team 1', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' },
        { id: '2', name: 'Team 2', description: 'Test team 2', createdBy: 'user-2', memberCount: 3, createdAt: '2023-01-02', updatedAt: '2023-01-02' }
      ]
      const mockResponse = {
        content: mockTeams,
        number: 0,
        totalPages: 1,
        totalElements: 2,
        size: 20
      }
      vi.mocked(teamService.getTeams).mockResolvedValue(mockResponse as any)

      const result = await store.fetchTeams()

      expect(result).toEqual(mockResponse)
      expect(store.teams).toEqual(mockTeams)
      expect(store.currentPage).toBe(0)
      expect(store.totalPages).toBe(1)
      expect(store.totalElements).toBe(2)
      expect(store.pageSize).toBe(20)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should handle fetch teams error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to fetch teams' } } }
      vi.mocked(teamService.getTeams).mockRejectedValue(mockError)

      await expect(store.fetchTeams()).rejects.toThrow()
      expect(store.error).toBe('Failed to fetch teams')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchTeam', () => {
    it('should fetch single team successfully', async () => {
      const store = useTeamStore()
      const mockTeam = { id: '1', name: 'Team 1', description: 'Test team', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' }
      vi.mocked(teamService.getTeam).mockResolvedValue(mockTeam as any)

      const result = await store.fetchTeam('1')

      expect(result).toEqual(mockTeam)
      expect(store.currentTeam).toEqual(mockTeam)
      expect(store.loading).toBe(false)
    })

    it('should update team in teams array if it exists', async () => {
      const store = useTeamStore()
      store.teams = [{ id: '1', name: 'Old Name', description: 'Old team', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any]
      const mockTeam = { id: '1', name: 'New Name', description: 'New team', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-02' }
      vi.mocked(teamService.getTeam).mockResolvedValue(mockTeam as any)

      await store.fetchTeam('1')

      expect(store.teams[0]).toEqual(mockTeam)
    })

    it('should add team to teams array if it does not exist', async () => {
      const store = useTeamStore()
      const mockTeam = { id: '1', name: 'Team 1', description: 'Test team', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' }
      vi.mocked(teamService.getTeam).mockResolvedValue(mockTeam as any)

      await store.fetchTeam('1')

      expect(store.teams).toHaveLength(1)
      expect(store.teams[0]).toEqual(mockTeam)
    })

    it('should handle fetch team error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to fetch team' } } }
      vi.mocked(teamService.getTeam).mockRejectedValue(mockError)

      await expect(store.fetchTeam('1')).rejects.toThrow()
      expect(store.error).toBe('Failed to fetch team')
      expect(store.loading).toBe(false)
    })
  })

  describe('createTeam', () => {
    it('should create team successfully', async () => {
      const store = useTeamStore()
      const mockTeam = { id: '1', name: 'New Team', description: 'New team description', createdBy: 'user-1', memberCount: 1, createdAt: '2023-01-01', updatedAt: '2023-01-01' }
      const createData = { name: 'New Team', description: 'New team description' }
      vi.mocked(teamService.createTeam).mockResolvedValue(mockTeam as any)

      const result = await store.createTeam(createData)

      expect(result).toEqual(mockTeam)
      expect(store.teams[0]).toEqual(mockTeam)
      expect(store.loading).toBe(false)
    })

    it('should handle create team error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to create team' } } }
      vi.mocked(teamService.createTeam).mockRejectedValue(mockError)

      await expect(store.createTeam({ name: 'New Team', description: 'New team description' })).rejects.toThrow()
      expect(store.error).toBe('Failed to create team')
      expect(store.loading).toBe(false)
    })
  })

  describe('updateTeam', () => {
    it('should update team successfully', async () => {
      const store = useTeamStore()
      store.teams = [{ id: '1', name: 'Old Name', description: 'Old description', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any]
      const mockTeam = { id: '1', name: 'New Name', description: 'New description', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-02' }
      vi.mocked(teamService.updateTeam).mockResolvedValue(mockTeam as any)

      const result = await store.updateTeam('1', { name: 'New Name', description: 'New description' })

      expect(result).toEqual(mockTeam)
      expect(store.teams[0]).toEqual(mockTeam)
      expect(store.loading).toBe(false)
    })

    it('should update currentTeam if it matches', async () => {
      const store = useTeamStore()
      store.currentTeam = { id: '1', name: 'Old Name', description: 'Old description', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any
      const mockTeam = { id: '1', name: 'New Name', description: 'New description', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-02' }
      vi.mocked(teamService.updateTeam).mockResolvedValue(mockTeam as any)

      await store.updateTeam('1', { name: 'New Name', description: 'New description' })

      expect(store.currentTeam).toEqual(mockTeam)
    })

    it('should handle update team error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to update team' } } }
      vi.mocked(teamService.updateTeam).mockRejectedValue(mockError)

      await expect(store.updateTeam('1', { name: 'New Name' })).rejects.toThrow()
      expect(store.error).toBe('Failed to update team')
      expect(store.loading).toBe(false)
    })
  })

  describe('deleteTeam', () => {
    it('should delete team successfully', async () => {
      const store = useTeamStore()
      store.teams = [
        { id: '1', name: 'Team 1', description: 'Team 1', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any,
        { id: '2', name: 'Team 2', description: 'Team 2', createdBy: 'user-2', memberCount: 3, createdAt: '2023-01-02', updatedAt: '2023-01-02' } as any
      ]
      store.teamMembers = [{ teamId: '1', userId: 'user-1', role: 'ADMIN', status: 'ACTIVE' }]
      vi.mocked(teamService.deleteTeam).mockResolvedValue()

      await store.deleteTeam('1')

      expect(store.teams).toHaveLength(1)
      expect(store.teams[0].id).toBe('2')
      expect(store.currentTeam).toBeNull()
      expect(store.teamMembers).toEqual([])
    })

    it('should clear currentTeam if it matches deleted team', async () => {
      const store = useTeamStore()
      store.teams = [{ id: '1', name: 'Team 1', description: 'Team 1', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any]
      store.currentTeam = { id: '1', name: 'Team 1', description: 'Team 1', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any
      vi.mocked(teamService.deleteTeam).mockResolvedValue()

      await store.deleteTeam('1')

      expect(store.currentTeam).toBeNull()
    })

    it('should handle delete team error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to delete team' } } }
      vi.mocked(teamService.deleteTeam).mockRejectedValue(mockError)

      await expect(store.deleteTeam('1')).rejects.toThrow()
      expect(store.error).toBe('Failed to delete team')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchTeamMembers', () => {
    it('should fetch team members successfully', async () => {
      const store = useTeamStore()
      const mockMembers = [
        { teamId: '1', userId: 'user-1', role: 'ADMIN', status: 'ACTIVE' },
        { teamId: '1', userId: 'user-2', role: 'MEMBER', status: 'ACTIVE' }
      ]
      vi.mocked(teamService.getTeamMembers).mockResolvedValue(mockMembers as any)

      const result = await store.fetchTeamMembers('1')

      expect(result).toEqual(mockMembers)
      expect(store.teamMembers).toEqual(mockMembers)
      expect(store.loading).toBe(false)
    })

    it('should handle fetch team members error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to fetch team members' } } }
      vi.mocked(teamService.getTeamMembers).mockRejectedValue(mockError)

      await expect(store.fetchTeamMembers('1')).rejects.toThrow()
      expect(store.error).toBe('Failed to fetch team members')
      expect(store.loading).toBe(false)
    })
  })

  describe('inviteTeamMember', () => {
    it('should invite team member successfully', async () => {
      const store = useTeamStore()
      const mockInvitation = { id: 'invite-1', email: 'new@test.com', role: 'MEMBER', status: 'PENDING' }
      const inviteData = { email: 'new@test.com', role: 'MEMBER' }
      vi.mocked(teamService.inviteTeamMember).mockResolvedValue(mockInvitation as any)
      vi.mocked(teamService.getTeamMembers).mockResolvedValue([])

      const result = await store.inviteTeamMember('1', inviteData)

      expect(result).toEqual(mockInvitation)
      expect(store.loading).toBe(false)
    })

    it('should fetch team members after invitation', async () => {
      const store = useTeamStore()
      const mockInvitation = { id: 'invite-1', email: 'new@test.com', role: 'MEMBER', status: 'PENDING' }
      vi.mocked(teamService.inviteTeamMember).mockResolvedValue(mockInvitation as any)
      vi.mocked(teamService.getTeamMembers).mockResolvedValue([])

      await store.inviteTeamMember('1', { email: 'new@test.com', role: 'MEMBER' })

      expect(teamService.getTeamMembers).toHaveBeenCalledWith('1', undefined)
    })

    it('should handle invite team member error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to invite team member' } } }
      vi.mocked(teamService.inviteTeamMember).mockRejectedValue(mockError)

      await expect(store.inviteTeamMember('1', { email: 'new@test.com', role: 'MEMBER' })).rejects.toThrow()
      expect(store.error).toBe('Failed to invite team member')
      expect(store.loading).toBe(false)
    })
  })

  describe('acceptTeamInvitation', () => {
    it('should accept team invitation successfully', async () => {
      const store = useTeamStore()
      vi.mocked(teamService.acceptTeamInvitation).mockResolvedValue()
      vi.mocked(teamService.getTeams).mockResolvedValue({ content: [], number: 0, totalPages: 0, totalElements: 0, size: 20 } as any)

      await store.acceptTeamInvitation('invite-1')

      expect(teamService.acceptTeamInvitation).toHaveBeenCalledWith('invite-1')
      expect(teamService.getTeams).toHaveBeenCalled()
      expect(store.loading).toBe(false)
    })

    it('should handle accept invitation error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to accept invitation' } } }
      vi.mocked(teamService.acceptTeamInvitation).mockRejectedValue(mockError)

      await expect(store.acceptTeamInvitation('invite-1')).rejects.toThrow()
      expect(store.error).toBe('Failed to accept invitation')
      expect(store.loading).toBe(false)
    })
  })

  describe('updateMemberRole', () => {
    it('should update member role successfully', async () => {
      const store = useTeamStore()
      store.teamMembers = [{ teamId: '1', userId: 'user-1', role: 'MEMBER', status: 'ACTIVE' }]
      const mockMember = { teamId: '1', userId: 'user-1', role: 'ADMIN', status: 'ACTIVE' }
      vi.mocked(teamService.updateMemberRole).mockResolvedValue(mockMember as any)

      const result = await store.updateMemberRole('1', 'user-1', 'ADMIN')

      expect(result).toEqual(mockMember)
      expect(store.teamMembers[0]).toEqual(mockMember)
      expect(store.loading).toBe(false)
    })

    it('should handle update member role error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to update member role' } } }
      vi.mocked(teamService.updateMemberRole).mockRejectedValue(mockError)

      await expect(store.updateMemberRole('1', 'user-1', 'ADMIN')).rejects.toThrow()
      expect(store.error).toBe('Failed to update member role')
      expect(store.loading).toBe(false)
    })
  })

  describe('removeTeamMember', () => {
    it('should remove team member successfully', async () => {
      const store = useTeamStore()
      store.teamMembers = [
        { teamId: '1', userId: 'user-1', role: 'ADMIN', status: 'ACTIVE' },
        { teamId: '1', userId: 'user-2', role: 'MEMBER', status: 'ACTIVE' }
      ]
      store.teams = [{ id: '1', name: 'Team 1', description: 'Team 1', createdBy: 'user-1', memberCount: 2, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any]
      vi.mocked(teamService.removeTeamMember).mockResolvedValue()

      await store.removeTeamMember('1', 'user-2')

      expect(store.teamMembers).toHaveLength(1)
      expect(store.teamMembers[0].userId).toBe('user-1')
      expect(store.teams[0].memberCount).toBe(1)
    })

    it('should handle remove team member error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to remove team member' } } }
      vi.mocked(teamService.removeTeamMember).mockRejectedValue(mockError)

      await expect(store.removeTeamMember('1', 'user-1')).rejects.toThrow()
      expect(store.error).toBe('Failed to remove team member')
      expect(store.loading).toBe(false)
    })
  })

  describe('getTeamsByProject', () => {
    it('should get teams by project successfully', async () => {
      const store = useTeamStore()
      const mockTeams = [
        { id: '1', name: 'Team 1', description: 'Team 1', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' },
        { id: '2', name: 'Team 2', description: 'Team 2', createdBy: 'user-2', memberCount: 3, createdAt: '2023-01-02', updatedAt: '2023-01-02' }
      ]
      vi.mocked(teamService.getTeamsByProject).mockResolvedValue(mockTeams as any)

      const result = await store.getTeamsByProject('project-1')

      expect(result).toEqual(mockTeams)
    })

    it('should handle get teams by project error', async () => {
      const store = useTeamStore()
      const mockError = { response: { data: { message: 'Failed to fetch teams by project' } } }
      vi.mocked(teamService.getTeamsByProject).mockRejectedValue(mockError)

      await expect(store.getTeamsByProject('project-1')).rejects.toThrow()
      expect(store.error).toBe('Failed to fetch teams by project')
    })
  })

  describe('clearError', () => {
    it('should clear error', () => {
      const store = useTeamStore()
      store.error = 'Test error'

      store.clearError()

      expect(store.error).toBeNull()
    })
  })

  describe('clearCurrentTeam', () => {
    it('should clear current team', () => {
      const store = useTeamStore()
      store.currentTeam = { id: '1', name: 'Team 1', description: 'Team 1', createdBy: 'user-1', memberCount: 5, createdAt: '2023-01-01', updatedAt: '2023-01-01' } as any

      store.clearCurrentTeam()

      expect(store.currentTeam).toBeNull()
    })
  })

  describe('clearTeamMembers', () => {
    it('should clear team members', () => {
      const store = useTeamStore()
      store.teamMembers = [{ teamId: '1', userId: 'user-1', role: 'ADMIN', status: 'ACTIVE' }]

      store.clearTeamMembers()

      expect(store.teamMembers).toEqual([])
    })
  })
})