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
          :requirement-links="requirementLinks"
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
import type { Requirement, RequirementLink } from '@/services/requirementService'
import { compareReqIds } from '@/utils/requirementSorting'

interface Props {
  requirements: Requirement[]
  requirementLinks?: RequirementLink[]
  parentId?: string | null
  selectedId?: string | null
  expanded: Set<string>
}

const props = withDefaults(defineProps<Props>(), {
  requirementLinks: () => [],
  parentId: null,
  selectedId: null
})

const emit = defineEmits<{
  navigate: [requirement: Requirement]
  toggleExpand: [id: string]
}>()

const rootRequirements = computed(() => {
  let results: Requirement[]

  if (props.parentId === null) {
    // Top-level requirements (no parent)
    results = props.requirements.filter(req => !req.parentId)
  } else {
    // Find children: either direct children via parentId OR linked children via outgoing links
    const childrenViaParent = props.requirements.filter(req => req.parentId === props.parentId)

    // Find linked children (outgoing links from this parent)
    // An outgoing link from the parent points to a child requirement
    const linkedChildIds = new Set(
      (props.requirementLinks || [])
        .filter(link => link.sourceRequirementId === props.parentId && link.direction === 'outgoing')
        .map(link => link.requirement.id)
    )

    const linkedChildren = props.requirements.filter(req => linkedChildIds.has(req.id))

    // Combine both and remove duplicates
    const allChildrenMap = new Map<string, Requirement>()
    for (const req of childrenViaParent) {
      allChildrenMap.set(req.id, req)
    }
    for (const req of linkedChildren) {
      allChildrenMap.set(req.id, req)
    }

    results = Array.from(allChildrenMap.values())
  }

  // Sort by requirement ID (natural number ordering)
  return results.sort((a, b) => compareReqIds(a.reqId, b.reqId))
})

function hasChildren(reqId: string): boolean {
  // Check for direct children
  const hasDirectChildren = props.requirements.some(req => req.parentId === reqId)
  if (hasDirectChildren) return true

  // Check for linked children (outgoing links from this requirement)
  const hasLinkedChildren = (props.requirementLinks || []).some(
    link => link.sourceRequirementId === reqId && link.direction === 'outgoing'
  )
  return hasLinkedChildren
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
