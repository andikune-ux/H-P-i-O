#!/usr/bin/env bash
set -euo pipefail

# Automated consistent keystore & GitHub Actions Secrets setup for H P i O (Andik Une).
# Generates (or reuses) a single release keystore outside the Git repo, computes its
# SHA-256 certificate fingerprint, and uploads all 5 ANDIKUNE_* secrets to GitHub
# using GitHub CLI (`gh`) without printing secrets to stdout or committing keys to Git.

REPO="${1:-andikune-ux/H-P-i-O}"
KEY_DIR="${HOME}/.andikune-keys"
KEYSTORE_FILE="${KEY_DIR}/hpio-andikune-release.jks"
META_ENV_FILE="${KEY_DIR}/hpio-andikune-release.env"
KEY_ALIAS="andikune"

for cmd in keytool base64 gh openssl awk tr grep; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: Tool '$cmd' tidak ditemukan. Install terlebih dahulu sebelum menjalankan script ini." >&2
    exit 1
  fi
done

if ! gh auth status >/dev/null 2>&1; then
  echo "ERROR: GitHub CLI belum login. Jalankan 'gh auth login' terlebih dahulu." >&2
  exit 1
fi

mkdir -p "$KEY_DIR"
chmod 700 "$KEY_DIR"

if [[ -f "$KEYSTORE_FILE" ]]; then
  if [[ ! -f "$META_ENV_FILE" ]]; then
    echo "ERROR: Keystore sudah ada di $KEYSTORE_FILE tetapi $META_ENV_FILE tidak ditemukan." >&2
    exit 1
  fi
  # shellcheck disable=SC1090
  source "$META_ENV_FILE"
  echo "Menggunakan ulang keystore konsisten yang sudah ada: $KEYSTORE_FILE"
else
  STORE_PASS="$(openssl rand -hex 24)"
  KEY_PASS="$(openssl rand -hex 24)"

  umask 077
  keytool -genkeypair -noprompt \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA -keysize 4096 -validity 10000 \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS" \
    -dname "CN=Andik Une, O=Andik Une, C=ID" >/dev/null

  cat > "$META_ENV_FILE" <<EOF
STORE_PASS='$STORE_PASS'
KEY_PASS='$KEY_PASS'
EOF
  chmod 600 "$KEYSTORE_FILE" "$META_ENV_FILE"
  echo "Keystore rilis baru berhasil dibuat secara lokal (di luar repo Git): $KEYSTORE_FILE"
fi

CERT_SHA256="$(
  keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$KEY_ALIAS" -storepass "$STORE_PASS" \
    | awk -F'SHA256:' '/SHA256:/ {gsub(/[:[:space:]]/, "", $2); print tolower($2); exit}'
)"

if [[ -z "$CERT_SHA256" || ! "$CERT_SHA256" =~ ^[0-9a-f]{64}$ ]]; then
  echo "ERROR: Gagal mengekstrak SHA-256 certificate fingerprint yang valid." >&2
  exit 1
fi

BASE64_PAYLOAD="$(base64 < "$KEYSTORE_FILE" | tr -d '\r\n')"

printf '%s' "$BASE64_PAYLOAD" | gh secret set ANDIKUNE_KEYSTORE_BASE64 --repo "$REPO"
printf '%s' "$STORE_PASS" | gh secret set ANDIKUNE_KEYSTORE_PASSWORD --repo "$REPO"
printf '%s' "$KEY_ALIAS" | gh secret set ANDIKUNE_KEY_ALIAS --repo "$REPO"
printf '%s' "$KEY_PASS" | gh secret set ANDIKUNE_KEY_PASSWORD --repo "$REPO"
printf '%s' "$CERT_SHA256" | gh secret set ANDIKUNE_CERT_SHA256 --repo "$REPO"

echo "Sukses mengunggah 5 secrets ke $REPO:"
echo "- ANDIKUNE_KEYSTORE_BASE64"
echo "- ANDIKUNE_KEYSTORE_PASSWORD"
echo "- ANDIKUNE_KEY_ALIAS ($KEY_ALIAS)"
echo "- ANDIKUNE_KEY_PASSWORD"
echo "- ANDIKUNE_CERT_SHA256 ($CERT_SHA256)"
echo "Simpan backup folder '$KEY_DIR' di tempat aman agar semua update berikutnya tetap konsisten."
