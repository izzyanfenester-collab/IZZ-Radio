package com.izzyan.izzradio;

import android.content.*;
import android.os.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.*;

public final class MainActivity extends AppCompatActivity {
    private PlaybackService service;
    private boolean bound,clockMode,wasPlaying;
    private LinearLayout dashboard,clock;
    private TextView message,hours,seconds,ampm,date,stationLabel;
    private final List<TextView> weekdays=new ArrayList<>();
    private final Map<String,LinearLayout> cards=new HashMap<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final PlaybackService.Observer observer=this::refresh;
    private final Runnable tick=new Runnable() { public void run() { updateClock(); handler.postDelayed(this,1000-System.currentTimeMillis()%1000); } };
    private final ServiceConnection connection=new ServiceConnection() {
        public void onServiceConnected(ComponentName name,IBinder binder) { service=((PlaybackService.LocalBinder)binder).service(); if(!service.initialized()) ContextCompat.startForegroundService(MainActivity.this,new Intent(MainActivity.this,PlaybackService.class).setAction(PlaybackService.LAUNCH)); service.observe(observer); }
        public void onServiceDisconnected(ComponentName name) { service=null; }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        buildUi();
        if(saved==null) ContextCompat.startForegroundService(this,new Intent(this,PlaybackService.class).setAction(PlaybackService.LAUNCH));
        clockMode=saved!=null && saved.getBoolean("clock"); showClock(clockMode);
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},4);
    }
    @Override protected void onStart() { super.onStart(); bound=bindService(new Intent(this,PlaybackService.class),connection,BIND_AUTO_CREATE); handler.post(tick); }
    @Override protected void onStop() { handler.removeCallbacks(tick); if(service!=null) service.removeObserver(observer); if(bound) { unbindService(connection); bound=false; } service=null; super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("clock",clockMode); super.onSaveInstanceState(state); }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView text(String value,float size,int color) { TextView v=new TextView(this); v.setText(value); v.setTextColor(color); v.setTextSize(size); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private GradientDrawable surface(int color,int border) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12)); if(border!=0) d.setStroke(dp(1),border); return d; }
    private void buildUi() {
        AspectFrame frame=new AspectFrame(this); setContentView(frame);
        FrameLayout content=new FrameLayout(this); content.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); frame.addView(content);
        dashboard=new LinearLayout(this); dashboard.setOrientation(LinearLayout.VERTICAL); dashboard.setPadding(dp(18),dp(8),dp(18),dp(8)); dashboard.setBackgroundColor(0xff080f1d); content.addView(dashboard,new FrameLayout.LayoutParams(-1,-1));
        TextView title=text("IZZ Radio — Malaysia & Singapore Radio",20,0xffd3dbe8); title.setTypeface(null,Typeface.BOLD); dashboard.addView(title,new LinearLayout.LayoutParams(-1,dp(36)));
        message=text("Doa Menaiki Kenderaan • select a station to queue",12,0xff98a9c4); dashboard.addView(message,new LinearLayout.LayoutParams(-1,dp(26))); message.setContentDescription("Playback status. Tap to resume the doa if paused."); message.setOnClickListener(v->{ if(service!=null) service.resumeDoa(); });
        LinearLayout panes=new LinearLayout(this); dashboard.addView(panes,new LinearLayout.LayoutParams(-1,0,1));
        List<Station> catalog=Station.load(this);
        for(String country:new String[]{"Malaysia","Singapore"}) {
            LinearLayout pane=new LinearLayout(this); pane.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-1,1); pp.setMargins(country.equals("Singapore")?dp(8):0,0,country.equals("Malaysia")?dp(8):0,0); panes.addView(pane,pp);
            TextView heading=text(country.toUpperCase(Locale.ROOT),13,0xff8b9bb8); pane.addView(heading,new LinearLayout.LayoutParams(-1,dp(28)));
            ScrollView scroll=new ScrollView(this); pane.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
            for(Station s:catalog) if(s.country.equals(country)) {
                LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(10),dp(6),dp(10),dp(6)); row.setBackground(surface(0xff121e31,0xff26334a));
                LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(64)); rp.bottomMargin=dp(8); list.addView(row,rp); cards.put(s.id,row);
                ImageView logo=new ImageView(this); logo.setScaleType(ImageView.ScaleType.FIT_CENTER); logo.setContentDescription(s.name+" artwork"); row.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
                if(s.localLogo.equals("logo_suria")) logo.setImageResource(R.drawable.logo_suria);
                else { String artwork=s.logo; if(artwork.isEmpty() && !s.logoPage.isEmpty()) artwork=android.net.Uri.parse(s.logoPage).buildUpon().path("/favicon.ico").clearQuery().fragment(null).build().toString(); Glide.with(this).load(artwork).placeholder(R.drawable.ic_radio).error(R.drawable.ic_radio).into(logo); }
                LinearLayout labels=new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL); labels.setPadding(dp(10),0,dp(4),0); row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
                TextView name=text(s.name,16,0xffd7dfeb); name.setMaxLines(1); name.setEllipsize(android.text.TextUtils.TruncateAt.END); labels.addView(name); labels.addView(text(country,11,0xff8191a9));
                TextView play=text("▶",22,0xff9cabc4); row.addView(play); row.setContentDescription("Play "+s.name+", "+country); row.setFocusable(true);
                row.setOnClickListener(v->{ if(service==null) { Toast.makeText(this,"Preparing playback…",Toast.LENGTH_SHORT).show(); return; } boolean queued=service.doaPlaying(); ContextCompat.startForegroundService(this,new Intent(this,PlaybackService.class).setAction(PlaybackService.SELECT).putExtra("station",s.id)); if(queued) Toast.makeText(this,s.name+" will start after the doa",Toast.LENGTH_SHORT).show(); });
            }
        }
        clock=new LinearLayout(this); clock.setOrientation(LinearLayout.VERTICAL); clock.setGravity(Gravity.CENTER); clock.setBackgroundColor(0xff000000); content.addView(clock,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout digits=new LinearLayout(this); digits.setGravity(Gravity.CENTER); clock.addView(digits);
        Typeface digitalFont;
        try { digitalFont=Typeface.createFromAsset(getAssets(),"fonts/ds_digital.ttf"); }
        catch(Exception ignored) { digitalFont=Typeface.MONOSPACE; }
        final int ledGreen=0xff39e639;
        hours=text("",108,ledGreen); hours.setTypeface(digitalFont); hours.setShadowLayer(dp(5),0,0,0xff0d6b18); digits.addView(hours);
        LinearLayout side=new LinearLayout(this); side.setOrientation(LinearLayout.VERTICAL); side.setPadding(dp(16),0,0,0); digits.addView(side);
        seconds=text("",36,ledGreen); seconds.setTypeface(digitalFont); seconds.setShadowLayer(dp(3),0,0,0xff0d6b18);
        ampm=text("",22,ledGreen); ampm.setTypeface(digitalFont); ampm.setShadowLayer(dp(2),0,0,0xff0d6b18);
        side.addView(seconds); side.addView(ampm);
        LinearLayout days=new LinearLayout(this); days.setGravity(Gravity.CENTER); clock.addView(days);
        for(String day:new String[]{"MON","TUE","WED","THU","FRI","SAT","SUN"}) { TextView tv=text(day,16,0xff102314); tv.setTypeface(digitalFont); tv.setPadding(dp(10),dp(8),dp(10),dp(8)); days.addView(tv); weekdays.add(tv); }
        date=text("",26,ledGreen); date.setTypeface(digitalFont); date.setShadowLayer(dp(2),0,0,0xff0d6b18); date.setGravity(Gravity.CENTER); clock.addView(date);
        stationLabel=text("",26,0xffd2d6d9); stationLabel.setPadding(0,dp(16),0,0); stationLabel.setGravity(Gravity.CENTER); stationLabel.setTypeface(null,Typeface.BOLD); clock.addView(stationLabel);
        clock.setContentDescription("Night clock. Tap for stations; long press to stop radio."); clock.setOnClickListener(v->showClock(false)); clock.setOnLongClickListener(v->{if(service!=null) service.stopPlayback(); showClock(false); return true;});
        updateClock();
    }
    private void refresh() {
        if(service==null) return;
        message.setText(service.status()); Station current=service.station(); stationLabel.setText(current==null?"":current.name);
        boolean playing=service.radioPlaying(); if(playing && !wasPlaying) showClock(true); wasPlaying=playing;
        for(Map.Entry<String,LinearLayout> entry:cards.entrySet()) entry.getValue().setBackground(surface(current!=null && entry.getKey().equals(current.id)?0xff203552:0xff121e31,current!=null && entry.getKey().equals(current.id)?0xff8599b7:0xff26334a));
        if(current==null && !service.doaPlaying()) showClock(false);
    }
    private void showClock(boolean value) { clockMode=value; dashboard.setVisibility(value?View.GONE:View.VISIBLE); clock.setVisibility(value?View.VISIBLE:View.GONE); WindowManager.LayoutParams params=getWindow().getAttributes(); params.screenBrightness=value?0.10f:WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; getWindow().setAttributes(params); if(value) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); }
    private void updateClock() { if(hours==null) return; Date now=new Date(); hours.setText(new SimpleDateFormat("hh:mm",Locale.US).format(now)); seconds.setText(new SimpleDateFormat("ss",Locale.US).format(now)); ampm.setText(new SimpleDateFormat("a",Locale.US).format(now)); date.setText(new SimpleDateFormat("dd MM yyyy",Locale.US).format(now)); int active=(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+5)%7; for(int i=0;i<weekdays.size();i++) weekdays.get(i).setTextColor(i==active?0xff39e639:0xff102314); }
    @Override public void onBackPressed() { if(clockMode) showClock(false); else super.onBackPressed(); }
}
