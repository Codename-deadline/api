FROM gradle:9.2.1-jdk25 AS builder

COPY . /workspace/app

WORKDIR /workspace/app

RUN gradle build --no-daemon

FROM eclipse-temurin:25-jre-alpine-3.23
COPY --from=builder /workspace/app/build/libs/*.jar /app/deadlines_api.jar

CMD ["java", "-jar", "/app/deadlines_api.jar"]
