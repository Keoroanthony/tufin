# =============================================================================
# Stage 1: builder
# =============================================================================
# eclipse-temurin is the official Adoptium OpenJDK distribution — well-maintained
# and widely trusted. We use the JDK here (not JRE) because we need the full
# compiler and Gradle toolchain to build the application.
# '21-jdk-jammy' pins us to Java 21 on Ubuntu 22.04 (Jammy). Pinning the OS
# variant avoids silent base-image changes between builds.
FROM eclipse-temurin:21-jdk-jammy AS builder

# WORKDIR creates the directory if it doesn't exist and sets it as the working
# directory for all subsequent RUN, COPY, and CMD instructions in this stage.
WORKDIR /app

# ── Dependency caching layer ──────────────────────────────────────────────────
# Copy only the files Gradle needs to resolve dependencies, BEFORE copying src/.
# Docker caches each layer; this layer is only invalidated when build.gradle or
# settings.gradle changes — not every time source code changes.
# Copying gradle/ first ensures the wrapper JAR and properties are present so
# that ./gradlew can bootstrap itself without downloading Gradle twice.
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./

# Ensure the wrapper script is executable (git may not preserve file modes on
# some systems, and the Dockerfile must work on any developer's machine).
RUN chmod +x gradlew

# Download all compile and runtime dependencies into the Gradle cache.
# --no-daemon: don't start a persistent Gradle daemon; appropriate for a
#              one-shot container build where the daemon would be killed anyway.
# --quiet:     suppresses download progress noise in build output.
RUN ./gradlew dependencies --no-daemon --quiet

# ── Source compile ────────────────────────────────────────────────────────────
# Copy source only after dependencies are cached. A code-only change hits this
# layer and everything below it, but not the dependency-download layer above.
COPY src/ src/

# bootJar produces a single self-contained "fat JAR" containing the application
# class files and all dependency JARs. It is the standard Spring Boot
# deployment artefact.
RUN ./gradlew bootJar --no-daemon --quiet

# =============================================================================
# Stage 2: runtime
# =============================================================================
# Switch to the JRE image — no compiler, no javac, no Gradle. The JRE is
# roughly 200 MB smaller than the JDK and reduces the attack surface.
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# ── Non-root user ─────────────────────────────────────────────────────────────
# Running as root inside a container is a security risk: a container escape
# would give the attacker root on the host. Creating a dedicated system user
# and group (no login shell, no home directory) follows the principle of
# least privilege.
# Installing curl in the same RUN layer keeps the layer count and size minimal;
# apt caches are removed immediately to avoid bloating the image.
RUN groupadd --system appgroup \
    && useradd --system --gid appgroup appuser \
    && apt-get update -qq \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy only the compiled fat JAR from the builder stage. Nothing from the
# builder (JDK, Gradle cache, source code) is included in this image.
COPY --from=builder /app/build/libs/*.jar app.jar

# Switch to the non-root user before the process starts.
USER appuser

# EXPOSE documents the port this service listens on. It does not publish the
# port — that is controlled by docker-compose or `docker run -p`. It is
# metadata for operators and tools like docker-compose that read it.
EXPOSE 8080

# ── JVM tuning ────────────────────────────────────────────────────────────────
# -XX:MaxRAMPercentage=75.0: Without this flag, the JVM calculates its default
# heap size based on the HOST's total memory, not the container's cgroup limit.
# This can cause the JVM to allocate more heap than the container allows,
# leading to an OOM-kill from the OS rather than a clean OutOfMemoryError.
# Setting to 75% leaves headroom for off-heap memory (metaspace, threads, GC).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
