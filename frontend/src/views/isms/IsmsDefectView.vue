<template>
  <div class="p-6">
    <!-- 헤더 + 연도 선택 -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('ismsDefect.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('ismsDefect.subtitle') }}</p>
      </div>
      <div class="flex items-center flex-wrap gap-2 sm:gap-3">
        <div class="flex items-center gap-2 bg-white border border-gray-300 rounded-lg px-3 py-1.5">
          <button @click="changeYear(-1)" class="text-gray-400 hover:text-gray-700 px-1" title="이전 연도">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <span class="text-base font-bold text-gray-800 min-w-16 text-center">{{ selectedYear }}년</span>
          <button @click="changeYear(1)" class="text-gray-400 hover:text-gray-700 px-1" title="다음 연도">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
        <button @click="downloadCsv" :disabled="csvLoading"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          {{ csvLoading ? $t('common.loading') : 'CSV 다운로드' }}
        </button>
        <button v-if="isManager && tab === 'defects'" @click="openCreate"
          class="flex items-center gap-1.5 px-3 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          결함 등록
        </button>
      </div>
    </div>

    <!-- 알림 -->
    <div v-if="notice"
      class="mb-4 flex items-start gap-2 px-4 py-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-800">
      <svg class="w-4 h-4 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
      </svg>
      <span>{{ notice }}</span>
      <button @click="notice = ''" class="ml-auto text-green-500 hover:text-green-700">✕</button>
    </div>
    <div v-if="errorMsg"
      class="mb-4 flex items-start gap-2 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
      <svg class="w-4 h-4 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <span>{{ errorMsg }}</span>
      <button @click="errorMsg = ''" class="ml-auto text-red-400 hover:text-red-600">✕</button>
    </div>

    <!-- 요약 카드 -->
    <div v-if="summary" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 sm:gap-4 mb-6">
      <div class="bg-white rounded-lg border p-4 text-center">
        <p class="text-2xl font-bold text-gray-900">{{ summary.total }}</p>
        <p class="text-xs text-gray-500 mt-1">전체 결함</p>
      </div>
      <div class="bg-red-50 rounded-lg border border-red-200 p-4 text-center">
        <p class="text-2xl font-bold text-red-700">{{ summary.open }}</p>
        <p class="text-xs text-red-600 mt-1">미조치</p>
      </div>
      <div class="bg-yellow-50 rounded-lg border border-yellow-200 p-4 text-center">
        <p class="text-2xl font-bold text-yellow-700">{{ summary.inProgress }}</p>
        <p class="text-xs text-yellow-600 mt-1">조치중</p>
      </div>
      <div class="bg-green-50 rounded-lg border border-green-200 p-4 text-center">
        <p class="text-2xl font-bold text-green-700">{{ summary.completed }}</p>
        <p class="text-xs text-green-600 mt-1">조치완료</p>
      </div>
      <div class="bg-orange-50 rounded-lg border border-orange-200 p-4 text-center">
        <p class="text-2xl font-bold text-orange-700">{{ summary.overdue }}</p>
        <p class="text-xs text-orange-600 mt-1">기한초과</p>
      </div>
      <div class="bg-primary-50 rounded-lg border border-primary-200 p-4 text-center">
        <p class="text-2xl font-bold text-primary-700">{{ summary.completionRate }}%</p>
        <p class="text-xs text-primary-600 mt-1">조치율</p>
      </div>
    </div>

    <p class="text-xs text-gray-500 mb-3 flex items-center gap-1.5">
      <svg class="w-3.5 h-3.5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      여기에 등록한 결함은 <strong class="font-semibold text-gray-600">보안 운영 &gt; 보안 결함사항</strong>에 자동으로 반영됩니다(수정·삭제도 함께 반영).
    </p>

    <!-- 탭 -->
    <div class="flex items-center gap-1 border-b border-gray-200 mb-4">
      <button @click="tab = 'defects'"
        :class="['px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors',
                 tab === 'defects' ? 'border-primary-600 text-primary-700' : 'border-transparent text-gray-500 hover:text-gray-700']">
        결함 내역
        <span class="ml-1.5 text-xs bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">{{ defects.length }}</span>
      </button>
      <button @click="tab = 'report'"
        :class="['px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors',
                 tab === 'report' ? 'border-primary-600 text-primary-700' : 'border-transparent text-gray-500 hover:text-gray-700']">
        보고서 내용
        <span v-if="summary && summary.reportWritten"
          class="ml-1.5 text-xs bg-green-100 text-green-700 px-1.5 py-0.5 rounded">작성됨</span>
        <span v-else class="ml-1.5 text-xs bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded">미작성</span>
      </button>
    </div>

    <!-- ── 결함 내역 탭 ───────────────────────────────────────────────── -->
    <template v-if="tab === 'defects'">
      <div class="card mb-4 flex flex-wrap gap-3 items-end">
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">심사구분</label>
          <select v-model="filters.auditType" @change="fetchDefects" class="input w-32 text-sm">
            <option value="">{{ $t('common.all') }}</option>
            <option v-for="t in AUDIT_TYPES" :key="t" :value="t">{{ AUDIT_TYPE_LABEL[t] }}</option>
          </select>
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">결함구분</label>
          <select v-model="filters.defectType" @change="fetchDefects" class="input w-28 text-sm">
            <option value="">{{ $t('common.all') }}</option>
            <option v-for="t in DEFECT_TYPES" :key="t" :value="t">{{ DEFECT_TYPE_LABEL[t] }}</option>
          </select>
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">중요도</label>
          <select v-model="filters.severity" @change="fetchDefects" class="input w-28 text-sm">
            <option value="">{{ $t('common.all') }}</option>
            <option v-for="s in SEVERITIES" :key="s" :value="s">{{ SEVERITY_LABEL[s] }}</option>
          </select>
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">조치상태</label>
          <select v-model="filters.status" @change="fetchDefects" class="input w-28 text-sm">
            <option value="">{{ $t('common.all') }}</option>
            <option v-for="s in STATUSES" :key="s" :value="s">{{ STATUS_LABEL[s] }}</option>
          </select>
        </div>
        <div class="flex flex-col gap-1 flex-1 min-w-40">
          <label class="text-xs text-gray-500">{{ $t('common.search') }}</label>
          <input v-model="filters.keyword" @input="debouncedFetch" class="input text-sm"
            placeholder="결함번호, 제목, 내용, 인증기준 코드" />
        </div>
        <button @click="resetFilters" class="btn-secondary text-sm self-end">초기화</button>
      </div>

      <div class="card">
        <div v-if="loading" class="text-center py-12 text-gray-400">{{ $t('common.loading') }}</div>
        <div v-else-if="defects.length === 0" class="text-center py-12 text-gray-400">
          {{ selectedYear }}년에 등록된 결함이 없습니다.
        </div>
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b">
                <th class="text-left py-3 px-3 font-semibold text-gray-600 w-24">결함번호</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600 w-24">심사구분</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600 w-28">인증기준</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600">결함 제목</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600 w-20">구분</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600 w-20">중요도</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600 w-28">조치기한</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600 w-24">상태</th>
                <th class="py-3 px-3 w-16"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in defects" :key="d.id" class="border-b hover:bg-gray-50 cursor-pointer"
                @click="openDetail(d)">
                <td class="py-3 px-3 font-mono text-xs text-gray-700">{{ d.defectNo || '-' }}</td>
                <td class="py-3 px-3">
                  <span class="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded">
                    {{ AUDIT_TYPE_LABEL[d.auditType] }}
                  </span>
                </td>
                <td class="py-3 px-3">
                  <div v-if="d.itemCode" class="font-mono text-xs font-medium text-gray-700">{{ d.itemCode }}</div>
                  <div v-if="d.itemName" class="text-xs text-gray-400 truncate max-w-[140px]">{{ d.itemName }}</div>
                  <span v-if="!d.itemCode && !d.itemName" class="text-xs text-gray-300">-</span>
                </td>
                <td class="py-3 px-3">
                  <p class="font-medium text-gray-900">{{ d.title }}</p>
                  <p v-if="d.domainName" class="text-xs text-gray-400 mt-0.5">{{ d.domainName }}</p>
                </td>
                <td class="py-3 px-3">
                  <span :class="defectTypeBadge(d.defectType)" class="text-xs px-2 py-0.5 rounded font-medium">
                    {{ DEFECT_TYPE_LABEL[d.defectType] }}
                  </span>
                </td>
                <td class="py-3 px-3">
                  <span :class="severityBadge(d.severity)" class="text-xs px-2 py-0.5 rounded font-medium">
                    {{ SEVERITY_LABEL[d.severity] }}
                  </span>
                </td>
                <td class="py-3 px-3 text-xs" :class="isOverdue(d) ? 'text-red-600 font-semibold' : 'text-gray-500'">
                  {{ d.dueDate ? formatDate(d.dueDate) : '-' }}
                  <span v-if="isOverdue(d)" class="block text-[11px]">기한초과</span>
                </td>
                <td class="py-3 px-3">
                  <span :class="statusBadge(d.status)" class="text-xs px-2 py-0.5 rounded font-medium">
                    {{ STATUS_LABEL[d.status] }}
                  </span>
                </td>
                <td class="py-3 px-3 text-right" @click.stop>
                  <div v-if="isManager" class="flex gap-1 justify-end">
                    <button @click="openEdit(d)" class="p-1 rounded hover:bg-gray-100 text-gray-400 hover:text-gray-700" title="수정">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                    </button>
                    <button @click="confirmDelete(d)" class="p-1 rounded hover:bg-red-50 text-gray-400 hover:text-red-500" title="삭제">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ── 보고서 탭 ─────────────────────────────────────────────────── -->
    <template v-else>
      <div class="card space-y-5">
        <div class="flex items-start justify-between gap-3 flex-wrap">
          <div>
            <h2 class="text-lg font-semibold text-gray-900">{{ selectedYear }}년 ISMS-P 결함 조치 보고서</h2>
            <p class="text-xs text-gray-500 mt-1">
              심사 개요와 보고서 본문을 연도별로 1건 보관합니다.
              <span v-if="report.updatedAt" class="ml-1">· 최종 수정 {{ formatDateTime(report.updatedAt) }}</span>
              <span v-if="report.registrantName" class="ml-1">· 작성자 {{ report.registrantName }}</span>
            </p>
          </div>
          <div v-if="isManager" class="flex items-center gap-2">
            <button v-if="report.exists" @click="confirmDeleteReport" class="btn-secondary text-sm">보고서 삭제</button>
            <button @click="saveReport" :disabled="reportSaving" class="btn-primary text-sm disabled:opacity-50">
              {{ reportSaving ? $t('common.loading') : '보고서 저장' }}
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div class="sm:col-span-2 lg:col-span-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">보고서 제목</label>
            <input v-model="report.title" :disabled="!isManager" class="input w-full"
              :placeholder="`${selectedYear}년 ISMS-P 결함 조치 보고서`" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">심사 구분</label>
            <select v-model="report.auditType" :disabled="!isManager" class="input w-full">
              <option v-for="t in AUDIT_TYPES" :key="t" :value="t">{{ AUDIT_TYPE_LABEL[t] }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">심사기관</label>
            <input v-model="report.auditOrg" :disabled="!isManager" class="input w-full" placeholder="예: 한국인터넷진흥원(KISA)" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">심사 시작일</label>
            <input v-model="report.auditStartDate" :disabled="!isManager" type="date" class="input w-full" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">심사 종료일</label>
            <input v-model="report.auditEndDate" :disabled="!isManager" type="date" class="input w-full" />
          </div>
          <div class="sm:col-span-2">
            <label class="block text-sm font-medium text-gray-700 mb-1">심사원</label>
            <input v-model="report.auditors" :disabled="!isManager" class="input w-full" placeholder="심사팀장 / 심사원 명단" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">보고일</label>
            <input v-model="report.reportedAt" :disabled="!isManager" type="date" class="input w-full" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">보고자</label>
            <input v-model="report.reporter" :disabled="!isManager" class="input w-full" />
          </div>
          <div class="sm:col-span-2 lg:col-span-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">인증 범위</label>
            <input v-model="report.auditScope" :disabled="!isManager" class="input w-full" placeholder="인증 대상 서비스·조직 범위" />
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">심사 총평</label>
          <textarea v-model="report.summary" :disabled="!isManager" rows="4" class="input w-full"
            placeholder="심사 결과 요약 — 결함 건수, 주요 지적 분야 등"></textarea>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">보고서 내용</label>
          <textarea v-model="report.content" :disabled="!isManager" rows="14" class="input w-full font-mono text-sm"
            placeholder="결함별 조치 경과, 조치 결과, 증빙 자료 등 보고서 본문을 작성합니다."></textarea>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">종합 의견 · 결론</label>
          <textarea v-model="report.conclusion" :disabled="!isManager" rows="4" class="input w-full"
            placeholder="조치 완료 여부에 대한 종합 의견"></textarea>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">첨부파일</label>
          <div v-if="report.fileName" class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border mb-2">
            <svg class="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/></svg>
            <span class="flex-1 text-sm text-gray-700 truncate">{{ report.fileName }}</span>
            <button @click="downloadReportFile" class="text-primary-600 hover:text-primary-700 text-xs font-medium">다운로드</button>
            <button v-if="isManager" @click="removeReportFile" class="text-red-500 hover:text-red-600 text-xs font-medium">삭제</button>
          </div>
          <input v-if="isManager" type="file" ref="reportFileInput" @change="onReportFileChange"
            class="block w-full text-sm text-gray-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-sm file:bg-gray-100 file:text-gray-700 hover:file:bg-gray-200" />
          <p v-if="isManager" class="text-xs text-gray-400 mt-1">보고서 원본(PDF·문서 등)을 함께 보관할 수 있습니다.</p>
        </div>
      </div>
    </template>

    <!-- 결함 상세 모달 -->
    <div v-if="showDetail && detailItem" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
      @click.self="showDetail = false">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-3xl max-h-[92vh] flex flex-col">
        <div class="px-6 py-4 border-b flex items-start justify-between">
          <div class="min-w-0">
            <div class="flex gap-2 mb-1 flex-wrap">
              <span class="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded">{{ AUDIT_TYPE_LABEL[detailItem.auditType] }}</span>
              <span :class="defectTypeBadge(detailItem.defectType)" class="text-xs px-2 py-0.5 rounded font-medium">{{ DEFECT_TYPE_LABEL[detailItem.defectType] }}</span>
              <span :class="severityBadge(detailItem.severity)" class="text-xs px-2 py-0.5 rounded font-medium">{{ SEVERITY_LABEL[detailItem.severity] }}</span>
              <span :class="statusBadge(detailItem.status)" class="text-xs px-2 py-0.5 rounded font-medium">{{ STATUS_LABEL[detailItem.status] }}</span>
            </div>
            <h2 class="text-lg font-semibold truncate">
              <span v-if="detailItem.defectNo" class="font-mono text-gray-500 mr-2">{{ detailItem.defectNo }}</span>
              {{ detailItem.title }}
            </h2>
          </div>
          <button @click="showDetail = false" class="text-gray-400 hover:text-gray-600 p-1 flex-shrink-0 ml-4">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="overflow-y-auto flex-1 px-6 py-5 space-y-4 text-sm">
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div><span class="text-gray-500">연도</span><p class="font-medium mt-0.5">{{ detailItem.year }}년</p></div>
            <div><span class="text-gray-500">분야</span><p class="font-medium mt-0.5">{{ detailItem.domainName || '-' }}</p></div>
            <div><span class="text-gray-500">인증기준</span>
              <p class="font-mono font-medium mt-0.5">{{ detailItem.itemCode || '-' }}</p>
              <p v-if="detailItem.itemName" class="text-xs text-gray-500">{{ detailItem.itemName }}</p>
            </div>
            <div><span class="text-gray-500">조치 기한</span><p class="font-medium mt-0.5">{{ detailItem.dueDate ? formatDate(detailItem.dueDate) : '-' }}</p></div>
            <div><span class="text-gray-500">조치 완료일</span><p class="font-medium mt-0.5">{{ detailItem.completedDate ? formatDate(detailItem.completedDate) : '-' }}</p></div>
            <div><span class="text-gray-500">담당자</span><p class="font-medium mt-0.5">{{ [detailItem.department, detailItem.assignee].filter(Boolean).join(' / ') || '-' }}</p></div>
          </div>
          <div v-for="f in DETAIL_TEXT_FIELDS" :key="f.key">
            <template v-if="detailItem[f.key]">
              <span class="text-gray-500 block mb-1">{{ f.label }}</span>
              <p class="text-gray-800 whitespace-pre-wrap p-3 rounded-lg text-sm" :class="f.class">{{ detailItem[f.key] }}</p>
            </template>
          </div>
          <div v-if="detailItem.fileName" class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border">
            <svg class="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/></svg>
            <span class="flex-1 text-sm text-gray-700 truncate">{{ detailItem.fileName }}</span>
            <button @click="downloadDefectFile(detailItem)" class="text-primary-600 hover:text-primary-700 text-xs font-medium">다운로드</button>
          </div>
          <p class="text-xs text-gray-400">
            등록자 {{ detailItem.registrantName || '-' }} · 등록 {{ formatDateTime(detailItem.createdAt) }}
            <span v-if="detailItem.updatedAt"> · 수정 {{ formatDateTime(detailItem.updatedAt) }}</span>
          </p>
        </div>
        <div v-if="isManager" class="px-6 py-3 border-t flex justify-end gap-2">
          <button @click="openEdit(detailItem)" class="btn-primary text-sm">수정</button>
        </div>
      </div>
    </div>

    <!-- 결함 등록/수정 모달 -->
    <div v-if="showForm" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-3xl max-h-[92vh] flex flex-col">
        <div class="px-6 py-4 border-b">
          <h2 class="text-lg font-semibold">{{ editItem ? '결함 수정' : '결함 등록' }}</h2>
        </div>
        <div class="overflow-y-auto flex-1 px-6 py-5 space-y-4">
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">연도</label>
              <input v-model.number="form.year" type="number" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">심사 구분</label>
              <select v-model="form.auditType" class="input w-full">
                <option v-for="t in AUDIT_TYPES" :key="t" :value="t">{{ AUDIT_TYPE_LABEL[t] }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">결함번호</label>
              <input v-model="form.defectNo" class="input w-full" placeholder="예: 결함-01" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">결함 구분</label>
              <select v-model="form.defectType" class="input w-full">
                <option v-for="t in DEFECT_TYPES" :key="t" :value="t">{{ DEFECT_TYPE_LABEL[t] }}</option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">인증기준 (ISMS-P 항목)</label>
              <select v-model="selectedItemCode" @change="applyItem" class="input w-full">
                <option value="">직접 입력</option>
                <option v-for="it in ismsItems" :key="it.id" :value="it.itemCode">
                  {{ it.itemCode }} {{ it.itemName }}
                </option>
              </select>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">기준 코드</label>
                <input v-model="form.itemCode" class="input w-full" placeholder="2.10.1" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">분야</label>
                <input v-model="form.domainName" class="input w-full" placeholder="2.10 시스템 및 서비스 보안관리" />
              </div>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">인증기준명</label>
            <input v-model="form.itemName" class="input w-full" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">결함 제목 <span class="text-red-500">*</span></label>
            <input v-model="form.title" class="input w-full" placeholder="결함 요약을 한 줄로 입력" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">결함 내용</label>
            <textarea v-model="form.content" rows="5" class="input w-full" placeholder="심사원이 지적한 결함 내용 전문"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">결함 원인</label>
            <textarea v-model="form.cause" rows="3" class="input w-full"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">조치 계획</label>
            <textarea v-model="form.actionPlan" rows="3" class="input w-full"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">조치 내용 (결과)</label>
            <textarea v-model="form.actionResult" rows="3" class="input w-full"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">재발방지 대책</label>
            <textarea v-model="form.preventionPlan" rows="3" class="input w-full"></textarea>
          </div>

          <div class="grid grid-cols-2 sm:grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">중요도</label>
              <select v-model="form.severity" class="input w-full">
                <option v-for="s in SEVERITIES" :key="s" :value="s">{{ SEVERITY_LABEL[s] }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">조치 상태</label>
              <select v-model="form.status" class="input w-full">
                <option v-for="s in STATUSES" :key="s" :value="s">{{ STATUS_LABEL[s] }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">표시 순서</label>
              <input v-model.number="form.sortOrder" type="number" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">조치 기한</label>
              <input v-model="form.dueDate" type="date" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">조치 완료일</label>
              <input v-model="form.completedDate" type="date" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">담당 부서</label>
              <input v-model="form.department" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">담당자</label>
              <input v-model="form.assignee" class="input w-full" />
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">첨부파일</label>
            <div v-if="editItem && editItem.fileName && !removeFormFile" class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border mb-2">
              <span class="flex-1 text-sm text-gray-700 truncate">{{ editItem.fileName }}</span>
              <button @click="removeFormFile = true" class="text-red-500 hover:text-red-600 text-xs font-medium">삭제</button>
            </div>
            <input type="file" ref="formFileInput" @change="onFormFileChange"
              class="block w-full text-sm text-gray-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-sm file:bg-gray-100 file:text-gray-700 hover:file:bg-gray-200" />
          </div>
        </div>
        <div class="px-6 py-3 border-t flex justify-end gap-2">
          <button @click="showForm = false" class="btn-secondary text-sm">{{ $t('common.cancel') }}</button>
          <button @click="saveDefect" :disabled="saving" class="btn-primary text-sm disabled:opacity-50">
            {{ saving ? $t('common.loading') : $t('common.save') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 확인 모달 -->
    <div v-if="confirmModal.show" class="fixed inset-0 bg-black/50 z-[60] flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6">
        <p class="text-sm text-gray-800 mb-5 whitespace-pre-line">{{ confirmModal.message }}</p>
        <div class="flex justify-end gap-2">
          <button @click="confirmModal.show = false" class="btn-secondary text-sm">{{ $t('common.cancel') }}</button>
          <button @click="confirmModal.onConfirm" class="btn-primary text-sm">{{ $t('common.confirm') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ismsDefectApi, ismsApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const isManager = computed(() => auth.isAdmin || auth.user?.role === 'MANAGER')

const AUDIT_TYPES = ['INITIAL', 'FOLLOWUP', 'RENEWAL', 'INTERNAL', 'OTHER']
const DEFECT_TYPES = ['DEFECT', 'RECOMMENDATION', 'IMPROVEMENT']
const SEVERITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const STATUSES = ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'HOLD']

const AUDIT_TYPE_LABEL = {
  INITIAL: '최초심사', FOLLOWUP: '사후심사', RENEWAL: '갱신심사', INTERNAL: '내부심사', OTHER: '기타'
}
const DEFECT_TYPE_LABEL = { DEFECT: '결함', RECOMMENDATION: '권고', IMPROVEMENT: '개선사항' }
const SEVERITY_LABEL = { CRITICAL: '매우높음', HIGH: '높음', MEDIUM: '보통', LOW: '낮음' }
const STATUS_LABEL = { OPEN: '미조치', IN_PROGRESS: '조치중', COMPLETED: '조치완료', HOLD: '보류' }

const DETAIL_TEXT_FIELDS = [
  { key: 'content', label: '결함 내용', class: 'bg-gray-50' },
  { key: 'cause', label: '결함 원인', class: 'bg-gray-50' },
  { key: 'actionPlan', label: '조치 계획', class: 'bg-blue-50' },
  { key: 'actionResult', label: '조치 내용', class: 'bg-green-50' },
  { key: 'preventionPlan', label: '재발방지 대책', class: 'bg-amber-50' }
]

const selectedYear = ref(new Date().getFullYear())
const tab = ref('defects')
const defects = ref([])
const summary = ref(null)
const ismsItems = ref([])
const loading = ref(false)
const saving = ref(false)
const csvLoading = ref(false)
const notice = ref('')
const errorMsg = ref('')

const filters = ref({ auditType: '', defectType: '', severity: '', status: '', keyword: '' })

const showForm = ref(false)
const showDetail = ref(false)
const editItem = ref(null)
const detailItem = ref(null)
const formFile = ref(null)
const formFileInput = ref(null)
const removeFormFile = ref(false)
const selectedItemCode = ref('')
const confirmModal = ref({ show: false, message: '', onConfirm: () => {} })

const report = ref(emptyReport(selectedYear.value))
const reportFile = ref(null)
const reportFileInput = ref(null)
const reportSaving = ref(false)

const form = ref(emptyForm())

function emptyForm() {
  return {
    year: selectedYear.value, auditType: 'RENEWAL', defectNo: '', defectType: 'DEFECT',
    severity: 'MEDIUM', domainName: '', itemCode: '', itemName: '', title: '', content: '',
    cause: '', actionPlan: '', actionResult: '', preventionPlan: '', dueDate: '',
    completedDate: '', assignee: '', department: '', status: 'OPEN', sortOrder: null
  }
}

function emptyReport(year) {
  return {
    year, exists: false, title: '', auditType: 'RENEWAL', auditOrg: '', auditors: '',
    auditScope: '', auditStartDate: '', auditEndDate: '', summary: '', content: '',
    conclusion: '', reportedAt: '', reporter: '', fileName: null, registrantName: null, updatedAt: null
  }
}

onMounted(async () => {
  // 보안 결함사항 화면에서 "원본 열기"로 넘어온 경우 해당 연도·결함을 바로 연다
  const qYear = Number(route.query.year)
  if (Number.isInteger(qYear) && qYear > 1900) selectedYear.value = qYear

  try {
    const res = await ismsApi.listItems({})
    ismsItems.value = res?.data || []
  } catch { ismsItems.value = [] }
  await reload()

  const qDefectId = Number(route.query.defectId)
  if (Number.isInteger(qDefectId) && qDefectId > 0) {
    const target = defects.value.find(d => d.id === qDefectId)
    if (target) openDetail(target)
  }
})

watch(selectedYear, reload)

async function reload() {
  await Promise.all([fetchDefects(), fetchSummary(), fetchReport()])
}

function changeYear(delta) {
  selectedYear.value += delta
}

async function fetchDefects() {
  loading.value = true
  try {
    const params = { year: selectedYear.value }
    if (filters.value.auditType) params.auditType = filters.value.auditType
    if (filters.value.defectType) params.defectType = filters.value.defectType
    if (filters.value.severity) params.severity = filters.value.severity
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.keyword) params.keyword = filters.value.keyword
    const res = await ismsDefectApi.list(params)
    defects.value = res?.data || []
  } catch (e) {
    errorMsg.value = e || '결함 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  try {
    const res = await ismsDefectApi.summary(selectedYear.value)
    summary.value = res?.data || null
  } catch { summary.value = null }
}

async function fetchReport() {
  try {
    const res = await ismsDefectApi.getReport(selectedYear.value)
    const r = res?.data
    report.value = r
      ? { ...emptyReport(selectedYear.value), ...r, ...normalizeReportDates(r) }
      : emptyReport(selectedYear.value)
  } catch {
    report.value = emptyReport(selectedYear.value)
  }
  reportFile.value = null
  if (reportFileInput.value) reportFileInput.value.value = ''
}

function normalizeReportDates(r) {
  return {
    title: r.title || '',
    auditType: r.auditType || 'RENEWAL',
    auditOrg: r.auditOrg || '',
    auditors: r.auditors || '',
    auditScope: r.auditScope || '',
    auditStartDate: r.auditStartDate || '',
    auditEndDate: r.auditEndDate || '',
    summary: r.summary || '',
    content: r.content || '',
    conclusion: r.conclusion || '',
    reportedAt: r.reportedAt || '',
    reporter: r.reporter || ''
  }
}

let debounceTimer = null
function debouncedFetch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fetchDefects, 400)
}

function resetFilters() {
  filters.value = { auditType: '', defectType: '', severity: '', status: '', keyword: '' }
  fetchDefects()
}

function openCreate() {
  editItem.value = null
  form.value = emptyForm()
  selectedItemCode.value = ''
  formFile.value = null
  removeFormFile.value = false
  showForm.value = true
}

function openEdit(d) {
  editItem.value = d
  form.value = {
    year: d.year, auditType: d.auditType, defectNo: d.defectNo || '', defectType: d.defectType,
    severity: d.severity, domainName: d.domainName || '', itemCode: d.itemCode || '',
    itemName: d.itemName || '', title: d.title, content: d.content || '', cause: d.cause || '',
    actionPlan: d.actionPlan || '', actionResult: d.actionResult || '',
    preventionPlan: d.preventionPlan || '', dueDate: d.dueDate || '',
    completedDate: d.completedDate || '', assignee: d.assignee || '', department: d.department || '',
    status: d.status, sortOrder: d.sortOrder ?? null
  }
  selectedItemCode.value = ismsItems.value.some(it => it.itemCode === d.itemCode) ? d.itemCode : ''
  formFile.value = null
  removeFormFile.value = false
  showDetail.value = false
  showForm.value = true
}

function openDetail(d) {
  detailItem.value = d
  showDetail.value = true
}

function applyItem() {
  const it = ismsItems.value.find(i => i.itemCode === selectedItemCode.value)
  if (!it) return
  form.value.itemCode = it.itemCode
  form.value.itemName = it.itemName
  form.value.domainName = it.domainName || form.value.domainName
}

function onFormFileChange(e) {
  formFile.value = e.target.files[0] || null
}

async function saveDefect() {
  if (!form.value.title || !form.value.title.trim()) {
    errorMsg.value = '결함 제목을 입력하세요.'
    return
  }
  saving.value = true
  errorMsg.value = ''
  try {
    const payload = {
      ...form.value,
      dueDate: form.value.dueDate || null,
      completedDate: form.value.completedDate || null,
      removeFile: removeFormFile.value
    }
    if (editItem.value) {
      await ismsDefectApi.update(editItem.value.id, payload, formFile.value)
      notice.value = '결함을 수정했습니다.'
    } else {
      await ismsDefectApi.create(payload, formFile.value)
      notice.value = '결함을 등록했습니다.'
    }
    showForm.value = false
    if (payload.year !== selectedYear.value) selectedYear.value = payload.year
    else await Promise.all([fetchDefects(), fetchSummary()])
  } catch (e) {
    errorMsg.value = e || '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

function confirmDelete(d) {
  confirmModal.value = {
    show: true,
    message: `"${d.title}" 결함을 삭제하시겠습니까?\n삭제한 내용은 복구할 수 없습니다.`,
    onConfirm: async () => {
      confirmModal.value.show = false
      try {
        await ismsDefectApi.delete(d.id)
        notice.value = '결함을 삭제했습니다.'
        await Promise.all([fetchDefects(), fetchSummary()])
      } catch (e) { errorMsg.value = e || '삭제에 실패했습니다.' }
    }
  }
}

function downloadDefectFile(d) {
  ismsDefectApi.downloadFile(d.id, d.fileName)
}

function onReportFileChange(e) {
  reportFile.value = e.target.files[0] || null
}

/** 보고서 저장 요청 본문 — 날짜는 비우기도 의미가 있어 항상 전체 값을 보낸다 */
function buildReportPayload(removeFile) {
  return {
    title: report.value.title,
    auditType: report.value.auditType,
    auditOrg: report.value.auditOrg,
    auditors: report.value.auditors,
    auditScope: report.value.auditScope,
    auditStartDate: report.value.auditStartDate || null,
    auditEndDate: report.value.auditEndDate || null,
    summary: report.value.summary,
    content: report.value.content,
    conclusion: report.value.conclusion,
    reportedAt: report.value.reportedAt || null,
    reporter: report.value.reporter,
    removeFile
  }
}

async function saveReport() {
  reportSaving.value = true
  errorMsg.value = ''
  try {
    await ismsDefectApi.saveReport(selectedYear.value, buildReportPayload(false), reportFile.value)
    notice.value = `${selectedYear.value}년 보고서를 저장했습니다.`
    await Promise.all([fetchReport(), fetchSummary()])
  } catch (e) {
    errorMsg.value = e || '보고서 저장에 실패했습니다.'
  } finally {
    reportSaving.value = false
  }
}

function confirmDeleteReport() {
  confirmModal.value = {
    show: true,
    message: `${selectedYear.value}년 보고서를 삭제하시겠습니까?`,
    onConfirm: async () => {
      confirmModal.value.show = false
      try {
        await ismsDefectApi.deleteReport(selectedYear.value)
        notice.value = '보고서를 삭제했습니다.'
        await Promise.all([fetchReport(), fetchSummary()])
      } catch (e) { errorMsg.value = e || '삭제에 실패했습니다.' }
    }
  }
}

function downloadReportFile() {
  ismsDefectApi.downloadReportFile(selectedYear.value, report.value.fileName)
}

async function removeReportFile() {
  try {
    await ismsDefectApi.saveReport(selectedYear.value, buildReportPayload(true), null)
    await fetchReport()
    notice.value = '첨부파일을 삭제했습니다.'
  } catch (e) { errorMsg.value = e || '삭제에 실패했습니다.' }
}

async function downloadCsv() {
  csvLoading.value = true
  try {
    await ismsDefectApi.exportCsv(selectedYear.value)
  } catch (e) {
    errorMsg.value = e || 'CSV 다운로드에 실패했습니다.'
  } finally {
    csvLoading.value = false
  }
}

function isOverdue(d) {
  if (!d.dueDate || d.status === 'COMPLETED') return false
  return new Date(d.dueDate) < new Date(new Date().toDateString())
}

function defectTypeBadge(t) {
  return {
    DEFECT: 'bg-red-100 text-red-700',
    RECOMMENDATION: 'bg-blue-100 text-blue-700',
    IMPROVEMENT: 'bg-gray-100 text-gray-600'
  }[t] || 'bg-gray-100 text-gray-500'
}

function severityBadge(s) {
  return {
    CRITICAL: 'bg-red-100 text-red-700',
    HIGH: 'bg-orange-100 text-orange-700',
    MEDIUM: 'bg-yellow-100 text-yellow-700',
    LOW: 'bg-blue-100 text-blue-700'
  }[s] || 'bg-gray-100 text-gray-500'
}

function statusBadge(s) {
  return {
    OPEN: 'bg-red-100 text-red-700',
    IN_PROGRESS: 'bg-yellow-100 text-yellow-700',
    COMPLETED: 'bg-green-100 text-green-700',
    HOLD: 'bg-gray-100 text-gray-600'
  }[s] || 'bg-gray-100 text-gray-500'
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function formatDateTime(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}
</script>
