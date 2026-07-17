<template>
  <PrivacyCrudView
    title="개인정보 파일관리"
    description="개인정보파일대장 — 파일별 담당부서·DB 테이블·개인정보 항목·보유기간과 민감정보·고유식별정보 포함 여부를 관리합니다."
    :api="privacyFileApi"
    menu-key="privacy_files"
    :fields="fields"
    :columns="columns"
    :filters="filters"
    :search-keys="['name', 'department', 'systemName', 'dbTable', 'infoItems']"
    title-key="name"
    :stats-fn="statsFn"
  />
</template>

<script setup>
import PrivacyCrudView from '@/components/privacy/PrivacyCrudView.vue'
import { privacyFileApi } from '@/api'

const STATUS = { ACTIVE: '운영중', INACTIVE: '폐기' }

const fields = [
  { key: 'name', label: '개인정보파일명', required: true, placeholder: '예: 가입자정보, 배송정보, 본인인증정보, VOC, 마케팅' },
  { key: 'department', label: '담당부서' },
  { key: 'systemName', label: '운영 시스템' },
  { key: 'dbTable', label: 'DB 테이블', placeholder: '예: tb_subscriber' },
  { key: 'infoItems', label: '개인정보 항목', type: 'textarea', span: 2 },
  { key: 'retentionPeriod', label: '보유기간' },
  { key: 'recordCount', label: '보유 건수', type: 'number' },
  { key: 'sensitiveInfo', label: '민감정보 포함', type: 'checkbox', checkboxLabel: '민감정보를 포함합니다' },
  { key: 'uniqueIdentifier', label: '고유식별정보 포함', type: 'checkbox', checkboxLabel: '고유식별정보를 포함합니다' },
  { key: 'status', label: '상태', type: 'select', default: 'ACTIVE',
    options: [{ value: 'ACTIVE', label: '운영중' }, { value: 'INACTIVE', label: '폐기' }] },
  { key: 'notes', label: '비고', type: 'textarea', span: 2 },
]

const columns = [
  { key: 'name', label: '파일명', strong: true },
  { key: 'department', label: '담당부서' },
  { key: 'dbTable', label: 'DB 테이블' },
  { key: 'retentionPeriod', label: '보유기간' },
  { key: 'recordCount', label: '보유 건수', render: (r) => r.recordCount != null ? r.recordCount.toLocaleString() : '—' },
  { key: 'sensitiveInfo', label: '민감정보', type: 'bool' },
  { key: 'uniqueIdentifier', label: '고유식별정보', type: 'bool' },
  { key: 'status', label: '상태', type: 'badge', labels: STATUS,
    badges: { ACTIVE: 'badge-green', INACTIVE: 'badge-gray' } },
]

const filters = [
  { key: 'status', label: '상태', options: [{ value: 'ACTIVE', label: '운영중' }, { value: 'INACTIVE', label: '폐기' }] },
]

function statsFn(items) {
  return [
    { label: '전체 파일', value: items.length },
    { label: '민감정보 포함', value: items.filter(i => i.sensitiveInfo).length, class: 'text-orange-600' },
    { label: '고유식별정보 포함', value: items.filter(i => i.uniqueIdentifier).length, class: 'text-red-600' },
    { label: '보유 건수 합계', value: items.reduce((a, i) => a + (i.recordCount || 0), 0).toLocaleString() },
  ]
}
</script>
