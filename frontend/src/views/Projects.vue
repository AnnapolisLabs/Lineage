<template>
  <div class="min-h-screen bg-gradient-to-br from-annapolis-navy via-annapolis-charcoal to-annapolis-teal-dark">
    <!-- Header -->
    <header class="bg-annapolis-charcoal/80 backdrop-blur-sm shadow-lg border-b border-annapolis-teal/20">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <h1 class="text-3xl font-bold text-white">Lineage</h1>
          <div class="flex items-center space-x-4">
            <span class="text-sm text-annapolis-gray-300">
              {{ authStore.user?.name || authStore.user?.email }}
            </span>
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
      <div class="mb-8 flex justify-between items-center">
        <h2 class="text-3xl font-bold text-white">Projects</h2>
        <button
          @click="showCreateModal = true"
          class="px-8 py-3 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
        >
          + New Project
        </button>
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
              <span class="px-3 py-1 text-xs font-mono bg-annapolis-teal/20 text-annapolis-teal rounded-lg border border-annapolis-teal/30">
                {{ project.projectKey }}
              </span>
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
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Project Name *</label>
              <input
                v-model="newProject.name"
                type="text"
                required
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
                placeholder="My Awesome Project"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">
                Project Key * (2-10 uppercase letters)
              </label>
              <input
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
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Description</label>
              <textarea
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useProjectStore } from '@/stores/projects'

const router = useRouter()
const authStore = useAuthStore()
const projectStore = useProjectStore()

const showCreateModal = ref(false)
const newProject = ref({
  name: '',
  projectKey: '',
  description: ''
})

onMounted(async () => {
  await authStore.fetchCurrentUser()
  await projectStore.fetchProjects()
})

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
</script>
