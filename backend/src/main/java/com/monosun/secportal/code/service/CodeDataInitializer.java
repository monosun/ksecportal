package com.monosun.secportal.code.service;

import com.monosun.secportal.code.entity.CodeGroup;
import com.monosun.secportal.code.entity.CodeValue;
import com.monosun.secportal.code.repository.CodeGroupRepository;
import com.monosun.secportal.code.repository.CodeValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeDataInitializer implements ApplicationRunner {

    private final CodeGroupRepository groupRepo;
    private final CodeValueRepository valueRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initDepartments();
        initSecuritySolutionTypes();
        initAssetTypes();
        initThreatTypes();
        initPiType();
        initPiBasicId();
        initPiContact();
        initPiIdentityDoc();
        initPiDeviceLine();
        initPiPaymentFin();
        initPiCredit();
        initPiServiceUsage();
        initPiLocation();
        initPiWelfare();
        initPiNumberPort();
        initPiMinorAgent();
        initPiMarketing();
        initPiSensitive();
        initSecDocOrgs();
    }

    private void initSecDocOrgs() {
        if (groupRepo.existsById("SEC_DOC_ORG")) return;

        groupRepo.save(CodeGroup.builder()
                .groupCode("SEC_DOC_ORG")
                .groupName("문서 제작기관")
                .description("보안문서 등록 시 선택하는 제작기관 목록 (직접 입력도 가능)")
                .sortOrder(18)
                .build());

        List<String> orgs = List.of(
                "정보보안팀", "IT운영팀", "개인정보보호팀", "컴플라이언스팀",
                "외부 컨설팅사", "인증기관", "기타"
        );

        for (int i = 0; i < orgs.size(); i++) {
            String org = orgs.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("SEC_DOC_ORG")
                    .value(org)
                    .label(org)
                    .sortOrder(i + 1)
                    .active(true)
                    .build());
        }

        log.info("코드 초기 데이터 생성 완료: SEC_DOC_ORG ({} 항목)", orgs.size());
    }

    private void initDepartments() {
        if (groupRepo.existsById("DEPARTMENT")) return;

        groupRepo.save(CodeGroup.builder()
                .groupCode("DEPARTMENT")
                .groupName("부서")
                .description("조직 내 부서 목록")
                .sortOrder(1)
                .build());

        List<String> departments = List.of(
                "경영지원팀", "기획팀", "개발팀", "인프라팀",
                "보안팀", "운영팀", "마케팅팀", "영업팀",
                "인사팀", "재무팀", "법무팀", "연구개발팀"
        );

        for (int i = 0; i < departments.size(); i++) {
            String dept = departments.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("DEPARTMENT")
                    .value(dept)
                    .label(dept)
                    .sortOrder(i + 1)
                    .active(true)
                    .build());
        }

        log.info("코드 초기 데이터 생성 완료: DEPARTMENT ({} 항목)", departments.size());
    }

    private void initSecuritySolutionTypes() {
        if (groupRepo.existsById("SECURITY_SOLUTION_TYPE")) return;

        groupRepo.save(CodeGroup.builder()
                .groupCode("SECURITY_SOLUTION_TYPE")
                .groupName("보안솔루션 유형")
                .description("보안이벤트 관리에서 사용하는 보안솔루션 분류")
                .sortOrder(2)
                .build());

        List<String[]> types = List.of(
                new String[]{"FIREWALL",   "방화벽"},
                new String[]{"IDS_IPS",    "IDS/IPS"},
                new String[]{"WAF",        "WAF"},
                new String[]{"SIEM",       "SIEM"},
                new String[]{"EDR",        "EDR"},
                new String[]{"DLP",        "DLP"},
                new String[]{"ANTIVIRUS",  "안티바이러스"},
                new String[]{"NAC",        "NAC"},
                new String[]{"VPN",        "VPN"},
                new String[]{"OTHER",      "기타"}
        );

        for (int i = 0; i < types.size(); i++) {
            valueRepo.save(CodeValue.builder()
                    .groupCode("SECURITY_SOLUTION_TYPE")
                    .value(types.get(i)[0])
                    .label(types.get(i)[1])
                    .sortOrder(i + 1)
                    .active(true)
                    .build());
        }

        log.info("코드 초기 데이터 생성 완료: SECURITY_SOLUTION_TYPE ({} 항목)", types.size());
    }

    private void initAssetTypes() {
        if (groupRepo.existsById("ASSET_TYPE")) return;

        groupRepo.save(CodeGroup.builder()
                .groupCode("ASSET_TYPE")
                .groupName("자산 유형")
                .description("자산 관리에서 사용하는 자산 유형 분류")
                .sortOrder(3)
                .build());

        List<String[]> types = List.of(
                new String[]{"SERVER",       "서버"},
                new String[]{"WORKSTATION",  "워크스테이션"},
                new String[]{"NETWORK",      "네트워크 장비"},
                new String[]{"APPLICATION",  "애플리케이션"},
                new String[]{"DATABASE",     "데이터베이스"},
                new String[]{"EC2",          "EC2 인스턴스"},
                new String[]{"RDS",          "RDS"},
                new String[]{"S3",           "S3 버킷"},
                new String[]{"ELB",          "로드밸런서"},
                new String[]{"LAMBDA",       "Lambda"},
                new String[]{"CLOUD_OTHER",  "클라우드 기타"},
                new String[]{"OTHER",        "기타"}
        );

        for (int i = 0; i < types.size(); i++) {
            valueRepo.save(CodeValue.builder()
                    .groupCode("ASSET_TYPE")
                    .value(types.get(i)[0])
                    .label(types.get(i)[1])
                    .sortOrder(i + 1)
                    .active(true)
                    .build());
        }

        log.info("코드 초기 데이터 생성 완료: ASSET_TYPE ({} 항목)", types.size());
    }

    private void initThreatTypes() {
        if (groupRepo.existsById("THREAT_TYPE")) return;

        groupRepo.save(CodeGroup.builder()
                .groupCode("THREAT_TYPE")
                .groupName("위협 유형")
                .description("위협 카탈로그에서 사용하는 위협 분류")
                .sortOrder(4)
                .build());

        List<String> types = List.of(
                "외부공격", "내부위협", "기술적위협", "물리적위협",
                "인적위협", "자연재해", "기타"
        );

        for (int i = 0; i < types.size(); i++) {
            String t = types.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("THREAT_TYPE")
                    .value(t)
                    .label(t)
                    .sortOrder(i + 1)
                    .active(true)
                    .build());
        }

        log.info("코드 초기 데이터 생성 완료: THREAT_TYPE ({} 항목)", types.size());
    }

    private void initPiType() {
        if (groupRepo.existsById("PI_TYPE")) return;

        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_TYPE")
                .groupName("개인정보 유형")
                .description("개인정보 분류 유형 목록")
                .sortOrder(5)
                .build());

        List<String> items = List.of(
                "기본 식별정보",
                "연락처 정보",
                "신분증 및 본인확인 정보",
                "단말기 및 회선 정보",
                "결제·금융 정보",
                "신용 관련 정보",
                "서비스 이용 정보",
                "위치정보",
                "복지·감면 자격 정보",
                "번호이동 관련 정보",
                "미성년자·대리인 정보",
                "마케팅·프로모션 정보",
                "민감정보"
        );

        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_TYPE").value(v).label(v).sortOrder(i + 1).active(true).build());
        }

        log.info("코드 초기 데이터 생성 완료: PI_TYPE ({} 항목)", items.size());
    }

    private void initPiBasicId() {
        if (groupRepo.existsById("PI_BASIC_ID")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_BASIC_ID")
                .groupName("기본 식별정보")
                .description("개인정보 분류: 기본 식별정보 항목")
                .sortOrder(5)
                .build());
        List<String> items = List.of(
                "성명(법인명)", "생년월일", "주민등록번호", "외국인등록번호",
                "여권번호", "운전면허번호", "사업자등록번호", "국적", "고객 연계정보(CI)"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_BASIC_ID").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_BASIC_ID ({} 항목)", items.size());
    }

    private void initPiContact() {
        if (groupRepo.existsById("PI_CONTACT")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_CONTACT")
                .groupName("연락처 정보")
                .description("개인정보 분류: 연락처 정보 항목")
                .sortOrder(6)
                .build());
        List<String> items = List.of(
                "이동전화번호", "유선전화번호", "이메일 주소", "연락 가능한 전화번호",
                "주소(거주지, 설치장소, 배송지 등)", "우편번호"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_CONTACT").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_CONTACT ({} 항목)", items.size());
    }

    private void initPiIdentityDoc() {
        if (groupRepo.existsById("PI_IDENTITY_DOC")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_IDENTITY_DOC")
                .groupName("신분증 및 본인확인 정보")
                .description("개인정보 분류: 신분증 및 본인확인 정보 항목")
                .sortOrder(7)
                .build());
        List<String> items = List.of(
                "신분증 이미지", "신분증 기재사항", "신분증 발급일자",
                "신분증 얼굴사진 원본", "얼굴사진으로부터 추출된 특징정보(안면인증)",
                "법정대리인 신분증 정보", "가족관계 정보"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_IDENTITY_DOC").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_IDENTITY_DOC ({} 항목)", items.size());
    }

    private void initPiDeviceLine() {
        if (groupRepo.existsById("PI_DEVICE_LINE")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_DEVICE_LINE")
                .groupName("단말기 및 회선 정보")
                .description("개인정보 분류: 단말기 및 회선 정보 항목")
                .sortOrder(8)
                .build());
        List<String> items = List.of(
                "단말기 모델명", "IMEI", "USIM 번호", "eSIM 번호", "MAC Address",
                "회선번호", "개통일자", "가입일·해지일·가입기간", "이동통신사 정보"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_DEVICE_LINE").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_DEVICE_LINE ({} 항목)", items.size());
    }

    private void initPiPaymentFin() {
        if (groupRepo.existsById("PI_PAYMENT_FIN")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_PAYMENT_FIN")
                .groupName("결제·금융 정보")
                .description("개인정보 분류: 결제 및 금융 정보 항목")
                .sortOrder(9)
                .build());
        List<String> items = List.of(
                "계좌번호", "카드번호", "카드 유효기간", "예금주/카드소유자 정보",
                "카드 비밀번호 앞 2자리", "납부자 정보(이름, 생년월일, 주소)",
                "수납금액", "결제기록", "휴대폰 결제 비밀번호"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_PAYMENT_FIN").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_PAYMENT_FIN ({} 항목)", items.size());
    }

    private void initPiCredit() {
        if (groupRepo.existsById("PI_CREDIT")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_CREDIT")
                .groupName("신용 관련 정보")
                .description("개인정보 분류: 신용 관련 정보 항목")
                .sortOrder(10)
                .build());
        List<String> items = List.of(
                "연체정보", "신용도 판단정보", "신용거래정보", "신용등급",
                "신용보험 가입 가능 여부", "신용정보 조회 기록", "채무조정 관련 정보"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_CREDIT").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_CREDIT ({} 항목)", items.size());
    }

    private void initPiServiceUsage() {
        if (groupRepo.existsById("PI_SERVICE_USAGE")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_SERVICE_USAGE")
                .groupName("서비스 이용 정보")
                .description("개인정보 분류: 서비스 이용 정보 항목")
                .sortOrder(11)
                .build());
        List<String> items = List.of(
                "발신번호", "수신번호(통화 상대방 번호 포함)", "통화시각", "사용도수",
                "서비스 이용기록", "접속로그", "쿠키", "IP 주소",
                "이용정지 기록", "요금 과금 관련 데이터", "전화/문자/데이터 사용량"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_SERVICE_USAGE").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_SERVICE_USAGE ({} 항목)", items.size());
    }

    private void initPiLocation() {
        if (groupRepo.existsById("PI_LOCATION")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_LOCATION")
                .groupName("위치정보")
                .description("개인정보 분류: 위치정보 항목")
                .sortOrder(12)
                .build());
        List<String> items = List.of(
                "이동전화 단말기 위치정보", "기지국 위치정보", "GPS 정보",
                "Zigbee AP 정보", "Wi-Fi AP 정보", "RFID 태그 정보", "USIM 관련 위치정보"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_LOCATION").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_LOCATION ({} 항목)", items.size());
    }

    private void initPiWelfare() {
        if (groupRepo.existsById("PI_WELFARE")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_WELFARE")
                .groupName("복지·감면 자격 정보")
                .description("개인정보 분류: 복지 및 감면 자격 정보 항목")
                .sortOrder(13)
                .build());
        List<String> items = List.of(
                "국가유공자 증명 정보", "복지할인 증명 정보", "장애인 여부",
                "기초생활수급자 여부", "차상위계층 여부 등 감면 자격 확인 정보"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_WELFARE").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_WELFARE ({} 항목)", items.size());
    }

    private void initPiNumberPort() {
        if (groupRepo.existsById("PI_NUMBER_PORT")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_NUMBER_PORT")
                .groupName("번호이동 관련 정보")
                .description("개인정보 분류: 번호이동 관련 정보 항목")
                .sortOrder(14)
                .build());
        List<String> items = List.of(
                "기존 통신사 정보", "이용 중 요금제", "위약금/할부 정보",
                "소멸 예정 혜택 정보", "번호이동 ARS 녹음파일"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_NUMBER_PORT").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_NUMBER_PORT ({} 항목)", items.size());
    }

    private void initPiMinorAgent() {
        if (groupRepo.existsById("PI_MINOR_AGENT")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_MINOR_AGENT")
                .groupName("미성년자·대리인 정보")
                .description("개인정보 분류: 미성년자 및 법정대리인 정보 항목")
                .sortOrder(15)
                .build());
        List<String> items = List.of(
                "법정대리인 이름", "법정대리인 연락처", "가족관계 정보", "법정대리인 신분증 정보"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_MINOR_AGENT").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_MINOR_AGENT ({} 항목)", items.size());
    }

    private void initPiMarketing() {
        if (groupRepo.existsById("PI_MARKETING")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_MARKETING")
                .groupName("마케팅·프로모션 정보")
                .description("개인정보 분류: 마케팅 및 프로모션 정보 항목 (선택 동의)")
                .sortOrder(16)
                .build());
        List<String> items = List.of(
                "휴대전화번호", "이메일", "주소", "단말식별번호(Device ID)",
                "PUSH 토큰", "상담 내용에 입력한 개인정보"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_MARKETING").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_MARKETING ({} 항목)", items.size());
    }

    private void initPiSensitive() {
        if (groupRepo.existsById("PI_SENSITIVE")) return;
        groupRepo.save(CodeGroup.builder()
                .groupCode("PI_SENSITIVE")
                .groupName("민감정보")
                .description("개인정보 분류: 민감정보 항목")
                .sortOrder(17)
                .build());
        List<String> items = List.of(
                "얼굴 특징정보(안면인증 특징점)"
        );
        for (int i = 0; i < items.size(); i++) {
            String v = items.get(i);
            valueRepo.save(CodeValue.builder()
                    .groupCode("PI_SENSITIVE").value(v).label(v).sortOrder(i + 1).active(true).build());
        }
        log.info("코드 초기 데이터 생성 완료: PI_SENSITIVE ({} 항목)", items.size());
    }
}
