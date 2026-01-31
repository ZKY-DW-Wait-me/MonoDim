# Repository Guidelines

## Project Structure & Module Organization
This is a single-module Android app. The main module lives in `app/`.
- Source code: `app/src/main/java/com/example/monodim/` (Kotlin).
- Android resources: `app/src/main/res/` (layouts, drawables, values, etc.).
- Unit tests: `app/src/test/java/com/example/monodim/`.
- Instrumented tests: `app/src/androidTest/java/com/example/monodim/`.
- Build outputs: `app/build/` (generated files and APKs).

## Build, Test, and Development Commands
Run from the repository root:
- `./gradlew assembleDebug` — builds a debug APK.
- `./gradlew assembleRelease` — builds a release APK (unsigned).
- `./gradlew test` — runs JVM unit tests in `app/src/test`.
- `./gradlew connectedAndroidTest` — runs instrumented tests on a connected device/emulator.
- `./gradlew clean` — clears build outputs if you hit stale artifacts.

## Coding Style & Naming Conventions
- Kotlin with 4-space indentation; keep lines concise and avoid deeply nested scopes.
- File/class names use PascalCase (e.g., `MainActivity.kt`, `ARSessionManager.kt`).
- Packages follow `com.example.monodim` and subpackages (e.g., `ar/`).
- Resources use Android conventions: `layout/activity_main.xml`, `drawable/ic_*`, `string` keys in `values/strings.xml`.

## Testing Guidelines
- Unit tests: JUnit4 (`app/src/test`), name tests `*Test.kt`.
- Instrumented tests: AndroidX JUnit + Espresso (`app/src/androidTest`).
- Prefer small, focused tests; add at least one unit test or instrumented test for behavior changes.

## Commit & Pull Request Guidelines
- Commit messages follow a short prefix + description pattern observed in history:
  - Examples: `docs: ...`, `release: ...`, `chore: ...`, `license: ...`.
- PRs should include: a concise summary, linked issue (if any), and screenshots or screen recordings for UI changes.
- Note any device requirements (ARCore-capable device) when PRs affect AR features.

## Configuration & Security Notes
- `local.properties` should point to your Android SDK and must not be committed with machine-specific paths.
- Do not commit keystores or signing configs.
- The app relies on ARCore and camera permissions; test on an ARCore-supported device.
