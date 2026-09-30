# Contributing to Truedown Android

Thank you for your interest in contributing to Truedown Android! We welcome community contributions, bug reports, and improvements.

---

## 1. Code of Conduct & Core Rules

To keep the codebase clean, secure, and maintainable, all contributors are required to adhere to the following rules:

1. **Strict Localization (No Hardcoded Strings):** All user-facing strings **must** be declared in both `res/values/strings.xml` (Indonesian default) and `res/values-en/strings.xml` (English). Never hardcode user-facing text directly in Kotlin / Compose files.
2. **Permission Guardrails:** **Do not add new Android permissions** (`<uses-permission>`) without prior discussion and approval in a GitHub Issue. Truedown follows the least-privilege principle and uses MediaStore APIs for storage operations without requiring broad storage permissions.
3. **Provider Changes & Fixtures:** Any changes to the TikTok / TikWM provider logic should be accompanied by adapter unit tests using local JSON fixtures. Do not include personal tokens or sensitive URLs in test fixtures.
4. **Scope Control:** Features outside the MVP scope (e.g., Douyin support, custom SAF folders, background audio playback) must be proposed in a feature request issue first before submitting a PR.
5. **Never Commit Secrets or Keystores:** Do not commit `*.jks`, `*.keystore`, `.env`, API keys, passwords, or personal credentials. Never paste secrets or keystore values in issues or PR comments.
6. **No Release Signing in PRs:** Contributors do not sign release builds; signing and release tagging are managed automatically by repository maintainers.
7. **License Agreement:** By submitting a Pull Request, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).

---

## 2. Setting Up Your Development Environment

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer recommended.
- **JDK:** Version 17 or higher.
- **Android SDK:** Platform 35 and Android SDK Build-Tools 35.0.0.

### Getting the Code
```bash
git clone https://github.com/aryaxzell/truedown-android.git
cd truedown-android
```

---

## 3. Building and Testing

### Running Tests
Before submitting changes, ensure all unit tests pass:
```bash
./gradlew testDebugUnitTest
```

### Running Lint
Verify that the codebase complies with Android lint rules:
```bash
./gradlew lintDebug
```

### Building Release APKs Locally
To build unsigned release APK variants:
```bash
./gradlew assembleRelease
```
The output APKs will be generated in `app/build/outputs/apk/release/`.

---

## 4. Development Workflow & Pull Request Process

1. **Fork and Branch:** Fork the repository and create a new feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. **Make Focused Changes:** Keep changes atomic and well-documented. Avoid unrelated cosmetic or formatting changes.
3. **Test on Real / Emulated Devices:** Verify changes on an emulator or physical device running Android 10+ (API 29+).
4. **Submit a Pull Request:**
   - Open a PR against `main`.
   - Fill in the PR template checklist completely.
   - Attach screenshots or screen recordings for UI modifications.
   - Reference the relevant issue number (e.g., `Fixes #12`).
