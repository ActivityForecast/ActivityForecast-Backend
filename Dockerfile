# 멀티 스테이지 빌드를 사용하여 최종 이미지 크기 최적화
FROM openjdk:17-jdk-slim as builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 래퍼와 빌드 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew bootJar --no-daemon

# 실행 스테이지
FROM openjdk:17-jre-slim

# 시스템 패키지 업데이트 및 필요한 도구 설치
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 애플리케이션 사용자 생성 (보안상 root 사용 지양)
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/test/health || exit 1

# JVM 옵션 및 프로파일 설정
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]