<template>
  <PrivacyCrudView
    title="개인정보 파기관리"
    description="파기계획·승인·이력·증적을 관리합니다. 상태를 '파기완료'로 저장하면 연계된 보유기간 항목도 파기완료로 전이됩니다."
    :api="privacyDisposalApi"
    menu-key="privacy_disposal"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['title', 'targetName', 'department', 'method']"
    title-key="title"
    :stats-fn="statsFn"
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacyDisposalApi } from '@/api'

const STATUS = { PLANNED: '계획', IN_PROGRESS: '진행중', COMPLETED: '파기완료' }
const APPROVAL = { PENDING: '승인대기', APPROVED: '승인', REJECTED: '반려' }

const fields = [
  { key: 'title', label: '파기계획명', required: true, placeholder: '예: 2026년 상반기 만료 가입자정보 파기' },
  { key: 'targetName', label: '파기대상', required: true },
  { key: 'retentionId', label: '연계 보유기간 항목 ID', type: 'number',
    hint: '보유기간 관리 항목의 ID를 입력하면 파기완료 시 해당 항목이 자동으로 파기완료 처리됩니다.' },
  { key: 'department', label: '담당부서' },
  { key: 'infoItems', label: '파기 개인정보 항목', type: 'textarea', span: 2 },
  { key: 'recordCount', label: '파기 건수', type: 'number' },
  { key: 'method', label: '파기방법', placeholder: '예: DB 레코드 영구삭제 / 문서 파쇄 / 소각' },
  { key: 'plannedDate', label: '파기예정일', type: 'date' },
  { key: 'disposalDate', label: '실제 파기일', type: 'date' },
  { key: 'performer', label: '파기 수행자' },
  { key: 'approver', label: '파기 승인자' },
  { key: 'approvalStatus', label: '승인 상태', type: 'select', default: 'PENDING',
    options: [
      { value: 'PENDING', label: '승인대기' },
      { value: 'APPROVED', label: '승인' },
      { value: 'REJECTED', label: '반려' },
    ] },
  { key: 'approvedDate', label: '승인일', type: 'date' },
  { key: 'status', label: '진행 상태', type: 'select', default: 'PLANNED',
    options: [
      { value: 'PLANNED', label: '계획' },
      { value: 'IN_PROGRESS', label: '진행중' },
      { value: 'COMPLETED', label: '파기완료' },
    ] },
  { key: 'evidence', label: '파기증적', type: 'textarea', span: 2, rows: 4,
    hint: '파기 결과 캡처·로그·확인서 등 증적의 위치와 내용을 기록합니다.' },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'title', label: '파기계획명', strong: true },
  { key: 'targetName', label: '파기대상' },
  { key: 'method', label: '파기방법' },
  { key: 'recordCount', label: '건수', render: (r) => r.recordCount != null ? r.recordCount.toLocaleString() : '—' },
  { key: 'plannedDate', label: '파기예정일' },
  { key: 'disposalDate', label: '실제 파기일' },
  { key: 'approvalStatus', label: '승인', type: 'badge', labels: APPROVAL,
    badges: { PENDING: 'badge-yellow', APPROVED: 'badge-green', REJECTED: 'badge-red' } },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { PLANNED: 'badge-gray', IN_PROGRESS: 'badge-blue', COMPLETED: 'badge-green' } },
]

const filters = [
  { key: 'status', label: '상태', options: [
    { value: 'PLANNED', label: '계획' },
    { value: 'IN_PROGRESS', label: '진행중' },
    { value: 'COMPLETED', label: '파기완료' },
  ] },
  { key: 'approvalStatus', label: '승인', options: [
    { value: 'PENDING', label: '승인대기' },
    { value: 'APPROVED', label: '승인' },
    { value: 'REJECTED', label: '반려' },
  ] },
]

function statsFn(items) {
  return [
    { label: '전체 파기계획', value: items.length },
    { label: '승인대기', value: items.filter(i => i.approvalStatus === 'PENDING').length, class: 'text-yellow-600' },
    { label: '진행중', value: items.filter(i => i.status === 'IN_PROGRESS').length, class: 'text-blue-600' },
    { label: '파기완료', value: items.filter(i => i.status === 'COMPLETED').length, class: 'text-emerald-600' },
  ]
}
</script>
