<template>
  <div class="min-h-screen bg-gradient-to-br from-annapolis-navy via-annapolis-charcoal to-annapolis-teal-dark">
    <!-- Header -->
    <header class="bg-annapolis-charcoal/80 backdrop-blur-sm shadow-lg border-b border-annapolis-teal/20">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <h1 class="text-3xl font-bold text-white">Lineage</h1>
          <div class="flex items-center space-x-4">
            <div class="relative">
              <button
                @click="showUserMenu = !showUserMenu"
                class="flex items-center space-x-2 px-3 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white transition-colors duration-300 rounded-lg hover:bg-annapolis-teal/10"
              >
                <div class="flex flex-col items-end">
                  <span class="text-sm">{{ authStore.user?.name || authStore.user?.email }}</span>
                  <span class="text-xs text-annapolis-gray-400 capitalize">{{ authStore.user?.globalRole?.toLowerCase().replace('_', ' ') }}</span>
                </div>
                <svg class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': showUserMenu }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
              </button>

              <!-- User Menu Dropdown -->
              <div
                v-if="showUserMenu"
                class="absolute right-0 mt-2 w-48 bg-annapolis-charcoal rounded-lg shadow-lg border border-annapolis-teal/20 py-1 z-50"
                @click.stop
              >
                <router-link
                  to="/profile"
                  class="flex items-center px-4 py-2 text-sm text-annapolis-gray-300 hover:text-white hover:bg-annapolis-teal/10 transition-colors"
                  @click="showUserMenu = false"
                >
                  <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                  </svg>
                  Profile Settings
                </router-link>

                <router-link
                  to="/security"
                  class="flex items-center px-4 py-2 text-sm text-annapolis-gray-300 hover:text-white hover:bg-annapolis-teal/10 transition-colors"
                  @click="showUserMenu = false"
                >
                  <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                  </svg>
                  Security Settings
                </router-link>

                <router-link
                  v-if="authStore.isAdmin"
                  to="/admin/users"
                  class="flex items-center px-4 py-2 text-sm text-annapolis-gray-300 hover:text-white hover:bg-annapolis-teal/10 transition-colors"
                  @click="showUserMenu = false"
                >
                  <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  </svg>
                  Admin Panel
                </router-link>

                <div class="border-t border-annapolis-teal/20 my-1"></div>

                <button
                  @click="handleLogout"
                  class="flex items-center w-full px-4 py-2 text-sm text-annapolis-gray-300 hover:text-white hover:bg-red-500/10 transition-colors text-left"
                >
                  <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                  </svg>
                  Logout
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="w-full px-4 sm:px-6 lg:px-8 py-8">
      <div class="mb-8 flex justify-between items-center">
        <h2 class="text-3xl font-bold text-white">Projects</h2>
        <div class="flex gap-4">
          <button
            @click="showImportModal = true"
            class="px-6 py-3 bg-annapolis-navy hover:bg-annapolis-navy/80 text-white font-semibold rounded-lg border border-annapolis-teal/30 transition-all duration-300 transform hover:scale-105 shadow-lg flex items-center"
          >
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
            </svg>
            Import Project
          </button>
          <button
            @click="showCreateModal = true"
            class="px-8 py-3 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
          >
            + New Project
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="projectStore.loading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-annapolis-teal"></div>
        <p class="mt-2 text-annapolis-gray-300">Loading projects...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="projectStore.error" class="bg-red-500/20 border border-red-500/30 p-4 rounded-lg">
        <p class="text-sm text-red-400">{{ projectStore.error }}</p>
      </div>

      <!-- Empty State -->
      <div v-else-if="projectStore.projects.length === 0" class="text-center py-16 bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20">
        <svg class="mx-auto h-16 w-16 text-annapolis-teal mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
        </svg>
        <h3 class="text-xl font-medium text-white mb-2">No projects yet</h3>
        <p class="text-annapolis-gray-300 mb-6">Create your first project to get started with requirements management</p>
        <button
          @click="showCreateModal = true"
          class="inline-flex items-center px-8 py-3 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          Create Project
        </button>
      </div>

      <!-- Projects Grid -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="project in projectStore.projects"
          :key="project.id"
          class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 hover:border-annapolis-teal/40 hover:shadow-xl transition-all duration-300 cursor-pointer overflow-hidden group"
          @click="router.push(`/projects/${project.id}`)"
        >
          <div class="p-6">
            <div class="flex items-start justify-between mb-3">
              <h3 class="text-xl font-semibold text-white group-hover:text-annapolis-teal transition-colors">{{ project.name }}</h3>
              <div class="flex items-center gap-2">
                <span class="px-3 py-1 text-xs font-mono bg-annapolis-teal/20 text-annapolis-teal rounded-lg border border-annapolis-teal/30">
                  {{ project.projectKey }}
                </span>
                <button
                  @click.stop="openDeleteModal(project)"
                  class="p-1.5 text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded transition-colors"
                  title="Delete project"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
            <p class="text-sm text-annapolis-gray-300 mb-4 line-clamp-2 min-h-[40px]">
              {{ project.description || 'No description' }}
            </p>
            <div class="flex items-center text-xs text-annapolis-gray-400">
              <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              Created by {{ project.createdByName || project.createdByEmail }}
            </div>
          </div>
          <div class="h-1 bg-gradient-to-r from-annapolis-teal/0 via-annapolis-teal/50 to-annapolis-teal/0 opacity-0 group-hover:opacity-100 transition-opacity"></div>
        </div>
      </div>
    </main>

    <!-- Import Project Modal -->
    <div
      v-if="showImportModal"
      class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50"
      @click.self="showImportModal = false"
    >
      <div class="bg-annapolis-charcoal rounded-lg px-8 py-6 w-full max-w-md shadow-2xl border border-annapolis-teal/30">
        <h3 class="text-xl font-semibold mb-6 text-white">Import Project</h3>
        <div
          class="border-2 border-dashed border-annapolis-teal/30 rounded-lg p-8 text-center transition-colors"
          :class="{ 'border-annapolis-teal bg-annapolis-teal/10': isDragging }"
          @dragenter.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @dragover.prevent
          @drop.prevent="handleDrop"
        >
          <input
            type="file"
            ref="fileInput"
            class="hidden"
            accept=".json"
            @change="handleFileSelect"
          />
          <div v-if="!selectedFile">
            <svg class="mx-auto h-12 w-12 text-annapolis-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            <p class="text-annapolis-gray-300 mb-2">Drag and drop your JSON file here</p>
            <p class="text-sm text-annapolis-gray-400 mb-4">or</p>
            <button
              @click="fileInput?.click()"
              class="px-4 py-2 bg-annapolis-navy hover:bg-annapolis-navy/80 text-white text-sm font-medium rounded-lg border border-annapolis-teal/30 transition-colors"
            >
              Browse Files
            </button>
          </div>
          <div v-else class="text-left">
            <div class="flex items-center justify-between bg-annapolis-navy/50 p-3 rounded-lg border border-annapolis-teal/30">
              <div class="flex items-center overflow-hidden">
                <svg class="w-5 h-5 text-annapolis-teal mr-3 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <span class="text-sm text-white truncate">{{ selectedFile.name }}</span>
              </div>
              <button
                @click="selectedFile = null"
                class="ml-2 text-annapolis-gray-400 hover:text-red-400 transition-colors"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- File Format Help -->
        <div class="mt-6 p-4 bg-annapolis-navy/30 rounded-lg border border-annapolis-teal/20">
          <div class="flex items-start gap-3">
            <svg class="w-5 h-5 text-annapolis-teal flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <h4 class="text-sm font-medium text-white mb-2">File Format Requirements</h4>
              <p class="text-xs text-annapolis-gray-300 mb-3">
                Your JSON file must contain both <code class="px-2 py-1 bg-annapolis-charcoal rounded text-annapolis-teal">project</code> 
                and <code class="px-2 py-1 bg-annapolis-charcoal rounded text-annapolis-teal">requirements</code> sections.
              </p>
              <button
                @click="downloadTemplate"
                class="inline-flex items-center px-3 py-2 text-xs font-medium text-annapolis-teal hover:text-annapolis-teal-light border border-annapolis-teal/30 rounded-lg hover:bg-annapolis-teal/10 transition-colors"
              >
                <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                Download Sample Template
              </button>
            </div>
          </div>
        </div>

        <div class="mt-8 flex justify-end gap-3">
          <button
            type="button"
            @click="showImportModal = false"
            class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white border border-annapolis-teal/20 rounded-lg hover:bg-annapolis-teal/10 transition-all duration-300"
          >
            Cancel
          </button>
          <button
            type="button"
            @click="handleImportProject"
            :disabled="!selectedFile || projectStore.loading"
            class="px-8 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            Import
          </button>
        </div>
      </div>
    </div>

    <!-- Create Project Modal -->
    <div
      v-if="showCreateModal"
      class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50"
      @click.self="showCreateModal = false"
    >
      <div class="bg-annapolis-charcoal rounded-lg px-8 py-6 w-full max-w-md shadow-2xl border border-annapolis-teal/30">
        <h3 class="text-xl font-semibold mb-6 text-white">Create New Project</h3>
        <form @submit.prevent="handleCreateProject">
          <div class="space-y-5">
            <div>
              <label for="project-name" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Project Name *</label>
              <input
                id="project-name"
                v-model="newProject.name"
                type="text"
                required
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
                placeholder="My Awesome Project"
              />
            </div>
            <div>
              <label for="project-key" class="block text-sm font-medium text-annapolis-gray-300 mb-2">
                Project Key * (2-10 uppercase letters)
              </label>
              <input
                id="project-key"
                v-model="newProject.projectKey"
                type="text"
                required
                pattern="[A-Z]{2,10}"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all uppercase font-mono"
                placeholder="PROJ"
                maxlength="10"
              />
              <p class="mt-2 text-xs text-annapolis-gray-400">Used as a prefix for requirement IDs (e.g., PROJ-001)</p>
            </div>
            <div>
              <label for="project-description" class="block text-sm font-medium text-annapolis-gray-300 mb-2">Description</label>
              <textarea
                id="project-description"
                v-model="newProject.description"
                rows="3"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
                placeholder="Brief description of your project..."
              ></textarea>
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
              :disabled="projectStore.loading"
              class="px-8 py-2 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
            >
              Create
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete Project Confirmation Modal -->
    <div
      v-if="showDeleteModal"
      class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50"
      @click.self="closeDeleteModal"
    >
      <div class="bg-annapolis-charcoal rounded-lg px-8 py-6 w-full max-w-md shadow-2xl border border-red-500/30">
        <div class="flex items-start gap-3 mb-4">
          <svg class="w-6 h-6 text-red-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div>
            <h3 class="text-xl font-semibold text-white">Delete Project</h3>
            <p class="mt-2 text-sm text-annapolis-gray-300">
              This action cannot be undone. This will permanently delete the project
              <span class="font-semibold text-white">{{ projectToDelete?.name }}</span>
              and all its requirements.
            </p>
          </div>
        </div>

        <div class="mt-6">
          <label for="delete-confirm" class="block text-sm font-medium text-annapolis-gray-300 mb-2">
            Type <span class="font-mono font-semibold text-white">{{ projectToDelete?.name }}</span> to confirm:
          </label>
          <input
            id="delete-confirm"
            v-model="deleteConfirmText"
            type="text"
            class="w-full px-4 py-3 bg-annapolis-navy/50 border border-red-500/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition-all"
            :placeholder="projectToDelete?.name"
            @keyup.enter="handleDeleteProject"
          />
        </div>

        <div class="mt-6 flex justify-end gap-3">
          <button
            type="button"
            @click="closeDeleteModal"
            class="px-4 py-2 text-sm font-medium text-annapolis-gray-300 hover:text-white border border-annapolis-teal/20 rounded-lg hover:bg-annapolis-teal/10 transition-all duration-300"
          >
            Cancel
          </button>
          <button
            type="button"
            @click="handleDeleteProject"
            :disabled="deleteConfirmText !== projectToDelete?.name || projectStore.loading"
            class="px-6 py-2 bg-red-500 hover:bg-red-600 text-white font-semibold rounded-lg transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Delete Project
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useProjectStore } from '@/stores/projects'
import type { Project } from '@/services/projectService'

const router = useRouter()
const authStore = useAuthStore()
const projectStore = useProjectStore()

const showCreateModal = ref(false)
const showImportModal = ref(false)
const isDragging = ref(false)
const selectedFile = ref<File | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const newProject = ref({
  name: '',
  projectKey: '',
  description: ''
})

const showDeleteModal = ref(false)
const projectToDelete = ref<Project | null>(null)
const deleteConfirmText = ref('')
const showUserMenu = ref(false)

onMounted(async () => {
  await authStore.fetchCurrentUser()
  await projectStore.fetchProjects()
  if (typeof document !== 'undefined') {
    document.addEventListener('click', handleClickOutside)
  }
})

onUnmounted(() => {
  if (typeof document !== 'undefined') {
    document.removeEventListener('click', handleClickOutside)
  }
})

function handleClickOutside(event: Event) {
  const target = event.target as HTMLElement
  if (!target.closest('.relative')) {
    showUserMenu.value = false
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

async function handleCreateProject() {
  const project = await projectStore.createProject(newProject.value)
  if (project) {
    showCreateModal.value = false
    newProject.value = { name: '', projectKey: '', description: '' }
    router.push(`/projects/${project.id}`)
  }
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    const file = input.files[0]
    if (file) {
      selectedFile.value = file
    }
  }
}

function handleDrop(event: DragEvent) {
  isDragging.value = false
  if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
    const file = event.dataTransfer.files[0]
    if (file && file.name.endsWith('.json')) {
      selectedFile.value = file
    }
  }
}

async function handleImportProject() {
  if (!selectedFile.value) return

  try {
    const project = await projectStore.importProject(selectedFile.value)
    if (project) {
      showImportModal.value = false
      selectedFile.value = null
      router.push(`/projects/${project.id}`)
    }
  } catch (error: any) {
    console.error('Import error:', error)
    // The error is already handled in the projectStore with better messages
  }
}

function downloadTemplate() {
  const template = {
    "project": {
      "name": "Sample Project Name",
      "description": "Project description here",
      "key": "SAMPLE",
      "levelPrefixes": {
        "1": "BUS",
        "2": "SYS",
        "3": "SUB"
      }
    },
    "requirements": [
      {
        "reqId": "BUS-001",
        "title": "Business Requirement 1",
        "description": "Description of the business requirement",
        "status": "DRAFT",
        "priority": "HIGH",
        "parentId": null
      },
      {
        "reqId": "SYS-001", 
        "title": "System Requirement 1",
        "description": "Description of the system requirement",
        "status": "DRAFT",
        "priority": "MEDIUM",
        "parentId": "BUS-001"
      }
    ]
  }
  
  const blob = new Blob([JSON.stringify(template, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'project-import-template.json'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function openDeleteModal(project: Project) {
  projectToDelete.value = project
  deleteConfirmText.value = ''
  showDeleteModal.value = true
}

function closeDeleteModal() {
  showDeleteModal.value = false
  projectToDelete.value = null
  deleteConfirmText.value = ''
}

async function handleDeleteProject() {
  if (!projectToDelete.value || deleteConfirmText.value !== projectToDelete.value.name) {
    return
  }

  const success = await projectStore.deleteProject(projectToDelete.value.id)
  if (success) {
    closeDeleteModal()
  }
}
</script>
