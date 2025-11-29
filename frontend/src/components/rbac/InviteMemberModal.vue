<template>
  <div
    v-if="modelValue"
    class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50"
    @click.self="$emit('update:modelValue', false)"
  >
    <div class="bg-annapolis-charcoal rounded-lg px-8 py-6 w-full max-w-lg shadow-2xl border border-annapolis-teal/30">
      <div class="flex justify-between items-center mb-6">
        <h3 class="text-xl font-semibold text-white">Add Team Member</h3>
        <button
          @click="$emit('update:modelValue', false)"
          class="text-annapolis-gray-400 hover:text-white transition-colors"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="space-y-6">
          <div>
            <label for="team-name-display" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Team</label>
            <div class="p-3 bg-annapolis-navy/30 rounded-lg border border-annapolis-teal/20">
              <span class="text-white font-medium">{{ team?.name || 'Loading...' }}</span>
            </div>
          </div>

          <div>
            <label for="invitee-email" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Email Address *</label>
            <input
              id="invitee-email"
              v-model="formData.email"
              type="email"
              required
              class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              placeholder="user@example.com"
            />
          </div>

          <div>
            <label for="member-role" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Role *</label>
            <select
              id="member-role"
              v-model="formData.role"
              required
              class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
            >
              <option value="VIEWER">Viewer - Read-only access</option>
              <option value="MEMBER">Member - Standard participation</option>
              <option value="ADMIN">Admin - Team administration</option>
              <option v-if="canAssignOwner" value="OWNER">Owner - Full team management</option>
            </select>
            <p class="mt-1 text-xs text-annapolis-gray-400">
              Choose the appropriate role for this team member
            </p>
          </div>

          <div>
            <label for="invitation-message" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Message (Optional)</label>
            <textarea
              id="invitation-message"
              v-model="formData.message"
              rows="3"
              class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              placeholder="Welcome to the team!"
            ></textarea>
          </div>
        </div>

        <div class="mt-8 flex justify-end gap-3">
          <button
            type="button"
            @click="$emit('update:modelValue', false)"
            class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white border border-annapolis-teal/20 rounded-lg hover:bg-annapolis-teal/10 transition-all duration-300"
          >
            Cancel
          </button>
          <button
            type="submit"
            :disabled="loading || !formData.email || !formData.role"
            class="px-8 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 disabled:bg-annapolis-gray-600 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg disabled:transform-none disabled:cursor-not-allowed"
          >
          <span v-if="loading" class="flex items-center gap-2">
            <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
              Adding...
            </span>
            <span v-else>Add Member</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Team, TeamRole, InviteTeamMemberRequest } from '@/types/rbac'

interface Props {
  modelValue: boolean
  team?: Team
  canAssignOwner?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  canAssignOwner: false
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'invite': [data: InviteTeamMemberRequest, teamId: string]
}>()

const loading = ref(false)

const formData = ref<InviteTeamMemberRequest>({
  email: '',
  role: 'MEMBER',
  message: ''
})

function handleSubmit() {
  if (!props.team || !formData.value.email || !formData.value.role) {
    return
  }

  loading.value = true
  
  try {
    emit('invite', formData.value, props.team.id)
    emit('update:modelValue', false)
    
    // Reset form
    formData.value = {
      email: '',
      role: 'MEMBER',
      message: ''
    }
  } catch (error) {
    console.error('Failed to invite team member:', error)
  } finally {
    loading.value = false
  }
}
</script>