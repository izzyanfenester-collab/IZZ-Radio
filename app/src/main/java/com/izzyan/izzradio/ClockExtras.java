package com.izzyan.izzradio;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Hijrah calendar date above the existing digital clock.
 * No moon phase, tide graphs, location tracking, or extra panels.
 */
final class ClockExtras {
    private final MainActivity activity;
    private final TextView hijriLabel;
    private String currentDay="";

    ClockExtras(MainActivity a, FrameLayout holder) {
        activity=a;
        hijriLabel=new TextView(a);
        hijriLabel.setTextSize(44);
        hijriLabel.setGravity(Gravity.CENTER);
        hijriLabel.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        hijriLabel.setTextColor(0xff9bdcab);
        hijriLabel.setBackgroundColor(0xcc05090b);
        hijriLabel.setPadding(dp(16),dp(3),dp(16),dp(4));
        FrameLayout.LayoutParams title=new FrameLayout.LayoutParams(-2,FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        title.topMargin=dp(4);
        holder.addView(hijriLabel,title);
        updateHijriCalendar();
    }
    private int dp(float value) {
        return Math.round(value*activity.getResources().getDisplayMetrics().density);
    }
    void setClockColour(int colour) {
        int r=Math.max(100,(Color.red(colour)*3+120)/4);
        int g=Math.max(100,(Color.green(colour)*3+120)/4);
        int b=Math.max(100,(Color.blue(colour)*3+120)/4);
        hijriLabel.setTextColor(Color.rgb(r,g,b));
    }
    void updateHijriCalendar() {
        String day=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        if(day.equals(currentDay)) return;
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
    }
}
