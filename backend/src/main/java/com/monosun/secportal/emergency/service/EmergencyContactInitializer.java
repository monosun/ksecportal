package com.monosun.secportal.emergency.service;

import com.monosun.secportal.emergency.entity.EmergencyContact;
import com.monosun.secportal.emergency.entity.EmergencyContactGroup;
import com.monosun.secportal.emergency.entity.EmergencyContactGroup.ContactType;
import com.monosun.secportal.emergency.repository.EmergencyContactGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 비상연락망 기본 골격을 테이블이 비어 있을 때만 시드한다.
 *
 * 내부 대응반은 조직마다 구성이 다르므로 그룹(연락 계통)만 만들어 두고 담당자는 화면에서 채운다.
 * 외부 신고·문의 기관은 공개된 대표 신고번호라 개인정보가 아니므로 값까지 함께 넣는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(62)
public class EmergencyContactInitializer implements ApplicationRunner {

    private final EmergencyContactGroupRepository repository;

    private record ExternalRow(String name, String organization, String roleName,
                               String officePhone, boolean available24h, String note) {}

    /** 공개된 국가기관 대표 신고·상담 번호 */
    private static final List<ExternalRow> EXTERNAL_AGENCIES = List.of(
            new ExternalRow("인터넷침해대응센터", "한국인터넷진흥원(KISA)", "침해사고 신고",
                    "118", true, "해킹·악성코드·DDoS 등 침해사고 신고 및 기술 지원 (국번없이 118)"),
            new ExternalRow("개인정보침해 신고상담센터", "한국인터넷진흥원(KISA)", "개인정보 침해 신고",
                    "118", true, "개인정보 유출·오남용 신고 및 상담 (국번없이 118)"),
            new ExternalRow("사이버범죄 신고상담", "경찰청", "수사기관 신고",
                    "182", true, "해킹·전자금융사기 등 사이버범죄 신고 (국번없이 182)"),
            new ExternalRow("산업기밀보호센터", "국가정보원", "기술유출 신고",
                    "111", true, "산업기술·영업비밀 유출 신고 (국번없이 111)")
    );

    /** 내부 대응 계통 — 담당자는 조직에 맞게 화면에서 등록한다 */
    private record GroupRow(String name, ContactType type, int sortOrder, String description) {}

    private static final List<GroupRow> GROUPS = List.of(
            new GroupRow("침해사고 대응반", ContactType.INTERNAL, 1,
                    "해킹·악성코드·서비스 거부 등 보안 침해사고 발생 시 연락 계통. 총괄 → 기술 대응 → 대외 신고 순으로 연락합니다."),
            new GroupRow("개인정보 유출 대응반", ContactType.INTERNAL, 2,
                    "개인정보 유출·오남용 사고 발생 시 연락 계통. 개인정보보호책임자(CPO)를 1차로 연락하고 법정 신고 기한을 관리합니다."),
            new GroupRow("재해복구·시스템 장애 대응반", ContactType.INTERNAL, 3,
                    "정전·화재·시스템 장애 등 업무연속성 위협 상황의 연락 계통. 재해복구·BCP 훈련 시나리오의 담당 역할과 맞추어 관리합니다."),
            new GroupRow("경영진 보고 계통", ContactType.INTERNAL, 4,
                    "중대 사고 발생 시 경영진 보고 순서. 사고 등급 판정 후 지체 없이 보고합니다."),
            new GroupRow("외부 신고·문의 기관", ContactType.EXTERNAL, 5,
                    "법정 신고 및 기술 지원을 받을 수 있는 외부 기관 연락처."),
            new GroupRow("협력사·유지보수 업체", ContactType.PARTNER, 6,
                    "장애 발생 시 기술 지원을 요청할 협력사·유지보수 업체 연락처. 계약상 대응 시간(SLA)을 비고에 기록합니다.")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;

        for (GroupRow row : GROUPS) {
            EmergencyContactGroup group = EmergencyContactGroup.builder()
                    .name(row.name())
                    .contactType(row.type())
                    .sortOrder(row.sortOrder())
                    .description(row.description())
                    .build();

            if (row.type() == ContactType.EXTERNAL) {
                int order = 1;
                for (ExternalRow e : EXTERNAL_AGENCIES) {
                    group.getContacts().add(EmergencyContact.builder()
                            .group(group)
                            .name(e.name())
                            .organization(e.organization())
                            .roleName(e.roleName())
                            .contactOrder(order++)
                            .officePhone(e.officePhone())
                            .available24h(e.available24h())
                            .note(e.note())
                            .build());
                }
            }
            repository.save(group);
        }
        log.info("[비상연락망] 기본 연락 계통 {}개 그룹·외부기관 연락처 {}건 초기화",
                GROUPS.size(), EXTERNAL_AGENCIES.size());
    }
}
