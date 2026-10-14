package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

/**
 * Amber sea-level curve on a PURE BLACK background (no panel, border, gradient).
 * Height is modelled sea level relative to MSL, never marine chart datum.
 */
final class TideChartView extends View {
    private static final int AMBER=0xffffb400;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path curve=new Path();
    private float[] metres=new float[0];
    private String[] times=new String[0];
    private String status="Mengesan lokasi…";

    TideChartView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }
    void setStatus(String text) { status=text; metres=new float[0]; invalidate(); }
    void setForecast(float[] height,String[] labels) {
        metres=height==null?new float[0]:height.clone();
        times=labels==null?new String[0]:labels.clone();
        status=metres.length<2?"Data pasang surut tidak tersedia":"";
        invalidate();
    }
    private float scaled(float sp) { return sp*getResources().getDisplayMetrics().scaledDensity; }
    private void text(Canvas c,String value,float x,float y,float size,int color,Paint.Align align) {
        p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextAlign(align);
        p.setTextSize(scaled(size));p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        p.clearShadowLayer();c.drawText(value,x,y,p);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w=getWidth(),h=getHeight();if(w<20 || h<20) return;
        canvas.drawColor(Color.BLACK);
        if(metres.length<3) {
            text(canvas,"PASANG SURUT",w/2f,h*.43f,12,0xffe8ac33,Paint.Align.CENTER);
            text(canvas,status,w/2f,h*.57f,9,0xffa9a7a3,Paint.Align.CENTER);
            return;
        }
        final float x0=w*.04f,x1=w*.96f,y0=h*.15f,y1=h*.83f;
        float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;
        for(float v:metres) { if(!Float.isFinite(v)){text(canvas,"Data tidak tersedia",w/2f,h/2f,11,0xff9b9b9b,Paint.Align.CENTER);return;} min=Math.min(min,v);max=Math.max(max,v); }
        float span=Math.max(.16f,max-min);
        float low=min-.12f*span,high=max+.15f*span;
        float[] px=new float[metres.length],py=new float[metres.length];
        for(int i=0;i<metres.length;i++) {
            px[i]=x0+(x1-x0)*i/(metres.length-1f);
            py[i]=y1-(metres[i]-low)/(high-low)*(y1-y0);
        }
        curve.reset();curve.moveTo(px[0],py[0]);
        for(int i=0;i<metres.length-1;i++) {
            float mx=(px[i]+px[i+1])*.5f;
            curve.cubicTo(mx,py[i],mx,py[i+1],px[i+1],py[i+1]);
        }
        p.setColor(AMBER);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2.2f,w*.008f));
        p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
        p.setShadowLayer(3f,0,0,0x99cc7b00);
        canvas.drawPath(curve,p);p.clearShadowLayer();

        // Tag local extrema, not fabricated future high and low tide events.
        ArrayList<Integer> extrema=new ArrayList<>();
        for(int i=1;i<metres.length-1;i++) {
            if((metres[i]>=metres[i-1]&&metres[i]>metres[i+1]) ||
                    (metres[i]<=metres[i-1]&&metres[i]<metres[i+1])) extrema.add(i);
        }
        if(extrema.isEmpty()) {
            int hi=0,lo=0;
            for(int i=1;i<metres.length;i++){if(metres[i]>metres[hi])hi=i;if(metres[i]<metres[lo])lo=i;}
            extrema.add(hi);if(lo!=hi) extrema.add(lo);
        }
        int shown=0;
        for(int i:extrema) {
            if(shown++>=4) break;
            boolean top=metres[i]>metres[Math.max(0,i-1)];
            float markerY=Math.max(scaled(9),Math.min(h-scaled(6),py[i]+(top?-scaled(15):scaled(18))));
            float markerX=Math.max(scaled(25),Math.min(w-scaled(25),px[i]));
            String suffix=times.length==metres.length?" "+times[i]:"";
            text(canvas,String.format(Locale.US,"%.2fm%s",metres[i],suffix),markerX,markerY,8,0xffe8e4df,Paint.Align.CENTER);
        }
        text(canvas,"ANGGARAN • MSL",w/2f,h*.985f,8,0xff8f887d,Paint.Align.CENTER);
    }
}
