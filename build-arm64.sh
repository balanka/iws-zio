
#!/bin/bash
set -e

VERSION=${1:-2.5.0}
PLATFORM="linux/arm64"
IMAGE_TAG="iws-api:${VERSION}-arm64"

echo "Building for $PLATFORM as $IMAGE_TAG"

docker buildx build \
  --platform $PLATFORM \
  --load \
  -t $IMAGE_TAG \
  .

# Tag Docker image with the version tag
docker image tag "iws-api:${VERSION}" "bateka/iws-api:${VERSION}"
echo "Done: $IMAGE_TAG"
