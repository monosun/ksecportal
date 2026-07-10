<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">위험 평가</h1>
        <p class="text-sm text-gray-500 mt-1">연도/차수별 위험평가를 관리합니다</p>
      </div>
    </div>

    <!-- 연도 네비게이션 + 차수 선택 + 버튼 -->
    <div class="flex items-center gap-3 mb-5 flex-wrap">
      <div class="flex items-center gap-2 bg-white border border-gray-200 rounded-lg px-3 py-1.5">
        <button @click="prevYear" class="text-gray-400 hover:text-gray-700 px-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
          </svg>
        </button>
        <span class="text-base font-bold text-gray-800 min-w-16 text-center">{{ currentYear }}년</span>
        <button @click="nextYear" class="text-gray-400 hover:text-gray-700 px-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </button>
      </div>

      <select v-if="rounds.length > 0" v-model="selectedRoundId" @change="onRoundChange" class="input w-56">
        <option v-for="r in rounds" :key="r.id" :value="r.id">
          {{ r.roundNo }}차 ({{ r.roundDate }}) — {{ r.status === 'COMPLETED' ? '완료' : '진행중' }}
        </option>
      </select>
      <span v-else class="text-sm text-gray-400">이 연도에 차수가 없습니다</span>

      <button @click="openRoundModal(null)" class="btn-secondary text-sm flex items-center gap-1.5">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        새 차수 추가
      </button>

      <button v-if="selectedRound" @click="openRoundModal(selectedRound)" class="btn-secondary text-sm">
        차수 수정
      </button>

      <button v-if="selectedRound" @click="confirmDeleteRound(selectedRound)"
        class="text-sm text-red-500 hover:text-red-700 border border-red-200 rounded-lg px-3 py-1.5 hover:bg-red-50">
        차수 삭제
      </button>

      <div class="flex-1"></div>

      <!-- 엑셀 다운로드 -->
      <button v-if="selectedRound && assessments.length > 0" @click="downloadExcel"
        :disabled="excelDownloading"
        class="btn-secondary text-sm flex items-center gap-1.5">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        {{ excelDownloading ? '다운로드 중...' : '엑셀 다운로드' }}
      </button>

      <!-- 자산 자동 불러오기 (진행중 차수만) -->
      <button v-if="selectedRound && selectedRound.status === 'IN_PROGRESS'" @click="confirmAutoPopulate"
        :disabled="autoPopulating"
        class="btn-secondary text-sm flex items-center gap-1.5">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
        </svg>
        {{ autoPopulating ? '불러오는 중...' : '자산 자동 불러오기' }}
      </button>

      <button v-if="selectedRound && selectedRound.status === 'IN_PROGRESS'"
        @click="openAssessmentModal(null)" class="btn-primary flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        평가 추가
      </button>
    </div>

    <!-- 선택된 차수 정보 -->
    <div v-if="selectedRound" class="bg-blue-50 border border-blue-200 rounded-lg px-4 py-2.5 mb-4 flex items-center gap-4 text-sm flex-wrap">
      <span class="font-semibold text-blue-800">{{ selectedRound.roundNo }}차 평가</span>
      <span class="text-blue-600">{{ selectedRound.roundDate }}</span>
      <span v-if="selectedRound.title" class="text-blue-600">· {{ selectedRound.title }}</span>
      <span :class="selectedRound.status === 'COMPLETED' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'"
        class="px-2 py-0.5 rounded-full text-xs font-semibold">
        {{ selectedRound.status === 'COMPLETED' ? '완료' : '진행중' }}
      </span>
      <span v-if="selectedRound.status === 'COMPLETED'" class="text-xs text-gray-500">
        완료된 차수는 수정이 비활성화됩니다. 코드 변경에 영향을 받지 않습니다.
      </span>
      <button v-if="selectedRound.status === 'IN_PROGRESS'"
        @click="completeRound"
        class="ml-auto text-xs text-green-700 border border-green-300 rounded-lg px-3 py-1 hover:bg-green-50">
        완료 처리
      </button>
    </div>

    <!-- Summary Cards (차수 전체 기준 — 아래 필터 조회에 영향받지 않음) -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
      <div class="card text-center">
        <p class="text-3xl font-bold text-gray-900">{{ assessments.length }}</p>
        <p class="text-sm text-gray-500 mt-1">전체 평가</p>
      </div>
      <div class="card text-center">
        <p class="text-3xl font-bold text-red-600">{{ countByGrade('HIGH') }}</p>
        <p class="text-sm text-gray-500 mt-1">고위험</p>
      </div>
      <div class="card text-center">
        <p class="text-3xl font-bold text-yellow-600">{{ countByGrade('MEDIUM') }}</p>
        <p class="text-sm text-gray-500 mt-1">중위험</p>
      </div>
      <div class="card text-center">
        <p class="text-3xl font-bold text-green-600">{{ countByGrade('LOW') }}</p>
        <p class="text-sm text-gray-500 mt-1">저위험</p>
      </div>
    </div>

    <!-- 처리방법별 현황 (차수 전체 기준) -->
    <div v-if="selectedRound && assessments.length > 0" class="bg-white border border-gray-200 rounded-lg px-4 py-2.5 mb-3 flex items-center gap-2 flex-wrap text-sm">
      <span class="text-xs font-semibold text-gray-500 mr-1">처리방법</span>
      <span class="flex items-center gap-1 px-2.5 py-1 rounded-full bg-blue-50 text-blue-700 font-medium text-xs">
        감소 <span class="font-bold">{{ treatmentCounts['감소'] }}</span>
      </span>
      <span class="flex items-center gap-1 px-2.5 py-1 rounded-full bg-green-50 text-green-700 font-medium text-xs">
        수용 <span class="font-bold">{{ treatmentCounts['수용'] }}</span>
      </span>
      <span class="flex items-center gap-1 px-2.5 py-1 rounded-full bg-orange-50 text-orange-700 font-medium text-xs">
        회피 <span class="font-bold">{{ treatmentCounts['회피'] }}</span>
      </span>
      <span class="flex items-center gap-1 px-2.5 py-1 rounded-full bg-purple-50 text-purple-700 font-medium text-xs">
        이전 <span class="font-bold">{{ treatmentCounts['이전'] }}</span>
      </span>
    </div>

    <!-- 위험도 수용 기준 설정 -->
    <div v-if="selectedRound" class="bg-amber-50 border border-amber-200 rounded-lg px-4 py-2.5 mb-3 flex items-center gap-3 flex-wrap text-sm">
      <svg class="w-4 h-4 text-amber-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"/>
      </svg>
      <span class="font-semibold text-amber-800">위험수용 기준 설정</span>
      <div class="flex items-center gap-2">
        <span class="text-amber-700">수용 기준 위험점수:</span>
        <input v-model.number="acceptanceThreshold" type="number" min="1" max="25"
          class="input w-16 text-sm py-1 text-center font-bold" />
        <span class="text-amber-600 text-xs">이하</span>
      </div>
      <div class="h-4 w-px bg-amber-200"></div>
      <span class="text-xs text-amber-600">
        위험등급 기준: 고위험 ≥ {{ highThreshold }}, 중위험 ≥ {{ medThreshold }}, 저위험 &lt; {{ medThreshold }}
      </span>
      <div class="h-4 w-px bg-amber-200"></div>
      <button @click="selectByThreshold"
        class="text-xs font-medium text-amber-800 border border-amber-300 rounded px-3 py-1 hover:bg-amber-100 transition-colors flex items-center gap-1">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
        </svg>
        수용기준 이하 자동선택 ({{ belowThresholdCount }}개)
      </button>
    </div>

    <!-- 검색 필터 -->
    <div v-if="selectedRound" class="bg-white border border-gray-200 rounded-lg px-4 py-3 mb-3 flex items-center gap-3 flex-wrap">
      <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
      </svg>
      <input v-model="searchAsset" type="text" placeholder="자산명 검색"
        class="input w-32 text-sm py-1.5"/>
      <select v-model="searchAssetType" class="input w-32 text-sm py-1.5">
        <option value="">자산 유형 전체</option>
        <option v-for="t in assetTypeOptions" :key="t" :value="t">{{ t }}</option>
      </select>
      <select v-model="searchAssetEnv" class="input w-28 text-sm py-1.5">
        <option value="">환경 전체</option>
        <option value="PRODUCTION">운영</option>
        <option value="STAGING">스테이징</option>
        <option value="DEVELOPMENT">개발</option>
        <option value="TEST">테스트</option>
      </select>
      <input v-model="searchThreat" type="text" placeholder="위협명 검색"
        class="input w-32 text-sm py-1.5"/>
      <input v-model="searchVulnerability" type="text" placeholder="취약점 검색"
        class="input w-32 text-sm py-1.5"/>
      <select v-model="searchThreatType" class="input w-28 text-sm py-1.5">
        <option value="">위협 유형 전체</option>
        <option v-for="t in threatTypeOptions" :key="t" :value="t">{{ t }}</option>
      </select>
      <select v-model="searchGrade" class="input w-24 text-sm py-1.5">
        <option value="">등급 전체</option>
        <option value="HIGH">고위험</option>
        <option value="MEDIUM">중위험</option>
        <option value="LOW">저위험</option>
      </select>
      <select v-model="searchTreatment" class="input w-28 text-sm py-1.5">
        <option value="">처리방법 전체</option>
        <option v-for="t in treatments" :key="t" :value="t">{{ t }}</option>
      </select>
      <!-- 위험점수 범위 검색 -->
      <div class="flex items-center gap-1">
        <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/>
        </svg>
        <input v-model.number="searchScoreMin" type="number" min="1" max="25" placeholder="점수 최소"
          class="input w-20 text-sm py-1.5 text-center"/>
        <span class="text-gray-400 text-xs">~</span>
        <input v-model.number="searchScoreMax" type="number" min="1" max="25" placeholder="점수 최대"
          class="input w-20 text-sm py-1.5 text-center"/>
      </div>
      <button v-if="hasActiveFilter"
        @click="clearSearch" class="text-xs text-gray-400 hover:text-gray-600">초기화</button>
    </div>

    <!-- 일괄 처리 바 -->
    <div v-if="selectedIds.length > 0" class="bg-blue-50 border border-blue-200 rounded-lg px-4 py-2.5 mb-3 flex items-center gap-3 text-sm flex-wrap">
      <svg class="w-4 h-4 text-blue-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <span class="text-blue-800 font-medium">{{ selectedIds.length }}개 항목 선택됨</span>
      <span class="text-xs text-gray-500">처리방법 일괄 적용:</span>
      <button v-for="t in treatments" :key="t" @click="bulkSetTreatment(t)" :disabled="bulkProcessing"
        class="text-xs font-semibold px-3 py-1.5 rounded border disabled:opacity-50 transition-colors"
        :class="bulkBtnClass(t)">
        {{ bulkProcessing ? '처리 중...' : t }}
      </button>
      <button @click="selectedIds = []" class="text-xs text-gray-500 hover:text-gray-700 border border-gray-300 rounded px-2.5 py-1.5 hover:bg-gray-50">
        선택해제
      </button>
    </div>

    <!-- Table -->
    <div class="card p-0 overflow-hidden">
      <div v-if="!selectedRound" class="p-12 text-center text-gray-400">
        <p class="text-sm">차수를 선택하거나 새 차수를 추가하세요</p>
      </div>
      <div v-else-if="loadingAssessments" class="p-8 text-center text-gray-400 text-sm">로딩 중...</div>
      <div v-else-if="filteredAssessments.length === 0" class="p-12 text-center text-gray-400">
        <p class="text-sm">
          <span v-if="assessments.length === 0">이 차수에 평가 항목이 없습니다. "평가 추가" 또는 "자산 자동 불러오기"를 클릭하세요.</span>
          <span v-else>검색 결과가 없습니다.</span>
        </p>
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b border-gray-100">
            <tr>
              <!-- 전체 선택 체크박스 -->
              <th class="px-3 py-3 w-10">
                <input type="checkbox"
                  :checked="allSelected"
                  :ref="el => { if (el) el.indeterminate = someSelected && !allSelected }"
                  @change="toggleSelectAll"
                  class="w-4 h-4 rounded border-gray-300 text-primary-600 cursor-pointer"/>
              </th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600">자산명</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600">자산유형</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600">위협명</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600">위협유형</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600">취약점</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600">발생가능성</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600">영향도</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600">위험점수</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600">위험등급</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600">처리방법</th>
              <th v-if="isEditable" class="text-center px-4 py-3 font-semibold text-gray-600">관리</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="item in pagedAssessments" :key="item.id"
              class="hover:bg-gray-50 transition-colors"
              :class="{ 'bg-amber-50/40': item.riskScore <= acceptanceThreshold }">
              <!-- 개별 선택 체크박스 -->
              <td class="px-3 py-3.5 w-10">
                <input type="checkbox"
                  :checked="selectedIds.includes(item.id)"
                  @change="toggleSelect(item.id)"
                  class="w-4 h-4 rounded border-gray-300 text-primary-600 cursor-pointer"/>
              </td>
              <td class="px-4 py-3.5 font-medium text-gray-900">{{ item.assetName || '-' }}</td>
              <td class="px-4 py-3.5">
                <span v-if="item.assetType" class="px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700">{{ item.assetType }}</span>
                <span v-else class="text-gray-400">-</span>
              </td>
              <td class="px-4 py-3.5 text-gray-700">{{ item.threatName || '-' }}</td>
              <td class="px-4 py-3.5">
                <span v-if="item.threatType" class="px-2 py-0.5 rounded text-xs font-medium bg-purple-50 text-purple-700">{{ item.threatType }}</span>
                <span v-else class="text-gray-400">-</span>
              </td>
              <td class="px-4 py-3.5 text-gray-500 text-xs max-w-36 truncate" :title="item.vulnerability || ''">{{ item.vulnerability || '-' }}</td>
              <td class="px-4 py-3.5 text-center font-semibold text-gray-700">{{ item.likelihood }}</td>
              <td class="px-4 py-3.5 text-center font-semibold text-gray-700">{{ item.impact }}</td>
              <td class="px-4 py-3.5 text-center">
                <div class="flex items-center justify-center gap-1">
                  <span class="font-bold text-base" :class="scoreColor(item.riskScore)">{{ item.riskScore }}</span>
                  <!-- 수용 기준 이하 표시 -->
                  <span v-if="item.riskScore <= acceptanceThreshold"
                    class="text-xs text-amber-600 font-medium bg-amber-100 px-1 rounded" title="수용 기준 이하">수용가능</span>
                </div>
              </td>
              <td class="px-4 py-3.5 text-center">
                <span class="px-2.5 py-1 rounded-full text-xs font-bold" :class="gradeBadge(item.riskGrade)">
                  {{ gradeLabel(item.riskGrade) }}
                </span>
              </td>
              <td class="px-4 py-3.5">
                <span class="px-2 py-0.5 rounded text-xs font-medium"
                  :class="item.treatment === '수용' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'">
                  {{ item.treatment }}
                </span>
              </td>
              <td v-if="isEditable" class="px-4 py-3.5 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button @click="openAssessmentModal(item)" class="text-blue-600 hover:text-blue-800 text-xs font-medium">수정</button>
                  <button @click="confirmDeleteAssessment(item)" class="text-red-500 hover:text-red-700 text-xs font-medium">삭제</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 페이지네이션 -->
        <div v-if="filteredAssessments.length > 0" class="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
          <!-- 좌: 건수 정보 + 페이지당 행 수 -->
          <div class="flex items-center gap-3 text-sm text-gray-500">
            <span>
              {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, filteredAssessments.length) }}
              / {{ filteredAssessments.length }}건
            </span>
            <select v-model.number="pageSize" class="text-xs border border-gray-200 rounded px-2 py-1 text-gray-600 bg-white">
              <option :value="10">10개</option>
              <option :value="20">20개</option>
              <option :value="50">50개</option>
              <option :value="100">100개</option>
            </select>
          </div>

          <!-- 우: 페이지 버튼 -->
          <div class="flex items-center gap-1">
            <button @click="currentPage = 1" :disabled="currentPage === 1"
              class="px-2 py-1 rounded text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30">«</button>
            <button @click="currentPage--" :disabled="currentPage === 1"
              class="px-2 py-1 rounded text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30">‹</button>

            <button v-for="p in pageRange" :key="p" @click="currentPage = p"
              class="min-w-[28px] px-2 py-1 rounded text-xs font-medium transition-colors"
              :class="p === currentPage
                ? 'bg-primary-500 text-white'
                : 'text-gray-600 hover:bg-gray-100'">
              {{ p }}
            </button>

            <button @click="currentPage++" :disabled="currentPage === totalPages"
              class="px-2 py-1 rounded text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30">›</button>
            <button @click="currentPage = totalPages" :disabled="currentPage === totalPages"
              class="px-2 py-1 rounded text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30">»</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 차수 추가/수정 모달 -->
    <div v-if="showRoundModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">{{ editRound ? '차수 수정' : '새 차수 추가' }}</h2>
          <button @click="showRoundModal = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">연도 <span class="text-red-500">*</span></label>
              <input v-model.number="roundForm.year" type="number" class="input w-full" placeholder="2026"/>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">차수 <span class="text-red-500">*</span></label>
              <input v-model.number="roundForm.roundNo" type="number" class="input w-full" placeholder="1" min="1"/>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">평가 날짜 <span class="text-red-500">*</span></label>
            <input v-model="roundForm.roundDate" type="date" class="input w-full"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">제목</label>
            <input v-model="roundForm.title" class="input w-full" placeholder="예: 2026년 상반기 위험평가"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">상태</label>
            <select v-model="roundForm.status" class="input w-full">
              <option value="IN_PROGRESS">진행중</option>
              <option value="COMPLETED">완료</option>
            </select>
          </div>
          <!-- 신규 차수일 때만: 자산 자동 불러오기 옵션 -->
          <div v-if="!editRound" class="flex items-center gap-2 p-3 rounded-lg bg-blue-50 border border-blue-200">
            <input id="auto-pop" v-model="autoPopOnCreate" type="checkbox" class="w-4 h-4 text-primary-600"/>
            <label for="auto-pop" class="text-sm text-blue-800 cursor-pointer select-none">
              차수 생성 후 모든 자산 자동 불러오기
            </label>
          </div>
        </div>
        <p v-if="roundError" class="mt-3 text-sm text-red-600">{{ roundError }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="showRoundModal = false" class="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="saveRound" :disabled="roundSaving" class="btn-primary disabled:opacity-50">
            {{ roundSaving ? '저장 중...' : '저장' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 평가 추가/수정 모달 -->
    <div v-if="showAssessmentModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-xl p-6 max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">{{ editAssessment ? '평가 수정' : '위험 평가 추가' }}</h2>
          <button @click="showAssessmentModal = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="space-y-4">
          <!-- 자산 -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">자산 선택</label>
              <select v-model="form.assetId" @change="onAssetChange" class="input w-full text-sm">
                <option :value="null">직접 입력</option>
                <option v-for="a in assets" :key="a.id" :value="a.id">{{ a.name }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">자산명 <span class="text-red-500">*</span></label>
              <input v-model="form.assetName" class="input w-full text-sm" placeholder="자산명"/>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">자산 유형 <span class="text-xs text-gray-400">(스냅샷 저장)</span></label>
            <input v-model="form.assetType" class="input w-full text-sm" placeholder="예: 서버, 애플리케이션" :readonly="!!form.assetId"/>
          </div>
          <!-- 위협 -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">위협 선택</label>
              <select v-model="form.threatId" @change="onThreatChange" class="input w-full text-sm">
                <option :value="null">직접 입력</option>
                <optgroup v-for="(group, type) in groupedThreats" :key="type" :label="type">
                  <option v-for="t in group" :key="t.id" :value="t.id">{{ t.name }}</option>
                </optgroup>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">위협명 <span class="text-red-500">*</span></label>
              <input v-model="form.threatName" class="input w-full text-sm" placeholder="위협명"/>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">위협 유형 <span class="text-xs text-gray-400">(스냅샷 저장)</span></label>
            <input v-model="form.threatType" class="input w-full text-sm" placeholder="예: 외부공격, 내부위협" :readonly="!!form.threatId"/>
          </div>
          <!-- 취약점 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              취약점 <span class="text-xs text-gray-400">(위협 선택 시 위협 설명이 자동 입력됩니다)</span>
            </label>
            <textarea v-model="form.vulnerability" rows="2" class="input w-full text-sm resize-none" placeholder="관련 취약점 설명"/>
          </div>
          <!-- 발생가능성 / 영향도 -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">발생 가능성 ({{ form.likelihood }})</label>
              <div class="flex gap-1.5">
                <button v-for="n in 5" :key="n" @click="form.likelihood = n"
                  class="flex-1 py-1.5 rounded text-xs font-bold border transition-colors"
                  :class="form.likelihood === n ? 'bg-blue-600 text-white border-blue-600' : 'border-gray-200 text-gray-500 hover:border-gray-400'">
                  {{ n }}
                </button>
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">영향도 ({{ form.impact }})</label>
              <div class="flex gap-1.5">
                <button v-for="n in 5" :key="n" @click="form.impact = n"
                  class="flex-1 py-1.5 rounded text-xs font-bold border transition-colors"
                  :class="form.impact === n ? 'bg-orange-500 text-white border-orange-500' : 'border-gray-200 text-gray-500 hover:border-gray-400'">
                  {{ n }}
                </button>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-4 p-3 rounded-lg bg-gray-50">
            <div>
              <span class="text-sm text-gray-600">위험점수:</span>
              <span class="ml-2 text-lg font-bold" :class="scoreColor(form.likelihood * form.impact)">
                {{ form.likelihood * form.impact }}
              </span>
              <span v-if="form.likelihood * form.impact <= acceptanceThreshold"
                class="ml-1 text-xs text-amber-600 bg-amber-100 px-1.5 py-0.5 rounded">수용가능</span>
            </div>
            <div>
              <span class="text-sm text-gray-600">위험등급:</span>
              <span class="ml-2 px-2.5 py-1 rounded-full text-xs font-bold" :class="gradeBadge(calcGrade(form.likelihood * form.impact))">
                {{ gradeLabel(calcGrade(form.likelihood * form.impact)) }}
              </span>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">처리방법</label>
            <select v-model="form.treatment" class="input w-full text-sm">
              <option v-for="t in treatments" :key="t" :value="t">{{ t }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">비고</label>
            <textarea v-model="form.notes" class="input w-full text-sm" rows="3" placeholder="평가 의견을 입력하세요"></textarea>
          </div>
        </div>
        <p v-if="assessmentError" class="mt-3 text-sm text-red-600">{{ assessmentError }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="showAssessmentModal = false" class="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="saveAssessment" :disabled="assessmentSaving || !form.assetName || !form.threatName"
            class="btn-primary disabled:opacity-50">
            {{ assessmentSaving ? '저장 중...' : (editAssessment ? '수정 완료' : '추가') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 삭제 확인 모달 -->
    <div v-if="showDeleteModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">{{ deleteTarget?.type === 'round' ? '차수 삭제' : '평가 삭제' }}</h2>
        <p class="text-sm text-gray-600 mb-2">{{ deleteTarget?.message }}</p>
        <p v-if="deleteTarget?.warning" class="text-sm text-orange-600 bg-orange-50 p-2 rounded">{{ deleteTarget.warning }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="showDeleteModal = false" class="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="executeDelete" :disabled="deleting" class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50">
            {{ deleting ? '삭제 중...' : '삭제' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 자산 자동 불러오기 확인 모달 -->
    <div v-if="showAutoPopModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">자산 자동 불러오기</h2>
        <p class="text-sm text-gray-600">현재 등록된 <strong>모든 활성 자산</strong>을 이 차수에 평가 항목으로 추가합니다.<br/>
        이미 등록된 자산은 건너뜁니다. 계속하시겠습니까?</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="showAutoPopModal = false" class="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="doAutoPopulate" :disabled="autoPopulating" class="btn-primary disabled:opacity-50">
            {{ autoPopulating ? '불러오는 중...' : '확인' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { riskApi, assetApi, threatApi } from '@/api'

// 연도/차수 상태
const currentYear = ref(new Date().getFullYear())
const years = ref([])
const rounds = ref([])
const selectedRoundId = ref(null)

const selectedRound = computed(() => rounds.value.find(r => r.id === selectedRoundId.value) || null)
const isEditable = computed(() => selectedRound.value?.status === 'IN_PROGRESS')

// 평가 항목
const assessments = ref([])
const loadingAssessments = ref(false)

// 위험등급 기준 (표시용, 현재 백엔드 고정값)
const highThreshold = 15
const medThreshold = 8

// 위험수용 기준 점수 (localStorage 유지)
const acceptanceThreshold = ref(parseInt(localStorage.getItem('risk_acceptance_threshold') || '6'))
watch(acceptanceThreshold, v => localStorage.setItem('risk_acceptance_threshold', String(v)))

// 검색 필터
const searchAsset = ref('')
const searchAssetType = ref('')
const searchAssetEnv = ref('')
const searchThreat = ref('')
const searchVulnerability = ref('')
const searchThreatType = ref('')
const searchGrade = ref('')
const searchTreatment = ref('')
const searchScoreMin = ref('')
const searchScoreMax = ref('')

const hasActiveFilter = computed(() =>
  !!(searchAsset.value || searchAssetType.value || searchAssetEnv.value ||
     searchThreat.value || searchVulnerability.value || searchThreatType.value || searchGrade.value || searchTreatment.value ||
     searchScoreMin.value !== '' || searchScoreMax.value !== '')
)

const filteredAssessments = computed(() => {
  return assessments.value.filter(a => {
    if (searchAsset.value && !a.assetName?.toLowerCase().includes(searchAsset.value.toLowerCase())) return false
    if (searchAssetType.value && a.assetType !== searchAssetType.value) return false
    if (searchAssetEnv.value && a.assetEnvironment !== searchAssetEnv.value) return false
    if (searchThreat.value && !a.threatName?.toLowerCase().includes(searchThreat.value.toLowerCase())) return false
    if (searchVulnerability.value && !a.vulnerability?.toLowerCase().includes(searchVulnerability.value.toLowerCase())) return false
    if (searchThreatType.value && a.threatType !== searchThreatType.value) return false
    if (searchGrade.value && a.riskGrade !== searchGrade.value) return false
    if (searchTreatment.value && a.treatment !== searchTreatment.value) return false
    if (searchScoreMin.value !== '' && a.riskScore < Number(searchScoreMin.value)) return false
    if (searchScoreMax.value !== '' && a.riskScore > Number(searchScoreMax.value)) return false
    return true
  })
})

// 수용 기준 이하 항목 수
const belowThresholdCount = computed(() =>
  filteredAssessments.value.filter(a => a.riskScore <= acceptanceThreshold.value).length
)

// 페이지네이션
const currentPage = ref(1)
const pageSize = ref(20)

const totalPages = computed(() => Math.ceil(filteredAssessments.value.length / pageSize.value) || 1)

const pagedAssessments = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredAssessments.value.slice(start, start + pageSize.value)
})

const pageRange = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const half = 3
  let start = Math.max(1, cur - half)
  let end = Math.min(total, cur + half)
  if (end - start < 6) {
    if (start === 1) end = Math.min(total, 7)
    else start = Math.max(1, end - 6)
  }
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

watch(filteredAssessments, () => { currentPage.value = 1 })

// 체크박스 선택 상태
const selectedIds = ref([])
const bulkProcessing = ref(false)

const allSelected = computed(() =>
  filteredAssessments.value.length > 0 &&
  filteredAssessments.value.every(a => selectedIds.value.includes(a.id))
)

const someSelected = computed(() => selectedIds.value.length > 0)

function toggleSelectAll() {
  if (allSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = filteredAssessments.value.map(a => a.id)
  }
}

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

function selectByThreshold() {
  selectedIds.value = filteredAssessments.value
    .filter(a => a.riskScore <= acceptanceThreshold.value)
    .map(a => a.id)
}

async function bulkSetTreatment(treatment) {
  if (!selectedIds.value.length || bulkProcessing.value) return
  bulkProcessing.value = true
  try {
    await riskApi.bulkUpdateTreatment([...selectedIds.value], treatment)
    selectedIds.value = []
    await loadAssessments()
  } catch (e) {
    alert(e || '일괄 처리에 실패했습니다.')
  } finally {
    bulkProcessing.value = false
  }
}

function bulkBtnClass(t) {
  return {
    수용: 'border-green-300 text-green-700 bg-green-50 hover:bg-green-100',
    감소: 'border-blue-300 text-blue-700 bg-blue-50 hover:bg-blue-100',
    회피: 'border-orange-300 text-orange-700 bg-orange-50 hover:bg-orange-100',
    이전: 'border-purple-300 text-purple-700 bg-purple-50 hover:bg-purple-100',
  }[t] || 'border-gray-300 text-gray-700 bg-gray-50 hover:bg-gray-100'
}

// 필터 옵션 (현재 데이터에서 유니크 값 추출)
const assetTypeOptions = computed(() => {
  return [...new Set(assessments.value.map(a => a.assetType).filter(Boolean))].sort()
})

const threatTypeOptions = computed(() => {
  return [...new Set(assessments.value.map(a => a.threatType).filter(Boolean))].sort()
})

function clearSearch() {
  searchAsset.value = ''
  searchAssetType.value = ''
  searchAssetEnv.value = ''
  searchThreat.value = ''
  searchVulnerability.value = ''
  searchThreatType.value = ''
  searchGrade.value = ''
  searchTreatment.value = ''
  searchScoreMin.value = ''
  searchScoreMax.value = ''
}

// 자산/위협 목록
const assets = ref([])
const threats = ref([])

const groupedThreats = computed(() => {
  const groups = {}
  for (const t of threats.value) {
    const type = t.type || '기타'
    if (!groups[type]) groups[type] = []
    groups[type].push(t)
  }
  return groups
})

// 처리방법
const treatments = ['수용', '감소', '회피', '이전']

// 차수 모달
const showRoundModal = ref(false)
const editRound = ref(null)
const roundSaving = ref(false)
const roundError = ref('')
const autoPopOnCreate = ref(false)
const roundForm = reactive({ year: new Date().getFullYear(), roundNo: 1, roundDate: '', title: '', status: 'IN_PROGRESS' })

// 평가 모달
const showAssessmentModal = ref(false)
const editAssessment = ref(null)
const assessmentSaving = ref(false)
const assessmentError = ref('')
const form = reactive({
  assetId: null, assetName: '', assetType: '', assetEnvironment: '',
  threatId: null, threatName: '', threatType: '',
  vulnerability: '', likelihood: 3, impact: 3, treatment: '감소', notes: ''
})

// 삭제
const showDeleteModal = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)

// 자산 자동 불러오기
const showAutoPopModal = ref(false)
const autoPopulating = ref(false)

// 엑셀 다운로드
const excelDownloading = ref(false)

// 연도 이동
function prevYear() { currentYear.value-- }
function nextYear() { currentYear.value++ }

function onRoundChange() { loadAssessments() }

async function loadYears() {
  try {
    const res = await riskApi.listYears()
    years.value = res.data || []
  } catch { years.value = [] }
}

async function loadRounds() {
  try {
    const res = await riskApi.listRounds(currentYear.value)
    rounds.value = res.data || []
    if (rounds.value.length > 0) {
      const stillExists = rounds.value.find(r => r.id === selectedRoundId.value)
      if (!stillExists) {
        selectedRoundId.value = rounds.value[0].id
        await loadAssessments()
      }
    } else {
      selectedRoundId.value = null
      assessments.value = []
    }
  } catch { rounds.value = [] }
}

async function loadAssessments() {
  if (!selectedRoundId.value) { assessments.value = []; return }
  loadingAssessments.value = true
  selectedIds.value = []
  try {
    const res = await riskApi.listAssessments(selectedRoundId.value)
    assessments.value = res.data || []
  } catch { assessments.value = [] } finally {
    loadingAssessments.value = false
  }
}

async function loadAssets() {
  try {
    const res = await assetApi.list({ active: true, size: 5000 })
    assets.value = res.data?.content || []
  } catch { assets.value = [] }
}

async function loadThreats() {
  try {
    const res = await threatApi.list()
    threats.value = res.data || res || []
  } catch { threats.value = [] }
}

onMounted(async () => {
  await Promise.all([loadYears(), loadAssets(), loadThreats()])
  await loadRounds()
})

watch(currentYear, loadRounds)

function onAssetChange() {
  const asset = assets.value.find(a => a.id === form.assetId)
  if (asset) {
    form.assetName = asset.name
    form.assetType = asset.type || ''
    form.assetEnvironment = asset.environment || ''
  }
}

function onThreatChange() {
  const threat = threats.value.find(t => t.id === form.threatId)
  if (threat) {
    form.threatName = threat.name
    form.threatType = threat.type || ''
    // 취약점에는 선택한 위협의 설명(description)을 채운다
    form.vulnerability = threat.description || ''
  }
}

// 차수 모달
function openRoundModal(round) {
  editRound.value = round
  roundError.value = ''
  roundSaving.value = false
  autoPopOnCreate.value = false
  if (round) {
    roundForm.year = round.year
    roundForm.roundNo = round.roundNo
    roundForm.roundDate = round.roundDate
    roundForm.title = round.title || ''
    roundForm.status = round.status
  } else {
    roundForm.year = currentYear.value
    roundForm.roundNo = rounds.value.length + 1
    roundForm.roundDate = new Date().toISOString().slice(0, 10)
    roundForm.title = ''
    roundForm.status = 'IN_PROGRESS'
  }
  showRoundModal.value = true
}

async function saveRound() {
  if (!roundForm.year || !roundForm.roundNo || !roundForm.roundDate) {
    roundError.value = '연도, 차수, 날짜는 필수입니다'
    return
  }
  roundSaving.value = true
  roundError.value = ''
  try {
    if (editRound.value) {
      await riskApi.updateRound(editRound.value.id, { ...roundForm })
    } else {
      const res = await riskApi.createRound({ ...roundForm })
      const newId = res.data?.id
      showRoundModal.value = false
      await loadRounds()
      await loadYears()
      if (newId) {
        selectedRoundId.value = newId
        await loadAssessments()
        if (autoPopOnCreate.value) {
          autoPopulating.value = true
          try {
            const popRes = await riskApi.autoPopulate(newId)
            await loadAssessments()
            alert(`자산 ${popRes.data}개가 추가되었습니다.`)
          } catch (e) {
            alert(e || '자산 불러오기에 실패했습니다.')
          } finally {
            autoPopulating.value = false
          }
        }
      }
      return
    }
    showRoundModal.value = false
    await loadRounds()
    await loadYears()
    await loadAssessments()
  } catch (e) {
    roundError.value = e || '저장에 실패했습니다'
  } finally {
    roundSaving.value = false
  }
}

async function completeRound() {
  if (!selectedRound.value) return
  try {
    await riskApi.updateRound(selectedRound.value.id, {
      year: selectedRound.value.year,
      roundNo: selectedRound.value.roundNo,
      roundDate: selectedRound.value.roundDate,
      title: selectedRound.value.title,
      status: 'COMPLETED'
    })
    await loadRounds()
  } catch (e) {
    alert(e || '상태 변경에 실패했습니다')
  }
}

async function downloadExcel() {
  if (!selectedRoundId.value || excelDownloading.value) return
  excelDownloading.value = true
  try {
    const blob = await riskApi.exportExcel(selectedRoundId.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const r = selectedRound.value
    a.href = url
    a.download = `위험평가_${r.year}년_${r.roundNo}차${r.title ? '_' + r.title : ''}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    alert('엑셀 다운로드에 실패했습니다.')
  } finally {
    excelDownloading.value = false
  }
}

function confirmAutoPopulate() { showAutoPopModal.value = true }

async function doAutoPopulate() {
  if (!selectedRoundId.value) return
  autoPopulating.value = true
  try {
    const res = await riskApi.autoPopulate(selectedRoundId.value)
    showAutoPopModal.value = false
    await loadAssessments()
    alert(`자산 ${res.data}개가 추가되었습니다.`)
  } catch (e) {
    alert(e || '자산 불러오기에 실패했습니다.')
  } finally {
    autoPopulating.value = false
  }
}

// 평가 모달
function openAssessmentModal(assessment) {
  editAssessment.value = assessment
  assessmentError.value = ''
  assessmentSaving.value = false
  if (assessment) {
    form.assetId = assessment.assetId || null
    form.assetName = assessment.assetName || ''
    form.assetType = assessment.assetType || ''
    form.assetEnvironment = assessment.assetEnvironment || ''
    form.threatId = assessment.threatId || null
    form.threatName = assessment.threatName || ''
    form.threatType = assessment.threatType || ''
    form.vulnerability = assessment.vulnerability || ''
    form.likelihood = assessment.likelihood
    form.impact = assessment.impact
    form.treatment = assessment.treatment || '감소'
    form.notes = assessment.notes || ''
  } else {
    form.assetId = null; form.assetName = ''; form.assetType = ''; form.assetEnvironment = ''
    form.threatId = null; form.threatName = ''; form.threatType = ''
    form.vulnerability = ''
    form.likelihood = 3; form.impact = 3
    form.treatment = '감소'; form.notes = ''
  }
  showAssessmentModal.value = true
}

async function saveAssessment() {
  if (!form.assetName || !form.threatName) return
  assessmentSaving.value = true
  assessmentError.value = ''
  try {
    const payload = {
      roundId: selectedRoundId.value,
      assetId: form.assetId || null,
      assetName: form.assetName,
      assetType: form.assetType || null,
      assetEnvironment: form.assetEnvironment || null,
      threatId: form.threatId || null,
      threatName: form.threatName,
      threatType: form.threatType || null,
      vulnerability: form.vulnerability || null,
      likelihood: form.likelihood,
      impact: form.impact,
      treatment: form.treatment,
      notes: form.notes || null
    }
    if (editAssessment.value) {
      await riskApi.updateAssessment(editAssessment.value.id, payload)
    } else {
      await riskApi.createAssessment(payload)
    }
    showAssessmentModal.value = false
    await loadAssessments()
  } catch (e) {
    assessmentError.value = e || '저장에 실패했습니다'
  } finally {
    assessmentSaving.value = false
  }
}

// 삭제
function confirmDeleteRound(round) {
  deleteTarget.value = {
    type: 'round',
    id: round.id,
    message: `${round.roundNo}차 평가를 삭제하시겠습니까?`,
    warning: round.itemCount > 0 ? `포함된 평가 항목 ${round.itemCount}개도 함께 삭제됩니다.` : ''
  }
  showDeleteModal.value = true
}

function confirmDeleteAssessment(item) {
  deleteTarget.value = {
    type: 'assessment',
    id: item.id,
    message: `"${item.assetName}" — "${item.threatName}" 평가를 삭제하시겠습니까?`,
    warning: ''
  }
  showDeleteModal.value = true
}

async function executeDelete() {
  deleting.value = true
  try {
    if (deleteTarget.value.type === 'round') {
      await riskApi.deleteRound(deleteTarget.value.id)
      showDeleteModal.value = false
      selectedRoundId.value = null
      await loadRounds()
      await loadYears()
    } else {
      await riskApi.deleteAssessment(deleteTarget.value.id)
      showDeleteModal.value = false
      await loadAssessments()
    }
  } catch (e) {
    alert(e || '삭제에 실패했습니다')
  } finally {
    deleting.value = false
  }
}

// 헬퍼 함수
function countByGrade(grade) {
  // 요약 카드는 차수 전체 기준 — 필터 조회에 영향받지 않는다
  return assessments.value.filter(a => a.riskGrade === grade).length
}

const treatmentCounts = computed(() => {
  // 처리방법별 현황도 차수 전체 기준
  const counts = { 감소: 0, 수용: 0, 회피: 0, 이전: 0 }
  for (const a of assessments.value) {
    if (a.treatment in counts) counts[a.treatment]++
  }
  return counts
})

function calcGrade(score) {
  if (score >= highThreshold) return 'HIGH'
  if (score >= medThreshold) return 'MEDIUM'
  return 'LOW'
}

function gradeBadge(grade) {
  return { HIGH: 'bg-red-100 text-red-700', MEDIUM: 'bg-yellow-100 text-yellow-700', LOW: 'bg-green-100 text-green-700' }[grade] || 'bg-gray-100 text-gray-700'
}

function gradeLabel(grade) {
  return { HIGH: '고위험', MEDIUM: '중위험', LOW: '저위험' }[grade] || grade
}

function scoreColor(score) {
  if (score >= highThreshold) return 'text-red-600'
  if (score >= medThreshold) return 'text-yellow-600'
  return 'text-green-600'
}
</script>
