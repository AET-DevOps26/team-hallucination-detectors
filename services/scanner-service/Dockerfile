# syntax=docker/dockerfile:1.7
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
