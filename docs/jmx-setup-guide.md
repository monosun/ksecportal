# JMX 모니터링 설정 가이드

SecPortal 백엔드(Spring Boot)의 JVM 상태를 JMX로 모니터링하는 방법을 설명합니다.

---

## 개요

JMX(Java Management Extensions)를 사용하면 JVM 메모리, 스레드, HikariCP 커넥션 풀 등  
런타임 지표를 외부 도구(JConsole, VisualVM, java-monitor 등)로 실시간 확인할 수 있습니다.

> **보안 원칙**: JMX 포트는 외부 인터넷에 노출하지 않습니다.  
> 운영 서버에서는 SSH 터널 또는 컨테이너 exec를 통해서만 접속합니다.

---

## 1. 기본 구성 (현재 설정)

현재 `docker-compose.yml`은 JMX를 비활성화 상태로 배포합니다.

```yaml
# docker-compose.yml
backend:
  environment:
    JAVA_TOOL_OPTIONS: "-Duser.timezone=Asia/Seoul"
    # JMX 모니터링이 필요한 경우: docker exec -it secportal-backend sh 후 로컬에서 접속
```

JMX가 필요한 경우 아래 방법 중 하나를 선택해 활성화합니다.

---

## 2. 로컬 개발 환경 — JMX 활성화

로컬(`docker compose` 직접 실행)에서는 인증 없이 간단하게 활성화할 수 있습니다.

### 2-1. docker-compose.yml 수정

```yaml
backend:
  environment:
    JAVA_TOOL_OPTIONS: >-
      -Duser.timezone=Asia/Seoul
      -Dcom.sun.management.jmxremote
      -Dcom.sun.management.jmxremote.port=9999
      -Dcom.sun.management.jmxremote.rmi.port=9999
      -Dcom.sun.management.jmxremote.authenticate=false
      -Dcom.sun.management.jmxremote.ssl=false
      -Djava.rmi.server.hostname=127.0.0.1
  ports:
    - "127.0.0.1:9999:9999"    # 로컬호스트에서만 접속 가능
```

### 2-2. 적용 및 접속

```bash
docker compose up -d --build

# JConsole로 접속
jconsole 127.0.0.1:9999

# VisualVM으로 접속
jvisualvm
# → Add JMX Connection → 127.0.0.1:9999
```

---

## 3. 운영 서버 — SSH 터널을 통한 JMX 접속

운영 서버에서는 JMX 포트를 외부에 노출하지 않고 SSH 터널을 사용합니다.

### 3-1. 운영 서버 docker-compose.yml 수정

`docker-compose.yml`에서 JMX 포트를 로컬호스트에만 바인딩합니다.

```yaml
backend:
  environment:
    JAVA_TOOL_OPTIONS: >-
      -Duser.timezone=Asia/Seoul
      -Dcom.sun.management.jmxremote
      -Dcom.sun.management.jmxremote.port=9999
      -Dcom.sun.management.jmxremote.rmi.port=9999
      -Dcom.sun.management.jmxremote.authenticate=false
      -Dcom.sun.management.jmxremote.ssl=false
      -Djava.rmi.server.hostname=127.0.0.1
  ports:
    - "127.0.0.1:9999:9999"    # 서버 로컬호스트에서만 바인딩 (외부 차단)
```

```bash
docker compose up -d --build
```

### 3-2. 로컬 PC에서 SSH 터널 설정

로컬 PC의 터미널에서 실행합니다.

```bash
# SSH 터널 열기
ssh -L 9999:127.0.0.1:9999 <서버_사용자>@<서버_IP>

# 터널이 열린 상태에서 로컬 PC에서 접속
jconsole 127.0.0.1:9999
```

| 옵션 | 설명 |
|------|------|
| `-L 9999:127.0.0.1:9999` | 로컬 9999 → 서버의 127.0.0.1:9999로 포워딩 |
| 서버 IP | 운영 서버의 공인 IP 또는 도메인 |

---

## 4. JMX 인증 설정 (보안 강화)

인증 없이 JMX를 활성화하면 같은 네트워크의 누구든 접속할 수 있습니다.  
보안이 중요한 환경에서는 비밀번호 파일을 만들어 인증을 적용합니다.

### 4-1. 비밀번호 파일 생성

```bash
# 컨테이너 내부에서 실행
docker exec -it secportal-backend sh

# JMX 비밀번호 파일 생성
mkdir -p /opt/java/openjdk/conf/management

# 비밀번호 파일 (형식: <사용자명> <비밀번호>)
cat > /opt/java/openjdk/conf/management/jmxremote.password << 'EOF'
monitorRole  <모니터링_비밀번호>
controlRole  <관리_비밀번호>
EOF

chmod 600 /opt/java/openjdk/conf/management/jmxremote.password
```

> **주의**: 컨테이너를 재시작하면 파일이 사라집니다.  
> 영구 적용이 필요하면 `Dockerfile`에서 파일을 COPY하거나 볼륨을 마운트하세요.

### 4-2. 볼륨 마운트로 영구 적용

```yaml
# docker-compose.yml
backend:
  environment:
    JAVA_TOOL_OPTIONS: >-
      -Duser.timezone=Asia/Seoul
      -Dcom.sun.management.jmxremote
      -Dcom.sun.management.jmxremote.port=9999
      -Dcom.sun.management.jmxremote.rmi.port=9999
      -Dcom.sun.management.jmxremote.authenticate=true
      -Dcom.sun.management.jmxremote.ssl=false
      -Djava.rmi.server.hostname=127.0.0.1
  volumes:
    - ./uploads:/app/uploads
    - ./jmx/jmxremote.password:/opt/java/openjdk/conf/management/jmxremote.password:ro
    - ./jmx/jmxremote.access:/opt/java/openjdk/conf/management/jmxremote.access:ro
  ports:
    - "127.0.0.1:9999:9999"
```

```bash
# jmx/ 디렉토리 생성 (프로젝트 루트)
mkdir -p jmx

# jmx/jmxremote.password
echo "monitorRole <비밀번호>" > jmx/jmxremote.password
chmod 400 jmx/jmxremote.password

# jmx/jmxremote.access (역할별 권한)
cat > jmx/jmxremote.access << 'EOF'
monitorRole   readonly
controlRole   readwrite
EOF
```

> **보안 주의**: `jmx/jmxremote.password`를 `.gitignore`에 추가하여 git에 포함되지 않도록 합니다.
>
> ```bash
> echo "jmx/jmxremote.password" >> .gitignore
> ```

### 4-3. 인증 적용 후 접속

```bash
jconsole 127.0.0.1:9999
# → 사용자명: monitorRole
# → 비밀번호: 설정한 비밀번호
```

---

## 5. java-monitor 연동

SecPortal 프로젝트에 포함된 `java-monitor`를 사용하면 JMX 지표를 웹 대시보드로 확인할 수 있습니다.

자세한 설정 방법은 `java-monitoring-setup-guide.md`를 참조하세요.

---

## 6. 모니터링 항목

JMX로 확인할 수 있는 주요 지표입니다.

| 항목 | MBean 경로 | 설명 |
|------|-----------|------|
| 힙 메모리 | `java.lang:type=Memory` | 사용 중·최대 힙 메모리 |
| 스레드 | `java.lang:type=Threading` | 활성·대기·총 스레드 수 |
| CPU | `java.lang:type=OperatingSystem` | CPU 사용률, 프로세서 수 |
| GC | `java.lang:type=GarbageCollector,*` | GC 횟수·소요 시간 |
| HikariCP | `com.zaxxer.hikari:type=Pool (HikariPool-secportal)` | 활성·유휴·대기 커넥션 수 |

> **HikariCP MBean 등록**: `application.yml`의 `hikari.register-mbeans: true` 설정이 필요합니다.  
> 현재 SecPortal은 기본 활성화 상태입니다.

---

## 7. 문제 해결

### 비밀번호 파일 없음 오류

```
Error: Password file not found: /opt/java/openjdk/conf/management/jmxremote.password
```

`authenticate=true`로 설정했지만 비밀번호 파일이 없을 때 발생합니다.

**해결**: 섹션 4의 볼륨 마운트 방법으로 파일을 제공하거나,  
비밀번호 파일이 필요 없는 경우 `authenticate=false`로 설정합니다 (로컬 전용).

### RMI 연결 실패

JMX 접속 시 연결이 되지 않는 경우 `-Djava.rmi.server.hostname` 설정을 확인합니다.

```yaml
# SSH 터널 사용 시 (서버에서 실행, 로컬에서 터널로 접속)
-Djava.rmi.server.hostname=127.0.0.1

# 같은 네트워크에서 직접 접속 시
-Djava.rmi.server.hostname=<서버_내부_IP>
```
