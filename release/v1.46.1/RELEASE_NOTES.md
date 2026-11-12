# KSecPortal v1.46.1 릴리즈 노트

**릴리즈 일자**: 2026-11-13

**[보안 수정]** 웹 취약점 점검(OWASP ZAP baseline)에서 나온 경고 2건을 조치합니다. 점검 결과 실패는 0건이었고, 경고는 5건에서 3건으로 줄었습니다.

---

## 1. 정적 파일 응답에 보안 헤더 누락 (nginx)

- **원인**: 정적 파일(`.js`·`.css`·`.svg`·이미지·폰트) location 에서 `add_header` 로 캐시 헤더를 설정하고 있었습니다. nginx 는 location 에 `add_header` 가 하나라도 있으면 server 단위 `add_header` 를 물려받지 않습니다. 그래서 정적 파일 응답에는 CSP·X-Frame-Options·Referrer-Policy·Permissions-Policy 가 빠져 있었습니다.
- **조치**: 캐시 헤더를 headers-more 모듈의 `more_set_headers` 로 설정합니다. 이 지시어는 `add_header` 상속을 끊지 않습니다. 이제 모든 응답에 같은 보안 헤더가 붙습니다.
- 캐시 정책은 그대로입니다: `Cache-Control: public, max-age=31536000, immutable`. 이 값을 덮어쓰던 `expires 1y` 는 제거했습니다(`Expires` 헤더는 더 이상 보내지 않음).

## 2. 외부 폰트 스타일시트 제거 (SRI 미적용 경고)

- `index.html` 이 Google Fonts(Inter·Noto Sans KR) 스타일시트를 무결성 검증(SRI) 없이 불러오고 있었습니다.
- 이 폰트는 화면 폰트 설정(`main.css` 의 `--font-sans`: Pretendard·시스템 폰트)에서 쓰지 않았고, CSP(`style-src`·`font-src 'self'`)가 외부 출처를 허용하지 않아 브라우저가 이미 차단하던 요청이었습니다.
- 링크와 `preconnect` 2줄을 제거했습니다. **화면 모양은 바뀌지 않습니다.** 외부로 나가는 요청이 하나 줄어듭니다.

## 3. 남은 점검 경고 (조치 보류)

| 항목 | 판단 |
|------|------|
| CSP `style-src 'unsafe-inline'` | Vue·Tailwind 인라인 스타일에 필요. 스크립트는 `'self'` 만 허용하므로 위험 제한적 |
| Cross-Origin-Embedder-Policy 없음 | 교차 출처 격리가 필요한 기능을 쓰지 않음 |
| Non-Storable Content | 정보성. API 응답의 `no-store` 는 의도된 설정 |

## 4. 업그레이드

```bash
docker compose build frontend
docker compose up -d frontend
```

백엔드·DB 변경 없음. nginx 설정은 frontend 이미지에 포함되므로 frontend 만 다시 빌드하면 됩니다. 배포 후 브라우저 강력 새로고침(Ctrl+Shift+R)이 필요합니다.
