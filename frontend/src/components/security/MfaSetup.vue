<template>
  <div class="mfa-setup">
    <div class="setup-header">
      <h3>Multi-Factor Authentication (MFA)</h3>
      <p>Enhance your account security with two-factor authentication</p>
    </div>

    <!-- MFA Status -->
    <div v-if="isMfaEnabled" class="mfa-status enabled">
      <div class="status-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </div>
      <div class="status-content">
        <h4>MFA is Enabled</h4>
        <p>Your account is protected with multi-factor authentication</p>
        <button @click="showDisableDialog = true" class="btn-outline">
          Disable MFA
        </button>
      </div>
    </div>

    <!-- MFA Setup -->
    <div v-else-if="mfaSetup && !mfaSetup.setupComplete" class="mfa-setup-section">
      <div class="setup-steps">
        <div class="step">
          <div class="step-number">1</div>
          <div class="step-content">
            <h4>Install Authenticator App</h4>
            <p>Download and install an authenticator app like Google Authenticator, Authy, or Microsoft Authenticator on your phone.</p>
            <div class="app-links">
              <a href="https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2" target="_blank" class="app-link">
                Google Authenticator (Android)
              </a>
              <a href="https://apps.apple.com/app/google-authenticator/id388497605" target="_blank" class="app-link">
                Google Authenticator (iOS)
              </a>
            </div>
          </div>
        </div>

        <div class="step">
          <div class="step-number">2</div>
          <div class="step-content">
            <h4>Scan QR Code</h4>
            <p>Open your authenticator app and scan this QR code:</p>
            <div v-if="mfaSetup.qrCodeUrl" class="qr-code-container">
              <img :src="generateQrCodeUrl(mfaSetup.qrCodeUrl)" alt="MFA QR Code" class="qr-code" />
            </div>
            <p class="manual-code">
              Or enter this code manually: <code>{{ mfaSetup.secretKey }}</code>
            </p>
          </div>
        </div>

        <div class="step">
          <div class="step-number">3</div>
          <div class="step-content">
            <h4>Enter Verification Code</h4>
            <p>Enter the 6-digit code from your authenticator app:</p>
            <form @submit.prevent="handleEnableMfa" class="verification-form">
              <input
                v-model="verificationCode"
                type="text"
                placeholder="000000"
                maxlength="6"
                class="code-input"
                required
              />
              <button type="submit" class="btn-primary" :disabled="!verificationCode || verificationCode.length !== 6">
                Enable MFA
              </button>
            </form>
          </div>
        </div>
      </div>

      <!-- Backup Codes -->
      <div v-if="mfaSetup.backupCodes" class="backup-codes-section">
        <h4>Save Your Backup Codes</h4>
        <p class="warning-text">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
            <line x1="12" y1="9" x2="12" y2="13"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
          Important: Save these backup codes in a safe place. You can use them to access your account if you lose your device.
        </p>
        <div class="backup-codes-grid">
          <div v-for="code in mfaSetup.backupCodes" :key="code" class="backup-code">
            {{ code }}
          </div>
        </div>
        <button @click="downloadBackupCodes" class="btn-secondary">
          Download Backup Codes
        </button>
      </div>
    </div>

    <!-- Setup MFA Button -->
    <div v-else class="mfa-setup-prompt">
      <div class="setup-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
          <circle cx="12" cy="16" r="1"/>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
      </div>
      <h4>Enable Multi-Factor Authentication</h4>
      <p>Add an extra layer of security to your account with MFA</p>
      <button @click="startMfaSetup" class="btn-primary" :disabled="loading">
        {{ loading ? 'Setting up...' : 'Set up MFA' }}
      </button>
    </div>

    <!-- Disable MFA Dialog -->
    <div v-if="showDisableDialog" class="modal-overlay" @click="showDisableDialog = false">
      <div class="modal-content" @click.stop>
        <h3>Disable Multi-Factor Authentication</h3>
        <p>Are you sure you want to disable MFA? This will make your account less secure.</p>

        <div class="disable-form">
          <label for="disableCode">Enter your current MFA code to confirm:</label>
          <input
            id="disableCode"
            v-model="disableCode"
            type="text"
            placeholder="000000"
            maxlength="6"
            class="code-input"
          />
        </div>

        <div class="modal-actions">
          <button @click="showDisableDialog = false" class="btn-secondary">
            Cancel
          </button>
          <button
            @click="handleDisableMfa"
            class="btn-danger"
            :disabled="!disableCode || disableCode.length !== 6"
          >
            Disable MFA
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useSecurityStore } from '@/stores/security'
import type { MfaSetupResponse } from '@/services/securityService'

interface Props {
  mfaSetup: MfaSetupResponse | null
  loading?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'setup-mfa': []
  'enable-mfa': [code: string]
  'disable-mfa': [code: string]
}>()

const securityStore = useSecurityStore()
const verificationCode = ref('')
const disableCode = ref('')
const showDisableDialog = ref(false)

const isMfaEnabled = computed(() => props.mfaSetup?.enabled || false)

function startMfaSetup() {
  emit('setup-mfa')
}

function handleEnableMfa() {
  if (verificationCode.value.length === 6) {
    emit('enable-mfa', verificationCode.value)
    verificationCode.value = ''
  }
}

function handleDisableMfa() {
  if (disableCode.value.length === 6) {
    emit('disable-mfa', disableCode.value)
    disableCode.value = ''
    showDisableDialog.value = false
  }
}

function generateQrCodeUrl(qrCodeUrl: string): string {
  // In a real implementation, you might need to generate the QR code URL
  // For now, we'll assume the backend provides the full URL
  return qrCodeUrl
}

function downloadBackupCodes() {
  if (!props.mfaSetup?.backupCodes) return

  const codes = props.mfaSetup.backupCodes.join('\n')
  const blob = new Blob([codes], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'mfa-backup-codes.txt'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.mfa-setup {
  max-width: 600px;
}

.setup-header {
  margin-bottom: 2rem;
}

.setup-header h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}

.setup-header p {
  margin: 0;
  color: #6b7280;
}

.mfa-status {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: 8px;
  margin-bottom: 2rem;
}

.mfa-status.enabled {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.status-icon {
  color: #16a34a;
}

.status-content h4 {
  margin: 0 0 0.5rem 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
}

.status-content p {
  margin: 0 0 1rem 0;
  color: #4b5563;
}

.mfa-setup-section {
  background: #f9fafb;
  border-radius: 8px;
  padding: 2rem;
  border: 1px solid #e5e7eb;
}

.setup-steps {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  margin-bottom: 2rem;
}

.step {
  display: flex;
  gap: 1rem;
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #3b82f6;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.step-content h4 {
  margin: 0 0 0.5rem 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
}

.step-content p {
  margin: 0 0 1rem 0;
  color: #4b5563;
  line-height: 1.5;
}

.app-links {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.app-link {
  color: #3b82f6;
  text-decoration: none;
  font-size: 0.875rem;
}

.app-link:hover {
  text-decoration: underline;
}

.qr-code-container {
  display: inline-block;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  margin: 1rem 0;
}

.qr-code {
  width: 150px;
  height: 150px;
}

.manual-code {
  margin: 1rem 0 0 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.manual-code code {
  background: #e5e7eb;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: monospace;
}

.verification-form {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-top: 1rem;
}

.code-input {
  width: 120px;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 1rem;
  font-family: monospace;
  text-align: center;
  letter-spacing: 0.5rem;
}

.backup-codes-section {
  border-top: 1px solid #e5e7eb;
  padding-top: 2rem;
}

.backup-codes-section h4 {
  margin: 0 0 1rem 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
}

.warning-text {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 1rem;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 6px;
  color: #92400e;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.backup-codes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.backup-code {
  background: #f3f4f6;
  padding: 0.5rem;
  border-radius: 4px;
  text-align: center;
  font-family: monospace;
  font-size: 0.875rem;
  color: #374151;
}

.mfa-setup-prompt {
  text-align: center;
  padding: 3rem 2rem;
  background: #f9fafb;
  border: 2px dashed #d1d5db;
  border-radius: 8px;
}

.setup-icon {
  color: #9ca3af;
  margin-bottom: 1rem;
}

.mfa-setup-prompt h4 {
  margin: 0 0 0.5rem 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
}

.mfa-setup-prompt p {
  margin: 0 0 2rem 0;
  color: #6b7280;
}

.modal-overlay {
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
}

.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  width: 100%;
  max-width: 400px;
}

.modal-content h3 {
  margin: 0 0 1rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}

.modal-content p {
  margin: 0 0 1.5rem 0;
  color: #4b5563;
}

.disable-form {
  margin-bottom: 2rem;
}

.disable-form label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

.btn-primary,
.btn-secondary,
.btn-outline,
.btn-danger {
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

.btn-outline {
  background: transparent;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.btn-outline:hover {
  background: #f9fafb;
}

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-danger:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .step {
    flex-direction: column;
    gap: 0.5rem;
  }

  .verification-form {
    flex-direction: column;
  }

  .code-input {
    width: 100%;
  }

  .backup-codes-grid {
    grid-template-columns: 1fr;
  }
}
</style>