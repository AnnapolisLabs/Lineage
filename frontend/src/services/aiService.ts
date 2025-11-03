/**
 * AI Service - Simplified HTTP client for backend AI Agent API
 */

export interface Message {
  role: 'user' | 'assistant' | 'system'
  content: string
}

export interface ChatListItem {
  id: string
  title: string
  timestamp: string
  messageCount: number
}

export interface AIMessageResponse {
  message: string
  chatId: string
  isProcessing: boolean
  error: string | null
}

export class AIService {
  private apiUrl: string

  constructor() {
    this.apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
  }

  /**
   * Send message to AI agent
   */
  async sendMessage(message: string, projectId: string, chatId?: string): Promise<AIMessageResponse> {
    const response = await fetch(`${this.apiUrl}/api/ai/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
      },
      body: JSON.stringify({
        message,
        projectId,
        chatId
      })
    })

    if (!response.ok) {
      throw new Error(`API error: ${response.statusText}`)
    }

    return await response.json()
  }

  /**
   * Create new conversation
   */
  async createNewConversation(): Promise<string> {
    const response = await fetch(`${this.apiUrl}/api/ai/chat/new`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
      }
    })

    if (!response.ok) {
      throw new Error(`API error: ${response.statusText}`)
    }

    return await response.text()
  }

  /**
   * Get chat list
   */
  async getChatList(): Promise<ChatListItem[]> {
    const response = await fetch(`${this.apiUrl}/api/ai/chat/history`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
      }
    })

    if (!response.ok) {
      throw new Error(`API error: ${response.statusText}`)
    }

    return await response.json()
  }

  /**
   * Delete conversation
   */
  async deleteConversation(chatId: string): Promise<void> {
    const response = await fetch(`${this.apiUrl}/api/ai/chat/${chatId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
      }
    })

    if (!response.ok) {
      throw new Error(`API error: ${response.statusText}`)
    }
  }
}

export default new AIService()
