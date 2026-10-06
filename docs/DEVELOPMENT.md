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

The supplied `suria_logo.jpg.b64` contains a literal `[... ELLIPSIZATION ...]` marker at character 9995, so it cannot reconstruct the complete JPEG. The bundled local `logo_suria.jpg` is a normalized recovery from the intact prefix and renders blank. Suria uses this local resource; a correct logo requires replacing the damaged source asset and decoding it again. No complete logo was invented or downloaded.

The doa MP3 is decoded from the supplied base64 bytes. FFprobe reports 17.568 seconds. FFmpeg reports one malformed audio packet and a file-size/duration mismatch; do not assume the supplied file is a complete recording. The app handles an unplayable file by unlocking radio, as required. Replace the source if device playback confirms truncation or corruption.

## Device acceptance

Host tests verify queuing, reset, bounded retries, the 46-station catalog (28 Malaysia / 18 Singapore), and 16:9 measurement on three screen shapes. They cannot establish actual audio output or broadcaster availability.

Before release, install the debug APK and verify: cold launch stops previous radio; doa finishes before the latest queued station starts; no selection waits; tap/long-press clock; dim clock and restored dashboard brightness; background and screen-off audio; notification Stop; headphone unplug and audio-focus pause/resume; network loss and station-switch cancellation. Check all 46 primary/fallback pairs on a real device, particularly HLS and broadcaster redirects. Stream endpoints are maintained by broadcasters and may change. Artwork hints use direct URLs or website favicons with a vector fallback.

GitHub Actions builds on push to main or manual dispatch, using Java 17, Android SDK and Gradle 8.9; artifact: `IZZ-Radio-debug-apk`.
