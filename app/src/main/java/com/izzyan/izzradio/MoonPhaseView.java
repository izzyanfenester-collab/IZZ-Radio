package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Locale;

/** Approximate astronomical moon illumination. Illustration, not a sighting report. */
final class MoonPhaseView extends View {
    static final double SYNODIC_DAYS=29.530588853;
    static final long KNOWN_NEW_MOON_UTC=947182440000L; // 2000-01-06 18:14 UTC
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape=new Path();
    private long instant=System.currentTimeMillis();

    MoonPhaseView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
    void setInstant(long milliseconds) { instant=milliseconds; invalidate(); }
    static double phase(long utcMillis) {
        double days=(utcMillis-KNOWN_NEW_MOON_UTC)/86400000.0;
        double fraction=(days/SYNODIC_DAYS)%1.0;
        return fraction<0?fraction+1:fraction;
    }
    static String phaseName(double phase) {
        if(phase<0.035 || phase>=0.965) return "Bulan baharu";
        if(phase<0.215) return "Sabit membesar";
        if(phase<0.285) return "Suku pertama";
        if(phase<0.465) return "Hampir purnama";
        if(phase<0.535) return "Bulan purnama";
        if(phase<0.715) return "Selepas purnama";
        if(phase<0.785) return "Suku akhir";
        return "Sabit mengecil";
    }
    private float sp(float value) { return value*getResources().getDisplayMetrics().scaledDensity; }
    private void drawCenter(Canvas c,String value,float baseline,float font,int color,boolean bold) {
        paint.setShader(null); paint.clearShadowLayer(); paint.setStyle(Paint.Style.FILL);
        paint.setColor(color); paint.setTypeface(bold?Typeface.create("sans-serif-medium",Typeface.NORMAL):Typeface.DEFAULT);
        paint.setTextSize(sp(font)); paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(value,getWidth()/2f,baseline,paint);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w=getWidth(),h=getHeight();
        if(w<10 || h<10) return;
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.clearShadowLayer();
        paint.setColor(0xff121e2a); c.drawRoundRect(0,0,w,h,15,15,paint);
        paint.setColor(0xff314455); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f);
        c.drawRoundRect(1,1,w-1,h-1,15,15,paint);
        drawCenter(c,"FASA BULAN",h*.12f,14,0xffc4d3d9,true);

        double f=phase(instant);
        float cx=w/2f, cy=h*.40f;
        float radius=Math.min(w*.265f,h*.235f);
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xff02060a);
        c.drawCircle(cx,cy,radius,paint);
        c.save();
        Path disk=new Path(); disk.addCircle(cx,cy,radius,Path.Direction.CW);
        c.clipPath(disk);
        shape.reset();
        final int steps=80;
        double cos=Math.cos(2.0*Math.PI*f);
        boolean waxing=f<=0.5;
        for(int i=0;i<=steps;i++) {
            double y=-radius+(2.0*radius*i/steps);
            double edge=Math.sqrt(Math.max(0,radius*radius-y*y));
            float x=(float)((waxing?cos:-cos)*edge);
            if(i==0) shape.moveTo(cx+x,cy+(float)y);
            else shape.lineTo(cx+x,cy+(float)y);
        }
        for(int i=steps;i>=0;i--) {
            double y=-radius+(2.0*radius*i/steps);
            double edge=Math.sqrt(Math.max(0,radius*radius-y*y));
            shape.lineTo(cx+(float)((waxing?1:-1)*edge),cy+(float)y);
        }
        shape.close();
        paint.setColor(0xffe3e4d6);
        c.drawPath(shape,paint);
        c.restore();
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.4f); paint.setColor(0xff738190);
        c.drawCircle(cx,cy,radius,paint);

        drawCenter(c,phaseName(f),h*.76f,13,0xffe0e7ec,true);
        int illuminated=(int)Math.round((1.0-Math.cos(2.0*Math.PI*f))*50.0);
        drawCenter(c,String.format(Locale.US,"%d%% bercahaya • anggaran",illuminated),h*.875f,11,0xff9db7c8,false);
    }
}
