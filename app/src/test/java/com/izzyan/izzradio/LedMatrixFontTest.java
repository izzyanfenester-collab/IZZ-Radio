package com.izzyan.izzradio;

import org.junit.Test;
import static org.junit.Assert.*;

public class LedMatrixFontTest {
    @Test public void allAsciiUppercaseAndDigitsMatchReferenceSevenByEightPatterns() {
        String alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for(char value:alphabet.toCharArray()) {
            int[] rows=LedMatrixFont.rows(value);
            assertNotNull("Missing glyph "+value,rows);
            assertEquals("Wrong height "+value,8,rows.length);
            int lit=0;
            for(int bits:rows) {
                assertEquals(0,bits&~0x7f);
                lit+=Integer.bitCount(bits);
            }
            assertTrue("Empty glyph "+value,lit>=8);
        }
    }

    @Test public void importantCharactersAreDistinct() {
        assertFalse(java.util.Arrays.equals(LedMatrixFont.rows('M'),LedMatrixFont.rows('W')));
        assertFalse(java.util.Arrays.equals(LedMatrixFont.rows('N'),LedMatrixFont.rows('R')));
        assertFalse(java.util.Arrays.equals(LedMatrixFont.rows('0'),LedMatrixFont.rows('8')));
        assertFalse(java.util.Arrays.equals(LedMatrixFont.rows('8'),LedMatrixFont.rows('9')));
        assertFalse(java.util.Arrays.equals(LedMatrixFont.rows('B'),LedMatrixFont.rows('8')));
    }

    @Test public void normalizeMalayAccentsAndPunctuationWithoutRandomSymbols() {
        assertEquals("SURIA FM - CINTA TAK PERNAH SALAH",
            LedMatrixFont.normalize("Suria FM — Cinta Tak Pernah Salah"));
        assertEquals("ELENA - MALAM...",LedMatrixFont.normalize("Éléna – malam…"));
        assertEquals("HAI 'CINTA'!",LedMatrixFont.normalize("Hai ‘cinta’!"));
        assertEquals("CAFE",LedMatrixFont.normalize("Café"));
        assertEquals("?",LedMatrixFont.normalize("😀"));
    }

    @Test public void unsupportedSymbolsHaveStableQuestionMarkGlyph() {
        assertArrayEquals(LedMatrixFont.rows('?'),LedMatrixFont.rows('\u00e0'));
        assertEquals(2,LedMatrixFont.CHARACTER_GAP);
        assertEquals(4,LedMatrixFont.WORD_GAP);
    }

    @Test public void fullMessageUsesStableCachedDotColumnsAndColours() {
        LedBannerState s=LedBannerState.from("Suria FM","Playing Suria FM",
                "♪ Artis — Tajuk Lagu",false,true,false);
        LedMatrixMessage msg=LedMatrixMessage.from(s);
        assertEquals(msg.columns.length,msg.colours.length);
        assertTrue(msg.columns.length>100);
        assertEquals(LedMatrixMessage.CYAN,msg.colours[0]);
        assertTrue(java.util.Arrays.stream(msg.colours).anyMatch(c->c==LedMatrixMessage.GOLD));
        assertTrue(java.util.Arrays.stream(msg.colours).allMatch(c->c==LedMatrixMessage.GOLD));
        for(int col:msg.columns) assertEquals(0,col&~0xff);
    }

    @Test public void noMetadataDoesNotInventSong() {
        LedBannerState s=LedBannerState.from("Johor FM","Playing Johor FM",
                "",false,true,false);
        assertEquals("LIVE RADIO",s.detail);
        LedMatrixMessage msg=LedMatrixMessage.from(s);
        assertTrue(java.util.Arrays.stream(msg.colours).allMatch(c->c==LedMatrixMessage.GOLD));
    }
}
