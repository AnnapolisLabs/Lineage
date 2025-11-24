<template>
  <div v-if="authorized">
    <slot />
  </div>
  <div v-else-if="showFallback" class="opacity-50 pointer-events-none">
    <slot name="fallback" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { usePermissions } from '@/composables/usePermissions'

interface Props {
  permission: string
  resourceId: string
  showFallback?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showFallback: false
})

const emit = defineEmits<{
  authorized: []
  denied: []
  error: [error: string]
}>()

const { hasPermission, loading } = usePermissions()
const authorized = ref(false)

onMounted(async () => {
  try {
    authorized.value = await hasPermission(props.permission, props.resourceId)
    
    if (authorized.value) {
      emit('authorized')
    } else {
      emit('denied')
    }
  } catch (error: any) {
    const errorMsg = error.message || 'Permission check failed'
    emit('error', errorMsg)
  }
})
</script>