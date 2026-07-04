<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">시스템 설정</h1>
        <p class="text-sm text-gray-400 mt-0.5">로고, 세션, 알림, RSS 피드 등 시스템 전반을 설정합니다.</p>
      </div>
    </div>

    <div class="page-body">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

      <!-- 로고 -->
      <div class="card">
        <h2 class="text-base font-bold text-gray-800 mb-1">로고</h2>
        <p class="text-sm text-gray-400 mb-5">사이드바와 로그인 화면에 표시될 로고 이미지를 업로드하세요.</p>
        <div class="flex items-center gap-5">
          <div class="w-16 h-16 rounded-xl border-2 border-gray-200 flex items-center justify-center bg-gray-50 flex-shrink-0 overflow-hidden">
            <img v-if="ui.effectiveLogoUrl()" :src="ui.effectiveLogoUrl()" alt="Logo" class="max-w-full max-h-full object-contain p-1" />
            <svg v-else class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
            </svg>
          </div>
          <div class="flex flex-col gap-3">
            <div class="flex gap-2 flex-wrap">
              <label class="cursor-pointer">
                <span class="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border-2 border-gray-200 bg-white text-sm font-semibold text-gray-600 hover:border-gray-300 transition-all">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/>
                  </svg>
                  이미지 업로드
                </span>
                <input type="file" accept="image/*" class="hidden" @change="onLogoUpload" />
              </label>
              <button @click="onSaveLogoToServer"
                :disabled="logoSaving || !ui.effectiveLogoUrl()"
                class="px-4 py-2 rounded-xl border-2 border-primary-200 bg-primary-50 text-sm font-semibold text-primary-600 hover:border-primary-300 disabled:opacity-40 transition-all">
                {{ logoSaving ? '저장 중...' : '서버에 저장' }}
              </button>
              <button v-if="ui.logoUrl" @click="ui.clearLogoUrl()"
                class="px-4 py-2 rounded-xl border-2 border-gray-200 bg-white text-sm font-semibold text-gray-500 hover:border-gray-300 hover:text-red-500 transition-all">
                초기화
              </button>
            </div>
            <p class="text-xs text-gray-400">"서버에 저장" 클릭 시 모든 사용자의 로그인 화면 기본 로고가 변경됩니다.</p>
            <p v-if="logoSaved" class="text-xs text-green-600 font-semibold">서버에 저장되었습니다.</p>
            <div class="flex items-center gap-2">
              <input
                :value="ui.logoText || ui.dbLogoText"
                @input="ui.setLogoText($event.target.value)"
                type="text"
                placeholder="로고 옆 텍스트"
                class="input w-48 text-sm"
              />
              <button @click="onSaveLogoTextToServer"
                :disabled="logoTextSaving"
                class="px-3 py-1.5 rounded-xl border border-primary-200 bg-primary-50 text-xs font-semibold text-primary-600 hover:border-primary-300 disabled:opacity-40 transition-all">
                {{ logoTextSaving ? '저장 중...' : '서버 저장' }}
              </button>
              <span class="text-xs text-gray-400">로고 옆에 표시될 텍스트</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 세션 타임아웃 -->
      <div class="card">
        <h2 class="text-base font-bold text-gray-800 mb-1">세션 타임아웃</h2>
        <p class="text-sm text-gray-400 mb-5">로그인 세션이 유지되는 시간을 설정합니다. 설정 후 다음 로그인부터 적용됩니다.</p>
        <div class="flex items-center gap-4 flex-wrap">
          <div class="flex items-center gap-2">
            <input v-model.number="sessionTimeoutInput" type="number" min="5" max="1440" step="5"
              class="input w-28 text-center text-lg font-bold"
              @change="sessionTimeoutInput = Math.min(1440, Math.max(5, sessionTimeoutInput || 60))"/>
            <span class="text-sm text-gray-600 font-medium">분</span>
          </div>
          <div class="flex gap-2 flex-wrap">
            <button v-for="m in [15, 30, 60, 120, 240, 480]" :key="m"
              @click="sessionTimeoutInput = m"
              class="px-3 py-1.5 rounded-lg border text-xs font-semibold transition-all"
              :class="sessionTimeoutInput === m
                ? 'border-primary-500 bg-primary-50 text-primary-600'
                : 'border-gray-200 text-gray-500 hover:border-gray-300'">
              {{ m >= 60 ? (m / 60) + '시간' : m + '분' }}
            </button>
          </div>
          <button @click="onSaveSessionTimeout" :disabled="sessionTimeoutSaving"
            class="btn-primary text-sm px-5 py-2">
            {{ sessionTimeoutSaving ? '저장 중...' : '저장' }}
          </button>
          <span v-if="sessionTimeoutSaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
        </div>
        <p class="mt-3 text-xs text-gray-400">현재 설정: <b>{{ ui.sessionTimeoutMinutes }}분</b>
          ({{ ui.sessionTimeoutMinutes >= 60 ? (ui.sessionTimeoutMinutes / 60).toFixed(1) + '시간' : '' }})
          · 세션 만료 2분 전에 연장 여부 확인 메시지가 표시됩니다.</p>
      </div>

      <!-- 알림 설정 -->
      <div class="card">
        <h2 class="text-base font-bold text-gray-800 mb-1">승인 알림 설정</h2>
        <p class="text-sm text-gray-400 mb-5">계정 삭제·ADMIN 승격 요청 시 알림을 수신할 방식과 대상을 설정합니다.</p>

        <div class="mb-5">
          <p class="text-sm font-semibold text-gray-700 mb-2">알림 방식</p>
          <div class="flex gap-2">
            <button v-for="opt in notifyMethods" :key="opt.value"
              @click="notifyCfg.method = opt.value"
              class="flex-1 py-2.5 rounded-xl border-2 text-sm font-semibold transition-all"
              :class="notifyCfg.method === opt.value
                ? 'border-primary-500 bg-primary-50 text-primary-600'
                : 'border-gray-200 bg-white text-gray-500 hover:border-gray-300'">
              {{ opt.label }}
            </button>
          </div>
        </div>

        <div v-if="notifyCfg.method !== 'SLACK' && notifyCfg.method !== 'INBOX'" class="mb-4">
          <label class="block text-sm font-semibold text-gray-700 mb-1">수신 이메일 주소</label>
          <input v-model="notifyCfg.approvalEmail" type="email" class="input w-full text-sm" placeholder="approval@example.com" />
        </div>

        <div v-if="notifyCfg.method !== 'EMAIL' && notifyCfg.method !== 'INBOX'" class="mb-4">
          <label class="block text-sm font-semibold text-gray-700 mb-1">Slack Webhook URL</label>
          <input v-model="notifyCfg.slackWebhookUrl" type="url" class="input w-full text-sm" placeholder="https://hooks.slack.com/services/..." />
          <p class="text-xs text-gray-400 mt-1">Slack 앱 → Incoming Webhooks에서 생성한 URL을 입력하세요.</p>
        </div>

        <div v-if="notifyCfg.method === 'INBOX'" class="mb-4 flex items-start gap-2 p-3 rounded-xl bg-primary-50 border border-primary-200 text-sm text-primary-800">
          <svg class="w-4 h-4 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          승인 요청이 발생하면 이메일·Slack 없이 <b>앱 내 수신함</b>으로만 전달됩니다.
        </div>

        <div class="flex items-center gap-3 mt-2">
          <button @click="saveNotifyConfig" :disabled="notifySaving"
            class="btn-primary px-6 py-2 text-sm rounded-xl">
            {{ notifySaving ? '저장 중...' : '저장' }}
          </button>
          <span v-if="notifySaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
        </div>
      </div>

      <!-- RSS 피드 설정 -->
      <div class="card">
        <h2 class="text-base font-bold text-gray-800 mb-1">RSS 피드 설정</h2>
        <p class="text-sm text-gray-400 mb-5">대시보드 KRCERT 보안공지 위젯에 표시할 피드와 조회 기간을 설정합니다.</p>

        <div class="flex items-center gap-4 mb-5">
          <label class="text-sm font-semibold text-gray-700 whitespace-nowrap">최근 일수</label>
          <div class="flex items-center gap-2">
            <input v-model.number="rssDays" type="number" min="1" max="90"
              class="input w-24 text-center text-sm" />
            <span class="text-sm text-gray-400">일 이내 게시물 표시</span>
          </div>
          <div class="flex gap-1.5">
            <button v-for="d in [3,7,14,30]" :key="d"
              @click="rssDays = d"
              class="px-3 py-1 rounded-lg border text-xs font-semibold transition-all"
              :class="rssDays === d ? 'border-primary-500 bg-primary-50 text-primary-600' : 'border-gray-200 text-gray-500 hover:border-gray-300'">
              {{ d }}일
            </button>
          </div>
        </div>

        <div class="mb-4">
          <div class="flex items-center justify-between mb-2">
            <label class="text-sm font-semibold text-gray-700">RSS 피드 목록</label>
            <button @click="rssAddFeed"
              class="flex items-center gap-1 text-xs text-primary-600 hover:text-primary-700 font-medium px-3 py-1.5 rounded-lg border border-primary-200 hover:bg-primary-50 transition-colors">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              피드 추가
            </button>
          </div>
          <div class="space-y-2">
            <div v-for="(feed, idx) in rssFeeds" :key="idx"
              class="flex items-center gap-2 p-3 rounded-xl bg-gray-50 border border-gray-100">
              <div class="flex-1 grid grid-cols-3 gap-2">
                <div>
                  <label class="block text-[11px] text-gray-400 mb-0.5">피드 URL</label>
                  <input v-model="feed.url" type="url" placeholder="https://..."
                    class="input text-xs py-1.5 w-full" />
                </div>
                <div>
                  <label class="block text-[11px] text-gray-400 mb-0.5">카테고리 키</label>
                  <input v-model="feed.category" type="text" placeholder="vuln / notice / ..."
                    class="input text-xs py-1.5 w-full" />
                </div>
                <div>
                  <label class="block text-[11px] text-gray-400 mb-0.5">탭 이름</label>
                  <input v-model="feed.label" type="text" placeholder="취약점 정보"
                    class="input text-xs py-1.5 w-full" />
                </div>
              </div>
              <button @click="rssRemoveFeed(idx)"
                class="flex-shrink-0 p-1.5 text-gray-400 hover:text-red-500 rounded-lg hover:bg-red-50 transition-colors mt-3.5">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                </svg>
              </button>
            </div>
            <div v-if="rssFeeds.length === 0" class="py-4 text-center text-xs text-gray-400 border border-dashed border-gray-200 rounded-xl">
              피드가 없습니다. "피드 추가"를 클릭하여 추가하세요.
            </div>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <button @click="saveRssConfig" :disabled="rssSaving"
            class="btn-primary text-sm px-6 py-2">
            {{ rssSaving ? '저장 중...' : '저장' }}
          </button>
          <button @click="resetRssDefaults" class="text-sm text-gray-500 hover:text-gray-700 px-3 py-2 rounded-lg hover:bg-gray-100 transition-colors">
            기본값으로 복원
          </button>
          <span v-if="rssSaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
        </div>
      </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useUiSettingsStore } from '@/stores/uiSettings'
import { notificationConfigApi, appSettingApi } from '@/api/index.js'

const ui = useUiSettingsStore()

const logoSaving = ref(false)
const logoSaved = ref(false)
const logoTextSaving = ref(false)

const sessionTimeoutInput = ref(ui.sessionTimeoutMinutes)
watch(() => ui.sessionTimeoutMinutes, (v) => { sessionTimeoutInput.value = v })
const sessionTimeoutSaving = ref(false)
const sessionTimeoutSaved = ref(false)

const notifyMethods = [
  { value: 'EMAIL', label: '이메일' },
  { value: 'SLACK', label: 'Slack' },
  { value: 'BOTH',  label: '이메일 + Slack' },
  { value: 'INBOX', label: '수신함' }
]
const notifyCfg = ref({ method: 'EMAIL', approvalEmail: '', slackWebhookUrl: '' })
const notifySaving = ref(false)
const notifySaved = ref(false)

const DEFAULT_RSS_FEEDS = [
  { url: 'https://knvd.krcert.or.kr/rss/security/info',   category: 'vuln',   label: '취약점 정보' },
  { url: 'https://knvd.krcert.or.kr/rss/security/notice', category: 'notice', label: '보안공지' }
]
const rssDays  = ref(7)
const rssFeeds = ref(DEFAULT_RSS_FEEDS.map(f => ({ ...f })))
const rssSaving = ref(false)
const rssSaved  = ref(false)

function onLogoUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => ui.setLogoUrl(ev.target.result)
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function onSaveLogoToServer() {
  const url = ui.effectiveLogoUrl()
  if (!url) return
  logoSaving.value = true
  logoSaved.value = false
  try {
    await ui.saveLogoToServer(url)
    logoSaved.value = true
    setTimeout(() => { logoSaved.value = false }, 3000)
  } catch {
    alert('서버 저장에 실패했습니다.')
  } finally {
    logoSaving.value = false
  }
}

async function onSaveLogoTextToServer() {
  const text = ui.logoText || ui.dbLogoText
  if (!text) return
  logoTextSaving.value = true
  try {
    await ui.saveLogoTextToServer(text)
  } catch {
    alert('서버 저장에 실패했습니다.')
  } finally {
    logoTextSaving.value = false
  }
}

async function onSaveSessionTimeout() {
  const m = Math.min(1440, Math.max(5, sessionTimeoutInput.value || 60))
  sessionTimeoutInput.value = m
  sessionTimeoutSaving.value = true
  sessionTimeoutSaved.value = false
  try {
    await ui.saveSessionTimeoutToServer(m)
    sessionTimeoutSaved.value = true
    setTimeout(() => { sessionTimeoutSaved.value = false }, 3000)
  } catch {
    alert('저장에 실패했습니다.')
  } finally {
    sessionTimeoutSaving.value = false
  }
}

async function saveNotifyConfig() {
  notifySaving.value = true
  notifySaved.value = false
  try {
    await notificationConfigApi.update({
      method: notifyCfg.value.method,
      approvalEmail: notifyCfg.value.approvalEmail,
      slackWebhookUrl: notifyCfg.value.slackWebhookUrl
    })
    notifySaved.value = true
    setTimeout(() => { notifySaved.value = false }, 3000)
  } finally {
    notifySaving.value = false
  }
}

function rssAddFeed() {
  rssFeeds.value.push({ url: '', category: '', label: '' })
}
function rssRemoveFeed(idx) {
  rssFeeds.value.splice(idx, 1)
}
function resetRssDefaults() {
  rssDays.value = 7
  rssFeeds.value = DEFAULT_RSS_FEEDS.map(f => ({ ...f }))
}
async function saveRssConfig() {
  rssSaving.value = true
  rssSaved.value = false
  try {
    await appSettingApi.update('rss.days', String(rssDays.value))
    await appSettingApi.update('rss.feeds', JSON.stringify(rssFeeds.value))
    rssSaved.value = true
    setTimeout(() => { rssSaved.value = false }, 3000)
  } catch {
    alert('RSS 설정 저장에 실패했습니다.')
  } finally {
    rssSaving.value = false
  }
}

async function loadRssConfig() {
  try {
    const res = await appSettingApi.getAll()
    const settings = res.data || {}
    if (settings['rss.days']) rssDays.value = parseInt(settings['rss.days']) || 7
    if (settings['rss.feeds']) {
      const feeds = JSON.parse(settings['rss.feeds'])
      if (Array.isArray(feeds) && feeds.length > 0) rssFeeds.value = feeds
    }
  } catch {}
}

onMounted(async () => {
  try {
    const data = await notificationConfigApi.get()
    notifyCfg.value = { ...notifyCfg.value, ...data }
  } catch {}
  loadRssConfig()
})
</script>
