<template>
  <div class="p-8 max-w-3xl mx-auto">
    <RouterLink :to="isEdit ? `/assets/${route.params.id}` : '/assets'" class="text-sm text-primary-600 hover:underline mb-4 inline-block">← {{ $t('common.back') }}</RouterLink>
    <h1 class="text-2xl font-bold text-gray-900 mb-6">
      {{ isEdit ? $t('common.edit') + ' ' + $t('asset.title') : $t('asset.create') }}
    </h1>

    <form @submit.prevent="submit" class="space-y-5">

      <!-- 식별 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">식별</h2>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.name') }} *</label>
          <input v-model="form.name" required class="input w-full" />
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">자산유형</label>
            <select v-model="form.assetCategory" class="input w-full">
              <option value="">선택 안함</option>
              <option v-for="c in assetCategories" :key="c.value" :value="c.value">{{ c.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.type') }} *</label>
            <select v-model="form.type" required class="input w-full">
              <option value="">선택</option>
              <option v-for="t in assetTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>
        </div>

        <!-- SW 자산 → SBOM 맵핑 -->
        <div v-if="form.assetCategory === 'SW'" class="bg-blue-50 rounded-lg p-4 space-y-2">
          <label class="block text-sm font-medium text-gray-700">SBOM 맵핑</label>
          <template v-if="sbomLoaded && sbomList.length">
            <select v-model="form.sbomSoftwareId" class="input w-full">
              <option value="">맵핑 안함</option>
              <option v-for="s in sbomList" :key="s.id" :value="s.id">
                {{ s.name }} {{ s.version }}{{ s.vendor ? ` — ${s.vendor}` : '' }} (라이브러리 {{ s.componentCount }}개)
              </option>
            </select>
            <p class="text-xs text-blue-700">SBOM 관리에 등록된 SW를 선택하면 이 자산과 라이브러리 구성 정보가 연결됩니다.</p>
          </template>
          <p v-else-if="sbomLoaded" class="text-sm text-gray-500">
            SBOM 관리에 등록된 SW가 없습니다.
            <RouterLink to="/sbom" class="text-blue-600 hover:underline">SBOM 관리</RouterLink>에서 먼저 SW를 등록하세요.
          </p>
          <p v-else class="text-sm text-gray-400">SBOM 목록 확인 중...</p>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.description') }}</label>
          <textarea v-model="form.description" rows="2" class="input w-full resize-none" placeholder="용도 및 역할" />
        </div>
      </div>

      <!-- 책임 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">책임</h2>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.owner') }} *</label>
            <select v-model="form.owner" class="input w-full" @change="onOwnerChange">
              <option value="">선택 안함</option>
              <option v-for="u in users" :key="u.id" :value="u.name">
                {{ u.name }}{{ u.department ? ` (${u.department})` : '' }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.department') }} *</label>
            <input v-model="form.department" class="input w-full" />
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">운영담당자</label>
          <input v-model="form.operator" class="input w-full" placeholder="실무 담당자" />
        </div>
      </div>

      <!-- 현황 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">현황</h2>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.environment') }}</label>
            <select v-model="form.environment" class="input w-full">
              <option v-for="e in environments" :key="e" :value="e">{{ $t(`asset.environment_label.${e}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">위치</label>
            <input v-model="form.location" class="input w-full" placeholder="AWS, IDC 서울, 사무실 3층 등" />
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">상태</label>
          <select v-model="form.status" class="input w-full">
            <option v-for="s in statuses" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
      </div>

      <!-- 클라우드 정보 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">클라우드 / 네트워크</h2>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.cloudProvider') }}</label>
            <select v-model="form.cloudProvider" class="input w-full">
              <option v-for="p in cloudProviders" :key="p" :value="p">{{ $t(`asset.cloud_provider_label.${p}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.ipAddress') }}</label>
            <input v-model="form.ipAddress" class="input w-full" placeholder="192.168.1.10" />
          </div>
        </div>

        <div v-if="isCloudAsset" class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.cloudResourceId') }}</label>
            <input v-model="form.cloudResourceId" class="input w-full font-mono" placeholder="i-0a1b2c3d4e5f67890" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.region') }}</label>
            <input v-model="form.region" class="input w-full" placeholder="ap-northeast-2" />
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.osType') }}</label>
            <input v-model="form.osType" class="input w-full" placeholder="Ubuntu 22.04 LTS" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.spec') }}</label>
            <input v-model="form.spec" class="input w-full" placeholder="CPU: 8코어 / RAM: 32GB" />
          </div>
        </div>
      </div>

      <!-- 평가 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">평가</h2>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">기밀성 (C)</label>
            <select v-model="form.confidentiality" class="input w-full">
              <option v-for="c in criticalities" :key="c" :value="c">{{ $t(`asset.criticality_label.${c}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">무결성 (I)</label>
            <select v-model="form.integrity" class="input w-full">
              <option v-for="c in criticalities" :key="c" :value="c">{{ $t(`asset.criticality_label.${c}`) }}</option>
            </select>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">가용성 (A)</label>
            <select v-model="form.availability" class="input w-full">
              <option v-for="c in criticalities" :key="c" :value="c">{{ $t(`asset.criticality_label.${c}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.criticality') }} (종합)</label>
            <select v-model="form.criticality" class="input w-full">
              <option v-for="c in criticalities" :key="c" :value="c">{{ $t(`asset.criticality_label.${c}`) }}</option>
            </select>
          </div>
        </div>
      </div>

      <!-- 개인정보 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">개인정보</h2>

        <div class="grid grid-cols-2 gap-4">
          <div class="flex items-center gap-3 pt-5">
            <input type="checkbox" id="personalInfoIncluded" v-model="form.personalInfoIncluded" class="w-4 h-4 text-blue-600" />
            <label for="personalInfoIncluded" class="text-sm font-medium text-gray-700">개인정보 포함</label>
          </div>
          <div class="flex items-center gap-3 pt-5">
            <input type="checkbox" id="personalInfoProcessing" v-model="form.personalInfoProcessing" class="w-4 h-4 text-blue-600" />
            <label for="personalInfoProcessing" class="text-sm font-medium text-gray-700">개인정보 처리</label>
          </div>
        </div>

        <div v-if="form.personalInfoIncluded || form.personalInfoProcessing">
          <label class="block text-sm font-medium text-gray-700 mb-1">개인정보 유형</label>
          <input v-model="form.personalInfoType" class="input w-full" placeholder="고객정보, 임직원정보 등" />
        </div>
      </div>

      <!-- 연계 및 보안 관리 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">연계 및 보안 관리</h2>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">연계 시스템</label>
          <input v-model="form.linkedSystems" class="input w-full" placeholder="HR 시스템, 결제 시스템 등" />
        </div>

        <div class="grid grid-cols-3 gap-4">
          <div class="flex items-center gap-2">
            <input type="checkbox" id="accessControlTarget" v-model="form.accessControlTarget" class="w-4 h-4 text-blue-600" />
            <label for="accessControlTarget" class="text-sm font-medium text-gray-700">접근권한 관리 대상</label>
          </div>
          <div class="flex items-center gap-2">
            <input type="checkbox" id="backupTarget" v-model="form.backupTarget" class="w-4 h-4 text-blue-600" />
            <label for="backupTarget" class="text-sm font-medium text-gray-700">백업 대상</label>
          </div>
          <div class="flex items-center gap-2">
            <input type="checkbox" id="logManagementTarget" v-model="form.logManagementTarget" class="w-4 h-4 text-blue-600" />
            <label for="logManagementTarget" class="text-sm font-medium text-gray-700">로그 관리 대상</label>
          </div>
        </div>
      </div>

      <!-- 비용 / 계약 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">비용 / 계약</h2>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.monthlyCost') }}</label>
            <input v-model="form.monthlyCost" type="number" min="0" class="input w-full" placeholder="0" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.contractExpiry') }}</label>
            <input v-model="form.contractExpiry" type="date" class="input w-full" />
          </div>
        </div>
      </div>

      <!-- 점검 / 검토 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">점검 / 검토</h2>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.lastInspectionDate') }}</label>
            <input v-model="form.lastInspectionDate" type="date" class="input w-full" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('asset.nextInspectionDate') }}</label>
            <input v-model="form.nextInspectionDate" type="date" class="input w-full" />
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">최종 검토일</label>
          <input v-model="form.lastReviewDate" type="date" class="input w-full" />
        </div>
      </div>

      <!-- 비고 -->
      <div class="card space-y-4">
        <h2 class="text-base font-semibold text-gray-800 border-b pb-2">비고</h2>
        <div>
          <textarea v-model="form.remarks" rows="3" class="input w-full resize-none" placeholder="특이사항" />
        </div>
      </div>

      <div v-if="error" class="text-sm text-red-600 bg-red-50 p-3 rounded-lg">{{ error }}</div>

      <div class="flex gap-3 pt-2">
        <button type="submit" :disabled="submitting" class="btn-primary">
          {{ submitting ? $t('common.loading') : $t('common.save') }}
        </button>
        <RouterLink :to="isEdit ? `/assets/${route.params.id}` : '/assets'" class="btn-secondary">
          {{ $t('common.cancel') }}
        </RouterLink>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { assetApi, adminApi, codeApi, sbomApi } from '@/api'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id && route.path.includes('/edit'))

const assetTypes = ref([])
const cloudTypeValues = ['EC2', 'RDS', 'S3', 'ELB', 'LAMBDA', 'CLOUD_OTHER']
const criticalities = ['HIGH', 'MEDIUM', 'LOW']
const cloudProviders = ['ON_PREMISE', 'AWS', 'GCP', 'AZURE', 'OTHER']
const environments = ['PRODUCTION', 'STAGING', 'DEVELOPMENT', 'TEST']

const assetCategories = [
  { value: 'INFO',      label: '정보(INFO)' },
  { value: 'SW',        label: '소프트웨어(SW)' },
  { value: 'HW',        label: '하드웨어(HW)' },
  { value: 'SERVICE',   label: '서비스(SERVICE)' },
  { value: 'PERSONNEL', label: '인력(PERSONNEL)' },
  { value: 'FACILITY',  label: '시설(FACILITY)' },
]

const statuses = [
  { value: 'OPERATIONAL', label: '운영중' },
  { value: 'SUSPENDED',   label: '중지' },
  { value: 'DISPOSED',    label: '폐기' },
]

const form = ref({
  name: '', assetCategory: '', type: '', description: '',
  owner: '', department: '', operator: '',
  environment: 'PRODUCTION', location: '', status: 'OPERATIONAL',
  cloudProvider: 'ON_PREMISE', cloudResourceId: '', region: '',
  ipAddress: '', osType: '', spec: '',
  criticality: 'MEDIUM', confidentiality: 'MEDIUM', integrity: 'MEDIUM', availability: 'MEDIUM',
  personalInfoIncluded: false, personalInfoType: '', personalInfoProcessing: false,
  linkedSystems: '', sbomSoftwareId: '',
  accessControlTarget: false, backupTarget: false, logManagementTarget: false,
  monthlyCost: '', contractExpiry: '', lastInspectionDate: '', nextInspectionDate: '',
  lastReviewDate: '', remarks: ''
})
const users = ref([])
const submitting = ref(false)
const error = ref(null)

const isCloudAsset = computed(() => cloudTypeValues.includes(form.value.type))

// ── SBOM 맵핑 (자산유형 SW 선택 시 SBOM 등록 항목 조회) ─────────────
const sbomList = ref([])
const sbomLoaded = ref(false)

watch(() => form.value.assetCategory, async (category) => {
  if (category !== 'SW' || sbomLoaded.value) return
  try {
    const res = await sbomApi.listAll()
    sbomList.value = res.data || []
  } catch {
    sbomList.value = []
  } finally {
    sbomLoaded.value = true
  }
}, { immediate: true })

onMounted(async () => {
  try {
    const res = await codeApi.getValues('ASSET_TYPE')
    assetTypes.value = res.data || []
  } catch {
    assetTypes.value = []
  }

  try {
    const res = await adminApi.listUsersSimple()
    users.value = res.data || []
  } catch {
    users.value = []
  }

  if (isEdit.value) {
    const res = await assetApi.get(route.params.id)
    const a = res.data
    form.value = {
      name: a.name, assetCategory: a.assetCategory || '', type: a.type, description: a.description || '',
      owner: a.owner || '', department: a.department || '', operator: a.operator || '',
      environment: a.environment || 'PRODUCTION',
      location: a.location || '',
      status: a.status || 'OPERATIONAL',
      cloudProvider: a.cloudProvider || 'ON_PREMISE',
      cloudResourceId: a.cloudResourceId || '', region: a.region || '',
      ipAddress: a.ipAddress || '', osType: a.osType || '', spec: a.spec || '',
      criticality: a.criticality || 'MEDIUM',
      confidentiality: a.confidentiality || 'MEDIUM',
      integrity: a.integrity || 'MEDIUM',
      availability: a.availability || 'MEDIUM',
      personalInfoIncluded: a.personalInfoIncluded || false,
      personalInfoType: a.personalInfoType || '',
      personalInfoProcessing: a.personalInfoProcessing || false,
      linkedSystems: a.linkedSystems || '',
      sbomSoftwareId: a.sbomSoftwareId || '',
      accessControlTarget: a.accessControlTarget || false,
      backupTarget: a.backupTarget || false,
      logManagementTarget: a.logManagementTarget || false,
      monthlyCost: a.monthlyCost || '',
      contractExpiry: a.contractExpiry || '',
      lastInspectionDate: a.lastInspectionDate || '',
      nextInspectionDate: a.nextInspectionDate || '',
      lastReviewDate: a.lastReviewDate || '',
      remarks: a.remarks || ''
    }
  }
})

function onOwnerChange() {
  const selected = users.value.find(u => u.name === form.value.owner)
  if (selected?.department) form.value.department = selected.department
}

async function submit() {
  submitting.value = true
  error.value = null
  try {
    const payload = { ...form.value }
    if (!payload.monthlyCost) payload.monthlyCost = null
    if (!payload.contractExpiry) payload.contractExpiry = null
    if (!payload.lastInspectionDate) payload.lastInspectionDate = null
    if (!payload.nextInspectionDate) payload.nextInspectionDate = null
    if (!payload.lastReviewDate) payload.lastReviewDate = null
    if (!payload.assetCategory) payload.assetCategory = null
    // SW가 아니거나 선택 해제 시: 수정에서는 0(매핑 해제), 등록에서는 null
    if (payload.assetCategory === 'SW' && payload.sbomSoftwareId) {
      payload.sbomSoftwareId = Number(payload.sbomSoftwareId)
    } else {
      payload.sbomSoftwareId = isEdit.value ? 0 : null
    }
    if (isEdit.value) {
      await assetApi.update(route.params.id, payload)
      router.push(`/assets/${route.params.id}`)
    } else {
      const res = await assetApi.create(payload)
      router.push(`/assets/${res.data.id}`)
    }
  } catch (e) {
    error.value = e
  } finally {
    submitting.value = false
  }
}
</script>
