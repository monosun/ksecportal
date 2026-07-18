// 좌측 네비게이션 메뉴 정의 — AppLayout 과 메뉴순서 설정화면(MenuOrderView)이 공유하는 단일 원본.
// 순서는 app_settings 의 menu_order(JSON) 로 오버라이드된다. (uiSettings.menuOrder)
import {
  ChartBarIcon, DocumentTextIcon, ShieldExclamationIcon, AcademicCapIcon, ClipboardCheckIcon
} from '@/components/layout/icons'

export const navGroups = [
  {
    key: 'dashboard',
    labelKey: 'nav.group.dashboard',
    icon: ChartBarIcon,
    items: [
      { to: '/dashboard/risks',            label: 'nav.dashRisks',     icon: ShieldExclamationIcon, menuKey: 'dash_risks' },
      { to: '/dashboard/vulns-status',     label: 'nav.dashVulns',     icon: ShieldExclamationIcon, menuKey: 'dash_vulns' },
      { to: '/dashboard/incidents-status', label: 'nav.dashIncidents', icon: ShieldExclamationIcon, menuKey: 'dash_incidents' },
      { to: '/dashboard/isms-progress',    label: 'nav.dashIsms',      icon: ClipboardCheckIcon,    menuKey: 'dash_isms' },
      { to: '/dashboard/evidence-status',  label: 'nav.dashEvidence',  icon: DocumentTextIcon,      menuKey: 'dash_evidence' },
    ]
  },
  {
    key: 'isms_framework',
    labelKey: 'nav.group.ismsFramework',
    icon: ShieldExclamationIcon,
    items: [
      { to: '/policies',          label: 'nav.policies',       icon: DocumentTextIcon,      menuKey: 'policies' },
      { to: '/assets',            label: 'nav.assets',          icon: DocumentTextIcon,      menuKey: 'assets' },
      { to: '/sbom',              label: 'nav.sbom',            icon: DocumentTextIcon,      menuKey: 'sbom' },
      { to: '/threat-management', label: 'nav.threats',         icon: ShieldExclamationIcon, menuKey: 'threats' },
      { to: '/vulnerabilities',   label: 'nav.vulnerabilities', icon: ShieldExclamationIcon, menuKey: 'vulnerabilities' },
      { to: '/risk-assessment',   label: 'nav.riskAssessment',  icon: ShieldExclamationIcon, menuKey: 'risk_assessment' },
      { to: '/risk-treatment',    label: 'nav.riskTreatment',   icon: DocumentTextIcon,      menuKey: 'risk_treatment' },
      { to: '/isms-mapping',      label: 'nav.ismsMapping',     icon: ClipboardCheckIcon,    menuKey: 'isms_mapping' },
      { to: '/isms',              label: 'nav.isms',            icon: ClipboardCheckIcon,    menuKey: 'isms' },
    ]
  },
  {
    key: 'sec_ops',
    labelKey: 'nav.group.secOps',
    icon: ShieldExclamationIcon,
    items: [
      { to: '/security-events',   label: 'nav.securityEvents', icon: ShieldExclamationIcon, menuKey: 'security_events' },
      { to: '/incidents',          label: 'nav.incidents',       icon: ShieldExclamationIcon, menuKey: 'incidents' },
      { to: '/security-findings',  label: 'nav.secFindings',    icon: ShieldExclamationIcon, menuKey: 'sec_findings' },
      { to: '/monthly-checks',     label: 'nav.monthlyChecks',   icon: ClipboardCheckIcon,    menuKey: 'monthly_checks' },
      { to: '/source-scan',        label: 'nav.sourceScan',      icon: ShieldExclamationIcon, menuKey: 'source_scan' },
    ]
  },
  {
    key: 'log_mgmt',
    labelKey: 'nav.group.logMgmt',
    icon: DocumentTextIcon,
    items: [
      { to: '/logs/personal-info', label: 'nav.logPersonalInfo', icon: DocumentTextIcon, menuKey: 'log_personal_info' },
      { to: '/logs/ad',            label: 'nav.logAd',           icon: DocumentTextIcon, menuKey: 'log_ad' },
      { to: '/logs/nac',           label: 'nav.logNac',          icon: DocumentTextIcon, menuKey: 'log_nac' },
      { to: '/logs/network-link',  label: 'nav.logNetworkLink',  icon: DocumentTextIcon, menuKey: 'log_network_link' },
      { to: '/logs/search',        label: 'nav.logSearch',       icon: DocumentTextIcon, menuKey: 'log_search' },
    ]
  },
  {
    key: 'governance',
    labelKey: 'nav.group.governance',
    icon: ClipboardCheckIcon,
    items: [
      { to: '/committee',      label: 'nav.committee',    icon: ClipboardCheckIcon, menuKey: 'committee' },
      { to: '/internal-audit', label: 'nav.internalAudit',icon: ClipboardCheckIcon, menuKey: 'internal_audit' },
    ]
  },
  {
    key: 'education',
    labelKey: 'nav.group.education',
    icon: AcademicCapIcon,
    items: [
      { to: '/training',         label: 'nav.training',        icon: AcademicCapIcon,       menuKey: 'training' },
      { to: '/phishing',         label: 'nav.phishing',        icon: ShieldExclamationIcon, menuKey: 'phishing' },
      { to: '/training-results', label: 'nav.trainingResults', icon: ClipboardCheckIcon,    menuKey: 'training' },
    ]
  },
  {
    key: 'doc_mgmt',
    labelKey: 'nav.group.docMgmt',
    icon: DocumentTextIcon,
    items: [
      { to: '/sec-docs', label: 'nav.secDocs', icon: DocumentTextIcon, menuKey: 'sec_docs' },
    ]
  },
  {
    key: 'privacy',
    labelKey: 'nav.group.privacy',
    icon: ShieldExclamationIcon,
    items: [
      { to: '/privacy/processing',        label: 'nav.privacyProcessing',    icon: DocumentTextIcon,      menuKey: 'privacy_processing' },
      { to: '/privacy/files',             label: 'nav.privacyFiles',         icon: DocumentTextIcon,      menuKey: 'privacy_files' },
      { to: '/privacy/consents',          label: 'nav.privacyConsent',       icon: ClipboardCheckIcon,    menuKey: 'privacy_consent' },
      { to: '/privacy/provisions',        label: 'nav.privacyProvision',     icon: DocumentTextIcon,      menuKey: 'privacy_provision' },
      { to: '/privacy/contractors',       label: 'nav.privacyContractors',   icon: ClipboardCheckIcon,    menuKey: 'privacy_contractors' },
      { to: '/privacy/contractor-checks', label: 'nav.contractorChecks',     icon: ClipboardCheckIcon,    menuKey: 'privacy_contractors' },
      { to: '/privacy/retentions',        label: 'nav.privacyRetention',     icon: ClipboardCheckIcon,    menuKey: 'privacy_retention' },
      { to: '/privacy/disposals',         label: 'nav.privacyDisposal',      icon: ShieldExclamationIcon, menuKey: 'privacy_disposal' },
      { to: '/privacy/dpia',              label: 'nav.privacyDpia',          icon: ClipboardCheckIcon,    menuKey: 'privacy_dpia' },
      { to: '/privacy/breaches',          label: 'nav.privacyBreach',        icon: ShieldExclamationIcon, menuKey: 'privacy_breach' },
      { to: '/privacy/rights-requests',   label: 'nav.privacyRights',        icon: ClipboardCheckIcon,    menuKey: 'privacy_rights' },
      { to: '/privacy/safeguards',        label: 'nav.privacySafeguard',     icon: ShieldExclamationIcon, menuKey: 'privacy_safeguard' },
      { to: '/privacy/legal-compliance',  label: 'nav.legalCompliance',      icon: DocumentTextIcon,      menuKey: 'privacy_contractors' },
      { to: '/privacy/report',            label: 'nav.privacyReport',        icon: DocumentTextIcon,      menuKey: 'privacy_report' },
    ]
  },
]

/**
 * 저장된 순서(order)에 따라 그룹·아이템을 정렬한다.
 * order = { groups: [groupKey...], items: { groupKey: [to...] } }
 * order 목록에 없는 항목은 원본 순서를 유지하며 뒤로 배치된다(신규 메뉴 대응).
 * Array.prototype.sort 는 안정 정렬이므로 미지정 항목의 상대 순서가 보존된다.
 */
export function orderNavGroups(groups, order) {
  if (!order || typeof order !== 'object') return groups

  const groupRank = {}
  ;(order.groups || []).forEach((key, i) => { groupRank[key] = i })
  const rankOf = (rankMap, key) => (key in rankMap ? rankMap[key] : Number.MAX_SAFE_INTEGER)

  const sortedGroups = [...groups].sort((a, b) => rankOf(groupRank, a.key) - rankOf(groupRank, b.key))

  return sortedGroups.map(group => {
    const itemOrder = order.items && order.items[group.key]
    if (!Array.isArray(itemOrder) || itemOrder.length === 0) return group
    const itemRank = {}
    itemOrder.forEach((to, i) => { itemRank[to] = i })
    const items = [...group.items].sort((a, b) => rankOf(itemRank, a.to) - rankOf(itemRank, b.to))
    return { ...group, items }
  })
}

// 사용자가 추가한 상위 메뉴의 기본 아이콘
export const CUSTOM_GROUP_ICON = DocumentTextIcon
const MAX_RANK = Number.MAX_SAFE_INTEGER

/**
 * 저장된 커스터마이즈(cfg)를 원본 메뉴(groups)에 적용해 최종 좌측 메뉴를 만든다.
 * orderNavGroups 를 확장한 상위호환 형식으로, 기존 { groups, items } 만 있어도 그대로 동작한다.
 *
 * cfg = {
 *   groups: [groupKey...],            // 상위 그룹 표시 순서 (커스텀 그룹 키 포함)
 *   items:  { groupKey: [to...] },    // 그룹별 하위 메뉴 소속·순서 (타 그룹으로의 이동을 표현)
 *   labels: { groupKey: '표시명' },    // 이름 변경 (기본 그룹 라벨 override)
 *   custom: [ { key, label } ],       // 사용자가 추가한 상위 그룹
 *   hidden: [groupKey...],            // 삭제(숨김)된 기본 그룹
 * }
 *
 * 반환 그룹: { key, labelKey|null, labelText|null, icon, items, custom }
 * labelText 가 있으면 그대로 표시하고, 없으면 labelKey 를 i18n 번역한다.
 */
export function resolveMenu(groups, cfg) {
  cfg = cfg && typeof cfg === 'object' ? cfg : {}
  const labels = cfg.labels || {}
  const hidden = new Set(cfg.hidden || [])
  const customDefs = Array.isArray(cfg.custom) ? cfg.custom : []
  const itemAssign = cfg.items || {}

  // 1) to → 원본 아이템 풀
  const pool = {}
  groups.forEach(g => g.items.forEach(it => { pool[it.to] = it }))

  // 2) to → 소속 그룹키 (원본 소속에서 시작, 명시적 배치가 있으면 override)
  const itemGroup = {}
  groups.forEach(g => g.items.forEach(it => { itemGroup[it.to] = g.key }))
  Object.entries(itemAssign).forEach(([gk, tos]) => {
    ;(tos || []).forEach(to => { if (pool[to]) itemGroup[to] = gk })
  })

  // 3) 전체 그룹 목록 (기본 + 커스텀) → 숨김 제외 → 순서 정렬
  const builtinByKey = {}
  groups.forEach(g => { builtinByKey[g.key] = g })
  const customByKey = {}
  customDefs.forEach(c => { if (c && c.key) customByKey[c.key] = c })

  const rank = {}
  ;(cfg.groups || []).forEach((k, i) => { rank[k] = i })
  const orderedKeys = [...new Set([...groups.map(g => g.key), ...customDefs.map(c => c.key)])]
    .filter(k => k && !hidden.has(k))
    .sort((a, b) => (rank[a] ?? MAX_RANK) - (rank[b] ?? MAX_RANK))

  return orderedKeys.map(key => {
    const base = builtinByKey[key]
    const custom = customByKey[key]
    const orderList = Array.isArray(itemAssign[key]) ? itemAssign[key]
      : (base ? base.items.map(it => it.to) : [])
    const orderRank = {}
    orderList.forEach((to, i) => { orderRank[to] = i })
    const items = Object.keys(itemGroup)
      .filter(to => itemGroup[to] === key)
      .map(to => pool[to])
      .sort((a, b) => (orderRank[a.to] ?? MAX_RANK) - (orderRank[b.to] ?? MAX_RANK))
    return {
      key,
      labelKey: base ? base.labelKey : null,
      labelText: labels[key] ?? (custom ? custom.label : null),
      icon: base ? base.icon : CUSTOM_GROUP_ICON,
      items,
      custom: !!custom && !base,
    }
  })
}
