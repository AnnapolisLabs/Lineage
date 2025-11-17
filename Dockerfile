# Multi-stage build for optimized Lineage container
# Builds both frontend and backend into a single deployable image

# ============================================================================
# Stage 1: Build Frontend (Vue.js/Vite)
# ============================================================================
FROM node:20-alpine AS frontend-builder

WORKDIR /frontend

# Copy package files and install dependencies
COPY frontend/package*.json ./
RUN npm ci --prefer-offline --no-audit

# Copy frontend source and build (skip type-checking, tests run separately)
COPY frontend/ ./
RUN npx vite build

# ============================================================================
# Stage 2: Build Backend (Spring Boot/Gradle)
# ============================================================================
FROM gradle:8.10.0-jdk21-jammy AS backend-builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Copy frontend build output to Spring Boot static resources
COPY --from=frontend-builder /frontend/dist ./src/main/resources/static

# Build the application
RUN ./gradlew bootJar --no-daemon -x test

# ============================================================================
# Stage 3: Runtime Image (Production)
# ============================================================================
FROM eclipse-temurin:21-jre-jammy

# Install curl for healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=backend-builder /app/build/libs/*.jar app.jar

# Create directory for application data
RUN mkdir -p /app/data && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM tuning for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]