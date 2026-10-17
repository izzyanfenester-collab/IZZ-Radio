package com.izzyan.izzradio;

import org.junit.Test;
import static org.junit.Assert.*;

public class WakeCycleTrackerTest {
    @Test public void wakeAfterNextDayAndAfterSeveralDays() {
        WakeCycleTracker tracker=new WakeCycleTracker(false);
        tracker.onScreenOff();
        assertTrue(tracker.onScreenOn(true,10000L));
        tracker.onScreenOff();
        assertTrue(tracker.onScreenOn(true,86400000L+12000L));
        tracker.onScreenOff();
        assertTrue(tracker.onScreenOn(true,4L*86400000L+12000L));
    }
    @Test public void noRepeatedLaunchOnHomeOrUserPresent() {
        WakeCycleTracker tracker=new WakeCycleTracker(false);
        assertFalse(tracker.onScreenOn(true,5000L));
        tracker.onScreenOff();
        assertTrue(tracker.onScreenOn(true,10000L));
        assertFalse(tracker.onScreenOn(true,11000L));
    }
    @Test public void shortDuplicateScreenCyclesAreDebounced() {
        WakeCycleTracker tracker=new WakeCycleTracker(false);
        tracker.onScreenOff();
        assertTrue(tracker.onScreenOn(true,10000L));
        tracker.onScreenOff();
        assertFalse(tracker.onScreenOn(true,13000L));
        tracker.onScreenOff();
        assertTrue(tracker.onScreenOn(true,17000L));
    }
    @Test public void optedOutNeverStartsAndNoSpuriousCatchup() {
        WakeCycleTracker tracker=new WakeCycleTracker(false);
        tracker.onScreenOff();
        assertFalse(tracker.onScreenOn(false,10000L));
        assertFalse(tracker.onScreenOn(true,20000L));
        tracker.onScreenOff();
        assertTrue(tracker.onScreenOn(true,25000L));
    }
    @Test public void overnightProcessRestartIsFreshCycle() {
        WakeCycleTracker tracker=new WakeCycleTracker(true);
        assertTrue(tracker.onScreenOn(true,1000L));
        assertFalse(tracker.onScreenOn(true,1001L));
    }
}
