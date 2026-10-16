# ==============================================================================
# Multi-stage Dockerfile for XCMailr
# Stage 1: Build application using OpenJDK 25 and Maven
# Stage 2: Minimal runtime image using Eclipse Temurin 25 JRE
# ==============================================================================

# ------------------------------------------------------------------------------
# Build Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# Install Maven
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

# Copy POM files and license header first to cache Maven dependency layers
COPY pom.xml ./
COPY xcmailr-client/pom.xml xcmailr-client/
COPY xcmailr-webapp/pom.xml xcmailr-webapp/
COPY xcmailr-load-test-suite/pom.xml xcmailr-load-test-suite/
COPY license-header.txt ./

# Copy source trees needed to build xcmailr-webapp and its internal dependencies
COPY xcmailr-client/src xcmailr-client/src
COPY xcmailr-webapp/src xcmailr-webapp/src

# Build the executable fat JAR for xcmailr-webapp
RUN mvn clean package -pl xcmailr-webapp -am -DskipTests

# ------------------------------------------------------------------------------
# Runtime Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runner

# Metadata
LABEL org.opencontainers.image.title="XCMailr" \
      org.opencontainers.image.description="Disposable and temporary email service for automated testing" \
      org.opencontainers.image.url="https://github.com/Xceptance/XCMailr" \
      org.opencontainers.image.source="https://github.com/Xceptance/XCMailr" \
      org.opencontainers.image.vendor="Xceptance Software Technologies GmbH"

# Create non-root user and group
RUN groupadd -g 10001 xcmailr && \
    useradd -u 10001 -g xcmailr -m -s /bin/bash xcmailr

WORKDIR /app

# Pre-create data and log directories with appropriate non-root ownership
RUN mkdir -p /data /app/log /data/log && \
    chown -R xcmailr:xcmailr /data /app

# Copy fat JAR from builder stage
COPY --from=builder --chown=xcmailr:xcmailr /workspace/xcmailr-webapp/target/xcmailr-webapp-*.jar /app/xcmailr.jar

# Declare volume mount point for persistent database and logs
VOLUME ["/data"]

# Default runtime configuration
ENV PORT=8080 \
    MBOX_PORT=25000 \
    SPRING_DATASOURCE_URL="jdbc:h2:/data/xcmailrDB;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE" \
    LOG_FILE="/data/log/xcmailr.log" \
    JAVA_OPTS="-Xmx512m"

# Expose Web HTTP port and SubEthaSMTP inbound port
EXPOSE 8080 25000

# Run as non-privileged user
USER xcmailr:xcmailr

# Execute application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/xcmailr.jar"]

