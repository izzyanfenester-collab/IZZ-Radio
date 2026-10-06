package com.izzyan.izzradio;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ClockAppearanceTest {
    @Test public void clockUsesBundledDigitalFontGreenLedAndNightBrightness() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->{
                Typeface digital=Typeface.createFromAsset(a.getAssets(),"fonts/ds_digital.ttf");
                assertNotEquals("The bundled font must not resolve to monospace",Typeface.MONOSPACE,digital);
                for(String name:new String[]{"hours","seconds","ampm","date"}) {
                    TextView view=field(a,name);
                    assertEquals(name+" uses the actual bundled DS-Digital face",digital,view.getTypeface());
                    assertNotEquals(name+" must not use the fallback",Typeface.MONOSPACE,view.getTypeface());
                    assertEquals(0xff39e639,view.getCurrentTextColor());
                    assertTrue(view.getShadowRadius()>0);
                    assertEquals(0xff0d6b18,view.getShadowColor());
                }
                List<TextView> days=field(a,"weekdays");
                assertEquals(7,days.size()); int highlighted=0;
                for(TextView day:days) {
                    assertEquals(digital,day.getTypeface());
                    if(day.getCurrentTextColor()==0xff39e639) highlighted++;
                    else assertEquals(0xff102314,day.getCurrentTextColor());
                }
                assertEquals("Exactly today's label is highlighted",1,highlighted);
                float scaled=a.getResources().getDisplayMetrics().scaledDensity;
                TextView hours=field(a,"hours"); assertTrue("Main clock remains large",hours.getTextSize()/scaled>=100);
                TextView station=field(a,"stationLabel");
                assertEquals(26f,station.getTextSize()/scaled,0.01f);
                assertTrue(station.getTypeface().isBold()); assertNotEquals(digital,station.getTypeface());
                assertEquals(0xffd2d6d9,station.getCurrentTextColor());
                LinearLayout clock=field(a,"clock");
                assertSame("Station label remains below the clock",station,clock.getChildAt(clock.getChildCount()-1));
                assertEquals(0xff000000,((ColorDrawable)clock.getBackground()).getColor());
                showClock(a,true);
                assertEquals(0.10f,a.getWindow().getAttributes().screenBrightness,0.001f);
                assertTrue((a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0);
                showClock(a,false);
                assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,a.getWindow().getAttributes().screenBrightness,0f);
                assertEquals(0,a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        } finally { context.stopService(new Intent(context,PlaybackService.class)); }
    }
    @SuppressWarnings("unchecked") private static <T> T field(MainActivity a,String name) {
        try { Field f=MainActivity.class.getDeclaredField(name); f.setAccessible(true); return (T)f.get(a); }
        catch(Exception e) { throw new AssertionError(e); }
    }
    private static void showClock(MainActivity a,boolean show) {
        try { Method m=MainActivity.class.getDeclaredMethod("showClock",boolean.class); m.setAccessible(true); m.invoke(a,show); }
        catch(Exception e) { throw new AssertionError(e); }
    }
}
