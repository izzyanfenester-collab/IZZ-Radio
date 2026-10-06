package com.izzyan.izzradio;
import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class DashboardTest {
 @Test public void freshActivityRequestsDoaServiceAndRendersDashboard() throws Exception {
  MainActivity activity=org.robolectric.Robolectric.buildActivity(MainActivity.class).create().get();
  android.content.Intent launch=org.robolectric.Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService();
  assertNotNull(launch); assertEquals(PlaybackService.LAUNCH,launch.getAction());
  android.view.ViewGroup root=activity.findViewById(android.R.id.content);
  assertTrue(root.getChildAt(0) instanceof AspectFrame);
  assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,activity.getPackageManager().getActivityInfo(new android.content.ComponentName(activity,MainActivity.class),0).screenOrientation);
 }
 @Test public void catalogHasAll46StationsAndLocalSuria() { java.util.List<Station> stations=Station.load(RuntimeEnvironment.getApplication()); assertEquals(46,stations.size()); assertEquals(28,stations.stream().filter(s->s.country.equals("Malaysia")).count()); assertEquals(18,stations.stream().filter(s->s.country.equals("Singapore")).count()); assertEquals("logo_suria",stations.stream().filter(s->s.name.equals("Suria FM")).findFirst().get().localLogo); }
 @Test public void ultrawideScreenPillarboxesWithoutStretching() { verify(2400,1080,1920,1080,240,0); }
 @Test public void tallerScreenLetterboxesWithoutStretching() { verify(1600,1200,1600,900,0,150); }
 @Test public void exact169FillsCanvas() { verify(1920,1080,1920,1080,0,0); }
 private void verify(int width,int height,int cw,int ch,int left,int top) { AspectFrame frame=new AspectFrame(RuntimeEnvironment.getApplication()); View child=new FrameLayout(RuntimeEnvironment.getApplication()); frame.addView(child); frame.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY)); frame.layout(0,0,width,height); assertEquals(cw,child.getWidth()); assertEquals(ch,child.getHeight()); assertEquals(left,child.getLeft()); assertEquals(top,child.getTop()); }
}
