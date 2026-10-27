package com.izzyan.izzradio;
import org.junit.Test;
import static org.junit.Assert.*;
public class ScreensaverPreferenceTest {
    @Test public void originalDigitalRemainsDefault() {
        assertEquals("digital",ScreensaverPreference.normalize(null));
        assertEquals("digital",ScreensaverPreference.normalize("unknown"));
        assertEquals("digital",ScreensaverPreference.normalize("digital"));
    }
    @Test public void aquariumSelectionPersistsAsExplicitValue() {
        assertEquals("aquarium",ScreensaverPreference.normalize("aquarium"));
    }
}