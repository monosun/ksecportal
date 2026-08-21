# 오피스 문서(PPT·DOC 등) → PDF 변환 서버
# Gotenberg = LibreOffice 를 감싼 변환 HTTP API. 백엔드는 /forms/libreoffice/convert 로 파일을 보낸다.
#
# 기본 이미지에는 한글 글꼴이 없어 한글 슬라이드가 □ 로 깨진다.
# 나눔·본고딕(Noto CJK)을 넣고, 사내 문서가 쓰는 윈도우 글꼴을 여기에 잇는다.
FROM gotenberg/gotenberg:8

USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
        fonts-nanum \
        fonts-nanum-extra \
        fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*
COPY korean-fonts.conf /etc/fonts/conf.d/99-korean-fonts.conf
RUN fc-cache -f
USER gotenberg
