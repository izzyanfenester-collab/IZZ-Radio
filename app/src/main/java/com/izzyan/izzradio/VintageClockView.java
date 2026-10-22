package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.Base64;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Optional second screensaver. Single un-stretched background image and three
 * integrated live components: engraved radio display, wooden tear-off calendar,
 * and mechanical flip clock. Original LED screensaver remains untouched.
 */
final class VintageClockView extends View {
    private static final float ART_W=1448f, ART_H=1086f;
    private static final int GOLD=0xffc99559;
    private static final int IVORY=0xffffe2b9;
    private static final int INK=0xff6b3218;
    private static final int WOOD=0xff36180f;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    private Bitmap art;
    private String station="", song="";
    private long songChanged=SystemClock.uptimeMillis();

    VintageClockView(Context ctx) {
        super(ctx);
        setBackgroundColor(0xff1c0e0a);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        try(InputStream input=ctx.getAssets().open("vintage_console.webp.b64")) {
            ByteArrayOutputStream data=new ByteArrayOutputStream();
            byte[] bytes=new byte[4096];
            int n;
            while((n=input.read(bytes))!=-1) data.write(bytes,0,n);
            byte[] raw=Base64.decode(data.toByteArray(),Base64.DEFAULT);
            art=BitmapFactory.decodeByteArray(raw,0,raw.length);
        } catch(Exception ignored) {
            art=null;
        }
    }

    void setRadioInfo(String name,String track) {
        String s=name==null?"":name.trim();
        String t=track==null?"":track.trim();
        if(!station.equals(s)||!song.equals(t)){
            station=s; song=t; songChanged=SystemClock.uptimeMillis();
            invalidate();
        }
    }
    void tick(){ invalidate(); }

    private void reset(){
        p.reset();
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        p.setDither(true);
    }
    private void rect(Canvas canvas,float left,float top,float right,float bottom,float radius,int color) {
        reset();
        p.setColor(color);
        bounds.set(left,top,right,bottom);
        canvas.drawRoundRect(bounds,radius,radius,p);
    }
    private void grad(Canvas canvas,float left,float top,float right,float bottom,float radius,int topColor,int bottomColor) {
        reset();
        p.setShader(new LinearGradient(0,top,0,bottom,topColor,bottomColor,Shader.TileMode.CLAMP));
        bounds.set(left,top,right,bottom);
        canvas.drawRoundRect(bounds,radius,radius,p);
        p.setShader(null);
    }
    private void edge(Canvas c,float left,float top,float right,float bottom,float radius,int color,float stroke) {
        reset();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);p.setColor(color);
        bounds.set(left+stroke/2,top+stroke/2,right-stroke/2,bottom-stroke/2);
        c.drawRoundRect(bounds,radius,radius,p);p.setStyle(Paint.Style.FILL);
    }
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float stroke) {
        reset();p.setStrokeWidth(stroke);p.setColor(color);c.drawLine(x,y,x2,y2,p);
    }
    private void text(Canvas c,String label,float centerX,float baseline,float pointSize,
                      float maxWidth,int color,boolean bold,boolean serif) {
        if(label==null||label.isEmpty())return;
        reset();
        p.setTypeface(Typeface.create(serif?"serif":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(pointSize);
        String displayed=label;
        float size=pointSize;
        while(size>11&&p.measureText(displayed)>maxWidth){size-=1;p.setTextSize(size);}
        if(p.measureText(displayed)>maxWidth) {
            while(displayed.length()>2&&p.measureText(displayed+"…")>maxWidth)
                displayed=displayed.substring(0,displayed.length()-1);
            displayed+="…";
        }
        p.setColor(color);
        p.setShadowLayer(1.7f,0,1,0xaa0c0704);
        c.drawText(displayed,centerX,baseline,p);
        p.clearShadowLayer();
    }
    private void copperRivet(Canvas c,float x,float y,float rad) {
        reset();
        p.setShader(new RadialGradient(x-rad*.4f,y-rad*.4f,rad*1.7f,
                new int[]{0xffffd69c,0xffb17b42,0xff462013},
                null,Shader.TileMode.CLAMP));
        c.drawCircle(x,y,rad,p);p.setShader(null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w=getWidth(),h=getHeight();
        if(w<=0||h<=0)return;
        // Exactly ONE centered image. Empty letterbox areas are a solid color,
        // NEVER a stretched, blurred, reflected or transparent copy.
        canvas.drawColor(0xff1b0b09);
        float scale=Math.min(w/ART_W,h/ART_H);
        canvas.save();
        canvas.translate((w-ART_W*scale)/2f,(h-ART_H*scale)/2f);
        canvas.scale(scale,scale);
        if(art!=null) {
            reset();p.setColor(Color.WHITE);
            canvas.drawBitmap(art,null,new RectF(0,0,ART_W,ART_H),p);
        }else{
            grad(canvas,0,0,ART_W,ART_H,0,0xff4c281d,0xff190907);
        }
        Date date=new Date();
        drawStation(canvas);
        drawCalendar(canvas,date);
        drawClock(canvas,date);
        canvas.restore();
    }

    private void drawStation(Canvas c) {
        // Custom metal and inlaid dark-glass surrounds, aligned inside
        // the radio's original gold bezel (500..987, 276..538).
        rect(c,519,294,972,524,15,0xff1b0a04);
        grad(c,521,295,970,523,13,0xffae7440,0xff5a3117);
        edge(c,522,296,970,523,13,0xfff4c17c,2.0f);
        grad(c,530,305,962,513,9,0xff1c0904,0xff3e1a0d);
        edge(c,531,306,962,512,8,0xff694021,2.0f);
        // Restrained inset glass reflection, no opaque card sticking out.
        rect(c,537,310,955,322,4,0x203ba0a0);
        copperRivet(c,528,305,3);
        copperRivet(c,964,305,3);
        copperRivet(c,528,510,3);
        copperRivet(c,964,510,3);
        String title=station.isEmpty()?"IZZ RADIO":station.toUpperCase(Locale.ROOT);
        text(c,title,746,411,57,385,IVORY,true,true);
        line(c,570,438,924,438,0xff98633b,2);
        // Small electro-acoustic mark, subtly centred in the divider.
        int[] bars={6,16,25,13,8};
        for(int i=0;i<bars.length;i++) {
            rect(c,732+i*9,438-bars[i]/2f,736+i*9,438+bars[i]/2f,1,0xffc18a50);
        }
        String track=song.isEmpty()?"Live Radio":song;
        if(track.startsWith("♪")) track=track.substring(1).trim();
        if(track.startsWith("♫")) track=track.substring(1).trim();
        if(track.isEmpty()) track="Live Radio";
        text(c,"♫  "+track,746,483,24,381,0xffe8c8a5,false,false);
    }

    private void flipLeaf(Canvas c,float left,float top,float right,float bottom,String value) {
        rect(c,left+1,top+3,right+3,bottom+4,9,0xff090505); // shallow drop shadow
        grad(c,left,top,right,bottom,9,0xff4c3330,0xff160e10);
        edge(c,left,top,right,bottom,9,0xffac7654,1.6f);
        float split=(top+bottom)/2f;
        grad(c,left+4,top+3,right-4,split-1,6,0xff332426,0xff211618);
        grad(c,left+4,split+1,right-4,bottom-3,6,0xff201718,0xff312125);
        line(c,left+5,split,right-5,split,0xff050304,2.8f);
        line(c,left+6,split-3,right-6,split-3,0x668d6e5c,1f);
        // Keep both halves reading like a real printed flip card.
        text(c,value,(left+right)/2f,split+27f,76,right-left-9,0xffffebd1,true,false);
        // Hinges on both sides of each flip leaf.
        for(float x:new float[]{left+3,right-3}) {
            rect(c,x-3,split-10,x+3,split+10,2,0xff0c0706);
            rect(c,x-1,split-7,x+1,split+7,1,0xff91715c);
        }
    }
    private void drawClock(Canvas c,Date now) {
        // Do not cover the wood and gold clock housing with an artificial large card.
        // Only refresh the two leaves inside the existing opening.
        final float x1=732f,x2=851f,top=615f,bottom=729f;
        String hour=new SimpleDateFormat("HH",Locale.US).format(now);
        String minute=new SimpleDateFormat("mm",Locale.US).format(now);
        flipLeaf(c,x1,top,835,bottom,hour);
        flipLeaf(c,x2,top,953,bottom,minute);
        // Mechanical centre pin and short separator, both within the frame.
        rect(c,837,646,848,703,3,0xff0e0908);
        copperRivet(c,842,673,5.0f);
        // Small in-frame indicator; use the DEVICE local timezone.
        String tz=new SimpleDateFormat("z",Locale.US).format(now);
        text(c,tz,842,750,13,112,0xffc6a58b,false,false);
    }

    private void drawCalendar(Canvas c,Date now) {
        // Cover just the printed paper, keeping the illustration's real
        // wooden frame, metallic rings and drop shadow visible around it.
        rect(c,513,635,663,770,5,0xff24100b);
        grad(c,516,634,659,763,3,0xffffe1b4,0xffe9c593);
        edge(c,516,634,659,763,3,0xffa0633d,1.8f);
        grad(c,516,635,659,662,3,0xffab6138,0xff853c22);
        line(c,517,664,658,664,0xffb2764e,1.3f);
        // Fine grain lines on the paper make this read as printed stationery.
        for(int i=0;i<12;i++){
            float y=670+i*6f;
            line(c,520,y,655,y,0x10a66b43,.65f);
        }
        String month=new SimpleDateFormat("MMMM",Locale.ENGLISH).format(now).toUpperCase(Locale.ENGLISH);
        String day=new SimpleDateFormat("dd",Locale.US).format(now);
        String week=new SimpleDateFormat("EEEE",Locale.ENGLISH).format(now).toUpperCase(Locale.ENGLISH);
        text(c,month,588,654,18,134,0xffffeee0,true,false);
        text(c,day,588,739,83,126,INK,true,true);
        line(c,531,743,646,743,0x448e5d3c,1);
        text(c,week,588,756,13.5f,125,0xff8a5634,true,false);
        // Raised brass loops, after the page has been drawn.
        for(float cx:new float[]{540,585,637}){
            copperRivet(c,cx,618,6);
            rect(c,cx-4,592,cx+4,623,4,0xff3a1b10);
            grad(c,cx-3,593,cx+3,618,3,0xffffe6b4,0xff97704b);
            edge(c,cx-4,592,cx+4,623,4,0xff59351d,.9f);
        }
        // Small folded-paper corner at bottom right.
        reset();
        Path fold=new Path();
        fold.moveTo(643,747);
        fold.lineTo(657,747);
        fold.lineTo(657,760);
        fold.close();
        p.setColor(0xffd0aa7e);
        c.drawPath(fold,p);
        line(c,643,747,657,760,0xffb18a60,1);
    }
}
