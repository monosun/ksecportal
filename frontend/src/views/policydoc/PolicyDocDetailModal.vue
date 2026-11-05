<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[92vh] flex flex-col">
      <div class="flex items-start justify-between gap-4 px-5 py-3 border-b shrink-0">
        <div v-if="doc" class="min-w-0">
          <h2 class="text-lg font-semibold text-gray-900 truncate">{{ doc.title }}</h2>
          <div class="flex flex-wrap items-center gap-1.5 mt-1.5">
            <span :class="categoryBadge(doc.category)" class="text-xs font-medium px-2 py-0.5 rounded">{{ categoryLabel(doc.category) }}</span>
            <span :class="doc.status === 'ABOLISHED' ? 'badge-red' : 'badge-green'">{{ doc.status === 'ABOLISHED' ? '폐지' : '시행' }}</span>
            <span v-if="doc.current" class="badge-gray">현행 v{{ doc.current.version }}</span>
            <span v-if="doc.docNo" class="text-xs text-gray-400 font-mono">{{ doc.docNo }}</span>
          </div>
        </div>
        <div v-else class="text-lg font-semibold text-gray-900">보안정책관리</div>
        <div class="flex items-center gap-2 shrink-0">
          <button v-if="doc?.current?.fileName" @click="view(doc.current)" class="btn-primary text-sm">현행본 보기</button>
          <button v-if="doc && perm.canWrite()" @click="$emit('edit', doc.id)" class="btn-secondary text-sm">{{ $t('common.edit') }}</button>
          <button v-if="doc && perm.canDelete()" @click="removeDoc" class="btn-danger text-sm">{{ $t('common.delete') }}</button>
          <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1">
        <div v-if="loading" class="py-16 text-center text-gray-400">{{ $t('common.loading') }}</div>
        <div v-else-if="loadError" class="py-16 text-center text-sm text-red-500">{{ loadError }}</div>
        <template v-else-if="doc">
          <div class="border border-gray-200 rounded-lg p-4 mb-4 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div><p class="text-gray-500">주관부서</p><p class="font-medium mt-1">{{ doc.department || '-' }}</p></div>
            <div><p class="text-gray-500">관리 담당자</p><p class="font-medium mt-1">{{ doc.owner || '-' }}</p></div>
            <div><p class="text-gray-500">현행 시행일</p><p class="font-medium mt-1">{{ doc.current?.effectiveDate || '-' }}</p></div>
            <div><p class="text-gray-500">등록자</p><p class="font-medium mt-1">{{ doc.registrantName || '-' }}</p></div>
            <div v-if="doc.description" class="col-span-2 md:col-span-4">
              <p class="text-gray-500">문서 설명</p>
              <p class="mt-1 text-gray-800 whitespace-pre-line">{{ doc.description }}</p>
            </div>
          </div>

          <div class="flex items-center justify-between mb-2">
            <h3 class="text-sm font-semibold text-gray-700">버전 이력 <span class="text-gray-400 font-normal">({{ doc.revisionCount }})</span></h3>
            <button v-if="perm.canWrite()" @click="openRevision(null)" class="btn-secondary text-xs">+ 새 버전 등록</button>
          </div>

          <ol class="relative border-l-2 border-gray-100 ml-2 space-y-4">
            <li v-for="r in doc.revisions" :key="r.id" class="ml-4">
              <span :class="['absolute -left-[7px] mt-1.5 w-3 h-3 rounded-full border-2 border-white',
                             r.current ? 'bg-primary-600' : 'bg-gray-300']"></span>
              <div :class="['rounded-lg border p-3', r.current ? 'border-primary-200 bg-primary-50/40' : 'border-gray-200']">
                <div class="flex flex-wrap items-center justify-between gap-2">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="font-mono font-semibold text-sm">v{{ r.version }}</span>
                    <span :class="revisionBadge(r.revisionType)">{{ revisionLabel(r.revisionType) }}</span>
                    <span v-if="r.current" class="text-xs bg-primary-600 text-white px-1.5 py-0.5 rounded">현행</span>
                    <span class="text-xs text-gray-500">
                      {{ revisionLabel(r.revisionType) }}일 {{ r.revisionDate }}<template v-if="r.effectiveDate"> · 시행 {{ r.effectiveDate }}</template>
                    </span>
                  </div>
                  <div class="flex items-center gap-2 text-xs">
                    <template v-if="r.fileName">
                      <button @click="view(r)" class="text-primary-600 hover:text-primary-800 font-medium">보기</button>
                      <button @click="download(r)" class="text-gray-500 hover:text-primary-600">다운로드</button>
                    </template>
                    <button v-if="perm.canWrite()" @click="openRevision(r)" class="text-gray-500 hover:text-primary-600">수정</button>
                    <button v-if="perm.canDelete() && doc.revisionCount > 1" @click="removeRevision(r)" class="text-red-400 hover:text-red-600">삭제</button>
                  </div>
                </div>
                <div class="text-xs text-gray-400 mt-1">
                  <template v-if="r.approver">승인 {{ r.approver }} · </template>등록 {{ r.uploaderName || '-' }} · {{ formatDateTime(r.createdAt) }}
                </div>
                <div v-if="r.fileName" class="mt-2 flex items-center gap-1.5 text-xs text-gray-600">
                  <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                  <button @click="view(r)" class="hover:text-primary-600 hover:underline truncate">{{ r.fileName }}</button>
                  <span class="text-gray-400 shrink-0">{{ formatSize(r.fileSize) }}</span>
                </div>
                <div v-else class="mt-2 text-xs text-gray-400">첨부 파일 없음</div>
                <div v-if="r.changeReason" class="mt-2 text-sm">
                  <span class="text-xs font-semibold text-gray-500">{{ reasonTitle(r.revisionType) }}</span>
                  <p class="text-gray-800 whitespace-pre-line mt-0.5">{{ r.changeReason }}</p>
                </div>
                <div v-if="r.changeDetails" class="mt-2 text-sm">
                  <span class="text-xs font-semibold text-gray-500">주요 개정사항</span>
                  <p class="text-gray-800 whitespace-pre-line mt-0.5 bg-white border border-gray-100 rounded p-2 text-xs leading-relaxed">{{ r.changeDetails }}</p>
                </div>
              </div>
            </li>
          </ol>
        </template>
      </div>
    </div>

    <PolicyDocRevisionModal :open="revisionModal.open" :doc-id="doc?.id" :doc-title="doc?.title"
      :revision="revisionModal.revision" :current-version="doc?.current?.version"
      @close="revisionModal.open = false" @saved="onRevisionSaved" />

    <!-- 정책 문서 뷰어 — PDF·이미지·엑셀·텍스트는 바로, DOC·PPT 는 서버에서 PDF 로 변환해 보여준다 -->
    <FilePreviewModal :open="!!viewing" :file-name="viewing?.fileName"
      :title="viewing ? `${doc?.title} v${viewing.version} (${revisionLabel(viewing.revisionType)} ${viewing.revisionDate})` : ''"
      :loader="() => policyDocApi.fetchBlob(viewing.id)"
      :pdf-loader="() => policyDocApi.fetchPreviewPdf(viewing.id)"
      @close="viewing = null" @download="download(viewing)" />
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { policyDocApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import FilePreviewModal from '@/components/FilePreviewModal.vue'
import PolicyDocRevisionModal from './PolicyDocRevisionModal.vue'
import {
  categoryLabel, categoryBadge, revisionLabel, revisionBadge, formatSize, usePolicyDocPermission,
} from './policyDocMeta'

const props = defineProps({
  open: { type: Boolean, default: false },
  itemId: { type: [Number, String], default: null },
  // 목록에서 "보기"로 바로 열 때 — 상세를 띄우면서 현행본 뷰어를 연다
  autoView: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'edit', 'changed'])

const auth = useAuthStore()
const perm = usePolicyDocPermission(auth)

const doc = ref(null)
const loading = ref(false)
const loadError = ref('')
const viewing = ref(null)
const revisionModal = reactive({ open: false, revision: null })

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await policyDocApi.get(props.itemId)
    doc.value = res.data
  } catch (e) {
    loadError.value = e || '문서를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

watch(() => [props.open, props.itemId], async ([open]) => {
  viewing.value = null
  revisionModal.open = false
  if (!open || !props.itemId) { doc.value = null; return }
  await load()
  if (props.autoView && doc.value?.current?.fileName) view(doc.value.current)
}, { immediate: true })

function view(r) {
  viewing.value = r
}

async function download(r) {
  if (!r) return
  try {
    await policyDocApi.download(r.id, r.fileName)
  } catch (e) {
    alert('다운로드에 실패했습니다.')
  }
}

function openRevision(r) {
  revisionModal.revision = r
  revisionModal.open = true
}

async function onRevisionSaved() {
  revisionModal.open = false
  await load()
  emit('changed')
}

async function removeRevision(r) {
  if (!confirm(`v${r.version} 버전을 삭제하시겠습니까?\n첨부 파일도 함께 삭제되며 되돌릴 수 없습니다.`)) return
  try {
    await policyDocApi.deleteRevision(r.id)
    await load()
    emit('changed')
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  }
}

async function removeDoc() {
  if (!confirm(`"${doc.value.title}" 문서의 모든 버전(${doc.value.revisionCount}개)과 파일을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.`)) return
  try {
    await policyDocApi.delete(doc.value.id)
    emit('changed')
    emit('close')
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  }
}

function reasonTitle(type) {
  return { ENACTMENT: '제정 사유', ABOLITION: '폐지 사유' }[type] || '개정 사유'
}

function formatDateTime(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString()
}

defineExpose({ reload: load })
</script>
