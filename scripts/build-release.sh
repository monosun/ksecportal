#!/usr/bin/env bash
# KSecPortal — ghcr.io 이미지 배포용 릴리즈 패키지 빌드
#
# 사용법:
#   bash scripts/build-release.sh <버전> [GitHub 조직/사용자] [--no-push] [--skip-build]
#   bash scripts/build-release.sh v1.45.1 monosun
#   bash scripts/build-release.sh v1.45.1 monosun --no-push     # 빌드·패키지만(레지스트리 업로드 없이 점검)
#
# 수행 작업:
#   1. dist/release/<버전>/ 에 서버 배포 패키지 구성
#      - docker-compose.yml : 루트 compose 의 build 를 ghcr 이미지(image:)로 바꾼 것, DB 포트 노출 제거
#      - db/init/*.sql, .env.example(+GITHUB_OWNER·VERSION), RELEASE_NOTES.md, QUICKSTART.md, VERSION
#   2. backend · frontend · gotenberg 이미지 빌드 (운영 compose 와 같은 Dockerfile 사용)
#   3. ghcr.io/<owner>/secportal-{backend,frontend,gotenberg}:<버전> 및 :latest push
#   4. dist/secportal-<버전>.tar.gz 아카이브 생성
#
# 배포 서버에는 소스코드가 필요 없다 — 아카이브만 풀고 .env 작성 후 docker compose pull && up -d.

set -euo pipefail

VERSION=""
GITHUB_OWNER=""
PUSH=1
BUILD=1
for arg in "$@"; do
  case "$arg" in
    --no-push)    PUSH=0 ;;
    --skip-build) BUILD=0 ;;
    -h|--help)    sed -n '2,17p' "$0"; exit 0 ;;
    -*)           echo "알 수 없는 옵션: $arg" >&2; exit 1 ;;
    *)            if [ -z "$VERSION" ]; then VERSION="$arg"; else GITHUB_OWNER="$arg"; fi ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

# ── 입력값 검증 ──────────────────────────────
if [ -z "$VERSION" ]; then
  read -rp "릴리즈 버전을 입력하세요 (예: v1.45.1): " VERSION
fi
if [ -z "$GITHUB_OWNER" ]; then
  read -rp "GitHub 조직/사용자명을 입력하세요 [monosun]: " GITHUB_OWNER
  GITHUB_OWNER="${GITHUB_OWNER:-monosun}"
fi
if ! [[ "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "버전 형식이 올바르지 않습니다: $VERSION (예: v1.45.1)" >&2; exit 1
fi
if ! [[ "$GITHUB_OWNER" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "GitHub 조직/사용자명 형식이 올바르지 않습니다: $GITHUB_OWNER" >&2; exit 1
fi
# ghcr 이미지 경로는 소문자여야 한다
OWNER_LC="$(echo "$GITHUB_OWNER" | tr '[:upper:]' '[:lower:]')"

# 소스 버전과 일치하는지 확인 — 이미지 메타데이터와 화면 버전 표시가 어긋나지 않도록
SRC_VERSION="v$(sed -n 's/^version = '"'"'\(.*\)'"'"'/\1/p' backend/build.gradle)"
if [ "$SRC_VERSION" != "$VERSION" ]; then
  echo "경고: backend/build.gradle 버전($SRC_VERSION)과 릴리즈 버전($VERSION)이 다릅니다."
  read -rp "그래도 계속하시겠습니까? [y/N]: " ok
  [[ "$ok" =~ ^[Yy]$ ]] || { echo "취소됨."; exit 1; }
fi

REGISTRY="ghcr.io/$OWNER_LC"
SERVICES=(backend frontend gotenberg)
OUT_BASE="$ROOT_DIR/dist/release"
PKG_DIR="$OUT_BASE/$VERSION"
ARCHIVE="$ROOT_DIR/dist/secportal-$VERSION.tar.gz"

echo "========================================"
echo " KSecPortal 릴리즈 빌드: $VERSION"
echo " 이미지: $REGISTRY/secportal-{backend,frontend,gotenberg}"
echo " push: $([ $PUSH -eq 1 ] && echo 예 || echo '아니오(--no-push)')"
echo "========================================"

# ── 1. 배포 패키지 구성 ─────────────────────
echo ""
echo "[1/4] 배포 패키지 구성: $PKG_DIR"
rm -rf "$PKG_DIR"
mkdir -p "$PKG_DIR/db/init"

cp db/init/*.sql "$PKG_DIR/db/init/"

# docker-compose.yml: 각 서비스의 build: 블록을 image: 로 교체하고, 디버깅용 DB 포트 노출(127.0.0.1:3306)은 제거한다.
awk -v reg="$REGISTRY" '
  /^  [a-z][a-z0-9_-]*:[[:space:]]*$/ { svc = $1; sub(":", "", svc) }
  /^    build:[[:space:]]*$/ {
    print "    image: " reg "/secportal-" svc ":${VERSION:-latest}"
    skipping = 1; next
  }
  skipping && /^      / { next }
  { skipping = 0 }
  /^    ports:[[:space:]]*$/ {
    held = $0
    if ((getline nxt) > 0) {
      if (nxt ~ /127\.0\.0\.1:3306/) next
      print held; print nxt; next
    }
    print held; next
  }
  /^version:/ { next }
  { print }
' docker-compose.yml > "$PKG_DIR/docker-compose.yml"

if grep -qE '^    build:' "$PKG_DIR/docker-compose.yml"; then
  echo "docker-compose.yml 변환 실패: build: 블록이 남아 있습니다." >&2; exit 1
fi

{
  cat .env.example
  echo ""
  echo "# ── 릴리즈 이미지 (scripts/build-release.sh) ──"
  echo "# 업그레이드: VERSION 을 새 버전으로 바꾸고 docker compose pull && docker compose up -d"
  echo "GITHUB_OWNER=$OWNER_LC"
  echo "VERSION=$VERSION"
} > "$PKG_DIR/.env.example"

if [ -f "release/$VERSION/RELEASE_NOTES.md" ]; then
  cp "release/$VERSION/RELEASE_NOTES.md" "$PKG_DIR/RELEASE_NOTES.md"
else
  echo "  (release/$VERSION/RELEASE_NOTES.md 없음 — 릴리즈 노트 없이 패키징)"
fi

cat > "$PKG_DIR/QUICKSTART.md" <<EOF
# KSecPortal $VERSION 빠른 시작 (이미지 배포)

소스코드 없이 ghcr.io 이미지로 실행합니다. Docker · Docker Compose v2 가 필요합니다.

\`\`\`bash
tar -xzf secportal-$VERSION.tar.gz
cd $VERSION
cp .env.example .env
nano .env          # DB 비밀번호·JWT_SECRET·JASYPT_ENCRYPTOR_PASSWORD·PI_ENCRYPTION_KEY·CORS·APP_BASE_URL 등 설정
docker compose pull
docker compose up -d
docker compose logs backend --tail 50
\`\`\`

- 접속: http://<서버 주소> (nginx 80 포트)
- 비공개 패키지면 먼저 \`docker login ghcr.io\` (GitHub PAT, read:packages 권한)
- **업그레이드**: 새 패키지의 \`db/init\`·\`docker-compose.yml\` 을 반영하고, \`.env\` 의 \`VERSION\` 을 바꾼 뒤
  \`docker compose pull && docker compose up -d\`. 릴리즈 노트에 마이그레이션이 있으면 먼저 실행하세요.
- \`db/init/*.sql\` 은 DB 볼륨이 비어 있는 **최초 기동 때만** 실행됩니다.
EOF

echo "$VERSION" > "$PKG_DIR/VERSION"

# ── 2. 이미지 빌드 ──────────────────────────
echo ""
if [ $BUILD -eq 1 ]; then
  echo "[2/4] 이미지 빌드..."
  docker build -t "$REGISTRY/secportal-backend:$VERSION"   -t "$REGISTRY/secportal-backend:latest"   ./backend
  # frontend 는 운영 compose 와 같은 Dockerfile(루트 컨텍스트, nginx 설정·도움말 문서 포함)을 쓴다
  docker build -f frontend/Dockerfile \
               -t "$REGISTRY/secportal-frontend:$VERSION"  -t "$REGISTRY/secportal-frontend:latest"  .
  docker build -t "$REGISTRY/secportal-gotenberg:$VERSION" -t "$REGISTRY/secportal-gotenberg:latest" ./gotenberg
else
  echo "[2/4] 이미지 빌드 건너뜀(--skip-build) — 로컬에 같은 태그가 있어야 합니다."
fi

# ── 3. 이미지 Push ──────────────────────────
echo ""
if [ $PUSH -eq 1 ]; then
  echo "[3/4] 이미지 Push to ghcr.io..."
  if ! docker login ghcr.io </dev/null >/dev/null 2>&1; then
    echo "ghcr.io 로그인이 필요합니다. GitHub PAT(write:packages 권한)를 비밀번호로 입력하세요."
    docker login ghcr.io -u "$GITHUB_OWNER"
  fi
  for s in "${SERVICES[@]}"; do
    docker push "$REGISTRY/secportal-$s:$VERSION"
    docker push "$REGISTRY/secportal-$s:latest"
  done
else
  echo "[3/4] Push 건너뜀(--no-push)"
fi

# ── 4. 배포 아카이브 생성 ───────────────────
echo ""
echo "[4/4] 배포 아카이브 생성..."
tar -czf "$ARCHIVE" -C "$OUT_BASE" "$VERSION"

FILESIZE="$(du -sh "$ARCHIVE" | cut -f1)"
echo ""
echo "========================================"
echo " 릴리즈 빌드 완료"
echo "========================================"
echo "배포 아카이브 : $ARCHIVE ($FILESIZE)"
echo "패키지 폴더   : $PKG_DIR"
echo ""
echo "배포 서버에서:"
echo "  scp $ARCHIVE user@server:~/"
echo "  ssh user@server"
echo "  tar -xzf secportal-$VERSION.tar.gz && cd $VERSION && cp .env.example .env && nano .env"
echo "  docker compose pull && docker compose up -d"
echo ""
echo "GitHub Release 에 첨부: gh release upload $VERSION $ARCHIVE"
