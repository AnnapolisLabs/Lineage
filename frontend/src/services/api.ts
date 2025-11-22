import axios, {type AxiosInstance} from 'axios'

const api: AxiosInstance = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

let csrfToken: string | null = null
let csrfTokenId: string | null = null

// Function to get a fresh CSRF token
async function getCsrfToken(): Promise<void> {
    try {
        const response = await axios.get('/api/csrf/token')
        csrfToken = response.data.token
        csrfTokenId = response.data.tokenId || generateTokenId()
        console.log('CSRF token refreshed:', csrfToken ? 'SUCCESS' : 'FAILED')
    } catch (error) {
        console.error('Failed to get CSRF token:', error)
        csrfToken = null
        csrfTokenId = null
    }
}

// Generate a simple token ID for client-side tracking
function generateTokenId(): string {
    return Math.random().toString(36).substring(2, 15) + 
           Math.random().toString(36).substring(2, 15)
}

// Request interceptor to add auth token and CSRF token
api.interceptors.request.use(
    async (config) => {
        // Add JWT token
        const authToken = localStorage.getItem('auth_token')
        if (authToken) {
            config.headers.Authorization = `Bearer ${authToken}`
        }

        // Handle FormData - remove Content-Type to let browser set it with boundary
        if (config.data instanceof FormData) {
            console.log('🔧 DEBUG: FormData detected, removing Content-Type header for multipart upload')
            console.log('🔧 DEBUG: Original headers:', config.headers)
            delete config.headers['Content-Type']
            console.log('🔧 DEBUG: Updated headers:', config.headers)
        }

        // Ensure we have a CSRF token for state-changing requests
        const method = config.method?.toLowerCase()
        if (method && ['post', 'put', 'patch', 'delete'].includes(method)) {
            if (!csrfToken) {
                await getCsrfToken()
            }
            
            if (csrfToken && csrfTokenId) {
                config.headers['X-CSRF-TOKEN'] = csrfToken
                config.headers['X-CSRF-TOKEN-ID'] = csrfTokenId
            }
        }

        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// Response interceptor for error handling and CSRF token refresh
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        // Handle authentication errors
        if (error.response?.status === 401) {
            localStorage.removeItem('auth_token')
            globalThis.location.href = '/login'
            return Promise.reject(error)
        }
        
        // Handle CSRF token errors
        if (error.response?.status === 403 && 
            error.response.data?.error?.includes('CSRF')) {
            console.log('CSRF token invalid, refreshing...')
            await getCsrfToken()
            
            // Retry the original request with new token
            const originalRequest = error.config
            if (originalRequest && !originalRequest._retry) {
                originalRequest._retry = true
                return api(originalRequest)
            }
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
