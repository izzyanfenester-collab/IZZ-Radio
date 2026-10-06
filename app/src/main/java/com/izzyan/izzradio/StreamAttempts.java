package com.izzyan.izzradio;
/** A bounded retry sequence; a new station always gets a new sequence. */
final class StreamAttempts {
    private int index;
    int index() { return index; }
    boolean advance(boolean hasFallback) { if(index>=2) return false; index++; if(index==1 && !hasFallback) index=2; return true; }
    long delayMillis() { return index==2?3000:0; }
}
