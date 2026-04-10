# Chezan Fitness Application - Production Dockerfile
# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Production stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S chezan && adduser -S chezan -G chezan
USER chezan

# Copy the built JAR
COPY --from=build /app/target/chezan-fitness-1.0-SNAPSHOT.jar app.jar

# Create logs directory
RUN mkdir -p logs && chown -R chezan:chezan logs

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=UTC
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]