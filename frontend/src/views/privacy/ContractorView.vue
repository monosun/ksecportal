<template>
  <div class="p-6 max-w-7xl mx-auto">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">수탁사 관리</h1>
        <p class="text-sm text-gray-500 mt-1">개인정보 처리 수탁사 정보 및 점검 이력 관리</p>
      </div>
      <div class="flex items-center gap-2">
        <button @click="openImportModal()"
          class="flex items-center gap-2 px-4 py-2 bg-white border border-primary-200 text-primary-600 rounded-xl text-sm font-semibold hover:bg-primary-50 transition-all">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          개인정보처리방침에서 수탁사 일괄등록
        </button>
        <button @click="openContractorModal(null)"
          class="flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded-xl text-sm font-semibold hover:bg-primary-600 transition-all">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          수탁사 등록
        </button>
      </div>
    </div>

    <div class="flex gap-6">
      <!-- Left: Contractor List -->
      <div class="w-80 flex-shrink-0">
        <div class="mb-3 flex gap-2">
          <input v-model="searchName" type="text" placeholder="수탁사명 검색..."
            class="flex-1 px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
          <select v-model="filterStatus"
            class="px-2 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400">
            <option value="">전체</option>
            <option value="ACTIVE">계약중</option>
            <option value="INACTIVE">종료</option>
          </select>
        </div>

        <div class="space-y-2 max-h-[calc(100vh-220px)] overflow-y-auto pr-1">
          <div v-if="loading" class="text-center py-8 text-gray-400 text-sm">로딩 중...</div>
          <div v-else-if="filteredContractors.length === 0" class="text-center py-8 text-gray-400 text-sm">등록된 수탁사가 없습니다</div>
          <button v-for="c in filteredContractors" :key="c.id"
            @click="selectContractor(c)"
            class="w-full text-left p-4 rounded-xl border transition-all"
            :class="selected?.id === c.id
              ? 'border-primary-400 bg-primary-50'
              : 'border-gray-200 bg-white hover:border-gray-300 hover:bg-gray-50'">
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <p class="font-semibold text-gray-900 truncate text-sm">{{ c.name }}</p>
                <p class="text-xs text-gray-500 mt-0.5 truncate">{{ c.serviceType || '—' }}</p>
                <p class="text-xs text-gray-400 mt-1">
                  {{ c.contractStart || '?' }} ~ {{ c.contractEnd || '?' }}
                </p>
              </div>
              <span class="flex-shrink-0 text-[11px] font-semibold px-2 py-0.5 rounded-full"
                :class="c.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'">
                {{ c.status === 'ACTIVE' ? '계약중' : '종료' }}
              </span>
            </div>
            <div class="mt-2 flex items-center gap-1 text-xs text-gray-400">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
              </svg>
              점검 {{ c.checkCount ?? 0 }}건
            </div>
          </button>
        </div>
      </div>

      <!-- Right: Detail Panel -->
      <div class="flex-1 min-w-0">
        <div v-if="!selected" class="flex items-center justify-center h-full text-gray-400 text-sm py-20">
          왼쪽에서 수탁사를 선택하세요
        </div>
        <template v-else>
          <!-- Contractor Info Card -->
          <div class="bg-white rounded-2xl border border-gray-200 p-6 mb-4">
            <div class="flex items-start justify-between mb-4">
              <div>
                <h2 class="text-lg font-bold text-gray-900">{{ selected.name }}</h2>
                <p class="text-sm text-gray-500">{{ selected.businessNumber || '사업자번호 미등록' }}</p>
              </div>
              <div class="flex gap-2">
                <button @click="openContractorModal(selected)"
                  class="px-3 py-1.5 text-sm font-medium border border-gray-200 rounded-xl hover:bg-gray-50 transition-all">
                  수정
                </button>
                <button @click="confirmDeleteContractor(selected)"
                  class="px-3 py-1.5 text-sm font-medium border border-red-200 text-red-600 rounded-xl hover:bg-red-50 transition-all">
                  삭제
                </button>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p class="text-gray-400 text-xs font-medium uppercase tracking-wide mb-1">위탁 업무</p>
                <p class="text-gray-800">{{ selected.serviceType || '—' }}</p>
              </div>
              <div>
                <p class="text-gray-400 text-xs font-medium uppercase tracking-wide mb-1">재수탁사</p>
                <p class="text-gray-800">{{ selected.subContractor || '—' }}</p>
              </div>
              <div>
                <p class="text-gray-400 text-xs font-medium uppercase tracking-wide mb-1">대표자</p>
                <p class="text-gray-800">{{ selected.representative || '—' }}</p>
              </div>
              <div>
                <p class="text-gray-400 text-xs font-medium uppercase tracking-wide mb-1">계약기간</p>
                <p class="text-gray-800">{{ selected.contractStart || '?' }} ~ {{ selected.contractEnd || '?' }}</p>
              </div>
              <div>
                <p class="text-gray-400 text-xs font-medium uppercase tracking-wide mb-1">담당자</p>
                <p class="text-gray-800">{{ selected.contactPerson || '—' }}</p>
              </div>
              <div>
                <p class="text-gray-400 text-xs font-medium uppercase tracking-wide mb-1">이메일</p>
                <p class="text-gray-800">{{ selected.contactEmail || '—' }}</p>
              </div>
              <div>
                <p class="text-gray-400 text-xs font-medium uppercase tracking-wide mb-1">연락처</p>
                <p class="text-gray-800">{{ selected.contactPhone || '—' }}</p>
              </div>
            </div>
            <div v-if="selected.notes" class="mt-4 pt-4 border-t border-gray-100">
              <p class="text-gray-400 text-xs font-medium uppercase tracking-wide mb-1">비고</p>
              <p class="text-gray-700 text-sm whitespace-pre-wrap">{{ selected.notes }}</p>
            </div>
          </div>

          <!-- Checks Section -->
          <div class="bg-white rounded-2xl border border-gray-200 p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-semibold text-gray-900">점검 이력</h3>
              <button @click="openCheckModal(null)"
                class="flex items-center gap-1.5 px-3 py-1.5 bg-primary-500 text-white text-sm font-medium rounded-xl hover:bg-primary-600 transition-all">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
                점검 등록
              </button>
            </div>

            <div v-if="loadingChecks" class="text-center py-8 text-gray-400 text-sm">로딩 중...</div>

            <div v-else-if="contractorChecks.length === 0" class="text-center py-8 text-gray-400 text-sm">
              등록된 점검 이력이 없습니다.<br/>
              <span class="text-xs">"점검 등록" 버튼으로 연도별 점검을 시작하세요.</span>
            </div>

            <div v-else class="space-y-3">
              <div v-for="chk in contractorChecks" :key="chk.id"
                class="border border-gray-100 rounded-xl p-4 hover:border-gray-200 transition-colors">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex-1 min-w-0">
                    <!-- Year + Status -->
                    <div class="flex items-center gap-2 mb-2">
                      <span class="text-base font-bold text-gray-900">{{ chk.checkYear }}년</span>
                      <span class="text-[11px] font-semibold px-2 py-0.5 rounded-full"
                        :class="checkStatusClass(chk.status)">
                        {{ checkStatusLabel(chk.status) }}
                      </span>
                    </div>
                    <!-- Date + Inspector -->
                    <div class="flex items-center gap-3 text-xs text-gray-500 mb-3">
                      <span v-if="chk.checkDate">
                        <svg class="w-3 h-3 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                        </svg>
                        {{ chk.checkDate }}
                      </span>
                      <span v-if="chk.inspector">
                        <svg class="w-3 h-3 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                        </svg>
                        {{ chk.inspector }}
                      </span>
                    </div>
                    <!-- Progress Bar -->
                    <div v-if="chk.totalItems > 0">
                      <div class="flex items-center gap-1 mb-1">
                        <div class="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden flex">
                          <div class="bg-green-500 h-full transition-all" :style="`width:${(chk.passCount/chk.totalItems*100).toFixed(1)}%`"/>
                          <div class="bg-red-400 h-full transition-all" :style="`width:${(chk.failCount/chk.totalItems*100).toFixed(1)}%`"/>
                          <div class="bg-gray-300 h-full transition-all" :style="`width:${(chk.naCount/chk.totalItems*100).toFixed(1)}%`"/>
                        </div>
                        <span class="text-xs text-gray-500 flex-shrink-0">
                          {{ chk.totalItems - chk.notCheckedCount }}/{{ chk.totalItems }}
                        </span>
                      </div>
                      <div class="flex gap-3 text-xs">
                        <span class="text-green-600 font-medium">통과 {{ chk.passCount }}</span>
                        <span class="text-red-500 font-medium">미흡 {{ chk.failCount }}</span>
                        <span class="text-gray-400">해당없음 {{ chk.naCount }}</span>
                        <span class="text-gray-300">미점검 {{ chk.notCheckedCount }}</span>
                      </div>
                    </div>
                    <div v-else class="text-xs text-gray-400">점검 항목 없음</div>
                    <p v-if="chk.notes" class="mt-2 text-xs text-gray-500 truncate">{{ chk.notes }}</p>
                  </div>
                  <!-- Actions -->
                  <div class="flex flex-col gap-1.5 flex-shrink-0">
                    <button @click="openDetailModal(chk)"
                      class="px-2.5 py-1 text-xs font-medium bg-primary-50 text-primary-600 border border-primary-200 rounded-lg hover:bg-primary-100 transition-all whitespace-nowrap">
                      결과 보기
                    </button>
                    <button @click="openCheckModal(chk)"
                      class="px-2.5 py-1 text-xs font-medium border border-gray-200 rounded-lg hover:bg-gray-50 transition-all">
                      수정
                    </button>
                    <button @click="confirmDeleteCheck(chk)"
                      class="px-2.5 py-1 text-xs font-medium border border-red-200 text-red-600 rounded-lg hover:bg-red-50 transition-all">
                      삭제
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- ─── Contractor Modal ─── -->
    <Transition name="fade">
      <div v-if="contractorModal.open" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="contractorModal.open = false"/>
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg p-6">
          <h3 class="text-lg font-bold text-gray-900 mb-5">
            {{ contractorModal.isEdit ? '수탁사 수정' : '수탁사 등록' }}
          </h3>
          <div class="grid grid-cols-2 gap-4">
            <div class="col-span-2">
              <label class="block text-xs font-medium text-gray-600 mb-1">수탁사명 *</label>
              <input v-model="contractorModal.form.name" type="text" placeholder="수탁사명"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">사업자등록번호</label>
              <input v-model="contractorModal.form.businessNumber" type="text" placeholder="000-00-00000"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">대표자</label>
              <input v-model="contractorModal.form.representative" type="text" placeholder="대표자명"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div class="col-span-2">
              <label class="block text-xs font-medium text-gray-600 mb-1">위탁 업무</label>
              <input v-model="contractorModal.form.serviceType" type="text" placeholder="예: 고객센터 운영, 배송 서비스 등"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div class="col-span-2">
              <label class="block text-xs font-medium text-gray-600 mb-1">재수탁사</label>
              <input v-model="contractorModal.form.subContractor" type="text" placeholder="재위탁(재수탁) 업체가 있는 경우 입력"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">계약 시작일</label>
              <input v-model="contractorModal.form.contractStart" type="date"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">계약 종료일</label>
              <input v-model="contractorModal.form.contractEnd" type="date"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">담당자</label>
              <input v-model="contractorModal.form.contactPerson" type="text" placeholder="담당자명"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">연락처</label>
              <input v-model="contractorModal.form.contactPhone" type="text" placeholder="010-0000-0000"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div class="col-span-2">
              <label class="block text-xs font-medium text-gray-600 mb-1">이메일</label>
              <input v-model="contractorModal.form.contactEmail" type="email" placeholder="email@example.com"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">상태</label>
              <select v-model="contractorModal.form.status"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400">
                <option value="ACTIVE">계약중</option>
                <option value="INACTIVE">종료</option>
              </select>
            </div>
            <div class="col-span-2">
              <label class="block text-xs font-medium text-gray-600 mb-1">비고</label>
              <textarea v-model="contractorModal.form.notes" rows="2" placeholder="비고"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 resize-none"/>
            </div>
          </div>
          <div class="flex gap-3 mt-6">
            <button @click="contractorModal.open = false"
              class="flex-1 py-2.5 rounded-xl border-2 border-gray-200 text-sm font-semibold text-gray-600 hover:bg-gray-50 transition-all">
              취소
            </button>
            <button @click="saveContractor" :disabled="saving"
              class="flex-1 py-2.5 rounded-xl bg-primary-500 text-sm font-semibold text-white hover:bg-primary-600 disabled:opacity-60 transition-all">
              {{ saving ? '저장 중...' : '저장' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ─── Check Detail Modal (점검 결과 보기) ─── -->
    <Transition name="fade">
      <div v-if="detailModal.open" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="detailModal.open = false"/>
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[85vh] flex flex-col">
          <!-- Header -->
          <div class="flex items-start justify-between p-6 border-b border-gray-100">
            <div>
              <div class="flex items-center gap-2 mb-1">
                <h3 class="text-lg font-bold text-gray-900">{{ detailModal.check?.checkYear }}년 점검 결과</h3>
                <span v-if="detailModal.check?.status" class="text-[11px] font-semibold px-2 py-0.5 rounded-full"
                  :class="checkStatusClass(detailModal.check.status)">
                  {{ checkStatusLabel(detailModal.check.status) }}
                </span>
              </div>
              <div class="flex items-center gap-3 text-xs text-gray-500">
                <span v-if="detailModal.check?.checkDate">점검일: {{ detailModal.check.checkDate }}</span>
                <span v-if="detailModal.check?.inspector">점검자: {{ detailModal.check.inspector }}</span>
              </div>
            </div>
            <button @click="detailModal.open = false" class="p-1 hover:bg-gray-100 rounded-lg transition-colors">
              <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <!-- Stats Bar -->
          <div v-if="detailModal.totalItems > 0" class="px-6 py-3 border-b border-gray-100 bg-gray-50">
            <div class="flex items-center gap-2 mb-1">
              <div class="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden flex">
                <div class="bg-green-500 h-full" :style="`width:${pct(detailModal.passCount, detailModal.totalItems)}%`"/>
                <div class="bg-red-400 h-full" :style="`width:${pct(detailModal.failCount, detailModal.totalItems)}%`"/>
                <div class="bg-gray-300 h-full" :style="`width:${pct(detailModal.naCount, detailModal.totalItems)}%`"/>
              </div>
              <span class="text-xs text-gray-500 flex-shrink-0">
                {{ detailModal.totalItems - detailModal.notCheckedCount }}/{{ detailModal.totalItems }}
              </span>
            </div>
            <div class="flex gap-4 text-xs">
              <span class="text-green-600 font-medium">통과 {{ detailModal.passCount }}</span>
              <span class="text-red-500 font-medium">미흡 {{ detailModal.failCount }}</span>
              <span class="text-gray-400">해당없음 {{ detailModal.naCount }}</span>
              <span class="text-gray-300">미점검 {{ detailModal.notCheckedCount }}</span>
            </div>
          </div>
          <!-- Results -->
          <div class="flex-1 overflow-y-auto p-6">
            <div v-if="detailModal.loading" class="text-center py-8 text-gray-400 text-sm">로딩 중...</div>
            <div v-else-if="detailModal.groupedResults.length === 0" class="text-center py-8 text-gray-400 text-sm">점검 결과가 없습니다</div>
            <div v-else class="space-y-5">
              <div v-for="group in detailModal.groupedResults" :key="group.category">
                <div class="flex items-center gap-2 mb-2">
                  <span class="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-indigo-100 text-indigo-700">{{ group.category }}</span>
                </div>
                <div class="divide-y divide-gray-50 border border-gray-100 rounded-xl overflow-hidden">
                  <div v-for="r in group.items" :key="r.checkItemId"
                    class="flex items-start gap-3 py-2.5 px-3 bg-white hover:bg-gray-50">
                    <div class="flex-1 min-w-0">
                      <p class="text-sm text-gray-800">{{ r.checkItemName }}</p>
                      <p v-if="r.notes && r.result === 'FAIL'" class="text-xs text-red-500 mt-0.5">{{ r.notes }}</p>
                    </div>
                    <span class="flex-shrink-0 text-[11px] font-semibold px-2 py-0.5 rounded-full"
                      :class="resultClass(r.result)">
                      {{ resultLabel(r.result) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <p v-if="detailModal.check?.notes" class="mt-4 p-3 bg-gray-50 rounded-xl text-xs text-gray-600">
              <span class="font-medium">비고:</span> {{ detailModal.check.notes }}
            </p>
          </div>
          <!-- Footer -->
          <div class="flex gap-3 px-6 py-4 border-t border-gray-100">
            <button @click="detailModal.open = false"
              class="flex-1 py-2.5 rounded-xl border-2 border-gray-200 text-sm font-semibold text-gray-600 hover:bg-gray-50 transition-all">
              닫기
            </button>
            <button @click="goToContractorCheck()"
              class="flex-1 py-2.5 rounded-xl bg-primary-500 text-sm font-semibold text-white hover:bg-primary-600 transition-all flex items-center justify-center gap-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
              수탁사 점검에서 결과 수정
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ─── Check Modal (점검 등록/수정) ─── -->
    <Transition name="fade">
      <div v-if="checkModal.open" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="checkModal.open = false"/>
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
          <h3 class="text-lg font-bold text-gray-900 mb-5">
            {{ checkModal.isEdit ? '점검 정보 수정' : '점검 등록' }}
          </h3>
          <div class="space-y-4">
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">점검 연도 *</label>
              <select v-model.number="checkModal.form.checkYear" :disabled="checkModal.isEdit"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 disabled:bg-gray-50 disabled:text-gray-400">
                <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
              </select>
              <p v-if="!checkModal.isEdit" class="text-xs text-gray-400 mt-1">
                동일 연도·수탁사 조합이 이미 있으면 해당 점검을 불러옵니다.
              </p>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">점검일</label>
              <input v-model="checkModal.form.checkDate" type="date"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">점검자</label>
              <input v-model="checkModal.form.inspector" type="text" placeholder="점검자명"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">상태</label>
              <select v-model="checkModal.form.status"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400">
                <option value="PLANNED">계획</option>
                <option value="IN_PROGRESS">진행중</option>
                <option value="COMPLETED">완료</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">비고</label>
              <textarea v-model="checkModal.form.notes" rows="2" placeholder="비고"
                class="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 resize-none"/>
            </div>
          </div>
          <div class="flex gap-3 mt-6">
            <button @click="checkModal.open = false"
              class="flex-1 py-2.5 rounded-xl border-2 border-gray-200 text-sm font-semibold text-gray-600 hover:bg-gray-50 transition-all">
              취소
            </button>
            <button @click="saveCheck" :disabled="saving"
              class="flex-1 py-2.5 rounded-xl bg-primary-500 text-sm font-semibold text-white hover:bg-primary-600 disabled:opacity-60 transition-all">
              {{ saving ? '저장 중...' : (checkModal.isEdit ? '저장' : '등록') }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ─── Import Modal (개인정보처리방침에서 수탁사 일괄등록) ─── -->
    <Transition name="fade">
      <div v-if="importModal.open" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeImportModal"/>
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-5xl max-h-[92vh] flex flex-col">
          <!-- Header -->
          <div class="flex items-start justify-between p-6 border-b border-gray-100">
            <div>
              <h3 class="text-lg font-bold text-gray-900">개인정보처리방침에서 수탁사 일괄등록</h3>
              <p class="text-sm text-gray-500 mt-1">
                방침 페이지의 위탁 표에서 수탁사 · 위탁업무 · 재수탁사를 읽어옵니다. 내용을 확인·수정한 뒤 일괄 등록하세요.
              </p>
            </div>
            <button @click="closeImportModal" class="p-1 hover:bg-gray-100 rounded-lg transition-colors">
              <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <!-- URL bar -->
          <div class="px-6 py-4 border-b border-gray-100 bg-gray-50">
            <label class="block text-xs font-medium text-gray-600 mb-1">개인정보처리방침 URL</label>
            <div class="flex gap-2">
              <input v-model="importModal.url" type="url" placeholder="https://example.com/privacy"
                @keyup.enter="parsePolicy"
                class="flex-1 px-3 py-2 border border-gray-200 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary-400"/>
              <button @click="parsePolicy" :disabled="importModal.parsing || !importModal.url.trim()"
                class="px-4 py-2 rounded-xl bg-primary-500 text-sm font-semibold text-white hover:bg-primary-600 disabled:opacity-60 transition-all whitespace-nowrap">
                {{ importModal.parsing ? '불러오는 중...' : '불러오기' }}
              </button>
            </div>
            <p v-if="importModal.error" class="mt-2 text-xs text-red-600">{{ importModal.error }}</p>
            <p v-else-if="importModal.parsed" class="mt-2 text-xs text-gray-500">
              표 {{ importModal.tableCount }}개에서 {{ importModal.rows.length }}건을 읽었습니다.
              <span v-if="existingCount > 0" class="text-amber-600">이미 등록된 수탁사 {{ existingCount }}건은 선택에서 제외했습니다.</span>
            </p>
          </div>

          <!-- Rows -->
          <div class="flex-1 overflow-y-auto px-6 py-4">
            <div v-if="importModal.parsing" class="text-center py-16 text-gray-400 text-sm">
              개인정보처리방침을 분석하고 있습니다...
            </div>
            <div v-else-if="!importModal.parsed" class="text-center py-16 text-gray-400 text-sm">
              URL을 입력하고 "불러오기"를 누르세요.
            </div>
            <div v-else-if="importModal.rows.length === 0" class="text-center py-16 text-gray-400 text-sm">
              추출된 수탁사가 없습니다.
            </div>
            <table v-else class="w-full text-sm">
              <thead>
                <tr class="text-left text-xs text-gray-500 border-b border-gray-200">
                  <th class="py-2 pr-2 w-10">
                    <input type="checkbox" :checked="allSelected" @change="toggleAll($event.target.checked)"
                      class="rounded border-gray-300 text-primary-500 focus:ring-primary-400"/>
                  </th>
                  <th class="py-2 pr-3 font-medium w-1/4">수탁사 *</th>
                  <th class="py-2 pr-3 font-medium">위탁업무</th>
                  <th class="py-2 pr-3 font-medium w-1/4">재수탁사</th>
                  <th class="py-2 w-10"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in importModal.rows" :key="i"
                  class="border-b border-gray-50 align-top"
                  :class="row.existing ? 'bg-amber-50/50' : ''">
                  <td class="py-2 pr-2">
                    <input type="checkbox" v-model="row.selected"
                      class="mt-2 rounded border-gray-300 text-primary-500 focus:ring-primary-400"/>
                  </td>
                  <td class="py-2 pr-3">
                    <input v-model="row.name" type="text" placeholder="수탁사명"
                      class="w-full px-2 py-1.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
                    <p v-if="row.existing" class="text-[11px] text-amber-600 mt-1">이미 등록된 수탁사</p>
                  </td>
                  <td class="py-2 pr-3">
                    <textarea v-model="row.serviceType" rows="2" placeholder="위탁업무"
                      class="w-full px-2 py-1.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 resize-y"/>
                  </td>
                  <td class="py-2 pr-3">
                    <input v-model="row.subContractor" type="text" placeholder="—"
                      class="w-full px-2 py-1.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"/>
                  </td>
                  <td class="py-2">
                    <button @click="importModal.rows.splice(i, 1)" title="행 삭제"
                      class="p-1.5 text-gray-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M4 7h16M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3"/>
                      </svg>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Footer -->
          <div class="flex items-center gap-3 px-6 py-4 border-t border-gray-100">
            <p class="flex-1 text-sm text-gray-500">
              <span v-if="importModal.parsed">선택 <span class="font-semibold text-gray-800">{{ selectedRows.length }}</span>건 / 전체 {{ importModal.rows.length }}건</span>
            </p>
            <button @click="closeImportModal"
              class="px-5 py-2.5 rounded-xl border-2 border-gray-200 text-sm font-semibold text-gray-600 hover:bg-gray-50 transition-all">
              취소
            </button>
            <button @click="bulkRegister" :disabled="importModal.importing || selectedRows.length === 0"
              class="px-5 py-2.5 rounded-xl bg-primary-500 text-sm font-semibold text-white hover:bg-primary-600 disabled:opacity-60 transition-all">
              {{ importModal.importing ? '등록 중...' : `선택한 ${selectedRows.length}건 일괄등록` }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { contractorApi, contractorCheckApi } from '@/api'

const router = useRouter()

const contractors = ref([])
const loading = ref(false)
const saving = ref(false)
const selected = ref(null)
const contractorChecks = ref([])
const loadingChecks = ref(false)
const searchName = ref('')
const filterStatus = ref('')

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 6 }, (_, i) => currentYear + 1 - i)

const filteredContractors = computed(() => {
  return contractors.value.filter(c => {
    const matchName = !searchName.value || c.name.includes(searchName.value)
    const matchStatus = !filterStatus.value || c.status === filterStatus.value
    return matchName && matchStatus
  })
})

async function fetchContractors() {
  loading.value = true
  try {
    const data = await api.get('/privacy/contractors')
    contractors.value = data.data || []
    if (selected.value) {
      const refreshed = contractors.value.find(c => c.id === selected.value.id)
      if (refreshed) selected.value = refreshed
    }
  } finally {
    loading.value = false
  }
}

async function selectContractor(c) {
  selected.value = c
  await loadContractorChecks(c.id)
}

async function loadContractorChecks(contractorId) {
  loadingChecks.value = true
  contractorChecks.value = []
  try {
    const res = await contractorCheckApi.listByContractor(contractorId)
    contractorChecks.value = res.data ?? res
  } catch (e) {
    console.error('점검 이력 로드 실패', e)
  } finally {
    loadingChecks.value = false
  }
}

// ── Contractor Modal ───────────────────────────────────────────────

const contractorModal = reactive({
  open: false, isEdit: false, editId: null,
  form: { name: '', businessNumber: '', representative: '', serviceType: '', subContractor: '', contractStart: '', contractEnd: '', contactPerson: '', contactEmail: '', contactPhone: '', status: 'ACTIVE', notes: '' }
})

function openContractorModal(c) {
  contractorModal.isEdit = !!c
  contractorModal.editId = c?.id ?? null
  contractorModal.form = {
    name: c?.name ?? '',
    businessNumber: c?.businessNumber ?? '',
    representative: c?.representative ?? '',
    serviceType: c?.serviceType ?? '',
    subContractor: c?.subContractor ?? '',
    contractStart: c?.contractStart ?? '',
    contractEnd: c?.contractEnd ?? '',
    contactPerson: c?.contactPerson ?? '',
    contactEmail: c?.contactEmail ?? '',
    contactPhone: c?.contactPhone ?? '',
    status: c?.status ?? 'ACTIVE',
    notes: c?.notes ?? ''
  }
  contractorModal.open = true
}

async function saveContractor() {
  if (!contractorModal.form.name.trim()) { alert('수탁사명을 입력하세요.'); return }
  saving.value = true
  try {
    const payload = { ...contractorModal.form }
    if (!payload.contractStart) delete payload.contractStart
    if (!payload.contractEnd) delete payload.contractEnd
    if (contractorModal.isEdit) {
      await api.patch(`/privacy/contractors/${contractorModal.editId}`, payload)
    } else {
      await api.post('/privacy/contractors', payload)
    }
    contractorModal.open = false
    await fetchContractors()
  } finally {
    saving.value = false
  }
}

async function confirmDeleteContractor(c) {
  if (!confirm(`"${c.name}" 수탁사를 삭제하시겠습니까?\n모든 점검 이력이 함께 삭제됩니다.`)) return
  await api.delete(`/privacy/contractors/${c.id}`)
  selected.value = null
  contractorChecks.value = []
  await fetchContractors()
}

// ── Import Modal (개인정보처리방침 → 수탁사 일괄등록) ────────────────

const importModal = reactive({
  open: false,
  url: '',
  parsing: false,
  importing: false,
  parsed: false,
  error: '',
  tableCount: 0,
  rows: []   // { name, serviceType, subContractor, existing, selected }
})

const selectedRows = computed(() => importModal.rows.filter(r => r.selected && r.name.trim()))
const existingCount = computed(() => importModal.rows.filter(r => r.existing).length)
const allSelected = computed(() =>
  importModal.rows.length > 0 && importModal.rows.every(r => r.selected))

function openImportModal() {
  importModal.open = true
  importModal.url = ''
  importModal.parsing = false
  importModal.importing = false
  importModal.parsed = false
  importModal.error = ''
  importModal.tableCount = 0
  importModal.rows = []
}

function closeImportModal() {
  if (importModal.parsing || importModal.importing) return
  importModal.open = false
}

function toggleAll(checked) {
  importModal.rows.forEach(r => { r.selected = checked })
}

async function parsePolicy() {
  const url = importModal.url.trim()
  if (!url) return
  importModal.parsing = true
  importModal.error = ''
  importModal.parsed = false
  importModal.rows = []
  try {
    const res = await contractorApi.parsePolicy(url)
    const data = res.data ?? res
    importModal.tableCount = data.tableCount ?? 0
    importModal.rows = (data.items ?? []).map(i => ({
      name: i.name ?? '',
      serviceType: i.serviceType ?? '',
      subContractor: i.subContractor ?? '',
      existing: !!i.existing,
      selected: !i.existing        // 이미 등록된 수탁사는 기본 선택 해제
    }))
    importModal.parsed = true
  } catch (e) {
    importModal.error = typeof e === 'string' ? e : (e?.message || '개인정보처리방침을 불러오지 못했습니다.')
  } finally {
    importModal.parsing = false
  }
}

async function bulkRegister() {
  const items = selectedRows.value.map(r => ({
    name: r.name.trim(),
    serviceType: r.serviceType?.trim() || null,
    subContractor: r.subContractor?.trim() || null,
    status: 'ACTIVE'
  }))
  if (items.length === 0) return
  importModal.importing = true
  try {
    const res = await contractorApi.bulkCreate(items)
    const data = res.data ?? res
    let msg = `${data.created}건을 등록했습니다.`
    if (data.skipped > 0) {
      msg += `\n이미 등록되어 건너뛴 ${data.skipped}건: ${data.skippedNames.join(', ')}`
    }
    alert(msg)
    importModal.open = false
    await fetchContractors()
  } catch (e) {
    alert(e || '일괄 등록에 실패했습니다.')
  } finally {
    importModal.importing = false
  }
}

// ── Check Detail Modal (결과 보기) ─────────────────────────────────

const detailModal = reactive({
  open: false,
  loading: false,
  check: null,
  groupedResults: [],
  totalItems: 0,
  passCount: 0,
  failCount: 0,
  naCount: 0,
  notCheckedCount: 0
})

async function openDetailModal(chk) {
  detailModal.open = true
  detailModal.loading = true
  detailModal.check = chk
  detailModal.groupedResults = []
  detailModal.totalItems = chk.totalItems ?? 0
  detailModal.passCount = chk.passCount ?? 0
  detailModal.failCount = chk.failCount ?? 0
  detailModal.naCount = chk.naCount ?? 0
  detailModal.notCheckedCount = chk.notCheckedCount ?? 0
  try {
    const res = await contractorCheckApi.getDetail(chk.id)
    const detail = res.data ?? res
    detailModal.check = detail.check ?? chk
    const results = detail.results ?? []
    // Recompute stats from actual results
    detailModal.totalItems = results.length
    detailModal.passCount = results.filter(r => r.result === 'PASS').length
    detailModal.failCount = results.filter(r => r.result === 'FAIL').length
    detailModal.naCount = results.filter(r => r.result === 'NA').length
    detailModal.notCheckedCount = results.filter(r => r.result === 'NOT_CHECKED').length
    // Group by category
    const map = {}
    for (const r of results) {
      const cat = r.checkItemCategory || '기타'
      if (!map[cat]) map[cat] = []
      map[cat].push(r)
    }
    detailModal.groupedResults = Object.entries(map).map(([category, items]) => ({ category, items }))
  } catch (e) {
    console.error('점검 결과 로드 실패', e)
  } finally {
    detailModal.loading = false
  }
}

function resultLabel(result) {
  return { PASS: '통과', FAIL: '미흡', NA: '해당없음', NOT_CHECKED: '미점검' }[result] || result
}

function resultClass(result) {
  return {
    PASS: 'bg-green-100 text-green-700',
    FAIL: 'bg-red-100 text-red-600',
    NA: 'bg-gray-100 text-gray-500',
    NOT_CHECKED: 'bg-gray-50 text-gray-300'
  }[result] || 'bg-gray-100 text-gray-500'
}

function pct(count, total) {
  return total > 0 ? ((count / total) * 100).toFixed(1) : 0
}

// ── Check Modal (점검 등록/수정) ────────────────────────────────────

const checkModal = reactive({
  open: false, isEdit: false, editId: null,
  form: { checkYear: currentYear, checkDate: '', inspector: '', status: 'PLANNED', notes: '' }
})

function openCheckModal(chk) {
  checkModal.isEdit = !!chk
  checkModal.editId = chk?.id ?? null
  checkModal.form = {
    checkYear: chk?.checkYear ?? currentYear,
    checkDate: chk?.checkDate ?? '',
    inspector: chk?.inspector ?? '',
    status: chk?.status ?? 'PLANNED',
    notes: chk?.notes ?? ''
  }
  checkModal.open = true
}

async function saveCheck() {
  saving.value = true
  try {
    const payload = {
      contractorId: selected.value.id,
      checkYear: checkModal.form.checkYear,
      checkDate: checkModal.form.checkDate || null,
      inspector: checkModal.form.inspector || null,
      status: checkModal.form.status,
      notes: checkModal.form.notes || null
    }
    let savedId = null
    if (checkModal.isEdit) {
      await contractorCheckApi.update(checkModal.editId, payload)
    } else {
      const res = await contractorCheckApi.create(payload)
      savedId = (res.data ?? res)?.id
    }
    checkModal.open = false
    await loadContractorChecks(selected.value.id)
    fetchContractors()  // 목록 카드의 점검 건수 갱신
    // 등록 후 결과 보기 모달 자동 오픈
    if (savedId) {
      const newChk = contractorChecks.value.find(c => c.id === savedId)
      if (newChk) openDetailModal(newChk)
    }
  } catch (e) {
    alert(e || '저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function confirmDeleteCheck(chk) {
  if (!confirm(`${chk.checkYear}년 점검 이력을 삭제하시겠습니까?\n점검 결과가 모두 삭제됩니다.`)) return
  try {
    await contractorCheckApi.delete(chk.id)
    await loadContractorChecks(selected.value.id)
    fetchContractors()  // 목록 카드의 점검 건수 갱신
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  }
}

function goToContractorCheck() {
  router.push('/privacy/contractor-checks')
}

// ── Helpers ────────────────────────────────────────────────────────

function checkStatusLabel(status) {
  return { PLANNED: '계획', IN_PROGRESS: '진행중', COMPLETED: '완료' }[status] || status
}

function checkStatusClass(status) {
  return {
    PLANNED: 'bg-blue-100 text-blue-700',
    IN_PROGRESS: 'bg-yellow-100 text-yellow-700',
    COMPLETED: 'bg-green-100 text-green-700'
  }[status] || 'bg-gray-100 text-gray-600'
}

onMounted(fetchContractors)
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
