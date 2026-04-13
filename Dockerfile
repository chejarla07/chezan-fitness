# Chezan Fitness Application - Production Dockerfile
# Build JAR locally first: ./mvnw clean package -DskipTests

# Production stage - uses standard eclipse-temurin which supports ARM64 and AMD64
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create non-root user for security
RUN groupadd -r chezan && useradd -r -g chezan chezan

# Copy the built JAR (build locally first)
COPY target/chezan-fitness-1.0-SNAPSHOT.jar app.jar

# Create logs directory
RUN mkdir -p logs && chown -R chezan:chezan /app

USER chezan

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=UTC
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]