<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">재해복구·BCP 훈련</h1>
        <p class="text-sm text-gray-400 mt-0.5">재해복구(DR)·업무연속성(BCP) 훈련 시나리오를 관리하고 훈련을 실시·평가합니다</p>
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

      <!-- ── Tab 1: 훈련 시나리오 ──────────────────────────────────────── -->
      <div v-if="activeTab === 'scenarios'">
        <div class="flex justify-between items-center mb-4">
          <p class="text-sm text-gray-500">총 {{ scenarios.length }}개 시나리오 · 활성 {{ activeScenarios.length }}개</p>
          <button class="btn-primary text-sm" @click="openScenarioModal()">+ 시나리오 등록</button>
        </div>
        <div class="card overflow-hidden p-0">
          <table class="w-full text-sm">
            <thead>
              <tr class="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider">
                <th class="px-4 py-3 text-left">시나리오명</th>
                <th class="px-4 py-3 text-left">재해 유형</th>
                <th class="px-4 py-3 text-center">난이도</th>
                <th class="px-4 py-3 text-left">대상 시스템·업무</th>
                <th class="px-4 py-3 text-center">목표 RTO/RPO</th>
                <th class="px-4 py-3 text-center">단계</th>
                <th class="px-4 py-3 text-center">상태</th>
                <th class="px-4 py-3 text-center">작업</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-if="!scenarios.length">
                <td colspan="8" class="py-12 text-center text-gray-400 text-sm">등록된 시나리오가 없습니다.</td>
              </tr>
              <tr v-for="s in scenarios" :key="s.id" class="hover:bg-gray-50 transition-colors"
                :class="{ 'opacity-50': !s.active }">
                <td class="px-4 py-3 font-medium text-gray-900">
                  <button @click="viewScenario(s)" class="hover:text-primary-600 text-left">{{ s.name }}</button>
                </td>
                <td class="px-4 py-3">
                  <span class="px-2 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700">{{ s.category }}</span>
                </td>
                <td class="px-4 py-3 text-center">
                  <span :class="difficultyClass(s.difficulty)" class="px-2 py-0.5 rounded-full text-xs font-semibold">
                    {{ difficultyLabel(s.difficulty) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-gray-600 text-xs">{{ s.targetSystem || '—' }}</td>
                <td class="px-4 py-3 text-center text-xs text-gray-600">
                  {{ fmtMin(s.rtoMinutes) }} <span class="text-gray-300">/</span> {{ fmtMin(s.rpoMinutes) }}
                </td>
                <td class="px-4 py-3 text-center font-semibold text-gray-700">{{ s.stepCount }}</td>
                <td class="px-4 py-3 text-center">
                  <button @click="toggleScenario(s)"
                    :class="s.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-400'"
                    class="px-2.5 py-0.5 rounded-full text-xs font-semibold transition-colors">
                    {{ s.active ? '활성' : '비활성' }}
                  </button>
                </td>
                <td class="px-4 py-3">
                  <div class="flex justify-center gap-1.5 flex-wrap">
                    <button @click="viewScenario(s)" class="text-xs text-blue-600 hover:underline">상세</button>
                    <button v-if="s.active" @click="openExerciseModal(s.id)" class="text-xs text-green-600 hover:underline">훈련 실시</button>
                    <button @click="openScenarioModal(s)" class="text-xs text-gray-600 hover:underline">수정</button>
                    <button @click="confirmDeleteScenario(s)" class="text-xs text-red-500 hover:underline">삭제</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ── Tab 2: 훈련 실시 현황 ─────────────────────────────────────── -->
      <div v-else>
        <div class="flex justify-between items-center mb-4">
          <div class="flex items-center gap-3 text-sm">
            <p class="text-gray-500">총 {{ exercises.length }}건</p>
            <span class="text-blue-600 font-medium">진행중 {{ countStatus('RUNNING') }}</span>
            <span class="text-green-600 font-medium">완료 {{ countStatus('COMPLETED') }}</span>
          </div>
          <button class="btn-primary text-sm" @click="openExerciseModal()">+ 훈련 등록</button>
        </div>
        <div class="card overflow-hidden p-0">
          <table class="w-full text-sm">
            <thead>
              <tr class="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider">
                <th class="px-4 py-3 text-left">훈련명</th>
                <th class="px-4 py-3 text-left">시나리오</th>
                <th class="px-4 py-3 text-center">방식</th>
                <th class="px-4 py-3 text-center">진행</th>
                <th class="px-4 py-3 text-center">달성률</th>
                <th class="px-4 py-3 text-center">판정</th>
                <th class="px-4 py-3 text-center">상태</th>
                <th class="px-4 py-3 text-left">실시일</th>
                <th class="px-4 py-3 text-center">작업</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-if="!exercises.length">
                <td colspan="9" class="py-12 text-center text-gray-400 text-sm">실시된 훈련이 없습니다.</td>
              </tr>
              <tr v-for="e in exercises" :key="e.id" class="hover:bg-gray-50 transition-colors">
                <td class="px-4 py-3 font-medium text-gray-900">
                  <button @click="openRunModal(e.id)" class="hover:text-primary-600 text-left">{{ e.name }}</button>
                </td>
                <td class="px-4 py-3 text-gray-600 text-xs">{{ e.scenarioName }}</td>
                <td class="px-4 py-3 text-center text-xs text-gray-600">{{ methodLabel(e.method) }}</td>
                <td class="px-4 py-3 text-center text-xs">
                  <span class="text-gray-700 font-semibold">{{ e.totalSteps - e.pendingSteps }}</span>
                  <span class="text-gray-400">/{{ e.totalSteps }}</span>
                </td>
                <td class="px-4 py-3 text-center">
                  <span v-if="e.score != null" class="font-mono font-semibold" :class="scoreColor(e.score)">{{ e.score }}%</span>
                  <span v-else class="text-gray-300">—</span>
                </td>
                <td class="px-4 py-3 text-center">
                  <span v-if="e.result" :class="resultClass(e.result)" class="px-2 py-0.5 rounded-full text-xs font-semibold">
                    {{ resultLabel(e.result) }}
                  </span>
                  <span v-else class="text-gray-300">—</span>
                </td>
                <td class="px-4 py-3 text-center">
                  <span :class="statusClass(e.status)" class="px-2 py-0.5 rounded-full text-xs font-semibold">
                    {{ statusLabel(e.status) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-gray-400 text-xs">{{ fmtDate(e.startedAt || e.plannedAt) }}</td>
                <td class="px-4 py-3">
                  <div class="flex justify-center gap-1.5 flex-wrap">
                    <button v-if="e.status === 'DRAFT'" @click="startExercise(e)" class="text-xs text-green-600 hover:underline">시작</button>
                    <button @click="openRunModal(e.id)" class="text-xs text-blue-600 hover:underline">
                      {{ e.status === 'RUNNING' ? '진행' : '결과' }}
                    </button>
                    <button v-if="e.status === 'DRAFT' || e.status === 'RUNNING'" @click="cancelExercise(e)"
                      class="text-xs text-amber-600 hover:underline">취소</button>
                    <button v-if="e.status !== 'RUNNING'" @click="confirmDeleteExercise(e)"
                      class="text-xs text-red-500 hover:underline">삭제</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ── 시나리오 등록/수정 모달 ───────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="scenarioModal.open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 p-4 overflow-y-auto">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-3xl my-8">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <h3 class="font-bold text-gray-900">{{ scenarioModal.id ? '시나리오 수정' : '시나리오 등록' }}</h3>
            <button @click="scenarioModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <form @submit.prevent="saveScenario" class="p-6 space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div class="col-span-2">
                <label class="label">시나리오명 <span class="text-red-500">*</span></label>
                <input v-model="scenarioModal.form.name" class="input" required />
              </div>
              <div>
                <label class="label">재해 유형 <span class="text-red-500">*</span></label>
                <select v-model="scenarioModal.form.category" class="input" required>
                  <option value="">선택</option>
                  <option v-for="c in CATEGORIES" :key="c" :value="c">{{ c }}</option>
                </select>
              </div>
              <div>
                <label class="label">난이도</label>
                <select v-model="scenarioModal.form.difficulty" class="input">
                  <option value="EASY">쉬움</option>
                  <option value="MEDIUM">보통</option>
                  <option value="HARD">어려움</option>
                </select>
              </div>
              <div class="col-span-2">
                <label class="label">대상 시스템·업무</label>
                <input v-model="scenarioModal.form.targetSystem" class="input" placeholder="예) 기간계 DB, 그룹웨어" />
              </div>
              <div>
                <label class="label">목표 복구시간 RTO (분)</label>
                <input v-model.number="scenarioModal.form.rtoMinutes" type="number" min="0" class="input" />
              </div>
              <div>
                <label class="label">목표 복구시점 RPO (분)</label>
                <input v-model.number="scenarioModal.form.rpoMinutes" type="number" min="0" class="input" />
              </div>
              <div class="col-span-2">
                <label class="label">상황 설정</label>
                <textarea v-model="scenarioModal.form.situation" class="input" rows="3"
                  placeholder="훈련 참가자에게 제시할 재해 상황을 기술합니다."></textarea>
              </div>
              <div class="col-span-2">
                <label class="label">훈련 목표</label>
                <textarea v-model="scenarioModal.form.objective" class="input" rows="2"></textarea>
              </div>
              <div class="col-span-2">
                <label class="label">설명</label>
                <input v-model="scenarioModal.form.description" class="input" />
              </div>
            </div>

            <!-- 대응 단계 -->
            <div>
              <div class="flex items-center justify-between mb-2">
                <label class="label !mb-0">대응 단계 <span class="text-red-500">*</span></label>
                <button type="button" @click="addStep" class="text-xs text-primary-600 hover:underline">+ 단계 추가</button>
              </div>
              <p class="text-xs text-gray-400 mb-2">훈련 실시 시 이 단계들이 복사되어 단계별로 수행 결과를 기록합니다.</p>
              <div class="space-y-3">
                <div v-for="(st, i) in scenarioModal.form.steps" :key="i" class="border rounded-xl p-3 bg-gray-50">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-bold text-gray-500">STEP {{ i + 1 }}</span>
                    <button type="button" @click="removeStep(i)" class="text-xs text-red-500 hover:underline">삭제</button>
                  </div>
                  <div class="grid grid-cols-12 gap-2">
                    <div class="col-span-6">
                      <input v-model="st.title" class="input !py-1.5 text-sm" placeholder="단계명 *" required />
                    </div>
                    <div class="col-span-4">
                      <input v-model="st.roleName" class="input !py-1.5 text-sm" placeholder="담당 역할" />
                    </div>
                    <div class="col-span-2">
                      <input v-model.number="st.targetMinutes" type="number" min="0" class="input !py-1.5 text-sm" placeholder="분" />
                    </div>
                    <div class="col-span-12">
                      <textarea v-model="st.action" class="input !py-1.5 text-sm" rows="2" placeholder="수행 절차"></textarea>
                    </div>
                    <div class="col-span-12">
                      <input v-model="st.successCriteria" class="input !py-1.5 text-sm" placeholder="성공 판정 기준" />
                    </div>
                  </div>
                </div>
                <div v-if="!scenarioModal.form.steps.length"
                  class="border border-dashed rounded-xl py-6 text-center text-gray-400 text-sm">
                  대응 단계를 1개 이상 추가하세요.
                </div>
              </div>
            </div>

            <div v-if="scenarioModal.error" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">{{ scenarioModal.error }}</div>
            <div class="flex justify-end gap-3 pt-2">
              <button type="button" @click="scenarioModal.open = false" class="btn-secondary">취소</button>
              <button type="submit" class="btn-primary" :disabled="scenarioModal.saving">
                {{ scenarioModal.saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- ── 시나리오 상세 모달 ────────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="detailModal.open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 p-4 overflow-y-auto">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-2xl my-8">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <div>
              <h3 class="font-bold text-gray-900">{{ detailModal.scenario?.name }}</h3>
              <div class="flex gap-3 mt-1 text-xs text-gray-500">
                <span>{{ detailModal.scenario?.category }}</span>
                <span>난이도 {{ difficultyLabel(detailModal.scenario?.difficulty) }}</span>
                <span>RTO {{ fmtMin(detailModal.scenario?.rtoMinutes) }} · RPO {{ fmtMin(detailModal.scenario?.rpoMinutes) }}</span>
              </div>
            </div>
            <button @click="detailModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="p-6 space-y-5 max-h-[70vh] overflow-y-auto">
            <div v-if="detailModal.scenario?.targetSystem">
              <p class="text-xs font-bold text-gray-500 mb-1">대상 시스템·업무</p>
              <p class="text-sm text-gray-700">{{ detailModal.scenario.targetSystem }}</p>
            </div>
            <div v-if="detailModal.scenario?.situation">
              <p class="text-xs font-bold text-gray-500 mb-1">상황 설정</p>
              <p class="text-sm text-gray-700 whitespace-pre-line bg-amber-50 border border-amber-100 rounded-lg p-3">{{ detailModal.scenario.situation }}</p>
            </div>
            <div v-if="detailModal.scenario?.objective">
              <p class="text-xs font-bold text-gray-500 mb-1">훈련 목표</p>
              <p class="text-sm text-gray-700 whitespace-pre-line">{{ detailModal.scenario.objective }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-gray-500 mb-2">대응 단계 ({{ detailModal.scenario?.steps?.length || 0 }})</p>
              <ol class="space-y-2">
                <li v-for="st in detailModal.scenario?.steps" :key="st.id" class="border rounded-xl p-3">
                  <div class="flex items-start justify-between gap-3">
                    <p class="text-sm font-semibold text-gray-800">
                      <span class="text-primary-600 mr-1">{{ st.stepOrder }}.</span>{{ st.title }}
                    </p>
                    <span class="text-xs text-gray-400 flex-shrink-0">{{ st.roleName || '—' }} · {{ fmtMin(st.targetMinutes) }}</span>
                  </div>
                  <p v-if="st.action" class="text-xs text-gray-600 mt-1.5 whitespace-pre-line">{{ st.action }}</p>
                  <p v-if="st.successCriteria" class="text-xs text-green-700 mt-1.5">판정 기준: {{ st.successCriteria }}</p>
                </li>
              </ol>
            </div>
          </div>
          <div class="flex justify-end gap-3 px-6 py-4 border-t">
            <button @click="detailModal.open = false" class="btn-secondary">닫기</button>
            <button v-if="detailModal.scenario?.active"
              @click="detailModal.open = false; openExerciseModal(detailModal.scenario.id)" class="btn-primary">이 시나리오로 훈련 실시</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ── 훈련 등록 모달 ────────────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="exerciseModal.open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 p-4 overflow-y-auto">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-xl my-8">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <h3 class="font-bold text-gray-900">훈련 등록</h3>
            <button @click="exerciseModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <form @submit.prevent="saveExercise" class="p-6 space-y-4">
            <div>
              <label class="label">훈련명 <span class="text-red-500">*</span></label>
              <input v-model="exerciseModal.form.name" class="input" required />
            </div>
            <div>
              <label class="label">훈련 시나리오 <span class="text-red-500">*</span></label>
              <select v-model="exerciseModal.form.scenarioId" class="input" required>
                <option value="">시나리오 선택</option>
                <option v-for="s in activeScenarios" :key="s.id" :value="s.id">
                  {{ s.name }} ({{ s.stepCount }}단계)
                </option>
              </select>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">훈련 방식</label>
                <select v-model="exerciseModal.form.method" class="input">
                  <option value="TABLETOP">도상훈련</option>
                  <option value="SIMULATION">시뮬레이션</option>
                  <option value="FAILOVER">실제 전환</option>
                </select>
              </div>
              <div>
                <label class="label">계획 일시</label>
                <input v-model="exerciseModal.form.plannedAt" type="datetime-local" class="input" />
              </div>
              <div>
                <label class="label">훈련 총괄자</label>
                <input v-model="exerciseModal.form.leaderName" class="input" />
              </div>
              <div>
                <label class="label">참가 인원</label>
                <input v-model.number="exerciseModal.form.participantCount" type="number" min="0" class="input" />
              </div>
            </div>
            <div>
              <label class="label">참가자</label>
              <textarea v-model="exerciseModal.form.participants" class="input" rows="2" placeholder="쉼표로 구분하여 입력"></textarea>
            </div>
            <div>
              <label class="label">비고</label>
              <textarea v-model="exerciseModal.form.description" class="input" rows="2"></textarea>
            </div>
            <div v-if="exerciseModal.error" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">{{ exerciseModal.error }}</div>
            <div class="flex justify-end gap-3 pt-2">
              <button type="button" @click="exerciseModal.open = false" class="btn-secondary">취소</button>
              <button type="submit" class="btn-primary" :disabled="exerciseModal.saving">
                {{ exerciseModal.saving ? '등록 중...' : '등록' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- ── 훈련 진행/결과 모달 ───────────────────────────────────────── -->
    <Transition name="modal-fade">
      <div v-if="runModal.open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 p-4 overflow-y-auto">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-3xl my-8">
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <div>
              <h3 class="font-bold text-gray-900">{{ runEx?.name }}</h3>
              <div class="flex gap-3 mt-1 text-xs text-gray-500 flex-wrap">
                <span>{{ runEx?.scenarioName }}</span>
                <span>{{ methodLabel(runEx?.method) }}</span>
                <span :class="statusClass(runEx?.status)" class="px-1.5 rounded font-semibold">{{ statusLabel(runEx?.status) }}</span>
                <span v-if="runEx?.leaderName">총괄 {{ runEx.leaderName }}</span>
                <span v-if="runEx?.participantCount">참가 {{ runEx.participantCount }}명</span>
              </div>
            </div>
            <button @click="runModal.open = false" class="text-gray-400 hover:text-gray-600">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <div class="p-6 space-y-5 max-h-[70vh] overflow-y-auto">
            <!-- 진행률 -->
            <div>
              <div class="flex items-center justify-between text-xs text-gray-500 mb-1">
                <span>수행 진행률</span>
                <span>{{ recordedCount }}/{{ runSteps.length }} 단계 기록됨</span>
              </div>
              <div class="h-2.5 rounded-full bg-gray-100 overflow-hidden">
                <div class="h-full rounded-full bg-primary-500 transition-all"
                  :style="{ width: pct(recordedCount, runSteps.length) + '%' }"></div>
              </div>
            </div>

            <!-- 완료된 훈련 요약 -->
            <div v-if="runEx?.status === 'COMPLETED'" class="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div class="bg-gray-50 rounded-xl p-3 text-center">
                <p class="text-xl font-bold" :class="scoreColor(runEx.score)">{{ runEx.score }}%</p>
                <p class="text-xs text-gray-500 mt-0.5">달성률</p>
              </div>
              <div class="bg-gray-50 rounded-xl p-3 text-center">
                <p class="text-xl font-bold" :class="resultTextColor(runEx.result)">{{ resultLabel(runEx.result) }}</p>
                <p class="text-xs text-gray-500 mt-0.5">최종 판정</p>
              </div>
              <div class="bg-gray-50 rounded-xl p-3 text-center">
                <p class="text-xl font-bold text-gray-700">{{ fmtMin(runEx.actualRtoMinutes) }}</p>
                <p class="text-xs text-gray-500 mt-0.5">
                  실제 RTO
                  <span v-if="runEx.rtoMet === true" class="text-green-600 font-semibold">달성</span>
                  <span v-else-if="runEx.rtoMet === false" class="text-red-500 font-semibold">미달</span>
                </p>
              </div>
              <div class="bg-gray-50 rounded-xl p-3 text-center">
                <p class="text-xl font-bold text-gray-700">{{ fmtMin(runEx.actualRpoMinutes) }}</p>
                <p class="text-xs text-gray-500 mt-0.5">실제 RPO (목표 {{ fmtMin(runEx.rpoMinutes) }})</p>
              </div>
            </div>

            <!-- 단계별 수행 -->
            <div class="space-y-3">
              <div v-for="st in runSteps" :key="st.id" class="border rounded-xl p-4"
                :class="stepBorderClass(st.result)">
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800">
                      <span class="text-primary-600 mr-1">{{ st.stepOrder }}.</span>{{ st.title }}
                    </p>
                    <p class="text-xs text-gray-400 mt-0.5">{{ st.roleName || '—' }} · 목표 {{ fmtMin(st.targetMinutes) }}</p>
                  </div>
                  <span :class="stepResultClass(st.result)" class="px-2 py-0.5 rounded-full text-xs font-semibold flex-shrink-0">
                    {{ stepResultLabel(st.result) }}
                  </span>
                </div>
                <p v-if="st.action" class="text-xs text-gray-600 mt-2 whitespace-pre-line">{{ st.action }}</p>
                <p v-if="st.successCriteria" class="text-xs text-green-700 mt-1.5">판정 기준: {{ st.successCriteria }}</p>

                <!-- 진행중일 때만 결과 입력 -->
                <div v-if="runEx?.status === 'RUNNING'" class="mt-3 pt-3 border-t grid grid-cols-12 gap-2 items-center">
                  <div class="col-span-12 sm:col-span-4 flex gap-1">
                    <button v-for="opt in STEP_RESULTS" :key="opt.value" type="button"
                      @click="setStepResult(st, opt.value)"
                      :class="['flex-1 px-2 py-1.5 rounded-lg text-xs font-semibold border transition-colors',
                        st.result === opt.value ? opt.activeClass : 'border-gray-200 text-gray-500 hover:bg-gray-50']">
                      {{ opt.label }}
                    </button>
                  </div>
                  <div class="col-span-4 sm:col-span-2">
                    <input v-model.number="st.actualMinutes" type="number" min="0"
                      class="input !py-1.5 text-sm" placeholder="소요(분)" />
                  </div>
                  <div class="col-span-8 sm:col-span-5">
                    <input v-model="st.note" class="input !py-1.5 text-sm" placeholder="수행 내용·특이사항" />
                  </div>
                  <div class="col-span-12 sm:col-span-1">
                    <button type="button" @click="saveStep(st)" :disabled="st._saving"
                      class="w-full px-2 py-1.5 rounded-lg text-xs font-semibold bg-primary-50 text-primary-600 hover:bg-primary-100 disabled:opacity-50">
                      {{ st._saving ? '...' : '기록' }}
                    </button>
                  </div>
                </div>

                <!-- 완료된 훈련의 기록 표시 -->
                <div v-else-if="st.actualMinutes != null || st.note" class="mt-2 pt-2 border-t text-xs text-gray-600">
                  <span v-if="st.actualMinutes != null" class="mr-3">실제 소요 {{ fmtMin(st.actualMinutes) }}</span>
                  <span v-if="st.note">{{ st.note }}</span>
                </div>
              </div>
            </div>

            <!-- 완료 처리 입력 -->
            <div v-if="runEx?.status === 'RUNNING'" class="border-t pt-5">
              <h4 class="text-sm font-bold text-gray-800 mb-3">훈련 완료 처리</h4>
              <p class="text-xs text-gray-400 mb-3">모든 단계의 수행 결과를 기록해야 완료 처리할 수 있습니다.</p>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">실제 복구시간 RTO (분)</label>
                  <input v-model.number="runModal.complete.actualRtoMinutes" type="number" min="0" class="input"
                    :placeholder="`목표 ${runEx?.rtoMinutes ?? '-'}분`" />
                </div>
                <div>
                  <label class="label">실제 복구시점 RPO (분)</label>
                  <input v-model.number="runModal.complete.actualRpoMinutes" type="number" min="0" class="input"
                    :placeholder="`목표 ${runEx?.rpoMinutes ?? '-'}분`" />
                </div>
                <div class="col-span-2">
                  <label class="label">훈련 총평</label>
                  <textarea v-model="runModal.complete.summary" class="input" rows="3"></textarea>
                </div>
                <div class="col-span-2">
                  <label class="label">도출된 개선사항</label>
                  <textarea v-model="runModal.complete.improvement" class="input" rows="3"></textarea>
                </div>
              </div>
            </div>

            <!-- 완료된 훈련의 총평 -->
            <template v-else-if="runEx?.status === 'COMPLETED'">
              <div v-if="runEx.summary">
                <p class="text-xs font-bold text-gray-500 mb-1">훈련 총평</p>
                <p class="text-sm text-gray-700 whitespace-pre-line">{{ runEx.summary }}</p>
              </div>
              <div v-if="runEx.improvement">
                <p class="text-xs font-bold text-gray-500 mb-1">개선사항</p>
                <p class="text-sm text-gray-700 whitespace-pre-line bg-amber-50 border border-amber-100 rounded-lg p-3">{{ runEx.improvement }}</p>
              </div>
            </template>

            <div v-if="runModal.error" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">{{ runModal.error }}</div>
          </div>

          <div class="flex justify-end gap-3 px-6 py-4 border-t">
            <button @click="downloadResultExcel" :disabled="exportingResult"
              class="btn-secondary mr-auto flex items-center gap-1.5 disabled:opacity-50 disabled:cursor-wait"
              title="이 훈련의 개요·단계별 결과·총평을 엑셀로 내려받기">
              <svg v-if="exportingResult" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
              </svg>
              <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              {{ exportingResult ? '다운로드 중...' : '결과 엑셀 다운로드' }}
            </button>
            <button @click="runModal.open = false" class="btn-secondary">닫기</button>
            <button v-if="runEx?.status === 'DRAFT'" @click="startExercise(runEx, true)" class="btn-primary">훈련 시작</button>
            <button v-if="runEx?.status === 'RUNNING'" @click="completeExercise" class="btn-primary" :disabled="runModal.saving">
              {{ runModal.saving ? '처리 중...' : '훈련 완료 처리' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { bcpApi } from '@/api'

const CATEGORIES = ['시설·전원', '시스템장애', '사이버공격', '네트워크', '자연재해·시설', '인적', '협력사', '기타']

const STEP_RESULTS = [
  { value: 'PASS',    label: '성공',   activeClass: 'border-green-500 bg-green-50 text-green-700' },
  { value: 'PARTIAL', label: '부분',   activeClass: 'border-amber-500 bg-amber-50 text-amber-700' },
  { value: 'FAIL',    label: '실패',   activeClass: 'border-red-500 bg-red-50 text-red-600' },
]

const tabs = [
  { key: 'scenarios', label: '훈련 시나리오' },
  { key: 'exercises', label: '훈련 실시 현황' },
]
const activeTab = ref('scenarios')

// ── State ─────────────────────────────────────────────────────────────────
const scenarios = ref([])
const exercises = ref([])

const activeScenarios = computed(() => scenarios.value.filter(s => s.active))
function countStatus(s) { return exercises.value.filter(e => e.status === s).length }

async function loadAll() {
  const [sc, ex] = await Promise.all([bcpApi.listScenarios(), bcpApi.listExercises()])
  scenarios.value = sc.data ?? []
  exercises.value = ex.data ?? []
}
async function reloadScenarios() { scenarios.value = (await bcpApi.listScenarios()).data ?? [] }
async function reloadExercises() { exercises.value = (await bcpApi.listExercises()).data ?? [] }

onMounted(loadAll)

// ── 시나리오 모달 ──────────────────────────────────────────────────────────
function emptyScenarioForm() {
  return {
    name: '', category: '', difficulty: 'MEDIUM', targetSystem: '',
    rtoMinutes: null, rpoMinutes: null, situation: '', objective: '', description: '', steps: [],
  }
}
const scenarioModal = reactive({ open: false, id: null, saving: false, error: '', form: emptyScenarioForm() })

async function openScenarioModal(s = null) {
  scenarioModal.id = s?.id ?? null
  scenarioModal.error = ''
  if (s) {
    // 목록 응답에도 steps가 포함되지만, 수정 시점의 최신 값을 다시 읽는다.
    const detail = (await bcpApi.getScenario(s.id)).data
    scenarioModal.form = {
      name: detail.name, category: detail.category, difficulty: detail.difficulty,
      targetSystem: detail.targetSystem ?? '', rtoMinutes: detail.rtoMinutes, rpoMinutes: detail.rpoMinutes,
      situation: detail.situation ?? '', objective: detail.objective ?? '', description: detail.description ?? '',
      steps: (detail.steps ?? []).map(st => ({
        stepOrder: st.stepOrder, title: st.title, roleName: st.roleName ?? '',
        action: st.action ?? '', targetMinutes: st.targetMinutes, successCriteria: st.successCriteria ?? '',
      })),
    }
  } else {
    scenarioModal.form = emptyScenarioForm()
  }
  scenarioModal.open = true
}
function addStep() {
  scenarioModal.form.steps.push({
    stepOrder: scenarioModal.form.steps.length + 1,
    title: '', roleName: '', action: '', targetMinutes: null, successCriteria: '',
  })
}
function removeStep(i) {
  scenarioModal.form.steps.splice(i, 1)
  scenarioModal.form.steps.forEach((st, idx) => { st.stepOrder = idx + 1 })
}
async function saveScenario() {
  if (!scenarioModal.form.steps.length) {
    scenarioModal.error = '대응 단계를 1개 이상 추가해야 합니다.'
    return
  }
  scenarioModal.saving = true
  scenarioModal.error = ''
  try {
    if (scenarioModal.id) await bcpApi.updateScenario(scenarioModal.id, scenarioModal.form)
    else await bcpApi.createScenario(scenarioModal.form)
    scenarioModal.open = false
    await reloadScenarios()
  } catch (e) {
    scenarioModal.error = typeof e === 'string' ? e : '저장에 실패했습니다.'
  } finally {
    scenarioModal.saving = false
  }
}
async function toggleScenario(s) {
  try {
    await bcpApi.toggleScenario(s.id)
    s.active = !s.active
  } catch (e) { alert('상태 변경 실패') }
}
async function confirmDeleteScenario(s) {
  if (!confirm(`"${s.name}" 시나리오를 삭제하시겠습니까?`)) return
  try {
    await bcpApi.deleteScenario(s.id)
    scenarios.value = scenarios.value.filter(x => x.id !== s.id)
  } catch (e) { alert(typeof e === 'string' ? e : '삭제 실패') }
}

// ── 시나리오 상세 모달 ─────────────────────────────────────────────────────
const detailModal = reactive({ open: false, scenario: null })
async function viewScenario(s) {
  try {
    detailModal.scenario = (await bcpApi.getScenario(s.id)).data
    detailModal.open = true
  } catch (e) { alert('데이터 로드 실패') }
}

// ── 훈련 등록 모달 ─────────────────────────────────────────────────────────
const exerciseModal = reactive({
  open: false, saving: false, error: '',
  form: { name: '', scenarioId: '', method: 'TABLETOP', plannedAt: '', leaderName: '', participants: '', participantCount: null, description: '' }
})
function openExerciseModal(scenarioId = '') {
  const sc = scenarios.value.find(s => s.id === scenarioId)
  exerciseModal.error = ''
  exerciseModal.form = {
    name: sc ? `${sc.name} 훈련` : '',
    scenarioId: scenarioId || '',
    method: 'TABLETOP', plannedAt: '', leaderName: '', participants: '', participantCount: null, description: '',
  }
  exerciseModal.open = true
}
async function saveExercise() {
  exerciseModal.saving = true
  exerciseModal.error = ''
  try {
    await bcpApi.createExercise({
      ...exerciseModal.form,
      scenarioId: Number(exerciseModal.form.scenarioId),
      plannedAt: exerciseModal.form.plannedAt || null,
    })
    exerciseModal.open = false
    await reloadExercises()
    activeTab.value = 'exercises'
  } catch (e) {
    exerciseModal.error = typeof e === 'string' ? e : '등록에 실패했습니다.'
  } finally {
    exerciseModal.saving = false
  }
}

// ── 훈련 진행/결과 모달 ────────────────────────────────────────────────────
const runModal = reactive({
  open: false, id: null, detail: null, saving: false, error: '',
  complete: { actualRtoMinutes: null, actualRpoMinutes: null, summary: '', improvement: '' },
})
const runEx = computed(() => runModal.detail?.exercise ?? null)
const runSteps = ref([])
const recordedCount = computed(() => runSteps.value.filter(s => s.result !== 'PENDING').length)

// 훈련 결과 엑셀 내려받기 (개요 + 단계별 수행 결과 + 총평·개선사항)
const exportingResult = ref(false)
async function downloadResultExcel() {
  if (!runModal.id || exportingResult.value) return
  exportingResult.value = true
  try {
    const blob = await bcpApi.exportExerciseExcel(runModal.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `BCP훈련결과_${String(runEx.value?.name || '훈련').replace(/[\\/:*?"<>|]/g, '_')}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    runModal.error = typeof e === 'string' ? e : '엑셀 다운로드에 실패했습니다.'
  } finally {
    exportingResult.value = false
  }
}

async function openRunModal(id) {
  try {
    const detail = (await bcpApi.getExercise(id)).data
    runModal.id = id
    runModal.detail = detail
    runModal.error = ''
    runModal.complete = { actualRtoMinutes: null, actualRpoMinutes: null, summary: '', improvement: '' }
    // 단계는 화면에서 편집하므로 로컬 사본으로 다룬다.
    runSteps.value = (detail.steps ?? []).map(st => ({ ...st, _saving: false }))
    runModal.open = true
  } catch (e) { alert('데이터 로드 실패') }
}
function setStepResult(st, value) { st.result = value }
async function saveStep(st) {
  if (st.result === 'PENDING') {
    runModal.error = '수행 결과(성공·부분·실패)를 선택한 뒤 기록하세요.'
    return
  }
  st._saving = true
  runModal.error = ''
  try {
    await bcpApi.recordStep(runModal.id, st.id, {
      result: st.result,
      actualMinutes: st.actualMinutes ?? null,
      note: st.note ?? null,
    })
  } catch (e) {
    runModal.error = typeof e === 'string' ? e : '단계 결과 저장에 실패했습니다.'
  } finally {
    st._saving = false
  }
}
async function completeExercise() {
  runModal.saving = true
  runModal.error = ''
  try {
    await bcpApi.completeExercise(runModal.id, runModal.complete)
    await refreshRunModal()
    await reloadExercises()
  } catch (e) {
    runModal.error = typeof e === 'string' ? e : '완료 처리에 실패했습니다.'
  } finally {
    runModal.saving = false
  }
}
async function refreshRunModal() {
  const detail = (await bcpApi.getExercise(runModal.id)).data
  runModal.detail = detail
  runSteps.value = (detail.steps ?? []).map(st => ({ ...st, _saving: false }))
}

// ── 훈련 상태 조작 ─────────────────────────────────────────────────────────
async function startExercise(e, fromModal = false) {
  if (!confirm(`"${e.name}" 훈련을 시작하시겠습니까?`)) return
  try {
    await bcpApi.startExercise(e.id)
    await reloadExercises()
    if (fromModal) await refreshRunModal()
  } catch (err) { alert(typeof err === 'string' ? err : '시작 실패') }
}
async function cancelExercise(e) {
  if (!confirm(`"${e.name}" 훈련을 취소하시겠습니까?`)) return
  try {
    await bcpApi.cancelExercise(e.id)
    await reloadExercises()
  } catch (err) { alert(typeof err === 'string' ? err : '취소 실패') }
}
async function confirmDeleteExercise(e) {
  if (!confirm(`"${e.name}" 훈련 기록을 삭제하시겠습니까?`)) return
  try {
    await bcpApi.deleteExercise(e.id)
    exercises.value = exercises.value.filter(x => x.id !== e.id)
  } catch (err) { alert(typeof err === 'string' ? err : '삭제 실패') }
}

// ── Helpers ────────────────────────────────────────────────────────────────
function pct(v, t) { return t ? Math.round(v / t * 100) : 0 }
function fmtDate(d) { return d ? new Date(d).toLocaleDateString('ko-KR') : '—' }
function fmtMin(m) {
  if (m == null) return '—'
  if (m < 60) return `${m}분`
  const h = Math.floor(m / 60), r = m % 60
  return r ? `${h}시간 ${r}분` : `${h}시간`
}

function difficultyLabel(d) { return { EASY: '쉬움', MEDIUM: '보통', HARD: '어려움' }[d] ?? d ?? '—' }
function difficultyClass(d) {
  return { EASY: 'bg-green-100 text-green-700', MEDIUM: 'bg-amber-100 text-amber-700', HARD: 'bg-red-100 text-red-700' }[d] ?? 'bg-gray-100 text-gray-500'
}

function methodLabel(m) { return { TABLETOP: '도상훈련', SIMULATION: '시뮬레이션', FAILOVER: '실제 전환' }[m] ?? m ?? '—' }

function statusLabel(s) { return { DRAFT: '계획', RUNNING: '진행중', COMPLETED: '완료', CANCELLED: '취소' }[s] ?? s }
function statusClass(s) {
  return {
    DRAFT: 'bg-gray-100 text-gray-600',
    RUNNING: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-red-100 text-red-500',
  }[s] ?? 'bg-gray-100 text-gray-600'
}

function resultLabel(r) { return { PASS: '적합', PARTIAL: '보완필요', FAIL: '부적합' }[r] ?? r }
function resultClass(r) {
  return { PASS: 'bg-green-100 text-green-700', PARTIAL: 'bg-amber-100 text-amber-700', FAIL: 'bg-red-100 text-red-600' }[r] ?? 'bg-gray-100 text-gray-500'
}
function resultTextColor(r) {
  return { PASS: 'text-green-600', PARTIAL: 'text-amber-600', FAIL: 'text-red-600' }[r] ?? 'text-gray-500'
}
function scoreColor(v) {
  if (v == null) return 'text-gray-400'
  return v >= 80 ? 'text-green-600' : v >= 60 ? 'text-amber-600' : 'text-red-600'
}

function stepResultLabel(r) { return { PENDING: '미수행', PASS: '성공', PARTIAL: '부분', FAIL: '실패' }[r] ?? r }
function stepResultClass(r) {
  return {
    PENDING: 'bg-gray-100 text-gray-400',
    PASS: 'bg-green-100 text-green-700',
    PARTIAL: 'bg-amber-100 text-amber-700',
    FAIL: 'bg-red-100 text-red-600',
  }[r] ?? 'bg-gray-100 text-gray-500'
}
function stepBorderClass(r) {
  return {
    PASS: 'border-green-200', PARTIAL: 'border-amber-200', FAIL: 'border-red-200',
  }[r] ?? 'border-gray-200'
}
</script>

<style scoped>
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.label { @apply block text-sm font-medium text-gray-700 mb-1; }
</style>
