#!/bin/bash
set -e

# Parse arguments
PLATFORM=${1:-linux/amd64}  # optional second argument for platform
VERSION=${2:-2.5.0}
JAR_NAME="iws-zio-assembly-${VERSION}.jar"

echo "Building version ${VERSION} for platform ${PLATFORM}"

# Build the fat JAR
sbt "set version := \"${VERSION}\"" assembly

# Copy the JAR to the project root as app.jar
cp "target/scala-3.8.3/${JAR_NAME}" app.jar

# Build Docker image with platform support
docker buildx build --platform "${PLATFORM}" -t "iws-api:${VERSION}" --load .

# Tag Docker image
docker image tag "iws-api:${VERSION}" "bateka/iws-api:${VERSION}"
echo "Done: iws-api:${VERSION} for ${PLATFORM}"
