<template>
  <div
    class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 hover:border-annapolis-teal/40 hover:shadow-xl transition-all duration-300 overflow-hidden cursor-pointer"
    @click="$emit('click')"
  >
    <div class="p-6">
      <div class="flex justify-between items-start">
        <div class="flex-1">
          <div class="flex items-center gap-3 mb-3">
            <span class="inline-flex items-center px-3 py-1 rounded-lg text-sm font-mono font-medium bg-annapolis-teal/20 text-annapolis-teal border border-annapolis-teal/30">
              {{ requirement.reqId }}
            </span>
            <span class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-annapolis-gray-600/20 text-annapolis-gray-300 border border-annapolis-gray-600/30">
              Level {{ requirement.level }}
            </span>
            <h3 class="text-lg font-semibold text-white">{{ requirement.title }}</h3>
          </div>
          <p class="text-annapolis-gray-300 leading-relaxed mb-4">
            {{ requirement.description || 'No description provided' }}
          </p>
          <div class="flex items-center gap-3">
            <StatusBadge :status="requirement.status" />
            <PriorityBadge :priority="requirement.priority" />
          </div>
        </div>
        <div class="ml-6">
          <svg class="w-6 h-6 text-annapolis-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type Requirement } from '@/services/requirementService'
import StatusBadge from './StatusBadge.vue'
import PriorityBadge from './PriorityBadge.vue'

interface Props {
  requirement: Requirement
}

defineProps<Props>()
defineEmits<{
  click: []
}>()
</script>
