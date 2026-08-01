#!/bin/sh
# Fix bind-mount directories that may have been created by root in previous runs
mkdir -p /app/uploads /app/backups
chown -R appuser:appgroup /app/uploads /app/backups 2>/dev/null || true

# 비밀 값을 파일로 주입할 수 있게 한다 (docker secret / 마운트 파일).
# 예) JASYPT_ENCRYPTOR_PASSWORD_FILE=/run/secrets/jasypt_key
# 파일로 주입하면 `docker inspect`·프로세스 환경에 평문이 남지 않는다.
load_secret_file() {
  var="$1"
  file_var="${var}_FILE"
  eval "file_path=\${${file_var}:-}"
  if [ -n "$file_path" ]; then
    if [ -r "$file_path" ]; then
      # 마지막 개행 제거 후 환경변수로 설정
      value=$(cat "$file_path")
      export "$var=$value"
      unset "$file_var"
      echo "[entrypoint] loaded $var from $file_path"
    else
      echo "[entrypoint] WARN: $file_var=$file_path 를 읽을 수 없습니다. $var 값을 그대로 사용합니다." >&2
    fi
  fi
}

load_secret_file JASYPT_ENCRYPTOR_PASSWORD
load_secret_file PI_ENCRYPTION_KEY
load_secret_file SPRING_DATASOURCE_PASSWORD
load_secret_file JWT_SECRET
load_secret_file MAIL_PASSWORD

exec su-exec appuser java -jar /app/app.jar "$@"
