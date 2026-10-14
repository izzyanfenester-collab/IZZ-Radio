package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

/** Coastal model sea-level plot. Values referenced to MSL, NOT chart datum. */
final class TideChartView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private float[] heights=new float[0];
    private String[] labels=new String[0];
    private String status="Lokasi sedang dikesan…";
    private String position="";
    TideChartView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }

    void setStatus(String s) { status=s; heights=new float[0]; invalidate(); }
    void setPosition(String s) { position=s; invalidate(); }
    void setForecast(float[] values,String[] times,String location) {
        heights=values==null?new float[0]:values.clone();
        labels=times==null?new String[0]:times.clone();
        position=location;
        status=heights.length>=2?"":"Data pasang surut tidak tersedia";
        invalidate();
    }
    private float sp(float x) { return x*getResources().getDisplayMetrics().scaledDensity; }
    private void txt(Canvas c,String s,float x,float y,float size,int color,Paint.Align align) {
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.clearShadowLayer();
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        p.setTextSize(sp(size)); p.setTextAlign(align); p.setColor(color); c.drawText(s,x,y,p);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w=getWidth(),h=getHeight(); if(w<10||h<10) return;
        p.setStyle(Paint.Style.FILL);p.setColor(0xff101f2c); c.drawRoundRect(0,0,w,h,15,15,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(0xff334d61);p.setStrokeWidth(1.3f);
        c.drawRoundRect(1,1,w-1,h-1,15,15,p);
        txt(c,"PASANG SURUT",w/2,h*.11f,14,0xffb2e2e9,Paint.Align.CENTER);
        if(!position.isEmpty()) txt(c,position,w/2,h*.18f,10,0xff96b8c6,Paint.Align.CENTER);

        if(heights.length<2) {
            txt(c,status,w/2,h*.51f,11,0xffa9bdc8,Paint.Align.CENTER);
        } else {
            float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
            for(float value:heights) {
                if(!Float.isFinite(value)) continue;
                min=Math.min(min,value); max=Math.max(max,value);
            }
            if(!Float.isFinite(min)||!Float.isFinite(max)){txt(c,"Tiada ramalan",w/2,h*.52f,12,0xffafc5d0,Paint.Align.CENTER);return;}
            float span=Math.max(.12f,max-min),lower=min-.08f*span,upper=max+.08f*span;
            float left=w*.10f,right=w*.92f,top=h*.28f,bottom=h*.77f;
            for(int i=0;i<=3;i++) {
                float yy=top+(bottom-top)*i/3f;
                p.setColor(0xff253f50);p.setStrokeWidth(1);p.setStyle(Paint.Style.STROKE);c.drawLine(left,yy,right,yy,p);
            }
            path.reset();
            for(int i=0;i<heights.length;i++) {
                float x=left+(right-left)*i/(heights.length-1f);
                float y=bottom-(heights[i]-lower)/(upper-lower)*(bottom-top);
                if(i==0) path.moveTo(x,y); else path.lineTo(x,y);
            }
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,w*.008f));
            p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(0xff77d9e5);p.setShadowLayer(4,0,0,0x886cd9ed);c.drawPath(path,p);p.clearShadowLayer();
            int hi=0,lo=0;
            for(int i=1;i<heights.length;i++) {
                if(heights[i]>heights[hi]) hi=i;
                if(heights[i]<heights[lo]) lo=i;
            }
            txt(c,String.format(Locale.US,"↑ %.2f m",heights[hi]),left, h*.86f,11,0xffb6e8e9,Paint.Align.LEFT);
            txt(c,String.format(Locale.US,"↓ %.2f m",heights[lo]),right,h*.86f,11,0xffb6e8e9,Paint.Align.RIGHT);
            if(labels.length==heights.length && labels.length>1) {
                txt(c,labels[0],left,h*.94f,9,0xffa0b3bd,Paint.Align.LEFT);
                txt(c,labels[labels.length-1],right,h*.94f,9,0xffa0b3bd,Paint.Align.RIGHT);
            }
        }
        txt(c,"Model MSL • Bukan untuk navigasi",w/2,h*.985f,8,0xff879cab,Paint.Align.CENTER);
    }
}
