<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('internalAudit.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('internalAudit.subtitle') }}</p>
      </div>
      <button v-if="isManager" @click="openCreateAudit" class="btn-primary flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        {{ $t('internalAudit.addAudit') }}
      </button>
    </div>

    <!-- Year Tabs -->
    <div class="flex gap-2 mb-5 flex-wrap">
      <button v-for="y in yearOptions" :key="y" @click="selectedYear = y"
        :class="['px-4 py-1.5 rounded-full text-sm font-medium transition-colors',
          selectedYear === y ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']">
        {{ y }}년
      </button>
      <button @click="showYearPicker = true"
        class="px-4 py-1.5 rounded-full text-sm font-medium bg-gray-100 text-gray-500 hover:bg-gray-200">
        + 연도 선택
      </button>
    </div>

    <!-- Audit List -->
    <div v-if="loading" class="text-center py-16 text-gray-400">{{ $t('common.loading') }}</div>
    <div v-else-if="audits.length === 0" class="card text-center py-16 text-gray-400">
      {{ $t('internalAudit.noAudits') }}
    </div>
    <div v-else class="space-y-3">
      <div v-for="audit in audits" :key="audit.id"
        class="card hover:shadow-md transition-shadow cursor-pointer"
        @click="openAuditDetail(audit.id)">
        <div class="flex items-start justify-between">
          <div>
            <h3 class="font-semibold text-gray-900 text-base">{{ audit.title }}</h3>
            <div class="flex flex-wrap gap-3 mt-1.5 text-sm text-gray-500">
              <span v-if="audit.auditStart">📅 {{ formatDate(audit.auditStart) }}{{ audit.auditEnd ? ' ~ ' + formatDate(audit.auditEnd) : '' }}</span>
              <span v-if="audit.auditor">👤 {{ audit.auditor }}</span>
            </div>
            <div class="flex gap-2 mt-2">
              <span :class="auditStatusBadge(audit.status)" class="text-xs px-2 py-0.5 rounded font-medium">
                {{ $t(`internalAudit.status_label.${audit.status}`) }}
              </span>
            </div>
          </div>
          <div v-if="isManager" class="flex gap-1 flex-shrink-0" @click.stop>
            <button @click="openEditAudit(audit)" class="p-1.5 rounded hover:bg-gray-100 text-gray-400 hover:text-gray-700">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
            </button>
            <button @click="confirmDeleteAudit(audit)" class="p-1.5 rounded hover:bg-red-50 text-gray-400 hover:text-red-500">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Audit Form Modal -->
    <div v-if="showAuditModal" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg">
        <div class="px-6 py-4 border-b">
          <h2 class="text-lg font-semibold">{{ editAuditTarget ? $t('internalAudit.editAudit') : $t('internalAudit.addAudit') }}</h2>
        </div>
        <div class="px-6 py-5 space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">연도</label>
              <input v-model.number="auditForm.year" type="number" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.status') }}</label>
              <select v-model="auditForm.status" class="input w-full">
                <option v-for="s in AUDIT_STATUSES" :key="s" :value="s">{{ $t(`internalAudit.status_label.${s}`) }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">감사명 *</label>
            <input v-model="auditForm.title" class="input w-full" placeholder="예: 2025년 상반기 내부감사" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.auditStart') }}</label>
              <input v-model="auditForm.auditStart" type="date" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.auditEnd') }}</label>
              <input v-model="auditForm.auditEnd" type="date" class="input w-full" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.auditor') }}</label>
            <input v-model="auditForm.auditor" class="input w-full" placeholder="예: 정보보안팀" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.description') }}</label>
            <textarea v-model="auditForm.description" rows="2" class="input w-full resize-none" />
          </div>
        </div>
        <div class="px-6 py-4 border-t flex justify-end gap-2">
          <button @click="showAuditModal = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="saveAudit" :disabled="saving" class="btn-primary">{{ saving ? $t('common.loading') : $t('common.save') }}</button>
        </div>
      </div>
    </div>

    <!-- Audit Detail Drawer -->
    <div v-if="showDetail && auditDetail" class="fixed inset-0 bg-black/50 z-50 flex items-start justify-end">
      <div class="bg-white w-full max-w-2xl h-full overflow-y-auto flex flex-col shadow-2xl">
        <div class="px-6 py-4 border-b flex items-center justify-between sticky top-0 bg-white z-10">
          <div>
            <p class="text-xs text-gray-500">{{ auditDetail.year }}년</p>
            <h2 class="text-lg font-semibold">{{ auditDetail.title }}</h2>
          </div>
          <button @click="showDetail = false" class="text-gray-400 hover:text-gray-600 p-1">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="px-6 py-5 space-y-6 flex-1">
          <!-- Basic Info -->
          <div class="grid grid-cols-2 gap-3 text-sm">
            <div><span class="text-gray-500">기간</span>
              <p class="font-medium mt-0.5">{{ auditDetail.auditStart ? formatDate(auditDetail.auditStart) : '-' }} ~ {{ auditDetail.auditEnd ? formatDate(auditDetail.auditEnd) : '' }}</p></div>
            <div><span class="text-gray-500">감사자</span><p class="font-medium mt-0.5">{{ auditDetail.auditor || '-' }}</p></div>
            <div><span class="text-gray-500">상태</span>
              <p class="mt-0.5"><span :class="auditStatusBadge(auditDetail.status)" class="text-xs px-2 py-0.5 rounded font-medium">{{ $t(`internalAudit.status_label.${auditDetail.status}`) }}</span></p></div>
          </div>

          <!-- Targets -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <h3 class="font-semibold text-gray-800">{{ $t('internalAudit.targets') }}</h3>
              <button v-if="isManager" @click="openAddTarget" class="btn-secondary text-xs py-1 px-3">+ {{ $t('internalAudit.addTarget') }}</button>
            </div>
            <div v-if="!auditDetail.targets?.length" class="text-sm text-gray-400 py-2 text-center border rounded-lg">{{ $t('internalAudit.noTargets') }}</div>
            <div v-else class="space-y-1.5">
              <div v-for="t in auditDetail.targets" :key="t.id"
                class="flex items-center justify-between p-2.5 bg-gray-50 rounded-lg border text-sm">
                <div>
                  <span class="font-medium">{{ t.targetName }}</span>
                  <span v-if="t.targetType" class="ml-2 text-xs text-gray-400">{{ t.targetType }}</span>
                  <p v-if="t.description" class="text-xs text-gray-500 mt-0.5">{{ t.description }}</p>
                </div>
                <div v-if="isManager" class="flex gap-1">
                  <button @click="openEditTarget(t)" class="p-1 rounded hover:bg-gray-200 text-gray-400 hover:text-gray-700">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                  </button>
                  <button @click="deleteTarget(t)" class="p-1 rounded hover:bg-red-50 text-gray-400 hover:text-red-500">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Items -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <h3 class="font-semibold text-gray-800">{{ $t('internalAudit.items') }}</h3>
              <button v-if="isManager" @click="openAddItem" class="btn-secondary text-xs py-1 px-3">+ {{ $t('internalAudit.addItem') }}</button>
            </div>
            <div v-if="!auditDetail.items?.length" class="text-sm text-gray-400 py-2 text-center border rounded-lg">{{ $t('internalAudit.noItems') }}</div>
            <div v-else class="space-y-2">
              <div v-for="item in auditDetail.items" :key="item.id"
                class="p-3 bg-gray-50 rounded-lg border text-sm">
                <div class="flex items-start justify-between">
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 flex-wrap">
                      <span class="font-medium text-gray-900">{{ item.itemName }}</span>
                      <span v-if="item.targetName" class="text-xs text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">{{ item.targetName }}</span>
                      <span v-if="item.result" :class="resultBadge(item.result)" class="text-xs px-2 py-0.5 rounded font-medium">
                        {{ $t(`internalAudit.result_label.${item.result}`) }}
                      </span>
                    </div>
                    <p v-if="item.checkMethod" class="text-xs text-gray-500 mt-1">점검방법: {{ item.checkMethod }}</p>
                    <p v-if="item.finding" class="text-xs text-amber-700 mt-1 bg-amber-50 px-2 py-1 rounded">발견: {{ item.finding }}</p>
                    <p v-if="item.actionRequired" class="text-xs text-blue-700 mt-1 bg-blue-50 px-2 py-1 rounded">조치: {{ item.actionRequired }}</p>
                  </div>
                  <div v-if="isManager" class="flex gap-1 flex-shrink-0 ml-2">
                    <button @click="openEditItem(item)" class="p-1 rounded hover:bg-gray-200 text-gray-400 hover:text-gray-700">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                    </button>
                    <button @click="deleteItem(item)" class="p-1 rounded hover:bg-red-50 text-gray-400 hover:text-red-500">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Files -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <h3 class="font-semibold text-gray-800">{{ $t('internalAudit.files') }}</h3>
              <button v-if="isManager" @click="openAddAuditFile" class="btn-secondary text-xs py-1 px-3">+ {{ $t('internalAudit.addFile') }}</button>
            </div>
            <div v-if="!auditDetail.files?.length" class="text-sm text-gray-400 py-2 text-center border rounded-lg">첨부 파일 없음</div>
            <div v-else class="space-y-2">
              <div v-for="f in auditDetail.files" :key="f.id"
                class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border text-sm">
                <div>
                  <p class="font-medium text-gray-900">{{ f.title }}</p>
                  <p v-if="f.fileName" class="text-xs text-gray-400">{{ f.fileName }}</p>
                </div>
                <div class="flex gap-1">
                  <button v-if="f.fileName" @click="downloadAuditFile(f)"
                    class="p-1.5 rounded hover:bg-gray-200 text-gray-500">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
                  </button>
                  <button v-if="isManager" @click="deleteAuditFile(f)"
                    class="p-1.5 rounded hover:bg-red-50 text-gray-400 hover:text-red-500">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Target Form Modal -->
    <div v-if="showTargetModal" class="fixed inset-0 bg-black/50 z-[60] flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div class="px-6 py-4 border-b"><h2 class="text-lg font-semibold">{{ editTargetItem ? '점검대상 수정' : $t('internalAudit.addTarget') }}</h2></div>
        <div class="px-6 py-5 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.targetName') }} *</label>
            <input v-model="targetForm.targetName" class="input w-full" placeholder="예: 인사시스템" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.targetType') }}</label>
            <input v-model="targetForm.targetType" class="input w-full" placeholder="예: 웹 애플리케이션" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea v-model="targetForm.description" rows="2" class="input w-full resize-none" />
          </div>
        </div>
        <div class="px-6 py-4 border-t flex justify-end gap-2">
          <button @click="showTargetModal = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="saveTarget" :disabled="saving" class="btn-primary">{{ saving ? $t('common.loading') : $t('common.save') }}</button>
        </div>
      </div>
    </div>

    <!-- Item Form Modal -->
    <div v-if="showItemModal" class="fixed inset-0 bg-black/50 z-[60] flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg">
        <div class="px-6 py-4 border-b"><h2 class="text-lg font-semibold">{{ editItemTarget ? $t('internalAudit.editItem') : $t('internalAudit.addItem') }}</h2></div>
        <div class="px-6 py-5 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검대상</label>
            <select v-model="itemForm.targetId" class="input w-full">
              <option :value="null">-- 선택 안함 --</option>
              <option v-for="t in auditDetail.targets" :key="t.id" :value="t.id">{{ t.targetName }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.itemName') }} *</label>
            <input v-model="itemForm.itemName" class="input w-full" placeholder="예: 관리자 계정 현황 점검" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.checkMethod') }}</label>
            <input v-model="itemForm.checkMethod" class="input w-full" placeholder="점검 방법" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.result') }}</label>
            <select v-model="itemForm.result" class="input w-full">
              <option value="">-- 미결정 --</option>
              <option v-for="r in RESULTS" :key="r" :value="r">{{ $t(`internalAudit.result_label.${r}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.finding') }}</label>
            <textarea v-model="itemForm.finding" rows="2" class="input w-full resize-none" placeholder="발견사항" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('internalAudit.actionRequired') }}</label>
            <textarea v-model="itemForm.actionRequired" rows="2" class="input w-full resize-none" placeholder="조치 필요사항" />
          </div>
        </div>
        <div class="px-6 py-4 border-t flex justify-end gap-2">
          <button @click="showItemModal = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="saveItem" :disabled="saving" class="btn-primary">{{ saving ? $t('common.loading') : $t('common.save') }}</button>
        </div>
      </div>
    </div>

    <!-- Audit File Modal -->
    <div v-if="showAuditFileModal" class="fixed inset-0 bg-black/50 z-[60] flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div class="px-6 py-4 border-b"><h2 class="text-lg font-semibold">{{ $t('internalAudit.addFile') }}</h2></div>
        <div class="px-6 py-5 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">제목 *</label>
            <input v-model="auditFileForm.title" class="input w-full" placeholder="예: 감사 결과 보고서" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">파일 선택</label>
            <input ref="auditFileInput" type="file" @change="onAuditFileChange"
              class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0
              file:text-sm file:font-semibold file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100" />
          </div>
        </div>
        <div class="px-6 py-4 border-t flex justify-end gap-2">
          <button @click="showAuditFileModal = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="saveAuditFile" :disabled="saving" class="btn-primary">{{ saving ? $t('common.loading') : $t('common.save') }}</button>
        </div>
      </div>
    </div>

    <!-- Year Picker -->
    <div v-if="showYearPicker" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-xs p-6">
        <h3 class="text-base font-semibold mb-4">연도 선택</h3>
        <input v-model.number="pickerYear" type="number" class="input w-full mb-4" />
        <div class="flex justify-end gap-2">
          <button @click="showYearPicker = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="applyYear" class="btn-primary">확인</button>
        </div>
      </div>
    </div>

    <!-- Confirm -->
    <div v-if="confirmModal.show" class="fixed inset-0 bg-black/50 z-[70] flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6">
        <h3 class="text-base font-semibold mb-2">삭제 확인</h3>
        <p class="text-sm text-gray-500 mb-5">{{ confirmModal.message }}</p>
        <div class="flex justify-end gap-2">
          <button @click="confirmModal.show = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="confirmModal.onConfirm" class="btn-danger">{{ $t('common.delete') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { internalAuditApi } from '@/api'

const auth = useAuthStore()
const isManager = computed(() => auth.isAdmin || auth.user?.role === 'MANAGER')

const currentYear = new Date().getFullYear()
const yearOptions = ref([currentYear])
const selectedYear = ref(currentYear)

const audits = ref([])
const auditDetail = ref(null)
const loading = ref(false)
const saving = ref(false)

const showAuditModal = ref(false)
const showDetail = ref(false)
const showTargetModal = ref(false)
const showItemModal = ref(false)
const showAuditFileModal = ref(false)
const showYearPicker = ref(false)
const pickerYear = ref(currentYear)

const editAuditTarget = ref(null)
const editTargetItem = ref(null)
const editItemTarget = ref(null)

const confirmModal = ref({ show: false, message: '', onConfirm: () => {} })

const AUDIT_STATUSES = ['PLANNED', 'IN_PROGRESS', 'COMPLETED']
const RESULTS = ['PASS', 'FAIL', 'NA']

const auditForm = ref(emptyAuditForm())
const targetForm = ref({ targetName: '', targetType: '', description: '', sortOrder: 0 })
const itemForm = ref({ targetId: null, itemName: '', checkMethod: '', result: '', finding: '', actionRequired: '', sortOrder: 0 })
const auditFileForm = ref({ title: '', file: null })
const auditFileInput = ref(null)

function emptyAuditForm() {
  return { year: new Date().getFullYear(), title: '', auditStart: '', auditEnd: '', auditor: '', status: 'PLANNED', description: '' }
}

onMounted(async () => {
  const years = await internalAuditApi.years()
  if (years?.data?.length) {
    yearOptions.value = years.data
    selectedYear.value = years.data[0]
  }
  fetchAudits()
})

watch(selectedYear, fetchAudits)

async function fetchAudits() {
  loading.value = true
  try {
    const res = await internalAuditApi.list(selectedYear.value)
    audits.value = res?.data || []
  } finally {
    loading.value = false
  }
}

async function openAuditDetail(id) {
  const res = await internalAuditApi.get(id)
  auditDetail.value = res?.data
  showDetail.value = true
}

function openCreateAudit() {
  editAuditTarget.value = null
  auditForm.value = emptyAuditForm()
  auditForm.value.year = selectedYear.value
  showAuditModal.value = true
}

function openEditAudit(a) {
  editAuditTarget.value = a
  auditForm.value = { year: a.year, title: a.title, auditStart: a.auditStart || '',
    auditEnd: a.auditEnd || '', auditor: a.auditor || '', status: a.status, description: a.description || '' }
  showAuditModal.value = true
}

async function saveAudit() {
  saving.value = true
  try {
    if (editAuditTarget.value) {
      await internalAuditApi.update(editAuditTarget.value.id, auditForm.value)
    } else {
      await internalAuditApi.create(auditForm.value)
      if (!yearOptions.value.includes(auditForm.value.year)) {
        yearOptions.value = [auditForm.value.year, ...yearOptions.value].sort((a, b) => b - a)
      }
      selectedYear.value = auditForm.value.year
    }
    showAuditModal.value = false
    fetchAudits()
  } catch (e) { alert(e || '저장 실패') } finally { saving.value = false }
}

function confirmDeleteAudit(a) {
  confirmModal.value = { show: true, message: '감사를 삭제하시겠습니까? 모든 항목과 파일이 함께 삭제됩니다.',
    onConfirm: async () => { confirmModal.value.show = false; await internalAuditApi.delete(a.id); fetchAudits() } }
}

// Targets
function openAddTarget() { editTargetItem.value = null; targetForm.value = { targetName: '', targetType: '', description: '', sortOrder: 0 }; showTargetModal.value = true }
function openEditTarget(t) { editTargetItem.value = t; targetForm.value = { targetName: t.targetName, targetType: t.targetType || '', description: t.description || '', sortOrder: t.sortOrder }; showTargetModal.value = true }
async function saveTarget() {
  saving.value = true
  try {
    if (editTargetItem.value) {
      const res = await internalAuditApi.updateTarget(editTargetItem.value.id, targetForm.value)
      const idx = auditDetail.value.targets.findIndex(x => x.id === editTargetItem.value.id)
      if (idx !== -1) auditDetail.value.targets[idx] = res.data
    } else {
      const res = await internalAuditApi.addTarget(auditDetail.value.id, targetForm.value)
      auditDetail.value.targets.push(res.data)
    }
    showTargetModal.value = false
  } catch (e) { alert(e || '저장 실패') } finally { saving.value = false }
}
async function deleteTarget(t) {
  if (!confirm('점검대상을 삭제하시겠습니까?')) return
  await internalAuditApi.deleteTarget(t.id)
  auditDetail.value.targets = auditDetail.value.targets.filter(x => x.id !== t.id)
}

// Items
function openAddItem() { editItemTarget.value = null; itemForm.value = { targetId: null, itemName: '', checkMethod: '', result: '', finding: '', actionRequired: '', sortOrder: 0 }; showItemModal.value = true }
function openEditItem(i) { editItemTarget.value = i; itemForm.value = { targetId: i.targetId, itemName: i.itemName, checkMethod: i.checkMethod || '', result: i.result || '', finding: i.finding || '', actionRequired: i.actionRequired || '', sortOrder: i.sortOrder }; showItemModal.value = true }
async function saveItem() {
  saving.value = true
  try {
    const payload = { ...itemForm.value, result: itemForm.value.result || null }
    if (editItemTarget.value) {
      const res = await internalAuditApi.updateItem(editItemTarget.value.id, payload)
      const idx = auditDetail.value.items.findIndex(x => x.id === editItemTarget.value.id)
      if (idx !== -1) auditDetail.value.items[idx] = res.data
    } else {
      const res = await internalAuditApi.addItem(auditDetail.value.id, payload)
      auditDetail.value.items.push(res.data)
    }
    showItemModal.value = false
  } catch (e) { alert(e || '저장 실패') } finally { saving.value = false }
}
async function deleteItem(i) {
  if (!confirm('점검항목을 삭제하시겠습니까?')) return
  await internalAuditApi.deleteItem(i.id)
  auditDetail.value.items = auditDetail.value.items.filter(x => x.id !== i.id)
}

// Files
function openAddAuditFile() { auditFileForm.value = { title: '', file: null }; showAuditFileModal.value = true }
function onAuditFileChange(e) { auditFileForm.value.file = e.target.files[0] || null }
async function saveAuditFile() {
  if (!auditFileForm.value.title.trim()) { alert('제목을 입력하세요.'); return }
  saving.value = true
  try {
    const res = await internalAuditApi.addFile(auditDetail.value.id, auditFileForm.value)
    auditDetail.value.files.push(res.data)
    showAuditFileModal.value = false
  } catch (e) { alert(e || '파일 추가 실패') } finally { saving.value = false }
}
function downloadAuditFile(f) { internalAuditApi.downloadFile(f.id, f.fileName) }
async function deleteAuditFile(f) {
  if (!confirm('파일을 삭제하시겠습니까?')) return
  await internalAuditApi.deleteFile(f.id)
  auditDetail.value.files = auditDetail.value.files.filter(x => x.id !== f.id)
}

function applyYear() {
  if (pickerYear.value > 0) {
    if (!yearOptions.value.includes(pickerYear.value)) {
      yearOptions.value = [pickerYear.value, ...yearOptions.value].sort((a, b) => b - a)
    }
    selectedYear.value = pickerYear.value
  }
  showYearPicker.value = false
}

function auditStatusBadge(s) {
  return { PLANNED: 'bg-blue-100 text-blue-700', IN_PROGRESS: 'bg-yellow-100 text-yellow-700', COMPLETED: 'bg-green-100 text-green-700' }[s] || 'bg-gray-100 text-gray-500'
}
function resultBadge(r) {
  return { PASS: 'bg-green-100 text-green-700', FAIL: 'bg-red-100 text-red-700', NA: 'bg-gray-100 text-gray-500' }[r] || 'bg-gray-100 text-gray-500'
}
function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}
</script>
