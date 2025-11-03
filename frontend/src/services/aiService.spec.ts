import { describe, it, expect, beforeEach, vi } from 'vitest'
import { AIService } from './aiService'

describe('AIService', () => {
  let service: AIService
  let fetchMock: any

  beforeEach(() => {
    service = new AIService()
    fetchMock = vi.fn()
    global.fetch = fetchMock
    localStorage.clear()
    localStorage.setItem('auth_token', 'test-token')
  })

  describe('sendMessage', () => {
    it('should send message successfully', async () => {
      const mockResponse = {
        message: 'AI response',
        chatId: 'chat-123',
        isProcessing: false,
        error: null
      }
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => mockResponse
      })

      const result = await service.sendMessage('Hello', 'project-1', 'chat-123')

      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/api/ai/chat'),
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer test-token'
          },
          body: JSON.stringify({
            message: 'Hello',
            projectId: 'project-1',
            chatId: 'chat-123'
          })
        })
      )
      expect(result).toEqual(mockResponse)
    })

    it('should throw error on failed request', async () => {
      fetchMock.mockResolvedValue({
        ok: false,
        statusText: 'Bad Request'
      })

      await expect(
        service.sendMessage('Hello', 'project-1')
      ).rejects.toThrow('API error: Bad Request')
    })
  })

  describe('createNewConversation', () => {
    it('should create new conversation successfully', async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        text: async () => 'new-chat-id'
      })

      const result = await service.createNewConversation()

      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/api/ai/chat/new'),
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Authorization': 'Bearer test-token'
          }
        })
      )
      expect(result).toBe('new-chat-id')
    })

    it('should throw error on failed request', async () => {
      fetchMock.mockResolvedValue({
        ok: false,
        statusText: 'Unauthorized'
      })

      await expect(service.createNewConversation()).rejects.toThrow('API error: Unauthorized')
    })
  })

  describe('getChatList', () => {
    it('should get chat list successfully', async () => {
      const mockChatList = [
        { id: 'chat-1', title: 'Chat 1', timestamp: '2025-01-01', messageCount: 5 },
        { id: 'chat-2', title: 'Chat 2', timestamp: '2025-01-02', messageCount: 3 }
      ]
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => mockChatList
      })

      const result = await service.getChatList()

      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/api/ai/chat/history'),
        expect.objectContaining({
          method: 'GET',
          headers: {
            'Authorization': 'Bearer test-token'
          }
        })
      )
      expect(result).toEqual(mockChatList)
    })

    it('should throw error on failed request', async () => {
      fetchMock.mockResolvedValue({
        ok: false,
        statusText: 'Internal Server Error'
      })

      await expect(service.getChatList()).rejects.toThrow('API error: Internal Server Error')
    })
  })

  describe('deleteConversation', () => {
    it('should delete conversation successfully', async () => {
      fetchMock.mockResolvedValue({
        ok: true
      })

      await service.deleteConversation('chat-123')

      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/api/ai/chat/chat-123'),
        expect.objectContaining({
          method: 'DELETE',
          headers: {
            'Authorization': 'Bearer test-token'
          }
        })
      )
    })

    it('should throw error on failed request', async () => {
      fetchMock.mockResolvedValue({
        ok: false,
        statusText: 'Not Found'
      })

      await expect(service.deleteConversation('chat-123')).rejects.toThrow('API error: Not Found')
    })
  })
})
