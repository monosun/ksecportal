<template>
  <PrivacyCrudView
    title="개인정보 처리현황"
    description="개인정보 처리업무 단위로 처리부서·목적·항목·보유기간·처리근거·처리시스템을 관리합니다."
    :api="privacyProcessingApi"
    menu-key="privacy_processing"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['name', 'department', 'purpose', 'systemName']"
    title-key="name"
    :stats-fn="statsFn"
  >
    <template #header-actions>
      <button @click="showMap = true" class="btn-secondary flex items-center gap-2 text-sm">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
        </svg>
        전체 흐름 지도
      </button>
    </template>

    <template #row-actions="{ row }">
      <button @click="openProvisions(row)" class="text-xs text-gray-500 hover:text-gray-700 px-2 py-1">
        제공
      </button>
      <button @click="openFlow(row)" class="text-xs text-primary-600 hover:text-primary-700 px-2 py-1">
        흐름도
      </button>
    </template>
  </PrivacyCrudView>

  <PrivacyFlowModal :open="showFlow" :item-id="flowId"
    @close="showFlow = false" @manage-provisions="onManageProvisions" />
  <PrivacyFlowMapModal :open="showMap" @close="showMap = false" />
  <PrivacyProvisionForProcessingModal
    :open="showProvisions"
    :processing-id="provisionTarget?.id"
    :processing-name="provisionTarget?.name || ''"
    :processing-info-items="provisionTarget?.infoItems || ''"
    @close="showProvisions = false" />
</template>

<script setup>
import { ref } from 'vue'
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import PrivacyFlowModal from './PrivacyFlowModal.vue'
import PrivacyFlowMapModal from './PrivacyFlowMapModal.vue'
import PrivacyProvisionForProcessingModal from './PrivacyProvisionForProcessingModal.vue'
import { privacyProcessingApi } from '@/api'
import { loadServiceAssets } from '@/utils/serviceAssets'

const showFlow = ref(false)
const flowId = ref(null)
const showMap = ref(false)
const showProvisions = ref(false)
const provisionTarget = ref(null)

function openFlow(row) {
  flowId.value = row.id
  showFlow.value = true
}

function openProvisions(row) {
  provisionTarget.value = row
  showProvisions.value = true
}

/** 흐름도에서 '제공 관리'를 누르면 흐름도를 닫고 제공 관리로 넘어간다. */
function onManageProvisions(item) {
  showFlow.value = false
  provisionTarget.value = item
  showProvisions.value = true
}

const STATUS = { ACTIVE: '운영중', INACTIVE: '중단' }

const fields = [
  { key: 'name', label: '처리업무명', required: true, placeholder: '예: 셀프개통, 고객센터, 배송, 정산, 마케팅' },
  { key: 'department', label: '처리부서' },
  { key: 'purpose', label: '처리목적', type: 'textarea', span: 2 },
  { key: 'infoItems', label: '개인정보 항목', type: 'textarea', span: 2, placeholder: '예: 성명, 휴대전화번호, 주소, 생년월일' },
  { key: 'retentionPeriod', label: '보유기간', placeholder: '예: 수집일로부터 3년' },
  { key: 'legalBasis', label: '처리근거', placeholder: '예: 정보주체 동의 / 계약 이행 / 법령상 의무' },
  { key: 'systemName', label: '처리시스템', type: 'combobox',
    optionsFrom: loadServiceAssets,
    placeholder: '자산에서 선택하거나 직접 입력',
    hint: '자산관리의 서비스·애플리케이션 자산에서 고르거나 직접 입력합니다.' },
  { key: 'lifecycle', label: '개인정보 라이프사이클', placeholder: '예: 수집,보유,이용,제공,파기', hint: '해당 단계를 쉼표로 구분해 입력합니다.' },
  { key: 'workflow', label: '업무흐름도', type: 'textarea', span: 2, rows: 4, hint: '개인정보가 흐르는 경로를 단계별로 기술합니다.' },
  { key: 'status', label: '상태', type: 'select', default: 'ACTIVE',
    options: [{ value: 'ACTIVE', label: '운영중' }, { value: 'INACTIVE', label: '중단' }] },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'name', label: '처리업무명', strong: true },
  { key: 'department', label: '처리부서' },
  { key: 'systemName', label: '처리시스템' },
  { key: 'infoItems', label: '개인정보 항목', render: (r) => truncate(r.infoItems) },
  { key: 'retentionPeriod', label: '보유기간' },
  { key: 'lifecycle', label: '라이프사이클' },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { ACTIVE: 'badge-green', INACTIVE: 'badge-gray' } },
]

const filters = [
  { key: 'status', label: '상태', options: [{ value: 'ACTIVE', label: '운영중' }, { value: 'INACTIVE', label: '중단' }] },
]

function truncate(s, n = 30) {
  if (!s) return '—'
  return s.length > n ? s.slice(0, n) + '…' : s
}

function statsFn(items) {
  return [
    { label: '전체 처리업무', value: items.length },
    { label: '운영중', value: items.filter(i => i.status === 'ACTIVE').length, class: 'text-emerald-600' },
    { label: '중단', value: items.filter(i => i.status === 'INACTIVE').length, class: 'text-gray-400' },
  ]
}
</script>
