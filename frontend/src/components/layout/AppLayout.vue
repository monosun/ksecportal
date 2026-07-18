<template>
  <div class="h-screen flex overflow-hidden">

    <!-- Sidebar -->
    <aside class="w-[220px] flex-shrink-0 flex flex-col border-r transition-colors duration-200"
      :class="isDark ? 'bg-gray-900 border-gray-700/50' : 'bg-white border-gray-100'">

      <!-- Logo -->
      <div class="h-[60px] px-5 flex items-center border-b flex-shrink-0"
        :class="isDark ? 'border-gray-700/50' : 'border-gray-100'">
        <div class="flex items-center gap-2">
          <img
            v-if="ui.effectiveLogoUrl()"
            :src="ui.effectiveLogoUrl()"
            alt="SecPortal"
            :style="{ height: '22px', width: 'auto' }"
          />
          <span class="text-[15px] font-bold tracking-tight"
            :class="isDark ? 'text-white' : 'text-gray-900'">{{ ui.effectiveLogoText() }}</span>
        </div>
      </div>

      <!-- Nav -->
      <nav class="flex-1 min-h-0 overflow-y-auto px-3 py-3">

        <!-- Home (Main Dashboard) -->
        <RouterLink to="/dashboard" custom v-slot="{ isExactActive, navigate }">
          <button @click="navigate"
            :class="[
              'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all text-sm font-semibold text-left mb-2',
              isExactActive
                ? (isDark ? 'bg-primary-500 text-white shadow-sm' : 'bg-primary-50 text-primary-600')
                : (isDark ? 'text-gray-300 hover:bg-gray-800 hover:text-gray-100' : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900')
            ]">
            <HomeIcon class="w-[15px] h-[15px] flex-shrink-0" />
            <span>{{ $t('nav.home') }}</span>
          </button>
        </RouterLink>

        <!-- 그룹별 메뉴 -->
        <template v-for="group in visibleGroups" :key="group.key">
          <!-- 그룹 헤더 (토글 버튼) -->
          <button @click="toggleGroup(group.key)"
            class="w-full flex items-center justify-between mt-2 mb-0.5 px-2 py-1.5 rounded-lg transition-colors"
            :class="groupOpen[group.key]
              ? (isDark ? 'bg-gray-800 text-primary-300' : 'bg-primary-50 text-primary-600')
              : (isDark ? 'text-gray-500 hover:text-gray-300 hover:bg-gray-800' : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100')">
            <div class="flex items-center gap-2">
              <component :is="group.icon" class="w-[14px] h-[14px] flex-shrink-0" />
              <span class="text-[10px] font-bold uppercase tracking-widest">{{ group.labelText || $t(group.labelKey) }}</span>
            </div>
            <svg class="w-3 h-3 transition-transform duration-200 flex-shrink-0"
              :class="groupOpen[group.key] ? 'rotate-180' : ''"
              fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 9l-7 7-7-7"/>
            </svg>
          </button>

          <!-- 그룹 아이템 -->
          <Transition
            enter-active-class="transition-all duration-200 ease-out"
            enter-from-class="opacity-0 -translate-y-1"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition-all duration-150 ease-in"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 -translate-y-1">
            <div v-if="groupOpen[group.key]" class="space-y-0.5 mb-1">
              <RouterLink
                v-for="item in visibleItems(group)" :key="item.to"
                :to="item.to"
                custom
                v-slot="{ isActive, navigate }">
                <button @click="navigate"
                  :class="[
                    'w-full flex items-center gap-3 pl-5 pr-3 py-2 rounded-xl transition-all text-sm font-medium text-left',
                    isActive
                      ? (isDark ? 'bg-primary-500 text-white shadow-sm' : 'bg-primary-50 text-primary-600')
                      : (isDark ? 'text-gray-400 hover:bg-gray-800 hover:text-gray-100' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900')
                  ]">
                  <component :is="item.icon" class="w-[15px] h-[15px] flex-shrink-0" />
                  <span class="truncate">{{ $t(item.label) }}</span>
                </button>
              </RouterLink>
            </div>
          </Transition>
        </template>

        <!-- 관리 섹션 (admin only) -->
        <template v-if="auth.isAdmin">
          <button @click="toggleAdmin"
            class="w-full flex items-center justify-between mt-2 mb-0.5 px-2 py-1.5 rounded-lg transition-colors"
            :class="adminOpen
              ? (isDark ? 'bg-gray-800 text-primary-300' : 'bg-primary-50 text-primary-600')
              : (isDark ? 'text-gray-500 hover:text-gray-300 hover:bg-gray-800' : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100')">
            <div class="flex items-center gap-2">
              <svg class="w-[14px] h-[14px] flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
              </svg>
              <span class="text-[10px] font-bold uppercase tracking-widest">{{ $t('nav.admin') }}</span>
            </div>
            <svg class="w-3 h-3 transition-transform duration-200 flex-shrink-0"
              :class="adminOpen ? 'rotate-180' : ''"
              fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 9l-7 7-7-7"/>
            </svg>
          </button>
          <Transition
            enter-active-class="transition-all duration-200 ease-out"
            enter-from-class="opacity-0 -translate-y-1"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition-all duration-150 ease-in"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 -translate-y-1">
            <div v-if="adminOpen" class="space-y-0.5 mb-1">
              <RouterLink
                v-for="item in adminNavItems" :key="item.to"
                :to="item.to"
                custom
                v-slot="{ isActive, navigate }">
                <button @click="navigate"
                  :class="[
                    'w-full flex items-center gap-3 pl-5 pr-3 py-2 rounded-xl transition-all text-sm font-medium text-left',
                    isActive
                      ? (isDark ? 'bg-primary-500 text-white shadow-sm' : 'bg-primary-50 text-primary-600')
                      : (isDark ? 'text-gray-400 hover:bg-gray-800 hover:text-gray-100' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900')
                  ]">
                  <component :is="item.icon" class="w-[15px] h-[15px] flex-shrink-0" />
                  <span>{{ $t(item.label) }}</span>
                </button>
              </RouterLink>
            </div>
          </Transition>
        </template>

      </nav>

      <!-- Bottom -->
      <div class="flex-shrink-0 px-3 pb-3 pt-2 border-t space-y-0.5"
        :class="isDark ? 'border-gray-700/50' : 'border-gray-100'">

        <!-- 수신함 -->
        <RouterLink to="/inbox" custom v-slot="{ isActive, navigate }">
          <button @click="navigate"
            :class="[
              'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all text-sm font-medium text-left',
              isActive
                ? (isDark ? 'bg-primary-500 text-white' : 'bg-primary-50 text-primary-600')
                : (isDark ? 'text-gray-400 hover:bg-gray-800 hover:text-gray-100' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900')
            ]">
            <div class="relative flex-shrink-0">
              <svg class="w-[17px] h-[17px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0l-8 4-8-4"/>
              </svg>
              <span v-if="inbox.unreadCount > 0"
                class="absolute -top-1.5 -right-1.5 min-w-[14px] h-[14px] bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center px-0.5">
                {{ inbox.unreadCount > 99 ? '99+' : inbox.unreadCount }}
              </span>
            </div>
            <span>{{ $t('nav.inbox') }}</span>
          </button>
        </RouterLink>

        <!-- 환경설정 -->
        <RouterLink to="/settings" custom v-slot="{ isActive, navigate }">
          <button @click="navigate"
            :class="[
              'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all text-sm font-medium text-left',
              isActive
                ? (isDark ? 'bg-primary-500 text-white' : 'bg-primary-50 text-primary-600')
                : (isDark ? 'text-gray-400 hover:bg-gray-800 hover:text-gray-100' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900')
            ]">
            <svg class="w-[17px] h-[17px] flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
            <span>{{ $t('nav.settings') }}</span>
          </button>
        </RouterLink>

        <!-- 도움말 -->
        <RouterLink to="/help" custom v-slot="{ isActive, navigate }">
          <button @click="navigate"
            :class="[
              'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all text-sm font-medium text-left',
              isActive
                ? (isDark ? 'bg-primary-500 text-white' : 'bg-primary-50 text-primary-600')
                : (isDark ? 'text-gray-400 hover:bg-gray-800 hover:text-gray-100' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900')
            ]">
            <svg class="w-[17px] h-[17px] flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <span>{{ $t('nav.help') }}</span>
          </button>
        </RouterLink>

        <!-- 프로필 -->
        <div class="flex items-center gap-2.5 px-3 py-2.5 rounded-xl mt-1"
          :class="isDark ? 'bg-gray-800' : 'bg-gray-50'">
          <div class="w-7 h-7 bg-primary-500 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0">
            {{ userInitial }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-[13px] font-semibold truncate leading-tight"
              :class="isDark ? 'text-gray-100' : 'text-gray-900'">{{ auth.user?.name }}</p>
            <p class="text-[11px] truncate leading-tight mt-0.5"
              :class="isDark ? 'text-gray-500' : 'text-gray-400'">{{ auth.user?.role }}</p>
          </div>
          <div class="flex gap-1">
            <button @click="toggleLocale"
              class="text-[11px] font-semibold px-1.5 py-1 rounded-lg transition-colors"
              :class="isDark ? 'text-gray-500 hover:text-gray-200 hover:bg-gray-700' : 'text-gray-400 hover:text-gray-700 hover:bg-gray-200'">
              {{ locale === 'ko' ? 'EN' : '한' }}
            </button>
            <button @click="handleLogout"
              class="p-1.5 rounded-lg transition-colors"
              :class="isDark ? 'text-gray-500 hover:text-gray-200 hover:bg-gray-700' : 'text-gray-400 hover:text-gray-700 hover:bg-gray-200'"
              :title="$t('nav.logout')">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main -->
    <main class="flex-1 overflow-auto bg-gray-50 min-w-0">
      <RouterView />
    </main>
  </div>

  <!-- ─── 세션 만료 경고 모달 ─── -->
  <Transition name="fade">
    <div v-if="session.showWarning.value" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"/>
      <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-sm mx-4 p-7 text-center">
        <!-- 경고 아이콘 -->
        <div class="w-14 h-14 rounded-full bg-amber-100 flex items-center justify-center mx-auto mb-4">
          <svg class="w-7 h-7 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
          </svg>
        </div>
        <h3 class="text-lg font-bold text-gray-900 mb-1">세션이 곧 만료됩니다</h3>
        <p class="text-sm text-gray-500 mb-1">자동 로그아웃까지</p>
        <p class="text-3xl font-mono font-bold text-amber-500 mb-5">{{ session.formattedTime.value }}</p>
        <p class="text-sm text-gray-500 mb-6">세션을 연장하시겠습니까?</p>
        <div class="flex gap-3">
          <button @click="session.logout()"
            class="flex-1 py-2.5 rounded-xl border-2 border-gray-200 text-sm font-semibold text-gray-600 hover:bg-gray-50 transition-all">
            로그아웃
          </button>
          <button @click="session.extend()"
            :disabled="session.extending.value"
            class="flex-1 py-2.5 rounded-xl bg-primary-500 text-sm font-semibold text-white hover:bg-primary-600 disabled:opacity-60 transition-all">
            {{ session.extending.value ? '연장 중...' : '세션 연장' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useUiSettingsStore } from '@/stores/uiSettings'
import { useInboxStore } from '@/stores/inbox'
import { useSessionTimer } from '@/composables/useSessionTimer'
import { HomeIcon, DocumentTextIcon, ShieldExclamationIcon } from './icons'
import { navGroups, resolveMenu } from '@/config/navMenu'

const auth = useAuthStore()
const ui = useUiSettingsStore()
const inbox = useInboxStore()
const router = useRouter()
const route = useRoute()
const { locale } = useI18n()
const session = useSessionTimer()

onMounted(() => {
  inbox.startPolling()
  if (auth.isAuthenticated) session.start()
})
onUnmounted(() => {
  inbox.stopPolling()
  session.stop()
})

const isDark = computed(() => ui.sidebarStyle === 'dark')

// ── 메뉴 그룹 정의 (config/navMenu.js 공유) + 저장된 커스터마이즈 적용 ─────
const orderedGroups = computed(() => resolveMenu(navGroups, ui.menuOrder))

// ── 그룹 열림/닫힘 상태 ───────────────────────────────────────────
const groupOpen = reactive({})

function isGroupActive(group) {
  return group.items.some(item => route.path.startsWith(item.to))
}

orderedGroups.value.forEach(g => { groupOpen[g.key] = isGroupActive(g) })

watch(() => route.path, () => {
  orderedGroups.value.forEach(g => { if (isGroupActive(g)) groupOpen[g.key] = true })
})

function toggleGroup(key) {
  const willOpen = !groupOpen[key]
  // 아코디언: 하나를 펼치면 다른 펼쳐진 그룹(관리 섹션 포함)은 닫는다
  if (willOpen) {
    orderedGroups.value.forEach(g => { groupOpen[g.key] = false })
    adminOpen.value = false
  }
  groupOpen[key] = willOpen
}

// ── RBAC 필터링 ───────────────────────────────────────────────────
function visibleItems(group) {
  return group.items.filter(item => !item.menuKey || auth.canRead(item.menuKey))
}

const visibleGroups = computed(() =>
  orderedGroups.value.filter(g => visibleItems(g).length > 0)
)

// ── 관리(admin) 섹션 ──────────────────────────────────────────────
const adminOpen = ref(route.path.startsWith('/admin'))
watch(() => route.path, (path) => {
  if (path.startsWith('/admin')) adminOpen.value = true
})

function toggleAdmin() {
  const willOpen = !adminOpen.value
  // 아코디언: 관리 섹션을 펼치면 다른 그룹은 닫는다
  if (willOpen) orderedGroups.value.forEach(g => { groupOpen[g.key] = false })
  adminOpen.value = willOpen
}

const adminNavItems = [
  { to: '/admin/users',               label: 'admin.users',       icon: DocumentTextIcon },
  { to: '/admin/rbac',                label: 'admin.rbac',        icon: ShieldExclamationIcon },
  { to: '/admin/notices',             label: 'admin.notices',     icon: DocumentTextIcon },
  { to: '/admin/codes',               label: 'admin.codes',       icon: DocumentTextIcon },
  { to: '/admin/menu-order',          label: 'admin.menuOrder',   icon: DocumentTextIcon },
  { to: '/admin/quiz-bank',           label: 'admin.quizBank',    icon: DocumentTextIcon },
  { to: '/admin/audit-logs',          label: 'admin.auditLogs',   icon: DocumentTextIcon },
  { to: '/admin/backup',              label: 'admin.backup',      icon: DocumentTextIcon },
  { to: '/admin/settings-management', label: 'admin.settingsMgmt',icon: ShieldExclamationIcon },
]

const userInitial = computed(() => auth.user?.name?.[0]?.toUpperCase() || '?')

function toggleLocale() {
  locale.value = locale.value === 'ko' ? 'en' : 'ko'
  localStorage.setItem('locale', locale.value)
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
