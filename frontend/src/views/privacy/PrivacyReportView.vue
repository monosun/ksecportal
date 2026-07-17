<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-4">
      <div>
        <h1 class="text-xl font-bold text-gray-900">개인정보 현황보고서</h1>
        <p class="text-xs text-gray-500 mt-0.5">
          경영진 보고 및 ISMS-P 심사 대응을 위해 개인정보보호 전 영역을 집계합니다.
          <span v-if="r" class="text-gray-400">· 기준일 {{ r.generatedAt }}</span>
        </p>
      </div>
      <div class="flex gap-2">
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

    <!-- 한 화면에 들어가도록 3열 그리드로 압축 배치 -->
    <div v-else-if="r" class="grid grid-cols-1 lg:grid-cols-3 gap-3">

      <Section title="개인정보 처리현황">
        <Stat label="전체" :value="r.processing.total" />
        <Stat label="운영중" :value="r.processing.active" tone="green" />
        <Stat label="중단" :value="r.processing.inactive" tone="gray" />
      </Section>

      <Section title="개인정보파일 현황">
        <Stat label="전체" :value="r.files.total" />
        <Stat label="운영중" :value="r.files.active" tone="green" />
        <Stat label="민감정보" :value="r.files.sensitive" tone="orange" />
        <Stat label="고유식별" :value="r.files.uniqueIdentifier" tone="red" />
      </Section>

      <Section title="수탁사 현황">
        <Stat label="전체" :value="r.contractors.total" />
        <Stat label="점검함" :value="r.contractors.checked" tone="green" />
        <Stat label="미점검" :value="r.contractors.unchecked" tone="red" />
      </Section>

      <Section title="제3자 제공 현황">
        <Stat label="전체" :value="r.provisions.total" />
        <Stat label="제3자" :value="r.provisions.thirdParty" />
        <Stat label="공동이용" :value="r.provisions.jointUse" />
        <Stat label="국외이전" :value="r.provisions.overseas" tone="orange" />
      </Section>

      <Section title="보유기간 현황">
        <Stat label="전체" :value="r.retentions.total" />
        <Stat label="30일내 만료" :value="r.retentions.expiringIn30Days" tone="yellow" />
        <Stat label="만료 경과" :value="r.retentions.overdue" tone="red" />
        <Stat label="파기완료" :value="r.retentions.disposed" tone="gray" />
      </Section>

      <Section title="파기 현황">
        <Stat label="전체" :value="r.disposals.total" />
        <Stat label="계획" :value="r.disposals.planned" tone="gray" />
        <Stat label="승인대기" :value="r.disposals.pendingApproval" tone="yellow" />
        <Stat label="완료" :value="r.disposals.completed" tone="green" />
      </Section>

      <Section title="정보주체 권리행사 현황" class="lg:col-span-2">
        <Stat label="전체" :value="r.rights.total" />
        <Stat label="처리중" :value="r.rights.inProgress" tone="blue" />
        <Stat label="완료" :value="r.rights.completed" tone="green" />
        <Stat label="기한 초과" :value="r.rights.slaBreached" tone="red" />
        <template #footer>
          <div class="flex flex-wrap gap-1">
            <span v-for="(v, k) in r.rights.byType" :key="k"
              class="px-1.5 py-0.5 rounded bg-gray-50 border border-gray-100 text-[11px] text-gray-500">
              {{ RIGHTS_TYPE[k] || k }} <b class="text-gray-800">{{ v }}</b>
            </span>
          </div>
        </template>
      </Section>

      <Section title="유출사고 현황">
        <Stat label="전체" :value="r.breaches.total" />
        <Stat label="미종결" :value="r.breaches.open" tone="red" />
        <Stat label="신고기한 경과" :value="r.breaches.reportOverdue" tone="red" />
        <Stat label="유출 주체" :value="r.breaches.affectedSubjects" />
      </Section>

      <Section title="법령 준수현황" class="lg:col-span-3">
        <Stat label="DPIA 전체" :value="r.compliance.dpiaTotal" />
        <Stat label="DPIA 완료" :value="r.compliance.dpiaCompleted" tone="green" />
        <Stat label="DPIA 위험 높음" :value="r.compliance.dpiaHighRisk" tone="red" />
        <Stat label="보호조치 전체" :value="r.compliance.safeguardTotal" />
        <Stat label="보호조치 완료" :value="r.compliance.safeguardCompleted" tone="green" />
        <template #footer>
          <div v-if="r.compliance.safeguardByType?.length" class="flex flex-wrap gap-1">
            <span v-for="t in r.compliance.safeguardByType" :key="t.type"
              class="px-1.5 py-0.5 rounded bg-gray-50 border border-gray-100 text-[11px] text-gray-500">
              {{ SAFEGUARD_TYPE[t.type] || t.type }} <b class="text-gray-800">{{ t.count }}</b>
            </span>
          </div>
        </template>
      </Section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue'
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

const TONE = {
  green: 'text-emerald-600', red: 'text-red-600', orange: 'text-orange-600',
  yellow: 'text-yellow-600', blue: 'text-blue-600', gray: 'text-gray-400',
}

/** 보고서 지표 타일 — 한 화면에 담기도록 작게 유지한다 */
const Stat = (props) => h('div', { class: 'text-center' }, [
  h('p', { class: `text-lg font-bold leading-tight ${TONE[props.tone] || 'text-gray-900'}` },
    Number(props.value ?? 0).toLocaleString()),
  h('p', { class: 'text-[11px] text-gray-500 mt-0.5 leading-tight' }, props.label),
])
Stat.props = ['label', 'value', 'tone']

/** 지표를 담는 섹션 카드 */
const Section = (props, { slots }) => h('section', { class: 'card !p-3.5' }, [
  h('h2', { class: 'text-xs font-bold text-gray-700 mb-2.5' }, props.title),
  h('div', { class: 'flex justify-around gap-2' }, slots.default?.()),
  slots.footer ? h('div', { class: 'mt-2.5 pt-2.5 border-t border-gray-100' }, slots.footer()) : null,
])
Section.props = ['title']

const r = ref(null)
const loading = ref(false)
const pdfLoading = ref(false)
const error = ref('')

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
