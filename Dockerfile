#MEthod 1 -> simple build
#FROM amazoncorretto:8u462-alpine3.21-jre
#WORKDIR /app
#COPY build/libs/*.jar /app/app.jar
#EXPOSE 3006
#ENTRYPOINT ["java", "-jar", "/app/app.jar"]


#Method 2 -> multistage builder
#FROM gradle:jdk21-alpine AS builder
#WORKDIR /app
#COPY gradlew build.gradle settings.gradle ./
#COPY gradle gradle
#RUN ./gradlew dependencies --no-daemon
#COPY src ./src
#RUN ./gradlew clean bootJar --no-daemon
#
#FROM amazoncorretto:8u462-alpine3.21-jre
#WORKDIR /app
#COPY --from=builder /app/build/libs/*.jar /app/app.jar
#EXPOSE 3006
#ENTRYPOINT ["java", "-jar", "/app/app.jar"]



# ─────────────── BUILD ───────────────
FROM gradle:jdk21 AS builder
WORKDIR /app
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# ─────────────── EXTRACT ───────────────
FROM eclipse-temurin:latest AS extractor
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ─────────────── RUNTIME ───────────────
FROM eclipse-temurin:latest
WORKDIR /app
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/application/ ./
EXPOSE 3006
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
