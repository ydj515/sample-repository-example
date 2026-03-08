FROM eclipse-temurin:21-jdk AS build

ARG APP_MODULE

WORKDIR /workspace

COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY oidc-common/build.gradle.kts oidc-common/build.gradle.kts
COPY app1/build.gradle.kts app1/build.gradle.kts
COPY app2/build.gradle.kts app2/build.gradle.kts

RUN chmod +x gradlew

COPY oidc-common oidc-common
COPY app1 app1
COPY app2 app2

RUN ./gradlew :${APP_MODULE}:bootJar --no-daemon

FROM eclipse-temurin:21-jre

ARG APP_MODULE

WORKDIR /app

COPY --from=build /workspace/${APP_MODULE}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
