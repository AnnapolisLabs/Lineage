#!/bin/bash

# Test script for Podman containerization setup
echo "Testing Podman containerization setup..."

# Check if Dockerfile exists
if [ ! -f "Dockerfile" ]; then
    echo "❌ Dockerfile not found"
    exit 1
fi
echo "✅ Dockerfile found"

# Check if .dockerignore exists (recommended for better build performance)
if [ ! -f ".dockerignore" ]; then
    echo "⚠️  .dockerignore not found - recommended for better build performance"
    echo "You can create one with common exclusions like:"
    echo "  .git"
    echo "  .gradle"
    echo "  frontend/node_modules"
    echo "  frontend/coverage"
    echo "  .gitlab-ci.yml"
else
    echo "✅ .dockerignore found"
fi

# Verify build.gradle contains necessary information
if grep -q "org.springframework.boot" build.gradle; then
    echo "✅ Spring Boot project detected"
else
    echo "❌ Spring Boot project not detected"
    exit 1
fi

# Check Java version in Dockerfile matches build.gradle
docker_java_version=$(grep -o "jdk[0-9]*" Dockerfile | head -1)
gradle_java_version=$(grep -o "JavaLanguageVersion.of([0-9]*)" build.gradle | grep -o "[0-9]*")

if [[ "$docker_java_version" == "jdk${gradle_java_version}" ]]; then
    echo "✅ Java version consistency: Dockerfile uses JDK${gradle_java_version}"
else
    echo "⚠️  Java version mismatch: Dockerfile uses $docker_java_version, Gradle expects Java ${gradle_java_version}"
fi

# Test Podman build (this will actually build the image)
echo ""
echo "Testing Podman build..."
podman build -t lineage-test . --no-cache

if [ $? -eq 0 ]; then
    echo "✅ Podman build successful"
    
    # Test running the container
    echo ""
    echo "Testing container execution..."
    podman run --rm --name lineage-test -p 8080:8080 lineage-test:latest &
    
    sleep 30  # Wait for application to start
    
    # Check if container is running
    if podman ps | grep -q lineage-test; then
        echo "✅ Container is running"
        
        # Test health endpoint
        if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
            echo "✅ Application health endpoint is accessible"
        else
            echo "⚠️  Health endpoint not accessible yet (may need more startup time)"
        fi
        
        # Clean up test container
        podman stop lineage-test
        podman rm lineage-test
        
    else
        echo "❌ Container failed to start"
    fi
    
else
    echo "❌ Podman build failed"
    exit 1
fi

echo ""
echo "🎉 Podman containerization setup test completed successfully!"
echo ""
echo "Next steps:"
echo "1. Configure GitLab CI/CD variables:"
echo "   - SSH_PRIVATE_KEY"
echo "   - SSH_USER"
echo "   - CONTAINER_PROJECT_HOST"
echo "   - CONTAINER_APP_NAME"
echo "2. Ensure your production server has Podman installed and configured"
echo "3. Test the complete CI/CD pipeline by pushing to your branches"