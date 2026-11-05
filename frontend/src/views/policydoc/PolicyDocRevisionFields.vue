<template>
  <div class="space-y-3">
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
      <div>
        <label class="text-xs text-gray-500">버전 *</label>
        <input :value="modelValue.version" @input="set('version', $event.target.value)" class="input w-full mt-1"
          :placeholder="versionPlaceholder || '예: 1.0'" />
      </div>
      <div>
        <label class="text-xs text-gray-500">구분 *</label>
        <select :value="modelValue.revisionType" @change="set('revisionType', $event.target.value)" class="input w-full mt-1">
          <option v-for="t in REVISION_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
        </select>
      </div>
      <div>
        <label class="text-xs text-gray-500">제·개정일 *</label>
        <input type="date" :value="modelValue.revisionDate" @input="set('revisionDate', $event.target.value)" class="input w-full mt-1" />
      </div>
      <div>
        <label class="text-xs text-gray-500">시행일</label>
        <input type="date" :value="modelValue.effectiveDate" @input="set('effectiveDate', $event.target.value || null)" class="input w-full mt-1" />
      </div>
    </div>
    <div>
      <label class="text-xs text-gray-500">승인자</label>
      <UserNameInput :model-value="modelValue.approver" @update:model-value="set('approver', $event)" class="mt-1"
        placeholder="예: 정보보호최고책임자(CISO) 홍길동" />
    </div>
    <div>
      <label class="text-xs text-gray-500">{{ reasonLabel }}</label>
      <textarea :value="modelValue.changeReason" @input="set('changeReason', $event.target.value)" rows="2" class="input w-full mt-1"
        :placeholder="reasonPlaceholder"></textarea>
    </div>
    <div>
      <label class="text-xs text-gray-500">주요 개정사항</label>
      <textarea :value="modelValue.changeDetails" @input="set('changeDetails', $event.target.value)" rows="5" class="input w-full mt-1 font-mono text-xs"
        placeholder="예)&#10;- 제5조(접근통제) 비밀번호 변경주기 90일 → 180일&#10;- 제12조(개인정보 파기) 신설&#10;- 별표 2 서식 개정"></textarea>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import UserNameInput from '@/components/UserNameInput.vue'
import { REVISION_TYPES } from './policyDocMeta'

// 버전(개정본) 입력 항목 — 신규 문서 등록과 버전 추가·수정 화면이 함께 쓴다
const props = defineProps({
  modelValue: { type: Object, required: true },
  versionPlaceholder: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

function set(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}

const reasonLabel = computed(() => ({
  ENACTMENT: '제정 사유', ABOLITION: '폐지 사유',
}[props.modelValue.revisionType] || '개정 사유'))

const reasonPlaceholder = computed(() => ({
  ENACTMENT: '예: ISMS-P 인증 대비 정보보호 정책 체계 수립',
  ABOLITION: '예: 「정보보호 지침」으로 통합',
}[props.modelValue.revisionType] || '예: 개인정보보호법 개정사항 반영, 내부감사 지적사항 조치'))
</script>
