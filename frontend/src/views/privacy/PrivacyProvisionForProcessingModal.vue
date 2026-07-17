<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <div>
          <h2 class="text-lg font-semibold text-gray-900">{{ processingName }} — 제공 정보</h2>
          <p class="text-xs text-gray-500 mt-0.5">
            이 처리업무에서 외부로 나가는 개인정보(제3자 제공 · 공동이용 · 국외이전)를 관리합니다.
          </p>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1">

        <!-- 목록 -->
        <div v-if="!editing">
          <div class="flex justify-between items-center mb-3">
            <p class="text-sm text-gray-500">
              등록된 제공 <b class="text-gray-900">{{ items.length }}</b>건
            </p>
            <button v-if="canWrite" @click="openCreate" class="btn-primary text-sm flex items-center gap-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              제공 등록
            </button>
          </div>

          <div v-if="loading" class="py-10 text-center text-gray-400">불러오는 중...</div>

          <div v-else-if="!items.length"
            class="py-12 text-center border border-dashed border-gray-200 rounded-xl">
            <p class="text-sm text-gray-400">이 처리업무에 등록된 제공이 없습니다.</p>
            <p class="text-xs text-gray-400 mt-1">
              제3자 제공 · 공동이용 · 국외이전이 있다면 등록하세요.
            </p>
          </div>

          <div v-else class="space-y-2">
            <div v-for="p in items" :key="p.id"
              class="border border-gray-200 rounded-xl px-4 py-3 hover:bg-gray-50 transition-colors">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="text-sm font-semibold text-gray-900">{{ p.recipient }}</span>
                    <span :class="TYPE_BADGE[p.provisionType]">{{ TYPE[p.provisionType] }}</span>
                    <span v-if="p.country" class="badge-orange">{{ p.country }}</span>
                    <span :class="p.status === 'ACTIVE' ? 'badge-green' : 'badge-gray'">
                      {{ STATUS[p.status] }}
                    </span>
                  </div>
                  <p v-if="p.purpose" class="text-xs text-gray-500 mt-1.5">{{ p.purpose }}</p>
                  <p v-if="p.infoItems" class="text-xs text-gray-400 mt-1">제공 항목: {{ p.infoItems }}</p>
                </div>
                <div v-if="canWrite" class="flex gap-1 shrink-0">
                  <button @click="openEdit(p)"
                    class="text-xs text-primary-600 hover:text-primary-700 px-2 py-1">수정</button>
                  <button @click="remove(p)"
                    class="text-xs text-red-500 hover:text-red-600 px-2 py-1">삭제</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 등록/수정 폼 -->
        <form v-else id="provisionForProcessingForm" @submit.prevent="submit" class="space-y-4">
          <div class="flex items-center gap-2 pb-2 border-b">
            <button type="button" @click="editing = false"
              class="text-gray-400 hover:text-gray-600">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
              </svg>
            </button>
            <h3 class="text-sm font-bold text-gray-800">{{ editId ? '제공 수정' : '제공 등록' }}</h3>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">제공 유형 <span class="text-red-500">*</span></label>
              <select v-model="form.provisionType" class="input w-full" required>
                <option value="THIRD_PARTY">제3자 제공</option>
                <option value="JOINT_USE">공동이용</option>
                <option value="OVERSEAS">국외이전</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">제공받는 자 <span class="text-red-500">*</span></label>
              <input v-model="form.recipient" type="text" class="input w-full" required />
            </div>

            <div v-if="form.provisionType === 'OVERSEAS'" class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">이전 국가 <span class="text-red-500">*</span></label>
              <input v-model="form.country" type="text" class="input w-full" required placeholder="예: 미국" />
              <p class="text-xs text-gray-400 mt-1">
                국외이전은 이전 국가·목적·항목을 개인정보 처리방침에 공개해야 합니다.
              </p>
            </div>

            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">제공 항목</label>
              <textarea v-model="form.infoItems" class="input w-full" rows="2"
                :placeholder="processingInfoItems ? `처리업무 항목: ${processingInfoItems}` : ''"></textarea>
              <button v-if="processingInfoItems" type="button" @click="form.infoItems = processingInfoItems"
                class="text-xs text-primary-600 hover:text-primary-700 mt-1">
                처리업무의 개인정보 항목 가져오기
              </button>
            </div>

            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">제공목적</label>
              <textarea v-model="form.purpose" class="input w-full" rows="2"></textarea>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">제공근거</label>
              <input v-model="form.legalBasis" type="text" class="input w-full"
                placeholder="예: 정보주체 동의 / 법령상 근거" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">제공받는 자의 보유기간</label>
              <input v-model="form.retentionPeriod" type="text" class="input w-full" />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">제공 방법</label>
              <input v-model="form.method" type="text" class="input w-full" placeholder="예: API 연계, 파일 전송" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">최초 제공일</label>
              <input v-model="form.provisionDate" type="date" class="input w-full" />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">계약 시작일</label>
              <input v-model="form.contractStart" type="date" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">계약 종료일</label>
              <input v-model="form.contractEnd" type="date" class="input w-full" />
            </div>

            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">제공 계약 정보</label>
              <textarea v-model="form.contractInfo" class="input w-full" rows="2"></textarea>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">상태</label>
              <select v-model="form.status" class="input w-full">
                <option value="ACTIVE">제공중</option>
                <option value="TERMINATED">종료</option>
              </select>
            </div>

            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">비고</label>
              <textarea v-model="form.notes" class="input w-full" rows="2"></textarea>
            </div>
          </div>

          <p v-if="error" class="text-red-600 text-sm">{{ error }}</p>
        </form>
      </div>

      <div class="flex justify-between items-center px-5 py-3 border-t shrink-0">
        <p class="text-xs text-gray-400">
          여기서 등록한 제공은 <b>제공관리</b> 메뉴에도 함께 나타납니다.
        </p>
        <div class="flex gap-2">
          <template v-if="editing">
            <button type="button" @click="editing = false" class="btn-secondary text-sm">취소</button>
            <button type="submit" form="provisionForProcessingForm" class="btn-primary text-sm" :disabled="saving">
              {{ saving ? '저장 중...' : '저장' }}
            </button>
          </template>
          <button v-else @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { privacyProvisionApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  open: { type: Boolean, default: false },
  processingId: { type: [Number, String], default: null },
  processingName: { type: String, default: '' },
  /** 처리업무의 개인정보 항목 — 제공 항목 입력을 돕는 기본값으로 쓴다 */
  processingInfoItems: { type: String, default: '' },
})
const emit = defineEmits(['close', 'changed'])

const auth = useAuthStore()
const canWrite = auth.canWrite('privacy_provision')

const TYPE = { THIRD_PARTY: '제3자 제공', JOINT_USE: '공동이용', OVERSEAS: '국외이전' }
const TYPE_BADGE = { THIRD_PARTY: 'badge-blue', JOINT_USE: 'badge-yellow', OVERSEAS: 'badge-orange' }
const STATUS = { ACTIVE: '제공중', TERMINATED: '종료' }

const items = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const editing = ref(false)
const editId = ref(null)

function emptyForm() {
  return {
    provisionType: 'THIRD_PARTY', recipient: '', country: '', infoItems: '', purpose: '',
    legalBasis: '', retentionPeriod: '', method: '', contractInfo: '',
    contractStart: '', contractEnd: '', provisionDate: '', status: 'ACTIVE', notes: '',
  }
}
const form = ref(emptyForm())

async function load() {
  if (!props.processingId) return
  loading.value = true
  try {
    const res = await privacyProvisionApi.listByProcessing(props.processingId)
    items.value = res.data || []
  } catch (e) {
    error.value = typeof e === 'string' ? e : '제공 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editId.value = null
  form.value = emptyForm()
  error.value = ''
  editing.value = true
}

function openEdit(p) {
  editId.value = p.id
  form.value = {
    provisionType: p.provisionType, recipient: p.recipient, country: p.country || '',
    infoItems: p.infoItems || '', purpose: p.purpose || '', legalBasis: p.legalBasis || '',
    retentionPeriod: p.retentionPeriod || '', method: p.method || '',
    contractInfo: p.contractInfo || '', contractStart: p.contractStart || '',
    contractEnd: p.contractEnd || '', provisionDate: p.provisionDate || '',
    status: p.status, notes: p.notes || '',
  }
  error.value = ''
  editing.value = true
}

async function submit() {
  saving.value = true
  error.value = ''
  try {
    // 이 화면에서 만든 제공은 항상 현재 처리업무에 연계된다
    const payload = { processingId: Number(props.processingId) }
    Object.entries(form.value).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined) payload[k] = v
    })
    // 국외이전이 아니면 이전 국가는 의미가 없으므로 비운다
    if (payload.provisionType !== 'OVERSEAS') payload.country = ''

    if (editId.value) await privacyProvisionApi.update(editId.value, payload)
    else await privacyProvisionApi.create(payload)

    editing.value = false
    await load()
    emit('changed')
  } catch (e) {
    error.value = typeof e === 'string' ? e : '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

async function remove(p) {
  if (!confirm(`"${p.recipient}" 제공 정보를 삭제하시겠습니까?`)) return
  try {
    await privacyProvisionApi.delete(p.id)
    await load()
    emit('changed')
  } catch (e) {
    error.value = typeof e === 'string' ? e : '삭제에 실패했습니다.'
  }
}

watch(() => props.open, (open) => {
  if (!open) return
  editing.value = false
  error.value = ''
  load()
})
</script>
