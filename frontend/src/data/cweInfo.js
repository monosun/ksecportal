/**
 * 소스 취약점 점검(SAST)에서 사용하는 CWE 설명 사전.
 *
 * SastEngine 의 룰이 부여하는 CWE 만 담는다. 새 룰을 추가하면 여기에도 항목을 추가한다.
 * 각 항목: 명칭 / 무엇인지 / 무엇이 위험한지 / 어떻게 조치하는지.
 */
export const CWE_INFO = {
  'CWE-89': {
    name: 'SQL 인젝션 (Improper Neutralization of Special Elements used in an SQL Command)',
    summary: '외부 입력을 SQL 문자열에 그대로 붙여 질의를 만들면, 입력에 섞인 SQL 구문이 그대로 실행됩니다.',
    impact: '인증 우회, 전체 테이블 열람·변조·삭제, DB 계정 권한에 따라 서버 장악까지 이어질 수 있습니다.',
    fix: [
      '값은 반드시 바인딩 파라미터(PreparedStatement, JPA 파라미터)로 전달합니다.',
      '테이블·컬럼처럼 파라미터로 넘길 수 없는 식별자는 허용 목록(화이트리스트)이나 형식 검증 후 사용합니다.',
      'ORM 사용 시에도 네이티브 쿼리 문자열 결합을 피합니다.',
    ],
  },
  'CWE-78': {
    name: 'OS 명령 인젝션 (Improper Neutralization of Special Elements used in an OS Command)',
    summary: '외부 입력이 셸 명령 문자열에 포함되면 공격자가 명령을 덧붙여 실행할 수 있습니다.',
    impact: '서버에서 임의 명령 실행 — 데이터 유출, 백도어 설치, 내부망 침투로 확대됩니다.',
    fix: [
      '셸을 거치지 않고 실행 파일과 인자를 배열로 분리해 전달합니다(ProcessBuilder 인자 배열).',
      '입력값은 허용 목록으로 검증하고, 셸 메타문자(; | & $ ` 등)를 허용하지 않습니다.',
      '가능하면 명령 실행 대신 라이브러리 API 를 사용합니다.',
    ],
  },
  'CWE-95': {
    name: '동적 코드 실행 (Improper Neutralization of Directives in Dynamically Evaluated Code)',
    summary: 'eval, new Function 등으로 문자열을 코드로 실행하면 입력이 그대로 코드가 됩니다.',
    impact: '임의 코드 실행 — 세션 탈취, 데이터 조작, 클라이언트 측이면 XSS 와 동일한 피해가 발생합니다.',
    fix: [
      'eval / new Function / setTimeout(문자열) 사용을 제거합니다.',
      'JSON 은 JSON.parse 로, 계산식은 전용 파서로 처리합니다.',
      '동적 실행이 불가피하면 입력을 코드가 아닌 데이터로 다루는 구조로 바꿉니다.',
    ],
  },
  'CWE-284': {
    name: '부적절한 접근통제 (Improper Access Control)',
    summary: '모든 요청을 permitAll 로 열어두는 등 인가 검사가 빠진 지점이 있습니다.',
    impact: '비인가 사용자가 관리 기능·타인 데이터에 접근할 수 있습니다.',
    fix: [
      '기본은 차단(deny by default)으로 두고 공개가 필요한 경로만 예외로 허용합니다.',
      '서버 측에서 역할·소유자 검사를 수행합니다(화면 숨김만으로는 부족).',
      '메뉴 권한과 API 권한을 각각 점검합니다.',
    ],
  },
  'CWE-295': {
    name: '인증서 검증 미흡 (Improper Certificate Validation)',
    summary: 'TLS 인증서·호스트명 검증을 끄면 통신 상대가 진짜인지 확인하지 못합니다.',
    impact: '중간자 공격으로 자격증명·개인정보가 평문처럼 노출되고 응답이 위조될 수 있습니다.',
    fix: [
      '인증서·호스트명 검증을 절대 비활성화하지 않습니다(TrustAllCerts, rejectUnauthorized:false 등 금지).',   // sast:ignore TLS 설정이 아니라 화면에 보여 주는 조치 안내 문구다
      '사설 CA 를 쓰면 해당 CA 를 신뢰 저장소에 등록합니다.',
      '테스트용 우회 코드가 운영 빌드에 포함되지 않도록 분리합니다.',
    ],
  },
  'CWE-327': {
    name: '취약한 암호 알고리즘 사용 (Use of a Broken or Risky Cryptographic Algorithm)',
    summary: 'MD5·SHA-1·DES·RC4·ECB 모드처럼 안전성이 깨진 알고리즘을 사용하고 있습니다.',
    impact: '해시 충돌·복호화가 현실적으로 가능해 무결성·기밀성이 보장되지 않습니다.',
    fix: [
      '해시는 SHA-256 이상, 비밀번호는 bcrypt·Argon2 등 전용 알고리즘을 사용합니다.',
      '대칭키는 AES-256 GCM 등 인증된 암호 모드를 사용합니다(ECB 금지).',
      '레거시 데이터는 재암호화·재해싱 계획을 세웁니다.',
    ],
  },
  'CWE-330': {
    name: '부적합한 난수 사용 (Use of Insufficiently Random Values)',
    summary: '보안 목적에 비암호학적 난수(Math.random, java.util.Random)를 사용했습니다.',
    impact: '토큰·인증코드·무작위 추출 결과를 예측당해 우회·부정이 가능합니다.',
    fix: [
      '자바는 SecureRandom, 브라우저는 crypto.getRandomValues / crypto.randomUUID 를 사용합니다.',
      '모듈로 연산으로 범위를 줄일 때 편향이 생기지 않도록 처리합니다.',
      '화면 표시용 키처럼 보안과 무관한 용도는 예외로 두되 사유를 남깁니다.',
    ],
  },
  'CWE-352': {
    name: 'CSRF (Cross-Site Request Forgery)',
    summary: 'CSRF 보호가 비활성화되어 있어 다른 사이트에서 사용자의 권한으로 요청을 보낼 수 있습니다.',
    impact: '사용자가 모르는 사이에 설정 변경·데이터 삭제·권한 부여가 실행됩니다.',
    fix: [
      '세션 쿠키 기반 인증이라면 CSRF 토큰을 사용합니다.',
      '쿠키에 SameSite=Lax/Strict 를 지정합니다.',
      'Bearer 토큰만 사용하고 쿠키 인증이 없다면 비활성화 사유를 명시적으로 문서화합니다.',
    ],
  },
  'CWE-489': {
    name: '운영 환경의 디버그 기능 (Active Debug Code)',
    summary: '디버그 모드가 켜진 설정이 있습니다.',
    impact: '상세 오류·스택트레이스·내부 경로가 노출되어 공격 정보로 활용됩니다.',
    fix: [
      '운영 프로파일에서는 디버그를 끕니다.',
      '오류 응답에 내부 정보를 담지 않습니다.',
      '디버그 설정은 환경변수로 분리합니다.',
    ],
  },
  'CWE-502': {
    name: '안전하지 않은 역직렬화 (Deserialization of Untrusted Data)',
    summary: '신뢰할 수 없는 데이터를 객체로 역직렬화하고 있습니다.',
    impact: '역직렬화 과정에서 임의 코드가 실행되어 서버가 장악될 수 있습니다.',
    fix: [
      '외부 입력은 JSON 등 데이터 포맷으로만 받고 객체 역직렬화를 피합니다.',
      '불가피하면 허용 클래스 목록을 지정합니다(YAML 은 SafeLoader).',
      '역직렬화 대상에 무결성 서명을 적용합니다.',
    ],
  },
  'CWE-611': {
    name: 'XXE (Improper Restriction of XML External Entity Reference)',
    summary: 'XML 파서가 외부 엔티티·DTD 를 처리하면 문서가 서버 파일이나 내부 URL 을 참조할 수 있습니다.',
    impact: '서버 파일 유출, 내부망 스캔(SSRF), 파서 자원 고갈이 발생합니다.',
    fix: [
      'DOCTYPE 선언 자체를 금지합니다(disallow-doctype-decl = true).',
      '외부 일반/파라미터 엔티티와 외부 DTD 로드를 비활성화합니다.',
      'ACCESS_EXTERNAL_DTD / ACCESS_EXTERNAL_SCHEMA 를 빈 값으로 두고 XInclude 를 끕니다.',
    ],
  },
  'CWE-798': {
    name: '자격증명 하드코딩 (Use of Hard-coded Credentials)',
    summary: '비밀번호·API 키·개인키가 소스코드에 직접 들어 있습니다.',
    impact: '저장소 접근자·유출된 코드로 즉시 인증을 우회할 수 있고 교체도 어렵습니다.',
    fix: [
      '환경변수·시크릿 저장소로 옮기고 소스에서 제거합니다.',
      '이미 커밋된 비밀은 이력에서 제거하고 반드시 폐기·재발급합니다.',
      '시크릿 스캔을 CI 에 넣어 재유입을 막습니다.',
    ],
  },
  'CWE-942': {
    name: '과도한 교차 도메인 허용 (Permissive Cross-domain Policy)',
    summary: 'CORS 가 모든 출처(*)를 허용하도록 설정되어 있습니다.',
    impact: '임의 사이트가 인증된 API 응답을 읽어 데이터가 유출될 수 있습니다.',
    fix: [
      '허용 출처를 명시적으로 나열합니다.',
      '자격증명 포함 요청(credentials)에는 와일드카드를 쓸 수 없습니다.',
      '허용 메서드·헤더도 최소한으로 제한합니다.',
    ],
  },
}

/** 문자열에서 CWE 식별자 추출 (예: "A03:2021-Injection · CWE-89" → "CWE-89") */
export function extractCwe(text) {
  const m = /CWE-\d+/i.exec(text || '')
  return m ? m[0].toUpperCase() : null
}

export function cweUrl(cweId) {
  const num = (cweId || '').replace(/\D/g, '')
  return num ? `https://cwe.mitre.org/data/definitions/${num}.html` : 'https://cwe.mitre.org/'
}
