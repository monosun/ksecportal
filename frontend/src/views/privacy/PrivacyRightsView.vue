<template>
  <PrivacyCrudView
    title="정보주체 권리행사 관리"
    description="열람·정정·삭제·처리정지·동의철회 요청을 접수부터 처리결과까지 관리합니다. 처리기한(요청일 + 10일)을 자동 산정합니다."
    :api="privacyRightsApi"
    menu-key="privacy_rights"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['subjectName', 'contact', 'content', 'handler']"
    title-key="subjectName"
    :stats-fn="statsFn"
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacyRightsApi } from '@/api'

const TYPE = {
  ACCESS: '열람', CORRECTION: '정정', DELETION: '삭제',
  SUSPENSION: '처리정지', CONSENT_WITHDRAWAL: '동의철회',
}
const STATUS = { RECEIVED: '접수', IN_PROGRESS: '진행중', COMPLETED: '완료', REJECTED: '거절' }

const fields = [
  { key: 'requestType', label: '요청 유형', type: 'select', default: 'ACCESS', required: true,
    options: [
      { value: 'ACCESS', label: '열람' },
      { value: 'CORRECTION', label: '정정' },
      { value: 'DELETION', label: '삭제' },
      { value: 'SUSPENSION', label: '처리정지' },
      { value: 'CONSENT_WITHDRAWAL', label: '동의철회' },
    ] },
  { key: 'subjectName', label: '정보주체명', required: true },
  { key: 'contact', label: '연락처' },
  { key: 'channel', label: '접수 채널', placeholder: '예: 고객센터, 이메일, 웹' },
  { key: 'content', label: '요청 내용', type: 'textarea', span: 2 },
  { key: 'requestDate', label: '요청일(접수일)', type: 'date', required: true },
  { key: 'dueDate', label: '처리기한', type: 'date', hint: '비워두면 요청일 + 10일로 자동 산정됩니다.' },
  { key: 'status', label: '처리 상태', type: 'select', default: 'RECEIVED',
    options: [
      { value: 'RECEIVED', label: '접수' },
      { value: 'IN_PROGRESS', label: '진행중' },
      { value: 'COMPLETED', label: '완료' },
      { value: 'REJECTED', label: '거절' },
    ] },
  { key: 'handler', label: '처리 담당자' },
  { key: 'completedDate', label: '처리 완료일', type: 'date', hint: '완료·거절로 저장하면 비어 있을 때 오늘 날짜가 채워집니다.' },
  { key: 'result', label: '처리결과', type: 'textarea', span: 2, rows: 4 },
  { key: 'rejectReason', label: '거절 사유', type: 'textarea', span: 2, hint: '거절한 경우 법적 근거를 함께 기록합니다.' },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'requestDate', label: '요청일' },
  { key: 'requestType', label: '유형', type: 'badge', labels: TYPE,
    badges: {
      ACCESS: 'badge-blue', CORRECTION: 'badge-yellow', DELETION: 'badge-red',
      SUSPENSION: 'badge-orange', CONSENT_WITHDRAWAL: 'badge-gray',
    } },
  { key: 'subjectName', label: '정보주체', strong: true },
  { key: 'channel', label: '접수 채널' },
  { key: 'dueDate', label: '처리기한', render: (r) => dueText(r) },
  { key: 'handler', label: '담당자' },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { RECEIVED: 'badge-yellow', IN_PROGRESS: 'badge-blue', COMPLETED: 'badge-green', REJECTED: 'badge-gray' } },
]

const filters = [
  { key: 'requestType', label: '유형', options: [
    { value: 'ACCESS', label: '열람' },
    { value: 'CORRECTION', label: '정정' },
    { value: 'DELETION', label: '삭제' },
    { value: 'SUSPENSION', label: '처리정지' },
    { value: 'CONSENT_WITHDRAWAL', label: '동의철회' },
  ] },
  { key: 'status', label: '상태', options: [
    { value: 'RECEIVED', label: '접수' },
    { value: 'IN_PROGRESS', label: '진행중' },
    { value: 'COMPLETED', label: '완료' },
    { value: 'REJECTED', label: '거절' },
  ] },
]

function dueText(r) {
  if (!r.dueDate) return '—'
  if (r.slaBreached) return `${r.dueDate} (기한 초과)`
  if (r.daysUntilDue != null) return `${r.dueDate} (${r.daysUntilDue}일 남음)`
  return r.dueDate
}

function statsFn(items) {
  return [
    { label: '전체 요청', value: items.length },
    { label: '처리중', value: items.filter(i => i.status === 'RECEIVED' || i.status === 'IN_PROGRESS').length, class: 'text-blue-600' },
    { label: '기한 초과', value: items.filter(i => i.slaBreached).length, class: 'text-red-600' },
    { label: '완료', value: items.filter(i => i.status === 'COMPLETED').length, class: 'text-emerald-600' },
  ]
}
</script>
