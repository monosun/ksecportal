FROM gradle:8.10-jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    apk add --no-cache su-exec
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh && \
    chown appuser:appgroup app.jar
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
