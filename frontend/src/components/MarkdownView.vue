<template>
  <div class="md-body" v-html="renderedHtml"></div>
</template>

<script setup>
import { computed } from 'vue'
import { Marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  content: { type: String, default: '' },
})

// breaks: true — 줄바꿈을 그대로 <br> 로 렌더링한다.
// 정책 문서는 개발자가 아닌 담당자가 작성하므로 "엔터 = 줄바꿈" 이 기대 동작이다.
const marked = new Marked({ gfm: true, breaks: true })

const renderedHtml = computed(() =>
  DOMPurify.sanitize(marked.parse(props.content || ''))
)
</script>

<style scoped>
/* 도움말 뷰어(HelpView)와 동일한 본문 스타일.
   Tailwind preflight 가 기본 태그 스타일을 초기화하므로 명시적으로 지정해야 한다.
   (@tailwindcss/typography 플러그인은 이 프로젝트에 설치되어 있지 않다) */
.md-body { @apply text-[14px] leading-relaxed text-gray-700; }

.md-body :deep(h1) { @apply text-xl font-bold text-gray-900 tracking-tight mt-6 mb-3 first:mt-0; }
.md-body :deep(h2) { @apply text-lg font-bold text-gray-900 tracking-tight mt-7 mb-2.5 pb-2 border-b border-gray-100 first:mt-0; }
.md-body :deep(h3) { @apply text-[15px] font-bold text-gray-800 mt-5 mb-2 first:mt-0; }
.md-body :deep(h4) { @apply text-sm font-bold text-gray-700 mt-4 mb-1.5 first:mt-0; }
.md-body :deep(p)  { @apply my-2.5; }
.md-body :deep(hr) { @apply my-6 border-gray-100; }
.md-body :deep(strong) { @apply font-semibold text-gray-900; }
.md-body :deep(em) { @apply italic; }
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

.md-body :deep(img) { @apply my-4 rounded-xl border border-gray-200 max-w-full; }
</style>
