# Anthropic OSS Scanner build image for KSecPortal.
# The scanner builds this with the repository root as the build context; network access is only
# available during this build, so every dependency (Gradle, npm, DB server) is fetched here.
FROM debian:bookworm
ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    GRADLE_VERSION=8.10 \
    GRADLE_USER_HOME=/opt/gradle-home \
    JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64

RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl gnupg unzip git python3 jq procps \
      mariadb-server mariadb-client \
    && rm -rf /var/lib/apt/lists/*

# JDK 17 (Temurin, same as CI) and Node 20 (same as frontend/Dockerfile)
RUN curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
      | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main" \
      > /etc/apt/sources.list.d/adoptium.list \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get update && apt-get install -y --no-install-recommends temurin-17-jdk nodejs \
    && rm -rf /var/lib/apt/lists/*

# The repo has no Gradle wrapper; use Gradle 8.10 as backend/Dockerfile and CI do
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt && rm /tmp/gradle.zip \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle

# scanner contract: the checkout lives inside the image, at /src
COPY . /src
WORKDIR /src

# Backend: Spring Boot executable jar -> backend/build/libs/*.jar (no test sources exist in the repo)
RUN cd /src/backend && gradle bootJar --no-daemon -x test

# Frontend: Vue 3 SPA -> frontend/dist (help docs are copied in the same way as frontend/Dockerfile)
RUN cd /src/frontend \
    && npm install --no-audit --no-fund \
    && mkdir -p public/help \
    && cp /src/docs/user-manual.md /src/docs/security-cases-basic.md /src/docs/security-cases-executive.md public/help/ \
    && cp -r /src/docs/images public/help/ \
    && npm run build

# Local database for running the API offline (MariaDB stands in for MySQL 8; Hibernate ddl-auto creates the schema)
RUN service mariadb start \
    && mysql -e "CREATE DATABASE secportal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; \
                 CREATE USER 'secportal'@'localhost' IDENTIFIED BY 'secportal123'; \
                 GRANT ALL ON secportal.* TO 'secportal'@'localhost'; FLUSH PRIVILEGES;" \
    && service mariadb stop

# Helper to run the API at http://localhost:8080/api (see .oss-scanner/threat_model.md)
RUN printf '%s\n' '#!/bin/sh' \
      'service mariadb start' \
      'cd /src/backend && exec java -jar build/libs/*.jar "$@"' \
      > /usr/local/bin/run-ksecportal \
    && chmod +x /usr/local/bin/run-ksecportal
