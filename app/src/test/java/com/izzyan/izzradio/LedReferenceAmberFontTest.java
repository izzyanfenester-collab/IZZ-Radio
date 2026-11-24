package com.izzyan.izzradio;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

/** Regression fixtures traced from the user-supplied LED dot matrix reference image. */
public class LedReferenceAmberFontTest {
    @Test public void primaryGlyphsMatchTheSuppliedReference() {
        assertArrayEquals(new int[]{62,127,103,103,127,127,127,103},LedMatrixFont.rows('A'));
        assertArrayEquals(new int[]{126,127,103,127,126,124,110,103},LedMatrixFont.rows('R'));
        assertArrayEquals(new int[]{62,127,103,126,127,103,127,62},LedMatrixFont.rows('8'));
        assertArrayEquals(new int[]{127,127,7,30,60,112,126,127},LedMatrixFont.rows('Z'));
    }

    @Test public void referenceCharacterDimensionsAndGlyphsArePresent() {
        assertEquals(7,LedMatrixFont.WIDTH);
        assertEquals(8,LedMatrixFont.HEIGHT);
        for(char ch:"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()) {
            int[] rows=LedMatrixFont.rows(ch);
            assertEquals(8,rows.length);
            for(int row:rows) assertEquals(0,row&~127);
        }
        for(char ch:"-+.:()!?".toCharArray())
            assertEquals(8,LedMatrixFont.rows(ch).length);
    }

    @Test public void stationSongLiveStatusAllUseReferenceAmber() {
        LedBannerState v=LedBannerState.from("Suria FM","Playing Suria FM",
                 "♪ Doechii - Anxiety",false,true,false);
        LedMatrixMessage message=LedMatrixMessage.from(v);
        assertTrue(Arrays.stream(message.colours).allMatch(c->c==0xffffbd37));
        for(int i=message.columns.length-8;i<message.columns.length;i++)
            assertEquals(0,message.columns[i]);
    }

    @Test public void originalSeamlessMarqueeIndexWrapRemainsUnchanged() {
        LedMatrixMessage m=LedMatrixMessage.from(LedBannerState.ready());
        for(int i=0;i<m.columns.length;i++) {
            int j=LedSeamlessLoop.columnIndex(i,m.columns.length,m.columns.length);
            assertEquals(m.columns[i],m.columns[j]);
        }
    }
}
