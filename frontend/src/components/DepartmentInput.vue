<template>
  <!--
    부서 입력 — 코드관리(DEPARTMENT)에 등록된 부서를 목록에서 고르되, 목록에 없는 부서는 직접 입력할 수 있다.
    기존 데이터에 코드값에 없는 부서가 남아 있으므로(예: CS운영팀) 엄격한 select 로 만들지 않는다.
  -->
  <input
    v-model="model"
    :list="listId"
    :class="inputClass"
    :placeholder="placeholder"
    autocomplete="off" />
  <datalist :id="listId">
    <option v-for="d in departments" :key="d.value" :value="d.value">{{ d.label }}</option>
  </datalist>
</template>

<script>
import { codeApi } from '@/api'

// 모듈 스코프 — 화면·인스턴스가 여럿이어도 코드값 조회는 1회만 한다
let seq = 0
let cache = null

export function loadDepartments() {
  if (!cache) {
    cache = codeApi.getValues('DEPARTMENT')
      .then(res => res.data || [])
      .catch(() => [])   // 조회 실패해도 직접 입력은 가능해야 하므로 빈 목록으로 진행
  }
  return cache
}

export function nextListId() {
  return `dept-options-${++seq}`
}
</script>

<script setup>
import { computed, onMounted, ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  inputClass: { type: String, default: 'input w-full' },
  placeholder: { type: String, default: '부서 선택 또는 직접 입력' }
})
const emit = defineEmits(['update:modelValue'])

const model = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

// datalist 는 id 로 input 과 연결되므로 인스턴스마다 고유해야 한다
const listId = nextListId()

const departments = ref([])
onMounted(async () => { departments.value = await loadDepartments() })
</script>
