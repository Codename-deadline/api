FROM gradle:8.14.3 AS builder

COPY . /workspace/app

WORKDIR /workspace/app

RUN gradle build -Dproduction=true --no-daemon

FROM openjdk:21-jdk
COPY --from=builder /workspace/app/build/libs/*.jar /app/deadlines_api.jar

CMD ["java", "-jar", "/app/deadlines_api.jar"]
