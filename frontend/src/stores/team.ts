import {defineStore} from 'pinia'
import {ref, computed} from 'vue'
import {teamService, type TeamListParams} from '@/services/teamService'
import type {Team, TeamMember, CreateTeamRequest, InviteTeamMemberRequest, TeamRole} from '@/types/rbac'

export const useTeamStore = defineStore('team', () => {
    // State
    const teams = ref<Team[]>([])
    const currentTeam = ref<Team | null>(null)
    const teamMembers = ref<TeamMember[]>([])
    const loading = ref(false)
    const error = ref<string | null>(null)

    // Pagination
    const currentPage = ref(0)
    const totalPages = ref(0)
    const totalElements = ref(0)
    const pageSize = ref(20)

    // Getters
    const hasTeam = computed(() => teams.value.length > 0)

    const getTeamById = computed(() => {
        return (id: string) => teams.value.find(team => team.id === id)
    })

    const getUserTeamRole = computed(() => {
        return (teamId: string, userId: string): TeamRole => {
            // Try to resolve the user's role from the loaded team member data.
            // teamMembers is typically populated via fetchTeamMembers(teamId).
            const member = teamMembers.value.find(
                (m) => m.teamId === teamId && m.userId === userId && m.status === 'ACTIVE'
            )

            if (member) {
                return member.role
            }

            // Fallback: if the user created the team, treat them as OWNER.
            const team = teams.value.find((t) => t.id === teamId)
            if (team && team.createdBy === userId) {
                return 'OWNER'
            }

            // Safe default when we have no explicit membership information.
            return 'MEMBER'
        }
    })

    // Actions
    async function fetchTeams(params?: TeamListParams) {
        loading.value = true
        error.value = null

        try {
            const response = await teamService.getTeams(params)
            teams.value = response.content

            // Update pagination info
            currentPage.value = response.number
            totalPages.value = response.totalPages
            totalElements.value = response.totalElements
            pageSize.value = response.size

            return response
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to fetch teams'
            error.value = errorMsg
            console.error('Fetch teams error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function fetchTeam(teamId: string) {
        loading.value = true
        error.value = null

        try {
            const team = await teamService.getTeam(teamId)
            currentTeam.value = team

            // Update in teams array if it exists
            const index = teams.value.findIndex(t => t.id === teamId)
            if (index >= 0) {
                teams.value[index] = team
            } else {
                teams.value.push(team)
            }

            return team
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to fetch team'
            error.value = errorMsg
            console.error('Fetch team error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function createTeam(data: CreateTeamRequest) {
        loading.value = true
        error.value = null

        try {
            const newTeam = await teamService.createTeam(data)
            teams.value.unshift(newTeam) // Add to beginning of array
            return newTeam
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to create team'
            error.value = errorMsg
            console.error('Create team error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function updateTeam(teamId: string, data: Partial<CreateTeamRequest>) {
        loading.value = true
        error.value = null

        try {
            const updatedTeam = await teamService.updateTeam(teamId, data)

            // Update in teams array
            const index = teams.value.findIndex(t => t.id === teamId)
            if (index >= 0) {
                teams.value[index] = updatedTeam
            }

            // Update current team if it matches
            if (currentTeam.value?.id === teamId) {
                currentTeam.value = updatedTeam
            }

            return updatedTeam
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to update team'
            error.value = errorMsg
            console.error('Update team error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function deleteTeam(teamId: string) {
        loading.value = true
        error.value = null

        try {
            await teamService.deleteTeam(teamId)

            // Remove from teams array
            teams.value = teams.value.filter(t => t.id !== teamId)

            // Clear current team if it matches
            if (currentTeam.value?.id === teamId) {
                currentTeam.value = null
            }

            // Clear team members
            if (teamMembers.value.length > 0) {
                teamMembers.value = []
            }
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to delete team'
            error.value = errorMsg
            console.error('Delete team error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function fetchTeamMembers(teamId: string, params?: { page?: number; size?: number }) {
        loading.value = true
        error.value = null

        try {
            // Backend returns a plain array of TeamMember, not a paginated response
            const members = await teamService.getTeamMembers(teamId, params)
            teamMembers.value = members
            return members
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to fetch team members'
            error.value = errorMsg
            console.error('Fetch team members error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function inviteTeamMember(teamId: string, data: InviteTeamMemberRequest) {
        loading.value = true
        error.value = null

        try {
            const invitation = await teamService.inviteTeamMember(teamId, data)

            // Refresh team members to show pending invitation
            await fetchTeamMembers(teamId)

            return invitation
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to invite team member'
            error.value = errorMsg
            console.error('Invite team member error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function acceptTeamInvitation(invitationId: string) {
        loading.value = true
        error.value = null

        try {
            await teamService.acceptTeamInvitation(invitationId)

            // Refresh all teams to update member counts
            await fetchTeams()
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to accept invitation'
            error.value = errorMsg
            console.error('Accept invitation error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function updateMemberRole(teamId: string, userId: string, role: TeamRole) {
        loading.value = true
        error.value = null

        try {
            const updatedMember = await teamService.updateMemberRole(teamId, userId, {role})

            // Update in team members array
            const index = teamMembers.value.findIndex(m => m.userId === userId)
            if (index >= 0) {
                teamMembers.value[index] = updatedMember
            }

            return updatedMember
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to update member role'
            error.value = errorMsg
            console.error('Update member role error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function removeTeamMember(teamId: string, userId: string) {
        loading.value = true
        error.value = null

        try {
            await teamService.removeTeamMember(teamId, userId)

            // Remove from team members array
            teamMembers.value = teamMembers.value.filter(m => m.userId !== userId)

            // Update team member count
            const team = teams.value.find(t => t.id === teamId)
            if (team) {
                team.memberCount = Math.max(0, team.memberCount - 1)
            }
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to remove team member'
            error.value = errorMsg
            console.error('Remove team member error:', errorMsg)
            throw err
        } finally {
            loading.value = false
        }
    }

    async function getTeamsByProject(projectId: string) {
        try {
            const teams = await teamService.getTeamsByProject(projectId)
            return teams
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || 'Failed to fetch teams by project'
            error.value = errorMsg
            console.error('Fetch teams by project error:', errorMsg)
            throw err
        }
    }

    function clearError() {
        error.value = null
    }

    function clearCurrentTeam() {
        currentTeam.value = null
    }

    function clearTeamMembers() {
        teamMembers.value = []
    }

    return {
        // State
        teams,
        currentTeam,
        teamMembers,
        loading,
        error,
        currentPage,
        totalPages,
        totalElements,
        pageSize,

        // Getters
        hasTeam,
        getTeamById,
        getUserTeamRole,

        // Actions
        fetchTeams,
        fetchTeam,
        createTeam,
        updateTeam,
        deleteTeam,
        fetchTeamMembers,
        inviteTeamMember,
        acceptTeamInvitation,
        updateMemberRole,
        removeTeamMember,
        getTeamsByProject,
        clearError,
        clearCurrentTeam,
        clearTeamMembers
    }
})