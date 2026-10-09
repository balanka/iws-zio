#!/bin/bash
set -e

# Use first argument as version, default to 2.5.0 if not provided
VERSION=${1:-2.5.0}
JAR_NAME="iws-zio-assembly-${VERSION}.jar"

echo "Building version ${VERSION}"

# Build the fat JAR (ensure sbt uses the same version)
sbt "set version := \"${VERSION}\"" assembly

# Copy the JAR to the project root as app.jar
cp "target/scala-3.8.3/${JAR_NAME}" app.jar

# Build Docker image with the version tag
docker build -t "iws-api:${VERSION}" .

# Tag Docker image with the version tag
docker image tag "iws-api:${VERSION}" "bateka/iws-api:${VERSION}"
echo "Done: iws-api:${VERSION}"
