# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Prefetch dependencies to cache them in Docker layer
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/pulse-metrics-backend-0.0.1-SNAPSHOT.jar app.jar

# Download CockroachDB root certificate at build-time to establish secure SSL connection
RUN mkdir -p /root/.postgresql && \
    apt-get update && apt-get install -y curl && \
    curl -sS -o /root/.postgresql/root.crt 'https://cockroachlabs.cloud/clusters/1f8539d0-195c-410a-96a1-7874197dadd3/cert' && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
