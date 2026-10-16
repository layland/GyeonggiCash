#!/usr/bin/env bash
# Register an existing signing key without generating or replacing it.
set -euo pipefail
if [[ $# -ne 2 ]]; then
  echo 'Usage: bash tools/import-signing.sh /path/to/existing.jks key-alias' >&2
  exit 1
fi
for program in gh keytool base64; do
  command -v "$program" >/dev/null || { echo "Install $program first." >&2; exit 1; }
done
[[ -f "$1" ]] || { echo 'Keystore file not found.' >&2; exit 1; }
gh auth status
IFS= read -r -s -p 'Keystore password: ' IMPORT_STORE_PASSWORD
printf '\n'
IFS= read -r -s -p 'Key password: ' IMPORT_KEY_PASSWORD
printf '\n'
export IMPORT_STORE_PASSWORD IMPORT_KEY_PASSWORD
trap 'unset IMPORT_STORE_PASSWORD IMPORT_KEY_PASSWORD' EXIT
keytool -list -keystore "$1" -alias "$2" -storepass:env IMPORT_STORE_PASSWORD >/dev/null
base64 < "$1" | gh secret set ANDROID_KEYSTORE_BASE64 --repo layland/GyeonggiCash
printf '%s' "$IMPORT_STORE_PASSWORD" | gh secret set ANDROID_KEYSTORE_PASSWORD --repo layland/GyeonggiCash
printf '%s' "$IMPORT_KEY_PASSWORD" | gh secret set ANDROID_KEY_PASSWORD --repo layland/GyeonggiCash
printf '%s' "$2" | gh secret set ANDROID_KEY_ALIAS --repo layland/GyeonggiCash
echo 'Existing signing key registered for GyeonggiCash.'
gh workflow run release.yml --repo layland/GyeonggiCash --ref main
