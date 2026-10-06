# IZZ Radio v1.7 FINAL — Landscape 16:9

Final behaviour:
- Entire UI is landscape with a centered **16:9 content area**. Screens with a different shape show black bars so the UI is never stretched.
- On every fresh app launch, any existing radio playback is stopped and the bundled **Doa Menaiki Kenderaan** autoplays.
- A station may be selected while the doa is playing, but **radio playback cannot start until the doa finishes**. The latest selected station is queued and starts automatically after completion.
- Radio screen uses Malaysia/Singapore two-column selection with station artwork.
- Digital clock uses black background with DS-Digital soft green LED (#39E639) digits for night driving.
- Current station name appears below the clock.
- Player includes current primary + fallback stream URLs, HLS support, redirect handling, network timeouts and automatic retry/fallback.

## Main features
- **46 live radio stations** grouped into Malaysia and Singapore.
- Digital clock screen saver with black background while radio is playing.
- Radio continues playing in a foreground service/background.
- GitHub Actions workflow builds a debug APK artifact.

## Malaysia (28)
ERA FM, Suria FM, Hot FM, HITZ FM, SINAR FM, Fly FM, GEGAR Pantai Timur, ZAYAN, LITE, MIX, MY, MELODY, goXuan, RAAGA, Eight FM, Molek FM, Kool 101, 988 FM, BFM 89.9, Best FM, Manis FM, Nasional FM, TraXX FM, Ai FM, Minnal FM, Radio Klasik, Asyik FM, Johor FM.

## Singapore (18)
RIA 897, WARNA 942, Class 95, GOLD 905, 987, Kiss92, ONE FM 91.3, POWER 98, MONEY FM 89.3, CNA938, Symphony 924, YES 933, LOVE 972, CAPITAL 958, OLI 968, UFM100.3, 96.3 Hao FM, 88.3JIA.

> Internet radio stream URLs can be changed by broadcasters at any time. The app includes fallback URLs where practical, but individual stations may still require future updates.

## Build APK on GitHub
The included workflow `.github/workflows/android-build.yml` builds `app-debug.apk` and uploads it as the `IZZ-Radio-debug-apk` artifact.
