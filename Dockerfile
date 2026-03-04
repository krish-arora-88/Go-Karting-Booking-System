# Railway production build — context is repo root, so backend/ prefix is required.
# For local development, use docker-compose.yml which builds from backend/Dockerfile instead.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN apk add --no-cache maven && \
    mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/booking-service-1.0.0.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
