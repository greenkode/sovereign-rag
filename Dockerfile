# Multi-stage build for Compilot AI
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom files for dependency resolution
COPY pom.xml .
COPY core-ms/pom.xml core-ms/
COPY core-ms/commons/pom.xml core-ms/commons/
COPY core-ms/auth/pom.xml core-ms/auth/
COPY core-ms/tenant/pom.xml core-ms/tenant/
COPY core-ms/guardrail/pom.xml core-ms/guardrail/
COPY core-ms/agent-core/pom.xml core-ms/agent-core/
COPY core-ms/client/pom.xml core-ms/client/
COPY core-ms/messaging/pom.xml core-ms/messaging/
COPY core-ms/messaging-adapter-sendgrid/pom.xml core-ms/messaging-adapter-sendgrid/
COPY core-ms/messaging-adapter-javamail/pom.xml core-ms/messaging-adapter-javamail/
COPY core-ms/support/pom.xml core-ms/support/
COPY core-ms/support-adapter-zoho/pom.xml core-ms/support-adapter-zoho/
COPY core-ms/support-adapter-freshdesk/pom.xml core-ms/support-adapter-freshdesk/
COPY core-ms/app/pom.xml core-ms/app/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY core-ms/ core-ms/

# Build the application
RUN mvn clean package -DskipTests -pl core-ms/app -am

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the JAR from builder
COPY --from=builder /app/core-ms/app/target/*.jar app.jar

# Expose application port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8000/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
