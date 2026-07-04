# Java 애플리케이션 성능 실시간 모니터링 툴 설치 매뉴얼

## 목차
1. [모니터링 툴 개요 및 선택 가이드](#1-모니터링-툴-개요-및-선택-가이드)
2. [VisualVM](#2-visualvm)
3. [Java Mission Control (JMC)](#3-java-mission-control-jmc)
4. [Arthas (아르타스)](#4-arthas-아르타스)
5. [Prometheus + Grafana + Micrometer](#5-prometheus--grafana--micrometer)
6. [Pinpoint APM](#6-pinpoint-apm)
7. [Apache SkyWalking](#7-apache-skywalking)
8. [IntelliJ Profiler (Community 내장)](#8-intellij-profiler-community-내장)
9. [모니터링 항목 체크리스트](#9-모니터링-항목-체크리스트)

---

## 1. 모니터링 툴 개요 및 선택 가이드

### 도구별 비교표

| 툴 | 유형 | 비용 | 특징 | 적합 환경 |
|----|------|------|------|-----------|
| VisualVM | 로컬 프로파일러 | 무료 | JVM 메모리·스레드 시각화 | 개발 PC |
| JMC (Java Mission Control) | 로컬 프로파일러 | 무료 | JFR 기반 저오버헤드 분석 | 개발/스테이징 |
| Arthas | CLI 진단 | 무료 | 런타임 트레이싱, 무중단 | 운영 서버 |
| Prometheus + Grafana | 메트릭 수집/시각화 | 무료 | 장기 지표 추세 분석 | 운영 서버 |
| Pinpoint APM | 분산 추적 | 무료 | MSA 호출 추적, 분산 시스템 | 운영 MSA |
| SkyWalking | 분산 추적 | 무료 | CNCF 프로젝트, 경량 에이전트 | 운영 MSA |

### 추천 조합
- **개발 단계**: VisualVM + IntelliJ Profiler
- **운영 단계 (단일 서버)**: Arthas + Prometheus + Grafana
- **운영 단계 (MSA)**: Pinpoint 또는 SkyWalking + Prometheus + Grafana

---

## 2. VisualVM

> JVM 프로세스의 CPU, 메모리, 스레드, 힙 덤프를 GUI로 실시간 확인

### 2.1 설치

**방법 1: 독립 설치 (권장)**
1. https://visualvm.github.io 에서 최신 버전 다운로드
2. 압축 해제 후 `bin/visualvm` (macOS/Linux) 또는 `bin\visualvm.exe` (Windows) 실행

**방법 2: JDK 내장 (JDK 8 이하)**
```bash
# JDK 설치 경로에 포함
$JAVA_HOME/bin/jvisualvm
```

### 2.2 JVM 옵션 설정 (원격 모니터링)

모니터링 대상 애플리케이션 실행 시 JVM 인자 추가:
```bash
java \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9090 \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -jar your-application.jar
```

### 2.3 원격 호스트 연결
1. VisualVM 좌측 `Remote` 우클릭 → `Add Remote Host`
2. 호스트 IP 입력
3. `JMX Connection` 추가 → 포트 입력 (위에서 설정한 9090)

### 2.4 주요 기능
| 탭 | 내용 |
|----|------|
| `Monitor` | CPU, 힙 메모리, 스레드 수, GC 실시간 그래프 |
| `Threads` | 스레드 상태 (RUNNING / WAITING / BLOCKED) |
| `Sampler` | CPU·메모리 샘플링 (저오버헤드) |
| `Profiler` | 정밀 CPU·메모리 프로파일링 |
| `Heap Dump` | 힙 스냅샷 분석 |

### 2.5 유용한 플러그인 설치
`Tools` → `Plugins` → `Available Plugins`
- `VisualVM-MBeans` : MBean 속성 모니터링
- `VisualVM-Tracer` : 시계열 트레이서

---

## 3. Java Mission Control (JMC)

> Java Flight Recorder(JFR) 기반 저오버헤드 프로파일링 — 운영 환경에서도 사용 가능

### 3.1 설치

**JDK 17+ 내장 (JFR)**
```bash
# JMC 별도 다운로드: https://jdk.java.net/jmc/
# 또는 Adoptium JMC: https://adoptium.net/jmc
```

압축 해제 후 실행:
```bash
# Windows
jmc.exe

# macOS / Linux
./jmc
```

### 3.2 JFR 녹화 시작

**방법 1: JVM 기동 시 자동 녹화**
```bash
java \
  -XX:+FlightRecorder \
  -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
  -jar your-application.jar
```

**방법 2: 실행 중인 프로세스에 연결**
```bash
# PID 확인
jps -l

# 30초 녹화
jcmd <PID> JFR.start duration=30s filename=recording.jfr

# 녹화 중지
jcmd <PID> JFR.stop name=1
```

**방법 3: JMC GUI에서 연결**
1. JMC 실행 → 좌측 JVM 목록에서 대상 프로세스 선택
2. `Start Flight Recording` 클릭
3. 녹화 시간 및 프로파일 설정 후 시작

### 3.3 주요 분석 탭
| 탭 | 내용 |
|----|------|
| `Automated Analysis` | 성능 문제 자동 진단 결과 |
| `Memory` | GC 이벤트, 힙 사용량 추세 |
| `Code` | 핫메서드, 예외 발생 위치 |
| `Threads` | 스레드 덤프, 락 경합 |
| `I/O` | 파일 / 소켓 I/O 병목 |

---

## 4. Arthas (아르타스)

> Alibaba 오픈소스 — 재배포 없이 운영 중인 JVM을 실시간 진단

### 4.1 설치 및 실행

```bash
# 다운로드
curl -O https://arthas.aliyun.com/arthas-boot.jar

# 실행 (Java 8+ 필요)
java -jar arthas-boot.jar
```

실행 시 연결할 Java 프로세스 목록이 표시됨 → 번호 입력하여 연결

### 4.2 주요 명령어

#### 대시보드 (실시간 현황)
```bash
dashboard
```
CPU, 메모리, GC, 스레드 현황을 1초마다 갱신

#### 스레드 분석
```bash
# 전체 스레드 목록 + CPU 사용률
thread

# CPU 가장 많이 쓰는 스레드 TOP 3
thread -n 3

# BLOCKED 상태 스레드 찾기
thread -b

# 특정 스레드 스택 추적
thread <thread-id>
```

#### 메서드 실행 추적
```bash
# 특정 메서드 실행 시간 측정
trace com.example.service.UserService getUserById

# 호출 스택 + 반환값 확인
watch com.example.service.UserService getUserById "{params, returnObj}" -x 2

# 메서드 실행 기록 재실행
tt -t com.example.controller.UserController list
tt -i 1000 -p    # 1000번 인덱스 재실행
```

#### 클래스 / 메서드 정보
```bash
# 클래스 정보 조회
sc -d com.example.service.UserService

# 메서드 정보 조회
sm com.example.service.UserService getUserById

# 현재 로드된 클래스의 소스 역컴파일
jad com.example.service.UserService
```

#### JVM 상태
```bash
# JVM 기본 정보
jvm

# 힙 메모리 상태
memory

# GC 수행
ognl "@java.lang.Runtime@getRuntime().gc()"
```

#### 종료
```bash
exit        # Arthas에서 나가기 (JVM은 유지)
stop        # Arthas 완전 종료
```

---

## 5. Prometheus + Grafana + Micrometer

> 장기적 메트릭 수집 및 대시보드 시각화 — Spring Boot 프로젝트에 최적

### 5.1 Spring Boot 애플리케이션 설정

#### `pom.xml` 의존성 추가
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

#### `application.yml` 설정
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 메트릭 엔드포인트 확인
```
http://localhost:8080/actuator/prometheus
```

### 5.2 Prometheus 설치

**Docker로 실행 (권장)**
```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

#### `prometheus.yml` 설정
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']   # Windows/macOS
        # targets: ['172.17.0.1:8080']           # Linux
```

**Prometheus UI 접속**: http://localhost:9090

### 5.3 Grafana 설치

**Docker로 실행**
```bash
docker run -d \
  --name grafana \
  -p 3000:3000 \
  grafana/grafana
```

**접속**: http://localhost:3000
- 초기 계정: `admin` / `admin`

#### Prometheus 데이터소스 연결
1. 좌측 메뉴 `Connections` → `Data Sources` → `Add data source`
2. `Prometheus` 선택
3. URL 입력: `http://host.docker.internal:9090` (Windows/macOS) 또는 `http://prometheus:9090`
4. `Save & Test` 클릭

#### 대시보드 임포트
1. 좌측 메뉴 `Dashboards` → `Import`
2. 아래 ID 입력 후 `Load`:
   - `4701`: JVM (Micrometer) — 메모리, GC, 스레드
   - `11378`: Spring Boot 2.1 Statistics
   - `12900`: Spring Boot Actuator 전용

### 5.4 Docker Compose로 한 번에 실행

`docker-compose.yml`:
```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    extra_hosts:
      - "host.docker.internal:host-gateway"

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus

volumes:
  grafana-data:
```

실행:
```bash
docker-compose up -d
```

---

## 6. Pinpoint APM

> 분산 추적 기반 APM — MSA 환경의 서비스 간 호출 흐름 시각화

### 6.1 구성 요소
| 컴포넌트 | 역할 |
|----------|------|
| Pinpoint Agent | 애플리케이션에 붙는 Java 에이전트 |
| Pinpoint Collector | 에이전트 데이터 수집 서버 |
| Pinpoint Web | 시각화 웹 UI |
| HBase | 분산 추적 데이터 저장소 |

### 6.2 Docker Compose로 설치 (권장)

```bash
# 공식 Docker Compose 저장소 클론
git clone https://github.com/pinpoint-apm/pinpoint-docker.git
cd pinpoint-docker

# 실행 (첫 실행은 이미지 다운로드로 5~10분 소요)
docker-compose up -d
```

**접속**: http://localhost:8080

### 6.3 Pinpoint Agent 적용

```bash
# 에이전트 다운로드 (버전 확인 후 최신 사용)
wget https://github.com/pinpoint-apm/pinpoint/releases/download/v2.5.3/pinpoint-agent-2.5.3.tar.gz
tar xzf pinpoint-agent-2.5.3.tar.gz
```

#### `pinpoint-root.config` 수정
```properties
# Collector 서버 주소
profiler.transport.grpc.collector.ip=127.0.0.1
```

#### 애플리케이션 실행 시 에이전트 연결
```bash
java \
  -javaagent:/path/to/pinpoint-agent/pinpoint-bootstrap.jar \
  -Dpinpoint.agentId=my-agent-001 \
  -Dpinpoint.applicationName=MY-APP \
  -jar your-application.jar
```

### 6.4 주요 기능
- **서버 맵**: 서비스 간 호출 관계 시각화
- **분산 트랜잭션 추적**: 요청 → 서비스 → DB 전체 경로
- **응답 시간 히스토그램**: 지연 구간 분포 확인
- **JVM 메트릭**: 힙, GC, 스레드 통합 뷰

---

## 7. Apache SkyWalking

> CNCF 인큐베이팅 프로젝트 — 경량 에이전트 기반 분산 추적 (Pinpoint 대안)

### 7.1 설치

```bash
# 다운로드: https://skywalking.apache.org/downloads/
wget https://archive.apache.org/dist/skywalking/9.7.0/apache-skywalking-apm-9.7.0.tar.gz
tar xzf apache-skywalking-apm-9.7.0.tar.gz
cd apache-skywalking-apm-bin
```

### 7.2 백엔드(OAP) 및 UI 실행

```bash
# OAP 서버 시작
bin/oapService.sh      # Linux/macOS
bin\oapService.bat     # Windows

# UI 서버 시작
bin/webappService.sh   # Linux/macOS
bin\webappService.bat  # Windows
```

**UI 접속**: http://localhost:8080

### 7.3 Java 에이전트 적용

```bash
java \
  -javaagent:/path/to/skywalking-agent/skywalking-agent.jar \
  -Dskywalking.agent.service_name=my-service \
  -Dskywalking.collector.backend_service=localhost:11800 \
  -jar your-application.jar
```

### 7.4 Docker Compose로 실행

```yaml
version: '3.8'

services:
  oap:
    image: apache/skywalking-oap-server:9.7.0
    container_name: skywalking-oap
    ports:
      - "11800:11800"
      - "12800:12800"
    environment:
      SW_STORAGE: h2

  ui:
    image: apache/skywalking-ui:9.7.0
    container_name: skywalking-ui
    ports:
      - "8080:8080"
    environment:
      SW_OAP_ADDRESS: http://oap:12800
    depends_on:
      - oap
```

---

## 8. IntelliJ Profiler (Community 내장)

> IntelliJ IDEA 2023.1+ 내장 프로파일러 — 별도 설치 불필요

### 8.1 실행 방법
1. `Run` → `Run '...' with Profiler`
2. 또는 `Run/Debug Configuration` 편집 → `Async Profiler` 또는 `Java Flight Recorder` 선택

### 8.2 Async Profiler 설정
- `Run` → `Edit Configurations` → `Modify options` → `Run with async profiler`
- 프로파일 유형: `CPU`, `Allocation`, `Wall Clock`

### 8.3 결과 분석
- 플레임 그래프(Flame Graph): 메서드별 CPU 점유율 시각화
- Call Tree: 호출 트리 계층 분석
- `File` → `Open Profiler Snapshot`으로 `.jfr` 파일 열기 가능

---

## 9. 모니터링 항목 체크리스트

### JVM 핵심 메트릭

| 분류 | 항목 | 경고 기준 |
|------|------|-----------|
| **메모리** | Heap 사용률 | 80% 이상 지속 시 위험 |
| **메모리** | Non-Heap (Metaspace) 사용률 | 급증 시 클래스 로드 문제 의심 |
| **GC** | GC 빈도 | Full GC 빈번 시 메모리 누수 의심 |
| **GC** | GC 소요 시간 | Full GC 1초 이상 시 튜닝 필요 |
| **스레드** | 활성 스레드 수 | 급증 시 스레드 풀 설정 점검 |
| **스레드** | BLOCKED 스레드 | 1개 이상 지속 시 데드락 의심 |
| **CPU** | CPU 사용률 | 70% 이상 지속 시 프로파일링 필요 |

### 애플리케이션 메트릭 (Spring Boot)

| 항목 | Actuator 엔드포인트 |
|------|---------------------|
| HTTP 요청 처리 시간 | `http.server.requests` |
| DB 커넥션 풀 상태 | `hikaricp.connections.*` |
| 캐시 히트율 | `cache.gets` |
| 큐 사이즈 | `executor.queue.remaining` |

### JVM 튜닝 파라미터 예시 (운영 참고)

```bash
java \
  -Xms2g -Xmx2g \                          # 힙 고정 (GC 안정화)
  -XX:+UseG1GC \                            # G1 GC 사용 (Java 9+)
  -XX:MaxGCPauseMillis=200 \               # 최대 GC 중단 목표
  -XX:+HeapDumpOnOutOfMemoryError \        # OOM 시 힙 덤프 자동 저장
  -XX:HeapDumpPath=/logs/heapdump.hprof \
  -XX:+FlightRecorder \                    # JFR 활성화
  -Xlog:gc*:file=/logs/gc.log:time:filecount=5,filesize=20m \  # GC 로그
  -jar your-application.jar
```

---

## 빠른 시작 가이드 (개발 환경)

```bash
# 1. VisualVM 설치 및 실행
# https://visualvm.github.io 에서 다운로드 후 실행

# 2. 애플리케이션을 JMX 옵션과 함께 실행
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9090 \
     -Dcom.sun.management.jmxremote.ssl=false \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -jar your-application.jar

# 3. Arthas로 런타임 진단
java -jar arthas-boot.jar

# 4. Prometheus + Grafana 모니터링 스택 실행
docker-compose up -d
```

---

*작성일: 2026-05-16 | Java 17+ / Spring Boot 3.x 기준*
