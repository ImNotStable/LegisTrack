# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper and build files first for better caching
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Fix line endings for gradlew (convert Windows CRLF to Unix LF) and make executable
RUN apk add --no-cache dos2unix && \
    dos2unix gradlew && \
    chmod +x gradlew

# Pre-fetch dependencies
RUN ./gradlew --version && ./gradlew dependencies --no-daemon --quiet

# Copy source only
COPY src src
COPY config config

# Build the application
RUN ./gradlew bootJar --no-daemon --quiet

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

USER appuser

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimization for containers
ENV JVM_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -Xms512m -Xmx2g"

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar /app/app.jar"]
