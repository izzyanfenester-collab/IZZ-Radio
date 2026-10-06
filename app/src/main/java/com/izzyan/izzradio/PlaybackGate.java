package com.izzyan.izzradio;

/** All calls are serialized on the service main thread. */
final class PlaybackGate {
    private boolean locked = true;
    private String queued;
    void freshLaunch() { locked = true; queued = null; }
    String select(String id) { if (locked) { queued = id; return null; } return id; }
    String doaFinished() { locked = false; String next = queued; queued = null; return next; }
    boolean locked() { return locked; }
    void clearQueue() { queued = null; }
}
