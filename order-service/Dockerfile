FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY inventory-service/pom.xml ./inventory-service/pom.xml
COPY order-service/pom.xml ./order-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml

RUN mvn -pl order-service -am -DskipTests dependency:go-offline

COPY order-service/src ./order-service/src

RUN mvn -pl order-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine-3.24

RUN apk add --no-cache curl \
    && addgroup -S orderflow \
    && adduser -S orderflow -G orderflow

WORKDIR /app

COPY --from=build /workspace/order-service/target/*.jar /app/app.jar

USER orderflow

EXPOSE 8083

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD curl --fail --silent http://localhost:8083/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]