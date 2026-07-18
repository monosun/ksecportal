# v1.12.0 릴리즈 노트

**릴리즈 일자**: 2026-07-18

개인정보 영향평가(DPIA)에 **보고서 등 산출물 문서를 첨부**할 수 있게 했습니다. 평가 → 개선 → 완료 단계별로 여러 건의 문서를 한 평가에 함께 보관합니다.

## 신규 기능

### 개인정보 영향평가(DPIA) — 보고서 첨부파일

**개인정보보호 > 개인정보 영향평가(DPIA)** 목록에서 행 오른쪽의 **첨부** 버튼으로 첨부파일 창을 엽니다.

- **파일 추가** 시 **구분**(결과보고서 · 개선이행 · 체크리스트 · 기타), **문서 제목**, 파일을 지정합니다. 문서 제목을 비우면 파일명이 사용됩니다.
- 한 평가에 **여러 개의 파일**을 첨부할 수 있습니다. 영향평가 결과보고서와 개선이행 확인서를 함께 보관하는 용도입니다.
- 첨부 가능한 형식은 PDF · 이미지 · 문서(doc/xls/ppt) · zip 입니다.
- 목록에 **첨부 건수** 컬럼이 표시되며, 파일명 클릭으로 내려받고 개별 삭제할 수 있습니다.
- 등록·삭제는 **ADMIN/MANAGER** 권한이 필요하고, 조회·다운로드는 해당 메뉴 열람 권한으로 가능합니다.
- 영향평가를 삭제하면 **첨부 레코드와 실제 파일도 함께 삭제**됩니다.

문서 구분은 MySQL ENUM이 아닌 문자열 컬럼으로 저장하여, 이후 구분을 추가해도 스키마 마이그레이션이 필요하지 않습니다.

## API 변경

### 추가

```
GET    /api/privacy/dpia/{id}/files            # 첨부파일 목록
POST   /api/privacy/dpia/{id}/files            # 첨부 등록 (multipart: data + file)
DELETE /api/privacy/dpia/files/{fileId}        # 첨부 삭제
GET    /api/privacy/dpia/files/{fileId}/download   # 첨부 다운로드
```

- 등록은 동의서 버전이력과 동일한 multipart 방식입니다 — `data`(JSON: `category`, `title`) + `file`.

### 변경

- `GET /api/privacy/dpia`, `GET /api/privacy/dpia/{id}` 응답에 **`fileCount`**(첨부 건수) 필드가 추가되었습니다. 기존 필드는 그대로이며 **기존 호출은 영향받지 않습니다.**

## 데이터베이스

- **신규 테이블 `privacy_dpia_files`** — `dpia_id`(FK), `category`, `title`, `file_name`, `file_path`, `file_size`, `uploader_id`.
- `ddl-auto: update` 로 **기동 시 자동 생성**되므로 별도 마이그레이션이 필요 없습니다. 기존 데이터에는 영향이 없습니다.

## 업그레이드 방법

```bash
git pull
docker compose build backend frontend
docker compose up -d backend frontend
```

> 배포 후 브라우저 하드 리프레시(Ctrl+Shift+R)가 필요합니다.
