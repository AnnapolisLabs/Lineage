<template>
  <span :class="statusClasses">
    {{ status }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRequirementStyles } from '@/composables/useRequirementStyles'

interface Props {
  status: string
}

const props = defineProps<Props>()
const { getStatusClasses } = useRequirementStyles()

const statusClasses = computed(() => getStatusClasses(props.status))
</script>
