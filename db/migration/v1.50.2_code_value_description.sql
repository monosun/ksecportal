-- code_values 테이블에 description 컬럼 추가
ALTER TABLE code_values ADD COLUMN description VARCHAR(500) NULL AFTER label;

-- ── PI_BASIC_ID ──────────────────────────────────────────
UPDATE code_values SET description = '개인 또는 법인의 성명 및 상호' WHERE group_code = 'PI_BASIC_ID' AND label = '성명(법인명)';
UPDATE code_values SET description = '태어난 연월일 (YYYYMMDD)' WHERE group_code = 'PI_BASIC_ID' AND label = '생년월일';
UPDATE code_values SET description = '주민등록법상 부여된 13자리 고유 식별번호 (고유식별정보)' WHERE group_code = 'PI_BASIC_ID' AND label = '주민등록번호';
UPDATE code_values SET description = '외국인 등록 시 부여되는 고유 번호 (고유식별정보)' WHERE group_code = 'PI_BASIC_ID' AND label = '외국인등록번호';
UPDATE code_values SET description = '여권에 기재된 여권 고유번호 (고유식별정보)' WHERE group_code = 'PI_BASIC_ID' AND label = '여권번호';
UPDATE code_values SET description = '운전면허증에 기재된 고유 번호 (고유식별정보)' WHERE group_code = 'PI_BASIC_ID' AND label = '운전면허번호';
UPDATE code_values SET description = '사업자에게 부여된 고유 등록번호' WHERE group_code = 'PI_BASIC_ID' AND label = '사업자등록번호';
UPDATE code_values SET description = '국적 또는 시민권 국가 정보' WHERE group_code = 'PI_BASIC_ID' AND label = '국적';
UPDATE code_values SET description = '주민번호 기반 연계정보, 온라인 본인확인 서비스에 사용' WHERE group_code = 'PI_BASIC_ID' AND label = '고객 연계정보(CI)';

-- ── PI_CONTACT ───────────────────────────────────────────
UPDATE code_values SET description = '개인 휴대전화 번호' WHERE group_code = 'PI_CONTACT' AND label = '이동전화번호';
UPDATE code_values SET description = '집 또는 사무실 유선 전화번호' WHERE group_code = 'PI_CONTACT' AND label = '유선전화번호';
UPDATE code_values SET description = '전자우편 주소' WHERE group_code = 'PI_CONTACT' AND label = '이메일 주소';
UPDATE code_values SET description = '수신 가능한 전화번호 (휴대·유선 포함)' WHERE group_code = 'PI_CONTACT' AND label = '연락 가능한 전화번호';
UPDATE code_values SET description = '실거주지 또는 서비스 이용 장소 주소' WHERE group_code = 'PI_CONTACT' AND label = '주소(거주지, 설치장소, 배송지 등)';
UPDATE code_values SET description = '주소에 대응하는 5자리 우편번호' WHERE group_code = 'PI_CONTACT' AND label = '우편번호';

-- ── PI_IDENTITY_DOC ──────────────────────────────────────
UPDATE code_values SET description = '신분증 촬영 이미지 파일' WHERE group_code = 'PI_IDENTITY_DOC' AND label = '신분증 이미지';
UPDATE code_values SET description = '신분증에 기재된 정보 (성명, 번호, 발급일 등)' WHERE group_code = 'PI_IDENTITY_DOC' AND label = '신분증 기재사항';
UPDATE code_values SET description = '신분증이 발급된 날짜' WHERE group_code = 'PI_IDENTITY_DOC' AND label = '신분증 발급일자';
UPDATE code_values SET description = '신분증에 부착된 증명사진' WHERE group_code = 'PI_IDENTITY_DOC' AND label = '신분증 얼굴사진 원본';
UPDATE code_values SET description = '안면인식 알고리즘이 추출한 얼굴 특징점 데이터 (민감정보)' WHERE group_code = 'PI_IDENTITY_DOC' AND label = '얼굴사진으로부터 추출된 특징정보(안면인증)';
UPDATE code_values SET description = '미성년자 법정대리인의 신분증 기재 정보' WHERE group_code = 'PI_IDENTITY_DOC' AND label = '법정대리인 신분증 정보';
UPDATE code_values SET description = '가족관계증명서 등을 통한 가족 구성 정보' WHERE group_code = 'PI_IDENTITY_DOC' AND label = '가족관계 정보';

-- ── PI_DEVICE_LINE ───────────────────────────────────────
UPDATE code_values SET description = '스마트폰 제조사 및 모델명' WHERE group_code = 'PI_DEVICE_LINE' AND label = '단말기 모델명';
UPDATE code_values SET description = '단말기 고유 식별번호 (15자리)' WHERE group_code = 'PI_DEVICE_LINE' AND label = 'IMEI';
UPDATE code_values SET description = 'SIM 카드 고유 식별번호 (ICCID)' WHERE group_code = 'PI_DEVICE_LINE' AND label = 'USIM 번호';
UPDATE code_values SET description = '내장형 SIM(eSIM)의 고유 식별번호' WHERE group_code = 'PI_DEVICE_LINE' AND label = 'eSIM 번호';
UPDATE code_values SET description = '단말기 네트워크 인터페이스 고유 주소' WHERE group_code = 'PI_DEVICE_LINE' AND label = 'MAC Address';
UPDATE code_values SET description = '이동통신 서비스에 사용되는 전화번호' WHERE group_code = 'PI_DEVICE_LINE' AND label = '회선번호';
UPDATE code_values SET description = '회선 또는 서비스 최초 개통 날짜' WHERE group_code = 'PI_DEVICE_LINE' AND label = '개통일자';
UPDATE code_values SET description = '서비스 가입 및 해지 일자, 총 이용 기간' WHERE group_code = 'PI_DEVICE_LINE' AND label = '가입일·해지일·가입기간';
UPDATE code_values SET description = '현재 또는 이전 이동통신사 정보' WHERE group_code = 'PI_DEVICE_LINE' AND label = '이동통신사 정보';

-- ── PI_PAYMENT_FIN ───────────────────────────────────────
UPDATE code_values SET description = '금융기관 예금 계좌번호' WHERE group_code = 'PI_PAYMENT_FIN' AND label = '계좌번호';
UPDATE code_values SET description = '신용·체크카드 번호 (16자리)' WHERE group_code = 'PI_PAYMENT_FIN' AND label = '카드번호';
UPDATE code_values SET description = '카드 사용 만료 연월' WHERE group_code = 'PI_PAYMENT_FIN' AND label = '카드 유효기간';
UPDATE code_values SET description = '계좌 또는 카드 명의자 정보' WHERE group_code = 'PI_PAYMENT_FIN' AND label = '예금주/카드소유자 정보';
UPDATE code_values SET description = '카드 결제 비밀번호의 앞 2자리' WHERE group_code = 'PI_PAYMENT_FIN' AND label = '카드 비밀번호 앞 2자리';
UPDATE code_values SET description = '요금 납부자의 인적사항 (이름, 생년월일, 주소)' WHERE group_code = 'PI_PAYMENT_FIN' AND label = '납부자 정보(이름, 생년월일, 주소)';
UPDATE code_values SET description = '결제 또는 납부한 금액' WHERE group_code = 'PI_PAYMENT_FIN' AND label = '수납금액';
UPDATE code_values SET description = '결제 일시·금액·가맹점 등 거래 내역' WHERE group_code = 'PI_PAYMENT_FIN' AND label = '결제기록';
UPDATE code_values SET description = '소액결제 서비스 인증 비밀번호' WHERE group_code = 'PI_PAYMENT_FIN' AND label = '휴대폰 결제 비밀번호';

-- ── PI_CREDIT ────────────────────────────────────────────
UPDATE code_values SET description = '대출·카드·공과금 등 연체 내역' WHERE group_code = 'PI_CREDIT' AND label = '연체정보';
UPDATE code_values SET description = '신용평가에 활용되는 종합 신용도 정보' WHERE group_code = 'PI_CREDIT' AND label = '신용도 판단정보';
UPDATE code_values SET description = '대출·보증·신용카드 이용 내역' WHERE group_code = 'PI_CREDIT' AND label = '신용거래정보';
UPDATE code_values SET description = '신용평가기관이 산정한 신용등급 또는 점수' WHERE group_code = 'PI_CREDIT' AND label = '신용등급';
UPDATE code_values SET description = '신용보험 가입 적격 여부' WHERE group_code = 'PI_CREDIT' AND label = '신용보험 가입 가능 여부';
UPDATE code_values SET description = '신용정보 조회 일시 및 기관' WHERE group_code = 'PI_CREDIT' AND label = '신용정보 조회 기록';
UPDATE code_values SET description = '개인회생·워크아웃 등 채무조정 이력' WHERE group_code = 'PI_CREDIT' AND label = '채무조정 관련 정보';

-- ── PI_SERVICE_USAGE ─────────────────────────────────────
UPDATE code_values SET description = '통화 발신자 전화번호' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '발신번호';
UPDATE code_values SET description = '통화 수신자 전화번호' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '수신번호(통화 상대방 번호 포함)';
UPDATE code_values SET description = '통화 시작 및 종료 시각' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '통화시각';
UPDATE code_values SET description = '통화·문자·데이터 사용 횟수' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '사용도수';
UPDATE code_values SET description = '앱·웹 서비스 접속 및 이용 내역' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '서비스 이용기록';
UPDATE code_values SET description = '서버 접속 일시·IP·브라우저 등 접속 기록' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '접속로그';
UPDATE code_values SET description = '웹 브라우저에 저장되는 사용자 식별 데이터' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '쿠키';
UPDATE code_values SET description = '인터넷 접속 시 할당된 IP 주소' WHERE group_code = 'PI_SERVICE_USAGE' AND label = 'IP 주소';
UPDATE code_values SET description = '서비스 이용정지 일시 및 사유' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '이용정지 기록';
UPDATE code_values SET description = '요금 산정에 사용된 이용 데이터' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '요금 과금 관련 데이터';
UPDATE code_values SET description = '통화·SMS·데이터 사용 통계' WHERE group_code = 'PI_SERVICE_USAGE' AND label = '전화/문자/데이터 사용량';

-- ── PI_LOCATION ──────────────────────────────────────────
UPDATE code_values SET description = '스마트폰 위치를 나타내는 좌표 정보' WHERE group_code = 'PI_LOCATION' AND label = '이동전화 단말기 위치정보';
UPDATE code_values SET description = '이동통신 기지국 기반 위치 정보' WHERE group_code = 'PI_LOCATION' AND label = '기지국 위치정보';
UPDATE code_values SET description = '위성항법장치(GPS)가 수집한 위도·경도 좌표' WHERE group_code = 'PI_LOCATION' AND label = 'GPS 정보';
UPDATE code_values SET description = 'Zigbee 무선통신 접속점 기반 위치 정보' WHERE group_code = 'PI_LOCATION' AND label = 'Zigbee AP 정보';
UPDATE code_values SET description = 'Wi-Fi 접속점(SSID·BSSID) 기반 위치 정보' WHERE group_code = 'PI_LOCATION' AND label = 'Wi-Fi AP 정보';
UPDATE code_values SET description = 'RFID 리더가 수집한 태그 위치 정보' WHERE group_code = 'PI_LOCATION' AND label = 'RFID 태그 정보';
UPDATE code_values SET description = 'USIM 칩을 통해 확인된 위치 정보' WHERE group_code = 'PI_LOCATION' AND label = 'USIM 관련 위치정보';

-- ── PI_WELFARE ───────────────────────────────────────────
UPDATE code_values SET description = '국가보훈처 발급 국가유공자 확인 서류 정보' WHERE group_code = 'PI_WELFARE' AND label = '국가유공자 증명 정보';
UPDATE code_values SET description = '복지 요금 감면을 위한 자격 증명 정보' WHERE group_code = 'PI_WELFARE' AND label = '복지할인 증명 정보';
UPDATE code_values SET description = '장애인복지법에 따른 장애인 등록 여부' WHERE group_code = 'PI_WELFARE' AND label = '장애인 여부';
UPDATE code_values SET description = '국민기초생활보장법에 따른 수급자 여부' WHERE group_code = 'PI_WELFARE' AND label = '기초생활수급자 여부';
UPDATE code_values SET description = '차상위계층 해당 여부 및 감면 자격 정보' WHERE group_code = 'PI_WELFARE' AND label = '차상위계층 여부 등 감면 자격 확인 정보';

-- ── PI_NUMBER_PORT ───────────────────────────────────────
UPDATE code_values SET description = '번호이동 전 사용 중인 이동통신사' WHERE group_code = 'PI_NUMBER_PORT' AND label = '기존 통신사 정보';
UPDATE code_values SET description = '현재 가입된 이동통신 요금제명' WHERE group_code = 'PI_NUMBER_PORT' AND label = '이용 중 요금제';
UPDATE code_values SET description = '단말기 할부금 잔액 및 위약금 정보' WHERE group_code = 'PI_NUMBER_PORT' AND label = '위약금/할부 정보';
UPDATE code_values SET description = '번호이동 시 소멸되는 포인트·혜택 정보' WHERE group_code = 'PI_NUMBER_PORT' AND label = '소멸 예정 혜택 정보';
UPDATE code_values SET description = '번호이동 신청 과정 ARS 통화 녹음 파일' WHERE group_code = 'PI_NUMBER_PORT' AND label = '번호이동 ARS 녹음파일';

-- ── PI_MINOR_AGENT ───────────────────────────────────────
UPDATE code_values SET description = '미성년자의 법적 보호자 성명' WHERE group_code = 'PI_MINOR_AGENT' AND label = '법정대리인 이름';
UPDATE code_values SET description = '법정대리인의 전화번호 또는 이메일' WHERE group_code = 'PI_MINOR_AGENT' AND label = '법정대리인 연락처';
UPDATE code_values SET description = '법정대리인과 미성년자의 가족관계 정보' WHERE group_code = 'PI_MINOR_AGENT' AND label = '가족관계 정보';
UPDATE code_values SET description = '법정대리인의 신분증 기재 정보' WHERE group_code = 'PI_MINOR_AGENT' AND label = '법정대리인 신분증 정보';

-- ── PI_MARKETING ─────────────────────────────────────────
UPDATE code_values SET description = '마케팅 정보 수신에 동의한 휴대전화번호' WHERE group_code = 'PI_MARKETING' AND label = '휴대전화번호';
UPDATE code_values SET description = '마케팅 정보 수신에 동의한 이메일 주소' WHERE group_code = 'PI_MARKETING' AND label = '이메일';
UPDATE code_values SET description = '우편 마케팅 수신 주소' WHERE group_code = 'PI_MARKETING' AND label = '주소';
UPDATE code_values SET description = '마케팅 푸시 발송에 사용되는 단말기 고유 ID' WHERE group_code = 'PI_MARKETING' AND label = '단말식별번호(Device ID)';
UPDATE code_values SET description = '앱 푸시 알림 발송에 사용되는 FCM/APNS 토큰' WHERE group_code = 'PI_MARKETING' AND label = 'PUSH 토큰';
UPDATE code_values SET description = '고객 상담 과정에서 수집된 개인정보' WHERE group_code = 'PI_MARKETING' AND label = '상담 내용에 입력한 개인정보';

-- ── PI_SENSITIVE ─────────────────────────────────────────
UPDATE code_values SET description = '안면인식 시스템이 추출한 얼굴의 특징점 데이터 (개인정보보호법 민감정보)' WHERE group_code = 'PI_SENSITIVE' AND label = '얼굴 특징정보(안면인증 특징점)';
