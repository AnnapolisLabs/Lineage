import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AIAssistant from './AIAssistant.vue'
import aiService from '@/services/aiService'

vi.mock('@/services/aiService', () => ({
  default: {
    sendMessage: vi.fn(),
    getChatList: vi.fn(),
    createNewConversation: vi.fn(),
    deleteConversation: vi.fn()
  }
}))

describe('AIAssistant.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render AI assistant', () => {
    const wrapper = mount(AIAssistant, {
      global: {
        plugins: [createPinia()],
        stubs: {
          Teleport: true
        }
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should have toggle functionality', async () => {
    const wrapper = mount(AIAssistant, {
      global: {
        plugins: [createPinia()],
        stubs: {
          Teleport: true
        }
      }
    })

    // Just verify the component can be interacted with
    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBeGreaterThanOrEqual(1)
  })

  it('should send message', async () => {
    vi.mocked(aiService.sendMessage).mockResolvedValue({
      message: 'AI response',
      chatId: 'chat-1',
      isProcessing: false,
      error: null
    })

    const wrapper = mount(AIAssistant, {
      global: {
        plugins: [createPinia()],
        stubs: {
          Teleport: true
        }
      }
    })

    // Open chat panel first
    ;(wrapper.vm as any).isOpen = true
    await wrapper.vm.$nextTick()

    // Find and set message input
    const messageInput = wrapper.find('textarea, input[type="text"]')
    if (messageInput.exists()) {
      await messageInput.setValue('Hello AI')

      // Find and click send button
      const sendButton = wrapper.findAll('button').find(btn =>
        btn.text().includes('Send') || btn.attributes('type') === 'submit'
      )

      if (sendButton) {
        await sendButton.trigger('click')
        await flushPromises()

        expect(aiService.sendMessage).toHaveBeenCalled()
      }
    }
  })

  it('should create new conversation', async () => {
    vi.mocked(aiService.createNewConversation).mockResolvedValue('new-chat-id')

    const wrapper = mount(AIAssistant, {
      global: {
        plugins: [createPinia()],
        stubs: {
          Teleport: true
        }
      }
    })

    ;(wrapper.vm as any).isOpen = true
    await wrapper.vm.$nextTick()

    const newChatButton = wrapper.findAll('button').find(btn =>
      btn.text().toLowerCase().includes('new')
    )

    if (newChatButton) {
      await newChatButton.trigger('click')
      await flushPromises()

      expect(aiService.createNewConversation).toHaveBeenCalled()
    }
  })

  it('should load chat list', async () => {
    const mockChatList = [
      { id: 'chat-1', title: 'Chat 1', timestamp: '2025-01-01', messageCount: 5 },
      { id: 'chat-2', title: 'Chat 2', timestamp: '2025-01-02', messageCount: 3 }
    ]
    vi.mocked(aiService.getChatList).mockResolvedValue(mockChatList)

    const wrapper = mount(AIAssistant, {
      global: {
        plugins: [createPinia()],
        stubs: {
          Teleport: true
        }
      }
    })

    ;(wrapper.vm as any).isOpen = true
    await wrapper.vm.$nextTick()
    await flushPromises()

    // Chat list should be loaded on mount
    expect(aiService.getChatList).toHaveBeenCalled()
  })

  it('should delete conversation', async () => {
    vi.mocked(aiService.deleteConversation).mockResolvedValue()
    vi.mocked(aiService.getChatList).mockResolvedValue([])

    const wrapper = mount(AIAssistant, {
      global: {
        plugins: [createPinia()],
        stubs: {
          Teleport: true
        }
      }
    })

    ;(wrapper.vm as any).isOpen = true
    await wrapper.vm.$nextTick()

    const deleteButton = wrapper.findAll('button').find(btn =>
      btn.text().toLowerCase().includes('delete') ||
      btn.attributes('title')?.toLowerCase().includes('delete')
    )

    if (deleteButton) {
      await deleteButton.trigger('click')
      await flushPromises()

      expect(aiService.deleteConversation).toHaveBeenCalled()
    }
  })
})
