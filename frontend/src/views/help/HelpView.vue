<template>
  <div>
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">도움말</h1>
        <p class="text-sm text-gray-400 mt-1">SecPortal 사용자 매뉴얼 {{ manualVersion }}</p>
      </div>
      <div class="flex items-center gap-2">
        <div class="relative">
          <svg class="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
          </svg>
          <input v-model="tocFilter" type="text" placeholder="목차 검색"
            class="input pl-9 py-2 w-52 text-sm" />
        </div>
        <button @click="printManual" class="btn-secondary flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4H7v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z"/>
          </svg>
          인쇄
        </button>
      </div>
    </div>

    <div class="page-body">
      <div v-if="loading" class="card text-center py-16 text-gray-400 text-sm">매뉴얼을 불러오는 중...</div>
      <div v-else-if="error" class="card text-center py-16">
        <p class="text-sm text-red-500 font-semibold">매뉴얼을 불러오지 못했습니다</p>
        <p class="text-xs text-gray-400 mt-1">{{ error }}</p>
      </div>

      <div v-else class="flex gap-6 items-start">
        <!-- TOC -->
        <aside class="w-60 flex-shrink-0 sticky top-6 hidden lg:block help-toc">
          <div class="card !p-4 max-h-[calc(100vh-8rem)] overflow-y-auto">
            <p class="text-[10px] font-bold uppercase tracking-widest text-gray-400 px-2 mb-2">목차</p>
            <nav class="space-y-0.5">
              <a v-for="item in filteredToc" :key="item.id"
                :href="'#' + item.id"
                @click.prevent="scrollTo(item.id)"
                class="block rounded-lg px-2 py-1.5 text-[13px] transition-colors truncate"
                :class="[
                  item.level === 3 ? 'pl-5 text-gray-400' : 'font-medium text-gray-600',
                  activeId === item.id ? '!text-primary-600 bg-primary-50 font-semibold' : 'hover:bg-gray-50 hover:text-gray-900'
                ]">
                {{ item.text }}
              </a>
              <p v-if="filteredToc.length === 0" class="px-2 py-2 text-xs text-gray-300">검색 결과가 없습니다</p>
            </nav>
          </div>
        </aside>

        <!-- Content -->
        <div class="flex-1 min-w-0">
          <div ref="contentEl" class="card md-body" v-html="renderedHtml" @click="onContentClick"></div>
        </div>
      </div>
    </div>

    <!-- 이미지 라이트박스 -->
    <Transition name="fade">
      <div v-if="lightboxSrc" class="fixed inset-0 z-[9999] flex items-center justify-center p-8 cursor-zoom-out"
        @click="lightboxSrc = null">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm"/>
        <img :src="lightboxSrc" class="relative max-w-full max-h-full rounded-xl shadow-2xl" />
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Marked } from 'marked'
import DOMPurify from 'dompurify'

const MANUAL_URL = '/help/user-manual.md'

const loading = ref(true)
const error = ref('')
const renderedHtml = ref('')
const manualVersion = ref('')
const toc = ref([])
const tocFilter = ref('')
const activeId = ref('')
const contentEl = ref(null)
const lightboxSrc = ref(null)

const filteredToc = computed(() => {
  const q = tocFilter.value.trim().toLowerCase()
  if (!q) return toc.value
  return toc.value.filter(t => t.text.toLowerCase().includes(q))
})

// GitHub 스타일 슬러그 (한글 유지, 구두점 제거)
function slugify(text, seen) {
  let slug = text.toLowerCase().trim()
    .replace(/<[^>]+>/g, '')
    .replace(/[^\p{L}\p{N}\s-]/gu, '')
    .replace(/\s+/g, '-')
  if (seen[slug] !== undefined) {
    seen[slug]++
    slug = `${slug}-${seen[slug]}`
  } else {
    seen[slug] = 0
  }
  return slug
}

function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function buildMarked() {
  const seen = {}
  const marked = new Marked({ gfm: true })
  marked.use({
    renderer: {
      heading({ tokens, depth }) {
        const html = this.parser.parseInline(tokens)
        const raw = tokens.map(t => t.raw ?? '').join('')
        const id = slugify(raw, seen)
        return `<h${depth} id="${id}">${html}</h${depth}>\n`
      },
      image({ href, text }) {
        const src = /^(https?:)?\/\//.test(href) ? href : `/help/${href}`
        return `<img src="${src}" alt="${escapeHtml(text || '')}" loading="lazy" class="md-img">`
      },
      code({ text, lang }) {
        if (lang === 'mermaid') {
          return `<details class="md-erd"><summary>ERD 다이어그램 정의 보기 (Mermaid)</summary><pre><code>${escapeHtml(text)}</code></pre></details>\n`
        }
        return `<pre><code>${escapeHtml(text)}</code></pre>\n`
      }
    }
  })
  return marked
}

async function loadManual() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetch(MANUAL_URL, { cache: 'no-cache' })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const md = await res.text()

    const vMatch = md.match(/\*\*버전\*\*:\s*(v[\d.]+)/)
    manualVersion.value = vMatch ? vMatch[1] : ''

    const html = buildMarked().parse(md)
    renderedHtml.value = DOMPurify.sanitize(html, { ADD_TAGS: ['details', 'summary'] })

    loading.value = false // 본문 DOM이 렌더링된 후에야 목차를 만들 수 있다
    await nextTick()
    buildToc()
    observeHeadings()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

function buildToc() {
  if (!contentEl.value) return
  toc.value = [...contentEl.value.querySelectorAll('h2, h3')]
    .filter(h => h.textContent.trim() !== '목차')
    .map(h => ({
      id: h.id,
      text: h.textContent,
      level: h.tagName === 'H3' ? 3 : 2,
    }))
}

let observer = null
function observeHeadings() {
  if (!contentEl.value || typeof IntersectionObserver === 'undefined') return
  observer?.disconnect()
  observer = new IntersectionObserver(entries => {
    const visible = entries.filter(e => e.isIntersecting)
    if (visible.length > 0) activeId.value = visible[0].target.id
  }, { rootMargin: '0px 0px -70% 0px' })
  contentEl.value.querySelectorAll('h2, h3').forEach(h => observer.observe(h))
}

function scrollTo(id) {
  const el = contentEl.value?.querySelector(`[id="${CSS.escape(id)}"]`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    activeId.value = id
  }
}

function onContentClick(e) {
  // 본문 내 앵커 링크(#...)는 부드러운 스크롤로 처리
  const a = e.target.closest('a')
  if (a) {
    const href = a.getAttribute('href') || ''
    if (href.startsWith('#')) {
      e.preventDefault()
      scrollTo(decodeURIComponent(href.slice(1)))
      return
    }
    if (/^https?:\/\//.test(href)) a.target = '_blank'
    return
  }
  // 스크린샷 클릭 시 확대
  if (e.target.tagName === 'IMG') lightboxSrc.value = e.target.src
}

function printManual() {
  window.print()
}

onMounted(loadManual)
onUnmounted(() => observer?.disconnect())
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ─── 마크다운 본문 스타일 ─── */
.md-body { @apply text-[14px] leading-relaxed text-gray-700; }

.md-body :deep(h1) { @apply text-2xl font-bold text-gray-900 tracking-tight mb-2; }
.md-body :deep(h2) { @apply text-lg font-bold text-gray-900 tracking-tight mt-10 mb-3 pb-2 border-b border-gray-100 scroll-mt-6; }
.md-body :deep(h3) { @apply text-[15px] font-bold text-gray-800 mt-7 mb-2 scroll-mt-6; }
.md-body :deep(h4) { @apply text-sm font-bold text-gray-700 mt-5 mb-2 scroll-mt-6; }
.md-body :deep(p)  { @apply my-2.5; }
.md-body :deep(hr) { @apply my-8 border-gray-100; }
.md-body :deep(strong) { @apply font-semibold text-gray-900; }
.md-body :deep(a) { @apply text-primary-600 hover:underline; }

.md-body :deep(ul) { @apply list-disc pl-5 my-2.5 space-y-1; }
.md-body :deep(ol) { @apply list-decimal pl-5 my-2.5 space-y-1; }
.md-body :deep(li > ul), .md-body :deep(li > ol) { @apply my-1; }

.md-body :deep(table) { @apply w-full my-4 text-[13px] border-collapse; }
.md-body :deep(th) { @apply text-left font-semibold text-gray-500 bg-gray-50 px-3 py-2 border border-gray-100; }
.md-body :deep(td) { @apply px-3 py-2 border border-gray-100 align-top; }

.md-body :deep(blockquote) {
  @apply my-4 px-4 py-3 rounded-xl bg-amber-50 border border-amber-100 text-amber-800 text-[13px];
}
.md-body :deep(blockquote p) { @apply my-0; }

.md-body :deep(code) { @apply font-mono text-[12.5px] bg-gray-100 text-gray-800 px-1.5 py-0.5 rounded; }
.md-body :deep(pre) { @apply my-4 p-4 rounded-xl bg-gray-900 overflow-x-auto; }
.md-body :deep(pre code) { @apply bg-transparent text-gray-100 p-0 text-xs leading-relaxed; }

.md-body :deep(.md-img) {
  @apply my-4 rounded-xl border border-gray-200 shadow-sm max-w-full cursor-zoom-in;
}

.md-body :deep(.md-erd) { @apply my-4; }
.md-body :deep(.md-erd summary) {
  @apply cursor-pointer text-[13px] font-semibold text-gray-500 hover:text-gray-800 select-none;
}
.md-body :deep(.md-erd pre) { @apply mt-2; }

/* ─── 인쇄 ─── */
@media print {
  .page-header, .help-toc { display: none !important; }
  .md-body { box-shadow: none; border: none; }
}
</style>
