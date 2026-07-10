import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res.data,
  err => {
    const status = err.response?.status
    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
      router.push('/login')
    }
    if (status >= 500) {
      return Promise.reject(null)
    }
    return Promise.reject(err.response?.data?.message || null)
  }
)

const getLang = () => localStorage.getItem('locale') || 'ko'

export const authApi = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  me: () => api.get('/auth/me'),
  changePassword: (data) => api.patch('/auth/password', data),
  refresh: () => api.post('/auth/refresh'),
  mfaStatus: () => api.get('/auth/mfa/status'),
  mfaSetup: () => api.post('/auth/mfa/setup'),
  mfaEnable: (code) => api.post('/auth/mfa/enable', { code }),
  mfaDisable: (code) => api.post('/auth/mfa/disable', { code }),
  mfaVerify: (tempToken, code) => api.post('/auth/mfa/verify', { tempToken, code }),
  oktaConfig: () => api.get('/auth/okta/config'),
  oktaToken: (data) => api.post('/auth/okta/token', data),
  oktaTest: () => api.get('/admin/okta/test')
}

export const securityConfigApi = {
  get: () => api.get('/admin/security-config'),
  update: (data) => api.put('/admin/security-config', data)
}

export const policyApi = {
  list: (params) => api.get('/policies', { params }),
  get: (id) => api.get(`/policies/${id}`),
  create: (data) => api.post('/policies', data),
  update: (id, data) => api.patch(`/policies/${id}`, data),
  delete: (id) => api.delete(`/policies/${id}`),
  acknowledge: (id) => api.post(`/policies/${id}/acknowledge`)
}

export const vulnApi = {
  list: (params) => api.get('/vulnerabilities', { params }),
  stats: () => api.get('/vulnerabilities/stats'),
  get: (id) => api.get(`/vulnerabilities/${id}`),
  create: (data) => api.post('/vulnerabilities', data),
  update: (id, data) => api.patch(`/vulnerabilities/${id}`, data),
  delete: (id) => api.delete(`/vulnerabilities/${id}`)
}

export const nvdApi = {
  lookup: (cveId) => api.get(`/nvd/cve/${encodeURIComponent(cveId)}`)
}

export const trainingApi = {
  list: (params) => api.get('/training/courses', { params }),
  get: (id) => api.get(`/training/courses/${id}`),
  create: (data) => api.post('/training/courses', data),
  update: (id, data) => api.patch(`/training/courses/${id}`, data),
  submitQuiz: (id, data) => api.post(`/training/courses/${id}/submit`, data),
  delete: (id) => api.delete(`/training/courses/${id}`),
  results: () => api.get('/training/results'),
  resultCompletions: (params) => api.get('/training/results/completions', { params })
}

export const quizBankApi = {
  list: (params) => api.get('/quiz-bank', { params }),
  categories: () => api.get('/quiz-bank/categories'),
  categoryStats: () => api.get('/quiz-bank/categories/stats'),
  deleteByCategory: (category) => api.delete('/quiz-bank/by-category', { params: { category: category ?? '' } }),
  create: (data) => api.post('/quiz-bank', data),
  update: (id, data) => api.patch(`/quiz-bank/${id}`, data),
  delete: (id) => api.delete(`/quiz-bank/${id}`),
  template: () => downloadBlob('/quiz-bank/bulk/template', 'quiz-bank-template.xlsx'),
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/quiz-bank/bulk', fd)
  }
}

export const vulnCommentApi = {
  list: (vulnId) => api.get(`/vulnerabilities/${vulnId}/comments`),
  add: (vulnId, data) => api.post(`/vulnerabilities/${vulnId}/comments`, data)
}

export const adminApi = {
  listUsers: (params) => api.get('/admin/users', { params }),
  listUsersSimple: () => api.get('/admin/users/simple'),
  createUser: (data) => api.post('/admin/users', data),
  updateUser: (id, data) => api.patch(`/admin/users/${id}`, data),
  deleteUser: (id) => api.delete(`/admin/users/${id}`),
  hardDeleteUser: (id) => api.delete(`/admin/users/${id}/permanent`),
  unlockUser: (id) => api.post(`/admin/users/${id}/unlock`),
  listAuditLogs: (params) => api.get('/admin/audit-logs', { params }),

  listCodeGroups: () => api.get('/admin/codes'),
  createCodeGroup: (data) => api.post('/admin/codes', data),
  updateCodeGroup: (groupCode, data) => api.patch(`/admin/codes/${groupCode}`, data),
  deleteCodeGroup: (groupCode) => api.delete(`/admin/codes/${groupCode}`),
  listCodeValues: (groupCode) => api.get(`/admin/codes/${groupCode}/values`),
  createCodeValue: (groupCode, data) => api.post(`/admin/codes/${groupCode}/values`, data),
  updateCodeValue: (groupCode, id, data) => api.patch(`/admin/codes/${groupCode}/values/${id}`, data),
  deleteCodeValue: (groupCode, id) => api.delete(`/admin/codes/${groupCode}/values/${id}`)
}

export const codeApi = {
  getValues: (groupCode) => api.get(`/codes/${groupCode}`)
}

export const reportApi = {
  vulnerabilityPdf: () => api.get('/reports/vulnerabilities/pdf', { responseType: 'blob' })
}

export const assetApi = {
  list: (params) => api.get('/assets', { params }),
  get: (id) => api.get(`/assets/${id}`),
  typeStats: () => api.get('/assets/types/stats'),
  deleteByType: (type) => api.delete('/assets/by-type', { params: { type: type ?? '' } }),
  create: (data) => api.post('/assets', data),
  update: (id, data) => api.patch(`/assets/${id}`, data),
  delete: (id) => api.delete(`/assets/${id}`),
  // 시점(스냅샷) 이력
  listSnapshots: () => api.get('/assets/snapshots'),
  createSnapshot: (data) => api.post('/assets/snapshots', data),
  snapshotItems: (id) => api.get(`/assets/snapshots/${id}/items`),
  deleteSnapshot: (id) => api.delete(`/assets/snapshots/${id}`)
}

export const sbomApi = {
  list: (params) => api.get('/sbom/software', { params }),
  listAll: () => api.get('/sbom/software/all'),
  get: (id) => api.get(`/sbom/software/${id}`),
  create: (data) => api.post('/sbom/software', data),
  update: (id, data) => api.patch(`/sbom/software/${id}`, data),
  delete: (id) => api.delete(`/sbom/software/${id}`),
  addComponent: (id, data) => api.post(`/sbom/software/${id}/components`, data),
  updateComponent: (componentId, data) => api.patch(`/sbom/components/${componentId}`, data),
  deleteComponent: (componentId) => api.delete(`/sbom/components/${componentId}`),
  template: () => downloadBlob('/sbom/bulk/template', 'sbom-upload-template.xlsx'),
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/sbom/bulk', fd)
  },
  exportCdx: (id, name, version) =>
    downloadBlob(`/sbom/software/${id}/cyclonedx`, `${name}-${version}.cdx.json`.replace(/[\\/:*?"<>|\s]+/g, '_')),
  importCdx: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/sbom/import/cyclonedx', fd)
  }
}

export const metricsApi = {
  summary: () => api.get('/metrics/summary')
}

export const notificationConfigApi = {
  get: () => api.get('/admin/notification-config'),
  update: (data) => api.put('/admin/notification-config', data)
}

export const incidentApi = {
  list: (params) => api.get('/incidents', { params }),
  get: (id) => api.get(`/incidents/${id}`),
  create: (data) => api.post('/incidents', data),
  update: (id, data) => api.patch(`/incidents/${id}`, data),
  delete: (id) => api.delete(`/incidents/${id}`)
}

export const noticeApi = {
  listActive: () => api.get('/notices'),
  listAll: () => api.get('/notices/all'),
  create: (data) => api.post('/notices', data),
  update: (id, data) => api.put(`/notices/${id}`, data),
  delete: (id) => api.delete(`/notices/${id}`)
}

export const inboxApi = {
  list: () => api.get('/inbox'),
  unreadCount: () => api.get('/inbox/unread-count'),
  markRead: (id) => api.patch(`/inbox/${id}/read`),
  markAllRead: () => api.patch('/inbox/read-all'),
  deleteAll: () => api.delete('/inbox'),
  approve: (id) => api.post(`/inbox/${id}/approve`),
  reject: (id) => api.post(`/inbox/${id}/reject`)
}

export const rssApi = {
  krcert: () => api.get('/rss/krcert')
}

export const securityIntegrationApi = {
  list: () => api.get('/security-integrations'),
  create: (data) => api.post('/security-integrations', data),
  update: (id, data) => api.patch(`/security-integrations/${id}`, data),
  delete: (id) => api.delete(`/security-integrations/${id}`),
  listEvents: (id, params) => api.get(`/security-integrations/${id}/events`, { params }),
  createEvent: (id, data) => api.post(`/security-integrations/${id}/events`, data),
  deleteEvent: (eventId) => api.delete(`/security-integrations/events/${eventId}`)
}

const downloadBlob = (url, filename, params) =>
  api.get(url, { responseType: 'blob', params }).then(blob => {
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = filename
    document.body.appendChild(a)
    a.click()
    setTimeout(() => {
      URL.revokeObjectURL(a.href)
      document.body.removeChild(a)
    }, 100)
  })

const today = () => new Date().toISOString().slice(0, 10).replace(/-/g, '')

export const exportApi = {
  // Policy
  policyCsv: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `보안정책-${today()}.csv` : `policies-${today()}.csv`
    return downloadBlob('/reports/policies/csv', name, { lang })
  },
  policyPdf: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `보안정책-보고서-${today()}.pdf` : `security-policy-report-${today()}.pdf`
    return downloadBlob('/reports/policies/pdf', name, { lang })
  },
  // Vulnerability
  vulnCsv: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `취약점관리-${today()}.csv` : `vulnerabilities-${today()}.csv`
    return downloadBlob('/reports/vulnerabilities/csv', name, { lang })
  },
  vulnPdf: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `취약점-보고서-${today()}.pdf` : `vulnerability-report-${today()}.pdf`
    return downloadBlob('/reports/vulnerabilities/pdf', name, { lang })
  },
  // Asset
  assetCsv: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `자산관리-${today()}.csv` : `assets-${today()}.csv`
    return downloadBlob('/reports/assets/csv', name, { lang })
  },
  assetPdf: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `자산관리-보고서-${today()}.pdf` : `asset-management-report-${today()}.pdf`
    return downloadBlob('/reports/assets/pdf', name, { lang })
  },
  // Incident
  incidentCsv: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `보안인시던트-${today()}.csv` : `incidents-${today()}.csv`
    return downloadBlob('/reports/incidents/csv', name, { lang })
  },
  incidentPdf: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `보안인시던트-보고서-${today()}.pdf` : `security-incident-report-${today()}.pdf`
    return downloadBlob('/reports/incidents/pdf', name, { lang })
  },
  // User
  userCsv: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `사용자관리-${today()}.csv` : `users-${today()}.csv`
    return downloadBlob('/reports/users/csv', name, { lang })
  },
  userPdf: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `사용자관리-보고서-${today()}.pdf` : `user-management-report-${today()}.pdf`
    return downloadBlob('/reports/users/pdf', name, { lang })
  },
  // ISMS
  ismsPdf: (year) => {
    const lang = getLang()
    const name = lang === 'ko'
      ? `ISMS-P-증적관리-${year}-보고서-${today()}.pdf`
      : `isms-p-evidence-report-${year}-${today()}.pdf`
    return downloadBlob('/reports/isms/pdf', name, { year, lang })
  },
  // Training
  trainingPdf: () => {
    const lang = getLang()
    const name = lang === 'ko' ? `교육이수-보고서-${today()}.pdf` : `training-report-${today()}.pdf`
    return downloadBlob('/reports/training/pdf', name, { lang })
  },
  // Asset bulk template
  assetTemplate: () => downloadBlob('/assets/bulk/template', 'asset-upload-template.xlsx')
}

export const ismsApi = {
  listItems: (params) => api.get('/isms/items', { params }),
  getItem: (id) => api.get(`/isms/items/${id}`),
  mapPolicy: (itemId, policyId) => api.post(`/isms/items/${itemId}/policies/${policyId}`),
  unmapPolicy: (itemId, policyId) => api.delete(`/isms/items/${itemId}/policies/${policyId}`),
  listEvidences: (itemId, year) => api.get(`/isms/items/${itemId}/evidences`, { params: { year } }),
  createEvidence: (itemId, { year, title, content, status, file }) => {
    const fd = new FormData()
    fd.append('year', year)
    fd.append('title', title)
    if (content) fd.append('content', content)
    if (status) fd.append('status', status)
    if (file) fd.append('file', file)
    return api.post(`/isms/items/${itemId}/evidences`, fd)
  },
  updateEvidence: (evidenceId, { title, content, status, file }) => {
    const fd = new FormData()
    if (title !== undefined) fd.append('title', title)
    if (content !== undefined) fd.append('content', content)
    if (status !== undefined) fd.append('status', status)
    if (file) fd.append('file', file)
    return api.patch(`/isms/evidences/${evidenceId}`, fd)
  },
  deleteEvidence: (evidenceId) => api.delete(`/isms/evidences/${evidenceId}`),
  downloadFile: (evidenceId, fileName) => downloadBlob(`/isms/evidences/${evidenceId}/file`, fileName),
  removeFile: (evidenceId) => api.delete(`/isms/evidences/${evidenceId}/file`),
  searchEvidences: (params) => api.get('/isms/evidences/search', { params }),
  createEvidenceRef: (itemId, data) => api.post(`/isms/items/${itemId}/evidence-refs`, data),
  summary: (year) => api.get('/isms/summary', { params: { year } }),
  exportCsv: (year) => {
    const lang = getLang()
    const name = lang === 'ko' ? `ISMS-P-증적-${year}.csv` : `isms-evidences-${year}.csv`
    return downloadBlob(`/isms/export/csv?year=${year}`, name)
  },
  importTemplate: () => downloadBlob('/isms/import/template', 'isms_import_template.xlsx'),
  bulkImport: (year, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/isms/import', fd, { params: { year } })
  }
}

export const monthlyCheckApi = {
  list: (yearMonth) => api.get('/monthly-checks', { params: { yearMonth } }),
  summary: (yearMonth) => api.get('/monthly-checks/summary', { params: { yearMonth } }),
  months: () => api.get('/monthly-checks/months'),
  create: (data) => api.post('/monthly-checks', data),
  update: (id, data) => api.patch(`/monthly-checks/${id}`, data),
  delete: (id) => api.delete(`/monthly-checks/${id}`),
  clearAll: (yearMonth) => api.delete('/monthly-checks', { params: { yearMonth } }),
  checkDefaults: (yearMonth) => api.get('/monthly-checks/defaults/check', { params: { yearMonth } }),
  loadDefaults: (yearMonth) => api.post('/monthly-checks/defaults', null, { params: { yearMonth } }),

  listEvidences: (itemId) => api.get(`/monthly-checks/${itemId}/evidences`),
  createEvidence: (itemId, { title, content, file }) => {
    const fd = new FormData()
    fd.append('title', title)
    if (content) fd.append('content', content)
    if (file) fd.append('file', file)
    return api.post(`/monthly-checks/${itemId}/evidences`, fd)
  },
  updateEvidence: (evidenceId, { title, content, file }) => {
    const fd = new FormData()
    if (title !== undefined) fd.append('title', title)
    if (content !== undefined) fd.append('content', content)
    if (file) fd.append('file', file)
    return api.patch(`/monthly-checks/evidences/${evidenceId}`, fd)
  },
  deleteEvidence: (evidenceId) => api.delete(`/monthly-checks/evidences/${evidenceId}`),
  downloadEvidenceFile: (evidenceId, fileName) =>
    downloadBlob(`/monthly-checks/evidences/${evidenceId}/file`, fileName)
}

export const rbacApi = {
  myPermissions: () => api.get('/auth/my-permissions'),
  listRoles: () => api.get('/admin/roles'),
  getRole: (id) => api.get(`/admin/roles/${id}`),
  createRole: (data) => api.post('/admin/roles', data),
  updateRole: (id, data) => api.put(`/admin/roles/${id}`, data),
  deleteRole: (id) => api.delete(`/admin/roles/${id}`),
  getRoleUsers: (id) => api.get(`/admin/roles/${id}/users`),
  assignUser: (roleId, userId) => api.post(`/admin/roles/${roleId}/users/${userId}`),
  removeUser: (roleId, userId) => api.delete(`/admin/roles/${roleId}/users/${userId}`)
}

export const secDocApi = {
  list: (params) => api.get('/sec-docs', { params }),
  get: (id) => api.get(`/sec-docs/${id}`),
  versions: (id) => api.get(`/sec-docs/${id}/versions`),
  create: (data, file) => {
    const fd = new FormData()
    fd.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    if (file) fd.append('file', file)
    return api.post('/sec-docs', fd)
  },
  addVersion: (id, data, file) => {
    const fd = new FormData()
    fd.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    if (file) fd.append('file', file)
    return api.post(`/sec-docs/${id}/versions`, fd)
  },
  update: (id, data) => api.patch(`/sec-docs/${id}`, data),
  delete: (id) => api.delete(`/sec-docs/${id}`),
  deleteVersion: (id) => api.delete(`/sec-docs/${id}/version`),
  download: (id, fileName) => downloadBlob(`/sec-docs/${id}/download`, fileName)
}

export const committeeApi = {
  years: () => api.get('/committee/years'),
  list: (year) => api.get('/committee', { params: { year } }),
  get: (id) => api.get(`/committee/${id}`),
  create: (data) => api.post('/committee', data),
  update: (id, data) => api.patch(`/committee/${id}`, data),
  delete: (id) => api.delete(`/committee/${id}`),
  addFile: (meetingId, { fileType, title, file }) => {
    const fd = new FormData()
    fd.append('fileType', fileType)
    fd.append('title', title)
    if (file) fd.append('file', file)
    return api.post(`/committee/${meetingId}/files`, fd)
  },
  deleteFile: (fileId) => api.delete(`/committee/files/${fileId}`),
  downloadFile: (fileId, fileName) => downloadBlob(`/committee/files/${fileId}/download`, fileName)
}

export const internalAuditApi = {
  years: () => api.get('/internal-audit/years'),
  list: (year) => api.get('/internal-audit', { params: { year } }),
  get: (id) => api.get(`/internal-audit/${id}`),
  create: (data) => api.post('/internal-audit', data),
  update: (id, data) => api.patch(`/internal-audit/${id}`, data),
  delete: (id) => api.delete(`/internal-audit/${id}`),
  addTarget: (auditId, data) => api.post(`/internal-audit/${auditId}/targets`, data),
  updateTarget: (targetId, data) => api.patch(`/internal-audit/targets/${targetId}`, data),
  deleteTarget: (targetId) => api.delete(`/internal-audit/targets/${targetId}`),
  addItem: (auditId, data) => api.post(`/internal-audit/${auditId}/items`, data),
  updateItem: (itemId, data) => api.patch(`/internal-audit/items/${itemId}`, data),
  deleteItem: (itemId) => api.delete(`/internal-audit/items/${itemId}`),
  addFile: (auditId, { title, file }) => {
    const fd = new FormData()
    fd.append('title', title)
    if (file) fd.append('file', file)
    return api.post(`/internal-audit/${auditId}/files`, fd)
  },
  deleteFile: (fileId) => api.delete(`/internal-audit/files/${fileId}`),
  downloadFile: (fileId, fileName) => downloadBlob(`/internal-audit/files/${fileId}/download`, fileName)
}

export const secFindingApi = {
  years: () => api.get('/security-findings/years'),
  list: (params) => api.get('/security-findings', { params }),
  get: (id) => api.get(`/security-findings/${id}`),
  create: (data, file) => {
    const fd = new FormData()
    fd.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    if (file) fd.append('file', file)
    return api.post('/security-findings', fd)
  },
  update: (id, data, file) => {
    const fd = new FormData()
    fd.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    if (file) fd.append('file', file)
    return api.patch(`/security-findings/${id}`, fd)
  },
  delete: (id) => api.delete(`/security-findings/${id}`),
  download: (id, fileName) => downloadBlob(`/security-findings/${id}/download`, fileName)
}

export const assetBulkApi = {
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/assets/bulk', fd)
  }
}

export const policyBulkApi = {
  template: () => downloadBlob('/policies/bulk/template', 'policy-upload-template.xlsx'),
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/policies/bulk', fd)
  }
}

export const vulnBulkApi = {
  template: () => downloadBlob('/vulnerabilities/bulk/template', 'vulnerability-upload-template.xlsx'),
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/vulnerabilities/bulk', fd)
  }
}

export const incidentBulkApi = {
  template: () => downloadBlob('/incidents/bulk/template', 'incident-upload-template.xlsx'),
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/incidents/bulk', fd)
  }
}

export const userBulkApi = {
  template: () => downloadBlob('/admin/users/bulk/template', 'user-upload-template.xlsx'),
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/admin/users/bulk', fd)
  }
}

export const phishingApi = {
  listTemplates:  ()         => api.get('/phishing/templates'),
  createTemplate: (data)     => api.post('/phishing/templates', data),
  updateTemplate: (id, data) => api.patch(`/phishing/templates/${id}`, data),
  deleteTemplate: (id)       => api.delete(`/phishing/templates/${id}`),

  listTargets:  ()         => api.get('/phishing/targets'),
  createTarget: (data)     => api.post('/phishing/targets', data),
  updateTarget: (id, data) => api.patch(`/phishing/targets/${id}`, data),
  toggleTarget: (id)       => api.patch(`/phishing/targets/${id}/toggle`),
  deleteTarget: (id)       => api.delete(`/phishing/targets/${id}`),

  listCampaigns:    ()   => api.get('/phishing/campaigns'),
  getCampaign:      (id) => api.get(`/phishing/campaigns/${id}`),
  createCampaign:   (data) => api.post('/phishing/campaigns', data),
  launchCampaign:   (id) => api.post(`/phishing/campaigns/${id}/launch`),
  completeCampaign: (id) => api.post(`/phishing/campaigns/${id}/complete`),
  cancelCampaign:   (id) => api.post(`/phishing/campaigns/${id}/cancel`),
  deleteCampaign:   (id) => api.delete(`/phishing/campaigns/${id}`),
}

export const contractorCheckItemApi = {
  list: () => api.get('/privacy/contractor-check-items'),
  create: (data) => api.post('/privacy/contractor-check-items', data),
  update: (id, data) => api.patch(`/privacy/contractor-check-items/${id}`, data),
  delete: (id) => api.delete(`/privacy/contractor-check-items/${id}`),
  checkDefaults: () => api.get('/privacy/contractor-check-items/defaults/check'),
  loadDefaults: () => api.post('/privacy/contractor-check-items/defaults'),
  listDefaults: () => api.get('/privacy/contractor-check-items/defaults'),
  createDefault: (data) => api.post('/privacy/contractor-check-items/defaults/item', data),
  updateDefault: (id, data) => api.patch(`/privacy/contractor-check-items/defaults/${id}`, data),
  deleteDefault: (id) => api.delete(`/privacy/contractor-check-items/defaults/${id}`),
  reset: () => api.post('/privacy/contractor-check-items/reset'),
}

export const contractorCheckApi = {
  years: () => api.get('/privacy/contractor-checks/years'),
  listByYear: (year) => api.get('/privacy/contractor-checks', { params: { year } }),
  listByContractor: (contractorId) => api.get(`/privacy/contractor-checks/contractor/${contractorId}`),
  createOrGet: (data) => api.post('/privacy/contractor-checks', data),
  update: (id, data) => api.patch(`/privacy/contractor-checks/${id}`, data),
  delete: (id) => api.delete(`/privacy/contractor-checks/${id}`),
  getDetail: (id) => api.get(`/privacy/contractor-checks/${id}`),
  saveResult: (id, data) => api.put(`/privacy/contractor-checks/${id}/results`, data),
  bulkResult: (id, result) => api.post(`/privacy/contractor-checks/${id}/results/bulk`, { result }),
  syncResults: (id) => api.post(`/privacy/contractor-checks/${id}/sync`),
}

export const threatApi = {
  list: () => api.get('/threats'),
  get: (id) => api.get(`/threats/${id}`),
  create: (data) => api.post('/threats', data),
  update: (id, data) => api.patch(`/threats/${id}`, data),
  delete: (id) => api.delete(`/threats/${id}`),
  checkDefaults: () => api.get('/threats/defaults/check'),
  loadDefaults: () => api.post('/threats/defaults'),
  reset: () => api.post('/threats/reset'),
}

export const riskApi = {
  listYears: () => api.get('/risk/years'),
  listRounds: (year) => api.get('/risk/rounds', { params: { year } }),
  createRound: (data) => api.post('/risk/rounds', data),
  updateRound: (id, data) => api.patch(`/risk/rounds/${id}`, data),
  deleteRound: (id) => api.delete(`/risk/rounds/${id}`),
  listAssessments: (roundId, params) => api.get(`/risk/rounds/${roundId}/assessments`, { params }),
  exportExcel: (roundId) => api.get(`/risk/rounds/${roundId}/export/excel`, { responseType: 'blob' }),
  autoPopulate: (roundId) => api.post(`/risk/rounds/${roundId}/auto-populate`),
  createAssessment: (data) => api.post('/risk/assessments', data),
  updateAssessment: (id, data) => api.patch(`/risk/assessments/${id}`, data),
  deleteAssessment: (id) => api.delete(`/risk/assessments/${id}`),
  bulkUpdateTreatment: (ids, treatment) => api.patch('/risk/assessments/bulk-treatment', { ids, treatment }),
  // 위험 처리 계획 (완료 차수 '감소' 항목)
  listTreatmentPlans: (roundId) => api.get(`/risk/rounds/${roundId}/treatment-plans`),
  updateTreatmentPlan: (id, data) => api.patch(`/risk/assessments/${id}/treatment-plan`, data),
}

export const monthlyCheckDefaultApi = {
  list: () => api.get('/monthly-checks/default-items'),
  create: (data) => api.post('/monthly-checks/default-items', data),
  update: (id, data) => api.patch(`/monthly-checks/default-items/${id}`, data),
  delete: (id) => api.delete(`/monthly-checks/default-items/${id}`),
}

export const threatDefaultApi = {
  list: (page = 0, size = 20) => api.get('/threats/defaults', { params: { page, size } }),
  create: (data) => api.post('/threats/defaults/item', data),
  update: (id, data) => api.patch(`/threats/defaults/${id}`, data),
  delete: (id) => api.delete(`/threats/defaults/${id}`),
}

export const appSettingApi = {
  getAll: () => api.get('/public/app-settings'),
  update: (key, value) => api.put(`/admin/app-settings/${key}`, { value }),
}

export const aiApi = {
  getConfig:      () => api.get('/admin/ai-config'),
  saveConfig:     (data) => api.put('/admin/ai-config', data),
  testConnection: () => api.post('/admin/ai-config/test'),
  analyze:        (data) => api.post('/ai/analyze', data, { timeout: 120000 }),
}

export const githubConfigApi = {
  get: () => api.get('/admin/github-config'),
  save: (data) => api.put('/admin/github-config', data),
  test: () => api.post('/admin/github-config/test'),
}

export const sourceScanApi = {
  repos: () => api.get('/source-scan/repos'),
  run: (repository) => api.post('/source-scan/run', { repository }, { timeout: 120000 }),
  scans: (params) => api.get('/source-scan/scans', { params }),
  scan: (id) => api.get(`/source-scan/scans/${id}`),
  delete: (id) => api.delete(`/source-scan/scans/${id}`),
}

export const backupApi = {
  download: (password) =>
    api.post('/admin/backup/download', { password }, { responseType: 'blob' }),
  save: (password) => api.post('/admin/backup/save', { password }),
  restore: (file, password) => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('password', password)
    return api.post('/admin/backup/restore', fd)
  },
  listFiles: () => api.get('/admin/backup/files'),
  downloadFile: (filename) => downloadBlob(`/admin/backup/files/${encodeURIComponent(filename)}/download`, filename),
  deleteFile: (filename) => api.delete(`/admin/backup/files/${encodeURIComponent(filename)}`),
  listHistory: () => api.get('/admin/backup/history'),
  getConfig: () => api.get('/admin/backup/config'),
  updateConfig: (data) => api.put('/admin/backup/config', data),
}

export default api
