<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">비상연락망</h1>
        <p class="text-sm text-gray-400 mt-0.5">침해사고·개인정보 유출·재해 상황별 연락 계통과 외부 신고기관 연락처를 관리합니다</p>
      </div>
      <PiMaskToggle screen="비상연락망" />
    </div>

    <div class="page-body">
      <!-- 검색 · 필터 · 추가 -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div class="flex flex-wrap items-center gap-2">
          <input v-model="keyword" class="input w-64 text-sm !py-1.5" placeholder="이름·소속·역할·부서 검색" />
          <div class="flex gap-1 p-1 bg-gray-100 rounded-lg">
            <button v-for="f in TYPE_FILTERS" :key="f.value" @click="typeFilter = f.value"
              :class="['px-3 py-1 rounded-md text-xs font-semibold transition-all',
                typeFilter === f.value ? 'bg-white text-primary-600 shadow-sm' : 'text-gray-500 hover:text-gray-700']">
              {{ f.label }}
            </button>
          </div>
          <label class="flex items-center gap-1.5 text-xs text-gray-500 cursor-pointer">
            <input type="checkbox" v-model="showInactive" class="w-3.5 h-3.5 rounded" />
            비활성 포함
          </label>
        </div>
        <button class="btn-primary text-sm" @click="openGroupModal()">+ 연락 그룹 추가</button>
      </div>

      <div v-if="loading" class="flex items-center justify-center py-20 text-gray-400">
        <svg class="animate-spin w-6 h-6 mr-2" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
        </svg>
        불러오는 중...
      </div>

      <div v-else-if="!visibleGroups.length" class="card text-center py-16 text-gray-400 text-sm">
        {{ keyword ? '검색 결과가 없습니다.' : '등록된 연락 그룹이 없습니다.' }}
      </div>

      <!-- 그룹별 연락 계통 -->
      <div v-else class="space-y-5">
        <div v-for="g in visibleGroups" :key="g.id" class="card p-0 overflow-hidden"
          :class="{ 'opacity-60': !g.active }">
          <!-- 그룹 헤더 -->
          <div class="flex items-start justify-between gap-4 px-5 py-4 border-b bg-gray-50">
            <div class="min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <h2 class="text-sm font-bold text-gray-900">{{ g.name }}</h2>
                <span :class="typeClass(g.contactType)" class="px-2 py-0.5 rounded-full text-[11px] font-semibold">
                  {{ typeLabel(g.contactType) }}
                </span>
                <span class="text-[11px] text-gray-400">{{ g.contacts.length }}명</span>
                <button @click="toggleGroup(g)"
                  :class="g.active ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-500'"
                  class="px-2 py-0.5 rounded-full text-[11px] font-semibold transition-colors">
                  {{ g.active ? '활성' : '비활성' }}
                </button>
              </div>
              <p v-if="g.description" class="text-xs text-gray-500 mt-1">{{ g.description }}</p>
            </div>
            <div class="flex items-center gap-2 flex-shrink-0">
              <button @click="openContactModal(g.id)" class="text-xs text-primary-600 hover:underline">+ 연락처</button>
              <button @click="openGroupModal(g)" class="text-xs text-gray-600 hover:underline">수정</button>
              <button @click="confirmDeleteGroup(g)" class="text-xs text-red-500 hover:underline">삭제</button>
            </div>
          </div>

          <!-- 연락처 목록 -->
          <div v-if="!g.contacts.length" class="py-10 text-center text-gray-400 text-sm">
            등록된 연락처가 없습니다. <button @click="openContactModal(g.id)" class="text-primary-600 hover:underline">연락처를 추가</button>하세요.
          </div>
          <div v-else class="overflow-x-auto"><table class="w-full text-sm">
            <thead>
              <tr class="text-gray-500 text-xs uppercase tracking-wider border-b">
                <th class="px-4 py-2.5 text-center w-16">순서</th>
                <th class="px-4 py-2.5 text-left">이름 · 소속</th>
                <th class="px-4 py-2.5 text-left">역할</th>
                <th class="px-4 py-2.5 text-left">부서 · 직위</th>
                <th class="px-4 py-2.5 text-left w-36">사무실</th>
                <th class="px-4 py-2.5 text-left w-40">휴대전화</th>
                <th class="px-4 py-2.5 text-left w-48">이메일</th>
                <th class="px-4 py-2.5 text-center w-16">24H</th>
                <th class="px-4 py-2.5 text-center w-24">작업</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="c in g.contacts" :key="c.id" class="hover:bg-gray-50 transition-colors"
                :class="{ 'opacity-50': !c.active }">
                <td class="px-4 py-2.5 text-center">
                  <span class="inline-flex items-center justify-center w-6 h-6 rounded-full bg-primary-50 text-primary-600 text-xs font-bold">
                    {{ c.contactOrder }}
                  </span>
                </td>
                <td class="px-4 py-2.5">
                  <p class="font-medium text-gray-900">{{ c.name }}</p>
                  <p v-if="c.organization" class="text-xs text-gray-400">{{ c.organization }}</p>
                </td>
                <td class="px-4 py-2.5 text-gray-600 text-xs">{{ c.roleName || '—' }}</td>
                <td class="px-4 py-2.5 text-gray-500 text-xs">
                  {{ [c.department, c.position].filter(Boolean).join(' · ') || '—' }}
                </td>
                <!-- 사무실·기관 대표번호는 비상 시 즉시 걸 수 있도록 원문을 유지한다 -->
                <td class="px-4 py-2.5">
                  <a v-if="c.officePhone" :href="`tel:${c.officePhone}`" class="text-primary-600 hover:underline font-mono text-xs">{{ c.officePhone }}</a>
                  <span v-else class="text-gray-300">—</span>
                </td>
                <!-- 개인 휴대전화·이메일은 코드관리의 항목별 마스킹 기준에 따라 가려서 표시한다 -->
                <td class="px-4 py-2.5">
                  <template v-if="c.mobile">
                    <a v-if="pi.revealed" :href="`tel:${c.mobile}`" class="text-primary-600 hover:underline font-mono text-xs">{{ c.mobile }}</a>
                    <span v-else class="font-mono text-xs text-gray-600">{{ pi.mask('phone', c.mobile) }}</span>
                  </template>
                  <span v-else class="text-gray-300">—</span>
                </td>
                <td class="px-4 py-2.5">
                  <template v-if="c.email">
                    <a v-if="pi.revealed" :href="`mailto:${c.email}`" class="text-primary-600 hover:underline text-xs">{{ c.email }}</a>
                    <span v-else class="text-xs text-gray-600">{{ pi.mask('email', c.email) }}</span>
                  </template>
                  <span v-else class="text-gray-300">—</span>
                </td>
                <td class="px-4 py-2.5 text-center">
                  <span v-if="c.available24h" class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-green-100 text-green-700">24H</span>
                  <span v-else class="text-gray-300">—</span>
                </td>
                <td class="px-4 py-2.5">
                  <div class="flex justify-center gap-1.5">
                    <button @click="openContactModal(g.id, c)" class="text-xs text-gray-600 hover:underline">수정</button>
                    <button @click="confirmDeleteContact(g, c)" class="text-xs text-red-500 hover:underline">삭제</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table></div>

          <!-- 비고 -->
          <div v-if="notesOf(g).length" class="px-5 py-3 border-t bg-gray-50 space-y-1">
            <p v-for="c in notesOf(g)" :key="c.id" class="text-xs text-gray-500">
              <span class="font-semibold text-gray-600">{{ c.name }}</span> — {{ c.note }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 그룹 등록/수정 모달 ───────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="groupModal.open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <h3 class="font-bold text-gray-900">{{ groupModal.id ? '연락 그룹 수정' : '연락 그룹 추가' }}</h3>
            <button @click="groupModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <form @submit.prevent="saveGroup" class="p-6 space-y-4">
            <div>
              <label class="label">그룹명 <span class="text-red-500">*</span></label>
              <input v-model="groupModal.form.name" class="input" required placeholder="예) 침해사고 대응반" />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">구분</label>
                <select v-model="groupModal.form.contactType" class="input">
                  <option value="INTERNAL">내부 조직</option>
                  <option value="EXTERNAL">외부 기관</option>
                  <option value="PARTNER">협력사·유지보수</option>
                </select>
              </div>
              <div>
                <label class="label">표시 순서</label>
                <input v-model.number="groupModal.form.sortOrder" type="number" min="0" class="input" />
              </div>
            </div>
            <div>
              <label class="label">설명</label>
              <textarea v-model="groupModal.form.description" class="input" rows="3"
                placeholder="어떤 상황에서 이 계통으로 연락하는지 적습니다."></textarea>
            </div>
            <div v-if="groupModal.error" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">{{ groupModal.error }}</div>
            <div class="flex justify-end gap-3 pt-2">
              <button type="button" @click="groupModal.open = false" class="btn-secondary">취소</button>
              <button type="submit" class="btn-primary" :disabled="groupModal.saving">
                {{ groupModal.saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- ── 연락처 등록/수정 모달 ─────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="contactModal.open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 p-4 overflow-y-auto">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-xl my-8">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <h3 class="font-bold text-gray-900">{{ contactModal.id ? '연락처 수정' : '연락처 추가' }}</h3>
            <button @click="contactModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <form @submit.prevent="saveContact" class="p-6 space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">연락 그룹 <span class="text-red-500">*</span></label>
                <select v-model="contactModal.form.groupId" class="input" required>
                  <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
                </select>
              </div>
              <div>
                <label class="label">연락 순서</label>
                <input v-model.number="contactModal.form.contactOrder" type="number" min="1" class="input" />
              </div>
              <div>
                <label class="label">이름 · 부서명 <span class="text-red-500">*</span></label>
                <input v-model="contactModal.form.name" class="input" required />
              </div>
              <div>
                <label class="label">소속 기관·회사</label>
                <input v-model="contactModal.form.organization" class="input" placeholder="외부기관·협력사인 경우" />
              </div>
              <div>
                <label class="label">부서</label>
                <input v-model="contactModal.form.department" class="input" />
              </div>
              <div>
                <label class="label">직위</label>
                <input v-model="contactModal.form.position" class="input" />
              </div>
              <div class="col-span-2">
                <label class="label">비상 시 역할</label>
                <input v-model="contactModal.form.roleName" class="input" placeholder="예) 총괄, 상황 전파, 기술 대응, 대외 신고" />
              </div>
              <div>
                <label class="label">사무실·대표 전화</label>
                <input v-model="contactModal.form.officePhone" class="input" />
              </div>
              <div>
                <label class="label">휴대전화</label>
                <input v-model="contactModal.form.mobile" class="input" />
              </div>
              <div class="col-span-2">
                <label class="label">이메일</label>
                <input v-model="contactModal.form.email" type="email" class="input" />
              </div>
              <div class="col-span-2">
                <label class="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input type="checkbox" v-model="contactModal.form.available24h" class="w-4 h-4 rounded" />
                  24시간 연락 가능
                </label>
              </div>
              <div class="col-span-2">
                <label class="label">비고</label>
                <textarea v-model="contactModal.form.note" class="input" rows="2"
                  placeholder="대체 연락자, 대응 시간(SLA) 등"></textarea>
              </div>
            </div>
            <p class="text-xs text-gray-400">휴대전화·이메일은 저장 시 암호화되며, 목록에서는 마스킹되어 표시됩니다.</p>
            <div v-if="contactModal.error" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">{{ contactModal.error }}</div>
            <div class="flex justify-end gap-3 pt-2">
              <button type="button" @click="contactModal.open = false" class="btn-secondary">취소</button>
              <button type="submit" class="btn-primary" :disabled="contactModal.saving">
                {{ contactModal.saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { emergencyContactApi } from '@/api'
import PiMaskToggle from '@/components/privacy/PiMaskToggle.vue'
import { usePiMaskingStore } from '@/stores/piMasking'

// 개인 휴대전화·이메일은 코드관리의 항목별 마스킹 기준에 따라 가려서 표시한다
const pi = usePiMaskingStore()

const TYPE_FILTERS = [
  { value: null,       label: '전체' },
  { value: 'INTERNAL', label: '내부 조직' },
  { value: 'EXTERNAL', label: '외부 기관' },
  { value: 'PARTNER',  label: '협력사' },
]

const loading = ref(true)
const groups = ref([])
const keyword = ref('')
const typeFilter = ref(null)
const showInactive = ref(false)

/** 검색어는 마스킹되지 않는 항목(이름·소속·역할·부서·직위)에만 적용한다 */
function matches(c, kw) {
  return [c.name, c.organization, c.roleName, c.department, c.position]
    .filter(Boolean).some(v => v.toLowerCase().includes(kw))
}

const visibleGroups = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return groups.value
    .filter(g => showInactive.value || g.active)
    .filter(g => !typeFilter.value || g.contactType === typeFilter.value)
    .map(g => ({
      ...g,
      contacts: g.contacts
        .filter(c => showInactive.value || c.active)
        .filter(c => !kw || matches(c, kw)),
    }))
    // 검색 중에는 결과가 있는 그룹만 남긴다
    .filter(g => !kw || g.contacts.length > 0)
})

function notesOf(g) { return g.contacts.filter(c => c.note) }

async function load() {
  loading.value = true
  try {
    groups.value = (await emergencyContactApi.listGroups()).data ?? []
  } finally {
    loading.value = false
  }
}
onMounted(load)

// ── 그룹 모달 ──────────────────────────────────────────────────────────────
const groupModal = reactive({
  open: false, id: null, saving: false, error: '',
  form: { name: '', contactType: 'INTERNAL', description: '', sortOrder: null },
})
function openGroupModal(g = null) {
  groupModal.id = g?.id ?? null
  groupModal.error = ''
  groupModal.form = g
    ? { name: g.name, contactType: g.contactType, description: g.description ?? '', sortOrder: g.sortOrder }
    : { name: '', contactType: 'INTERNAL', description: '', sortOrder: null }
  groupModal.open = true
}
async function saveGroup() {
  groupModal.saving = true
  groupModal.error = ''
  try {
    if (groupModal.id) await emergencyContactApi.updateGroup(groupModal.id, groupModal.form)
    else await emergencyContactApi.createGroup(groupModal.form)
    groupModal.open = false
    await load()
  } catch (e) {
    groupModal.error = typeof e === 'string' ? e : '저장에 실패했습니다.'
  } finally {
    groupModal.saving = false
  }
}
async function toggleGroup(g) {
  try {
    await emergencyContactApi.toggleGroup(g.id)
    const target = groups.value.find(x => x.id === g.id)
    if (target) target.active = !target.active
  } catch (e) { alert('상태 변경 실패') }
}
async function confirmDeleteGroup(g) {
  if (!confirm(`"${g.name}" 연락 그룹을 삭제하시겠습니까?`)) return
  try {
    await emergencyContactApi.deleteGroup(g.id)
    groups.value = groups.value.filter(x => x.id !== g.id)
  } catch (e) { alert(typeof e === 'string' ? e : '삭제 실패') }
}

// ── 연락처 모달 ────────────────────────────────────────────────────────────
const contactModal = reactive({
  open: false, id: null, saving: false, error: '',
  form: {
    groupId: '', name: '', organization: '', department: '', position: '', roleName: '',
    contactOrder: null, mobile: '', officePhone: '', email: '', available24h: false, note: '',
  },
})
function openContactModal(groupId, c = null) {
  contactModal.id = c?.id ?? null
  contactModal.error = ''
  contactModal.form = c
    ? {
        groupId: c.groupId, name: c.name, organization: c.organization ?? '',
        department: c.department ?? '', position: c.position ?? '', roleName: c.roleName ?? '',
        contactOrder: c.contactOrder, mobile: c.mobile ?? '', officePhone: c.officePhone ?? '',
        email: c.email ?? '', available24h: c.available24h, note: c.note ?? '',
      }
    : {
        groupId, name: '', organization: '', department: '', position: '', roleName: '',
        contactOrder: null, mobile: '', officePhone: '', email: '', available24h: false, note: '',
      }
  contactModal.open = true
}
async function saveContact() {
  contactModal.saving = true
  contactModal.error = ''
  try {
    const payload = { ...contactModal.form, groupId: Number(contactModal.form.groupId) }
    if (contactModal.id) await emergencyContactApi.updateContact(contactModal.id, payload)
    else await emergencyContactApi.createContact(payload)
    contactModal.open = false
    await load()
  } catch (e) {
    contactModal.error = typeof e === 'string' ? e : '저장에 실패했습니다.'
  } finally {
    contactModal.saving = false
  }
}
async function confirmDeleteContact(g, c) {
  if (!confirm(`"${c.name}" 연락처를 삭제하시겠습니까?`)) return
  try {
    await emergencyContactApi.deleteContact(c.id)
    const target = groups.value.find(x => x.id === g.id)
    if (target) target.contacts = target.contacts.filter(x => x.id !== c.id)
  } catch (e) { alert(typeof e === 'string' ? e : '삭제 실패') }
}

// ── Helpers ────────────────────────────────────────────────────────────────
function typeLabel(t) { return { INTERNAL: '내부 조직', EXTERNAL: '외부 기관', PARTNER: '협력사·유지보수' }[t] ?? t }
function typeClass(t) {
  return {
    INTERNAL: 'bg-blue-100 text-blue-700',
    EXTERNAL: 'bg-purple-100 text-purple-700',
    PARTNER:  'bg-amber-100 text-amber-700',
  }[t] ?? 'bg-gray-100 text-gray-600'
}
</script>

<style scoped>
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.label { @apply block text-sm font-medium text-gray-700 mb-1; }
</style>
