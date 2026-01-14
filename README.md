# Buteyko Breathing Android App

A native Android application for tracking Buteyko breathing exercises, Control Pause (CP) measurements, and progress visualization. Built for a friend.

## Installation

Download and install the APK from [Releases](https://github.com/andrejsstepanovs/breathing/releases/latest) or build from source.

## Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Dependency Injection:** Dagger Hilt
* **Persistence:** Room Database (SQLite)
* **Asynchronicity:** Kotlin Coroutines & Flows
* **Build System:** Gradle (Kotlin DSL)

## Features

1.  **Control Pause (CP) Timer:**
    * High-precision timer (`SS.s` format).
    * Persists records to local DB.
    * "Best Result" and "Recent History" display.
    * **Direct Entry:** Ability to start a breathing session immediately using the recorded CP value.

2.  **Breathing Exercise Loop:**
    * State Machine: `Pre-Check CP` -> `Reduced Breathing` -> `Recovery (30s)` -> `Post-Check CP`.
    * Supports multiple loops (rounds) per session.
    * **Wakelock:** Keeps screen on during breathing phases.
    * **Skip Logic:** Ability to skip recovery countdowns.
    * Session-based notes and aggregation.

3.  **History & Analysis:**
    * Unified list of standalone CP records and full exercise sessions.
    * Detailed drill-down view for sessions (loops, timestamps, notes).
    * Deletion capabilities.

4.  **Charts:**
    * Custom `Canvas`-based line chart visualizing CP progress over time.

## Build & Run

**Prerequisites:**
* JDK 17+
* Android SDK 34

**Commands:**

```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Run Tests
./gradlew test