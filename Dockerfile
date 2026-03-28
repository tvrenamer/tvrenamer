FROM eclipse-temurin:8-jdk

RUN apt-get update && apt-get install -y ant && rm -rf /var/lib/apt/lists/*

# Create a non-root user so that read-only file permission tests work correctly
# (root bypasses file permission checks, which breaks tests that expect moves to fail)
RUN useradd -m -u 1001 builder

# Download Ivy and place it where Ant auto-discovers it
ARG IVY_VERSION=2.5.2
RUN mkdir -p /home/builder/.ant/lib && \
    wget -q "https://repo1.maven.org/maven2/org/apache/ivy/ivy/${IVY_VERSION}/ivy-${IVY_VERSION}.jar" \
    -O "/home/builder/.ant/lib/ivy.jar" && \
    chown -R builder:builder /home/builder/.ant

USER builder
WORKDIR /workspace
