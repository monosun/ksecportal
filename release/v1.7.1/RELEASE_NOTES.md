# v1.7.1 릴리즈 노트

**릴리즈 일자**: 2026-07-11

수탁사 점검을 **건별 이력**으로 보관하도록 개선하고, 점검항목 기본 템플릿 관리를 **코드관리**로 일원화한 릴리즈입니다. 월간·수탁사 기본 점검항목의 초기 데이터 누락 문제도 함께 해결했습니다.

## 버그 수정

### 수탁사 점검 — 건별 이력 보관
같은 수탁사·연도에 대해 점검을 다시 등록하면 **기존 점검이 덮어써지던 문제**를 수정했습니다.

- `contractor_checks`의 `(contractor_id, check_year)` 유니크 제약을 제거하여, 한 수탁사에 대해 점검을 등록할 때마다 **별도 이력으로 누적**됩니다.
- 저장 로직을 "덮어쓰기(createOrGet)"에서 **항상 새 건 생성(create)** 으로 변경했습니다.
- 점검 목록에 **점검일**을 표시해 같은 수탁사의 여러 점검을 구분할 수 있고, 새 점검 등록 시 점검일 기본값이 오늘로 설정됩니다.

### 기본 점검항목 초기 데이터 누락
월간·수탁사 기본 점검항목 템플릿이 마이그레이션에만 존재해, 일부 환경에서 **코드관리 탭이 비어 있고 "기본 항목 불러오기"가 동작하지 않던 문제**를 해결했습니다.

- 시작 시 템플릿 테이블이 비어 있으면 자동으로 채우는 초기화기를 추가했습니다.
  - **월간 보안점검 기본 항목 32개** (`MonthlyCheckDefaultInitializer`)
  - **수탁사 점검 기본 항목 20개** — 개인정보보호법 기준 7개 분야 (`ContractorCheckItemDefaultInitializer`)
- 신규 설치·기존 빈 DB·볼륨 재생성 등 모든 경우에 자동 복구됩니다.

## 개선

### 점검항목 관리 일원화 — 코드관리
- **수탁사 점검항목 관리** 메뉴를 제거했습니다. 점검항목 템플릿은 **관리 > 코드관리 > 수탁사 기본점검항목** 탭에서 관리합니다.
- 새 수탁사 점검을 등록하면 코드관리의 기본점검항목이 점검 항목으로 반영됩니다.
- (월간 보안점검 기본 항목은 기존과 동일하게 **코드관리 > 월간 점검 기본 항목** 탭에서 관리)

## 데이터베이스

- 마이그레이션 `db/migration/v1.7.1_contractor_check_history.sql` — `contractor_checks`의 `uk_contractor_year` 유니크 제약 제거 (기존 설치 대상 1회 실행)
- 신규 설치용 스키마(`v1.52.0_contractor_check.sql`)에서도 해당 유니크 제약 제거
- 기본 템플릿 테이블(`monthly_check_defaults`, `contractor_check_item_defaults`)은 시작 시 자동 시드

## 업그레이드 방법

```bash
git pull
# 기존 설치: 수탁사 점검 건별 이력을 위해 유니크 제약 제거 (1회)
docker compose exec -T db mysql -usecportal -p<password> secportal < db/migration/v1.7.1_contractor_check_history.sql
docker compose build backend frontend
docker compose up -d backend frontend
```

> 기본 점검항목(월간 32개·수탁사 20개)은 앱 기동 시 비어 있을 때 자동으로 채워집니다.
