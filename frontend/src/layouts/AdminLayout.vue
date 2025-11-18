<template>
  <div class="admin-layout">
    <!-- Admin Header -->
    <header class="admin-header">
      <div class="header-content">
        <div class="header-left">
          <router-link to="/" class="brand-link">
            <h1>Lineage Admin</h1>
          </router-link>
        </div>

        <nav class="admin-nav">
          <router-link
            to="/admin/users"
            class="nav-link"
            :class="{ active: $route.path.startsWith('/admin/users') }"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            Users
          </router-link>

          <router-link
            to="/admin/audit"
            class="nav-link"
            :class="{ active: $route.path.startsWith('/admin/audit') }"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            Audit Logs
          </router-link>

          <router-link
            to="/admin/stats"
            class="nav-link"
            :class="{ active: $route.path.startsWith('/admin/stats') }"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="20" x2="18" y2="10"/>
              <line x1="12" y1="20" x2="12" y2="4"/>
              <line x1="6" y1="20" x2="6" y2="14"/>
            </svg>
            Statistics
          </router-link>
        </nav>

        <div class="header-right">
          <router-link to="/" class="back-link">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
            </svg>
            Back to App
          </router-link>
        </div>
      </div>
    </header>

    <!-- Admin Content -->
    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
// Admin layout component - provides consistent navigation and layout for admin pages
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f9fafb;
}

.admin-header {
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  border-bottom: 1px solid #475569;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.header-left .brand-link {
  text-decoration: none;
  color: white;
}

.header-left h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
}

.admin-nav {
  display: flex;
  gap: 2rem;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  text-decoration: none;
  color: #cbd5e1;
  font-weight: 500;
  transition: all 0.2s;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-link.active {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.nav-link.active:hover {
  background: rgba(59, 130, 246, 0.3);
}

.header-right .back-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  text-decoration: none;
  color: #cbd5e1;
  font-weight: 500;
  transition: all 0.2s;
}

.header-right .back-link:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.admin-main {
  padding: 2rem 0;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    height: auto;
    padding: 1rem;
    gap: 1rem;
  }

  .admin-nav {
    gap: 1rem;
  }

  .nav-link {
    padding: 0.5rem;
    font-size: 0.875rem;
  }

  .nav-link span {
    display: none;
  }
}
</style>