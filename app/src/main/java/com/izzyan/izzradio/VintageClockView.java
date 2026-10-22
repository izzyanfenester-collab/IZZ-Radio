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
        // All coatings sit INSIDE the illustration's brass frame. The original
        // metal rim stays visible, so it looks like one manufactured radio.
        grad(c,524,299,970,519,12,0xff8e572f,0xff382012);
        edge(c,525,300,969,518,11,0xffd7a263,2.2f);
        grad(c,530,306,964,513,9,0xff150805,0xff37170d);
        edge(c,533,309,961,511,8,0xff5d3720,2f);
        line(c,539,316,955,316,0x55ffd49d,1.2f);
        // Gloss is confined to the inner glass, not an extra opaque card.
        grad(c,541,317,951,337,4,0x263f554b,0x00170a07);
        String title=station.isEmpty()?"IZZ RADIO":station.toUpperCase(Locale.ROOT);
        text(c,title,747,414,58,379,IVORY,true,true);
        line(c,567,438,927,438,0xff95643f,1.5f);
        int[] bars={8,16,23,12,7};
        for(int i=0;i<bars.length;i++) {
            rect(c,732+i*9,438-bars[i]/2f,736+i*9,438+bars[i]/2f,1,0xffc18a50);
        }
        String track=song.isEmpty()?"Live Radio":song;
        if(track.startsWith("♪")||track.startsWith("♫")) track=track.substring(1).trim();
        if(track.isEmpty())track="Live Radio";
        text(c,"♫  "+track,746,480,24,377,0xffefd3b0,false,false);
    }

    private void flipLeaf(Canvas c,float left,float top,float right,float bottom,String value) {
        // Top and bottom are ONE mechanical leaf with a fine centre fold;
        // letter forms stay printed on the card rather than floating.
        grad(c,left,top,right,bottom,9,0xff4a3730,0xff140d0d);
        edge(c,left,top,right,bottom,9,0xffb38961,1.6f);
        float mid=(top+bottom)/2f;
        grad(c,left+3,top+3,right-3,mid-1,5,0xff372827,0xff241719);
        grad(c,left+3,mid+1,right-3,bottom-3,5,0xff1b1214,0xff322329);
        line(c,left+4,mid,right-4,mid,0xff0b0506,2f);
        line(c,left+7,mid-3,right-7,mid-3,0x4db99879,.8f);
        text(c,value,(left+right)/2f,mid+27,79,right-left-12,0xffffe8c9,true,false);
        for(float x:new float[]{left+3,right-3}) {
            rect(c,x-3,mid-10,x+3,mid+10,2,0xff100b0b);
            grad(c,x-1,mid-7,x+1,mid+7,1,0xffbf9671,0xff50362d);
        }
    }
    private void drawClock(Canvas c,Date now) {
        // Separate clock opening is part of the wallpaper's wooden chassis.
        // Only the small INNER display is repainted.
        grad(c,724,607,974,747,11,0xff140d0c,0xff32221d);
        edge(c,725,608,973,746,10,0xff996a46,1.4f);
        grad(c,731,616,966,735,8,0xff090606,0xff20110e);
        boolean use24Hour=android.text.format.DateFormat.is24HourFormat(getContext());
        String hour=new SimpleDateFormat(use24Hour?"HH":"hh",Locale.US).format(now);
        String minute=new SimpleDateFormat("mm",Locale.US).format(now);
        flipLeaf(c,735,620,839,729,hour);
        flipLeaf(c,856,620,961,729,minute);
        rect(c,839,651,856,697,3,0xff080505);
        copperRivet(c,847,674,5);
        if(!use24Hour) {
            text(c,new SimpleDateFormat("a",Locale.US).format(now),848,745,13,90,0xffe9bf8f,false,false);
        }
    }

    private void drawCalendar(Canvas c,Date now) {
        // Traditional paper page. Leave the metal hooks, wood and cast shadow
        // in the original artwork visible instead of painting over them.
        rect(c,513,636,661,769,5,0x9a190b07);
        grad(c,516,635,658,765,3,0xffffe8bb,0xffd6ab7f);
        edge(c,517,636,658,765,3,0xffa16b45,1.3f);
        grad(c,517,637,657,668,2,0xffb97345,0xff844224);
        line(c,519,669,655,669,0xffd9a26f,1.2f);
        for(int i=0;i<9;i++) {
            line(c,524,674+7*i,652,674+7*i,0x13a46835,.65f);
        }
        String month=new SimpleDateFormat("MMMM",Locale.ENGLISH).format(now).toUpperCase(Locale.ENGLISH);
        String day=new SimpleDateFormat("dd",Locale.US).format(now);
        String weekday=new SimpleDateFormat("EEEE",Locale.ENGLISH).format(now).toUpperCase(Locale.ENGLISH);
        text(c,month,588,658,20,129,0xffffecda,true,false);
        text(c,day,588,742,84,124,0xff67341b,true,true);
        line(c,529,748,645,748,0x698c5339,1);
        text(c,weekday,588,762,14,126,0xff87492a,true,false);
        // Small folded page edge is part of the paper, not a floating square.
        reset();
        Path fold=new Path();
        fold.moveTo(640,749);
        fold.lineTo(656,749);
        fold.lineTo(656,763);
        fold.close();
        p.setColor(0xffb7885e);
        c.drawPath(fold,p);
        line(c,640,749,656,763,0xff785032,1f);
    }
}
