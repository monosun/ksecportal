export const CATEGORIES = [
  { key: 'fintech',    label: '금융·핀테크',      color: 'bg-blue-100 text-blue-700' },
  { key: 'telecom',   label: '통신',              color: 'bg-purple-100 text-purple-700' },
  { key: 'ecommerce', label: '전자상거래·플랫폼',  color: 'bg-orange-100 text-orange-700' },
  { key: 'cloud',     label: '클라우드·SaaS',      color: 'bg-teal-100 text-teal-700' },
  { key: 'other',     label: '기타 업종',          color: 'bg-gray-100 text-gray-600' },
]

export const INDUSTRIES = [
  // ─────────────────────────────────────────────────────────────────
  // 금융·핀테크
  // ─────────────────────────────────────────────────────────────────
  {
    id: 1,
    name: '전자금융업 (PG·선불전자지급수단·전자지급결제대행업)',
    category: 'fintech',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '표준 개인정보보호 지침',                                        ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/표준개인정보보호지침' },
      { name: '전자금융거래법',                                               ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/전자금융거래법' },
      { name: '전자금융거래법 시행령',                                         ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/전자금융거래법시행령' },
      { name: '전자금융감독규정',                                             ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/전자금융감독규정' },
      { name: '전자금융감독규정 시행세칙',                                     ministry: '금융감독원',         type: '시행세칙', url: 'https://www.law.go.kr/행정규칙/전자금융감독규정시행세칙' },
      { name: '신용정보의 이용 및 보호에 관한 법률',                           ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률' },
      { name: '신용정보의 이용 및 보호에 관한 법률 시행령',                     ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률시행령' },
      { name: '신용정보업 감독규정',                                           ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/신용정보업감독규정' },
      { name: '금융소비자보호에 관한 법률',                                    ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/금융소비자보호에관한법률' },
      { name: '금융소비자보호에 관한 법률 시행령',                              ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/금융소비자보호에관한법률시행령' },
      { name: '특정 금융거래정보의 보고 및 이용 등에 관한 법률 (AML)',          ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/특정금융거래정보의보고및이용등에관한법률' },
      { name: '특정 금융거래정보의 보고 및 이용 등에 관한 법률 시행령',          ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/특정금융거래정보의보고및이용등에관한법률시행령' },
    ]
  },
  {
    id: 2,
    name: '간편결제 (Pay 서비스)',
    category: 'fintech',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '전자금융거래법',                                               ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/전자금융거래법' },
      { name: '전자금융거래법 시행령',                                         ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/전자금융거래법시행령' },
      { name: '전자금융감독규정',                                             ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/전자금융감독규정' },
      { name: '신용정보의 이용 및 보호에 관한 법률',                           ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률' },
      { name: '신용정보의 이용 및 보호에 관한 법률 시행령',                     ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률시행령' },
      { name: '금융소비자보호에 관한 법률',                                    ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/금융소비자보호에관한법률' },
      { name: '금융소비자보호에 관한 법률 시행령',                              ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/금융소비자보호에관한법률시행령' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률',                      ministry: '공정거래위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률 시행령',               ministry: '공정거래위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률시행령' },
    ]
  },
  {
    id: 3,
    name: '마이데이터 사업자',
    category: 'fintech',
    laws: [
      { name: '신용정보의 이용 및 보호에 관한 법률',                           ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률' },
      { name: '신용정보의 이용 및 보호에 관한 법률 시행령',                     ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률시행령' },
      { name: '신용정보업 감독규정',                                           ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/신용정보업감독규정' },
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '전자금융거래법',                                               ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/전자금융거래법' },
      { name: '전자금융거래법 시행령',                                         ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/전자금융거래법시행령' },
      { name: '전자금융감독규정',                                             ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/전자금융감독규정' },
    ]
  },
  {
    id: 4,
    name: '은행',
    category: 'fintech',
    laws: [
      { name: '은행법',                                                      ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/은행법' },
      { name: '은행법 시행령',                                               ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/은행법시행령' },
      { name: '은행업 감독규정',                                             ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/은행업감독규정' },
      { name: '은행업 감독업무 시행세칙',                                     ministry: '금융감독원',         type: '시행세칙', url: 'https://www.law.go.kr/행정규칙/은행업감독업무시행세칙' },
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '신용정보의 이용 및 보호에 관한 법률',                           ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률' },
      { name: '신용정보의 이용 및 보호에 관한 법률 시행령',                     ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률시행령' },
      { name: '신용정보업 감독규정',                                           ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/신용정보업감독규정' },
      { name: '전자금융거래법',                                               ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/전자금융거래법' },
      { name: '전자금융거래법 시행령',                                         ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/전자금융거래법시행령' },
      { name: '전자금융감독규정',                                             ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/전자금융감독규정' },
      { name: '금융소비자보호에 관한 법률',                                    ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/금융소비자보호에관한법률' },
      { name: '금융소비자보호에 관한 법률 시행령',                              ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/금융소비자보호에관한법률시행령' },
    ]
  },
  {
    id: 5,
    name: '증권사',
    category: 'fintech',
    laws: [
      { name: '자본시장과 금융투자업에 관한 법률',                             ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/자본시장과금융투자업에관한법률' },
      { name: '자본시장과 금융투자업에 관한 법률 시행령',                       ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/자본시장과금융투자업에관한법률시행령' },
      { name: '금융투자업 규정',                                               ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/금융투자업규정' },
      { name: '금융투자업 규정 시행세칙',                                       ministry: '금융감독원',         type: '시행세칙', url: 'https://www.law.go.kr/행정규칙/금융투자업규정시행세칙' },
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '신용정보의 이용 및 보호에 관한 법률',                           ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률' },
      { name: '신용정보의 이용 및 보호에 관한 법률 시행령',                     ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률시행령' },
      { name: '전자금융거래법',                                               ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/전자금융거래법' },
      { name: '전자금융거래법 시행령',                                         ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/전자금융거래법시행령' },
      { name: '전자금융감독규정',                                             ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/전자금융감독규정' },
    ]
  },
  {
    id: 6,
    name: '보험사',
    category: 'fintech',
    laws: [
      { name: '보험업법',                                                    ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/보험업법' },
      { name: '보험업법 시행령',                                             ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/보험업법시행령' },
      { name: '보험업법 시행규칙',                                           ministry: '금융위원회',         type: '시행규칙', url: 'https://www.law.go.kr/법령/보험업법시행규칙' },
      { name: '보험업 감독규정',                                             ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/보험업감독규정' },
      { name: '보험업 감독업무 시행세칙',                                     ministry: '금융감독원',         type: '시행세칙', url: 'https://www.law.go.kr/행정규칙/보험업감독업무시행세칙' },
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '신용정보의 이용 및 보호에 관한 법률',                           ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률' },
      { name: '신용정보의 이용 및 보호에 관한 법률 시행령',                     ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률시행령' },
      { name: '전자금융거래법',                                               ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/전자금융거래법' },
      { name: '전자금융거래법 시행령',                                         ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/전자금융거래법시행령' },
      { name: '전자금융감독규정',                                             ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/전자금융감독규정' },
    ]
  },
  {
    id: 7,
    name: '여신전문금융회사 (카드사·캐피탈)',
    category: 'fintech',
    laws: [
      { name: '여신전문금융업법',                                            ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/여신전문금융업법' },
      { name: '여신전문금융업법 시행령',                                      ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/여신전문금융업법시행령' },
      { name: '여신전문금융업 감독규정',                                      ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/여신전문금융업감독규정' },
      { name: '신용정보의 이용 및 보호에 관한 법률',                           ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률' },
      { name: '신용정보의 이용 및 보호에 관한 법률 시행령',                     ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/신용정보의이용및보호에관한법률시행령' },
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '전자금융거래법',                                               ministry: '금융위원회',         type: '법령',    url: 'https://www.law.go.kr/법령/전자금융거래법' },
      { name: '전자금융거래법 시행령',                                         ministry: '금융위원회',         type: '시행령',  url: 'https://www.law.go.kr/법령/전자금융거래법시행령' },
      { name: '전자금융감독규정',                                             ministry: '금융위원회',         type: '규정',    url: 'https://www.law.go.kr/행정규칙/전자금융감독규정' },
    ]
  },

  // ─────────────────────────────────────────────────────────────────
  // 통신
  // ─────────────────────────────────────────────────────────────────
  {
    id: 8,
    name: 'MVNO (알뜰폰)',
    category: 'telecom',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '전기통신사업법',                                               ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/전기통신사업법' },
      { name: '전기통신사업법 시행령',                                         ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/전기통신사업법시행령' },
      { name: '통신비밀보호법',                                               ministry: '법무부',             type: '법령',    url: 'https://www.law.go.kr/법령/통신비밀보호법' },
      { name: '통신비밀보호법 시행령',                                         ministry: '법무부',             type: '시행령',  url: 'https://www.law.go.kr/법령/통신비밀보호법시행령' },
      { name: '위치정보의 보호 및 이용 등에 관한 법률',                        ministry: '방송통신위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/위치정보의보호및이용등에관한법률' },
      { name: '위치정보의 보호 및 이용 등에 관한 법률 시행령',                  ministry: '방송통신위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/위치정보의보호및이용등에관한법률시행령' },
    ]
  },
  {
    id: 9,
    name: 'MNO (이동통신사)',
    category: 'telecom',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '전기통신사업법',                                               ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/전기통신사업법' },
      { name: '전기통신사업법 시행령',                                         ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/전기통신사업법시행령' },
      { name: '통신비밀보호법',                                               ministry: '법무부',             type: '법령',    url: 'https://www.law.go.kr/법령/통신비밀보호법' },
      { name: '통신비밀보호법 시행령',                                         ministry: '법무부',             type: '시행령',  url: 'https://www.law.go.kr/법령/통신비밀보호법시행령' },
      { name: '위치정보의 보호 및 이용 등에 관한 법률',                        ministry: '방송통신위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/위치정보의보호및이용등에관한법률' },
      { name: '위치정보의 보호 및 이용 등에 관한 법률 시행령',                  ministry: '방송통신위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/위치정보의보호및이용등에관한법률시행령' },
      { name: '전파법',                                                      ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/전파법' },
      { name: '전파법 시행령',                                               ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/전파법시행령' },
      { name: '전파법 시행규칙',                                             ministry: '과학기술정보통신부', type: '시행규칙', url: 'https://www.law.go.kr/법령/전파법시행규칙' },
    ]
  },

  // ─────────────────────────────────────────────────────────────────
  // 전자상거래·플랫폼
  // ─────────────────────────────────────────────────────────────────
  {
    id: 10,
    name: '온라인 쇼핑몰 (전자상거래)',
    category: 'ecommerce',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률',                     ministry: '공정거래위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률 시행령',              ministry: '공정거래위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률시행령' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률 시행규칙',            ministry: '공정거래위원회',     type: '시행규칙', url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률시행규칙' },
      { name: '표시·광고의 공정화에 관한 법률',                               ministry: '공정거래위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/표시·광고의공정화에관한법률' },
      { name: '표시·광고의 공정화에 관한 법률 시행령',                         ministry: '공정거래위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/표시·광고의공정화에관한법률시행령' },
      { name: '전자문서 및 전자거래 기본법',                                  ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/전자문서및전자거래기본법' },
      { name: '전자문서 및 전자거래 기본법 시행령',                            ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/전자문서및전자거래기본법시행령' },
      { name: '전자서명법',                                                  ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/전자서명법' },
      { name: '전자서명법 시행령',                                           ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/전자서명법시행령' },
    ]
  },
  {
    id: 11,
    name: '온라인 플랫폼 (중개 플랫폼)',
    category: 'ecommerce',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률',                     ministry: '공정거래위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률 시행령',              ministry: '공정거래위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률시행령' },
      { name: '표시·광고의 공정화에 관한 법률',                               ministry: '공정거래위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/표시·광고의공정화에관한법률' },
      { name: '표시·광고의 공정화에 관한 법률 시행령',                         ministry: '공정거래위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/표시·광고의공정화에관한법률시행령' },
      { name: '독점규제 및 공정거래에 관한 법률',                              ministry: '공정거래위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/독점규제및공정거래에관한법률' },
      { name: '독점규제 및 공정거래에 관한 법률 시행령',                        ministry: '공정거래위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/독점규제및공정거래에관한법률시행령' },
    ]
  },
  {
    id: 20,
    name: 'SNS / 커뮤니티',
    category: 'ecommerce',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '청소년보호법',                                                ministry: '여성가족부',         type: '법령',    url: 'https://www.law.go.kr/법령/청소년보호법' },
      { name: '청소년보호법 시행령',                                          ministry: '여성가족부',         type: '시행령',  url: 'https://www.law.go.kr/법령/청소년보호법시행령' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률',                     ministry: '공정거래위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률 시행령',              ministry: '공정거래위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률시행령' },
    ]
  },

  // ─────────────────────────────────────────────────────────────────
  // 클라우드·SaaS
  // ─────────────────────────────────────────────────────────────────
  {
    id: 12,
    name: 'SaaS 사업자',
    category: 'cloud',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '클라우드컴퓨팅 발전 및 이용자 보호에 관한 법률',                ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/클라우드컴퓨팅발전및이용자보호에관한법률' },
      { name: '클라우드컴퓨팅 발전 및 이용자 보호에 관한 법률 시행령',          ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/클라우드컴퓨팅발전및이용자보호에관한법률시행령' },
      { name: '클라우드컴퓨팅 발전 및 이용자 보호에 관한 법률 시행규칙',        ministry: '과학기술정보통신부', type: '시행규칙', url: 'https://www.law.go.kr/법령/클라우드컴퓨팅발전및이용자보호에관한법률시행규칙' },
      { name: '전자문서 및 전자거래 기본법',                                  ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/전자문서및전자거래기본법' },
      { name: '전자문서 및 전자거래 기본법 시행령',                            ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/전자문서및전자거래기본법시행령' },
    ]
  },
  {
    id: 13,
    name: '클라우드 서비스 제공사업자 (CSP)',
    category: 'cloud',
    laws: [
      { name: '클라우드컴퓨팅 발전 및 이용자 보호에 관한 법률',                ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/클라우드컴퓨팅발전및이용자보호에관한법률' },
      { name: '클라우드컴퓨팅 발전 및 이용자 보호에 관한 법률 시행령',          ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/클라우드컴퓨팅발전및이용자보호에관한법률시행령' },
      { name: '클라우드컴퓨팅 발전 및 이용자 보호에 관한 법률 시행규칙',        ministry: '과학기술정보통신부', type: '시행규칙', url: 'https://www.law.go.kr/법령/클라우드컴퓨팅발전및이용자보호에관한법률시행규칙' },
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
    ]
  },
  {
    id: 14,
    name: '공공 클라우드 사업자',
    category: 'cloud',
    laws: [
      { name: '클라우드컴퓨팅 발전 및 이용자 보호에 관한 법률',                ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/클라우드컴퓨팅발전및이용자보호에관한법률' },
      { name: '클라우드컴퓨팅 발전 및 이용자 보호에 관한 법률 시행령',          ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/클라우드컴퓨팅발전및이용자보호에관한법률시행령' },
      { name: '클라우드컴퓨팅 발전 및 이용자 보호에 관한 법률 시행규칙',        ministry: '과학기술정보통신부', type: '시행규칙', url: 'https://www.law.go.kr/법령/클라우드컴퓨팅발전및이용자보호에관한법률시행규칙' },
      { name: '전자정부법',                                                  ministry: '행정안전부',         type: '법령',    url: 'https://www.law.go.kr/법령/전자정부법' },
      { name: '전자정부법 시행령',                                           ministry: '행정안전부',         type: '시행령',  url: 'https://www.law.go.kr/법령/전자정부법시행령' },
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
    ]
  },

  // ─────────────────────────────────────────────────────────────────
  // 기타 업종
  // ─────────────────────────────────────────────────────────────────
  {
    id: 15,
    name: '온라인 게임',
    category: 'other',
    laws: [
      { name: '게임산업진흥에 관한 법률',                                    ministry: '문화체육관광부',     type: '법령',    url: 'https://www.law.go.kr/법령/게임산업진흥에관한법률' },
      { name: '게임산업진흥에 관한 법률 시행령',                              ministry: '문화체육관광부',     type: '시행령',  url: 'https://www.law.go.kr/법령/게임산업진흥에관한법률시행령' },
      { name: '게임산업진흥에 관한 법률 시행규칙',                            ministry: '문화체육관광부',     type: '시행규칙', url: 'https://www.law.go.kr/법령/게임산업진흥에관한법률시행규칙' },
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '청소년보호법',                                                ministry: '여성가족부',         type: '법령',    url: 'https://www.law.go.kr/법령/청소년보호법' },
      { name: '청소년보호법 시행령',                                          ministry: '여성가족부',         type: '시행령',  url: 'https://www.law.go.kr/법령/청소년보호법시행령' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률',                     ministry: '공정거래위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률' },
      { name: '전자상거래 등에서의 소비자보호에 관한 법률 시행령',              ministry: '공정거래위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/전자상거래등에서의소비자보호에관한법률시행령' },
    ]
  },
  {
    id: 16,
    name: '의료 플랫폼',
    category: 'other',
    laws: [
      { name: '의료법',                                                      ministry: '보건복지부',         type: '법령',    url: 'https://www.law.go.kr/법령/의료법' },
      { name: '의료법 시행령',                                               ministry: '보건복지부',         type: '시행령',  url: 'https://www.law.go.kr/법령/의료법시행령' },
      { name: '의료법 시행규칙',                                             ministry: '보건복지부',         type: '시행규칙', url: 'https://www.law.go.kr/법령/의료법시행규칙' },
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '생명윤리 및 안전에 관한 법률',                                 ministry: '보건복지부',         type: '법령',    url: 'https://www.law.go.kr/법령/생명윤리및안전에관한법률' },
      { name: '생명윤리 및 안전에 관한 법률 시행령',                           ministry: '보건복지부',         type: '시행령',  url: 'https://www.law.go.kr/법령/생명윤리및안전에관한법률시행령' },
      { name: '생명윤리 및 안전에 관한 법률 시행규칙',                         ministry: '보건복지부',         type: '시행규칙', url: 'https://www.law.go.kr/법령/생명윤리및안전에관한법률시행규칙' },
      { name: '전자서명법',                                                  ministry: '과학기술정보통신부', type: '법령',    url: 'https://www.law.go.kr/법령/전자서명법' },
      { name: '전자서명법 시행령',                                           ministry: '과학기술정보통신부', type: '시행령',  url: 'https://www.law.go.kr/법령/전자서명법시행령' },
    ]
  },
  {
    id: 17,
    name: '교육 플랫폼',
    category: 'other',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '초·중등교육법',                                              ministry: '교육부',             type: '법령',    url: 'https://www.law.go.kr/법령/초·중등교육법' },
      { name: '초·중등교육법 시행령',                                        ministry: '교육부',             type: '시행령',  url: 'https://www.law.go.kr/법령/초·중등교육법시행령' },
      { name: '고등교육법',                                                  ministry: '교육부',             type: '법령',    url: 'https://www.law.go.kr/법령/고등교육법' },
      { name: '고등교육법 시행령',                                           ministry: '교육부',             type: '시행령',  url: 'https://www.law.go.kr/법령/고등교육법시행령' },
    ]
  },
  {
    id: 18,
    name: '물류·배송 플랫폼',
    category: 'other',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '위치정보의 보호 및 이용 등에 관한 법률',                        ministry: '방송통신위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/위치정보의보호및이용등에관한법률' },
      { name: '위치정보의 보호 및 이용 등에 관한 법률 시행령',                  ministry: '방송통신위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/위치정보의보호및이용등에관한법률시행령' },
      { name: '화물자동차 운수사업법',                                        ministry: '국토교통부',         type: '법령',    url: 'https://www.law.go.kr/법령/화물자동차운수사업법' },
      { name: '화물자동차 운수사업법 시행령',                                  ministry: '국토교통부',         type: '시행령',  url: 'https://www.law.go.kr/법령/화물자동차운수사업법시행령' },
      { name: '화물자동차 운수사업법 시행규칙',                                ministry: '국토교통부',         type: '시행규칙', url: 'https://www.law.go.kr/법령/화물자동차운수사업법시행규칙' },
    ]
  },
  {
    id: 19,
    name: '모빌리티 플랫폼',
    category: 'other',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '위치정보의 보호 및 이용 등에 관한 법률',                        ministry: '방송통신위원회',     type: '법령',    url: 'https://www.law.go.kr/법령/위치정보의보호및이용등에관한법률' },
      { name: '위치정보의 보호 및 이용 등에 관한 법률 시행령',                  ministry: '방송통신위원회',     type: '시행령',  url: 'https://www.law.go.kr/법령/위치정보의보호및이용등에관한법률시행령' },
      { name: '여객자동차 운수사업법',                                        ministry: '국토교통부',         type: '법령',    url: 'https://www.law.go.kr/법령/여객자동차운수사업법' },
      { name: '여객자동차 운수사업법 시행령',                                  ministry: '국토교통부',         type: '시행령',  url: 'https://www.law.go.kr/법령/여객자동차운수사업법시행령' },
      { name: '여객자동차 운수사업법 시행규칙',                                ministry: '국토교통부',         type: '시행규칙', url: 'https://www.law.go.kr/법령/여객자동차운수사업법시행규칙' },
    ]
  },
  {
    id: 21,
    name: 'AI 서비스 사업자',
    category: 'other',
    laws: [
      { name: '개인정보 보호법',                                             ministry: '개인정보보호위원회', type: '법령',    url: 'https://www.law.go.kr/법령/개인정보보호법' },
      { name: '개인정보 보호법 시행령',                                       ministry: '개인정보보호위원회', type: '시행령',  url: 'https://www.law.go.kr/법령/개인정보보호법시행령' },
      { name: '개인정보의 안전성 확보조치 기준',                               ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/개인정보의안전성확보조치기준' },
      { name: '표준 개인정보보호 지침',                                        ministry: '개인정보보호위원회', type: '고시',    url: 'https://www.law.go.kr/행정규칙/표준개인정보보호지침' },
    ]
  },
]
