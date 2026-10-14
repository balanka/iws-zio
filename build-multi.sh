#!/bin/bash
PLATFORM=${1:-linux/amd64}
VERSION=${2:-2.5.0}
IMAGE_TAG="iws-api:${VERSION}-$(echo $PLATFORM | tr / -)"

docker buildx build --platform $PLATFORM --load -t $IMAGE_TAG .
# Usage:
# ./build.sh linux/amd64 2.5.4
# ./build.sh linux/arm64 2.5.4
