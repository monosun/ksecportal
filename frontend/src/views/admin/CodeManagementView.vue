<template>
  <div>
    <!-- 페이지 헤더 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ $t('admin.codes') }}</h1>
        <p class="text-sm text-gray-400 mt-0.5">코드, 점검 기본항목, 위협 기본항목, 개인정보 항목을 관리합니다</p>
      </div>
    </div>

    <div class="page-body">

    <!-- 탭 -->
    <div class="flex gap-1 mb-6 border-b border-gray-200">
      <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
        class="px-4 py-2.5 text-sm font-medium transition-colors border-b-2 -mb-px"
        :class="activeTab === tab.key
          ? 'border-primary-500 text-primary-600'
          : 'border-transparent text-gray-500 hover:text-gray-700'">
        {{ tab.label }}
      </button>
    </div>

    <!-- ─── 탭 1: 코드 관리 ─── -->
    <div v-if="activeTab === 'code'">
      <div class="mb-5 flex justify-end">
        <button @click="openGroupModal()" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          {{ $t('admin.addCodeGroup') }}
        </button>
      </div>

      <div class="flex gap-6">
        <!-- 코드 그룹 목록 -->
        <div class="w-80 flex-shrink-0">
          <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-4 py-3 bg-gray-50 border-b border-gray-200">
              <h2 class="text-sm font-semibold text-gray-700">코드 그룹</h2>
            </div>
            <div v-if="loadingGroups" class="p-6 text-center text-gray-400 text-sm">로딩 중...</div>
            <div v-else-if="groups.length === 0" class="p-6 text-center text-gray-400 text-sm">코드 그룹이 없습니다</div>
            <ul v-else class="divide-y divide-gray-100">
              <li v-for="group in nonPiGroups" :key="group.groupCode"
                class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors"
                :class="selectedGroup?.groupCode === group.groupCode ? 'bg-primary-50 border-l-2 border-primary-500' : ''"
                @click="selectGroup(group)">
                <div class="min-w-0 flex-1">
                  <p class="text-sm font-medium text-gray-900 truncate">{{ group.groupName }}</p>
                  <p class="text-xs text-gray-400 font-mono">{{ group.groupCode }}</p>
                </div>
                <div class="flex items-center gap-2 ml-2 flex-shrink-0">
                  <span class="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">{{ group.valueCount }}</span>
                  <button @click.stop="openGroupModal(group)" class="text-gray-400 hover:text-primary-600 transition-colors">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                    </svg>
                  </button>
                  <button @click.stop="confirmDeleteGroup(group)" class="text-gray-400 hover:text-red-600 transition-colors">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                  </button>
                </div>
              </li>
            </ul>
          </div>
        </div>

        <!-- 코드값 목록 -->
        <div class="flex-1">
          <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-4 py-3 bg-gray-50 border-b border-gray-200 flex items-center justify-between">
              <h2 class="text-sm font-semibold text-gray-700">
                {{ selectedGroup ? `${selectedGroup.groupName} 코드값` : '그룹을 선택하세요' }}
              </h2>
              <button v-if="selectedGroup" @click="openValueModal()"
                class="btn-primary text-xs py-1.5 px-3 flex items-center gap-1">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
                {{ $t('admin.addCodeValue') }}
              </button>
            </div>
            <div v-if="!selectedGroup" class="p-12 text-center text-gray-400">
              <p class="text-sm">왼쪽에서 코드 그룹을 선택하세요</p>
            </div>
            <div v-else-if="loadingValues" class="p-8 text-center text-gray-400 text-sm">로딩 중...</div>
            <div v-else-if="values.length === 0" class="p-8 text-center text-gray-400 text-sm">
              등록된 코드값이 없습니다.
            </div>
            <table v-else class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">표시명</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">값</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">설명</th>
                  <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">정렬</th>
                  <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">상태</th>
                  <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">작업</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-100">
                <tr v-for="val in values" :key="val.id" class="hover:bg-gray-50">
                  <td class="px-4 py-3 text-sm font-medium text-gray-900 whitespace-nowrap">{{ val.label }}</td>
                  <td class="px-4 py-3 text-sm text-gray-500 font-mono whitespace-nowrap">{{ val.value }}</td>
                  <td class="px-4 py-3 text-xs text-gray-500">{{ val.description || '' }}</td>
                  <td class="px-4 py-3 text-sm text-center text-gray-400">{{ val.sortOrder }}</td>
                  <td class="px-4 py-3 text-center">
                    <span :class="val.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'"
                      class="text-xs px-2 py-0.5 rounded-full font-medium">
                      {{ val.active ? '사용' : '미사용' }}
                    </span>
                  </td>
                  <td class="px-4 py-3 text-right">
                    <div class="flex items-center justify-end gap-2">
                      <button @click="openValueModal(val)" class="text-xs text-primary-600 hover:text-primary-800 font-medium">수정</button>
                      <button @click="confirmDeleteValue(val)" class="text-xs text-red-500 hover:text-red-700 font-medium">삭제</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── 탭 2: 월간 점검 기본 항목 ─── -->
    <div v-else-if="activeTab === 'monthly'">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-gray-800">월간 보안점검 기본 항목 관리</h2>
        <button @click="openDefaultItemModal(null)" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          기본 항목 추가
        </button>
      </div>

      <!-- 검색 필터 -->
      <div class="mb-4 flex flex-wrap gap-3">
        <input v-model="searchMonthlyKeyword" type="text" placeholder="카테고리, 점검항목명, 점검방법 검색..."
          class="input flex-1 min-w-48 text-sm"/>
        <select v-model="searchMonthlyPriority" class="input w-36 text-sm">
          <option value="">전체 우선순위</option>
          <option value="HIGH">높음</option>
          <option value="MEDIUM">중간</option>
          <option value="LOW">낮음</option>
        </select>
        <button v-if="searchMonthlyKeyword || searchMonthlyPriority" @click="searchMonthlyKeyword=''; searchMonthlyPriority=''"
          class="btn-secondary text-sm px-3">초기화</button>
        <span class="flex items-center text-sm text-gray-500">
          {{ filteredMonthlyDefaults.length }}건
        </span>
      </div>

      <div class="card p-0 overflow-hidden">
        <div v-if="loadingMonthlyDefaults" class="p-8 text-center text-gray-400 text-sm">로딩 중...</div>
        <div v-else-if="filteredMonthlyDefaults.length === 0" class="p-8 text-center text-gray-400 text-sm">
          {{ monthlyDefaults.length === 0 ? '등록된 기본 항목이 없습니다.' : '검색 결과가 없습니다.' }}
        </div>
        <table v-else class="w-full text-sm">
          <thead class="bg-gray-50 border-b border-gray-100">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">우선순위</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">카테고리</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">점검항목명</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">점검방법</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">정렬</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">활성</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">관리</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="d in pagedMonthlyDefaults" :key="d.id" class="hover:bg-gray-50">
              <td class="px-4 py-3">
                <span :class="priorityBadge(d.priority)" class="text-xs px-2 py-0.5 rounded-full font-semibold">{{ priorityLabel(d.priority) }}</span>
              </td>
              <td class="px-4 py-3 text-gray-700 text-sm">{{ d.category }}</td>
              <td class="px-4 py-3 font-medium text-gray-900">{{ d.itemName }}</td>
              <td class="px-4 py-3 text-gray-500 text-xs max-w-xs truncate">{{ d.checkMethod || '-' }}</td>
              <td class="px-4 py-3 text-center text-gray-400 text-sm">{{ d.sortOrder }}</td>
              <td class="px-4 py-3 text-center">
                <span :class="d.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'"
                  class="text-xs px-2 py-0.5 rounded-full font-medium">
                  {{ d.active ? '사용' : '미사용' }}
                </span>
              </td>
              <td class="px-4 py-3 text-right">
                <div class="flex items-center justify-end gap-2">
                  <button @click="openDefaultItemModal(d)" class="text-xs text-primary-600 hover:text-primary-800 font-medium">수정</button>
                  <button @click="confirmDeleteMonthlyDefault(d)" class="text-xs text-red-500 hover:text-red-700 font-medium">삭제</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="monthlyTotalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
          <span class="text-xs text-gray-400">{{ filteredMonthlyDefaults.length }}건 중 {{ monthlyPage * MONTHLY_PAGE_SIZE + 1 }}–{{ Math.min((monthlyPage + 1) * MONTHLY_PAGE_SIZE, filteredMonthlyDefaults.length) }}건</span>
          <div class="flex items-center gap-1">
            <button @click="monthlyPage--" :disabled="monthlyPage === 0"
              class="px-2 py-1 text-sm rounded-lg border border-gray-200 disabled:opacity-30 hover:bg-gray-50">←</button>
            <template v-if="monthlyTotalPages <= 10">
              <button v-for="n in monthlyTotalPages" :key="n" @click="monthlyPage = n - 1"
                class="px-2.5 py-1 text-sm rounded-lg"
                :class="monthlyPage === n - 1 ? 'bg-primary-600 text-white' : 'border border-gray-200 text-gray-600 hover:bg-gray-50'">{{ n }}</button>
            </template>
            <span v-else class="text-sm text-gray-600 px-2">{{ monthlyPage + 1 }} / {{ monthlyTotalPages }}</span>
            <button @click="monthlyPage++" :disabled="monthlyPage >= monthlyTotalPages - 1"
              class="px-2 py-1 text-sm rounded-lg border border-gray-200 disabled:opacity-30 hover:bg-gray-50">→</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── 탭 3: 위협 기본 항목 ─── -->
    <div v-else-if="activeTab === 'threat'">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-gray-800">위협 기본 항목 관리</h2>
        <button @click="openThreatDefaultModal(null)" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          기본 항목 추가
        </button>
      </div>

      <!-- 검색 필터 -->
      <div class="mb-4 flex flex-wrap gap-3">
        <input v-model="searchThreatKeyword" type="text" placeholder="위협명, Risk ID, 카테고리 검색..."
          class="input flex-1 min-w-48 text-sm"/>
        <select v-model="searchThreatTypeFilter" class="input w-44 text-sm">
          <option value="">전체 유형</option>
          <option v-for="t in allThreatTypeOptions" :key="t" :value="t">{{ t }}</option>
        </select>
        <button v-if="searchThreatKeyword || searchThreatTypeFilter" @click="searchThreatKeyword=''; searchThreatTypeFilter=''"
          class="btn-secondary text-sm px-3">초기화</button>
        <span class="flex items-center text-sm text-gray-500">
          {{ filteredThreatDefaults.length }}건
        </span>
      </div>

      <div class="card p-0 overflow-hidden">
        <div v-if="loadingAllThreats || loadingThreatDefaults" class="p-8 text-center text-gray-400 text-sm">로딩 중...</div>
        <div v-else-if="filteredThreatDefaults.length === 0" class="p-8 text-center text-gray-400 text-sm">
          {{ allThreatDefaults.length === 0 ? '등록된 위협 기본 항목이 없습니다.' : '검색 결과가 없습니다.' }}
        </div>
        <table v-else class="w-full text-sm">
          <thead class="bg-gray-50 border-b border-gray-100">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Risk ID</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">위협명</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">유형</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">카테고리</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">발생가능성</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">잠재영향</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">관리</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="d in pagedThreatDefaults" :key="d.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 text-gray-400 font-mono text-xs">{{ d.riskId || '-' }}</td>
              <td class="px-4 py-3 font-medium text-gray-900">{{ d.name }}</td>
              <td class="px-4 py-3 text-gray-600 text-xs">{{ d.type || '-' }}</td>
              <td class="px-4 py-3 text-gray-500 text-xs">{{ d.category || '-' }}</td>
              <td class="px-4 py-3 text-center text-gray-700 font-semibold">{{ d.likelihood }}</td>
              <td class="px-4 py-3 text-center text-gray-700 font-semibold">{{ d.impact }}</td>
              <td class="px-4 py-3 text-right">
                <div class="flex items-center justify-end gap-2">
                  <button @click="openThreatDefaultModal(d)" class="text-xs text-primary-600 hover:text-primary-800 font-medium">수정</button>
                  <button @click="confirmDeleteThreatDefault(d)" class="text-xs text-red-500 hover:text-red-700 font-medium">삭제</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="threatTotalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
          <span class="text-xs text-gray-400">{{ filteredThreatDefaults.length }}건 중 {{ threatPage * THREAT_PAGE_SIZE + 1 }}–{{ Math.min((threatPage + 1) * THREAT_PAGE_SIZE, filteredThreatDefaults.length) }}건</span>
          <div class="flex items-center gap-1">
            <button @click="threatPage--" :disabled="threatPage === 0"
              class="px-2 py-1 text-sm rounded-lg border border-gray-200 disabled:opacity-30 hover:bg-gray-50">←</button>
            <template v-if="threatTotalPages <= 10">
              <button v-for="n in threatTotalPages" :key="n" @click="threatPage = n - 1"
                class="px-2.5 py-1 text-sm rounded-lg"
                :class="threatPage === n - 1 ? 'bg-primary-600 text-white' : 'border border-gray-200 text-gray-600 hover:bg-gray-50'">{{ n }}</button>
            </template>
            <span v-else class="text-sm text-gray-600 px-2">{{ threatPage + 1 }} / {{ threatTotalPages }}</span>
            <button @click="threatPage++" :disabled="threatPage >= threatTotalPages - 1"
              class="px-2 py-1 text-sm rounded-lg border border-gray-200 disabled:opacity-30 hover:bg-gray-50">→</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── 탭 4: 위협 유형별 관리 ─── -->
    <div v-else-if="activeTab === 'threatByType'">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-gray-800">위협 유형별 위협명 관리</h2>
        <button v-if="selectedThreatType" @click="openThreatByTypeModal(null)" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          위협명 추가
        </button>
      </div>

      <div v-if="loadingAllThreats" class="p-8 text-center text-gray-400 text-sm">로딩 중...</div>
      <div v-else class="flex gap-6">
        <!-- 위협 유형 목록 -->
        <div class="w-60 flex-shrink-0">
          <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-4 py-3 bg-gray-50 border-b border-gray-200">
              <h3 class="text-sm font-semibold text-gray-700">위협 유형</h3>
            </div>
            <div v-if="threatTypeList.length === 0" class="p-6 text-center text-gray-400 text-sm">
              위협 기본 항목 탭에서 위협을 먼저 추가하세요.
            </div>
            <ul v-else class="divide-y divide-gray-100">
              <li v-for="type in threatTypeList" :key="type"
                class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors"
                :class="selectedThreatType === type ? 'bg-primary-50 border-l-2 border-primary-500' : ''"
                @click="selectedThreatType = type">
                <div>
                  <p class="text-sm font-medium text-gray-900">{{ type }}</p>
                  <p class="text-xs text-gray-400">
                    {{ allThreatDefaults.filter(t => (t.type || '기타') === type).length }}개
                  </p>
                </div>
                <svg class="w-4 h-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </li>
            </ul>
          </div>
        </div>

        <!-- 선택된 유형의 위협명 목록 -->
        <div class="flex-1">
          <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-4 py-3 bg-gray-50 border-b border-gray-200">
              <h3 class="text-sm font-semibold text-gray-700">
                {{ selectedThreatType ? `${selectedThreatType} 위협명 목록` : '위협 유형을 선택하세요' }}
              </h3>
            </div>
            <div v-if="!selectedThreatType" class="p-12 text-center text-gray-400">
              <p class="text-sm">왼쪽에서 위협 유형을 선택하세요</p>
            </div>
            <div v-else-if="threatsBySelectedType.length === 0" class="p-8 text-center text-gray-400 text-sm">
              이 유형의 위협명이 없습니다.
            </div>
            <table v-else class="w-full text-sm">
              <thead class="bg-gray-50 border-b border-gray-100">
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Risk ID</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">위협명</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">카테고리</th>
                  <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">L</th>
                  <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">I</th>
                  <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">점수</th>
                  <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">관리</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="d in pagedThreatsByType" :key="d.id" class="hover:bg-gray-50">
                  <td class="px-4 py-3 text-gray-400 font-mono text-xs">{{ d.riskId || '-' }}</td>
                  <td class="px-4 py-3 font-medium text-gray-900">{{ d.name }}</td>
                  <td class="px-4 py-3 text-gray-500 text-xs">{{ d.category || '-' }}</td>
                  <td class="px-4 py-3 text-center text-gray-700 text-xs font-semibold">{{ d.likelihood }}</td>
                  <td class="px-4 py-3 text-center text-gray-700 text-xs font-semibold">{{ d.impact }}</td>
                  <td class="px-4 py-3 text-center">
                    <span class="text-xs font-bold"
                      :class="(d.likelihood*d.impact)>=15?'text-red-600':(d.likelihood*d.impact)>=8?'text-yellow-600':'text-green-600'">
                      {{ d.likelihood * d.impact }}
                    </span>
                  </td>
                  <td class="px-4 py-3 text-right">
                    <div class="flex items-center justify-end gap-2">
                      <button @click="openThreatByTypeModal(d)" class="text-xs text-primary-600 hover:text-primary-800 font-medium">수정</button>
                      <button @click="confirmDeleteThreatDefault(d)"
                        class="text-xs text-red-500 hover:text-red-700 font-medium">삭제</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="threatByTypeTotalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
              <span class="text-xs text-gray-400">{{ threatsBySelectedType.length }}건 중 {{ threatByTypePage * THREAT_BY_TYPE_PAGE_SIZE + 1 }}–{{ Math.min((threatByTypePage + 1) * THREAT_BY_TYPE_PAGE_SIZE, threatsBySelectedType.length) }}건</span>
              <div class="flex items-center gap-1">
                <button @click="threatByTypePage--" :disabled="threatByTypePage === 0"
                  class="px-2 py-1 text-sm rounded-lg border border-gray-200 disabled:opacity-30 hover:bg-gray-50">←</button>
                <template v-if="threatByTypeTotalPages <= 10">
                  <button v-for="n in threatByTypeTotalPages" :key="n" @click="threatByTypePage = n - 1"
                    class="px-2.5 py-1 text-sm rounded-lg"
                    :class="threatByTypePage === n - 1 ? 'bg-primary-600 text-white' : 'border border-gray-200 text-gray-600 hover:bg-gray-50'">{{ n }}</button>
                </template>
                <span v-else class="text-sm text-gray-600 px-2">{{ threatByTypePage + 1 }} / {{ threatByTypeTotalPages }}</span>
                <button @click="threatByTypePage++" :disabled="threatByTypePage >= threatByTypeTotalPages - 1"
                  class="px-2 py-1 text-sm rounded-lg border border-gray-200 disabled:opacity-30 hover:bg-gray-50">→</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── 탭 5: 개인정보 유형별 관리 ─── -->
    <div v-else-if="activeTab === 'piByType'">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-gray-800">개인정보 유형별 항목 관리</h2>
        <button v-if="selectedPiGroup" @click="openValueModal()" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          항목 추가
        </button>
      </div>

      <div v-if="loadingGroups" class="p-8 text-center text-gray-400 text-sm">로딩 중...</div>
      <div v-else class="flex gap-6">
        <!-- 개인정보 유형 목록 -->
        <div class="w-64 flex-shrink-0">
          <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-4 py-3 bg-gray-50 border-b border-gray-200">
              <h3 class="text-sm font-semibold text-gray-700">개인정보 유형</h3>
            </div>
            <div v-if="piGroups.length === 0" class="p-6 text-center text-gray-400 text-sm">
              개인정보 유형 코드 그룹이 없습니다.
            </div>
            <ul v-else class="divide-y divide-gray-100">
              <li v-for="group in piGroups" :key="group.groupCode"
                class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors"
                :class="selectedPiGroup?.groupCode === group.groupCode ? 'bg-primary-50 border-l-2 border-primary-500' : ''"
                @click="selectPiGroup(group)">
                <div>
                  <p class="text-sm font-medium text-gray-900">{{ group.groupName }}</p>
                  <p class="text-xs text-gray-400">{{ group.valueCount }}개 항목</p>
                </div>
                <svg class="w-4 h-4 text-gray-300 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </li>
            </ul>
          </div>
        </div>

        <!-- 선택된 유형의 개인정보 항목 목록 -->
        <div class="flex-1">
          <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-4 py-3 bg-gray-50 border-b border-gray-200">
              <h3 class="text-sm font-semibold text-gray-700">
                {{ selectedPiGroup ? `${selectedPiGroup.groupName} 항목 목록` : '유형을 선택하세요' }}
              </h3>
            </div>
            <div v-if="!selectedPiGroup" class="p-12 text-center text-gray-400">
              <p class="text-sm">왼쪽에서 개인정보 유형을 선택하세요</p>
            </div>
            <div v-else-if="loadingValues" class="p-8 text-center text-gray-400 text-sm">로딩 중...</div>
            <div v-else-if="values.length === 0" class="p-8 text-center text-gray-400 text-sm">
              등록된 항목이 없습니다.
            </div>
            <table v-else class="w-full text-sm">
              <thead class="bg-gray-50 border-b border-gray-100">
                <tr>
                  <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase w-16">순서</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase w-44">항목명</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">설명</th>
                  <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase w-20">상태</th>
                  <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase w-24">관리</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="val in values" :key="val.id" class="hover:bg-gray-50">
                  <td class="px-4 py-3 text-center text-gray-400 text-sm">{{ val.sortOrder }}</td>
                  <td class="px-4 py-3 font-medium text-gray-900">{{ val.label }}</td>
                  <td class="px-4 py-3 text-gray-500 text-xs">{{ val.description || '' }}</td>
                  <td class="px-4 py-3 text-center">
                    <span :class="val.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'"
                      class="text-xs px-2 py-0.5 rounded-full font-medium">
                      {{ val.active ? '사용' : '미사용' }}
                    </span>
                  </td>
                  <td class="px-4 py-3 text-right">
                    <div class="flex items-center justify-end gap-2">
                      <button @click="openValueModal(val)" class="text-xs text-primary-600 hover:text-primary-800 font-medium">수정</button>
                      <button @click="confirmDeleteValue(val)" class="text-xs text-red-500 hover:text-red-700 font-medium">삭제</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── 탭 6: 수탁사 기본점검항목 ─── -->
    <div v-else-if="activeTab === 'contractorDefault'">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-gray-800">수탁사 점검 기본항목 관리</h2>
        <button @click="openContractorDefaultModal(null)" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          기본 항목 추가
        </button>
      </div>

      <div class="mb-4 flex flex-wrap gap-3">
        <input v-model="searchContractorKeyword" type="text" placeholder="분야, 항목명, 점검방법 검색..."
          class="input flex-1 min-w-48 text-sm"/>
        <button v-if="searchContractorKeyword" @click="searchContractorKeyword = ''"
          class="btn-secondary text-sm px-3">초기화</button>
        <span class="flex items-center text-sm text-gray-500">{{ filteredContractorDefaults.length }}건</span>
      </div>

      <div class="card p-0 overflow-hidden">
        <div v-if="loadingContractorDefaults" class="p-8 text-center text-gray-400 text-sm">로딩 중...</div>
        <div v-else-if="filteredContractorDefaults.length === 0" class="p-8 text-center text-gray-400 text-sm">
          {{ contractorDefaults.length === 0 ? '등록된 기본 항목이 없습니다.' : '검색 결과가 없습니다.' }}
        </div>
        <table v-else class="w-full text-sm">
          <thead class="bg-gray-50 border-b border-gray-100">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">점검 분야</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">점검항목명</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">점검방법</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">점검기준</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">정렬</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">관리</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="d in pagedContractorDefaults" :key="d.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 whitespace-nowrap">
                <span class="px-2 py-0.5 rounded-full text-xs font-semibold" :class="contractorCategoryClass(d.category)">
                  {{ d.category }}
                </span>
              </td>
              <td class="px-4 py-3 font-medium text-gray-900">{{ d.itemName }}</td>
              <td class="px-4 py-3 text-gray-500 text-xs max-w-xs truncate" :title="d.checkMethod">{{ d.checkMethod || '-' }}</td>
              <td class="px-4 py-3 text-gray-500 text-xs max-w-xs truncate" :title="d.checkStandard">{{ d.checkStandard || '-' }}</td>
              <td class="px-4 py-3 text-center text-gray-400 text-sm">{{ d.sortOrder }}</td>
              <td class="px-4 py-3 text-right">
                <div class="flex items-center justify-end gap-2">
                  <button @click="openContractorDefaultModal(d)" class="text-xs text-primary-600 hover:text-primary-800 font-medium">수정</button>
                  <button @click="confirmDeleteContractorDefault(d)" class="text-xs text-red-500 hover:text-red-700 font-medium">삭제</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="contractorDefaultTotalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
          <span class="text-xs text-gray-400">{{ filteredContractorDefaults.length }}건 중 {{ contractorDefaultPage * CONTRACTOR_DEFAULT_PAGE_SIZE + 1 }}–{{ Math.min((contractorDefaultPage + 1) * CONTRACTOR_DEFAULT_PAGE_SIZE, filteredContractorDefaults.length) }}건</span>
          <div class="flex items-center gap-1">
            <button @click="contractorDefaultPage--" :disabled="contractorDefaultPage === 0"
              class="px-2 py-1 text-sm rounded-lg border border-gray-200 disabled:opacity-30 hover:bg-gray-50">←</button>
            <template v-if="contractorDefaultTotalPages <= 10">
              <button v-for="n in contractorDefaultTotalPages" :key="n" @click="contractorDefaultPage = n - 1"
                class="px-2.5 py-1 text-sm rounded-lg"
                :class="contractorDefaultPage === n - 1 ? 'bg-primary-600 text-white' : 'border border-gray-200 text-gray-600 hover:bg-gray-50'">{{ n }}</button>
            </template>
            <span v-else class="text-sm text-gray-600 px-2">{{ contractorDefaultPage + 1 }} / {{ contractorDefaultTotalPages }}</span>
            <button @click="contractorDefaultPage++" :disabled="contractorDefaultPage >= contractorDefaultTotalPages - 1"
              class="px-2 py-1 text-sm rounded-lg border border-gray-200 disabled:opacity-30 hover:bg-gray-50">→</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── 수탁사 기본점검항목 모달 ─── -->
    <div v-if="contractorDefaultModal.show" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="contractorDefaultModal.show = false"/>
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-5">
          {{ contractorDefaultModal.editing ? '기본 항목 수정' : '기본 항목 추가' }}
        </h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검 분야 <span class="text-red-500">*</span></label>
            <input v-model="contractorDefaultModal.form.category" class="input w-full" placeholder="예: 계약 관리, 기술적 보호조치"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검항목명 <span class="text-red-500">*</span></label>
            <input v-model="contractorDefaultModal.form.itemName" class="input w-full" placeholder="점검항목명을 입력하세요"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검방법</label>
            <textarea v-model="contractorDefaultModal.form.checkMethod" class="input w-full" rows="2" placeholder="점검방법을 입력하세요 (선택)"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검기준</label>
            <textarea v-model="contractorDefaultModal.form.checkStandard" class="input w-full" rows="2" placeholder="관련 법령 또는 기준 (선택)"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">정렬 순서</label>
            <input v-model.number="contractorDefaultModal.form.sortOrder" type="number" min="0" class="input w-28"/>
          </div>
        </div>
        <p v-if="contractorDefaultModal.error" class="mt-3 text-sm text-red-600">{{ contractorDefaultModal.error }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="contractorDefaultModal.show = false" class="btn-secondary">취소</button>
          <button @click="saveContractorDefault" :disabled="contractorDefaultModal.saving" class="btn-primary">
            {{ contractorDefaultModal.saving ? '저장 중...' : '저장' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ─── 코드 그룹 모달 ─── -->
    <div v-if="groupModal.show" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="groupModal.show = false"/>
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-5">
          {{ groupModal.editing ? '코드 그룹 수정' : '코드 그룹 추가' }}
        </h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">그룹 코드 <span class="text-red-500">*</span></label>
            <input v-model="groupModal.form.groupCode" :disabled="groupModal.editing" type="text"
              placeholder="예: DEPARTMENT" class="input uppercase"
              :class="groupModal.editing ? 'bg-gray-50 text-gray-500' : ''"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">그룹명 <span class="text-red-500">*</span></label>
            <input v-model="groupModal.form.groupName" type="text" placeholder="예: 부서" class="input"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <input v-model="groupModal.form.description" type="text" class="input"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">정렬 순서</label>
            <input v-model.number="groupModal.form.sortOrder" type="number" min="0" class="input w-28"/>
          </div>
        </div>
        <p v-if="groupModal.error" class="mt-3 text-sm text-red-600">{{ groupModal.error }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="groupModal.show = false" class="btn-secondary">취소</button>
          <button @click="saveGroup" :disabled="groupModal.saving" class="btn-primary">
            {{ groupModal.saving ? '저장 중...' : '저장' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ─── 코드값 모달 ─── -->
    <div v-if="valueModal.show" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="valueModal.show = false"/>
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-5">
          {{ valueModal.editing ? '코드값 수정' : '코드값 추가' }}
        </h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">표시명 <span class="text-red-500">*</span></label>
            <input v-model="valueModal.form.label" type="text" placeholder="예: 보안팀" class="input"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">값 <span class="text-red-500">*</span></label>
            <input v-model="valueModal.form.value" type="text" placeholder="예: 보안팀" class="input"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <input v-model="valueModal.form.description" type="text" placeholder="항목에 대한 설명 (선택)" class="input"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">정렬 순서</label>
            <input v-model.number="valueModal.form.sortOrder" type="number" min="0" class="input w-28"/>
          </div>
          <div class="flex items-center gap-2">
            <input id="active-toggle" v-model="valueModal.form.active" type="checkbox" class="w-4 h-4 text-primary-600 rounded border-gray-300"/>
            <label for="active-toggle" class="text-sm text-gray-700">사용</label>
          </div>
        </div>
        <p v-if="valueModal.error" class="mt-3 text-sm text-red-600">{{ valueModal.error }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="valueModal.show = false" class="btn-secondary">취소</button>
          <button @click="saveValue" :disabled="valueModal.saving" class="btn-primary">
            {{ valueModal.saving ? '저장 중...' : '저장' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ─── 월간 기본항목 모달 ─── -->
    <div v-if="defaultItemModal.show" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="defaultItemModal.show = false"/>
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-5">
          {{ defaultItemModal.editing ? '기본 항목 수정' : '기본 항목 추가' }}
        </h3>
        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">우선순위 <span class="text-red-500">*</span></label>
              <select v-model="defaultItemModal.form.priority" class="input w-full">
                <option value="HIGH">HIGH (높음)</option>
                <option value="MEDIUM">MEDIUM (중간)</option>
                <option value="LOW">LOW (낮음)</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">카테고리 <span class="text-red-500">*</span></label>
              <input v-model="defaultItemModal.form.category" class="input w-full" placeholder="예: 계정관리"/>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검항목명 <span class="text-red-500">*</span></label>
            <input v-model="defaultItemModal.form.itemName" class="input w-full" placeholder="점검항목명을 입력하세요"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검방법</label>
            <input v-model="defaultItemModal.form.checkMethod" class="input w-full" placeholder="점검 방법 설명"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검예시</label>
            <input v-model="defaultItemModal.form.checkExample" class="input w-full" placeholder="점검 예시"/>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">정렬 순서</label>
              <input v-model.number="defaultItemModal.form.sortOrder" type="number" min="0" class="input w-full"/>
            </div>
            <div class="flex items-center pt-6">
              <input id="di-active" v-model="defaultItemModal.form.active" type="checkbox" class="w-4 h-4 text-primary-600 rounded border-gray-300 mr-2"/>
              <label for="di-active" class="text-sm text-gray-700">활성</label>
            </div>
          </div>
        </div>
        <p v-if="defaultItemModal.error" class="mt-3 text-sm text-red-600">{{ defaultItemModal.error }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="defaultItemModal.show = false" class="btn-secondary">취소</button>
          <button @click="saveDefaultItem" :disabled="defaultItemModal.saving" class="btn-primary">
            {{ defaultItemModal.saving ? '저장 중...' : '저장' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ─── 위협 기본항목 모달 ─── -->
    <div v-if="threatDefaultModal.show" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="threatDefaultModal.show = false"/>
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-5">
          {{ threatDefaultModal.editing ? '위협 기본항목 수정' : '위협 기본항목 추가' }}
        </h3>
        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Risk ID</label>
              <input v-model="threatDefaultModal.form.riskId" class="input w-full" placeholder="예: R-001"/>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">위협 유형</label>
              <input v-model="threatDefaultModal.form.type" class="input w-full" placeholder="예: 외부공격"/>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">위협명 <span class="text-red-500">*</span></label>
            <input v-model="threatDefaultModal.form.name" class="input w-full" placeholder="위협명을 입력하세요"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">카테고리</label>
            <input v-model="threatDefaultModal.form.category" class="input w-full" placeholder="예: IAM, WEB/API"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">자산 상세</label>
            <input v-model="threatDefaultModal.form.assetDetail" class="input w-full"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea v-model="threatDefaultModal.form.description" class="input w-full" rows="3"></textarea>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">발생가능성 ({{ threatDefaultModal.form.likelihood }})</label>
              <div class="flex gap-1">
                <button v-for="n in 5" :key="n" @click="threatDefaultModal.form.likelihood = n"
                  class="flex-1 py-1 rounded text-xs font-bold border"
                  :class="threatDefaultModal.form.likelihood === n ? 'bg-blue-600 text-white border-blue-600' : 'border-gray-200 text-gray-500'">
                  {{ n }}
                </button>
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">잠재영향 ({{ threatDefaultModal.form.impact }})</label>
              <div class="flex gap-1">
                <button v-for="n in 5" :key="n" @click="threatDefaultModal.form.impact = n"
                  class="flex-1 py-1 rounded text-xs font-bold border"
                  :class="threatDefaultModal.form.impact === n ? 'bg-orange-500 text-white border-orange-500' : 'border-gray-200 text-gray-500'">
                  {{ n }}
                </button>
              </div>
            </div>
          </div>
        </div>
        <p v-if="threatDefaultModal.error" class="mt-3 text-sm text-red-600">{{ threatDefaultModal.error }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="threatDefaultModal.show = false" class="btn-secondary">취소</button>
          <button @click="saveThreatDefault" :disabled="threatDefaultModal.saving" class="btn-primary">
            {{ threatDefaultModal.saving ? '저장 중...' : '저장' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ─── 공통 삭제 확인 모달 ─── -->
    <div v-if="deleteConfirm.show" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="deleteConfirm.show = false"/>
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-sm mx-4 p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-2">삭제 확인</h3>
        <p class="text-sm text-gray-600">{{ deleteConfirm.message }}</p>
        <p v-if="deleteConfirm.warning" class="text-sm text-orange-600 mt-2 bg-orange-50 px-3 py-2 rounded-lg">
          {{ deleteConfirm.warning }}
        </p>
        <div class="flex justify-end gap-3 mt-5">
          <button @click="deleteConfirm.show = false" class="btn-secondary">취소</button>
          <button @click="deleteConfirm.action"
            class="bg-red-600 hover:bg-red-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
            삭제
          </button>
        </div>
      </div>
    </div>
    </div><!-- /page-body -->
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { adminApi, monthlyCheckDefaultApi, threatDefaultApi, contractorCheckItemApi } from '@/api'

const tabs = [
  { key: 'code', label: '코드 관리' },
  { key: 'monthly', label: '월간 점검 기본 항목' },
  { key: 'threat', label: '위협 기본 항목' },
  { key: 'threatByType', label: '위협 유형별 관리' },
  { key: 'piByType', label: '개인정보 유형별 관리' },
  { key: 'contractorDefault', label: '수탁사 기본점검항목' },
]
const activeTab = ref('code')

// ─── 코드 관리 ───
const groups = ref([])
const values = ref([])
const selectedGroup = ref(null)
const loadingGroups = ref(false)
const loadingValues = ref(false)

const nonPiGroups = computed(() => groups.value.filter(g => !g.groupCode.startsWith('PI_') || g.groupCode === 'PI_TYPE'))

const groupModal = reactive({
  show: false, editing: false, saving: false, error: '',
  form: { groupCode: '', groupName: '', description: '', sortOrder: 0 }
})

const valueModal = reactive({
  show: false, editing: false, saving: false, error: '',
  editId: null,
  form: { value: '', label: '', description: '', sortOrder: 0, active: true }
})

const deleteConfirm = reactive({ show: false, message: '', warning: '', action: null })

async function loadGroups() {
  loadingGroups.value = true
  try {
    const res = await adminApi.listCodeGroups()
    groups.value = res.data
  } finally {
    loadingGroups.value = false
  }
}

async function selectGroup(group) {
  selectedGroup.value = group
  loadingValues.value = true
  try {
    const res = await adminApi.listCodeValues(group.groupCode)
    values.value = res.data
  } finally {
    loadingValues.value = false
  }
}

function openGroupModal(group = null) {
  groupModal.editing = !!group
  groupModal.error = ''
  groupModal.saving = false
  if (group) {
    groupModal.form = { groupCode: group.groupCode, groupName: group.groupName, description: group.description || '', sortOrder: group.sortOrder }
  } else {
    groupModal.form = { groupCode: '', groupName: '', description: '', sortOrder: 0 }
  }
  groupModal.show = true
}

async function saveGroup() {
  if (!groupModal.form.groupCode || !groupModal.form.groupName) {
    groupModal.error = '그룹 코드와 그룹명은 필수입니다'
    return
  }
  groupModal.saving = true
  groupModal.error = ''
  try {
    const payload = { ...groupModal.form, groupCode: groupModal.form.groupCode.toUpperCase() }
    if (groupModal.editing) {
      await adminApi.updateCodeGroup(groupModal.form.groupCode, payload)
    } else {
      await adminApi.createCodeGroup(payload)
    }
    groupModal.show = false
    await loadGroups()
    if (selectedGroup.value && selectedGroup.value.groupCode === groupModal.form.groupCode) {
      selectedGroup.value = groups.value.find(g => g.groupCode === groupModal.form.groupCode)
    }
  } catch (e) {
    groupModal.error = e || '저장에 실패했습니다'
  } finally {
    groupModal.saving = false
  }
}

function confirmDeleteGroup(group) {
  deleteConfirm.message = `'${group.groupName}' 코드 그룹을 삭제하시겠습니까?`
  deleteConfirm.warning = group.valueCount > 0 ? `포함된 코드값 ${group.valueCount}개도 함께 삭제됩니다.` : ''
  deleteConfirm.action = async () => {
    deleteConfirm.show = false
    await adminApi.deleteCodeGroup(group.groupCode)
    if (selectedGroup.value?.groupCode === group.groupCode) {
      selectedGroup.value = null
      values.value = []
    }
    await loadGroups()
  }
  deleteConfirm.show = true
}

function openValueModal(val = null) {
  valueModal.editing = !!val
  valueModal.editId = val?.id || null
  valueModal.error = ''
  valueModal.saving = false
  if (val) {
    valueModal.form = { value: val.value, label: val.label, description: val.description || '', sortOrder: val.sortOrder, active: val.active }
  } else {
    valueModal.form = { value: '', label: '', description: '', sortOrder: values.value.length + 1, active: true }
  }
  valueModal.show = true
}

async function saveValue() {
  if (!valueModal.form.label || !valueModal.form.value) {
    valueModal.error = '표시명과 값은 필수입니다'
    return
  }
  valueModal.saving = true
  valueModal.error = ''
  try {
    if (valueModal.editing) {
      await adminApi.updateCodeValue(selectedGroup.value.groupCode, valueModal.editId, valueModal.form)
    } else {
      await adminApi.createCodeValue(selectedGroup.value.groupCode, valueModal.form)
    }
    valueModal.show = false
    await selectGroup(selectedGroup.value)
    await loadGroups()
  } catch (e) {
    valueModal.error = e || '저장에 실패했습니다'
  } finally {
    valueModal.saving = false
  }
}

function confirmDeleteValue(val) {
  deleteConfirm.message = `'${val.label}' 코드값을 삭제하시겠습니까?`
  deleteConfirm.warning = ''
  deleteConfirm.action = async () => {
    deleteConfirm.show = false
    await adminApi.deleteCodeValue(selectedGroup.value.groupCode, val.id)
    await selectGroup(selectedGroup.value)
    await loadGroups()
  }
  deleteConfirm.show = true
}

// ─── 월간 기본항목 ───
const monthlyDefaults = ref([])
const loadingMonthlyDefaults = ref(false)

const defaultItemModal = reactive({
  show: false, editing: false, saving: false, error: '',
  editId: null,
  form: { priority: 'HIGH', category: '', itemName: '', checkMethod: '', checkExample: '', sortOrder: 0, active: true }
})

async function loadMonthlyDefaults() {
  loadingMonthlyDefaults.value = true
  try {
    const res = await monthlyCheckDefaultApi.list()
    monthlyDefaults.value = res.data || []
  } catch {
    monthlyDefaults.value = []
  } finally {
    loadingMonthlyDefaults.value = false
  }
}

function openDefaultItemModal(d = null) {
  defaultItemModal.editing = !!d
  defaultItemModal.editId = d?.id || null
  defaultItemModal.error = ''
  defaultItemModal.saving = false
  if (d) {
    defaultItemModal.form = { priority: d.priority, category: d.category, itemName: d.itemName,
      checkMethod: d.checkMethod || '', checkExample: d.checkExample || '', sortOrder: d.sortOrder, active: d.active }
  } else {
    defaultItemModal.form = { priority: 'HIGH', category: '', itemName: '', checkMethod: '', checkExample: '',
      sortOrder: monthlyDefaults.value.length > 0 ? Math.max(...monthlyDefaults.value.map(x => x.sortOrder)) + 10 : 10, active: true }
  }
  defaultItemModal.show = true
}

async function saveDefaultItem() {
  if (!defaultItemModal.form.category || !defaultItemModal.form.itemName) {
    defaultItemModal.error = '카테고리와 항목명은 필수입니다'
    return
  }
  defaultItemModal.saving = true
  defaultItemModal.error = ''
  try {
    if (defaultItemModal.editing) {
      await monthlyCheckDefaultApi.update(defaultItemModal.editId, defaultItemModal.form)
    } else {
      await monthlyCheckDefaultApi.create(defaultItemModal.form)
    }
    defaultItemModal.show = false
    await loadMonthlyDefaults()
  } catch (e) {
    defaultItemModal.error = e || '저장에 실패했습니다'
  } finally {
    defaultItemModal.saving = false
  }
}

function confirmDeleteMonthlyDefault(d) {
  deleteConfirm.message = `'${d.itemName}' 기본 항목을 삭제하시겠습니까?`
  deleteConfirm.warning = ''
  deleteConfirm.action = async () => {
    deleteConfirm.show = false
    await monthlyCheckDefaultApi.delete(d.id)
    await loadMonthlyDefaults()
  }
  deleteConfirm.show = true
}

function priorityBadge(p) {
  return { HIGH: 'bg-red-100 text-red-700', MEDIUM: 'bg-yellow-100 text-yellow-700', LOW: 'bg-green-100 text-green-700' }[p] || 'bg-gray-100 text-gray-700'
}

function priorityLabel(p) {
  return { HIGH: '높음', MEDIUM: '중간', LOW: '낮음' }[p] || p
}

// ─── 위협 기본항목 ───
const threatDefaults = ref([])
const loadingThreatDefaults = ref(false)
const threatDefaultPage = ref(0)
const threatDefaultSize = ref(20)
const threatDefaultTotal = ref(0)
const threatDefaultTotalPages = ref(0)

const threatDefaultModal = reactive({
  show: false, editing: false, saving: false, error: '',
  editId: null,
  form: { riskId: '', name: '', type: '', category: '', assetDetail: '', description: '', likelihood: 3, impact: 3 }
})

async function loadThreatDefaults() {
  loadingThreatDefaults.value = true
  try {
    const res = await threatDefaultApi.list(threatDefaultPage.value, threatDefaultSize.value)
    const data = res.data || res
    threatDefaults.value = data.items || []
    threatDefaultTotal.value = data.total || 0
    threatDefaultTotalPages.value = Math.ceil(threatDefaultTotal.value / threatDefaultSize.value)
  } catch {
    threatDefaults.value = []
  } finally {
    loadingThreatDefaults.value = false
  }
}

async function loadThreatDefaultPage(page) {
  threatDefaultPage.value = page
  await loadThreatDefaults()
}

function openThreatDefaultModal(d = null) {
  threatDefaultModal.editing = !!d
  threatDefaultModal.editId = d?.id || null
  threatDefaultModal.error = ''
  threatDefaultModal.saving = false
  if (d) {
    threatDefaultModal.form = { riskId: d.riskId || '', name: d.name, type: d.type || '',
      category: d.category || '', assetDetail: d.assetDetail || '', description: d.description || '',
      likelihood: d.likelihood, impact: d.impact }
  } else {
    threatDefaultModal.form = { riskId: '', name: '', type: '', category: '', assetDetail: '', description: '', likelihood: 3, impact: 3 }
  }
  threatDefaultModal.show = true
}

async function saveThreatDefault() {
  if (!threatDefaultModal.form.name) {
    threatDefaultModal.error = '위협명은 필수입니다'
    return
  }
  threatDefaultModal.saving = true
  threatDefaultModal.error = ''
  try {
    if (threatDefaultModal.editing) {
      await threatDefaultApi.update(threatDefaultModal.editId, threatDefaultModal.form)
    } else {
      await threatDefaultApi.create(threatDefaultModal.form)
    }
    threatDefaultModal.show = false
    await loadAllThreatDefaults()
  } catch (e) {
    threatDefaultModal.error = e || '저장에 실패했습니다'
  } finally {
    threatDefaultModal.saving = false
  }
}

function confirmDeleteThreatDefault(d) {
  deleteConfirm.message = `'${d.name}' 위협 기본 항목을 삭제하시겠습니까?`
  deleteConfirm.warning = ''
  deleteConfirm.action = async () => {
    deleteConfirm.show = false
    await threatDefaultApi.delete(d.id)
    await loadAllThreatDefaults()
  }
  deleteConfirm.show = true
}

// ─── 월간 점검 기본 항목 검색 + 페이지네이션 ───
const searchMonthlyKeyword = ref('')
const searchMonthlyPriority = ref('')
const monthlyPage = ref(0)
const MONTHLY_PAGE_SIZE = 20

const filteredMonthlyDefaults = computed(() => {
  return monthlyDefaults.value.filter(d => {
    if (searchMonthlyPriority.value && d.priority !== searchMonthlyPriority.value) return false
    if (searchMonthlyKeyword.value) {
      const kw = searchMonthlyKeyword.value.toLowerCase()
      return (d.category || '').toLowerCase().includes(kw) ||
             (d.itemName || '').toLowerCase().includes(kw) ||
             (d.checkMethod || '').toLowerCase().includes(kw)
    }
    return true
  })
})

const monthlyTotalPages = computed(() => Math.ceil(filteredMonthlyDefaults.value.length / MONTHLY_PAGE_SIZE))
const pagedMonthlyDefaults = computed(() => {
  const start = monthlyPage.value * MONTHLY_PAGE_SIZE
  return filteredMonthlyDefaults.value.slice(start, start + MONTHLY_PAGE_SIZE)
})

watch([searchMonthlyKeyword, searchMonthlyPriority], () => { monthlyPage.value = 0 })

// ─── 위협 기본 항목 검색 + 페이지네이션 ───
const searchThreatKeyword = ref('')
const searchThreatTypeFilter = ref('')
const threatPage = ref(0)
const THREAT_PAGE_SIZE = 20

const filteredThreatDefaults = computed(() => {
  return allThreatDefaults.value.filter(d => {
    if (searchThreatTypeFilter.value && (d.type || '') !== searchThreatTypeFilter.value) return false
    if (searchThreatKeyword.value) {
      const kw = searchThreatKeyword.value.toLowerCase()
      return (d.name || '').toLowerCase().includes(kw) ||
             (d.riskId || '').toLowerCase().includes(kw) ||
             (d.category || '').toLowerCase().includes(kw)
    }
    return true
  })
})

const threatTotalPages = computed(() => Math.ceil(filteredThreatDefaults.value.length / THREAT_PAGE_SIZE))
const pagedThreatDefaults = computed(() => {
  const start = threatPage.value * THREAT_PAGE_SIZE
  return filteredThreatDefaults.value.slice(start, start + THREAT_PAGE_SIZE)
})

watch([searchThreatKeyword, searchThreatTypeFilter], () => { threatPage.value = 0 })

// ─── 위협 유형별 관리 ───
const allThreatDefaults = ref([])
const loadingAllThreats = ref(false)
const selectedThreatType = ref(null)

const allThreatTypeOptions = computed(() => {
  return [...new Set(allThreatDefaults.value.map(t => t.type).filter(Boolean))].sort()
})

const threatTypeList = computed(() => {
  const types = [...new Set(allThreatDefaults.value.map(t => t.type || '기타'))].sort()
  return types
})

const threatsBySelectedType = computed(() => {
  if (!selectedThreatType.value) return []
  return allThreatDefaults.value.filter(t => (t.type || '기타') === selectedThreatType.value)
})

const threatByTypePage = ref(0)
const THREAT_BY_TYPE_PAGE_SIZE = 20
const threatByTypeTotalPages = computed(() => Math.ceil(threatsBySelectedType.value.length / THREAT_BY_TYPE_PAGE_SIZE))
const pagedThreatsByType = computed(() => {
  const start = threatByTypePage.value * THREAT_BY_TYPE_PAGE_SIZE
  return threatsBySelectedType.value.slice(start, start + THREAT_BY_TYPE_PAGE_SIZE)
})

watch(selectedThreatType, () => { threatByTypePage.value = 0 })

async function loadAllThreatDefaults() {
  loadingAllThreats.value = true
  try {
    const res = await threatDefaultApi.list(0, 9999)
    const data = res.data || res
    allThreatDefaults.value = data.items || []
  } catch { allThreatDefaults.value = [] } finally {
    loadingAllThreats.value = false
  }
}

function openThreatByTypeModal(item) {
  threatDefaultModal.editing = !!item
  threatDefaultModal.editId = item?.id ?? null
  threatDefaultModal.error = ''
  threatDefaultModal.saving = false
  if (item) {
    Object.assign(threatDefaultModal.form, {
      riskId: item.riskId || '', name: item.name, type: item.type || '',
      category: item.category || '', assetDetail: item.assetDetail || '',
      description: item.description || '', likelihood: item.likelihood || 3, impact: item.impact || 3
    })
  } else {
    Object.assign(threatDefaultModal.form, {
      riskId: '', name: '', type: selectedThreatType.value || '',
      category: '', assetDetail: '', description: '', likelihood: 3, impact: 3
    })
  }
  threatDefaultModal.show = true
}

async function saveThreatByTypeItem() {
  await saveThreatDefault()
  await loadAllThreatDefaults()
}

// ─── 개인정보 유형별 관리 ───
const selectedPiGroup = ref(null)

const piGroups = computed(() => groups.value.filter(g => g.groupCode.startsWith('PI_') && g.groupCode !== 'PI_TYPE'))

async function selectPiGroup(group) {
  selectedPiGroup.value = group
  await selectGroup(group)
}

// ─── 수탁사 기본점검항목 ───
const contractorDefaults = ref([])
const loadingContractorDefaults = ref(false)
const savingContractorDefault = ref(false)

const contractorDefaultModal = reactive({
  show: false, editing: false, saving: false, error: '',
  editId: null,
  form: { category: '', itemName: '', checkMethod: '', checkStandard: '', sortOrder: 0 }
})

const searchContractorKeyword = ref('')
const contractorDefaultPage = ref(0)
const CONTRACTOR_DEFAULT_PAGE_SIZE = 20

const filteredContractorDefaults = computed(() => {
  if (!searchContractorKeyword.value) return contractorDefaults.value
  const kw = searchContractorKeyword.value.toLowerCase()
  return contractorDefaults.value.filter(d =>
    (d.category || '').toLowerCase().includes(kw) ||
    (d.itemName || '').toLowerCase().includes(kw) ||
    (d.checkMethod || '').toLowerCase().includes(kw)
  )
})

const contractorDefaultTotalPages = computed(() =>
  Math.ceil(filteredContractorDefaults.value.length / CONTRACTOR_DEFAULT_PAGE_SIZE)
)
const pagedContractorDefaults = computed(() => {
  const start = contractorDefaultPage.value * CONTRACTOR_DEFAULT_PAGE_SIZE
  return filteredContractorDefaults.value.slice(start, start + CONTRACTOR_DEFAULT_PAGE_SIZE)
})

watch(searchContractorKeyword, () => { contractorDefaultPage.value = 0 })

async function loadContractorDefaults() {
  loadingContractorDefaults.value = true
  try {
    const res = await contractorCheckItemApi.listDefaults()
    contractorDefaults.value = res.data ?? res
  } catch {
    contractorDefaults.value = []
  } finally {
    loadingContractorDefaults.value = false
  }
}

function openContractorDefaultModal(d = null) {
  contractorDefaultModal.editing = !!d
  contractorDefaultModal.editId = d?.id ?? null
  contractorDefaultModal.error = ''
  contractorDefaultModal.saving = false
  if (d) {
    contractorDefaultModal.form = {
      category: d.category, itemName: d.itemName,
      checkMethod: d.checkMethod || '', checkStandard: d.checkStandard || '',
      sortOrder: d.sortOrder
    }
  } else {
    const maxSort = contractorDefaults.value.length > 0
      ? Math.max(...contractorDefaults.value.map(x => x.sortOrder)) + 10 : 10
    contractorDefaultModal.form = { category: '', itemName: '', checkMethod: '', checkStandard: '', sortOrder: maxSort }
  }
  contractorDefaultModal.show = true
}

async function saveContractorDefault() {
  if (!contractorDefaultModal.form.category || !contractorDefaultModal.form.itemName) {
    contractorDefaultModal.error = '분야와 항목명은 필수입니다'
    return
  }
  contractorDefaultModal.saving = true
  contractorDefaultModal.error = ''
  try {
    if (contractorDefaultModal.editing) {
      await contractorCheckItemApi.updateDefault(contractorDefaultModal.editId, contractorDefaultModal.form)
    } else {
      await contractorCheckItemApi.createDefault(contractorDefaultModal.form)
    }
    contractorDefaultModal.show = false
    await loadContractorDefaults()
  } catch (e) {
    contractorDefaultModal.error = e || '저장에 실패했습니다'
  } finally {
    contractorDefaultModal.saving = false
  }
}

function confirmDeleteContractorDefault(d) {
  deleteConfirm.message = `'${d.itemName}' 기본 점검항목을 삭제하시겠습니까?`
  deleteConfirm.warning = ''
  deleteConfirm.action = async () => {
    deleteConfirm.show = false
    await contractorCheckItemApi.deleteDefault(d.id)
    await loadContractorDefaults()
  }
  deleteConfirm.show = true
}

const contractorCategoryColors = [
  'bg-blue-50 text-blue-700', 'bg-purple-50 text-purple-700',
  'bg-green-50 text-green-700', 'bg-orange-50 text-orange-700',
  'bg-pink-50 text-pink-700', 'bg-teal-50 text-teal-700',
  'bg-yellow-50 text-yellow-700', 'bg-red-50 text-red-700',
]
const contractorCategoryColorMap = {}
function contractorCategoryClass(cat) {
  if (!contractorCategoryColorMap[cat]) {
    const idx = Object.keys(contractorCategoryColorMap).length % contractorCategoryColors.length
    contractorCategoryColorMap[cat] = contractorCategoryColors[idx]
  }
  return contractorCategoryColorMap[cat]
}

// 탭 변경 시 데이터 로드
watch(activeTab, (tab) => {
  if (tab === 'monthly' && monthlyDefaults.value.length === 0) loadMonthlyDefaults()
  if (tab === 'threat' && allThreatDefaults.value.length === 0) loadAllThreatDefaults()
  if (tab === 'threatByType' && allThreatDefaults.value.length === 0) loadAllThreatDefaults()
  if (tab === 'piByType' && groups.value.length === 0) loadGroups()
  if (tab === 'contractorDefault' && contractorDefaults.value.length === 0) loadContractorDefaults()
})

onMounted(loadGroups)
</script>
