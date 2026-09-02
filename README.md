# Local music

An offline music player for Android that plays audio files already stored on the phone.
No network, no accounts, no streaming.

## Status

P0 — project skeleton and delivery path. The app builds and installs; the library and
playback layers land next. See the plan for the full v1 feature list.

## Build

There is no Android SDK on the development machine, so CI is the compiler. Every push to
`main` runs `assembleDebug`, `lintDebug` and `testDebugUnitTest` on GitHub Actions and
uploads the APK as a workflow artifact.

To install on a phone: open the latest run under Actions, download the
`local-music-debug-apk` artifact, unzip it, and install the APK. "Install unknown apps"
must be allowed for whichever app opens it.

Tagging a commit `v0.1.0` publishes the APK to a GitHub Release instead, which is a single
link the phone can open directly.

## Stack

Kotlin, Jetpack Compose, Material 3, minSdk 26, targetSdk 36, JDK 17.
Playback will use androidx.media3 (ExoPlayer in a `MediaLibraryService`), which also serves
the Android Auto browse tree.
