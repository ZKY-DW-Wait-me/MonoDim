# MonoDim - Android Project

## Project Overview

MonoDim is an Android application project built with Gradle using Kotlin as the primary programming language. It follows the standard Android project structure and uses modern Android development practices.

| Attribute | Value |
|-----------|-------|
| **Project Name** | MonoDim |
| **Package Name** | com.example.monodim |
| **Application ID** | com.example.monodim |
| **Language** | Kotlin |
| **Build System** | Gradle with Kotlin DSL |
| **Minimum SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 (Android 16) |
| **Compile SDK** | 36 |

## Technology Stack

### Build Tools
| Tool | Version |
|------|---------|
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.0.21 |
| Gradle | 8.13 |
| Java Compatibility | VERSION_11 |
| JVM Target | 11 |

### Key Dependencies
- **AndroidX Core KTX** (1.10.1) - Kotlin extensions for Android core libraries
- **AndroidX AppCompat** (1.6.1) - Backward compatibility support
- **Material Design Components** (1.10.0) - UI components following Material Design
- **AndroidX Activity** (1.8.0) - Activity support library
- **AndroidX ConstraintLayout** (2.1.4) - Flexible layout system

### Testing Dependencies
- **JUnit** (4.13.2) - Unit testing framework
- **AndroidX Test JUnit** (1.1.5) - AndroidX testing extensions
- **Espresso Core** (3.5.1) - UI testing framework

## Project Structure

```
project_AR-Leads/
├── app/                          # Main application module
│   ├── build.gradle.kts          # App-level build configuration
│   ├── proguard-rules.pro        # ProGuard obfuscation rules
│   ├── .gitignore                # Module-specific ignore rules
│   └── src/
│       ├── main/                 # Main source set
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/monodim/
│       │   │   └── MainActivity.kt
│       │   └── res/              # Android resources
│       │       ├── drawable/     # Drawable resources
│       │       ├── layout/       # Layout XML files
│       │       │   └── activity_main.xml
│       │       ├── mipmap-*/     # App icons for different densities
│       │       ├── values/       # Strings, colors, themes
│       │       └── xml/          # Data extraction & backup rules
│       ├── test/                 # Unit tests
│       │   └── java/com/example/monodim/
│       │       └── ExampleUnitTest.kt
│       └── androidTest/          # Instrumented tests
│           └── java/com/example/monodim/
│               └── ExampleInstrumentedTest.kt
├── gradle/
│   ├── libs.versions.toml        # Version catalog for dependencies
│   └── wrapper/                  # Gradle wrapper files
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Gradle properties
├── local.properties              # Local SDK configuration (not in VCS)
├── gradlew / gradlew.bat         # Gradle wrapper scripts
└── .gitignore                    # Project ignore rules
```

## Build and Test Commands

### Build Commands

```bash
# Build the entire project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build artifacts
./gradlew clean
```

> **Note:** On Windows, use `gradlew.bat` instead of `./gradlew`

### Test Commands

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run all tests
./gradlew check
```

### Installation Commands

```bash
# Install debug APK to connected device
./gradlew installDebug

# Uninstall from device
./gradlew uninstallDebug
```

## Code Style Guidelines

### Kotlin Code Style
- The project uses the **official** Kotlin code style (`kotlin.code.style=official` in `gradle.properties`)
- Follow standard Kotlin naming conventions:
  - `PascalCase` for classes and interfaces
  - `camelCase` for functions and variables
  - `UPPER_SNAKE_CASE` for constants

### Android Conventions
- **AndroidX** is enabled (`android.useAndroidX=true`)
- **Non-transitive R classes** are enabled to reduce build size
- **Package namespace:** `com.example.monodim`
- Layout IDs use snake_case (e.g., `@+id/main`)
- Resources are organized by type in `res/` subdirectories

## Testing Strategy

### Unit Tests
- Located in `app/src/test/java/`
- Uses JUnit 4 framework
- Runs on JVM without Android dependencies
- Example: `ExampleUnitTest.kt`

### Instrumented Tests
- Located in `app/src/androidTest/java/`
- Requires Android device/emulator
- Uses AndroidX Test framework with AndroidJUnit4 runner
- Uses Espresso for UI interactions
- Example: `ExampleInstrumentedTest.kt`

### Test Runner Configuration
```kotlin
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```

## Dependency Management

The project uses **Gradle Version Catalogs** for dependency management:

- Configuration file: `gradle/libs.versions.toml`
- Benefits: Centralized version management, type-safe accessors, IDE support

### Adding Dependencies

1. Add version to `[versions]` section in `libs.versions.toml`
2. Add library definition to `[libraries]` section
3. Reference in `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.library.name)
}
```

## Security Considerations

- **ProGuard** is configured but disabled for release builds (`isMinifyEnabled = false`)
- ProGuard rules can be defined in `app/proguard-rules.pro`
- **Data extraction rules** defined in `res/xml/data_extraction_rules.xml`
- **Backup rules** defined in `res/xml/backup_rules.xml`
- Local SDK path is stored in `local.properties` (excluded from version control)

## Version Control

### Ignored Files (`.gitignore`)
- `.gradle/` - Gradle cache
- `/local.properties` - Local SDK configuration
- `/.idea/` - IDE-specific files
- `/build` - Build outputs
- `.DS_Store` - macOS system files
- `/captures` - Screen captures during testing

## Main Entry Point

The application's main entry point is `MainActivity`:
- **Class:** `com.example.monodim.MainActivity`
- **Extends:** `AppCompatActivity`
- **Layout:** `R.layout.activity_main`
- **Theme:** `Theme.MonoDim` (Material3 DayNight, no action bar)
- **Features:** Edge-to-edge display with window insets handling

## IDE Support

This project is configured for Android Studio with:
- `.idea/` directory containing IDE-specific settings
- Gradle wrapper for consistent builds across environments
- Local properties file for SDK path configuration
