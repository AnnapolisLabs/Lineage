<template>
  <div class="avatar-upload-modal" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>Change Profile Picture</h3>
        <button @click="$emit('close')" class="close-button">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>

      <div class="modal-body">
        <div class="current-avatar">
          <div class="avatar-preview">
            <img
              v-if="currentAvatar"
              :src="currentAvatar"
              alt="Current avatar"
              class="avatar-image"
            />
            <div v-else class="avatar-placeholder">
              {{ getInitials() }}
            </div>
          </div>
          <p class="avatar-label">Current Picture</p>
        </div>

        <div class="upload-section">
          <div
            @dragover.prevent
            @drop.prevent="handleDrop"
            class="upload-area"
            :class="{ 'drag-over': isDragOver }"
          >
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              @change="handleFileSelect"
              class="file-input"
            />

            <div v-if="!selectedFile" class="upload-placeholder">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <path d="M14.828 14.828a4 4 0 0 1-5.656 0M9 10h1.586a1 1 0 0 1 .707.293l.707.707A1 1 0 0 0 12.414 11H15m-3-3h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
              </svg>
              <p>Drag and drop an image here, or click to browse</p>
              <button @click="$refs.fileInput?.click()" class="btn-secondary">
                Choose File
              </button>
            </div>

            <div v-else class="file-preview">
              <img :src="previewUrl" alt="Preview" class="preview-image" />
              <div class="file-info">
                <p class="file-name">{{ selectedFile.name }}</p>
                <p class="file-size">{{ formatFileSize(selectedFile.size) }}</p>
              </div>
              <button @click="clearSelection" class="remove-file">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>

          <div class="upload-info">
            <p>Supported formats: JPG, PNG, GIF</p>
            <p>Maximum file size: 5MB</p>
            <p>Recommended: Square images, at least 200x200px</p>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button @click="$emit('close')" class="btn-secondary">
          Cancel
        </button>
        <button
          @click="handleUpload"
          class="btn-primary"
          :disabled="!selectedFile || uploading"
        >
          {{ uploading ? 'Uploading...' : 'Upload Picture' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  currentAvatar?: string | null
  userName?: string
  userEmail?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'close': []
  'upload': [file: File]
}>()

const fileInput = ref<HTMLInputElement>()
const selectedFile = ref<File | null>(null)
const isDragOver = ref(false)
const uploading = ref(false)

const previewUrl = computed(() => {
  if (!selectedFile.value) return ''
  return URL.createObjectURL(selectedFile.value)
})

function getInitials(): string {
  const name = props.userName || ''
  const email = props.userEmail || ''

  if (name) {
    return name.split(' ').map(n => n.charAt(0)).join('').toUpperCase()
  }

  return email.charAt(0).toUpperCase()
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    validateAndSetFile(file)
  }
}

function handleDrop(event: DragEvent) {
  isDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    validateAndSetFile(file)
  }
}

function validateAndSetFile(file: File) {
  // Validate file type
  if (!file.type.startsWith('image/')) {
    alert('Please select an image file.')
    return
  }

  // Validate file size (5MB)
  if (file.size > 5 * 1024 * 1024) {
    alert('File size must be less than 5MB.')
    return
  }

  selectedFile.value = file
}

function clearSelection() {
  selectedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

async function handleUpload() {
  if (!selectedFile.value) return

  uploading.value = true
  try {
    emit('upload', selectedFile.value)
    emit('close')
  } catch (error) {
    console.error('Upload failed:', error)
  } finally {
    uploading.value = false
  }
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<style scoped>
.avatar-upload-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}

.close-button {
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  padding: 0.25rem;
  border-radius: 4px;
  transition: color 0.2s;
}

.close-button:hover {
  color: #374151;
}

.modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.current-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #f3f4f6;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.5rem;
  font-weight: bold;
}

.avatar-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
}

.upload-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  padding: 2rem;
  text-align: center;
  transition: all 0.2s;
  cursor: pointer;
  position: relative;
}

.upload-area:hover,
.upload-area.drag-over {
  border-color: #3b82f6;
  background: #eff6ff;
}

.file-input {
  position: absolute;
  opacity: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  color: #6b7280;
}

.upload-placeholder svg {
  color: #9ca3af;
}

.upload-placeholder p {
  margin: 0;
  font-size: 0.875rem;
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.preview-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.file-info {
  flex: 1;
  text-align: left;
}

.file-name {
  margin: 0 0 0.25rem 0;
  font-size: 0.875rem;
  font-weight: 500;
  color: #1f2937;
}

.file-size {
  margin: 0;
  font-size: 0.75rem;
  color: #6b7280;
}

.remove-file {
  background: none;
  border: none;
  cursor: pointer;
  color: #ef4444;
  padding: 0.25rem;
  border-radius: 4px;
  transition: background 0.2s;
}

.remove-file:hover {
  background: #fef2f2;
}

.upload-info {
  font-size: 0.75rem;
  color: #6b7280;
  line-height: 1.4;
}

.upload-info p {
  margin: 0 0 0.25rem 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.btn-primary,
.btn-secondary {
  padding: 0.75rem 1.5rem;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-size: 0.875rem;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-primary:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6b7280;
  color: white;
}

.btn-secondary:hover {
  background: #4b5563;
}

@media (max-width: 640px) {
  .modal-content {
    margin: 1rem;
  }

  .modal-body {
    padding: 1rem;
  }

  .upload-area {
    padding: 1.5rem;
  }

  .file-preview {
    flex-direction: column;
    text-align: center;
  }
}
</style>