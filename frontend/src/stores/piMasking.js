import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { codeApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { MASKERS, MASKING_TYPE, kindOfLabel, maskValue } from '@/utils/piMasking'

/**
 * 화면 목록에 표시되는 개인정보를 마스킹한다.
 *
 * 마스킹 방식은 관리 > 코드관리 > 개인정보 유형별 항목관리에 등록된 항목별 기준을 따르며,
 * 기준을 아직 불러오지 못했거나 등록되지 않은 항목은 안전하게 '부분 마스킹'으로 처리한다.
 * 원문 열람(마스킹 해제)은 기본적으로 ADMIN 만 가능하고 해제할 때마다 감사로그를 남긴다.
 *
 * 예외로 MANAGER_REVEALABLE_SCREENS 에 등록한 화면은 MANAGER 도 해제할 수 있다.
 * 이때 해제는 그 화면에서만 유효해서 다른 화면으로 이동하면 다시 마스킹된다(ADMIN 은 종전대로 전 화면 유지).
 */

/**
 * ADMIN 외에 MANAGER 도 원문을 열람할 수 있는 화면.
 * 비상연락망은 사고 발생 시 담당자에게 즉시 연락해야 해서, 대응 실무자인 MANAGER 에게도 열람을 허용한다.
 * 값은 PiMaskToggle 의 screen 속성(감사로그에 남는 화면 이름)과 같아야 한다.
 */
const MANAGER_REVEALABLE_SCREENS = new Set(['비상연락망'])

/** 화면에서 쓰는 별칭 → 코드관리 항목명 후보(앞에 있을수록 우선) */
const ALIAS_LABELS = {
  name: ['성명(법인명)', '성명', '이름', '법정대리인 이름'],
  email: ['이메일 주소', '이메일'],
  phone: ['이동전화번호', '휴대전화번호', '연락 가능한 전화번호', '회선번호'],
  landline: ['유선전화번호'],
  contact: ['연락 가능한 전화번호', '이동전화번호', '이메일 주소'],
  address: ['주소(거주지, 설치장소, 배송지 등)', '주소'],
  postal: ['우편번호'],
  bizNo: ['사업자등록번호'],
  birth: ['생년월일'],
  rrn: ['주민등록번호'],
  account: ['계좌번호'],
  card: ['카드번호'],
  ip: ['IP 주소'],
  deviceId: ['단말식별번호(Device ID)', 'IMEI'],
  mac: ['MAC Address'],
}

export const usePiMaskingStore = defineStore('piMasking', () => {
  const rules = ref([])
  const loaded = ref(false)
  let loading = null

  /** 원문 열람 중인지 — 마스킹을 해제한 상태 */
  const revealed = ref(false)
  /** 현재 화면 — PiMaskToggle 이 mount 시 등록한다 */
  const activeScreen = ref('')
  /** 해제를 승인받은 화면 — MANAGER 의 해제는 이 화면에서만 유효하다 */
  const revealedScreen = ref('')

  const auth = useAuthStore()

  /** 해당 화면에서 원문을 열람할 수 있는지 */
  function canRevealOn(screen) {
    if (auth.isAdmin) return true
    return auth.isManager && MANAGER_REVEALABLE_SCREENS.has(screen)
  }
  const canReveal = computed(() => canRevealOn(activeScreen.value))

  // 계정이 바뀌어 권한을 잃으면 해제 상태가 남아 있어도 다시 마스킹한다
  const isRevealed = computed(() => {
    if (!revealed.value) return false
    if (auth.isAdmin) return true
    return canReveal.value && revealedScreen.value === activeScreen.value
  })

  /** 화면 진입 시 현재 화면을 등록한다 */
  function setScreen(screen) {
    activeScreen.value = screen || ''
  }

  /**
   * 화면을 떠날 때 등록을 해제한다.
   * 다음 화면이 먼저 등록될 수도 있어(마운트·언마운트 순서), 아직 내 화면일 때만 지운다.
   */
  function clearScreen(screen) {
    if (activeScreen.value === screen) activeScreen.value = ''
  }

  /** 항목명 → 마스킹 기준 */
  const byLabel = computed(() => {
    const m = {}
    rules.value.forEach(r => { if (!m[r.label]) m[r.label] = r })
    return m
  })

  async function load() {
    if (loaded.value) return
    if (loading) return loading
    loading = (async () => {
      try {
        const res = await codeApi.piMaskingRules()
        rules.value = res.data || []
      } catch {
        // 기준을 못 불러와도 화면은 떠야 하므로 기본 규칙으로 마스킹한다
        rules.value = []
      } finally {
        loaded.value = true
        loading = null
      }
    })()
    return loading
  }

  /** 별칭(또는 코드관리 항목명)에 해당하는 기준을 찾는다 */
  function ruleOf(alias) {
    const candidates = ALIAS_LABELS[alias] || [alias]
    for (const label of candidates) {
      const found = byLabel.value[label]
      if (found) return found
    }
    return null
  }

  /**
   * 목록에 표시할 값을 마스킹한다.
   * @param {string} alias 항목 별칭(name·email·phone·ip …) 또는 코드관리 항목명
   * @param {*} value 원본 값
   */
  function mask(alias, value) {
    if (isRevealed.value) return value
    if (value === null || value === undefined || value === '') return value
    const rule = ruleOf(alias)
    const kind = MASKERS[alias] ? alias : kindOfLabel(rule?.label || alias)
    return maskValue(value, rule?.maskingType || MASKING_TYPE.PARTIAL, kind)
  }

  /** 해당 항목이 실제로 가려지는지 — 안내 문구 노출 판단에 쓴다 */
  function isMasked(alias) {
    if (isRevealed.value) return false
    return (ruleOf(alias)?.maskingType || MASKING_TYPE.PARTIAL) !== MASKING_TYPE.NONE
  }

  /** 항목별 마스킹 기준 설명(툴팁용) */
  function ruleText(alias) {
    const r = ruleOf(alias)
    if (!r) return '기준 미등록 — 기본 부분 마스킹이 적용됩니다.'
    return [r.label, r.maskingType, r.maskingRule].filter(Boolean).join(' · ')
  }

  /** 마스킹 해제/재적용. 해제 시 감사로그를 남긴다. */
  async function toggleReveal(screen) {
    if (!canRevealOn(screen)) return false
    const next = !revealed.value
    if (next) {
      try {
        await codeApi.logPiUnmask({ screen, reason: '목록 화면 원문 열람' })
      } catch {
        // 감사로그 실패가 화면 조작을 막지는 않는다(서버 로그로 남는다)
      }
    }
    revealed.value = next
    revealedScreen.value = next ? screen : ''
    return revealed.value
  }

  function reset() {
    revealed.value = false
    revealedScreen.value = ''
  }

  return {
    rules, loaded, revealed: isRevealed, canReveal, canRevealOn,
    load, setScreen, clearScreen, mask, isMasked, ruleText, toggleReveal, reset,
  }
})
