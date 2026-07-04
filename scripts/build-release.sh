#!/usr/bin/env bash
# SecPortal — 릴리즈 패키지 빌드 스크립트
#
# 사용법:
#   bash scripts/build-release.sh v1.0.0 [your-org]
#
# 수행 작업:
#   1. Docker 이미지 빌드 (backend, frontend)
#   2. ghcr.io에 이미지 Push (로그인 필요)
#   3. release/v1.0.0/ 디렉토리 업데이트
#   4. 배포 가능한 tar.gz 아카이브 생성

set -euo pipefail

VERSION="${1:-}"
GITHUB_OWNER="${2:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# ── 입력값 검증 ──────────────────────────────
if [ -z "$VERSION" ]; then
  read -rp "릴리즈 버전을 입력하세요 (예: v1.0.0): " VERSION
fi
if [ -z "$GITHUB_OWNER" ]; then
  read -rp "GitHub 조직/사용자명을 입력하세요: " GITHUB_OWNER
fi

BACKEND_IMAGE="ghcr.io/$GITHUB_OWNER/secportal-backend:$VERSION"
FRONTEND_IMAGE="ghcr.io/$GITHUB_OWNER/secportal-frontend:$VERSION"
RELEASE_DIR="$ROOT_DIR/release/$VERSION"

echo "========================================"
echo " SecPortal 릴리즈 빌드: $VERSION"
echo " 이미지: ghcr.io/$GITHUB_OWNER/secportal-*"
echo "========================================"

cd "$ROOT_DIR"

# ── 1. 릴리즈 디렉토리 준비 ─────────────────
echo ""
echo "[1/5] 릴리즈 디렉토리 준비..."
mkdir -p "$RELEASE_DIR"/{db/init,nginx,scripts,backups}

# 최신 소스에서 SQL / nginx / 스크립트 복사
cp db/init/*.sql          "$RELEASE_DIR/db/init/"
cp nginx/nginx.conf        "$RELEASE_DIR/nginx/"
cp nginx/nginx.https.conf  "$RELEASE_DIR/nginx/" 2>/dev/null || \
  cp release/v1.0.0/nginx/nginx.https.conf "$RELEASE_DIR/nginx/" 2>/dev/null || true
cp release/v1.0.0/scripts/*.sh "$RELEASE_DIR/scripts/"
chmod +x "$RELEASE_DIR/scripts/"*.sh

# .env.example — GITHUB_OWNER와 VERSION 반영
sed "s/GITHUB_OWNER=your-org/GITHUB_OWNER=$GITHUB_OWNER/;s/VERSION=v1.0.0/VERSION=$VERSION/" \
  release/v1.0.0/.env.example > "$RELEASE_DIR/.env.example"

# docker-compose.yml — 최신본 복사
cp release/v1.0.0/docker-compose.yml "$RELEASE_DIR/docker-compose.yml"

# RELEASE_NOTES, QUICKSTART 복사 (새 버전이면 수정 필요)
cp release/v1.0.0/RELEASE_NOTES.md "$RELEASE_DIR/RELEASE_NOTES.md" 2>/dev/null || true
cp release/v1.0.0/QUICKSTART.md    "$RELEASE_DIR/QUICKSTART.md"

echo "$VERSION" > "$RELEASE_DIR/VERSION"

echo "릴리즈 디렉토리: $RELEASE_DIR"

# ── 2. 백엔드 이미지 빌드 ───────────────────
echo ""
echo "[2/5] 백엔드 이미지 빌드..."
docker build \
  -t "$BACKEND_IMAGE" \
  -t "ghcr.io/$GITHUB_OWNER/secportal-backend:latest" \
  ./backend

# ── 3. 프론트엔드 이미지 빌드 ───────────────
echo ""
echo "[3/5] 프론트엔드 이미지 빌드..."
docker build \
  -f frontend/Dockerfile.release \
  -t "$FRONTEND_IMAGE" \
  -t "ghcr.io/$GITHUB_OWNER/secportal-frontend:latest" \
  .

# ── 4. 이미지 Push ──────────────────────────
echo ""
echo "[4/5] 이미지 Push to ghcr.io..."
if ! docker login ghcr.io 2>/dev/null; then
  echo "ghcr.io 로그인이 필요합니다."
  echo "GitHub PAT(packages:write 권한)로 로그인:"
  docker login ghcr.io -u "$GITHUB_OWNER"
fi
docker push "$BACKEND_IMAGE"
docker push "ghcr.io/$GITHUB_OWNER/secportal-backend:latest"
docker push "$FRONTEND_IMAGE"
docker push "ghcr.io/$GITHUB_OWNER/secportal-frontend:latest"

# ── 5. 배포 아카이브 생성 ───────────────────
echo ""
echo "[5/5] 배포 아카이브 생성..."
ARCHIVE="$ROOT_DIR/secportal-$VERSION.tar.gz"
tar -czf "$ARCHIVE" \
  -C "$ROOT_DIR/release" \
  "$VERSION"

FILESIZE="$(du -sh "$ARCHIVE" | cut -f1)"
echo ""
echo "========================================"
echo " 릴리즈 빌드 완료"
echo "========================================"
echo ""
echo "배포 아카이브: $ARCHIVE ($FILESIZE)"
echo "릴리즈 디렉토리: $RELEASE_DIR"
echo ""
echo "배포 서버에서 실행할 명령:"
echo "  scp $ARCHIVE user@server:/home/ubuntu/"
echo "  ssh user@server 'cd /home/ubuntu && tar -xzf secportal-$VERSION.tar.gz'"
echo "  ssh user@server 'cd /home/ubuntu/$VERSION && bash scripts/setup.sh'"
echo ""
echo "또는 GitHub Release에 아카이브를 첨부하면"
echo "사용자가 직접 다운로드할 수 있습니다."
