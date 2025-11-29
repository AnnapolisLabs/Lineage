<template>
  <span
    :class="[
      'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border',
      roleClasses[role]
    ]"
    :title="`${role} role`"
  >
    {{ role }}
  </span>
</template>

<script setup lang="ts">
import type { TeamRole } from '@/types/rbac'

interface Props {
  role: TeamRole | string
}

const props = defineProps<Props>()

const roleClasses: Record<string, string> = {
  // Team roles
  OWNER: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
  ADMIN: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  MEMBER: 'bg-annapolis-teal/20 text-annapolis-teal border-annapolis-teal/30',
  VIEWER: 'bg-annapolis-gray-600/20 text-annapolis-gray-300 border-annapolis-gray-600/30',
  
  // Global roles
  ADMINISTRATOR: 'bg-red-500/20 text-red-400 border-red-500/30',
  USER: 'bg-annapolis-teal/20 text-annapolis-teal border-annapolis-teal/30'
}
</script>