package com.izzyan.izzradio;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

/** Regression fixtures traced from the user-supplied LED dot matrix reference image. */
public class LedReferenceAmberFontTest {
    @Test public void sampledGlyphsMatchOriginalImageMeasuredDotGrid() {
        // Reference: original 259x180 LED image. Grid = 7 columns x 9 rows.
        assertArrayEquals(new int[]{62,127,99,99,99,127,127,99,99},LedMatrixFont.rows('A'));
        assertArrayEquals(new int[]{126,127,99,99,127,126,108,102,99},LedMatrixFont.rows('R'));
        assertArrayEquals(new int[]{62,127,99,99,62,99,99,127,62},LedMatrixFont.rows('8'));
        assertArrayEquals(new int[]{127,127,3,6,28,48,96,127,127},LedMatrixFont.rows('Z'));
        assertArrayEquals(new int[]{62,127,99,99,99,99,99,127,62},LedMatrixFont.rows('0'));
        assertArrayEquals(new int[]{0,0,12,12,0,12,12,0,0},LedMatrixFont.rows(':'));
    }

    @Test public void measuredSevenByNineGlyphsAndPunctuationRemainAvailable() {
        assertEquals(7,LedMatrixFont.WIDTH);
        assertEquals(9,LedMatrixFont.HEIGHT);
        for(char ch:"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()) {
            int[] rows=LedMatrixFont.rows(ch);
            assertEquals(9,rows.length);
            for(int row:rows) assertEquals(0,row&~127);
        }
        for(char ch:"-+.:()!?×÷Σ".toCharArray())
            assertEquals(9,LedMatrixFont.rows(ch).length);
    }

    @Test public void stationSongLiveStatusUseV142Multicolours() {
        LedBannerState v=LedBannerState.from("Suria FM","Playing Suria FM",
                 "♪ Doechii - Anxiety",false,true,false);
        LedMatrixMessage message=LedMatrixMessage.from(v);
        assertEquals(LedMatrixMessage.CYAN,message.colours[0]);
        assertTrue(Arrays.stream(message.colours).anyMatch(c->c==LedMatrixMessage.GOLD));
        assertTrue(Arrays.stream(message.colours).anyMatch(c->c==LedMatrixMessage.RED));
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
