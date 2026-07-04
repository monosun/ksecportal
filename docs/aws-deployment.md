# AWS 배포 환경 구성 및 운영 매뉴얼

> **현재 버전: v1.3.0**  
> CI/CD 파이프라인: `.github/workflows/ci.yml`  
> 서버 초기화 스크립트: `scripts/server-setup.sh`

## 목차

1. [아키텍처 선택](#1-아키텍처-선택)
2. [사전 준비](#2-사전-준비)
3. [AWS 인프라 구성](#3-aws-인프라-구성)
4. [서버 초기 설정](#4-서버-초기-설정)
5. [애플리케이션 배포](#5-애플리케이션-배포)
6. [HTTPS 설정](#6-https-설정)
7. [CI/CD 파이프라인](#7-cicd-파이프라인)
8. [운영 매뉴얼](#8-운영-매뉴얼)
9. [모니터링](#9-모니터링)
10. [장애 대응](#10-장애-대응)

---

## 1. 아키텍처 선택

### 권장 구성 (소규모 팀 / 스타트업)

```
인터넷
  │
  ▼
Route 53 (DNS)
  │
  ▼
EC2 (t3.small ~ t3.medium)
  ├── Nginx  :80 / :443  ← Vue 3 SPA + HTTPS
  ├── Spring Boot  :8080
  └── MySQL  :3306        ← Docker named volume
```

단일 EC2에 Docker Compose로 전체 스택을 운영합니다.  
**월 비용 기준**: t3.small(약 $16) + 도메인(약 $12/년) = 약 $17~20/월

### 확장 구성 (50인 이상 / 높은 가용성 필요 시)

```
Route 53 → ALB → EC2 (Auto Scaling)
                       └── Spring Boot (ECS Fargate도 가능)
                RDS MySQL (Multi-AZ)
                ECR (Docker Image 레지스트리)
                CloudFront + S3 (Vue SPA 정적 호스팅)
```

이 문서는 **권장 구성(단일 EC2)**을 기준으로 작성합니다.

---

## 2. 사전 준비

### 2-1. 필요 항목 체크리스트

- [ ] AWS 계정 (루트 계정 MFA 설정 완료)
- [ ] IAM 사용자 생성 + AWS CLI 설치 및 설정
- [ ] 도메인 (Route 53 또는 외부 도메인 등록기)
- [ ] GitHub 리포지토리 (CI/CD 자동화용)
- [ ] 이메일 발신용 Gmail 앱 비밀번호 (선택)

### 2-2. AWS CLI 설치

```bash
# macOS
brew install awscli

# Ubuntu/Debian
sudo apt install awscli

# Windows (PowerShell)
winget install Amazon.AWSCLI
```

```bash
aws configure
# AWS Access Key ID: [IAM 사용자 액세스 키]
# AWS Secret Access Key: [IAM 사용자 시크릿 키]
# Default region name: ap-northeast-2   ← 서울 리전
# Default output format: json
```

---

## 3. AWS 인프라 구성

### 3-1. VPC / 보안 그룹

AWS 콘솔 → EC2 → 보안 그룹 → **보안 그룹 생성**

| 규칙 | 유형 | 포트 | 소스 | 설명 |
|------|------|------|------|------|
| 인바운드 | SSH | 22 | 내 IP | 관리자 SSH 접속 |
| 인바운드 | HTTP | 80 | 0.0.0.0/0 | 웹 서비스 |
| 인바운드 | HTTPS | 443 | 0.0.0.0/0 | 웹 서비스 (SSL) |
| 아웃바운드 | 전체 | 전체 | 0.0.0.0/0 | 외부 통신 |

> 포트 8080(Spring Boot), 3306(MySQL)은 외부에 열지 않습니다.  
> 컨테이너 간 통신은 Docker 내부 네트워크를 사용합니다.

### 3-2. EC2 인스턴스 생성

```
AMI: Ubuntu Server 24.04 LTS (64비트 x86)
인스턴스 유형: t3.small (vCPU 2, 메모리 2GiB) — 최소 권장
              t3.medium (vCPU 2, 메모리 4GiB) — 안정적 운영 권장
스토리지: 20GB gp3 (최소) / 30GB 권장
키 페어: 새 키 페어 생성 → secportal-key.pem 다운로드 후 안전하게 보관
보안 그룹: 3-1에서 생성한 그룹 선택
```

### 3-3. Elastic IP 할당

인스턴스 재시작 시 IP가 바뀌지 않도록 고정 IP를 할당합니다.

```
EC2 콘솔 → 탄력적 IP → 탄력적 IP 주소 할당 → 할당
→ 작업 → 탄력적 IP 주소 연결 → 인스턴스 선택
```

할당된 IP를 메모해 둡니다. (예: `13.124.xxx.xxx`)

### 3-4. Route 53 DNS 설정

도메인이 있는 경우 A 레코드를 추가합니다.

```
Route 53 → 호스팅 영역 → 도메인 선택 → 레코드 생성
  레코드 유형: A
  레코드 이름: secportal.yourdomain.com (또는 @)
  값: 13.124.xxx.xxx (Elastic IP)
  TTL: 300
```

도메인이 없으면 Elastic IP로 직접 접속합니다 (HTTP only).

---

## 4. 서버 초기 설정

### 4-1. SSH 접속

```bash
# 키 파일 권한 설정 (macOS/Linux)
chmod 400 secportal-key.pem

# SSH 접속
ssh -i secportal-key.pem ubuntu@13.124.xxx.xxx
```

### 4-2. 자동 초기화 스크립트 (권장)

아래 한 줄로 서버 초기 구성을 자동화합니다.

```bash
# EC2 서버에서 실행
sudo bash <(curl -fsSL https://raw.githubusercontent.com/monosun/secportal/main/scripts/server-setup.sh)
```

스크립트가 자동으로 수행하는 작업:

| 단계 | 내용 |
|------|------|
| 1 | 시스템 패키지 업데이트 (`apt upgrade`) |
| 2 | 타임존 설정 (`Asia/Seoul`) |
| 3 | 스왑 메모리 2GB 설정 (t3.small OOM 방지) |
| 4 | Docker + Docker Compose 플러그인 설치 |
| 5 | 소스코드 클론 (`/home/ubuntu/secportal`) |
| 6 | `.env` 파일 생성 및 JWT_SECRET 자동 생성 |
| 7 | 자동 백업 cron 등록 (매일 02:00) |

스크립트 완료 후 **반드시 DB 비밀번호를 변경**해야 합니다:

```bash
nano /home/ubuntu/secportal/.env
# DB_ROOT_PASSWORD, DB_PASSWORD 변경
```

### 4-3. 수동 초기화 (단계별)

자동 스크립트 없이 직접 구성하는 경우:

```bash
# 1. 시스템 업데이트
sudo apt update && sudo apt upgrade -y
sudo apt install -y git curl unzip

# 2. 타임존
sudo timedatectl set-timezone Asia/Seoul

# 3. 스왑 메모리 (t3.small 필수)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 4. Docker 설치
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
newgrp docker  # 재로그인 대신 즉시 적용

# 5. 소스코드 클론
git clone https://github.com/monosun/secportal.git /home/ubuntu/secportal
cd /home/ubuntu/secportal
```

---

## 5. 애플리케이션 배포

### 5-1. 환경변수 설정

```bash
cd /home/ubuntu/secportal
cp .env.example .env
nano .env
```

`.env` 파일 필수 수정 항목:

```bash
# DB 비밀번호 — 반드시 강력한 값으로 변경
DB_ROOT_PASSWORD=강력한루트비밀번호123!
DB_PASSWORD=강력한앱비밀번호456!

# JWT 시크릿 — 32자 이상 무작위 문자열
# 자동 생성: openssl rand -base64 48
JWT_SECRET=여기에32자이상시크릿키입력

# 이미지 레지스트리 (ghcr.io 사용 시)
GITHUB_OWNER=monosun
VERSION=latest

# 이메일 발신 설정 (알림 + 이메일 승인 워크플로우)
# Gmail 앱 비밀번호 발급: Google 계정 → 보안 → 2단계 인증 → 앱 비밀번호
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=앱비밀번호16자리

# 이메일 승인 링크 기준 URL (승인/거부 링크가 이 URL로 생성됨)
# 프로덕션 도메인 사용 시: https://secportal.yourdomain.com/api
APP_BASE_URL=https://secportal.yourdomain.com/api
```

### 5-2. 최초 배포 실행

```bash
cd /home/ubuntu/secportal
docker compose up -d --build

# 실시간 로그 확인
docker compose logs -f
```

### 5-3. 배포 확인

```bash
docker compose ps
```

기대 출력:
```
NAME                  STATUS          PORTS
secportal-db          Up (healthy)    3306/tcp
secportal-backend     Up              8080/tcp
secportal-frontend    Up              0.0.0.0:80->80/tcp
```

브라우저에서 `http://13.124.xxx.xxx` 접속:
- 이메일: `secportal@monosun.com`
- 비밀번호: `Admin1234!`

> **초기 접속 후 반드시 관리자 비밀번호를 변경하세요.**

---

## 6. HTTPS 설정

도메인이 있는 경우 Let's Encrypt로 무료 SSL 인증서를 발급합니다.

### 6-1. Certbot 설치

```bash
sudo apt install -y certbot
```

### 6-2. 인증서 발급

```bash
# standalone 모드 (포트 80을 일시 사용)
docker compose stop frontend
sudo certbot certonly --standalone -d secportal.yourdomain.com
docker compose start frontend
```

발급된 인증서 위치:
```
/etc/letsencrypt/live/secportal.yourdomain.com/fullchain.pem
/etc/letsencrypt/live/secportal.yourdomain.com/privkey.pem
```

### 6-3. HTTPS nginx 설정 적용

```bash
cd /home/ubuntu/secportal

# nginx.https.conf의 도메인을 실제 도메인으로 변경
sed -i 's/your-domain.com/secportal.yourdomain.com/g' nginx/nginx.https.conf

# nginx.conf 교체
cp nginx/nginx.https.conf nginx/nginx.conf

# 재시작
docker compose restart frontend
```

### 6-4. 인증서 자동 갱신 설정

Let's Encrypt 인증서는 90일마다 만료됩니다.

```bash
sudo crontab -e
```

아래 줄 추가:

```cron
0 3 * * * certbot renew --quiet && docker compose -f /home/ubuntu/secportal/docker-compose.yml restart frontend
```

---

## 7. CI/CD 파이프라인

### 7-1. 파이프라인 구조

```
git push main / PR 오픈
        │
        ▼
  ┌─────────────┐
  │  build job  │  백엔드 Gradle 빌드 + 프론트엔드 npm build
  └──────┬──────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌─────────────────────────────────────┐
│ deploy │ │ release  (태그 push v*.*.* 시에만)  │
│  job   │ │  · ghcr.io 이미지 빌드 & Push       │
│(main   │ │  · 릴리즈 아카이브 생성             │
│ push   │ │  · GitHub Release 생성              │
│  시)   │ └─────────────────────────────────────┘
└────────┘
```

### 7-2. 배포 ON/OFF 스위치

배포는 기본적으로 **OFF** 상태입니다. Repository Variable로 언제든 켜고 끌 수 있습니다.

```
GitHub 리포지토리 → Settings → Secrets and variables → Actions → Variables 탭
→ DEPLOY_ENABLED 값 변경
```

| 값 | 동작 |
|----|------|
| `true` | `main` push 시 EC2 자동 배포 실행 |
| `false` | 배포 잡 건너뜀 (기본값) |

> 상세 활성화 절차 → [docs/ec2-deploy-setup.md](ec2-deploy-setup.md)

### 7-3. GitHub Secrets 설정

GitHub 리포지토리 → **Settings → Secrets and variables → Actions → New repository secret**

| Secret 이름 | 값 | 설명 |
|-------------|-----|------|
| `EC2_HOST` | `13.124.xxx.xxx` | 서버 Elastic IP 또는 도메인 |
| `EC2_USER` | `ubuntu` | SSH 접속 사용자명 |
| `EC2_SSH_KEY` | PEM 키 전체 내용 | `cat secportal-key.pem` |
| `ENV_FILE` | `.env` 파일 전체 내용 | `cat /home/ubuntu/secportal/.env` |

> **ENV_FILE에 포함되어야 할 주요 항목** (이메일 승인 기능 사용 시):
> ```
> MAIL_USERNAME=your-email@gmail.com
> MAIL_PASSWORD=Gmail앱비밀번호16자리
> APP_BASE_URL=https://secportal.yourdomain.com/api
> ```
> `APP_BASE_URL`이 없으면 이메일 승인 링크가 `localhost`로 생성되어 수신자가 클릭해도 접속이 안 됩니다.

**EC2_SSH_KEY 값 형식** (BEGIN ~ END 줄 모두 포함):
```
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEA...
-----END RSA PRIVATE KEY-----
```

### 7-4. GitHub production 환경 설정

`ci.yml`의 deploy job이 `environment: production`을 사용하므로 환경을 생성해야 합니다.

```
GitHub 리포지토리 → Settings → Environments → New environment
→ 이름: production → Save
```

환경을 생성하지 않으면 deploy job이 대기 상태로 멈춥니다.

### 7-5. 자동 배포 흐름 (main push 시)

```
개발자 git push origin main
  │
  ▼
GitHub Actions build job (빌드 검증)
  │ 성공 시
  ▼
deploy job: SSH로 EC2 접속
  → git pull origin main      (최신 코드 반영)
  → printf ENV_FILE > .env    (환경변수 갱신)
  → docker compose up -d --build  (무중단 재시작)
  → docker image prune -f     (구 이미지 정리)
```

### 7-6. 릴리즈 이미지 빌드 흐름 (태그 push 시)

```bash
# 로컬에서 태그 생성 및 push
git tag v1.2.0
git push origin v1.2.0
```

```
GitHub Actions build job
  │ 성공 시
  ▼
release job:
  · ghcr.io 이미지 빌드 & Push (backend, frontend)
  · release/v1.2.0/ 아카이브 생성
  · GitHub Release 페이지에 업로드
```

---

## 8. 운영 매뉴얼

### 8-0. 이메일 승인 워크플로우 (사용자 관리)

계정 삭제 또는 ADMIN 권한 부여 시 이메일 승인이 필요합니다.

**흐름:**
```
ADMIN 화면에서 삭제/ADMIN 승격 요청
  │
  ▼
백엔드가 pending_admin_actions 테이블에 토큰 저장 (24시간 유효)
  │
  ▼
noreply@monosun.com(승인 알림 수신 주소)으로 [승인] / [거부] 링크 포함 이메일 발송
  │
  ▼ (수신자가 링크 클릭)
GET /api/admin/approve/{token}  →  사용자 비활성화 or ADMIN 승격 실행
GET /api/admin/reject/{token}   →  요청 취소
  │
  ▼
브라우저에 결과 HTML 페이지 표시
```

**전제 조건:** `.env`에 `MAIL_USERNAME`, `MAIL_PASSWORD`, `APP_BASE_URL` 설정 필수.  
이메일 미설정 시 요청은 DB에 저장되지만 이메일이 발송되지 않습니다.

**Gmail 앱 비밀번호 발급:**
```
Google 계정 → 보안 → 2단계 인증 → 앱 비밀번호
→ 앱: 기타(직접 입력) → 이름: SecPortal → 생성
→ 16자리 비밀번호 복사 → MAIL_PASSWORD에 입력
```

**승인 링크 수동 확인 (이메일 미발송 시):**
```bash
# 대기 중인 액션 토큰 조회
docker exec secportal-db mysql -usecportal -psecportal123 secportal \
  -e "SELECT token, action_type, status, expires_at FROM pending_admin_actions WHERE status='PENDING';"

# 수동 승인
curl http://localhost:8080/api/admin/approve/{토큰값}
```

### 8-1. 일상 운영 명령어

```bash
# EC2 접속
ssh -i secportal-key.pem ubuntu@13.124.xxx.xxx
cd /home/ubuntu/secportal

# 컨테이너 상태 확인
docker compose ps

# 전체 로그 (실시간)
docker compose logs -f

# 서비스별 로그
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db

# 최근 100줄
docker compose logs --tail=100 backend
```

### 8-2. 수동 배포 (GitHub Actions 없이)

```bash
cd /home/ubuntu/secportal
git pull origin main
nano .env               # 필요 시 수정
docker compose up -d --build
docker image prune -f
```

### 8-3. 서비스 재시작

```bash
# 전체 재시작
docker compose restart

# 특정 서비스만
docker compose restart backend

# 완전 중단 후 재시작 (데이터 유지)
docker compose down && docker compose up -d
```

### 8-4. 데이터베이스 백업

```bash
# 수동 백업
bash /home/ubuntu/secportal/scripts/backup.sh

# 백업 파일 확인
ls -lh /home/ubuntu/secportal/backups/
```

자동 백업은 서버 초기화 스크립트가 매일 02:00 cron으로 등록합니다.  
백업 보관 기간은 `.env`의 `BACKUP_KEEP_DAYS` 값으로 조정합니다 (기본 30일).

### 8-5. 데이터베이스 복원

```bash
bash /home/ubuntu/secportal/scripts/restore.sh backups/secportal_20260516_020000.sql
```

### 8-6. 백업 파일 로컬로 다운로드

```bash
# 로컬 PC에서 실행
scp -i secportal-key.pem \
  ubuntu@13.124.xxx.xxx:/home/ubuntu/secportal/backups/secportal_20260516.sql \
  ./
```

### 8-7. 디스크 사용량 모니터링

```bash
df -h                     # 전체 디스크
docker system df          # Docker 리소스
docker system prune -f    # 불필요 리소스 정리 (db_data 볼륨 제외)
```

### 8-8. 환경변수 변경 후 반영

```bash
nano .env
docker compose up -d   # 이미지 재빌드 없이 환경변수만 반영
```

---

## 9. 모니터링

### 9-1. 서버 상태 확인 스크립트

```bash
cat > /home/ubuntu/check-status.sh << 'EOF'
#!/bin/bash
echo "=== $(date) ==="
echo "--- 컨테이너 상태 ---"
docker compose -f /home/ubuntu/secportal/docker-compose.yml ps
echo "--- 디스크 사용량 ---"
df -h /
echo "--- 메모리 사용량 ---"
free -h
echo "--- 백엔드 헬스 체크 ---"
curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost:8080/actuator/health 2>/dev/null || \
  curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost:8080/api/auth/me 2>/dev/null
echo ""
EOF
chmod +x /home/ubuntu/check-status.sh
```

### 9-2. UptimeRobot 외부 모니터링 (무료)

[UptimeRobot](https://uptimerobot.com) 무료 플랜으로 5분 간격 모니터링:
- URL: `https://secportal.yourdomain.com`
- 다운 시 이메일/Slack 알림 수신 가능

### 9-3. CloudWatch 에이전트 (선택)

```bash
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard
sudo systemctl enable --now amazon-cloudwatch-agent
```

EC2 IAM 역할에 `CloudWatchAgentServerPolicy` 권한 필요.

---

## 10. 장애 대응

### 10-1. 컨테이너가 죽었을 때

```bash
docker compose ps
docker compose logs --tail=50 backend
docker compose restart backend
```

### 10-2. DB 연결 실패

```bash
docker compose ps db
docker exec secportal-db mysqladmin ping -h localhost
docker compose restart db
sleep 10
docker compose restart backend   # 커넥션 풀 초기화
```

### 10-3. 디스크 가득 참

```bash
du -sh /var/* 2>/dev/null | sort -rh | head -10
docker system prune -af          # ⚠️ db_data 볼륨은 제외됨
find /home/ubuntu/secportal/backups -name '*.sql' -mtime +14 -delete
```

### 10-4. 메모리 부족 (OOM)

```bash
free -h
docker stats --no-stream

# JVM 힙 조정 — docker-compose.yml backend environment에 추가:
#   JAVA_OPTS: "-Xmx512m -Xms256m"
docker compose up -d backend
```

### 10-5. 배포 실패 후 롤백

```bash
cd /home/ubuntu/secportal
git log --oneline -5
git checkout <이전커밋해시>
docker compose up -d --build
# 확인 후
git checkout main
```

---

## 부록: 비용 최적화 팁

| 항목 | 방법 | 절감 효과 |
|------|------|----------|
| EC2 예약 인스턴스 | 1년 약정 구매 | ~30% 절감 |
| EC2 Savings Plans | 1년 약정 | ~20% 절감 |
| 야간 중단 (개발 서버) | EventBridge 스케줄로 start/stop | ~65% 절감 |
| 스냅샷 관리 | 30일 이상 스냅샷 자동 삭제 | EBS 비용 절감 |
| CloudFront | S3 정적 호스팅 + CDN | EC2 대역폭 절감 |
