# Threat model — KSecPortal

## What this project does and where untrusted input enters
KSecPortal is a self-hosted security & privacy management portal (ISMS-P compliance, assets, risks, incidents,
vulnerabilities, contractors, training, phishing simulation, SBOM/source scanning). Spring Boot 3 REST API
(`backend/`, context path `/api`) + Vue 3 SPA (`frontend/`) + MySQL, deployed behind nginx (`nginx/`).
It stores sensitive organisational data, including personal information (PI) of employees and contractors.

Untrusted input:
- **Unauthenticated HTTP** — the `permitAll` routes in `backend/.../common/config/SecurityConfig.java`:
  `/auth/login`, `/auth/register`, `/auth/mfa/verify`, `/auth/okta/**`, `GET /admin/approve/**`,
  `GET /admin/reject/**` (emailed one-click links), `GET /phishing/track/**` (phishing-simulation tracking pixels/links),
  `GET /codes/**`, `GET /public/**`, `GET /actuator/health`.
- **Authenticated users with role `USER`** — every other endpoint. Roles are `USER` < `MANAGER` < `ADMIN`
  (`@PreAuthorize`), plus a per-menu RBAC layer (`rbac` module). A low-privilege user is an attacker we care about.
- **File uploads** (`MultipartFile`, ~40 endpoints): Excel/CSV imports, attachments, SBOM (CycloneDX/SPDX JSON) uploads,
  document conversion via Gotenberg. Files are stored under `APP_UPLOAD_DIR` and downloaded back.
- **Server-side outbound fetches driven by user data**: related-site fetcher (`relatedsite/SiteContentFetcher`),
  RSS feeds (`rss`), GitHub repo source scanning (`sourcescan/GithubApiClient`, `SastEngine`), NVD lookups,
  law.go.kr proxy (`legal/LawApiProxyController`), Slack/SMTP notifications.
- **Data rendered in the SPA**: user-entered rich text / markdown, fetched RSS/site content, imported spreadsheets.

## Components that matter most / least
Most important:
- Authentication & session: JWT issuance/validation, MFA, Okta SSO, registration/approval flow, password reset.
- Authorization: missing or wrong `@PreAuthorize`, IDOR across users/departments, RBAC menu bypass, USER→ADMIN escalation.
- Personal-information protection: PI column encryption (`PI_ENCRYPTION_KEY`), masking (`/codes/pi-masking`),
  jasypt `ENC(...)` secrets, anything that leaks PI or secrets to a lower-privileged user or in logs/exports.
- File handling: path traversal on upload/download, unrestricted file types served back, XXE/zip-bomb in
  xlsx/docx parsing, formula injection in exported spreadsheets.
- SSRF via the outbound fetchers above (cloud metadata, internal services such as `db`, `gotenberg`).
- Injection: SQL/JPQL (native queries), command execution in `sourcescan`, stored XSS in the SPA (`v-html`).
- The unauthenticated phishing-tracking and approve/reject endpoints (token guessing, open redirect, state change via GET).

Less important / out of scope:
- `release/`, `docs/`, `scripts/` (shell helpers run by the operator), `capture/`, `backups/`, `db/migration/` — not runtime code.
- Default credentials and secrets in `application.yml` / `.env.example` / README are documented placeholders that the
  installer must change; do not report them by themselves (do report code that *prevents* changing them, or that
  silently falls back to them in production).
- `CORS`/`csrf` disabled is by design (bearer tokens, no cookies) — report only with a concrete exploit.

## How to exercise it
- The image has the backend jar (`backend/build/libs/*.jar`) and the SPA build (`frontend/dist`).
- `run-ksecportal` starts the bundled MariaDB and the API on `http://localhost:8080/api`. On first start seed
  initializers create the schema data and a default admin `secportal@monosun.com` / `Ksecurity!!!` (password change
  required on first login). Create `USER`/`MANAGER` accounts via `/auth/register` + admin approval to test privilege boundaries.
- Endpoint reference: `docs/api.md`. There are no automated test suites in the repo.
- External services (SMTP, Slack, Okta, NVD, law.go.kr, GitHub, Gotenberg) are unreachable offline; failures there are expected.

## How you rate severity
- **Critical**: unauthenticated RCE, auth bypass, unauthenticated access to PI or to admin functions, SQLi reachable without login.
- **High**: authenticated RCE; USER→ADMIN/MANAGER privilege escalation; IDOR or authz bypass exposing other users' PI
  or allowing writes; SSRF reaching internal services; path traversal reading/writing arbitrary files; JWT forgery;
  decryption/leak of PI-encryption or jasypt keys; stored XSS executable in an ADMIN's session from a USER's input.
- **Medium**: stored XSS needing higher privileges to plant, reflected XSS, CSV/formula injection, open redirect,
  user enumeration, token brute-forcibility on approve/reject or tracking links, sensitive info in logs.
- **Low**: DoS (including large uploads/zip bombs), missing hardening headers, verbose errors without sensitive data.
- An ADMIN abusing ADMIN-only features is not a vulnerability unless it crosses the host boundary (RCE, file read).

## Anything to leave alone
- Vulnerable third-party dependency versions alone (we track those with SCA separately) — report only with a reachable exploit path in our code.
- Rate limiting / brute-force protection absence on its own.
- Reports and patches: please include the affected endpoint, required role, a curl-level reproducer, and a minimal patch
  that follows the existing domain-module layout (`<domain>/controller|service|repository|entity|dto`).
