package com.izzyan.izzradio;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScreensaverStyleTest {
    @Test public void defaultsToUnchangedDigitalClock() {
        assertEquals(ScreensaverStyle.DIGITAL,ScreensaverStyle.normalize(null));
        assertEquals(ScreensaverStyle.DIGITAL,ScreensaverStyle.normalize(""));
        assertEquals(ScreensaverStyle.DIGITAL,ScreensaverStyle.normalize("unexpected"));
    }
    @Test public void keepsExplicitVintageSelection() {
        assertEquals(ScreensaverStyle.VINTAGE,ScreensaverStyle.normalize("vintage"));
        assertEquals(ScreensaverStyle.DIGITAL,ScreensaverStyle.normalize("digital"));
    }
}
