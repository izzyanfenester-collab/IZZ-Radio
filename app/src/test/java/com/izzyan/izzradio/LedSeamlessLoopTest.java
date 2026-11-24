package com.izzyan.izzradio;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LedSeamlessLoopTest {
    @Test public void offsetWrapIsContinuousAtCycleBoundary() {
        assertEquals(0f,LedSeamlessLoop.wrap(0f,140f),0.0001f);
        assertEquals(0f,LedSeamlessLoop.wrap(140f,140f),0.0001f);
        assertEquals(1.25f,LedSeamlessLoop.wrap(141.25f,140f),0.0001f);
        assertEquals(139.25f,LedSeamlessLoop.wrap(-0.75f,140f),0.0001f);
    }

    @Test public void repeatedColumnsJoinCorrectlyWithNoBlankReset() {
        int[] oldPixels={0x003,0x1fe,0x0ff,0,0,0x155};
        for(int index=0;index<oldPixels.length*4;index++) {
            assertEquals(oldPixels[index % oldPixels.length],
                    oldPixels[LedSeamlessLoop.columnIndex(index,0,oldPixels.length)]);
        }
        assertEquals(5,LedSeamlessLoop.columnIndex(0,-1,oldPixels.length));
        assertEquals(0,LedSeamlessLoop.columnIndex(5,1,oldPixels.length));
    }

    @Test public void bothShortAndLongMessagesIncludeSeamlessGap() {
        LedBannerState shortState=LedBannerState.ready();
        LedBannerState longState=LedBannerState.from("SURIA FM","Playing SURIA FM",
            "♪ DOECHII - ANXIETY",false,true,false);
        LedMatrixMessage shortMessage=LedMatrixMessage.from(shortState);
        LedMatrixMessage longMessage=LedMatrixMessage.from(longState);
        for(LedMatrixMessage message:new LedMatrixMessage[]{shortMessage,longMessage}) {
            assertTrue(message.columns.length>0);
            assertEquals(message.columns.length,message.colours.length);
            for(int i=message.columns.length-8;i<message.columns.length;i++)
                assertEquals("Fixed short trailing gap",0,message.columns[i]);
            for(int n=0;n<message.columns.length;n++)
                assertEquals(message.columns[n],message.columns[
                    LedSeamlessLoop.columnIndex(n,message.columns.length,message.columns.length)]);
        }
    }

    @Test public void displayRetainsCyanSongGoldAndRedLive() {
        LedBannerState data=LedBannerState.from("SURIA FM","Playing SURIA FM",
            "♪ Doechii - Anxiety",false,true,false);
        LedMatrixMessage message=LedMatrixMessage.from(data);
        assertTrue(java.util.Arrays.stream(message.colours).anyMatch(x->x==LedMatrixMessage.CYAN));
        assertTrue(java.util.Arrays.stream(message.colours).anyMatch(x->x==LedMatrixMessage.GOLD));
        assertTrue(java.util.Arrays.stream(message.colours).anyMatch(x->x==LedMatrixMessage.RED));
    }

    @Test public void allTenRowsCanBeDrawn() {
        assertEquals(7,LedMatrixFont.WIDTH);
        assertEquals(9,LedMatrixFont.HEIGHT);
        for(char c:"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()) {
            int[] rows=LedMatrixFont.rows(c);
            assertEquals(9,rows.length);
            for(int row:rows) assertEquals(0,row&~0x7f);
        }
    }
}
