<template>
  <div class="flex items-center gap-1">
    <svg 
      v-if="authorized"
      class="w-4 h-4 text-annapolis-teal" 
      fill="none" 
      stroke="currentColor" 
      viewBox="0 0 24 24"
      :title="`Permission: ${permission}`"
    >
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
    <svg 
      v-else
      class="w-4 h-4 text-red-400" 
      fill="none" 
      stroke="currentColor" 
      viewBox="0 0 24 24"
      :title="`No permission: ${permission}`"
    >
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728L5.636 5.636m12.728 12.728L18.364 5.636M5.636 18.364l12.728-12.728" />
    </svg>
    <span 
      :class="[
        'text-xs font-medium',
        authorized ? 'text-annapolis-teal' : 'text-red-400'
      ]"
    >
      {{ permission.split('.').pop() }}
    </span>
  </div>
</template>

<script setup lang="ts">
interface Props {
  permission: string
  authorized: boolean
  showText?: boolean
}

defineProps<Props>()
</script>