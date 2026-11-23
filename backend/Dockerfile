# Stage 1: Build the Spring Boot application with Gradle + JDK 21.
FROM gradle:8.7-jdk21 AS builder

WORKDIR /app

# Copy only the files needed to resolve dependencies first (layer caching).
# Paths are relative to the build context (the repo root), since the CD pipeline
# runs `docker build -f backend/Dockerfile .`.
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/gradlew ./
COPY backend/gradle/ gradle/
COPY backend/src/ src/
# generateOpenApiModels resolves $projectDir/../docs/02-contracts/02-rest-api-spec.yaml
# (projectDir is the backend dir, i.e. /app here), so the contract lives at /docs.
COPY docs/ /docs/

# Make the wrapper executable and build the boot JAR (no tests: they need Docker).
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

# Stage 2: Minimal JRE runtime image.
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Copy the fat JAR from the builder stage.
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring Boot runs on 8080; virtual threads keep the port busy with cheap work.
EXPOSE 8080

# Production defaults; override via environment variables (Render / Docker).
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Dspring.threads.virtual.enabled=true"

ENTRYPOINT ["java", "-jar", "app.jar"]