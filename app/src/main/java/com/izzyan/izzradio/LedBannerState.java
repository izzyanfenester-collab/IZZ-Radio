package com.izzyan.izzradio;

import java.util.Locale;
import java.util.Objects;

/** Pure, testable mapping from existing radio service state to the LED banner. */
final class LedBannerState {
    static final int NORMAL=0, SELAWAT=1, WAITING=2, ERROR=3, READY=4;
    final String station;
    final String detail;
    final int mode;
    final boolean live;

    LedBannerState(String station, String detail, int mode, boolean live) {
        this.station=station==null?"":station.trim();
        this.detail=detail==null?"":detail.trim();
        this.mode=mode;
        this.live=live;
    }

    static LedBannerState ready() { return new LedBannerState("IZZ RADIO","READY",READY,false); }

    static LedBannerState from(String stationName, String status, String nowPlaying,
                               boolean selawat, boolean playing, boolean buffering) {
        String state=status==null?"":status.trim();
        if(selawat) {
            if(state.toLowerCase(Locale.ROOT).contains("failed"))
                return new LedBannerState("SELAWAT","TAP TO RETRY",ERROR,false);
            String queueSuffix=" will start after Selawat";
            int queue=state.toLowerCase(Locale.ROOT).indexOf(queueSuffix.toLowerCase(Locale.ROOT));
            if(queue>0)
                return new LedBannerState("SELAWAT","NEXT: "+state.substring(0,queue),SELAWAT,false);
            return new LedBannerState("SELAWAT","NOW PLAYING",SELAWAT,false);
        }

        if(stationName==null || stationName.trim().isEmpty()) {
            String errorPrefix="Unable to play ";
            if(state.startsWith(errorPrefix)) {
                String name=state.substring(errorPrefix.length());
                int end=name.indexOf('.');
                if(end>=0) name=name.substring(0,end);
                return new LedBannerState(name,"CONNECTION ERROR",ERROR,false);
            }
            return ready();
        }

        String name=stationName.trim();
        if(state.toLowerCase(Locale.ROOT).contains("unable to play") ||
                state.toLowerCase(Locale.ROOT).contains("error"))
            return new LedBannerState(name,"CONNECTION ERROR",ERROR,false);
        if(buffering && !state.startsWith("Connecting"))
            return new LedBannerState(name,"BUFFERING...",WAITING,false);
        if(state.startsWith("Connecting") || !playing)
            return new LedBannerState(name,"CONNECTING...",WAITING,false);

        String song=nowPlaying==null?"":nowPlaying.trim();
        if(song.startsWith("♪")) song=song.substring(1).trim();
        String lowered=song.toLowerCase(Locale.ROOT);
        if(song.isEmpty() || lowered.startsWith("getting song title") ||
            lowered.startsWith("connecting") || lowered.startsWith("live radio") ||
            lowered.contains("song title not provided"))
            return new LedBannerState(name,"LIVE RADIO",NORMAL,true);
        return new LedBannerState(name,song,NORMAL,true);
    }

    @Override public boolean equals(Object value) {
        if(!(value instanceof LedBannerState)) return false;
        LedBannerState other=(LedBannerState)value;
        return station.equals(other.station) && detail.equals(other.detail) &&
                mode==other.mode && live==other.live;
    }
    @Override public int hashCode() { return Objects.hash(station,detail,mode,live); }
}
