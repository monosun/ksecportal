<template>
  <PrivacyCrudView
    title="개인정보 보유기간 관리"
    description="보유기간 만료·삭제 예정을 추적합니다. 파기관리에서 파기가 완료되면 해당 항목은 자동으로 파기완료 처리됩니다."
    :api="privacyRetentionApi"
    menu-key="privacy_retention"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['targetName', 'department', 'infoItems']"
    title-key="targetName"
    :stats-fn="statsFn"
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacyRetentionApi } from '@/api'

const STATUS = { ACTIVE: '보유중', EXTENDED: '연장', DISPOSED: '파기완료' }

const fields = [
  { key: 'targetName', label: '관리 대상', required: true, placeholder: '예: 가입자정보 (개인정보파일명 또는 처리업무명)' },
  { key: 'department', label: '담당부서' },
  { key: 'infoItems', label: '개인정보 항목', type: 'textarea', span: 2 },
  { key: 'retentionPeriod', label: '보유기간', placeholder: '예: 수집일로부터 3년' },
  { key: 'legalBasis', label: '보유근거', placeholder: '예: 전자상거래법 제6조 (5년)' },
  { key: 'startDate', label: '보유 시작일', type: 'date' },
  { key: 'expiryDate', label: '만료예정일', type: 'date' },
  { key: 'disposalDueDate', label: '삭제(파기)예정일', type: 'date', hint: '만료 후 지체 없이 파기해야 합니다.' },
  { key: 'status', label: '상태', type: 'select', default: 'ACTIVE',
    options: [
      { value: 'ACTIVE', label: '보유중' },
      { value: 'EXTENDED', label: '연장' },
      { value: 'DISPOSED', label: '파기완료' },
    ] },
  { key: 'notifyEnabled', label: '자동알림', type: 'checkbox', default: true, checkboxLabel: '만료 예정 시 알림을 받습니다' },
  { key: 'notifyDaysBefore', label: '알림 시점(일 전)', type: 'number', default: 30 },
  { key: 'extensionReason', label: '연장사유', type: 'textarea', span: 2, hint: '보유기간을 연장한 경우 근거를 기록합니다.' },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'targetName', label: '관리 대상', strong: true },
  { key: 'department', label: '담당부서' },
  { key: 'retentionPeriod', label: '보유기간' },
  { key: 'expiryDate', label: '만료예정일' },
  { key: 'daysUntilExpiry', label: '만료까지', render: (r) => expiryText(r) },
  { key: 'disposalDueDate', label: '파기예정일' },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { ACTIVE: 'badge-green', EXTENDED: 'badge-yellow', DISPOSED: 'badge-gray' } },
]

const filters = [
  { key: 'status', label: '상태', options: [
    { value: 'ACTIVE', label: '보유중' },
    { value: 'EXTENDED', label: '연장' },
    { value: 'DISPOSED', label: '파기완료' },
  ] },
]

function expiryText(r) {
  if (r.status === 'DISPOSED') return '—'
  if (r.daysUntilExpiry == null) return '—'
  if (r.daysUntilExpiry < 0) return `${Math.abs(r.daysUntilExpiry)}일 경과`
  if (r.daysUntilExpiry === 0) return '오늘'
  return `${r.daysUntilExpiry}일 남음`
}

function statsFn(items) {
  const live = items.filter(i => i.status !== 'DISPOSED')
  return [
    { label: '전체', value: items.length },
    { label: '30일 내 만료예정',
      value: live.filter(i => i.daysUntilExpiry != null && i.daysUntilExpiry >= 0 && i.daysUntilExpiry <= 30).length,
      class: 'text-yellow-600' },
    { label: '만료 경과·미파기',
      value: live.filter(i => i.daysUntilExpiry != null && i.daysUntilExpiry < 0).length,
      class: 'text-red-600' },
    { label: '파기완료', value: items.filter(i => i.status === 'DISPOSED').length, class: 'text-gray-400' },
  ]
}
</script>
