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
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacyProcessingApi } from '@/api'

const STATUS = { ACTIVE: '운영중', INACTIVE: '중단' }

const fields = [
  { key: 'name', label: '처리업무명', required: true, placeholder: '예: 셀프개통, 고객센터, 배송, 정산, 마케팅' },
  { key: 'department', label: '처리부서' },
  { key: 'purpose', label: '처리목적', type: 'textarea', span: 2 },
  { key: 'infoItems', label: '개인정보 항목', type: 'textarea', span: 2, placeholder: '예: 성명, 휴대전화번호, 주소, 생년월일' },
  { key: 'retentionPeriod', label: '보유기간', placeholder: '예: 수집일로부터 3년' },
  { key: 'legalBasis', label: '처리근거', placeholder: '예: 정보주체 동의 / 계약 이행 / 법령상 의무' },
  { key: 'systemName', label: '처리시스템' },
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
