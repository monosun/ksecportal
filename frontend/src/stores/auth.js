import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, rbacApi } from '@/api'

function parseTokenExpiry(jwt) {
  if (!jwt) return null
  try {
    const payload = JSON.parse(atob(jwt.split('.')[1]))
    return payload.exp ? payload.exp * 1000 : null
  } catch { return null }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token'))
  const tokenExpiresAt = ref(parseTokenExpiry(localStorage.getItem('token')))
  const permissions = ref({})
  // 메뉴 권한 검사를 건너뛰고 전체를 허용해야 하는 상태.
  // ADMIN 이거나, 서버에 기본 역할 권한 행이 없어 판단 근거가 없을 때(안전 폴백) true.
  const fullAccess = ref(true)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role))

  // MANAGER 도 기본 역할 권한(권한관리 > 기본 역할)에 따라 메뉴가 결정된다.
  // ADMIN 만 무조건 전체 허용이며, 그 외에는 서버가 합산해 준 permissions 를 따른다.
  function canRead(menuKey) {
    if (isAdmin.value || fullAccess.value) return true
    return !!permissions.value[menuKey]?.canRead
  }

  function canWrite(menuKey) {
    if (isAdmin.value || fullAccess.value) return true
    return !!permissions.value[menuKey]?.canWrite
  }

  function canDelete(menuKey) {
    if (isAdmin.value || fullAccess.value) return true
    return !!permissions.value[menuKey]?.canDelete
  }

  async function loadPermissions() {
    if (!token.value) return
    try {
      const res = await rbacApi.myPermissions()
      const data = res.data || res
      fullAccess.value = !!(data.isAdmin || data.fullAccess)
      permissions.value = fullAccess.value ? {} : (data.permissions || {})
    } catch (e) {
      // 권한 조회 실패 시 화면이 통째로 사라지지 않도록 전체 허용으로 폴백한다(서버가 최종 방어).
      fullAccess.value = true
      permissions.value = {}
    }
  }

  function _applyToken(accessToken, expiresIn) {
    token.value = accessToken
    localStorage.setItem('token', accessToken)
    tokenExpiresAt.value = expiresIn ? Date.now() + expiresIn : parseTokenExpiry(accessToken)
  }

  async function login(credentials) {
    const res = await authApi.login(credentials)
    if (res.data.mfaRequired) {
      return { mfaRequired: true, tempToken: res.data.tempToken }
    }
    _applyToken(res.data.accessToken, res.data.expiresIn)
    user.value = res.data.user
    await loadPermissions()
    return { mfaRequired: false }
  }

  async function oktaLogin(code, codeVerifier) {
    const res = await authApi.oktaToken({ code, codeVerifier })
    _applyToken(res.data.accessToken, res.data.expiresIn)
    user.value = res.data.user
    await loadPermissions()
  }

  async function verifyMfa(tempToken, code) {
    const res = await authApi.mfaVerify(tempToken, code)
    _applyToken(res.data.accessToken, res.data.expiresIn)
    user.value = res.data.user
    await loadPermissions()
  }

  async function refreshToken() {
    const res = await authApi.refresh()
    _applyToken(res.data.accessToken, res.data.expiresIn)
    user.value = res.data.user
  }

  async function fetchMe() {
    if (!token.value) return
    const res = await authApi.me()
    user.value = res.data
    await loadPermissions()
  }

  function logout() {
    token.value = null
    user.value = null
    tokenExpiresAt.value = null
    permissions.value = {}
    fullAccess.value = true
    localStorage.removeItem('token')
  }

  return {
    user, token, tokenExpiresAt, permissions, fullAccess,
    isAuthenticated, isAdmin, isManager,
    canRead, canWrite, canDelete,
    login, oktaLogin, verifyMfa, fetchMe, logout, loadPermissions, refreshToken
  }
})
