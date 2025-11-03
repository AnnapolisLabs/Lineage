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
      <div v-else-if="requirement" class="flex gap-6">
        <!-- Tree View Sidebar -->
        <div
          v-if="allRequirements.length > 0"
          class="transition-all duration-300 flex-shrink-0"
        >
          <!-- Collapsed - small button -->
          <div
            v-if="!showTreeView"
            class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 overflow-hidden flex items-center justify-center"
            style="width: 48px; height: 42px;"
          >
            <button
              @click="showTreeView = true"
              class="p-2 text-annapolis-gray-300 hover:text-annapolis-teal transition-colors group"
              title="Show Tree View"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
          </div>

          <!-- Expanded - full height sidebar -->
          <div
            v-else
            class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 overflow-hidden flex flex-col"
            style="width: 320px; height: calc(100vh - 160px); position: sticky; top: 20px;"
          >
            <div class="flex flex-col h-full p-4">
              <div class="flex items-center justify-between mb-4 flex-shrink-0">
                <h3 class="text-sm font-semibold text-white uppercase tracking-wide">Requirements Tree</h3>
                <button
                  @click="showTreeView = false"
                  class="text-annapolis-gray-400 hover:text-annapolis-teal transition-colors"
                  title="Collapse Tree View"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                  </svg>
                </button>
              </div>
              <div class="flex-1 overflow-y-auto min-h-0">
                <RequirementTreeView
                  :requirements="allRequirements"
                  :requirement-links="allRequirementLinks"
                  :selected-id="requirementId"
                  :expanded="expandedNodes"
                  @navigate="handleTreeNavigate"
                  @toggle-expand="toggleNode"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Main Content -->
        <div class="flex-1 max-w-7xl space-y-6">
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
              <h2 class="text-2xl font-bold text-white mb-4">{{ requirement.title }}</h2>
              <p class="text-annapolis-gray-300 leading-relaxed mb-6">
                {{ requirement.description || 'No description provided' }}
              </p>
              <div class="flex items-center gap-3 flex-wrap">
                <StatusBadge :status="requirement.status" />
                <PriorityBadge :priority="requirement.priority" />
                <span v-if="requirement.section" class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-blue-500/20 text-blue-400 border border-blue-500/30">
                  Section {{ requirement.section }}
                </span>
                <button
                  @click="showLinksFiltered('incoming')"
                  class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-purple-500/20 text-purple-400 border border-purple-500/30 hover:bg-purple-500/30 hover:border-purple-500/50 transition-all cursor-pointer"
                  title="Click to view links to higher-level requirements (parents)"
                >
                  ↑ {{ requirement.inLinkCount || 0 }} In
                </button>
                <button
                  @click="showLinksFiltered('outgoing')"
                  class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-green-500/20 text-green-400 border border-green-500/30 hover:bg-green-500/30 hover:border-green-500/50 transition-all cursor-pointer"
                  title="Click to view links to lower-level requirements (children)"
                >
                  {{ requirement.outLinkCount || 0 }} Out ↓
                </button>
              </div>
            </div>
            <div class="flex items-center gap-3 ml-6">
              <button
                @click="showHistoryModal = true"
                class="inline-flex items-center px-4 py-2 border border-purple-500/30 shadow-sm text-sm font-medium rounded-lg text-purple-400 bg-purple-500/10 hover:bg-purple-500/20 focus:outline-none focus:ring-2 focus:ring-purple-500 transition-all duration-300"
                title="View version history"
              >
                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                History
              </button>
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
            <RequirementCard
              v-for="child in children"
              :key="child.id"
              :requirement="child"
              @click="navigateToChild(child.id)"
            />
          </div>
        </div>
        </div>
      </div>
    </main>

    <!-- Edit Modal -->
    <RequirementModal v-model="showEditModal" title="Edit Requirement">
      <RequirementForm
        v-model="formData"
        submit-label="Update"
        @submit="handleUpdateRequirement"
        @cancel="showEditModal = false"
      />
    </RequirementModal>

    <!-- Create Child Modal -->
    <RequirementModal v-model="showCreateModal" title="Create Child Requirement">
      <RequirementForm
        v-model="createFormData"
        :parent-requirement="requirement"
        submit-label="Create"
        @submit="handleCreateChild"
        @cancel="showCreateModal = false"
      />
    </RequirementModal>

    <!-- Link Management Modal -->
    <RequirementModal v-model="showLinkModal" title="Manage Requirement Links">
      <div class="space-y-4">
        <div class="bg-blue-500/10 border border-blue-500/30 rounded-lg p-3">
          <p class="text-sm text-blue-400">
            <svg class="w-4 h-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Links can only be created between requirements on different hierarchical levels.
          </p>
        </div>
        <div>
          <label for="link-requirement" class="block text-sm font-medium text-annapolis-gray-300 mb-2">
            Link to another requirement (Level {{ requirement?.level }})
          </label>
          <select
            id="link-requirement"
            v-model="selectedRequirementToLink"
            class="w-full px-4 py-2 bg-annapolis-navy border border-annapolis-gray-600 rounded-lg text-white focus:ring-2 focus:ring-annapolis-teal focus:border-transparent"
          >
            <option value="">Select a requirement...</option>
            <option
              v-for="req in availableRequirementsToLink"
              :key="req.id"
              :value="req.id"
            >
              {{ req.reqId }} (Level {{ req.level }}) - {{ req.title }}
            </option>
          </select>
        </div>
        <div class="flex justify-end gap-3">
          <button
            @click="showLinkModal = false"
            class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white transition-colors"
          >
            Cancel
          </button>
          <button
            @click="addLink"
            :disabled="!selectedRequirementToLink"
            class="px-6 py-2 bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-semibold rounded-lg transition-all duration-300 shadow-lg"
          >
            Add Link
          </button>
        </div>
      </div>
    </RequirementModal>

    <!-- History Modal -->
    <Transition name="fade">
      <div
        v-if="showHistoryModal"
        @click="showHistoryModal = false"
        class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
      >
        <div
          @click.stop
          class="bg-annapolis-charcoal rounded-xl shadow-2xl border border-annapolis-teal/30 w-full max-w-4xl max-h-[85vh] overflow-hidden flex flex-col"
        >
          <!-- Header -->
          <div class="flex justify-between items-center p-6 border-b border-annapolis-teal/20">
            <h2 class="text-2xl font-bold text-white">Version History</h2>
            <button
              @click="showHistoryModal = false"
              class="text-annapolis-gray-400 hover:text-white transition-colors"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Content -->
          <div class="overflow-y-auto p-6 flex-1">
            <RequirementHistory v-if="showHistoryModal" :requirement-id="requirementId" />
          </div>
        </div>
      </div>
    </Transition>

    <!-- Links Overlay Modal -->
    <Transition name="fade">
      <div
        v-if="showLinksOverlay"
        @click="showLinksOverlay = false"
        class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
      >
        <div
          @click.stop
          class="bg-annapolis-charcoal rounded-xl shadow-2xl border border-annapolis-teal/30 w-full max-w-3xl max-h-[80vh] overflow-hidden flex flex-col"
        >
          <!-- Header -->
          <div class="flex justify-between items-center p-6 border-b border-annapolis-teal/20">
            <div class="flex items-center gap-4">
              <h2 class="text-2xl font-bold text-white">Requirement Links</h2>
              <!-- Filter Buttons -->
              <div class="flex gap-2">
                <button
                  @click="linkDirectionFilter = 'all'"
                  class="px-3 py-1 rounded text-xs font-medium transition-all"
                  :class="linkDirectionFilter === 'all'
                    ? 'bg-annapolis-teal text-white'
                    : 'bg-annapolis-navy text-annapolis-gray-300 hover:bg-annapolis-navy/70'"
                >
                  All ({{ links.length }})
                </button>
                <button
                  @click="linkDirectionFilter = 'incoming'"
                  class="px-3 py-1 rounded text-xs font-medium transition-all"
                  :class="linkDirectionFilter === 'incoming'
                    ? 'bg-purple-500 text-white'
                    : 'bg-purple-500/20 text-purple-400 hover:bg-purple-500/30'"
                >
                  ↑ In ({{ links.filter(l => l.direction === 'incoming').length }})
                </button>
                <button
                  @click="linkDirectionFilter = 'outgoing'"
                  class="px-3 py-1 rounded text-xs font-medium transition-all"
                  :class="linkDirectionFilter === 'outgoing'
                    ? 'bg-green-500 text-white'
                    : 'bg-green-500/20 text-green-400 hover:bg-green-500/30'"
                >
                  Out ↓ ({{ links.filter(l => l.direction === 'outgoing').length }})
                </button>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <button
                @click="openLinkModal(); showLinksOverlay = false"
                class="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white text-sm font-semibold rounded-lg transition-all duration-300"
              >
                + Manage
              </button>
              <button
                @click="showLinksOverlay = false"
                class="text-annapolis-gray-400 hover:text-white transition-colors"
              >
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Content -->
          <div class="overflow-y-auto p-6 flex-1">
            <div v-if="links.length === 0" class="text-center py-16 text-annapolis-gray-400">
              <svg class="mx-auto h-16 w-16 text-annapolis-teal/30 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
              </svg>
              <p class="text-lg font-medium">No links to other requirements</p>
            </div>
            <div v-else-if="filteredLinks.length === 0" class="text-center py-16 text-annapolis-gray-400">
              <p class="text-lg font-medium">No {{ linkDirectionFilter }} links</p>
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="link in filteredLinks"
                :key="link.id"
                class="flex items-center justify-between p-4 bg-annapolis-navy/40 rounded-lg border border-annapolis-gray-600/40 hover:border-annapolis-teal/60 hover:bg-annapolis-navy/60 transition-all hover:shadow-lg group cursor-pointer"
                @click="navigateToRequirement(link.requirement.id)"
              >
                <div class="flex items-center gap-4 flex-1">
                  <span
                    class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-bold"
                    :class="link.direction === 'incoming' ? 'bg-purple-500/30 text-purple-300' : 'bg-green-500/30 text-green-300'"
                    :title="link.direction === 'incoming' ? 'Link from higher level (parent)' : 'Link to lower level (child)'"
                  >
                    {{ link.direction === 'incoming' ? '↑ In' : 'Out ↓' }}
                  </span>
                  <span class="text-sm text-annapolis-gray-400 font-semibold">L{{ link.requirement.level }}</span>
                  <span class="text-annapolis-teal font-mono text-base font-bold">
                    {{ link.requirement.reqId }}
                  </span>
                  <span class="text-white font-medium">{{ link.requirement.title }}</span>
                </div>
                <button
                  @click.stop="removeLink(link.id)"
                  class="text-red-400 hover:text-red-300 transition-colors opacity-0 group-hover:opacity-100"
                  title="Remove link"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { projectService, type Project } from '@/services/projectService'
import { requirementService, type Requirement, type RequirementLink } from '@/services/requirementService'
import StatusBadge from '@/components/requirements/StatusBadge.vue'
import PriorityBadge from '@/components/requirements/PriorityBadge.vue'
import RequirementCard from '@/components/requirements/RequirementCard.vue'
import RequirementModal from '@/components/requirements/RequirementModal.vue'
import RequirementForm from '@/components/requirements/RequirementForm.vue'
import RequirementTreeView from '@/components/RequirementTreeView.vue'
import RequirementHistory from '@/components/requirements/RequirementHistory.vue'
import { compareReqIds } from '@/utils/requirementSorting'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const projectId = computed(() => route.params.projectId as string)
const requirementId = computed(() => route.params.requirementId as string)

const project = ref<Project | null>(null)
const requirement = ref<Requirement | null>(null)
const children = ref<Requirement[]>([])
const allRequirements = ref<Requirement[]>([])
const links = ref<RequirementLink[]>([])
const loading = ref(true)

const showEditModal = ref(false)
const showCreateModal = ref(false)
const showLinkModal = ref(false)
const showLinksOverlay = ref(false)
const showHistoryModal = ref(false)
const linkDirectionFilter = ref<'all' | 'incoming' | 'outgoing'>('all')
const selectedRequirementToLink = ref('')

const showTreeView = ref(true)
const expandedNodes = ref(new Set<string>())
const allRequirementLinks = ref<any[]>([])

const formData = ref({
  title: '',
  description: '',
  status: 'DRAFT',
  priority: 'MEDIUM',
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

const filteredLinks = computed(() => {
  if (linkDirectionFilter.value === 'all') {
    return links.value
  }
  return links.value.filter(link => link.direction === linkDirectionFilter.value)
})

const availableRequirementsToLink = computed(() => {
  if (!requirement.value) return []

  // Filter out current requirement, already linked requirements, and same-level requirements
  const linkedIds = new Set(links.value.map(link => link.requirement.id))
  return allRequirements.value.filter(
    req => req.id !== requirement.value?.id &&
           !linkedIds.has(req.id) &&
           req.level !== requirement.value?.level
  )
})

onMounted(async () => {
  await loadData()

  // Check if we should auto-open links overlay
  const showLinks = route.query.showLinks as string | undefined
  if (showLinks === 'incoming' || showLinks === 'outgoing') {
    linkDirectionFilter.value = showLinks
    showLinksOverlay.value = true
  }
})

// Watch for route changes to reload data when navigating between requirements
watch(() => route.params.requirementId, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    await loadData()
  }
})

async function loadData() {
  loading.value = true
  try {
    project.value = await projectService.getById(projectId.value)
    allRequirements.value = await requirementService.getByProject(projectId.value)
    requirement.value = allRequirements.value.find(r => r.id === requirementId.value) || null

    if (requirement.value) {
      // Load links first
      links.value = await requirementService.getLinks(requirement.value.id)

      // Find children based on OUT links (lower level requirements)
      const childIds = new Set(
        links.value
          .filter(link => link.direction === 'outgoing')
          .map(link => link.requirement.id)
      )
      children.value = allRequirements.value
        .filter(r => childIds.has(r.id))
        .sort((a, b) => compareReqIds(a.reqId, b.reqId))
    }

    // Load all requirement links for tree view
    const linkPromises = allRequirements.value.map(req =>
      requirementService.getLinks(req.id).catch(err => {
        console.error(`Failed to load links for ${req.reqId}:`, err)
        return []
      })
    )
    const allLinks = await Promise.all(linkPromises)

    // Flatten and store all links with their source requirement ID
    allRequirementLinks.value = allLinks.flatMap((links, index) => {
      const sourceReqId = allRequirements.value[index].id
      return links.map(link => ({
        ...link,
        sourceRequirementId: sourceReqId
      }))
    })

    // Auto-expand parent nodes to show current requirement
    if (requirement.value) {
      expandParentNodes(requirement.value.id)
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
      parentId: requirement.value.parentId,
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

function openLinkModal() {
  selectedRequirementToLink.value = ''
  showLinkModal.value = true
}

async function addLink() {
  if (!requirement.value || !selectedRequirementToLink.value) return

  try {
    await requirementService.createLink(requirement.value.id, selectedRequirementToLink.value)
    showLinkModal.value = false
    selectedRequirementToLink.value = ''
    await loadData()
  } catch (err) {
    console.error('Failed to create link:', err)
    alert('Failed to create link')
  }
}

async function removeLink(linkId: string) {
  if (!confirm('Are you sure you want to remove this link?')) return

  try {
    await requirementService.deleteLink(linkId)
    await loadData()
  } catch (err) {
    console.error('Failed to delete link:', err)
    alert('Failed to delete link')
  }
}

function showLinksFiltered(direction: 'incoming' | 'outgoing') {
  linkDirectionFilter.value = direction
  showLinksOverlay.value = true
}

function navigateToRequirement(requirementId: string) {
  showLinksOverlay.value = false
  router.push(`/projects/${projectId.value}/requirements/${requirementId}`)
}

function handleTreeNavigate(req: Requirement) {
  router.push(`/projects/${projectId.value}/requirements/${req.id}`)
}

function toggleNode(id: string) {
  if (expandedNodes.value.has(id)) {
    expandedNodes.value.delete(id)
  } else {
    expandedNodes.value.add(id)
  }
}

function expandParentNodes(reqId: string) {
  const req = allRequirements.value.find(r => r.id === reqId)
  if (!req) return

  // Expand all parent nodes
  let current = req
  while (current.parentId) {
    expandedNodes.value.add(current.parentId)
    const parent = allRequirements.value.find(r => r.id === current.parentId)
    if (!parent) break
    current = parent
  }

  // Also expand nodes with outgoing links to this requirement
  for (const link of allRequirementLinks.value) {
    if (link.requirement.id === reqId && link.direction === 'outgoing') {
      expandedNodes.value.add(link.sourceRequirementId)
    }
  }
}

</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
