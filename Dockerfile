# Build Spring Boot fat-jar
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package spring-boot:repackage

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/loan-management-system-0.0.2.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
