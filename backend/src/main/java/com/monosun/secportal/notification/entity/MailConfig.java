package com.monosun.secportal.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 발송 메일서버(SMTP) 설정 (단일 행).
 * 비밀번호는 ADMIN 전용 API에서만 다루며 조회 시 마스킹된다 — 공개 app_settings에 저장하지 않는다.
 * 미설정 시 application.yml(spring.mail.*)의 기본 설정으로 폴백한다.
 */
@Entity
@Table(name = "mail_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SMTP 서버 호스트 (예: smtp.gmail.com) */
    @Column(length = 200)
    private String host;

    /** SMTP 포트 (예: 587) */
    private Integer port;

    /** SMTP 인증 계정 */
    @Column(length = 200)
    private String username;

    /** SMTP 인증 비밀번호 (마스킹 응답, 평문 저장) */
    @Column(length = 500)
    private String password;

    /** 발신자 주소 (From) */
    @Column(length = 200)
    private String fromAddress;

    /** 발신자 표시 이름 (선택) */
    @Column(length = 200)
    private String fromName;

    /** SMTP 인증 사용 여부 */
    @Column(nullable = false)
    @Builder.Default
    private boolean useAuth = true;

    /** STARTTLS 사용 여부 */
    @Column(nullable = false)
    @Builder.Default
    private boolean useStartTls = true;

    /** 이 설정으로 메일 발송 활성화 여부 (false 시 application.yml 기본 설정 사용) */
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
