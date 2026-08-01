package com.monosun.secportal.phishing;

import java.util.List;

/**
 * 모의 악성메일 훈련(피싱 시뮬레이션) 기본 템플릿 예제 10종.
 *
 * 임직원 보안인식 제고를 위한 <b>사내 훈련용</b> 예시 메일이다. 실제 발송은 관리자가
 * 캠페인으로 지정한 사내 대상자에게만 이루어지며, 본문에는 클릭·열람 추적용 변수
 * ({@code {TARGET_NAME}}, {@code {TARGET_EMAIL}}, {@code {CLICK_URL}}, {@code {OPEN_URL}})가
 * 포함된다.
 *
 * 최초 기동 시 {@code phishing_templates} 테이블이 비어 있을 때만 시드된다
 * ({@link com.monosun.secportal.phishing.service.PhishingTemplateInitializer}).
 * 카테고리는 프론트의 TEMPLATE_CATEGORIES(IT · HR · DELIVERY · FINANCE · SECURITY ·
 * MARKETING · 기타)와 맞춘다.
 */
public final class PhishingTemplateDefaults {

    private PhishingTemplateDefaults() {}

    public record Row(String name, String category, String difficulty, String subject,
                      String senderName, String senderEmail, String bodyHtml, String description) {}

    private static Row d(String name, String category, String difficulty, String subject,
                         String senderName, String senderEmail, String bodyHtml, String description) {
        return new Row(name, category, difficulty, subject, senderName, senderEmail, bodyHtml, description);
    }

    /** 파란 헤더 카드 레이아웃 공통 래퍼 */
    private static String card(String accent, String header, String inner) {
        return """
                <div style="font-family:'Malgun Gothic',Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px">
                <div style="background:%s;padding:15px 20px;border-radius:8px 8px 0 0">
                  <h2 style="color:#fff;margin:0;font-size:18px">%s</h2>
                </div>
                <div style="background:#fff;border:1px solid #e5e7eb;border-top:none;padding:25px;border-radius:0 0 8px 8px">
                %s
                  <img src="{OPEN_URL}" width="1" height="1" style="display:none" alt="">
                </div>
                </div>""".formatted(accent, header, inner);
    }

    /** 버튼 공통 마크업 */
    private static String button(String accent, String label) {
        return """
                  <div style="text-align:center;margin:30px 0">
                    <a href="{CLICK_URL}" style="background:%s;color:#fff;padding:12px 28px;border-radius:6px;text-decoration:none;font-weight:bold;display:inline-block">%s</a>
                  </div>""".formatted(accent, label);
    }

    public static final List<Row> TEMPLATES = List.of(

            // 1. IT — 메일함 용량 초과 (MEDIUM)
            d("메일함 용량 초과 경고", "IT", "MEDIUM",
                    "[알림] 메일함 용량이 가득 차 수신이 중단됩니다",
                    "메일 관리 시스템", "postmaster@mail-service-notice.com",
                    card("#0f766e", "메일 저장공간 알림", """
                          <p style="color:#374151">{TARGET_NAME}님, 안녕하세요.</p>
                          <p style="color:#374151">귀하의 메일함 사용량이 <strong>98%</strong>에 도달하여 곧 신규 메일 수신이 중단됩니다.</p>
                          <p style="color:#374151">아래 버튼을 눌러 저장공간을 즉시 확장해 주세요. 미조치 시 24시간 후 수신 메일이 자동 삭제됩니다.</p>
                          """ + button("#0f766e", "저장공간 확장하기") + """
                          <p style="color:#6b7280;font-size:13px">본 메일은 시스템에서 자동 발송되었습니다.</p>"""),
                    "메일함 용량 초과를 빙자해 계정 로그인을 유도하는 중간 난이도 템플릿"),

            // 2. IT — 비밀번호 만료 (EASY)
            d("계정 비밀번호 만료 안내", "IT", "EASY",
                    "[중요] 비밀번호가 오늘 만료됩니다",
                    "IT 헬프데스크", "helpdesk@it-support-notice.com",
                    card("#1a56db", "비밀번호 만료 안내", """
                          <p style="color:#374151">{TARGET_NAME}님 안녕하세요,</p>
                          <p style="color:#374151">보안 정책에 따라 귀하의 비밀번호가 <strong>오늘 만료</strong>됩니다. 만료 후에는 사내 시스템 접속이 제한됩니다.</p>
                          <p style="color:#374151">아래 버튼에서 비밀번호를 갱신하세요.</p>
                          """ + button("#1a56db", "비밀번호 갱신") + """
                          <p style="color:#6b7280;font-size:13px">요청한 적이 없다면 IT 헬프데스크로 문의해 주세요.</p>"""),
                    "비밀번호 만료를 빙자한 기본 난이도 템플릿"),

            // 3. HR — 급여명세서 (MEDIUM)
            d("이달의 급여명세서 확인", "HR", "MEDIUM",
                    "[인사팀] 이달 급여명세서가 발행되었습니다 — 확인 요청",
                    "인사팀", "payroll@hr-notice-center.com",
                    card("#6d28d9", "급여명세서 발행 안내", """
                          <p style="color:#374151">{TARGET_NAME}님,</p>
                          <p style="color:#374151">이번 달 급여명세서가 발행되었습니다. 상여·수당 항목이 일부 조정되어 반드시 확인이 필요합니다.</p>
                          <p style="color:#374151">아래 버튼에서 사번 인증 후 명세서를 열람하세요.</p>
                          """ + button("#6d28d9", "급여명세서 열람") + """
                          <p style="color:#6b7280;font-size:13px">문의: 인사팀 내선 3300</p>"""),
                    "급여명세서 열람을 빙자해 사내 인증정보를 노리는 중간 난이도 템플릿"),

            // 4. HR — 연차 승인 (EASY)
            d("연차 신청 승인 요청", "HR", "EASY",
                    "[승인요청] 연차 신청 건 결재 대기 중",
                    "그룹웨어 결재", "approval@groupware-notice.com",
                    card("#2563eb", "결재 대기 알림", """
                          <p style="color:#374151">{TARGET_NAME}님, 결재 대기 문서가 있습니다.</p>
                          <p style="color:#374151">연차 신청 건이 <strong>귀하의 승인</strong>을 기다리고 있습니다. 기한 내 미처리 시 자동 반려됩니다.</p>
                          """ + button("#2563eb", "결재 문서 확인") + """
                          <p style="color:#6b7280;font-size:13px">그룹웨어 전자결재 시스템에서 자동 발송되었습니다.</p>"""),
                    "전자결재 알림을 빙자한 기본 난이도 템플릿"),

            // 5. FINANCE — 전자세금계산서 (HARD)
            d("전자세금계산서 발급 확인", "FINANCE", "HARD",
                    "[국세청] 전자세금계산서 발급 내역 확인 요청",
                    "홈택스 알림", "no-reply@hometax-notice.go.kr.com",
                    card("#b45309", "전자세금계산서 안내", """
                          <p style="color:#374151">{TARGET_NAME}님(수신: {TARGET_EMAIL})</p>
                          <p style="color:#374151">사업자 앞으로 <strong>전자세금계산서</strong> 1건이 발급되었습니다. 공급가액 대사가 필요하니 내역을 확인해 주세요.</p>
                          <table style="width:100%;border-collapse:collapse;margin:16px 0;font-size:14px;color:#374151">
                            <tr><td style="padding:6px;border:1px solid #e5e7eb">승인번호</td><td style="padding:6px;border:1px solid #e5e7eb">2026-0802-00483921</td></tr>
                            <tr><td style="padding:6px;border:1px solid #e5e7eb">공급가액</td><td style="padding:6px;border:1px solid #e5e7eb">4,180,000원</td></tr>
                          </table>
                          """ + button("#b45309", "발급 내역 확인") + """
                          <p style="color:#6b7280;font-size:12px">본 안내는 발급 확인용이며 세액은 홈택스에서 확인하시기 바랍니다.</p>"""),
                    "국세청 홈택스를 정교하게 사칭한 고난이도 템플릿(유사 도메인 사용)"),

            // 6. DELIVERY — 택배 배송 조회 (MEDIUM)
            d("택배 배송 상태 확인", "DELIVERY", "MEDIUM",
                    "[배송알림] 부재중으로 배송이 보류되었습니다",
                    "택배 고객센터", "cs@delivery-track-notice.net",
                    card("#f59e0b", "배송 보류 안내", """
                          <p style="color:#374151">{TARGET_NAME}님께,</p>
                          <p style="color:#374151">오늘 방문한 택배가 <strong>부재중</strong>으로 배송이 보류되었습니다. 재배송 일정을 선택해 주세요.</p>
                          <p style="color:#374151">운송장 번호: <strong>6428-9910-3372</strong></p>
                          """ + button("#f59e0b", "재배송 신청") + """
                          <p style="color:#6b7280;font-size:12px">48시간 내 미신청 시 물품은 반송 처리됩니다.</p>"""),
                    "택배 부재중 재배송을 빙자한 중간 난이도 템플릿"),

            // 7. SECURITY — 비정상 로그인 (HARD)
            d("비정상 로그인 시도 감지", "SECURITY", "HARD",
                    "[보안경고] 해외 IP에서 계정 로그인이 감지되었습니다",
                    "계정 보안팀", "security-alert@account-protect-notice.com",
                    card("#b91c1c", "계정 보안 경고", """
                          <p style="color:#374151">{TARGET_NAME}님, 계정에서 평소와 다른 접속이 감지되었습니다.</p>
                          <table style="width:100%;border-collapse:collapse;margin:16px 0;font-size:14px;color:#374151">
                            <tr><td style="padding:6px;border:1px solid #e5e7eb">위치</td><td style="padding:6px;border:1px solid #e5e7eb">러시아 (185.220.xx.xx)</td></tr>
                            <tr><td style="padding:6px;border:1px solid #e5e7eb">시각</td><td style="padding:6px;border:1px solid #e5e7eb">방금 전</td></tr>
                          </table>
                          <p style="color:#374151">본인이 아니라면 즉시 계정을 보호하세요.</p>
                          """ + button("#b91c1c", "내 계정 보호하기") + """
                          <p style="color:#6b7280;font-size:12px">30분 내 미조치 시 계정이 일시 잠깁니다.</p>"""),
                    "해외 로그인 보안경고로 긴박감을 조성하는 고난이도 템플릿"),

            // 8. MARKETING — 경품 당첨 (EASY)
            d("사내 복지몰 경품 당첨 안내", "MARKETING", "EASY",
                    "🎉 축하합니다! 커피 기프티콘에 당첨되셨습니다",
                    "복지몰 이벤트", "event@welfare-mall-notice.com",
                    card("#db2777", "이벤트 당첨 안내", """
                          <p style="color:#374151">{TARGET_NAME}님, 축하드립니다!</p>
                          <p style="color:#374151">사내 복지몰 추첨 이벤트에 <strong>당첨</strong>되셨습니다. 아래에서 수령 정보를 입력하면 기프티콘이 발송됩니다.</p>
                          """ + button("#db2777", "경품 수령하기") + """
                          <p style="color:#6b7280;font-size:12px">수령 기한: 발송일로부터 3일 이내</p>"""),
                    "경품 당첨으로 개인정보 입력을 유도하는 기본 난이도 템플릿"),

            // 9. SECURITY — MFA 재등록 (HARD)
            d("다중인증(MFA) 재등록 요청", "SECURITY", "HARD",
                    "[필수] OTP 인증기기 재등록이 필요합니다",
                    "정보보안팀", "iam@mfa-reset-notice.com",
                    card("#0369a1", "다중인증 재등록 안내", """
                          <p style="color:#374151">{TARGET_NAME}님,</p>
                          <p style="color:#374151">인증 서버 이전에 따라 모든 임직원의 <strong>OTP 기기 재등록</strong>이 필요합니다. 기한 내 미등록 시 사내 시스템 접속이 차단됩니다.</p>
                          <p style="color:#374151">아래 버튼에서 사번·비밀번호 인증 후 재등록을 진행하세요.</p>
                          """ + button("#0369a1", "MFA 재등록") + """
                          <p style="color:#6b7280;font-size:12px">등록 마감: 오늘 18:00</p>"""),
                    "MFA 재등록을 빙자해 사내 자격증명을 노리는 고난이도 템플릿"),

            // 10. 기타 — 사내 설문조사 (MEDIUM)
            d("복지제도 개선 설문 참여 요청", "기타", "MEDIUM",
                    "[요청] 복지포인트 지급을 위한 설문에 참여해 주세요",
                    "경영지원팀", "survey@internal-notice-center.com",
                    card("#4f46e5", "임직원 설문 안내", """
                          <p style="color:#374151">{TARGET_NAME}님, 안녕하세요.</p>
                          <p style="color:#374151">복지제도 개선을 위한 설문을 진행합니다. 참여자 전원에게 <strong>복지포인트 5,000P</strong>가 지급됩니다.</p>
                          <p style="color:#374151">아래 버튼에서 사내 계정으로 로그인 후 응답해 주세요. (소요 3분)</p>
                          """ + button("#4f46e5", "설문 참여하기") + """
                          <p style="color:#6b7280;font-size:12px">응답 기한: 이번 주 금요일까지</p>"""),
                    "복지포인트 지급을 미끼로 사내 로그인을 유도하는 중간 난이도 템플릿")
    );
}
