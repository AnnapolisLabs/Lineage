import axios, {type AxiosInstance} from 'axios'

const api: AxiosInstance = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

let csrfToken: string | null = null
let csrfTokenId: string | null = null

// Track when the current CSRF token was last refreshed so we can
// proactively renew it based on user activity.
let lastCsrfRefreshAt = 0

// Basic inactivity/session tracking for UX warnings. This is a
// frontend-only heuristic and does not change backend session TTL,
// but it gives users a clear signal that their session is about to
// expire and lets us opportunistically refresh CSRF tokens.
const INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000 // 30 minutes
const WARNING_BEFORE_EXPIRY_MS = 2 * 60 * 1000 // warn 2 minutes before
const CSRF_PROACTIVE_REFRESH_INTERVAL_MS = 5 * 60 * 1000 // refresh every 5 minutes of activity

let lastUserActivityAt = Date.now()
let warningTimer: number | null = null
let expiryTimer: number | null = null

// Function to get a fresh CSRF token
async function getCsrfToken(): Promise<void> {
    try {
        const response = await axios.get('/api/csrf/token')
        csrfToken = response.data.token
        csrfTokenId = response.data.tokenId || generateTokenId()
        console.log('CSRF token refreshed:', csrfToken ? 'SUCCESS' : 'FAILED')
        lastCsrfRefreshAt = Date.now()
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

// Response interceptor for error handling, CSRF token refresh, and
// post-interaction CSRF/session management.
api.interceptors.response.use(
    (response) => {
        // After a successful state-changing request, drop the cached
        // CSRF token so that the next modifying call fetches a fresh
        // token. This aligns with the backend's single-use semantics
        // and avoids 403s on subsequent interactions.
        const method = response.config?.method?.toLowerCase()
        if (method && ['post', 'put', 'patch', 'delete'].includes(method)) {
            csrfToken = null
            csrfTokenId = null
        }

        return response
    },
    async (error) => {
        // Handle authentication errors
        if (error.response?.status === 401) {
            localStorage.removeItem('auth_token')
            globalThis.location.href = '/login'
            return Promise.reject(error)
        }
        
        // Handle CSRF token errors. The backend returns an error
        // object, so we need to inspect nested fields instead of
        // assuming a plain string.
        if (error.response?.status === 403) {
            const data = error.response.data
            const errorObj = data?.error

            const errorCode = typeof errorObj === 'string'
                ? errorObj
                : (errorObj?.code || errorObj?.error || '')

            const errorMessage = typeof errorObj === 'string'
                ? errorObj
                : (errorObj?.message || '')

            const isCsrfError = [errorCode, errorMessage]
                .filter(Boolean)
                .some((value) => String(value).toUpperCase().includes('CSRF'))

            if (isCsrfError) {
                console.log('CSRF token invalid or expired, refreshing...')
                await getCsrfToken()
                
                // Retry the original request with new token
                const originalRequest = error.config
                if (originalRequest && !originalRequest._retry) {
                    originalRequest._retry = true
                    return api(originalRequest)
                }
            }
        }
        
        return Promise.reject(error)
    }
)

// ---------------------------------------------------------------------------
// Session / inactivity management for UX warnings and CSRF auto-renewal
// ---------------------------------------------------------------------------

function scheduleSessionTimers() {
    if (warningTimer !== null) {
        clearTimeout(warningTimer)
    }
    if (expiryTimer !== null) {
        clearTimeout(expiryTimer)
    }

    const now = Date.now()
    const timeSinceActivity = now - lastUserActivityAt
    const remainingUntilTimeout = Math.max(INACTIVITY_TIMEOUT_MS - timeSinceActivity, 0)
    const warningDelay = Math.max(remainingUntilTimeout - WARNING_BEFORE_EXPIRY_MS, 0)

    // Schedule warning before expiry
    warningTimer = window.setTimeout(async () => {
        const remainingMs = Math.max(INACTIVITY_TIMEOUT_MS - (Date.now() - lastUserActivityAt), 0)

        // Final guard in case of race conditions
        if (remainingMs <= 0) {
            return
        }

        const minutes = Math.ceil(remainingMs / 60000)
        const stayLoggedIn = window.confirm(
            `Your session is about to expire in about ${minutes} minute${minutes === 1 ? '' : 's'}. ` +
            'Click OK to stay signed in.'
        )

        if (stayLoggedIn) {
            // Treat this as activity and proactively refresh CSRF
            lastUserActivityAt = Date.now()

            // If the CSRF token is old or missing, refresh it
            const csrfAge = Date.now() - lastCsrfRefreshAt
            if (!csrfToken || csrfAge > CSRF_PROACTIVE_REFRESH_INTERVAL_MS) {
                await getCsrfToken()
            }

            scheduleSessionTimers()
        }
    }, warningDelay)

    // Schedule expiry handling
    expiryTimer = window.setTimeout(() => {
        // Clear tokens locally; backend may also expire the JWT
        csrfToken = null
        csrfTokenId = null

        // Inform the user and send them to login to obtain a fresh
        // authenticated session.
        alert('Your session has expired due to inactivity. Please log in again.')
        localStorage.removeItem('auth_token')
        globalThis.location.href = '/login'
    }, remainingUntilTimeout)
}

function handleUserActivity() {
    lastUserActivityAt = Date.now()

    // Proactively refresh CSRF token during active usage if it has
    // become stale, so that the next mutating request does not fail
    // with a CSRF 403.
    const csrfAge = Date.now() - lastCsrfRefreshAt
    if (!csrfToken || csrfAge > CSRF_PROACTIVE_REFRESH_INTERVAL_MS) {
        void getCsrfToken()
    }

    scheduleSessionTimers()
}

// Attach global listeners once at module load. This is safe for an SPA
// because the module is loaded a single time and reused across views.
if (typeof window !== 'undefined') {
    window.addEventListener('click', handleUserActivity)
    window.addEventListener('keydown', handleUserActivity)
    window.addEventListener('mousemove', handleUserActivity)
    window.addEventListener('scroll', handleUserActivity)

    // Initialize timers on first load
    scheduleSessionTimers()
}

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
