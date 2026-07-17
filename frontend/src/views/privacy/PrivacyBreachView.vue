<template>
  <PrivacyCrudView
    title="개인정보 유출관리"
    description="유출사고 인지·신고기한·통지·조사결과·재발방지를 관리합니다. 보안 인시던트와는 별도로 운영합니다."
    :api="privacyBreachApi"
    menu-key="privacy_breach"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['title', 'cause', 'infoItems', 'authorityName']"
    title-key="title"
    :stats-fn="statsFn"
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacyBreachApi } from '@/api'

const STATUS = { DETECTED: '인지', INVESTIGATING: '조사중', NOTIFIED: '통지·신고완료', CLOSED: '종결' }

const fields = [
  { key: 'title', label: '사고명', required: true },
  { key: 'status', label: '진행 상태', type: 'select', default: 'DETECTED',
    options: [
      { value: 'DETECTED', label: '인지' },
      { value: 'INVESTIGATING', label: '조사중' },
      { value: 'NOTIFIED', label: '통지·신고완료' },
      { value: 'CLOSED', label: '종결' },
    ] },
  { key: 'occurredAt', label: '유출 발생일시', type: 'datetime' },
  { key: 'detectedAt', label: '인지일시', type: 'datetime',
    hint: '신고기한(72시간)의 기산점입니다. 비워두면 기한이 계산되지 않습니다.' },
  { key: 'reportDueAt', label: '신고기한', type: 'datetime',
    hint: '비워두면 인지일시 + 72시간으로 자동 계산됩니다.' },
  { key: 'affectedCount', label: '유출 정보주체 수', type: 'number' },
  { key: 'infoItems', label: '유출 개인정보 항목', type: 'textarea', span: 2 },
  { key: 'cause', label: '유출 경로·원인', type: 'textarea', span: 2, rows: 4 },
  { key: 'subjectNotified', label: '정보주체 통지', type: 'checkbox', checkboxLabel: '통지를 완료했습니다' },
  { key: 'notifiedDate', label: '통지일', type: 'date' },
  { key: 'notifyMethod', label: '통지 방법', placeholder: '예: 이메일, SMS, 홈페이지 게시' },
  { key: 'authorityReported', label: '관계기관 신고', type: 'checkbox', checkboxLabel: '신고를 완료했습니다' },
  { key: 'authorityName', label: '신고 기관', placeholder: '예: 개인정보보호위원회, KISA' },
  { key: 'reportedDate', label: '신고일', type: 'date' },
  { key: 'investigation', label: '조사결과', type: 'textarea', span: 2, rows: 4 },
  { key: 'preventionPlan', label: '재발방지 대책', type: 'textarea', span: 2, rows: 4 },
  { key: 'closedDate', label: '종결일', type: 'date' },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'title', label: '사고명', strong: true },
  { key: 'detectedAt', label: '인지일시', render: (r) => fmtDateTime(r.detectedAt) },
  { key: 'reportDueAt', label: '신고기한', render: (r) => reportText(r) },
  { key: 'affectedCount', label: '유출 규모', render: (r) => r.affectedCount != null ? r.affectedCount.toLocaleString() + '명' : '—' },
  { key: 'subjectNotified', label: '통지', type: 'bool' },
  { key: 'authorityReported', label: '신고', type: 'bool' },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { DETECTED: 'badge-red', INVESTIGATING: 'badge-yellow', NOTIFIED: 'badge-blue', CLOSED: 'badge-gray' } },
]

const filters = [
  { key: 'status', label: '상태', options: [
    { value: 'DETECTED', label: '인지' },
    { value: 'INVESTIGATING', label: '조사중' },
    { value: 'NOTIFIED', label: '통지·신고완료' },
    { value: 'CLOSED', label: '종결' },
  ] },
]

function fmtDateTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 16)
}

function reportText(r) {
  if (!r.reportDueAt) return '—'
  const base = fmtDateTime(r.reportDueAt)
  if (r.authorityReported) return base
  if (r.reportOverdue) return `${base} (기한 경과)`
  if (r.hoursUntilReportDue != null) return `${base} (${r.hoursUntilReportDue}시간 남음)`
  return base
}

function statsFn(items) {
  return [
    { label: '전체 사고', value: items.length },
    { label: '미종결', value: items.filter(i => i.status !== 'CLOSED').length, class: 'text-red-600' },
    { label: '신고기한 경과', value: items.filter(i => i.reportOverdue).length, class: 'text-red-600' },
    { label: '유출 정보주체 합계', value: items.reduce((a, i) => a + (i.affectedCount || 0), 0).toLocaleString() },
  ]
}
</script>
