package com.monosun.secportal.rbac;

import java.util.List;

/**
 * RBAC 권한 설정 대상 메뉴 키 목록.
 *
 * 프론트엔드 `src/config/navMenu.js` 의 menuKey 와 1:1로 대응한다.
 * 기본 역할(MANAGER·USER) 권한을 시드할 때 "전체 메뉴"의 기준이 되므로,
 * 메뉴를 추가·삭제하면 navMenu.js·RbacManagementView.vue 의 MENUS 와 함께 이 목록도 갱신할 것.
 */
public final class MenuKeys {

    private MenuKeys() {}

    public static final List<String> ALL = List.of(
            // 대시보드
            "dash_risks", "dash_vulns", "dash_incidents", "dash_isms", "dash_evidence",
            // 정보보호 관리체계
            "policies", "assets", "sbom", "threats", "vulnerabilities",
            "risk_assessment", "risk_treatment", "isms_mapping", "isms",
            // 보안 운영
            "security_events", "incidents", "sec_findings", "monthly_checks",
            "source_scan", "security_reviews", "operation_status",
            // 로그 통합관리
            "log_personal_info", "log_ad", "log_nac", "log_network_link", "log_search",
            // 보안 거버넌스
            "committee", "internal_audit",
            // 교육 및 훈련
            "training", "phishing",
            // 보안 가이드 및 자료
            "sec_docs", "glossary",
            // 개인정보보호
            "privacy_processing", "privacy_files", "privacy_consent", "privacy_provision",
            "privacy_contractors", "privacy_retention", "privacy_disposal", "privacy_dpia",
            "privacy_breach", "privacy_rights", "privacy_safeguard", "privacy_report"
    );

    /** USER 역할의 초기 권한 — 기존 하드코딩 기본값(보안교육·보안 가이드 읽기)을 그대로 옮긴 것 */
    public static final List<String> DEFAULT_USER_READ = List.of("training", "sec_docs");
}
