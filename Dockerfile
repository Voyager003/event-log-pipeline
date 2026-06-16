FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
