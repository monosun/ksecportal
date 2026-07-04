<template>
  <span :class="badgeClass" class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium">
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({ status: String })
const { t } = useI18n()

const badgeClass = computed(() => {
  switch (props.status) {
    case 'COMPLIANT': return 'bg-green-100 text-green-800'
    case 'PARTIAL': return 'bg-yellow-100 text-yellow-800'
    case 'NON_COMPLIANT': return 'bg-red-100 text-red-800'
    case 'NA': return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-400'
  }
})

const label = computed(() => {
  switch (props.status) {
    case 'COMPLIANT': return t('isms.statusCompliant')
    case 'PARTIAL': return t('isms.statusPartial')
    case 'NON_COMPLIANT': return t('isms.statusNonCompliant')
    case 'NA': return t('isms.statusNa')
    default: return t('isms.noEvidence')
  }
})
</script>
