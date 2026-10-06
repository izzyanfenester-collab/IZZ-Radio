package com.izzyan.izzradio;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackGateTest {
 @Test public void latestStationWaitsForCompletion() { PlaybackGate g=new PlaybackGate(); assertNull(g.select("era")); assertNull(g.select("suria")); assertTrue(g.locked()); assertEquals("suria",g.doaFinished()); assertFalse(g.locked()); assertEquals("ria",g.select("ria")); }
 @Test public void freshLaunchDiscardsOldQueueAndLocksAgain() { PlaybackGate g=new PlaybackGate(); g.select("old"); g.doaFinished(); g.freshLaunch(); assertTrue(g.locked()); assertNull(g.doaFinished()); }
 @Test public void stopDuringDoaDoesNotUnlock() { PlaybackGate g=new PlaybackGate(); g.select("era"); g.clearQueue(); assertTrue(g.locked()); assertNull(g.doaFinished()); }
 @Test public void noSelectionWaitsAfterCompletion() { PlaybackGate g=new PlaybackGate(); assertNull(g.doaFinished()); assertFalse(g.locked()); }
}
