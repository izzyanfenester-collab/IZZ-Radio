# Codex Task — Build IZZ Radio v1.7

Implement the complete Android application in this repository.

Read AGENTS.md first. Treat it as the product contract.

## Product flow
1. User opens IZZ Radio.
2. Any existing radio playback is stopped.
3. Doa Menaiki Kenderaan autoplays immediately.
4. The landscape 16:9 radio-selection dashboard is visible while the doa plays.
5. Tapping a station during the doa does NOT interrupt it. Queue the latest selected station and show a short message that it will start after the doa.
6. When the doa finishes, start the queued station automatically. If no station was selected, wait for the user.
7. Once a station starts, show the 16:9 night clock screen.
8. Radio remains audible in the background/screen-off.
9. Tap the clock screen to return to station selection. Long-press the clock to stop radio.

## Radio selection design
- Landscape only.
- Centered 16:9 canvas.
- Black/dark navy background.
- Header: IZZ Radio — Malaysia & Singapore Radio.
- Two main panes: Malaysia on left, Singapore on right.
- Scroll each pane if needed.
- Every station card shows station-specific logo/artwork, station name, country label and a play icon.
- Current playing station should be visually indicated.
- Use the original local Suria artwork at app/src/main/res/drawable-nodpi/logo_suria.jpg.

## Clock design
- Pure black background.
- DS-Digital soft green LED digits (#39E639) with a subtle dark-green glow.
- Large HH:mm, seconds smaller at the side.
- AM/PM indicator.
- MON–SUN row with current day highlighted in the same soft green.
- dd MM yyyy below.
- Current station name centered below the clock in soft grey.
- Brightness about 0.08 to 0.12 while this screen is active.
- Keep screen awake.

## Radio data
Use stations.json as the source of truth for station names, country, primary stream, fallback stream and artwork hints.

## Reliability
- Media3 ExoPlayer + HLS module.
- Foreground service.
- Primary -> fallback -> one primary retry.
- 12s connect timeout, ~25s read timeout.
- Follow cross-protocol redirects.
- Handle network failures without crashing.
- Notification with station name and Stop action.

## Build system
Create a normal Gradle Android app with:
- namespace/applicationId: com.izzyan.izzradio
- app name: IZZ Radio
- minSdk 24
- targetSdk 35
- compileSdk 35
- Java 17
- Android Gradle Plugin compatible with Gradle 8.9
- dependencies: appcompat, core, material, media3-exoplayer, media3-exoplayer-hls, Glide (or equivalent)

Create .github/workflows/android-build.yml:
- checkout
- JDK 17
- Android SDK
- Gradle 8.9
- gradle :app:assembleDebug --stacktrace
- upload app/build/outputs/apk/debug/app-debug.apk as IZZ-Radio-debug-apk

## Acceptance checks
- Project compiles.
- Doa always finishes before radio can start.
- Tapping during doa queues rather than interrupts.
- 46 stations appear: 28 Malaysia, 18 Singapore.
- Both screens are 16:9 landscape and do not stretch.
- Clock uses DS-Digital soft green LED digits on black.
- Background playback works via foreground service.
- Primary/fallback logic is present.
- GitHub Actions workflow is valid.

After implementation, commit all generated project files to the branch and summarize any stream endpoints that still need real-device verification.
