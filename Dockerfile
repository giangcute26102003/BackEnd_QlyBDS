# Multi-stage build for Spring Boot application
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Copy project files
COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY src src

# Build application
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Stage 2: Runtime with minimal JRE
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Expose port
EXPOSE 8088

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
