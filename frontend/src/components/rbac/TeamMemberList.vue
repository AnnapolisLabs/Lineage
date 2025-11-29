<template>
  <div class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20">
    <div class="p-6">
      <div class="flex justify-between items-center mb-6">
        <div class="flex items-center gap-3">
          <h3 class="text-lg font-semibold text-white">Team Members</h3>
          <span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-annapolis-teal/20 text-annapolis-teal border border-annapolis-teal/30">
            {{ members.length }} members
          </span>
        </div>
        <button
          v-if="canManageMembers"
          @click="emit('invite-member')"
          class="inline-flex items-center px-4 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white text-sm font-medium rounded-lg transition-all duration-300"
        >
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Add Member
        </button>
      </div>

      <div v-if="loading" class="text-center py-8">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-annapolis-teal"></div>
        <p class="text-annapolis-gray-400 mt-2">Loading team members...</p>
      </div>

      <div v-else-if="members.length === 0" class="text-center py-8">
        <svg class="mx-auto h-12 w-12 text-annapolis-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
        </svg>
        <h4 class="text-lg font-medium text-white mb-2">No team members</h4>
        <p class="text-annapolis-gray-400 mb-4">Get started by adding your first team member</p>
        <button
          v-if="canManageMembers"
          @click="emit('invite-member')"
          class="inline-flex items-center px-4 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white text-sm font-medium rounded-lg transition-all duration-300"
        >
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Add First Member
        </button>
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="member in members"
          :key="member.id"
          class="flex items-center justify-between p-4 bg-annapolis-navy/30 rounded-lg border border-annapolis-teal/20 hover:border-annapolis-teal/30 transition-all duration-200"
        >
          <div class="flex items-center gap-4">
            <div class="flex-shrink-0">
              <div class="w-10 h-10 rounded-full bg-annapolis-teal/20 flex items-center justify-center">
                <span class="text-annapolis-teal font-semibold text-sm">
                  {{ getInitials(member.user?.name || member.user?.email || 'Unknown') }}
                </span>
              </div>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <p class="text-sm font-medium text-white truncate">
                  {{ member.user?.name || member.user?.email || 'Unnamed User' }}
                </p>
                <span
                  v-if="member.status === 'PENDING'"
                  class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-yellow-500/20 text-yellow-400 border border-yellow-500/30"
                >
                  Pending
                </span>
              </div>
              <p class="text-xs text-annapolis-gray-400 truncate">{{ member.user?.email || 'No email available' }}</p>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <RoleBadge :role="member.role" />
            
            <div v-if="canManageMembers && member.userId !== currentUserId" class="flex items-center gap-2">
              <select
                :value="member.role"
                @change="handleRoleChange(member, $event)"
                class="text-xs bg-annapolis-navy/50 border border-annapolis-teal/30 rounded px-2 py-1 text-white focus:outline-none focus:ring-1 focus:ring-annapolis-teal"
              >
                <option value="VIEWER">Viewer</option>
                <option value="MEMBER">Member</option>
                <option value="ADMIN">Admin</option>
                <option value="OWNER">Owner</option>
              </select>
              
              <button
                @click="handleRemoveMember(member)"
                class="p-1 text-red-400 hover:text-red-300 transition-colors"
                title="Remove member"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import type { TeamMember, TeamRole } from '@/types/rbac'
import RoleBadge from '@/components/rbac/RoleBadge.vue'

interface Props {
  // Members list is optional at the prop level but will default to
  // an empty array so the template can safely use `members.length`.
  members?: TeamMember[]

  // Loading can be omitted and will default to `false`.
  loading?: boolean

  // Permission-related props are required and must be provided by
  // the parent based on real auth/permission checks. We do NOT
  // supply defaults here to avoid silently treating the user as
  // having no permissions when the parent forgets to wire them.
  canManageMembers: boolean
  currentUserId: string
}

const props = withDefaults(defineProps<Props>(), {
  members: () => [],
  loading: false
})

// Use toRefs to keep props reactive when destructuring
const { members, loading, canManageMembers, currentUserId } = toRefs(props)

const emit = defineEmits<{
  'invite-member': []
  'update-role': [member: TeamMember, role: TeamRole]
  'remove-member': [member: TeamMember]
}>()

function getInitials(nameOrEmail: string): string {
  if (!nameOrEmail) return '??'
  
  // If it's an email, take the first letter of the username part
  if (nameOrEmail.includes('@')) {
    return nameOrEmail.charAt(0).toUpperCase()
  }
  
  // If it's a name, take first letter of first and last name
  const parts = nameOrEmail.split(' ').filter(Boolean)
  if (parts.length >= 2) {
    const firstInitial = parts[0]?.charAt(0) ?? ''
    const lastInitial = parts[parts.length - 1]?.charAt(0) ?? ''
    const initials = (firstInitial + lastInitial).toUpperCase()

    if (initials.trim().length > 0) {
      return initials
    }
  }
  
  return nameOrEmail.charAt(0).toUpperCase()
}

function handleRoleChange(member: TeamMember, event: Event) {
  const target = event.target as HTMLSelectElement
  const newRole = target.value as TeamRole
  
  if (newRole !== member.role) {
    emit('update-role', member, newRole)
  }
}

function handleRemoveMember(member: TeamMember) {
  const displayName = member.user?.name || member.user?.email || 'this member'
  if (confirm(`Remove ${displayName} from the team?`)) {
    emit('remove-member', member)
  }
}
</script>