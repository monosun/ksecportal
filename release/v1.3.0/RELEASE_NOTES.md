# v1.3.0 릴리즈 노트

**릴리즈 일자**: 2026-07-08

SBOM 관리를 **CycloneDX 1.5 표준** 기준으로 전환한 릴리즈입니다. CycloneDX JSON 내보내기/가져오기를 지원하여 syft·cdxgen·trivy 등 SCA 도구 산출물과 호환됩니다.

## 신규 기능

### CycloneDX 표준 데이터 모델
SBOM 구성요소 필드를 CycloneDX 1.5 component 스펙에 정렬합니다.

- **컴포넌트 유형(type)** — CycloneDX 허용값 12종 검증: application, framework, library(기본), container, platform, operating-system, device, device-driver, firmware, file, machine-learning-model, data
- **그룹(group)** — 네임스페이스 (예: org.springframework.boot)
- **PURL** — Package URL (예: pkg:maven/org.apache.poi/poi-ooxml@5.2.5)
- **라이선스** — SPDX 라이선스 ID 기준 (CycloneDX licenses.license.id)

### CycloneDX JSON 내보내기
SW 목록의 **CycloneDX** 버튼으로 해당 SW의 SBOM을 CycloneDX 1.5 JSON(`{SW명}-{버전}.cdx.json`)으로 다운로드합니다.

- `bomFormat`/`specVersion`/`serialNumber`(urn:uuid)/`metadata.component`(SW 정보)/`components[]` 표준 구조
- 라이선스는 SPDX ID 형태면 `license.id`, 아니면 `license.name`으로 출력

### CycloneDX JSON 가져오기
**CycloneDX 가져오기** 버튼으로 SCA 도구가 생성한 CycloneDX JSON을 업로드해 SBOM을 일괄 등록합니다.

- `metadata.component` → SW(SW명+버전), `components[]` → 라이브러리 자동 매핑
- 동일 SW명+버전이 이미 있으면 라이브러리 병합 (동일 name+version 컴포넌트는 갱신 — 재가져오기 시 중복 없음)
- 컴포넌트별 성공/실패·오류 사유 결과 표시

## 변경 사항

- 엑셀 일괄등록 템플릿에 `그룹(네임스페이스)`, `PURL`, `컴포넌트 유형`, `라이선스(SPDX ID)` 컬럼 추가 — v1.2.0 템플릿과 컬럼 구성이 다르므로 새 템플릿을 다운로드해 사용하세요
- 구성요소 관리 화면에 유형·그룹·PURL 입력/표시 추가

## API

- `GET /api/sbom/software/:id/cyclonedx` — CycloneDX 1.5 JSON 내보내기
- `POST /api/sbom/import/cyclonedx` — CycloneDX JSON 가져오기 (MANAGER+)
- 컴포넌트 요청/응답에 `componentType`·`groupName`·`purl` 필드 추가

## 데이터베이스 변경

- `sbom_components`에 `component_type`(NOT NULL, 기본 'library')·`group_name`·`purl` 컬럼 추가
- 기존 설치는 JPA(ddl-auto: update)가 자동 반영하므로 수동 마이그레이션 불필요

## 업그레이드

```bash
git pull
docker compose up -d --build
```

## 라이선스

MIT License — [LICENSE](../../LICENSE) / [LICENSE.ko.md](../../LICENSE.ko.md)
