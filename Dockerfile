# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage - using non-alpine that supports ARM64
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r chatapp && useradd -r -g chatapp chatapp
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R chatapp:chatapp /app
USER chatapp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "-Xmx512m", "-Xms256m", "app.jar"]