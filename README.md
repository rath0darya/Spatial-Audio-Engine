# Spatial Audio Engine

Native Kotlin Android prototype for testing whether Android playback capture can observe eligible live media audio on a device. Target test setup: vivo Y400 Pro / OriginOS 6 + boAt Nirvana Ion ANC + YouTube.

## Current scope
- Android MediaProjection consent flow and `AudioPlaybackCapture` for media/game usages.
- Real-time signal-level detection only. Audio is processed in memory and is never written to storage.
- No cloud backend, analytics, network permissions, downloaded audio, or pull requests.
- GitHub Actions builds the APK and publishes it as an asset on the GitHub Releases page. Build output is not committed back into the repository.

## Critical limitation
Android playback capture provides a copy of eligible audio. It does not grant an ordinary app the ability to replace YouTube's output stream. YouTube may also opt out of capture, or the OEM may impose additional restrictions. This prototype does **not** apply spatial DSP to the audible output and does not claim to deliver 360-degree sound. It is a feasibility probe before attempting any output-routing design.

A seamless system-wide effect generally requires an Android/OEM audio-effect insertion point, a supported audio-effect API, or routing that is not available to ordinary third-party apps on every device. Sending captured audio to the earbuds while YouTube remains audible can produce echo/double audio, so this build intentionally does not do that.

## Build
Open in Android Studio with JDK 17 and Android SDK 35, or use the included GitHub Actions workflow. The workflow downloads Gradle 8.9, builds the debug APK, and attaches it to a versioned GitHub Release. It runs on pushes to `main` and manual dispatch only; it has no pull-request or scheduled trigger.

## Manual device test
1. Install the APK from the latest GitHub Release.
2. Open YouTube and start a video.
3. Open Spatial Audio Engine and press **TEST LIVE CAPTURE**.
4. Accept Android's system capture prompt.
5. Return to YouTube and play audio.
6. Check the ongoing notification and Logcat (`SpatialAudioEngine`) for signal detection.
7. If the app reports no signal, playback capture is unsupported or blocked for that session/build. No capture data is saved.

## Permissions and privacy
The app asks Android for explicit MediaProjection consent, which is required for playback capture. Captured samples remain in RAM for signal-level analysis. The app has no Internet permission.
