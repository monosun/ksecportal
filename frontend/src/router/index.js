import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/auth/okta/callback',
    name: 'OktaCallback',
    component: () => import('@/views/auth/OktaCallbackView.vue'),
    meta: { public: true }
  },
  {
    path: '/force-change-password',
    name: 'ForceChangePassword',
    component: () => import('@/views/auth/ForceChangePasswordView.vue')
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue')
      },
      {
        path: 'policies',
        name: 'PolicyList',
        component: () => import('@/views/policy/PolicyListView.vue')
      },
      {
        path: 'policies/new',
        name: 'PolicyCreate',
        component: () => import('@/views/policy/PolicyFormView.vue')
      },
      {
        path: 'policies/:id',
        name: 'PolicyDetail',
        component: () => import('@/views/policy/PolicyDetailView.vue')
      },
      {
        path: 'policies/:id/edit',
        name: 'PolicyEdit',
        component: () => import('@/views/policy/PolicyFormView.vue')
      },
      {
        path: 'vulnerabilities',
        name: 'VulnerabilityList',
        component: () => import('@/views/vulnerability/VulnerabilityListView.vue')
      },
      {
        path: 'vulnerabilities/new',
        name: 'VulnerabilityCreate',
        component: () => import('@/views/vulnerability/VulnerabilityFormView.vue')
      },
      {
        path: 'vulnerabilities/:id',
        name: 'VulnerabilityDetail',
        component: () => import('@/views/vulnerability/VulnerabilityDetailView.vue')
      },
      {
        path: 'vulnerabilities/:id/edit',
        name: 'VulnerabilityEdit',
        component: () => import('@/views/vulnerability/VulnerabilityFormView.vue')
      },
      {
        path: 'training',
        name: 'TrainingList',
        component: () => import('@/views/training/TrainingListView.vue')
      },
      {
        path: 'training/new',
        name: 'TrainingCreate',
        component: () => import('@/views/training/TrainingFormView.vue')
      },
      {
        path: 'training/:id',
        name: 'TrainingDetail',
        component: () => import('@/views/training/TrainingDetailView.vue')
      },
      {
        path: 'training/:id/edit',
        name: 'TrainingEdit',
        component: () => import('@/views/training/TrainingFormView.vue')
      },
      {
        path: 'incidents',
        name: 'IncidentList',
        component: () => import('@/views/incident/IncidentListView.vue')
      },
      {
        path: 'incidents/new',
        name: 'IncidentCreate',
        component: () => import('@/views/incident/IncidentFormView.vue')
      },
      {
        path: 'incidents/:id',
        name: 'IncidentDetail',
        component: () => import('@/views/incident/IncidentDetailView.vue')
      },
      {
        path: 'incidents/:id/edit',
        name: 'IncidentEdit',
        component: () => import('@/views/incident/IncidentFormView.vue')
      },
      {
        path: 'assets',
        name: 'AssetList',
        component: () => import('@/views/asset/AssetListView.vue')
      },
      {
        path: 'security-events',
        name: 'SecurityEvents',
        component: () => import('@/views/security/SecurityEventView.vue')
      },
      {
        path: 'assets/new',
        name: 'AssetCreate',
        component: () => import('@/views/asset/AssetFormView.vue')
      },
      {
        path: 'assets/:id',
        name: 'AssetDetail',
        component: () => import('@/views/asset/AssetDetailView.vue')
      },
      {
        path: 'assets/:id/edit',
        name: 'AssetEdit',
        component: () => import('@/views/asset/AssetFormView.vue')
      },
      {
        path: 'sbom',
        name: 'SbomManagement',
        component: () => import('@/views/sbom/SbomManagementView.vue')
      },
      {
        path: 'isms',
        name: 'IsmsList',
        component: () => import('@/views/isms/IsmsListView.vue')
      },
      {
        path: 'monthly-checks',
        name: 'MonthlyChecks',
        component: () => import('@/views/monthlycheck/MonthlyCheckView.vue')
      },
      {
        path: 'isms/:id',
        name: 'IsmsDetail',
        component: () => import('@/views/isms/IsmsDetailView.vue')
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManagementView.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'admin/audit-logs',
        name: 'AdminAuditLogs',
        component: () => import('@/views/admin/AuditLogView.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'admin/rbac',
        name: 'AdminRbac',
        component: () => import('@/views/admin/RbacManagementView.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'admin/codes',
        name: 'AdminCodes',
        component: () => import('@/views/admin/CodeManagementView.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'admin/quiz-bank',
        name: 'AdminQuizBank',
        component: () => import('@/views/admin/QuizBankView.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'admin/notices',
        name: 'AdminNotices',
        component: () => import('@/views/admin/AdminNoticeView.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'sec-docs',
        name: 'SecDocs',
        component: () => import('@/views/secdoc/SecDocListView.vue')
      },
      {
        path: 'committee',
        name: 'Committee',
        component: () => import('@/views/committee/CommitteeView.vue')
      },
      {
        path: 'internal-audit',
        name: 'InternalAudit',
        component: () => import('@/views/internalaudit/InternalAuditView.vue')
      },
      {
        path: 'security-findings',
        name: 'SecurityFindings',
        component: () => import('@/views/secfinding/SecFindingView.vue')
      },
      {
        path: 'source-scan',
        name: 'SourceScan',
        component: () => import('@/views/sourcescan/SourceScanView.vue')
      },
      // 위협 관리
      {
        path: 'threat-management',
        name: 'ThreatManagement',
        component: () => import('@/views/threat/ThreatManagementView.vue')
      },
      // 위험평가
      {
        path: 'risk-assessment',
        name: 'RiskAssessment',
        component: () => import('@/views/risk/RiskAssessmentView.vue')
      },
      // 위험처리 계획
      {
        path: 'risk-treatment',
        name: 'RiskTreatment',
        component: () => import('@/views/risk/RiskTreatmentView.vue')
      },
      // ISMS-P 통제항목 매핑
      {
        path: 'isms-mapping',
        name: 'IsmsMapping',
        component: () => import('@/views/isms/IsmsControlMappingView.vue')
      },
      // 로그 통합관리
      {
        path: 'logs/personal-info',
        name: 'LogPersonalInfo',
        component: () => import('@/views/log/LogView.vue'),
        props: { logType: 'PERSONAL_INFO' }
      },
      {
        path: 'logs/ad',
        name: 'LogAd',
        component: () => import('@/views/log/LogView.vue'),
        props: { logType: 'AD' }
      },
      {
        path: 'logs/nac',
        name: 'LogNac',
        component: () => import('@/views/log/LogView.vue'),
        props: { logType: 'NAC' }
      },
      {
        path: 'logs/network-link',
        name: 'LogNetworkLink',
        component: () => import('@/views/log/LogView.vue'),
        props: { logType: 'NETWORK_LINK' }
      },
      {
        path: 'logs/search',
        name: 'LogSearch',
        component: () => import('@/views/log/LogView.vue'),
        props: { logType: 'SEARCH' }
      },
      // 대시보드 현황 서브뷰
      {
        path: 'dashboard/risks',
        name: 'DashboardRisks',
        component: () => import('@/views/dashboard/DashboardRisksView.vue')
      },
      {
        path: 'dashboard/vulns-status',
        name: 'DashboardVulns',
        component: () => import('@/views/dashboard/DashboardVulnsView.vue')
      },
      {
        path: 'dashboard/incidents-status',
        name: 'DashboardIncidents',
        component: () => import('@/views/dashboard/DashboardIncidentsView.vue')
      },
      {
        path: 'dashboard/isms-progress',
        name: 'DashboardIsmsProgress',
        component: () => import('@/views/dashboard/DashboardIsmsProgressView.vue')
      },
      {
        path: 'dashboard/evidence-status',
        name: 'DashboardEvidence',
        component: () => import('@/views/dashboard/DashboardEvidenceView.vue')
      },
      {
        path: 'inbox',
        name: 'Inbox',
        component: () => import('@/views/inbox/InboxView.vue')
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/UISettingsView.vue')
      },
      {
        path: 'help',
        name: 'Help',
        component: () => import('@/views/help/HelpView.vue')
      },
      {
        path: 'phishing',
        name: 'Phishing',
        component: () => import('@/views/phishing/PhishingView.vue')
      },
      {
        path: 'training-results',
        name: 'TrainingResults',
        component: () => import('@/views/training/TrainingResultsView.vue')
      },
      {
        path: 'admin/backup',
        name: 'AdminBackup',
        component: () => import('@/views/admin/BackupManagementView.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'admin/settings-management',
        name: 'AdminSettingsManagement',
        component: () => import('@/views/admin/AdminSettingsView.vue'),
        meta: { adminOnly: true }
      },
      // 개인정보보호
      {
        path: 'privacy/contractors',
        name: 'PrivacyContractors',
        component: () => import('@/views/privacy/ContractorView.vue')
      },
      {
        path: 'privacy/contractor-checks',
        name: 'ContractorChecks',
        component: () => import('@/views/privacy/ContractorCheckView.vue')
      },
      {
        path: 'privacy/legal-compliance',
        name: 'LegalCompliance',
        component: () => import('@/views/privacy/LegalComplianceView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'Login' }
  }
  if (to.meta.public && auth.isAuthenticated) {
    return { name: 'Dashboard' }
  }
  if (auth.isAuthenticated && !auth.user) {
    await auth.fetchMe()
  }
  if (auth.isAuthenticated && auth.user?.mustChangePassword && to.name !== 'ForceChangePassword') {
    return { name: 'ForceChangePassword' }
  }
  if (to.meta.adminOnly && !auth.isAdmin) {
    return { name: 'Dashboard' }
  }
})

export default router
