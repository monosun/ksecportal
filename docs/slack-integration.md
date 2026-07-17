# Slack 연동 메뉴얼

SecPortal의 알림을 Slack 채널로 전송하는 방법을 단계별로 설명합니다.  
이 메뉴얼을 따라 구현하면 취약점 기한 초과, 신규 인시던트, 정책 열람 요청 등을 Slack으로 받을 수 있습니다.

## 목차

1. [Slack App 생성 및 Webhook URL 발급](#1-slack-app-생성-및-webhook-url-발급)
2. [알림 시나리오 정의](#2-알림-시나리오-정의)
3. [백엔드 구현 — SlackService 추가](#3-백엔드-구현--slackservice-추가)
4. [취약점 기한 초과 알림 연동](#4-취약점-기한-초과-알림-연동)
5. [신규 인시던트 등록 알림 연동](#5-신규-인시던트-등록-알림-연동)
6. [환경변수 및 설정 파일 수정](#6-환경변수-및-설정-파일-수정)
7. [테스트 방법](#7-테스트-방법)
8. [메시지 포맷 커스터마이징](#8-메시지-포맷-커스터마이징)
9. [트러블슈팅](#9-트러블슈팅)

---

## 1. Slack App 생성 및 Webhook URL 발급

### 1-1. Slack App 생성

1. [Slack API 대시보드](https://api.slack.com/apps) 접속 (Slack 계정 로그인 필요)
2. **Create New App** 클릭
3. **From scratch** 선택
4. App Name: `SecPortal` 입력
5. 알림을 받을 워크스페이스 선택 → **Create App**

### 1-2. Incoming Webhooks 활성화

1. 좌측 메뉴 → **Incoming Webhooks** 클릭
2. **Activate Incoming Webhooks** 토글 → **On**
3. 페이지 하단 → **Add New Webhook to Workspace** 클릭
4. 알림을 받을 채널 선택 (예: `#secportal-alerts`)
5. **허용** 클릭

발급된 Webhook URL 형식:
```
https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX
```

이 URL을 안전하게 복사해 둡니다. 나중에 환경변수로 등록합니다.

### 1-3. 채널 구성 권장안

| 채널 | 용도 |
|------|------|
| `#secportal-alerts` | 취약점 기한 초과, 심각도 CRITICAL 인시던트 |
| `#secportal-incidents` | 모든 신규 인시던트 등록 |
| `#secportal-info` | 정책 게시, 교육 마감 안내 등 |

채널별로 별도 Webhook URL을 발급하면 알림을 분리할 수 있습니다.

---

## 2. 알림 시나리오 정의

SecPortal에서 Slack으로 보낼 이벤트를 정의합니다.

| 이벤트 | 트리거 시점 | 권장 채널 | 우선순위 |
|--------|-----------|----------|---------|
| 취약점 기한 초과 | 매일 오전 9시 스케줄러 | `#secportal-alerts` | 높음 |
| CRITICAL 취약점 신규 등록 | 취약점 생성 API 호출 시 | `#secportal-alerts` | 높음 |
| 신규 인시던트 등록 | 인시던트 생성 API 호출 시 | `#secportal-incidents` | 중간 |
| CRITICAL 인시던트 등록 | 인시던트 생성 API 호출 시 | `#secportal-alerts` | 높음 |
| 정책 게시 (PUBLISHED) | 정책 상태 변경 시 | `#secportal-info` | 낮음 |

---

## 3. 백엔드 구현 — SlackService 추가

### 3-1. build.gradle에 의존성 추가

```groovy
// build.gradle 의 dependencies 블록에 추가
dependencies {
    // ... 기존 의존성 ...

    // HTTP 클라이언트 (Spring Boot에 포함)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
}
```

> Spring Boot 3.5에는 `WebClient`를 쓰기 위해 `spring-boot-starter-webflux`가 필요합니다.  
> 이미 포함된 경우 추가하지 않아도 됩니다.

---

### 3-2. SlackService 클래스 생성

파일 위치: `backend/src/main/java/com/monosun/secportal/notification/service/SlackService.java`

```java
package com.monosun.secportal.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class SlackService {

    private final WebClient webClient;

    @Value("${slack.webhook.alerts:}")
    private String alertsWebhookUrl;

    @Value("${slack.webhook.incidents:}")
    private String incidentsWebhookUrl;

    @Value("${slack.webhook.info:}")
    private String infoWebhookUrl;

    @Value("${slack.enabled:false}")
    private boolean enabled;

    public SlackService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 알림 채널로 메시지 전송
     */
    @Async
    public void sendAlert(String message) {
        send(alertsWebhookUrl, message);
    }

    /**
     * 인시던트 채널로 메시지 전송
     */
    @Async
    public void sendIncidentNotice(String message) {
        send(incidentsWebhookUrl, message);
    }

    /**
     * 정보 채널로 메시지 전송
     */
    @Async
    public void sendInfo(String message) {
        send(infoWebhookUrl, message);
    }

    private void send(String webhookUrl, String message) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Slack 알림 비활성화 또는 Webhook URL 미설정");
            return;
        }

        try {
            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("text", message))
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            res -> log.debug("Slack 전송 성공: {}", res),
                            err -> log.warn("Slack 전송 실패: {}", err.getMessage())
                    );
        } catch (Exception e) {
            log.warn("Slack 전송 중 오류 발생: {}", e.getMessage());
        }
    }
}
```

---

### 3-3. Block Kit 형식의 리치 메시지 (선택)

단순 텍스트 대신 Slack Block Kit을 사용하면 더 보기 좋은 알림을 만들 수 있습니다.

`SlackMessageBuilder` 유틸리티 클래스:

파일 위치: `backend/src/main/java/com/monosun/secportal/notification/service/SlackMessageBuilder.java`

```java
package com.monosun.secportal.notification.service;

import com.monosun.secportal.incident.entity.Incident;
import com.monosun.secportal.vulnerability.entity.Vulnerability;

import java.time.format.DateTimeFormatter;

public class SlackMessageBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 취약점 기한 초과 알림 메시지
     */
    public static String overdueVulnerability(Vulnerability v) {
        String severityEmoji = switch (v.getSeverity()) {
            case CRITICAL -> ":red_circle:";
            case HIGH     -> ":orange_circle:";
            case MEDIUM   -> ":yellow_circle:";
            default       -> ":white_circle:";
        };

        return String.format("""
                %s *[취약점 기한 초과]* %s
                > *제목:* %s
                > *심각도:* %s
                > *처리 기한:* %s
                > *담당자:* %s
                > *상태:* %s
                SecPortal에서 즉시 처리해 주세요.""",
                severityEmoji,
                v.getCveId() != null ? "(" + v.getCveId() + ")" : "",
                v.getTitle(),
                v.getSeverity().name(),
                v.getDueDate() != null ? v.getDueDate().format(DATE_FMT) : "미설정",
                v.getAssignee() != null ? v.getAssignee().getName() : "미지정",
                v.getStatus().name()
        );
    }

    /**
     * 신규 인시던트 알림 메시지
     */
    public static String newIncident(Incident incident) {
        String severityEmoji = switch (incident.getSeverity()) {
            case CRITICAL -> ":rotating_light:";
            case HIGH     -> ":red_circle:";
            case MEDIUM   -> ":orange_circle:";
            default       -> ":yellow_circle:";
        };

        return String.format("""
                %s *[신규 인시던트 등록]* %s
                > *제목:* %s
                > *유형:* %s
                > *심각도:* %s
                > *영향 시스템:* %s
                > *담당자:* %s
                SecPortal에서 즉시 확인해 주세요.""",
                severityEmoji,
                incident.getSeverity() == Incident.Severity.CRITICAL ? "<!here>" : "",
                incident.getTitle(),
                incident.getType().name(),
                incident.getSeverity().name(),
                incident.getAffectedSystems() != null ? incident.getAffectedSystems() : "미입력",
                incident.getAssignee() != null ? incident.getAssignee().getName() : "미지정"
        );
    }

    /**
     * CRITICAL 취약점 신규 등록 알림
     */
    public static String criticalVulnerability(Vulnerability v) {
        return String.format("""
                :rotating_light: *[CRITICAL 취약점 등록]* <!here>
                > *제목:* %s
                > *CVE:* %s
                > *CVSS 점수:* %s
                > *대상 자산:* %s
                > *담당자:* %s
                즉시 검토가 필요합니다.""",
                v.getTitle(),
                v.getCveId() != null ? v.getCveId() : "N/A",
                v.getCvssScore() != null ? v.getCvssScore().toPlainString() : "N/A",
                v.getAssetName() != null ? v.getAssetName() : "미입력",
                v.getAssignee() != null ? v.getAssignee().getName() : "미지정"
        );
    }
}
```

---

## 4. 취약점 기한 초과 알림 연동

기존 `NotificationScheduler.java`에 Slack 발송을 추가합니다.

파일 위치: `backend/src/main/java/com/monosun/secportal/notification/service/NotificationScheduler.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final VulnerabilityRepository vulnerabilityRepository;
    private final EmailService emailService;
    private final SlackService slackService;  // 추가

    @Scheduled(cron = "${notification.overdue-check-cron}")
    public void notifyOverdueVulnerabilities() {
        LocalDate today = LocalDate.now();
        List<Vulnerability> overdue = vulnerabilityRepository.findOverdue(today);
        log.info("Overdue vulnerability check: {} items found", overdue.size());

        for (Vulnerability v : overdue) {
            // 이메일 알림 (기존)
            if (v.getAssignee() != null && v.getAssignee().getEmail() != null) {
                String subject = "[SecPortal] 취약점 처리 기한 초과: " + v.getTitle();
                emailService.send(v.getAssignee().getEmail(), subject, buildOverdueEmailHtml(v));
            }

            // Slack 알림 추가
            slackService.sendAlert(SlackMessageBuilder.overdueVulnerability(v));
        }

        // 전체 요약 메시지 (기한 초과 건수 > 0인 경우)
        if (!overdue.isEmpty()) {
            slackService.sendAlert(String.format(
                ":warning: 오늘 기준 기한 초과 취약점이 *%d건* 있습니다.", overdue.size()
            ));
        }
    }

    // buildOverdueEmailHtml() 메서드는 기존과 동일
}
```

---

## 5. 신규 인시던트 등록 알림 연동

`IncidentService.java`에 Slack 발송을 추가합니다.

파일 위치: `backend/src/main/java/com/monosun/secportal/incident/service/IncidentService.java`

```java
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final SlackService slackService;  // 추가

    @Transactional
    public IncidentDto.Response create(IncidentDto.CreateRequest request, Long reporterId) {
        // ... 기존 생성 로직 ...
        Incident saved = incidentRepository.save(incident);
        auditLogService.log("INCIDENT_CREATED", "INCIDENT", saved.getId(), saved.getTitle());

        // Slack 알림 추가
        slackService.sendIncidentNotice(SlackMessageBuilder.newIncident(saved));
        if (saved.getSeverity() == Incident.Severity.CRITICAL) {
            slackService.sendAlert(SlackMessageBuilder.newIncident(saved));  // 알림 채널에도 발송
        }

        return IncidentDto.Response.from(saved);
    }
}
```

---

### CRITICAL 취약점 신규 등록 알림 연동

`VulnerabilityService.java`에 Slack 발송을 추가합니다.

```java
@Service
@RequiredArgsConstructor
public class VulnerabilityService {

    private final SlackService slackService;  // 추가

    @Transactional
    public VulnerabilityDto.Response create(VulnerabilityDto.CreateRequest request, Long reporterId) {
        // ... 기존 생성 로직 ...
        Vulnerability saved = vulnRepository.save(vuln);
        auditLogService.log("VULN_CREATED", "VULNERABILITY", saved.getId(), saved.getTitle());

        // CRITICAL 취약점만 즉시 Slack 알림
        if (saved.getSeverity() == Vulnerability.Severity.CRITICAL) {
            slackService.sendAlert(SlackMessageBuilder.criticalVulnerability(saved));
        }

        return VulnerabilityDto.Response.from(saved);
    }
}
```

---

## 6. 환경변수 및 설정 파일 수정

### 6-1. application.yml 수정

`backend/src/main/resources/application.yml`에 Slack 설정 블록을 추가합니다:

```yaml
# 기존 내용 아래에 추가
slack:
  enabled: ${SLACK_ENABLED:false}
  webhook:
    alerts: ${SLACK_WEBHOOK_ALERTS:}
    incidents: ${SLACK_WEBHOOK_INCIDENTS:}
    info: ${SLACK_WEBHOOK_INFO:}
```

### 6-2. .env 파일 수정

`.env` 파일에 Slack 설정을 추가합니다:

```bash
# Slack 연동 설정
SLACK_ENABLED=true

# Slack Webhook URL (Slack 앱 대시보드에서 발급)
SLACK_WEBHOOK_ALERTS=https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXX
SLACK_WEBHOOK_INCIDENTS=https://hooks.slack.com/services/T00000000/B11111111/YYYYYYYYYYYYYYYY
SLACK_WEBHOOK_INFO=https://hooks.slack.com/services/T00000000/B22222222/ZZZZZZZZZZZZZZZZ
```

> 채널을 구분하지 않을 경우 동일한 URL을 모두 입력하거나,  
> `SLACK_WEBHOOK_ALERTS`만 설정하고 나머지는 비워두면 됩니다.

### 6-3. .env.example 업데이트

```bash
# .env.example에도 추가 (실제 값 없이 키만)
SLACK_ENABLED=false
SLACK_WEBHOOK_ALERTS=
SLACK_WEBHOOK_INCIDENTS=
SLACK_WEBHOOK_INFO=
```

---

## 7. 테스트 방법

### 7-1. 직접 Webhook 테스트 (curl)

Webhook URL이 정상인지 먼저 확인합니다:

```bash
curl -X POST https://hooks.slack.com/services/T00000000/B00000000/XXXXXX \
  -H 'Content-Type: application/json' \
  -d '{"text":"SecPortal Slack 연동 테스트 메시지입니다."}'

# 정상 응답: ok
```

### 7-2. 스프링 테스트 코드 작성

파일 위치: `backend/src/test/java/com/monosun/secportal/notification/SlackServiceTest.java`

```java
@SpringBootTest
class SlackServiceTest {

    @Autowired
    private SlackService slackService;

    @Test
    void testSendAlert() {
        // SLACK_ENABLED=true, SLACK_WEBHOOK_ALERTS 설정 필요
        slackService.sendAlert("*[테스트]* SlackService 정상 동작 확인");
        // Slack 채널에서 메시지 수신 여부 수동 확인
    }
}
```

### 7-3. 스케줄러 즉시 실행으로 테스트

개발 환경에서 스케줄러를 즉시 실행하려면 cron을 임시로 변경합니다:

```yaml
# application.yml (개발 시 임시 — 매 분 실행)
notification:
  overdue-check-cron: "0 * * * * *"
```

또는 IntelliJ의 HTTP Client를 사용해 API를 직접 호출하거나,  
테스트용 엔드포인트를 임시로 추가하는 방법도 있습니다.

### 7-4. 취약점 기한 초과 데이터 생성

DB에서 테스트 데이터를 직접 삽입합니다:

```sql
-- 기한이 어제인 OPEN 상태 취약점 (테스트용)
INSERT INTO vulnerabilities (title, description, severity, status, reporter_id, due_date, created_at, updated_at)
VALUES (
    '[TEST] Slack 연동 테스트 취약점',
    '테스트용 취약점입니다.',
    'HIGH',
    'OPEN',
    1,
    CURDATE() - INTERVAL 1 DAY,
    NOW(),
    NOW()
);
```

---

## 8. 메시지 포맷 커스터마이징

### 8-1. Slack Block Kit 사용 (고급)

`SlackService.send()` 메서드를 수정하여 구조화된 메시지를 보낼 수 있습니다:

```java
private void sendBlocks(String webhookUrl, Object blocks) {
    webClient.post()
            .uri(webhookUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(blocks)
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(...);
}
```

Block Kit 메시지 예시 (JSON):

```json
{
  "blocks": [
    {
      "type": "header",
      "text": {
        "type": "plain_text",
        "text": "🔴 CRITICAL 취약점 기한 초과"
      }
    },
    {
      "type": "section",
      "fields": [
        { "type": "mrkdwn", "text": "*제목:*\nLog4Shell 취약점" },
        { "type": "mrkdwn", "text": "*심각도:*\nCRITICAL" },
        { "type": "mrkdwn", "text": "*기한:*\n2025-05-10" },
        { "type": "mrkdwn", "text": "*담당자:*\n홍길동" }
      ]
    },
    {
      "type": "actions",
      "elements": [
        {
          "type": "button",
          "text": { "type": "plain_text", "text": "SecPortal에서 확인" },
          "url": "https://secportal.yourdomain.com/vulnerabilities/1",
          "style": "danger"
        }
      ]
    }
  ]
}
```

[Block Kit Builder](https://app.slack.com/block-kit-builder)에서 미리보기 및 편집이 가능합니다.

### 8-2. 멘션 추가

```java
// 채널 전체 알림
"<!here> 취약점 기한 초과 알림"

// 특정 사람 멘션 (Slack User ID 필요)
"<@U00000000> 담당 취약점 기한이 초과되었습니다."
```

User ID는 Slack 프로필 → **View Full Profile** → 점 세 개 메뉴 → **Copy member ID**에서 확인합니다.

---

## 9. 트러블슈팅

### Slack 메시지가 오지 않는 경우

**1. SLACK_ENABLED 확인**
```bash
# 컨테이너 환경변수 확인
docker exec secportal-backend env | grep SLACK
# SLACK_ENABLED=true 가 있는지 확인
```

**2. Webhook URL 형식 확인**

Webhook URL은 반드시 `https://hooks.slack.com/services/`로 시작해야 합니다.

**3. 백엔드 로그 확인**
```bash
docker compose logs -f backend | grep -i slack
```

정상 발송 시:
```
DEBUG - Slack 전송 성공: ok
```

실패 시:
```
WARN - Slack 전송 실패: 404 Not Found
```

**4. Webhook URL 만료 확인**

Webhook URL은 유효하지 않은 경우 `https://api.slack.com/apps`에서 재발급합니다.

---

### `WebClient` 빈이 없다는 오류 발생

```
Parameter 0 of constructor in SlackService required a bean of type 'WebClient.Builder'
```

`build.gradle`에 `spring-boot-starter-webflux` 의존성이 추가되었는지 확인합니다:

```bash
./gradlew dependencies | grep webflux
```

---

### 비동기 발송 후 앱이 종료되기 전에 메시지가 전송되지 않음

개발 환경에서 테스트 시 앱이 즉시 종료되면 `@Async` 스레드 풀이 종료됩니다.  
`SlackService`에서 `subscribe()` 대신 `block()`을 사용하거나, 테스트에서 `Thread.sleep()`으로 잠시 대기합니다.

---

### Slack 채널에 앱이 초대되지 않은 경우

`/invite @SecPortal` 명령을 채널에서 실행하거나,  
채널 설정 → **Integrations** → **Add apps**에서 앱을 추가합니다.

---

## 부록: 향후 확장 방안

| 기능 | 구현 방법 |
|------|----------|
| 정책 게시 알림 | `PolicyService.update()` 내 `status == PUBLISHED` 조건 분기에서 `slackService.sendInfo()` 호출 |
| 교육 마감 D-7 알림 | `NotificationScheduler`에 새 `@Scheduled` 메서드 추가 |
| 인시던트 상태 변경 알림 | `IncidentService.update()` 내 상태 전환 로직에 알림 추가 |
| Slack Slash Command | Slack App에 Slash Command 등록 + 백엔드 `/api/slack/command` 엔드포인트 구현 |
| Slack OAuth 로그인 | Slack App의 OAuth & Permissions 설정 + Spring Security OAuth2 클라이언트 추가 |
