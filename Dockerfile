# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first so they're cached across builds unless pom.xml changes
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/maintenance-system-1.0.0.jar app.jar

# Render sets $PORT at runtime; application.properties already reads server.port=${PORT:8080}
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar", "-b"]