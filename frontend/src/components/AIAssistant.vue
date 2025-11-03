<template>
  <div class="ai-assistant" :class="{ 'expanded': isExpanded }">
    <!-- Toggle Button -->
    <button
      v-if="!isExpanded"
      @click="isExpanded = true"
      class="ai-toggle-btn"
      title="Open AI Assistant"
    >
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
      </svg>
      <span class="ai-badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
    </button>

    <!-- Chat Panel -->
    <div v-else class="ai-chat-panel">
      <!-- Header -->
      <div class="ai-header">
        <div class="ai-header-content">
          <button @click="showChatList = !showChatList" class="ai-menu-btn" title="Chat History">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <svg class="w-5 h-5 text-annapolis-teal" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
          <h3 class="ai-title">AI Assistant</h3>
        </div>
        <div class="ai-header-actions">
          <button @click="startNewChat" class="ai-new-chat-btn" title="New Chat">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
          </button>
          <button @click="isExpanded = false" class="ai-close-btn">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Chat History Sidebar -->
      <div v-if="showChatList" class="ai-chat-list">
        <div class="ai-chat-list-header">
          <h4>Chat History</h4>
          <button @click="showChatList = false" class="ai-list-close">×</button>
        </div>
        <div class="ai-chat-list-items">
          <div
            v-for="chat in chatList"
            :key="chat.id"
            class="ai-chat-item"
            :class="{ 'active': chat.id === currentChatId }"
            @click="loadChat(chat.id)"
          >
            <div class="ai-chat-item-content">
              <div class="ai-chat-item-title">{{ chat.title }}</div>
              <div class="ai-chat-item-meta">
                {{ chat.messageCount }} messages • {{ formatTime(chat.timestamp) }}
              </div>
            </div>
            <button @click.stop="deleteChat(chat.id)" class="ai-chat-delete" title="Delete">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
          <div v-if="chatList.length === 0" class="ai-chat-list-empty">
            No chat history yet
          </div>
        </div>
      </div>

      <!-- Messages -->
      <div class="ai-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="ai-empty-state">
          <svg class="w-12 h-12 text-annapolis-teal/50 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <p class="text-annapolis-gray-400 text-sm">Ask me anything about your requirements!</p>
          <div class="ai-suggestions">
            <button @click="sendSuggestion('List all requirements in this project')" class="ai-suggestion-btn">
              List requirements
            </button>
            <button @click="sendSuggestion('Create a new requirement for user authentication')" class="ai-suggestion-btn">
              Create requirement
            </button>
            <button @click="sendSuggestion('Show me high priority requirements')" class="ai-suggestion-btn">
              Filter by priority
            </button>
          </div>
        </div>

        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="ai-message"
          :class="msg.role"
        >
          <div class="ai-message-content">
            <div class="ai-message-avatar" :class="msg.role">
              <svg v-if="msg.role === 'user'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <div class="ai-message-text">
              {{ msg.content }}
            </div>
          </div>
        </div>

        <div v-if="isProcessing" class="ai-message assistant">
          <div class="ai-message-content">
            <div class="ai-message-avatar assistant">
              <svg class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </div>
            <div class="ai-message-text">
              <span class="ai-thinking">Thinking...</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input -->
      <div class="ai-input-container">
        <form @submit.prevent="sendMessage" class="ai-input-form">
          <input
            v-model="inputText"
            type="text"
            placeholder="Ask me anything..."
            class="ai-input"
            :disabled="isProcessing"
          />
          <button
            type="submit"
            class="ai-send-btn"
            :disabled="!inputText.trim() || isProcessing"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import aiService, { type ChatListItem } from '@/services/aiService'

const route = useRoute()

// State
const isExpanded = ref(false)
const isProcessing = ref(false)
const messages = ref<Array<{ role: 'user' | 'assistant', content: string }>>([])
const inputText = ref('')
const unreadCount = ref(0)
const messagesContainer = ref<HTMLElement | null>(null)
const showChatList = ref(false)
const chatList = ref<ChatListItem[]>([])
const currentChatId = ref<string | undefined>(undefined)

onMounted(() => {
  refreshChatList()
})

async function refreshChatList() {
  try {
    chatList.value = await aiService.getChatList()
  } catch (error) {
    console.error('Failed to load chat list:', error)
  }
}

async function startNewChat() {
  try {
    const newChatId = await aiService.createNewConversation()
    currentChatId.value = newChatId
    messages.value = []
    showChatList.value = false
    await refreshChatList()
  } catch (error) {
    console.error('Failed to create new chat:', error)
  }
}

function loadChat(chatId: string) {
  // For now, just switch to the chat - messages will load from backend
  currentChatId.value = chatId
  messages.value = []
  showChatList.value = false
  // Note: In a full implementation, you'd want to load the chat history here
  // by adding a GET /api/ai/chat/{chatId} endpoint on the backend
}

async function deleteChat(chatId: string) {
  if (confirm('Delete this conversation?')) {
    try {
      await aiService.deleteConversation(chatId)
      if (chatId === currentChatId.value) {
        await startNewChat()
      }
      await refreshChatList()
    } catch (error) {
      console.error('Failed to delete chat:', error)
    }
  }
}

function formatTime(timestamp: string): string {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return 'Just now'
  if (minutes < 60) return `${minutes}m ago`
  if (hours < 24) return `${hours}h ago`
  if (days < 7) return `${days}d ago`
  return date.toLocaleDateString()
}

async function sendMessage() {
  if (!inputText.value.trim() || isProcessing.value) {
    return
  }

  const userMessage = inputText.value.trim()
  inputText.value = ''

  // Add user message to UI
  messages.value.push({ role: 'user', content: userMessage })
  scrollToBottom()

  // Reset unread count
  unreadCount.value = 0

  // Send to backend
  isProcessing.value = true

  try {
    // Get current project ID from route
    const projectId = (route.params.id as string) || ''

    // Call backend API
    const response = await aiService.sendMessage(userMessage, projectId, currentChatId.value)

    // Update chatId if this was a new conversation
    if (response.chatId && !currentChatId.value) {
      currentChatId.value = response.chatId
      await refreshChatList()
    }

    // Display response
    if (response.message) {
      messages.value.push({
        role: 'assistant',
        content: response.message
      })
    }

    if (response.error) {
      messages.value.push({
        role: 'assistant',
        content: `Error: ${response.error}`
      })
    }

  } catch (error: any) {
    messages.value.push({
      role: 'assistant',
      content: `Error: ${error.message}`
    })
  } finally {
    isProcessing.value = false
    scrollToBottom()
  }
}

function sendSuggestion(suggestion: string) {
  inputText.value = suggestion
  sendMessage()
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
}

.ai-toggle-btn {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #00BFA5 0%, #00838F 100%);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 191, 165, 0.4);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-toggle-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 191, 165, 0.6);
}

.ai-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #FF5252;
  color: white;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.ai-chat-panel {
  width: 420px;
  height: 600px;
  background: rgba(30, 41, 59, 0.98);
  border: 1px solid rgba(0, 191, 165, 0.2);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(0, 191, 165, 0.1);
  border-bottom: 1px solid rgba(0, 191, 165, 0.2);
}

.ai-header-content {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.ai-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-menu-btn,
.ai-new-chat-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  padding: 4px;
  display: flex;
  transition: color 0.2s;
}

.ai-menu-btn:hover,
.ai-new-chat-btn:hover {
  color: white;
}

.ai-title {
  color: white;
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.ai-close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  padding: 4px;
  display: flex;
  transition: color 0.2s;
}

.ai-close-btn:hover {
  color: white;
}

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.ai-suggestions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}

.ai-suggestion-btn {
  padding: 8px 16px;
  background: rgba(0, 191, 165, 0.1);
  border: 1px solid rgba(0, 191, 165, 0.3);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.ai-suggestion-btn:hover {
  background: rgba(0, 191, 165, 0.2);
  border-color: rgba(0, 191, 165, 0.5);
  color: white;
}

.ai-message {
  display: flex;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-message-content {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.ai-message.user .ai-message-content {
  margin-left: auto;
  flex-direction: row-reverse;
}

.ai-message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-message-avatar.user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.ai-message-avatar.assistant {
  background: linear-gradient(135deg, #00BFA5 0%, #00838F 100%);
  color: white;
}

.ai-message-text {
  background: rgba(255, 255, 255, 0.05);
  padding: 10px 14px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-message.user .ai-message-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.ai-thinking {
  color: rgba(255, 255, 255, 0.6);
  font-style: italic;
}

.ai-input-container {
  padding: 16px 20px;
  border-top: 1px solid rgba(0, 191, 165, 0.2);
  background: rgba(0, 0, 0, 0.2);
}

.ai-input-form {
  display: flex;
  gap: 10px;
}

.ai-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(0, 191, 165, 0.3);
  border-radius: 12px;
  padding: 10px 14px;
  color: white;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.ai-input:focus {
  border-color: rgba(0, 191, 165, 0.6);
  background: rgba(255, 255, 255, 0.08);
}

.ai-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.ai-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-send-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #00BFA5 0%, #00838F 100%);
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.ai-send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 191, 165, 0.4);
}

.ai-send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Scrollbar styling */
.ai-messages::-webkit-scrollbar {
  width: 6px;
}

.ai-messages::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
}

.ai-messages::-webkit-scrollbar-thumb {
  background: rgba(0, 191, 165, 0.3);
  border-radius: 3px;
}

.ai-messages::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 191, 165, 0.5);
}

/* Chat History Sidebar */
.ai-chat-list {
  position: absolute;
  left: 0;
  top: 60px;
  bottom: 0;
  width: 280px;
  background: rgba(20, 30, 48, 0.98);
  border-right: 1px solid rgba(0, 191, 165, 0.2);
  display: flex;
  flex-direction: column;
  z-index: 10;
}

.ai-chat-list-header {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 191, 165, 0.2);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ai-chat-list-header h4 {
  color: white;
  font-size: 14px;
  font-weight: 600;
  margin: 0;
}

.ai-list-close {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-list-close:hover {
  color: white;
}

.ai-chat-list-items {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.ai-chat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  margin-bottom: 4px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.ai-chat-item:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(0, 191, 165, 0.3);
}

.ai-chat-item.active {
  background: rgba(0, 191, 165, 0.15);
  border-color: rgba(0, 191, 165, 0.5);
}

.ai-chat-item-content {
  flex: 1;
  min-width: 0;
}

.ai-chat-item-title {
  color: white;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.ai-chat-item-meta {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
}

.ai-chat-delete {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  padding: 4px;
  display: flex;
  opacity: 0;
  transition: all 0.2s;
}

.ai-chat-item:hover .ai-chat-delete {
  opacity: 1;
}

.ai-chat-delete:hover {
  color: #F44336;
}

.ai-chat-list-empty {
  text-align: center;
  padding: 32px 16px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}
</style>
