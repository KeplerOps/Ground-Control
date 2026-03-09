FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S gc && adduser -S gc -G gc
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER gc
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
