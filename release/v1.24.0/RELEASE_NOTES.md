# KSecPortal v1.24.0 릴리즈 노트

**릴리즈 일자**: 2026-07-30

퀴즈 문항의 **복수 정답**을 지원하고, 문제은행 문항을 **Excel로 내려받는** 기능을 추가했습니다. 최초 로그인 비밀번호 변경 화면에는 **비밀번호 표시 토글**을 넣었습니다.

---

## 1. 퀴즈 복수 정답 허용

문제은행과 교육 퀴즈에서 정답을 A~D 중 **여러 개** 지정할 수 있습니다.

### 저장 형식
- 정답은 콤마로 구분한 오름차순 문자열로 저장합니다 — 단일은 `A`, 복수는 `A,C`.
- 입력은 느슨하게 받습니다: `A,C` / `AC` / `a c` / `A/C` 모두 `A,C` 로 정규화됩니다.
- 기존 단일 정답 데이터는 그대로 유효하며 변환이 필요 없습니다.

### 등록·수정
- 문제은행, 교육 등록/수정 화면의 정답 입력이 **드롭다운 → A~D 다중 선택 버튼**으로 바뀌었습니다.
- 보기 C·D는 **내용을 입력해야** 정답으로 지정할 수 있습니다(빈 보기를 정답으로 두는 것을 방지).
- 정답을 하나도 고르지 않으면 저장되지 않습니다(프론트 사전 검증 + 서버 검증).

### 엑셀 일괄등록 / 다운로드
- 정답 열 머리글이 **`정답(A~D, 복수는 A,C)*`** 로 바뀌고, 템플릿에 **복수 정답 예시 행**이 추가되었습니다.
- 업로드 시 정답 검증은 행 단위로 처리되어, 잘못된 행만 사유와 함께 실패로 표시되고 나머지는 정상 등록됩니다.

### 응시·채점
- 복수 정답 문항은 응시 화면에서 **체크박스**(단일 정답 문항은 기존 라디오 유지)로 표시되고, **"복수 정답 문항입니다 — 정답을 모두 선택하세요"** 안내가 함께 노출됩니다.
- 채점은 **집합 비교**입니다. 정답 보기를 **모두 정확히** 선택해야 정답으로 인정하며, 하나라도 빠지거나 오답을 함께 고르면 오답 처리됩니다.
- 오답 리뷰도 복수 정답 기준으로 표시되며, 정답이면서 내가 고른 보기는 **"정답 · 내가 선택한 답"** 으로 표시됩니다.

---

## 2. 문제은행 문항 Excel 다운로드

관리 > 문제은행에서 등록된 문항을 Excel로 내려받을 수 있습니다.

- **전체 다운로드** — 문제은행 전체 문항을 저장합니다.
- **검색결과 다운로드** — 분류·난이도·검색어 필터가 걸려 있으면 버튼이 바뀌어 해당 조건의 문항만 저장합니다.
- **선택 문제 다운로드** — 체크한 문항만 저장합니다(페이지를 이동해도 선택 유지, 기존 기능).
- 출력 형식은 **일괄등록 양식과 동일**하므로 내려받은 파일을 수정해 그대로 재업로드할 수 있습니다.
- 서버에서 500건씩 나눠 조회하므로 문항이 많아도 누락 없이 저장됩니다.

---

## 3. 최초 로그인 비밀번호 변경 — 비밀번호 표시 토글

초기 비밀번호로 처음 로그인하면 나타나는 비밀번호 변경 화면에서, **현재 비밀번호·새 비밀번호·새 비밀번호 확인** 세 입력란 모두 오른쪽 **눈 아이콘**으로 입력값을 표시/숨김 전환할 수 있습니다. 로그인 화면과 동일한 조작 방식입니다.

---

## 데이터베이스 변경

정답 컬럼을 `VARCHAR(7)` 로 확장합니다. **기존 운영 DB에는 아래 마이그레이션을 적용해야 합니다.**

```bash
docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.24.0_quiz_multi_answer.sql
```

| 테이블 | 컬럼 | 변경 |
|--------|------|------|
| `quiz_bank_questions` | `correct_answer` | `VARCHAR(1)` → `VARCHAR(7)` |
| `quiz_questions` | `correct_answer` | `CHAR(1)` → `VARCHAR(7)` |

> 신규 설치(`db/init/01_schema.sql`)에는 이미 반영되어 있어 별도 작업이 필요 없습니다. 기존 데이터 변환은 필요하지 않습니다.

---

## 변경 파일 요약

**백엔드**
- `training/service/QuizAnswers.java` (신규) — 정답 파싱·정규화·집합 비교·정답 보기 검증 유틸
- `training/service/QuizBankService.java` — 복수 정답 검증/정규화, 엑셀 헤더·복수 정답 예시 행, 업로드 행별 오류 처리
- `training/service/TrainingService.java` — 문항 저장 시 정규화, `submitQuiz` 채점을 집합 비교로 변경
- `training/entity/QuizBankQuestion.java`, `training/entity/QuizQuestion.java` — 정답 컬럼 길이 확장

**프론트엔드**
- `utils/quizAnswer.js` (신규) — 백엔드와 동일 규칙의 정답 유틸
- `views/admin/QuizBankView.vue` — 정답 다중 선택, 복수 정답 표시, 전체/검색결과 다운로드
- `views/training/TrainingFormModal.vue`, `views/training/TrainingFormView.vue` — 문항 정답 다중 선택·저장 전 검증
- `views/training/TrainingDetailModal.vue`, `views/training/TrainingDetailView.vue` — 복수 정답 문항 체크박스 응시·오답 리뷰
- `views/auth/ForceChangePasswordView.vue` — 비밀번호 표시 토글 3개

**DB·문서**
- `db/migration/v1.24.0_quiz_multi_answer.sql` (신규), `db/init/01_schema.sql`
- `docs/user-manual.md`(+ `frontend/public/help/user-manual.md`), `README.md`
