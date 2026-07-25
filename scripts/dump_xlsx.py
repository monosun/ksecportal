"""의존성 없이 xlsx 시트 내용을 텍스트로 덤프한다 (openpyxl 미설치 환경용).

사용: python scripts/dump_xlsx.py <파일.xlsx> [최대행]
"""
import re
import sys
import zipfile
import xml.etree.ElementTree as ET

NS = '{http://schemas.openxmlformats.org/spreadsheetml/2006/main}'
RNS = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}'


def col_index(ref):
    m = re.match(r'([A-Z]+)', ref or '')
    if not m:
        return 0
    n = 0
    for ch in m.group(1):
        n = n * 26 + (ord(ch) - 64)
    return n - 1


def load_shared(z):
    try:
        root = ET.fromstring(z.read('xl/sharedStrings.xml'))
    except KeyError:
        return []
    out = []
    for si in root.findall(NS + 'si'):
        out.append(''.join(t.text or '' for t in si.iter(NS + 't')))
    return out


def sheet_names(z):
    wb = ET.fromstring(z.read('xl/workbook.xml'))
    rels = ET.fromstring(z.read('xl/_rels/workbook.xml.rels'))
    target = {r.get('Id'): r.get('Target') for r in rels}
    out = []
    for sh in wb.find(NS + 'sheets'):
        t = target.get(sh.get(RNS + 'id'), '')
        out.append((sh.get('name'), 'xl/' + t.lstrip('/').replace('xl/', '')))
    return out


def dump(path, max_rows=200):
    z = zipfile.ZipFile(path)
    shared = load_shared(z)
    for name, part in sheet_names(z):
        print('\n' + '=' * 70)
        print('[시트] %s  (%s)' % (name, part))
        print('=' * 70)
        try:
            root = ET.fromstring(z.read(part))
        except KeyError:
            print('  (읽기 실패)')
            continue
        data = root.find(NS + 'sheetData')
        if data is None:
            continue
        for i, row in enumerate(data.findall(NS + 'row')):
            if i >= max_rows:
                print('  ... (이하 생략)')
                break
            cells = {}
            for c in row.findall(NS + 'c'):
                v = c.find(NS + 'v')
                is_el = c.find(NS + 'is')
                if c.get('t') == 's' and v is not None:
                    text = shared[int(v.text)]
                elif is_el is not None:
                    text = ''.join(t.text or '' for t in is_el.iter(NS + 't'))
                elif v is not None:
                    text = v.text
                else:
                    text = ''
                text = (text or '').strip().replace('\n', ' / ')
                if text:
                    cells[col_index(c.get('r'))] = text
            if cells:
                width = max(cells) + 1
                cols = [cells.get(j, '') for j in range(width)]
                print('%3s | %s' % (row.get('r'), ' | '.join(cols)))


if __name__ == '__main__':
    dump(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 200)
