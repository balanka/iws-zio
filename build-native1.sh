#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?usage: $0 <version> <arch>}"
ARCH="${2:?usage: $0 <version> <arch>}"
NETWORK="${NETWORK:-iws-zio_node-network}"
ENV_FILE="${ENV_FILE:-IWS_DEV.env}"
IMAGE="bateka/iws-api:${VERSION}-native-${ARCH}"

VERSION="$VERSION" sbt clean assembly
JAR="target/scala-3.9.0/iws-api-assembly-${VERSION}.jar"
[[ -f "$JAR" ]] || { echo "missing $JAR" >&2; exit 1; }

docker buildx build \
  --platform "linux/${ARCH}" \
  -f Dockerfile.native-build \
  -t "$IMAGE" \
  --load .

docker rm -f iws-api-smoke 2>/dev/null || true
cid=$(docker run -d --name iws-api-smoke \
  -p 8080:8080 \
  --env-file "$ENV_FILE" \
  --network "$NETWORK" \
  "$IMAGE")
trap 'docker rm -f iws-api-smoke >/dev/null 2>&1 || true' EXIT

for i in $(seq 1 30); do
  curl -fsS http://localhost:8080/health >/dev/null 2>&1 && break
  sleep 1
  [[ $i -eq 30 ]] && { docker logs "$cid"; exit 1; }
done
echo "✅ $IMAGE ready and healthy"
