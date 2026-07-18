<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ $t('admin.menuOrder') }}</h1>
        <p class="text-sm text-gray-400 mt-0.5">좌측 메뉴의 상위 그룹을 추가·삭제·이름변경하고, 하위 메뉴를 다른 상위 메뉴로 이동하거나 순서를 변경합니다.</p>
      </div>
      <div class="flex items-center gap-2">
        <button @click="addGroup" class="btn-secondary flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          상위 메뉴 추가
        </button>
        <button @click="resetToDefault" class="btn-secondary">기본값으로 초기화</button>
        <button @click="save" :disabled="saving || !dirty" class="btn-primary">
          {{ saving ? '저장 중...' : '저장' }}
        </button>
      </div>
    </div>

    <div class="page-body">
      <p v-if="savedMsg" class="mb-4 text-sm text-green-600 bg-green-50 border border-green-100 rounded-lg px-3 py-2">
        {{ savedMsg }}
      </p>

      <div class="space-y-3">
        <div
          v-for="(group, gi) in groups"
          :key="group.key"
          class="card p-0 overflow-hidden"
          :class="{ 'ring-2 ring-primary-400': dragGroupIndex === gi }"
          draggable="true"
          @dragstart="onGroupDragStart(gi, $event)"
          @dragover.prevent="onGroupDragOver(gi)"
          @dragend="onGroupDragEnd"
        >
          <!-- 그룹 헤더 -->
          <div class="flex items-center gap-3 px-4 py-3 bg-gray-50 border-b border-gray-100">
            <span class="cursor-grab text-gray-300 select-none" title="드래그하여 이동">⠿</span>
            <button @click="toggleExpand(group.key)" class="flex items-center gap-2 shrink-0">
              <svg class="w-3.5 h-3.5 text-gray-400 transition-transform" :class="expanded[group.key] ? 'rotate-90' : ''"
                fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M9 5l7 7-7 7"/>
              </svg>
            </button>

            <!-- 이름 (인라인 편집) -->
            <template v-if="editingKey === group.key">
              <input ref="editInput" v-model="editingText" @keyup.enter="commitRename(group)"
                @keyup.esc="cancelRename" @blur="commitRename(group)"
                class="input py-1 text-sm font-bold w-64" />
            </template>
            <div v-else class="flex items-center gap-2 flex-1 min-w-0">
              <span class="font-bold text-gray-800 truncate">{{ groupName(group) }}</span>
              <span v-if="group.custom" class="badge-blue text-[10px]">사용자 추가</span>
              <button @click="startRename(group)" class="text-gray-300 hover:text-primary-500 shrink-0" title="이름 변경">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                </svg>
              </button>
            </div>

            <span class="text-xs text-gray-400 shrink-0">하위 {{ group.items.length }}개</span>
            <div class="flex items-center gap-1 shrink-0">
              <button @click="moveGroup(gi, -1)" :disabled="gi === 0"
                class="p-1.5 rounded hover:bg-gray-200 disabled:opacity-30 disabled:hover:bg-transparent" title="위로">
                <ArrowIcon dir="up" />
              </button>
              <button @click="moveGroup(gi, 1)" :disabled="gi === groups.length - 1"
                class="p-1.5 rounded hover:bg-gray-200 disabled:opacity-30 disabled:hover:bg-transparent" title="아래로">
                <ArrowIcon dir="down" />
              </button>
              <button @click="removeGroup(gi)"
                class="p-1.5 rounded hover:bg-red-50 text-gray-400 hover:text-red-500" title="상위 메뉴 삭제">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 하위 메뉴 -->
          <div v-if="expanded[group.key]" class="divide-y divide-gray-50">
            <div v-if="!group.items.length" class="pl-10 pr-4 py-3 text-sm text-gray-400">
              하위 메뉴가 없습니다. 다른 상위 메뉴에서 이곳으로 이동할 수 있습니다.
            </div>
            <div
              v-for="(item, ii) in group.items"
              :key="item.to"
              class="flex items-center gap-3 pl-10 pr-4 py-2.5"
              :class="{ 'bg-primary-50': dragItem && dragItem.groupKey === group.key && dragItem.index === ii }"
              draggable="true"
              @dragstart.stop="onItemDragStart(group.key, ii, $event)"
              @dragover.prevent.stop="onItemDragOver(group.key, ii)"
              @dragend.stop="onItemDragEnd"
            >
              <span class="cursor-grab text-gray-300 select-none" title="드래그하여 이동">⠿</span>
              <span class="flex-1 text-sm text-gray-700 truncate">{{ $t(item.label) }}</span>
              <span class="text-xs text-gray-300 font-mono hidden sm:inline">{{ item.to }}</span>

              <!-- 상위 메뉴 이동 -->
              <select :value="group.key" @change="moveItemToGroup(gi, ii, $event.target.value)"
                class="input py-1 text-xs w-40" title="다른 상위 메뉴로 이동">
                <option v-for="g in groups" :key="g.key" :value="g.key">{{ groupName(g) }}</option>
              </select>

              <div class="flex items-center gap-1">
                <button @click="moveItem(gi, ii, -1)" :disabled="ii === 0"
                  class="p-1.5 rounded hover:bg-gray-100 disabled:opacity-30 disabled:hover:bg-transparent" title="위로">
                  <ArrowIcon dir="up" />
                </button>
                <button @click="moveItem(gi, ii, 1)" :disabled="ii === group.items.length - 1"
                  class="p-1.5 rounded hover:bg-gray-100 disabled:opacity-30 disabled:hover:bg-transparent" title="아래로">
                  <ArrowIcon dir="down" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUiSettingsStore } from '@/stores/uiSettings'
import { navGroups, resolveMenu } from '@/config/navMenu'

const ui = useUiSettingsStore()
const { t } = useI18n()

const saving = ref(false)
const dirty = ref(false)
const savedMsg = ref('')
const expanded = reactive({})

// 화살표 아이콘 (렌더 함수로 인라인 정의)
const ArrowIcon = (props) => h('svg', {
  class: 'w-3.5 h-3.5 text-gray-500', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24'
}, [h('path', {
  'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2.5',
  d: props.dir === 'up' ? 'M5 15l7-7 7 7' : 'M19 9l-7 7-7-7'
})])

// 현재 저장된 커스터마이즈를 반영한 작업용 모델 생성
function buildModel(cfg) {
  return resolveMenu(navGroups, cfg).map(g => ({
    key: g.key,
    custom: g.custom,
    labelKey: g.labelKey,          // 기본 그룹의 i18n 키 (커스텀은 null)
    labelText: g.labelText || '',  // 이름 변경/커스텀 표시명
    items: g.items.map(it => ({ to: it.to, label: it.label })),
  }))
}

const groups = ref(buildModel(ui.menuOrder))

function groupName(g) {
  return g.labelText || (g.labelKey ? t(g.labelKey) : '메뉴')
}

function toggleExpand(key) {
  expanded[key] = !expanded[key]
}

function markDirty() {
  dirty.value = true
  savedMsg.value = ''
}

// ── 이름 변경 ─────────────────────────────────────────────────
const editingKey = ref(null)
const editingText = ref('')
const editInput = ref(null)

async function startRename(group) {
  editingKey.value = group.key
  editingText.value = groupName(group)
  await nextTick()
  const el = Array.isArray(editInput.value) ? editInput.value[0] : editInput.value
  el?.focus?.()
  el?.select?.()
}
function commitRename(group) {
  if (editingKey.value !== group.key) return
  const text = editingText.value.trim()
  const prev = group.labelText
  // 기본 그룹에서 원래 이름과 같게 두면 override 를 제거(i18n 기본값 사용)
  group.labelText = (!group.custom && text === t(group.labelKey)) ? '' : text
  if (group.labelText !== prev) markDirty()
  editingKey.value = null
}
function cancelRename() {
  editingKey.value = null
}

// ── 상위 메뉴 추가/삭제 ───────────────────────────────────────
function addGroup() {
  const key = `custom_${Date.now()}`
  groups.value.push({ key, custom: true, labelKey: null, labelText: '새 메뉴', items: [] })
  expanded[key] = true
  markDirty()
  const g = groups.value[groups.value.length - 1]
  startRename(g)
}

function removeGroup(gi) {
  const g = groups.value[gi]
  if (g.items.length > 0) {
    alert('하위 메뉴가 있는 상위 메뉴는 삭제할 수 없습니다.\n하위 메뉴를 다른 상위 메뉴로 먼저 이동해 주세요.')
    return
  }
  if (!confirm(`"${groupName(g)}" 상위 메뉴를 삭제하시겠습니까?`)) return
  groups.value.splice(gi, 1)
  delete expanded[g.key]
  markDirty()
}

// ── 버튼 이동 ─────────────────────────────────────────────────
function moveGroup(index, delta) {
  const target = index + delta
  if (target < 0 || target >= groups.value.length) return
  const arr = groups.value
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
  markDirty()
}

function moveItem(gi, ii, delta) {
  const items = groups.value[gi].items
  const target = ii + delta
  if (target < 0 || target >= items.length) return
  ;[items[ii], items[target]] = [items[target], items[ii]]
  markDirty()
}

// 하위 메뉴를 다른 상위 메뉴로 이동
function moveItemToGroup(gi, ii, targetKey) {
  if (targetKey === groups.value[gi].key) return
  const target = groups.value.find(g => g.key === targetKey)
  if (!target) return
  const [moved] = groups.value[gi].items.splice(ii, 1)
  target.items.push(moved)
  expanded[targetKey] = true
  markDirty()
}

// ── 그룹 드래그 앤 드롭 ───────────────────────────────────────
const dragGroupIndex = ref(null)
function onGroupDragStart(index, e) {
  dragGroupIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
}
function onGroupDragOver(index) {
  const from = dragGroupIndex.value
  if (from === null || from === index) return
  const arr = groups.value
  const [moved] = arr.splice(from, 1)
  arr.splice(index, 0, moved)
  dragGroupIndex.value = index
  markDirty()
}
function onGroupDragEnd() {
  dragGroupIndex.value = null
}

// ── 아이템 드래그 앤 드롭 (동일 그룹 내에서만) ────────────────
const dragItem = ref(null)
function onItemDragStart(groupKey, index, e) {
  dragItem.value = { groupKey, index }
  e.dataTransfer.effectAllowed = 'move'
}
function onItemDragOver(groupKey, index) {
  const d = dragItem.value
  if (!d || d.groupKey !== groupKey || d.index === index) return
  const group = groups.value.find(g => g.key === groupKey)
  const items = group.items
  const [moved] = items.splice(d.index, 1)
  items.splice(index, 0, moved)
  dragItem.value = { groupKey, index }
  markDirty()
}
function onItemDragEnd() {
  dragItem.value = null
}

// ── 저장 / 초기화 ─────────────────────────────────────────────
function currentCfg() {
  const workingKeys = new Set(groups.value.map(g => g.key))
  const items = {}
  const labels = {}
  const custom = []
  groups.value.forEach(g => {
    items[g.key] = g.items.map(it => it.to)
    if (g.custom) {
      custom.push({ key: g.key, label: g.labelText || '메뉴' })
    } else if (g.labelText) {
      labels[g.key] = g.labelText
    }
  })
  // 작업 목록에 없는 기본 그룹 = 삭제(숨김)
  const hidden = navGroups.map(g => g.key).filter(k => !workingKeys.has(k))
  return { groups: groups.value.map(g => g.key), items, labels, custom, hidden }
}

async function save() {
  saving.value = true
  savedMsg.value = ''
  try {
    await ui.saveMenuOrder(currentCfg())
    dirty.value = false
    savedMsg.value = '메뉴 구성이 저장되었습니다. 좌측 메뉴에 즉시 반영됩니다.'
  } catch (e) {
    savedMsg.value = '저장 실패: ' + (typeof e === 'string' ? e : (e?.message || '알 수 없는 오류'))
  } finally {
    saving.value = false
  }
}

function resetToDefault() {
  if (!confirm('상위 메뉴 추가·삭제·이름변경·이동 내용을 모두 초기화하고 기본 메뉴 구성으로 되돌립니다. 진행하시겠습니까?')) return
  groups.value = buildModel(null)
  markDirty()
}
</script>
