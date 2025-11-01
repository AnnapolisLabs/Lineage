import { defineStore } from 'pinia'
import { ref } from 'vue'
import { projectService, type Project, type CreateProjectRequest } from '@/services/projectService'

export const useProjectStore = defineStore('projects', () => {
  const projects = ref<Project[]>([])
  const currentProject = ref<Project | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchProjects() {
    loading.value = true
    error.value = null
    try {
      projects.value = await projectService.getAll()
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to fetch projects'
    } finally {
      loading.value = false
    }
  }

  async function fetchProject(id: string) {
    loading.value = true
    error.value = null
    try {
      currentProject.value = await projectService.getById(id)
      return currentProject.value
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to fetch project'
      return null
    } finally {
      loading.value = false
    }
  }

  async function createProject(data: CreateProjectRequest) {
    loading.value = true
    error.value = null
    try {
      const project = await projectService.create(data)
      projects.value.push(project)
      return project
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to create project'
      return null
    } finally {
      loading.value = false
    }
  }

  async function updateProject(id: string, data: CreateProjectRequest) {
    loading.value = true
    error.value = null
    try {
      const project = await projectService.update(id, data)
      const index = projects.value.findIndex(p => p.id === id)
      if (index !== -1) {
        projects.value[index] = project
      }
      if (currentProject.value?.id === id) {
        currentProject.value = project
      }
      return project
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to update project'
      return null
    } finally {
      loading.value = false
    }
  }

  async function deleteProject(id: string) {
    loading.value = true
    error.value = null
    try {
      await projectService.delete(id)
      projects.value = projects.value.filter(p => p.id !== id)
      if (currentProject.value?.id === id) {
        currentProject.value = null
      }
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to delete project'
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    projects,
    currentProject,
    loading,
    error,
    fetchProjects,
    fetchProject,
    createProject,
    updateProject,
    deleteProject
  }
})
