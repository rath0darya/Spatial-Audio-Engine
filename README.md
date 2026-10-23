# Spatial Audio Engine

A native Kotlin Android listening companion with a Spotify-inspired interface, embedded YouTube playback, and best-effort Android system audio effects.

## Current features
- Paste a YouTube watch, Shorts, live, or youtu.be link and play it in the embedded YouTube player.
- Dark music-player UI with separate green, cyan, and violet accents.
- **Wide**, **Cinema**, and **Deep Field** sound presets.
- Attempts Android output-mix `Virtualizer` for a wider/binaural-style stage and `BassBoost` for deeper low frequencies.
- Adjustable bass-effect strength.
- Optional Android MediaProjection playback-capture diagnostic; samples are analysed in memory and not saved.
- No audio downloads, no app backend, and no pull-request or scheduled workflow triggers.

## Important audio reality check
This is **not Dolby Atmos-certified** and does not create true object-based Dolby Atmos rendering. The app uses Android's public output-mix audio-effect interface where the device permits it. Many OEM builds, including some Android customisations, reject or ignore global effects for third-party apps. The app reports when an effect cannot be enabled; it does not bypass device security or claim that an unavailable effect is working.

YouTube is played by YouTube's official embedded web player, so playback, ads, restrictions, age-gating, and account requirements remain controlled by YouTube. Android playback capture is a separate diagnostic copy and is not an output-replacement pipeline. Capture success alone does not prove spatial processing is audible. Actual system-wide low-latency spatial rendering requires device/OEM audio-effect integration or an output path supported by that device.

## Build and releases
- Pushes to `main` and manual workflow dispatch build the debug APK.
- The workflow verifies the APK file and attaches it to a versioned **GitHub Release**.
- APKs are release assets, not committed to the repository.
- Pull requests and scheduled builds are intentionally not workflow triggers.

## Device test
1. Install the APK from [GitHub Releases](https://github.com/rath0darya/Spatial-Audio-Engine/releases).
2. Paste a YouTube URL and tap **LOAD YOUTUBE**, then start playback using the embedded player.
3. Try **ENABLE 3D STAGE** and **ENABLE DEEP FIELD**. Listen and verify whether your vivo Y400 Pro / OriginOS 6 and boAt Nirvana Ion ANC expose the effects. An enabled flag is not a substitute for listening tests.
4. If system effects are unavailable, that is an OEM limitation rather than a setting this app can safely override.
5. Use **TEST LIVE PLAYBACK CAPTURE** only to check whether Android permits capture; approve the system prompt and inspect the ongoing notification.

## Privacy
The app has no audio-file storage feature and does not download YouTube media. YouTube embeds require Internet access. Playback-capture samples are used only in memory for signal-level diagnostics. The app does not include a cloud backend or analytics service.
