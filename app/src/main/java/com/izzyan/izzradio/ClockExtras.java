package com.izzyan.izzradio;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Three nonintrusive panels around the existing digital clock. */
final class ClockExtras {
    static final int REQUEST_LOCATION=51;
    private static final long MAX_LOCATION_AGE_MS=3*60*60*1000L;
    private final MainActivity activity;
    private final FrameLayout stage;
    private final TextView hijriLabel;
    private final TideChartView tide;
    private final MoonPhaseView moon;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean active, destroyed, fetching;
    private long lastRequest=0;
    private double fetchedLatitude=Double.NaN,fetchedLongitude=Double.NaN;
    private String fetchedDay="";
    private String currentDay="";
    private final Runnable noLocationNotice=()->{
        if(active && tide!=null && Double.isNaN(fetchedLatitude)) {
            tide.setStatus("Lokasi tidak tersedia • Semak GPS");
        }
    };

    ClockExtras(MainActivity activity,FrameLayout parent) {
        this.activity=activity; stage=parent;
        hijriLabel=new TextView(activity);
        hijriLabel.setTextSize(18);
        hijriLabel.setGravity(Gravity.CENTER);
        hijriLabel.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));
        hijriLabel.setTextColor(0xff9bdcab);
        hijriLabel.setBackgroundColor(0xcc05090b);
        hijriLabel.setPadding(dp(16),dp(3),dp(16),dp(4));
        FrameLayout.LayoutParams h=new FrameLayout.LayoutParams(-2,dp(34),Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        h.topMargin=dp(4);
        stage.addView(hijriLabel,h);

        tide=new TideChartView(activity);
        FrameLayout.LayoutParams l=new FrameLayout.LayoutParams(dp(180),dp(240),Gravity.BOTTOM|Gravity.LEFT);
        stage.addView(tide,l);
        moon=new MoonPhaseView(activity);
        FrameLayout.LayoutParams r=new FrameLayout.LayoutParams(dp(180),dp(240),Gravity.BOTTOM|Gravity.RIGHT);
        stage.addView(moon,r);

        stage.addOnLayoutChangeListener((v,left,top,right,bottom,ol,ot,oright,ob)->positionPanels());
        updateCalendarAndMoon();
    }
    private int dp(float n) { return Math.round(activity.getResources().getDisplayMetrics().density*n); }

    private void positionPanels() {
        int w=stage.getWidth(),h=stage.getHeight();
        if(w==0||h==0) return;
        int sideWidth=Math.max(dp(132),Math.round(w*.232f));
        int panelHeight=Math.max(dp(142),Math.round(h*.47f));
        int edge=Math.max(dp(9),Math.round(w*.018f));
        int bottom=Math.max(dp(9),Math.round(h*.025f));
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)tide.getLayoutParams();
        lp.width=sideWidth;lp.height=panelHeight;lp.leftMargin=edge;lp.bottomMargin=bottom;
        tide.setLayoutParams(lp);
        FrameLayout.LayoutParams rp=(FrameLayout.LayoutParams)moon.getLayoutParams();
        rp.width=sideWidth;rp.height=panelHeight;rp.rightMargin=edge;rp.bottomMargin=bottom;
        moon.setLayoutParams(rp);
    }
    void setClockColour(int colour) {
        // Muted to avoid glare and preserve the five user-selectable clock colours.
        int r=Math.max(100,(Color.red(colour)*3+120)/4);
        int g=Math.max(100,(Color.green(colour)*3+120)/4);
        int b=Math.max(100,(Color.blue(colour)*3+120)/4);
        hijriLabel.setTextColor(Color.rgb(r,g,b));
    }
    void updateCalendarAndMoon() {
        String day=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        if(!day.equals(currentDay)) {
            currentDay=day;
            String[] months={"Muharam","Safar","Rabiulawal","Rabiulakhir","Jamadilawal",
                    "Jamadilakhir","Rejab","Syaaban","Ramadan","Syawal","Zulkaedah","Zulhijah"};
            try {
                android.icu.util.IslamicCalendar islam=new android.icu.util.IslamicCalendar();
                islam.setCalculationType(android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
                islam.setTime(new Date());
                int d=islam.get(android.icu.util.Calendar.DATE);
                int m=islam.get(android.icu.util.Calendar.MONTH);
                int y=islam.get(android.icu.util.Calendar.YEAR);
                hijriLabel.setText("KALENDAR HIJRAH  •  "+d+" "+months[m]+" "+y+" H");
            } catch(Exception e) { hijriLabel.setText("KALENDAR HIJRAH  •  Tidak tersedia"); }
            moon.setInstant(System.currentTimeMillis());
        }
    }
    void start() {
        if(destroyed) return;
        updateCalendarAndMoon();
        if(active) return;
        active=true;
        requestLocation();
    }
    void stop() {
        active=false;main.removeCallbacks(noLocationNotice);
        if(locationManager!=null && locationListener!=null) {
            try { locationManager.removeUpdates(locationListener); }catch(Exception ignored) {}
        }
        locationListener=null;
    }
    void destroy() {
        destroyed=true;stop();io.shutdownNow();
    }
    void onPermissionResult(int code) {
        if(code==REQUEST_LOCATION && active) requestLocation();
    }
    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void requestLocation() {
        if(!active || destroyed) return;
        if(!hasPermission()) {
            tide.setStatus("Benarkan lokasi untuk graf pasang surut");
            // The user may grant approximate location rather than precise location.
            activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_LOCATION);
            return;
        }
        locationManager=(LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager==null) {
            tide.setStatus("GPS/lokasi tiada pada peranti");
            return;
        }
        boolean providerAvailable=false;
        Location candidate=null;
        for(String provider:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER}) {
            try {
                if(!locationManager.isProviderEnabled(provider)) continue;
                providerAvailable=true;
                Location last=locationManager.getLastKnownLocation(provider);
                if(last!=null && System.currentTimeMillis()-last.getTime()<MAX_LOCATION_AGE_MS &&
                        (candidate==null || last.getTime()>candidate.getTime())) candidate=last;
            } catch(SecurityException | IllegalArgumentException ignored) {}
        }
        if(candidate!=null) foundLocation(candidate);
        else if(!providerAvailable) tide.setStatus("Lokasi dimatikan atau GPS tiada");
        else tide.setStatus("Mengesan lokasi semasa…");
        if(locationListener!=null) {
            try { locationManager.removeUpdates(locationListener); } catch(Exception ignored) {}
        }
        locationListener=location-> {
            if(active && location!=null) foundLocation(location);
        };
        for(String provider:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER}) {
            try {
                if(locationManager.isProviderEnabled(provider)) locationManager.requestLocationUpdates(provider,60000L,500f,locationListener);
            } catch(SecurityException | IllegalArgumentException ignored) {}
        }
        main.removeCallbacks(noLocationNotice);
        main.postDelayed(noLocationNotice,25000L);
    }
    private void foundLocation(Location location) {
        if(!active) return;
        double lat=location.getLatitude(),lon=location.getLongitude();
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat>90||lat< -90||lon>180||lon< -180) return;
        double distance=Math.hypot((lat-fetchedLatitude)*111.0,(lon-fetchedLongitude)*105.0);
        String day=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        String position=String.format(Locale.US,"Lokasi auto %.3f°, %.3f°",lat,lon);
        if(!fetching && (Double.isNaN(fetchedLatitude)||distance>10||System.currentTimeMillis()-lastRequest>3*60*60*1000L||!day.equals(fetchedDay))) {
            tide.setStatus("Memuatkan graf laut berhampiran…");
            tide.setPosition(position);
            loadForecast(lat,lon,position,day);
        }
    }
    private void loadForecast(double latitude,double longitude,String position,String day) {
        if(fetching||destroyed) return;
        fetching=true;
        final long when=System.currentTimeMillis();
        io.execute(()->{
            float[] values=null;String[] times=null;String error=null;
            try {
                String endpoint="https://marine-api.open-meteo.com/v1/marine?latitude="+
                        String.format(Locale.US,"%.5f",latitude)+"&longitude="+
                        String.format(Locale.US,"%.5f",longitude)+
                        "&hourly=sea_level_height_msl&forecast_days=1&timezone=auto&cell_selection=sea";
                HttpURLConnection conn=(HttpURLConnection)new URL(endpoint).openConnection();
                conn.setConnectTimeout(12000);conn.setReadTimeout(12000);
                conn.setRequestProperty("Accept","application/json");
                try {
                    if(conn.getResponseCode()!=200) throw new IOException("HTTP "+conn.getResponseCode());
                    ByteArrayOutputStream out=new ByteArrayOutputStream();
                    try(InputStream input=conn.getInputStream()){
                        byte[] buffer=new byte[4096];int n;
                        while((n=input.read(buffer))!=-1) {
                            if(out.size()+n>250000) throw new IOException("Forecast too large");
                            out.write(buffer,0,n);
                        }
                    }
                    JSONObject json=new JSONObject(out.toString("UTF-8"));
                    JSONObject hourly=json.getJSONObject("hourly");
                    JSONArray heights=hourly.getJSONArray("sea_level_height_msl");
                    JSONArray labels=hourly.getJSONArray("time");
                    int length=Math.min(heights.length(),labels.length());
                    if(length<2) throw new IOException("No tide model coverage");
                    float[] raw=new float[length];String[] keys=new String[length];
                    for(int i=0;i<length;i++) {
                        if(heights.isNull(i)) throw new IOException("Sea level unavailable");
                        raw[i]=(float)heights.getDouble(i);
                        if(!Float.isFinite(raw[i])) throw new IOException("Invalid sea level");
                        String dt=labels.getString(i);
                        keys[i]=dt.length()>=16?dt.substring(11,16):dt;
                    }
                    values=raw;times=keys;
                } finally {conn.disconnect();}
            } catch(Exception e) { error="Ramalan tidak tersedia • Periksa internet"; }
            final float[] v=values;final String[] t=times;final String failure=error;
            main.post(()->{
                if(destroyed) return;
                fetching=false;
                if(failure==null) {
                    fetchedLatitude=latitude;fetchedLongitude=longitude;
                    fetchedDay=day;lastRequest=when;
                    tide.setForecast(v,t,position);
                } else {
                    tide.setStatus(failure);
                    tide.setPosition(position);
                    // Rate-limit repeated failures.
                    fetchedLatitude=latitude;fetchedLongitude=longitude;
                    fetchedDay=day;lastRequest=when;
                }
            });
        });
    }
}
