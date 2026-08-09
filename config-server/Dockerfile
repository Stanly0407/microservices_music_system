FROM gradle:jdk21-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
COPY config-server/build.gradle config-server/

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :config-server:dependencies --no-daemon

COPY config-server/src config-server/src
RUN ./gradlew :config-server:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/config-server/build/libs/*.jar app.jar

EXPOSE 8888

CMD ["java", "-jar", "app.jar"]
