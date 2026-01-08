# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR C:\\workspace_bank-sa
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR C:\\workspace_bank-sa

# Add non-root user for security
RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser

# Copy the built JAR file
COPY --from=build C:\workspace_bank-sa\\target\*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
