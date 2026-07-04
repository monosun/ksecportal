<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ $t('notice.title') }}</h1>
      </div>
      <button @click="openCreate" class="btn-primary flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        {{ $t('notice.add') }}
      </button>
    </div>

    <div class="page-body">
      <div class="card">
        <div v-if="notices.length === 0" class="py-12 text-center text-sm text-gray-400">
          {{ $t('notice.empty') }}
        </div>
        <table v-else class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-100">
              <th class="text-left pb-3 text-gray-500 font-medium pl-2">{{ $t('notice.formTitle') }}</th>
              <th class="text-left pb-3 text-gray-500 font-medium w-20">고정</th>
              <th class="text-left pb-3 text-gray-500 font-medium w-20">상태</th>
              <th class="text-left pb-3 text-gray-500 font-medium w-32">등록자</th>
              <th class="text-left pb-3 text-gray-500 font-medium w-36">등록일</th>
              <th class="pb-3 w-24"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="n in notices" :key="n.id" class="border-b border-gray-50 hover:bg-gray-50 transition-colors">
              <td class="py-3 pl-2">
                <div class="font-medium text-gray-900">{{ n.title }}</div>
                <div v-if="n.content" class="text-xs text-gray-400 mt-0.5 line-clamp-1">{{ n.content }}</div>
              </td>
              <td class="py-3">
                <span v-if="n.pinned" class="inline-flex items-center gap-1 text-xs font-semibold text-primary-600 bg-primary-50 px-2 py-0.5 rounded-full">
                  <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M16 3a1 1 0 00-1.447-.894L8 6H5a2 2 0 00-2 2v4a2 2 0 002 2h3l1 7h2l1-7 3.553-1.553A1 1 0 0016 11V3z"/>
                  </svg>
                  {{ $t('notice.pinned') }}
                </span>
              </td>
              <td class="py-3">
                <span class="text-xs px-2 py-0.5 rounded-full font-semibold"
                  :class="n.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'">
                  {{ n.active ? $t('notice.active') : $t('notice.inactive') }}
                </span>
              </td>
              <td class="py-3 text-gray-600">{{ n.createdByName || '-' }}</td>
              <td class="py-3 text-gray-400 text-xs">{{ formatDate(n.createdAt) }}</td>
              <td class="py-3">
                <div class="flex gap-1 justify-end pr-2">
                  <button @click="openEdit(n)" class="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400 hover:text-gray-700 transition-colors">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                    </svg>
                  </button>
                  <button @click="handleDelete(n)" class="p-1.5 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-600 transition-colors">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 공지사항 등록/수정 모달 -->
    <Transition name="modal-fade">
      <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="showModal = false">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg">
          <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
            <h3 class="font-semibold text-gray-900">{{ editTarget ? $t('notice.edit') : $t('notice.add') }}</h3>
            <button @click="showModal = false" class="p-1 rounded-lg hover:bg-gray-100 text-gray-400">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="px-6 py-5 space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('notice.formTitle') }} *</label>
              <input v-model="form.title" type="text" class="input" :placeholder="$t('notice.formTitle')" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('notice.formContent') }}</label>
              <textarea v-model="form.content" class="input resize-none" rows="4" :placeholder="$t('notice.formContent')"></textarea>
            </div>
            <div class="flex items-center gap-6">
              <label class="flex items-center gap-2 cursor-pointer select-none text-sm text-gray-700">
                <input v-model="form.pinned" type="checkbox" class="w-4 h-4 text-primary-600 border-gray-300 rounded" />
                {{ $t('notice.formPinned') }}
              </label>
              <label v-if="editTarget" class="flex items-center gap-2 cursor-pointer select-none text-sm text-gray-700">
                <input v-model="form.active" type="checkbox" class="w-4 h-4 text-primary-600 border-gray-300 rounded" />
                {{ $t('notice.formActive') }}
              </label>
            </div>
          </div>
          <div class="px-6 py-4 border-t border-gray-100 flex justify-end gap-2">
            <button @click="showModal = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
            <button @click="handleSave" :disabled="saving || !form.title.trim()" class="btn-primary">
              {{ saving ? $t('common.loading') : $t('common.save') }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { noticeApi } from '@/api'

const notices = ref([])
const showModal = ref(false)
const editTarget = ref(null)
const saving = ref(false)
const form = ref({ title: '', content: '', pinned: false, active: true })

onMounted(load)

async function load() {
  const res = await noticeApi.listAll()
  notices.value = res.data || []
}

function openCreate() {
  editTarget.value = null
  form.value = { title: '', content: '', pinned: false, active: true }
  showModal.value = true
}

function openEdit(n) {
  editTarget.value = n
  form.value = { title: n.title, content: n.content || '', pinned: n.pinned, active: n.active }
  showModal.value = true
}

async function handleSave() {
  if (!form.value.title.trim()) return
  saving.value = true
  try {
    if (editTarget.value) {
      await noticeApi.update(editTarget.value.id, form.value)
    } else {
      await noticeApi.create(form.value)
    }
    showModal.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(n) {
  if (!confirm('공지사항을 삭제하시겠습니까?')) return
  await noticeApi.delete(n.id)
  await load()
}

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.line-clamp-1 { overflow: hidden; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; }
</style>
