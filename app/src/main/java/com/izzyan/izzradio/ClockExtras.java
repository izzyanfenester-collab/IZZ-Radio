package com.izzyan.izzradio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hijrah date + lunar globe left + tide-only curve right.
 * Pure black background throughout. No cards, titles or change to clock controls.
 */
final class ClockExtras {
    static final int LOCATION_REQUEST=51;
    private static final long REFRESH_MILLIS=3*60*60*1000L;
    private final MainActivity activity;
    private final FrameLayout stage;
    private final TextView hijriLabel;
    private final MoonPhaseView moon;
    private final TideChartView tide;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LocationManager locations;
    private LocationListener locationListener;
    private boolean active,destroyed,fetching;
    private double lastLat=Double.NaN,lastLon=Double.NaN;
    private long lastFetchMillis;
    private String currentDay="";

    ClockExtras(MainActivity a,FrameLayout holder) {
        activity=a;stage=holder;
        hijriLabel=new TextView(a);
        hijriLabel.setTextSize(18);
        hijriLabel.setGravity(Gravity.CENTER);
        hijriLabel.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        hijriLabel.setTextColor(0xff9bdcab);
        hijriLabel.setBackgroundColor(0xcc05090b);
        hijriLabel.setPadding(dp(16),dp(3),dp(16),dp(4));
        FrameLayout.LayoutParams title=new FrameLayout.LayoutParams(-2,dp(34),Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        title.topMargin=dp(4);stage.addView(hijriLabel,title);

        moon=new MoonPhaseView(a);
        tide=new TideChartView(a);
        stage.addView(moon,new FrameLayout.LayoutParams(dp(190),dp(220),Gravity.TOP|Gravity.LEFT));
        stage.addView(tide,new FrameLayout.LayoutParams(dp(240),dp(150),Gravity.TOP|Gravity.RIGHT));
        stage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,oright,ob)->layoutExtras());
        updateHijriCalendar();
    }
    private int dp(float n) { return Math.round(n*activity.getResources().getDisplayMetrics().density); }

    private void layoutExtras() {
        int w=stage.getWidth(),h=stage.getHeight();
        if(w<10||h<10) return;
        int moonWidth=Math.min(dp(245),Math.round(w*.20f));
        int moonHeight=Math.min(dp(260),Math.round(h*.41f));
        FrameLayout.LayoutParams lm=(FrameLayout.LayoutParams)moon.getLayoutParams();
        lm.width=moonWidth;lm.height=moonHeight;
        lm.leftMargin=Math.round(w*.012f);
        lm.topMargin=Math.round(h*.18f);
        moon.setLayoutParams(lm);

        FrameLayout.LayoutParams lt=(FrameLayout.LayoutParams)tide.getLayoutParams();
        lt.width=Math.min(dp(285),Math.round(w*.22f));
        lt.height=Math.min(dp(190),Math.round(h*.28f));
        lt.rightMargin=Math.round(w*.015f);
        lt.topMargin=Math.round(h*.245f);
        tide.setLayoutParams(lt);
    }

    void setClockColour(int colour) {
        int r=Math.max(100,(Color.red(colour)*3+120)/4);
        int g=Math.max(100,(Color.green(colour)*3+120)/4);
        int b=Math.max(100,(Color.blue(colour)*3+120)/4);
        hijriLabel.setTextColor(Color.rgb(r,g,b));
    }

    void updateHijriCalendar() {
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
                hijriLabel.setText(d+" "+months[m]+" "+y+" H");
            } catch(Exception ignored) {
                hijriLabel.setText("Tarikh tidak tersedia");
            }
            moon.setTime(System.currentTimeMillis());
        }
        if(active && !fetching && Double.isFinite(lastLat) && System.currentTimeMillis()-lastFetchMillis>REFRESH_MILLIS)
            fetchTide(lastLat,lastLon);
    }

    void start() {
        if(destroyed || active) return;
        active=true;
        updateHijriCalendar();
        locate();
    }
    void stop() {
        active=false;
        if(locations!=null && locationListener!=null) {
            try{locations.removeUpdates(locationListener);}catch(Exception ignored){}
        }
        locationListener=null;
    }
    void destroy() { destroyed=true;stop();io.shutdownNow(); }

    void onLocationPermissionResult(int requestCode) {
        if(requestCode==LOCATION_REQUEST && active)locate();
    }
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void locate() {
        if(!active || destroyed) return;
        if(!hasLocationPermission()) {
            boolean asked=activity.getSharedPreferences("izz_radio_prefs",Context.MODE_PRIVATE)
                    .getBoolean("tide_location_permission_asked",false);
            if(asked) tide.setStatus("Benarkan lokasi dalam Settings");
            else {
                tide.setStatus("Benarkan lokasi untuk pasang surut");
                activity.getSharedPreferences("izz_radio_prefs",Context.MODE_PRIVATE).edit()
                        .putBoolean("tide_location_permission_asked",true).apply();
                activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_REQUEST);
            }
            return;
        }
        locations=(LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
        if(locations==null) { tide.setStatus("Lokasi tidak tersedia");return; }
        Location best=null;
        boolean enabled=false;
        for(String provider:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER}) {
            try {
                if(!locations.isProviderEnabled(provider))continue;
                enabled=true;
                Location l=locations.getLastKnownLocation(provider);
                if(l!=null && l.getTime()>System.currentTimeMillis()-4*60*60*1000L &&
                        (best==null || l.getTime()>best.getTime())) best=l;
            }catch(SecurityException|IllegalArgumentException ignored){}
        }
        if(best!=null) useLocation(best);
        else if(!enabled)tide.setStatus("Aktifkan lokasi pada Android");
        else tide.setStatus("Mengesan lokasi…");

        if(locationListener!=null) try{locations.removeUpdates(locationListener);}catch(Exception ignored){}
        locationListener=l->{if(l!=null && active)useLocation(l);};
        for(String provider:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER}) {
            try {
                if(locations.isProviderEnabled(provider)) locations.requestLocationUpdates(provider,60000,500,locationListener);
            }catch(SecurityException|IllegalArgumentException ignored){}
        }
    }
    private void useLocation(Location location) {
        if(!active) return;
        double latitude=location.getLatitude(), longitude=location.getLongitude();
        if(!Double.isFinite(latitude)||!Double.isFinite(longitude))return;
        double delta=Double.isFinite(lastLat)?Math.hypot((latitude-lastLat)*111,(longitude-lastLon)*105):Double.POSITIVE_INFINITY;
        if(!fetching && (delta>10 || System.currentTimeMillis()-lastFetchMillis>REFRESH_MILLIS))
            fetchTide(latitude,longitude);
    }
    private void fetchTide(double latitude,double longitude) {
        if(fetching||destroyed)return;
        fetching=true;
        lastLat=latitude;lastLon=longitude;lastFetchMillis=System.currentTimeMillis();
        tide.setStatus("Memuatkan pasang surut…");
        io.execute(()->{
            float[] values=null;String[] times=null;String error=null;
            HttpURLConnection connection=null;
            try {
                String url="https://marine-api.open-meteo.com/v1/marine?latitude="+
                        String.format(Locale.US,"%.5f",latitude)+"&longitude="+
                        String.format(Locale.US,"%.5f",longitude)+
                        "&hourly=sea_level_height_msl&forecast_days=1&timezone=auto&cell_selection=sea";
                connection=(HttpURLConnection)new URL(url).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("Accept","application/json");
                if(connection.getResponseCode()!=200) throw new Exception("Server unavailable");
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream stream=connection.getInputStream()) {
                    byte[] bytes=new byte[4096];int n;
                    while((n=stream.read(bytes))>=0){
                        if(out.size()+n>250000)throw new Exception("Response too large");
                        out.write(bytes,0,n);
                    }
                }
                JSONObject hourly=new JSONObject(out.toString("UTF-8")).getJSONObject("hourly");
                JSONArray heights=hourly.getJSONArray("sea_level_height_msl");
                JSONArray labels=hourly.getJSONArray("time");
                int count=Math.min(heights.length(),labels.length());
                if(count<3)throw new Exception("No sea-level forecast");
                values=new float[count];times=new String[count];
                for(int i=0;i<count;i++){
                    if(heights.isNull(i))throw new Exception("Missing sea level");
                    values[i]=(float)heights.getDouble(i);
                    if(!Float.isFinite(values[i]))throw new Exception("Invalid sea level");
                    String datetime=labels.getString(i);
                    times[i]=datetime.length()>=16?datetime.substring(11,16):datetime;
                }
            }catch(Exception ignored){
                error="Data tidak tersedia / internet";
            }finally{
                if(connection!=null)connection.disconnect();
            }
            final float[] v=values;final String[] t=times;final String failure=error;
            handler.post(()->{
                fetching=false;
                if(destroyed)return;
                if(failure!=null)tide.setStatus(failure);
                else tide.setForecast(v,t);
            });
        });
    }
}
