<template>
  <div
    v-if="modelValue"
    class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50"
    @click.self="$emit('update:modelValue', false)"
  >
    <div class="bg-annapolis-charcoal rounded-lg px-8 py-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto shadow-2xl border border-annapolis-teal/30">
      <h3 class="text-xl font-semibold mb-6 text-white">{{ title }}</h3>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  modelValue: boolean
  title: string
}

defineProps<Props>()
defineEmits<{
  'update:modelValue': [value: boolean]
}>()
</script>
