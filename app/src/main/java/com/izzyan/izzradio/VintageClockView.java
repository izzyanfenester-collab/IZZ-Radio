package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.*;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Second screensaver. Uses the user's vintage radio artwork as a noninteractive skin;
 * the station/song, flip clock and calendar are live foreground drawings.
 * The original digital screensaver is a completely separate view.
 */
final class VintageClockView extends View {
    private static final float ART_WIDTH=1448f, ART_HEIGHT=1086f;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF rect=new RectF();
    private Bitmap art;
    private String station="";
    private String song="";
    private final int CREAM=0xffffe8c5;
    private final int BROWN=0xff361705;

    VintageClockView(Context context) {
        super(context);
        setBackgroundColor(0xff24100d);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        try(InputStream input=context.getAssets().open("vintage_console.webp.b64")) {
            ByteArrayOutputStream data=new ByteArrayOutputStream();
            byte[] bytes=new byte[2048];
            int n;
            while((n=input.read(bytes))!=-1) data.write(bytes,0,n);
            byte[] decoded=Base64.decode(data.toByteArray(),Base64.DEFAULT);
            art=BitmapFactory.decodeByteArray(decoded,0,decoded.length);
        }catch(Exception ignored){ art=null; }
    }
    void setRadioInfo(String stationName,String track) {
        String s=stationName==null?"":stationName.trim();
        String t=track==null?"":track.trim();
        if(!s.equals(station) || !t.equals(song)) {
            station=s;
            song=t;
            invalidate();
        }
    }
    void tick(){ invalidate(); }

    private void fill(Canvas canvas,float l,float t,float r,float b,float rad,int color) {
        paint.setShader(null);
        paint.setColorFilter(null);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.clearShadowLayer();
        rect.set(l,t,r,b);
        canvas.drawRoundRect(rect,rad,rad,paint);
    }
    private void gradient(Canvas canvas,float l,float t,float r,float b,float rad,int top,int bottom) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColorFilter(null);
        paint.setColor(Color.WHITE);
        paint.clearShadowLayer();
        paint.setShader(new LinearGradient(0,t,0,b,top,bottom,Shader.TileMode.CLAMP));
        rect.set(l,t,r,b);
        canvas.drawRoundRect(rect,rad,rad,paint);
        paint.setShader(null);
    }
    private void stroke(Canvas canvas,float l,float t,float r,float b,float radius,int color,float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        rect.set(l,t,r,b);
        canvas.drawRoundRect(rect,radius,radius,paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private void write(Canvas canvas,String value,float x,float baseline,
                       float size,float maxWidth,int color,boolean bold) {
        if(value==null || value.isEmpty())return;
        paint.setShader(null);
        paint.setColorFilter(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(bold?"sans-serif-condensed":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        float currentSize=size;
        paint.setTextSize(currentSize);
        while(currentSize>12 && paint.measureText(value)>maxWidth) {
            currentSize-=1.25f;
            paint.setTextSize(currentSize);
        }
        String s=value;
        if(paint.measureText(s)>maxWidth) {
            while(s.length()>3 && paint.measureText(s+"…")>maxWidth) s=s.substring(0,s.length()-1);
            s+="…";
        }
        paint.setColor(color);
        paint.setShadowLayer(2f,0f,1f,0x88400d00);
        canvas.drawText(s,x,baseline,paint);
        paint.clearShadowLayer();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w=getWidth(), h=getHeight();
        if(w<1 || h<1)return;
        canvas.drawColor(0xff25100e);
        if(art!=null) {
            // Soft extension fills side bands on 16:9 without stretching the
            // original vintage dials and clock out of proportion.
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setColor(Color.WHITE);
            paint.setAlpha(68);
            canvas.drawBitmap(art,null,new RectF(0,0,w,h),paint);
            paint.setAlpha(255);
        }
        final float scale=Math.min(w/ART_WIDTH,h/ART_HEIGHT);
        canvas.save();
        canvas.translate((w-ART_WIDTH*scale)/2f,(h-ART_HEIGHT*scale)/2f);
        canvas.scale(scale,scale);
        if(art!=null) {
            paint.setAlpha(255);
            paint.setShader(null);
            paint.setColor(Color.WHITE);
            canvas.drawBitmap(art,null,new RectF(0,0,ART_WIDTH,ART_HEIGHT),paint);
        } else {
            // App remains usable even if the decorative file is unavailable.
            gradient(canvas,0,0,ART_WIDTH,ART_HEIGHT,0,0xff4a251c,0xff190b08);
        }
        drawLiveStation(canvas);
        Date now=new Date();
        drawLiveCalendar(canvas,now);
        drawLiveClock(canvas,now);
        canvas.restore();
    }
    private void drawLiveStation(Canvas canvas) {
        // Preserve the gold outside bezel; replace only the old VU dial face.
        gradient(canvas,566,321,961,517,9,0xff2a1005,0xff381708);
        stroke(canvas,570,325,956,511,7,0xff99613a,2);
        String shown=station.isEmpty()?"IZZ RADIO":station.toUpperCase(Locale.ROOT);
        write(canvas,shown,760,415,56,344,CREAM,true);
        paint.setColor(0xffae7848);
        paint.setStrokeWidth(1.8f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(600,441,926,441,paint);
        paint.setStyle(Paint.Style.FILL);
        String caption=song.isEmpty()?"♪ Live Radio":song;
        if(caption.startsWith("♪")) caption=caption.substring(1).trim();
        if(caption.isEmpty()) caption="Live Radio";
        write(canvas,"♫ "+caption,760,476,24,354,0xfff4d8bd,false);
    }
    private void drawLiveClock(Canvas canvas,Date now) {
        boolean is24=android.text.format.DateFormat.is24HourFormat(getContext());
        String hour=new SimpleDateFormat(is24?"HH":"hh",Locale.US).format(now);
        String minute=new SimpleDateFormat("mm",Locale.US).format(now);
        flipTile(canvas,720,626,838,739,hour);
        flipTile(canvas,847,626,973,739,minute);
        // Leave hinge and the physical wood frame visible.
        fill(canvas,838,650,849,713,4,0xff190f0c);
        fill(canvas,840,680,847,692,4,0xffc6a583);
        if(!is24) write(canvas,new SimpleDateFormat("a",Locale.US).format(now),845,760,15,90,0xfff1cfaa,false);
    }
    private void flipTile(Canvas c,float l,float t,float r,float b,String value) {
        gradient(c,l,t,r,b,8,0xff170e0f,0xff312126);
        stroke(c,l+2,t+2,r-2,b-2,7,0xff85573e,2);
        fill(c,l+4,(t+b)/2-1,r-4,(t+b)/2+1,0,0xff090607);
        write(c,value,(l+r)/2,b-19,84,r-l-8,0xfff9e4c7,true);
    }
    private void drawLiveCalendar(Canvas canvas,Date now) {
        String month=new SimpleDateFormat("MMMM",Locale.ENGLISH).format(now).toUpperCase(Locale.ENGLISH);
        String day=new SimpleDateFormat("dd",Locale.US).format(now);
        String week=new SimpleDateFormat("EEEE",Locale.ENGLISH).format(now).toUpperCase(Locale.ENGLISH);

        // Draw inside the illustrated tear-off notepad without obscuring its rings.
        gradient(canvas,517,625,662,784,3,0xffffe0aa,0xffe0bd8a);
        gradient(canvas,517,625,662,660,2,0xffbd6e38,0xff90411f);
        write(canvas,month,589,649,18,130,0xffffffff,true);
        write(canvas,day,589,735,77,124,0xff693719,true);
        write(canvas,week,589,767,15,130,0xff7f4a2c,true);
    }
}
