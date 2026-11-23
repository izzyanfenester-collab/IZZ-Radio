package com.izzyan.izzradio;

import org.junit.Test;
import static org.junit.Assert.*;

public class LedBannerStateTest {
    @Test public void noSongMetadataNeverInventsTitle() {
        LedBannerState s=LedBannerState.from("Suria FM","Playing Suria FM",
                "♪ Getting song title…",false,true,false);
        assertEquals("Suria FM",s.station);
        assertEquals("LIVE RADIO",s.detail);
        assertTrue(s.live);
    }

    @Test public void realMetadataDisplaysOriginalTitle() {
        LedBannerState s=LedBannerState.from("Suria FM","Playing Suria FM",
                "♪ Artist — Song Title",false,true,false);
        assertEquals("Artist — Song Title",s.detail);
        assertEquals(LedBannerState.NORMAL,s.mode);
    }

    @Test public void latestQueuedStationIsShownDuringSelawat() {
        LedBannerState s=LedBannerState.from(null,
                "Johor FM will start after Selawat","",true,false,false);
        assertEquals("SELAWAT",s.station);
        assertEquals("NEXT: Johor FM",s.detail);
        assertEquals(LedBannerState.SELAWAT,s.mode);
    }

    @Test public void selawatNoQueueAndFailure() {
        assertEquals("NOW PLAYING",LedBannerState.from(null,
                "Playing Selawat • select a station to queue","",true,false,false).detail);
        assertEquals("TAP TO RETRY",LedBannerState.from(null,
                "Selawat playback failed • tap to retry","",true,false,false).detail);
    }

    @Test public void errorAndStoppedAreAccurate() {
        LedBannerState e=LedBannerState.from(null,
                "Unable to play Best FM. Try again or choose another station.","",false,false,false);
        assertEquals("Best FM",e.station);
        assertEquals("CONNECTION ERROR",e.detail);
        assertEquals("READY",LedBannerState.from(null,"Playback stopped","",false,false,false).detail);
    }

    @Test public void connectingBufferingAndNewStationClearOldMetadata() {
        assertEquals("CONNECTING...",LedBannerState.from("ERA FM",
                "Connecting to ERA FM","♪ previous song",false,false,true).detail);
        assertEquals("BUFFERING...",LedBannerState.from("ERA FM",
                "Playing ERA FM","",false,false,true).detail);
    }

    @Test public void identicalDisplayStatesDoNotForceRedraw() {
        LedBannerState a=LedBannerState.from("ZAYAN","Playing ZAYAN",
                "♪ Song A",false,true,false);
        LedBannerState b=LedBannerState.from("ZAYAN","Playing ZAYAN",
                "♪ Song A",false,true,false);
        assertEquals(a,b);
        assertEquals(a.hashCode(),b.hashCode());
    }
}
