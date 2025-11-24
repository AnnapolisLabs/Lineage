<template>
  <div
    v-if="modelValue"
    class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50"
    @click.self="$emit('update:modelValue', false)"
  >
    <div class="bg-annapolis-charcoal rounded-lg px-8 py-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto shadow-2xl border border-annapolis-teal/30">
      <div class="flex justify-between items-center mb-6">
        <h3 class="text-xl font-semibold text-white">Create New Team</h3>
        <button
          @click="$emit('update:modelValue', false)"
          class="text-annapolis-gray-400 hover:text-white transition-colors"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="space-y-6">
          <div>
            <label for="team-name" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Team Name *</label>
            <input
              id="team-name"
              v-model="formData.name"
              type="text"
              required
              class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              placeholder="Enter team name"
            />
          </div>

          <div>
            <label for="team-description" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Description</label>
            <textarea
              id="team-description"
              v-model="formData.description"
              rows="3"
              class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              placeholder="Describe the team's purpose"
            ></textarea>
          </div>

          <!-- Only show project selection if no projectId is provided -->
          <div v-if="!projectId">
            <label for="project-id" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Project *</label>
            <select
              id="project-id"
              v-model="formData.project_id"
              required
              class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
            >
              <option value="">Select a project</option>
              <option v-for="project in projects" :key="project.id" :value="project.id">
                {{ project.name }}
              </option>
            </select>
          </div>

          <div class="border-t border-annapolis-teal/20 pt-6">
            <h4 class="text-sm font-medium text-annapolis-gray-300 mb-4">Team Settings</h4>

            <div class="space-y-4">
              <div class="flex items-center justify-between">
                <div>
                  <label class="text-sm font-medium text-annapolis-gray-300">Require Peer Review</label>
                  <p class="text-xs text-annapolis-gray-400">Team members must complete peer reviews before task completion</p>
                </div>
                <button
                  type="button"
                  @click="formData.settings.require_peer_review = !formData.settings.require_peer_review"
                  :class="[
                    'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:ring-offset-2',
                    formData.settings.require_peer_review ? 'bg-annapolis-teal' : 'bg-annapolis-gray-600'
                  ]"
                >
                  <span
                    :class="[
                      'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                      formData.settings.require_peer_review ? 'translate-x-5' : 'translate-x-0'
                    ]"
                  ></span>
                </button>
              </div>

              <div>
                <label for="max-members" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Maximum Members</label>
                <input
                  id="max-members"
                  v-model.number="formData.settings.max_members"
                  type="number"
                  min="1"
                  max="50"
                  class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
                  placeholder="10"
                />
                <p class="mt-1 text-xs text-annapolis-gray-400">Limit the number of team members (1-50)</p>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-8 flex justify-end gap-3">
          <button
            type="button"
            @click="$emit('update:modelValue', false)"
            class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white border border-annapolis-teal/20 rounded-lg hover:bg-annapolis-teal/10 transition-all duration-300"
          >
            Cancel
          </button>
          <button
            type="submit"
            :disabled="loading"
            class="px-8 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 disabled:bg-annapolis-gray-600 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg disabled:transform-none disabled:cursor-not-allowed"
          >
            <span v-if="loading" class="flex items-center gap-2">
              <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
              Creating...
            </span>
            <span v-else>Create Team</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { CreateTeamRequest, Project } from '@/types/rbac'

interface Props {
  modelValue: boolean
  projects?: Project[]
  projectId?: string
}

const props = withDefaults(defineProps<Props>(), {
  projects: () => []
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'create': [data: CreateTeamRequest]
}>()

const loading = ref(false)

const formData = ref<CreateTeamRequest>({
  name: '',
  description: '',
  project_id: props.projectId || '',
  settings: {
    require_peer_review: false,
    max_members: 10
  }
})

// Reset form when modal closes or projectId changes
watch([() => props.modelValue, () => props.projectId], ([isOpen, newProjectId]) => {
  if (!isOpen) {
    formData.value = {
      name: '',
      description: '',
      project_id: newProjectId || '',
      settings: {
        require_peer_review: false,
        max_members: 10
      }
    }
  } else if (newProjectId) {
    formData.value.project_id = newProjectId
  }
})

async function handleSubmit() {
  if (!formData.value.name.trim() || !formData.value.project_id) {
    return
  }

  loading.value = true

  try {
    await emit('create', formData.value)
    emit('update:modelValue', false)
  } catch (error) {
    console.error('Failed to create team:', error)
  } finally {
    loading.value = false
  }
}
</script>