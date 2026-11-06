#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?usage: $0 <version> <arch>  (arch: arm64 | amd64)}"
ARCH="${2:?usage: $0 <version> <arch>  (arch: arm64 | amd64)}"

case "$ARCH" in
  arm64) PLATFORM=linux/arm64 ;;
  amd64) PLATFORM=linux/amd64 ;;
  *) echo "unsupported arch: $ARCH (use arm64 or amd64)" >&2; exit 1 ;;
esac

# 1. Build the assembly jar (note: your build.sbt reads APP_VERSION, not VERSION)
APP_VERSION="$VERSION" sbt clean assembly

JAR="target/scala-3.9.0/iws-api-assembly-${VERSION}.jar"
[[ -f "$JAR" ]] || { echo "missing $JAR" >&2; exit 1; }

# 2. Build the distroless native image for the requested platform
IMAGE="bateka/iws-api:${VERSION}-native-${ARCH}-distroless"

docker buildx build \
  --platform "$PLATFORM" \
  -f Dockerfile.native-distroless \
  -t "$IMAGE" \
  --load .

# 3. Smoke test against the existing compose network
echo "=== smoke test ==="
docker rm -f iws-api-smoke 2>/dev/null || true

cid=$(docker run -d --name iws-api-smoke \
  -p 8080:8080 \
  --env-file IWS_DEV.env \
  --network iws-zio_node-network \
  "$IMAGE")

trap 'docker rm -f iws-api-smoke >/dev/null 2>&1 || true' EXIT

for i in $(seq 1 30); do
  if curl -fsS http://localhost:8080/health >/dev/null 2>&1; then
    echo "✅ $IMAGE healthy"
    break
  fi
  sleep 1
  if [[ $i -eq 30 ]]; then
    echo "❌ health check failed" >&2
    docker logs "$cid" >&2
    exit 1
  fi
done
