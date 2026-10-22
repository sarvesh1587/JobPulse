# --- Build stage: compile and package with Maven on JDK 21 ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
# Download dependencies in their own layer, so code changes don't re-download them.
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Runtime stage: JRE only, non-root user ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 jobpulse && mkdir -p /app/data/resumes && chown -R jobpulse /app
COPY --from=build /app/target/jobpulse-backend-*.jar app.jar
USER jobpulse
ENV SPRING_PROFILES_ACTIVE=prod \
    RESUME_STORAGE_DIR=/app/data/resumes \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fs http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
