<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">권한관리 (RBAC)</h1>
        <p class="text-sm text-gray-400 mt-0.5">Role을 만들고 메뉴별 권한을 설정한 후 사용자를 배정합니다</p>
      </div>
      <button @click="openCreate" class="btn-primary flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Role 추가
      </button>
    </div>

    <div class="page-body">

    <!-- 기본 역할(ADMIN·MANAGER·USER) 권한 -->
    <section>
      <div class="flex items-baseline gap-2 mb-2">
        <h2 class="text-sm font-bold text-gray-800">기본 역할 권한</h2>
        <p class="text-xs text-gray-400">
          계정의 역할(ADMIN·MANAGER·USER)에 자동으로 적용되는 권한입니다. 아래 Role을 추가로 배정하면 권한이 합산(OR)됩니다.
        </p>
      </div>

      <div v-if="builtinLoading" class="card text-center py-6 text-sm text-gray-500">로딩 중...</div>
      <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-3">
        <div v-for="b in builtinRoles" :key="b.role" class="card !p-4 flex flex-col">
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <h3 class="text-sm font-bold text-gray-900">{{ b.label }}</h3>
              <p class="text-[11px] text-gray-400 mt-0.5">계정 {{ b.userCount }}명</p>
            </div>
            <button v-if="b.editable" @click="openBuiltinEdit(b)"
              class="text-xs px-2.5 py-1 rounded border border-blue-200 text-blue-600 hover:bg-blue-50 shrink-0">
              수정
            </button>
            <span v-else class="text-[11px] px-2 py-1 rounded bg-gray-100 text-gray-500 shrink-0">고정</span>
          </div>

          <p class="text-[11px] text-gray-500 mt-2 leading-relaxed">{{ b.description }}</p>

          <!-- 권한 요약 — 전체 허용이면 나열 대신 한 줄로 -->
          <div class="mt-3 pt-3 border-t border-gray-100">
            <div v-if="isAllMenus(b)" class="text-xs text-gray-600">
              <span class="font-semibold text-gray-800">전체 메뉴</span>
              <span class="text-green-600 font-medium ml-1.5">읽기</span>
              <span class="text-blue-600 font-medium ml-1">쓰기</span>
              <span class="text-red-600 font-medium ml-1">삭제</span>
            </div>
            <div v-else-if="b.permissions.length === 0" class="text-xs text-gray-400">
              설정된 권한 없음 — 이 역할 계정은 메뉴가 보이지 않습니다
            </div>
            <div v-else>
              <p class="text-[11px] text-gray-400 mb-1.5">
                허용 메뉴 {{ b.permissions.length }}개 / 전체 {{ MENUS.length }}개
                · 쓰기 {{ b.permissions.filter(p => p.canWrite).length }} · 삭제 {{ b.permissions.filter(p => p.canDelete).length }}
              </p>
              <div class="flex flex-wrap gap-1 max-h-24 overflow-y-auto">
                <span v-for="perm in b.permissions" :key="perm.menuKey"
                  class="inline-flex items-center gap-1 px-1.5 py-0.5 bg-gray-50 rounded text-[11px] border border-gray-100">
                  <span class="text-gray-700">{{ menuLabel(perm.menuKey) }}</span>
                  <span v-if="perm.canRead" class="text-green-600">R</span>
                  <span v-if="perm.canWrite" class="text-blue-600">W</span>
                  <span v-if="perm.canDelete" class="text-red-600">D</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <p class="text-[11px] text-gray-400 mt-2">
        기본 역할 권한은 <strong>화면(메뉴·버튼) 노출</strong>을 제어합니다. 서버 API는 별도로 역할 기반 검사를 수행하므로,
        MANAGER의 쓰기를 꺼도 API 자체가 차단되지는 않습니다.
      </p>
    </section>

    <!-- Role 목록 -->
    <h2 class="text-sm font-bold text-gray-800 !mt-6">사용자 지정 Role</h2>
    <div v-if="loading" class="card text-center py-10 text-gray-500">로딩 중...</div>
    <div v-else-if="roles.length === 0" class="card text-center py-10 text-gray-400">
      등록된 Role이 없습니다. Role을 추가해주세요.
    </div>
    <div v-else class="space-y-4">
      <div v-for="role in roles" :key="role.id" class="card">
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-3">
              <h3 class="text-base font-bold text-gray-900">{{ role.name }}</h3>
              <span class="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded font-medium">
                사용자 {{ role.userCount }}명
              </span>
            </div>
            <p v-if="role.description" class="text-sm text-gray-500 mt-1">{{ role.description }}</p>

            <!-- 메뉴 권한 표시 -->
            <div class="mt-3">
              <div v-if="role.permissions.length === 0" class="text-xs text-gray-400">설정된 권한 없음</div>
              <div v-else class="flex flex-wrap gap-2">
                <div v-for="perm in role.permissions" :key="perm.menuKey"
                  class="flex items-center gap-1 px-2 py-1 bg-gray-50 rounded text-xs border border-gray-100">
                  <span class="font-medium text-gray-700">{{ menuLabel(perm.menuKey) }}</span>
                  <span v-if="perm.canRead" class="text-green-600 font-medium">읽기</span>
                  <span v-if="perm.canWrite" class="text-blue-600 font-medium">쓰기</span>
                  <span v-if="perm.canDelete" class="text-red-600 font-medium">삭제</span>
                </div>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-2 ml-4 flex-shrink-0">
            <button @click="openUsers(role)"
              class="text-sm px-3 py-1.5 rounded border border-gray-200 text-gray-600 hover:bg-gray-50">
              사용자 배정
            </button>
            <button @click="openEdit(role)"
              class="text-sm px-3 py-1.5 rounded border border-blue-200 text-blue-600 hover:bg-blue-50">
              수정
            </button>
            <button @click="confirmDelete(role)"
              class="text-sm px-3 py-1.5 rounded border border-red-100 text-red-500 hover:bg-red-50">
              삭제
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Role 생성/수정 모달 -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div class="p-6">
          <h2 class="text-lg font-bold mb-4">
            {{ editingBuiltin ? `기본 역할 권한 수정 — ${editingBuiltin.label}` : (editingRole ? 'Role 수정' : 'Role 추가') }}
          </h2>

          <div class="space-y-4">
            <template v-if="!editingBuiltin">
              <div>
                <label class="text-sm font-medium text-gray-700">Role 이름 *</label>
                <input v-model="form.name" class="input w-full mt-1" placeholder="예: 보안담당자, 감사팀" />
              </div>
              <div>
                <label class="text-sm font-medium text-gray-700">설명</label>
                <input v-model="form.description" class="input w-full mt-1" placeholder="Role에 대한 설명 (선택)" />
              </div>
            </template>
            <div v-else class="rounded-lg bg-amber-50 border border-amber-100 px-3 py-2 text-xs text-amber-800">
              이 역할({{ editingBuiltin.role }})을 가진 <strong>{{ editingBuiltin.userCount }}명</strong>의 계정에 즉시 적용됩니다.
              해제한 메뉴는 화면에서 숨겨집니다.
            </div>

            <!-- 메뉴별 권한 -->
            <div>
              <div class="flex items-center justify-between mb-2">
                <label class="text-sm font-medium text-gray-700">메뉴별 권한</label>
                <div class="flex gap-1.5">
                  <button type="button" @click="setAllPerms(true)"
                    class="text-xs px-2 py-1 rounded border border-gray-200 text-gray-600 hover:bg-gray-50">전체 선택</button>
                  <button type="button" @click="setAllPerms(false)"
                    class="text-xs px-2 py-1 rounded border border-gray-200 text-gray-600 hover:bg-gray-50">전체 해제</button>
                </div>
              </div>
              <div class="border border-gray-200 rounded-lg overflow-hidden">
                <div class="overflow-x-auto"><table class="w-full text-sm">
                  <thead class="bg-gray-50">
                    <tr>
                      <th class="text-left px-4 py-2.5 text-xs font-semibold text-gray-600">메뉴</th>
                      <th class="text-center px-3 py-2.5 text-xs font-semibold text-gray-600 w-20">읽기</th>
                      <th class="text-center px-3 py-2.5 text-xs font-semibold text-gray-600 w-20">쓰기</th>
                      <th class="text-center px-3 py-2.5 text-xs font-semibold text-gray-600 w-20">삭제</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="menu in MENUS" :key="menu.key"
                      class="border-t border-gray-100 hover:bg-gray-50">
                      <td class="px-4 py-2.5 font-medium text-gray-700">{{ menu.label }}</td>
                      <td class="px-3 py-2.5 text-center">
                        <input type="checkbox" v-model="formPerms[menu.key].canRead"
                          class="w-4 h-4 rounded text-green-600 cursor-pointer"
                          @change="onReadChange(menu.key)" />
                      </td>
                      <td class="px-3 py-2.5 text-center">
                        <input type="checkbox" v-model="formPerms[menu.key].canWrite"
                          class="w-4 h-4 rounded text-blue-600 cursor-pointer"
                          @change="onWriteChange(menu.key)" />
                      </td>
                      <td class="px-3 py-2.5 text-center">
                        <input type="checkbox" v-model="formPerms[menu.key].canDelete"
                          class="w-4 h-4 rounded text-red-600 cursor-pointer"
                          @change="onDeleteChange(menu.key)" />
                      </td>
                    </tr>
                  </tbody>
                </table></div>
              </div>
              <p class="text-xs text-gray-400 mt-1.5">쓰기/삭제 권한 선택 시 읽기 권한이 자동으로 포함됩니다.</p>
            </div>
          </div>

          <div class="flex justify-end gap-2 mt-5">
            <button @click="showForm = false" class="btn-secondary">취소</button>
            <button @click="submitForm" :disabled="saving" class="btn-primary">
              {{ saving ? '저장 중...' : (editingRole || editingBuiltin ? '수정' : '추가') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 사용자 배정 모달 -->
    <div v-if="showUsers" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-xl max-h-[90vh] overflow-y-auto">
        <div class="p-6">
          <div class="flex items-center justify-between mb-1">
            <h2 class="text-lg font-bold">사용자 배정</h2>
            <button @click="showUsers = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <p class="text-sm text-gray-500 mb-4">Role: <strong>{{ selectedRole?.name }}</strong></p>

          <!-- 현재 배정된 사용자 -->
          <div class="mb-4">
            <h3 class="text-sm font-semibold text-gray-700 mb-2">배정된 사용자 ({{ assignedUsers.length }})</h3>
            <div v-if="assignedUsers.length === 0" class="text-xs text-gray-400 py-2">배정된 사용자가 없습니다</div>
            <div v-else class="space-y-1.5">
              <div v-for="u in assignedUsers" :key="u.id"
                class="flex items-center justify-between px-3 py-2 bg-blue-50 rounded-lg">
                <div>
                  <span class="text-sm font-medium text-gray-900">{{ u.name }}</span>
                  <span class="text-xs text-gray-500 ml-2">{{ u.email }}</span>
                  <span class="text-xs text-gray-400 ml-1">({{ u.role }})</span>
                </div>
                <button @click="removeUser(u.id)"
                  class="text-xs text-red-500 hover:text-red-700 px-2 py-0.5 rounded hover:bg-red-50">
                  제거
                </button>
              </div>
            </div>
          </div>

          <!-- 사용자 추가 -->
          <div>
            <h3 class="text-sm font-semibold text-gray-700 mb-2">사용자 추가</h3>
            <input v-model="userSearch" placeholder="이름 또는 이메일 검색..."
              class="input w-full text-sm mb-2" />
            <div class="space-y-1.5 max-h-48 overflow-y-auto">
              <div v-for="u in filteredAllUsers" :key="u.id"
                class="flex items-center justify-between px-3 py-2 border border-gray-100 rounded-lg hover:bg-gray-50">
                <div>
                  <span class="text-sm font-medium text-gray-900">{{ u.name }}</span>
                  <span class="text-xs text-gray-500 ml-2">{{ u.email }}</span>
                  <span class="text-xs text-gray-400 ml-1">({{ u.role }})</span>
                </div>
                <button v-if="!isAssigned(u.id)" @click="addUser(u.id)"
                  class="text-xs text-blue-600 hover:text-blue-800 px-2 py-0.5 rounded hover:bg-blue-50">
                  추가
                </button>
                <span v-else class="text-xs text-green-600">배정됨</span>
              </div>
            </div>
          </div>

          <div class="flex justify-end mt-4">
            <button @click="showUsers = false" class="btn-secondary">닫기</button>
          </div>
        </div>
      </div>
    </div>
    </div><!-- /page-body -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { rbacApi, adminApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const MENUS = [
  // 대시보드
  { key: 'dash_risks',        label: '[대시보드] 위험현황' },
  { key: 'dash_vulns',        label: '[대시보드] 취약점 현황' },
  { key: 'dash_incidents',    label: '[대시보드] 인시던트 현황' },
  { key: 'dash_isms',         label: '[대시보드] ISMS-P 이행률' },
  { key: 'dash_evidence',     label: '[대시보드] 증적 제출 현황' },
  // 정보보호 관리체계
  { key: 'policies',          label: '보안 정책' },
  { key: 'assets',            label: '자산 관리' },
  { key: 'sbom',              label: 'SBOM 관리' },
  { key: 'threats',           label: '위협 관리' },
  { key: 'vulnerabilities',   label: '취약점 관리' },
  { key: 'risk_assessment',   label: '위험평가' },
  { key: 'risk_treatment',    label: '위험처리 계획' },
  { key: 'isms_mapping',      label: 'ISMS-P 통제항목 매핑' },
  { key: 'isms',              label: 'ISMS-P 증적관리' },
  // 보안 운영
  { key: 'security_events',   label: '보안이벤트 관리' },
  { key: 'incidents',         label: '보안 인시던트' },
  { key: 'sec_findings',      label: '보안 결함사항' },
  { key: 'monthly_checks',    label: '월간 보안점검' },
  { key: 'source_scan',       label: '소스 취약점 점검 (SAST)' },
  { key: 'security_reviews',  label: '보안성 심의' },
  { key: 'operation_status',  label: '운영현황관리' },
  { key: 'emergency_contacts', label: '비상연락망' },
  // 로그 통합관리
  { key: 'log_personal_info', label: '개인정보처리시스템 로그' },
  { key: 'log_ad',            label: 'AD 로그' },
  { key: 'log_nac',           label: 'NAC 로그' },
  { key: 'log_network_link',  label: '망연계 로그' },
  { key: 'log_search',        label: '로그 통합검색' },
  // 보안 거버넌스
  { key: 'committee',         label: '정보보호위원회' },
  { key: 'internal_audit',    label: '내부감사' },
  // 교육 및 훈련 / 보안 가이드 및 자료
  { key: 'training',          label: 'IT 및 정보보호 교육' },
  { key: 'phishing',          label: '모의 악성메일 훈련' },
  { key: 'bcp_training',      label: '재해복구·BCP 훈련' },
  { key: 'sec_docs',          label: '보안 가이드 및 자료' },
  { key: 'glossary',          label: '보안용어집' },
  { key: 'related_sites',     label: '관련 사이트' },
  // 개인정보보호
  { key: 'privacy_processing', label: '[개인정보] 처리현황' },
  { key: 'privacy_files',      label: '[개인정보] 파일관리' },
  { key: 'privacy_consent',    label: '[개인정보] 수집·이용 관리' },
  { key: 'privacy_provision',  label: '[개인정보] 제공관리' },
  { key: 'privacy_contractors',label: '[개인정보] 수탁사 관리·점검·법령준수' },
  { key: 'privacy_retention',  label: '[개인정보] 보유기간 관리' },
  { key: 'privacy_disposal',   label: '[개인정보] 파기관리' },
  { key: 'privacy_dpia',       label: '[개인정보] 영향평가(DPIA)' },
  { key: 'privacy_breach',     label: '[개인정보] 유출관리' },
  { key: 'privacy_rights',     label: '[개인정보] 정보주체 권리행사' },
  { key: 'privacy_safeguard',  label: '[개인정보] 보호조치 관리' },
  { key: 'privacy_report',     label: '[개인정보] 현황보고서' },
]

const auth = useAuthStore()

const roles = ref([])
const loading = ref(true)
const saving = ref(false)

const builtinRoles = ref([])
const builtinLoading = ref(true)

const showForm = ref(false)
const showUsers = ref(false)
const editingRole = ref(null)      // 커스텀 Role 수정 중
const editingBuiltin = ref(null)   // 기본 역할 권한 수정 중 (둘은 동시에 설정되지 않는다)
const selectedRole = ref(null)

const form = ref({ name: '', description: '' })
const formPerms = ref({})

const assignedUsers = ref([])
const allUsers = ref([])
const userSearch = ref('')

function initFormPerms(existing = []) {
  const map = {}
  existing.forEach(p => { map[p.menuKey] = { canRead: p.canRead, canWrite: p.canWrite, canDelete: p.canDelete } })
  MENUS.forEach(m => {
    if (!map[m.key]) map[m.key] = { canRead: false, canWrite: false, canDelete: false }
  })
  return map
}

function onReadChange(key) {
  // 읽기 해제 시 쓰기/삭제도 해제
  if (!formPerms.value[key].canRead) {
    formPerms.value[key].canWrite = false
    formPerms.value[key].canDelete = false
  }
}

function onWriteChange(key) {
  // 쓰기 선택 시 읽기도 자동 선택
  if (formPerms.value[key].canWrite) formPerms.value[key].canRead = true
}

function onDeleteChange(key) {
  // 삭제 선택 시 읽기도 자동 선택
  if (formPerms.value[key].canDelete) formPerms.value[key].canRead = true
}

function setAllPerms(on) {
  MENUS.forEach(m => {
    formPerms.value[m.key] = { canRead: on, canWrite: on, canDelete: on }
  })
}

/** 전 메뉴 읽기·쓰기·삭제가 모두 허용된 상태인지 — 카드에서 목록 나열 대신 한 줄로 줄이기 위한 판정 */
function isAllMenus(b) {
  if (!b?.permissions || b.permissions.length < MENUS.length) return false
  const map = new Map(b.permissions.map(p => [p.menuKey, p]))
  return MENUS.every(m => {
    const p = map.get(m.key)
    return p && p.canRead && p.canWrite && p.canDelete
  })
}

async function load() {
  loading.value = true
  try {
    const res = await rbacApi.listRoles()
    roles.value = res.data || res
  } finally {
    loading.value = false
  }
}

async function loadBuiltin() {
  builtinLoading.value = true
  try {
    const res = await rbacApi.listBuiltinRoles()
    builtinRoles.value = res.data || res
  } catch (e) {
    builtinRoles.value = []
  } finally {
    builtinLoading.value = false
  }
}

function openCreate() {
  editingRole.value = null
  editingBuiltin.value = null
  form.value = { name: '', description: '' }
  formPerms.value = initFormPerms()
  showForm.value = true
}

function openEdit(role) {
  editingRole.value = role
  editingBuiltin.value = null
  form.value = { name: role.name, description: role.description || '' }
  formPerms.value = initFormPerms(role.permissions)
  showForm.value = true
}

function openBuiltinEdit(builtin) {
  editingRole.value = null
  editingBuiltin.value = builtin
  form.value = { name: builtin.label, description: builtin.description || '' }
  formPerms.value = initFormPerms(builtin.permissions)
  showForm.value = true
}

/** 체크된 메뉴만 추려 payload 로 만든다 (읽기·쓰기·삭제가 모두 꺼진 메뉴는 보내지 않는다) */
function collectPermissions() {
  return MENUS
    .filter(m => formPerms.value[m.key].canRead || formPerms.value[m.key].canWrite || formPerms.value[m.key].canDelete)
    .map(m => ({ menuKey: m.key, ...formPerms.value[m.key] }))
}

async function submitForm() {
  const permissions = collectPermissions()

  if (editingBuiltin.value) {
    if (permissions.length === 0 &&
        !confirm(`${editingBuiltin.value.label} 의 모든 메뉴 권한을 해제합니다.\n해당 역할 계정은 메뉴가 하나도 보이지 않게 됩니다. 계속할까요?`)) {
      return
    }
    saving.value = true
    try {
      await rbacApi.updateBuiltinRole(editingBuiltin.value.role, { permissions })
      showForm.value = false
      await loadBuiltin()
      // 관리자 본인이 MANAGER/USER 로 바뀌는 일은 없지만, 변경 결과를 즉시 반영하기 위해 재조회한다.
      await auth.loadPermissions()
    } catch (e) {
      alert(e || '저장에 실패했습니다.')
    } finally {
      saving.value = false
    }
    return
  }

  if (!form.value.name.trim()) return
  saving.value = true
  try {
    const payload = { name: form.value.name.trim(), description: form.value.description, permissions }

    if (editingRole.value) {
      await rbacApi.updateRole(editingRole.value.id, payload)
    } else {
      await rbacApi.createRole(payload)
    }
    showForm.value = false
    load()
  } catch (e) {
    alert(e || '저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function confirmDelete(role) {
  if (!confirm(`"${role.name}" Role을 삭제하시겠습니까?\n배정된 사용자의 Role도 해제됩니다.`)) return
  try {
    await rbacApi.deleteRole(role.id)
    load()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  }
}

async function openUsers(role) {
  selectedRole.value = role
  userSearch.value = ''
  try {
    const [usersRes, allRes] = await Promise.all([
      rbacApi.getRoleUsers(role.id),
      adminApi.listUsersSimple()
    ])
    assignedUsers.value = usersRes.data || usersRes
    allUsers.value = allRes.data || allRes
    showUsers.value = true
  } catch (e) {
    console.error(e)
  }
}

const filteredAllUsers = computed(() => {
  const q = userSearch.value.toLowerCase()
  return allUsers.value.filter(u =>
    !q || u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
  )
})

function isAssigned(userId) {
  return assignedUsers.value.some(u => u.id === userId)
}

async function addUser(userId) {
  try {
    await rbacApi.assignUser(selectedRole.value.id, userId)
    const res = await rbacApi.getRoleUsers(selectedRole.value.id)
    assignedUsers.value = res.data || res
    load()
  } catch (e) {
    alert(e || '추가에 실패했습니다.')
  }
}

async function removeUser(userId) {
  try {
    await rbacApi.removeUser(selectedRole.value.id, userId)
    assignedUsers.value = assignedUsers.value.filter(u => u.id !== userId)
    load()
  } catch (e) {
    alert(e || '제거에 실패했습니다.')
  }
}

function menuLabel(key) {
  return MENUS.find(m => m.key === key)?.label || key
}

onMounted(() => { load(); loadBuiltin() })
</script>
