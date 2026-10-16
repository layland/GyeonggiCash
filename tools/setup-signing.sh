#!/usr/bin/env bash
# Run once on your own computer, not on an untrusted shared machine.
set -euo pipefail
cd "$(dirname "$0")/.."
for program in gh keytool openssl base64; do
  command -v "$program" >/dev/null || { echo "Install $program first." >&2; exit 1; }
done
gh auth status
signing_dir="${1:-$PWD/signing}"
mkdir -p "$signing_dir"
chmod 700 "$signing_dir"
if [[ -e "$signing_dir/release.jks" || -e "$signing_dir/password.txt" ]]; then
  echo 'Signing backup already exists. Do not replace your existing release key.' >&2
  exit 1
fi
umask 077
export SIGNING_PASSWORD
SIGNING_PASSWORD=$(openssl rand -hex 32)
keytool -genkeypair -keystore "$signing_dir/release.jks" -storetype JKS \
  -storepass:env SIGNING_PASSWORD -keypass:env SIGNING_PASSWORD \
  -alias release -keyalg RSA -keysize 3072 -validity 10000 \
  -dname 'CN=GyeonggiCash Release'
printf '%s' "$SIGNING_PASSWORD" > "$signing_dir/password.txt"
base64 < "$signing_dir/release.jks" | gh secret set ANDROID_KEYSTORE_BASE64 --repo layland/GyeonggiCash
printf '%s' "$SIGNING_PASSWORD" | gh secret set ANDROID_KEYSTORE_PASSWORD --repo layland/GyeonggiCash
printf '%s' "$SIGNING_PASSWORD" | gh secret set ANDROID_KEY_PASSWORD --repo layland/GyeonggiCash
printf '%s' release | gh secret set ANDROID_KEY_ALIAS --repo layland/GyeonggiCash
unset SIGNING_PASSWORD
echo "Secrets saved. Back up $signing_dir securely; never commit its contents."
gh workflow run release.yml --repo layland/GyeonggiCash --ref main
