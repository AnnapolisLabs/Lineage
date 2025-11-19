import axios, {type AxiosInstance} from 'axios'

const api: AxiosInstance = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

// Request interceptor to add auth token
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('auth_token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// Response interceptor for error handling
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('auth_token')
            globalThis.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

// Export function for CSV/JSON/Markdown files
export async function exportData(projectId: string, format: 'csv' | 'json' | 'markdown') {
    try {
        const response = await api.get(`/projects/${projectId}/export/${format}`, {
            responseType: 'blob'
        })

        // Create download link
        const blob = new Blob([response.data], {
            type: format === 'csv' ? 'text/csv' : (format === 'json' ? 'application/json' : 'text/plain')
        })
        const url = globalThis.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url

        // Set filename based on format
        link.download = format === 'csv' ? 'requirements.csv' :
            format === 'json' ? 'requirements.json' : 'requirements.md'

        // Trigger download
        document.body.appendChild(link)
        link.click()
        link.remove()
        globalThis.URL.revokeObjectURL(url)

        return true
    } catch (error) {
        console.error(`Failed to export ${format}:`, error)
        throw error
    }
}

export default api
