
# Use a lightweight Java 17 runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create a non-root application user
RUN useradd --system --no-create-home appuser

# Copy the JAR already built and tested by Jenkins
COPY --chown=appuser:appuser target/student-task-planner.jar app.jar

# Run the application as a non-root user
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
