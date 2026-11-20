#!/usr/bin/env bash
set -euo pipefail

# Semantic Versioning generator for H P i O (never uses NSiO format).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION_FILE="${ROOT_DIR}/version.properties"

if [[ ! -f "$VERSION_FILE" ]]; then
  echo "ERROR: version.properties not found at $VERSION_FILE" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "$VERSION_FILE"

for num_var in VERSION_MAJOR VERSION_MINOR VERSION_PATCH; do
  if ! [[ "${!num_var:-}" =~ ^[0-9]+$ ]]; then
    echo "ERROR: $num_var must be a non-negative integer" >&2
    exit 1
  fi
done

OFFSET="${VERSION_CODE_OFFSET:-0}"
RUN_ORDINAL="${GITHUB_RUN_NUMBER:-$(git rev-list --count HEAD 2>/dev/null || echo 1)}"
if ! [[ "$RUN_ORDINAL" =~ ^[1-9][0-9]*$ ]]; then
  echo "ERROR: RUN_ORDINAL must be a positive integer, got: $RUN_ORDINAL" >&2
  exit 1
fi

VERSION_CODE=$(( OFFSET + RUN_ORDINAL ))
VERSION_NAME="${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"
TAG="v${VERSION_NAME}"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "tag=$TAG"
    echo "version_name=$VERSION_NAME"
    echo "version_code=$VERSION_CODE"
  } >> "$GITHUB_OUTPUT"
else
  echo "tag=$TAG"
  echo "version_name=$VERSION_NAME"
  echo "version_code=$VERSION_CODE"
fi

echo "H P i O Version: $TAG (versionName=$VERSION_NAME, versionCode=$VERSION_CODE)"
