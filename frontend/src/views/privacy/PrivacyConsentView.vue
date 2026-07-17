<template>
  <PrivacyCrudView
    title="개인정보 수집·이용 관리"
    description="수집동의서를 버전 단위로 관리합니다. 필수·선택 구분, 수집 항목, 이용목적, 처리근거를 함께 기록합니다."
    :api="privacyConsentApi"
    menu-key="privacy_consent"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['title', 'purpose', 'infoItems', 'channel']"
    title-key="title"
    :stats-fn="statsFn"
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacyConsentApi } from '@/api'

const TYPE = { REQUIRED: '필수', OPTIONAL: '선택' }
const STATUS = { DRAFT: '초안', ACTIVE: '시행중', ARCHIVED: '만료' }

const fields = [
  { key: 'title', label: '동의서명', required: true, placeholder: '예: 회원가입 개인정보 수집·이용 동의' },
  { key: 'version', label: '버전', default: '1.0', placeholder: '1.0' },
  { key: 'consentType', label: '필수/선택', type: 'select', default: 'REQUIRED',
    options: [{ value: 'REQUIRED', label: '필수' }, { value: 'OPTIONAL', label: '선택' }] },
  { key: 'channel', label: '수집 채널/화면', placeholder: '예: 웹 회원가입 페이지' },
  { key: 'infoItems', label: '수집 항목', type: 'textarea', span: 2 },
  { key: 'purpose', label: '이용목적', type: 'textarea', span: 2 },
  { key: 'legalBasis', label: '처리근거', placeholder: '예: 개인정보 보호법 제15조제1항제1호' },
  { key: 'retentionPeriod', label: '보유·이용기간' },
  { key: 'effectiveDate', label: '시행일', type: 'date' },
  { key: 'status', label: '상태', type: 'select', default: 'DRAFT',
    options: [{ value: 'DRAFT', label: '초안' }, { value: 'ACTIVE', label: '시행중' }, { value: 'ARCHIVED', label: '만료' }] },
  { key: 'content', label: '동의서 본문', type: 'textarea', span: 2, rows: 6 },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'title', label: '동의서명', strong: true },
  { key: 'version', label: '버전', render: (r) => 'v' + (r.version || '—') },
  { key: 'consentType', label: '구분', type: 'badge', labels: TYPE,
    badges: { REQUIRED: 'badge-blue', OPTIONAL: 'badge-gray' } },
  { key: 'channel', label: '수집 채널' },
  { key: 'retentionPeriod', label: '보유기간' },
  { key: 'effectiveDate', label: '시행일' },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { DRAFT: 'badge-gray', ACTIVE: 'badge-green', ARCHIVED: 'badge-yellow' } },
]

const filters = [
  { key: 'consentType', label: '구분', options: [{ value: 'REQUIRED', label: '필수' }, { value: 'OPTIONAL', label: '선택' }] },
  { key: 'status', label: '상태', options: [
    { value: 'DRAFT', label: '초안' }, { value: 'ACTIVE', label: '시행중' }, { value: 'ARCHIVED', label: '만료' }] },
]

function statsFn(items) {
  return [
    { label: '전체 동의서', value: items.length },
    { label: '시행중', value: items.filter(i => i.status === 'ACTIVE').length, class: 'text-emerald-600' },
    { label: '필수 동의', value: items.filter(i => i.consentType === 'REQUIRED').length },
    { label: '선택 동의', value: items.filter(i => i.consentType === 'OPTIONAL').length },
  ]
}
</script>
