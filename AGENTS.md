# Repository workflow

- After each completed modification, run relevant checks, commit, and push to `main` in `layland/GyeonggiCash`, as requested by the owner. Do not force push or overwrite concurrent changes.
- Each push to `main` triggers `.github/workflows/release.yml`, which builds, lints, verifies APK signing, and publishes a GitHub Release with the APK, SHA-256 checksum, and certificate information.
- Keep the release signing key stable. Never commit keystores, signing passwords, or other credentials. Read them from GitHub Actions Secrets.
- If signing secrets or Actions permissions block a release, report the actual failure and the remaining setup. Do not substitute a debug key or silently generate a new release key.
- Run `bash tools/test.sh` for scheduling changes. When Android SDK is available, also run the relevant Gradle build and lint checks. Do not claim a build or device test succeeded without running it.
