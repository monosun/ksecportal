<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <h2 class="text-lg font-semibold text-gray-900">{{ isEdit ? '관련 사이트 수정' : '관련 사이트 등록' }}</h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <form id="relatedSiteForm" @submit.prevent="handleSubmit" class="px-5 py-4 overflow-y-auto flex-1 space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">사이트 이름 *</label>
          <input v-model="form.name" type="text" class="input w-full" required placeholder="예: KISA 인터넷 보호나라" />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">홈페이지 주소 *</label>
          <input v-model="form.url" type="text" class="input w-full" required placeholder="https://www.boho.or.kr" />
          <p class="text-[11px] text-gray-400 mt-1">http:// 를 생략하면 https:// 로 저장됩니다.</p>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">게시물 목록(RSS/Atom) 주소</label>
          <input v-model="form.feedUrl" type="text" class="input w-full"
            placeholder="비워두면 홈페이지에서 자동으로 찾습니다" />
          <p class="text-[11px] text-gray-400 mt-1">
            피드를 찾지 못하면 사이트 소개 내용(og 메타)만 가져와 보여줍니다.
          </p>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">분류</label>
            <input v-model="form.category" type="text" class="input w-full" list="relatedSiteCategories"
              placeholder="예: 유관기관" />
            <datalist id="relatedSiteCategories">
              <option v-for="c in categories" :key="c" :value="c" />
            </datalist>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">정렬 순서</label>
            <input v-model.number="form.sortOrder" type="number" class="input w-full" placeholder="숫자가 작을수록 위" />
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
          <textarea v-model="form.description" rows="3" class="input w-full"
            placeholder="이 사이트를 어떤 업무에 참고하는지 적어두면 좋습니다"></textarea>
        </div>

        <label class="flex items-center gap-2 text-sm text-gray-700">
          <input v-model="form.active" type="checkbox" class="rounded border-gray-300" />
          사용 (해제하면 관련 사이트 화면에서 숨겨집니다)
        </label>

        <p v-if="error" class="text-red-600 text-sm">{{ error }}</p>
        <p v-if="loading" class="text-xs text-gray-400">사이트 내용을 가져오는 중입니다. 잠시 걸릴 수 있습니다…</p>
      </form>

      <div class="flex justify-end gap-3 px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">취소</button>
        <button type="submit" form="relatedSiteForm" class="btn-primary text-sm" :disabled="loading">
          {{ loading ? '저장 중…' : '저장' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { relatedSiteApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  editId: { type: [Number, String], default: null },
  categories: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'saved'])

const isEdit = computed(() => !!props.editId)

function emptyForm() {
  return { name: '', url: '', feedUrl: '', category: '', description: '', sortOrder: null, active: true }
}
const form = ref(emptyForm())
const loading = ref(false)
const error = ref('')

watch(() => props.open, async (open) => {
  if (!open) return
  error.value = ''
  form.value = emptyForm()
  if (props.editId) {
    try {
      const res = await relatedSiteApi.get(props.editId)
      const s = res.data || res
      form.value = {
        name: s.name || '',
        url: s.url || '',
        feedUrl: s.feedUrl || '',
        category: s.category || '',
        description: s.description || '',
        sortOrder: s.sortOrder ?? null,
        active: s.active !== false,
      }
    } catch (e) {
      error.value = e?.message || e || '사이트 정보를 불러오지 못했습니다.'
    }
  }
})

async function handleSubmit() {
  loading.value = true
  error.value = ''
  try {
    const payload = { ...form.value, feedUrl: form.value.feedUrl?.trim() || '' }
    if (isEdit.value) await relatedSiteApi.update(props.editId, payload)
    else await relatedSiteApi.create(payload)
    emit('saved')
  } catch (e) {
    error.value = e?.message || e || '저장에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>
