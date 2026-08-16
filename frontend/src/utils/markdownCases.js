/**
 * 사례집 마크다운을 "항목 카드 + 상세" 로 쪼갠다.
 *
 * <p>규칙은 두 문서(쉽게 이해하기 / 경영진 투자 사례)에 모두 맞도록 단순하게 잡았다.
 * <ul>
 *   <li>`# ` 머리글을 큰 덩어리로 본다.</li>
 *   <li>덩어리 안에 `## ` 가 있으면 <b>각 `##` 이 항목</b>이고 `#` 제목은 그룹 이름이 된다.
 *       (예: `# Part 1.` 아래 `## 사례 1..15`)</li>
 *   <li>`## ` 가 없으면 <b>덩어리 자체가 항목</b>이다. (예: `# 1. 아는 사람이 …`)</li>
 *   <li>맨 앞 덩어리가 번호 없는 제목이면 목록 위에 띄우는 <b>머리말</b>로 쓴다.</li>
 * </ul>
 * 문서를 손대지 않고 그대로 싣기 위한 방식이라, 원문 머리글 구조만 지키면 내용을 자유롭게 고칠 수 있다.
 */
export function parseCaseDoc(md) {
  const blocks = splitByHeading(String(md).replace(/\r\n?/g, '\n'), 1)
  const items = []
  const groupIntros = {}
  let intro = ''

  blocks.forEach((block, i) => {
    const { preamble, sections } = splitSections(block.body)

    if (sections.length === 0) {
      // 하위 머리글이 없는 덩어리 — 그 자체가 하나의 항목이다.
      if (i === 0 && !hasNumber(block.title)) { intro = preamble; return }
      items.push(makeItem(block.title, preamble, null))
      return
    }

    // 하위 머리글이 있는 덩어리 — 각 하위 머리글이 항목이고, 덩어리 제목은 그룹이 된다.
    // 다만 맨 앞 덩어리는 문서 제목이므로 그룹으로 쓰지 않고 머리말만 뽑는다.
    const group = i === 0 ? null : block.title
    if (i === 0) intro = preamble
    else if (preamble) groupIntros[group] = preamble
    sections.forEach(s => items.push(makeItem(s.title, s.body, group)))
  })

  return { intro, items, groupIntros }
}

/** `# ` / `## ` 머리글로 문서를 자른다. 코드펜스 안의 `#` 은 머리글로 보지 않는다. */
function splitByHeading(md, level) {
  const marker = new RegExp(`^#{${level}} (.+)$`)
  const blocks = []
  let current = null
  let fenced = false

  for (const line of md.split('\n')) {
    if (/^\s*```/.test(line)) fenced = !fenced
    const m = fenced ? null : marker.exec(line)
    if (m) {
      current = { title: m[1].trim(), lines: [] }
      blocks.push(current)
      continue
    }
    if (current) current.lines.push(line)
  }
  return blocks.map(b => ({ title: b.title, body: b.lines.join('\n').trim() }))
}

/** 덩어리 본문을 `## ` 기준으로 머리말 + 하위 절로 나눈다. */
function splitSections(body) {
  const sections = splitByHeading(body, 2)
  if (sections.length === 0) return { preamble: body, sections }
  const cut = body.indexOf('\n## ')
  const head = body.startsWith('## ') ? '' : (cut === -1 ? body : body.slice(0, cut))
  return { preamble: head.trim(), sections }
}

function makeItem(title, body, group) {
  const { label, text } = splitLabel(title)
  return {
    id: `${group || ''}|${title}`,
    title, label, text, group, body,
    summary: summarize(body),
    keywords: `${title} ${body}`.toLowerCase()
  }
}

/** "사례 3. 보안팀 인원이…" → 배지로 쓸 `사례 3` 과 본 제목으로 나눈다. */
function splitLabel(title) {
  const m = /^((?:사례|원칙|Part|Step)?\s*\d+(?:단계|장|차)?)\.\s*(.+)$/.exec(title)
  if (!m) return { label: '', text: title }
  return { label: m[1].trim(), text: m[2].trim() }
}

/** 카드에 보여 줄 한 줄 요약 — 첫 문단에서 마크다운 기호를 걷어낸다. */
function summarize(body) {
  let tableLine = ''
  for (const raw of body.split('\n')) {
    const line = raw.trim()
    if (!line) continue
    // 표로 시작하는 항목은 보여 줄 문장이 없으므로 첫 데이터 행을 요약으로 쓴다.
    if (line.startsWith('|')) {
      if (!tableLine && !/^\|[\s|:-]+\|$/.test(line)) {
        tableLine = line.replace(/^\||\|$/g, '').split('|').map(c => c.trim()).filter(Boolean).join(' · ')
      }
      continue
    }
    if (/^(#{1,6} |[-*_]{3,}$|```)/.test(line)) continue
    const text = line
      .replace(/^>\s*/, '')
      .replace(/^[-*]\s+/, '')
      .replace(/^\d+\.\s+/, '')
      .replace(/\*\*|__|[*_`]/g, '')
      .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
      .trim()
    if (text) return text
  }
  return tableLine
}

function hasNumber(title) {
  return /\d/.test(title.split('.')[0])
}
