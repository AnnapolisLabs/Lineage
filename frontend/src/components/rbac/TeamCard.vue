<template>
  <div class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 hover:border-annapolis-teal/40 hover:shadow-xl transition-all duration-300 overflow-hidden">
    <div class="p-6">
      <div class="flex justify-between items-start mb-4">
        <div class="flex-1">
          <div class="flex items-center gap-3 mb-2">
            <h3 class="text-lg font-semibold text-white">{{ team.name }}</h3>
            <RoleBadge :role="userRole" />
          </div>
          <p class="text-annapolis-gray-300 text-sm mb-3">{{ team.description }}</p>
          <div class="flex items-center gap-4 text-xs text-annapolis-gray-400">
            <span class="flex items-center gap-1">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
              </svg>
              {{ team.memberCount }} members
            </span>
            <span v-if="team.settings.require_peer_review" class="flex items-center gap-1">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Peer Review Required
            </span>
          </div>
        </div>
        
        <div class="flex items-center gap-2">
          <button
            v-if="canManage"
            @click="emit('edit', team)"
            class="p-2 text-annapolis-gray-300 hover:text-annapolis-teal transition-colors"
            title="Edit team"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </button>
          <button
            v-if="canManage"
            @click="emit('delete', team)"
            class="p-2 text-annapolis-gray-300 hover:text-red-400 transition-colors"
            title="Delete team"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
      
      <div class="flex justify-between items-center">
        <div class="text-xs text-annapolis-gray-400">
          Created {{ formatDate(team.createdAt) }}
        </div>
        <div class="flex gap-2">
          <button
            @click="emit('view-members', team)"
            class="inline-flex items-center px-3 py-1 border border-annapolis-teal/30 shadow-sm text-xs font-medium rounded-lg text-annapolis-teal bg-annapolis-teal/10 hover:bg-annapolis-teal/20 focus:outline-none focus:ring-2 focus:ring-annapolis-teal transition-all duration-300"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            Members
          </button>
          <button
            v-if="canInvite"
            @click="emit('invite-member', team)"
            class="inline-flex items-center px-3 py-1 border border-green-500/30 shadow-sm text-xs font-medium rounded-lg text-green-400 bg-green-500/10 hover:bg-green-500/20 focus:outline-none focus:ring-2 focus:ring-green-500 transition-all duration-300"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
            Add Member
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Team } from '@/types/rbac'
import RoleBadge from '@/components/rbac/RoleBadge.vue'

interface Props {
  team: Team
  userRole?: string
  canManage?: boolean
  canInvite?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  canManage: false,
  canInvite: false
})

const emit = defineEmits<{
  'edit': [team: Team]
  'delete': [team: Team]
  'view-members': [team: Team]
  'invite-member': [team: Team]
}>()

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  })
}
</script>