<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-6xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <div>
          <h2 class="text-lg font-semibold text-gray-900">개인정보 전체 흐름 지도</h2>
          <p class="text-xs text-gray-500 mt-0.5">
            처리업무에서 시스템·개인정보파일을 거쳐 외부로 제공되기까지의 흐름입니다.
          </p>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="px-5 py-5 overflow-y-auto flex-1">
        <div v-if="loading" class="text-center py-12 text-gray-400">불러오는 중...</div>
        <div v-else-if="!rows.length" class="text-center py-12 text-gray-400">
          등록된 처리업무가 없습니다.
        </div>

        <div v-else>
          <!-- 컬럼 헤더 -->
          <div class="grid grid-cols-[minmax(0,1fr)_28px_minmax(0,1.1fr)_28px_minmax(0,1.1fr)] gap-y-3 mb-2">
            <p class="text-xs font-bold text-gray-500 uppercase tracking-wider">처리업무</p>
            <span></span>
            <p class="text-xs font-bold text-gray-500 uppercase tracking-wider">시스템 · 개인정보파일</p>
            <span></span>
            <p class="text-xs font-bold text-gray-500 uppercase tracking-wider">제공 · 국외이전</p>
          </div>

          <div class="space-y-3">
            <div v-for="row in rows" :key="row.id"
              class="grid grid-cols-[minmax(0,1fr)_28px_minmax(0,1.1fr)_28px_minmax(0,1.1fr)] items-center gap-y-3">

              <!-- 처리업무 -->
              <div class="rounded-xl border-2 border-primary-200 bg-primary-50/50 px-4 py-3">
                <p class="text-sm font-bold text-gray-900">{{ row.name }}</p>
                <p v-if="row.department" class="text-xs text-gray-500 mt-0.5">{{ row.department }}</p>
                <div v-if="row.lifecycle" class="flex flex-wrap gap-1 mt-2">
                  <span v-for="s in row.lifecycle.split(',')" :key="s"
                    class="px-1.5 py-0.5 rounded bg-white border border-primary-200 text-[10px] text-primary-600">
                    {{ s.trim() }}
                  </span>
                </div>
              </div>

              <Arrow />

              <!-- 시스템 · 파일 -->
              <div v-if="row.systemName || row.files.length"
                class="rounded-xl border border-gray-200 px-4 py-3">
                <p class="text-sm font-semibold text-gray-800">{{ row.systemName || '—' }}</p>
                <div v-if="row.files.length" class="mt-2 space-y-1">
                  <div v-for="f in row.files" :key="f.id" class="flex items-center gap-1.5 flex-wrap">
                    <span class="text-xs text-gray-600">{{ f.name }}</span>
                    <span v-if="f.sensitiveInfo" class="badge-orange">민감</span>
                    <span v-if="f.uniqueIdentifier" class="badge-red">고유식별</span>
                  </div>
                </div>
                <p v-else class="text-xs text-gray-400 mt-1">연결된 개인정보파일 없음</p>
              </div>
              <div v-else class="rounded-xl border border-dashed border-gray-200 px-4 py-3 text-xs text-gray-300">
                시스템 미지정
              </div>

              <Arrow :muted="!row.provisions.length" />

              <!-- 제공 · 국외이전 -->
              <div v-if="row.provisions.length" class="space-y-1.5">
                <div v-for="p in row.provisions" :key="p.id"
                  class="rounded-xl border border-gray-200 px-3 py-2">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="text-sm font-medium text-gray-800">{{ p.recipient }}</span>
                    <span :class="TYPE_BADGE[p.provisionType]">{{ TYPE[p.provisionType] }}</span>
                    <span v-if="p.country" class="text-xs text-orange-600">{{ p.country }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="rounded-xl border border-dashed border-gray-200 px-4 py-3 text-xs text-gray-300">
                외부 제공 없음
              </div>
            </div>
          </div>

          <!-- 연결되지 않은 개인정보파일 -->
          <section v-if="orphanFiles.length" class="mt-8">
            <h3 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
              처리업무에 연결되지 않은 개인정보파일 ({{ orphanFiles.length }})
            </h3>
            <p class="text-xs text-gray-400 mb-3">
              이 파일들은 흐름 지도에 나타나지 않습니다.
              보유 중인데 처리업무가 없다면 <b>처리 근거가 불명확한 개인정보</b>일 수 있으니 확인이 필요합니다.
              시스템명을 처리현황과 동일하게 맞추면 연결됩니다.
            </p>
            <div class="flex flex-wrap gap-2">
              <div v-for="f in orphanFiles" :key="f.id"
                class="rounded-xl border border-amber-200 bg-amber-50/50 px-3 py-2">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="text-sm font-medium text-gray-800">{{ f.name }}</span>
                  <span v-if="f.sensitiveInfo" class="badge-orange">민감</span>
                  <span v-if="f.uniqueIdentifier" class="badge-red">고유식별</span>
                </div>
                <p class="text-xs text-gray-500 mt-0.5">{{ f.systemName || '시스템 미지정' }}</p>
              </div>
            </div>
          </section>

          <!-- 연결되지 않은 제공 -->
          <section v-if="unlinked.length" class="mt-8">
            <h3 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
              처리업무에 연결되지 않은 제공 ({{ unlinked.length }})
            </h3>
            <p class="text-xs text-gray-400 mb-3">
              제공관리에서 <b>연계 처리업무</b>를 지정하면 위 흐름 지도에 함께 표시됩니다.
            </p>
            <div class="flex flex-wrap gap-2">
              <div v-for="p in unlinked" :key="p.id"
                class="rounded-xl border border-gray-200 bg-gray-50 px-3 py-2">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="text-sm text-gray-700">{{ p.recipient }}</span>
                  <span :class="TYPE_BADGE[p.provisionType]">{{ TYPE[p.provisionType] }}</span>
                  <span v-if="p.country" class="text-xs text-orange-600">{{ p.country }}</span>
                </div>
              </div>
            </div>
          </section>

          <!-- 위탁 -->
          <section v-if="contractors.length" class="mt-8">
            <h3 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
              처리위탁 — 수탁사 ({{ contractors.length }})
            </h3>
            <p class="text-xs text-gray-400 mb-3">
              수탁사는 처리업무와 연결하는 항목이 없어 목록으로 표시합니다.
            </p>
            <div class="flex flex-wrap gap-2">
              <div v-for="c in contractors" :key="c.id"
                class="rounded-xl border border-gray-200 bg-gray-50 px-3 py-2 max-w-xs">
                <p class="text-sm font-medium text-gray-800">{{ c.name }}</p>
                <p v-if="c.serviceType" class="text-xs text-gray-500 mt-0.5 truncate">{{ c.serviceType }}</p>
                <p v-if="c.subContractor" class="text-[11px] text-gray-400 mt-0.5 truncate">
                  재위탁: {{ c.subContractor }}
                </p>
              </div>
            </div>
          </section>

          <p class="text-xs text-gray-400 mt-8 pt-4 border-t">
            처리업무와 개인정보파일은 <b>시스템명이 같을 때</b> 연결합니다.
            제공처는 제공관리의 <b>연계 처리업무</b> 지정을 따릅니다.
          </p>
        </div>
      </div>

      <div class="flex justify-end px-5 py-3 border-t shrink-0">
        <button @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, h } from 'vue'
import { privacyProcessingApi, privacyFileApi, privacyProvisionApi, contractorApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
})
defineEmits(['close'])

const TYPE = { THIRD_PARTY: '제3자 제공', JOINT_USE: '공동이용', OVERSEAS: '국외이전' }
const TYPE_BADGE = { THIRD_PARTY: 'badge-blue', JOINT_USE: 'badge-yellow', OVERSEAS: 'badge-orange' }

/** 컬럼 사이 연결 화살표 */
const Arrow = (props) => h('div', { class: 'flex justify-center' }, [
  h('svg', {
    class: `w-5 h-5 ${props.muted ? 'text-gray-200' : 'text-gray-300'}`,
    fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24',
  }, [
    h('path', {
      'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2',
      d: 'M14 5l7 7m0 0l-7 7m7-7H3',
    }),
  ]),
])
Arrow.props = ['muted']

const processing = ref([])
const files = ref([])
const provisions = ref([])
const contractors = ref([])
const loading = ref(false)

const rows = computed(() =>
  processing.value.map(p => ({
    ...p,
    // 처리업무와 개인정보파일은 시스템명이 일치할 때 연결한다 (스키마상 FK가 없음)
    files: p.systemName
      ? files.value.filter(f => f.systemName && f.systemName === p.systemName)
      : [],
    provisions: provisions.value.filter(v => v.processingId === p.id),
  }))
)

const unlinked = computed(() => provisions.value.filter(v => !v.processingId))

/** 어떤 처리업무의 시스템명과도 일치하지 않는 개인정보파일 — 처리 근거 공백일 수 있어 별도로 드러낸다. */
const orphanFiles = computed(() => {
  const systems = new Set(processing.value.map(p => p.systemName).filter(Boolean))
  return files.value.filter(f => !f.systemName || !systems.has(f.systemName))
})

async function load() {
  loading.value = true
  try {
    const [pr, fi, pv, co] = await Promise.all([
      privacyProcessingApi.list(),
      privacyFileApi.list(),
      privacyProvisionApi.list(),
      contractorApi.list(),
    ])
    processing.value = pr.data || []
    files.value = fi.data || []
    provisions.value = pv.data || []
    contractors.value = co.data || []
  } finally {
    loading.value = false
  }
}

watch(() => props.open, (open) => { if (open) load() })
</script>
