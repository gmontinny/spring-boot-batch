# Build stage
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy gradle wrapper and definition files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Grant execution rights on gradlew
RUN chmod +x ./gradlew

# Download dependencies (caching layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Package the application without running tests
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
