#!/usr/bin/env bash
# SecPortal — EC2 서버 최초 초기화 스크립트
# Ubuntu 22.04 / 24.04 LTS 기준
# 사용법: bash <(curl -fsSL https://raw.githubusercontent.com/monosun/secportal/main/scripts/server-setup.sh)
# 또는:   bash scripts/server-setup.sh

set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/monosun/secportal.git}"
DEPLOY_DIR="${DEPLOY_DIR:-/home/ubuntu/secportal}"
SWAP_SIZE="${SWAP_SIZE:-2G}"

# ── 색상 출력 헬퍼 ─────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }
step()  { echo -e "\n${GREEN}══ $* ══${NC}"; }

# ── root 체크 ──────────────────────────────
if [ "$(id -u)" -ne 0 ]; then
  error "root 권한으로 실행하세요: sudo bash $0"
fi

UBUNTU_USER="${SUDO_USER:-ubuntu}"

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   SecPortal — EC2 서버 초기화 스크립트  ║"
echo "╚══════════════════════════════════════════╝"
echo ""
echo "  배포 디렉토리  : $DEPLOY_DIR"
echo "  저장소        : $REPO_URL"
echo "  스왑 크기     : $SWAP_SIZE"
echo "  실행 사용자   : $UBUNTU_USER"
echo ""
read -rp "계속하시겠습니까? [y/N]: " confirm
[[ "$confirm" =~ ^[Yy]$ ]] || { echo "취소됨."; exit 0; }

# ────────────────────────────────────────────
step "1/7  시스템 업데이트 및 기본 패키지 설치"
# ────────────────────────────────────────────
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get upgrade -y -qq
apt-get install -y -qq git curl unzip ca-certificates gnupg lsb-release
info "시스템 패키지 업데이트 완료"

# ────────────────────────────────────────────
step "2/7  타임존 설정 (Asia/Seoul)"
# ────────────────────────────────────────────
timedatectl set-timezone Asia/Seoul
info "타임존: $(timedatectl | grep 'Time zone')"

# ────────────────────────────────────────────
step "3/7  스왑 메모리 설정 ($SWAP_SIZE)"
# ────────────────────────────────────────────
if swapon --show | grep -q '/swapfile'; then
  warn "스왑파일이 이미 존재합니다. 건너뜁니다."
else
  fallocate -l "$SWAP_SIZE" /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  info "스왑 설정 완료: $(swapon --show)"
fi

# ────────────────────────────────────────────
step "4/7  Docker 설치"
# ────────────────────────────────────────────
if command -v docker &>/dev/null; then
  warn "Docker가 이미 설치되어 있습니다: $(docker --version)"
else
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg

  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    > /etc/apt/sources.list.d/docker.list

  apt-get update -qq
  apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  systemctl enable --now docker
  info "Docker 설치 완료: $(docker --version)"
fi

# docker 그룹에 ubuntu 사용자 추가
if ! groups "$UBUNTU_USER" | grep -q docker; then
  usermod -aG docker "$UBUNTU_USER"
  info "$UBUNTU_USER 사용자를 docker 그룹에 추가했습니다. (재로그인 후 적용)"
fi

# Docker 자동 시작 확인
systemctl is-enabled docker &>/dev/null || systemctl enable docker
info "Docker 자동 시작 활성화됨"

# ────────────────────────────────────────────
step "5/7  소스코드 클론"
# ────────────────────────────────────────────
if [ -d "$DEPLOY_DIR/.git" ]; then
  warn "이미 클론된 저장소가 존재합니다. git pull을 실행합니다."
  sudo -u "$UBUNTU_USER" git -C "$DEPLOY_DIR" pull origin main
else
  sudo -u "$UBUNTU_USER" git clone "$REPO_URL" "$DEPLOY_DIR"
  info "저장소 클론 완료: $DEPLOY_DIR"
fi

# ────────────────────────────────────────────
step "6/7  환경변수 파일 생성"
# ────────────────────────────────────────────
if [ -f "$DEPLOY_DIR/.env" ]; then
  warn ".env 파일이 이미 존재합니다. 건너뜁니다."
else
  sudo -u "$UBUNTU_USER" cp "$DEPLOY_DIR/.env.example" "$DEPLOY_DIR/.env"

  # JWT_SECRET 자동 생성
  JWT_SECRET=$(openssl rand -base64 48)
  sed -i "s|CHANGE_THIS_TO_A_RANDOM_32_CHAR_OR_LONGER_SECRET_KEY|$JWT_SECRET|" "$DEPLOY_DIR/.env"
  sed -i "s|GITHUB_OWNER=your-org|GITHUB_OWNER=monosun|" "$DEPLOY_DIR/.env"

  info ".env 파일이 생성되었습니다."
  warn "DB_ROOT_PASSWORD 와 DB_PASSWORD 를 반드시 변경하세요:"
  warn "  nano $DEPLOY_DIR/.env"
fi

# ────────────────────────────────────────────
step "7/7  자동 백업 cron 등록"
# ────────────────────────────────────────────
CRON_CMD="0 2 * * * cd $DEPLOY_DIR && bash scripts/backup.sh >> /var/log/secportal-backup.log 2>&1"
CRON_FILE="/etc/cron.d/secportal-backup"

if [ -f "$CRON_FILE" ]; then
  warn "백업 cron이 이미 등록되어 있습니다."
else
  echo "$CRON_CMD" > "$CRON_FILE"
  chmod 644 "$CRON_FILE"
  info "자동 백업 cron 등록 완료 (매일 02:00)"
fi

# ────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════╗"
echo "║        초기화 완료!                      ║"
echo "╚══════════════════════════════════════════╝"
echo ""
echo "  다음 단계:"
echo ""
echo "  1. DB 비밀번호 설정 (필수)"
echo "     nano $DEPLOY_DIR/.env"
echo "     → DB_ROOT_PASSWORD, DB_PASSWORD 변경"
echo ""
echo "  2. 서비스 시작"
echo "     cd $DEPLOY_DIR"
echo "     sudo -u $UBUNTU_USER docker compose up -d --build"
echo ""
echo "  3. 상태 확인"
echo "     docker compose ps"
echo "     docker compose logs -f"
echo ""
echo "  GitHub Actions 자동 배포를 위한 Secrets:"
echo "    EC2_HOST     : $(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo '<서버 IP>')"
echo "    EC2_USER     : $UBUNTU_USER"
echo "    EC2_SSH_KEY  : PEM 키 파일 내용"
echo "    ENV_FILE     : $DEPLOY_DIR/.env 파일 내용"
echo ""
