# Repository workflow

- After each completed modification, run relevant checks, commit, and push to `main` in `layland/GyeonggiCash`, as requested by the owner. Do not force push or overwrite concurrent changes.
- Each push to `main` triggers `.github/workflows/release.yml`, which builds, lints, verifies APK signing, and publishes a GitHub Release with the APK, SHA-256 checksum, and certificate information.
- Keep the release signing key stable. Never commit keystores, signing passwords, or other credentials. Read them from GitHub Actions Secrets.
- If signing secrets or Actions permissions block a release, report the actual failure and the remaining setup. Do not substitute a debug key or silently generate a new release key.
- Run `bash tools/test.sh` for scheduling changes. When Android SDK is available, also run the relevant Gradle build and lint checks. Do not claim a build or device test succeeded without running it.

- Signing uses the existing MyParkingAndroid secrets via its isolated `build/gyeonggicash` branch, following MyExercise. After each source push, push a new commit to that build branch to trigger its workflow. Do not change MyParkingAndroid master or build/myexercise. Keep GyeonggiCash source checkout on main.
- APKs must appear in layland/GyeonggiCash Releases. The publication workflow polls shared signed builds, checks SHA-256/signature/package/version, and republishes the original APK. Wait for build and publication results and provide the final APK link.
