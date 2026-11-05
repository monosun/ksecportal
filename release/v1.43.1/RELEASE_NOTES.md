# KSecPortal v1.43.1 릴리즈 노트

**릴리즈 일자**: 2026-11-06

**보안정책관리**에 **문서번호 자동 부여**와 **기타** 분류를 추가했습니다.

---

## 1. 문서번호 자동 부여

문서번호를 비워 두면 **분류별 구분 키워드 + 3자리 일련번호**로 자동 부여합니다.

| 분류 | 키워드 | 예시 |
|------|--------|------|
| 규정 | REG | REG-001 |
| 지침 | GDL | GDL-001 |
| 가이드 | GDE | GDE-001 |
| 절차서 | PRC | PRC-001 |
| 매뉴얼 | MNL | MNL-001 |
| 기타 | ETC | ETC-001 |

- 일련번호는 같은 키워드의 **가장 큰 번호 + 1** — 직접 입력한 `REG-010` 도 이어서 셉니다(다음 REG-011).
- 등록 화면에서 분류를 고르면 부여될 번호를 미리 보여 줍니다(`비워 두면 REG-001 로 자동 부여`).
- 직접 입력도 가능하며 **이미 사용 중인 문서번호는 등록할 수 없습니다.**
- **분류를 바꿔도 기존 번호는 유지**합니다(외부 문서·결재에 인용되므로). 번호를 지우고 저장하면 현재 분류로 새 번호를 부여합니다.
- 이전에 번호 없이 등록된 문서는 **서버 기동 시 등록 순서대로 번호가 채워집니다.**

## 2. 기타 분류 추가

규정·지침·가이드·절차서·매뉴얼에 속하지 않는 서식·양식·참고 문서를 위한 **기타** 분류와 탭을 추가했습니다.

## 3. DB 변경 — 마이그레이션 필수

`policy_mgmt_docs.category` 는 MySQL ENUM 이라 `ddl-auto: update` 로는 값이 늘지 않습니다. **백엔드를 올리기 전에** 실행하세요(값 추가만 하며 여러 번 실행해도 안전).

```bash
docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.43.1_policy_doc_other_category.sql
```

신규 설치는 `db/init/14_policy_mgmt.sql` 에 반영되어 있어 별도 작업이 필요 없습니다.

## 4. 업그레이드

```bash
git pull
docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.43.1_policy_doc_other_category.sql
docker compose build backend frontend
docker compose up -d backend frontend
```

배포 후 브라우저 강력 새로고침(Ctrl+Shift+R).

## API

- `GET /api/policy-docs/next-doc-no?category=` — 문서번호를 비웠을 때 부여될 번호 `{ docNo }` (쓰기 권한)
- `category` 에 `OTHER` 추가, `docNo` 미입력 시 자동 부여·중복 시 400
