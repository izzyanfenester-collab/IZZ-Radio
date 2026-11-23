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
    private boolean bound,clockMode,wasPlaying,manualClockRequest;
    private LinearLayout dashboard,clock;
    private FrameLayout clockStage;
    private ClockExtras clockExtras;
    private ImageView selectionWallpaper;
    private View selectionShade;
    private LedMarqueeView ledBanner;
    private TextView message,hours,seconds,ampm,date,stationLabel,nowPlayingLabel;
    private int clockColor=0xff39e639;
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
        if(saved==null && (getIntent()==null || !getIntent().getBooleanExtra("wake_ui_only",false))) {
            Intent serviceIntent=new Intent(this,PlaybackService.class).setAction(PlaybackService.LAUNCH);
            if(getIntent()!=null && getIntent().getBooleanExtra("auto_resume_last",false)) {
                String last=getSharedPreferences("izz_radio_prefs",MODE_PRIVATE).getString("last_station_id","");
                if(last!=null && !last.isEmpty()) serviceIntent.putExtra("resume_station",last);
            }
            ContextCompat.startForegroundService(this,serviceIntent);
        }
        manualClockRequest=saved!=null && saved.getBoolean("manualClock",false);
        clockMode=saved!=null && saved.getBoolean("clock"); showClock(clockMode);
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},4);
    }
    @Override protected void onStart() { super.onStart(); if(ledBanner!=null) ledBanner.setActive(!clockMode); bound=bindService(new Intent(this,PlaybackService.class),connection,BIND_AUTO_CREATE); handler.post(tick); }
    @Override protected void onStop() { if(ledBanner!=null) ledBanner.setActive(false); handler.removeCallbacks(tick); if(service!=null) service.removeObserver(observer); if(bound) { unbindService(connection); bound=false; } service=null; super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("clock",clockMode); state.putBoolean("manualClock",manualClockRequest); super.onSaveInstanceState(state); }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView text(String value,float size,int color) { TextView v=new TextView(this); v.setText(value); v.setTextColor(color); v.setTextSize(size); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private GradientDrawable surface(int color,int border) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12)); if(border!=0) d.setStroke(dp(1),border); return d; }
    private int shadeColor(int color,float factor) {
        int r=Math.max(0,Math.min(255,Math.round(((color>>16)&255)*factor)));
        int g=Math.max(0,Math.min(255,Math.round(((color>>8)&255)*factor)));
        int b=Math.max(0,Math.min(255,Math.round((color&255)*factor)));
        return 0xff000000|(r<<16)|(g<<8)|b;
    }
    private void applyClockColor(int color) {
        clockColor=color;
        getSharedPreferences("izz_radio_prefs",MODE_PRIVATE).edit().putInt("clock_color",color).apply();
        if(clockExtras!=null) clockExtras.setClockColour(color);
        if(hours==null) return;
        int glow=shadeColor(color,0.35f);
        hours.setTextColor(color); seconds.setTextColor(color); ampm.setTextColor(color); date.setTextColor(color);
        hours.setShadowLayer(dp(5),0,0,glow); seconds.setShadowLayer(dp(3),0,0,glow); ampm.setShadowLayer(dp(2),0,0,glow); date.setShadowLayer(dp(2),0,0,glow);
        updateClock();
    }
    private void showSettings() {
        android.content.SharedPreferences prefs=getSharedPreferences("izz_radio_prefs",MODE_PRIVATE);
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(22),dp(10),dp(22),dp(8));

        Switch autoBoot=new Switch(this); autoBoot.setText("Auto-launch on Boot"); autoBoot.setTextSize(16); autoBoot.setChecked(prefs.getBoolean("auto_launch_boot",false));
        autoBoot.setOnCheckedChangeListener((button,checked)->prefs.edit().putBoolean("auto_launch_boot",checked).apply());
        panel.addView(autoBoot,new LinearLayout.LayoutParams(-1,dp(52)));

        Switch autoWake=new Switch(this); autoWake.setText("Auto-launch after Wake / Sleep"); autoWake.setTextSize(16); autoWake.setChecked(prefs.getBoolean("auto_launch_wake",false));
        autoWake.setOnCheckedChangeListener((button,checked)->prefs.edit().putBoolean("auto_launch_wake",checked).apply());
        panel.addView(autoWake,new LinearLayout.LayoutParams(-1,dp(52)));

        TextView colorTitle=text("Digital clock colour",16,0xff202020); colorTitle.setTypeface(null,Typeface.BOLD); colorTitle.setPadding(0,dp(12),0,dp(4)); panel.addView(colorTitle);

        final String[] names={"Hijau","Biru","Amber / Oren","Putih","Merah"};
        final int[] colors={0xff39e639,0xff245a9c,0xffffa000,0xfff0f0f0,0xffd94141};
        RadioGroup group=new RadioGroup(this); group.setOrientation(RadioGroup.VERTICAL);
        for(int i=0;i<names.length;i++) {
            RadioButton rb=new RadioButton(this); rb.setText(names[i]); rb.setTextSize(16); rb.setId(View.generateViewId());
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(colors[i]));
            if(clockColor==colors[i]) rb.setChecked(true);
            final int selected=colors[i];
            rb.setOnClickListener(v->applyClockColor(selected));
            group.addView(rb,new RadioGroup.LayoutParams(-1,dp(42)));
        }
        panel.addView(group);

        ScrollView settingsScroll=new ScrollView(this);
        settingsScroll.setFillViewport(true);
        settingsScroll.setVerticalScrollBarEnabled(true);
        settingsScroll.addView(panel,new ScrollView.LayoutParams(-1,-2));

        androidx.appcompat.app.AlertDialog dialog=new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("IZZ Radio Settings")
                .setView(settingsScroll)
                .setPositiveButton("Done",null)
                .create();
        dialog.setOnShowListener(d->{
            Window w=dialog.getWindow();
            if(w!=null) {
                WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
                lp.copyFrom(w.getAttributes());
                lp.width=Math.min(dp(790),getResources().getDisplayMetrics().widthPixels-dp(48));
                lp.height=Math.min(dp(510),getResources().getDisplayMetrics().heightPixels-dp(36));
                w.setAttributes(lp);
            }
        });
        dialog.show();
    }
    private void buildUi() {
        AspectFrame frame=new AspectFrame(this); setContentView(frame);
        FrameLayout content=new FrameLayout(this); content.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); frame.addView(content);

        selectionWallpaper=new ImageView(this); selectionWallpaper.setImageResource(R.drawable.radio_selector_wallpaper); selectionWallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP); content.addView(selectionWallpaper,new FrameLayout.LayoutParams(-1,-1));
        selectionShade=new View(this); selectionShade.setBackgroundColor(0x59030a12); content.addView(selectionShade,new FrameLayout.LayoutParams(-1,-1));

        dashboard=new LinearLayout(this); dashboard.setOrientation(LinearLayout.VERTICAL); dashboard.setPadding(dp(24),dp(12),dp(24),dp(12)); dashboard.setBackgroundColor(0x00000000); content.addView(dashboard,new FrameLayout.LayoutParams(-1,-1));
        clockColor=getSharedPreferences("izz_radio_prefs",MODE_PRIVATE).getInt("clock_color",0xff39e639);

        LinearLayout titleRow=new LinearLayout(this); titleRow.setGravity(Gravity.CENTER_VERTICAL); dashboard.addView(titleRow,new LinearLayout.LayoutParams(-1,dp(48)));
        // V1.41: real dot-matrix LED takes the old title's space without moving controls.
        ledBanner=new LedMarqueeView(this);
        LinearLayout.LayoutParams bannerLp=new LinearLayout.LayoutParams(0,dp(42),1);
        bannerLp.setMargins(0,dp(3),dp(6),dp(3));
        titleRow.addView(ledBanner,bannerLp);

        TextView saver=text("Screen saver",16,0xffffffff);
        saver.setTypeface(null,Typeface.BOLD);
        saver.setGravity(Gravity.CENTER);
        saver.setBackground(surface(0xff11668c,0xff2d88a9));
        saver.setContentDescription("Open Digital Clock screensaver");
        LinearLayout.LayoutParams saverLp=new LinearLayout.LayoutParams(dp(132),dp(40));
        saverLp.setMargins(dp(4),dp(4),dp(5),dp(4));
        titleRow.addView(saver,saverLp);
        saver.setOnClickListener(v->{ manualClockRequest=true; showClock(true); });

        TextView stop=text("■  STOP",14,0xffffffff); stop.setTypeface(null,Typeface.BOLD); stop.setGravity(Gravity.CENTER); stop.setBackground(surface(0xdd8b2525,0xffd05a5a));
        LinearLayout.LayoutParams stopLp=new LinearLayout.LayoutParams(dp(104),dp(40)); stopLp.setMargins(dp(8),dp(4),dp(8),dp(4)); titleRow.addView(stop,stopLp);
        stop.setOnClickListener(v->{ if(service!=null) service.stopPlayback(); showClock(false); });

        TextView gear=text("⚙",27,0xfff4f0e8); gear.setGravity(Gravity.CENTER); gear.setBackground(surface(0xaa152435,0x997c8fa6));
        LinearLayout.LayoutParams gearLp=new LinearLayout.LayoutParams(dp(52),dp(40)); gearLp.setMargins(dp(2),dp(4),0,dp(4)); titleRow.addView(gear,gearLp);
        gear.setContentDescription("Settings"); gear.setOnClickListener(v->showSettings());

        message=text("Playing Selawat • select a station to queue",13,0xffd6dce5);
        dashboard.addView(message,new LinearLayout.LayoutParams(-1,dp(30)));
        message.setVisibility(View.GONE); // Avoid duplicate text while the LED already carries status.
        message.setContentDescription("Playback warning. Tap to retry Selawat if playback failed.");
        message.setOnClickListener(v->{ if(service!=null && service.doaPlaying()) service.resumeDoa(); });
        LinearLayout panes=new LinearLayout(this); dashboard.addView(panes,new LinearLayout.LayoutParams(-1,0,1));
        List<Station> catalog=Station.load(this);
        for(String country:new String[]{"Malaysia","Singapore"}) {
            LinearLayout pane=new LinearLayout(this); pane.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-1,1); pp.setMargins(country.equals("Singapore")?dp(8):0,0,country.equals("Malaysia")?dp(8):0,0); panes.addView(pane,pp);
            TextView heading=text(country.toUpperCase(Locale.ROOT),14,0xffffd6a0); heading.setTypeface(null,Typeface.BOLD); pane.addView(heading,new LinearLayout.LayoutParams(-1,dp(30)));
            ScrollView scroll=new ScrollView(this);
            scroll.setFillViewport(true);
            pane.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            GridLayout grid=new GridLayout(this);
            grid.setColumnCount(3);
            grid.setOrientation(GridLayout.HORIZONTAL);
            scroll.addView(grid,new ScrollView.LayoutParams(-1,-2));
            // Keep all three columns square as the car head-unit width changes.
            grid.addOnLayoutChangeListener((v,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
                int width=grid.getWidth();
                if(width<=0) return;
                int side=Math.max(1,(width-dp(24))/3);
                for(int i=0;i<grid.getChildCount();i++) {
                    View tile=grid.getChildAt(i);
                    GridLayout.LayoutParams lp=(GridLayout.LayoutParams)tile.getLayoutParams();
                    if(lp.width!=side || lp.height!=side) {
                        lp.width=side;
                        lp.height=side;
                        tile.setLayoutParams(lp);
                    }
                }
            });
            for(Station s:catalog) if(s.country.equals(country)) {
                LinearLayout tile=new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER_HORIZONTAL);
                // Keep the original outer radio card; reclaim padding for larger frameless artwork.
                tile.setPadding(dp(3),dp(3),dp(3),dp(3));
                tile.setBackground(surface(0xdc101a28,0xaa526171));
                tile.setElevation(dp(2));
                tile.setForeground(new android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(0x22ffd6a0),null,null));
                GridLayout.LayoutParams tileLp=new GridLayout.LayoutParams();
                tileLp.width=dp(90);
                tileLp.height=dp(90);
                tileLp.setMargins(dp(4),dp(4),dp(4),dp(4));
                grid.addView(tile,tileLp);
                cards.put(s.id,tile);

                ImageView logo=new ImageView(this);
                logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                logo.setContentDescription(s.name+" artwork");
                // V1.40: absolutely no inner logo frame, stroke, shadow or extra padding.
                // Source logo dimensions and transparency remain unchanged.
                logo.setBackground(null);
                logo.setPadding(0,0,0,0);
                logo.setClipToOutline(false);
                logo.setCropToPadding(false);
                LinearLayout.LayoutParams imageLp=new LinearLayout.LayoutParams(-1,0,1);
                imageLp.setMargins(dp(2),dp(1),dp(2),dp(1));
                tile.addView(logo,imageLp);
                if(s.localLogo.startsWith("logos/")) setExactStationLogo(logo,s.localLogo);
                else if(s.name.equals("SINAR FM")) setExactStationLogo(logo,"logos/sinar.png");
                else if(s.name.equals("HITZ FM")) setExactStationLogo(logo,"logos/hitz.webp");
                else if(s.localLogo.equals("logo_suria")) logo.setImageResource(R.drawable.logo_suria);
                else {
                    String artwork=s.logo;
                    if(artwork.isEmpty() && !s.logoPage.isEmpty()) {
                        artwork="https://www.google.com/s2/favicons?sz=128&domain_url="+android.net.Uri.encode(s.logoPage);
                    }
                    Glide.with(this)
                            .load(artwork)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_radio)
                            .error(R.drawable.ic_radio)
                            .dontAnimate()
                            .into(logo);
                }

                TextView name=text(s.name,15,0xfff1f4f8);
                name.setTypeface(null,Typeface.BOLD);
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(1);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                // Give the frameless logo as much vertical card area as possible.
                tile.addView(name,new LinearLayout.LayoutParams(-1,dp(21)));
                LinearLayout footer=new LinearLayout(this);
                footer.setOrientation(LinearLayout.HORIZONTAL);
                footer.setGravity(Gravity.CENTER_VERTICAL);
                tile.addView(footer,new LinearLayout.LayoutParams(-1,dp(18)));
                TextView location=text(country,11,0xffb5c0cf);
                location.setGravity(Gravity.CENTER);
                footer.addView(location,new LinearLayout.LayoutParams(0,-1,1));
                TextView play=text("▶",16,0xffdce6f2);
                play.setGravity(Gravity.CENTER);
                footer.addView(play,new LinearLayout.LayoutParams(dp(25),-1));

                tile.setContentDescription("Play "+s.name+", "+country);
                tile.setFocusable(true);
                tile.setOnClickListener(v->{
                    if(service==null) {
                        Toast.makeText(this,"Preparing playback…",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean queued=service.doaPlaying();
                    ContextCompat.startForegroundService(this,new Intent(this,PlaybackService.class)
                            .setAction(PlaybackService.SELECT).putExtra("station",s.id));
                    if(queued) Toast.makeText(this,s.name+" will start after Selawat",Toast.LENGTH_SHORT).show();
                });
            }
        }
        clockStage=new FrameLayout(this); clockStage.setBackgroundColor(0xff000000); content.addView(clockStage,new FrameLayout.LayoutParams(-1,-1));
        clock=new LinearLayout(this); clock.setOrientation(LinearLayout.VERTICAL); clock.setGravity(Gravity.CENTER); clock.setBackgroundColor(0xff000000); clockStage.addView(clock,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout digits=new LinearLayout(this); digits.setGravity(Gravity.CENTER); clock.addView(digits);
        Typeface digitalFont;
        try { digitalFont=Typeface.createFromAsset(getAssets(),"fonts/ds_digital.ttf"); }
        catch(Exception ignored) { digitalFont=Typeface.MONOSPACE; }
        int glow=shadeColor(clockColor,0.35f);
        hours=text("",216,clockColor); hours.setTypeface(digitalFont); hours.setShadowLayer(dp(5),0,0,glow); digits.addView(hours);
        LinearLayout side=new LinearLayout(this); side.setOrientation(LinearLayout.VERTICAL); side.setPadding(dp(16),0,0,0); digits.addView(side);
        seconds=text("",72,clockColor); seconds.setTypeface(digitalFont); seconds.setShadowLayer(dp(3),0,0,glow);
        ampm=text("",44,clockColor); ampm.setTypeface(digitalFont); ampm.setShadowLayer(dp(2),0,0,glow);
        side.addView(seconds); side.addView(ampm);
        LinearLayout days=new LinearLayout(this); days.setGravity(Gravity.CENTER); clock.addView(days);
        for(String day:new String[]{"MON","TUE","WED","THU","FRI","SAT","SUN"}) { TextView tv=text(day,32,shadeColor(clockColor,0.20f)); tv.setTypeface(digitalFont); tv.setPadding(dp(10),dp(8),dp(10),dp(8)); days.addView(tv); weekdays.add(tv); }
        date=text("",52,clockColor); date.setTypeface(digitalFont); date.setShadowLayer(dp(2),0,0,glow); date.setGravity(Gravity.CENTER); clock.addView(date);
        stationLabel=text("",52,0xffd2d6d9); stationLabel.setPadding(0,dp(16),0,0); stationLabel.setGravity(Gravity.CENTER); stationLabel.setTypeface(null,Typeface.BOLD); clock.addView(stationLabel);
        nowPlayingLabel=text("",27,0xfff0f0f0); nowPlayingLabel.setPadding(dp(28),dp(4),dp(28),0); nowPlayingLabel.setGravity(Gravity.CENTER); nowPlayingLabel.setSingleLine(true); nowPlayingLabel.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE); nowPlayingLabel.setMarqueeRepeatLimit(-1); nowPlayingLabel.setSelected(true); clock.addView(nowPlayingLabel,new LinearLayout.LayoutParams(-1,dp(42)));
        clockExtras=new ClockExtras(this,clockStage);
        clockExtras.setClockColour(clockColor);
        clock.setContentDescription("Night clock. Tap for stations; long press to stop radio."); clock.setOnClickListener(v->showClock(false)); clock.setOnLongClickListener(v->{if(service!=null) service.stopPlayback(); showClock(false); return true;});
        updateClock();
    }
    /** Exact bundled logos; never request the old network logos or reuse their Glide cache. */
    private void setExactStationLogo(ImageView view,String assetPath) {
        try(java.io.InputStream stream=getAssets().open(assetPath);
            java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096];
            int n;
            while((n=stream.read(buffer))!=-1) output.write(buffer,0,n);
            byte[] bytes=output.toByteArray();
            android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            if(bitmap==null) throw new java.io.IOException("Invalid bundled station logo");
            view.setImageBitmap(bitmap);
        }catch(Exception error){
            android.util.Log.e("IZZRadio","Could not decode "+assetPath,error);
            view.setImageResource(R.drawable.ic_radio);
        }
    }

    private void refresh() {
        if(service==null) return;
        String status=service.status();
        Station current=service.station();
        ledBanner.setDisplay(LedBannerState.from(current==null?null:current.name,
                status, service.nowPlaying(), service.doaPlaying(),
                service.radioPlaying(), service.buffering()));
        // Retain retry and error instructions, but remove duplicated normal status text.
        boolean warning=status!=null && (status.toLowerCase(Locale.ROOT).contains("failed") ||
                status.startsWith("Unable to play"));
        message.setText(status);
        message.setVisibility(warning?View.VISIBLE:View.GONE);
        stationLabel.setText(current==null?"":current.name);
        nowPlayingLabel.setText(current==null?"":service.nowPlaying());
        boolean playing=service.radioPlaying(); if(playing && !wasPlaying) showClock(true); wasPlaying=playing;
        for(Map.Entry<String,LinearLayout> entry:cards.entrySet()) entry.getValue().setBackground(
                surface(current!=null && entry.getKey().equals(current.id)?0xee294766:0xdc101a28,
                        current!=null && entry.getKey().equals(current.id)?0xffe3bf80:0xaa526171));
        if(current==null && !service.doaPlaying() && !manualClockRequest) showClock(false);
    }
    private void showClock(boolean value) { if(!value) manualClockRequest=false; clockMode=value; if(ledBanner!=null) ledBanner.setActive(!value); selectionWallpaper.setVisibility(value?View.GONE:View.VISIBLE); selectionShade.setVisibility(value?View.GONE:View.VISIBLE); dashboard.setVisibility(value?View.GONE:View.VISIBLE); clockStage.setVisibility(value?View.VISIBLE:View.GONE); WindowManager.LayoutParams params=getWindow().getAttributes(); params.screenBrightness=value?0.10f:WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; getWindow().setAttributes(params); if(value) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); }
    private void updateClock() { if(hours==null) return; if(clockExtras!=null) clockExtras.updateHijriCalendar(); Date now=new Date(); hours.setText(new SimpleDateFormat("hh:mm",Locale.US).format(now)); seconds.setText(new SimpleDateFormat("ss",Locale.US).format(now)); ampm.setText(new SimpleDateFormat("a",Locale.US).format(now)); date.setText(new SimpleDateFormat("dd MM yyyy",Locale.US).format(now)); int active=(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+5)%7; for(int i=0;i<weekdays.size();i++) weekdays.get(i).setTextColor(i==active?clockColor:shadeColor(clockColor,0.20f)); }
    @Override public void onBackPressed() { if(clockMode) showClock(false); else super.onBackPressed(); }
}
