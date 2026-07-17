<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-2">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">개인정보 현황보고서</h1>
        <p class="text-sm text-gray-500 mt-1">
          경영진 보고 및 ISMS-P 심사 대응을 위해 개인정보보호 전 영역을 집계합니다.
        </p>
      </div>
      <button @click="load" :disabled="loading" class="btn-secondary flex items-center gap-2 text-sm">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        {{ loading ? '집계 중...' : '새로고침' }}
      </button>
    </div>

    <div v-if="loading" class="card text-center py-12 text-gray-400">집계 중...</div>
    <div v-else-if="error" class="card text-center py-12 text-red-500">{{ error }}</div>

    <div v-else-if="r" class="page-body space-y-4">
      <p class="text-xs text-gray-400">기준일 {{ r.generatedAt }}</p>

      <!-- 1. 개인정보 처리현황 -->
      <section class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-3">개인정보 처리현황</h2>
        <div class="grid grid-cols-3 gap-4">
          <Stat label="전체 처리업무" :value="r.processing.total" />
          <Stat label="운영중" :value="r.processing.active" tone="green" />
          <Stat label="중단" :value="r.processing.inactive" tone="gray" />
        </div>
      </section>

      <!-- 2. 개인정보파일 현황 -->
      <section class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-3">개인정보파일 현황</h2>
        <div class="grid grid-cols-4 gap-4">
          <Stat label="전체 파일" :value="r.files.total" />
          <Stat label="운영중" :value="r.files.active" tone="green" />
          <Stat label="민감정보 포함" :value="r.files.sensitive" tone="orange" />
          <Stat label="고유식별정보 포함" :value="r.files.uniqueIdentifier" tone="red" />
        </div>
      </section>

      <!-- 3. 수탁사 현황 -->
      <section class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-3">수탁사 현황</h2>
        <div class="grid grid-cols-3 gap-4">
          <Stat label="전체 수탁사" :value="r.contractors.total" />
          <Stat label="점검 이력 있음" :value="r.contractors.checked" tone="green" />
          <Stat label="점검 이력 없음" :value="r.contractors.unchecked" tone="red" />
        </div>
      </section>

      <!-- 4. 제3자 제공 현황 -->
      <section class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-3">제3자 제공 현황</h2>
        <div class="grid grid-cols-4 gap-4">
          <Stat label="전체" :value="r.provisions.total" />
          <Stat label="제3자 제공" :value="r.provisions.thirdParty" />
          <Stat label="공동이용" :value="r.provisions.jointUse" />
          <Stat label="국외이전" :value="r.provisions.overseas" tone="orange" />
        </div>
      </section>

      <!-- 5. 보유기간 현황 -->
      <section class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-3">보유기간 현황</h2>
        <div class="grid grid-cols-4 gap-4">
          <Stat label="전체" :value="r.retentions.total" />
          <Stat label="30일 내 만료예정" :value="r.retentions.expiringIn30Days" tone="yellow" />
          <Stat label="만료 경과·미파기" :value="r.retentions.overdue" tone="red" />
          <Stat label="파기완료" :value="r.retentions.disposed" tone="gray" />
        </div>
      </section>

      <!-- 6. 파기 현황 -->
      <section class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-3">파기 현황</h2>
        <div class="grid grid-cols-4 gap-4">
          <Stat label="전체 파기계획" :value="r.disposals.total" />
          <Stat label="계획" :value="r.disposals.planned" tone="gray" />
          <Stat label="승인대기" :value="r.disposals.pendingApproval" tone="yellow" />
          <Stat label="파기완료" :value="r.disposals.completed" tone="green" />
        </div>
      </section>

      <!-- 7. 권리행사 현황 -->
      <section class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-3">정보주체 권리행사 현황</h2>
        <div class="grid grid-cols-4 gap-4 mb-4">
          <Stat label="전체 요청" :value="r.rights.total" />
          <Stat label="처리중" :value="r.rights.inProgress" tone="blue" />
          <Stat label="완료" :value="r.rights.completed" tone="green" />
          <Stat label="처리기한 초과" :value="r.rights.slaBreached" tone="red" />
        </div>
        <div class="flex flex-wrap gap-2">
          <span v-for="(v, k) in r.rights.byType" :key="k"
            class="px-2.5 py-1 rounded bg-gray-50 border border-gray-100 text-xs text-gray-600">
            {{ RIGHTS_TYPE[k] || k }} <b class="text-gray-900 ml-1">{{ v }}</b>
          </span>
        </div>
      </section>

      <!-- 8. 유출사고 현황 -->
      <section class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-3">유출사고 현황</h2>
        <div class="grid grid-cols-4 gap-4">
          <Stat label="전체 사고" :value="r.breaches.total" />
          <Stat label="미종결" :value="r.breaches.open" tone="red" />
          <Stat label="신고기한 경과" :value="r.breaches.reportOverdue" tone="red" />
          <Stat label="유출 정보주체 합계" :value="r.breaches.affectedSubjects" />
        </div>
      </section>

      <!-- 9. 법령 준수현황 -->
      <section class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-3">법령 준수현황</h2>
        <div class="grid grid-cols-5 gap-4 mb-4">
          <Stat label="DPIA 전체" :value="r.compliance.dpiaTotal" />
          <Stat label="DPIA 완료" :value="r.compliance.dpiaCompleted" tone="green" />
          <Stat label="DPIA 위험도 높음" :value="r.compliance.dpiaHighRisk" tone="red" />
          <Stat label="보호조치 전체" :value="r.compliance.safeguardTotal" />
          <Stat label="보호조치 완료" :value="r.compliance.safeguardCompleted" tone="green" />
        </div>
        <div v-if="r.compliance.safeguardByType?.length" class="flex flex-wrap gap-2">
          <span v-for="t in r.compliance.safeguardByType" :key="t.type"
            class="px-2.5 py-1 rounded bg-gray-50 border border-gray-100 text-xs text-gray-600">
            {{ SAFEGUARD_TYPE[t.type] || t.type }} <b class="text-gray-900 ml-1">{{ t.count }}</b>
          </span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue'
import { privacyReportApi } from '@/api'

const RIGHTS_TYPE = {
  ACCESS: '열람', CORRECTION: '정정', DELETION: '삭제',
  SUSPENSION: '처리정지', CONSENT_WITHDRAWAL: '동의철회',
}
const SAFEGUARD_TYPE = {
  ACCESS_REVIEW: '접근권한 점검', ACCESS_REVOKE: '권한회수', ENCRYPTION: '암호화',
  ACCESS_LOG_REVIEW: '접속기록 점검', PRINTOUT: '출력물', EXPORT: '반출', DORMANT_ACCOUNT: '휴면계정',
}

const TONE = {
  green: 'text-emerald-600', red: 'text-red-600', orange: 'text-orange-600',
  yellow: 'text-yellow-600', blue: 'text-blue-600', gray: 'text-gray-400',
}

/** 보고서 지표 타일 */
const Stat = (props) => h('div', {}, [
  h('p', { class: 'text-xs text-gray-500' }, props.label),
  h('p', { class: `text-2xl font-bold mt-1 ${TONE[props.tone] || 'text-gray-900'}` },
    Number(props.value ?? 0).toLocaleString()),
])
Stat.props = ['label', 'value', 'tone']

const r = ref(null)
const loading = ref(false)
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

onMounted(load)
</script>
