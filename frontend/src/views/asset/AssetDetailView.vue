<template>
  <div class="p-8 max-w-3xl mx-auto">
    <div v-if="loading" class="text-center py-16 text-gray-400">{{ $t('common.loading') }}</div>
    <template v-else-if="asset">
      <RouterLink to="/assets" class="text-sm text-primary-600 hover:underline mb-4 inline-block">← {{ $t('common.back') }}</RouterLink>

      <div class="flex items-start justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">{{ asset.name }}</h1>
          <div class="flex flex-wrap gap-2 mt-2">
            <span v-if="asset.assetCategory" class="badge-gray">{{ assetCategoryLabel(asset.assetCategory) }}</span>
            <span class="badge-blue">{{ $t(`asset.type_label.${asset.type}`) }}</span>
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
        <RouterLink v-if="isManager" :to="`/assets/${asset.id}/edit`" class="btn-secondary">{{ $t('common.edit') }}</RouterLink>
      </div>

      <!-- 식별 -->
      <div class="card mb-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">식별</h2>
        <div class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <p class="text-gray-500 mb-1">자산 ID</p>
            <p class="font-medium font-mono text-xs">A-{{ String(asset.id).padStart(5, '0') }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">자산유형</p>
            <p class="font-medium">{{ assetCategoryLabel(asset.assetCategory) || '-' }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.type') }}</p>
            <p class="font-medium">{{ $t(`asset.type_label.${asset.type}`) }}</p>
          </div>
          <div v-if="asset.sbomSoftwareId" class="col-span-2">
            <p class="text-gray-500 mb-1">SBOM 맵핑</p>
            <RouterLink to="/sbom" class="inline-flex items-center gap-2 text-blue-600 hover:underline font-medium">
              {{ asset.sbomSoftwareName }} <span class="font-mono">{{ asset.sbomSoftwareVersion }}</span>
              <span class="badge-blue">라이브러리 {{ asset.sbomComponentCount ?? 0 }}개</span>
            </RouterLink>
          </div>
          <div v-if="asset.description" class="col-span-2">
            <p class="text-gray-500 mb-1">{{ $t('asset.description') }}</p>
            <p class="text-gray-700 whitespace-pre-wrap">{{ asset.description }}</p>
          </div>
        </div>
      </div>

      <!-- 책임 -->
      <div class="card mb-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">책임</h2>
        <div class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.owner') }}</p>
            <p class="font-medium">{{ asset.owner || '-' }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.department') }}</p>
            <p class="font-medium">{{ asset.department || '-' }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">운영담당자</p>
            <p class="font-medium">{{ asset.operator || '-' }}</p>
          </div>
        </div>
      </div>

      <!-- 현황 -->
      <div class="card mb-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">현황</h2>
        <div class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.environment') }}</p>
            <p class="font-medium">{{ $t(`asset.environment_label.${asset.environment}`) }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">위치</p>
            <p class="font-medium">{{ asset.location || '-' }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">상태</p>
            <span :class="statusBadge(asset.status)">{{ statusLabel(asset.status) }}</span>
          </div>
          <div v-if="asset.ipAddress">
            <p class="text-gray-500 mb-1">{{ $t('asset.ipAddress') }}</p>
            <p class="font-medium font-mono">{{ asset.ipAddress }}</p>
          </div>
          <div v-if="asset.osType">
            <p class="text-gray-500 mb-1">{{ $t('asset.osType') }}</p>
            <p class="font-medium">{{ asset.osType }}</p>
          </div>
          <div v-if="asset.spec" class="col-span-2">
            <p class="text-gray-500 mb-1">{{ $t('asset.spec') }}</p>
            <p class="font-medium text-gray-700">{{ asset.spec }}</p>
          </div>
        </div>
      </div>

      <!-- 클라우드 정보 -->
      <div v-if="isCloudAsset" class="card mb-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">클라우드 정보</h2>
        <div class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.cloudProvider') }}</p>
            <p class="font-medium">{{ $t(`asset.cloud_provider_label.${asset.cloudProvider}`) }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.region') }}</p>
            <p class="font-medium font-mono">{{ asset.region || '-' }}</p>
          </div>
          <div v-if="asset.cloudResourceId" class="col-span-2">
            <p class="text-gray-500 mb-1">{{ $t('asset.cloudResourceId') }}</p>
            <p class="font-medium font-mono text-xs break-all">{{ asset.cloudResourceId }}</p>
          </div>
        </div>
      </div>

      <!-- 평가 -->
      <div class="card mb-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">평가</h2>
        <div class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <p class="text-gray-500 mb-1">기밀성 (C)</p>
            <span :class="criticalityClass(asset.confidentiality)">{{ $t(`asset.criticality_label.${asset.confidentiality}`) }}</span>
          </div>
          <div>
            <p class="text-gray-500 mb-1">무결성 (I)</p>
            <span :class="criticalityClass(asset.integrity)">{{ $t(`asset.criticality_label.${asset.integrity}`) }}</span>
          </div>
          <div>
            <p class="text-gray-500 mb-1">가용성 (A)</p>
            <span :class="criticalityClass(asset.availability)">{{ $t(`asset.criticality_label.${asset.availability}`) }}</span>
          </div>
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.criticality') }} (종합)</p>
            <span :class="criticalityClass(asset.criticality)">{{ $t(`asset.criticality_label.${asset.criticality}`) }}</span>
          </div>
        </div>
      </div>

      <!-- 개인정보 -->
      <div class="card mb-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">개인정보</h2>
        <div class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <p class="text-gray-500 mb-1">개인정보 포함 여부</p>
            <span :class="asset.personalInfoIncluded ? 'badge-red' : 'badge-gray'">
              {{ asset.personalInfoIncluded ? 'O' : 'X' }}
            </span>
          </div>
          <div>
            <p class="text-gray-500 mb-1">개인정보 처리 여부</p>
            <span :class="asset.personalInfoProcessing ? 'badge-red' : 'badge-gray'">
              {{ asset.personalInfoProcessing ? 'O' : 'X' }}
            </span>
          </div>
          <div v-if="asset.personalInfoType" class="col-span-2">
            <p class="text-gray-500 mb-1">개인정보 유형</p>
            <p class="font-medium">{{ asset.personalInfoType }}</p>
          </div>
        </div>
      </div>

      <!-- 연계 및 보안 관리 -->
      <div class="card mb-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">연계 및 보안 관리</h2>
        <div class="grid grid-cols-2 gap-5 text-sm">
          <div v-if="asset.linkedSystems" class="col-span-2">
            <p class="text-gray-500 mb-1">연계 시스템</p>
            <p class="font-medium">{{ asset.linkedSystems }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">접근권한 관리 대상</p>
            <span :class="asset.accessControlTarget ? 'badge-blue' : 'badge-gray'">
              {{ asset.accessControlTarget ? 'O' : 'X' }}
            </span>
          </div>
          <div>
            <p class="text-gray-500 mb-1">백업 대상</p>
            <span :class="asset.backupTarget ? 'badge-blue' : 'badge-gray'">
              {{ asset.backupTarget ? 'O' : 'X' }}
            </span>
          </div>
          <div>
            <p class="text-gray-500 mb-1">로그 관리 대상</p>
            <span :class="asset.logManagementTarget ? 'badge-blue' : 'badge-gray'">
              {{ asset.logManagementTarget ? 'O' : 'X' }}
            </span>
          </div>
        </div>
      </div>

      <!-- 비용 / 계약 -->
      <div class="card mb-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">비용 / 계약</h2>
        <div class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.monthlyCost') }}</p>
            <p class="font-medium">{{ asset.monthlyCost ? '₩' + Number(asset.monthlyCost).toLocaleString() : '-' }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.contractExpiry') }}</p>
            <p class="font-medium" :class="isExpiringSoon(asset.contractExpiry) ? 'text-red-600' : ''">
              {{ asset.contractExpiry || '-' }}
              <span v-if="isExpiringSoon(asset.contractExpiry)" class="ml-1 text-xs badge-red">만료 임박</span>
            </p>
          </div>
        </div>
      </div>

      <!-- 점검 / 검토 -->
      <div class="card mb-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">점검 / 검토</h2>
        <div class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.lastInspectionDate') }}</p>
            <p class="font-medium">{{ asset.lastInspectionDate || '-' }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">{{ $t('asset.nextInspectionDate') }}</p>
            <p class="font-medium" :class="isOverdue(asset.nextInspectionDate) ? 'text-red-600' : ''">
              {{ asset.nextInspectionDate || '-' }}
              <span v-if="isOverdue(asset.nextInspectionDate)" class="ml-1 text-xs badge-red">점검 지연</span>
            </p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">최종 검토일</p>
            <p class="font-medium">{{ asset.lastReviewDate || '-' }}</p>
          </div>
          <div>
            <p class="text-gray-500 mb-1">등록일</p>
            <p class="font-medium">{{ formatDate(asset.createdAt) }}</p>
          </div>
        </div>
      </div>

      <!-- 비고 -->
      <div v-if="asset.remarks" class="card">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">비고</h2>
        <p class="text-sm text-gray-700 whitespace-pre-wrap">{{ asset.remarks }}</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { assetApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const isManager = auth.isManager

const asset = ref(null)
const loading = ref(true)

const cloudTypes = ['EC2', 'RDS', 'S3', 'ELB', 'LAMBDA', 'CLOUD_OTHER', 'CLOUD']
const isCloudAsset = computed(() => asset.value && (cloudTypes.includes(asset.value.type) || (asset.value.cloudProvider && asset.value.cloudProvider !== 'ON_PREMISE')))

const assetCategoryMap = {
  INFO: '정보(INFO)', SW: '소프트웨어(SW)', HW: '하드웨어(HW)',
  SERVICE: '서비스(SERVICE)', PERSONNEL: '인력(PERSONNEL)', FACILITY: '시설(FACILITY)'
}
function assetCategoryLabel(v) { return assetCategoryMap[v] || v || '' }

const statusMap = { OPERATIONAL: '운영중', SUSPENDED: '중지', DISPOSED: '폐기' }
function statusLabel(v) { return statusMap[v] || v || '-' }
function statusBadge(v) {
  return { OPERATIONAL: 'badge-success', SUSPENDED: 'badge-yellow', DISPOSED: 'badge-danger' }[v] || 'badge-gray'
}

onMounted(async () => {
  try {
    const res = await assetApi.get(route.params.id)
    asset.value = res.data
  } finally {
    loading.value = false
  }
})

function formatDate(dt) { return dt ? new Date(dt).toLocaleDateString('ko-KR') : '-' }

function criticalityClass(c) {
  return { HIGH: 'badge-red', MEDIUM: 'badge-yellow', LOW: 'badge-green' }[c] || 'badge-gray'
}

function environmentBadge(e) {
  return { PRODUCTION: 'badge-red', STAGING: 'badge-yellow', DEVELOPMENT: 'badge-blue', TEST: 'badge-gray' }[e] || 'badge-gray'
}

function cloudBadge(p) {
  return { AWS: 'badge-orange', GCP: 'badge-blue', AZURE: 'badge-blue' }[p] || 'badge-gray'
}

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
