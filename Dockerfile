# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM alpine:3.20 AS newrelic
RUN apk add --no-cache curl unzip \
    && mkdir -p /newrelic \
    && curl -fsSL https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip -o /tmp/newrelic-java.zip \
    && unzip -q /tmp/newrelic-java.zip -d /tmp \
    && cp /tmp/newrelic/newrelic.jar /newrelic/newrelic.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk upgrade --no-cache

RUN addgroup -S -g 10001 appgroup && adduser -S -u 10001 -G appgroup appuser

COPY --from=build /app/target/*.jar app.jar
COPY --from=newrelic /newrelic/newrelic.jar /app/newrelic/newrelic.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
