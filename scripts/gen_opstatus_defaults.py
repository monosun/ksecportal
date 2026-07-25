"""참고 엑셀(1.3.3.정보보호_관리체계_운영현황표)에서 운영현황관리 기본 항목 Java 상수를 생성한다.

시트1 = 정보보호관리체계(ISMS), 시트2 = 개인정보보호관리체계 연간 운영(PRIVACY).
월 표시(○ / 예정)는 계획(plan) 비트마스크로 변환한다. (bit 0 = 1월)

사용: python scripts/gen_opstatus_defaults.py
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dump_xlsx as X  # noqa: E402

SRC = 'docs/1.3.3.정보보호_관리체계_운영현황표v1.1_250424.xlsx'
OUT = 'backend/src/main/java/com/monosun/secportal/opstatus/OperationStatusDefaults.java'


def read_rows(path, sheet_index):
    import xml.etree.ElementTree as ET
    import zipfile
    z = zipfile.ZipFile(path)
    shared = X.load_shared(z)
    name, part = X.sheet_names(z)[sheet_index]
    root = ET.fromstring(z.read(part))
    out = []
    for row in root.find(X.NS + 'sheetData').findall(X.NS + 'row'):
        cells = {}
        for c in row.findall(X.NS + 'c'):
            v = c.find(X.NS + 'v')
            if c.get('t') == 's' and v is not None:
                text = shared[int(v.text)]
            elif v is not None:
                text = v.text
            else:
                text = ''
            # 셀 안의 줄바꿈은 산출물 목록 구분이므로 ' / ' 로 보존한다
            parts = [' '.join(ln.split()) for ln in (text or '').splitlines()]
            text = ' / '.join(p for p in parts if p)
            if text:
                cells[X.col_index(c.get('r'))] = text
        out.append((int(row.get('r')), cells))
    return name, out


def clean_note(v):
    """비고 칸에 월 표시(○)만 들어간 경우가 있어 기호만 있는 값은 버린다."""
    v = (v or '').strip()
    return '' if v in ('○', '●', '-', 'ㅇ') else v


def esc(s):
    return (s or '').replace('\\', '\\\\').replace('"', '\\"')


def mask(cells, first_month_col):
    """월 컬럼 12개에 표시가 있으면 해당 비트를 켠다."""
    bits = 0
    for m in range(12):
        if cells.get(first_month_col + m):
            bits |= (1 << m)
    return bits


def build_isms():
    _, rows = read_rows(SRC, 0)
    # 4행 헤더: 1=구분 2=점검기준 3=주기 4=보안적용실적 5=책임자 6=실무자 7~18=1~12월 19=비고
    items = []
    category = ''
    for rnum, c in rows:
        if rnum < 5 or not c.get(2):
            continue
        category = c.get(1) or category  # 병합셀이라 구분은 첫 행에만 있다
        items.append({
            'category': category,
            'name': c.get(2, ''),
            'cycle': c.get(3, ''),
            'deliverable': c.get(4, ''),
            'owner': c.get(5, ''),
            'manager': c.get(6, ''),
            'note': clean_note(c.get(19, '')),
            'plan': mask(c, 7),
        })
    return items


# 개인정보 시트는 셀 폭이 좁아 단어 중간에 줄바꿈이 들어가 있다("내부관리계획 업\n데이트").
# 자동 추출하면 "업 데이트" 처럼 깨지므로 문구만 아래 표로 정제하고, 월별 계획은 엑셀에서 그대로 읽는다.
PRIVACY_TEXT = [
    ('개인정보처리방침 업데이트', '연 1회 이상 (권장)',
     '개인정보처리방침 점검 / 연 1회 이상 정기적 점검 권장 / 변경사항 발생 시 지체 없이 변경'
     ' / 제3자 제공·위탁 및 재위탁 등의 추가 / 양립성 평가에 의한 추가처리의 판단기준'
     ' / 가명정보·공개정보 등의 처리에 관한 사항 / 국내대리인의 성명·주소·전화번호 및 전자우편주소 등'),
    ('내부관리계획 업데이트', '연 1회 이상 (권장)',
     '내부관리계획 점검 / 연 1회 이상 정기적 점검 권장 / 중요한 변경이 있는 경우 즉시 반영 / 수정 이력 관리'),
    ('내부관리계획 이행실태 점검·관리 (접근권한 관리, 접속기록 보관 및 점검, 암호화 조치 등)', '연 1회 이상 (의무)',
     '내부관리계획 점검 / 내부관리계획 이행 실태 연 1회 이상 점검·관리'
     ' / 접근권한 관리, 접속기록 보관 및 점검, 암호화 조치 등'),
    ('개인정보보호교육', '연 1회 이상 (권장/의무)',
     '취급자에 대한 개인정보보호교육 / 매년 교육 계획 수립·실시'
     ' / 모든 개인정보취급자가 연 1회 이상 교육 이수 / 대표자·CPO도 교육 대상'),
    ('수탁자 관리·감독 (처리현황 점검)', '연 1회 이상 (권장/의무)',
     '수탁자 관리·감독 / 개인정보 처리현황 등 점검 / 위탁업무 수행 목적 외 개인정보 처리 여부'
     ' / 개인정보 파기 여부 및 파기 방법의 적절성 확인 / 기술적·관리적 보호조치 여부'
     ' / 재위탁 제한 위반 여부 / 접근제한 등 안전성 확보 조치 여부'
     ' / 그 밖에 법 또는 영에 따라 수탁자가 준수해야 할 사항 / 개인정보보호교육 정기 실시'
     ' / 위·수탁 계약 체결 및 계약서 내용 점검'),
    ('홈페이지 취약점 점검', '연 1회 이상 (의무)',
     '홈페이지 취약점 점검 / 고유식별정보 유출·변조·훼손 방지를 위해 연 1회 이상 취약점 점검'
     ' / 검색엔진을 통한 개인정보 DB 노출 여부 / P2P 등을 통한 고객정보 공유설정 여부'
     ' / 운영자·담당자 등의 부주의에 의한 개인정보 게시 여부'
     ' / 파라미터 변조, Credential stuffing 공격 등 해킹 취약점 점검'),
    ('이용내역 통지', '연 1회 이상 (의무)',
     '이용내역 통지 의무 / 서면 등의 방법으로 연 1회 이상 통지'
     ' / 정보통신서비스 부문 매출액 100억 원 이상이거나 저장·관리되고 있는 이용자 수'
     ' 일일평균 100만 명 이상인 정보통신서비스 제공자 등'),
    ('개인정보 수집출처 고지 여부 점검', '3개월 이내 의무 (정기적 권장)',
     '개인정보 수집출처 통지 / 수집출처 통지 여부 정기적 점검'
     ' / 정보주체 요구 시 지체 없이, 제3자로부터 수집 시 30일 이내'
     ' / (100만 명 이상, 5만 명 이상 민감·고유식별정보)'),
    ('고유식별정보 안전성 확보조치 조사·보고', '2년 1회 이상 (의무)',
     '고유식별정보 안전성 확보조치 / 암호화 여부 조사 / 주민등록번호의 수집·보관 근거'
     ' / 안전성 확보조치 조사·보고 2년마다 1회 이상'),
    ('손해배상보험 등의 유지 여부 확인·점검', '매년 6월 확인 (정기적 권장)',
     '보험·공제·준비금 등 손해배상책임 이행 조치 / 유효기간·준비금액 등의 정기적 확인·점검'
     ' / 매출액 5천만 원 이상이거나 저장·관리되고 있는 이용자 수 일일평균 1천 명 이상인'
     ' 정보통신서비스 제공자 등'),
]


def build_privacy():
    _, rows = read_rows(SRC, 1)
    # 4행 헤더: 1=번호 2=점검항목 3=점검주기 4=상세내용 5~16=1~12월
    masks = [mask(c, 5) for rnum, c in rows if rnum >= 5 and c.get(2)]
    if len(masks) != len(PRIVACY_TEXT):
        raise SystemExit('엑셀 행 수(%d)와 정제 문구 수(%d)가 다릅니다 — PRIVACY_TEXT 를 맞추세요.'
                         % (len(masks), len(PRIVACY_TEXT)))
    return [{
        'category': '', 'owner': '', 'manager': '', 'note': '',
        'name': name, 'cycle': cycle, 'deliverable': detail, 'plan': m,
    } for (name, cycle, detail), m in zip(PRIVACY_TEXT, masks)]


def emit(items, indent='            '):
    # List.of(...) 는 후행 콤마를 허용하지 않으므로 마지막 줄에는 콤마를 붙이지 않는다
    lines = []
    for it in items:
        lines.append(
            '%sd("%s", "%s", "%s", "%s", "%s", "%s", "%s", 0b%012d)' % (
                indent, esc(it['category']), esc(it['name']), esc(it['cycle']),
                esc(it['deliverable']), esc(it['owner']), esc(it['manager']), esc(it['note']),
                int(bin(it['plan'])[2:]) if it['plan'] else 0,
            ))
    return ',\n'.join(lines)


def main():
    isms = build_isms()
    privacy = build_privacy()

    java = '''package com.monosun.secportal.opstatus;

import com.monosun.secportal.opstatus.entity.OperationStatusItem;

import java.util.List;

/**
 * 운영현황관리 기본 항목 — "정보보호 관리체계 운영현황표" 서식을 참고해 구성한 표준 점검 항목.
 *
 * 연도별 화면에서 "기본 항목 불러오기"를 누르면 이 목록이 해당 연도로 복제된다.
 * planMonths 는 12비트 마스크(bit 0 = 1월)로 서식의 월별 계획 표시(○ / 예정)를 옮긴 것이며,
 * 실제 이행 실적(doneMonths)은 항상 0으로 시작한다.
 *
 * 이 파일은 scripts/gen_opstatus_defaults.py 로 생성된다 — 직접 수정하지 말고 스크립트를 다시 실행할 것.
 */
public final class OperationStatusDefaults {

    private OperationStatusDefaults() {}

    /** 정보보호 관리체계 (%d개 항목) */
    public static final List<Row> ISMS = List.of(
%s
    );

    /** 개인정보보호 관리체계 연간 운영 (%d개 항목) */
    public static final List<Row> PRIVACY = List.of(
%s
    );

    public static List<Row> of(OperationStatusItem.Type type) {
        return type == OperationStatusItem.Type.PRIVACY ? PRIVACY : ISMS;
    }

    /** 기본 항목 한 줄 */
    public record Row(String category, String name, String cycle, String deliverable,
                      String owner, String manager, String note, int planMonths) {}

    private static Row d(String category, String name, String cycle, String deliverable,
                         String owner, String manager, String note, int planMonths) {
        return new Row(category, name, cycle, deliverable, owner, manager, note, planMonths);
    }
}
''' % (len(isms), emit(isms), len(privacy), emit(privacy))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    io.open(OUT, 'w', encoding='utf-8', newline='\n').write(java)
    print('생성: %s  (ISMS %d개 / PRIVACY %d개)' % (OUT, len(isms), len(privacy)))


if __name__ == '__main__':
    main()
