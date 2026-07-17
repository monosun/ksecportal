<template>
  <PrivacyCrudView
    title="개인정보 보호조치 관리"
    description="접근권한 점검·권한회수·암호화 적용현황·접속기록 점검·출력물·반출·휴면계정 관리현황을 기록합니다."
    :api="privacySafeguardApi"
    menu-key="privacy_safeguard"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['title', 'targetSystem', 'department', 'performer']"
    title-key="title"
    :stats-fn="statsFn"
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacySafeguardApi } from '@/api'

const TYPE = {
  ACCESS_REVIEW: '접근권한 점검',
  ACCESS_REVOKE: '권한회수',
  ENCRYPTION: '암호화 적용현황',
  ACCESS_LOG_REVIEW: '접속기록 점검',
  PRINTOUT: '출력물 관리',
  EXPORT: '반출관리',
  DORMANT_ACCOUNT: '휴면계정 관리',
}
const STATUS = { PLANNED: '계획', IN_PROGRESS: '진행중', COMPLETED: '완료' }

const typeOptions = Object.entries(TYPE).map(([value, label]) => ({ value, label }))

const fields = [
  { key: 'safeguardType', label: '보호조치 유형', type: 'select', default: 'ACCESS_REVIEW', required: true,
    options: typeOptions },
  { key: 'title', label: '조치·점검명', required: true, placeholder: '예: 2026년 3분기 개인정보처리시스템 접근권한 점검' },
  { key: 'targetSystem', label: '대상 시스템' },
  { key: 'department', label: '담당부서' },
  { key: 'checkDate', label: '점검(조치)일', type: 'date' },
  { key: 'performer', label: '수행자' },
  { key: 'targetCount', label: '점검 대상 건수', type: 'number' },
  { key: 'actionCount', label: '조치 건수', type: 'number', hint: '권한회수·휴면전환 등 실제 조치한 건수입니다.' },
  { key: 'status', label: '진행 상태', type: 'select', default: 'PLANNED',
    options: [
      { value: 'PLANNED', label: '계획' },
      { value: 'IN_PROGRESS', label: '진행중' },
      { value: 'COMPLETED', label: '완료' },
    ] },
  { key: 'nextCheckDate', label: '차기 점검 예정일', type: 'date' },
  { key: 'result', label: '점검·조치 결과', type: 'textarea', span: 2, rows: 4 },
  { key: 'findings', label: '발견된 문제점', type: 'textarea', span: 2, rows: 3 },
  { key: 'improvement', label: '개선조치 내용', type: 'textarea', span: 2, rows: 3 },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'safeguardType', label: '유형', type: 'badge', labels: TYPE,
    badges: {
      ACCESS_REVIEW: 'badge-blue', ACCESS_REVOKE: 'badge-orange', ENCRYPTION: 'badge-green',
      ACCESS_LOG_REVIEW: 'badge-blue', PRINTOUT: 'badge-gray', EXPORT: 'badge-yellow',
      DORMANT_ACCOUNT: 'badge-gray',
    } },
  { key: 'title', label: '조치·점검명', strong: true },
  { key: 'targetSystem', label: '대상 시스템' },
  { key: 'checkDate', label: '점검일' },
  { key: 'performer', label: '수행자' },
  { key: 'targetCount', label: '대상/조치',
    render: (r) => `${r.targetCount ?? '—'} / ${r.actionCount ?? '—'}` },
  { key: 'nextCheckDate', label: '차기 점검' },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { PLANNED: 'badge-gray', IN_PROGRESS: 'badge-blue', COMPLETED: 'badge-green' } },
]

const filters = [
  { key: 'safeguardType', label: '유형', options: typeOptions },
  { key: 'status', label: '상태', options: [
    { value: 'PLANNED', label: '계획' },
    { value: 'IN_PROGRESS', label: '진행중' },
    { value: 'COMPLETED', label: '완료' },
  ] },
]

function statsFn(items) {
  return [
    { label: '전체 조치', value: items.length },
    { label: '진행중', value: items.filter(i => i.status === 'IN_PROGRESS').length, class: 'text-blue-600' },
    { label: '완료', value: items.filter(i => i.status === 'COMPLETED').length, class: 'text-emerald-600' },
    { label: '조치 건수 합계', value: items.reduce((a, i) => a + (i.actionCount || 0), 0).toLocaleString() },
  ]
}
</script>
