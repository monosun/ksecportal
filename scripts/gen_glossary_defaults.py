"""참고 엑셀(security_in_computing_glossary.xlsx)에서 보안용어집 기본 데이터 Java 상수를 생성한다.

'보안 용어집' 시트: 번호 | 한글 용어 | 영문 표기 | 약어 | 분류 | 의미 | 관련 키워드

사용: python scripts/gen_glossary_defaults.py
"""
import io
import os
import sys
import xml.etree.ElementTree as ET
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dump_xlsx as X  # noqa: E402

SRC = 'docs/security_in_computing_glossary.xlsx'
OUT = 'backend/src/main/java/com/monosun/secportal/glossary/GlossaryDefaults.java'
SHEET = '보안 용어집'


def read_sheet(path, sheet_name):
    z = zipfile.ZipFile(path)
    shared = X.load_shared(z)
    part = next(p for n, p in X.sheet_names(z) if n == sheet_name)
    root = ET.fromstring(z.read(part))
    rows = []
    for row in root.find(X.NS + 'sheetData').findall(X.NS + 'row'):
        cells = {}
        for c in row.findall(X.NS + 'c'):
            v = c.find(X.NS + 'v')
            is_el = c.find(X.NS + 'is')
            if c.get('t') == 's' and v is not None:
                text = shared[int(v.text)]
            elif is_el is not None:
                # 이 파일은 공유 문자열 대신 인라인 문자열(t="inlineStr")을 쓴다
                text = ''.join(t.text or '' for t in is_el.iter(X.NS + 't'))
            elif v is not None:
                text = v.text
            else:
                text = ''
            parts = [' '.join(ln.split()) for ln in (text or '').splitlines()]
            text = ' '.join(p for p in parts if p)
            if text:
                cells[X.col_index(c.get('r'))] = text
        rows.append((int(row.get('r')), cells))
    return rows


def esc(s):
    return (s or '').replace('\\', '\\\\').replace('"', '\\"')


def main():
    rows = read_sheet(SRC, SHEET)
    terms = []
    for rnum, c in rows:
        if rnum < 2:            # 1행은 헤더
            continue
        name = c.get(1, '')     # 한글 용어
        if not name:
            continue
        terms.append({
            'name': name,
            'nameEn': c.get(2, ''),
            'abbr': c.get(3, ''),
            'category': c.get(4, ''),
            'definition': c.get(5, ''),
            'keywords': c.get(6, ''),
        })

    lines = []
    for i, t in enumerate(terms, start=1):
        lines.append('            d("%s", "%s", "%s", "%s", "%s", "%s", %d)'
                     % (esc(t['name']), esc(t['nameEn']), esc(t['abbr']), esc(t['category']),
                        esc(t['definition']), esc(t['keywords']), i))

    categories = []
    for t in terms:
        if t['category'] and t['category'] not in categories:
            categories.append(t['category'])

    java = '''package com.monosun.secportal.glossary;

import java.util.List;

/**
 * 보안용어집 기본 데이터 — "Security in Computing 보안 용어집" 자료를 참고해 구성한 표준 용어 %d건.
 *
 * 최초 기동 시 {@code glossary_terms} 테이블이 비어 있을 때만 시드되며,
 * 이후에는 관리 &gt; 코드 관리 &gt; 용어집 탭에서 관리한다.
 *
 * 이 파일은 scripts/gen_glossary_defaults.py 로 생성된다 — 직접 수정하지 말고 스크립트를 다시 실행할 것.
 *
 * 분류(%d종): %s
 */
public final class GlossaryDefaults {

    private GlossaryDefaults() {}

    public static final List<Row> TERMS = List.of(
%s
    );

    /** 기본 용어 한 줄 */
    public record Row(String name, String nameEn, String abbreviation, String category,
                      String definition, String keywords, int sortOrder) {}

    private static Row d(String name, String nameEn, String abbreviation, String category,
                         String definition, String keywords, int sortOrder) {
        return new Row(name, nameEn, abbreviation, category, definition, keywords, sortOrder);
    }
}
''' % (len(terms), len(categories), ' · '.join(categories), ',\n'.join(lines))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    io.open(OUT, 'w', encoding='utf-8', newline='\n').write(java)
    print('생성: %s  (용어 %d건 / 분류 %d종)' % (OUT, len(terms), len(categories)))
    print('분류: %s' % ' · '.join(categories))


if __name__ == '__main__':
    main()
