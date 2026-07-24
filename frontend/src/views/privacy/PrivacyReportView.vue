<template>
  <div class="p-6 viz">
    <div class="flex items-center justify-between mb-4 flex-wrap gap-3">
      <div>
        <h1 class="text-xl font-bold text-gray-900">개인정보 현황보고서</h1>
        <p class="text-xs text-gray-500 mt-0.5">
          경영진 보고 및 ISMS-P 심사 대응을 위해 개인정보보호 전 영역을 집계합니다.
          <span v-if="r" class="text-gray-400">· 기준일 {{ r.generatedAt }}</span>
        </p>
      </div>
      <div class="flex gap-2">
        <button @click="showTable = !showTable" class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 10h18M3 14h18M7 6v12M3 6h18v12H3z"/>
          </svg>
          {{ showTable ? '그래프 보기' : '표로 보기' }}
        </button>
        <button @click="load" :disabled="loading" class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
          {{ loading ? '집계 중...' : '새로고침' }}
        </button>
        <button v-if="isManager" @click="downloadPdf" :disabled="pdfLoading || !r"
          class="btn-primary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          {{ pdfLoading ? '생성 중...' : 'PDF 다운로드' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="card text-center py-12 text-gray-400">집계 중...</div>
    <div v-else-if="error" class="card text-center py-12 text-red-500">{{ error }}</div>

    <template v-else-if="r">
      <!-- ── 조치 필요 알림 — 상태색 + 아이콘 + 라벨(색 단독 사용 금지) ── -->
      <div v-if="alerts.length"
        class="mb-4 rounded-2xl border border-red-100 bg-red-50/60 px-4 py-3 flex flex-wrap items-center gap-x-5 gap-y-2">
        <span class="text-sm font-bold text-red-700 flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
          </svg>
          즉시 조치 필요
        </span>
        <span v-for="a in alerts" :key="a.label" class="text-sm text-gray-700">
          {{ a.label }} <b class="text-red-700">{{ a.value.toLocaleString() }}</b>{{ a.unit || '건' }}
        </span>
      </div>
      <div v-else
        class="mb-4 rounded-2xl border border-emerald-100 bg-emerald-50/60 px-4 py-3 text-sm text-emerald-700 flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
        </svg>
        기한 초과·미조치 항목이 없습니다.
      </div>

      <!-- ── 표 보기 (접근성·수치 확인용) ── -->
      <div v-if="showTable" class="card !p-0 overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-xs text-gray-500">
            <tr>
              <th class="text-left px-5 py-2.5 font-medium">영역</th>
              <th class="text-left px-5 py-2.5 font-medium">지표</th>
              <th class="text-right px-5 py-2.5 font-medium">값</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="row in tableRows" :key="row.area + row.label">
              <td class="px-5 py-2 text-gray-500">{{ row.area }}</td>
              <td class="px-5 py-2 text-gray-700">{{ row.label }}</td>
              <td class="px-5 py-2 text-right font-semibold text-gray-900 tabular-nums">
                {{ row.value.toLocaleString() }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <template v-else>
        <!-- ── 핵심 지표 ── -->
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-3">
          <Kpi label="개인정보파일" :value="r.files.total"
            :sub="`운영중 ${r.files.active} · 민감 ${r.files.sensitive} · 고유식별 ${r.files.uniqueIdentifier}`" />
          <Kpi label="개인정보 처리업무" :value="r.processing.total"
            :sub="`운영중 ${r.processing.active} · 중단 ${r.processing.inactive}`" />
          <Kpi label="수탁사" :value="r.contractors.total"
            :sub="`점검 ${r.contractors.checked} · 미점검 ${r.contractors.unchecked}`" />
          <Kpi label="정보주체 권리행사" :value="r.rights.total"
            :sub="`처리중 ${r.rights.inProgress} · 완료 ${r.rights.completed}`" />
        </div>

        <!-- ── 이행률 미터 ── -->
        <section class="card !p-4 mb-3">
          <h2 class="text-xs font-bold text-gray-700 mb-3">이행률</h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-x-6 gap-y-4">
            <Meter label="수탁사 점검" :value="r.contractors.checked" :total="r.contractors.total" />
            <Meter label="파기 완료" :value="r.disposals.completed" :total="r.disposals.total" />
            <Meter label="영향평가(DPIA) 완료" :value="r.compliance.dpiaCompleted" :total="r.compliance.dpiaTotal" />
            <Meter label="안전조치 완료" :value="r.compliance.safeguardCompleted" :total="r.compliance.safeguardTotal" />
          </div>
        </section>

        <!-- ── 영역별 구성 ── -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-3 mb-3">
          <section class="card !p-4">
            <h2 class="text-xs font-bold text-gray-700 mb-3">영역별 구성</h2>
            <div class="space-y-4">
              <StackBar title="개인정보 처리업무" :segments="processingSegments" />
              <StackBar title="개인정보파일" :segments="fileSegments" />
              <StackBar title="제3자 제공·위탁" :segments="provisionSegments" />
              <StackBar title="유출사고" :segments="breachSegments" />
            </div>
          </section>

          <section class="card !p-4">
            <h2 class="text-xs font-bold text-gray-700 mb-3">보유·파기·권리행사</h2>
            <div class="space-y-4">
              <StackBar title="보유기간" :segments="retentionSegments" />
              <StackBar title="파기" :segments="disposalSegments" />
              <StackBar title="정보주체 권리행사" :segments="rightsSegments" />
              <div class="pt-1 text-[11px] text-gray-400">
                유출 정보주체 누계 {{ r.breaches.affectedSubjects.toLocaleString() }}명 ·
                영향평가 위험 높음 {{ r.compliance.dpiaHighRisk }}건
              </div>
            </div>
          </section>
        </div>

        <!-- ── 유형별 순위 막대 ── -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
          <section class="card !p-4">
            <h2 class="text-xs font-bold text-gray-700 mb-3">권리행사 유형별</h2>
            <RankBars :items="rightsByType" empty="접수된 권리행사가 없습니다." />
          </section>
          <section class="card !p-4">
            <h2 class="text-xs font-bold text-gray-700 mb-3">안전조치 유형별</h2>
            <RankBars :items="safeguardByType" empty="등록된 안전조치가 없습니다." />
          </section>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { privacyReportApi, exportApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const isManager = auth.isManager

const RIGHTS_TYPE = {
  ACCESS: '열람', CORRECTION: '정정', DELETION: '삭제',
  SUSPENSION: '처리정지', CONSENT_WITHDRAWAL: '동의철회',
}
const SAFEGUARD_TYPE = {
  ACCESS_REVIEW: '접근권한', ACCESS_REVOKE: '권한회수', ENCRYPTION: '암호화',
  ACCESS_LOG_REVIEW: '접속기록', PRINTOUT: '출력물', EXPORT: '반출', DORMANT_ACCOUNT: '휴면계정',
}

// 마크 색은 역할로만 쓴다. 상태색(양호/주의/심각)은 상태 표현 전용이고,
// 크기 비교용 막대는 단일 색(파랑) — 순서가 크기를 말하므로 색을 늘리지 않는다.
const C = {
  good: 'var(--good)', warning: 'var(--warning)', serious: 'var(--serious)',
  critical: 'var(--critical)', neutral: 'var(--neutral)', seq: 'var(--seq)',
}

/** 핵심 지표 타일 — 값은 크게, 보조 수치는 한 줄로 */
const Kpi = (props) => h('div', { class: 'card !p-4' }, [
  h('p', { class: 'text-[11px] text-gray-500' }, props.label),
  h('p', { class: 'text-3xl font-bold text-gray-900 leading-tight mt-0.5 tabular-nums' },
    Number(props.value ?? 0).toLocaleString()),
  h('p', { class: 'text-[11px] text-gray-400 mt-1' }, props.sub),
])
Kpi.props = ['label', 'value', 'sub']

/** 단일 비율 미터 — 트랙 위 한 줄, 값·분모를 항상 글자로 함께 보여준다 */
const Meter = (props) => {
  const total = Number(props.total ?? 0)
  const value = Number(props.value ?? 0)
  const pct = total > 0 ? Math.round((value / total) * 100) : 0
  const color = total === 0 ? C.neutral : pct >= 90 ? C.good : pct >= 70 ? C.warning : C.critical
  return h('div', null, [
    h('div', { class: 'flex items-baseline justify-between gap-2' }, [
      h('span', { class: 'text-xs text-gray-600' }, props.label),
      h('span', { class: 'text-sm font-bold text-gray-900 tabular-nums' },
        total > 0 ? `${pct}%` : '—'),
    ]),
    h('div', { class: 'mt-1.5 h-2.5 rounded-full bg-gray-100 overflow-hidden' }, [
      h('div', {
        class: 'h-full rounded-full transition-[width] duration-500',
        style: { width: `${pct}%`, background: color },
        title: `${props.label} ${value}/${total} (${pct}%)`,
      }),
    ]),
    h('p', { class: 'text-[11px] text-gray-400 mt-1 tabular-nums' },
      `${value.toLocaleString()} / ${total.toLocaleString()}건`),
  ])
}
Meter.props = ['label', 'value', 'total']

/** 부분-전체 가로 스택 막대 — 조각 사이 2px 표면 간격, 아래에 범례(색+라벨+값) */
const StackBar = (props) => {
  const segs = (props.segments || []).map(s => ({ ...s, value: Number(s.value ?? 0) }))
  const total = segs.reduce((a, s) => a + s.value, 0)
  return h('div', null, [
    h('div', { class: 'flex items-baseline justify-between gap-2 mb-1.5' }, [
      h('span', { class: 'text-xs font-medium text-gray-700' }, props.title),
      h('span', { class: 'text-xs text-gray-400 tabular-nums' }, `총 ${total.toLocaleString()}건`),
    ]),
    total === 0
      ? h('div', { class: 'h-3.5 rounded-full bg-gray-100' })
      : h('div', { class: 'flex gap-0.5 h-3.5' }, segs.filter(s => s.value > 0).map((s, i, arr) =>
          h('div', {
            key: s.label,
            class: ['h-full', i === 0 ? 'rounded-l-full' : '', i === arr.length - 1 ? 'rounded-r-full' : ''].join(' '),
            style: { width: `${(s.value / total) * 100}%`, background: s.color },
            title: `${s.label} ${s.value.toLocaleString()}건 (${Math.round((s.value / total) * 100)}%)`,
          }))),
    h('div', { class: 'flex flex-wrap gap-x-3 gap-y-1 mt-1.5' }, segs.map(s =>
      h('span', { key: s.label, class: 'inline-flex items-center gap-1 text-[11px] text-gray-500' }, [
        h('span', { class: 'w-2 h-2 rounded-sm shrink-0', style: { background: s.color } }),
        s.label,
        h('b', { class: 'text-gray-800 tabular-nums' }, s.value.toLocaleString()),
      ]))),
  ])
}
StackBar.props = ['title', 'segments']

/** 크기 비교용 가로 막대 — 큰 값 순 정렬, 단일 색, 값은 막대 끝에 직접 표기 */
const RankBars = (props) => {
  const items = [...(props.items || [])].sort((a, b) => b.value - a.value)
  if (!items.length) return h('p', { class: 'text-xs text-gray-400 py-6 text-center' }, props.empty)
  const max = Math.max(...items.map(i => i.value), 1)
  return h('div', { class: 'space-y-2' }, items.map(it =>
    h('div', { key: it.label, class: 'flex items-center gap-2' }, [
      h('span', { class: 'w-20 shrink-0 text-[11px] text-gray-500 truncate', title: it.label }, it.label),
      h('div', { class: 'flex-1 h-3.5 bg-gray-50 rounded-r' }, [
        h('div', {
          class: 'h-full rounded-r',
          style: { width: `${(it.value / max) * 100}%`, background: C.seq, minWidth: it.value > 0 ? '3px' : '0' },
          title: `${it.label} ${it.value.toLocaleString()}건`,
        }),
      ]),
      h('span', { class: 'w-10 text-right text-xs font-semibold text-gray-800 tabular-nums' },
        it.value.toLocaleString()),
    ])))
}
RankBars.props = ['items', 'empty']

const r = ref(null)
const loading = ref(false)
const pdfLoading = ref(false)
const error = ref('')
const showTable = ref(false)

// ── 그래프 데이터 ──────────────────────────────────────────────────────
const processingSegments = computed(() => [
  { label: '운영중', value: r.value.processing.active, color: C.good },
  { label: '중단', value: r.value.processing.inactive, color: C.neutral },
])
const fileSegments = computed(() => [
  { label: '운영중', value: r.value.files.active, color: C.good },
  { label: '그 외', value: Math.max(r.value.files.total - r.value.files.active, 0), color: C.neutral },
])
const provisionSegments = computed(() => [
  { label: '제3자 제공', value: r.value.provisions.thirdParty, color: C.seq },
  { label: '공동이용', value: r.value.provisions.jointUse, color: C.neutral },
  { label: '국외이전', value: r.value.provisions.overseas, color: C.serious },
])
const retentionSegments = computed(() => {
  const t = r.value.retentions
  const normal = Math.max(t.total - t.expiringIn30Days - t.overdue - t.disposed, 0)
  return [
    { label: '기간 내', value: normal, color: C.good },
    { label: '30일 내 만료', value: t.expiringIn30Days, color: C.warning },
    { label: '만료 경과', value: t.overdue, color: C.critical },
    { label: '파기 완료', value: t.disposed, color: C.neutral },
  ]
})
const disposalSegments = computed(() => [
  { label: '완료', value: r.value.disposals.completed, color: C.good },
  { label: '승인대기', value: r.value.disposals.pendingApproval, color: C.warning },
  { label: '계획', value: r.value.disposals.planned, color: C.neutral },
])
const rightsSegments = computed(() => [
  { label: '완료', value: r.value.rights.completed, color: C.good },
  { label: '처리중', value: r.value.rights.inProgress, color: C.seq },
  { label: '기한 초과', value: r.value.rights.slaBreached, color: C.critical },
])
const breachSegments = computed(() => [
  { label: '종결', value: Math.max(r.value.breaches.total - r.value.breaches.open, 0), color: C.good },
  { label: '미종결', value: r.value.breaches.open, color: C.critical },
])

const rightsByType = computed(() =>
  Object.entries(r.value.rights.byType || {})
    .map(([k, v]) => ({ label: RIGHTS_TYPE[k] || k, value: Number(v) })))
const safeguardByType = computed(() =>
  (r.value.compliance.safeguardByType || [])
    .map(t => ({ label: SAFEGUARD_TYPE[t.type] || t.type, value: Number(t.count) })))

/** 기한 초과·미조치 — 0인 항목은 표시하지 않는다 */
const alerts = computed(() => {
  if (!r.value) return []
  return [
    { label: '보유기간 만료 경과', value: r.value.retentions.overdue },
    { label: '유출 신고기한 경과', value: r.value.breaches.reportOverdue },
    { label: '권리행사 처리기한 초과', value: r.value.rights.slaBreached },
    { label: '유출사고 미종결', value: r.value.breaches.open },
    { label: '수탁사 미점검', value: r.value.contractors.unchecked },
    { label: '영향평가 위험 높음', value: r.value.compliance.dpiaHighRisk },
  ].filter(a => a.value > 0)
})

/** 표 보기 — 그래프와 같은 수치를 그대로 나열한다 */
const tableRows = computed(() => {
  if (!r.value) return []
  const d = r.value
  const rows = [
    ['개인정보 처리현황', '전체', d.processing.total], ['개인정보 처리현황', '운영중', d.processing.active],
    ['개인정보 처리현황', '중단', d.processing.inactive],
    ['개인정보파일', '전체', d.files.total], ['개인정보파일', '운영중', d.files.active],
    ['개인정보파일', '민감정보 포함', d.files.sensitive], ['개인정보파일', '고유식별정보 포함', d.files.uniqueIdentifier],
    ['수탁사', '전체', d.contractors.total], ['수탁사', '점검함', d.contractors.checked],
    ['수탁사', '미점검', d.contractors.unchecked],
    ['제3자 제공', '전체', d.provisions.total], ['제3자 제공', '제3자', d.provisions.thirdParty],
    ['제3자 제공', '공동이용', d.provisions.jointUse], ['제3자 제공', '국외이전', d.provisions.overseas],
    ['보유기간', '전체', d.retentions.total], ['보유기간', '30일 내 만료', d.retentions.expiringIn30Days],
    ['보유기간', '만료 경과', d.retentions.overdue], ['보유기간', '파기 완료', d.retentions.disposed],
    ['파기', '전체', d.disposals.total], ['파기', '계획', d.disposals.planned],
    ['파기', '승인대기', d.disposals.pendingApproval], ['파기', '완료', d.disposals.completed],
    ['권리행사', '전체', d.rights.total], ['권리행사', '처리중', d.rights.inProgress],
    ['권리행사', '완료', d.rights.completed], ['권리행사', '기한 초과', d.rights.slaBreached],
    ['유출사고', '전체', d.breaches.total], ['유출사고', '미종결', d.breaches.open],
    ['유출사고', '신고기한 경과', d.breaches.reportOverdue], ['유출사고', '유출 정보주체', d.breaches.affectedSubjects],
    ['법령 준수', 'DPIA 전체', d.compliance.dpiaTotal], ['법령 준수', 'DPIA 완료', d.compliance.dpiaCompleted],
    ['법령 준수', 'DPIA 위험 높음', d.compliance.dpiaHighRisk],
    ['법령 준수', '안전조치 전체', d.compliance.safeguardTotal],
    ['법령 준수', '안전조치 완료', d.compliance.safeguardCompleted],
  ].map(([area, label, value]) => ({ area, label, value: Number(value ?? 0) }))
  for (const [k, v] of Object.entries(d.rights.byType || {})) {
    rows.push({ area: '권리행사 유형', label: RIGHTS_TYPE[k] || k, value: Number(v) })
  }
  for (const t of d.compliance.safeguardByType || []) {
    rows.push({ area: '안전조치 유형', label: SAFEGUARD_TYPE[t.type] || t.type, value: Number(t.count) })
  }
  return rows
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await privacyReportApi.summary()
    r.value = res.data
  } catch (e) {
    error.value = typeof e === 'string' ? e : '보고서를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function downloadPdf() {
  pdfLoading.value = true
  try {
    await exportApi.privacyPdf()
  } catch (e) {
    error.value = typeof e === 'string' ? e : 'PDF 생성에 실패했습니다.'
  } finally {
    pdfLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
/* 그래프 색 — 역할별로만 정의한다.
   상태색(양호·주의·심각)은 상태 표현 전용, seq(파랑)는 크기 비교용 단일 색. */
.viz {
  --good: #0ca30c;
  --warning: #fab219;
  --serious: #ec835a;
  --critical: #d03b3b;
  --neutral: #b9b8b2;
  --seq: #2a78d6;
}
</style>
