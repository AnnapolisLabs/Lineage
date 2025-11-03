import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

describe('router', () => {
  let router: any

  beforeEach(async () => {
    setActivePinia(createPinia())

    // Recreate router with memory history for testing
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', name: 'login', component: { template: '<div>Login</div>' }, meta: { requiresAuth: false } },
        { path: '/', name: 'projects', component: { template: '<div>Projects</div>' }, meta: { requiresAuth: true } },
        { path: '/projects/:id', name: 'project-detail', component: { template: '<div>Project Detail</div>' }, meta: { requiresAuth: true } },
        { path: '/:pathMatch(.*)*', redirect: '/' }
      ]
    })

    router.beforeEach((to: any, _from: any, next: any) => {
      const authStore = useAuthStore()
      const requiresAuth = to.meta.requiresAuth !== false

      if (requiresAuth && !authStore.isAuthenticated) {
        next({ name: 'login' })
      } else if (to.name === 'login' && authStore.isAuthenticated) {
        next({ name: 'projects' })
      } else {
        next()
      }
    })

    await router.isReady()
  })

  it('should redirect to login when not authenticated', async () => {
    const authStore = useAuthStore()
    authStore.token = null

    await router.push('/')
    expect(router.currentRoute.value.name).toBe('login')
  })

  it('should allow access to protected routes when authenticated', async () => {
    const authStore = useAuthStore()
    authStore.token = 'test-token'

    await router.push('/')
    expect(router.currentRoute.value.name).toBe('projects')
  })

  it('should redirect to projects when authenticated user visits login', async () => {
    const authStore = useAuthStore()
    authStore.token = 'test-token'

    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('projects')
  })

  it('should allow access to login when not authenticated', async () => {
    const authStore = useAuthStore()
    authStore.token = null

    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('login')
  })

  it('should redirect unknown paths to home', async () => {
    const authStore = useAuthStore()
    authStore.token = 'test-token'

    await router.push('/unknown-path')
    expect(router.currentRoute.value.path).toBe('/')
  })
})
