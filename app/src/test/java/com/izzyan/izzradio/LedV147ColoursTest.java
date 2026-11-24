package com.izzyan.izzradio;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

/** V1.47: exact V1.42 colour mapping with unchanged V1.46 LED glyph geometry. */
public class LedV147ColoursTest {
    private static LedMatrixMessage display(String station,String state,String song,boolean selawat,boolean playing,boolean buffering) {
        return LedMatrixMessage.from(LedBannerState.from(station,state,song,selawat,playing,buffering));
    }

    private static boolean hasColour(LedMatrixMessage message,int colour) {
        return Arrays.stream(message.colours).anyMatch(c->c==colour);
    }

    @Test public void stationSongAndLiveUseCyanGoldRed() {
        LedMatrixMessage msg=display("Suria FM","Playing Suria FM","♪ Doechii - Anxiety",false,true,false);
        assertEquals(0xff00c8ff,msg.colours[0]);
        assertTrue(hasColour(msg,0xffffd447));
        assertTrue(hasColour(msg,0xffff4545));
        assertEquals(msg.columns.length,msg.colours.length);
    }

    @Test public void missingMetadataKeepsGoldLiveRadioNotRedLiveFlag() {
        LedMatrixMessage msg=display("Suria FM","Playing Suria FM","",false,true,false);
        assertEquals(LedMatrixMessage.CYAN,msg.colours[0]);
        assertTrue(hasColour(msg,LedMatrixMessage.GOLD));
        assertFalse(hasColour(msg,LedMatrixMessage.RED));
    }

    @Test public void selawatAndQueuedStationAreEmeraldGreen() {
        LedMatrixMessage msg=display(null,"Suria FM will start after Selawat","",true,false,false);
        for(int c:msg.colours) assertEquals(LedMatrixMessage.GREEN,c);
    }

    @Test public void connectingAndBufferingAreAmber() {
        for(LedMatrixMessage msg:new LedMatrixMessage[]{
          display("Suria FM","Connecting to Suria FM","",false,false,false),
          display("Suria FM","Playing Suria FM","",false,false,true)
        }) for(int c:msg.colours) assertEquals(LedMatrixMessage.GOLD,c);
    }

    @Test public void errorsAreRedAndStoppedIsWhite() {
        LedMatrixMessage error=display("Suria FM","Unable to play Suria FM","",false,false,false);
        for(int c:error.colours) assertEquals(LedMatrixMessage.RED,c);
        LedMatrixMessage ready=LedMatrixMessage.from(LedBannerState.ready());
        for(int c:ready.colours) assertEquals(LedMatrixMessage.WHITE,c);
    }

    @Test public void unchangedV146GlyphsKeepSevenByNineAndSeamlessGap() {
        assertEquals(7,LedMatrixFont.WIDTH);
        assertEquals(9,LedMatrixFont.HEIGHT);
        LedMatrixMessage msg=display("Suria FM","Playing Suria FM","♪ Doechii - Anxiety",false,true,false);
        for(int i=msg.columns.length-8;i<msg.columns.length;i++) assertEquals(0,msg.columns[i]);
        for(int i=0;i<msg.columns.length;i++) {
           int idx=LedSeamlessLoop.columnIndex(i,msg.columns.length,msg.columns.length);
           assertEquals(msg.columns[i],msg.columns[idx]);
        }
    }
}
