##Building using gradle
#FROM gradle:jdk8-corretto-al2023 as builder
#WORKDIR /app
#COPY . .
#RUN ["gradle" , "clean" , "bootJar" , "--no-daemon"]
##running
#FROM openjdk:26-slim
#WORKDIR /app
#COPY --from=builder /app/build/libs/*.jar /app/app.jar
#EXPOSE 3006
#ENTRYPOINT ["java", "-jar", "app.jar"]


FROM gradle:jdk21 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
RUN gradle clean build --no-daemon || true
COPY src ./src
RUN gradle bootJar --no-daemon

FROM gcr.io/distroless/java21-debian12
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar /app/app.jar
EXPOSE 3006
ENTRYPOINT ["java" , "-jar" , "/app/app.jar"]


