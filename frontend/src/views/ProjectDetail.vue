<template>
  <div class="min-h-screen bg-gradient-to-br from-annapolis-navy via-annapolis-charcoal to-annapolis-teal-dark">
    <!-- Header -->
    <header class="bg-annapolis-charcoal/80 backdrop-blur-sm shadow-lg border-b border-annapolis-teal/20">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <div class="flex items-center gap-6">
            <button @click="router.push('/projects')" class="text-annapolis-gray-300 hover:text-annapolis-teal transition-colors duration-300 flex items-center gap-2">
              <span class="text-xl">←</span>
              <span>Back</span>
            </button>
            <div class="h-6 w-px bg-annapolis-teal/30"></div>
            <h1 class="text-2xl font-bold text-white">
              {{ project?.name || 'Loading...' }}
            </h1>
            <span v-if="project" class="px-3 py-1 text-xs font-mono bg-annapolis-teal/20 text-annapolis-teal rounded-lg border border-annapolis-teal/30">
              {{ project.projectKey }}
            </span>
          </div>
          <div class="flex items-center gap-3">
            <button
              @click="router.push(`/projects/${projectId}/settings`)"
              class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-annapolis-teal transition-colors duration-300 flex items-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              Settings
            </button>
            <button
              @click="handleLogout"
              class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-annapolis-teal transition-colors duration-300"
            >
              Logout
            </button>
          </div>
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
      <div v-else-if="project" class="flex gap-6">
        <!-- Tree View Sidebar -->
        <div
          v-if="allRequirements.length > 0"
          class="transition-all duration-300 flex-shrink-0"
        >
          <!-- Collapsed - small button inline with search -->
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
                  :selected-id="selectedRequirement?.id"
                  :expanded="expandedNodes"
                  @navigate="handleTreeNavigate"
                  @toggle-expand="toggleNode"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Main Content -->
        <div class="flex-1 space-y-6">
        <!-- Actions Bar -->
        <div class="flex justify-between items-center">
          <div class="flex gap-2">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search requirements..."
              class="px-4 py-2 bg-annapolis-charcoal/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              @input="handleSearch"
            />
            <select
              v-model="filterStatus"
              class="px-4 py-2 bg-annapolis-charcoal/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              @change="handleSearch"
            >
              <option value="">All Status</option>
              <option value="DRAFT">Draft</option>
              <option value="REVIEW">Review</option>
              <option value="APPROVED">Approved</option>
              <option value="DEPRECATED">Deprecated</option>
            </select>
            <select
              v-model="filterPriority"
              class="px-4 py-2 bg-annapolis-charcoal/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              @change="handleSearch"
            >
              <option value="">All Priority</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
            <select
              v-model="filterLevel"
              class="px-4 py-2 bg-annapolis-charcoal/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              @change="handleSearch"
            >
              <option value="">All Levels</option>
              <option
                v-for="(prefix, level) in project?.levelPrefixes"
                :key="level"
                :value="level"
              >
                L{{ level }} - {{ prefix }}
              </option>
            </select>
          </div>
          <div class="flex space-x-2">
            <button
              @click="showExportMenu = !showExportMenu"
              class="relative px-4 py-2 bg-annapolis-charcoal/70 text-annapolis-gray-300 rounded-lg hover:bg-annapolis-charcoal hover:text-annapolis-teal border border-annapolis-teal/20 transition-all duration-300"
            >
              Export ▾
              <div
                v-if="showExportMenu"
                class="absolute right-0 mt-2 w-48 bg-annapolis-charcoal rounded-lg shadow-lg z-10 border border-annapolis-teal/30"
              >
                <a
                  :href="`/api/projects/${projectId}/export/csv`"
                  class="block px-4 py-2 text-sm text-annapolis-gray-300 hover:bg-annapolis-teal/20 hover:text-annapolis-teal first:rounded-t-lg transition-colors"
                  download
                >
                  Export as CSV
                </a>
                <a
                  :href="`/api/projects/${projectId}/export/json`"
                  class="block px-4 py-2 text-sm text-annapolis-gray-300 hover:bg-annapolis-teal/20 hover:text-annapolis-teal transition-colors"
                  download
                >
                  Export as JSON
                </a>
                <a
                  :href="`/api/projects/${projectId}/export/markdown`"
                  class="block px-4 py-2 text-sm text-annapolis-gray-300 hover:bg-annapolis-teal/20 hover:text-annapolis-teal last:rounded-b-lg transition-colors"
                  download
                >
                  Export as Markdown
                </a>
              </div>
            </button>
            <button
              @click="openCreateModal()"
              class="px-8 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
            >
              + New Requirement
            </button>
          </div>
        </div>

        <!-- Requirements List -->
        <div v-if="requirements.length === 0" class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 text-center py-16">
          <svg class="mx-auto h-12 w-12 text-annapolis-teal mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 class="text-lg font-medium text-white mb-2">No requirements yet</h3>
          <p class="text-annapolis-gray-300 mb-6">Get started by creating your first requirement</p>
          <button
            @click="openCreateModal()"
            class="inline-flex items-center px-8 py-3 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
          >
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            Create Requirement
          </button>
        </div>

        <div v-else class="grid gap-4">
          <div
            v-for="req in requirements"
            :key="req.id"
            :id="`req-${req.id}`"
            :class="[
              'bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border transition-all duration-300 overflow-hidden cursor-pointer',
              selectedRequirement?.id === req.id ? 'border-annapolis-teal ring-2 ring-annapolis-teal/30' : 'border-annapolis-teal/20 hover:border-annapolis-teal/40 hover:shadow-xl'
            ]"
            @click="navigateToRequirement(req.id)"
          >
            <div class="p-6">
              <div class="flex justify-between items-start mb-6">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-3">
                    <span class="inline-flex items-center px-3 py-1 rounded-lg text-sm font-mono font-medium bg-annapolis-teal/20 text-annapolis-teal border border-annapolis-teal/30">
                      {{ req.reqId }}
                    </span>
                    <span class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-annapolis-gray-600/20 text-annapolis-gray-300 border border-annapolis-gray-600/30">
                      Level {{ req.level || 1 }}
                    </span>
                    <h3 class="text-lg font-semibold text-white">{{ req.title }}</h3>
                  </div>
                  <p class="text-annapolis-gray-300 leading-relaxed mb-4">{{ req.description || 'No description provided' }}</p>

                  <div class="flex items-center gap-3 flex-wrap">
                    <span :class="[
                      'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border',
                      req.status === 'APPROVED' ? 'bg-green-500/20 text-green-400 border-green-500/30' :
                      req.status === 'REVIEW' ? 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30' :
                      req.status === 'DEPRECATED' ? 'bg-red-500/20 text-red-400 border-red-500/30' :
                      'bg-annapolis-gray-600/20 text-annapolis-gray-300 border-annapolis-gray-600/30'
                    ]">
                      {{ req.status }}
                    </span>
                    <span :class="[
                      'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border',
                      req.priority === 'CRITICAL' ? 'bg-red-500/20 text-red-400 border-red-500/30' :
                      req.priority === 'HIGH' ? 'bg-orange-500/20 text-orange-400 border-orange-500/30' :
                      req.priority === 'MEDIUM' ? 'bg-annapolis-teal/20 text-annapolis-teal border-annapolis-teal/30' :
                      'bg-annapolis-gray-600/20 text-annapolis-gray-300 border-annapolis-gray-600/30'
                    ]">
                      {{ req.priority }}
                    </span>
                    <span v-if="req.section" class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-blue-500/20 text-blue-400 border border-blue-500/30">
                      Section {{ req.section }}
                    </span>
                    <button
                      v-if="req.inLinkCount !== undefined && req.inLinkCount > 0"
                      @click.stop="router.push(`/projects/${projectId}/requirements/${req.id}?showLinks=incoming`)"
                      class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-purple-500/20 text-purple-400 border border-purple-500/30 hover:bg-purple-500/30 hover:border-purple-500/50 transition-all cursor-pointer"
                      title="Click to view incoming links"
                    >
                      ↑ {{ req.inLinkCount }} In
                    </button>
                    <span
                      v-else-if="req.inLinkCount !== undefined"
                      class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-purple-500/20 text-purple-400 border border-purple-500/30 opacity-50"
                      title="No incoming links"
                    >
                      ↑ 0 In
                    </span>
                    <button
                      v-if="req.outLinkCount !== undefined && req.outLinkCount > 0"
                      @click.stop="router.push(`/projects/${projectId}/requirements/${req.id}?showLinks=outgoing`)"
                      class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-500/20 text-green-400 border border-green-500/30 hover:bg-green-500/30 hover:border-green-500/50 transition-all cursor-pointer"
                      title="Click to view outgoing links"
                    >
                      {{ req.outLinkCount }} Out ↓
                    </button>
                    <span
                      v-else-if="req.outLinkCount !== undefined"
                      class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-500/20 text-green-400 border border-green-500/30 opacity-50"
                      title="No outgoing links"
                    >
                      0 Out ↓
                    </span>
                    <span v-if="req.parentReqId" class="text-xs text-annapolis-gray-400">
                      <span class="font-medium">Parent:</span> {{ req.parentReqId }}
                    </span>
                  </div>
                </div>

                <div class="flex items-center gap-3 ml-6">
                  <button
                    @click.stop="openCreateModal(req)"
                    class="inline-flex items-center px-3 py-2 border border-green-500/30 shadow-sm text-sm font-medium rounded-lg text-green-400 bg-green-500/10 hover:bg-green-500/20 focus:outline-none focus:ring-2 focus:ring-green-500 transition-all duration-300"
                    title="Add child requirement"
                  >
                    <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                    </svg>
                    Add Child
                  </button>
                  <button
                    @click.stop="openEditModal(req)"
                    class="inline-flex items-center px-3 py-2 border border-annapolis-teal/30 shadow-sm text-sm font-medium rounded-lg text-annapolis-teal bg-annapolis-teal/10 hover:bg-annapolis-teal/20 focus:outline-none focus:ring-2 focus:ring-annapolis-teal transition-all duration-300"
                  >
                    <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                    Edit
                  </button>
                  <button
                    @click.stop="deleteRequirement(req.id)"
                    class="inline-flex items-center px-3 py-2 border border-red-500/30 shadow-sm text-sm font-medium rounded-lg text-red-400 bg-red-500/10 hover:bg-red-500/20 focus:outline-none focus:ring-2 focus:ring-red-500 transition-all duration-300"
                  >
                    <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    Delete
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        </div>
      </div>
    </main>

    <!-- Create/Edit Modal -->
    <div
      v-if="showModal"
      class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50"
      @click.self="showModal = false"
    >
      <div class="bg-annapolis-charcoal rounded-lg px-8 py-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto shadow-2xl border border-annapolis-teal/30">
        <h3 class="text-xl font-semibold mb-6 text-white">
          {{ editingRequirement ? 'Edit Requirement' : 'Create New Requirement' }}
        </h3>
        <form @submit.prevent="handleSaveRequirement">
          <div class="space-y-5">
            <div>
              <label for="modal-title" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Title *</label>
              <input
                id="modal-title"
                v-model="formData.title"
                type="text"
                required
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              />
            </div>
            <div>
              <label for="modal-description" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Description</label>
              <textarea
                id="modal-description"
                v-model="formData.description"
                rows="5"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              ></textarea>
            </div>
            <div>
              <label for="modal-parent" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Parent Requirement (Optional)</label>
              <select
                id="modal-parent"
                v-model="formData.parentId"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              >
                <option :value="null">None (Top Level)</option>
                <option
                  v-for="req in availableParents"
                  :key="req.id"
                  :value="req.id"
                  :disabled="req.id === editingRequirement?.id"
                >
                  {{ req.reqId }} - {{ req.title }}
                </option>
              </select>
            </div>
            <div>
              <label for="modal-section" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Section (Optional)</label>
              <input
                id="modal-section"
                v-model="formData.section"
                type="text"
                placeholder="e.g., 1.1.1, 2.3.4"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              />
              <p class="mt-1 text-xs text-annapolis-gray-400">Optional hierarchical section number for organization</p>
            </div>
            <div class="grid grid-cols-2 gap-5">
              <div>
                <label for="modal-status" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Status</label>
                <select
                  id="modal-status"
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
                <label for="modal-priority" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Priority</label>
                <select
                  id="modal-priority"
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
              @click="showModal = false"
              class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white border border-annapolis-teal/20 rounded-lg hover:bg-annapolis-teal/10 transition-all duration-300"
            >
              Cancel
            </button>
            <button
              type="submit"
              class="px-8 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
            >
              {{ editingRequirement ? 'Update' : 'Create' }}
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
import RequirementTreeView from '@/components/RequirementTreeView.vue'
import { compareReqIds } from '@/utils/requirementSorting'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const projectId = computed(() => route.params.id as string)
const project = ref<Project | null>(null)
const allRequirements = ref<Requirement[]>([])
const allRequirementLinks = ref<any[]>([])
const requirements = ref<Requirement[]>([])
const loading = ref(true)

const searchQuery = ref(route.query.search as string || '')
const filterStatus = ref(route.query.status as string || '')
const filterPriority = ref(route.query.priority as string || '')
const filterLevel = ref(route.query.level as string || '')
const showExportMenu = ref(false)

const showModal = ref(false)
const editingRequirement = ref<Requirement | null>(null)
const selectedRequirement = ref<Requirement | null>(null)
const showTreeView = ref(false)
const expandedNodes = ref(new Set<string>())

const formData = ref({
  title: '',
  description: '',
  status: 'DRAFT',
  priority: 'MEDIUM',
  parentId: null as string | null,
  section: ''
})

const availableParents = computed(() => {
  return allRequirements.value
})

onMounted(async () => {
  await loadData()
  // Apply filters from URL on mount
  if (searchQuery.value || filterStatus.value || filterPriority.value || filterLevel.value) {
    handleSearch()
  }
})

async function loadData() {
  loading.value = true
  try {
    project.value = await projectService.getById(projectId.value)
    allRequirements.value = await requirementService.getByProject(projectId.value)
    requirements.value = allRequirements.value

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
  } catch (err) {
    console.error('Failed to load data:', err)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  // Update URL query parameters to persist filters
  router.replace({
    query: {
      search: searchQuery.value || undefined,
      status: filterStatus.value || undefined,
      priority: filterPriority.value || undefined,
      level: filterLevel.value || undefined
    }
  })

  // Client-side filtering for instant results
  let filtered = allRequirements.value

  // Filter by search query (searches in reqId, title, and description)
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(req =>
      req.reqId.toLowerCase().includes(query) ||
      req.title.toLowerCase().includes(query) ||
      (req.description && req.description.toLowerCase().includes(query))
    )
  }

  // Filter by status
  if (filterStatus.value) {
    filtered = filtered.filter(req => req.status === filterStatus.value)
  }

  // Filter by priority
  if (filterPriority.value) {
    filtered = filtered.filter(req => req.priority === filterPriority.value)
  }

  // Filter by level
  if (filterLevel.value) {
    filtered = filtered.filter(req => req.level?.toString() === filterLevel.value)
  }

  // Sort by requirement ID (natural number ordering)
  filtered.sort((a, b) => compareReqIds(a.reqId, b.reqId))

  requirements.value = filtered
}

function openCreateModal(parentReq?: Requirement) {
  editingRequirement.value = null
  formData.value = {
    title: '',
    description: '',
    status: 'DRAFT',
    priority: 'MEDIUM',
    parentId: parentReq?.id || null,
    section: ''
  }
  showModal.value = true
}

function openEditModal(req: Requirement) {
  editingRequirement.value = req
  formData.value = {
    title: req.title,
    description: req.description,
    status: req.status,
    priority: req.priority,
    parentId: req.parentId || null,
    section: req.section || ''
  }
  showModal.value = true
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

async function handleSaveRequirement() {
  try {
    if (editingRequirement.value) {
      await requirementService.update(editingRequirement.value.id, {
        ...formData.value,
        parentId: formData.value.parentId || undefined
      })
    } else {
      await requirementService.create(projectId.value, {
        ...formData.value,
        parentId: formData.value.parentId || undefined
      })
    }
    showModal.value = false
    await loadData()
  } catch (err) {
    console.error('Failed to save requirement:', err)
  }
}

async function deleteRequirement(id: string) {
  if (!confirm('Are you sure you want to delete this requirement?')) return

  try {
    console.log('Deleting requirement:', id)
    await requirementService.delete(id)
    console.log('Delete successful, reloading data')
    await loadData()
  } catch (err: any) {
    console.error('Failed to delete requirement:', err)
    alert(`Failed to delete requirement: ${err.response?.data?.message || err.message || 'Unknown error'}`)
  }
}

function navigateToRequirement(reqId: string) {
  router.push(`/projects/${projectId.value}/requirements/${reqId}`)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
