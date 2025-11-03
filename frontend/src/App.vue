<template>
  <div id="app">
    <router-view />
    <AIAssistant v-if="authStore.isAuthenticated" />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AIAssistant from '@/components/AIAssistant.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// Validate token on app mount by fetching current user from backend
onMounted(async () => {
  await authStore.validateToken()
})
</script>

<style>
#app {
  width: 100%;
  min-height: 100vh;
}
</style>
