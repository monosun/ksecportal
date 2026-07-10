<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-5xl max-h-[92vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-start justify-between gap-4 px-5 py-3 border-b shrink-0">
        <div v-if="asset" class="min-w-0">
          <h2 class="text-lg font-semibold text-gray-900 truncate">{{ asset.name }}</h2>
          <div class="flex flex-wrap gap-1.5 mt-1.5">
            <span v-if="asset.assetCategory" class="badge-gray">{{ assetCategoryLabel(asset.assetCategory) }}</span>
            <span class="badge-blue">{{ typeLabel(asset.type) }}</span>
            <span v-if="asset.cloudProvider && asset.cloudProvider !== 'ON_PREMISE'" :class="cloudBadge(asset.cloudProvider)">
              {{ $t(`asset.cloud_provider_label.${asset.cloudProvider}`) }}
            </span>
            <span v-if="asset.environment" :class="environmentBadge(asset.environment)">
              {{ $t(`asset.environment_label.${asset.environment}`) }}
            </span>
            <span :class="criticalityClass(asset.criticality)">{{ $t(`asset.criticality_label.${asset.criticality}`) }}</span>
            <span :class="statusBadge(asset.status)">{{ statusLabel(asset.status) }}</span>
          </div>
        </div>
        <div v-else class="text-lg font-semibold text-gray-900">{{ $t('asset.title') }}</div>
        <div class="flex items-center gap-2 shrink-0">
          <button v-if="isManager && asset" @click="$emit('edit', asset.id)" class="btn-secondary text-sm">
            {{ $t('common.edit') }}
          </button>
          <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Body -->
      <div class="px-5 py-4 overflow-y-auto flex-1">
        <div v-if="loading" class="py-16 text-center text-gray-400">{{ $t('common.loading') }}</div>
        <div v-else-if="asset" class="grid grid-cols-1 md:grid-cols-2 gap-x-5 gap-y-4">

          <!-- 식별 -->
          <section class="section md:col-span-2">
            <h3 class="section-title">식별</h3>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-x-5 gap-y-3">
              <Field label="자산 ID"><span class="font-mono text-xs">A-{{ String(asset.id).padStart(5, '0') }}</span></Field>
              <Field label="자산유형">{{ assetCategoryLabel(asset.assetCategory) || '-' }}</Field>
              <Field :label="$t('asset.type')">{{ typeLabel(asset.type) }}</Field>
              <Field label="등록일">{{ formatDate(asset.createdAt) }}</Field>
              <Field v-if="asset.sbomSoftwareId" label="SBOM 맵핑" class="col-span-2 md:col-span-4">
                <span class="inline-flex items-center gap-2 text-blue-700 font-medium">
                  {{ asset.sbomSoftwareName }} <span class="font-mono">{{ asset.sbomSoftwareVersion }}</span>
                  <span class="badge-blue">라이브러리 {{ asset.sbomComponentCount ?? 0 }}개</span>
                </span>
              </Field>
              <Field v-if="asset.description" :label="$t('asset.description')" class="col-span-2 md:col-span-4">
                <span class="text-gray-700 whitespace-pre-wrap">{{ asset.description }}</span>
              </Field>
            </div>
          </section>

          <!-- 책임 -->
          <section class="section">
            <h3 class="section-title">책임</h3>
            <div class="grid grid-cols-2 gap-x-5 gap-y-3">
              <Field :label="$t('asset.owner')">{{ asset.owner || '-' }}</Field>
              <Field :label="$t('asset.department')">{{ asset.department || '-' }}</Field>
              <Field label="운영담당자">{{ asset.operator || '-' }}</Field>
            </div>
          </section>

          <!-- 현황 -->
          <section class="section">
            <h3 class="section-title">현황</h3>
            <div class="grid grid-cols-2 gap-x-5 gap-y-3">
              <Field :label="$t('asset.environment')">{{ $t(`asset.environment_label.${asset.environment}`) }}</Field>
              <Field label="위치">{{ asset.location || '-' }}</Field>
              <Field label="상태"><span :class="statusBadge(asset.status)">{{ statusLabel(asset.status) }}</span></Field>
              <Field v-if="asset.ipAddress" :label="$t('asset.ipAddress')"><span class="font-mono">{{ asset.ipAddress }}</span></Field>
              <Field v-if="asset.osType" :label="$t('asset.osType')">{{ asset.osType }}</Field>
              <Field v-if="asset.spec" :label="$t('asset.spec')" class="col-span-2">{{ asset.spec }}</Field>
            </div>
          </section>

          <!-- 클라우드 정보 -->
          <section v-if="isCloudAsset" class="section">
            <h3 class="section-title">클라우드 정보</h3>
            <div class="grid grid-cols-2 gap-x-5 gap-y-3">
              <Field :label="$t('asset.cloudProvider')">{{ $t(`asset.cloud_provider_label.${asset.cloudProvider}`) }}</Field>
              <Field :label="$t('asset.region')"><span class="font-mono">{{ asset.region || '-' }}</span></Field>
              <Field v-if="asset.cloudResourceId" :label="$t('asset.cloudResourceId')" class="col-span-2">
                <span class="font-mono text-xs break-all">{{ asset.cloudResourceId }}</span>
              </Field>
            </div>
          </section>

          <!-- 평가 -->
          <section class="section">
            <h3 class="section-title">평가 (기밀·무결·가용)</h3>
            <div class="grid grid-cols-2 gap-x-5 gap-y-3">
              <Field label="기밀성 (C)"><span :class="criticalityClass(asset.confidentiality)">{{ $t(`asset.criticality_label.${asset.confidentiality}`) }}</span></Field>
              <Field label="무결성 (I)"><span :class="criticalityClass(asset.integrity)">{{ $t(`asset.criticality_label.${asset.integrity}`) }}</span></Field>
              <Field label="가용성 (A)"><span :class="criticalityClass(asset.availability)">{{ $t(`asset.criticality_label.${asset.availability}`) }}</span></Field>
              <Field :label="$t('asset.criticality') + ' (종합)'"><span :class="criticalityClass(asset.criticality)">{{ $t(`asset.criticality_label.${asset.criticality}`) }}</span></Field>
            </div>
          </section>

          <!-- 개인정보 -->
          <section class="section">
            <h3 class="section-title">개인정보</h3>
            <div class="grid grid-cols-2 gap-x-5 gap-y-3">
              <Field label="개인정보 포함"><span :class="asset.personalInfoIncluded ? 'badge-red' : 'badge-gray'">{{ asset.personalInfoIncluded ? 'O' : 'X' }}</span></Field>
              <Field label="개인정보 처리"><span :class="asset.personalInfoProcessing ? 'badge-red' : 'badge-gray'">{{ asset.personalInfoProcessing ? 'O' : 'X' }}</span></Field>
              <Field v-if="asset.personalInfoType" label="개인정보 유형" class="col-span-2">{{ asset.personalInfoType }}</Field>
            </div>
          </section>

          <!-- 연계 및 보안 관리 -->
          <section class="section">
            <h3 class="section-title">연계 및 보안 관리</h3>
            <div class="grid grid-cols-3 gap-x-5 gap-y-3">
              <Field v-if="asset.linkedSystems" label="연계 시스템" class="col-span-3">{{ asset.linkedSystems }}</Field>
              <Field label="접근권한 관리"><span :class="asset.accessControlTarget ? 'badge-blue' : 'badge-gray'">{{ asset.accessControlTarget ? 'O' : 'X' }}</span></Field>
              <Field label="백업 대상"><span :class="asset.backupTarget ? 'badge-blue' : 'badge-gray'">{{ asset.backupTarget ? 'O' : 'X' }}</span></Field>
              <Field label="로그 관리"><span :class="asset.logManagementTarget ? 'badge-blue' : 'badge-gray'">{{ asset.logManagementTarget ? 'O' : 'X' }}</span></Field>
            </div>
          </section>

          <!-- 비용 / 계약 -->
          <section class="section">
            <h3 class="section-title">비용 / 계약</h3>
            <div class="grid grid-cols-2 gap-x-5 gap-y-3">
              <Field :label="$t('asset.monthlyCost')">{{ asset.monthlyCost ? '₩' + Number(asset.monthlyCost).toLocaleString() : '-' }}</Field>
              <Field :label="$t('asset.contractExpiry')">
                <span :class="isExpiringSoon(asset.contractExpiry) ? 'text-red-600 font-medium' : ''">{{ asset.contractExpiry || '-' }}</span>
                <span v-if="isExpiringSoon(asset.contractExpiry)" class="ml-1 text-xs badge-red">만료 임박</span>
              </Field>
            </div>
          </section>

          <!-- 점검 / 검토 -->
          <section class="section">
            <h3 class="section-title">점검 / 검토</h3>
            <div class="grid grid-cols-2 gap-x-5 gap-y-3">
              <Field :label="$t('asset.lastInspectionDate')">{{ asset.lastInspectionDate || '-' }}</Field>
              <Field :label="$t('asset.nextInspectionDate')">
                <span :class="isOverdue(asset.nextInspectionDate) ? 'text-red-600 font-medium' : ''">{{ asset.nextInspectionDate || '-' }}</span>
                <span v-if="isOverdue(asset.nextInspectionDate)" class="ml-1 text-xs badge-red">점검 지연</span>
              </Field>
              <Field label="최종 검토일" class="col-span-2">{{ asset.lastReviewDate || '-' }}</Field>
            </div>
          </section>

          <!-- 비고 -->
          <section v-if="asset.remarks" class="section md:col-span-2">
            <h3 class="section-title">비고</h3>
            <p class="text-sm text-gray-700 whitespace-pre-wrap">{{ asset.remarks }}</p>
          </section>
        </div>
      </div>

      <!-- Footer -->
      <div class="flex justify-end px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, h } from 'vue'
import { assetApi, codeApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  open: { type: Boolean, default: false },
  assetId: { type: [Number, String], default: null }
})
defineEmits(['close', 'edit'])

const { t } = useI18n()
const auth = useAuthStore()
const isManager = auth.isManager

const asset = ref(null)
const loading = ref(false)
const assetTypes = ref([])

// 간단한 라벨/값 표시용 인라인 컴포넌트
const Field = (propsF, { slots }) =>
  h('div', {}, [
    h('p', { class: 'text-xs text-gray-500 mb-1' }, propsF.label),
    h('div', { class: 'text-sm font-medium text-gray-800' }, slots.default ? slots.default() : '-')
  ])
Field.props = ['label']

const cloudTypes = ['EC2', 'RDS', 'S3', 'ELB', 'LAMBDA', 'CLOUD_OTHER', 'CLOUD']
const isCloudAsset = computed(() => asset.value && (cloudTypes.includes(asset.value.type) || (asset.value.cloudProvider && asset.value.cloudProvider !== 'ON_PREMISE')))

const assetCategoryMap = {
  INFO: '정보(INFO)', SW: '소프트웨어(SW)', HW: '하드웨어(HW)',
  SERVICE: '서비스(SERVICE)', PERSONNEL: '인력(PERSONNEL)', FACILITY: '시설(FACILITY)'
}
function assetCategoryLabel(v) { return assetCategoryMap[v] || v || '' }

function typeLabel(value) {
  if (!value) return '-'
  const found = assetTypes.value.find(t => t.value === value)
  if (found) return found.label
  const key = `asset.type_label.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const statusMap = { OPERATIONAL: '운영중', SUSPENDED: '중지', DISPOSED: '폐기' }
function statusLabel(v) { return statusMap[v] || v || '-' }
function statusBadge(v) {
  return { OPERATIONAL: 'badge-success', SUSPENDED: 'badge-yellow', DISPOSED: 'badge-danger' }[v] || 'badge-gray'
}

watch(() => props.open, async (open) => {
  if (!open || !props.assetId) return
  loading.value = true
  asset.value = null
  if (!assetTypes.value.length) {
    try { assetTypes.value = (await codeApi.getValues('ASSET_TYPE')).data || [] } catch { assetTypes.value = [] }
  }
  try {
    asset.value = (await assetApi.get(props.assetId)).data
  } finally {
    loading.value = false
  }
})

function formatDate(dt) { return dt ? new Date(dt).toLocaleDateString('ko-KR') : '-' }
function criticalityClass(c) { return { HIGH: 'badge-red', MEDIUM: 'badge-yellow', LOW: 'badge-green' }[c] || 'badge-gray' }
function environmentBadge(e) { return { PRODUCTION: 'badge-red', STAGING: 'badge-yellow', DEVELOPMENT: 'badge-blue', TEST: 'badge-gray' }[e] || 'badge-gray' }
function cloudBadge(p) { return { AWS: 'badge-orange', GCP: 'badge-blue', AZURE: 'badge-blue' }[p] || 'badge-gray' }
function isExpiringSoon(date) {
  if (!date) return false
  const diff = (new Date(date) - new Date()) / (1000 * 60 * 60 * 24)
  return diff >= 0 && diff <= 30
}
function isOverdue(date) {
  if (!date) return false
  return new Date(date) < new Date()
}
</script>

<style scoped>
.section { @apply border border-gray-200 rounded-lg p-4; }
.section-title { @apply text-sm font-semibold text-gray-800 border-b pb-2 mb-3; }
</style>
