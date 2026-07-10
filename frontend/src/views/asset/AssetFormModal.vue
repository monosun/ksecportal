<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-5xl max-h-[92vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <h2 class="text-lg font-semibold text-gray-900">
          {{ isEdit ? $t('common.edit') + ' ' + $t('asset.title') : $t('asset.create') }}
        </h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- Body (scrolls internally) -->
      <form id="assetForm" @submit.prevent="submit" class="px-5 py-4 overflow-y-auto flex-1">
        <div v-if="loading" class="py-16 text-center text-gray-400">{{ $t('common.loading') }}</div>
        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-x-5 gap-y-4">

          <!-- 식별 -->
          <section class="section md:col-span-2">
            <h3 class="section-title">식별</h3>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div class="md:col-span-2">
                <label class="lbl">{{ $t('asset.name') }} *</label>
                <input v-model="form.name" required class="input w-full" />
              </div>
              <div>
                <label class="lbl">자산유형</label>
                <select v-model="form.assetCategory" class="input w-full">
                  <option value="">선택 안함</option>
                  <option v-for="c in assetCategories" :key="c.value" :value="c.value">{{ c.label }}</option>
                </select>
              </div>
              <div>
                <label class="lbl">{{ $t('asset.type') }} *</label>
                <select v-model="form.type" required class="input w-full">
                  <option value="">선택</option>
                  <option v-for="t in assetTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
                </select>
              </div>
              <div class="md:col-span-2">
                <label class="lbl">{{ $t('asset.description') }}</label>
                <textarea v-model="form.description" rows="2" class="input w-full resize-none" placeholder="용도 및 역할" />
              </div>
            </div>

            <!-- SW 자산 → SBOM 맵핑 -->
            <div v-if="form.assetCategory === 'SW'" class="bg-blue-50 rounded-lg p-3 space-y-1.5 mt-3">
              <label class="lbl">SBOM 맵핑</label>
              <template v-if="sbomLoaded && sbomList.length">
                <select v-model="form.sbomSoftwareId" class="input w-full">
                  <option value="">맵핑 안함</option>
                  <option v-for="s in sbomList" :key="s.id" :value="s.id">
                    {{ s.name }} {{ s.version }}{{ s.vendor ? ` — ${s.vendor}` : '' }} (라이브러리 {{ s.componentCount }}개)
                  </option>
                </select>
                <p class="text-xs text-blue-700">SBOM 관리에 등록된 SW를 선택하면 라이브러리 구성 정보가 연결됩니다.</p>
              </template>
              <p v-else-if="sbomLoaded" class="text-xs text-gray-500">
                SBOM 관리에 등록된 SW가 없습니다.
                <RouterLink to="/sbom" class="text-blue-600 hover:underline">SBOM 관리</RouterLink>에서 먼저 등록하세요.
              </p>
              <p v-else class="text-xs text-gray-400">SBOM 목록 확인 중...</p>
            </div>
          </section>

          <!-- 책임 -->
          <section class="section">
            <h3 class="section-title">책임</h3>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="lbl">{{ $t('asset.owner') }}</label>
                <select v-model="form.owner" class="input w-full" @change="onOwnerChange">
                  <option value="">선택 안함</option>
                  <option v-for="u in users" :key="u.id" :value="u.name">
                    {{ u.name }}{{ u.department ? ` (${u.department})` : '' }}
                  </option>
                </select>
              </div>
              <div>
                <label class="lbl">{{ $t('asset.department') }}</label>
                <input v-model="form.department" class="input w-full" />
              </div>
              <div class="col-span-2">
                <label class="lbl">운영담당자</label>
                <input v-model="form.operator" class="input w-full" placeholder="실무 담당자" />
              </div>
            </div>
          </section>

          <!-- 현황 -->
          <section class="section">
            <h3 class="section-title">현황</h3>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="lbl">{{ $t('asset.environment') }}</label>
                <select v-model="form.environment" class="input w-full">
                  <option v-for="e in environments" :key="e" :value="e">{{ $t(`asset.environment_label.${e}`) }}</option>
                </select>
              </div>
              <div>
                <label class="lbl">상태</label>
                <select v-model="form.status" class="input w-full">
                  <option v-for="s in statuses" :key="s.value" :value="s.value">{{ s.label }}</option>
                </select>
              </div>
              <div class="col-span-2">
                <label class="lbl">위치</label>
                <input v-model="form.location" class="input w-full" placeholder="AWS, IDC 서울, 사무실 3층 등" />
              </div>
            </div>
          </section>

          <!-- 클라우드 / 네트워크 -->
          <section class="section">
            <h3 class="section-title">클라우드 / 네트워크</h3>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="lbl">{{ $t('asset.cloudProvider') }}</label>
                <select v-model="form.cloudProvider" class="input w-full">
                  <option v-for="p in cloudProviders" :key="p" :value="p">{{ $t(`asset.cloud_provider_label.${p}`) }}</option>
                </select>
              </div>
              <div>
                <label class="lbl">{{ $t('asset.ipAddress') }}</label>
                <input v-model="form.ipAddress" class="input w-full" placeholder="192.168.1.10" />
              </div>
              <template v-if="isCloudAsset">
                <div>
                  <label class="lbl">{{ $t('asset.cloudResourceId') }}</label>
                  <input v-model="form.cloudResourceId" class="input w-full font-mono" placeholder="i-0a1b2c3d…" />
                </div>
                <div>
                  <label class="lbl">{{ $t('asset.region') }}</label>
                  <input v-model="form.region" class="input w-full" placeholder="ap-northeast-2" />
                </div>
              </template>
              <div>
                <label class="lbl">{{ $t('asset.osType') }}</label>
                <input v-model="form.osType" class="input w-full" placeholder="Ubuntu 22.04" />
              </div>
              <div>
                <label class="lbl">{{ $t('asset.spec') }}</label>
                <input v-model="form.spec" class="input w-full" placeholder="8코어 / 32GB" />
              </div>
            </div>
          </section>

          <!-- 평가 -->
          <section class="section">
            <h3 class="section-title">평가 (기밀·무결·가용)</h3>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="lbl">기밀성 (C)</label>
                <select v-model="form.confidentiality" class="input w-full">
                  <option v-for="c in criticalities" :key="c" :value="c">{{ $t(`asset.criticality_label.${c}`) }}</option>
                </select>
              </div>
              <div>
                <label class="lbl">무결성 (I)</label>
                <select v-model="form.integrity" class="input w-full">
                  <option v-for="c in criticalities" :key="c" :value="c">{{ $t(`asset.criticality_label.${c}`) }}</option>
                </select>
              </div>
              <div>
                <label class="lbl">가용성 (A)</label>
                <select v-model="form.availability" class="input w-full">
                  <option v-for="c in criticalities" :key="c" :value="c">{{ $t(`asset.criticality_label.${c}`) }}</option>
                </select>
              </div>
              <div>
                <label class="lbl">{{ $t('asset.criticality') }} (종합)</label>
                <select v-model="form.criticality" class="input w-full">
                  <option v-for="c in criticalities" :key="c" :value="c">{{ $t(`asset.criticality_label.${c}`) }}</option>
                </select>
              </div>
            </div>
          </section>

          <!-- 개인정보 -->
          <section class="section">
            <h3 class="section-title">개인정보</h3>
            <div class="flex flex-wrap gap-x-6 gap-y-2">
              <label class="chk"><input type="checkbox" v-model="form.personalInfoIncluded" class="chkbox" /> 개인정보 포함</label>
              <label class="chk"><input type="checkbox" v-model="form.personalInfoProcessing" class="chkbox" /> 개인정보 처리</label>
            </div>
            <div v-if="form.personalInfoIncluded || form.personalInfoProcessing" class="mt-3">
              <label class="lbl">개인정보 유형</label>
              <input v-model="form.personalInfoType" class="input w-full" placeholder="고객정보, 임직원정보 등" />
            </div>
          </section>

          <!-- 연계 및 보안 관리 -->
          <section class="section">
            <h3 class="section-title">연계 및 보안 관리</h3>
            <div>
              <label class="lbl">연계 시스템</label>
              <input v-model="form.linkedSystems" class="input w-full" placeholder="HR 시스템, 결제 시스템 등" />
            </div>
            <div class="flex flex-wrap gap-x-5 gap-y-2 mt-3">
              <label class="chk"><input type="checkbox" v-model="form.accessControlTarget" class="chkbox" /> 접근권한 관리</label>
              <label class="chk"><input type="checkbox" v-model="form.backupTarget" class="chkbox" /> 백업 대상</label>
              <label class="chk"><input type="checkbox" v-model="form.logManagementTarget" class="chkbox" /> 로그 관리</label>
            </div>
          </section>

          <!-- 비용 / 계약 -->
          <section class="section">
            <h3 class="section-title">비용 / 계약</h3>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="lbl">{{ $t('asset.monthlyCost') }}</label>
                <input v-model="form.monthlyCost" type="number" min="0" class="input w-full" placeholder="0" />
              </div>
              <div>
                <label class="lbl">{{ $t('asset.contractExpiry') }}</label>
                <input v-model="form.contractExpiry" type="date" class="input w-full" />
              </div>
            </div>
          </section>

          <!-- 점검 / 검토 -->
          <section class="section">
            <h3 class="section-title">점검 / 검토</h3>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="lbl">{{ $t('asset.lastInspectionDate') }}</label>
                <input v-model="form.lastInspectionDate" type="date" class="input w-full" />
              </div>
              <div>
                <label class="lbl">{{ $t('asset.nextInspectionDate') }}</label>
                <input v-model="form.nextInspectionDate" type="date" class="input w-full" />
              </div>
              <div class="col-span-2">
                <label class="lbl">최종 검토일</label>
                <input v-model="form.lastReviewDate" type="date" class="input w-full" />
              </div>
            </div>
          </section>

          <!-- 비고 -->
          <section class="section md:col-span-2">
            <h3 class="section-title">비고</h3>
            <textarea v-model="form.remarks" rows="2" class="input w-full resize-none" placeholder="특이사항" />
          </section>
        </div>

        <div v-if="error" class="text-sm text-red-600 bg-red-50 p-3 rounded-lg mt-4">{{ error }}</div>
      </form>

      <!-- Footer -->
      <div class="flex justify-end gap-3 px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">{{ $t('common.cancel') }}</button>
        <button type="submit" form="assetForm" :disabled="submitting || loading" class="btn-primary text-sm">
          {{ submitting ? $t('common.loading') : $t('common.save') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { assetApi, adminApi, codeApi, sbomApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  assetId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close', 'saved'])

const isEdit = computed(() => !!props.assetId)

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

function emptyForm() {
  return {
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
  }
}

const form = ref(emptyForm())
const users = ref([])
const submitting = ref(false)
const loading = ref(false)
const error = ref(null)

const isCloudAsset = computed(() => cloudTypeValues.includes(form.value.type))

// ── SBOM 맵핑 (자산유형 SW 선택 시 조회) ─────────────
const sbomList = ref([])
const sbomLoaded = ref(false)

watch(() => form.value.assetCategory, async (category) => {
  if (category !== 'SW' || sbomLoaded.value) return
  try { sbomList.value = (await sbomApi.listAll()).data || [] }
  catch { sbomList.value = [] }
  finally { sbomLoaded.value = true }
})

let refDataLoaded = false
async function loadRefData() {
  if (refDataLoaded) return
  try { assetTypes.value = (await codeApi.getValues('ASSET_TYPE')).data || [] } catch { assetTypes.value = [] }
  try { users.value = (await adminApi.listUsersSimple()).data || [] } catch { users.value = [] }
  refDataLoaded = true
}

// 모달이 열릴 때마다 초기화 · 데이터 로드
watch(() => props.open, async (open) => {
  if (!open) return
  error.value = null
  sbomLoaded.value = false
  sbomList.value = []
  loading.value = true
  await loadRefData()
  if (props.assetId) {
    try {
      const a = (await assetApi.get(props.assetId)).data
      form.value = {
        name: a.name, assetCategory: a.assetCategory || '', type: a.type, description: a.description || '',
        owner: a.owner || '', department: a.department || '', operator: a.operator || '',
        environment: a.environment || 'PRODUCTION', location: a.location || '',
        status: a.status || 'OPERATIONAL', cloudProvider: a.cloudProvider || 'ON_PREMISE',
        cloudResourceId: a.cloudResourceId || '', region: a.region || '',
        ipAddress: a.ipAddress || '', osType: a.osType || '', spec: a.spec || '',
        criticality: a.criticality || 'MEDIUM', confidentiality: a.confidentiality || 'MEDIUM',
        integrity: a.integrity || 'MEDIUM', availability: a.availability || 'MEDIUM',
        personalInfoIncluded: a.personalInfoIncluded || false, personalInfoType: a.personalInfoType || '',
        personalInfoProcessing: a.personalInfoProcessing || false,
        linkedSystems: a.linkedSystems || '', sbomSoftwareId: a.sbomSoftwareId || '',
        accessControlTarget: a.accessControlTarget || false, backupTarget: a.backupTarget || false,
        logManagementTarget: a.logManagementTarget || false,
        monthlyCost: a.monthlyCost || '', contractExpiry: a.contractExpiry || '',
        lastInspectionDate: a.lastInspectionDate || '', nextInspectionDate: a.nextInspectionDate || '',
        lastReviewDate: a.lastReviewDate || '', remarks: a.remarks || ''
      }
    } catch (e) { error.value = e || '자산 정보를 불러오지 못했습니다.' }
  } else {
    form.value = emptyForm()
  }
  loading.value = false
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
    if (payload.assetCategory === 'SW' && payload.sbomSoftwareId) {
      payload.sbomSoftwareId = Number(payload.sbomSoftwareId)
    } else {
      payload.sbomSoftwareId = isEdit.value ? 0 : null
    }
    let saved
    if (isEdit.value) {
      saved = (await assetApi.update(props.assetId, payload)).data
    } else {
      saved = (await assetApi.create(payload)).data
    }
    emit('saved', saved)
  } catch (e) {
    error.value = e
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.section { @apply border border-gray-200 rounded-lg p-4; }
.section-title { @apply text-sm font-semibold text-gray-800 border-b pb-2 mb-3; }
.lbl { @apply block text-xs font-medium text-gray-600 mb-1; }
.chk { @apply flex items-center gap-2 text-sm font-medium text-gray-700 cursor-pointer; }
.chkbox { @apply w-4 h-4 text-blue-600; }
</style>
