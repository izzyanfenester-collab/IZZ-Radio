package com.izzyan.izzradio;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** V1.44 acceptance checks: true slim hand-drawn 8x10, not stretched V1.43 artwork. */
public class LedPremiumSlimFontTest {
    @Test public void completeAlphabetAndDigitsAreUniqueLegibleEightByTen() {
        final String characters="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Set<String> shapes=new HashSet<>();
        for(char ch:characters.toCharArray()) {
            int[] rows=LedMatrixFont.rows(ch);
            assertEquals("Ten rows: "+ch,10,rows.length);
            int active=0;
            for(int row:rows) {
                assertEquals("Eight columns: "+ch,0,row & ~255);
                active+=Integer.bitCount(row);
            }
            assertTrue("Empty LED glyph: "+ch,active>=10);
            assertTrue("Too bulky: "+ch+" / "+active,active<44);
            assertTrue("Duplicated glyph shape: "+ch,shapes.add(Arrays.toString(rows)));
        }
    }

    @Test public void criticalGlyphsUseClearlyDifferentHandDesignedShapes() {
        assertFalse(Arrays.equals(LedMatrixFont.rows('M'),LedMatrixFont.rows('W')));
        assertFalse(Arrays.equals(LedMatrixFont.rows('N'),LedMatrixFont.rows('R')));
        assertFalse(Arrays.equals(LedMatrixFont.rows('O'),LedMatrixFont.rows('0')));
        assertFalse(Arrays.equals(LedMatrixFont.rows('B'),LedMatrixFont.rows('8')));
        assertFalse(Arrays.equals(LedMatrixFont.rows('S'),LedMatrixFont.rows('5')));

        // The premium 'A' uses a single thin crossbar row, not doubled/bold pixels.
        int[] a=LedMatrixFont.rows('A');
        assertEquals(255,a[5]);
        assertEquals(129,a[6]);
        assertNotEquals(a[5],a[4]);
        // 'M' has its own diagonal design, not an enlarged 7x9 row clone.
        int[] m=LedMatrixFont.rows('M');
        assertEquals(129,m[0]);
        assertEquals(195,m[1]);
        assertEquals(165,m[2]);
    }

    @Test public void punctuationAndAccentedWordsRemainSupported() {
        for(char ch:"-.,:;!?'/()[]+=&%#*@|<>$^~".toCharArray()) {
            int[] rows=LedMatrixFont.rows(ch);
            assertEquals(10,rows.length);
        }
        assertEquals("CAFE - ANXIETY LIVE",LedMatrixFont.normalize("Café — Anxiety LIVE"));
        assertEquals(2,LedMatrixFont.CHARACTER_GAP);
        assertEquals(4,LedMatrixFont.WORD_GAP);
    }

    @Test public void longTextStillUsesV143CyclicMarqueeAndColors() {
        LedBannerState value=LedBannerState.from("SURIA FM","Playing SURIA FM",
                "♪ Doechii - Anxiety",false,true,false);
        LedMatrixMessage message=LedMatrixMessage.from(value);
        assertTrue(message.columns.length>100);
        assertEquals(message.columns.length,message.colours.length);
        assertTrue(Arrays.stream(message.colours).anyMatch(x->x==LedMatrixMessage.CYAN));
        assertTrue(Arrays.stream(message.colours).anyMatch(x->x==LedMatrixMessage.GOLD));
        assertTrue(Arrays.stream(message.colours).anyMatch(x->x==LedMatrixMessage.RED));
        int n=message.columns.length;
        for(int i=0;i<n;i++) assertEquals(message.columns[i],
                message.columns[LedSeamlessLoop.columnIndex(i,n,n)]);
        for(int i=n-8;i<n;i++) assertEquals(0,message.columns[i]);
    }
}
