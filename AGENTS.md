# IZZ Radio — Codex Instructions

This repository is for an Android-only radio app. Build the app described in CODEX_PROMPT.md and preserve these requirements unless the user explicitly changes them.

## Non-negotiable startup behavior
- On every fresh app launch, stop any previous radio playback and autoplay the bundled Doa Menaiki Kenderaan.
- The doa must finish completely before any radio is allowed to start.
- If the user taps a station while the doa is playing, queue the latest selected station and start it automatically only after the doa completes.
- If the doa file cannot be played, fail gracefully and unlock radio playback.

## UI
- Lock the whole app to landscape.
- Render radio-selection and clock screens in a centered 16:9 content area, with black letterbox/pillarbox space on other aspect ratios. Never stretch the UI.
- Radio selection: dark premium dashboard; Malaysia left column, Singapore right column; station-specific logo, station name and play affordance.
- Clock screen: pure black background, DS-Digital soft green LED (#39E639) clock suitable for night driving, day/date/AM-PM/seconds and current station name under the clock.
- Keep screen awake in clock mode and lower screen brightness to about 8–12%.
- Tap clock to return to station list. Long-press may stop playback.

## Playback
- Use AndroidX Media3 ExoPlayer in a foreground mediaPlayback service.
- Audio must continue with screen off/background.
- Handle redirects, HLS, connection/read timeouts and audio-becoming-noisy.
- For each station, try primary URL first, then fallback URL, then one delayed retry of primary.
- Do not permanently claim streams are guaranteed. Broadcasters can change endpoints.
- Add a low-priority playback notification with Stop action.

## Assets
- Use the original binary at app/src/main/res/raw/doa_menaiki_kenderaan.mp3; do not regenerate it from old base64 placeholders.
- Store the supplied original Suria artwork at app/src/main/res/drawable-nodpi/logo_suria.jpg; do not reconstruct it from truncated base64.
- Suria must use the local bundled artwork.
- Other station logos may be loaded remotely with a safe vector radio-icon fallback.

## Build
- Java 17.
- Min SDK 24, target/compile SDK 35.
- Add a GitHub Actions workflow that runs on push to main and workflow_dispatch, builds debug APK, and uploads app-debug.apk as artifact IZZ-Radio-debug-apk.
- Before reporting completion, run the build and fix compile errors.
