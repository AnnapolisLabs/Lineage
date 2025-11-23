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

  async function importProject(file: File) {
    loading.value = true
    error.value = null
    try {
      const project = await projectService.importProject(file)
      projects.value.push(project)
      return project
    } catch (err: any) {
      // Provide more specific error messages for common validation issues
      const errorMessage = err.response?.data?.message || err.message || 'Failed to import project'
      
      if (errorMessage.includes('Project metadata is required') || 
          errorMessage.includes('project') && 
          (errorMessage.includes('null') || errorMessage.includes('rejected'))) {
        error.value = 'Invalid file format. Please ensure your JSON file contains both "project" and "requirements" sections. Use the template for guidance.'
      } else if (errorMessage.includes('Project name is required')) {
        error.value = 'Project name is required in the JSON file.'
      } else if (errorMessage.includes('Project key is required')) {
        error.value = 'Project key is required in the JSON file.'
      } else if (errorMessage.includes('already exists')) {
        error.value = 'A project with this key or requirement ID already exists.'
      } else if (errorMessage.includes('Invalid import payload') || errorMessage.includes('JSON')) {
        error.value = 'Invalid JSON format. Please check your file structure and syntax.'
      } else {
        error.value = errorMessage
      }
      return null
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
    deleteProject,
    importProject
  }
})
