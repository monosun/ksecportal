package com.monosun.secportal.monthlycheck.service;

import com.monosun.secportal.monthlycheck.entity.MonthlyCheckDefault;
import com.monosun.secportal.monthlycheck.entity.MonthlyCheckItem;
import com.monosun.secportal.monthlycheck.repository.MonthlyCheckDefaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 월간 보안점검 "기본 항목" 초기 데이터 시더.
 * monthly_check_defaults 가 비어 있을 때만 기본 32개 점검항목을 채운다.
 * (코드관리 > 월간 점검 기본 항목 탭에서 관리하며, 월간점검 화면의 "기본 항목 불러오기"가 이를 사용한다)
 */
@Component
@Order(21)
@RequiredArgsConstructor
@Slf4j
public class MonthlyCheckDefaultInitializer implements ApplicationRunner {

    private final MonthlyCheckDefaultRepository defaultRepository;

    /** priority, category, itemName, checkMethod, checkExample */
    private static final String[][] DEFAULTS = {
        {"HIGH", "계정관리", "퇴직자 계정 삭제 여부", "인사시스템 퇴직자 명단과 시스템 계정 목록 비교", "퇴사자 A의 AD, VPN, AWS 계정 삭제 확인"},
        {"HIGH", "계정관리", "관리자 권한 적정성", "관리자 권한 보유자 목록 검토", "일반 직원에게 Admin 권한 부여 여부 확인"},
        {"HIGH", "계정관리", "MFA 적용 여부", "MFA 설정 현황 점검", "관리자 계정 MFA 적용 여부 확인"},
        {"HIGH", "서버보안", "보안 패치 적용 여부", "OS 및 Middleware 패치 현황 점검", "Linux/Windows 최신 보안 패치 적용 확인"},
        {"HIGH", "개인정보보호", "개인정보 저장 현황 점검", "파일 서버 및 DB 스캔", "주민등록번호, 계좌번호 저장 여부 확인"},
        {"HIGH", "개인정보보호", "개인정보 반출 이력 점검", "DLP 로그 분석", "이메일, USB 등을 통한 대량 반출 여부 확인"},
        {"HIGH", "백업관리", "정기 백업 수행 여부", "백업 로그 확인", "일일 백업 성공 여부 확인"},
        {"HIGH", "백업관리", "복구 테스트 수행 여부", "복구 결과 검토", "월 1회 복구 테스트 수행 여부 확인"},
        {"HIGH", "로그관리", "관리자 행위 로그 점검", "감사 로그 분석", "권한 변경 및 시스템 설정 변경 이력 확인"},
        {"HIGH", "로그관리", "이상행위 탐지 여부", "SIEM 이벤트 분석", "비정상 로그인, 대량 조회 여부 확인"},
        {"HIGH", "취약점관리", "서버 취약점 점검", "취약점 스캐너 수행", "OpenVAS, Nessus 결과 검토"},
        {"HIGH", "취약점관리", "조치 미완료 취약점 확인", "취약점 조치 현황 검토", "High 등급 취약점 미조치 여부 확인"},
        {"HIGH", "클라우드보안", "IAM 권한 최소화 여부", "권한 정책 검토", "AdministratorAccess 과다 부여 여부 확인"},
        {"HIGH", "네트워크보안", "방화벽 정책 적정성", "허용 정책 검토", "Any-Any 허용 정책 존재 여부 확인"},
        {"HIGH", "네트워크보안", "VPN 계정 관리", "VPN 사용자 목록 검토", "퇴사자 VPN 계정 삭제 여부 확인"},
        {"MEDIUM", "계정관리", "휴면 계정 존재 여부", "최근 90일 이상 미사용 계정 조회", "장기 미사용 계정 잠금 확인"},
        {"MEDIUM", "패스워드 관리", "패스워드 정책 준수 여부", "정책 설정값 확인", "10자 이상, 특수문자 포함 여부 확인"},
        {"MEDIUM", "서버보안", "불필요 서비스 실행 여부", "서비스 목록 확인", "Telnet, FTP 사용 여부 확인"},
        {"MEDIUM", "서버보안", "불필요 포트 개방 여부", "포트 스캔 수행", "21, 23 포트 오픈 여부 확인"},
        {"MEDIUM", "개인정보보호", "개인정보 암호화 여부", "암호화 정책 확인", "고객정보 암호화 적용 여부 확인"},
        {"MEDIUM", "로그관리", "로그인 실패 로그 점검", "인증 로그 분석", "계정 공격 징후 확인"},
        {"MEDIUM", "클라우드보안", "Access Key 관리", "Key 사용 현황 점검", "90일 이상 미교체 Key 확인"},
        {"MEDIUM", "클라우드보안", "감사 로그 활성화 여부", "CloudTrail/Audit Log 확인", "감사 로그 활성화 여부 확인"},
        {"MEDIUM", "네트워크보안", "IDS/IPS 이벤트 확인", "탐지 로그 검토", "비정상 트래픽 탐지 여부 확인"},
        {"LOW", "패스워드 관리", "계정 잠금 정책 적용 여부", "로그인 실패 정책 확인", "5회 실패 시 잠금 설정 확인"},
        {"LOW", "개인정보보호", "공유폴더 접근권한 적정성", "권한 설정 검토", "공유폴더 권한 검토"},
        {"LOW", "백업관리", "백업 데이터 암호화 여부", "백업 정책 확인", "백업본 암호화 적용 여부 확인"},
        {"LOW", "취약점관리", "웹 취약점 점검", "웹 취약점 진단 수행", "XSS, SQL Injection 점검"},
        {"LOW", "취약점관리", "오픈소스 취약점 점검", "SCA 도구 점검", "Log4j, Spring 취약점 확인"},
        {"LOW", "보안운영", "보안 교육 이수 여부", "교육 현황 검토", "신규 입사자 교육 이수 확인"},
        {"LOW", "보안운영", "개선조치 이행 여부", "조치 결과 검토", "전월 미흡사항 조치 완료 확인"},
        {"LOW", "보안운영", "협력사 보안 점검 여부", "점검 결과 확인", "외주업체 보안 점검 수행 여부 확인"},
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (defaultRepository.count() > 0) return;

        List<MonthlyCheckDefault> toSave = new ArrayList<>(DEFAULTS.length);
        for (int i = 0; i < DEFAULTS.length; i++) {
            String[] d = DEFAULTS[i];
            toSave.add(MonthlyCheckDefault.builder()
                    .priority(MonthlyCheckItem.Priority.valueOf(d[0]))
                    .category(d[1])
                    .itemName(d[2])
                    .checkMethod(d[3])
                    .checkExample(d[4])
                    .sortOrder((i + 1) * 10)
                    .active(true)
                    .build());
        }
        defaultRepository.saveAll(toSave);
        log.info("월간 보안점검 기본 항목 초기 데이터 생성 완료: {} 항목", toSave.size());
    }
}
