# Multi-stage Docker build for Spring Boot application
# Stage 1: Build the JAR with Maven & JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image with JRE 17
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=build /app/target/ai-chatbot-0.0.1-SNAPSHOT.jar app.jar

# Cloud providers (Render, Railway, Cloud Run) inject PORT dynamically
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot application
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
