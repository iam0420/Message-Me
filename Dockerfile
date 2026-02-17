# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r chatapp && useradd -r -g chatapp chatapp

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R chatapp:chatapp /app

USER chatapp

EXPOSE 8080

# Use shell form so environment variables are resolved
ENTRYPOINT ["sh", "-c", "java -jar \
  -Dserver.port=${PORT:-8080} \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker} \
  -Dspring.datasource.url=${SPRING_DATASOURCE_URL} \
  -Dspring.datasource.username=${SPRING_DATASOURCE_USERNAME} \
  -Dspring.datasource.password=${SPRING_DATASOURCE_PASSWORD} \
  -Dspring.data.redis.host=${SPRING_DATA_REDIS_HOST:-localhost} \
  -Dspring.data.redis.port=${SPRING_DATA_REDIS_PORT:-6379} \
  -Xmx512m -Xms256m \
  app.jar"]