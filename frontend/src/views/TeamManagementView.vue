<template>
  <div class="min-h-screen bg-gradient-to-br from-annapolis-navy via-annapolis-charcoal to-annapolis-teal-dark">
    <!-- Header -->
    <header class="bg-annapolis-charcoal/80 backdrop-blur-sm shadow-lg border-b border-annapolis-teal/20">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <div class="flex items-center gap-6">
            <button 
              @click="$router.back()" 
              class="text-annapolis-gray-300 hover:text-annapolis-teal transition-colors duration-300 flex items-center gap-2"
            >
              <span class="text-xl">←</span>
              <span>Back</span>
            </button>
            <div class="h-6 w-px bg-annapolis-teal/30"></div>
            <div>
              <h1 class="text-2xl font-bold text-white">Team Management</h1>
              <p v-if="project" class="text-annapolis-gray-300 text-sm">{{ project.name }} - Teams</p>
            </div>
          </div>
          <div class="flex items-center gap-3">
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
      <div class="max-w-7xl mx-auto">
        <!-- Project selector for cross-project team management -->
        <div v-if="!project" class="mb-8">
          <div class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 p-6">
            <h2 class="text-lg font-semibold text-white mb-4">Select Project</h2>
            <select
              v-model="selectedProjectId"
              @change="handleProjectChange"
              class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
            >
              <option value="">Choose a project to manage teams</option>
              <option v-for="proj in projects" :key="proj.id" :value="proj.id">
                {{ proj.name }}
              </option>
            </select>
          </div>
        </div>

        <!-- Team Management Section -->
        <div v-if="project">
          <TeamList
            :projectId="project.id"
            :projects="projects"
            @team-created="handleTeamCreated"
            @team-updated="handleTeamUpdated"
            @team-deleted="handleTeamDeleted"
          />
        </div>

        <!-- Empty state when no project selected -->
        <div v-else-if="!selectedProjectId" class="text-center py-16">
          <svg class="mx-auto h-16 w-16 text-annapolis-teal mb-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          <h2 class="text-2xl font-bold text-white mb-4">Team Management</h2>
          <p class="text-annapolis-gray-300 mb-8 max-w-md mx-auto">
            Select a project above to start managing teams and collaborating with your project members.
          </p>
          <div class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 p-8 max-w-md mx-auto">
            <h3 class="text-lg font-semibold text-white mb-4">What you can do:</h3>
            <ul class="text-annapolis-gray-300 space-y-2">
              <li class="flex items-center gap-2">
                <svg class="w-4 h-4 text-annapolis-teal" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                Create and manage teams
              </li>
              <li class="flex items-center gap-2">
                <svg class="w-4 h-4 text-annapolis-teal" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                Add and manage team members
              </li>
              <li class="flex items-center gap-2">
                <svg class="w-4 h-4 text-annapolis-teal" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                Configure team settings and permissions
              </li>
              <li class="flex items-center gap-2">
                <svg class="w-4 h-4 text-annapolis-teal" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                Set up peer review workflows
              </li>
            </ul>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useRbacStore } from '@/stores/rbac'
import { useTeamStore } from '@/stores/team'
import { projectService, type Project } from '@/services/projectService'
import TeamList from '@/components/rbac/TeamList.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const rbacStore = useRbacStore()
const teamStore = useTeamStore()

// State
const loading = ref(true)
const projects = ref<Project[]>([])
const selectedProjectId = ref('')

// Computed
const project = computed(() => {
  if (!selectedProjectId.value) return null
  return projects.value.find(p => p.id === selectedProjectId.value) || null
})

// Methods
async function loadProjects() {
  try {
    loading.value = true
    projects.value = await projectService.getAll()
    
    // If route has projectId param, select it
    const routeProjectId = route.query.projectId as string
    if (routeProjectId) {
      selectedProjectId.value = routeProjectId
    }
  } catch (error) {
    console.error('Failed to load projects:', error)
  } finally {
    loading.value = false
  }
}

function handleProjectChange() {
  // Update route query to persist selection
  router.replace({
    query: {
      ...route.query,
      projectId: selectedProjectId.value || undefined
    }
  })
}

function handleTeamCreated(team: any) {
  // Team created successfully - could show success toast
  console.log('Team created:', team)
}

function handleTeamUpdated(team: any) {
  // Team updated successfully - could show success toast
  console.log('Team updated:', team)
}

function handleTeamDeleted(teamId: string) {
  // Team deleted successfully - could show success toast
  console.log('Team deleted:', teamId)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

// Lifecycle
onMounted(() => {
  loadProjects()
})
</script>