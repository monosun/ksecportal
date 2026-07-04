<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ $t('settings.title') }}</h1>
        <p class="text-sm text-gray-400 mt-0.5">{{ $t('settings.subtitle') }}</p>
      </div>
    </div>

    <div class="page-body">
      <!-- 탭 내비게이션 -->
      <div class="flex gap-1 border-b border-gray-200 mb-4">
        <button @click="activeTab = 'ui'"
          class="px-4 py-2 text-sm font-semibold border-b-2 -mb-px transition-colors"
          :class="activeTab === 'ui' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'">
          화면
        </button>
        <button @click="activeTab = 'account'"
          class="px-4 py-2 text-sm font-semibold border-b-2 -mb-px transition-colors"
          :class="activeTab === 'account' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'">
          계정
        </button>
        <button @click="activeTab = 'about'"
          class="px-4 py-2 text-sm font-semibold border-b-2 -mb-px transition-colors"
          :class="activeTab === 'about' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'">
          정보
        </button>
      </div>

      <!-- 탭: 화면 -->
      <div v-if="activeTab === 'ui'" class="grid grid-cols-1 lg:grid-cols-2 gap-3">

      <!-- 컬러 테마 -->
      <div class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-0.5">{{ $t('settings.colorTheme') }}</h2>
        <p class="text-xs text-gray-400 mb-3">{{ $t('settings.colorThemeDesc') }}</p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="(def, key) in THEMES" :key="key"
            @click="ui.setTheme(key)"
            :title="def.label"
            class="flex items-center gap-2 px-3 py-2 rounded-xl border-2 transition-all text-sm font-semibold"
            :class="ui.theme === key
              ? 'border-primary-500 bg-primary-50 text-primary-600 shadow-sm'
              : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'">
            <span class="w-3.5 h-3.5 rounded-full flex-shrink-0 shadow-sm"
              :style="{ backgroundColor: def.swatch }"></span>
            {{ def.label }}
            <svg v-if="ui.theme === key" class="w-3.5 h-3.5 ml-0.5 text-primary-500" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 폰트 -->
      <div class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-0.5">{{ $t('settings.font') }}</h2>
        <p class="text-xs text-gray-400 mb-3">{{ $t('settings.fontDesc') }}</p>
        <div class="space-y-1.5">
          <button
            v-for="(def, key) in FONTS" :key="key"
            @click="ui.setFont(key)"
            class="w-full flex items-center justify-between px-3 py-2 rounded-xl border-2 transition-all text-left"
            :class="ui.font === key
              ? 'border-primary-500 bg-primary-50'
              : 'border-gray-200 bg-white hover:border-gray-300'">
            <div>
              <p class="text-sm font-semibold" :class="ui.font === key ? 'text-primary-600' : 'text-gray-800'"
                :style="{ fontFamily: def.value }">
                {{ def.label }}
              </p>
              <p class="text-xs text-gray-400" :style="{ fontFamily: def.value }">
                가나다라 ABCDE 012345
              </p>
            </div>
            <span v-if="ui.font === key" class="w-4 h-4 rounded-full bg-primary-500 flex items-center justify-center flex-shrink-0">
              <svg class="w-2.5 h-2.5 text-white" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
              </svg>
            </span>
          </button>
        </div>
      </div>

      <!-- 글자 크기 -->
      <div class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-0.5">{{ $t('settings.fontSize') }}</h2>
        <p class="text-xs text-gray-400 mb-3">{{ $t('settings.fontSizeDesc') }}</p>
        <div class="flex gap-2">
          <button
            v-for="(def, key) in FONT_SIZES" :key="key"
            @click="ui.setFontSize(key)"
            class="flex-1 flex flex-col items-center gap-1.5 py-3 rounded-xl border-2 transition-all"
            :class="ui.fontSize === key
              ? 'border-primary-500 bg-primary-50'
              : 'border-gray-200 bg-white hover:border-gray-300'">
            <span :class="ui.fontSize === key ? 'text-primary-500' : 'text-gray-600'"
              :style="{ fontSize: def.value, fontWeight: 700, lineHeight: 1 }">가</span>
            <span class="text-xs font-semibold"
              :class="ui.fontSize === key ? 'text-primary-600' : 'text-gray-500'">
              {{ def.label }}
            </span>
            <span class="text-[11px] text-gray-400">{{ def.value }}</span>
          </button>
        </div>
      </div>

      <!-- 사이드바 스타일 -->
      <div class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-0.5">{{ $t('settings.sidebarStyle') }}</h2>
        <p class="text-xs text-gray-400 mb-3">{{ $t('settings.sidebarStyleDesc') }}</p>
        <div class="flex gap-3">

          <!-- 다크 미리보기 -->
          <button @click="ui.setSidebarStyle('dark')"
            class="flex-1 rounded-xl border-2 overflow-hidden transition-all"
            :class="ui.sidebarStyle === 'dark' ? 'border-primary-500 shadow-sm' : 'border-gray-200 hover:border-gray-300'">
            <div class="flex h-16">
              <div class="w-10 bg-gray-800 flex flex-col gap-1 p-1">
                <div class="h-1 rounded bg-gray-600 w-full"></div>
                <div class="h-1 rounded bg-primary-600 w-full"></div>
                <div class="h-1 rounded bg-gray-600 w-full"></div>
                <div class="h-1 rounded bg-gray-600 w-full"></div>
              </div>
              <div class="flex-1 bg-gray-100 p-1.5 space-y-1">
                <div class="h-1.5 rounded bg-gray-300 w-3/4"></div>
                <div class="h-1.5 rounded bg-gray-200 w-1/2"></div>
                <div class="h-5 rounded bg-white border border-gray-200"></div>
              </div>
            </div>
            <div class="px-3 py-1.5 border-t flex items-center justify-between"
              :class="ui.sidebarStyle === 'dark' ? 'border-primary-200 bg-primary-50' : 'border-gray-100 bg-white'">
              <span class="text-xs font-semibold"
                :class="ui.sidebarStyle === 'dark' ? 'text-primary-600' : 'text-gray-500'">
                {{ $t('settings.sidebarDark') }}
              </span>
              <span v-if="ui.sidebarStyle === 'dark'" class="w-3.5 h-3.5 rounded-full bg-primary-500 flex items-center justify-center">
                <svg class="w-2 h-2 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
              </span>
            </div>
          </button>

          <!-- 라이트 미리보기 -->
          <button @click="ui.setSidebarStyle('light')"
            class="flex-1 rounded-xl border-2 overflow-hidden transition-all"
            :class="ui.sidebarStyle === 'light' ? 'border-primary-500 shadow-sm' : 'border-gray-200 hover:border-gray-300'">
            <div class="flex h-16">
              <div class="w-10 bg-white border-r border-gray-100 flex flex-col gap-1 p-1">
                <div class="h-1 rounded bg-gray-200 w-full"></div>
                <div class="h-1 rounded w-full" :style="{ backgroundColor: 'var(--color-primary-100)' }"></div>
                <div class="h-1 rounded bg-gray-200 w-full"></div>
                <div class="h-1 rounded bg-gray-200 w-full"></div>
              </div>
              <div class="flex-1 bg-gray-50 p-1.5 space-y-1">
                <div class="h-1.5 rounded bg-gray-300 w-3/4"></div>
                <div class="h-1.5 rounded bg-gray-200 w-1/2"></div>
                <div class="h-5 rounded bg-white border border-gray-200"></div>
              </div>
            </div>
            <div class="px-3 py-1.5 border-t flex items-center justify-between"
              :class="ui.sidebarStyle === 'light' ? 'border-primary-200 bg-primary-50' : 'border-gray-100 bg-white'">
              <span class="text-xs font-semibold"
                :class="ui.sidebarStyle === 'light' ? 'text-primary-600' : 'text-gray-500'">
                {{ $t('settings.sidebarLight') }}
              </span>
              <span v-if="ui.sidebarStyle === 'light'" class="w-3.5 h-3.5 rounded-full bg-primary-500 flex items-center justify-center">
                <svg class="w-2 h-2 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
              </span>
            </div>
          </button>

        </div>
      </div>

      </div><!-- /탭: 화면 -->

      <!-- 탭: 계정 -->
      <div v-if="activeTab === 'account'" class="grid grid-cols-1 lg:grid-cols-2 gap-3">

      <!-- 비밀번호 변경 -->
      <div class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-0.5">비밀번호 변경</h2>
        <p class="text-xs text-gray-400 mb-3">현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.</p>
        <div class="space-y-2 max-w-sm">
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">현재 비밀번호</label>
            <div class="relative">
              <input v-model="pwForm.current" :type="pwShow.current ? 'text' : 'password'"
                class="input pr-10 text-sm" placeholder="현재 비밀번호" autocomplete="current-password" />
              <button type="button" @click="pwShow.current = !pwShow.current"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 focus:outline-none">
                <svg v-if="!pwShow.current" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">새 비밀번호</label>
            <div class="relative">
              <input v-model="pwForm.newPw" :type="pwShow.newPw ? 'text' : 'password'"
                class="input pr-10 text-sm" autocomplete="new-password" />
              <button type="button" @click="pwShow.newPw = !pwShow.newPw"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 focus:outline-none">
                <svg v-if="!pwShow.newPw" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
            <p class="text-xs text-gray-400 mt-0.5">{{ PASSWORD_HINT }}</p>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">새 비밀번호 확인</label>
            <input v-model="pwForm.confirm" :type="pwShow.newPw ? 'text' : 'password'"
              class="input text-sm" :class="pwForm.confirm && pwForm.newPw !== pwForm.confirm ? 'border-red-400' : ''"
              placeholder="새 비밀번호 재입력" autocomplete="new-password" />
            <p v-if="pwForm.confirm && pwForm.newPw !== pwForm.confirm"
              class="text-xs text-red-500 mt-0.5">비밀번호가 일치하지 않습니다.</p>
          </div>
          <div class="flex items-center gap-3">
            <button @click="changePassword"
              :disabled="pwSaving || !pwForm.current || !pwForm.newPw || pwForm.newPw !== pwForm.confirm"
              class="btn-primary px-5 py-1.5 text-sm rounded-xl disabled:opacity-50">
              {{ pwSaving ? '변경 중...' : '비밀번호 변경' }}
            </button>
            <span v-if="pwResult.message" class="text-sm font-semibold"
              :class="pwResult.success ? 'text-green-600' : 'text-red-500'">
              {{ pwResult.message }}
            </span>
          </div>
        </div>
      </div>

      <!-- MFA 설정 -->
      <div class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-0.5">MFA (다중 인증)</h2>
        <p class="text-xs text-gray-400 mb-3">Google Authenticator 등 TOTP 앱으로 2단계 인증을 설정합니다.</p>

        <!-- 현재 상태 -->
        <div class="flex items-center gap-3 mb-3">
          <span class="text-sm font-semibold text-gray-700">현재 상태</span>
          <span v-if="mfaEnabled" class="inline-flex items-center gap-1.5 text-xs font-bold px-2.5 py-1 rounded-full bg-green-100 text-green-700">
            <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
            활성화됨
          </span>
          <span v-else class="text-xs font-bold px-2.5 py-1 rounded-full bg-gray-100 text-gray-500">비활성화</span>
        </div>

        <!-- MFA 비활성화 -->
        <div v-if="mfaEnabled">
          <div v-if="!mfaDisableMode">
            <button @click="mfaDisableMode = true" class="px-4 py-1.5 text-sm border border-red-300 text-red-600 rounded-xl hover:bg-red-50 transition-colors">
              MFA 비활성화
            </button>
          </div>
          <div v-else class="space-y-2 max-w-xs">
            <p class="text-sm text-gray-600">비활성화하려면 현재 Authenticator 코드를 입력하세요.</p>
            <input v-model="mfaCode" type="text" inputmode="numeric" maxlength="6"
              class="input text-center text-xl tracking-widest font-mono" placeholder="000000" />
            <div class="flex gap-2">
              <button @click="handleMfaDisable" :disabled="mfaSaving || mfaCode.length !== 6"
                class="px-4 py-1.5 text-sm bg-red-600 text-white rounded-xl hover:bg-red-700 disabled:opacity-50 transition-colors">
                {{ mfaSaving ? '처리 중...' : '비활성화' }}
              </button>
              <button @click="mfaDisableMode = false; mfaCode = ''" class="px-4 py-1.5 text-sm border border-gray-300 text-gray-600 rounded-xl hover:bg-gray-50">취소</button>
            </div>
          </div>
        </div>

        <!-- MFA 설정 시작 -->
        <div v-else>
          <div v-if="!mfaSetupMode">
            <button @click="startMfaSetup" :disabled="mfaSaving"
              class="btn-primary px-4 py-1.5 text-sm rounded-xl">
              {{ mfaSaving ? '로딩 중...' : 'MFA 설정' }}
            </button>
          </div>
          <div v-else class="space-y-3">
            <div class="p-3 bg-gray-50 rounded-xl border border-gray-200">
              <p class="text-xs font-semibold text-gray-700 mb-2">1. Authenticator 앱으로 QR 코드를 스캔하세요</p>
              <canvas ref="qrCanvas" class="block mx-auto"></canvas>
              <p class="text-xs text-gray-500 mt-2">또는 아래 코드를 직접 입력하세요</p>
              <code class="block mt-1 text-xs bg-white border border-gray-200 rounded-lg px-3 py-1.5 font-mono break-all select-all">{{ mfaSetupData?.secret }}</code>
            </div>
            <div class="space-y-1.5 max-w-xs">
              <p class="text-xs font-semibold text-gray-700">2. Authenticator의 6자리 코드를 입력하세요</p>
              <input v-model="mfaCode" type="text" inputmode="numeric" maxlength="6"
                class="input text-center text-xl tracking-widest font-mono" placeholder="000000" />
              <div class="flex gap-2">
                <button @click="handleMfaEnable" :disabled="mfaSaving || mfaCode.length !== 6"
                  class="btn-primary px-4 py-1.5 text-sm rounded-xl disabled:opacity-50">
                  {{ mfaSaving ? '처리 중...' : '활성화' }}
                </button>
                <button @click="mfaSetupMode = false; mfaCode = ''" class="px-4 py-1.5 text-sm border border-gray-300 text-gray-600 rounded-xl hover:bg-gray-50">취소</button>
              </div>
            </div>
          </div>
        </div>

        <p v-if="mfaResult.message" class="mt-2 text-sm font-semibold"
          :class="mfaResult.success ? 'text-green-600' : 'text-red-500'">
          {{ mfaResult.message }}
        </p>
      </div>

      <!-- 메시지 수신함 -->
      <div class="card lg:col-span-2">
        <div class="flex items-center justify-between mb-0.5">
          <h2 class="text-sm font-bold text-gray-800">{{ $t('inbox.title') }}</h2>
          <span v-if="inboxUnread > 0"
            class="text-xs font-bold px-2 py-0.5 rounded-full bg-red-100 text-red-600">
            {{ inboxUnread }}개 미읽음
          </span>
        </div>
        <p class="text-xs text-gray-400 mb-3">계정 삭제·ADMIN 권한 부여 등 승인 요청 메시지를 확인하고 처리할 수 있습니다.</p>

        <div v-if="inboxLoading" class="py-4 text-center text-sm text-gray-400">{{ $t('common.loading') }}</div>
        <div v-else-if="inboxMessages.length === 0" class="py-4 text-center text-sm text-gray-400">{{ $t('inbox.empty') }}</div>
        <div v-else class="space-y-2">
          <div v-for="msg in inboxMessages" :key="msg.id"
            class="flex items-start gap-3 p-2.5 rounded-xl border transition-all"
            :class="!msg.read ? 'border-primary-200 bg-primary-50' : 'border-gray-100 bg-gray-50'">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-xs font-semibold px-2 py-0.5 rounded-full"
                  :class="msg.type === 'APPROVAL_REQUEST' ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'">
                  {{ msg.type === 'APPROVAL_REQUEST' ? $t('inbox.approvalRequest') : $t('inbox.info') }}
                </span>
                <span v-if="!msg.read" class="text-xs font-semibold text-primary-600">{{ $t('inbox.unread') }}</span>
              </div>
              <p class="mt-0.5 text-sm font-semibold text-gray-900">{{ msg.title }}</p>
              <p v-if="msg.content" class="text-xs text-gray-500">{{ msg.content }}</p>
              <div v-if="msg.hasAction && msg.type === 'APPROVAL_REQUEST' && !inboxActionDone[msg.id]"
                class="mt-1.5 flex gap-2">
                <button @click="inboxApprove(msg)" :disabled="inboxProcessing[msg.id]"
                  class="btn-primary text-xs px-3 py-1">{{ $t('inbox.approve') }}</button>
                <button @click="inboxReject(msg)" :disabled="inboxProcessing[msg.id]"
                  class="text-xs px-3 py-1 rounded-xl border border-red-300 text-red-600 hover:bg-red-50 transition-colors">{{ $t('inbox.reject') }}</button>
              </div>
              <div v-else-if="inboxActionDone[msg.id]" class="mt-1 text-xs font-semibold"
                :class="inboxActionDone[msg.id] === 'approved' ? 'text-green-600' : 'text-red-500'">
                ✓ {{ inboxActionDone[msg.id] === 'approved' ? $t('inbox.approved') : $t('inbox.rejected') }}
              </div>
            </div>
          </div>
        </div>

        <div class="mt-3 flex items-center gap-3 flex-wrap">
          <RouterLink to="/inbox" class="btn-secondary text-sm">전체 수신함 보기 →</RouterLink>
          <button v-if="inboxMessages.some(m => !m.read)" @click="inboxMarkAllRead" class="text-sm text-gray-500 hover:text-gray-700">
            {{ $t('inbox.markAllRead') }}
          </button>
          <button v-if="inboxMessages.length" @click="showInboxClearConfirm = true"
            class="flex items-center gap-1.5 text-sm text-red-500 hover:text-red-700 transition-colors">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
            {{ $t('inbox.clearInbox') }}
          </button>
        </div>
      </div>

      </div><!-- /탭: 계정 -->

      <!-- 탭: 정보 (제품 소개) -->
      <div v-if="activeTab === 'about'" class="max-w-3xl space-y-3">

        <!-- 제품 소개 -->
        <div class="card">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h2 class="text-lg font-bold text-gray-900">SecPortal — 오픈소스 정보보호 포탈</h2>
              <p class="text-sm text-gray-500 mt-1 leading-relaxed">
                스타트업·중소기업을 위한 <strong>올인원 정보보안 관리 시스템</strong>입니다.
                보안 정책, 취약점, 인시던트, 자산, 보안이벤트, 교육, ISMS-P 증적, 법령준수까지
                단일 플랫폼에서 관리합니다.
              </p>
            </div>
            <span class="flex-shrink-0 px-3 py-1 rounded-full bg-primary-50 text-primary-600 text-sm font-bold">
              v{{ appVersion }}
            </span>
          </div>
          <div class="mt-4 flex flex-wrap gap-x-6 gap-y-1 text-sm text-gray-500">
            <span><span class="font-semibold text-gray-700">만든이</span> monosun</span>
            <span><span class="font-semibold text-gray-700">라이선스</span> MIT License © 2025 Monosun</span>
          </div>
        </div>

        <!-- 주요 기능 요약 -->
        <div class="card">
          <h3 class="text-sm font-bold text-gray-800 mb-3">주요 기능</h3>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-x-6 gap-y-2 text-sm text-gray-600">
            <div class="flex gap-2"><span class="text-primary-500 font-bold flex-shrink-0">·</span>
              <span><b class="text-gray-700">정보보호관리체계</b> — 위협관리(MRR 560개)·위험평가·위험처리계획, ISMS-P 증적관리(101개 인증항목)·통제항목 매핑, 보안 결함사항</span></div>
            <div class="flex gap-2"><span class="text-primary-500 font-bold flex-shrink-0">·</span>
              <span><b class="text-gray-700">보안 운영</b> — 취약점(NVD 자동조회)·인시던트·자산(온프레미스+클라우드)·보안이벤트(10종 솔루션 연동)·월간 보안점검</span></div>
            <div class="flex gap-2"><span class="text-primary-500 font-bold flex-shrink-0">·</span>
              <span><b class="text-gray-700">보안 거버넌스</b> — 보안 정책·보안문서(버전 이력)·정보보호위원회·내부감사·감사 로그</span></div>
            <div class="flex gap-2"><span class="text-primary-500 font-bold flex-shrink-0">·</span>
              <span><b class="text-gray-700">개인정보보호</b> — 수탁사 관리·수탁사 점검, 법령준수관리(법제처 API 실시간 조문·법령검토·Excel 보고서)</span></div>
            <div class="flex gap-2"><span class="text-primary-500 font-bold flex-shrink-0">·</span>
              <span><b class="text-gray-700">IT 및 정보보호 교육</b> — 교육 코스·퀴즈·이수 추적, 모의 악성메일(피싱) 훈련 캠페인</span></div>
            <div class="flex gap-2"><span class="text-primary-500 font-bold flex-shrink-0">·</span>
              <span><b class="text-gray-700">플랫폼</b> — 대시보드 실시간 현황, PDF/CSV/Excel 리포트, RBAC 권한관리, MFA·Okta SSO, 암호화 백업, 다국어(한/영)</span></div>
          </div>
        </div>

        <!-- 기술 스택 / 역할 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
          <div class="card">
            <h3 class="text-sm font-bold text-gray-800 mb-3">기술 스택</h3>
            <div class="space-y-1.5 text-sm text-gray-600">
              <p><span class="inline-block w-20 font-semibold text-gray-700">Frontend</span> Vue 3 · Vite · Pinia · Tailwind CSS</p>
              <p><span class="inline-block w-20 font-semibold text-gray-700">Backend</span> Spring Boot 3.3 · Spring Security 6 (JWT) · JPA</p>
              <p><span class="inline-block w-20 font-semibold text-gray-700">Database</span> MySQL 8</p>
              <p><span class="inline-block w-20 font-semibold text-gray-700">인프라</span> Docker Compose · Nginx</p>
            </div>
          </div>
          <div class="card">
            <h3 class="text-sm font-bold text-gray-800 mb-3">역할(Role)</h3>
            <div class="space-y-1.5 text-sm text-gray-600">
              <p><span class="inline-block w-24 font-semibold text-gray-700">ADMIN</span> 전체 관리·사용자·감사 로그·설정</p>
              <p><span class="inline-block w-24 font-semibold text-gray-700">MANAGER</span> 정책·취약점·자산 등 생성·수정, 리포트</p>
              <p><span class="inline-block w-24 font-semibold text-gray-700">USER</span> 조회, 정책 수신 확인, 교육 이수</p>
              <p class="text-xs text-gray-400 pt-1">RBAC 권한관리로 메뉴별 세분화 권한 설정 가능</p>
            </div>
          </div>
        </div>

      </div><!-- /탭: 정보 -->

    </div>
  </div>

  <!-- 수신함 비우기 확인 모달 (환경설정) -->
  <div v-if="showInboxClearConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
      <div class="flex items-center gap-3 mb-3">
        <div class="flex-shrink-0 w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
          <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900">{{ $t('inbox.clearInboxConfirmTitle') }}</h3>
      </div>
      <p class="text-sm text-gray-600 mb-3">{{ $t('inbox.clearInboxConfirmMsg') }}</p>
      <p class="text-xs text-gray-500 bg-gray-50 border border-gray-200 rounded-lg px-3 py-2">
        총 <span class="font-semibold text-gray-800">{{ inboxMessages.length }}개</span>의 메시지가 삭제됩니다.
      </p>
      <div class="flex justify-end gap-3 mt-6">
        <button @click="showInboxClearConfirm = false"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ $t('common.cancel') }}
        </button>
        <button @click="inboxClearAll" :disabled="inboxClearing"
          class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors">
          {{ inboxClearing ? $t('common.loading') : $t('inbox.clearInboxConfirm') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { RouterLink } from 'vue-router'
import { useUiSettingsStore, THEMES, FONTS, FONT_SIZES } from '@/stores/uiSettings'
import { inboxApi, authApi } from '@/api/index.js'
import { useInboxStore } from '@/stores/inbox'
import { useAuthStore } from '@/stores/auth'
import QRCode from 'qrcode'
import { validatePassword, PASSWORD_HINT } from '@/utils/password'
import { version as appVersion } from '../../../package.json'
const ui = useUiSettingsStore()
const inboxStore = useInboxStore()
const auth = useAuthStore()

const activeTab = ref('ui')

// 비밀번호 변경
const pwForm = ref({ current: '', newPw: '', confirm: '' })
const pwShow = ref({ current: false, newPw: false })
const pwSaving = ref(false)
const pwResult = ref({ message: '', success: false })

async function changePassword() {
  if (pwForm.value.newPw !== pwForm.value.confirm) return
  const strengthError = validatePassword(pwForm.value.newPw)
  if (strengthError) {
    pwResult.value = { message: strengthError, success: false }
    return
  }
  pwSaving.value = true
  pwResult.value = { message: '', success: false }
  try {
    await authApi.changePassword({ currentPassword: pwForm.value.current, newPassword: pwForm.value.newPw })
    pwForm.value = { current: '', newPw: '', confirm: '' }
    pwResult.value = { message: '비밀번호가 변경되었습니다.', success: true }
    setTimeout(() => { pwResult.value.message = '' }, 4000)
  } catch (e) {
    pwResult.value = { message: e || '비밀번호 변경에 실패했습니다.', success: false }
  } finally {
    pwSaving.value = false
  }
}

// 수신함 (최근 10건만 표시)
const inboxMessages = ref([])
const inboxLoading = ref(false)
const inboxUnread = ref(0)
const inboxProcessing = ref({})
const inboxActionDone = ref({})
const showInboxClearConfirm = ref(false)
const inboxClearing = ref(false)

// MFA 상태
const mfaEnabled = ref(false)
const mfaSetupMode = ref(false)
const mfaDisableMode = ref(false)
const mfaSetupData = ref(null)
const mfaCode = ref('')
const mfaSaving = ref(false)
const mfaResult = ref({ message: '', success: false })
const qrCanvas = ref(null)

onMounted(async () => {
  loadInbox()
  loadMfaStatus()
})

async function loadMfaStatus() {
  try {
    const res = await authApi.mfaStatus()
    mfaEnabled.value = res.data.mfaEnabled
  } catch {}
}

async function startMfaSetup() {
  mfaSaving.value = true
  mfaResult.value = { message: '', success: false }
  try {
    const res = await authApi.mfaSetup()
    mfaSetupData.value = res.data
    mfaSetupMode.value = true
    await nextTick()
    if (qrCanvas.value) {
      await QRCode.toCanvas(qrCanvas.value, res.data.qrCodeUri, { width: 200 })
    }
  } catch (e) {
    mfaResult.value = { message: e || 'MFA 설정에 실패했습니다.', success: false }
  } finally {
    mfaSaving.value = false
  }
}

async function handleMfaEnable() {
  mfaSaving.value = true
  mfaResult.value = { message: '', success: false }
  try {
    await authApi.mfaEnable(mfaCode.value)
    mfaEnabled.value = true
    mfaSetupMode.value = false
    mfaCode.value = ''
    mfaSetupData.value = null
    mfaResult.value = { message: 'MFA가 활성화되었습니다.', success: true }
    setTimeout(() => { mfaResult.value.message = '' }, 4000)
  } catch (e) {
    mfaResult.value = { message: e || '활성화에 실패했습니다.', success: false }
  } finally {
    mfaSaving.value = false
  }
}

async function handleMfaDisable() {
  mfaSaving.value = true
  mfaResult.value = { message: '', success: false }
  try {
    await authApi.mfaDisable(mfaCode.value)
    mfaEnabled.value = false
    mfaDisableMode.value = false
    mfaCode.value = ''
    mfaResult.value = { message: 'MFA가 비활성화되었습니다.', success: true }
    setTimeout(() => { mfaResult.value.message = '' }, 4000)
  } catch (e) {
    mfaResult.value = { message: e || '비활성화에 실패했습니다.', success: false }
  } finally {
    mfaSaving.value = false
  }
}

async function loadInbox() {
  inboxLoading.value = true
  try {
    const res = await inboxApi.list()
    const all = res.data || []
    inboxMessages.value = all.slice(0, 10)
    inboxUnread.value = all.filter(m => !m.read).length
  } catch {} finally {
    inboxLoading.value = false
  }
}

async function inboxMarkAllRead() {
  await inboxApi.markAllRead()
  inboxMessages.value.forEach(m => (m.read = true))
  inboxUnread.value = 0
  inboxStore.fetchUnread()
}

async function inboxClearAll() {
  inboxClearing.value = true
  try {
    await inboxApi.deleteAll()
    inboxMessages.value = []
    inboxUnread.value = 0
    showInboxClearConfirm.value = false
    inboxStore.fetchUnread()
  } finally {
    inboxClearing.value = false
  }
}

async function inboxApprove(msg) {
  inboxProcessing.value[msg.id] = true
  try {
    await inboxApi.approve(msg.id)
    inboxActionDone.value[msg.id] = 'approved'
    msg.read = true
    inboxUnread.value = Math.max(0, inboxUnread.value - 1)
    inboxStore.fetchUnread()
  } catch (e) {
    alert(e || '오류가 발생했습니다.')
  } finally {
    inboxProcessing.value[msg.id] = false
  }
}

async function inboxReject(msg) {
  inboxProcessing.value[msg.id] = true
  try {
    await inboxApi.reject(msg.id)
    inboxActionDone.value[msg.id] = 'rejected'
    msg.read = true
    inboxUnread.value = Math.max(0, inboxUnread.value - 1)
    inboxStore.fetchUnread()
  } catch (e) {
    alert(e || '오류가 발생했습니다.')
  } finally {
    inboxProcessing.value[msg.id] = false
  }
}

</script>
