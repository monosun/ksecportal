<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">교육·훈련 결과</h1>
        <p class="text-sm text-gray-400 mt-0.5">IT 및 정보보호 교육 이수 현황과 모의 악성메일 훈련·재해복구 BCP 훈련 결과를 조회합니다</p>
      </div>
      <PiMaskToggle screen="교육·훈련 결과" />
    </div>

    <div class="page-body">

    <!-- 탭 -->
    <div class="flex gap-1 border-b border-gray-200 mb-4">
      <button @click="activeTab = 'training'"
        class="px-4 py-2 text-sm font-semibold border-b-2 -mb-px transition-colors"
        :class="activeTab === 'training' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'">
        IT 및 정보보호 교육 결과
      </button>
      <button @click="activeTab = 'phishing'"
        class="px-4 py-2 text-sm font-semibold border-b-2 -mb-px transition-colors"
        :class="activeTab === 'phishing' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'">
        모의훈련 결과
      </button>
      <button @click="activeTab = 'bcp'"
        class="px-4 py-2 text-sm font-semibold border-b-2 -mb-px transition-colors"
        :class="activeTab === 'bcp' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'">
        재해복구·BCP 훈련 결과
      </button>
    </div>

    <div v-if="forbidden" class="card text-center py-12 text-gray-400 text-sm">
      결과 조회 권한이 없습니다. (MANAGER 이상)
    </div>
    <div v-else-if="loading" class="flex items-center justify-center py-20 text-gray-400">
      <svg class="animate-spin w-6 h-6 mr-2" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
      </svg>
      데이터 불러오는 중...
    </div>

    <!-- ── 탭: 보안교육 결과 ── -->
    <template v-else-if="activeTab === 'training'">
      <!-- Stat Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">교육 코스</p>
          <p class="text-2xl font-bold text-gray-900">{{ courses.length }}<span class="text-sm font-normal text-gray-400 ml-1">개</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">총 이수 건수</p>
          <p class="text-2xl font-bold text-blue-600">{{ totalCompleted }}<span class="text-sm font-normal text-gray-400 ml-1">건</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">전체 합격률</p>
          <p class="text-2xl font-bold text-green-600">{{ overallPassRate }}<span class="text-sm font-normal text-gray-400 ml-1">%</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">전체 평균 점수</p>
          <p class="text-2xl font-bold text-indigo-600">{{ overallAvgScore }}<span class="text-sm font-normal text-gray-400 ml-1">점</span></p>
        </div>
      </div>

      <!-- 코스별 이수 현황 -->
      <div class="card mb-6">
        <h2 class="text-sm font-bold text-gray-800 mb-4">코스별 이수 현황</h2>
        <div v-if="courses.length === 0" class="text-center py-8 text-gray-400 text-sm">등록된 교육 코스가 없습니다.</div>
        <div v-else class="space-y-4">
          <div v-for="c in courses" :key="c.courseId">
            <div class="flex items-center justify-between text-sm mb-1">
              <div class="flex items-center gap-2 min-w-0">
                <span class="font-medium text-gray-800 truncate">{{ c.title }}</span>
                <span v-if="c.mandatory" class="flex-shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded bg-red-100 text-red-600">필수</span>
              </div>
              <span class="text-xs text-gray-500 flex-shrink-0 ml-3 flex items-center gap-2">
                <span>
                  이수 {{ c.completedCount }}/{{ c.totalUsers }}명 · 합격 {{ c.passedCount }}명
                  <template v-if="c.avgScore != null"> · 평균 {{ Math.round(c.avgScore) }}점</template>
                </span>
                <ExcelDownloadButton :busy="downloading === 'course-' + c.courseId"
                  title="이 교육의 결과를 엑셀로 내려받기"
                  @click="downloadCourse(c)" />
              </span>
            </div>
            <div class="h-3 rounded-full bg-gray-100 overflow-hidden">
              <div class="h-full rounded-full bg-blue-500 transition-all"
                :style="{ width: rate(c.completedCount, c.totalUsers) + '%' }"></div>
            </div>
            <p class="text-[11px] text-gray-400 mt-0.5 text-right">이수율 {{ rate(c.completedCount, c.totalUsers) }}%</p>
          </div>
        </div>
      </div>

      <!-- 이수 이력 조회 -->
      <div class="card">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-bold text-gray-800">이수 이력 ({{ filteredCompletions.length }}건)</h2>
          <select v-model="completionFilter" class="input w-64 text-sm !py-1.5">
            <option :value="null">전체 코스</option>
            <option v-for="c in courses" :key="c.courseId" :value="c.courseId">{{ c.title }}</option>
          </select>
        </div>
        <div v-if="filteredCompletions.length === 0" class="text-center py-8 text-gray-400 text-sm">이수 이력이 없습니다.</div>
        <div v-else class="overflow-x-auto"><table class="w-full text-sm">
          <thead>
            <tr class="border-b text-left text-gray-500">
              <th class="py-2.5 px-3 font-semibold">사용자</th>
              <th class="py-2.5 px-3 font-semibold">부서</th>
              <th class="py-2.5 px-3 font-semibold">교육 코스</th>
              <th class="py-2.5 px-3 font-semibold text-center w-20">점수</th>
              <th class="py-2.5 px-3 font-semibold text-center w-20">결과</th>
              <th class="py-2.5 px-3 font-semibold w-40">이수 일시</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in filteredCompletions" :key="r.id" class="border-b hover:bg-gray-50">
              <td class="py-2.5 px-3 text-gray-800">{{ r.userName ? pi.mask('name', r.userName) : '-' }}</td>
              <td class="py-2.5 px-3 text-gray-500">{{ r.userDepartment || '-' }}</td>
              <td class="py-2.5 px-3 text-gray-600">{{ r.courseTitle }}</td>
              <td class="py-2.5 px-3 text-center font-mono">{{ r.score ?? '-' }}</td>
              <td class="py-2.5 px-3 text-center">
                <span class="text-[11px] font-bold px-2 py-0.5 rounded-full"
                  :class="r.passed ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'">
                  {{ r.passed ? '합격' : '불합격' }}
                </span>
              </td>
              <td class="py-2.5 px-3 text-gray-400 text-xs">{{ formatDt(r.completedAt) }}</td>
            </tr>
          </tbody>
        </table></div>
      </div>
    </template>

    <!-- ── 탭: 모의훈련 결과 ── -->
    <template v-else-if="activeTab === 'phishing'">
      <!-- Stat Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">캠페인</p>
          <p class="text-2xl font-bold text-gray-900">{{ campaigns.length }}<span class="text-sm font-normal text-gray-400 ml-1">개</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">총 발송</p>
          <p class="text-2xl font-bold text-blue-600">{{ sum('sentCount') }}<span class="text-sm font-normal text-gray-400 ml-1">건</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">평균 열람률</p>
          <p class="text-2xl font-bold text-amber-500">{{ avgRate('openedCount') }}<span class="text-sm font-normal text-gray-400 ml-1">%</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">평균 클릭률</p>
          <p class="text-2xl font-bold text-red-500">{{ avgRate('clickedCount') }}<span class="text-sm font-normal text-gray-400 ml-1">%</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">신고</p>
          <p class="text-2xl font-bold text-green-600">{{ sum('reportedCount') }}<span class="text-sm font-normal text-gray-400 ml-1">건</span></p>
        </div>
      </div>

      <!-- 캠페인별 결과 시각화 -->
      <div class="card mb-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-bold text-gray-800">캠페인별 결과</h2>
          <div class="flex items-center gap-3 text-[11px] text-gray-500">
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-amber-400 inline-block"></span>열람</span>
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-red-500 inline-block"></span>클릭</span>
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-green-500 inline-block"></span>신고</span>
          </div>
        </div>
        <div v-if="campaigns.length === 0" class="text-center py-8 text-gray-400 text-sm">실시된 캠페인이 없습니다.</div>
        <div v-else class="space-y-5">
          <div v-for="c in campaigns" :key="c.id">
            <div class="flex items-center justify-between text-sm mb-1.5">
              <div class="flex items-center gap-2 min-w-0">
                <span class="font-medium text-gray-800 truncate">{{ c.name }}</span>
                <span class="flex-shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded"
                  :class="campaignStatusClass(c.status)">{{ campaignStatusLabel(c.status) }}</span>
              </div>
              <span class="text-xs text-gray-500 flex-shrink-0 ml-3">
                대상 {{ c.totalTargets }} · 발송 {{ c.sentCount }} · 열람 {{ c.openedCount }} · 클릭 {{ c.clickedCount }} · 신고 {{ c.reportedCount }}
              </span>
            </div>
            <div class="space-y-1">
              <div v-for="bar in campaignBars(c)" :key="bar.label" class="flex items-center gap-2">
                <span class="w-8 text-[10px] text-gray-400 text-right flex-shrink-0">{{ bar.label }}</span>
                <div class="flex-1 h-2.5 rounded-full bg-gray-100 overflow-hidden">
                  <div class="h-full rounded-full transition-all" :class="bar.color" :style="{ width: bar.pct + '%' }"></div>
                </div>
                <span class="w-10 text-[10px] text-gray-500 flex-shrink-0">{{ bar.pct }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 캠페인 상세 표 -->
      <div class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-4">캠페인 목록</h2>
        <div v-if="campaigns.length === 0" class="text-center py-8 text-gray-400 text-sm">캠페인이 없습니다.</div>
        <div v-else class="overflow-x-auto"><table class="w-full text-sm">
          <thead>
            <tr class="border-b text-left text-gray-500">
              <th class="py-2.5 px-3 font-semibold">캠페인</th>
              <th class="py-2.5 px-3 font-semibold">템플릿</th>
              <th class="py-2.5 px-3 font-semibold text-center w-20">상태</th>
              <th class="py-2.5 px-3 font-semibold text-center w-16">대상</th>
              <th class="py-2.5 px-3 font-semibold text-center w-16">발송</th>
              <th class="py-2.5 px-3 font-semibold text-center w-24">열람(률)</th>
              <th class="py-2.5 px-3 font-semibold text-center w-24">클릭(률)</th>
              <th class="py-2.5 px-3 font-semibold text-center w-24">신고(률)</th>
              <th class="py-2.5 px-3 font-semibold w-32">일시</th>
              <th class="py-2.5 px-3 font-semibold text-center w-24">다운로드</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in campaigns" :key="c.id" class="border-b hover:bg-gray-50">
              <td class="py-2.5 px-3 text-gray-800 font-medium">{{ c.name }}</td>
              <td class="py-2.5 px-3 text-gray-500">{{ c.templateName }}</td>
              <td class="py-2.5 px-3 text-center">
                <span class="text-[11px] font-bold px-2 py-0.5 rounded" :class="campaignStatusClass(c.status)">
                  {{ campaignStatusLabel(c.status) }}
                </span>
              </td>
              <td class="py-2.5 px-3 text-center">{{ c.totalTargets }}</td>
              <td class="py-2.5 px-3 text-center">{{ c.sentCount }}</td>
              <td class="py-2.5 px-3 text-center text-amber-600">{{ c.openedCount }} ({{ rate(c.openedCount, c.sentCount) }}%)</td>
              <td class="py-2.5 px-3 text-center text-red-600 font-semibold">{{ c.clickedCount }} ({{ rate(c.clickedCount, c.sentCount) }}%)</td>
              <td class="py-2.5 px-3 text-center text-green-600">{{ c.reportedCount }} ({{ rate(c.reportedCount, c.sentCount) }}%)</td>
              <td class="py-2.5 px-3 text-gray-400 text-xs">{{ formatDt(c.createdAt) }}</td>
              <td class="py-2.5 px-3 text-center">
                <ExcelDownloadButton :busy="downloading === 'campaign-' + c.id"
                  title="이 훈련의 대상자별 결과를 엑셀로 내려받기"
                  @click="downloadCampaign(c)" />
              </td>
            </tr>
          </tbody>
        </table></div>
      </div>
    </template>

    <!-- ── 탭: 재해복구·BCP 훈련 결과 ── -->
    <template v-else>
      <!-- Stat Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">실시 훈련</p>
          <p class="text-2xl font-bold text-gray-900">{{ exercises.length }}<span class="text-sm font-normal text-gray-400 ml-1">건</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">완료</p>
          <p class="text-2xl font-bold text-blue-600">{{ completedExercises.length }}<span class="text-sm font-normal text-gray-400 ml-1">건</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">적합 판정</p>
          <p class="text-2xl font-bold text-green-600">{{ bcpPassCount }}<span class="text-sm font-normal text-gray-400 ml-1">건</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">평균 달성률</p>
          <p class="text-2xl font-bold text-indigo-600">{{ bcpAvgScore }}<span class="text-sm font-normal text-gray-400 ml-1">%</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">목표 RTO 달성률</p>
          <p class="text-2xl font-bold text-amber-500">{{ bcpRtoMetRate }}<span class="text-sm font-normal text-gray-400 ml-1">%</span></p>
        </div>
      </div>

      <!-- 훈련별 단계 수행 결과 -->
      <div class="card mb-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-bold text-gray-800">훈련별 단계 수행 결과</h2>
          <div class="flex items-center gap-3 text-[11px] text-gray-500">
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-green-500 inline-block"></span>성공</span>
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-amber-400 inline-block"></span>부분</span>
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-red-500 inline-block"></span>실패</span>
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-gray-200 inline-block"></span>미수행</span>
          </div>
        </div>
        <div v-if="completedExercises.length === 0" class="text-center py-8 text-gray-400 text-sm">완료된 훈련이 없습니다.</div>
        <div v-else class="space-y-4">
          <div v-for="e in completedExercises" :key="e.id">
            <div class="flex items-center justify-between text-sm mb-1.5">
              <div class="flex items-center gap-2 min-w-0">
                <span class="font-medium text-gray-800 truncate">{{ e.name }}</span>
                <span class="flex-shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded" :class="bcpResultClass(e.result)">
                  {{ bcpResultLabel(e.result) }}
                </span>
              </div>
              <span class="text-xs text-gray-500 flex-shrink-0 ml-3">
                {{ bcpMethodLabel(e.method) }} · 단계 {{ e.totalSteps }} · 달성률 {{ e.score }}%
                <template v-if="e.actualRtoMinutes != null"> · 실제 RTO {{ e.actualRtoMinutes }}분</template>
              </span>
            </div>
            <div class="h-3 rounded-full bg-gray-200 overflow-hidden flex">
              <div class="h-full bg-green-500 transition-all" :style="{ width: rate(e.passedSteps, e.totalSteps) + '%' }"></div>
              <div class="h-full bg-amber-400 transition-all" :style="{ width: rate(e.partialSteps, e.totalSteps) + '%' }"></div>
              <div class="h-full bg-red-500 transition-all" :style="{ width: rate(e.failedSteps, e.totalSteps) + '%' }"></div>
            </div>
            <p class="text-[11px] text-gray-400 mt-0.5 text-right">
              성공 {{ e.passedSteps }} · 부분 {{ e.partialSteps }} · 실패 {{ e.failedSteps }}
              <template v-if="e.pendingSteps"> · 미수행 {{ e.pendingSteps }}</template>
            </p>
          </div>
        </div>
      </div>

      <!-- 훈련 목록 -->
      <div class="card mb-6">
        <h2 class="text-sm font-bold text-gray-800 mb-4">훈련 실시 이력 ({{ exercises.length }}건)</h2>
        <div v-if="exercises.length === 0" class="text-center py-8 text-gray-400 text-sm">실시된 훈련이 없습니다.</div>
        <div v-else class="overflow-x-auto"><table class="w-full text-sm">
          <thead>
            <tr class="border-b text-left text-gray-500">
              <th class="py-2.5 px-3 font-semibold">훈련명</th>
              <th class="py-2.5 px-3 font-semibold">시나리오</th>
              <th class="py-2.5 px-3 font-semibold text-center w-24">방식</th>
              <th class="py-2.5 px-3 font-semibold text-center w-16">참가</th>
              <th class="py-2.5 px-3 font-semibold text-center w-28">RTO 목표/실제</th>
              <th class="py-2.5 px-3 font-semibold text-center w-20">달성률</th>
              <th class="py-2.5 px-3 font-semibold text-center w-20">판정</th>
              <th class="py-2.5 px-3 font-semibold text-center w-20">상태</th>
              <th class="py-2.5 px-3 font-semibold w-40">실시 일시</th>
              <th class="py-2.5 px-3 font-semibold text-center w-24">다운로드</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in exercises" :key="e.id" class="border-b hover:bg-gray-50">
              <td class="py-2.5 px-3 text-gray-800 font-medium">{{ e.name }}</td>
              <td class="py-2.5 px-3 text-gray-500 text-xs">{{ e.scenarioName }}</td>
              <td class="py-2.5 px-3 text-center text-xs text-gray-600">{{ bcpMethodLabel(e.method) }}</td>
              <td class="py-2.5 px-3 text-center text-gray-600">{{ e.participantCount ?? '-' }}</td>
              <td class="py-2.5 px-3 text-center text-xs">
                <span class="text-gray-500">{{ e.rtoMinutes ?? '-' }}</span>
                <span class="text-gray-300 mx-1">/</span>
                <span :class="e.rtoMet === false ? 'text-red-600 font-semibold' : e.rtoMet === true ? 'text-green-600 font-semibold' : 'text-gray-400'">
                  {{ e.actualRtoMinutes ?? '-' }}
                </span>
              </td>
              <td class="py-2.5 px-3 text-center font-mono">{{ e.score != null ? e.score + '%' : '-' }}</td>
              <td class="py-2.5 px-3 text-center">
                <span v-if="e.result" class="text-[11px] font-bold px-2 py-0.5 rounded-full" :class="bcpResultClass(e.result)">
                  {{ bcpResultLabel(e.result) }}
                </span>
                <span v-else class="text-gray-300">-</span>
              </td>
              <td class="py-2.5 px-3 text-center">
                <span class="text-[11px] font-bold px-2 py-0.5 rounded" :class="bcpStatusClass(e.status)">
                  {{ bcpStatusLabel(e.status) }}
                </span>
              </td>
              <td class="py-2.5 px-3 text-gray-400 text-xs">{{ formatDt(e.startedAt || e.plannedAt) }}</td>
              <td class="py-2.5 px-3 text-center">
                <ExcelDownloadButton :busy="downloading === 'exercise-' + e.id"
                  title="이 훈련의 단계별 결과·총평을 엑셀로 내려받기"
                  @click="downloadExercise(e)" />
              </td>
            </tr>
          </tbody>
        </table></div>
      </div>

      <!-- 도출된 개선사항 -->
      <div class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-4">훈련 총평 및 개선사항</h2>
        <div v-if="!exercisesWithFindings.length" class="text-center py-8 text-gray-400 text-sm">기록된 총평·개선사항이 없습니다.</div>
        <div v-else class="space-y-4">
          <div v-for="e in exercisesWithFindings" :key="e.id" class="border rounded-xl p-4">
            <div class="flex items-center justify-between mb-2">
              <p class="text-sm font-semibold text-gray-800">{{ e.name }}</p>
              <span class="text-xs text-gray-400">{{ formatDt(e.endedAt) }}</span>
            </div>
            <p v-if="e.summary" class="text-xs text-gray-600 whitespace-pre-line mb-2">{{ e.summary }}</p>
            <p v-if="e.improvement" class="text-xs text-gray-700 whitespace-pre-line bg-amber-50 border border-amber-100 rounded-lg p-3">
              개선사항: {{ e.improvement }}
            </p>
          </div>
        </div>
      </div>
    </template>

    </div><!-- /page-body -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { trainingApi, phishingApi, bcpApi } from '@/api'
import PiMaskToggle from '@/components/privacy/PiMaskToggle.vue'
import ExcelDownloadButton from '@/components/ExcelDownloadButton.vue'
import { usePiMaskingStore } from '@/stores/piMasking'

// 이수자 이름은 코드관리의 항목별 마스킹 기준에 따라 가려서 표시한다
const pi = usePiMaskingStore()

const activeTab = ref('training')
const loading = ref(true)
const forbidden = ref(false)

const courses = ref([])          // 코스별 이수 요약
const completions = ref([])      // 이수 이력
const completionFilter = ref(null)
const campaigns = ref([])        // 모의훈련 캠페인
const exercises = ref([])        // 재해복구·BCP 훈련

const totalCompleted = computed(() => courses.value.reduce((s, c) => s + c.completedCount, 0))
const overallPassRate = computed(() => {
  const total = totalCompleted.value
  const passed = courses.value.reduce((s, c) => s + c.passedCount, 0)
  return rate(passed, total)
})
const overallAvgScore = computed(() => {
  const scored = completions.value.filter(c => c.score != null)
  if (!scored.length) return 0
  return Math.round(scored.reduce((s, c) => s + c.score, 0) / scored.length)
})

const filteredCompletions = computed(() =>
  completionFilter.value == null
    ? completions.value
    : completions.value.filter(c => c.courseId === completionFilter.value)
)

function rate(n, d) { return d > 0 ? Math.round((n / d) * 100) : 0 }
function sum(key) { return campaigns.value.reduce((s, c) => s + (c[key] ?? 0), 0) }
function avgRate(key) { return rate(sum(key), sum('sentCount')) }

function campaignBars(c) {
  const d = c.sentCount || c.totalTargets
  return [
    { label: '열람', pct: rate(c.openedCount, d), color: 'bg-amber-400' },
    { label: '클릭', pct: rate(c.clickedCount, d), color: 'bg-red-500' },
    { label: '신고', pct: rate(c.reportedCount, d), color: 'bg-green-500' },
  ]
}

// ── 재해복구·BCP 훈련 ──────────────────────────────────────────────────────
const completedExercises = computed(() => exercises.value.filter(e => e.status === 'COMPLETED'))
const exercisesWithFindings = computed(() => exercises.value.filter(e => e.summary || e.improvement))
const bcpPassCount = computed(() => exercises.value.filter(e => e.result === 'PASS').length)
const bcpAvgScore = computed(() => {
  const scored = exercises.value.filter(e => e.score != null)
  if (!scored.length) return 0
  return Math.round(scored.reduce((s, e) => s + e.score, 0) / scored.length)
})
const bcpRtoMetRate = computed(() => {
  // 목표·실제 RTO가 모두 기록된 훈련만 모수로 삼는다
  const measured = exercises.value.filter(e => e.rtoMet != null)
  return rate(measured.filter(e => e.rtoMet).length, measured.length)
})

function bcpMethodLabel(m) { return { TABLETOP: '도상훈련', SIMULATION: '시뮬레이션', FAILOVER: '실제 전환' }[m] || m || '-' }
function bcpResultLabel(r) { return { PASS: '적합', PARTIAL: '보완필요', FAIL: '부적합' }[r] || r }
function bcpResultClass(r) {
  return { PASS: 'bg-green-100 text-green-700', PARTIAL: 'bg-amber-100 text-amber-700', FAIL: 'bg-red-100 text-red-600' }[r] || 'bg-gray-100 text-gray-500'
}
function bcpStatusLabel(s) { return { DRAFT: '계획', RUNNING: '진행중', COMPLETED: '완료', CANCELLED: '취소' }[s] || s }
function bcpStatusClass(s) {
  return {
    RUNNING: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-gray-100 text-gray-500',
  }[s] || 'bg-gray-100 text-gray-600'
}

function campaignStatusLabel(s) {
  return { DRAFT: '대기', SCHEDULED: '예약', RUNNING: '진행중', SENT: '발송됨', COMPLETED: '완료', CANCELLED: '취소' }[s] || s
}
function campaignStatusClass(s) {
  return {
    RUNNING: 'bg-blue-100 text-blue-700', SENT: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-gray-100 text-gray-500',
  }[s] || 'bg-gray-100 text-gray-600'
}

// ── 훈련별 엑셀 내려받기 ───────────────────────────────────────────────────
// 한 번에 하나만 진행하도록 진행 중인 항목 키('course-3' 등)를 담아둔다
const downloading = ref(null)

async function download(key, request, filename) {
  if (downloading.value) return
  downloading.value = key
  try {
    const blob = await request()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    alert(typeof e === 'string' ? e : '엑셀 다운로드에 실패했습니다.')
  } finally {
    downloading.value = null
  }
}

function safeName(name) { return String(name || '').replace(/[\\/:*?"<>|]/g, '_') }

function downloadCourse(c) {
  download(`course-${c.courseId}`, () => trainingApi.exportCourseExcel(c.courseId),
    `교육결과_${safeName(c.title)}.xlsx`)
}
function downloadCampaign(c) {
  download(`campaign-${c.id}`, () => phishingApi.exportCampaignExcel(c.id),
    `모의훈련결과_${safeName(c.name)}.xlsx`)
}
function downloadExercise(e) {
  download(`exercise-${e.id}`, () => bcpApi.exportExerciseExcel(e.id),
    `BCP훈련결과_${safeName(e.name)}.xlsx`)
}

function formatDt(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  return `${d.toLocaleDateString()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(async () => {
  loading.value = true
  try {
    const [r1, r2, r3, r4] = await Promise.all([
      trainingApi.results(),
      trainingApi.resultCompletions(),
      phishingApi.listCampaigns(),
      bcpApi.listExercises(),
    ])
    courses.value = r1.data ?? []
    completions.value = r2.data ?? []
    campaigns.value = r3.data ?? []
    exercises.value = r4.data ?? []
  } catch (e) {
    if (String(e).includes('403') || e?.response?.status === 403) forbidden.value = true
    else console.error(e)
  } finally {
    loading.value = false
  }
})
</script>
