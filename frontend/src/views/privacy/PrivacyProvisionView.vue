<template>
  <PrivacyCrudView
    title="개인정보 제공관리"
    description="제3자 제공·공동이용·국외이전을 관리합니다. 처리위탁(수탁사 관리)과는 별도 항목입니다."
    :api="privacyProvisionApi"
    menu-key="privacy_provision"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['recipient', 'purpose', 'infoItems', 'country']"
    title-key="recipient"
    :stats-fn="statsFn"
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacyProvisionApi } from '@/api'

const TYPE = { THIRD_PARTY: '제3자 제공', JOINT_USE: '공동이용', OVERSEAS: '국외이전' }
const STATUS = { ACTIVE: '제공중', TERMINATED: '종료' }

const fields = [
  { key: 'provisionType', label: '제공 유형', type: 'select', default: 'THIRD_PARTY', required: true,
    options: [
      { value: 'THIRD_PARTY', label: '제3자 제공' },
      { value: 'JOINT_USE', label: '공동이용' },
      { value: 'OVERSEAS', label: '국외이전' },
    ] },
  { key: 'recipient', label: '제공받는 자', required: true },
  { key: 'country', label: '이전 국가', hint: '국외이전인 경우에만 입력합니다.' },
  { key: 'method', label: '제공 방법', placeholder: '예: API 연계, 파일 전송' },
  { key: 'infoItems', label: '제공 항목', type: 'textarea', span: 2 },
  { key: 'purpose', label: '제공목적', type: 'textarea', span: 2 },
  { key: 'legalBasis', label: '제공근거', placeholder: '예: 정보주체 동의 / 법령상 근거' },
  { key: 'retentionPeriod', label: '제공받는 자의 보유기간' },
  { key: 'provisionDate', label: '최초 제공일', type: 'date' },
  { key: 'status', label: '상태', type: 'select', default: 'ACTIVE',
    options: [{ value: 'ACTIVE', label: '제공중' }, { value: 'TERMINATED', label: '종료' }] },
  { key: 'contractStart', label: '계약 시작일', type: 'date' },
  { key: 'contractEnd', label: '계약 종료일', type: 'date' },
  { key: 'contractInfo', label: '제공 계약 정보', type: 'textarea', span: 2 },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'provisionType', label: '유형', type: 'badge', labels: TYPE,
    badges: { THIRD_PARTY: 'badge-blue', JOINT_USE: 'badge-yellow', OVERSEAS: 'badge-orange' } },
  { key: 'recipient', label: '제공받는 자', strong: true },
  { key: 'country', label: '국가' },
  { key: 'purpose', label: '제공목적', render: (r) => truncate(r.purpose) },
  { key: 'provisionDate', label: '최초 제공일' },
  { key: 'contractEnd', label: '계약 종료일' },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { ACTIVE: 'badge-green', TERMINATED: 'badge-gray' } },
]

const filters = [
  { key: 'provisionType', label: '유형', options: [
    { value: 'THIRD_PARTY', label: '제3자 제공' },
    { value: 'JOINT_USE', label: '공동이용' },
    { value: 'OVERSEAS', label: '국외이전' },
  ] },
  { key: 'status', label: '상태', options: [{ value: 'ACTIVE', label: '제공중' }, { value: 'TERMINATED', label: '종료' }] },
]

function truncate(s, n = 30) {
  if (!s) return '—'
  return s.length > n ? s.slice(0, n) + '…' : s
}

function statsFn(items) {
  return [
    { label: '전체', value: items.length },
    { label: '제3자 제공', value: items.filter(i => i.provisionType === 'THIRD_PARTY').length },
    { label: '공동이용', value: items.filter(i => i.provisionType === 'JOINT_USE').length },
    { label: '국외이전', value: items.filter(i => i.provisionType === 'OVERSEAS').length, class: 'text-orange-600' },
  ]
}
</script>
