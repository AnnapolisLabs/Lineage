<template>
  <div class="min-h-screen bg-gradient-to-br from-annapolis-navy via-annapolis-charcoal to-annapolis-teal-dark">
    <!-- Header -->
    <header class="bg-annapolis-charcoal/80 backdrop-blur-sm shadow-lg border-b border-annapolis-teal/20">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <div class="flex items-center gap-6">
            <button @click="goBack" class="text-annapolis-gray-300 hover:text-annapolis-teal transition-colors duration-300 flex items-center gap-2">
              <span class="text-xl">←</span>
              <span>Back</span>
            </button>
            <div class="h-6 w-px bg-annapolis-teal/30"></div>
            <div class="flex items-center gap-3">
              <span v-if="requirement" class="px-3 py-1 text-sm font-mono bg-annapolis-teal/20 text-annapolis-teal rounded-lg border border-annapolis-teal/30">
                {{ requirement.reqId }}
              </span>
              <span v-if="requirement" class="px-2 py-1 text-xs font-medium bg-annapolis-gray-600/20 text-annapolis-gray-300 rounded border border-annapolis-gray-600/30">
                Level {{ requirement.level }}
              </span>
              <h1 class="text-2xl font-bold text-white">
                {{ requirement?.title || 'Loading...' }}
              </h1>
            </div>
          </div>
          <button
            @click="handleLogout"
            class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-annapolis-teal transition-colors duration-300"
          >
            Logout
          </button>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="w-full px-4 sm:px-6 lg:px-8 py-8">
      <!-- Loading -->
      <div v-if="loading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-annapolis-teal"></div>
      </div>

      <!-- Content -->
      <div v-else-if="requirement" class="max-w-7xl mx-auto space-y-6">
        <!-- Breadcrumb -->
        <nav class="flex items-center gap-2 text-sm text-annapolis-gray-300">
          <router-link
            :to="`/projects/${projectId}`"
            class="hover:text-annapolis-teal transition-colors"
          >
            {{ project?.name || 'Project' }}
          </router-link>
          <span v-for="ancestor in breadcrumb" :key="ancestor.id">
            <span class="text-annapolis-gray-600">/</span>
            <router-link
              :to="`/projects/${projectId}/requirements/${ancestor.id}`"
              class="hover:text-annapolis-teal transition-colors ml-2"
            >
              {{ ancestor.reqId }}
            </router-link>
          </span>
          <span class="text-annapolis-gray-600">/</span>
          <span class="text-annapolis-teal ml-2">{{ requirement.reqId }}</span>
        </nav>

        <!-- Requirement Details Card -->
        <div class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 p-8">
          <div class="flex justify-between items-start mb-6">
            <div class="flex-1">
              <p class="text-annapolis-gray-300 leading-relaxed mb-6">
                {{ requirement.description || 'No description provided' }}
              </p>
              <div class="flex items-center gap-3">
                <span :class="[
                  'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border',
                  requirement.status === 'APPROVED' ? 'bg-green-500/20 text-green-400 border-green-500/30' :
                  requirement.status === 'REVIEW' ? 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30' :
                  requirement.status === 'DEPRECATED' ? 'bg-red-500/20 text-red-400 border-red-500/30' :
                  'bg-annapolis-gray-600/20 text-annapolis-gray-300 border-annapolis-gray-600/30'
                ]">
                  {{ requirement.status }}
                </span>
                <span :class="[
                  'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border',
                  requirement.priority === 'CRITICAL' ? 'bg-red-500/20 text-red-400 border-red-500/30' :
                  requirement.priority === 'HIGH' ? 'bg-orange-500/20 text-orange-400 border-orange-500/30' :
                  requirement.priority === 'MEDIUM' ? 'bg-annapolis-teal/20 text-annapolis-teal border-annapolis-teal/30' :
                  'bg-annapolis-gray-600/20 text-annapolis-gray-300 border-annapolis-gray-600/30'
                ]">
                  {{ requirement.priority }}
                </span>
                <span v-if="requirement.section" class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-blue-500/20 text-blue-400 border border-blue-500/30">
                  Section {{ requirement.section }}
                </span>
              </div>
            </div>
            <div class="flex items-center gap-3 ml-6">
              <button
                @click="openEditModal"
                class="inline-flex items-center px-4 py-2 border border-annapolis-teal/30 shadow-sm text-sm font-medium rounded-lg text-annapolis-teal bg-annapolis-teal/10 hover:bg-annapolis-teal/20 focus:outline-none focus:ring-2 focus:ring-annapolis-teal transition-all duration-300"
              >
                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
                Edit
              </button>
              <button
                @click="deleteRequirement"
                class="inline-flex items-center px-4 py-2 border border-red-500/30 shadow-sm text-sm font-medium rounded-lg text-red-400 bg-red-500/10 hover:bg-red-500/20 focus:outline-none focus:ring-2 focus:ring-red-500 transition-all duration-300"
              >
                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
                Delete
              </button>
            </div>
          </div>
        </div>

        <!-- Child Requirements Section -->
        <div class="space-y-4">
          <div class="flex justify-between items-center">
            <h2 class="text-xl font-semibold text-white">
              Child Requirements
              <span v-if="children.length > 0" class="ml-2 text-sm text-annapolis-gray-400">({{ children.length }})</span>
            </h2>
            <button
              @click="openCreateModal"
              class="px-6 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
            >
              + Add Child Requirement
            </button>
          </div>

          <!-- No children -->
          <div v-if="children.length === 0" class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 text-center py-16">
            <svg class="mx-auto h-12 w-12 text-annapolis-teal mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <h3 class="text-lg font-medium text-white mb-2">No child requirements</h3>
            <p class="text-annapolis-gray-300 mb-6">Add child requirements to break down this requirement</p>
            <button
              @click="openCreateModal"
              class="inline-flex items-center px-8 py-3 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
            >
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              Add Child
            </button>
          </div>

          <!-- Children List -->
          <div v-else class="grid gap-4">
            <div
              v-for="child in children"
              :key="child.id"
              class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 hover:border-annapolis-teal/40 hover:shadow-xl transition-all duration-300 overflow-hidden cursor-pointer"
              @click="navigateToChild(child.id)"
            >
              <div class="p-6">
                <div class="flex justify-between items-start">
                  <div class="flex-1">
                    <div class="flex items-center gap-3 mb-3">
                      <span class="inline-flex items-center px-3 py-1 rounded-lg text-sm font-mono font-medium bg-annapolis-teal/20 text-annapolis-teal border border-annapolis-teal/30">
                        {{ child.reqId }}
                      </span>
                      <span class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-annapolis-gray-600/20 text-annapolis-gray-300 border border-annapolis-gray-600/30">
                        Level {{ child.level }}
                      </span>
                      <h3 class="text-lg font-semibold text-white">{{ child.title }}</h3>
                    </div>
                    <p class="text-annapolis-gray-300 leading-relaxed mb-4">{{ child.description || 'No description provided' }}</p>
                    <div class="flex items-center gap-3">
                      <span :class="[
                        'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border',
                        child.status === 'APPROVED' ? 'bg-green-500/20 text-green-400 border-green-500/30' :
                        child.status === 'REVIEW' ? 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30' :
                        child.status === 'DEPRECATED' ? 'bg-red-500/20 text-red-400 border-red-500/30' :
                        'bg-annapolis-gray-600/20 text-annapolis-gray-300 border-annapolis-gray-600/30'
                      ]">
                        {{ child.status }}
                      </span>
                      <span :class="[
                        'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border',
                        child.priority === 'CRITICAL' ? 'bg-red-500/20 text-red-400 border-red-500/30' :
                        child.priority === 'HIGH' ? 'bg-orange-500/20 text-orange-400 border-orange-500/30' :
                        child.priority === 'MEDIUM' ? 'bg-annapolis-teal/20 text-annapolis-teal border-annapolis-teal/30' :
                        'bg-annapolis-gray-600/20 text-annapolis-gray-300 border-annapolis-gray-600/30'
                      ]">
                        {{ child.priority }}
                      </span>
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
          </div>
        </div>
      </div>
    </main>

    <!-- Edit Modal -->
    <div
      v-if="showEditModal"
      class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50"
      @click.self="showEditModal = false"
    >
      <div class="bg-annapolis-charcoal rounded-lg px-8 py-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto shadow-2xl border border-annapolis-teal/30">
        <h3 class="text-xl font-semibold mb-6 text-white">Edit Requirement</h3>
        <form @submit.prevent="handleUpdateRequirement">
          <div class="space-y-5">
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Title *</label>
              <input
                v-model="formData.title"
                type="text"
                required
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Description</label>
              <textarea
                v-model="formData.description"
                rows="5"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Section (Optional)</label>
              <input
                v-model="formData.section"
                type="text"
                placeholder="e.g., 1.1.1, 2.3.4"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              />
              <p class="mt-1 text-xs text-annapolis-gray-400">Optional hierarchical section number for organization</p>
            </div>
            <div class="grid grid-cols-2 gap-5">
              <div>
                <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Status</label>
                <select
                  v-model="formData.status"
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
                  v-model="formData.priority"
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
          <div class="mt-8 flex justify-end gap-3">
            <button
              type="button"
              @click="showEditModal = false"
              class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white border border-annapolis-teal/20 rounded-lg hover:bg-annapolis-teal/10 transition-all duration-300"
            >
              Cancel
            </button>
            <button
              type="submit"
              class="px-8 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
            >
              Update
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Create Child Modal -->
    <div
      v-if="showCreateModal"
      class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50"
      @click.self="showCreateModal = false"
    >
      <div class="bg-annapolis-charcoal rounded-lg px-8 py-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto shadow-2xl border border-annapolis-teal/30">
        <h3 class="text-xl font-semibold mb-6 text-white">Create Child Requirement</h3>
        <form @submit.prevent="handleCreateChild">
          <div class="space-y-5">
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Parent</label>
              <div class="px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-annapolis-teal">
                {{ requirement?.reqId }} - {{ requirement?.title }}
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Title *</label>
              <input
                v-model="createFormData.title"
                type="text"
                required
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Description</label>
              <textarea
                v-model="createFormData.description"
                rows="5"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Section (Optional)</label>
              <input
                v-model="createFormData.section"
                type="text"
                placeholder="e.g., 1.1.1, 2.3.4"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              />
              <p class="mt-1 text-xs text-annapolis-gray-400">Optional hierarchical section number for organization</p>
            </div>
            <div class="grid grid-cols-2 gap-5">
              <div>
                <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Status</label>
                <select
                  v-model="createFormData.status"
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
                  v-model="createFormData.priority"
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
          <div class="mt-8 flex justify-end gap-3">
            <button
              type="button"
              @click="showCreateModal = false"
              class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white border border-annapolis-teal/20 rounded-lg hover:bg-annapolis-teal/10 transition-all duration-300"
            >
              Cancel
            </button>
            <button
              type="submit"
              class="px-8 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
            >
              Create
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { projectService, type Project } from '@/services/projectService'
import { requirementService, type Requirement } from '@/services/requirementService'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const projectId = computed(() => route.params.projectId as string)
const requirementId = computed(() => route.params.requirementId as string)

const project = ref<Project | null>(null)
const requirement = ref<Requirement | null>(null)
const children = ref<Requirement[]>([])
const allRequirements = ref<Requirement[]>([])
const loading = ref(true)

const showEditModal = ref(false)
const showCreateModal = ref(false)

const formData = ref({
  title: '',
  description: '',
  status: 'DRAFT',
  priority: 'MEDIUM',
  parentId: null as string | null,
  section: ''
})

const createFormData = ref({
  title: '',
  description: '',
  status: 'DRAFT',
  priority: 'MEDIUM',
  section: ''
})

const breadcrumb = computed(() => {
  if (!requirement.value || !allRequirements.value.length) return []

  const ancestors: Requirement[] = []
  let current = requirement.value

  // Walk up the parent chain
  while (current.parentId) {
    const parent = allRequirements.value.find(r => r.id === current.parentId)
    if (!parent) break
    ancestors.unshift(parent)
    current = parent
  }

  return ancestors
})

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    project.value = await projectService.getById(projectId.value)
    allRequirements.value = await requirementService.getByProject(projectId.value)
    requirement.value = allRequirements.value.find(r => r.id === requirementId.value) || null

    if (requirement.value) {
      // Find direct children
      children.value = allRequirements.value.filter(r => r.parentId === requirement.value?.id)
    }
  } catch (err) {
    console.error('Failed to load data:', err)
  } finally {
    loading.value = false
  }
}

function goBack() {
  if (requirement.value?.parentId) {
    router.push(`/projects/${projectId.value}/requirements/${requirement.value.parentId}`)
  } else {
    router.push(`/projects/${projectId.value}`)
  }
}

function navigateToChild(childId: string) {
  router.push(`/projects/${projectId.value}/requirements/${childId}`)
}

function openEditModal() {
  if (!requirement.value) return
  formData.value = {
    title: requirement.value.title,
    description: requirement.value.description || '',
    status: requirement.value.status,
    priority: requirement.value.priority,
    parentId: requirement.value.parentId || null,
    section: requirement.value.section || ''
  }
  showEditModal.value = true
}

function openCreateModal() {
  createFormData.value = {
    title: '',
    description: '',
    status: 'DRAFT',
    priority: 'MEDIUM',
    section: ''
  }
  showCreateModal.value = true
}

async function handleUpdateRequirement() {
  if (!requirement.value) return

  try {
    await requirementService.update(requirement.value.id, {
      title: formData.value.title,
      description: formData.value.description,
      status: formData.value.status,
      priority: formData.value.priority,
      parentId: formData.value.parentId,
      customFields: {}
    })
    showEditModal.value = false
    await loadData()
  } catch (err) {
    console.error('Failed to update requirement:', err)
    alert('Failed to update requirement')
  }
}

async function handleCreateChild() {
  if (!requirement.value) return

  try {
    await requirementService.create(projectId.value, {
      title: createFormData.value.title,
      description: createFormData.value.description,
      status: createFormData.value.status,
      priority: createFormData.value.priority,
      parentId: requirement.value.id,
      customFields: {}
    })
    showCreateModal.value = false
    await loadData()
  } catch (err) {
    console.error('Failed to create child requirement:', err)
    alert('Failed to create child requirement')
  }
}

async function deleteRequirement() {
  if (!requirement.value || !confirm('Are you sure you want to delete this requirement? This will also delete all child requirements.')) {
    return
  }

  try {
    await requirementService.delete(requirement.value.id)
    goBack()
  } catch (err) {
    console.error('Failed to delete requirement:', err)
    alert('Failed to delete requirement')
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
