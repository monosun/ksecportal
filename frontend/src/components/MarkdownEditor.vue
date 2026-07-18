<template>
  <div class="border border-gray-200 rounded-lg overflow-hidden">
    <!-- 툴바 -->
    <div class="flex items-center gap-0.5 px-2 py-1.5 bg-gray-50 border-b border-gray-200 flex-wrap">
      <button v-for="t in TOOLS" :key="t.label" type="button" :title="t.title"
        :disabled="mode === 'preview'"
        @click="applyTool(t)"
        class="px-2 py-1 text-xs rounded text-gray-600 hover:bg-gray-200 disabled:opacity-40 disabled:hover:bg-transparent"
        :class="t.mono ? 'font-mono' : ''">
        <span :class="t.style">{{ t.label }}</span>
      </button>

      <div class="ml-auto flex items-center gap-0.5 bg-gray-200 rounded p-0.5">
        <button type="button" @click="mode = 'edit'"
          class="px-2.5 py-1 text-xs rounded transition-colors"
          :class="mode === 'edit' ? 'bg-white text-gray-900 shadow-sm font-medium' : 'text-gray-600 hover:text-gray-900'">
          편집
        </button>
        <button type="button" @click="mode = 'preview'"
          class="px-2.5 py-1 text-xs rounded transition-colors"
          :class="mode === 'preview' ? 'bg-white text-gray-900 shadow-sm font-medium' : 'text-gray-600 hover:text-gray-900'">
          미리보기
        </button>
      </div>
    </div>

    <!-- 편집 -->
    <textarea v-if="mode === 'edit'" ref="textarea"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @keydown.tab.prevent="onTab"
      :rows="rows" :required="required" :placeholder="placeholder"
      class="block w-full px-3 py-2.5 font-mono text-[13px] leading-relaxed text-gray-800 border-0 focus:ring-0 focus:outline-none resize-y"></textarea>

    <!-- 미리보기 -->
    <div v-else class="px-4 py-3 overflow-y-auto bg-white" :style="{ minHeight: rows * 1.6 + 'rem' }">
      <MarkdownView v-if="modelValue?.trim()" :content="modelValue" />
      <p v-else class="text-sm text-gray-400 py-6 text-center">미리볼 내용이 없습니다.</p>
    </div>

    <!-- 문법 안내 -->
    <div v-if="mode === 'edit'" class="px-3 py-1.5 bg-gray-50 border-t border-gray-100 text-[11px] text-gray-400">
      마크다운 문법 — <code class="font-mono"># 제목</code> ·
      <code class="font-mono">**굵게**</code> ·
      <code class="font-mono">- 목록</code> ·
      <code class="font-mono">| 표 |</code> ·
      <code class="font-mono">&gt; 인용</code>. 줄바꿈은 그대로 반영됩니다.
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import MarkdownView from './MarkdownView.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  rows: { type: Number, default: 14 },
  required: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const mode = ref('edit')
const textarea = ref(null)

/** wrap: 선택 영역을 앞뒤로 감싼다. line: 선택한 줄마다 접두어를 붙인다. block: 커서 위치에 블록을 삽입한다. */
const TOOLS = [
  { label: '제목', title: '제목 (## )', type: 'line', prefix: '## ' },
  { label: 'B', title: '굵게 (**텍스트**)', type: 'wrap', prefix: '**', suffix: '**', placeholder: '굵은 텍스트', style: 'font-bold' },
  { label: 'I', title: '기울임 (*텍스트*)', type: 'wrap', prefix: '*', suffix: '*', placeholder: '기울인 텍스트', style: 'italic' },
  { label: '목록', title: '글머리 목록 (- )', type: 'line', prefix: '- ' },
  { label: '번호', title: '번호 목록 (1. )', type: 'line', prefix: '1. ' },
  { label: '인용', title: '인용 (> )', type: 'line', prefix: '> ' },
  { label: '링크', title: '링크 [텍스트](주소)', type: 'wrap', prefix: '[', suffix: '](https://)', placeholder: '링크 텍스트' },
  { label: '코드', title: '코드 블록', type: 'block', text: '```\n코드\n```' },
  { label: '표', title: '표 삽입', type: 'block', text: '| 항목 | 내용 |\n|------|------|\n|  |  |' },
  { label: '구분선', title: '구분선', type: 'block', text: '---' },
]

async function applyTool(tool) {
  const el = textarea.value
  if (!el) return

  const value = props.modelValue || ''
  const start = el.selectionStart
  const end = el.selectionEnd
  const selected = value.slice(start, end)

  let next, cursorStart, cursorEnd

  if (tool.type === 'wrap') {
    const inner = selected || tool.placeholder
    next = value.slice(0, start) + tool.prefix + inner + tool.suffix + value.slice(end)
    // 선택이 없었으면 삽입한 placeholder 를 선택 상태로 둬서 바로 덮어쓸 수 있게 한다.
    cursorStart = start + tool.prefix.length
    cursorEnd = cursorStart + inner.length
  } else if (tool.type === 'line') {
    const lineStart = value.lastIndexOf('\n', start - 1) + 1
    const lineEnd = end < value.length && value.indexOf('\n', end) !== -1 ? value.indexOf('\n', end) : value.length
    const block = value.slice(lineStart, lineEnd)
    const prefixed = block.split('\n').map(l => l.startsWith(tool.prefix) ? l : tool.prefix + l).join('\n')
    next = value.slice(0, lineStart) + prefixed + value.slice(lineEnd)
    cursorStart = cursorEnd = lineStart + prefixed.length
  } else {
    const before = start > 0 && value[start - 1] !== '\n' ? '\n' : ''
    const after = value[end] && value[end] !== '\n' ? '\n' : ''
    next = value.slice(0, start) + before + tool.text + after + value.slice(end)
    cursorStart = cursorEnd = start + before.length + tool.text.length
  }

  emit('update:modelValue', next)
  await nextTick()
  el.focus()
  el.setSelectionRange(cursorStart, cursorEnd)
}

/** 폼 안에서 Tab 이 포커스를 옮기지 않고 들여쓰기가 되도록 한다. */
async function onTab(e) {
  const el = e.target
  const value = props.modelValue || ''
  const start = el.selectionStart
  const next = value.slice(0, start) + '  ' + value.slice(el.selectionEnd)
  emit('update:modelValue', next)
  await nextTick()
  el.focus()
  el.setSelectionRange(start + 2, start + 2)
}
</script>
