# v1.2.0 릴리즈 노트

**릴리즈 일자**: 2026-07-08

소프트웨어 자산의 구성요소를 관리하는 **SBOM 관리** 기능을 추가한 릴리즈입니다. SW별 포함 라이브러리·버전·라이선스를 관리하고, SW 자산 등록 시 SBOM과 맵핑할 수 있습니다.

## 신규 기능

### SBOM 관리 메뉴
자산 관리 메뉴 아래에 SBOM 관리 메뉴를 추가합니다. 소프트웨어(SW명+버전)별로 포함된 라이브러리 구성요소(SBOM: Software Bill of Materials)를 관리합니다.

- SW 등록·수정·삭제 (SW명·버전·공급업체·설명·비고, SW명+버전 중복 방지)
- SW별 라이브러리 관리 — 라이브러리명·버전·라이선스·비고 CRUD
- SW명/버전/공급업체 키워드 검색, 페이징
- RBAC 메뉴 권한(`sbom`) 및 한/영 i18n 지원

### 엑셀 일괄 등록
SBOM 데이터를 엑셀 양식으로 대량 등록합니다.

- 템플릿 다운로드 제공 (예시 행 + 입력 규칙 시트 포함)
- 한 행에 라이브러리 1건씩 입력 — 같은 SW명+버전 행은 하나의 SW로 자동 병합
- 이미 등록된 SW면 라이브러리만 추가, 동일 라이브러리명+버전은 라이선스/비고 갱신 (재업로드 시 중복 없음)
- 행별 성공/실패·오류 사유 결과 표시

### SW 자산 SBOM 맵핑
자산 등록/수정 시 자산유형을 소프트웨어(SW)로 선택하면 SBOM 관리에 등록된 항목을 확인하고 맵핑할 수 있습니다.

- 등록된 SW가 있으면 맵핑 드롭다운 표시 (SW명·버전·라이브러리 수)
- 등록된 SW가 없으면 SBOM 관리로 이동하는 안내 링크 표시
- 자산 상세 화면 식별 섹션에 맵핑된 SW·라이브러리 수 표시
- SBOM SW 삭제 시 맵핑된 자산은 자동으로 맵핑 해제

## API

- `GET/POST/PATCH/DELETE /api/sbom/software`, `GET /api/sbom/software/all`, `GET /api/sbom/software/:id`
- `POST /api/sbom/software/:id/components`, `PATCH/DELETE /api/sbom/components/:id`
- `GET /api/sbom/bulk/template`, `POST /api/sbom/bulk`
- 자산 API 요청/응답에 `sbomSoftwareId`(및 응답에 `sbomSoftwareName`·`sbomSoftwareVersion`·`sbomComponentCount`) 추가

## 데이터베이스 변경

- `sbom_software` 테이블 신규 (name+version UNIQUE)
- `sbom_components` 테이블 신규 (software_id FK, ON DELETE CASCADE)
- `assets.sbom_software_id` 컬럼 추가 (nullable)
- 신규 설치용 `db/init/10_sbom.sql` 추가 — 기존 설치는 JPA(ddl-auto: update)가 자동 반영하므로 수동 마이그레이션 불필요

## 업그레이드

```bash
git pull
docker compose up -d --build
```

## 라이선스

MIT License — [LICENSE](../../LICENSE) / [LICENSE.ko.md](../../LICENSE.ko.md)
