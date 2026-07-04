<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">백업 관리</h1>
        <p class="text-sm text-gray-400 mt-0.5">DB 데이터 및 설정을 암호화 백업·복원합니다</p>
      </div>
    </div>

    <div class="page-body">

    <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">

      <!-- 즉시 백업 -->
      <div class="card">
        <h2 class="text-base font-semibold text-gray-800 mb-4 flex items-center gap-2">
          <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          즉시 백업
        </h2>

        <div class="space-y-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              백업 비밀번호 <span class="text-red-500">*</span>
            </label>
            <div class="relative">
              <input v-model="backupPassword" :type="showBackupPw ? 'text' : 'password'"
                class="input w-full pr-10" placeholder="백업 파일 암호화 비밀번호"/>
              <button @click="showBackupPw = !showBackupPw"
                class="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path v-if="!showBackupPw" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                  <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
          </div>

          <p class="text-xs text-gray-500 bg-yellow-50 border border-yellow-200 rounded p-2">
            비밀번호를 잊으면 백업 파일을 복원할 수 없습니다. 안전한 곳에 보관하세요.
          </p>

          <div class="grid grid-cols-2 gap-2">
            <button @click="downloadBackup" :disabled="!backupPassword || downloading"
              class="btn-primary text-sm disabled:opacity-50 flex items-center justify-center gap-1.5 py-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
              </svg>
              {{ downloading ? '생성 중...' : '다운로드' }}
            </button>
            <button @click="saveBackup" :disabled="!backupPassword || saving"
              class="btn-secondary text-sm disabled:opacity-50 flex items-center justify-center gap-1.5 py-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"/>
              </svg>
              {{ saving ? '저장 중...' : '서버 저장' }}
            </button>
          </div>

          <p v-if="backupMsg" class="text-sm" :class="backupMsgType === 'ok' ? 'text-green-600' : 'text-red-600'">
            {{ backupMsg }}
          </p>
        </div>
      </div>

      <!-- 복원 -->
      <div class="card">
        <h2 class="text-base font-semibold text-gray-800 mb-4 flex items-center gap-2">
          <svg class="w-5 h-5 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
          백업 복원
        </h2>

        <div class="space-y-3">
          <div class="border-2 border-dashed border-gray-300 rounded-lg p-4 text-center hover:border-primary-400 transition-colors cursor-pointer"
            @click="restoreFileInput?.click()"
            @dragover.prevent="dragOver = true"
            @dragleave="dragOver = false"
            @drop.prevent="onFileDrop">
            <svg class="w-8 h-8 text-gray-400 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
            </svg>
            <p class="text-sm text-gray-600">
              {{ restoreFile ? restoreFile.name : '.bak 파일을 클릭하거나 드래그하세요' }}
            </p>
            <input ref="restoreFileInput" type="file" accept=".bak" class="hidden" @change="onFileSelect"/>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">복원 비밀번호 <span class="text-red-500">*</span></label>
            <div class="relative">
              <input v-model="restorePassword" :type="showRestorePw ? 'text' : 'password'"
                class="input w-full pr-10" placeholder="백업 파일 비밀번호"/>
              <button @click="showRestorePw = !showRestorePw"
                class="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
              </button>
            </div>
          </div>

          <div class="p-2 bg-red-50 border border-red-200 rounded text-xs text-red-700">
            복원 시 현재 모든 데이터가 백업 시점으로 덮어쓰여집니다. 신중하게 진행하세요.
          </div>

          <button @click="confirmRestore" :disabled="!restoreFile || !restorePassword || restoring"
            class="w-full py-2 text-sm font-medium text-white bg-orange-500 rounded-lg hover:bg-orange-600 disabled:opacity-50 flex items-center justify-center gap-1.5">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            {{ restoring ? '복원 중...' : '복원 실행' }}
          </button>

          <p v-if="restoreMsg" class="text-sm" :class="restoreMsgType === 'ok' ? 'text-green-600' : 'text-red-600'">
            {{ restoreMsg }}
          </p>
        </div>
      </div>

      <!-- 정기 백업 설정 -->
      <div class="card">
        <h2 class="text-base font-semibold text-gray-800 mb-4 flex items-center gap-2">
          <svg class="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          정기 백업 설정
        </h2>

        <div v-if="loadingConfig" class="text-sm text-gray-400">로딩 중...</div>
        <div v-else class="space-y-4">
          <div class="flex items-center gap-3">
            <input id="backup-enabled" v-model="config.enabled" type="checkbox" class="w-4 h-4 text-primary-600"/>
            <label for="backup-enabled" class="text-sm font-medium text-gray-700">정기 백업 활성화</label>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Cron 표현식</label>
            <input v-model="config.cron" class="input w-full text-sm font-mono" placeholder="0 0 2 * * ?"/>
            <p class="text-xs text-gray-400 mt-1">예: 매일 새벽 2시 → <code class="bg-gray-100 px-1 rounded">0 0 2 * * ?</code></p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">빠른 선택</label>
            <div class="flex flex-wrap gap-1.5">
              <button v-for="preset in cronPresets" :key="preset.cron"
                @click="config.cron = preset.cron"
                class="text-xs px-2 py-1 border rounded-md hover:bg-gray-50 transition-colors"
                :class="config.cron === preset.cron ? 'border-primary-400 text-primary-600 bg-primary-50' : 'border-gray-200 text-gray-600'">
                {{ preset.label }}
              </button>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">보관 개수 최대</label>
            <input v-model.number="config.keepCount" type="number" min="1" max="100" class="input w-24 text-sm"/>
            <span class="ml-2 text-xs text-gray-500">개 (초과 시 오래된 파일 자동 삭제)</span>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              정기 백업 기본 비밀번호
              <span v-if="config.defaultPasswordSet" class="text-xs text-green-600 ml-1">설정됨</span>
            </label>
            <div class="relative">
              <input v-model="config.defaultPassword" :type="showConfigPw ? 'text' : 'password'"
                class="input w-full pr-10 text-sm" placeholder="비밀번호를 입력하면 변경됩니다"/>
              <button @click="showConfigPw = !showConfigPw"
                class="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
              </button>
            </div>
          </div>

          <button @click="saveConfig" :disabled="savingConfig"
            class="w-full btn-primary text-sm disabled:opacity-50">
            {{ savingConfig ? '저장 중...' : '설정 저장' }}
          </button>

          <p v-if="configMsg" class="text-sm" :class="configMsgType === 'ok' ? 'text-green-600' : 'text-red-600'">
            {{ configMsg }}
          </p>
        </div>
      </div>
    </div>

    <!-- 서버 저장 백업 파일 목록 -->
    <div class="card mt-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-semibold text-gray-800 flex items-center gap-2">
          <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4"/>
          </svg>
          서버 저장 백업 파일
        </h2>
        <button @click="loadFiles" class="text-xs text-gray-400 hover:text-gray-600">새로고침</button>
      </div>

      <div v-if="loadingFiles" class="text-sm text-gray-400 text-center py-6">로딩 중...</div>
      <div v-else-if="backupFiles.length === 0" class="text-sm text-gray-400 text-center py-6">
        서버에 저장된 백업 파일이 없습니다.
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b border-gray-100">
            <tr>
              <th class="text-left px-4 py-2.5 font-semibold text-gray-600">파일명</th>
              <th class="text-right px-4 py-2.5 font-semibold text-gray-600">크기</th>
              <th class="text-center px-4 py-2.5 font-semibold text-gray-600">생성일시</th>
              <th class="text-center px-4 py-2.5 font-semibold text-gray-600">작업</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="f in backupFiles" :key="f.filename" class="hover:bg-gray-50">
              <td class="px-4 py-3 font-mono text-xs text-gray-700">{{ f.filename }}</td>
              <td class="px-4 py-3 text-right text-gray-600">{{ formatSize(f.fileSize) }}</td>
              <td class="px-4 py-3 text-center text-gray-600">{{ formatDateTime(f.lastModified) }}</td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button @click="downloadServerFile(f.filename)" class="text-xs text-blue-600 hover:text-blue-800 font-medium">다운로드</button>
                  <button @click="confirmDeleteFile(f.filename)" class="text-xs text-red-500 hover:text-red-700 font-medium">삭제</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 백업 이력 -->
    <div class="card mt-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-semibold text-gray-800 flex items-center gap-2">
          <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
          </svg>
          백업 이력
        </h2>
        <button @click="loadHistory" class="text-xs text-gray-400 hover:text-gray-600">새로고침</button>
      </div>

      <div v-if="loadingHistory" class="text-sm text-gray-400 text-center py-6">로딩 중...</div>
      <div v-else-if="history.length === 0" class="text-sm text-gray-400 text-center py-6">백업 이력이 없습니다.</div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b border-gray-100">
            <tr>
              <th class="text-left px-4 py-2.5 font-semibold text-gray-600">파일명</th>
              <th class="text-center px-4 py-2.5 font-semibold text-gray-600">유형</th>
              <th class="text-center px-4 py-2.5 font-semibold text-gray-600">크기</th>
              <th class="text-center px-4 py-2.5 font-semibold text-gray-600">상태</th>
              <th class="text-left px-4 py-2.5 font-semibold text-gray-600">메시지</th>
              <th class="text-center px-4 py-2.5 font-semibold text-gray-600">일시</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="h in history" :key="h.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 font-mono text-xs text-gray-700 max-w-xs truncate">{{ h.filename || '-' }}</td>
              <td class="px-4 py-3 text-center">
                <span class="px-2 py-0.5 rounded text-xs font-medium"
                  :class="typeClass(h.backupType)">{{ typeLabel(h.backupType) }}</span>
              </td>
              <td class="px-4 py-3 text-center text-gray-600">{{ h.fileSize ? formatSize(h.fileSize) : '-' }}</td>
              <td class="px-4 py-3 text-center">
                <span class="px-2 py-0.5 rounded text-xs font-medium"
                  :class="h.status === 'SUCCESS' ? 'bg-green-100 text-green-700' : h.status === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600'">
                  {{ statusLabel(h.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-gray-500 text-xs max-w-xs truncate">{{ h.message || '-' }}</td>
              <td class="px-4 py-3 text-center text-gray-600 text-xs">{{ formatDateTime(h.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 복원 확인 모달 -->
    <div v-if="showRestoreConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-gray-900">백업 복원 확인</h3>
            <p class="text-sm text-gray-500">이 작업은 되돌릴 수 없습니다</p>
          </div>
        </div>
        <p class="text-sm text-gray-700 mb-2">
          <strong>{{ restoreFile?.name }}</strong> 파일로 현재 데이터를 덮어씁니다.
        </p>
        <p class="text-sm text-red-600 bg-red-50 p-2 rounded mb-5">
          현재 모든 데이터(사용자, 정책, 자산 등)가 백업 시점 데이터로 완전히 교체됩니다.
        </p>
        <div class="flex justify-end gap-3">
          <button @click="showRestoreConfirm = false" class="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="doRestore" :disabled="restoring" class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50">
            {{ restoring ? '복원 중...' : '복원 실행' }}
          </button>
        </div>
      </div>
    </div>
    </div><!-- /page-body -->
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { backupApi } from '@/api'

// 파일 input 템플릿 ref
const restoreFileInput = ref(null)

// 즉시 백업
const backupPassword = ref('')
const showBackupPw = ref(false)
const downloading = ref(false)
const saving = ref(false)
const backupMsg = ref('')
const backupMsgType = ref('ok')

// 복원
const restoreFile = ref(null)
const restorePassword = ref('')
const showRestorePw = ref(false)
const restoring = ref(false)
const restoreMsg = ref('')
const restoreMsgType = ref('ok')
const showRestoreConfirm = ref(false)
const dragOver = ref(false)

// 설정
const loadingConfig = ref(false)
const savingConfig = ref(false)
const showConfigPw = ref(false)
const configMsg = ref('')
const configMsgType = ref('ok')
const config = ref({ enabled: false, cron: '0 0 2 * * ?', keepCount: 10, defaultPasswordSet: false, defaultPassword: '' })

// 파일 목록
const backupFiles = ref([])
const loadingFiles = ref(false)

// 이력
const history = ref([])
const loadingHistory = ref(false)

const cronPresets = [
  { label: '매일 02:00', cron: '0 0 2 * * ?' },
  { label: '매일 00:00', cron: '0 0 0 * * ?' },
  { label: '주 1회(일 02:00)', cron: '0 0 2 ? * SUN' },
  { label: '월 1회(1일 02:00)', cron: '0 0 2 1 * ?' },
]

onMounted(() => {
  loadConfig()
  loadFiles()
  loadHistory()
})

async function downloadBackup() {
  if (!backupPassword.value || downloading.value) return
  downloading.value = true
  backupMsg.value = ''
  try {
    const blob = await backupApi.download(backupPassword.value)
    const now = new Date().toISOString().replace(/[-:T]/g, '').slice(0, 15)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `secportal-backup-${now}.bak`
    a.click()
    URL.revokeObjectURL(url)
    backupMsg.value = '백업 파일이 다운로드되었습니다.'
    backupMsgType.value = 'ok'
  } catch (e) {
    backupMsg.value = e || '백업 생성에 실패했습니다.'
    backupMsgType.value = 'err'
  } finally {
    downloading.value = false
  }
}

async function saveBackup() {
  if (!backupPassword.value || saving.value) return
  saving.value = true
  backupMsg.value = ''
  try {
    const res = await backupApi.save(backupPassword.value)
    backupMsg.value = res.message || '서버에 백업이 저장되었습니다.'
    backupMsgType.value = 'ok'
    await loadFiles()
    await loadHistory()
  } catch (e) {
    backupMsg.value = e || '백업 저장에 실패했습니다.'
    backupMsgType.value = 'err'
  } finally {
    saving.value = false
  }
}

function onFileSelect(e) { restoreFile.value = e.target.files[0] || null }
function onFileDrop(e) {
  dragOver.value = false
  const f = e.dataTransfer.files[0]
  if (f?.name.endsWith('.bak')) restoreFile.value = f
}

function confirmRestore() { showRestoreConfirm.value = true }

async function doRestore() {
  if (!restoreFile.value || !restorePassword.value || restoring.value) return
  restoring.value = true
  restoreMsg.value = ''
  try {
    await backupApi.restore(restoreFile.value, restorePassword.value)
    showRestoreConfirm.value = false
    restoreMsg.value = '복원이 완료되었습니다. 페이지를 새로고침 하세요.'
    restoreMsgType.value = 'ok'
    restoreFile.value = null
    restorePassword.value = ''
    await loadHistory()
  } catch (e) {
    showRestoreConfirm.value = false
    restoreMsg.value = e || '복원에 실패했습니다.'
    restoreMsgType.value = 'err'
  } finally {
    restoring.value = false
  }
}

async function loadConfig() {
  loadingConfig.value = true
  try {
    const res = await backupApi.getConfig()
    config.value = { ...res.data, defaultPassword: '' }
  } catch { /* ignore */ } finally {
    loadingConfig.value = false
  }
}

async function saveConfig() {
  savingConfig.value = true
  configMsg.value = ''
  try {
    await backupApi.updateConfig(config.value)
    configMsg.value = '설정이 저장되었습니다.'
    configMsgType.value = 'ok'
    await loadConfig()
  } catch (e) {
    configMsg.value = e || '저장에 실패했습니다.'
    configMsgType.value = 'err'
  } finally {
    savingConfig.value = false
  }
}

async function loadFiles() {
  loadingFiles.value = true
  try {
    const res = await backupApi.listFiles()
    backupFiles.value = res.data || []
  } catch { backupFiles.value = [] } finally {
    loadingFiles.value = false
  }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const res = await backupApi.listHistory()
    history.value = res.data || []
  } catch { history.value = [] } finally {
    loadingHistory.value = false
  }
}

function downloadServerFile(filename) {
  backupApi.downloadFile(filename)
}

async function confirmDeleteFile(filename) {
  if (!confirm(`'${filename}' 파일을 삭제하시겠습니까?`)) return
  try {
    await backupApi.deleteFile(filename)
    await loadFiles()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  }
}

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDateTime(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function typeLabel(t) { return { MANUAL: '수동', SCHEDULED: '정기', RESTORE: '복원', DOWNLOAD: '다운로드' }[t] || t }
function typeClass(t) {
  return { MANUAL: 'bg-blue-100 text-blue-700', SCHEDULED: 'bg-purple-100 text-purple-700', RESTORE: 'bg-orange-100 text-orange-700', DOWNLOAD: 'bg-green-100 text-green-700' }[t] || 'bg-gray-100 text-gray-700'
}
function statusLabel(s) { return { SUCCESS: '성공', FAILED: '실패', RUNNING: '진행중' }[s] || s }
</script>
