<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ $t('nav.attackDetection') }}</h1>
        <p class="text-xs text-gray-400 mt-0.5">허니팟·허니토큰·행위 분석으로 이 포탈을 노리는 자동화 도구·AI 에이전트 공격을 탐지합니다.</p>
      </div>
      <div class="flex items-center gap-2">
        <span v-if="summary && !summary.enabled" class="text-xs font-semibold px-2 py-1 rounded bg-gray-200 text-gray-600">탐지 꺼짐</span>
        <button @click="refreshAll" class="btn-secondary text-sm">새로고침</button>
      </div>
    </div>

    <div class="page-body">
      <!-- 탭 -->
      <div class="flex flex-wrap gap-1 border-b mb-4">
        <button v-for="t in tabs" :key="t.key" @click="tab = t.key"
          class="px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors"
          :class="tab === t.key ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'">
          {{ t.label }}
          <span v-if="t.badge" class="ml-1 text-[11px] px-1.5 py-0.5 rounded-full bg-red-100 text-red-600">{{ t.badge }}</span>
        </button>
      </div>

      <!-- ── 개요 ── -->
      <div v-show="tab === 'overview'" class="space-y-4">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
          <div class="card">
            <div class="text-xs text-gray-500">최근 24시간 탐지</div>
            <div class="text-2xl font-semibold mt-1 text-gray-800">{{ summary?.last24h ?? '-' }}</div>
          </div>
          <button class="card text-left hover:border-primary-300" @click="goEvents({ status: 'NEW' })">
            <div class="text-xs text-gray-500">미처리(신규)</div>
            <div class="text-2xl font-semibold mt-1 text-red-600">{{ summary?.openCount ?? '-' }}</div>
          </button>
          <button class="card text-left hover:border-primary-300" @click="tab = 'blocks'">
            <div class="text-xs text-gray-500">차단 중인 IP</div>
            <div class="text-2xl font-semibold mt-1 text-orange-600">{{ summary?.activeBlocks ?? '-' }}</div>
          </button>
          <button class="card text-left hover:border-primary-300" @click="tab = 'live'">
            <div class="text-xs text-gray-500">현재 의심 IP(임계값 초과)</div>
            <div class="text-2xl font-semibold mt-1 text-gray-800">{{ liveOverThreshold }}</div>
          </button>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div class="card">
            <div class="text-sm font-semibold text-gray-700 mb-3">최근 7일 탐지 유형</div>
            <div class="space-y-2">
              <button v-for="(count, key) in categoryRows" :key="key" @click="goEvents({ category: key, days: 7 })"
                class="w-full text-left group">
                <div class="flex items-center justify-between text-sm">
                  <span class="text-gray-700 group-hover:text-primary-600">{{ CATEGORY_LABEL[key] }}</span>
                  <span class="font-semibold tabular-nums" :class="count ? 'text-gray-800' : 'text-gray-300'">{{ count }}</span>
                </div>
                <div class="h-1.5 rounded bg-gray-100 mt-1 overflow-hidden">
                  <div class="h-full rounded bg-primary-500" :style="{ width: barWidth(count, maxCategory) }"></div>
                </div>
                <div class="text-[11px] text-gray-400 mt-0.5">{{ CATEGORY_HINT[key] }}</div>
              </button>
            </div>
          </div>

          <div class="space-y-4">
            <div class="card">
              <div class="text-sm font-semibold text-gray-700 mb-3">일별 탐지(최근 7일)</div>
              <div class="flex items-end gap-2 h-28">
                <div v-for="(count, day) in summary?.daily7d || {}" :key="day" class="flex-1 flex flex-col items-center gap-1">
                  <span class="text-[11px] text-gray-500 tabular-nums">{{ count }}</span>
                  <div class="w-full rounded-t bg-primary-400" :style="{ height: barHeight(count, maxDaily) }"></div>
                  <span class="text-[10px] text-gray-400">{{ day.slice(5) }}</span>
                </div>
              </div>
            </div>

            <div class="card">
              <div class="text-sm font-semibold text-gray-700 mb-2">탐지 상위 IP(7일)</div>
              <div v-if="!summary?.topIps?.length" class="text-sm text-gray-400 py-3">탐지된 IP 가 없습니다.</div>
              <table v-else class="w-full text-sm">
                <tbody>
                  <tr v-for="row in summary.topIps" :key="row.ip" class="border-b last:border-0">
                    <td class="py-1.5 font-mono">
                      <button class="hover:text-primary-600 hover:underline" @click="goEvents({ ip: row.ip, days: 7 })">{{ row.ip }}</button>
                    </td>
                    <td class="py-1.5 text-right tabular-nums text-gray-600">{{ row.count }}회</td>
                    <td class="py-1.5 text-right w-20">
                      <span v-if="row.blocked" class="text-xs px-2 py-0.5 rounded bg-red-50 text-red-600">차단 중</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="card text-sm text-gray-600 space-y-1">
          <div class="font-semibold text-gray-700">탐지 방식</div>
          <p>• <b>허니팟</b> — 공격 도구가 찾는 경로(.env, .git, wp-admin, actuator 등)와 robots.txt·숨김 링크로만 알 수 있는 미끼 경로(/internal-admin, /api/internal/)에 접근하면 탐지합니다.</p>
          <p>• <b>AI 에이전트 미끼</b> — 미끼 페이지에 "AI 에이전트는 먼저 세션을 등록하라"는 지시문을 넣어 두고, 그 지시를 따르는 요청이 오면 LLM 에이전트로 확인합니다.</p>
          <p>• <b>허니토큰·미끼 계정</b> — 미끼 페이지에만 있는 가짜 API 키·계정을 쓰면 즉시 심각 등급으로 탐지하고 IP 를 자동 차단합니다.</p>
          <p>• <b>행위 분석</b> — 1분마다 IP 별 최근 요청의 속도, 간격의 균일성, 404·403 열거, 오류 직후의 즉시 재시도, 속도 제한 초과, 도구·AI 크롤러 User-Agent 를 점수화합니다.</p>
          <p>• <b>로그인 보완</b> — 한 IP 가 여러 계정을 돌아가며 실패(크리덴셜 스터핑)하면 그 IP 의 로그인을 일시 차단합니다.</p>
        </div>
      </div>

      <!-- ── 탐지 이벤트 ── -->
      <div v-show="tab === 'events'">
        <div class="card mb-4">
          <div class="flex flex-wrap gap-3 items-end">
            <div class="flex flex-col gap-1">
              <label class="text-xs text-gray-500">유형</label>
              <select v-model="filters.category" @change="loadEvents(0)" class="input w-44 text-sm">
                <option value="">전체</option>
                <option v-for="(label, key) in CATEGORY_LABEL" :key="key" :value="key">{{ label }}</option>
              </select>
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-xs text-gray-500">심각도</label>
              <select v-model="filters.severity" @change="loadEvents(0)" class="input w-28 text-sm">
                <option value="">전체</option>
                <option v-for="(label, key) in SEVERITY_LABEL" :key="key" :value="key">{{ label }}</option>
              </select>
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-xs text-gray-500">처리 상태</label>
              <select v-model="filters.status" @change="loadEvents(0)" class="input w-32 text-sm">
                <option value="">전체</option>
                <option v-for="(label, key) in STATUS_LABEL" :key="key" :value="key">{{ label }}</option>
              </select>
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-xs text-gray-500">기간</label>
              <select v-model.number="filters.days" @change="loadEvents(0)" class="input w-28 text-sm">
                <option :value="0">전체</option>
                <option :value="1">24시간</option>
                <option :value="7">7일</option>
                <option :value="30">30일</option>
                <option :value="90">90일</option>
              </select>
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-xs text-gray-500">IP</label>
              <input v-model="filters.ip" @keyup.enter="loadEvents(0)" class="input w-40 text-sm font-mono" placeholder="예: 10.0.1" />
            </div>
            <button @click="resetFilters" class="btn-secondary text-sm">초기화</button>
            <button v-if="canWrite" @click="createTest" class="text-xs px-2 py-1.5 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 ml-auto"
              title="알림(메일·Slack·수신함) 경로를 점검하는 테스트 이벤트를 만듭니다">
              알림 테스트
            </button>
          </div>
        </div>

        <div class="card p-0 overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-gray-50 text-xs text-gray-500">
                <tr>
                  <th class="px-3 py-2 text-left">최근 탐지</th>
                  <th class="px-3 py-2 text-left">심각도</th>
                  <th class="px-3 py-2 text-left">유형</th>
                  <th class="px-3 py-2 text-left">내용</th>
                  <th class="px-3 py-2 text-left">출발지 IP</th>
                  <th class="px-3 py-2 text-right">횟수</th>
                  <th class="px-3 py-2 text-left">상태</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="eventsLoading"><td colspan="7" class="px-3 py-8 text-center text-gray-400">불러오는 중…</td></tr>
                <tr v-else-if="!events.length"><td colspan="7" class="px-3 py-8 text-center text-gray-400">탐지 이벤트가 없습니다.</td></tr>
                <tr v-for="e in events" :key="e.id" @click="openDetail(e.id)"
                  class="border-t hover:bg-gray-50 cursor-pointer">
                  <td class="px-3 py-2 whitespace-nowrap text-gray-500 text-xs">{{ formatDate(e.lastSeenAt) }}</td>
                  <td class="px-3 py-2"><span :class="severityClass(e.severity)" class="text-xs font-semibold px-2 py-0.5 rounded whitespace-nowrap">{{ SEVERITY_LABEL[e.severity] }}</span></td>
                  <td class="px-3 py-2 whitespace-nowrap text-gray-700">{{ CATEGORY_LABEL[e.category] || e.category }}</td>
                  <td class="px-3 py-2 text-gray-800 max-w-md truncate" :title="e.title">
                    {{ e.title }}
                    <span v-if="e.autoBlocked" class="ml-1 text-[11px] px-1.5 py-0.5 rounded bg-red-50 text-red-600">자동 차단</span>
                  </td>
                  <td class="px-3 py-2 font-mono text-xs whitespace-nowrap">{{ e.sourceIp || '-' }}</td>
                  <td class="px-3 py-2 text-right tabular-nums">{{ e.occurrences }}</td>
                  <td class="px-3 py-2"><span :class="statusClass(e.status)" class="text-xs px-2 py-0.5 rounded whitespace-nowrap">{{ STATUS_LABEL[e.status] }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="totalPages > 1" class="flex items-center justify-between px-3 py-2 border-t text-sm">
            <span class="text-gray-500">총 {{ totalElements }}건</span>
            <div class="flex items-center gap-2">
              <button :disabled="page === 0" @click="loadEvents(page - 1)" class="btn-secondary text-xs disabled:opacity-40">이전</button>
              <span class="text-gray-600">{{ page + 1 }} / {{ totalPages }}</span>
              <button :disabled="page + 1 >= totalPages" @click="loadEvents(page + 1)" class="btn-secondary text-xs disabled:opacity-40">다음</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ── 실시간 의심 IP ── -->
      <div v-show="tab === 'live'">
        <div class="card mb-4 text-sm text-gray-600 flex flex-wrap gap-x-6 gap-y-1">
          <span>분석 시각: <b>{{ live?.analyzedAt ? formatDate(live.analyzedAt) : '대기 중(1분 주기)' }}</b></span>
          <span>분석 윈도: 최근 <b>{{ live?.windowMinutes }}</b>분</span>
          <span>이벤트 임계값: <b>{{ live?.scoreThreshold }}</b>점</span>
          <span>추적 중인 IP: <b>{{ live?.trackedIps }}</b>개</span>
        </div>
        <div class="card p-0 overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-gray-50 text-xs text-gray-500">
                <tr>
                  <th class="px-3 py-2 text-left">IP</th>
                  <th class="px-3 py-2 text-right">점수</th>
                  <th class="px-3 py-2 text-left">근거</th>
                  <th class="px-3 py-2 text-right">요청</th>
                  <th class="px-3 py-2 text-right">API</th>
                  <th class="px-3 py-2 text-right">404</th>
                  <th class="px-3 py-2 text-right">401/403</th>
                  <th class="px-3 py-2 text-left">계정·UA</th>
                  <th class="px-3 py-2"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!live?.ips?.length"><td colspan="9" class="px-3 py-8 text-center text-gray-400">점수가 매겨진 IP 가 없습니다.</td></tr>
                <tr v-for="r in live?.ips || []" :key="r.ip" class="border-t align-top">
                  <td class="px-3 py-2 font-mono text-xs whitespace-nowrap">
                    {{ r.ip }}
                    <div v-if="r.blocked" class="text-[11px] text-red-600">차단 중</div>
                    <div v-else-if="r.whitelisted" class="text-[11px] text-green-600">허용 목록</div>
                  </td>
                  <td class="px-3 py-2 text-right font-semibold tabular-nums" :class="scoreClass(r.score, live.scoreThreshold)">{{ r.score }}</td>
                  <td class="px-3 py-2 text-xs text-gray-600">
                    <div v-for="(reason, i) in r.reasons" :key="i">{{ reason }}</div>
                  </td>
                  <td class="px-3 py-2 text-right tabular-nums">{{ r.requests }}</td>
                  <td class="px-3 py-2 text-right tabular-nums">{{ r.distinctPaths }}</td>
                  <td class="px-3 py-2 text-right tabular-nums">{{ r.notFound }}</td>
                  <td class="px-3 py-2 text-right tabular-nums">{{ r.denied }}</td>
                  <td class="px-3 py-2 text-xs text-gray-500 max-w-xs">
                    <div v-if="r.lastUser" class="text-gray-700 break-all">{{ r.lastUser }}</div>
                    <div class="truncate" :title="r.userAgent">{{ r.userAgent || '(UA 없음)' }}</div>
                  </td>
                  <td class="px-3 py-2 text-right">
                    <button v-if="canWrite && !r.blocked && !r.whitelisted" @click="quickBlock(r.ip)"
                      class="text-xs px-2 py-1 rounded bg-red-600 text-white hover:bg-red-700 whitespace-nowrap">차단</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ── 차단 IP ── -->
      <div v-show="tab === 'blocks'">
        <div v-if="canWrite" class="card mb-4">
          <div class="text-sm font-semibold text-gray-700 mb-2">IP 수동 차단</div>
          <div class="flex flex-wrap gap-3 items-end">
            <div class="flex flex-col gap-1">
              <label class="text-xs text-gray-500">IP</label>
              <input v-model="blockForm.ip" class="input w-44 text-sm font-mono" placeholder="203.0.113.10" />
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-xs text-gray-500">기간</label>
              <select v-model.number="blockForm.minutes" class="input w-32 text-sm">
                <option :value="60">1시간</option>
                <option :value="1440">24시간</option>
                <option :value="10080">7일</option>
                <option :value="0">해제할 때까지</option>
              </select>
            </div>
            <div class="flex flex-col gap-1 flex-1 min-w-[12rem]">
              <label class="text-xs text-gray-500">사유</label>
              <input v-model="blockForm.reason" class="input w-full text-sm" placeholder="차단 사유" />
            </div>
            <button @click="submitBlock" :disabled="!blockForm.ip" class="btn-primary text-sm disabled:opacity-50">차단</button>
            <div class="w-full flex flex-wrap items-end gap-3 pt-3 border-t">
              <div class="flex flex-col gap-1">
                <label class="text-xs text-gray-500">로그인 일시 차단 해제(크리덴셜 스터핑 대응)</label>
                <input v-model="loginUnblockIp" class="input w-44 text-sm font-mono" placeholder="IP" />
              </div>
              <button @click="releaseLoginBlock" :disabled="!loginUnblockIp" class="btn-secondary text-sm disabled:opacity-50">로그인 차단 해제</button>
            </div>
          </div>
          <p v-if="blockError" class="text-sm text-red-600 mt-2">{{ blockError }}</p>
        </div>

        <div class="card p-0 overflow-hidden">
          <div class="flex items-center justify-between px-3 py-2 border-b">
            <span class="text-sm font-semibold text-gray-700">차단 이력</span>
            <label class="flex items-center gap-1.5 text-xs text-gray-500">
              <input v-model="showReleased" type="checkbox" class="rounded border-gray-300" /> 해제된 이력 포함
            </label>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-gray-50 text-xs text-gray-500">
                <tr>
                  <th class="px-3 py-2 text-left">IP</th>
                  <th class="px-3 py-2 text-left">사유</th>
                  <th class="px-3 py-2 text-left">차단 시각</th>
                  <th class="px-3 py-2 text-left">만료</th>
                  <th class="px-3 py-2 text-left">등록</th>
                  <th class="px-3 py-2 text-left">상태</th>
                  <th class="px-3 py-2"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!visibleBlocks.length"><td colspan="7" class="px-3 py-8 text-center text-gray-400">차단된 IP 가 없습니다.</td></tr>
                <tr v-for="b in visibleBlocks" :key="b.id" class="border-t">
                  <td class="px-3 py-2 font-mono text-xs whitespace-nowrap">{{ b.ip }}</td>
                  <td class="px-3 py-2 text-gray-700 max-w-md truncate" :title="b.reason">
                    {{ b.reason || '-' }}
                    <button v-if="b.eventId" @click="openDetail(b.eventId)" class="ml-1 text-xs text-primary-600 hover:underline">이벤트 #{{ b.eventId }}</button>
                  </td>
                  <td class="px-3 py-2 text-xs text-gray-500 whitespace-nowrap">{{ formatDate(b.createdAt) }}</td>
                  <td class="px-3 py-2 text-xs text-gray-500 whitespace-nowrap">{{ b.blockedUntil ? formatDate(b.blockedUntil) : '해제할 때까지' }}</td>
                  <td class="px-3 py-2 text-xs text-gray-500">{{ b.createdBy }}</td>
                  <td class="px-3 py-2">
                    <span v-if="b.effective" class="text-xs px-2 py-0.5 rounded bg-red-50 text-red-600">차단 중</span>
                    <span v-else class="text-xs px-2 py-0.5 rounded bg-gray-100 text-gray-500" :title="b.releasedBy ? `${b.releasedBy} · ${formatDate(b.releasedAt)}` : ''">해제됨</span>
                  </td>
                  <td class="px-3 py-2 text-right">
                    <button v-if="canWrite && b.effective" @click="release(b)" class="text-xs px-2 py-1 rounded border border-gray-200 hover:bg-gray-50 whitespace-nowrap">해제</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ── 설정 ── -->
      <div v-show="tab === 'settings'">
        <div v-if="settings" class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div class="card space-y-4">
            <div class="text-sm font-semibold text-gray-700">탐지·차단</div>
            <label class="flex items-center gap-2 text-sm">
              <input v-model="settings.enabled" type="checkbox" class="rounded border-gray-300" :disabled="!isAdmin" />
              공격 탐지 사용
            </label>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">행위 분석 윈도(분)</label>
                <input v-model.number="settings.windowMinutes" type="number" min="1" max="60" class="input w-full text-sm" :disabled="!isAdmin" />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">이벤트 임계 점수</label>
                <input v-model.number="settings.scoreThreshold" type="number" min="20" max="300" class="input w-full text-sm" :disabled="!isAdmin" />
              </div>
            </div>
            <label class="flex items-center gap-2 text-sm">
              <input v-model="settings.autoBlockEnabled" type="checkbox" class="rounded border-gray-300" :disabled="!isAdmin" />
              IP 자동 차단
            </label>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">자동 차단 기준 심각도</label>
                <select v-model="settings.autoBlockMinSeverity" class="input w-full text-sm" :disabled="!isAdmin">
                  <option value="CRITICAL">심각 이상</option>
                  <option value="HIGH">높음 이상</option>
                  <option value="MEDIUM">중간 이상</option>
                </select>
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">자동 차단 시간(분, 0=무기한)</label>
                <input v-model.number="settings.blockMinutes" type="number" min="0" class="input w-full text-sm" :disabled="!isAdmin" />
              </div>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">허용 목록 IP(차단·로그인 차단 제외) — 한 줄에 하나, CIDR 가능</label>
              <textarea v-model="settings.whitelistIps" rows="3" class="input w-full text-sm font-mono" :disabled="!isAdmin"
                placeholder="10.0.0.0/8&#10;192.168.10.5"></textarea>
              <p class="text-[11px] text-gray-400 mt-1">점검용 PC·사내 프록시·모니터링 서버 IP 를 넣으세요. 현재 접속 IP: <span class="font-mono">{{ myIpHint }}</span></p>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">알림 기준 심각도</label>
                <select v-model="settings.notifyMinSeverity" class="input w-full text-sm" :disabled="!isAdmin">
                  <option value="CRITICAL">심각만</option>
                  <option value="HIGH">높음 이상</option>
                  <option value="MEDIUM">중간 이상</option>
                  <option value="LOW">낮음 이상</option>
                </select>
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">이벤트 보존 기간(일)</label>
                <input v-model.number="settings.retentionDays" type="number" min="30" max="3650" class="input w-full text-sm" :disabled="!isAdmin" />
              </div>
            </div>
            <p class="text-[11px] text-gray-400">알림은 설정관리 &gt; 알림 설정의 발송 방식(메일·Slack)과 관리자 수신함으로 갑니다.</p>
          </div>

          <div class="space-y-4">
            <div class="card space-y-3">
              <div class="text-sm font-semibold text-gray-700">로그인 보완(IP 단위)</div>
              <p class="text-xs text-gray-400">계정 잠금은 계정 하나에 대한 반복 실패만 막습니다. 한 IP 가 여러 계정을 돌아가며 시도하면 아래 기준으로 그 IP 의 로그인을 일시 차단합니다.</p>
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="block text-xs text-gray-500 mb-1">집계 기간(분)</label>
                  <input v-model.number="settings.loginWindowMinutes" type="number" min="1" max="120" class="input w-full text-sm" :disabled="!isAdmin" />
                </div>
                <div>
                  <label class="block text-xs text-gray-500 mb-1">로그인 차단 시간(분)</label>
                  <input v-model.number="settings.loginLockMinutes" type="number" min="1" max="1440" class="input w-full text-sm" :disabled="!isAdmin" />
                </div>
                <div>
                  <label class="block text-xs text-gray-500 mb-1">실패 횟수 기준</label>
                  <input v-model.number="settings.loginFailThreshold" type="number" min="3" class="input w-full text-sm" :disabled="!isAdmin" />
                </div>
                <div>
                  <label class="block text-xs text-gray-500 mb-1">서로 다른 계정 수 기준</label>
                  <input v-model.number="settings.loginDistinctAccounts" type="number" min="2" class="input w-full text-sm" :disabled="!isAdmin" />
                </div>
              </div>
            </div>

            <div class="card space-y-3">
              <div class="text-sm font-semibold text-gray-700">미끼(허니토큰·미끼 계정)</div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">미끼 계정 — 실제로 없는 이메일, 한 줄에 하나</label>
                <textarea v-model="settings.honeyAccounts" rows="2" class="input w-full text-sm font-mono" :disabled="!isAdmin"></textarea>
                <p class="text-[11px] text-gray-400 mt-1">첫 번째 계정이 미끼 페이지에 노출됩니다. 실제 사용자 이메일과 같으면 탐지하지 않습니다.</p>
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">허니토큰(가짜 API 키)</label>
                <div class="flex gap-2">
                  <input :value="settings.honeyToken" readonly class="input w-full text-xs font-mono bg-gray-50" />
                  <button v-if="isAdmin" @click="regenerateToken" class="btn-secondary text-xs whitespace-nowrap">재발급</button>
                </div>
                <p class="text-[11px] text-gray-400 mt-1">미끼 페이지에만 노출됩니다. 이 값이 요청에 실려 오면 심각 등급으로 탐지합니다.</p>
              </div>
            </div>
          </div>

          <div v-if="isAdmin" class="lg:col-span-2 flex items-center gap-3">
            <button @click="saveSettings" :disabled="settingsSaving" class="btn-primary text-sm px-6 disabled:opacity-50">
              {{ settingsSaving ? '저장 중…' : '저장' }}
            </button>
            <span v-if="settingsMsg" class="text-sm" :class="settingsMsgOk ? 'text-green-600' : 'text-red-600'">{{ settingsMsg }}</span>
          </div>
          <p v-else class="lg:col-span-2 text-xs text-gray-400">설정 변경은 관리자만 할 수 있습니다.</p>
        </div>
      </div>
    </div>

    <DetectionEventDetailModal :open="detailOpen" :item-id="detailId" :can-write="canWrite" :is-admin="isAdmin"
      @close="detailOpen = false" @changed="onChanged" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { detectionApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import DetectionEventDetailModal from './DetectionEventDetailModal.vue'
import {
  CATEGORY_LABEL, CATEGORY_HINT, SEVERITY_LABEL, STATUS_LABEL,
  severityClass, statusClass, scoreClass, formatDate
} from './detectionLabels'

const auth = useAuthStore()
const isAdmin = computed(() => auth.isAdmin)
const canWrite = computed(() => auth.isManager && auth.canWrite('attack_detection'))

const tab = ref('overview')
const summary = ref(null)
const live = ref(null)

const tabs = computed(() => [
  { key: 'overview', label: '개요' },
  { key: 'events', label: '탐지 이벤트', badge: summary.value?.openCount || null },
  { key: 'live', label: '실시간 의심 IP' },
  { key: 'blocks', label: '차단 IP', badge: summary.value?.activeBlocks || null },
  { key: 'settings', label: '설정' }
])

// ── 개요 ──
const categoryRows = computed(() => {
  const rows = { ...(summary.value?.byCategory7d || {}) }
  delete rows.TEST
  return rows
})
const maxCategory = computed(() => Math.max(1, ...Object.values(categoryRows.value)))
const maxDaily = computed(() => Math.max(1, ...Object.values(summary.value?.daily7d || {})))
const liveOverThreshold = computed(() =>
  (live.value?.ips || []).filter(r => r.score >= (live.value?.scoreThreshold || 0)).length)
function barWidth(v, max) { return `${Math.round((v / max) * 100)}%` }
function barHeight(v, max) { return `${Math.max(2, Math.round((v / max) * 80))}px` }

async function loadSummary() {
  try { summary.value = (await detectionApi.summary()).data } catch {}
}
async function loadLive() {
  try { live.value = (await detectionApi.live()).data } catch {}
}

// ── 이벤트 ──
const filters = ref({ category: '', severity: '', status: '', ip: '', days: 0 })
const events = ref([])
const eventsLoading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

async function loadEvents(p = page.value) {
  eventsLoading.value = true
  try {
    const params = { page: p, size: 30 }
    for (const [k, v] of Object.entries(filters.value)) if (v) params[k] = v
    const res = await detectionApi.events(params)
    events.value = res.data.content
    page.value = res.data.number
    totalPages.value = res.data.totalPages
    totalElements.value = res.data.totalElements
  } catch {
    events.value = []
  } finally {
    eventsLoading.value = false
  }
}
function resetFilters() {
  filters.value = { category: '', severity: '', status: '', ip: '', days: 0 }
  loadEvents(0)
}
function goEvents(f) {
  filters.value = { category: '', severity: '', status: '', ip: '', days: 0, ...f }
  tab.value = 'events'
  loadEvents(0)
}
async function createTest() {
  try {
    await detectionApi.test()
    await Promise.all([loadEvents(0), loadSummary()])
  } catch (e) { alert(e) }
}

const detailOpen = ref(false)
const detailId = ref(null)
function openDetail(id) { detailId.value = id; detailOpen.value = true }
function onChanged() { loadEvents(); loadSummary(); loadBlocks() }

// ── 차단 ──
const blocks = ref([])
const showReleased = ref(false)
const blockForm = ref({ ip: '', minutes: 1440, reason: '' })
const blockError = ref('')
const loginUnblockIp = ref('')
const visibleBlocks = computed(() => showReleased.value ? blocks.value : blocks.value.filter(b => b.effective))

async function loadBlocks() {
  try { blocks.value = (await detectionApi.blocks()).data } catch {}
}
async function submitBlock() {
  blockError.value = ''
  try {
    await detectionApi.block({ ...blockForm.value, reason: blockForm.value.reason || '[수동] 관리자 차단' })
    blockForm.value = { ip: '', minutes: 1440, reason: '' }
    await Promise.all([loadBlocks(), loadSummary()])
  } catch (e) { blockError.value = e }
}
async function quickBlock(ip) {
  if (!confirm(`${ip} 를 24시간 차단할까요?`)) return
  try {
    await detectionApi.block({ ip, minutes: 1440, reason: '[수동] 실시간 의심 IP 차단' })
    await Promise.all([loadBlocks(), loadSummary(), loadLive()])
  } catch (e) { alert(e) }
}
async function release(b) {
  if (!confirm(`${b.ip} 차단을 해제할까요?`)) return
  try {
    await detectionApi.release(b.id)
    await Promise.all([loadBlocks(), loadSummary()])
  } catch (e) { alert(e) }
}
async function releaseLoginBlock() {
  blockError.value = ''
  try {
    await detectionApi.releaseLoginBlock(loginUnblockIp.value.trim())
    loginUnblockIp.value = ''
    blockError.value = ''
    alert('로그인 일시 차단을 해제했습니다.')
  } catch (e) { blockError.value = e }
}

// ── 설정 ──
const settings = ref(null)
const settingsSaving = ref(false)
const settingsMsg = ref('')
const settingsMsgOk = ref(true)
const myIpHint = computed(() => settings.value?.yourIp || '-')

async function loadSettings() {
  try { settings.value = (await detectionApi.getSettings()).data } catch {}
}
async function saveSettings() {
  settingsSaving.value = true
  settingsMsg.value = ''
  try {
    settings.value = (await detectionApi.saveSettings(settings.value)).data
    settingsMsgOk.value = true
    settingsMsg.value = '저장되었습니다.'
    setTimeout(() => { settingsMsg.value = '' }, 3000)
  } catch (e) {
    settingsMsgOk.value = false
    settingsMsg.value = e
  } finally {
    settingsSaving.value = false
  }
}
async function regenerateToken() {
  if (!confirm('허니토큰을 재발급하면 이전 값은 더 이상 탐지되지 않습니다. 계속할까요?')) return
  try { settings.value = (await detectionApi.regenerateHoneyToken()).data } catch (e) { alert(e) }
}

function refreshAll() {
  loadSummary(); loadLive(); loadEvents(); loadBlocks(); loadSettings()
}

watch(tab, (t) => {
  if (t === 'live') loadLive()
  if (t === 'blocks') loadBlocks()
})

let timer = null
onMounted(() => {
  refreshAll()
  // 실시간 의심 IP 는 1분마다 분석되므로 같은 주기로 다시 읽는다
  timer = setInterval(() => { loadSummary(); if (tab.value === 'live' || tab.value === 'overview') loadLive() }, 60_000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>
