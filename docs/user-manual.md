# SecPortal 사용자 매뉴얼

**버전**: v1.3.0  
**최종 업데이트**: 2026-07-08

---

## 목차

1. [시작하기](#1-시작하기)
2. [대시보드](#2-대시보드)
3. [자산 관리](#3-자산-관리)
    - [SBOM 관리](#31-sbom-관리)
4. [위협 관리](#4-위협-관리)
5. [위험평가](#5-위험평가)
6. [취약점 관리](#6-취약점-관리)
7. [인시던트 관리](#7-인시던트-관리)
8. [ISMS-P 이행 관리](#8-isms-p-이행-관리)
9. [내부 감사](#9-내부-감사)
10. [보안 문서](#10-보안-문서)
11. [보안위원회](#11-보안위원회)
12. [정보보호의 날](#12-정보보호의-날)
13. [피싱 훈련](#13-피싱-훈련)
14. [보안 솔루션 연동](#14-보안-솔루션-연동)
15. [알림](#15-알림)
16. [개인정보보호](#16-개인정보보호)
    - [수탁사 관리](#161-수탁사-관리)
    - [수탁사 점검항목 관리](#162-수탁사-점검항목-관리)
    - [수탁사 점검](#163-수탁사-점검)
    - [법령준수관리](#164-법령준수관리)
17. [관리 메뉴](#17-관리-메뉴)
    - [사용자 관리](#171-사용자-관리)
    - [역할 관리](#172-역할-관리)
    - [코드 관리](#173-코드-관리)
    - [앱 설정](#174-앱-설정)
    - [백업 관리](#175-백업-관리)
18. [내 설정](#18-내-설정)
- [부록 A. 데이터베이스 ERD](#부록-a-데이터베이스-erd)

---

## 1. 시작하기

### 로그인

1. 브라우저에서 시스템 URL로 접속합니다.
2. 이메일과 비밀번호를 입력하고 **로그인** 버튼을 클릭합니다.
3. MFA(다중 인증)가 설정된 경우 OTP 코드를 추가로 입력합니다.

### 역할 안내

| 역할 | 권한 |
|------|------|
| ADMIN | 모든 기능 접근 가능, 관리 메뉴 포함 |
| MANAGER | 일반 보안 관리 기능 사용 가능 |
| USER | 조회 및 제한된 기능 사용 |

---

## 2. 대시보드

로그인 후 메인 화면에 표시되는 종합 현황 화면입니다.

![대시보드](images/dashboard.png)

- **KRCERT 보안공지**: 최근 7일간 KRCERT 보안 공지 목록
- **보안이벤트 현황**: 연동 솔루션 수, 24시간 이벤트 수, 24시간 심각도 분포(심각/높음/중간/낮음/정보)
- **보안 KPI**: 미처리 취약점 수, 진행 중 인시던트 수, 총 자산 수 요약

---

## 3. 자산 관리

조직의 정보자산을 등록·조회·관리합니다.

![자산 관리](images/assets.png)

### 자산 등록

1. **자산 관리 > 자산 목록** 이동
2. 우상단 **+ 자산 등록** 버튼 클릭
3. 필수 항목 입력:
   - 자산명, 자산유형, 자산 소유자
   - 기밀성(C)·무결성(I)·가용성(A) 등급 (1~3)
4. ISMS-P 자산식별 기준 항목 입력 (확장 정보)
5. **저장** 버튼 클릭

### 자산 검색

- 자산명, 자산유형, 자산 소유자, CIA 등급 조합 검색 가능
- 검색 결과는 페이지네이션 형태로 표시

### 자산 일괄 등록

**엑셀 업로드** 버튼을 통해 CSV/Excel 형식으로 대량 등록 가능  
(템플릿 다운로드 후 작성하여 업로드)

### SW 자산 SBOM 맵핑 (v1.2.0 신규)

자산 등록/수정 시 **자산유형**을 `소프트웨어(SW)`로 선택하면 **SBOM 맵핑** 선택란이 나타납니다.

- SBOM 관리에 등록된 SW가 있으면 드롭다운에서 선택해 맵핑합니다 (SW명·버전·라이브러리 수 표시)
- 등록된 SW가 없으면 SBOM 관리 화면으로 이동하는 안내 링크가 표시됩니다
- 맵핑된 자산의 상세 화면 식별 섹션에 SW명·버전·라이브러리 수가 표시됩니다

---

## 3.1. SBOM 관리

**자산 관리** 메뉴 아래의 **SBOM 관리**에서 소프트웨어(SW)별 버전과 포함된 라이브러리 구성요소(SBOM: Software Bill of Materials)를 **CycloneDX 1.5 표준 기준**으로 관리합니다.
구성요소 필드는 CycloneDX component에 대응합니다 — 유형(type)·그룹(group)·라이브러리명(name)·버전(version)·PURL·라이선스(SPDX ID).

### SW 등록

1. **SBOM 관리** 이동
2. 우상단 **+ SW 등록** 버튼 클릭
3. SW명·버전(필수), 공급업체·설명·비고 입력 후 저장
   - 동일한 SW명+버전 조합은 중복 등록할 수 없습니다

### 라이브러리(구성요소) 관리

1. SW 목록에서 행 클릭 또는 **구성요소** 버튼 클릭
2. 모달 상단 입력란에 유형·그룹·라이브러리명(필수)·버전·PURL·라이선스·비고 입력 후 **추가**
3. 각 행의 수정/삭제 버튼으로 개별 관리

### CycloneDX 내보내기 / 가져오기

- **내보내기**: SW 목록 행의 **CycloneDX** 버튼 클릭 → `{SW명}-{버전}.cdx.json` 다운로드 (CycloneDX 1.5 JSON)
- **가져오기**: 우상단 **CycloneDX 가져오기** 버튼 → syft·cdxgen·trivy 등 SCA 도구가 생성한 CycloneDX JSON 업로드
  - metadata.component가 SW 정보로, components가 라이브러리로 등록됩니다
  - 동일 SW명+버전이 이미 있으면 라이브러리가 병합됩니다

### 엑셀 일괄 등록

1. **엑셀 일괄 등록** 버튼 클릭 → **엑셀 템플릿 다운로드**
2. 한 행에 라이브러리 1건씩 입력 (SW명·SW버전·라이브러리명 필수)
   - 같은 SW명+버전 행은 하나의 SW로 묶여 등록됩니다
   - 이미 등록된 SW면 라이브러리만 추가되고, 동일 라이브러리명+버전은 라이선스/비고만 갱신됩니다
3. 파일 선택 후 **등록 시작** — 행별 성공/실패 결과가 표시됩니다

### SW 삭제

SW 삭제 시 포함된 라이브러리 정보가 함께 삭제되며, 해당 SW에 맵핑된 자산은 맵핑이 자동 해제됩니다.

---

## 4. 위협 관리

위협 카탈로그를 관리합니다.

![위협 관리 화면](images/threat-management-filters.png)

### 기본 위협 목록

MRR-0001 ~ MRR-0560까지 ISMS-P 기반 기본 위협 항목이 제공됩니다.

### 인라인 필터 (v1.50.0 신규)

테이블 헤더 바로 아래에 **컬럼별 필터 행**이 제공됩니다.

| 필터 | 유형 | 설명 |
|------|------|------|
| 위협명 | 텍스트 입력 | 위협명 키워드 검색 |
| 유형 | 드롭다운 | 외부공격 / 내부위협 등 선택 |
| 카테고리 | 텍스트 입력 | 카테고리 키워드 검색 |
| 발생가능성 | 드롭다운 | 1~5 선택 |
| 잠재영향 | 드롭다운 | 1~5 선택 |
| 위험점수 | 드롭다운 | 낮음(≤7) / 중간(8~15) / 높음(≥16) |
| 수정일 | 날짜 입력 | 특정 날짜 이후 수정된 항목 필터 |

- 화면 상단의 **"전체 N건 중 M건 표시"** 문구로 필터 결과 수를 즉시 확인할 수 있습니다.
- 필터가 적용된 경우 우상단 **필터 초기화** 버튼이 표시됩니다.
- 필터 결과가 없으면 안내 메시지와 함께 초기화 버튼이 표시됩니다.

### 사용자 정의 위협 추가

1. **위협 관리 > 위협 목록** 이동
2. **+ 위협 추가** 버튼 클릭
3. 위협명, 위협유형, 설명 입력 후 저장

---

## 5. 위험평가

자산과 위협을 연결하여 위험을 평가하고 처리방안을 관리합니다.

![위험평가](images/risk-assessment.png)

### 위험평가 차수 관리

1. **위험평가** 메뉴 이동
2. 연도 및 차수 선택 드롭다운에서 평가 차수 선택 또는 **+ 새 차수 추가** 클릭
3. 차수별로 독립적인 평가 수행

### 위험수용 기준 설정

화면 상단 **위험수용 기준 설정** 패널에서 기준 점수를 입력합니다.

- 기본값: **6점** (브라우저별 독립 저장)
- 기준 이하 항목은 **노란색 행**으로 하이라이트되고 "수용가능" 배지가 표시됩니다
- 위험점수 범위: 1~25 (가능성 × 영향도, 각 1~5)
- 참고 등급 기준:
  - 고위험: 15점 이상
  - 중위험: 8~14점
  - 저위험: 7점 이하

### 위험 항목 추가

1. **+ 평가 추가** 버튼 클릭
2. 자산 선택, 위협 선택
3. 가능성(1~5), 영향도(1~5) 입력 → 위험점수 자동 계산
4. 현재 통제 현황 입력
5. **저장**

### 검색 및 필터링

| 검색 조건 | 설명 |
|-----------|------|
| 자산명 | 자산명 키워드 검색 |
| 자산유형 | 드롭다운 선택 |
| 위협명 | 위협명 키워드 검색 |
| 위협유형 | 드롭다운 선택 |
| 처리방법 | 수용/완화/회피/이전 선택 |
| 위험등급 | 고위험/중위험/저위험 선택 |
| 위험점수 범위 | 최소~최대 점수 입력 |

**초기화** 버튼으로 모든 필터를 한번에 초기화합니다.

### 체크박스 및 일괄 처리

**개별 선택**
- 각 행 맨 앞 체크박스를 클릭하여 선택

**전체 선택**
- 테이블 헤더의 체크박스 클릭
  - 전체 선택됨: ✓ 체크됨
  - 일부 선택됨: — (indeterminate) 상태
  - 없음: 빈 상태

**수용기준 이하 자동선택**
- 현재 필터링된 항목 중 수용 기준 이하 점수 항목을 모두 자동 선택

**수용완료 일괄처리**
- 선택된 항목의 처리방법을 "수용"으로 일괄 변경
- 실행 전 확인 모달이 표시됩니다

### 위험 처리방법

![위험처리 계획](images/risk-treatment.png)

| 처리방법 | 설명 |
|----------|------|
| 완화 | 통제수단을 적용하여 위험 감소 |
| 수용 | 위험을 인지하고 그대로 수용 |
| 회피 | 위험 유발 활동/자산 제거 |
| 이전 | 위험을 제3자(보험 등)에 이전 |

### 위험평가 결과 다운로드

**엑셀 다운로드** 버튼으로 자산별 개별 시트 형태의 엑셀 파일을 내려받습니다.

---

## 6. 취약점 관리

시스템 취약점을 등록하고 조치 현황을 추적합니다.

![취약점 관리](images/vulnerabilities.png)

### 취약점 등록

1. **취약점 관리** 메뉴 이동
2. **+ 취약점 등록** 버튼 클릭
3. 취약점명, 심각도(Critical/High/Medium/Low), 영향 시스템, 조치기한 입력
4. **저장**

### 상태 관리

- **미처리** → **처리중** → **처리완료** 순으로 상태 변경
- 조치기한 초과 시 자동으로 "기한초과" 표시

---

## 7. 인시던트 관리

보안 인시던트를 등록하고 처리 과정을 기록합니다.

![인시던트 관리](images/incidents.png)

### 인시던트 등록

1. **인시던트 관리** 메뉴 이동
2. **+ 인시던트 등록** 버튼 클릭
3. 제목, 발생일시, 유형, 심각도, 설명 입력
4. **저장**

### 처리 이력 기록

인시던트 상세 화면에서 처리 단계별 이력을 추가할 수 있습니다.

---

## 8. ISMS-P 이행 관리

ISMS-P 인증 심사 대비 통제항목별 이행 현황을 관리합니다.

![ISMS-P 증적관리](images/isms.png)

### 통제항목 조회

- 2.xx.x 형태의 ISMS-P 통제항목 코드 기준으로 구조화된 목록 제공
- 영역별 탭 필터 제공: 전체 / 1.1~1.4 관리체계 기반 / 2.2~2.12 보호대책 / 3.1~3.5 개인정보 보호

### 증적 자료 관리

1. 통제항목 클릭하여 상세 화면 이동
2. **증적 추가** 버튼으로 문서/파일 첨부
3. 이행 상태(계획/진행중/완료) 변경
4. 기존 증적 참조 검색으로 다른 통제항목의 증적을 재활용 가능

### ISMS-P 통제항목 매핑

![ISMS-P 통제항목 매핑](images/isms-mapping.png)

위험평가 결과와 ISMS-P 통제항목을 연결하여 이행 현황을 추적합니다.

---

## 9. 내부 감사

내부 정보보호 감사 계획 수립 및 수행 이력을 관리합니다.

![내부 감사](images/internal-audit.png)

### 감사 등록

1. **내부 감사** 메뉴 이동
2. **+ 감사 등록** 버튼 클릭
3. 감사명, 감사기간, 감사 대상 조직, 감사 유형 입력
4. 감사 항목 추가 및 체크리스트 작성

---

## 10. 보안 문서

보안 정책·지침·절차 문서를 관리합니다.

![보안 문서](images/sec-docs.png)

### 문서 등록

1. **보안 문서** 메뉴 이동
2. **+ 문서 등록** 버튼 클릭
3. 문서명, 카테고리, 버전, 파일 첨부
4. **저장**

---

## 11. 보안위원회

보안위원회 회의록을 등록하고 첨부파일을 관리합니다.

![보안위원회](images/committee.png)

---

## 12. 정보보호의 날

월별 정보보호 점검 항목을 체크하고 결과를 기록합니다.

![정보보호의 날](images/monthly-checks.png)

---

## 13. 피싱 훈련

임직원 대상 피싱 이메일 훈련 캠페인을 관리합니다.

![피싱 훈련](images/phishing.png)

### 캠페인 생성

1. **피싱 훈련** 메뉴 이동
2. **+ 캠페인 생성** 버튼 클릭
3. 캠페인명, 발송일시, 대상자 목록, 피싱 템플릿 선택
4. **저장 및 예약**

---

## 14. 보안 솔루션 연동

외부 보안 솔루션(SIEM, EDR 등)으로부터 이벤트를 수신하고 조회합니다.

![보안이벤트 현황](images/security-events.png)

![보안 결함사항](images/security-findings.png)

---

## 15. 알림

![수신함](images/inbox.png)

### 알림 확인

- 상단 메뉴바의 수신함 아이콘 클릭하여 미확인 알림 확인
- 알림 클릭 시 해당 항목으로 이동

### 알림 설정

- **내 설정 > 알림 설정** 에서 이메일 알림 수신 여부 설정

---

## 16. 개인정보보호

개인정보 처리 관련 수탁사 정보 및 점검 이력을 관리합니다.

### 16.1 수탁사 관리

개인정보 처리 업무를 위탁한 수탁사 목록과 연도별 점검 이력을 등록·관리합니다.

![수탁사 관리](images/privacy-contractors.png)

#### 화면 구성

| 영역 | 설명 |
|------|------|
| 왼쪽 패널 | 수탁사 목록 (이름 검색·상태 필터 제공) |
| 오른쪽 패널 | 선택한 수탁사의 기본 정보 및 연도별 점검 이력 |

#### 수탁사 등록

1. **개인정보보호 > 수탁사 관리** 이동
2. 우상단 **+ 수탁사 등록** 버튼 클릭
3. 아래 항목 입력:
   - 수탁사명 (필수), 사업자번호, 대표자명
   - 위탁 업무 (예: 고객센터 운영, 배송 서비스 등)
   - 계약 시작일 / 종료일
   - 담당자명, 연락처, 이메일
   - 상태: **계약중** / **종료**
   - 비고
4. **저장** 클릭

#### 연도별 점검 등록

수탁사 점검 시스템과 연동하여 연도별 점검을 생성하고 관리합니다.

1. 왼쪽 목록에서 수탁사 선택
2. 오른쪽 **점검 이력** 섹션의 **+ 점검 등록** 클릭
3. 아래 항목 입력:
   - **점검 연도** (필수): 동일 연도·수탁사 조합이 이미 존재하면 해당 점검을 불러옵니다
   - 점검일, 점검자, 상태 (계획 / 진행중 / 완료), 비고
4. **등록** 클릭 → 점검이 생성되고 결과 보기 모달이 자동으로 열립니다

#### 점검 결과 보기

등록된 점검 카드의 **결과 보기** 버튼을 클릭하면 해당 연도 점검의 세부 결과를 인라인 모달로 확인합니다.

모달에 표시되는 정보:

| 항목 | 설명 |
|------|------|
| 헤더 | 점검 연도, 상태 배지, 점검일, 점검자 |
| 통계 바 | 통과 / 미흡 / 해당없음 / 미점검 항목 수 및 진행 바 |
| 결과 테이블 | 카테고리별로 그룹화된 점검항목과 각 결과 |
| 하단 버튼 | **수탁사 점검에서 결과 수정** — 수탁사 점검 페이지로 이동하여 상세 결과를 입력합니다 |

결과 배지 색상:

| 배지 | 의미 |
|------|------|
| 통과 (초록) | 점검 기준 충족 |
| 미흡 (빨강) | 점검 기준 미충족 |
| 해당없음 (회색) | 해당 항목 적용 불가 |
| 미점검 (연회색) | 아직 결과 미입력 |

#### 점검 이력 카드

각 연도 점검 카드에는 다음 정보와 액션 버튼이 표시됩니다:

- **연도 + 상태 배지**: 계획(파랑) / 진행중(노랑) / 완료(초록)
- **점검일 / 점검자**
- **진행률 바**: 통과·미흡·해당없음·미점검 비율을 색상으로 표시
- **결과 보기**: 세부 결과 모달 열기
- **수정**: 점검 기본 정보(날짜·점검자·상태·비고) 수정
- **삭제**: 해당 연도 점검 및 모든 결과 삭제

> 수탁사를 삭제하면 해당 수탁사의 모든 점검 이력과 결과가 함께 삭제됩니다.

---

### 16.2 수탁사 점검항목 관리

수탁사 점검 시 평가할 점검항목 목록을 관리합니다. 이 목록은 새로운 점검을 등록할 때 결과 입력 행의 기준이 됩니다.

![수탁사 점검항목 관리](images/contractor-check-items.png)

#### 화면 구성

- 상단 헤더: **초기화** / **기본 점검항목 추가** / **점검항목 추가** 버튼
- 본문: 카테고리·항목명·점검방법·기준·필수 여부 테이블

#### 점검항목 추가

1. **개인정보보호 > 수탁사 점검항목 관리** 이동
2. **+ 점검항목 추가** 클릭
3. 아래 항목 입력:
   - **카테고리** (예: 개인정보 보호, 기술적 보호, 관리적 보호)
   - **항목명** (필수)
   - 점검 방법, 점검 기준, 정렬 순서, 필수 여부
4. **저장**

#### 기본 점검항목 불러오기

**+ 기본 점검항목 추가** 버튼을 클릭하면 코드 관리의 **수탁사 기본점검항목** 탭에 등록된 기본 항목 중 아직 추가되지 않은 항목을 일괄 불러옵니다.

#### 전체 초기화

**초기화** 버튼(빨간 테두리)을 클릭하면:

> ⚠️ 현재 등록된 **모든 점검항목과 점검결과**가 삭제되고 **기본 점검항목으로 초기화**됩니다.

확인 후 실행되며, 이 작업은 되돌릴 수 없습니다.

---

### 16.3 수탁사 점검

연도별·수탁사별로 각 점검항목에 대한 실제 이행 결과를 입력·관리합니다.

![수탁사 점검](images/contractor-checks.png)

#### 화면 구성

| 영역 | 설명 |
|------|------|
| 상단 연도 선택 | 점검 연도를 선택합니다 |
| 왼쪽 패널 | 선택 연도의 수탁사 목록과 통과/미흡/미점검 요약 |
| 오른쪽 패널 | 선택한 수탁사의 점검 헤더 정보 + 항목별 결과 입력 |

#### 결과 입력

1. **개인정보보호 > 수탁사 점검** 이동
2. 상단 연도 드롭다운에서 점검 연도 선택
3. 왼쪽 목록에서 수탁사 선택
4. 오른쪽에서 각 점검항목별 결과 버튼 클릭:

| 버튼 | 의미 |
|------|------|
| **통과** (초록) | 점검 기준 충족 |
| **미흡** (빨강) | 점검 기준 미충족 |
| **해당없음** (회색) | 해당 항목 적용 불가 |
| **미점검** (노랑) | 결과 미입력 상태 |

- **미흡** 선택 시 하단에 메모 입력란이 자동으로 표시됩니다.
- 메모 입력란 포커스 해제 시 자동 저장됩니다.
- 결과를 클릭할 때마다 즉시 서버에 저장됩니다.

#### 검색 필터

점검항목이 많을 때 **항목명·카테고리 검색** 입력란에 키워드를 입력하여 해당 항목만 표시할 수 있습니다.

- 필터 적용 시 `(N/전체건)` 형태로 필터된 항목 수가 헤더에 표시됩니다.
- 검색어를 지우면 전체 목록으로 돌아옵니다.

#### 일괄 처리 버튼

점검항목 테이블 헤더의 우측에 두 가지 일괄 처리 버튼이 있습니다:

| 버튼 | 동작 |
|------|------|
| **모두 통과** (초록) | 모든 점검항목을 "통과"로 일괄 설정 |
| **점검 초기화** (회색) | 모든 점검항목을 "미점검"으로 초기화하고 메모 삭제 |

> 두 버튼 모두 실행 전 확인 다이얼로그가 표시됩니다.

#### 진행률 표시

선택한 수탁사의 점검 헤더 카드에 진행률이 실시간으로 표시됩니다:

- **진행률 바**: 전체 항목 중 통과/미흡/해당없음 비율을 색상으로 표시
- **통계 요약**: 통과 N / 미흡 N / 해당없음 N / 미점검 N

#### 점검 추가

1. 상단 **+ 수탁사 점검 추가** 버튼 클릭
2. 수탁사 선택, 점검 연도, 점검일, 점검자, 상태, 비고 입력
3. **추가** 클릭 → 점검항목 관리에 등록된 항목 목록으로 결과 행이 자동 생성됩니다

> 동일 연도·수탁사 조합이 이미 존재하면 기존 점검 정보를 불러옵니다.

#### 항목 동기화

점검항목 관리에서 항목이 추가된 경우, 오른쪽 패널 헤더의 **항목 동기화** 버튼을 클릭하여 새 항목을 현재 점검에 추가합니다.

---

### 16.4 법령준수관리

업종별로 준수해야 할 법령(법령·시행령·시행규칙·규정·고시 등)을 조회하고, 선택한 법령을 조문(조·항) 단위로 Excel 다운로드하여 준수 여부를 관리합니다.

#### 주요 기능

| 기능 | 설명 |
|------|------|
| 업종 카테고리 필터 | 금융·핀테크 / 통신 / 전자상거래·플랫폼 / 클라우드·SaaS / 기타 업종 |
| 키워드 검색 | 업종명 또는 법령명·소관부처로 실시간 검색 |
| 우리 업종만 보기 | 설정관리 > 업종 설정에서 지정한 업종만 필터링 |
| 법령 선택 | 체크박스로 개별 법령 또는 업종 전체 선택 |
| Excel 다운로드 | 선택한 법령을 시트별 조·항 내용 포함하여 Excel 저장 |

#### 법령 선택 및 다운로드

1. **개인정보보호 > 법령준수관리** 이동
2. 카테고리 버튼 또는 검색창으로 원하는 업종 필터링
3. 업종 카드를 클릭하여 펼친 후 법령 목록에서 체크박스 선택
   - 업종 헤더 체크박스로 해당 업종 전체 법령 일괄 선택 가능
4. 우상단 **선택 법령 Excel 다운로드** 버튼 클릭

#### Excel 구성

| 시트 | 내용 |
|------|------|
| 법령목록 | 선택된 법령 전체 요약 (업종·구분·법령명·소관부처·링크·**판단**) |
| 개인정보 보호법 등 각 법령 시트 | 조 번호·조 제목·항 번호·내 용·**판단** 컬럼 |

- **판단** 컬럼은 준수/미준수/해당없음/검토필요 중 해당 항목을 직접 입력하는 자유 입력 셀입니다.
- 각 법령 시트에는 해당 법령의 주요 준수 조문이 행 단위로 표시됩니다.
- 시행령·규정·고시 등 하위 법규도 개별 시트로 분리됩니다.

#### 업종 설정

**설정관리 > 업종 설정** 탭에서 회사 업종을 선택하면 법령준수관리 화면에 **우리 업종만** 필터 버튼이 활성화됩니다.

---

## 17. 관리 메뉴

> ADMIN 역할만 접근 가능합니다.

### 17.1 사용자 관리

임직원 계정을 생성·수정·비활성화합니다.

![사용자 관리](images/admin-users.png)

1. **관리 > 사용자 관리** 이동
2. **+ 사용자 추가** 버튼 클릭
3. 이름, 이메일, 부서, 초기 비밀번호, 역할 설정
4. **저장**

> 비활성화된 계정은 로그인이 차단되나 데이터는 보존됩니다.

---

### 17.2 역할 관리

커스텀 역할 및 메뉴별 접근 권한을 설정합니다.

![역할 관리](images/admin-rbac.png)

---

### 17.3 코드 관리

드롭다운에서 사용되는 공통 코드 값 및 각종 기본 항목을 관리합니다.

![코드 관리 화면](images/codes-management-v150.png)

코드 관리 화면은 6개의 탭으로 구성됩니다.

| 탭 | 설명 |
|----|------|
| 코드 관리 | 부서·보안솔루션·자산·위협 유형 등 공통 코드 그룹 관리 |
| 월간 점검 기본 항목 | 정보보호의 날 월간 점검 항목 기본 목록 관리 |
| 위협 기본 항목 | 위험평가에서 사용할 기본 위협 항목 관리 |
| 위협 유형별 관리 | 위협 유형(THREAT_TYPE)별로 분류하여 항목 관리 |
| 개인정보 유형별 관리 | 개인정보 분류 유형별 항목 관리 |
| 수탁사 기본점검항목 | 수탁사 점검 시 사용할 기본 점검항목 템플릿 관리 (v1.52.0 신규) |

#### 코드 그룹 및 코드값 관리

1. 왼쪽 **코드 그룹** 패널에서 그룹을 클릭합니다.
2. 오른쪽에 해당 그룹의 코드값 테이블이 표시됩니다.
   - **표시명**: 화면에 표시되는 이름
   - **값**: 시스템 내부 코드값
   - **설명**: 코드값에 대한 부가 설명
   - **정렬**: 드롭다운에서 표시되는 순서
   - **상태**: 사용/미사용
3. **+ 코드값 추가** 버튼으로 새 항목을 추가합니다.
4. 행의 **수정** 버튼으로 기존 항목을 수정합니다.

#### 개인정보 유형별 관리 탭

개인정보 분류 기준에 따른 항목을 유형별로 관리합니다.

![개인정보 유형별 관리](images/pi-type-management.png)

- 왼쪽 패널: 개인정보 유형 목록 (기본 식별정보, 연락처 정보, 결제·금융 정보 등 13개 유형)
- 오른쪽 패널: 선택한 유형에 속하는 개인정보 항목 목록 (항목명·설명·상태 표시)
- **+ 항목 추가** 버튼으로 해당 유형에 새 개인정보 항목을 추가할 수 있습니다.

#### 수탁사 기본점검항목 탭 (v1.52.0 신규)

수탁사 점검항목의 템플릿이 되는 기본 항목을 관리합니다. 이 탭에서 관리하는 항목은 **수탁사 점검항목 관리**에서 "기본 점검항목 추가" 또는 "초기화" 시의 기준 데이터로 사용됩니다.

| 컬럼 | 설명 |
|------|------|
| 카테고리 | 항목 분류 (예: 개인정보 보호, 기술적 보호 등) |
| 항목명 | 점검항목 이름 |
| 점검 방법 | 점검 수행 방법 기술 |
| 점검 기준 | 합격/불합격 판단 기준 |
| 정렬 | 표시 순서 |

- **+ 기본 점검항목 추가** 버튼으로 새 기본 항목을 추가합니다.
- 기존 항목은 수정·삭제 가능합니다.

> 기본점검항목은 실제 점검 결과와는 독립적인 **템플릿** 데이터입니다. 이 목록을 변경해도 이미 생성된 점검 결과에는 영향을 주지 않습니다.

---

### 17.4 앱 설정

시스템 전반 설정을 관리합니다.

- 로그인 페이지 로고 이미지
- 세션 타임아웃 시간
- 이메일 알림 발송 여부
- 비밀번호 정책 등

---

### 17.5 백업 관리

시스템 환경 설정 및 DB 데이터를 백업하고 복원합니다.

![백업 관리](images/admin-backup.png)

#### 즉시 백업 (다운로드)

1. **관리 > 백업 관리** 이동
2. **즉시 백업** 카드의 비밀번호 입력란에 암호화 비밀번호 입력
3. **백업 파일 다운로드** 버튼 클릭
4. `.bak` 확장자의 암호화된 백업 파일이 다운로드됩니다

> 비밀번호는 복원 시 반드시 필요합니다. **분실 시 복원 불가**이므로 안전하게 보관하세요.

#### 즉시 백업 (서버 저장)

1. 비밀번호 입력 후 **서버에 저장** 버튼 클릭
2. 서버의 `/app/backups` 디렉토리에 파일이 저장됩니다
3. 저장된 파일은 아래 **서버 백업 파일** 목록에서 확인·다운로드·삭제 가능

#### 백업 복원

> **주의**: 복원 시 현재 DB 데이터가 백업 파일의 데이터로 교체됩니다.

1. **백업 복원** 카드에서 `.bak` 파일을 드래그 앤 드롭하거나 **파일 선택** 버튼으로 업로드
2. 백업 시 사용한 비밀번호 입력
3. **복원 실행** 버튼 클릭
4. 경고 확인 모달에서 "복원 실행" 버튼을 다시 클릭하여 확인
5. 복원 완료 후 페이지를 새로고침합니다

#### 정기 백업 설정

1. **정기 백업 설정** 카드에서 **정기 백업 활성화** 토글을 켭니다
2. Cron 표현식을 직접 입력하거나 **빠른 선택** 버튼 사용:
   - 매일 02:00: `0 0 2 * * ?`
   - 매일 00:00: `0 0 0 * * ?`
   - 주 1회 (일 02:00): `0 0 2 ? * SUN`
   - 월 1회 (1일 02:00): `0 0 2 1 * ?`
3. **기본 비밀번호** 설정 (정기 백업 시 이 비밀번호로 암호화)
4. **최대 보관 개수** 설정 (초과 시 오래된 파일부터 자동 삭제)
5. **설정 저장** 버튼 클릭

> Cron 표현식 형식: `초 분 시 일 월 요일`  
> 예: `0 0 2 * * ?` = 매일 새벽 2시 0분 0초

#### 서버 백업 파일 목록

- 서버에 저장된 백업 파일 목록 표시
- **다운로드** 아이콘: 해당 파일을 로컬에 다운로드
- **삭제** 아이콘: 해당 파일을 서버에서 삭제

#### 백업 이력

| 컬럼 | 설명 |
|------|------|
| 유형 | MANUAL(즉시)/SCHEDULED(정기)/RESTORE(복원) |
| 파일명 | 생성된 파일명 |
| 크기 | 파일 크기 (KB/MB) |
| 상태 | SUCCESS / FAILURE / IN_PROGRESS |
| 메시지 | 성공 메시지 또는 오류 내용 |
| 일시 | 실행 일시 |

---

## 18. 내 설정

### 환경설정

![환경설정](images/settings.png)

색상, 폰트, 글자 크기 등 UI를 원하는 대로 설정합니다.

- **컬러 테마**: Monosun / Blue / Navy / Emerald / Purple / Rose / Naver Green 중 선택
- **폰트**: 화면에 표시될 글꼴 선택
- **글자 크기**: 화면 전체 글자 크기 조절

### 프로필 수정

- 이름, 부서, 전화번호 변경
- **저장** 버튼 클릭

### 비밀번호 변경

1. **내 설정 > 비밀번호 변경** 이동
2. 현재 비밀번호, 새 비밀번호, 비밀번호 확인 입력
3. **변경** 버튼 클릭

### MFA 설정

1. **내 설정 > MFA 설정** 이동
2. **MFA 활성화** 버튼 클릭
3. 화면에 표시된 QR 코드를 Google Authenticator 등의 OTP 앱으로 스캔
4. OTP 앱에서 생성된 6자리 코드 입력하여 등록 완료

---

## 부록: 위험점수 계산표

위험점수 = 가능성(1~5) × 영향도(1~5)

| | 영향도 1 | 영향도 2 | 영향도 3 | 영향도 4 | 영향도 5 |
|---|---|---|---|---|---|
| **가능성 1** | 1 | 2 | 3 | 4 | 5 |
| **가능성 2** | 2 | 4 | 6 | 8 | 10 |
| **가능성 3** | 3 | 6 | 9 | 12 | 15 |
| **가능성 4** | 4 | 8 | 12 | 16 | 20 |
| **가능성 5** | 5 | 10 | 15 | 20 | 25 |

- **고위험** (빨강): 15점 이상
- **중위험** (노랑): 8~14점
- **저위험** (초록): 7점 이하

---

## 부록 A. 데이터베이스 ERD

전체 테이블을 기능 도메인별로 나누어 표현합니다. PK/FK 중심으로 핵심 컬럼만 표시합니다.

---

### A.1 사용자 & 인증

```mermaid
erDiagram
    users {
        bigint id PK
        varchar email UK
        varchar password
        varchar name
        enum role
        varchar department
        tinyint active
        bit mfa_enabled
        bit must_change_password
        int failed_login_attempts
        datetime locked_until
        datetime created_at
    }
    custom_roles {
        bigint id PK
        varchar name UK
        text description
    }
    user_custom_roles {
        bigint user_id PK,FK
        bigint role_id PK,FK
    }
    role_permissions {
        bigint id PK
        bigint role_id FK
        varchar menu_key
        bit can_read
        bit can_write
        bit can_delete
    }
    pending_admin_actions {
        bigint id PK
        varchar token UK
        varchar action_type
        bigint target_user_id FK
        bigint requester_id FK
        enum status
        datetime expires_at
    }
    inbox_messages {
        bigint id PK
        bigint recipient_id FK
        enum type
        varchar title
        text content
        bit is_read
        varchar action_token
    }
    audit_logs {
        bigint id PK
        bigint user_id FK
        varchar action
        varchar resource_type
        bigint resource_id
        text detail
        datetime created_at
    }

    users ||--o{ user_custom_roles : "has"
    custom_roles ||--o{ user_custom_roles : "assigned to"
    custom_roles ||--o{ role_permissions : "defines"
    users ||--o{ pending_admin_actions : "targets"
    users ||--o{ pending_admin_actions : "requests"
    users ||--o{ inbox_messages : "receives"
    users ||--o{ audit_logs : "creates"
```

---

### A.2 자산 & 위험 평가

```mermaid
erDiagram
    assets {
        bigint id PK
        varchar name
        varchar type
        varchar environment
        varchar criticality
        varchar status
        varchar confidentiality
        varchar integrity
        varchar availability
        tinyint personal_info_included
        tinyint active
        datetime created_at
    }
    risk_assessment_rounds {
        bigint id PK
        int year
        int round_no
        date round_date
        varchar title
        enum status
        bigint created_by FK
        datetime created_at
    }
    risk_assessments {
        bigint id PK
        bigint round_id FK
        bigint asset_id
        varchar asset_name
        bigint threat_id
        varchar threat_name
        varchar vulnerability
        int likelihood
        int impact
        enum risk_grade
        enum treatment
        datetime created_at
    }
    threats {
        bigint id PK
        varchar name
        varchar type
        varchar category
        int likelihood
        int impact
        text description
    }
    threat_defaults {
        bigint id PK
        varchar risk_id
        varchar name
        varchar type
        varchar category
        int likelihood
        int impact
    }
    users {
        bigint id PK
        varchar name
    }

    users ||--o{ risk_assessment_rounds : "creates"
    risk_assessment_rounds ||--o{ risk_assessments : "contains"
```

---

### A.3 취약점 & 인시던트

```mermaid
erDiagram
    vulnerabilities {
        bigint id PK
        varchar title
        text description
        varchar cve_id
        decimal cvss_score
        enum severity
        enum status
        varchar asset_name
        bigint reporter_id FK
        bigint assignee_id FK
        date due_date
        datetime resolved_at
        datetime created_at
    }
    vulnerability_comments {
        bigint id PK
        bigint vulnerability_id FK
        bigint user_id FK
        text content
        datetime created_at
    }
    incidents {
        bigint id PK
        varchar title
        text description
        varchar severity
        varchar status
        varchar type
        bigint reporter_id FK
        bigint assignee_id FK
        datetime detected_at
        datetime resolved_at
        datetime created_at
    }
    users {
        bigint id PK
        varchar name
    }

    users ||--o{ vulnerabilities : "reports"
    users ||--o{ vulnerabilities : "assigned to"
    vulnerabilities ||--o{ vulnerability_comments : "has"
    users ||--o{ vulnerability_comments : "writes"
    users ||--o{ incidents : "reports"
    users ||--o{ incidents : "assigned to"
```

---

### A.4 정책 & ISMS-P 증적

```mermaid
erDiagram
    policies {
        bigint id PK
        varchar title
        enum category
        enum status
        varchar version
        date effective_date
        bigint author_id FK
        datetime created_at
    }
    policy_acknowledgments {
        bigint id PK
        bigint policy_id FK
        bigint user_id FK
        datetime acknowledged_at
    }
    isms_items {
        bigint id PK
        varchar item_code UK
        varchar item_name
        varchar domain_code
        varchar domain_name
        int section_num
        varchar section_name
        int sort_order
    }
    isms_evidences {
        bigint id PK
        bigint item_id FK
        int year
        varchar title
        text content
        varchar file_path
        enum status
        bigint registrant_id FK
        bigint source_evidence_id FK
        datetime created_at
    }
    isms_policy_mappings {
        bigint id PK
        bigint isms_item_id FK
        bigint policy_id FK
        datetime created_at
    }
    sec_docs {
        bigint id PK
        varchar title
        enum category
        varchar version
        varchar file_path
        bit is_latest
        varchar document_key
        bigint uploader_id FK
        datetime created_at
    }
    users {
        bigint id PK
        varchar name
    }

    users ||--o{ policies : "authors"
    policies ||--o{ policy_acknowledgments : "acknowledged by"
    users ||--o{ policy_acknowledgments : "acknowledges"
    isms_items ||--o{ isms_evidences : "has"
    users ||--o{ isms_evidences : "registers"
    isms_evidences ||--o| isms_evidences : "copied from"
    isms_items ||--o{ isms_policy_mappings : "mapped to"
    policies ||--o{ isms_policy_mappings : "maps to"
    users ||--o{ sec_docs : "uploads"
```

---

### A.5 내부 감사 & 보안 조치

```mermaid
erDiagram
    internal_audits {
        bigint id PK
        varchar title
        int year
        date audit_start
        date audit_end
        varchar auditor
        enum status
        bigint created_by FK
        datetime created_at
    }
    audit_targets {
        bigint id PK
        bigint audit_id FK
        varchar target_name
        varchar target_type
        int sort_order
    }
    audit_items {
        bigint id PK
        bigint audit_id FK
        bigint target_id FK
        varchar item_name
        enum result
        text finding
        text action_required
    }
    audit_files {
        bigint id PK
        bigint audit_id FK
        bigint uploader_id FK
        varchar title
        varchar file_path
        bigint file_size
    }
    security_findings {
        bigint id PK
        int year
        enum audit_type
        varchar finding_summary
        text finding_detail
        enum risk_level
        enum status
        date action_deadline
        date resolved_at
        bigint created_by FK
        datetime created_at
    }
    committee_meetings {
        bigint id PK
        int year
        int session_no
        date meeting_date
        varchar title
        enum status
        bigint created_by FK
    }
    committee_files {
        bigint id PK
        bigint meeting_id FK
        bigint uploader_id FK
        varchar title
        enum file_type
        varchar file_path
    }
    users {
        bigint id PK
        varchar name
    }

    users ||--o{ internal_audits : "creates"
    internal_audits ||--o{ audit_targets : "has"
    internal_audits ||--o{ audit_items : "contains"
    audit_targets ||--o{ audit_items : "scoped to"
    internal_audits ||--o{ audit_files : "attaches"
    users ||--o{ audit_files : "uploads"
    users ||--o{ security_findings : "registers"
    users ||--o{ committee_meetings : "creates"
    committee_meetings ||--o{ committee_files : "attaches"
    users ||--o{ committee_files : "uploads"
```

---

### A.6 IT 및 정보보호 교육 & 피싱 훈련

```mermaid
erDiagram
    training_courses {
        bigint id PK
        varchar title
        enum content_type
        varchar content_url
        int passing_score
        tinyint mandatory
        tinyint active
        bigint created_by FK
        datetime created_at
    }
    quiz_questions {
        bigint id PK
        bigint course_id FK
        text question
        varchar option_a
        varchar option_b
        varchar option_c
        varchar option_d
        char correct_answer
        varchar difficulty
        text explanation
        int sort_order
    }
    quiz_bank_questions {
        bigint id PK
        varchar category
        varchar difficulty
        text question
        varchar option_a
        varchar option_b
        varchar option_c
        varchar option_d
        char correct_answer
        text explanation
    }
    training_completions {
        bigint id PK
        bigint course_id FK
        bigint user_id FK
        int score
        tinyint passed
        datetime completed_at
    }
    phishing_templates {
        bigint id PK
        varchar name
        varchar category
        enum difficulty
        varchar subject
        varchar sender_name
        varchar sender_email
        longtext body_html
        bigint created_by FK
    }
    phishing_campaigns {
        bigint id PK
        varchar name
        enum status
        datetime scheduled_at
        bigint template_id FK
        bigint created_by FK
    }
    phishing_targets {
        bigint id PK
        varchar name
        varchar email
        varchar department
        varchar position
        bit active
    }
    phishing_campaign_targets {
        bigint id PK
        bigint campaign_id FK
        bigint target_id FK
        datetime sent_at
        datetime opened_at
        datetime clicked_at
        datetime reported_at
        varchar tracking_token UK
    }
    users {
        bigint id PK
        varchar name
    }

    users ||--o{ training_courses : "creates"
    training_courses ||--o{ quiz_questions : "has"
    training_courses ||--o{ training_completions : "completed by"
    users ||--o{ training_completions : "completes"
    users ||--o{ phishing_templates : "creates"
    phishing_templates ||--o{ phishing_campaigns : "used in"
    users ||--o{ phishing_campaigns : "creates"
    phishing_campaigns ||--o{ phishing_campaign_targets : "targets"
    phishing_targets ||--o{ phishing_campaign_targets : "included in"
```

---

### A.7 월간 보안 점검

```mermaid
erDiagram
    monthly_check_defaults {
        bigint id PK
        varchar category
        varchar item_name
        enum priority
        varchar check_method
        varchar check_example
        int sort_order
        tinyint active
    }
    monthly_check_items {
        bigint id PK
        varchar check_month
        varchar category
        varchar item_name
        enum priority
        enum result
        bigint assignee_id FK
        bigint created_by FK
        text notes
        datetime created_at
    }
    monthly_check_evidences {
        bigint id PK
        bigint check_item_id FK
        varchar title
        text content
        varchar file_path
        bigint uploaded_by FK
        datetime created_at
    }
    users {
        bigint id PK
        varchar name
    }

    users ||--o{ monthly_check_items : "assigned to"
    users ||--o{ monthly_check_items : "creates"
    monthly_check_items ||--o{ monthly_check_evidences : "has"
    users ||--o{ monthly_check_evidences : "uploads"
```

---

### A.8 개인정보보호 — 수탁사

```mermaid
erDiagram
    privacy_contractors {
        bigint id PK
        varchar name
        varchar business_number
        varchar representative
        varchar service_type
        date contract_start
        date contract_end
        varchar contact_person
        varchar contact_email
        varchar status
        datetime created_at
    }
    contractor_inspections {
        bigint id PK
        bigint contractor_id FK
        date inspection_date
        varchar inspector
        varchar status
        text result
        datetime created_at
    }
    contractor_inspection_files {
        bigint id PK
        bigint inspection_id FK
        bigint uploader_id FK
        varchar title
        varchar file_path
        bigint file_size
        datetime created_at
    }
    contractor_check_item_defaults {
        bigint id PK
        varchar category
        varchar item_name
        varchar check_method
        varchar check_standard
        int sort_order
    }
    contractor_check_items {
        bigint id PK
        varchar category
        varchar item_name
        varchar check_method
        varchar check_standard
        bit is_required
        int sort_order
        datetime created_at
    }
    contractor_checks {
        bigint id PK
        bigint contractor_id FK
        int check_year
        date check_date
        varchar inspector
        enum status
        datetime created_at
    }
    contractor_check_results {
        bigint id PK
        bigint check_id FK
        bigint check_item_id FK
        enum result
        text notes
        datetime created_at
    }
    users {
        bigint id PK
        varchar name
    }

    privacy_contractors ||--o{ contractor_inspections : "has"
    contractor_inspections ||--o{ contractor_inspection_files : "attaches"
    users ||--o{ contractor_inspection_files : "uploads"
    privacy_contractors ||--o{ contractor_checks : "has"
    contractor_checks ||--o{ contractor_check_results : "contains"
    contractor_check_items ||--o{ contractor_check_results : "evaluated by"
```

---

### A.9 보안 통합 & 이벤트

```mermaid
erDiagram
    security_integrations {
        bigint id PK
        varchar name
        varchar solution_type
        varchar vendor
        varchar host
        varchar api_key
        enum status
        datetime last_sync_at
        datetime created_at
    }
    security_events {
        bigint id PK
        bigint integration_id FK
        varchar event_type
        enum severity
        text message
        varchar source_ip
        varchar destination_ip
        datetime detected_at
    }

    security_integrations ||--o{ security_events : "generates"
```

---

### A.10 코드 & 시스템 설정

```mermaid
erDiagram
    code_groups {
        varchar group_code PK
        varchar group_name
        varchar description
        int sort_order
    }
    code_values {
        bigint id PK
        varchar group_code
        varchar value
        varchar label
        varchar description
        int sort_order
        bit active
    }
    app_settings {
        bigint id PK
        varchar setting_key UK
        mediumtext setting_value
        varchar description
        datetime updated_at
    }
    notification_config {
        varchar config_key PK
        varchar config_value
    }
    notices {
        bigint id PK
        varchar title
        text content
        bit pinned
        bit active
        bigint created_by_id FK
        datetime created_at
    }
    backup_history {
        bigint id PK
        varchar filename
        bigint file_size
        varchar backup_type
        varchar status
        varchar message
        datetime created_at
    }
    users {
        bigint id PK
        varchar name
    }

    code_groups ||--o{ code_values : "contains"
    users ||--o{ notices : "creates"
```

---

## 변경 이력

| 버전 | 날짜 | 주요 변경 내용 |
|------|------|----------------|
| v1.53.0 | 2026-06-25 | 대시보드 RSS 피드 설정 가능화(조회 일수·피드 URL 환경설정), 사용자 관리 새 비밀번호 보이기 토글 추가 |
| v1.52.1 | 2026-06-25 | 스크린샷 추가(수탁사 점검항목 관리·수탁사 점검), 데이터베이스 ERD 부록 추가, 로그인 화면 버전 표시, 사용자 관리 비밀번호 변경 버그 수정 |
| v1.52.0 | 2026-06-25 | 수탁사 점검항목 관리·수탁사 점검 섹션 신규 추가, 수탁사 관리 점검 시스템 전면 개편, 코드관리 수탁사 기본점검항목 탭 추가 |
| v1.51.0 | 2026-06-24 | 개인정보보호 > 수탁사 관리 섹션 추가, 목차 번호 재편 |
| v1.50.3 | 2026-06-24 | 전체 화면 스크린샷 추가, nginx SPA 라우팅 버그 수정 |
| v1.50.3 | 2026-06-23 | 코드값 테이블 설명(description) 컬럼 추가, PI_TYPE 탭 표시 수정 |
| v1.50.1 | 2026-06-23 | 코드 관리에 개인정보 유형별 관리 탭 추가 |
| v1.50.0 | 2026-06-23 | 코드관리 개인정보 분류별 코드 추가, 위협 관리 인라인 필터 추가 |
| v1.49.5 | 2026-06-22 | Docker 업로드 디렉터리 소유권 수정 |
| v1.49.4 | 2026-06-21 | ISMS-P 증적 파일 다운로드 버그 수정 |
| v1.49.0 | 2026-06-20 | 위험평가 환경 필터, 처리방법별 현황 표시 추가 |

---

*SecPortal — 오픈소스 정보보호 관리 포탈*
