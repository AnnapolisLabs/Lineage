<template>
  <div class="requirement-tree">
    <div
      v-for="req in rootRequirements"
      :key="req.id"
      class="tree-node"
    >
      <div
        :class="[
          'tree-item flex items-center gap-2 px-3 py-2 rounded-lg cursor-pointer transition-all',
          selectedId === req.id ? 'bg-annapolis-teal/20 text-annapolis-teal' : 'text-annapolis-gray-300 hover:bg-annapolis-charcoal/50'
        ]"
        @click="$emit('navigate', req)"
      >
        <button
          v-if="hasChildren(req.id)"
          @click.stop="toggleExpand(req.id)"
          class="w-5 h-5 flex items-center justify-center hover:text-annapolis-teal transition-colors"
        >
          <svg
            class="w-4 h-4 transition-transform"
            :class="{ 'rotate-90': expanded.has(req.id) }"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>
        <span v-else class="w-5"></span>

        <span class="text-xs font-mono">{{ req.reqId }}</span>
        <span class="text-sm flex-1 truncate">{{ req.title }}</span>

        <span
          v-if="req.priority === 'CRITICAL' || req.priority === 'HIGH'"
          :class="[
            'w-2 h-2 rounded-full',
            req.priority === 'CRITICAL' ? 'bg-red-500' : 'bg-orange-500'
          ]"
        ></span>
      </div>

      <div
        v-if="expanded.has(req.id) && hasChildren(req.id)"
        class="ml-6 mt-1 border-l border-annapolis-teal/20 pl-2"
      >
        <RequirementTreeView
          :requirements="requirements"
          :parent-id="req.id"
          :selected-id="selectedId"
          :expanded="expanded"
          @navigate="$emit('navigate', $event)"
          @toggle-expand="toggleExpand"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Requirement } from '@/services/requirementService'

interface Props {
  requirements: Requirement[]
  parentId?: string | null
  selectedId?: string | null
  expanded: Set<string>
}

const props = withDefaults(defineProps<Props>(), {
  parentId: null,
  selectedId: null
})

const emit = defineEmits<{
  navigate: [requirement: Requirement]
  toggleExpand: [id: string]
}>()

const rootRequirements = computed(() => {
  return props.requirements.filter(req => {
    if (props.parentId === null) {
      return !req.parentId
    }
    return req.parentId === props.parentId
  })
})

function hasChildren(reqId: string): boolean {
  return props.requirements.some(req => req.parentId === reqId)
}

function toggleExpand(id: string) {
  emit('toggleExpand', id)
}
</script>

<style scoped>
.tree-node {
  user-select: none;
}
</style>
