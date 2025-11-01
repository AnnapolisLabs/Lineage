<template>
  <div class="requirement-history">
    <!-- Toggle for diff view mode -->
    <div v-if="!loading && !error && history.length > 0" class="flex justify-end mb-4">
      <div class="inline-flex rounded-lg bg-annapolis-navy/60 p-1 border border-annapolis-teal/20">
        <button
          @click="diffViewMode = 'side-by-side'"
          :class="[
            'px-3 py-1.5 text-xs font-medium rounded-md transition-all',
            diffViewMode === 'side-by-side'
              ? 'bg-annapolis-teal text-white shadow-sm'
              : 'text-annapolis-gray-300 hover:text-white'
          ]"
        >
          Side-by-Side
        </button>
        <button
          @click="diffViewMode = 'unified'"
          :class="[
            'px-3 py-1.5 text-xs font-medium rounded-md transition-all',
            diffViewMode === 'unified'
              ? 'bg-annapolis-teal text-white shadow-sm'
              : 'text-annapolis-gray-300 hover:text-white'
          ]"
        >
          Unified Redline
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-8">
      <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-annapolis-teal"></div>
    </div>

    <div v-else-if="error" class="bg-red-500/10 border border-red-500/30 text-red-400 px-4 py-3 rounded-lg">
      {{ error }}
    </div>

    <div v-else-if="history.length === 0" class="text-annapolis-gray-400 text-center py-16">
      <svg class="mx-auto h-12 w-12 text-annapolis-teal/30 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <p class="text-lg font-medium">No history available</p>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="entry in history"
        :key="entry.id"
        class="border rounded-lg p-4 hover:shadow-lg transition-all"
        :class="getChangeTypeClass(entry.changeType)"
      >
        <div class="flex items-start justify-between mb-2">
          <div class="flex items-center gap-2">
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                  :class="getChangeTypeBadgeClass(entry.changeType)">
              {{ formatChangeType(entry.changeType) }}
            </span>
            <span class="text-sm text-annapolis-gray-300">
              by {{ entry.changedBy || 'Unknown' }}
            </span>
          </div>
          <span class="text-sm text-annapolis-gray-400">
            {{ formatDate(entry.changedAt) }}
          </span>
        </div>

        <!-- Diff View -->
        <div v-if="entry.changeType === 'UPDATED' && entry.oldValue && entry.newValue" class="mt-3">
          <div class="space-y-3">
            <div v-for="field in getChangedFields(entry.oldValue, entry.newValue)" :key="field">
              <div class="text-sm font-medium text-annapolis-gray-200 mb-1">{{ formatFieldName(field) }}</div>

              <!-- Side-by-Side View -->
              <div v-if="diffViewMode === 'side-by-side'" class="grid grid-cols-2 gap-2">
                <div class="bg-red-500/10 border border-red-500/30 rounded-lg p-3">
                  <div class="text-xs text-red-400 font-medium mb-1">Old</div>
                  <div class="text-sm text-annapolis-gray-300 whitespace-pre-wrap break-words">
                    {{ formatValue(entry.oldValue[field]) }}
                  </div>
                </div>
                <div class="bg-green-500/10 border border-green-500/30 rounded-lg p-3">
                  <div class="text-xs text-green-400 font-medium mb-1">New</div>
                  <div class="text-sm text-annapolis-gray-300 whitespace-pre-wrap break-words">
                    {{ formatValue(entry.newValue[field]) }}
                  </div>
                </div>
              </div>

              <!-- Unified Redline View -->
              <div v-else class="bg-annapolis-navy/40 border border-annapolis-gray-600/30 rounded-lg p-3">
                <div class="text-sm leading-relaxed" v-html="generateRedline(entry.oldValue[field], entry.newValue[field])"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Created View -->
        <div v-else-if="entry.changeType === 'CREATED' && entry.newValue" class="mt-3">
          <div class="bg-blue-500/10 border border-blue-500/30 rounded-lg p-3">
            <div class="text-sm text-annapolis-gray-300 space-y-1">
              <div><span class="font-medium text-blue-400">ID:</span> {{ entry.newValue.reqId }}</div>
              <div><span class="font-medium text-blue-400">Title:</span> {{ entry.newValue.title }}</div>
              <div><span class="font-medium text-blue-400">Status:</span> {{ entry.newValue.status }}</div>
              <div><span class="font-medium text-blue-400">Priority:</span> {{ entry.newValue.priority }}</div>
            </div>
          </div>
        </div>

        <!-- Deleted View -->
        <div v-else-if="entry.changeType === 'DELETED' && entry.newValue" class="mt-3">
          <div class="bg-red-500/10 border border-red-500/30 rounded-lg p-3">
            <div class="text-sm text-annapolis-gray-300">
              <div><span class="font-medium text-red-400">Deleted at:</span> {{ formatDate(entry.newValue.deletedAt) }}</div>
              <div><span class="font-medium text-red-400">Deleted by:</span> {{ entry.newValue.deletedBy }}</div>
            </div>
          </div>
        </div>

        <!-- Link Added/Removed View -->
        <div v-else-if="(entry.changeType === 'LINK_ADDED' || entry.changeType === 'LINK_REMOVED') && entry.newValue" class="mt-3">
          <div class="border rounded-lg p-3" :class="entry.changeType === 'LINK_ADDED' ? 'bg-green-500/10 border-green-500/30' : 'bg-orange-500/10 border-orange-500/30'">
            <div class="text-sm text-annapolis-gray-300 space-y-1">
              <div>
                <span class="font-medium" :class="entry.changeType === 'LINK_ADDED' ? 'text-green-400' : 'text-orange-400'">From:</span>
                {{ entry.newValue.fromReqId }} - {{ entry.newValue.fromTitle }}
              </div>
              <div>
                <span class="font-medium" :class="entry.changeType === 'LINK_ADDED' ? 'text-green-400' : 'text-orange-400'">To:</span>
                {{ entry.newValue.toReqId }} - {{ entry.newValue.toTitle }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { requirementService } from '@/services/requirementService'
import { diffWords, diffChars } from 'diff'

const props = defineProps<{
  requirementId: string
}>()

const history = ref<any[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const diffViewMode = ref<'side-by-side' | 'unified'>('side-by-side')

onMounted(async () => {
  await loadHistory()
})

async function loadHistory() {
  loading.value = true
  error.value = null
  try {
    history.value = await requirementService.getHistory(props.requirementId)
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Failed to load history'
  } finally {
    loading.value = false
  }
}

function generateRedline(oldValue: any, newValue: any): string {
  const oldStr = formatValue(oldValue)
  const newStr = formatValue(newValue)

  // If values are very different or contain JSON, use character-level diff
  // Otherwise use word-level diff for better readability
  const shouldUseCharDiff = oldStr.length > 500 || newStr.length > 500 ||
                            oldStr.includes('{') || newStr.includes('{')

  const diff = shouldUseCharDiff ? diffChars(oldStr, newStr) : diffWords(oldStr, newStr)

  let result = ''

  diff.forEach((part) => {
    const escaped = escapeHtml(part.value)

    if (part.added) {
      result += `<span class="bg-green-500/20 text-green-300 px-0.5 rounded">${escaped}</span>`
    } else if (part.removed) {
      result += `<span class="bg-red-500/20 text-red-300 line-through px-0.5 rounded">${escaped}</span>`
    } else {
      result += `<span class="text-annapolis-gray-300">${escaped}</span>`
    }
  })

  return result || '<span class="text-annapolis-gray-400">(empty)</span>'
}

function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  return text.replace(/[&<>"']/g, (m) => map[m])
}

function formatChangeType(type: string): string {
  const map: Record<string, string> = {
    CREATED: 'Created',
    UPDATED: 'Updated',
    DELETED: 'Deleted',
    LINK_ADDED: 'Link Added',
    LINK_REMOVED: 'Link Removed',
    RESTORED: 'Restored'
  }
  return map[type] || type
}

function getChangeTypeClass(type: string): string {
  const map: Record<string, string> = {
    CREATED: 'bg-annapolis-navy/40 border-blue-500/30',
    UPDATED: 'bg-annapolis-navy/40 border-annapolis-gray-600/30',
    DELETED: 'bg-annapolis-navy/40 border-red-500/30',
    LINK_ADDED: 'bg-annapolis-navy/40 border-green-500/30',
    LINK_REMOVED: 'bg-annapolis-navy/40 border-orange-500/30',
    RESTORED: 'bg-annapolis-navy/40 border-purple-500/30'
  }
  return map[type] || 'bg-annapolis-navy/40 border-annapolis-gray-600/30'
}

function getChangeTypeBadgeClass(type: string): string {
  const map: Record<string, string> = {
    CREATED: 'bg-blue-500/20 text-blue-400 border border-blue-500/30',
    UPDATED: 'bg-annapolis-gray-600/20 text-annapolis-gray-300 border border-annapolis-gray-600/30',
    DELETED: 'bg-red-500/20 text-red-400 border border-red-500/30',
    LINK_ADDED: 'bg-green-500/20 text-green-400 border border-green-500/30',
    LINK_REMOVED: 'bg-orange-500/20 text-orange-400 border border-orange-500/30',
    RESTORED: 'bg-purple-500/20 text-purple-400 border border-purple-500/30'
  }
  return map[type] || 'bg-annapolis-gray-600/20 text-annapolis-gray-300 border border-annapolis-gray-600/30'
}

function getChangedFields(oldValue: any, newValue: any): string[] {
  if (!oldValue || !newValue) return []

  const fields = new Set([...Object.keys(oldValue), ...Object.keys(newValue)])
  return Array.from(fields).filter(field => {
    const oldVal = JSON.stringify(oldValue[field])
    const newVal = JSON.stringify(newValue[field])
    return oldVal !== newVal
  })
}

function formatFieldName(field: string): string {
  const map: Record<string, string> = {
    reqId: 'Requirement ID',
    title: 'Title',
    description: 'Description',
    status: 'Status',
    priority: 'Priority',
    parentId: 'Parent ID',
    customFields: 'Custom Fields'
  }
  return map[field] || field
}

function formatValue(value: any): string {
  if (value === null || value === undefined) return '(empty)'
  if (typeof value === 'object') return JSON.stringify(value, null, 2)
  return String(value)
}

function formatDate(dateString: string): string {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString()
}

defineExpose({ loadHistory })
</script>

<style scoped>
.requirement-history {
  max-height: 70vh;
  overflow-y: auto;
}

/* Custom scrollbar styling to match the theme */
.requirement-history::-webkit-scrollbar {
  width: 8px;
}

.requirement-history::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.3);
  border-radius: 4px;
}

.requirement-history::-webkit-scrollbar-thumb {
  background: rgba(96, 165, 250, 0.3);
  border-radius: 4px;
}

.requirement-history::-webkit-scrollbar-thumb:hover {
  background: rgba(96, 165, 250, 0.5);
}
</style>
