package com.izzyan.izzradio;
import org.junit.Test;
import static org.junit.Assert.*;

public class MoonPhaseTest {
    @Test public void knownNewMoonIsDark() {
        assertEquals(0.0,MoonPhaseView.phase(MoonPhaseView.KNOWN_NEW_MOON_UTC),0.00001);
        assertEquals("Bulan baharu",MoonPhaseView.phaseName(0.0));
    }
    @Test public void halfLunationIsFullMoon() {
        long time=MoonPhaseView.KNOWN_NEW_MOON_UTC +
                Math.round(MoonPhaseView.SYNODIC_DAYS*86400000.0/2.0);
        assertEquals(0.5,MoonPhaseView.phase(time),0.001);
        assertEquals("Bulan purnama",MoonPhaseView.phaseName(0.5));
    }
    @Test public void phaseRepeatsOverLunation() {
        long time=MoonPhaseView.KNOWN_NEW_MOON_UTC +
                Math.round(MoonPhaseView.SYNODIC_DAYS*86400000.0);
        assertEquals(0.0,MoonPhaseView.phase(time),0.001);
    }
}