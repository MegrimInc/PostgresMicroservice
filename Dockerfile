# Use a multi-stage build process

# Stage 1: Build
FROM maven:3.8.1-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/microservice-0.0.1-SNAPSHOT.jar /app/microservice.jar
ENTRYPOINT ["java", "-jar", "microservice.jar"]
