<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('monthlyCheck.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('monthlyCheck.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <button @click="openCreateModal"
          class="flex items-center gap-1.5 px-3 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          {{ $t('monthlyCheck.addItem') }}
        </button>
        <button v-if="items.length > 0" @click="showClearAllConfirm = true"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-red-200 text-red-600 rounded-lg hover:bg-red-50 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
          {{ $t('monthlyCheck.clearAllItems') }}
        </button>
        <button @click="confirmLoadDefaults" :disabled="loadingDefaults"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/>
          </svg>
          {{ loadingDefaults ? $t('common.loading') : $t('monthlyCheck.loadDefaults') }}
        </button>
      </div>
    </div>

    <!-- Year-Month Navigation -->
    <div class="flex items-center justify-center gap-4 mb-6">
      <button @click="prevMonth" class="p-2 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors">
        <svg class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>
      <span class="text-xl font-bold text-gray-900 min-w-[160px] text-center">{{ displayYearMonth }}</span>
      <button @click="nextMonth" class="p-2 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors">
        <svg class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
        </svg>
      </button>
    </div>

    <!-- Summary Cards -->
    <div v-if="summary" class="grid grid-cols-4 gap-4 mb-6">
      <div class="bg-white rounded-xl border p-4 text-center">
        <p class="text-2xl font-bold text-gray-900">{{ summary.total }}</p>
        <p class="text-xs text-gray-500 mt-1">{{ $t('monthlyCheck.total') }}</p>
      </div>
      <div class="bg-green-50 rounded-xl border border-green-200 p-4 text-center">
        <p class="text-2xl font-bold text-green-700">{{ summary.completed }}</p>
        <p class="text-xs text-green-600 mt-1">{{ $t('monthlyCheck.completed') }}</p>
      </div>
      <div class="bg-red-50 rounded-xl border border-red-200 p-4 text-center">
        <p class="text-2xl font-bold text-red-700">{{ summary.incomplete }}</p>
        <p class="text-xs text-red-600 mt-1">{{ $t('monthlyCheck.incomplete') }}</p>
      </div>
      <div class="bg-gray-50 rounded-xl border p-4 text-center">
        <p class="text-2xl font-bold text-gray-500">{{ summary.na }}</p>
        <p class="text-xs text-gray-500 mt-1">{{ $t('monthlyCheck.na') }}</p>
      </div>
    </div>

    <!-- Progress Bar -->
    <div v-if="summary && summary.total > 0" class="mb-6 bg-white rounded-xl border p-4">
      <div class="flex items-center justify-between mb-2">
        <span class="text-sm font-medium text-gray-700">{{ $t('monthlyCheck.progress') }}</span>
        <span class="text-sm font-semibold text-primary-600">{{ completionRate }}%</span>
      </div>
      <div class="w-full bg-gray-100 rounded-full h-2.5">
        <div class="bg-primary-500 h-2.5 rounded-full transition-all duration-500"
          :style="{ width: `${completionRate}%` }"></div>
      </div>
    </div>

    <!-- Priority Filter + Category Filter + Table -->
    <div class="bg-white rounded-xl shadow-sm border">
      <div class="flex border-b overflow-x-auto">
        <button v-for="tab in priorityTabs" :key="tab.value"
          :class="['px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors flex items-center gap-1.5',
            selectedPriority === tab.value
              ? 'border-b-2 border-primary-600 text-primary-700'
              : 'text-gray-500 hover:text-gray-700']"
          @click="selectedPriority = tab.value">
          <span v-if="tab.emoji">{{ tab.emoji }}</span>{{ tab.label }}
        </button>
      </div>

      <div v-if="categories.length > 0" class="flex flex-wrap gap-2 px-4 py-3 border-b bg-gray-50/50">
        <button v-for="cat in ['', ...categories]" :key="cat"
          @click="selectedCategory = cat"
          :class="['px-3 py-1 rounded-full text-xs font-medium transition-colors',
            selectedCategory === cat
              ? 'bg-primary-100 text-primary-700 border border-primary-200'
              : 'bg-white text-gray-500 border border-gray-200 hover:border-gray-300']">
          {{ cat === '' ? $t('common.all') : cat }}
        </button>
      </div>

      <div v-if="loading" class="py-12 text-center text-gray-500 text-sm">{{ $t('common.loading') }}</div>
      <table v-else class="w-full">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider w-16">우선순위</th>
            <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider w-24">구분</th>
            <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">점검항목</th>
            <th class="px-4 py-3 text-center text-xs font-semibold text-gray-500 uppercase tracking-wider w-20 hidden md:table-cell">담당자</th>
            <th class="px-4 py-3 text-center text-xs font-semibold text-gray-500 uppercase tracking-wider w-16 hidden sm:table-cell">증적</th>
            <th class="px-4 py-3 text-center text-xs font-semibold text-gray-500 uppercase tracking-wider w-28">결과</th>
            <th class="px-4 py-3 text-center text-xs font-semibold text-gray-500 uppercase tracking-wider w-24">작업</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-if="filteredItems.length === 0">
            <td colspan="7" class="px-4 py-10 text-center text-gray-400 text-sm">
              <div class="flex flex-col items-center gap-2">
                <svg class="w-10 h-10 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
                </svg>
                <p>{{ $t('monthlyCheck.noItems') }}</p>
                <p class="text-xs text-gray-400">{{ $t('monthlyCheck.noItemsHint') }}</p>
              </div>
            </td>
          </tr>
          <tr v-for="item in filteredItems" :key="item.id"
            class="hover:bg-gray-50/60 transition-colors">
            <td class="px-4 py-3">
              <span :class="priorityBadgeClass(item.priority)"
                class="text-xs font-semibold px-1.5 py-0.5 rounded-full whitespace-nowrap">
                {{ priorityEmoji(item.priority) }} {{ priorityLabel(item.priority) }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span class="text-xs font-medium text-gray-600 bg-gray-100 px-2 py-0.5 rounded-md">{{ item.category }}</span>
            </td>
            <td class="px-4 py-3">
              <p class="text-sm font-medium text-gray-900">{{ item.itemName }}</p>
              <p v-if="item.notes" class="text-xs text-gray-400 mt-0.5 truncate max-w-[240px]">{{ item.notes }}</p>
            </td>
            <td class="px-4 py-3 text-center hidden md:table-cell">
              <span v-if="item.assigneeName" class="text-xs text-gray-600 bg-blue-50 border border-blue-100 px-2 py-0.5 rounded-full">
                {{ item.assigneeName }}
              </span>
              <span v-else class="text-xs text-gray-300">—</span>
            </td>
            <td class="px-4 py-3 text-center hidden sm:table-cell">
              <button @click="openDetail(item)"
                :class="item.evidenceCount > 0
                  ? 'bg-primary-100 text-primary-700 border border-primary-200'
                  : 'bg-gray-100 text-gray-400 border border-gray-200'"
                class="text-xs font-semibold px-2 py-0.5 rounded-full hover:opacity-80 transition-opacity">
                {{ item.evidenceCount }}
              </button>
            </td>
            <td class="px-4 py-3 text-center">
              <button @click="quickToggleResult(item)"
                :class="resultBadgeClass(item.result)"
                class="text-xs font-semibold px-2.5 py-1.5 rounded-full cursor-pointer transition-all hover:opacity-80 border">
                {{ resultLabel(item.result) }}
              </button>
            </td>
            <td class="px-4 py-3 text-center">
              <div class="flex items-center justify-center gap-1">
                <button @click="openDetail(item)" title="상세/증적"
                  class="p-1.5 rounded-lg text-gray-400 hover:text-primary-600 hover:bg-primary-50 transition-colors">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15 12a3 3 0 11-6 0 3 3 0 016 0zM2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                  </svg>
                </button>
                <button @click="openEditModal(item)"
                  class="p-1.5 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                  </svg>
                </button>
                <button @click="confirmDelete(item)"
                  class="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <!-- ─── 우측 상세 패널 ──────────────────────────────────────────────── -->
  <Transition name="slide">
    <div v-if="detailItem" class="fixed inset-0 z-40 flex">
      <div class="flex-1 bg-black/30" @click="closeDetail"></div>
      <div class="w-full max-w-lg bg-white shadow-2xl flex flex-col overflow-hidden">

        <!-- 패널 헤더 -->
        <div class="flex items-center justify-between px-5 py-4 border-b bg-gray-50 flex-shrink-0">
          <div class="flex items-center gap-2">
            <span :class="priorityBadgeClass(detailItem.priority)"
              class="text-xs font-bold px-2 py-0.5 rounded-full">
              {{ priorityEmoji(detailItem.priority) }} {{ priorityLabel(detailItem.priority) }}
            </span>
            <span class="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-md font-medium">{{ detailItem.category }}</span>
          </div>
          <button @click="closeDetail" class="text-gray-400 hover:text-gray-600 transition-colors">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto">

          <!-- 항목 정보 -->
          <div class="px-5 py-4 border-b">
            <h2 class="text-base font-bold text-gray-900 mb-2">{{ detailItem.itemName }}</h2>
            <div v-if="detailItem.checkMethod" class="text-xs text-gray-500 mb-1">
              <span class="font-medium text-gray-600">점검방법:</span> {{ detailItem.checkMethod }}
            </div>
            <div v-if="detailItem.checkExample" class="text-xs text-gray-400">
              <span class="font-medium text-gray-500">점검예시:</span> {{ detailItem.checkExample }}
            </div>
          </div>

          <!-- 점검결과 / 비고 -->
          <div class="px-5 py-4 border-b">
            <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">점검결과</p>
            <div class="flex gap-2 flex-wrap mb-3">
              <button v-for="r in ['INCOMPLETE','COMPLETED','NA']" :key="r"
                @click="updateDetailResult(r)"
                :class="detailItem.result === r
                  ? resultBadgeClass(r) + ' ring-2 ring-offset-1 ring-current'
                  : 'bg-gray-50 text-gray-400 border border-gray-200'"
                class="text-xs font-semibold px-3 py-1.5 rounded-full border transition-all">
                {{ resultLabel(r) }}
              </button>
            </div>
            <div class="flex gap-2">
              <input v-model="detailNotes" type="text" placeholder="비고 입력..."
                class="flex-1 border border-gray-200 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"/>
              <button @click="saveDetailNotes" :disabled="savingNotes"
                class="px-3 py-1.5 text-xs bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors">
                {{ savingNotes ? '저장 중' : '저장' }}
              </button>
            </div>
          </div>

          <!-- 담당자 -->
          <div class="px-5 py-4 border-b">
            <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">담당자</p>
            <div class="relative">
              <input v-model="assigneeInput" type="text"
                placeholder="이름·이메일 검색 또는 직접 입력..."
                class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 pr-8"
                @input="onAssigneeInput" @focus="showAssigneeList = true" @keydown.escape="showAssigneeList = false"
                @keydown.enter.prevent="selectAssigneeText"/>
              <button v-if="assigneeInput" @click="clearAssignee"
                class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-300 hover:text-gray-500">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
              <!-- 사용자 목록 드롭다운 -->
              <div v-if="showAssigneeList && filteredUsers.length > 0"
                class="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto">
                <button v-for="u in filteredUsers" :key="u.id"
                  @mousedown.prevent="selectAssigneeUser(u)"
                  class="w-full px-3 py-2 text-left text-sm hover:bg-primary-50 transition-colors flex items-center gap-2">
                  <div class="w-6 h-6 bg-primary-100 text-primary-700 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0">
                    {{ u.name?.[0]?.toUpperCase() }}
                  </div>
                  <div>
                    <p class="font-medium text-gray-900">{{ u.name }}</p>
                    <p class="text-xs text-gray-400">{{ u.email }}</p>
                  </div>
                </button>
              </div>
            </div>
            <div v-if="assigneeMode === 'user'" class="mt-1.5 text-xs text-green-600 flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
              등록된 사용자로 지정됨
            </div>
            <div v-else-if="assigneeMode === 'text'" class="mt-1.5 text-xs text-gray-400 flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
              직접 입력 — 텍스트로 기록
            </div>
            <button @click="saveAssignee" :disabled="savingAssignee"
              class="mt-2 px-3 py-1.5 text-xs bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors w-full">
              {{ savingAssignee ? '저장 중...' : '담당자 저장' }}
            </button>
          </div>

          <!-- 증적 파일 -->
          <div class="px-5 py-4">
            <div class="flex items-center justify-between mb-3">
              <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                증적 파일
                <span class="ml-1 text-primary-600 font-bold">({{ evidences.length }})</span>
              </p>
              <button @click="showEvidenceForm = !showEvidenceForm"
                class="flex items-center gap-1 text-xs text-primary-600 hover:text-primary-700 font-medium">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
                증적 추가
              </button>
            </div>

            <!-- 증적 추가 폼 -->
            <div v-if="showEvidenceForm" class="bg-gray-50 rounded-lg p-4 mb-4 space-y-3 border border-gray-100">
              <div>
                <label class="block text-xs font-medium text-gray-600 mb-1">제목 <span class="text-red-500">*</span></label>
                <input v-model="evidenceForm.title" type="text"
                  class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  placeholder="점검 결과 요약 등"/>
              </div>
              <div>
                <label class="block text-xs font-medium text-gray-600 mb-1">내용</label>
                <textarea v-model="evidenceForm.content" rows="3"
                  class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
                  placeholder="상세 내용 입력..."></textarea>
              </div>
              <div>
                <label class="block text-xs font-medium text-gray-600 mb-1">파일 첨부</label>
                <input type="file" @change="e => evidenceForm.file = e.target.files[0]" ref="evidenceFileInput"
                  class="w-full text-xs text-gray-500 file:mr-2 file:py-1 file:px-2 file:rounded file:border-0 file:text-xs file:font-medium file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100"/>
                <p v-if="evidenceForm.file" class="mt-1 text-xs text-gray-500">{{ evidenceForm.file.name }}</p>
              </div>
              <div class="flex gap-2 pt-1">
                <button @click="cancelEvidenceForm"
                  class="flex-1 px-3 py-1.5 text-xs border border-gray-300 rounded-lg hover:bg-gray-50 text-gray-600">취소</button>
                <button @click="submitEvidence" :disabled="!evidenceForm.title || submittingEvidence"
                  class="flex-1 px-3 py-1.5 text-xs bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors">
                  {{ submittingEvidence ? '등록 중...' : '등록' }}
                </button>
              </div>
            </div>

            <!-- 증적 목록 -->
            <div v-if="loadingEvidences" class="py-4 text-center text-gray-400 text-xs">로딩 중...</div>
            <div v-else-if="evidences.length === 0 && !showEvidenceForm"
              class="py-6 text-center text-gray-300 text-sm">
              <svg class="w-8 h-8 mx-auto mb-2 text-gray-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                  d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/>
              </svg>
              등록된 증적이 없습니다
            </div>
            <div v-else class="space-y-2">
              <div v-for="ev in evidences" :key="ev.id"
                class="border border-gray-100 rounded-lg p-3 bg-white hover:border-gray-200 transition-colors">
                <div class="flex items-start justify-between gap-2">
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-gray-900 truncate">{{ ev.title }}</p>
                    <p v-if="ev.content" class="text-xs text-gray-500 mt-0.5 line-clamp-2">{{ ev.content }}</p>
                    <div class="flex items-center gap-2 mt-1.5">
                      <span v-if="ev.uploadedByName" class="text-xs text-gray-400">{{ ev.uploadedByName }}</span>
                      <span class="text-xs text-gray-300">{{ formatDate(ev.createdAt) }}</span>
                    </div>
                  </div>
                  <div class="flex items-center gap-1 flex-shrink-0">
                    <button v-if="ev.fileName" @click="downloadEvidence(ev)"
                      class="p-1.5 rounded-lg text-gray-400 hover:text-primary-600 hover:bg-primary-50 transition-colors" title="파일 다운로드">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
                      </svg>
                    </button>
                    <button @click="deleteEvidence(ev)"
                      class="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors" title="삭제">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                      </svg>
                    </button>
                  </div>
                </div>
                <!-- 파일 태그 -->
                <div v-if="ev.fileName" class="mt-1.5">
                  <span class="inline-flex items-center gap-1 text-xs text-blue-600 bg-blue-50 border border-blue-100 px-2 py-0.5 rounded-full">
                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/>
                    </svg>
                    {{ ev.fileName }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>

  <!-- ─── 항목 추가/수정 모달 ─────────────────────────────────────────── -->
  <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
      <div class="flex items-center justify-between px-6 py-4 border-b">
        <h2 class="text-lg font-semibold text-gray-900">
          {{ editingItem ? $t('monthlyCheck.editItem') : $t('monthlyCheck.addItem') }}
        </h2>
        <button @click="closeModal" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="px-6 py-5 space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">우선순위 <span class="text-red-500">*</span></label>
            <select v-model="form.priority"
              class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
              <option value="HIGH">🔴 상 (매월 필수)</option>
              <option value="MEDIUM">🟠 중 (매월 권장)</option>
              <option value="LOW">🟡 하 (분기/반기)</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">구분 <span class="text-red-500">*</span></label>
            <input v-model="form.category" list="category-list"
              class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="예: 계정관리"/>
            <datalist id="category-list">
              <option v-for="cat in PRESET_CATEGORIES" :key="cat" :value="cat"/>
            </datalist>
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">점검항목 <span class="text-red-500">*</span></label>
          <input v-model="form.itemName" type="text"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            placeholder="예: 퇴직자 계정 삭제 여부"/>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">점검방법</label>
          <input v-model="form.checkMethod" type="text"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            placeholder="예: 인사시스템과 계정 목록 비교"/>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">점검예시</label>
          <input v-model="form.checkExample" type="text"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            placeholder="예: 퇴사자 AD, VPN 계정 삭제 확인"/>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검결과</label>
            <select v-model="form.result"
              class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
              <option value="INCOMPLETE">미완료</option>
              <option value="COMPLETED">완료</option>
              <option value="NA">해당없음</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">비고</label>
            <input v-model="form.notes" type="text"
              class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="특이사항 입력"/>
          </div>
        </div>
        <p v-if="formError" class="text-sm text-red-600">{{ formError }}</p>
      </div>
      <div class="flex justify-end gap-3 px-6 py-4 border-t bg-gray-50 rounded-b-xl">
        <button @click="closeModal"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ $t('common.cancel') }}
        </button>
        <button @click="saveItem" :disabled="saving"
          class="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors">
          {{ saving ? $t('common.loading') : $t('common.save') }}
        </button>
      </div>
    </div>
  </div>

  <!-- 삭제 확인 모달 -->
  <div v-if="deletingItem" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
      <h3 class="text-lg font-semibold text-gray-900 mb-2">{{ $t('monthlyCheck.deleteConfirmTitle') }}</h3>
      <p class="text-sm text-gray-500 mb-6">
        <span class="font-medium text-gray-800">{{ deletingItem.itemName }}</span>
        {{ $t('monthlyCheck.deleteConfirmMsg') }}
      </p>
      <div class="flex justify-end gap-3">
        <button @click="deletingItem = null"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ $t('common.cancel') }}
        </button>
        <button @click="deleteItem" :disabled="deleting"
          class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors">
          {{ deleting ? $t('common.loading') : $t('common.delete') }}
        </button>
      </div>
    </div>
  </div>

  <!-- 항목 전체 초기화 확인 모달 -->
  <div v-if="showClearAllConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
      <div class="flex items-center gap-3 mb-3">
        <div class="flex-shrink-0 w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
          <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900">{{ $t('monthlyCheck.clearAllConfirmTitle') }}</h3>
      </div>
      <p class="text-sm text-gray-700 mb-3">
        <span class="font-semibold text-gray-900">{{ displayYearMonth }}</span> 기준
        총 <span class="font-semibold text-red-600">{{ items.length }}개</span>의 항목이 삭제됩니다.
      </p>
      <p class="text-xs text-red-700 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
        {{ $t('monthlyCheck.clearAllConfirmMsg') }}
      </p>
      <div class="flex justify-end gap-3 mt-6">
        <button @click="showClearAllConfirm = false"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ $t('common.cancel') }}
        </button>
        <button @click="handleClearAll" :disabled="clearingAll"
          class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors">
          {{ clearingAll ? $t('common.loading') : $t('monthlyCheck.clearAllConfirm') }}
        </button>
      </div>
    </div>
  </div>

  <!-- 기본 항목 이미 로딩됨 안내 모달 -->
  <div v-if="showDefaultsAlreadyLoaded" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
      <div class="flex items-center gap-3 mb-3">
        <div class="flex-shrink-0 w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
          <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900">기본 항목이 이미 추가되어 있습니다</h3>
      </div>
      <p class="text-sm text-gray-600 mb-3">
        <span class="font-semibold text-gray-900">{{ displayYearMonth }}</span>에는 이미 기본 32개 점검 항목이 모두 추가되어 있어 불러올 수 없습니다.
      </p>
      <p class="text-xs text-blue-700 bg-blue-50 border border-blue-200 rounded-lg px-3 py-2">
        항목을 추가하려면 <span class="font-semibold">항목 추가</span> 버튼을 이용하거나, 기존 항목을 삭제한 후 다시 시도하세요.
      </p>
      <div class="flex justify-end mt-6">
        <button @click="showDefaultsAlreadyLoaded = false"
          class="px-4 py-2 text-sm bg-gray-100 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-200 transition-colors">
          확인
        </button>
      </div>
    </div>
  </div>

  <!-- 기본 항목 불러오기 확인 모달 -->
  <div v-if="showDefaultsConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
      <div class="flex items-center gap-3 mb-3">
        <div class="flex-shrink-0 w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
          <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900">{{ $t('monthlyCheck.loadDefaultsTitle') }}</h3>
      </div>
      <p class="text-sm text-gray-700 mb-3">
        <span class="font-semibold text-gray-900">{{ displayYearMonth }}</span> — {{ $t('monthlyCheck.loadDefaultsMsg') }}
      </p>
      <p class="text-xs text-red-700 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
        {{ $t('monthlyCheck.loadDefaultsWarning') }}
      </p>
      <div class="flex justify-end gap-3 mt-6">
        <button @click="showDefaultsConfirm = false"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ $t('common.cancel') }}
        </button>
        <button @click="loadDefaults"
          class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors">
          {{ $t('monthlyCheck.loadDefaultsConfirm') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { monthlyCheckApi, adminApi } from '@/api'

const PRESET_CATEGORIES = ['계정관리', '패스워드 관리', '서버보안', '개인정보보호', '백업관리', '로그관리', '취약점관리', '클라우드보안', '네트워크보안', '보안운영']

// ── 목록 상태 ──────────────────────────────────────────────────────────
const currentYearMonth = ref(todayYearMonth())
const items = ref([])
const summary = ref(null)
const loading = ref(false)
const selectedPriority = ref('')
const selectedCategory = ref('')

// ── 모달 상태 ──────────────────────────────────────────────────────────
const showModal = ref(false)
const editingItem = ref(null)
const form = ref(defaultForm())
const formError = ref('')
const saving = ref(false)
const deletingItem = ref(null)
const deleting = ref(false)
const showDefaultsConfirm = ref(false)
const showDefaultsAlreadyLoaded = ref(false)
const loadingDefaults = ref(false)
const showClearAllConfirm = ref(false)
const clearingAll = ref(false)

// ── 상세 패널 상태 ─────────────────────────────────────────────────────
const detailItem = ref(null)
const detailNotes = ref('')
const savingNotes = ref(false)

// 담당자
const allUsers = ref([])
const assigneeInput = ref('')
const assigneeMode = ref('') // 'user' | 'text' | ''
const selectedAssigneeId = ref(null)
const showAssigneeList = ref(false)
const savingAssignee = ref(false)

// 증적
const evidences = ref([])
const loadingEvidences = ref(false)
const showEvidenceForm = ref(false)
const evidenceForm = ref({ title: '', content: '', file: null })
const evidenceFileInput = ref(null)
const submittingEvidence = ref(false)

// ── Computed ───────────────────────────────────────────────────────────
function todayYearMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function defaultForm() {
  return { priority: 'HIGH', category: '', itemName: '', checkMethod: '', checkExample: '', result: 'INCOMPLETE', notes: '' }
}

const displayYearMonth = computed(() => {
  const [y, m] = currentYearMonth.value.split('-')
  return `${y}년 ${parseInt(m)}월`
})

const completionRate = computed(() => {
  if (!summary.value || summary.value.total === 0) return 0
  return Math.round(((summary.value.completed + summary.value.na) / summary.value.total) * 100)
})

const priorityTabs = [
  { value: '', label: '전체' },
  { value: 'HIGH', label: '상', emoji: '🔴' },
  { value: 'MEDIUM', label: '중', emoji: '🟠' },
  { value: 'LOW', label: '하', emoji: '🟡' }
]

const categories = computed(() => [...new Set(items.value.map(i => i.category))])

const filteredItems = computed(() =>
  items.value.filter(item => {
    const pOk = !selectedPriority.value || item.priority === selectedPriority.value
    const cOk = !selectedCategory.value || item.category === selectedCategory.value
    return pOk && cOk
  })
)

const filteredUsers = computed(() => {
  if (!assigneeInput.value.trim()) return allUsers.value.slice(0, 10)
  const q = assigneeInput.value.toLowerCase()
  return allUsers.value.filter(u =>
    u.name?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q)
  ).slice(0, 10)
})

// ── 월 네비게이션 ──────────────────────────────────────────────────────
function prevMonth() {
  const [y, m] = currentYearMonth.value.split('-').map(Number)
  const d = new Date(y, m - 2, 1)
  currentYearMonth.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
function nextMonth() {
  const [y, m] = currentYearMonth.value.split('-').map(Number)
  const d = new Date(y, m, 1)
  currentYearMonth.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

watch(currentYearMonth, () => { selectedCategory.value = ''; loadData() })

// ── 데이터 로드 ────────────────────────────────────────────────────────
async function loadData() {
  loading.value = true
  try {
    const [itemsRes, summaryRes] = await Promise.all([
      monthlyCheckApi.list(currentYearMonth.value),
      monthlyCheckApi.summary(currentYearMonth.value)
    ])
    items.value = itemsRes.data
    summary.value = summaryRes.data
  } finally {
    loading.value = false
  }
}

async function refreshSummary() {
  const res = await monthlyCheckApi.summary(currentYearMonth.value)
  summary.value = res.data
}

// ── 뱃지 헬퍼 ─────────────────────────────────────────────────────────
function priorityEmoji(p) { return { HIGH: '🔴', MEDIUM: '🟠', LOW: '🟡' }[p] ?? '' }
function priorityLabel(p) { return { HIGH: '상', MEDIUM: '중', LOW: '하' }[p] ?? p }
function priorityBadgeClass(p) {
  return { HIGH: 'bg-red-50 text-red-700', MEDIUM: 'bg-orange-50 text-orange-700', LOW: 'bg-yellow-50 text-yellow-700' }[p] ?? 'bg-gray-100 text-gray-600'
}
function resultLabel(r) { return { COMPLETED: '완료', INCOMPLETE: '미완료', NA: '해당없음' }[r] ?? r }
function resultBadgeClass(r) {
  return { COMPLETED: 'bg-green-50 text-green-700 border-green-200', INCOMPLETE: 'bg-red-50 text-red-700 border-red-200', NA: 'bg-gray-50 text-gray-500 border-gray-200' }[r] ?? 'bg-gray-50 text-gray-500 border-gray-200'
}
function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// ── 결과 토글 (테이블) ─────────────────────────────────────────────────
async function quickToggleResult(item) {
  const cycle = { INCOMPLETE: 'COMPLETED', COMPLETED: 'NA', NA: 'INCOMPLETE' }
  try {
    const res = await monthlyCheckApi.update(item.id, { result: cycle[item.result] })
    const idx = items.value.findIndex(i => i.id === item.id)
    if (idx !== -1) items.value[idx] = res.data
    if (detailItem.value?.id === item.id) detailItem.value = res.data
    await refreshSummary()
  } catch {}
}

// ── 상세 패널 ─────────────────────────────────────────────────────────
async function openDetail(item) {
  detailItem.value = item
  detailNotes.value = item.notes ?? ''
  showEvidenceForm.value = false
  evidenceForm.value = { title: '', content: '', file: null }
  // 담당자 초기화
  if (item.assigneeId) {
    selectedAssigneeId.value = item.assigneeId
    assigneeMode.value = 'user'
    assigneeInput.value = item.assigneeName ?? ''
  } else if (item.assigneeName) {
    selectedAssigneeId.value = null
    assigneeMode.value = 'text'
    assigneeInput.value = item.assigneeName
  } else {
    selectedAssigneeId.value = null
    assigneeMode.value = ''
    assigneeInput.value = ''
  }
  // 사용자 목록 & 증적 로드
  if (allUsers.value.length === 0) {
    try { const res = await adminApi.listUsersSimple(); allUsers.value = res.data } catch {}
  }
  await loadEvidences(item.id)
}

function closeDetail() {
  detailItem.value = null
  showEvidenceForm.value = false
  showAssigneeList.value = false
}

async function updateDetailResult(result) {
  if (!detailItem.value) return
  try {
    const res = await monthlyCheckApi.update(detailItem.value.id, { result })
    detailItem.value = res.data
    const idx = items.value.findIndex(i => i.id === res.data.id)
    if (idx !== -1) items.value[idx] = res.data
    await refreshSummary()
  } catch {}
}

async function saveDetailNotes() {
  if (!detailItem.value) return
  savingNotes.value = true
  try {
    const res = await monthlyCheckApi.update(detailItem.value.id, { notes: detailNotes.value })
    detailItem.value = res.data
    const idx = items.value.findIndex(i => i.id === res.data.id)
    if (idx !== -1) items.value[idx] = res.data
  } finally {
    savingNotes.value = false
  }
}

// ── 담당자 ─────────────────────────────────────────────────────────────
function onAssigneeInput() {
  selectedAssigneeId.value = null
  assigneeMode.value = assigneeInput.value ? 'text' : ''
  showAssigneeList.value = true
}
function selectAssigneeUser(user) {
  assigneeInput.value = user.name
  selectedAssigneeId.value = user.id
  assigneeMode.value = 'user'
  showAssigneeList.value = false
}
function selectAssigneeText() {
  if (selectedAssigneeId.value === null && assigneeInput.value) assigneeMode.value = 'text'
  showAssigneeList.value = false
}
function clearAssignee() {
  assigneeInput.value = ''
  selectedAssigneeId.value = null
  assigneeMode.value = ''
  showAssigneeList.value = false
}
async function saveAssignee() {
  if (!detailItem.value) return
  savingAssignee.value = true
  try {
    const payload = assigneeInput.value
      ? (selectedAssigneeId.value
          ? { assigneeId: selectedAssigneeId.value, clearAssignee: false }
          : { assigneeText: assigneeInput.value, clearAssignee: false })
      : { clearAssignee: true }
    const res = await monthlyCheckApi.update(detailItem.value.id, payload)
    detailItem.value = res.data
    const idx = items.value.findIndex(i => i.id === res.data.id)
    if (idx !== -1) items.value[idx] = res.data
  } finally {
    savingAssignee.value = false
  }
}

// ── 증적 ───────────────────────────────────────────────────────────────
async function loadEvidences(itemId) {
  loadingEvidences.value = true
  try {
    const res = await monthlyCheckApi.listEvidences(itemId)
    evidences.value = res.data
  } finally {
    loadingEvidences.value = false
  }
}

function cancelEvidenceForm() {
  showEvidenceForm.value = false
  evidenceForm.value = { title: '', content: '', file: null }
  if (evidenceFileInput.value) evidenceFileInput.value.value = ''
}

async function submitEvidence() {
  if (!detailItem.value || !evidenceForm.value.title) return
  submittingEvidence.value = true
  try {
    await monthlyCheckApi.createEvidence(detailItem.value.id, evidenceForm.value)
    await loadEvidences(detailItem.value.id)
    // 증적 수 갱신
    const idx = items.value.findIndex(i => i.id === detailItem.value.id)
    if (idx !== -1) items.value[idx] = { ...items.value[idx], evidenceCount: evidences.value.length }
    detailItem.value = { ...detailItem.value, evidenceCount: evidences.value.length }
    cancelEvidenceForm()
  } finally {
    submittingEvidence.value = false
  }
}

async function deleteEvidence(ev) {
  try {
    await monthlyCheckApi.deleteEvidence(ev.id)
    evidences.value = evidences.value.filter(e => e.id !== ev.id)
    const idx = items.value.findIndex(i => i.id === detailItem.value?.id)
    if (idx !== -1) items.value[idx] = { ...items.value[idx], evidenceCount: evidences.value.length }
    if (detailItem.value) detailItem.value = { ...detailItem.value, evidenceCount: evidences.value.length }
  } catch {}
}

async function downloadEvidence(ev) {
  if (!ev.fileName) return
  await monthlyCheckApi.downloadEvidenceFile(ev.id, ev.fileName)
}

// ── 항목 CRUD ──────────────────────────────────────────────────────────
function openCreateModal() {
  editingItem.value = null; form.value = defaultForm(); formError.value = ''; showModal.value = true
}
function openEditModal(item) {
  editingItem.value = item
  form.value = { priority: item.priority, category: item.category, itemName: item.itemName, checkMethod: item.checkMethod ?? '', checkExample: item.checkExample ?? '', result: item.result, notes: item.notes ?? '' }
  formError.value = ''; showModal.value = true
}
function closeModal() { showModal.value = false; editingItem.value = null }

async function saveItem() {
  if (!form.value.category || !form.value.itemName) { formError.value = '구분과 점검항목은 필수입니다.'; return }
  saving.value = true; formError.value = ''
  try {
    if (editingItem.value) {
      const res = await monthlyCheckApi.update(editingItem.value.id, form.value)
      const idx = items.value.findIndex(i => i.id === editingItem.value.id)
      if (idx !== -1) items.value[idx] = res.data
    } else {
      const res = await monthlyCheckApi.create({ ...form.value, yearMonth: currentYearMonth.value })
      items.value.push(res.data)
    }
    await refreshSummary(); closeModal()
  } catch (e) { formError.value = typeof e === 'string' ? e : '저장 중 오류가 발생했습니다.' }
  finally { saving.value = false }
}

function confirmDelete(item) { deletingItem.value = item }
async function deleteItem() {
  if (!deletingItem.value) return
  deleting.value = true
  try {
    await monthlyCheckApi.delete(deletingItem.value.id)
    items.value = items.value.filter(i => i.id !== deletingItem.value.id)
    if (detailItem.value?.id === deletingItem.value.id) closeDetail()
    await refreshSummary(); deletingItem.value = null
  } finally { deleting.value = false }
}

async function handleClearAll() {
  clearingAll.value = true
  try {
    await monthlyCheckApi.clearAll(currentYearMonth.value)
    items.value = []
    summary.value = { total: 0, completed: 0, incomplete: 0, na: 0 }
    showClearAllConfirm.value = false
    if (detailItem.value) closeDetail()
  } finally {
    clearingAll.value = false
  }
}

async function confirmLoadDefaults() {
  loadingDefaults.value = true
  try {
    const res = await monthlyCheckApi.checkDefaults(currentYearMonth.value)
    if (res.data) {
      showDefaultsAlreadyLoaded.value = true
    } else {
      showDefaultsConfirm.value = true
    }
  } catch {
    showDefaultsConfirm.value = true
  } finally {
    loadingDefaults.value = false
  }
}
async function loadDefaults() {
  showDefaultsConfirm.value = false; loadingDefaults.value = true
  try { await monthlyCheckApi.loadDefaults(currentYearMonth.value); await loadData() }
  finally { loadingDefaults.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.slide-enter-active, .slide-leave-active {
  transition: transform 0.25s ease;
}
.slide-enter-from, .slide-leave-to {
  transform: translateX(100%);
}
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
