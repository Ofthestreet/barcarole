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

Tagging a commit `v0.2.0` publishes a release instead: a signed release APK, which is a single
link the phone can open directly, and the `.aab` bundle the Play Store takes. Both are named
after the tag, and the tag is the only place a version number is written - `versionName` comes
from it and `versionCode` is derived from that (`1.2.3` becomes `10203`), so minor and patch
have to stay below 100.

Signing uses an upload key restored from the repository secrets. The development build falls
back to a debug key when they are absent, so a clone still builds; a release refuses to, since
an unsigned release APK cannot be installed at all.

## Unit tests

The pure logic is deliberately kept free of Android types so it can be tested on the JVM:
library grouping and sorting, search matching, the playlist windows, duplicate detection,
queue restoration and the Android Auto browse tree.
