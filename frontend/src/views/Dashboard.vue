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
              <span class="ml-2 px-2 py-1 text-xs rounded-full bg-annapolis-teal/20 text-annapolis-teal border border-annapolis-teal/30">
                {{ authStore.user?.role }}
              </span>
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
      <div class="bg-annapolis-charcoal/50 backdrop-blur-sm rounded-lg shadow-lg border border-annapolis-teal/20 p-6">
        <h2 class="text-2xl font-semibold text-white mb-4">Welcome to Lineage</h2>
        <p class="text-annapolis-gray-300 mb-6">
          Your open-source requirements management tool is up and running!
        </p>

        <div class="space-y-4">
          <div class="border-l-4 border-annapolis-teal pl-4 bg-annapolis-navy/30 py-3 rounded-r">
            <h3 class="font-medium text-white mb-2">Getting Started</h3>
            <ul class="mt-2 text-sm text-annapolis-gray-300 space-y-1">
              <li>✅ Backend is running on port 8080</li>
              <li>✅ Frontend is running on port 5173</li>
              <li>✅ Authentication is working</li>
              <li>✅ Projects and Requirements CRUD</li>
              <li>✅ Search and filtering</li>
              <li>⏳ Version history (coming soon)</li>
            </ul>
          </div>

          <div class="border-l-4 border-green-500 pl-4 bg-annapolis-navy/30 py-3 rounded-r">
            <h3 class="font-medium text-white mb-2">What's Next?</h3>
            <ul class="mt-2 text-sm text-annapolis-gray-300 space-y-1">
              <li>• Enhanced requirements tree view</li>
              <li>• Advanced linking and traceability</li>
              <li>• Version history and change tracking</li>
              <li>• Collaborative editing features</li>
              <li>• Custom requirement attributes</li>
            </ul>
          </div>

          <div class="border-l-4 border-yellow-500 pl-4 bg-annapolis-navy/30 py-3 rounded-r">
            <h3 class="font-medium text-white mb-2">API Documentation</h3>
            <p class="mt-2 text-sm text-annapolis-gray-300">
              Access the Swagger UI at:
              <a
                href="http://localhost:8080/swagger-ui.html"
                target="_blank"
                class="text-annapolis-teal hover:text-annapolis-teal/80 ml-1 underline transition-colors"
              >
                http://localhost:8080/swagger-ui.html
              </a>
            </p>
          </div>

          <div class="mt-8 pt-6 border-t border-annapolis-teal/20">
            <button
              @click="$router.push('/projects')"
              class="w-full py-3 px-4 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg"
            >
              Go to Projects
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

onMounted(async () => {
  await authStore.fetchCurrentUser()
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
