FROM openjdk:17

# Set the working directory
WORKDIR /app

# Copy the build artifacts to the container
COPY build/libs/crackhash-worker-0.0.1-SNAPSHOT.jar /app/worker.jar

# Expose the port on which the manager will listen for HTTP requests
EXPOSE 8080

# Start the application
CMD ["java", "-jar", "/app/worker.jar"]