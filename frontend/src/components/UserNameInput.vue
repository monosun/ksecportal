<template>
  <!--
    사용자 입력 — 등록된 사용자를 목록에서 고르되, 등록되지 않은 사람(외부 승인권자·퇴사자 등)은 직접 입력할 수 있다.
    값은 이름 문자열로 저장한다. 기존 값이 사용자 목록에 없으면 직접 입력 상태로 연다.
  -->
  <div class="space-y-1.5">
    <select :value="selectValue" @change="onSelect($event.target.value)" :class="inputClass">
      <option value="">선택 안 함</option>
      <option v-for="u in users" :key="u.id" :value="u.name">
        {{ u.name }}{{ u.department ? ` (${u.department})` : '' }}
      </option>
      <option :value="CUSTOM">직접 입력...</option>
    </select>
    <input v-if="custom" ref="customInput" :value="modelValue" @input="emit('update:modelValue', $event.target.value)"
      :class="inputClass" :placeholder="placeholder" />
  </div>
</template>

<script>
import { adminApi } from '@/api'

// 모듈 스코프 — 화면·인스턴스가 여럿이어도 사용자 목록 조회는 1회만 한다
let cache = null

export function loadUsers() {
  if (!cache) {
    cache = adminApi.listUsersSimple()
      .then(res => res.data || [])
      .catch(() => { cache = null; return [] })   // 조회 실패해도 직접 입력은 가능해야 한다
  }
  return cache
}
</script>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  inputClass: { type: String, default: 'input w-full' },
  placeholder: { type: String, default: '이름을 직접 입력하세요' }
})
const emit = defineEmits(['update:modelValue'])

const CUSTOM = '__custom__'
const users = ref([])
const custom = ref(false)
const customInput = ref(null)

const isUserName = (v) => users.value.some(u => u.name === v)
const selectValue = computed(() => (custom.value ? CUSTOM : (props.modelValue || '')))

// 값이 밖에서 채워지면(수정 화면의 비동기 로드 등) 목록에 없는 값일 때만 직접 입력 상태로 연다.
// 직접 입력 중에는 타이핑한 값이 우연히 사용자 이름과 같아져도 입력창을 유지한다.
function syncMode() {
  const v = props.modelValue || ''
  custom.value = !!v && !isUserName(v)
}
watch(() => props.modelValue, () => { if (!custom.value) syncMode() })

async function onSelect(v) {
  if (v === CUSTOM) {
    custom.value = true
    emit('update:modelValue', '')
    await nextTick()
    customInput.value?.focus()
  } else {
    custom.value = false
    emit('update:modelValue', v)
  }
}

onMounted(async () => {
  users.value = await loadUsers()
  syncMode()
})
</script>
