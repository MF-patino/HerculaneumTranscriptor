# First stage: building the application with Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# If these files don't change, Docker will reuse the cached dependency layer.
COPY pom.xml .
COPY .mvn/ .mvn/

# Download all project dependencies.
RUN mvn dependency:go-offline

# Copy the rest of the application's source code
COPY src/ src/

# Build the application, creating the final JAR file. Skip tests in the Docker build.
RUN mvn package -DskipTests

# Second stage: running the application
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR file from the 'build' stage.
COPY --from=build /app/target/HerculaneumTranscriptor-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the application will run on
EXPOSE 8080

# The command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]