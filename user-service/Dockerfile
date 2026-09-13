FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY inventory-service/pom.xml ./inventory-service/pom.xml
COPY order-service/pom.xml ./order-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml

RUN mvn -pl user-service -am -DskipTests dependency:go-offline

COPY user-service/src ./user-service/src

RUN mvn -pl user-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine-3.24

RUN apk add --no-cache curl \
    && addgroup -S orderflow \
    && adduser -S orderflow -G orderflow

WORKDIR /app

COPY --from=build /workspace/user-service/target/*.jar /app/app.jar

USER orderflow

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD curl --fail --silent http://localhost:8081/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]