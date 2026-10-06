package com.izzyan.izzradio;

import android.app.*;
import android.content.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.*;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.session.MediaSession;
import java.util.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class PlaybackService extends Service {
    static final String LAUNCH="com.izzyan.izzradio.LAUNCH", SELECT="com.izzyan.izzradio.SELECT", STOP="com.izzyan.izzradio.STOP";
    interface Observer { void changed(); }
    final class LocalBinder extends Binder { PlaybackService service() { return PlaybackService.this; } }
    private final LocalBinder binder=new LocalBinder();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final PlaybackGate gate=new PlaybackGate();
    private final List<Observer> observers=new ArrayList<>();
    private List<Station> stations;
    private ExoPlayer player;
    private MediaSession session;
    private Station current;
    private int generation;
    private StreamAttempts attempts=new StreamAttempts();
    private String status="Preparing doa…";
    private boolean stopped;
    private boolean initialized;
    boolean initialized() { return initialized; }
    void resumeDoa() { if(gate.locked() && initialized) player.play(); }
    private Runnable retry;
    void observe(Observer o) { observers.add(o); o.changed(); }
    void removeObserver(Observer o) { observers.remove(o); }
    boolean doaPlaying() { return gate.locked(); }
    Station station() { return current; }
    boolean radioPlaying() { return !gate.locked() && current!=null && player.isPlaying(); }
    String status() { return status; }
    @Override public void onCreate() {
        super.onCreate(); stations=Station.load(this);
        if(Build.VERSION.SDK_INT>=26) { NotificationChannel channel=new NotificationChannel("playback","Radio playback",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager.class).createNotificationChannel(channel); }
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setConnectTimeoutMs(12000).setReadTimeoutMs(25000).setAllowCrossProtocolRedirects(true).setUserAgent("IZZ-Radio/1.7");
        player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(new androidx.media3.datasource.DefaultDataSource.Factory(this,http)).setLoadErrorHandlingPolicy(new DefaultLoadErrorHandlingPolicy(0))).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);
        player.setHandleAudioBecomingNoisy(true); player.setWakeMode(C.WAKE_MODE_NETWORK);
        session=new MediaSession.Builder(this,player).setCallback(new MediaSession.Callback() {
            @Override public MediaSession.ConnectionResult onConnect(MediaSession s, MediaSession.ControllerInfo controller) {
                // External controls cannot seek/skip the startup doa or replace media.
                return MediaSession.ConnectionResult.reject();
            }
        }).build();
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if(state==Player.STATE_ENDED && gate.locked() && !stopped) finishDoa(false);
                else if(state==Player.STATE_ENDED && current!=null && !stopped) failedStream();
            }
            @Override public void onPlayerError(PlaybackException error) {
                if(stopped) return;
                if(gate.locked()) finishDoa(true); else failedStream();
            }
            @Override public void onIsPlayingChanged(boolean playing) {
                if(!gate.locked() && current!=null && playing) status="Playing "+current.name;
                publish();
            }
        });
    }
    @Override public IBinder onBind(Intent intent) { return binder; }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(intent==null) { stopSelf(); return START_NOT_STICKY; }
        String action=intent.getAction();
        if(STOP.equals(action)) { stopPlayback(); return START_NOT_STICKY; }
        startForeground(17,notification());
        if(LAUNCH.equals(action)) freshLaunch();
        else if(SELECT.equals(action)) select(intent.getStringExtra("station"));
        return START_NOT_STICKY;
    }
    private void cancelPending() { generation++; if(retry!=null) handler.removeCallbacks(retry); retry=null; }
    private void freshLaunch() {
        initialized=true;
        stopped=true; cancelPending(); player.stop(); player.clearMediaItems(); current=null;
        gate.freshLaunch(); stopped=false; status="Doa Menaiki Kenderaan • select a station to queue";
        player.setMediaItem(MediaItem.fromUri("android.resource://"+getPackageName()+"/"+R.raw.doa_menaiki_kenderaan));
        player.prepare(); player.play(); publish();
    }
    private void select(String id) {
        if(find(id)==null) return;
        String ready=gate.select(id);
        if(ready==null) { status=find(id).name+" will start after the doa"; publish(); }
        else startStation(ready);
    }
    private Station find(String id) { for(Station s:stations) if(s.id.equals(id)) return s; return null; }
    private void finishDoa(boolean error) {
        String next=gate.doaFinished();
        status=error?"Doa unavailable; radio playback unlocked":"Doa complete • select a station";
        if(next!=null) startStation(next); else { publish(); stopForeground(STOP_FOREGROUND_REMOVE); }
    }
    private void startStation(String id) {
        if(gate.locked()) return;
        stopped=true; cancelPending(); player.stop(); current=find(id); attempts=new StreamAttempts(); stopped=false;
        playAttempt();
    }
    private void playAttempt() {
        if(stopped || current==null || gate.locked()) return;
        String url=attempts.index()==1?current.fallback:current.primary;
        status="Connecting to "+current.name+(attempts.index()==1?" • fallback":attempts.index()==2?" • final retry":"");
        MediaItem.Builder item=new MediaItem.Builder().setUri(url).setMediaMetadata(new MediaMetadata.Builder().setTitle(current.name).setArtist(current.country).build());
        if(url.contains("m3u8")) item.setMimeType(MimeTypes.APPLICATION_M3U8);
        startForeground(17,notification()); player.setMediaItem(item.build()); player.prepare(); player.play(); publish();
    }
    private void failedStream() {
        if(current==null || retry!=null) return;
        if(!attempts.advance(!current.fallback.isEmpty())) { stopped=true; player.stop(); status="Unable to play "+current.name+". Try again or choose another station."; current=null; publish(); stopForeground(STOP_FOREGROUND_REMOVE); return; }
        int expected=generation; long delay=attempts.delayMillis();
        retry=()->{ retry=null; if(expected==generation && !stopped) playAttempt(); };
        handler.postDelayed(retry,delay);
    }
    void stopPlayback() {
        // Stop never unlocks or truncates the doa; discard queued radio instead.
        if(gate.locked()) { gate.clearQueue(); status="Doa playing • queued station cleared"; publish(); return; }
        stopped=true; cancelPending(); player.stop(); player.clearMediaItems(); current=null; status="Playback stopped"; publish(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,PlaybackService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this,"playback").setSmallIcon(R.drawable.ic_radio).setContentTitle(current==null?"IZZ Radio • Doa":current.name).setContentText(status).setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).addAction(R.drawable.ic_radio,"Stop",stop).setStyle(new androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)).build();
    }
    private void publish() { for(Observer o:new ArrayList<>(observers)) o.changed(); if(!stopped) getSystemService(NotificationManager.class).notify(17,notification()); }
    @Override public void onDestroy() { stopped=true; cancelPending(); session.release(); player.release(); super.onDestroy(); }
}
