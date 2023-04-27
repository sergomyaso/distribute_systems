FROM openjdk:17

WORKDIR /app

# Copy the jar file and the XSD schema file to the container
COPY build/libs/crackhash-manager-0.0.1-SNAPSHOT.jar /app/manager.jar

# Expose the port on which the service will run
EXPOSE 8080

CMD ["java", "-jar", "/app/manager.jar"]