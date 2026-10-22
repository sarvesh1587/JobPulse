# Single-image build for hosts that run one container (Render, Railway, Fly).
# The React app is built first and bundled into the Spring Boot jar, so the
# SPA and the API share one origin (the auth cookie stays first-party).
# docker-compose.yml uses the separate backend/ and frontend/ images instead.

# --- 1. Build the React app ---
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# --- 2. Build the Spring Boot jar with the SPA inside it ---
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -B -q package -DskipTests

# --- 3. Runtime: JRE only, non-root ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 jobpulse && mkdir -p /app/data/resumes && chown -R jobpulse /app
COPY --from=backend /backend/target/jobpulse-backend-*.jar app.jar
USER jobpulse
# Sized for small (512 MB) instances: cap the heap and use the lean serial GC.
ENV SPRING_PROFILES_ACTIVE=prod \
    RESUME_STORAGE_DIR=/app/data/resumes \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
