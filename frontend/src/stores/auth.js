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

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role))

  function canRead(menuKey) {
    if (isAdmin.value || isManager.value) return true
    return !!permissions.value[menuKey]?.canRead
  }

  function canWrite(menuKey) {
    if (isAdmin.value || isManager.value) return true
    return !!permissions.value[menuKey]?.canWrite
  }

  function canDelete(menuKey) {
    if (isAdmin.value || isManager.value) return true
    return !!permissions.value[menuKey]?.canDelete
  }

  async function loadPermissions() {
    if (!token.value) return
    try {
      const res = await rbacApi.myPermissions()
      const data = res.data || res
      permissions.value = data.isAdmin ? {} : (data.permissions || {})
    } catch (e) {
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
    localStorage.removeItem('token')
  }

  return {
    user, token, tokenExpiresAt, permissions,
    isAuthenticated, isAdmin, isManager,
    canRead, canWrite, canDelete,
    login, oktaLogin, verifyMfa, fetchMe, logout, loadPermissions, refreshToken
  }
})
