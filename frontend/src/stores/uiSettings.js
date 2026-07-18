import { defineStore } from 'pinia'
import { ref } from 'vue'
import { appSettingApi } from '@/api/index.js'

export const THEMES = {
  monosun: {
    label: 'Monosun',
    swatch: '#0d1117',
    colors: { 50:'#f6f8fb', 100:'#e8edf3', 200:'#c5d0dd', 300:'#9aaabb', 400:'#667085', 500:'#344054', 600:'#1f2937', 700:'#111827', 900:'#0d1117' }
  },
  blue: {
    label: 'Blue',
    swatch: '#0064FF',
    colors: { 50:'#E8F1FF', 100:'#C2D8FF', 200:'#96BAFF', 300:'#6A9CFF', 400:'#3378FF', 500:'#0064FF', 600:'#0057EB', 700:'#0048D4', 900:'#002B8A' }
  },
  navy: {
    label: 'Navy',
    swatch: '#3B5BDB',
    colors: { 50:'#EDF2FF', 100:'#BAC8FF', 200:'#91A7FF', 300:'#748FFC', 400:'#5C7CFA', 500:'#4C6EF5', 600:'#3B5BDB', 700:'#2F4AC4', 900:'#1A3276' }
  },
  emerald: {
    label: 'Emerald',
    swatch: '#10B981',
    colors: { 50:'#ECFDF5', 100:'#D1FAE5', 200:'#A7F3D0', 300:'#6EE7B7', 400:'#34D399', 500:'#10B981', 600:'#059669', 700:'#047857', 900:'#064E3B' }
  },
  purple: {
    label: 'Purple',
    swatch: '#8B5CF6',
    colors: { 50:'#F5F3FF', 100:'#EDE9FE', 200:'#DDD6FE', 300:'#C4B5FD', 400:'#A78BFA', 500:'#8B5CF6', 600:'#7C3AED', 700:'#6D28D9', 900:'#2E1065' }
  },
  rose: {
    label: 'Rose',
    swatch: '#F43F5E',
    colors: { 50:'#FFF1F2', 100:'#FFE4E6', 200:'#FECDD3', 300:'#FDA4AF', 400:'#FB7185', 500:'#F43F5E', 600:'#E11D48', 700:'#BE123C', 900:'#881337' }
  },
  naver: {
    label: 'Naver Green',
    swatch: '#03C75A',
    colors: { 50:'#F0FFF7', 100:'#C3FFE1', 200:'#87F5BE', 300:'#4AE89B', 400:'#1ADB76', 500:'#03C75A', 600:'#02A94C', 700:'#028A3D', 900:'#015426' }
  }
}

export const FONTS = {
  pretendard: { label: 'Pretendard', value: "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', sans-serif" },
  inter: { label: 'Inter', value: "Inter, 'Noto Sans KR', system-ui, -apple-system, BlinkMacSystemFont, sans-serif" },
  noto: { label: 'Noto Sans KR', value: "'Noto Sans KR', -apple-system, sans-serif" },
  system: { label: '시스템 기본', value: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'Apple SD Gothic Neo', sans-serif" }
}

export const FONT_SIZES = {
  small: { label: '작게', value: '13px' },
  medium: { label: '보통', value: '14px' },
  large: { label: '크게', value: '16px' }
}

function applyTheme(key) {
  const theme = THEMES[key]
  if (!theme) return
  const root = document.documentElement
  Object.entries(theme.colors).forEach(([shade, hex]) => {
    root.style.setProperty(`--color-primary-${shade}`, hex)
  })
}

function applyFont(key) {
  const font = FONTS[key] || FONTS.pretendard
  document.documentElement.style.setProperty('--font-sans', font.value)
}

function applyFontSize(key) {
  const size = FONT_SIZES[key] || FONT_SIZES.medium
  document.documentElement.style.setProperty('--font-size-base', size.value)
}

const DEFAULT_FAVICON = '/shield.svg'
// 브라우저 탭 아이콘(파비콘)을 설정된 로고로 적용 — 로고가 없으면 기본 아이콘으로 되돌린다.
function applyFavicon(url) {
  if (typeof document === 'undefined') return
  const href = url || DEFAULT_FAVICON
  let link = document.querySelector("link[rel~='icon']")
  if (!link) {
    link = document.createElement('link')
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  if (href.startsWith('data:')) {
    const end = Math.min(...[href.indexOf(';'), href.indexOf(',')].filter(i => i > -1))
    const mime = href.slice(5, end)
    if (mime) link.type = mime
  } else if (href.endsWith('.svg')) {
    link.type = 'image/svg+xml'
  } else {
    link.removeAttribute('type')
  }
  link.href = href
}

export const useUiSettingsStore = defineStore('uiSettings', () => {
  const theme = ref(localStorage.getItem('ui-theme') || 'blue')
  const font = ref(localStorage.getItem('ui-font') || 'pretendard')
  const fontSize = ref(localStorage.getItem('ui-font-size') || 'medium')
  const sidebarStyle = ref(localStorage.getItem('ui-sidebar') || 'dark')
  const logoUrl = ref(localStorage.getItem('ui-logo') || null)
  const logoText = ref(localStorage.getItem('ui-logo-text') ?? 'monosun')
  // DB에서 로드한 기본 로고 (localStorage 미설정 시 사용)
  const dbLogoUrl = ref(null)
  const dbLogoText = ref(null)
  const sessionTimeoutMinutes = ref(60)
  // 좌측 메뉴 순서 오버라이드 — { groups: [key...], items: { key: [to...] } } 또는 null
  const menuOrder = ref(null)

  // 실제 표시 로고 — localStorage 우선, 없으면 DB 기본값
  const effectiveLogoUrl = () => logoUrl.value || dbLogoUrl.value || null
  const effectiveLogoText = () => logoText.value || dbLogoText.value || 'SecPortal'

  async function init() {
    applyTheme(theme.value)
    applyFont(font.value)
    applyFontSize(fontSize.value)
    applyFavicon(effectiveLogoUrl())   // localStorage 로고 우선 즉시 적용
    try {
      const res = await appSettingApi.getAll()
      const settings = res?.data || {}
      if (settings.login_logo) dbLogoUrl.value = settings.login_logo
      if (settings.login_logo_text) dbLogoText.value = settings.login_logo_text
      // 파비콘을 설정된 로고로 적용
      applyFavicon(effectiveLogoUrl())
      if (settings.session_timeout_minutes) {
        const m = parseInt(settings.session_timeout_minutes, 10)
        if (m >= 1) sessionTimeoutMinutes.value = m
      }
      if (settings.menu_order) {
        try {
          const parsed = JSON.parse(settings.menu_order)
          if (parsed && typeof parsed === 'object') menuOrder.value = parsed
        } catch {}
      }
    } catch {}
  }

  async function saveMenuOrder(order) {
    await appSettingApi.update('menu_order', JSON.stringify(order))
    menuOrder.value = order
  }

  function setTheme(key) {
    theme.value = key
    localStorage.setItem('ui-theme', key)
    applyTheme(key)
  }

  function setFont(key) {
    font.value = key
    localStorage.setItem('ui-font', key)
    applyFont(key)
  }

  function setFontSize(key) {
    fontSize.value = key
    localStorage.setItem('ui-font-size', key)
    applyFontSize(key)
  }

  function setSidebarStyle(style) {
    sidebarStyle.value = style
    localStorage.setItem('ui-sidebar', style)
  }

  function setLogoUrl(dataUrl) {
    logoUrl.value = dataUrl
    localStorage.setItem('ui-logo', dataUrl)
    applyFavicon(effectiveLogoUrl())
  }

  function clearLogoUrl() {
    logoUrl.value = null
    localStorage.removeItem('ui-logo')
    applyFavicon(effectiveLogoUrl())
  }

  function setLogoText(text) {
    logoText.value = text
    localStorage.setItem('ui-logo-text', text)
  }

  async function saveLogoToServer(dataUrl) {
    await appSettingApi.update('login_logo', dataUrl)
    dbLogoUrl.value = dataUrl
    setLogoUrl(dataUrl)   // setLogoUrl 이 파비콘도 갱신
  }

  async function saveLogoTextToServer(text) {
    await appSettingApi.update('login_logo_text', text)
    dbLogoText.value = text
    setLogoText(text)
  }

  async function saveSessionTimeoutToServer(minutes) {
    await appSettingApi.update('session_timeout_minutes', String(minutes))
    sessionTimeoutMinutes.value = minutes
  }

  return {
    theme, font, fontSize, sidebarStyle, logoUrl, logoText,
    dbLogoUrl, dbLogoText, sessionTimeoutMinutes, menuOrder,
    effectiveLogoUrl, effectiveLogoText,
    init, setTheme, setFont, setFontSize, setSidebarStyle,
    setLogoUrl, clearLogoUrl, setLogoText,
    saveLogoToServer, saveLogoTextToServer, saveSessionTimeoutToServer,
    saveMenuOrder
  }
})
