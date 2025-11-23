<template>
  <section class="min-h-screen flex items-center justify-center bg-gradient-to-br from-annapolis-navy via-annapolis-charcoal to-annapolis-teal-dark p-4">
    <div class="w-full max-w-md">
        <!-- Logo/Title -->
        <div class="text-center mb-8">
          <h1 class="text-5xl md:text-6xl font-bold text-white mb-4">
            Lineage
          </h1>
          <p class="text-xl text-annapolis-gray-300">
            Requirements Management Tool
          </p>
        </div>

        <!-- Login Card -->
        <div class="bg-annapolis-charcoal/70 backdrop-blur-sm rounded-xl shadow-2xl border border-annapolis-teal/30 px-8 py-8">
          <form class="space-y-8" @submit.prevent="handleLogin">
            <div v-if="authStore.error" class="rounded-lg bg-red-500/20 border border-red-500/30 p-4 mb-4">
              <p class="text-sm text-red-400">{{ authStore.error }}</p>
            </div>

            <div class="space-y-6">
              <div>
                <label for="email" class="block text-sm font-medium text-annapolis-gray-300 mb-3">
                  Email address
                </label>
                <input
                  id="email"
                  v-model="email"
                  type="email"
                  required
                  class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
                  placeholder="Enter your email"
                />
              </div>

              <div>
                <label for="password" class="block text-sm font-medium text-annapolis-gray-300 mb-3">
                  Password
                </label>
                <input
                  id="password"
                  v-model="password"
                  type="password"
                  required
                  class="w-full px-4 py-3 bg-annapolis-navy/50 border border-annapolis-teal/30 rounded-lg text-white placeholder-annapolis-gray-400 focus:outline-none focus:ring-2 focus:ring-annapolis-teal focus:border-transparent transition-all"
                  placeholder="Password"
                />
              </div>
            </div>

            <div>
              <button
                type="submit"
                :disabled="authStore.loading"
                class="w-full py-3 px-4 bg-annapolis-teal hover:bg-annapolis-teal/90 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
              >
                {{ authStore.loading ? 'Signing in...' : 'Sign in' }}
              </button>
            </div>

            <!-- Removed default credentials display for security -->
          </form>
        </div>

        <!-- Footer -->
        <div class="mt-8 text-center text-sm text-annapolis-gray-400">
          <p>Professional software solutions for developers</p>
        </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')

async function handleLogin() {
  const success = await authStore.login({ email: email.value, password: password.value })
  if (success) {
    router.push('/')
  }
}
</script>
