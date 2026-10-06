package com.izzyan.izzradio;
import org.junit.Test;
import static org.junit.Assert.*;
public class StreamAttemptsTest {
 @Test public void primaryFallbackDelayedPrimaryThenStop() { StreamAttempts s=new StreamAttempts(); assertEquals(0,s.index()); assertTrue(s.advance(true)); assertEquals(1,s.index()); assertEquals(0,s.delayMillis()); assertTrue(s.advance(true)); assertEquals(2,s.index()); assertEquals(3000,s.delayMillis()); assertFalse(s.advance(true)); }
 @Test public void absentFallbackStillRetriesPrimaryOnlyOnce() { StreamAttempts s=new StreamAttempts(); assertTrue(s.advance(false)); assertEquals(2,s.index()); assertEquals(3000,s.delayMillis()); assertFalse(s.advance(false)); }
}
