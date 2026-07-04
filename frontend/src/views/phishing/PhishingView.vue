<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ $t('phishing.title') }}</h1>
        <p class="text-sm text-gray-400 mt-0.5">{{ $t('phishing.subtitle') }}</p>
      </div>
    </div>

    <div class="page-body">
      <!-- Tabs -->
      <div class="flex gap-1 p-1 bg-gray-100 rounded-xl mb-6 w-fit">
        <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
          :class="['px-4 py-2 rounded-lg text-sm font-semibold transition-all',
            activeTab === tab.key ? 'bg-white text-primary-600 shadow-sm' : 'text-gray-500 hover:text-gray-700']">
          {{ tab.label }}
        </button>
      </div>

      <!-- ── Tab 1: Templates ─────────────────────────────────────────── -->
      <div v-if="activeTab === 'templates'">
        <div class="flex justify-between items-center mb-4">
          <p class="text-sm text-gray-500">총 {{ templates.length }}개 템플릿</p>
          <button class="btn-primary text-sm" @click="openTemplateModal()">+ {{ $t('phishing.addTemplate') }}</button>
        </div>
        <div class="card overflow-hidden p-0">
          <table class="w-full text-sm">
            <thead>
              <tr class="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider">
                <th class="px-4 py-3 text-left">{{ $t('phishing.templateName') }}</th>
                <th class="px-4 py-3 text-left">카테고리</th>
                <th class="px-4 py-3 text-left">난이도</th>
                <th class="px-4 py-3 text-left">발신자</th>
                <th class="px-4 py-3 text-left">생성일</th>
                <th class="px-4 py-3 text-center">작업</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-if="!templates.length">
                <td colspan="6" class="py-12 text-center text-gray-400 text-sm">등록된 템플릿이 없습니다.</td>
              </tr>
              <tr v-for="t in templates" :key="t.id" class="hover:bg-gray-50 transition-colors">
                <td class="px-4 py-3 font-medium text-gray-900">
                  <button @click="previewTemplate(t)" class="hover:text-primary-600 text-left">{{ t.name }}</button>
                </td>
                <td class="px-4 py-3">
                  <span class="px-2 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700">{{ t.category }}</span>
                </td>
                <td class="px-4 py-3">
                  <span :class="difficultyClass(t.difficulty)" class="px-2 py-0.5 rounded-full text-xs font-semibold">
                    {{ difficultyLabel(t.difficulty) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-gray-600">{{ t.senderName }} &lt;{{ t.senderEmail }}&gt;</td>
                <td class="px-4 py-3 text-gray-400 text-xs">{{ fmtDate(t.createdAt) }}</td>
                <td class="px-4 py-3">
                  <div class="flex justify-center gap-2">
                    <button @click="previewTemplate(t)" class="text-xs text-blue-600 hover:underline">미리보기</button>
                    <button @click="openTemplateModal(t)" class="text-xs text-gray-600 hover:underline">수정</button>
                    <button @click="confirmDeleteTemplate(t)" class="text-xs text-red-500 hover:underline">삭제</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ── Tab 2: Targets ──────────────────────────────────────────── -->
      <div v-else-if="activeTab === 'targets'">
        <div class="flex justify-between items-center mb-4">
          <p class="text-sm text-gray-500">총 {{ targets.length }}명 대상</p>
          <button class="btn-primary text-sm" @click="openTargetModal()">+ {{ $t('phishing.addTarget') }}</button>
        </div>
        <div class="card overflow-hidden p-0">
          <table class="w-full text-sm">
            <thead>
              <tr class="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider">
                <th class="px-4 py-3 text-left">이름</th>
                <th class="px-4 py-3 text-left">이메일</th>
                <th class="px-4 py-3 text-left">부서</th>
                <th class="px-4 py-3 text-left">직책</th>
                <th class="px-4 py-3 text-center">상태</th>
                <th class="px-4 py-3 text-center">작업</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-if="!targets.length">
                <td colspan="6" class="py-12 text-center text-gray-400 text-sm">등록된 대상이 없습니다.</td>
              </tr>
              <tr v-for="t in targets" :key="t.id" class="hover:bg-gray-50 transition-colors">
                <td class="px-4 py-3 font-medium text-gray-900">{{ t.name }}</td>
                <td class="px-4 py-3 text-gray-600">{{ t.email }}</td>
                <td class="px-4 py-3 text-gray-500">{{ t.department || '—' }}</td>
                <td class="px-4 py-3 text-gray-500">{{ t.position || '—' }}</td>
                <td class="px-4 py-3 text-center">
                  <button @click="toggleTarget(t)"
                    :class="t.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-400'"
                    class="px-2.5 py-0.5 rounded-full text-xs font-semibold transition-colors">
                    {{ t.active ? '활성' : '비활성' }}
                  </button>
                </td>
                <td class="px-4 py-3">
                  <div class="flex justify-center gap-2">
                    <button @click="openTargetModal(t)" class="text-xs text-gray-600 hover:underline">수정</button>
                    <button @click="confirmDeleteTarget(t)" class="text-xs text-red-500 hover:underline">삭제</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ── Tab 3: Campaigns ────────────────────────────────────────── -->
      <div v-else-if="activeTab === 'campaigns'">
        <div class="flex justify-between items-center mb-4">
          <p class="text-sm text-gray-500">총 {{ campaigns.length }}개 훈련</p>
          <button class="btn-primary text-sm" @click="openCampaignModal()">+ {{ $t('phishing.addCampaign') }}</button>
        </div>
        <div class="card overflow-hidden p-0">
          <table class="w-full text-sm">
            <thead>
              <tr class="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider">
                <th class="px-4 py-3 text-left">훈련명</th>
                <th class="px-4 py-3 text-left">템플릿</th>
                <th class="px-4 py-3 text-center">대상</th>
                <th class="px-4 py-3 text-center">발송/열람/클릭</th>
                <th class="px-4 py-3 text-center">상태</th>
                <th class="px-4 py-3 text-left">생성일</th>
                <th class="px-4 py-3 text-center">작업</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-if="!campaigns.length">
                <td colspan="7" class="py-12 text-center text-gray-400 text-sm">등록된 훈련이 없습니다.</td>
              </tr>
              <tr v-for="c in campaigns" :key="c.id" class="hover:bg-gray-50 transition-colors">
                <td class="px-4 py-3 font-medium text-gray-900">
                  <button @click="viewCampaign(c.id)" class="hover:text-primary-600 text-left">{{ c.name }}</button>
                </td>
                <td class="px-4 py-3 text-gray-600 text-xs">{{ c.templateName }}</td>
                <td class="px-4 py-3 text-center font-semibold text-gray-700">{{ c.totalTargets }}</td>
                <td class="px-4 py-3 text-center text-xs space-x-1">
                  <span class="text-gray-500">{{ c.sentCount }}</span>
                  <span class="text-gray-300">/</span>
                  <span class="text-amber-600">{{ c.openedCount }}</span>
                  <span class="text-gray-300">/</span>
                  <span class="text-red-600 font-semibold">{{ c.clickedCount }}</span>
                </td>
                <td class="px-4 py-3 text-center">
                  <span :class="campaignStatusClass(c.status)" class="px-2 py-0.5 rounded-full text-xs font-semibold">
                    {{ campaignStatusLabel(c.status) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-gray-400 text-xs">{{ fmtDate(c.createdAt) }}</td>
                <td class="px-4 py-3">
                  <div class="flex justify-center gap-1.5 flex-wrap">
                    <button @click="viewCampaign(c.id)" class="text-xs text-blue-600 hover:underline">결과</button>
                    <button v-if="c.status === 'DRAFT'" @click="launchCampaign(c)"
                      class="text-xs text-green-600 hover:underline">실시</button>
                    <button v-if="c.status === 'RUNNING'" @click="completeCampaign(c)"
                      class="text-xs text-primary-600 hover:underline">완료</button>
                    <button v-if="c.status === 'DRAFT' || c.status === 'RUNNING'" @click="cancelCampaign(c)"
                      class="text-xs text-amber-600 hover:underline">취소</button>
                    <button v-if="c.status !== 'RUNNING'" @click="confirmDeleteCampaign(c)"
                      class="text-xs text-red-500 hover:underline">삭제</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ── Template Modal ────────────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="templateModal.open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 p-4 overflow-y-auto">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-2xl my-8">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <h3 class="font-bold text-gray-900">{{ templateModal.id ? '템플릿 수정' : '템플릿 등록' }}</h3>
            <button @click="templateModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <form @submit.prevent="saveTemplate" class="p-6 space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div class="col-span-2">
                <label class="label">템플릿명 <span class="text-red-500">*</span></label>
                <input v-model="templateModal.form.name" class="input" required />
              </div>
              <div>
                <label class="label">카테고리 <span class="text-red-500">*</span></label>
                <select v-model="templateModal.form.category" class="input" required>
                  <option value="">선택</option>
                  <option v-for="c in TEMPLATE_CATEGORIES" :key="c" :value="c">{{ c }}</option>
                </select>
              </div>
              <div>
                <label class="label">난이도</label>
                <select v-model="templateModal.form.difficulty" class="input">
                  <option value="EASY">쉬움</option>
                  <option value="MEDIUM">보통</option>
                  <option value="HARD">어려움</option>
                </select>
              </div>
              <div class="col-span-2">
                <label class="label">메일 제목 <span class="text-red-500">*</span></label>
                <input v-model="templateModal.form.subject" class="input" required />
              </div>
              <div>
                <label class="label">발신자 이름 <span class="text-red-500">*</span></label>
                <input v-model="templateModal.form.senderName" class="input" required />
              </div>
              <div>
                <label class="label">발신자 이메일 <span class="text-red-500">*</span></label>
                <input v-model="templateModal.form.senderEmail" type="email" class="input" required />
              </div>
              <div class="col-span-2">
                <label class="label">메일 본문 (HTML) <span class="text-red-500">*</span></label>
                <p class="text-xs text-gray-400 mb-1">
                  변수: <code class="bg-gray-100 px-1 rounded">{TARGET_NAME}</code>
                  <code class="bg-gray-100 px-1 rounded ml-1">{TARGET_EMAIL}</code>
                  <code class="bg-gray-100 px-1 rounded ml-1">{CLICK_URL}</code>
                  <code class="bg-gray-100 px-1 rounded ml-1">{OPEN_URL}</code>
                </p>
                <textarea v-model="templateModal.form.bodyHtml" class="input font-mono text-xs" rows="10" required></textarea>
              </div>
              <div class="col-span-2">
                <label class="label">설명</label>
                <input v-model="templateModal.form.description" class="input" />
              </div>
            </div>
            <div v-if="templateModal.error" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">{{ templateModal.error }}</div>
            <div class="flex justify-end gap-3 pt-2">
              <button type="button" @click="templateModal.open = false" class="btn-secondary">취소</button>
              <button type="submit" class="btn-primary" :disabled="templateModal.saving">
                {{ templateModal.saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- ── Template Preview Modal ────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="previewModal.open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 p-4 overflow-y-auto">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-2xl my-8">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <div>
              <h3 class="font-bold text-gray-900">{{ previewModal.template?.name }}</h3>
              <p class="text-xs text-gray-400 mt-0.5">제목: {{ previewModal.template?.subject }}</p>
            </div>
            <button @click="previewModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="p-6">
            <div class="border rounded-xl overflow-hidden">
              <div class="bg-gray-50 px-4 py-2 text-xs text-gray-500 border-b flex gap-4">
                <span>보내는 사람: {{ previewModal.template?.senderName }} &lt;{{ previewModal.template?.senderEmail }}&gt;</span>
              </div>
              <div class="p-4 max-h-96 overflow-y-auto" v-html="previewModal.template?.bodyHtml"></div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ── Target Modal ──────────────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="targetModal.open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-md">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <h3 class="font-bold text-gray-900">{{ targetModal.id ? '대상 수정' : '대상 등록' }}</h3>
            <button @click="targetModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <form @submit.prevent="saveTarget" class="p-6 space-y-4">
            <div>
              <label class="label">이름 <span class="text-red-500">*</span></label>
              <input v-model="targetModal.form.name" class="input" required />
            </div>
            <div>
              <label class="label">이메일 <span class="text-red-500">*</span></label>
              <input v-model="targetModal.form.email" type="email" class="input" required />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">부서</label>
                <input v-model="targetModal.form.department" class="input" />
              </div>
              <div>
                <label class="label">직책</label>
                <input v-model="targetModal.form.position" class="input" />
              </div>
            </div>
            <div v-if="targetModal.error" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">{{ targetModal.error }}</div>
            <div class="flex justify-end gap-3 pt-2">
              <button type="button" @click="targetModal.open = false" class="btn-secondary">취소</button>
              <button type="submit" class="btn-primary" :disabled="targetModal.saving">
                {{ targetModal.saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- ── Campaign Modal ────────────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="campaignModal.open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 p-4 overflow-y-auto">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-xl my-8">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <h3 class="font-bold text-gray-900">모의훈련 등록</h3>
            <button @click="campaignModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <form @submit.prevent="saveCampaign" class="p-6 space-y-4">
            <div>
              <label class="label">훈련명 <span class="text-red-500">*</span></label>
              <input v-model="campaignModal.form.name" class="input" required />
            </div>
            <div>
              <label class="label">악성메일 템플릿 <span class="text-red-500">*</span></label>
              <select v-model="campaignModal.form.templateId" class="input" required>
                <option value="">템플릿 선택</option>
                <option v-for="t in templates" :key="t.id" :value="t.id">{{ t.name }}</option>
              </select>
            </div>
            <div>
              <label class="label">발송 예정일시</label>
              <input v-model="campaignModal.form.scheduledAt" type="datetime-local" class="input" />
            </div>
            <div>
              <label class="label">설명</label>
              <textarea v-model="campaignModal.form.description" class="input" rows="2"></textarea>
            </div>
            <div>
              <label class="label">발송 대상 <span class="text-red-500">*</span></label>
              <p class="text-xs text-gray-400 mb-2">선택: {{ campaignModal.form.targetIds.length }}명</p>
              <div class="border rounded-xl max-h-48 overflow-y-auto divide-y">
                <label v-for="t in activeTargets" :key="t.id"
                  class="flex items-center gap-3 px-4 py-2.5 hover:bg-gray-50 cursor-pointer">
                  <input type="checkbox" :value="t.id" v-model="campaignModal.form.targetIds"
                    class="w-4 h-4 text-primary-600 rounded" />
                  <span class="text-sm">
                    <span class="font-medium text-gray-900">{{ t.name }}</span>
                    <span class="text-gray-400 ml-2">{{ t.email }}</span>
                    <span v-if="t.department" class="text-gray-400 text-xs ml-1">({{ t.department }})</span>
                  </span>
                </label>
                <div v-if="!activeTargets.length" class="px-4 py-6 text-center text-gray-400 text-sm">
                  활성 대상이 없습니다.
                </div>
              </div>
              <div class="flex gap-2 mt-2">
                <button type="button" @click="selectAllTargets" class="text-xs text-primary-600 hover:underline">전체 선택</button>
                <span class="text-gray-300">|</span>
                <button type="button" @click="campaignModal.form.targetIds = []" class="text-xs text-gray-500 hover:underline">전체 해제</button>
              </div>
            </div>
            <div v-if="campaignModal.error" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">{{ campaignModal.error }}</div>
            <div class="flex justify-end gap-3 pt-2">
              <button type="button" @click="campaignModal.open = false" class="btn-secondary">취소</button>
              <button type="submit" class="btn-primary" :disabled="campaignModal.saving">
                {{ campaignModal.saving ? '등록 중...' : '등록' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- ── Campaign Results Modal ────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="resultModal.open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 p-4 overflow-y-auto">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-3xl my-8">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <div>
              <h3 class="font-bold text-gray-900">{{ resultModal.detail?.campaign?.name }}</h3>
              <div class="flex gap-4 mt-1 text-xs text-gray-500">
                <span>템플릿: {{ resultModal.detail?.campaign?.templateName }}</span>
                <span>대상: {{ resultModal.detail?.campaign?.totalTargets }}명</span>
              </div>
            </div>
            <button @click="resultModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="p-6">
            <!-- Summary -->
            <div class="grid grid-cols-4 gap-4 mb-6">
              <div v-for="stat in resultStats" :key="stat.label"
                class="bg-gray-50 rounded-xl p-4 text-center">
                <p class="text-2xl font-bold" :class="stat.color">{{ stat.value }}</p>
                <p class="text-xs text-gray-500 mt-1">{{ stat.label }}</p>
                <p class="text-xs font-semibold mt-0.5" :class="stat.color">{{ stat.rate }}%</p>
              </div>
            </div>
            <!-- Per-target table -->
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="bg-gray-50 text-gray-500 text-xs uppercase">
                    <th class="px-3 py-2 text-left">이름</th>
                    <th class="px-3 py-2 text-left">이메일</th>
                    <th class="px-3 py-2 text-left">부서</th>
                    <th class="px-3 py-2 text-center">발송</th>
                    <th class="px-3 py-2 text-center">열람</th>
                    <th class="px-3 py-2 text-center">클릭</th>
                    <th class="px-3 py-2 text-center">신고</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                  <tr v-for="r in resultModal.detail?.results" :key="r.id" class="hover:bg-gray-50">
                    <td class="px-3 py-2 font-medium text-gray-900">{{ r.targetName }}</td>
                    <td class="px-3 py-2 text-gray-600 text-xs">{{ r.targetEmail }}</td>
                    <td class="px-3 py-2 text-gray-500 text-xs">{{ r.department || '—' }}</td>
                    <td class="px-3 py-2 text-center">
                      <span v-if="r.sentAt" class="text-green-600" title="발송 완료">✓</span>
                      <span v-else class="text-gray-300">—</span>
                    </td>
                    <td class="px-3 py-2 text-center">
                      <span v-if="r.openedAt" class="text-amber-500 font-semibold" title="열람함">●</span>
                      <span v-else class="text-gray-300">—</span>
                    </td>
                    <td class="px-3 py-2 text-center">
                      <span v-if="r.clickedAt" class="text-red-600 font-bold" title="링크 클릭">⚠</span>
                      <span v-else class="text-gray-300">—</span>
                    </td>
                    <td class="px-3 py-2 text-center">
                      <span v-if="r.reportedAt" class="text-primary-600 font-semibold" title="신고함">✓</span>
                      <span v-else class="text-gray-300">—</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { phishingApi } from '@/api'

const TEMPLATE_CATEGORIES = ['IT', 'HR', 'DELIVERY', 'FINANCE', 'SECURITY', 'MARKETING', '기타']

const tabs = [
  { key: 'templates', label: '악성메일 템플릿' },
  { key: 'targets',   label: '발송대상 관리' },
  { key: 'campaigns', label: '모의훈련 현황' },
]
const activeTab = ref('templates')

// ── State ─────────────────────────────────────────────────────────────────
const templates = ref([])
const targets   = ref([])
const campaigns = ref([])

const activeTargets = computed(() => targets.value.filter(t => t.active))

// ── Load data ──────────────────────────────────────────────────────────────
async function loadAll() {
  const [tpl, tgt, cmp] = await Promise.all([
    phishingApi.listTemplates(),
    phishingApi.listTargets(),
    phishingApi.listCampaigns(),
  ])
  templates.value  = tpl.data ?? []
  targets.value    = tgt.data ?? []
  campaigns.value  = cmp.data ?? []
}

onMounted(loadAll)

// ── Template modal ─────────────────────────────────────────────────────────
const templateModal = reactive({
  open: false, id: null, saving: false, error: '',
  form: { name: '', category: '', difficulty: 'MEDIUM', subject: '', senderName: '', senderEmail: '', bodyHtml: '', description: '' }
})
function openTemplateModal(t = null) {
  templateModal.id    = t?.id ?? null
  templateModal.error = ''
  templateModal.form  = t
    ? { name: t.name, category: t.category, difficulty: t.difficulty, subject: t.subject, senderName: t.senderName, senderEmail: t.senderEmail, bodyHtml: t.bodyHtml, description: t.description ?? '' }
    : { name: '', category: '', difficulty: 'MEDIUM', subject: '', senderName: '', senderEmail: '', bodyHtml: '', description: '' }
  templateModal.open  = true
}
async function saveTemplate() {
  templateModal.saving = true
  templateModal.error  = ''
  try {
    if (templateModal.id) {
      await phishingApi.updateTemplate(templateModal.id, templateModal.form)
    } else {
      await phishingApi.createTemplate(templateModal.form)
    }
    templateModal.open = false
    const res = await phishingApi.listTemplates()
    templates.value = res.data ?? []
  } catch (e) {
    templateModal.error = typeof e === 'string' ? e : '저장에 실패했습니다.'
  } finally {
    templateModal.saving = false
  }
}
async function confirmDeleteTemplate(t) {
  if (!confirm(`"${t.name}" 템플릿을 삭제하시겠습니까?`)) return
  try {
    await phishingApi.deleteTemplate(t.id)
    templates.value = templates.value.filter(x => x.id !== t.id)
  } catch (e) { alert(typeof e === 'string' ? e : '삭제 실패') }
}

// ── Preview modal ──────────────────────────────────────────────────────────
const previewModal = reactive({ open: false, template: null })
function previewTemplate(t) { previewModal.template = t; previewModal.open = true }

// ── Target modal ───────────────────────────────────────────────────────────
const targetModal = reactive({
  open: false, id: null, saving: false, error: '',
  form: { name: '', email: '', department: '', position: '' }
})
function openTargetModal(t = null) {
  targetModal.id    = t?.id ?? null
  targetModal.error = ''
  targetModal.form  = t
    ? { name: t.name, email: t.email, department: t.department ?? '', position: t.position ?? '' }
    : { name: '', email: '', department: '', position: '' }
  targetModal.open  = true
}
async function saveTarget() {
  targetModal.saving = true
  targetModal.error  = ''
  try {
    if (targetModal.id) {
      await phishingApi.updateTarget(targetModal.id, targetModal.form)
    } else {
      await phishingApi.createTarget(targetModal.form)
    }
    targetModal.open = false
    const res = await phishingApi.listTargets()
    targets.value = res.data ?? []
  } catch (e) {
    targetModal.error = typeof e === 'string' ? e : '저장에 실패했습니다.'
  } finally {
    targetModal.saving = false
  }
}
async function toggleTarget(t) {
  try {
    await phishingApi.toggleTarget(t.id)
    t.active = !t.active
  } catch (e) { alert('상태 변경 실패') }
}
async function confirmDeleteTarget(t) {
  if (!confirm(`"${t.name}" 대상을 삭제하시겠습니까?`)) return
  try {
    await phishingApi.deleteTarget(t.id)
    targets.value = targets.value.filter(x => x.id !== t.id)
  } catch (e) { alert(typeof e === 'string' ? e : '삭제 실패') }
}

// ── Campaign modal ─────────────────────────────────────────────────────────
const campaignModal = reactive({
  open: false, saving: false, error: '',
  form: { name: '', templateId: '', scheduledAt: '', description: '', targetIds: [] }
})
function openCampaignModal() {
  campaignModal.error = ''
  campaignModal.form  = { name: '', templateId: '', scheduledAt: '', description: '', targetIds: [] }
  campaignModal.open  = true
}
function selectAllTargets() {
  campaignModal.form.targetIds = activeTargets.value.map(t => t.id)
}
async function saveCampaign() {
  if (!campaignModal.form.targetIds.length) {
    campaignModal.error = '발송 대상을 최소 1명 이상 선택해야 합니다.'
    return
  }
  campaignModal.saving = true
  campaignModal.error  = ''
  try {
    const payload = {
      ...campaignModal.form,
      templateId: Number(campaignModal.form.templateId),
      scheduledAt: campaignModal.form.scheduledAt || null,
    }
    await phishingApi.createCampaign(payload)
    campaignModal.open = false
    const res = await phishingApi.listCampaigns()
    campaigns.value = res.data ?? []
    activeTab.value = 'campaigns'
  } catch (e) {
    campaignModal.error = typeof e === 'string' ? e : '등록에 실패했습니다.'
  } finally {
    campaignModal.saving = false
  }
}

// ── Campaign actions ───────────────────────────────────────────────────────
async function launchCampaign(c) {
  if (!confirm(`"${c.name}" 훈련을 실시하시겠습니까?\n선택된 ${c.totalTargets}명에게 모의 피싱 메일이 발송됩니다.`)) return
  try {
    await phishingApi.launchCampaign(c.id)
    const res = await phishingApi.listCampaigns()
    campaigns.value = res.data ?? []
  } catch (e) { alert(typeof e === 'string' ? e : '실시 실패') }
}
async function completeCampaign(c) {
  if (!confirm(`"${c.name}" 훈련을 완료 처리하시겠습니까?`)) return
  try {
    await phishingApi.completeCampaign(c.id)
    const res = await phishingApi.listCampaigns()
    campaigns.value = res.data ?? []
  } catch (e) { alert(typeof e === 'string' ? e : '완료 처리 실패') }
}
async function cancelCampaign(c) {
  if (!confirm(`"${c.name}" 훈련을 취소하시겠습니까?`)) return
  try {
    await phishingApi.cancelCampaign(c.id)
    const res = await phishingApi.listCampaigns()
    campaigns.value = res.data ?? []
  } catch (e) { alert(typeof e === 'string' ? e : '취소 실패') }
}
async function confirmDeleteCampaign(c) {
  if (!confirm(`"${c.name}" 훈련을 삭제하시겠습니까?`)) return
  try {
    await phishingApi.deleteCampaign(c.id)
    campaigns.value = campaigns.value.filter(x => x.id !== c.id)
  } catch (e) { alert(typeof e === 'string' ? e : '삭제 실패') }
}

// ── Campaign result modal ──────────────────────────────────────────────────
const resultModal = reactive({ open: false, detail: null })
async function viewCampaign(id) {
  try {
    const res = await phishingApi.getCampaign(id)
    resultModal.detail = res.data
    resultModal.open   = true
  } catch (e) { alert('데이터 로드 실패') }
}
const resultStats = computed(() => {
  const d = resultModal.detail
  if (!d) return []
  const total = d.campaign.totalTargets || 1
  return [
    { label: '발송', value: d.campaign.sentCount,    rate: pct(d.campaign.sentCount,    total), color: 'text-gray-700' },
    { label: '열람', value: d.campaign.openedCount,  rate: pct(d.campaign.openedCount,  total), color: 'text-amber-600' },
    { label: '클릭', value: d.campaign.clickedCount, rate: pct(d.campaign.clickedCount, total), color: 'text-red-600'   },
    { label: '신고', value: d.campaign.reportedCount,rate: pct(d.campaign.reportedCount,total), color: 'text-primary-600' },
  ]
})

// ── Helpers ────────────────────────────────────────────────────────────────
function pct(v, t) { return t ? Math.round(v / t * 100) : 0 }
function fmtDate(d) { return d ? new Date(d).toLocaleDateString('ko-KR') : '—' }

function difficultyLabel(d) { return { EASY: '쉬움', MEDIUM: '보통', HARD: '어려움' }[d] ?? d }
function difficultyClass(d) {
  return { EASY: 'bg-green-100 text-green-700', MEDIUM: 'bg-amber-100 text-amber-700', HARD: 'bg-red-100 text-red-700' }[d] ?? 'bg-gray-100 text-gray-500'
}

function campaignStatusLabel(s) {
  return { DRAFT: '대기', RUNNING: '실시중', COMPLETED: '완료', CANCELLED: '취소' }[s] ?? s
}
function campaignStatusClass(s) {
  return {
    DRAFT:     'bg-gray-100 text-gray-600',
    RUNNING:   'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-red-100 text-red-500',
  }[s] ?? 'bg-gray-100 text-gray-600'
}
</script>

<style scoped>
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.label { @apply block text-sm font-medium text-gray-700 mb-1; }
</style>
