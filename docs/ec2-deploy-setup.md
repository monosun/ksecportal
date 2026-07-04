# EC2 자동 배포 활성화 가이드

GitHub Actions CI/CD 파이프라인을 실제 EC2 서버에 연결하는 절차입니다.  
배포는 기본적으로 **OFF** 상태입니다. 이 가이드를 완료하면 `main` 브랜치 push 시 EC2에 자동 배포됩니다.

---

## 전체 흐름

```
1. EC2 서버 준비 (SSH 접속 확인, Docker 설치)
2. GitHub Secrets 4개 등록
3. GitHub production 환경 생성
4. DEPLOY_ENABLED = true 설정
5. 배포 테스트
```

완료까지 소요 시간: **약 20분**

---

## 사전 조건

- [ ] AWS EC2 인스턴스 실행 중 (Ubuntu 22.04 / 24.04 권장)
- [ ] EC2에 Docker 및 Docker Compose 설치 완료
- [ ] EC2에 프로젝트 클론 완료 (`/home/ubuntu/secportal`)
- [ ] `.env` 파일 설정 완료
- [ ] GitHub 리포지토리 관리자 권한 보유

> EC2 초기 설정이 아직 안 된 경우 → [aws-deployment.md 3~5절](aws-deployment.md) 먼저 진행

---

## Step 1. EC2 접속 및 SSH 키 확인

로컬 PC에서 EC2에 SSH 접속이 되는지 확인합니다.

```bash
# 키 파일 권한 설정 (최초 1회)
chmod 400 secportal-key.pem

# SSH 접속 테스트
ssh -i secportal-key.pem ubuntu@<EC2-IP>
```

접속 성공 시 다음 Step으로 이동합니다.

---

## Step 2. EC2 서버 상태 확인

EC2 서버에서 아래 명령어를 실행해 배포 준비가 됐는지 확인합니다.

```bash
# 서버에서 실행
cd /home/ubuntu/secportal

# Docker 동작 확인
docker compose ps

# .env 파일 존재 확인
ls -la .env

# Git remote 확인
git remote -v
```

기대 결과:
```
secportal-db        Up (healthy)
secportal-backend   Up
secportal-frontend  Up
```

---

## Step 3. GitHub Secrets 4개 등록

GitHub 리포지토리 → **Settings → Secrets and variables → Actions → New repository secret**

### 3-1. EC2_HOST

EC2의 **Elastic IP** 또는 도메인을 입력합니다.

```
Name:  EC2_HOST
Value: 13.124.xxx.xxx
```

### 3-2. EC2_USER

EC2 접속 사용자명 (Ubuntu AMI 기본값: `ubuntu`)

```
Name:  EC2_USER
Value: ubuntu
```

### 3-3. EC2_SSH_KEY

로컬의 PEM 키 파일 **전체 내용**을 붙여넣습니다.

```bash
# 로컬 PC에서 실행 — 출력 내용 전체를 복사
cat secportal-key.pem
```

```
Name:  EC2_SSH_KEY
Value: -----BEGIN RSA PRIVATE KEY-----
       MIIEowIBAAKCAQEA...
       -----END RSA PRIVATE KEY-----
```

> `-----BEGIN`부터 `-----END`까지 전체를 포함해야 합니다.

### 3-4. ENV_FILE

EC2 서버의 `.env` 파일 **전체 내용**을 붙여넣습니다.

```bash
# EC2 서버에서 실행 — 출력 내용 전체를 복사
cat /home/ubuntu/secportal/.env
```

```
Name:  ENV_FILE
Value: DB_ROOT_PASSWORD=강력한비밀번호
       DB_PASSWORD=앱비밀번호
       JWT_SECRET=시크릿키
       MAIL_USERNAME=your-email@gmail.com
       MAIL_PASSWORD=Gmail앱비밀번호
       APP_BASE_URL=https://secportal.yourdomain.com/api
       JASYPT_ENCRYPTOR_PASSWORD=마스터키
       ...
```

> **중요**: `APP_BASE_URL`을 실제 도메인으로 설정해야 이메일 승인 링크가 정상 동작합니다.

---

## Step 4. GitHub production 환경 생성

CI/CD 워크플로우의 `deploy` 잡이 `environment: production`을 참조합니다.  
환경이 없으면 잡이 대기 상태로 멈춥니다.

```
GitHub 리포지토리
→ Settings
→ Environments
→ New environment
→ 이름: production
→ Configure environment (보호 규칙은 선택 사항)
→ Save protection rules
```

---

## Step 5. DEPLOY_ENABLED = true 설정

GitHub 리포지토리 → **Settings → Secrets and variables → Actions → Variables 탭**

`DEPLOY_ENABLED` 변수를 찾아 값을 변경합니다.

```
Name:  DEPLOY_ENABLED
Value: true        ← false에서 true로 변경
```

> **끄려면**: 다시 `false`로 변경하면 다음 push부터 배포 잡이 건너뜁니다.

---

## Step 6. 배포 테스트

변경 사항을 `main`에 push해서 파이프라인이 정상 동작하는지 확인합니다.

```bash
# 로컬에서 실행
git commit --allow-empty -m "test: EC2 자동 배포 테스트"
git push origin main
```

GitHub → **Actions 탭**에서 파이프라인 진행 상황을 확인합니다.

### 성공 시 기대 결과

```
✓ Build        — 백엔드·프론트엔드 빌드 통과
✓ Deploy to Server — SSH 접속 → git pull → docker compose up 완료
```

### EC2에서 배포 확인

```bash
ssh -i secportal-key.pem ubuntu@<EC2-IP>
cd /home/ubuntu/secportal
docker compose ps
```

---

## Step 7. 배포 완료 확인

브라우저에서 접속합니다.

| 항목 | 값 |
|------|----|
| 접속 URL | `http://<EC2-IP>` 또는 `https://your-domain.com` |
| 관리자 이메일 | `secportal@monosun.com` |
| 관리자 비밀번호 | `Admin1234!` (첫 접속 후 반드시 변경) |

---

## 배포 ON/OFF 요약

| 상태 | 설정 방법 |
|------|-----------|
| **배포 ON** | Variables → `DEPLOY_ENABLED` = `true` |
| **배포 OFF** | Variables → `DEPLOY_ENABLED` = `false` |

변경 즉시 적용됩니다 (다음 push부터 반영).

---

## 트러블슈팅

### 배포 잡이 실행되지 않음

```
원인 1: DEPLOY_ENABLED = false 상태
→ Step 5 확인

원인 2: production 환경이 없음
→ Step 4 확인

원인 3: main 브랜치가 아닌 다른 브랜치에 push
→ main 브랜치에 push해야 deploy 잡 실행
```

### SSH 접속 실패 (EC2_SSH_KEY 오류)

```
원인: PEM 키 내용에 개행 문자가 누락되거나 잘림
→ EC2_SSH_KEY 시크릿을 삭제 후 재등록
→ BEGIN/END 줄 포함 전체를 복사했는지 확인
```

### docker compose 실패

```bash
# EC2에서 수동으로 확인
cd /home/ubuntu/secportal
docker compose logs --tail=50 backend
```

```
원인 1: .env 파일의 DB 비밀번호 불일치
→ ENV_FILE 시크릿과 실제 DB 초기화 시 사용한 비밀번호 확인

원인 2: 포트 충돌
→ sudo lsof -i :80 으로 다른 프로세스 확인
```

### 이메일 승인 링크가 localhost로 생성됨

```
원인: ENV_FILE에 APP_BASE_URL 미설정 또는 localhost로 설정됨
→ ENV_FILE 시크릿 수정: APP_BASE_URL=https://your-domain.com/api
→ docker compose up -d 로 재시작
```

---

## 관련 문서

- [AWS 배포 환경 구성 전체 가이드](aws-deployment.md) — EC2 생성, HTTPS, 운영, 백업
- [CI/CD 파이프라인 설정](.github/workflows/ci.yml) — 워크플로우 소스
