<template>
  <div class="p-6">
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('isms.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('isms.subtitle') }}</p>
      </div>
      <div class="flex items-center flex-wrap gap-2 sm:gap-3">
        <div class="flex items-center gap-2 bg-white border border-gray-300 rounded-lg px-3 py-1.5">
          <button @click="prevYear" class="text-gray-400 hover:text-gray-700 px-1" title="이전 연도">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <span class="text-base font-bold text-gray-800 min-w-16 text-center">{{ selectedYear }}{{ $t('isms.year') }}</span>
          <button @click="nextYear" class="text-gray-400 hover:text-gray-700 px-1" title="다음 연도">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
        <button @click="openCopyPrev" :disabled="!copySourceYear || copyLoading"
          :title="copySourceYear ? `${copySourceYear}년 증적을 ${selectedYear}년으로 복사합니다` : '가져올 이전 연도 증적이 없습니다'"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
          {{ copyLoading ? $t('common.loading') : '전년도 증적 가져오기' }}
        </button>
        <button @click="openRevertCopy" :disabled="!copiedCount || revertLoading"
          :title="copiedCount ? `가져온 증적 ${copiedCount}건을 삭제하고 가져오기 전 상태로 되돌립니다` : '되돌릴 가져오기 내역이 없습니다'"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 10h10a5 5 0 015 5v1M3 10l4-4M3 10l4 4"/>
          </svg>
          {{ revertLoading ? $t('common.loading') : '가져오기 초기화' }}
        </button>
        <button @click="downloadCsv" :disabled="csvLoading"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          {{ csvLoading ? $t('common.loading') : $t('isms.downloadCsv') }}
        </button>
        <button @click="downloadPdf" :disabled="pdfLoading"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          {{ pdfLoading ? $t('common.loading') : $t('isms.downloadPdf') }}
        </button>
        <button @click="showImportModal = true"
          class="flex items-center gap-1.5 px-3 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/>
          </svg>
          {{ $t('isms.bulkImport') }}
        </button>
      </div>
    </div>

    <!-- 전년도 증적 가져오기 결과 -->
    <div v-if="copyResult"
      class="mb-4 flex items-start gap-2 px-4 py-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-800">
      <svg class="w-4 h-4 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
      </svg>
      <span>
        {{ copyResult.sourceYear }}년 → {{ copyResult.targetYear }}년 복사 완료 —
        증적 {{ copyResult.copiedEvidences }}건, 현재상태·의견 {{ copyResult.copiedNotes }}건
        <template v-if="copyResult.skippedItems">
          (이미 증적이 있는 {{ copyResult.skippedItems }}개 항목은 건너뜀)
        </template>
      </span>
      <button @click="copyResult = null" class="ml-auto text-green-500 hover:text-green-700">✕</button>
    </div>
    <div v-if="revertResult"
      class="mb-4 flex items-start gap-2 px-4 py-3 bg-gray-50 border border-gray-200 rounded-lg text-sm text-gray-700">
      <svg class="w-4 h-4 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a5 5 0 015 5v1M3 10l4-4M3 10l4 4"/>
      </svg>
      <span>
        {{ revertResult.targetYear }}년 가져오기 초기화 완료 —
        증적 {{ revertResult.removedEvidences }}건, 현재상태·의견 {{ revertResult.removedNotes }}건 삭제
        <template v-if="revertResult.removedReferences">
          (가져온 증적을 참조하던 증적 {{ revertResult.removedReferences }}건 포함)
        </template>
      </span>
      <button @click="revertResult = null" class="ml-auto text-gray-400 hover:text-gray-600">✕</button>
    </div>
    <div v-if="copyError"
      class="mb-4 flex items-start gap-2 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
      <svg class="w-4 h-4 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <span>{{ copyError }}</span>
      <button @click="copyError = ''" class="ml-auto text-red-400 hover:text-red-600">✕</button>
    </div>

    <!-- Summary Cards -->
    <div v-if="summary" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 sm:gap-4 mb-6">
      <div class="bg-white rounded-lg border p-4 text-center">
        <p class="text-2xl font-bold text-gray-900">{{ summary.totalItems }}</p>
        <p class="text-xs text-gray-500 mt-1">{{ $t('isms.totalItems') }}</p>
      </div>
      <div class="bg-green-50 rounded-lg border border-green-200 p-4 text-center">
        <p class="text-2xl font-bold text-green-700">{{ summary.compliant }}</p>
        <p class="text-xs text-green-600 mt-1">{{ $t('isms.statusCompliant') }}</p>
      </div>
      <div class="bg-yellow-50 rounded-lg border border-yellow-200 p-4 text-center">
        <p class="text-2xl font-bold text-yellow-700">{{ summary.partial }}</p>
        <p class="text-xs text-yellow-600 mt-1">{{ $t('isms.statusPartial') }}</p>
      </div>
      <div class="bg-red-50 rounded-lg border border-red-200 p-4 text-center">
        <p class="text-2xl font-bold text-red-700">{{ summary.nonCompliant }}</p>
        <p class="text-xs text-red-600 mt-1">{{ $t('isms.statusNonCompliant') }}</p>
      </div>
      <div class="bg-gray-50 rounded-lg border p-4 text-center">
        <p class="text-2xl font-bold text-gray-500">{{ summary.noEvidence }}</p>
        <p class="text-xs text-gray-500 mt-1">{{ $t('isms.noEvidence') }}</p>
      </div>
    </div>

    <!-- 검색 필터 -->
    <div class="flex flex-wrap items-center gap-3 mb-4">
      <div class="relative flex-1 min-w-56">
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none"
          fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
        </svg>
        <input v-model="searchKeyword" type="text" :placeholder="`${$t('isms.itemCode')} · ${$t('isms.itemName')} 검색...`"
          class="w-full border border-gray-300 rounded-lg pl-9 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"/>
      </div>
      <select v-model="statusFilter"
        class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
        <option value="">{{ $t('common.status') }}: {{ $t('common.all') }}</option>
        <option value="COMPLIANT">{{ $t('isms.statusCompliant') }}</option>
        <option value="PARTIAL">{{ $t('isms.statusPartial') }}</option>
        <option value="NON_COMPLIANT">{{ $t('isms.statusNonCompliant') }}</option>
        <option value="NA">N/A</option>
        <option value="NONE">{{ $t('isms.noEvidence') }}</option>
      </select>
      <button v-if="searchKeyword || statusFilter" @click="searchKeyword = ''; statusFilter = ''"
        class="px-3 py-2 text-sm text-gray-600 border border-gray-300 rounded-lg hover:bg-gray-50">초기화</button>
      <span class="text-sm text-gray-500">{{ filteredItems.length }}건</span>
    </div>

    <!-- Domain Filter Tabs -->
    <div class="bg-white rounded-xl shadow-sm border mb-4">
      <div class="flex flex-wrap border-b overflow-x-auto">
        <button
          :class="['px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors',
            selectedDomain === '' ? 'border-b-2 border-primary-600 text-primary-700' : 'text-gray-500 hover:text-gray-700']"
          @click="selectDomain('')">
          {{ $t('common.all') }}
        </button>
        <button v-for="domain in domains" :key="domain.code"
          :class="['px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors',
            selectedDomain === domain.code ? 'border-b-2 border-primary-600 text-primary-700' : 'text-gray-500 hover:text-gray-700']"
          @click="selectDomain(domain.code)">
          {{ domain.code }} {{ domain.name }}
        </button>
      </div>

      <!-- Items Table -->
      <div v-if="loading" class="py-12 text-center text-gray-500">{{ $t('common.loading') }}</div>
      <table v-else class="w-full">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
              {{ $t('isms.itemCode') }}
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {{ $t('isms.itemName') }}
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-40 hidden md:table-cell">
              {{ $t('isms.domain') }}
            </th>
            <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
              {{ $t('isms.evidenceCount') }}
            </th>
            <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider w-32">
              {{ $t('common.status') }}
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-if="filteredItems.length === 0">
            <td colspan="5" class="px-6 py-8 text-center text-gray-400 text-sm">{{ $t('common.noData') }}</td>
          </tr>
          <tr v-for="item in filteredItems" :key="item.id"
            class="hover:bg-gray-50 cursor-pointer transition-colors"
            @click="openItem(item)">
            <td class="px-6 py-4 font-mono text-sm font-semibold text-primary-700">{{ item.itemCode }}</td>
            <td class="px-6 py-4">
              <p class="text-sm font-medium text-gray-900">{{ item.itemName }}</p>
            </td>
            <td class="px-6 py-4 text-sm text-gray-500 hidden md:table-cell">
              {{ item.domainName }}
            </td>
            <td class="px-6 py-4 text-center">
              <span class="text-sm text-gray-700">{{ item.evidenceCount ?? 0 }}</span>
            </td>
            <td class="px-6 py-4 text-center">
              <StatusBadge :status="item.latestStatus" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <!-- 항목 증적 팝업 -->
  <IsmsEvidenceModal
    :open="evidenceOpen"
    :item-id="evidenceItem?.id"
    :item-code="evidenceItem?.itemCode"
    :item-name="evidenceItem?.itemName"
    :year="selectedYear"
    :position="evidenceIndex + 1"
    :total="filteredItems.length"
    :has-prev="evidenceIndex > 0"
    :has-next="evidenceIndex >= 0 && evidenceIndex < filteredItems.length - 1"
    @close="evidenceOpen = false"
    @changed="loadItems"
    @update:year="onModalYearChange"
    @prev="moveItem(-1)"
    @next="moveItem(1)"
  />

  <!-- 전년도 증적 가져오기 확인 모달 -->
  <div v-if="showCopyConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md">
      <div class="flex items-center justify-between p-5 border-b">
        <h2 class="text-lg font-semibold text-gray-900">전년도 증적 가져오기</h2>
        <button @click="showCopyConfirm = false" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="p-5 space-y-3 text-sm text-gray-600">
        <p>
          <span class="font-semibold text-gray-900">{{ copySourceYear }}년</span> 증적을
          <span class="font-semibold text-gray-900">{{ selectedYear }}년</span>으로 복사합니다.
        </p>
        <ul class="list-disc pl-5 space-y-1 text-gray-500">
          <li>증적제목 · 증적내용 · 준수상태 · 첨부파일을 그대로 가져옵니다.</li>
          <li>연도별 현재 상태 · 의견도 함께 가져옵니다.</li>
          <li>{{ selectedYear }}년에 이미 증적이 등록된 항목은 건너뜁니다.</li>
        </ul>
      </div>
      <div class="flex justify-end gap-3 px-5 py-4 border-t bg-gray-50 rounded-b-xl">
        <button @click="showCopyConfirm = false"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ $t('common.cancel') }}
        </button>
        <button @click="copyPrevYear" :disabled="copyLoading"
          class="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors">
          {{ copyLoading ? $t('common.loading') : '가져오기' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 가져오기 초기화 확인 모달 -->
  <div v-if="showRevertConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md">
      <div class="flex items-center justify-between p-5 border-b">
        <h2 class="text-lg font-semibold text-gray-900">가져오기 초기화</h2>
        <button @click="showRevertConfirm = false" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="p-5 space-y-3 text-sm text-gray-600">
        <p>
          <span class="font-semibold text-gray-900">{{ selectedYear }}년</span>을
          <span class="font-semibold text-gray-900">전년도 증적 가져오기 전 상태</span>로 되돌립니다.
        </p>
        <p class="px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg">
          삭제 대상: <span class="font-semibold text-gray-900">증적 {{ copiedCount }}건</span>
          <template v-if="copiedNoteCount">, 현재상태·의견 {{ copiedNoteCount }}건</template>
          <template v-if="copiedFromYear"> (원본 {{ copiedFromYear }}년)</template>
        </p>
        <ul class="list-disc pl-5 space-y-1 text-gray-500">
          <li>직접 등록·작성한 증적과 의견은 <span class="font-medium text-gray-700">삭제되지 않습니다</span>.</li>
          <li>가져온 뒤 수정한 내용도 함께 삭제되니 주의하세요.</li>
          <li>원본 연도({{ copiedFromYear || '이전 연도' }})의 증적과 첨부파일은 그대로 유지됩니다.</li>
        </ul>
      </div>
      <div class="flex justify-end gap-3 px-5 py-4 border-t bg-gray-50 rounded-b-xl">
        <button @click="showRevertConfirm = false"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ $t('common.cancel') }}
        </button>
        <button @click="revertCopy" :disabled="revertLoading"
          class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors">
          {{ revertLoading ? $t('common.loading') : '초기화' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 일괄 등록 모달 -->
  <div v-if="showImportModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-lg">
      <div class="flex items-center justify-between p-5 border-b">
        <h2 class="text-lg font-semibold text-gray-900">{{ $t('isms.importTitle') }}</h2>
        <button @click="closeImportModal" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="p-5 space-y-5">
        <p class="text-sm text-gray-500">{{ $t('isms.importDesc') }}</p>

        <!-- 템플릿 다운로드 -->
        <div class="flex items-center justify-between p-3 bg-blue-50 rounded-lg border border-blue-100">
          <div class="text-sm text-blue-800">
            <p class="font-medium">엑셀 템플릿</p>
            <p class="text-xs text-blue-600 mt-0.5">항목코드 · 증적제목 · 증적내용 · 이행가이드 · 파일명 · 준수상태</p>
          </div>
          <button @click="downloadTemplate"
            class="text-sm text-blue-700 border border-blue-300 px-3 py-1.5 rounded-lg hover:bg-blue-100 transition-colors">
            {{ $t('isms.downloadTemplate') }}
          </button>
        </div>

        <!-- 연도 선택 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('isms.year') }}</label>
          <div class="inline-flex items-center gap-2 bg-white border border-gray-300 rounded-lg px-3 py-1.5">
            <button type="button" @click="importYear--" class="text-gray-400 hover:text-gray-700 px-1" title="이전 연도">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
              </svg>
            </button>
            <span class="text-base font-bold text-gray-800 min-w-16 text-center">{{ importYear }}{{ $t('isms.year') }}</span>
            <button type="button" @click="importYear++" class="text-gray-400 hover:text-gray-700 px-1" title="다음 연도">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 파일 선택 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('isms.selectFile') }}</label>
          <input type="file" accept=".xlsx,.csv" @change="onFileChange" ref="fileInputRef"
            class="block w-full text-sm text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-sm file:font-medium file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100"/>
          <p v-if="importFile" class="mt-1 text-xs text-gray-500">{{ importFile.name }}</p>
        </div>

        <!-- 결과 -->
        <div v-if="importResult" class="rounded-lg border overflow-hidden">
          <div class="grid grid-cols-3 divide-x bg-gray-50 text-center text-sm font-medium">
            <div class="p-3">
              <p class="text-gray-500 text-xs">{{ $t('isms.importTotal') }}</p>
              <p class="text-xl font-bold text-gray-800 mt-0.5">{{ importResult.total }}</p>
            </div>
            <div class="p-3">
              <p class="text-green-600 text-xs">{{ $t('isms.importSuccess') }}</p>
              <p class="text-xl font-bold text-green-700 mt-0.5">{{ importResult.success }}</p>
            </div>
            <div class="p-3">
              <p class="text-red-500 text-xs">{{ $t('isms.importFailed') }}</p>
              <p class="text-xl font-bold text-red-600 mt-0.5">{{ importResult.failed }}</p>
            </div>
          </div>
          <div v-if="importResult.errors?.length" class="max-h-40 overflow-y-auto border-t">
            <table class="w-full text-xs">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-3 py-2 text-left text-gray-500">{{ $t('isms.importErrorRow') }}</th>
                  <th class="px-3 py-2 text-left text-gray-500">항목코드</th>
                  <th class="px-3 py-2 text-left text-gray-500">오류 내용</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="err in importResult.errors" :key="err.row" class="text-red-600">
                  <td class="px-3 py-2">{{ err.row }}</td>
                  <td class="px-3 py-2 font-mono">{{ err.itemCode }}</td>
                  <td class="px-3 py-2">{{ err.message }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <p v-if="importError" class="text-sm text-red-600">{{ importError }}</p>
      </div>

      <div class="flex justify-end gap-3 px-5 py-4 border-t bg-gray-50 rounded-b-xl">
        <button @click="closeImportModal"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ importResult ? $t('common.confirm') : $t('common.cancel') }}
        </button>
        <button v-if="!importResult" @click="startImport" :disabled="importLoading || !importFile"
          class="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors">
          {{ importLoading ? $t('common.loading') : $t('isms.startImport') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ismsApi, exportApi } from '@/api'
import StatusBadge from './IsmsStatusBadge.vue'
import IsmsEvidenceModal from './IsmsEvidenceModal.vue'

// 항목 클릭 시 상세 페이지로 이동하지 않고 팝업으로 연다.
const evidenceOpen = ref(false)
const evidenceItem = ref(null)

function openItem(item) {
  evidenceItem.value = item
  evidenceOpen.value = true
}

// 팝업에서 현재 필터가 걸린 목록 순서대로 앞뒤 항목으로 이동한다.
const evidenceIndex = computed(() =>
  evidenceItem.value ? filteredItems.value.findIndex(i => i.id === evidenceItem.value.id) : -1)

function moveItem(step) {
  const next = evidenceIndex.value + step
  if (evidenceIndex.value < 0 || next < 0 || next >= filteredItems.value.length) return
  evidenceItem.value = filteredItems.value[next]
}

// 팝업 안에서 연도를 바꾸면 목록의 연도도 맞춰 다시 읽는다.
function onModalYearChange(year) {
  selectedYear.value = year
  loadItems()
}

const route = useRoute()

const selectedYear = ref(new Date().getFullYear())
// 대시보드 ISMS-P 이행률에서 도메인을 클릭해 들어오면 해당 도메인 탭이 선택된 상태로 연다.
const selectedDomain = ref(typeof route.query.domain === 'string' ? route.query.domain : '')
const searchKeyword = ref('')
const statusFilter = ref('')
const items = ref([])
const summary = ref(null)
const loading = ref(false)

// CSV / PDF 다운로드
const csvLoading = ref(false)
const pdfLoading = ref(false)

// 전년도 증적 가져오기 — copySourceYear 는 선택 연도 이전에 증적이 있는 가장 최근 연도,
// copiedCount 는 선택 연도에 남아 있는 '가져오기로 생성된' 증적 건수(초기화 대상)
const copySourceYear = ref(null)
const copiedCount = ref(0)
const copiedNoteCount = ref(0)
const copiedFromYear = ref(null)
const showCopyConfirm = ref(false)
const copyLoading = ref(false)
const copyResult = ref(null)
const copyError = ref('')

// 가져오기 초기화
const showRevertConfirm = ref(false)
const revertLoading = ref(false)
const revertResult = ref(null)

// 일괄등록 모달
const showImportModal = ref(false)
const importYear = ref(new Date().getFullYear())
const importFile = ref(null)
const importLoading = ref(false)
const importResult = ref(null)
const importError = ref('')
const fileInputRef = ref(null)

const domains = computed(() => {
  const map = new Map()
  for (const item of items.value) {
    if (!map.has(item.domainCode)) {
      map.set(item.domainCode, { code: item.domainCode, name: item.domainName })
    }
  }
  return [...map.values()]
})

const filteredItems = computed(() => {
  let list = items.value
  if (selectedDomain.value) list = list.filter(i => i.domainCode === selectedDomain.value)
  // 표시 항목(항목코드·항목명·도메인) 기준 키워드 검색
  const kw = searchKeyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(i =>
      (i.itemCode || '').toLowerCase().includes(kw) ||
      (i.itemName || '').toLowerCase().includes(kw) ||
      (i.domainName || '').toLowerCase().includes(kw))
  }
  // 준수상태 필터 (NONE = 증적없음/미제출)
  if (statusFilter.value) {
    list = statusFilter.value === 'NONE'
      ? list.filter(i => !i.latestStatus)
      : list.filter(i => i.latestStatus === statusFilter.value)
  }
  return list
})

function selectDomain(code) {
  selectedDomain.value = code
}

// 연도 전후 이동 (< 연도 >)
function prevYear() { selectedYear.value--; loadItems() }
function nextYear() { selectedYear.value++; loadItems() }

async function loadItems() {
  loading.value = true
  try {
    const [itemsData, summaryData] = await Promise.all([
      ismsApi.listItems({ year: selectedYear.value }),
      ismsApi.summary(selectedYear.value)
    ])
    items.value = itemsData.data
    summary.value = summaryData.data
  } finally {
    loading.value = false
  }
  loadCopyStatus()
}

// 선택 연도가 바뀔 때마다 가져오기/초기화 버튼 상태를 다시 확인한다.
async function loadCopyStatus() {
  try {
    const s = (await ismsApi.copyPreviousStatus(selectedYear.value)).data || {}
    copySourceYear.value = s.previousYear ?? null
    copiedCount.value = s.copiedEvidences ?? 0
    copiedNoteCount.value = s.copiedNotes ?? 0
    copiedFromYear.value = s.copiedFromYear ?? null
  } catch {
    copySourceYear.value = null
    copiedCount.value = 0
    copiedNoteCount.value = 0
    copiedFromYear.value = null
  }
}

function openCopyPrev() {
  if (!copySourceYear.value) return
  copyResult.value = null
  revertResult.value = null
  copyError.value = ''
  showCopyConfirm.value = true
}

async function copyPrevYear() {
  copyLoading.value = true
  copyError.value = ''
  try {
    const res = await ismsApi.copyPrevious(selectedYear.value)
    copyResult.value = res.data
    showCopyConfirm.value = false
    await loadItems()
  } catch (e) {
    copyError.value = typeof e === 'string' ? e : '전년도 증적을 가져오지 못했습니다.'
    showCopyConfirm.value = false
  } finally {
    copyLoading.value = false
  }
}

function openRevertCopy() {
  if (!copiedCount.value) return
  copyResult.value = null
  revertResult.value = null
  copyError.value = ''
  showRevertConfirm.value = true
}

async function revertCopy() {
  revertLoading.value = true
  copyError.value = ''
  try {
    const res = await ismsApi.revertCopyPrevious(selectedYear.value)
    revertResult.value = res.data
    showRevertConfirm.value = false
    await loadItems()
  } catch (e) {
    copyError.value = typeof e === 'string' ? e : '가져오기 초기화에 실패했습니다.'
    showRevertConfirm.value = false
  } finally {
    revertLoading.value = false
  }
}

async function downloadCsv() {
  csvLoading.value = true
  try { await ismsApi.exportCsv(selectedYear.value) } finally { csvLoading.value = false }
}

async function downloadPdf() {
  pdfLoading.value = true
  try { await exportApi.ismsPdf(selectedYear.value) } finally { pdfLoading.value = false }
}

async function downloadTemplate() {
  await ismsApi.importTemplate()
}

function onFileChange(e) {
  importFile.value = e.target.files[0] || null
  importResult.value = null
  importError.value = ''
}

async function startImport() {
  if (!importFile.value) return
  importLoading.value = true
  importError.value = ''
  importResult.value = null
  try {
    const res = await ismsApi.bulkImport(importYear.value, importFile.value)
    importResult.value = res.data
    if (res.data.success > 0) await loadItems()
  } catch (e) {
    importError.value = typeof e === 'string' ? e : '등록 중 오류가 발생했습니다.'
  } finally {
    importLoading.value = false
  }
}

function closeImportModal() {
  showImportModal.value = false
  importFile.value = null
  importResult.value = null
  importError.value = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

onMounted(loadItems)
</script>
