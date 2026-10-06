package com.izzyan.izzradio;

import android.content.*;
import android.os.*;
import android.view.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** Exercises the real Android decoder/player, not a simulated completion callback. */
@RunWith(AndroidJUnit4.class)
public class DoaPlaybackTest {
    @Test public void suriaCardDisplaysBundledOriginalLogo() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)) {
            activity.onActivity(a->{
                android.widget.LinearLayout card=(android.widget.LinearLayout)find(a.getWindow().getDecorView(),"Play Suria FM, Malaysia");
                assertNotNull("Suria card is present",card);
                android.widget.ImageView image=(android.widget.ImageView)card.getChildAt(0);
                assertEquals("Suria FM artwork",image.getContentDescription().toString());
                assertEquals(android.widget.ImageView.ScaleType.FIT_CENTER,image.getScaleType());
                assertTrue(image.getDrawable() instanceof android.graphics.drawable.BitmapDrawable);
                android.graphics.Bitmap displayed=((android.graphics.drawable.BitmapDrawable)image.getDrawable()).getBitmap();
                android.graphics.Bitmap bundled=android.graphics.BitmapFactory.decodeResource(a.getResources(),R.drawable.logo_suria);
                assertNotNull("JPEG decodes on Android",bundled);
                assertEquals(128,bundled.getWidth()); assertEquals(128,bundled.getHeight());
                // Compare using Android's drawable decoder; BitmapFactory can use different JPEG chroma upsampling.
                android.graphics.Bitmap drawableReference=((android.graphics.drawable.BitmapDrawable)androidx.core.content.res.ResourcesCompat.getDrawable(a.getResources(),R.drawable.logo_suria,a.getTheme())).getBitmap();
                android.graphics.Bitmap actualPixels=displayed.copy(android.graphics.Bitmap.Config.ARGB_8888,false);
                android.graphics.Bitmap expectedPixels=drawableReference.copy(android.graphics.Bitmap.Config.ARGB_8888,false);
                assertTrue("Station card displays the local bundled pixels",actualPixels.sameAs(expectedPixels));
                int pixel=actualPixels.getPixel(10,10);
                assertTrue("Original artwork is yellow, not the blank placeholder",android.graphics.Color.red(pixel)>150 && android.graphics.Color.green(pixel)>80);
                actualPixels.recycle(); expectedPixels.recycle(); bundled.recycle();
            });
        } finally { context.stopService(new Intent(context,PlaybackService.class)); }
    }
    @Test public void freshLaunchPlaysCompleteDoaBeforeLatestQueuedStation() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicReference<PlaybackService> service=new AtomicReference<>();
        CountDownLatch connected=new CountDownLatch(1);
        ServiceConnection connection=new ServiceConnection() {
            public void onServiceConnected(ComponentName name,IBinder binder) { service.set(((PlaybackService.LocalBinder)binder).service()); connected.countDown(); }
            public void onServiceDisconnected(ComponentName name) { }
        };
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)) {
            assertTrue(context.bindService(new Intent(context,PlaybackService.class),connection,Context.BIND_AUTO_CREATE));
            try {
                assertTrue("Playback service connected",connected.await(15,TimeUnit.SECONDS));
                AtomicReference<ExoPlayer> player=new AtomicReference<>();
                AtomicBoolean ended=new AtomicBoolean(false);
                AtomicReference<PlaybackException> error=new AtomicReference<>();
                main(()->{
                    try { java.lang.reflect.Field field=PlaybackService.class.getDeclaredField("player"); field.setAccessible(true); player.set((ExoPlayer)field.get(service.get())); }
                    catch(Exception e) { throw new AssertionError(e); }
                    assertTrue(service.get().doaPlaying()); assertNull(service.get().station());
                    assertEquals("android.resource",player.get().getCurrentMediaItem().localConfiguration.uri.getScheme());
                    player.get().addListener(new Player.Listener() {
                        @Override public void onPlaybackStateChanged(int state) { if(state==Player.STATE_ENDED) ended.set(true); }
                        @Override public void onPlayerError(PlaybackException e) { if(!ended.get()) error.set(e); }
                    });
                });
                activity.onActivity(a->{ find(a.getWindow().getDecorView(),"Play ERA FM, Malaysia").performClick(); find(a.getWindow().getDecorView(),"Play Suria FM, Malaysia").performClick(); });
                long deadline=SystemClock.elapsedRealtime()+60000;
                AtomicLong maxPosition=new AtomicLong(),duration=new AtomicLong();
                AtomicBoolean unlocked=new AtomicBoolean();
                while(!unlocked.get() && SystemClock.elapsedRealtime()<deadline) {
                    main(()->{
                        if(service.get().doaPlaying()) {
                            assertNull("Radio cannot start during doa",service.get().station());
                            assertEquals("android.resource",player.get().getCurrentMediaItem().localConfiguration.uri.getScheme());
                            maxPosition.set(Math.max(maxPosition.get(),player.get().getCurrentPosition()));
                            if(player.get().getDuration()>0) duration.set(player.get().getDuration());
                        } else unlocked.set(true);
                    });
                    Thread.sleep(100);
                }
                assertNull("Doa decoder must report no error",error.get());
                assertTrue("Doa must naturally reach STATE_ENDED",ended.get());
                assertTrue("Doa duration is complete",duration.get()>17000);
                assertTrue("Decoder played through final audio",maxPosition.get()>=duration.get()-600);
                main(()->{
                    assertFalse(service.get().doaPlaying()); assertNotNull(service.get().station());
                    assertEquals("Suria FM",service.get().station().name);
                    assertEquals("https",player.get().getCurrentMediaItem().localConfiguration.uri.getScheme());
                    service.get().stopPlayback();
                });
            } finally { context.unbindService(connection); }
        } finally { context.stopService(new Intent(context,PlaybackService.class)); }
    }
    private static void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private static View find(View view,String description) {
        if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription())) return view;
        if(view instanceof ViewGroup) { ViewGroup group=(ViewGroup)view; for(int i=0;i<group.getChildCount();i++) { View found=find(group.getChildAt(i),description); if(found!=null) return found; } }
        return null;
    }
}
