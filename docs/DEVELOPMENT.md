# IZZ Radio development

Use JDK 17, Android SDK platform 35/build tools 35.0.0, and the checked-in Gradle 8.9 wrapper. The wrapper distribution has a SHA-256 pin. `stations.json` is the source of truth; Gradle packages it as an asset without duplicating the catalog in source.

In this cloud environment:

```sh
. /workspace/izz-env.sh
cd /workspace/IZZ-Radio
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=2
```

On another workstation, install the same prerequisites, set `ANDROID_HOME` (or an ignored `local.properties` SDK path), and run the wrapper. APK: `app/build/outputs/apk/debug/app-debug.apk`.

Playback is owned by a foreground Media3 service. Fresh activity launches send a startup command that stops the previous player, clears retries, and locks the gate for the doa. Station selections replace the queued station until completion. A doa decoding error unlocks the gate as required. Stop during doa clears the queue without interrupting it. If audio focus or a disconnected headset pauses the doa, tap the playback status to resume it. Radio errors attempt primary, fallback, and a primary retry after three seconds; switching stations cancels pending retries. Notification Stop terminates radio playback. External media commands cannot seek or skip the doa.

Both screens use a centered measured 16:9 frame. Clock brightness is 10%, restored on returning to the dashboard. Clock mode alone keeps the screen awake.

## Bundled asset limitations

Both old base64 placeholders have been removed. The original Suria logo was shown inline in chat, but its binary attachment was not available to the execution workspace. The current `logo_suria.jpg` remains the blank recovery from the truncated placeholder, pending the original JPG upload. It has not been presented as a successful replacement.

The doa MP3 is now the original uploaded binary, copied unchanged to `app/src/main/res/raw/doa_menaiki_kenderaan.mp3`. Its duration is 17.528163 seconds and FFmpeg decodes the complete recording with strict error checking and no errors. The old MP3 base64 placeholder has been removed.

## Device acceptance

All 11 host tests pass: queuing, reset, bounded retries, fresh-launch service command, the 46-station catalog (28 Malaysia / 18 Singapore), and 16:9 measurement on three screen shapes. The connected `DoaPlaybackTest` also passed on the API 28 software emulator: the actual Media3 player decoded through the final audio, reached `STATE_ENDED` with no decoder errors, and only then prepared the latest queued station (Suria FM). The emulator ran with host audio output disabled, so this verifies decoder/playback completion and gating, not audible hardware output or broadcaster availability.

Run the device test with an attached emulator/device using `./gradlew :app:connectedDebugAndroidTest --max-workers=2`. The instrumentation runner and test live under `app/src/androidTest`. The test observes normal playback completion; it does not seek, speed up, or simulate the end callback. The built APK was checked to contain the original MP3 byte-for-byte and `apksigner verify --verbose` verified its v2 signature.

Before release, install the debug APK and verify: cold launch stops previous radio; doa finishes before the latest queued station starts; no selection waits; tap/long-press clock; dim clock and restored dashboard brightness; background and screen-off audio; notification Stop; headphone unplug and audio-focus pause/resume; network loss and station-switch cancellation. Check all 46 primary/fallback pairs on a real device, particularly HLS and broadcaster redirects. Stream endpoints are maintained by broadcasters and may change. Artwork hints use direct URLs or website favicons with a vector fallback.

GitHub Actions builds on push to main or manual dispatch, using Java 17, Android SDK and Gradle 8.9; artifact: `IZZ-Radio-debug-apk`.
