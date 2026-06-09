FROM gradle:jdk21-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
COPY resource-service/build.gradle resource-service/

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :resource-service:dependencies --no-daemon

COPY resource-service/src resource-service/src
RUN ./gradlew :resource-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/resource-service/build/libs/*.jar app.jar

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]