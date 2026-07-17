<template>
  <PrivacyCrudView
    title="개인정보 영향평가(DPIA)"
    description="영향평가 대상 선정부터 체크리스트·위험도·개선계획·완료보고까지 관리합니다. 공공기관 PIA 및 민간 자체 영향평가에 사용합니다."
    :api="privacyDpiaApi"
    menu-key="privacy_dpia"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['title', 'targetSystem', 'department', 'assessor']"
    title-key="title"
    :stats-fn="statsFn"
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacyDpiaApi } from '@/api'

const RISK = { HIGH: '높음', MEDIUM: '보통', LOW: '낮음' }
const STATUS = { PLANNED: '대상선정', IN_PROGRESS: '평가중', IMPROVING: '개선중', COMPLETED: '완료' }

const fields = [
  { key: 'title', label: '영향평가명', required: true },
  { key: 'targetSystem', label: '평가 대상 시스템' },
  { key: 'department', label: '담당부서' },
  { key: 'assessor', label: '평가자(수행기관)' },
  { key: 'infoItems', label: '대상 개인정보 항목', type: 'textarea', span: 2 },
  { key: 'subjectCount', label: '대상 정보주체 수', type: 'number' },
  { key: 'assessmentDate', label: '평가일', type: 'date' },
  { key: 'riskLevel', label: '종합 위험도', type: 'select', default: 'MEDIUM',
    options: [{ value: 'HIGH', label: '높음' }, { value: 'MEDIUM', label: '보통' }, { value: 'LOW', label: '낮음' }] },
  { key: 'status', label: '진행 상태', type: 'select', default: 'PLANNED',
    options: [
      { value: 'PLANNED', label: '대상선정' },
      { value: 'IN_PROGRESS', label: '평가중' },
      { value: 'IMPROVING', label: '개선중' },
      { value: 'COMPLETED', label: '완료' },
    ] },
  { key: 'checklist', label: '체크리스트 결과', type: 'textarea', span: 2, rows: 5,
    hint: '평가 항목별 점검 결과를 기록합니다.' },
  { key: 'improvementPlan', label: '개선계획', type: 'textarea', span: 2, rows: 4 },
  { key: 'improvementDueDate', label: '개선 완료 예정일', type: 'date' },
  { key: 'completedDate', label: '완료일', type: 'date' },
  { key: 'completionReport', label: '완료보고', type: 'textarea', span: 2, rows: 4 },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'title', label: '영향평가명', strong: true },
  { key: 'targetSystem', label: '대상 시스템' },
  { key: 'assessor', label: '평가자' },
  { key: 'assessmentDate', label: '평가일' },
  { key: 'subjectCount', label: '정보주체 수', render: (r) => r.subjectCount != null ? r.subjectCount.toLocaleString() : '—' },
  { key: 'riskLevel', label: '위험도', type: 'badge', labels: RISK,
    badges: { HIGH: 'badge-red', MEDIUM: 'badge-yellow', LOW: 'badge-green' } },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { PLANNED: 'badge-gray', IN_PROGRESS: 'badge-blue', IMPROVING: 'badge-yellow', COMPLETED: 'badge-green' } },
]

const filters = [
  { key: 'riskLevel', label: '위험도', options: [
    { value: 'HIGH', label: '높음' }, { value: 'MEDIUM', label: '보통' }, { value: 'LOW', label: '낮음' }] },
  { key: 'status', label: '상태', options: [
    { value: 'PLANNED', label: '대상선정' },
    { value: 'IN_PROGRESS', label: '평가중' },
    { value: 'IMPROVING', label: '개선중' },
    { value: 'COMPLETED', label: '완료' },
  ] },
]

function statsFn(items) {
  return [
    { label: '전체 평가', value: items.length },
    { label: '위험도 높음', value: items.filter(i => i.riskLevel === 'HIGH').length, class: 'text-red-600' },
    { label: '진행중', value: items.filter(i => i.status === 'IN_PROGRESS' || i.status === 'IMPROVING').length, class: 'text-blue-600' },
    { label: '완료', value: items.filter(i => i.status === 'COMPLETED').length, class: 'text-emerald-600' },
  ]
}
</script>
