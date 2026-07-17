<template>
  <div class="p-8">
    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-2">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ title }}</h1>
        <p v-if="description" class="text-sm text-gray-500 mt-1">{{ description }}</p>
      </div>
      <div class="flex items-center gap-2">
        <slot name="header-actions" :items="items" />
        <button v-if="canWrite" @click="openCreate" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          등록
        </button>
      </div>
    </div>

    <!-- 요약 통계 -->
    <div v-if="stats.length" class="grid gap-3 mt-5 mb-5"
      :style="{ gridTemplateColumns: `repeat(${stats.length}, minmax(0, 1fr))` }">
      <div v-for="s in stats" :key="s.label" class="card">
        <p class="text-xs text-gray-500">{{ s.label }}</p>
        <p class="text-2xl font-bold mt-1" :class="s.class || 'text-gray-900'">{{ s.value }}</p>
      </div>
    </div>

    <!-- 필터 -->
    <div class="card mb-5 flex flex-wrap gap-3">
      <input v-model="keyword" type="text" placeholder="검색" class="input flex-1 min-w-48" />
      <select v-for="f in filters" :key="f.key" v-model="filterValues[f.key]" class="input w-44">
        <option value="">{{ f.label }}: 전체</option>
        <option v-for="o in f.options" :key="o.value" :value="o.value">{{ o.label }}</option>
      </select>
    </div>

    <!-- 목록 -->
    <div class="card p-0 overflow-hidden">
      <div v-if="loading" class="p-8 text-center text-gray-400">로딩 중...</div>
      <div v-else-if="!filtered.length" class="p-8 text-center text-gray-400">
        {{ items.length ? '조건에 맞는 항목이 없습니다.' : '등록된 항목이 없습니다.' }}
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50 border-b border-gray-100">
            <tr>
              <th v-for="c in columns" :key="c.key"
                class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                {{ c.label }}
              </th>
              <th v-if="canWrite || $slots['row-actions']" class="px-5 py-3 w-20"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="row in filtered" :key="row.id"
              class="hover:bg-gray-50 cursor-pointer transition-colors" @click="openEdit(row)">
              <td v-for="c in columns" :key="c.key" class="px-5 py-3.5 text-sm"
                :class="c.strong ? 'font-medium text-gray-900' : 'text-gray-600'">
                <span v-if="c.type === 'badge'" :class="badgeClass(c, row[c.key])">
                  {{ labelOf(c, row[c.key]) }}
                </span>
                <span v-else-if="c.type === 'bool'">
                  <span v-if="row[c.key]" class="badge-orange">예</span>
                  <span v-else class="text-gray-400">—</span>
                </span>
                <span v-else-if="c.render">{{ c.render(row) }}</span>
                <span v-else>{{ display(row[c.key]) }}</span>
              </td>
              <td v-if="canWrite || $slots['row-actions']" class="px-5 py-3.5 text-right whitespace-nowrap" @click.stop>
                <slot name="row-actions" :row="row" />
                <button v-if="canWrite" @click="confirmDelete(row)"
                  class="text-xs text-red-500 hover:text-red-600 px-2 py-1">
                  삭제
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 등록/수정 모달 -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
      <div class="absolute inset-0 bg-black/40" @click="showForm = false"></div>
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[92vh] flex flex-col">
        <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
          <h2 class="text-lg font-semibold text-gray-900">{{ editId ? title + ' 수정' : title + ' 등록' }}</h2>
          <button @click="showForm = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <form id="privacyCrudForm" @submit.prevent="submit" class="px-5 py-4 overflow-y-auto flex-1">
          <div class="grid grid-cols-2 gap-4">
            <div v-for="f in fields" :key="f.key" :class="f.span === 2 ? 'col-span-2' : ''">
              <label class="block text-sm font-medium text-gray-700 mb-1">
                {{ f.label }} <span v-if="f.required" class="text-red-500">*</span>
              </label>

              <textarea v-if="f.type === 'textarea'" v-model="form[f.key]" class="input w-full"
                :rows="f.rows || 3" :required="f.required" :placeholder="f.placeholder"></textarea>

              <select v-else-if="f.type === 'select'" v-model="form[f.key]" class="input w-full" :required="f.required">
                <option v-if="!f.required" value="">선택 안 함</option>
                <option v-for="o in optionsOf(f)" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>

              <label v-else-if="f.type === 'checkbox'" class="flex items-center gap-2 h-9">
                <input type="checkbox" v-model="form[f.key]" class="w-4 h-4 rounded border-gray-300" />
                <span class="text-sm text-gray-600">{{ f.checkboxLabel || '예' }}</span>
              </label>

              <input v-else v-model="form[f.key]" :type="inputType(f.type)" class="input w-full"
                :required="f.required" :placeholder="f.placeholder" />

              <p v-if="f.hint" class="text-xs text-gray-400 mt-1">{{ f.hint }}</p>
            </div>
          </div>
          <p v-if="error" class="text-red-600 text-sm mt-3">{{ error }}</p>
        </form>

        <div class="flex justify-end gap-3 px-5 py-3 border-t shrink-0">
          <button type="button" @click="showForm = false" class="btn-secondary text-sm">취소</button>
          <button type="submit" form="privacyCrudForm" class="btn-primary text-sm" :disabled="saving">
            {{ saving ? '저장 중...' : '저장' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 삭제 확인 -->
    <div v-if="deleteTarget" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40" @click="deleteTarget = null"></div>
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <h3 class="text-base font-bold text-gray-900 mb-2">삭제하시겠습니까?</h3>
        <p class="text-sm text-gray-500 mb-5">"{{ deleteTarget[titleKey] }}" 항목이 삭제되며 되돌릴 수 없습니다.</p>
        <div class="flex justify-end gap-2">
          <button @click="deleteTarget = null" class="btn-secondary text-sm">취소</button>
          <button @click="doDelete" class="btn-danger text-sm" :disabled="saving">삭제</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  api: { type: Object, required: true },
  menuKey: { type: String, default: '' },
  /** 폼 필드 스키마 */
  fields: { type: Array, required: true },
  /** 목록 컬럼 스키마 */
  columns: { type: Array, required: true },
  /** 키워드 검색 대상 필드 */
  searchKeys: { type: Array, default: () => [] },
  /** 셀렉트 필터 — [{ key, label, options }] */
  filters: { type: Array, default: () => [] },
  /** 삭제 확인 문구에 쓸 필드 */
  titleKey: { type: String, default: 'name' },
  /** 요약 통계 계산 — (items) => [{ label, value, class }] */
  statsFn: { type: Function, default: null },
})

const auth = useAuthStore()
const canWrite = computed(() => !props.menuKey || auth.canWrite(props.menuKey))

const items = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const keyword = ref('')
const filterValues = reactive({})
props.filters.forEach(f => { filterValues[f.key] = '' })

const showForm = ref(false)
const editId = ref(null)
const form = ref({})
const deleteTarget = ref(null)

function emptyForm() {
  const o = {}
  props.fields.forEach(f => {
    o[f.key] = f.type === 'checkbox' ? (f.default ?? false) : (f.default ?? '')
  })
  return o
}

async function load() {
  loading.value = true
  try {
    const res = await props.api.list()
    items.value = res.data || []
  } catch (e) {
    error.value = typeof e === 'string' ? e : '목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

// 다른 도메인을 참조하는 셀렉트(예: 연계 처리업무)는 옵션을 API에서 불러온다.
const asyncOptions = ref({})

function optionsOf(f) {
  return f.optionsFrom ? (asyncOptions.value[f.key] || []) : (f.options || [])
}

async function loadAsyncOptions() {
  const dynamic = props.fields.filter(f => f.optionsFrom)
  if (!dynamic.length) return
  const loaded = { ...asyncOptions.value }
  await Promise.all(dynamic.map(async (f) => {
    try {
      loaded[f.key] = await f.optionsFrom()
    } catch {
      loaded[f.key] = []
    }
  }))
  asyncOptions.value = loaded
}

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return items.value.filter(row => {
    for (const f of props.filters) {
      const v = filterValues[f.key]
      if (v && String(row[f.key]) !== v) return false
    }
    if (!kw) return true
    const keys = props.searchKeys.length ? props.searchKeys : props.columns.map(c => c.key)
    return keys.some(k => String(row[k] ?? '').toLowerCase().includes(kw))
  })
})

const stats = computed(() => (props.statsFn ? props.statsFn(items.value) : []))

function openCreate() {
  editId.value = null
  form.value = emptyForm()
  error.value = ''
  showForm.value = true
  loadAsyncOptions()
}

function openEdit(row) {
  editId.value = row.id
  const o = emptyForm()
  props.fields.forEach(f => {
    const v = row[f.key]
    if (v === null || v === undefined) return
    o[f.key] = f.type === 'checkbox' ? !!v : v
  })
  form.value = o
  error.value = ''
  showForm.value = true
  loadAsyncOptions()
}

async function submit() {
  saving.value = true
  error.value = ''
  try {
    const payload = {}
    props.fields.forEach(f => {
      const v = form.value[f.key]
      // 빈 문자열은 서버로 보내지 않는다 — PATCH 부분수정에서 기존 값을 지우지 않도록
      if (v === '' || v === null || v === undefined) return
      payload[f.key] = f.type === 'number' ? Number(v) : v
    })
    // 체크박스는 false도 의미가 있으므로 항상 포함한다
    props.fields.filter(f => f.type === 'checkbox').forEach(f => {
      payload[f.key] = !!form.value[f.key]
    })
    if (editId.value) await props.api.update(editId.value, payload)
    else await props.api.create(payload)
    showForm.value = false
    await load()
  } catch (e) {
    error.value = typeof e === 'string' ? e : '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

function confirmDelete(row) {
  deleteTarget.value = row
}

async function doDelete() {
  saving.value = true
  try {
    await props.api.delete(deleteTarget.value.id)
    deleteTarget.value = null
    await load()
  } catch (e) {
    error.value = typeof e === 'string' ? e : '삭제에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

function inputType(t) {
  if (t === 'date') return 'date'
  if (t === 'datetime') return 'datetime-local'
  if (t === 'number') return 'number'
  return 'text'
}

function labelOf(col, value) {
  if (value === null || value === undefined || value === '') return '—'
  return col.labels?.[value] ?? value
}

function badgeClass(col, value) {
  return col.badges?.[value] || 'badge-gray'
}

function display(v) {
  if (v === null || v === undefined || v === '') return '—'
  return v
}

onMounted(load)
defineExpose({ reload: load })
</script>
