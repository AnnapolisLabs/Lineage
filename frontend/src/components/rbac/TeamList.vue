<template>
  <div class="space-y-6">
    <!-- Header with actions -->
    <div class="flex justify-between items-center">
      <div>
        <h2 class="text-xl font-bold text-white">Teams</h2>
        <p class="text-annapolis-gray-300 text-sm">
          Manage teams and collaborate with your project members
        </p>
      </div>
      <div class="flex gap-3">
        <!--
          Only render the primary "Create Team" button when the user actually
          has permission. This avoids the confusing behaviour where the button
          appears disabled (greyed out) but can still be clicked to open the
          modal and create teams.

          The tooltip messaging is handled purely via the browser's title
          attribute, so when the button is not rendered there is no implied
          permission hint.
        -->
        <button
          v-if="canCreateTeam"
          @click="handleOpenCreateTeam"
          class="inline-flex items-center px-4 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-medium rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
          title="Create a new team"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Create Team
        </button>
      </div>
    </div>

    <!-- Filters and search -->
    <div class="flex gap-4 items-center">
      <div class="flex-1">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search teams..."
          class="w-full px-4 py-2 bg-annapolis-charcoal/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
        />
      </div>
      <select
        v-model="filterRole"
        class="px-4 py-2 bg-annapolis-charcoal/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
      >
        <option
          v-for="option in ROLE_FILTER_OPTIONS"
          :key="option.value || 'ALL'"
          :value="option.value"
        >
          {{ option.label }}
        </option>
      </select>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="text-center py-12">
      <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-annapolis-teal"></div>
      <p class="text-annapolis-gray-400 mt-2">Loading teams...</p>
    </div>

    <!-- Empty state -->
    <div v-else-if="filteredTeams.length === 0" class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 text-center py-16">
      <svg class="mx-auto h-12 w-12 text-annapolis-teal mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
      <h3 class="text-lg font-medium text-white mb-2">No teams found</h3>
      <p class="text-annapolis-gray-300 mb-6">
        {{ searchQuery || filterRole ? 'No teams match your search criteria' : 'Get started by creating your first team' }}
      </p>
      <button
        v-if="canCreateTeam && !searchQuery && !filterRole"
        @click="showCreateModal = true"
        class="inline-flex items-center px-8 py-3 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
      >
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
        </svg>
        Create Team
      </button>
    </div>

    <!-- Teams grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <TeamCard
        v-for="team in filteredTeams"
        :key="team.id"
        :team="team"
        :userRole="getUserTeamRole(team.id)"
        :canManage="canManageTeam(team.id)"
        :canInvite="canInviteToTeam(team.id)"
        @edit="handleEditTeam"
        @delete="handleDeleteTeam"
        @view-members="handleViewMembers"
        @invite-member="handleInviteMember"
      />
    </div>

    <!-- Team members sidebar -->
    <div
      v-if="selectedTeam && showMembersSidebar"
      class="fixed inset-y-0 right-0 w-96 bg-annapolis-charcoal border-l border-annapolis-teal/30 shadow-xl z-40 transform transition-transform duration-300"
    >
      <div class="h-full flex flex-col">
        <div class="flex items-center justify-between p-4 border-b border-annapolis-teal/20">
          <h3 class="text-lg font-semibold text-white">{{ selectedTeam.name }} Members</h3>
          <button
            @click="closeMembersSidebar"
            class="text-annapolis-gray-400 hover:text-white transition-colors"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-4">
          <TeamMemberList
            :members="teamMembers"
            :loading="membersLoading"
            :canManageMembers="canManageTeam(selectedTeam.id)"
            :currentUserId="currentUserId"
            @invite-member="handleInviteMemberFromSidebar"
            @update-role="handleUpdateMemberRole"
            @remove-member="handleRemoveMember"
          />
        </div>
      </div>
    </div>

    <!-- Backdrop for sidebar -->
    <div
      v-if="selectedTeam && showMembersSidebar"
      class="fixed inset-0 bg-black/50 z-30"
      @click="closeMembersSidebar"
    ></div>

    <!-- Modals -->
    <CreateTeamModal
      v-model="showCreateModal"
      :projects="projects"
      :projectId="projectId"
      @create="handleCreateTeam"
    />

    <InviteMemberModal
      v-model="showInviteModal"
      :team="selectedTeam"
      :canAssignOwner="canAssignOwnerRole"
      @invite="handleInviteMemberSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { Team, TeamMember, CreateTeamRequest, InviteTeamMemberRequest, Project, TeamRole } from '@/types/rbac'
import { TEAM_ROLE_VALUES, TEAM_ROLE_LABELS } from '@/types/rbac'
import { useAuthStore } from '@/stores/auth'
import { teamService } from '@/services/teamService'
import TeamCard from './TeamCard.vue'
import TeamMemberList from './TeamMemberList.vue'
import CreateTeamModal from './CreateTeamModal.vue'
import InviteMemberModal from './InviteMemberModal.vue'

interface Props {
  projectId?: string
  projects?: Project[]
}

const props = withDefaults(defineProps<Props>(), {
  projects: () => []
})

const emit = defineEmits<{
  'team-created': [team: Team]
  'team-updated': [team: Team]
  'team-deleted': [teamId: string]
}>()

const authStore = useAuthStore()

// State
const loading = ref(false)
const teams = ref<Team[]>([])
const selectedTeam = ref<Team | null>(null)
const showMembersSidebar = ref(false)
const showCreateModal = ref(false)
const showInviteModal = ref(false)
const searchQuery = ref('')
const filterRole = ref('')

// Team members state
const teamMembers = ref<TeamMember[]>([])
const membersLoading = ref(false)

// Static role filter options derived from shared role definitions. This
// avoids hard-coding role values/labels in the template so that future
// dynamic role sources can plug in here instead.
const ROLE_FILTER_OPTIONS: { value: '' | TeamRole; label: string }[] = [
  { value: '', label: 'All Roles' },
  ...TEAM_ROLE_VALUES.map((role) => ({
    value: role,
    label: TEAM_ROLE_LABELS[role]
  }))
]

// Computed
const currentUserId = computed(() => authStore.user?.id || '')

const canCreateTeam = computed(() => {
  // Check if user has team management permissions
  return authStore.isAdmin || authStore.isEditor
})

const canAssignOwnerRole = computed(() => {
  // Only allow assigning owner role if current user is admin or owner
  return authStore.isAdmin
})

const filteredTeams = computed(() => {
  let filtered = teams.value

  // Filter by search query
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(team =>
      team.name.toLowerCase().includes(query) ||
      team.description.toLowerCase().includes(query)
    )
  }

  // Filter by role if needed
  if (filterRole.value) {
    // This would need to be implemented based on user's role in each team
    // For now, show all teams
  }

  return filtered
})

// Methods
function handleOpenCreateTeam() {
  // Safety guard: in case this is ever triggered while permissions are
  // still resolving, prevent opening the modal when the user lacks
  // create permissions.
  if (!canCreateTeam.value) return
  showCreateModal.value = true
}

function getUserTeamRole(teamId: string): TeamRole {
  const userId = currentUserId.value

  if (!userId) {
    // Not signed in or user not yet resolved – treat as viewer.
    return 'VIEWER'
  }

  // If we know the team, treat its creator as the OWNER even before
  // member records have been loaded. This ensures the role badge on
  // each TeamCard correctly reflects that the creator "owns" the team
  // without requiring an extra members API call.
  const team = teams.value.find((t) => t.id === teamId)
  if (team && team.createdBy === userId) {
    return 'OWNER'
  }

  // When we have members loaded for the currently selected team, prefer
  // that data so roles such as ADMIN / MEMBER are shown accurately.
  if (selectedTeam.value && selectedTeam.value.id === teamId) {
    const member = teamMembers.value.find(
      (m) => m.teamId === teamId && m.userId === userId && m.status === 'ACTIVE'
    )

    if (member) {
      return member.role
    }
  }

  // Safe default: treat unknown membership as a basic viewer.
  return 'VIEWER'
}

function canManageTeam(teamId: string): boolean {
  // A user can manage a team if:
  // - They have global permissions (admin/editor), or
  // - They are the creator/owner of that specific team.
  const userId = currentUserId.value

  if (!userId) {
    return false
  }

  // Global permission check (admins/editors)
  if (canCreateTeam.value) {
    return true
  }

  // Team‑level permission: treat the team creator as a manager
  const team = teams.value.find((t) => t.id === teamId)
  if (team && team.createdBy === userId) {
    return true
  }

  return false
}

function canInviteToTeam(teamId: string): boolean {
  // Check if user can invite members to this team
  return canManageTeam(teamId)
}

function handleEditTeam(team: Team) {
  // Open edit modal or navigate to edit page
  console.log('Edit team:', team)
}

function handleDeleteTeam(team: Team) {
  if (confirm(`Are you sure you want to delete the team "${team.name}"?`)) {
    deleteTeam(team.id)
  }
}

function handleViewMembers(team: Team) {
  selectedTeam.value = team
  showMembersSidebar.value = true
  loadTeamMembers(team.id)
}

function handleInviteMember(team: Team) {
  selectedTeam.value = team
  showInviteModal.value = true
}

// Wrapper for the TeamMemberList "invite-member" event.
// TeamMemberList emits this event without any payload (signature () => void),
// while the primary handler expects a Team argument. This function bridges
// that gap by using the currently selectedTeam when present.
function handleInviteMemberFromSidebar() {
  if (!selectedTeam.value) return
  handleInviteMember(selectedTeam.value)
}

function closeMembersSidebar() {
  showMembersSidebar.value = false
  selectedTeam.value = null
  teamMembers.value = []
}

async function handleCreateTeam(data: CreateTeamRequest) {
  try {
    loading.value = true
    const newTeam = await teamService.createTeam(data)
    teams.value.push(newTeam)
    emit('team-created', newTeam)
    showCreateModal.value = false
  } catch (error) {
    console.error('Failed to create team:', error)
    // Show error toast
  } finally {
    loading.value = false
  }
}

async function handleInviteMemberSubmit(data: InviteTeamMemberRequest, teamId: string) {
  try {
    await teamService.inviteTeamMember(teamId, data)
    // Refresh team members if sidebar is open
    if (selectedTeam.value?.id === teamId) {
      await loadTeamMembers(teamId)
    }
    showInviteModal.value = false
  } catch (error) {
    console.error('Failed to invite member:', error)
    // Show error toast
  }
}

async function handleUpdateMemberRole(member: TeamMember, role: TeamRole) {
  if (!selectedTeam.value) return

  try {
    await teamService.updateMemberRole(selectedTeam.value.id, member.userId, { role })
    // Update local state
    const index = teamMembers.value.findIndex(m => m.id === member.id)
    if (index !== -1) {
      teamMembers.value[index].role = role
    }
  } catch (error) {
    console.error('Failed to update member role:', error)
    // Show error toast
  }
}

async function handleRemoveMember(member: TeamMember) {
  if (!selectedTeam.value) return

  try {
    await teamService.removeTeamMember(selectedTeam.value.id, member.userId)
    // Remove from local state
    teamMembers.value = teamMembers.value.filter(m => m.id !== member.id)
  } catch (error) {
    console.error('Failed to remove member:', error)
    // Show error toast
  }
}

async function loadTeams() {
  try {
    loading.value = true
    if (props.projectId) {
      teams.value = await teamService.getTeamsByProject(props.projectId)
    } else {
      const response = await teamService.getTeams({ size: 100 })
      teams.value = response.content
    }
  } catch (error) {
    console.error('Failed to load teams:', error)
  } finally {
    loading.value = false
  }
}

async function loadTeamMembers(teamId: string) {
  try {
    membersLoading.value = true
    const members = await teamService.getTeamMembers(teamId, { size: 100 })
    teamMembers.value = members
  } catch (error) {
    console.error('Failed to load team members:', error)
  } finally {
    membersLoading.value = false
  }
}

async function deleteTeam(teamId: string) {
  try {
    await teamService.deleteTeam(teamId)
    teams.value = teams.value.filter(t => t.id !== teamId)
    emit('team-deleted', teamId)
  } catch (error) {
    console.error('Failed to delete team:', error)
    // Show error toast
  }
}

// Lifecycle
onMounted(() => {
  loadTeams()
})
</script>