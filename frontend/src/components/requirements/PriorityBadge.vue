<template>
  <span :class="priorityClasses">
    {{ priority }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRequirementStyles } from '@/composables/useRequirementStyles'

interface Props {
  priority: string
}

const props = defineProps<Props>()
const { getPriorityClasses } = useRequirementStyles()

const priorityClasses = computed(() => getPriorityClasses(props.priority))
</script>
