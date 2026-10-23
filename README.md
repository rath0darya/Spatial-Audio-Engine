# Spatial Audio Engine

Native Android stream player with a custom real-time PCM DSP pipeline. Audio is processed in memory during playback; the app does not download or save media files.

## Features
- Direct HTTP/HTTPS audio stream URLs, including supported MP3/AAC and HLS streams.
- Real-time stereo width, low-frequency enhancement, cross-channel ambience and soft peak limiting.
- Wide, Cinema and Deep Field tuning presets.
- Intensity slider and instant original-audio bypass for A/B comparison.
- Responsive screen layout that respects Android status and navigation bars.
- Custom adaptive launcher icon.

## Important compatibility
- **YouTube links are not DSP sources in this build.** YouTube's embedded player does not expose decoded audio samples to this app, and the app does not extract or download YouTube media. Paste a direct audio stream URL instead.
- The engine implements virtual stereo DSP. It is not Dolby Atmos decoding or Dolby-certified processing.
- Some URLs may not be direct media streams or may require unsupported codecs, authentication, or special headers.
- Playback uses temporary streaming buffers; no media files are intentionally written to local storage.


YouTube links are not processed in-app and will not be opened as if DSP were applied. Use a supported direct audio-stream URL for in-app spatial DSP. Mono streams are expanded to stereo for virtual spatial cues; results depend on source audio and listening equipment.
