package com.monosun.secportal.code.service;

import com.monosun.secportal.code.entity.CodeValue;
import com.monosun.secportal.code.repository.CodeValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 개인정보 항목(PI_*)의 마스킹 기준 기본값 시더.
 * 마스킹 기준이 비어 있는 항목만 채우므로 관리자가 수정한 값은 덮어쓰지 않는다.
 * {@link CodeDataInitializer} 이후에 실행된다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class PiMaskingDefaultInitializer implements ApplicationRunner {

    private final CodeValueRepository valueRepo;

    private record MaskingDefault(String groupCode, String label, String type, String rule, String example) {}

    private static MaskingDefault m(String groupCode, String label, String type, String rule, String example) {
        return new MaskingDefault(groupCode, label, type, rule, example);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, MaskingDefault> defaults = DEFAULTS.stream()
                .collect(Collectors.toMap(d -> d.groupCode() + "|" + d.label(), Function.identity(), (a, b) -> a));

        List<CodeValue> targets = valueRepo.findAll().stream()
                .filter(v -> v.getGroupCode() != null && v.getGroupCode().startsWith("PI_"))
                .filter(v -> v.getMaskingType() == null || v.getMaskingType().isBlank())
                .toList();

        int applied = 0;
        for (CodeValue v : targets) {
            MaskingDefault d = defaults.get(v.getGroupCode() + "|" + v.getLabel());
            if (d == null) continue;
            v.setMaskingType(d.type());
            if (v.getMaskingRule() == null || v.getMaskingRule().isBlank()) v.setMaskingRule(d.rule());
            if (v.getMaskingExample() == null || v.getMaskingExample().isBlank()) v.setMaskingExample(d.example());
            applied++;
        }

        if (applied > 0) {
            log.info("개인정보 항목 마스킹 기준 기본값 적용: {} 항목", applied);
        }
    }

    private static final List<MaskingDefault> DEFAULTS = List.of(
            m("PI_BASIC_ID", "성명(법인명)", "부분 마스킹", "가운데 글자를 * 로 대체(2자리는 마지막 글자 마스킹), 법인명은 마스킹 제외", "홍*동"),
            m("PI_BASIC_ID", "생년월일", "부분 마스킹", "연도만 표시하고 월·일은 * 로 대체", "1990-**-**"),
            m("PI_BASIC_ID", "주민등록번호", "암호화 저장", "생년월일 6자리만 표시하고 뒤 7자리는 전부 * 로 대체. 저장 시 암호화 필수(고유식별정보)", "900101-*******"),
            m("PI_BASIC_ID", "외국인등록번호", "암호화 저장", "생년월일 6자리만 표시하고 뒤 7자리는 전부 * 로 대체. 저장 시 암호화 필수(고유식별정보)", "900101-*******"),
            m("PI_BASIC_ID", "여권번호", "암호화 저장", "앞 2자리만 표시하고 나머지는 * 로 대체. 저장 시 암호화 필수(고유식별정보)", "M1*******"),
            m("PI_BASIC_ID", "운전면허번호", "암호화 저장", "지역코드 2자리만 표시하고 나머지는 * 로 대체. 저장 시 암호화 필수(고유식별정보)", "11-**-******-**"),
            m("PI_BASIC_ID", "사업자등록번호", "부분 마스킹", "앞 3자리만 표시하고 나머지는 * 로 대체", "123-**-*****"),
            m("PI_BASIC_ID", "국적", "마스킹 불필요", "단독으로 개인 식별이 불가하여 마스킹 대상에서 제외", "대한민국"),
            m("PI_BASIC_ID", "고객 연계정보(CI)", "일방향 암호화(해시)", "원문 노출 금지, 화면에는 앞 4자리만 표시", "A1B2****…"),
            m("PI_CONTACT", "이동전화번호", "부분 마스킹", "국번 4자리를 * 로 대체", "010-****-1234"),
            m("PI_CONTACT", "유선전화번호", "부분 마스킹", "국번을 * 로 대체", "02-***-1234"),
            m("PI_CONTACT", "이메일 주소", "부분 마스킹", "아이디 앞 3자리만 표시하고 나머지는 * 로 대체, 도메인은 표시", "abc****@test.com"),
            m("PI_CONTACT", "연락 가능한 전화번호", "부분 마스킹", "국번 4자리를 * 로 대체", "010-****-1234"),
            m("PI_CONTACT", "주소(거주지, 설치장소, 배송지 등)", "부분 마스킹", "읍·면·동까지만 표시하고 상세주소(번지·동호수)는 * 로 대체", "서울시 강남구 역삼동 ***"),
            m("PI_CONTACT", "우편번호", "부분 마스킹", "앞 2자리만 표시하고 나머지는 * 로 대체", "06***"),
            m("PI_IDENTITY_DOC", "신분증 이미지", "전체 마스킹", "원본 이미지 비노출, 권한자만 열람하고 열람 사유·이력을 기록. 확인 후 지체 없이 파기", "(비노출)"),
            m("PI_IDENTITY_DOC", "신분증 기재사항", "부분 마스킹", "성명은 가운데 글자, 식별번호는 뒤 7자리를 * 로 대체", "홍*동 / 900101-*******"),
            m("PI_IDENTITY_DOC", "신분증 발급일자", "부분 마스킹", "연·월까지만 표시하고 일자는 * 로 대체", "2020-05-**"),
            m("PI_IDENTITY_DOC", "신분증 얼굴사진 원본", "전체 마스킹", "원본 사진 비노출, 본인확인 완료 후 지체 없이 파기", "(비노출)"),
            m("PI_IDENTITY_DOC", "얼굴사진으로부터 추출된 특징정보(안면인증)", "암호화 저장", "민감정보로 별도 동의 후 암호화 저장, 화면 표시 금지", "(비노출)"),
            m("PI_IDENTITY_DOC", "법정대리인 신분증 정보", "부분 마스킹", "성명은 가운데 글자, 식별번호는 뒤 7자리를 * 로 대체", "김*수 / 800101-*******"),
            m("PI_IDENTITY_DOC", "가족관계 정보", "전체 마스킹", "관계 구분만 표시하고 구성원 상세 정보는 비노출, 권한자만 열람", "부(父)"),
            m("PI_DEVICE_LINE", "단말기 모델명", "마스킹 불필요", "단독으로 개인 식별이 불가하여 마스킹 대상에서 제외", "Galaxy S24"),
            m("PI_DEVICE_LINE", "IMEI", "부분 마스킹", "뒤 4자리만 표시하고 나머지는 * 로 대체", "***********1234"),
            m("PI_DEVICE_LINE", "USIM 번호", "부분 마스킹", "뒤 4자리만 표시하고 나머지는 * 로 대체", "****************1234"),
            m("PI_DEVICE_LINE", "eSIM 번호", "부분 마스킹", "뒤 4자리만 표시하고 나머지는 * 로 대체", "****************1234"),
            m("PI_DEVICE_LINE", "MAC Address", "부분 마스킹", "앞 3옥텟(OUI)만 표시하고 뒤 3옥텟은 * 로 대체", "00:1A:2B:**:**:**"),
            m("PI_DEVICE_LINE", "회선번호", "부분 마스킹", "국번 4자리를 * 로 대체", "010-****-1234"),
            m("PI_DEVICE_LINE", "개통일자", "마스킹 불필요", "단독으로 개인 식별이 불가하여 마스킹 대상에서 제외", "2023-05-01"),
            m("PI_DEVICE_LINE", "가입일·해지일·가입기간", "마스킹 불필요", "단독으로 개인 식별이 불가하여 마스킹 대상에서 제외", "2023-05-01 ~ 2025-05-01"),
            m("PI_DEVICE_LINE", "이동통신사 정보", "마스킹 불필요", "단독으로 개인 식별이 불가하여 마스킹 대상에서 제외", "SKT"),
            m("PI_PAYMENT_FIN", "계좌번호", "암호화 저장", "앞 3자리와 뒤 3자리만 표시하고 나머지는 * 로 대체. 저장 시 암호화 필수", "110-***-***456"),
            m("PI_PAYMENT_FIN", "카드번호", "암호화 저장", "앞 6자리·뒤 4자리만 표시하고 나머지는 * 로 대체(PCI-DSS 기준). 저장 시 암호화 필수", "123456******1234"),
            m("PI_PAYMENT_FIN", "카드 유효기간", "전체 마스킹", "화면 표시 금지, 결제 처리 목적 외 조회 불가", "**/**"),
            m("PI_PAYMENT_FIN", "예금주/카드소유자 정보", "부분 마스킹", "성명은 가운데 글자를 * 로 대체", "홍*동"),
            m("PI_PAYMENT_FIN", "카드 비밀번호 앞 2자리", "일방향 암호화(해시)", "평문 저장·조회·화면 표시 금지, 일방향 암호화하여 저장", "**"),
            m("PI_PAYMENT_FIN", "납부자 정보(이름, 생년월일, 주소)", "부분 마스킹", "성명은 가운데 글자, 생년월일은 월·일, 주소는 상세주소를 * 로 대체", "홍*동 / 1990-**-** / 서울시 강남구 ***"),
            m("PI_PAYMENT_FIN", "수납금액", "마스킹 불필요", "금액 단독으로는 개인 식별이 불가하여 마스킹 대상에서 제외", "55,000원"),
            m("PI_PAYMENT_FIN", "결제기록", "부분 마스킹", "카드번호는 마스킹하여 표시하고 거래 일시·금액만 노출", "123456******1234 / 55,000원"),
            m("PI_PAYMENT_FIN", "휴대폰 결제 비밀번호", "일방향 암호화(해시)", "평문 저장·조회·화면 표시 금지, 일방향 암호화하여 저장", "********"),
            m("PI_CREDIT", "연체정보", "전체 마스킹", "신용정보로 권한자만 열람, 화면에는 해당 여부만 표시", "연체 있음 / 없음"),
            m("PI_CREDIT", "신용도 판단정보", "전체 마스킹", "신용정보로 권한자만 열람, 상세 판단 근거는 비노출", "(비노출)"),
            m("PI_CREDIT", "신용거래정보", "전체 마스킹", "신용정보로 권한자만 열람, 거래 상세 내역은 비노출", "(비노출)"),
            m("PI_CREDIT", "신용등급", "전체 마스킹", "신용정보로 권한자만 열람, 화면에는 심사 결과만 표시", "승인 / 거절"),
            m("PI_CREDIT", "신용보험 가입 가능 여부", "전체 마스킹", "가입 가능 여부만 표시하고 판단 근거는 비노출", "가능 / 불가"),
            m("PI_CREDIT", "신용정보 조회 기록", "전체 마스킹", "조회 이력은 권한자만 열람하며 열람 이력을 별도 기록", "(비노출)"),
            m("PI_CREDIT", "채무조정 관련 정보", "전체 마스킹", "신용정보로 권한자만 열람, 화면에는 진행 여부만 표시", "해당 / 비해당"),
            m("PI_SERVICE_USAGE", "발신번호", "부분 마스킹", "국번 4자리를 * 로 대체", "010-****-1234"),
            m("PI_SERVICE_USAGE", "수신번호(통화 상대방 번호 포함)", "부분 마스킹", "통화 상대방 번호는 국번과 뒤 4자리를 * 로 대체", "010-****-****"),
            m("PI_SERVICE_USAGE", "통화시각", "마스킹 불필요", "시각 단독으로는 개인 식별이 불가하여 마스킹 대상에서 제외", "2026-01-01 10:00"),
            m("PI_SERVICE_USAGE", "사용도수", "마스킹 불필요", "통계값 단독으로는 개인 식별이 불가하여 마스킹 대상에서 제외", "120회"),
            m("PI_SERVICE_USAGE", "서비스 이용기록", "부분 마스킹", "이용기록에 포함된 계정·연락처는 마스킹하여 표시", "abc****@test.com 로그인"),
            m("PI_SERVICE_USAGE", "접속로그", "부분 마스킹", "로그에 기록되는 IP는 뒤 2옥텟, 계정은 앞 3자리 외 * 로 대체", "192.168.*.* / abc****"),
            m("PI_SERVICE_USAGE", "쿠키", "일방향 암호화(해시)", "식별자는 해시 처리하여 저장, 원문 노출 금지", "(해시값)"),
            m("PI_SERVICE_USAGE", "IP 주소", "부분 마스킹", "뒤 2옥텟을 * 로 대체", "192.168.*.*"),
            m("PI_SERVICE_USAGE", "이용정지 기록", "부분 마스킹", "정지 일시·사유만 표시하고 회선번호는 마스킹", "010-****-1234 / 요금 미납"),
            m("PI_SERVICE_USAGE", "요금 과금 관련 데이터", "마스킹 불필요", "과금 데이터 단독으로는 개인 식별이 불가하여 마스킹 대상에서 제외", "55,000원"),
            m("PI_SERVICE_USAGE", "전화/문자/데이터 사용량", "마스킹 불필요", "사용량 통계는 개인 식별이 불가하여 마스킹 대상에서 제외", "12GB"),
            m("PI_LOCATION", "이동전화 단말기 위치정보", "부분 마스킹", "위치정보로 별도 동의 필요, 좌표는 소수점 2자리까지만 표시", "37.49, 127.03"),
            m("PI_LOCATION", "기지국 위치정보", "부분 마스킹", "기지국 식별자는 뒤 4자리를 * 로 대체하고 행정동 단위까지만 표시", "서울 강남구 / KT****"),
            m("PI_LOCATION", "GPS 정보", "부분 마스킹", "위치정보로 별도 동의 필요, 좌표는 소수점 2자리까지만 표시", "37.49, 127.03"),
            m("PI_LOCATION", "Zigbee AP 정보", "부분 마스킹", "AP 식별자는 뒤 3옥텟을 * 로 대체", "00:1A:2B:**:**:**"),
            m("PI_LOCATION", "Wi-Fi AP 정보", "부분 마스킹", "BSSID는 뒤 3옥텟을 * 로 대체", "00:1A:2B:**:**:**"),
            m("PI_LOCATION", "RFID 태그 정보", "부분 마스킹", "태그 식별자는 뒤 4자리만 표시", "********1234"),
            m("PI_LOCATION", "USIM 관련 위치정보", "부분 마스킹", "USIM 번호는 뒤 4자리만 표시하고 위치는 행정동 단위까지만 표시", "****1234 / 서울 강남구"),
            m("PI_WELFARE", "국가유공자 증명 정보", "전체 마스킹", "자격 보유 여부만 표시하고 증빙 상세는 비노출, 확인 후 지체 없이 파기", "해당 / 비해당"),
            m("PI_WELFARE", "복지할인 증명 정보", "전체 마스킹", "자격 보유 여부만 표시하고 증빙 상세는 비노출, 확인 후 지체 없이 파기", "해당 / 비해당"),
            m("PI_WELFARE", "장애인 여부", "전체 마스킹", "민감정보로 별도 동의 필요, 해당 여부만 표시하고 장애 유형·등급은 비노출", "해당 / 비해당"),
            m("PI_WELFARE", "기초생활수급자 여부", "전체 마스킹", "해당 여부만 표시하고 증빙 상세는 비노출, 권한자만 열람", "해당 / 비해당"),
            m("PI_WELFARE", "차상위계층 여부 등 감면 자격 확인 정보", "전체 마스킹", "해당 여부만 표시하고 증빙 상세는 비노출, 권한자만 열람", "해당 / 비해당"),
            m("PI_NUMBER_PORT", "기존 통신사 정보", "마스킹 불필요", "통신사명 단독으로는 개인 식별이 불가하여 마스킹 대상에서 제외", "KT"),
            m("PI_NUMBER_PORT", "이용 중 요금제", "마스킹 불필요", "요금제명 단독으로는 개인 식별이 불가하여 마스킹 대상에서 제외", "5G 슬림"),
            m("PI_NUMBER_PORT", "위약금/할부 정보", "마스킹 불필요", "금액 단독으로는 개인 식별이 불가하여 마스킹 대상에서 제외", "120,000원"),
            m("PI_NUMBER_PORT", "소멸 예정 혜택 정보", "마스킹 불필요", "혜택 정보 단독으로는 개인 식별이 불가하여 마스킹 대상에서 제외", "포인트 1,000점"),
            m("PI_NUMBER_PORT", "번호이동 ARS 녹음파일", "전체 마스킹", "음성 원본은 권한자만 열람하고 열람 이력을 기록, 보유기간 경과 시 파기", "(비노출)"),
            m("PI_MINOR_AGENT", "법정대리인 이름", "부분 마스킹", "가운데 글자를 * 로 대체(2자리는 마지막 글자 마스킹)", "김*수"),
            m("PI_MINOR_AGENT", "법정대리인 연락처", "부분 마스킹", "전화번호는 국번 4자리를, 이메일은 아이디 앞 3자리 외를 * 로 대체", "010-****-1234"),
            m("PI_MINOR_AGENT", "가족관계 정보", "전체 마스킹", "관계 구분만 표시하고 구성원 상세 정보는 비노출, 확인 후 지체 없이 파기", "부(父)"),
            m("PI_MINOR_AGENT", "법정대리인 신분증 정보", "부분 마스킹", "성명은 가운데 글자, 식별번호는 뒤 7자리를 * 로 대체. 원본 이미지는 확인 후 파기", "김*수 / 800101-*******"),
            m("PI_MARKETING", "휴대전화번호", "부분 마스킹", "국번 4자리를 * 로 대체", "010-****-1234"),
            m("PI_MARKETING", "이메일", "부분 마스킹", "아이디 앞 3자리만 표시하고 나머지는 * 로 대체, 도메인은 표시", "abc****@test.com"),
            m("PI_MARKETING", "주소", "부분 마스킹", "읍·면·동까지만 표시하고 상세주소는 * 로 대체", "서울시 강남구 역삼동 ***"),
            m("PI_MARKETING", "단말식별번호(Device ID)", "부분 마스킹", "뒤 4자리만 표시하고 나머지는 * 로 대체", "***********1234"),
            m("PI_MARKETING", "PUSH 토큰", "부분 마스킹", "앞 4자리만 표시하고 나머지는 * 로 대체", "fMz1****…"),
            m("PI_MARKETING", "상담 내용에 입력한 개인정보", "전체 마스킹", "상담 내용에 포함된 개인정보는 비노출, 권한자만 열람하고 열람 이력 기록", "(비노출)"),
            m("PI_SENSITIVE", "얼굴 특징정보(안면인증 특징점)", "암호화 저장", "민감정보로 별도 동의 후 암호화 저장, 화면 표시 금지 및 목적 달성 시 즉시 파기", "(비노출)")
    );
}
