package com.izzyan.izzradio;

/**
 * Treat one physical screen-off -> screen-on cycle as one wake.
 * Uses monotonic time within the running process, never wall-clock dates:
 * head units can lose accurate calendar time overnight until network sync.
 */
final class WakeCycleTracker {
    private static final long MIN_WAKE_INTERVAL_MS = 5000L;
    private boolean screenWasOff;
    private long lastWakeElapsedMs = -1L;

    WakeCycleTracker(boolean initiallyOff) {
        screenWasOff = initiallyOff;
    }

    void onScreenOff() {
        screenWasOff = true;
    }

    boolean onScreenOn(boolean enabled, long elapsedRealtimeMs) {
        if (!screenWasOff) return false;
        screenWasOff = false;
        if (!enabled) return false;
        if (lastWakeElapsedMs >= 0 && elapsedRealtimeMs >= lastWakeElapsedMs
                && elapsedRealtimeMs - lastWakeElapsedMs < MIN_WAKE_INTERVAL_MS) {
            return false;
        }
        lastWakeElapsedMs = elapsedRealtimeMs;
        return true;
    }
}
