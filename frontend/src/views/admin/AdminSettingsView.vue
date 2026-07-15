<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">설정관리</h1>
        <p class="text-sm text-gray-400 mt-0.5">보안 정책, Okta SSO, 시스템 전반을 설정합니다.</p>
      </div>
    </div>

    <!-- 탭 -->
    <div class="px-6 border-b" :class="isDark ? 'border-gray-700' : 'border-gray-200'">
      <div class="flex gap-1">
        <button v-for="tab in tabs" :key="tab.key"
          @click="activeTab = tab.key"
          class="px-4 py-3 text-sm font-semibold border-b-2 transition-colors"
          :class="activeTab === tab.key
            ? 'border-primary-500 text-primary-600'
            : 'border-transparent text-gray-500 hover:text-gray-700'">
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- ── 탭: 보안 설정 ── -->
    <div v-show="activeTab === 'security'" class="page-body">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

        <!-- 로그인 보안 정책 -->
        <div class="card">
          <h2 class="text-base font-bold text-gray-800 mb-1">보안 설정</h2>
          <p class="text-sm text-gray-400 mb-5">로그인 실패 시 계정 잠금 정책을 설정합니다.</p>
          <div class="space-y-4 max-w-sm">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">최대 로그인 실패 횟수</label>
              <div class="flex items-center gap-2">
                <input v-model.number="secCfg.maxAttempts" type="number" min="1" max="5" class="input w-24 text-sm" />
                <span class="text-sm text-gray-400">회 초과 시 계정 잠금</span>
              </div>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">기본 잠금 시간 (분)</label>
              <div class="flex items-center gap-2">
                <input v-model.number="secCfg.lockoutMinutes" type="number" min="1" max="1440" class="input w-24 text-sm" />
                <span class="text-sm text-gray-400">분, 재시도 실패 시 2배씩 증가</span>
              </div>
            </div>
            <div class="flex items-center gap-3 pt-1">
              <button @click="saveSecurityConfig" :disabled="secSaving"
                class="btn-primary px-6 py-2 text-sm rounded-xl disabled:opacity-50">
                {{ secSaving ? '저장 중...' : '저장' }}
              </button>
              <span v-if="secSaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
            </div>
          </div>
        </div>

        <!-- Okta SSO -->
        <div class="card">
          <div class="flex items-center justify-between mb-1">
            <h2 class="text-base font-bold text-gray-800 flex items-center gap-2">
              <svg class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="none">
                <path d="M12 2C6.477 2 2 6.477 2 12s4.477 10 10 10 10-4.477 10-10S17.523 2 12 2z" fill="#007DC1"/>
                <path d="M12 6.5a5.5 5.5 0 100 11 5.5 5.5 0 000-11z" fill="#fff"/>
                <path d="M12 9a3 3 0 100 6 3 3 0 000-6z" fill="#007DC1"/>
              </svg>
              Okta SSO 연동 설정
            </h2>
            <span v-if="oktaStatus === 'ok'" class="inline-flex items-center gap-1.5 text-xs font-bold px-2.5 py-1 rounded-full bg-green-100 text-green-700">
              <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
              연결됨
            </span>
            <span v-else-if="oktaStatus === 'fail'" class="inline-flex items-center gap-1.5 text-xs font-bold px-2.5 py-1 rounded-full bg-red-100 text-red-700">
              연결 실패
            </span>
          </div>
          <p class="text-sm text-gray-400 mb-5">Okta OIDC 앱 정보를 입력하면 로그인 화면에 "Okta로 로그인" 버튼이 표시됩니다.</p>

          <div class="space-y-4 max-w-2xl">
            <div class="flex items-center gap-3">
              <button
                @click="oktaCfg.enabled = !oktaCfg.enabled"
                class="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none"
                :class="oktaCfg.enabled ? 'bg-primary-500' : 'bg-gray-200'">
                <span
                  class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200"
                  :class="oktaCfg.enabled ? 'translate-x-5' : 'translate-x-0'"/>
              </button>
              <span class="text-sm font-semibold text-gray-700">Okta SSO 활성화</span>
              <span class="text-xs px-2 py-0.5 rounded-full font-semibold"
                :class="oktaCfg.enabled ? 'bg-primary-100 text-primary-700' : 'bg-gray-100 text-gray-500'">
                {{ oktaCfg.enabled ? '활성화' : '비활성화' }}
              </span>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">
                Client ID <span class="text-red-500">*</span>
              </label>
              <input v-model="oktaCfg.clientId" type="text" class="input w-full font-mono text-sm"
                placeholder="0oaXXXXXXXXXXXXXXXXX" />
              <p class="text-xs text-gray-400 mt-1">Okta 관리자 콘솔 → Applications → 앱 선택 → General 탭에서 확인</p>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">
                Issuer URI <span class="text-red-500">*</span>
              </label>
              <input v-model="oktaCfg.issuer" type="url" class="input w-full font-mono text-sm"
                placeholder="https://dev-XXXXXX.okta.com/oauth2/default" />
              <p class="text-xs text-gray-400 mt-1">Security → API → Authorization Servers → default의 Issuer URI</p>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">
                Redirect URI <span class="text-red-500">*</span>
              </label>
              <input v-model="oktaCfg.redirectUri" type="url" class="input w-full font-mono text-sm"
                placeholder="https://yourdomain.com/auth/okta/callback" />
              <p class="text-xs text-gray-400 mt-1">Okta 앱의 Sign-in redirect URIs에 등록한 값과 정확히 일치해야 합니다.</p>
            </div>

            <div class="flex items-start gap-2 p-3 rounded-xl bg-amber-50 border border-amber-200 text-sm text-amber-800">
              <svg class="w-4 h-4 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              <span>DB 설정이 환경변수(<code class="font-mono bg-amber-100 px-1 rounded">OKTA_*</code>)보다 우선 적용됩니다. 비워두면 환경변수 값을 사용합니다.</span>
            </div>

            <div class="flex items-center gap-3 flex-wrap pt-1">
              <button @click="saveOktaConfig" :disabled="oktaSaving"
                class="btn-primary px-6 py-2 text-sm rounded-xl disabled:opacity-50">
                {{ oktaSaving ? '저장 중...' : '저장' }}
              </button>
              <button @click="testOktaConnection" :disabled="oktaTesting"
                class="px-4 py-2 text-sm rounded-xl border-2 border-gray-200 bg-white text-gray-600 hover:border-gray-300 transition-all disabled:opacity-50">
                {{ oktaTesting ? '연결 확인 중...' : '연결 테스트' }}
              </button>
              <span v-if="oktaSaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
              <span v-if="oktaTestMsg" class="text-sm font-semibold"
                :class="oktaStatus === 'ok' ? 'text-green-600' : 'text-red-500'">
                {{ oktaTestMsg }}
              </span>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- ── 탭: AI 설정 ── -->
    <div v-show="activeTab === 'ai'" class="page-body">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

        <!-- LLM 프로바이더 선택 + 저장/테스트 -->
        <div class="card lg:col-span-2">
          <div class="flex items-center justify-between mb-1">
            <h2 class="text-base font-bold text-gray-800">AI LLM 연동 설정</h2>
          </div>
          <p class="text-sm text-gray-400 mb-4">분석 기능에 사용할 LLM 프로바이더를 선택하고 연결 정보를 입력하세요.</p>
          <div class="flex items-center gap-3 mb-4">
            <button v-for="p in aiProviders" :key="p.key"
              @click="aiCfg.provider = p.key"
              class="flex-1 py-2.5 rounded-xl border-2 text-sm font-semibold transition-all flex items-center justify-center gap-2"
              :class="aiCfg.provider === p.key
                ? 'border-primary-500 bg-primary-50 text-primary-600'
                : 'border-gray-200 bg-white text-gray-500 hover:border-gray-300'">
              <span>{{ p.icon }}</span>{{ p.label }}
            </button>
          </div>
          <div class="flex items-center gap-3 flex-wrap">
            <button @click="saveAiConfig" :disabled="aiSaving"
              class="btn-primary text-sm px-6 py-2 disabled:opacity-50">
              {{ aiSaving ? '저장 중...' : '저장' }}
            </button>
            <button @click="testAiConnection" :disabled="aiTesting"
              class="px-4 py-2 text-sm rounded-xl border-2 border-gray-200 bg-white text-gray-600 hover:border-gray-300 transition-all disabled:opacity-50">
              {{ aiTesting ? '테스트 중...' : '연결 테스트' }}
            </button>
            <span v-if="aiSaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
            <span v-if="aiTestResult.message" class="text-sm font-semibold"
              :class="aiTestResult.success ? 'text-green-600' : 'text-red-500'">
              {{ aiTestResult.message }}
            </span>
          </div>
        </div>

        <!-- OpenAI -->
        <div class="card transition-opacity" :class="aiCfg.provider !== 'openai' ? 'opacity-40' : ''">
          <h2 class="text-base font-bold text-gray-800 mb-1 flex items-center gap-2">
            🤖 OpenAI
            <span v-if="aiCfg.provider === 'openai'" class="ml-auto text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 font-semibold">사용 중</span>
          </h2>
          <p class="text-sm text-gray-400 mb-4">OpenAI API를 통해 GPT 모델을 사용합니다.</p>
          <div class="space-y-3">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">API Key <span class="text-red-500">*</span></label>
              <input v-model="aiCfg.openai.apiKey" type="password" class="input w-full font-mono text-sm"
                placeholder="sk-..." autocomplete="off" />
              <p class="text-xs text-gray-400 mt-1">platform.openai.com에서 발급</p>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">모델</label>
              <select v-model="aiCfg.openai.model" class="input w-full text-sm">
                <option value="gpt-4o">gpt-4o</option>
                <option value="gpt-4o-mini">gpt-4o-mini</option>
                <option value="gpt-4-turbo">gpt-4-turbo</option>
                <option value="gpt-3.5-turbo">gpt-3.5-turbo</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Claude -->
        <div class="card transition-opacity" :class="aiCfg.provider !== 'claude' ? 'opacity-40' : ''">
          <h2 class="text-base font-bold text-gray-800 mb-1 flex items-center gap-2">
            🧠 Claude (Anthropic)
            <span v-if="aiCfg.provider === 'claude'" class="ml-auto text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 font-semibold">사용 중</span>
          </h2>
          <p class="text-sm text-gray-400 mb-4">Anthropic의 Claude 모델을 사용합니다.</p>
          <div class="space-y-3">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">API Key <span class="text-red-500">*</span></label>
              <input v-model="aiCfg.claude.apiKey" type="password" class="input w-full font-mono text-sm"
                placeholder="sk-ant-..." autocomplete="off" />
              <p class="text-xs text-gray-400 mt-1">console.anthropic.com에서 발급</p>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">모델</label>
              <select v-model="aiCfg.claude.model" class="input w-full text-sm">
                <option value="claude-sonnet-4-6">claude-sonnet-4-6 (추천)</option>
                <option value="claude-haiku-4-5-20251001">claude-haiku-4-5</option>
                <option value="claude-opus-4-8">claude-opus-4-8</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Ollama -->
        <div class="card lg:col-span-2 transition-opacity" :class="aiCfg.provider !== 'ollama' ? 'opacity-40' : ''">
          <h2 class="text-base font-bold text-gray-800 mb-1 flex items-center gap-2">
            🦙 Ollama (로컬 모델)
            <span v-if="aiCfg.provider === 'ollama'" class="ml-auto text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 font-semibold">사용 중</span>
          </h2>
          <p class="text-sm text-gray-400 mb-4">자체 서버에 설치된 Ollama를 통해 로컬 LLM을 사용합니다. 인터넷 연결 없이 동작합니다.</p>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">Base URL <span class="text-red-500">*</span></label>
              <input v-model="aiCfg.ollama.baseUrl" type="url" class="input w-full font-mono text-sm"
                placeholder="http://localhost:11434" />
              <p class="text-xs text-gray-400 mt-1">Ollama 서버 주소 (기본: http://localhost:11434)</p>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">모델</label>
              <input v-model="aiCfg.ollama.model" type="text" class="input w-full font-mono text-sm"
                placeholder="llama3.2, qwen2.5, mistral 등" />
              <p class="text-xs text-gray-400 mt-1">설치된 모델명 (<code class="bg-gray-100 px-1 rounded">ollama list</code>로 확인)</p>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- ── 탭: 업종 설정 ── -->
    <div v-show="activeTab === 'industry'" class="page-body">
      <div class="card">
        <div class="flex items-center justify-between mb-1">
          <h2 class="text-base font-bold text-gray-800">우리 회사 적용 업종</h2>
          <div class="flex items-center gap-3">
            <span v-if="industrySaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
            <button @click="saveIndustryConfig" :disabled="industrySaving"
              class="btn-primary text-sm px-6 py-2 disabled:opacity-50">
              {{ industrySaving ? '저장 중...' : '저장' }}
            </button>
          </div>
        </div>
        <p class="text-sm text-gray-400 mb-5">해당하는 업종을 선택하면 법령준수관리 화면에서 관련 법령만 우선 표시됩니다. 복수 선택 가능합니다.</p>

        <div class="space-y-5">
          <div v-for="cat in CATEGORIES" :key="cat.key">
            <!-- 카테고리 헤더 -->
            <div class="flex items-center gap-2 mb-2">
              <input type="checkbox" class="w-4 h-4 rounded text-primary-500 cursor-pointer"
                :checked="isCategoryAllSelected(cat.key)"
                :indeterminate="isCategoryPartialSelected(cat.key)"
                @change="toggleCategory(cat.key, $event.target.checked)" />
              <span class="text-xs font-bold px-2 py-0.5 rounded-full" :class="cat.color">{{ cat.label }}</span>
            </div>
            <!-- 업종 체크박스 목록 -->
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-1.5 pl-6">
              <label v-for="ind in INDUSTRIES.filter(i => i.category === cat.key)" :key="ind.id"
                class="flex items-center gap-2.5 px-3 py-2 rounded-xl border cursor-pointer transition-all"
                :class="selectedIndustryIds.includes(ind.id)
                  ? 'border-primary-300 bg-primary-50'
                  : 'border-gray-200 bg-white hover:border-gray-300'">
                <input type="checkbox" class="w-4 h-4 rounded text-primary-500 cursor-pointer flex-shrink-0"
                  :checked="selectedIndustryIds.includes(ind.id)"
                  @change="toggleIndustry(ind.id)" />
                <span class="text-sm text-gray-700">{{ ind.name }}</span>
                <span class="ml-auto text-xs text-gray-400 flex-shrink-0">법령 {{ ind.laws.length }}개</span>
              </label>
            </div>
          </div>
        </div>

        <div class="mt-5 pt-4 border-t border-gray-100 flex items-center justify-between">
          <p class="text-xs text-gray-400">
            선택된 업종: <strong class="text-gray-700">{{ selectedIndustryIds.length }}개</strong>
          </p>
          <div class="flex items-center gap-3">
            <button @click="selectedIndustryIds = []"
              class="text-xs text-gray-500 hover:text-gray-700 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-colors">
              전체 해제
            </button>
            <button @click="saveIndustryConfig" :disabled="industrySaving"
              class="btn-primary text-sm px-6 py-2 disabled:opacity-50">
              {{ industrySaving ? '저장 중...' : '저장' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 법령 개정정보 조회 기간 -->
      <div class="card mt-6">
        <div class="flex items-center justify-between mb-1">
          <h2 class="text-base font-bold text-gray-800">법령 개정정보 조회 기간</h2>
          <div class="flex items-center gap-3">
            <span v-if="legalDaysSaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
            <button @click="saveLegalDays" :disabled="legalDaysSaving"
              class="btn-primary text-sm px-6 py-2 disabled:opacity-50">
              {{ legalDaysSaving ? '저장 중...' : '저장' }}
            </button>
          </div>
        </div>
        <p class="text-sm text-gray-400 mb-5">
          대시보드 <strong>법령 개정</strong> 위젯에서 위 업종의 관련 법령 중 최근 개정·공포된 법령을 표시할 기간입니다.
          법제처 Open API로 실시간 조회하며, <strong>API 연동</strong> 탭의 법제처 API 키가 필요합니다.
        </p>

        <!-- 법제처 OC 키 미설정 경고 -->
        <div v-if="!lawApiKeyStored" class="mb-5 flex items-start gap-2 px-3 py-2.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-700 text-sm">
          <svg class="w-4 h-4 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
          </svg>
          <span>
            법제처 Open API 키(OC 코드)가 설정되지 않았습니다. 법령 개정정보를 조회하려면
            <button type="button" @click="activeTab = 'api'" class="underline font-semibold hover:text-amber-800">API 연동 탭</button>에서 먼저 API 키를 등록하세요.
          </span>
        </div>
        <div class="flex flex-wrap items-center gap-4">
          <label class="text-sm font-semibold text-gray-700 whitespace-nowrap">조회 기간</label>
          <div class="flex gap-1.5">
            <button v-for="p in LEGAL_PRESETS" :key="p.days"
              @click="legalDays = p.days"
              class="px-3 py-1 rounded-lg border text-xs font-semibold transition-all"
              :class="legalDays === p.days ? 'border-primary-500 bg-primary-50 text-primary-600' : 'border-gray-200 text-gray-500 hover:border-gray-300'">
              {{ p.label }}
            </button>
          </div>
          <div class="flex items-center gap-2 text-sm text-gray-400">
            <span>직접 입력</span>
            <input v-model.number="legalDays" type="number" min="1" max="365"
              class="input w-20 text-center text-sm" />
            <span>일</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 탭: 시스템 설정 ── -->
    <div v-show="activeTab === 'system'" class="page-body">
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

          <div class="mb-4">
            <label class="block text-sm font-semibold text-gray-700 mb-1">이메일 발송 링크 도메인 주소</label>
            <input v-model="linkBaseUrl" type="url" class="input w-full text-sm" placeholder="https://sec.example.com" />
            <p class="text-xs text-gray-400 mt-1">
              승인/거부·모의 악성메일 훈련 등 메일에 포함되는 링크의 기준 주소입니다.
              비워두면 서버 환경변수(<code class="font-mono bg-gray-100 px-1 rounded">APP_BASE_URL</code>, 기본 localhost)를 사용합니다.
              도메인만 입력하면 <code class="font-mono bg-gray-100 px-1 rounded">/api</code> 경로가 자동으로 붙습니다.
            </p>
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

    <!-- ── 탭: 회사정보 ── -->
    <div v-show="activeTab === 'company'" class="page-body">
      <div class="max-w-2xl">
        <div class="card">
          <h2 class="text-base font-bold text-gray-800 mb-1">회사정보 등록</h2>
          <p class="text-sm text-gray-400 mb-5">회사 기본 정보를 등록합니다. 회사명은 법령검토 보고서 등 문서 출력에 사용됩니다.</p>

          <div class="space-y-4">
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-1">회사명</label>
                <input v-model="companyCfg.name" type="text" class="input w-full text-sm" placeholder="주식회사 모노선" />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-1">대표자</label>
                <input v-model="companyCfg.ceo" type="text" class="input w-full text-sm" placeholder="홍길동" />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-1">홈페이지</label>
                <input v-model="companyCfg.homepage" type="url" class="input w-full text-sm" placeholder="https://www.example.com" />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-1">대표 이메일</label>
                <input v-model="companyCfg.email" type="email" class="input w-full text-sm" placeholder="contact@example.com" />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-1">대표 전화</label>
                <input v-model="companyCfg.phone" type="tel" class="input w-full text-sm" placeholder="02-0000-0000" />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-1">주소</label>
                <input v-model="companyCfg.address" type="text" class="input w-full text-sm" placeholder="서울특별시 ..." />
              </div>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">회사소개</label>
              <textarea v-model="companyCfg.intro" rows="4" class="input w-full text-sm"
                placeholder="회사에 대한 간단한 소개를 입력하세요."></textarea>
            </div>
            <div class="flex items-center gap-3 pt-1">
              <button @click="saveCompanyConfig" :disabled="companySaving"
                class="btn-primary px-6 py-2 text-sm rounded-xl disabled:opacity-50">
                {{ companySaving ? '저장 중...' : '저장' }}
              </button>
              <span v-if="companySaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 탭: API 연동 ── -->
    <div v-show="activeTab === 'api'" class="page-body">
      <div class="max-w-xl space-y-6">
        <!-- GitHub 연동 -->
        <div class="card">
          <h2 class="text-base font-bold text-gray-800 mb-1">GitHub 연동</h2>
          <p class="text-sm text-gray-400 mb-5">
            <strong>소스 취약점 점검</strong> 메뉴에서 GitHub 저장소의 Dependabot·Code scanning·Secret scanning 알림을 조회합니다.<br>
            Personal Access Token은 <strong>github.com → Settings → Developer settings</strong>에서 발급하세요.
            (필요 권한: repo 읽기, Dependabot alerts·Code scanning alerts·Secret scanning alerts 읽기)
          </p>

          <div v-if="ghCfg.tokenStored" class="flex items-center gap-2 mb-4 px-3 py-2.5 rounded-xl bg-green-50 border border-green-200 text-green-700 text-sm">
            <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
            </svg>
            토큰 등록됨 — <code class="font-mono">{{ ghCfg.tokenMasked }}</code>
          </div>
          <div v-else class="flex items-center gap-2 mb-4 px-3 py-2.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-700 text-sm">
            <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
            </svg>
            토큰 미등록 — 소스 취약점 점검을 사용할 수 없습니다
          </div>

          <div class="space-y-4 max-w-sm">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">
                {{ ghCfg.tokenStored ? '새 토큰으로 교체' : 'Personal Access Token' }}
              </label>
              <input v-model="ghTokenInput" type="password" class="input text-sm font-mono"
                :placeholder="ghCfg.tokenStored ? '새 토큰을 입력하면 교체됩니다' : 'ghp_... 또는 github_pat_...'"
                autocomplete="new-password" />
              <p class="text-xs text-gray-400 mt-1">토큰은 서버에만 저장되며 화면에는 마스킹되어 표시됩니다.</p>
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">기본 Owner / 조직 (선택)</label>
              <input v-model="ghCfg.owner" type="text" class="input text-sm font-mono" placeholder="예: monosun" />
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">API Base URL (선택 — GitHub Enterprise)</label>
              <input v-model="ghCfg.apiBaseUrl" type="text" class="input text-sm font-mono" placeholder="기본: https://api.github.com" />
            </div>
            <div class="flex items-center gap-3 flex-wrap">
              <button @click="saveGithubConfig" :disabled="ghSaving"
                class="btn-primary px-6 py-2 text-sm rounded-xl disabled:opacity-50">
                {{ ghSaving ? '저장 중...' : '저장' }}
              </button>
              <button @click="testGithubConnection" :disabled="ghTesting || !ghCfg.tokenStored"
                class="px-4 py-2 text-sm border border-gray-300 text-gray-700 rounded-xl hover:bg-gray-50 disabled:opacity-50 transition-colors">
                {{ ghTesting ? '테스트 중...' : '연결 테스트' }}
              </button>
              <button v-if="ghCfg.tokenStored" @click="clearGithubToken" :disabled="ghSaving"
                class="px-4 py-2 text-sm border border-red-300 text-red-600 rounded-xl hover:bg-red-50 disabled:opacity-50 transition-colors">
                토큰 삭제
              </button>
            </div>
            <p v-if="ghMsg" class="text-sm font-semibold" :class="ghOk ? 'text-green-600' : 'text-red-500'">
              {{ ghMsg }}
            </p>
          </div>
        </div>

        <div class="card">
          <h2 class="text-base font-bold text-gray-800 mb-1">법제처 Open API 키</h2>
          <p class="text-sm text-gray-400 mb-5">
            법령준수관리 Excel 다운로드 시 <strong>law.go.kr</strong> 실제 조문을 실시간으로 가져옵니다.<br>
            API 키는 <strong>open.law.go.kr</strong>에서 무료로 발급받을 수 있습니다.
          </p>
          <!-- 현재 등록 상태 -->
          <div v-if="lawApiKeyStored" class="mb-4 space-y-2">
            <div class="flex items-center gap-2 px-3 py-2.5 rounded-xl bg-green-50 border border-green-200 text-green-700 text-sm">
              <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
              </svg>
              API 키 등록됨 — law.go.kr 실시간 조문 조회 활성화
            </div>
            <!-- 현재 키 값 표시 -->
            <div class="px-3 py-2.5 rounded-xl bg-gray-50 border border-gray-200">
              <div class="flex items-center justify-between gap-3">
                <div class="flex-1 min-w-0">
                  <p class="text-[11px] text-gray-400 font-medium mb-1">현재 등록된 OC 코드 (API 키)</p>
                  <code class="block text-sm font-mono text-gray-800 truncate">
                    {{ showApiKey ? lawApiKeyValue : '·'.repeat(Math.min(lawApiKeyValue.length, 24)) }}
                  </code>
                </div>
                <button @click="showApiKey = !showApiKey"
                  class="flex-shrink-0 text-xs text-blue-600 hover:text-blue-800 font-semibold px-2 py-1 rounded hover:bg-blue-50 transition-colors">
                  {{ showApiKey ? '숨기기' : '보기' }}
                </button>
              </div>
            </div>
          </div>
          <div v-else class="flex items-center gap-2 mb-5 px-3 py-2.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-700 text-sm">
            <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
            </svg>
            API 키 미등록 — 내장 정적 데이터 사용 중
          </div>

          <div class="space-y-4 max-w-sm">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">
                {{ lawApiKeyStored ? '새 API 키로 교체' : 'API 키 (OC 코드)' }}
              </label>
              <input v-model="lawApiKeyInput" type="text" class="input text-sm font-mono"
                :placeholder="lawApiKeyStored ? '새 키를 입력하면 교체됩니다' : '예: your-oc-id'"
                autocomplete="off" />
              <p class="text-xs text-gray-400 mt-1">법제처 open.law.go.kr에서 발급한 기관 ID(OC 코드)를 입력하세요.</p>
            </div>
            <div class="flex items-center gap-3 flex-wrap">
              <button @click="saveLawApiKey"
                :disabled="lawApiKeySaving || !lawApiKeyInput.trim()"
                class="btn-primary px-6 py-2 text-sm rounded-xl disabled:opacity-50">
                {{ lawApiKeySaving ? '저장 중...' : (lawApiKeyStored ? '교체' : '저장') }}
              </button>
              <button v-if="lawApiKeyStored" @click="clearLawApiKey" :disabled="lawApiKeySaving"
                class="px-4 py-2 text-sm border border-red-300 text-red-600 rounded-xl hover:bg-red-50 disabled:opacity-50 transition-colors">
                API 키 삭제
              </button>
              <span v-if="lawApiKeyMsg" class="text-sm font-semibold"
                :class="lawApiKeyOk ? 'text-green-600' : 'text-red-500'">
                {{ lawApiKeyMsg }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useUiSettingsStore } from '@/stores/uiSettings'
import { securityConfigApi, appSettingApi, authApi, notificationConfigApi, aiApi, githubConfigApi } from '@/api/index.js'
import { INDUSTRIES, CATEGORIES } from '@/data/legalIndustries.js'
import { setLawApiKeySet } from '@/services/legalApiService.js'

const ui = useUiSettingsStore()
const isDark = computed(() => ui.sidebarStyle === 'dark')

const tabs = [
  { key: 'security', label: '보안 설정' },
  { key: 'system',   label: '시스템 설정' },
  { key: 'company',  label: '회사정보' },
  { key: 'ai',       label: 'AI 설정' },
  { key: 'industry', label: '업종 설정' },
  { key: 'api',      label: 'API 연동' },
]
const activeTab = ref('security')

// ── API 연동 ──────────────────────────────────────
const lawApiKeyInput  = ref('')
const lawApiKeyStored = ref(false)
const lawApiKeyValue  = ref('')   // DB에 저장된 실제 키 값
const showApiKey      = ref(false)
const lawApiKeyMsg    = ref('')
const lawApiKeyOk     = ref(false)
const lawApiKeySaving = ref(false)

// ── GitHub 연동 설정 ─────────────────────────────
const ghCfg = ref({ tokenStored: false, tokenMasked: '', owner: '', apiBaseUrl: '' })
const ghTokenInput = ref('')
const ghSaving = ref(false)
const ghTesting = ref(false)
const ghMsg = ref('')
const ghOk = ref(false)

async function loadGithubConfig() {
  try {
    const res = await githubConfigApi.get()
    const d = res.data || {}
    ghCfg.value = {
      tokenStored: !!d.tokenStored,
      tokenMasked: d.tokenMasked || '',
      owner: d.owner || '',
      apiBaseUrl: d.apiBaseUrl || ''
    }
  } catch {}
}

async function saveGithubConfig() {
  ghSaving.value = true
  ghMsg.value = ''
  try {
    const res = await githubConfigApi.save({
      token: ghTokenInput.value.trim() || null,
      owner: ghCfg.value.owner ?? '',
      apiBaseUrl: ghCfg.value.apiBaseUrl ?? ''
    })
    const d = res.data || {}
    ghCfg.value.tokenStored = !!d.tokenStored
    ghCfg.value.tokenMasked = d.tokenMasked || ''
    ghTokenInput.value = ''
    ghOk.value = true
    ghMsg.value = '저장되었습니다.'
    setTimeout(() => { ghMsg.value = '' }, 3000)
  } catch {
    ghOk.value = false
    ghMsg.value = '저장에 실패했습니다.'
  } finally {
    ghSaving.value = false
  }
}

async function clearGithubToken() {
  if (!confirm('GitHub 토큰을 삭제하시겠습니까? 소스 취약점 점검을 사용할 수 없게 됩니다.')) return
  ghSaving.value = true
  try {
    await githubConfigApi.save({ token: '-' })
    ghCfg.value.tokenStored = false
    ghCfg.value.tokenMasked = ''
    ghOk.value = false
    ghMsg.value = '토큰이 삭제되었습니다.'
    setTimeout(() => { ghMsg.value = '' }, 3000)
  } catch {
    ghMsg.value = '삭제에 실패했습니다.'
  } finally {
    ghSaving.value = false
  }
}

async function testGithubConnection() {
  ghTesting.value = true
  ghMsg.value = ''
  try {
    const res = await githubConfigApi.test()
    const d = res.data || {}
    ghOk.value = !!d.success
    ghMsg.value = d.message || (d.success ? '연결 성공' : '연결 실패')
  } catch (e) {
    ghOk.value = false
    ghMsg.value = e || '연결 테스트에 실패했습니다.'
  } finally {
    ghTesting.value = false
  }
}

async function loadLawApiKeyStatus() {
  try {
    const res = await appSettingApi.getAll()
    const val = (res.data || {}).lawApiKey || ''
    lawApiKeyStored.value = val.length > 0
    lawApiKeyValue.value  = val
  } catch {}
}

async function saveLawApiKey() {
  if (!lawApiKeyInput.value.trim()) return
  lawApiKeySaving.value = true
  try {
    await appSettingApi.update('lawApiKey', lawApiKeyInput.value.trim())
    setLawApiKeySet(true)
    lawApiKeyValue.value  = lawApiKeyInput.value.trim()
    lawApiKeyStored.value = true
    lawApiKeyInput.value  = ''
    lawApiKeyOk.value     = true
    lawApiKeyMsg.value    = '저장되었습니다.'
    setTimeout(() => { lawApiKeyMsg.value = '' }, 3000)
  } catch {
    lawApiKeyOk.value  = false
    lawApiKeyMsg.value = '저장에 실패했습니다.'
  } finally {
    lawApiKeySaving.value = false
  }
}

async function clearLawApiKey() {
  lawApiKeySaving.value = true
  try {
    await appSettingApi.update('lawApiKey', '')
    setLawApiKeySet(false)
    lawApiKeyStored.value = false
    lawApiKeyValue.value  = ''
    lawApiKeyInput.value  = ''
    showApiKey.value      = false
    lawApiKeyOk.value     = false
    lawApiKeyMsg.value    = '삭제되었습니다.'
    setTimeout(() => { lawApiKeyMsg.value = '' }, 3000)
  } catch {
    lawApiKeyMsg.value = '삭제에 실패했습니다.'
  } finally {
    lawApiKeySaving.value = false
  }
}

// ── AI 설정 ──────────────────────────────────────
const aiProviders = [
  { key: 'openai', label: 'OpenAI', icon: '🤖' },
  { key: 'claude', label: 'Claude', icon: '🧠' },
  { key: 'ollama', label: 'Ollama', icon: '🦙' },
]
const aiCfg = ref({
  provider: 'openai',
  openai:  { apiKey: '', model: 'gpt-4o' },
  claude:  { apiKey: '', model: 'claude-sonnet-4-6' },
  ollama:  { baseUrl: 'http://localhost:11434', model: 'llama3.2' },
})
const aiSaving = ref(false)
const aiSaved  = ref(false)
const aiTesting    = ref(false)
const aiTestResult = ref({ message: '', success: false })

async function loadAiConfig() {
  try {
    const res = await aiApi.getConfig()
    if (res.data) aiCfg.value = { ...aiCfg.value, ...res.data }
  } catch {}
}
async function saveAiConfig() {
  aiSaving.value = true; aiSaved.value = false
  try {
    await aiApi.saveConfig(aiCfg.value)
    aiSaved.value = true
    setTimeout(() => { aiSaved.value = false }, 3000)
  } catch { alert('AI 설정 저장에 실패했습니다.') }
  finally { aiSaving.value = false }
}
async function testAiConnection() {
  aiTesting.value = true; aiTestResult.value = { message: '', success: false }
  try {
    const res = await aiApi.testConnection()
    aiTestResult.value = { message: res.data?.message || '연결 성공', success: true }
  } catch (e) {
    aiTestResult.value = { message: typeof e === 'string' ? e : '연결 실패', success: false }
  } finally {
    aiTesting.value = false
    setTimeout(() => { aiTestResult.value.message = '' }, 6000)
  }
}

// ── 보안 설정 ──────────────────────────────────────
const secCfg = ref({ maxAttempts: 5, lockoutMinutes: 10 })
const secSaving = ref(false)
const secSaved = ref(false)

const oktaCfg = ref({ enabled: false, clientId: '', issuer: '', redirectUri: '' })
const oktaSaving = ref(false)
const oktaSaved = ref(false)
const oktaTesting = ref(false)
const oktaTestMsg = ref('')
const oktaStatus = ref('')

async function loadSecurityConfig() {
  try { const res = await securityConfigApi.get(); secCfg.value = res.data } catch {}
}
async function saveSecurityConfig() {
  secSaving.value = true; secSaved.value = false
  try {
    await securityConfigApi.update(secCfg.value)
    secSaved.value = true
    setTimeout(() => { secSaved.value = false }, 3000)
  } finally { secSaving.value = false }
}
async function loadOktaConfig() {
  try {
    const res = await appSettingApi.getAll()
    const s = res.data || {}
    oktaCfg.value = {
      enabled:     s['okta.enabled']     === 'true',
      clientId:    s['okta.client_id']   || '',
      issuer:      s['okta.issuer']      || '',
      redirectUri: s['okta.redirect_uri'] || ''
    }
    if (!oktaCfg.value.clientId && !oktaCfg.value.issuer) {
      const cfgRes = await authApi.oktaConfig()
      const c = cfgRes.data || {}
      if (c.enabled) {
        oktaCfg.value = { enabled: true, clientId: c.clientId || '', issuer: c.issuer || '', redirectUri: c.redirectUri || '' }
      }
    }
  } catch {}
}
async function saveOktaConfig() {
  oktaSaving.value = true; oktaSaved.value = false; oktaTestMsg.value = ''
  try {
    await appSettingApi.update('okta.enabled',      String(oktaCfg.value.enabled))
    await appSettingApi.update('okta.client_id',    oktaCfg.value.clientId)
    await appSettingApi.update('okta.issuer',       oktaCfg.value.issuer)
    await appSettingApi.update('okta.redirect_uri', oktaCfg.value.redirectUri)
    oktaSaved.value = true
    setTimeout(() => { oktaSaved.value = false }, 3000)
  } catch { alert('Okta 설정 저장에 실패했습니다.') }
  finally { oktaSaving.value = false }
}
async function testOktaConnection() {
  oktaTesting.value = true; oktaTestMsg.value = ''; oktaStatus.value = ''
  try {
    const res = await authApi.oktaTest()
    const r = res.data || {}
    oktaStatus.value = r.ok ? 'ok' : 'fail'
    oktaTestMsg.value = r.message || ''
  } catch (e) {
    oktaStatus.value = 'fail'
    oktaTestMsg.value = typeof e === 'string' ? e : '연결 테스트 실패'
  } finally {
    oktaTesting.value = false
    setTimeout(() => { oktaTestMsg.value = ''; oktaStatus.value = '' }, 6000)
  }
}

// ── 시스템 설정 ──────────────────────────────────────
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
const linkBaseUrl = ref('')   // 이메일 발송 링크 도메인 (app.base_url)

const DEFAULT_RSS_FEEDS = [
  { url: 'https://knvd.krcert.or.kr/rss/security/info',   category: 'vuln',   label: '취약점 정보' },
  { url: 'https://knvd.krcert.or.kr/rss/security/notice', category: 'notice', label: '보안공지' }
]
const rssDays  = ref(7)
const rssFeeds = ref(DEFAULT_RSS_FEEDS.map(f => ({ ...f })))
const rssSaving = ref(false)
const rssSaved  = ref(false)

function onLogoUpload(e) {
  const file = e.target.files?.[0]; if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => ui.setLogoUrl(ev.target.result)
  reader.readAsDataURL(file); e.target.value = ''
}
async function onSaveLogoToServer() {
  const url = ui.effectiveLogoUrl(); if (!url) return
  logoSaving.value = true; logoSaved.value = false
  try { await ui.saveLogoToServer(url); logoSaved.value = true; setTimeout(() => { logoSaved.value = false }, 3000) }
  catch { alert('서버 저장에 실패했습니다.') }
  finally { logoSaving.value = false }
}
async function onSaveLogoTextToServer() {
  const text = ui.logoText || ui.dbLogoText; if (!text) return
  logoTextSaving.value = true
  try { await ui.saveLogoTextToServer(text) }
  catch { alert('서버 저장에 실패했습니다.') }
  finally { logoTextSaving.value = false }
}
async function onSaveSessionTimeout() {
  const m = Math.min(1440, Math.max(5, sessionTimeoutInput.value || 60))
  sessionTimeoutInput.value = m; sessionTimeoutSaving.value = true; sessionTimeoutSaved.value = false
  try { await ui.saveSessionTimeoutToServer(m); sessionTimeoutSaved.value = true; setTimeout(() => { sessionTimeoutSaved.value = false }, 3000) }
  catch { alert('저장에 실패했습니다.') }
  finally { sessionTimeoutSaving.value = false }
}
async function saveNotifyConfig() {
  notifySaving.value = true; notifySaved.value = false
  try {
    await notificationConfigApi.update({ method: notifyCfg.value.method, approvalEmail: notifyCfg.value.approvalEmail, slackWebhookUrl: notifyCfg.value.slackWebhookUrl })
    await appSettingApi.update('app.base_url', linkBaseUrl.value.trim())
    notifySaved.value = true; setTimeout(() => { notifySaved.value = false }, 3000)
  } finally { notifySaving.value = false }
}
function rssAddFeed() { rssFeeds.value.push({ url: '', category: '', label: '' }) }
function rssRemoveFeed(idx) { rssFeeds.value.splice(idx, 1) }
function resetRssDefaults() { rssDays.value = 7; rssFeeds.value = DEFAULT_RSS_FEEDS.map(f => ({ ...f })) }
async function saveRssConfig() {
  rssSaving.value = true; rssSaved.value = false
  try {
    await appSettingApi.update('rss.days', String(rssDays.value))
    await appSettingApi.update('rss.feeds', JSON.stringify(rssFeeds.value))
    rssSaved.value = true; setTimeout(() => { rssSaved.value = false }, 3000)
  } catch { alert('RSS 설정 저장에 실패했습니다.') }
  finally { rssSaving.value = false }
}
async function loadRssConfig() {
  try {
    const res = await appSettingApi.getAll()
    const s = res.data || {}
    if (s['rss.days']) rssDays.value = parseInt(s['rss.days']) || 7
    if (s['rss.feeds']) { const feeds = JSON.parse(s['rss.feeds']); if (Array.isArray(feeds) && feeds.length > 0) rssFeeds.value = feeds }
    linkBaseUrl.value = s['app.base_url'] || ''
  } catch {}
}

// ── 회사정보 ──────────────────────────────────────
const companyCfg = ref({ name: '', ceo: '', homepage: '', email: '', phone: '', address: '', intro: '' })
const companySaving = ref(false)
const companySaved  = ref(false)

async function loadCompanyConfig() {
  try {
    const res = await appSettingApi.getAll()
    const s = res.data || {}
    companyCfg.value = {
      name:     s['company.name']     || '',
      ceo:      s['company.ceo']      || '',
      homepage: s['company.homepage'] || '',
      email:    s['company.email']    || '',
      phone:    s['company.phone']    || '',
      address:  s['company.address']  || '',
      intro:    s['company.intro']    || '',
    }
  } catch {}
}

async function saveCompanyConfig() {
  companySaving.value = true; companySaved.value = false
  try {
    const c = companyCfg.value
    await appSettingApi.update('company.name',     c.name.trim())
    await appSettingApi.update('company.ceo',      c.ceo.trim())
    await appSettingApi.update('company.homepage', c.homepage.trim())
    await appSettingApi.update('company.email',    c.email.trim())
    await appSettingApi.update('company.phone',    c.phone.trim())
    await appSettingApi.update('company.address',  c.address.trim())
    await appSettingApi.update('company.intro',    c.intro.trim())
    companySaved.value = true
    setTimeout(() => { companySaved.value = false }, 3000)
  } catch { alert('회사정보 저장에 실패했습니다.') }
  finally { companySaving.value = false }
}

// ── 업종 설정 ──────────────────────────────────────
const selectedIndustryIds = ref([])
const industrySaving = ref(false)
const industrySaved  = ref(false)

// 법령 개정정보 조회 기간
const LEGAL_PRESETS = [
  { label: '1주일',  days: 7 },
  { label: '1개월',  days: 30 },
  { label: '3개월',  days: 90 },
  { label: '6개월',  days: 180 },
  { label: '12개월', days: 365 },
]
const legalDays = ref(30)
const legalDaysSaving = ref(false)
const legalDaysSaved  = ref(false)

async function saveLegalDays() {
  legalDaysSaving.value = true; legalDaysSaved.value = false
  try {
    const d = Math.min(365, Math.max(1, legalDays.value || 30))
    legalDays.value = d
    await appSettingApi.update('legal.days', String(d))
    legalDaysSaved.value = true; setTimeout(() => { legalDaysSaved.value = false }, 3000)
  } catch { alert('법령 조회 기간 저장에 실패했습니다.') }
  finally { legalDaysSaving.value = false }
}

function toggleIndustry(id) {
  const idx = selectedIndustryIds.value.indexOf(id)
  if (idx === -1) selectedIndustryIds.value = [...selectedIndustryIds.value, id]
  else selectedIndustryIds.value = selectedIndustryIds.value.filter(x => x !== id)
}

function isCategoryAllSelected(catKey) {
  return INDUSTRIES.filter(i => i.category === catKey).every(i => selectedIndustryIds.value.includes(i.id))
}
function isCategoryPartialSelected(catKey) {
  const inds = INDUSTRIES.filter(i => i.category === catKey)
  const some = inds.some(i => selectedIndustryIds.value.includes(i.id))
  return some && !isCategoryAllSelected(catKey)
}
function toggleCategory(catKey, checked) {
  const ids = INDUSTRIES.filter(i => i.category === catKey).map(i => i.id)
  if (checked) {
    const merged = new Set([...selectedIndustryIds.value, ...ids])
    selectedIndustryIds.value = [...merged]
  } else {
    selectedIndustryIds.value = selectedIndustryIds.value.filter(id => !ids.includes(id))
  }
}

async function loadIndustryConfig() {
  try {
    const res = await appSettingApi.getAll()
    const raw = res.data?.['company.industries']
    if (raw) {
      const ids = JSON.parse(raw)
      if (Array.isArray(ids)) selectedIndustryIds.value = ids
    }
    const ld = res.data?.['legal.days']
    if (ld) legalDays.value = parseInt(ld) || 30
  } catch {}
}

async function saveIndustryConfig() {
  industrySaving.value = true; industrySaved.value = false
  try {
    await appSettingApi.update('company.industries', JSON.stringify(selectedIndustryIds.value))
    industrySaved.value = true
    setTimeout(() => { industrySaved.value = false }, 3000)
  } catch { alert('업종 설정 저장에 실패했습니다.') }
  finally { industrySaving.value = false }
}

onMounted(async () => {
  loadAiConfig()
  loadCompanyConfig()
  loadIndustryConfig()
  loadSecurityConfig()
  loadOktaConfig()
  loadLawApiKeyStatus()
  loadGithubConfig()
  try { const data = await notificationConfigApi.get(); notifyCfg.value = { ...notifyCfg.value, ...data } } catch {}
  loadRssConfig()
})
</script>
