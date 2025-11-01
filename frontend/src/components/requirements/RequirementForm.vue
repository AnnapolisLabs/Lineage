<template>
  <form @submit.prevent="$emit('submit')">
    <div class="space-y-5">
      <!-- Parent Info (if provided) -->
      <div v-if="parentRequirement">
        <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Parent</label>
        <div class="px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-annapolis-teal">
          {{ parentRequirement.reqId }} - {{ parentRequirement.title }}
        </div>
      </div>

      <!-- Title -->
      <div>
        <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Title *</label>
        <input
          :value="modelValue.title"
          @input="updateField('title', ($event.target as HTMLInputElement).value)"
          type="text"
          required
          class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
        />
      </div>

      <!-- Description -->
      <div>
        <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Description</label>
        <textarea
          :value="modelValue.description"
          @input="updateField('description', ($event.target as HTMLTextAreaElement).value)"
          rows="5"
          class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
        ></textarea>
      </div>

      <!-- Section -->
      <div>
        <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Section (Optional)</label>
        <input
          :value="modelValue.section"
          @input="updateField('section', ($event.target as HTMLInputElement).value)"
          type="text"
          placeholder="e.g., 1.1.1, 2.3.4"
          class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
        />
        <p class="mt-1 text-xs text-annapolis-gray-400">Optional hierarchical section number for organization</p>
      </div>

      <!-- Status and Priority -->
      <div class="grid grid-cols-2 gap-5">
        <div>
          <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Status</label>
          <select
            :value="modelValue.status"
            @change="updateField('status', ($event.target as HTMLSelectElement).value)"
            class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
          >
            <option value="DRAFT">Draft</option>
            <option value="REVIEW">Review</option>
            <option value="APPROVED">Approved</option>
            <option value="DEPRECATED">Deprecated</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Priority</label>
          <select
            :value="modelValue.priority"
            @change="updateField('priority', ($event.target as HTMLSelectElement).value)"
            class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
          >
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Form Actions -->
    <div class="mt-8 flex justify-end gap-3">
      <button
        type="button"
        @click="$emit('cancel')"
        class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white border border-annapolis-teal/20 rounded-lg hover:bg-annapolis-teal/10 transition-all duration-300"
      >
        Cancel
      </button>
      <button
        type="submit"
        class="px-8 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
      >
        {{ submitLabel }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { type Requirement } from '@/services/requirementService'

interface FormData {
  title: string
  description: string
  status: string
  priority: string
  section: string
}

interface Props {
  modelValue: FormData
  parentRequirement?: Requirement | null
  submitLabel?: string
}

const props = withDefaults(defineProps<Props>(), {
  parentRequirement: null,
  submitLabel: 'Submit'
})

const emit = defineEmits<{
  'update:modelValue': [value: FormData]
  submit: []
  cancel: []
}>()

function updateField(field: keyof FormData, value: string) {
  emit('update:modelValue', {
    ...props.modelValue,
    [field]: value
  } as FormData)
}
</script>
