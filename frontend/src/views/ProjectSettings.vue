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
            <h1 class="text-2xl font-bold text-white">
              Project Settings
            </h1>
            <span v-if="project" class="px-3 py-1 text-xs font-mono bg-annapolis-teal/20 text-annapolis-teal rounded-lg border border-annapolis-teal/30">
              {{ project.projectKey }}
            </span>
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
    <main class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Loading -->
      <div v-if="loading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-annapolis-teal"></div>
      </div>

      <!-- Content -->
      <div v-else-if="project" class="space-y-6">
        <!-- General Settings -->
        <div class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 p-6">
          <h2 class="text-xl font-semibold text-white mb-6">General Information</h2>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Project Name</label>
              <input
                v-model="projectData.name"
                type="text"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Description</label>
              <textarea
                v-model="projectData.description"
                rows="3"
                class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-annapolis-gray-300 mb-2">Project Key</label>
              <input
                v-model="projectData.projectKey"
                type="text"
                disabled
                class="w-full px-4 py-3 bg-annapolis-navy/30 border border-annapolis-gray-600/30 rounded-lg text-annapolis-gray-400 cursor-not-allowed"
              />
              <p class="mt-1 text-xs text-annapolis-gray-400">Project key cannot be changed</p>
            </div>
          </div>
        </div>

        <!-- Level Prefixes Configuration -->
        <div class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 p-6">
          <div class="flex justify-between items-start mb-6">
            <div>
              <h2 class="text-xl font-semibold text-white">Requirement Level Prefixes</h2>
              <p class="text-sm text-annapolis-gray-400 mt-1">Define custom prefixes for requirement IDs at each level</p>
            </div>
            <button
              @click="addLevelPrefix"
              class="px-4 py-2 bg-annapolis-teal/20 hover:bg-annapolis-teal/30 text-annapolis-teal rounded-lg transition-all duration-300 flex items-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              Add Level
            </button>
          </div>

          <div v-if="levelPrefixEntries.length === 0" class="text-center py-8 text-annapolis-gray-400">
            <p>No level prefixes configured. Add levels to define custom requirement ID prefixes.</p>
            <p class="text-xs mt-2">Example: Level 1 = "CR" → generates CR-001, CR-002, etc.</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="(entry, index) in levelPrefixEntries"
              :key="entry.level"
              class="flex items-center gap-4 bg-annapolis-navy/30 p-4 rounded-lg"
            >
              <div class="flex-1 grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs font-medium text-annapolis-gray-400 mb-1">Level</label>
                  <input
                    v-model.number="entry.level"
                    type="number"
                    min="1"
                    class="w-full px-3 py-2 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-annapolis-teal"
                  />
                </div>
                <div>
                  <label class="block text-xs font-medium text-annapolis-gray-400 mb-1">Prefix</label>
                  <input
                    v-model="entry.prefix"
                    type="text"
                    placeholder="e.g., CR, REN, SYS"
                    class="w-full px-3 py-2 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal"
                  />
                </div>
              </div>
              <button
                @click="removeLevelPrefix(index)"
                class="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-all"
                title="Remove level"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="levelPrefixEntries.length > 0" class="mt-6 p-4 bg-blue-500/10 border border-blue-500/30 rounded-lg">
            <p class="text-sm text-blue-400 font-medium mb-2">Preview:</p>
            <div class="space-y-1 text-xs text-annapolis-gray-300">
              <div v-for="entry in levelPrefixEntries" :key="entry.level">
                <span class="font-mono text-annapolis-teal">Level {{ entry.level }}:</span>
                <span class="ml-2 font-mono">{{ entry.prefix }}-001, {{ entry.prefix }}-002, {{ entry.prefix }}-003...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Save Button -->
        <div class="flex justify-end gap-3">
          <button
            @click="goBack"
            class="px-6 py-3 text-sm font-medium text-annapolis-gray-300 hover:text-white border border-annapolis-teal/20 rounded-lg hover:bg-annapolis-teal/10 transition-all duration-300"
          >
            Cancel
          </button>
          <button
            @click="saveSettings"
            :disabled="saving"
            class="px-8 py-3 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ saving ? 'Saving...' : 'Save Settings' }}
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { projectService, type Project } from '@/services/projectService'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const projectId = computed(() => route.params.id as string)
const project = ref<Project | null>(null)
const loading = ref(true)
const saving = ref(false)

const projectData = ref({
  name: '',
  description: '',
  projectKey: ''
})

interface LevelPrefixEntry {
  level: number
  prefix: string
}

const levelPrefixEntries = ref<LevelPrefixEntry[]>([])

onMounted(async () => {
  await loadProject()
})

async function loadProject() {
  loading.value = true
  try {
    project.value = await projectService.getById(projectId.value)
    projectData.value = {
      name: project.value.name,
      description: project.value.description,
      projectKey: project.value.projectKey
    }

    // Convert levelPrefixes object to array of entries
    console.log('Loading level prefixes:', project.value.levelPrefixes)
    levelPrefixEntries.value = Object.entries(project.value.levelPrefixes || {})
      .map(([level, prefix]) => ({
        level: parseInt(level),
        prefix
      }))
      .sort((a, b) => a.level - b.level)
    console.log('Loaded entries:', levelPrefixEntries.value)

  } catch (err) {
    console.error('Failed to load project:', err)
    alert('Failed to load project settings')
  } finally {
    loading.value = false
  }
}

function addLevelPrefix() {
  const maxLevel = levelPrefixEntries.value.length > 0
    ? Math.max(...levelPrefixEntries.value.map(e => e.level))
    : 0

  levelPrefixEntries.value.push({
    level: maxLevel + 1,
    prefix: ''
  })
}

function removeLevelPrefix(index: number) {
  levelPrefixEntries.value.splice(index, 1)
}

async function saveSettings() {
  saving.value = true
  try {
    // Convert level prefix entries back to object
    const levelPrefixes: Record<string, string> = {}
    for (const entry of levelPrefixEntries.value) {
      if (entry.prefix.trim()) {
        levelPrefixes[entry.level.toString()] = entry.prefix.trim().toUpperCase()
      }
    }
    console.log('Saving level prefixes:', levelPrefixes)

    await projectService.update(projectId.value, {
      name: projectData.value.name,
      description: projectData.value.description,
      projectKey: projectData.value.projectKey,
      levelPrefixes
    } as any)

    alert('Settings saved successfully!')
    goBack()
  } catch (err) {
    console.error('Failed to save settings:', err)
    alert('Failed to save settings')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push(`/projects/${projectId.value}`)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
