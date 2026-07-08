<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">{{ $t('admin.users') }}</h1>
      <div class="flex gap-2">
        <button @click="downloadCsv" :disabled="csvLoading" class="btn-secondary text-sm flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          {{ csvLoading ? '...' : $t('admin.downloadCsv') }}
        </button>
        <button @click="downloadPdf" :disabled="pdfLoading" class="btn-secondary text-sm flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          {{ pdfLoading ? '...' : $t('admin.downloadPdf') }}
        </button>
        <button @click="showBulkModal = true" class="btn-secondary text-sm flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/>
          </svg>
          {{ $t('admin.bulkImport') }}
        </button>
        <button @click="showCreateModal = true" class="btn-primary text-sm">
          + {{ $t('admin.addUser') }}
        </button>
      </div>
    </div>

    <div class="page-body">

    <BulkImportModal
      v-if="showBulkModal"
      ref="bulkModalRef"
      :title="$t('admin.bulkImportTitle')"
      :desc="$t('admin.bulkImportDesc')"
      :template-loading="templateLoading"
      @close="showBulkModal = false; bulkModalRef?.reset()"
      @download-template="downloadTemplate"
      @upload="handleBulkUpload"
    />

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast"
        class="fixed top-4 right-4 z-50 bg-blue-600 text-white px-5 py-3 rounded-lg shadow-lg text-sm font-medium">
        {{ toast }}
      </div>
    </transition>

    <div class="card">
      <div v-if="loading" class="text-center py-10 text-gray-500">{{ $t('common.loading') }}</div>
      <div v-else-if="error" class="text-center py-10 text-red-500">{{ error }}</div>
      <table v-else class="w-full text-sm">
        <thead>
          <tr class="border-b">
            <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('auth.name') }}</th>
            <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('auth.email') }}</th>
            <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('auth.department') }}</th>
            <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('admin.role') }}</th>
            <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('common.status') }}</th>
            <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id" class="border-b hover:bg-gray-50">
            <td class="py-3 px-4 font-medium">{{ user.name }}</td>
            <td class="py-3 px-4 text-gray-600">{{ user.email }}</td>
            <td class="py-3 px-4 text-gray-600">{{ user.department || '-' }}</td>
            <td class="py-3 px-4">
              <select
                :value="user.role"
                @change="changeRole(user, $event.target.value, $event.target)"
                class="input py-1 text-xs"
              >
                <option value="ADMIN">ADMIN</option>
                <option value="MANAGER">MANAGER</option>
                <option value="USER">USER</option>
              </select>
            </td>
            <td class="py-3 px-4">
              <span :class="user.active ? 'badge-success' : 'badge-danger'">
                {{ user.active ? $t('admin.active') : $t('admin.inactive') }}
              </span>
            </td>
            <td class="py-3 px-4">
              <div class="flex gap-2">
                <button
                  @click="openEditModal(user)"
                  class="text-xs px-3 py-1 rounded border border-primary-300 text-primary-600 hover:bg-primary-50"
                >
                  {{ $t('common.edit') }}
                </button>
                <button
                  @click="toggleActive(user)"
                  class="text-xs px-3 py-1 rounded border"
                  :class="user.active
                    ? 'border-red-300 text-red-600 hover:bg-red-50'
                    : 'border-green-300 text-green-600 hover:bg-green-50'"
                >
                  {{ user.active ? $t('admin.deactivate') : $t('admin.activate') }}
                </button>
                <button
                  v-if="!user.active"
                  @click="hardDelete(user)"
                  class="text-xs px-3 py-1 rounded border border-red-400 text-red-600 hover:bg-red-50"
                >
                  완전 삭제
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalPages > 1" class="flex justify-center gap-2 mt-4">
      <button
        v-for="p in totalPages"
        :key="p"
        @click="page = p - 1; load()"
        :class="['px-3 py-1 rounded border text-sm',
          page === p - 1 ? 'bg-blue-600 text-white border-blue-600' : 'border-gray-300 hover:bg-gray-50']"
      >{{ p }}</button>
    </div>

    <!-- Edit User Modal -->
    <div v-if="showEditModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h2 class="text-lg font-bold text-gray-900 mb-1">계정 수정</h2>
        <p class="text-sm text-gray-400 mb-4">{{ editForm.email }}</p>
        <form @submit.prevent="saveEdit" class="space-y-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.name') }} *</label>
            <input v-model="editForm.name" type="text" class="input" required />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('admin.role') }}</label>
              <select v-model="editForm.role" class="input">
                <option value="USER">USER</option>
                <option value="MANAGER">MANAGER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.department') }}</label>
              <select v-model="editForm.department" class="input">
                <option value="">부서 없음</option>
                <option v-for="dept in departments" :key="dept.value" :value="dept.value">{{ dept.label }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              새 비밀번호
              <span class="text-gray-400 font-normal">(변경 시에만 입력)</span>
            </label>
            <div class="relative">
              <input v-model="editForm.newPassword" :type="showEditPassword ? 'text' : 'password'"
                class="input pr-10"
                placeholder="변경하지 않으려면 비워두세요" minlength="8" autocomplete="new-password" />
              <button type="button"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 focus:outline-none"
                @click="showEditPassword = !showEditPassword">
                <svg v-if="!showEditPassword" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
          </div>
          <div class="flex items-center gap-2 pt-1">
            <input id="edit-active" v-model="editForm.active" type="checkbox"
              class="w-4 h-4 text-primary-600 rounded border-gray-300" />
            <label for="edit-active" class="text-sm text-gray-700">계정 활성화</label>
          </div>
          <p v-if="editError" class="text-red-600 text-sm">{{ editError }}</p>
          <div class="flex gap-3 justify-end mt-2">
            <button type="button" @click="closeEditModal" class="btn-secondary">{{ $t('common.cancel') }}</button>
            <button type="submit" class="btn-primary" :disabled="saving">
              {{ saving ? $t('common.loading') : $t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Create User Modal -->
    <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h2 class="text-lg font-bold text-gray-900 mb-4">{{ $t('admin.addUser') }}</h2>
        <form @submit.prevent="createUser" class="space-y-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.email') }} *</label>
            <input v-model="createForm.email" type="email" class="input" required autocomplete="off" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.name') }} *</label>
            <input v-model="createForm.name" type="text" class="input" required />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.password') }} *</label>
            <div class="relative">
              <input v-model="createForm.password" :type="showCreatePassword ? 'text' : 'password'"
                class="input pr-10" required minlength="8" autocomplete="new-password" />
              <button type="button"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 focus:outline-none"
                @click="showCreatePassword = !showCreatePassword">
                <svg v-if="!showCreatePassword" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('admin.role') }}</label>
              <select v-model="createForm.role" class="input">
                <option value="USER">USER</option>
                <option value="MANAGER">MANAGER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.department') }}</label>
              <select v-model="createForm.department" class="input">
                <option value="">부서를 선택하세요</option>
                <option v-for="dept in departments" :key="dept.value" :value="dept.value">{{ dept.label }}</option>
              </select>
            </div>
          </div>
          <p v-if="createError" class="text-red-600 text-sm">{{ createError }}</p>
          <div class="flex gap-3 justify-end mt-2">
            <button type="button" @click="closeCreateModal" class="btn-secondary">{{ $t('common.cancel') }}</button>
            <button type="submit" class="btn-primary" :disabled="creating">
              {{ creating ? $t('common.loading') : $t('common.create') }}
            </button>
          </div>
        </form>
      </div>
    </div>
    </div><!-- /page-body -->
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi, exportApi, userBulkApi, codeApi } from '@/api'
import BulkImportModal from '@/components/BulkImportModal.vue'

const users = ref([])
const loading = ref(true)
const error = ref(null)
const page = ref(0)
const totalPages = ref(0)
const toast = ref('')
let toastTimer = null

const csvLoading = ref(false)
const pdfLoading = ref(false)
const showBulkModal = ref(false)
const templateLoading = ref(false)
const bulkModalRef = ref(null)

const showCreateModal = ref(false)
const creating = ref(false)
const createError = ref('')
const createForm = ref({ email: '', name: '', password: '', role: 'USER', department: '' })
const showCreatePassword = ref(false)

const showEditModal = ref(false)
const saving = ref(false)
const editError = ref('')
const editTargetId = ref(null)
const editForm = ref({ email: '', name: '', role: 'USER', department: '', active: true, newPassword: '' })
const showEditPassword = ref(false)

const departments = ref([])

async function downloadCsv() {
  csvLoading.value = true
  try { await exportApi.userCsv() } finally { csvLoading.value = false }
}

async function downloadPdf() {
  pdfLoading.value = true
  try { await exportApi.userPdf() } finally { pdfLoading.value = false }
}

async function downloadTemplate() {
  templateLoading.value = true
  try { await userBulkApi.template() } finally { templateLoading.value = false }
}

async function handleBulkUpload(file, resolve, reject) {
  try {
    const res = await userBulkApi.upload(file)
    resolve(res.data)
    load()
  } catch (e) {
    reject(typeof e === 'string' ? e : '업로드 중 오류가 발생했습니다.')
  }
}

function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 3500)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await adminApi.listUsers({ page: page.value, size: 20 })
    users.value = res.data.content
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
  } catch (e) {
    error.value = typeof e === 'string' ? e : '사용자 목록 로드에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

async function changeRole(user, role, selectEl) {
  if (role === user.role) return
  if (role === 'ADMIN') {
    if (!confirm('ADMIN 권한 부여는 관리자 승인이 필요합니다. 계속하시겠습니까?')) {
      selectEl.value = user.role
      return
    }
  }
  const originalRole = user.role
  try {
    const res = await adminApi.updateUser(user.id, { role })
    if (res.data?.pending) {
      showToast(res.data.message || '승인 요청이 전송되었습니다.')
      selectEl.value = originalRole
    } else {
      user.role = res.data.role
    }
  } catch (e) {
    selectEl.value = originalRole
    alert(typeof e === 'string' ? e : '역할 변경에 실패했습니다.')
  }
}

async function toggleActive(user) {
  try {
    const res = await adminApi.updateUser(user.id, { active: !user.active })
    user.active = res.data.active
  } catch (e) {
    alert(typeof e === 'string' ? e : '상태 변경에 실패했습니다.')
  }
}

async function hardDelete(user) {
  if (!confirm(`"${user.name}" 계정의 완전 삭제를 요청하시겠습니까?\n관리자 승인 후 영구 삭제됩니다.`)) return
  try {
    await adminApi.hardDeleteUser(user.id)
    showToast(`"${user.name}" 계정 완전 삭제 승인 요청이 전송되었습니다.`)
  } catch (e) {
    alert(typeof e === 'string' ? e : '완전 삭제 요청에 실패했습니다.')
  }
}

function openEditModal(user) {
  editTargetId.value = user.id
  editForm.value = {
    email: user.email,
    name: user.name,
    role: user.role,
    department: user.department || '',
    active: user.active,
    newPassword: ''
  }
  editError.value = ''
  showEditPassword.value = false
  showEditModal.value = true
}

function closeEditModal() {
  showEditModal.value = false
  editTargetId.value = null
  editError.value = ''
  showEditPassword.value = false
}

async function saveEdit() {
  saving.value = true
  editError.value = ''
  try {
    const payload = {
      name: editForm.value.name,
      role: editForm.value.role,
      department: editForm.value.department,
      active: editForm.value.active
    }
    if (editForm.value.newPassword) payload.newPassword = editForm.value.newPassword

    const res = await adminApi.updateUser(editTargetId.value, payload)

    if (res.data?.pending) {
      showToast(res.data.message || '승인 이메일이 발송되었습니다.')
    } else {
      const idx = users.value.findIndex(u => u.id === editTargetId.value)
      if (idx !== -1) users.value[idx] = res.data
      showToast('계정이 수정되었습니다.')
    }
    closeEditModal()
  } catch (e) {
    editError.value = typeof e === 'string' ? e : '수정에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

async function createUser() {
  creating.value = true
  createError.value = ''
  try {
    const res = await adminApi.createUser({ ...createForm.value })
    users.value.unshift(res.data)
    closeCreateModal()
    showToast('계정이 생성되었습니다.')
  } catch (e) {
    createError.value = typeof e === 'string' ? e : '계정 생성에 실패했습니다.'
  } finally {
    creating.value = false
  }
}

function closeCreateModal() {
  showCreateModal.value = false
  createForm.value = { email: '', name: '', password: '', role: 'USER', department: '' }
  createError.value = ''
  showCreatePassword.value = false
}

onMounted(async () => {
  await load()
  try {
    const res = await codeApi.getValues('DEPARTMENT')
    departments.value = res.data
  } catch {
    // 코드 없으면 빈 목록 유지
  }
})
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
