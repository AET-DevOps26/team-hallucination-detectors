# syntax=docker/dockerfile:1.7
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle --no-daemon clean bootJar -x test
# Zero-code distributed tracing: the OTel Java agent auto-instruments Spring MVC
# and outgoing HTTP clients, propagating W3C trace context across service calls.
RUN wget -q -O otel-agent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.29.0/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
COPY --from=build /workspace/otel-agent.jar otel-agent.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-jar", "/app/app.jar"]
