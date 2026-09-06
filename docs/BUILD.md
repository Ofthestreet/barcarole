# Building Barcarole

The user-facing guide is in the [README](../README.md). This file is the developer side.

## Stack

Kotlin, Jetpack Compose, Material 3, minSdk 26, targetSdk 36, JDK 17. Playback is
androidx.media3 (ExoPlayer in a `MediaLibraryService`), which also serves the Android Auto
browse tree. Hilt for injection, Room for the listening history, DataStore for settings and
the saved queue.

## CI is the compiler

There is no Android SDK on the development machine, so nothing is built locally. Every push
to `main` runs `assembleDebug`, `lintDebug` and `testDebugUnitTest` on GitHub Actions and
uploads the APK as the `barcarole-debug-apk` artifact. Lint failures break the build on
purpose: they are the only static check standing between a change and a phone.

Tagging a commit `v0.1.0` publishes the APK to a GitHub Release instead, which is a single
link the phone can open directly.

## Unit tests

The pure logic is deliberately kept free of Android types so it can be tested on the JVM:
library grouping and sorting, search matching, the playlist windows, duplicate detection,
queue restoration and the Android Auto browse tree.
