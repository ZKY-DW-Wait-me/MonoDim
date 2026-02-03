# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MonoDim is an Android AR measurement application built with Google ARCore. It allows users to measure distances between two points in real-world space by tapping on surfaces. The app uses ARCore's Visual Inertial Odometry (VIO) for centimeter-level accuracy.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Architecture

The app follows a simple single-activity architecture with AR rendering handled by a separate manager class.

### Core Components

- **MainActivity** (`app/src/main/java/com/example/monodim/MainActivity.kt`): Main entry point handling UI interactions, anchor management, and distance calculation. Uses ViewBinding for UI access.

- **ARSessionManager** (`app/src/main/java/com/example/monodim/ar/ARSessionManager.kt`): Implements `GLSurfaceView.Renderer` to manage the ARCore session lifecycle and frame updates. Exposes `currentFrame` for hit testing from MainActivity.

- **BackgroundRenderer** (`app/src/main/java/com/example/monodim/ar/BackgroundRenderer.kt`): OpenGL ES 2.0 renderer for camera background. Uses external texture (GL_TEXTURE_EXTERNAL_OES) with custom vertex/fragment shaders.

### Measurement Flow

1. User taps "+" button → `onAddPoint()` performs hit test at screen center
2. Hit test prioritizes: Plane hits → DepthPoint hits → Oriented Point hits → Any hit
3. First tap creates `anchor1`, second tap creates `anchor2`
4. Distance updates run via `postOnAnimation` loop with robust averaging (median-based outlier filtering)

### Key ARCore Configuration

- `InstantPlacementMode.LOCAL_Y_UP`: Enables quick placement before plane detection
- `DepthMode.AUTOMATIC`: Uses depth API when available for better hit testing
- Both horizontal and vertical plane finding enabled

## Tech Stack

- Language: Kotlin
- Min SDK: 24 (Android 7.0)
- Target SDK: 33
- ARCore: 1.46.0
- Build: Gradle with Kotlin DSL and version catalog (`gradle/libs.versions.toml`)
