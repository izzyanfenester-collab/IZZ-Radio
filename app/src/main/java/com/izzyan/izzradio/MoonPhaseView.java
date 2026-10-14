package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Random;

/**
 * A single photorealistic-inspired moon disk against pure #000000.
 * Textures are generated locally; no internet, location or third party assets required.
 * Lunar phase is approximate, not a religious moon-sighting determination.
 */
final class MoonPhaseView extends View {
    static final double LUNATION=29.530588853;
    static final long NEW_MOON_UTC=947182440000L;  // January 6, 2000, 18:14 UTC
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private Bitmap lunarSurface;
    private long time=System.currentTimeMillis();

    MoonPhaseView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }

    void setTime(long newTime) { time=newTime; invalidate(); }

    static double moonPhase(long utc) {
        double phase=((utc-NEW_MOON_UTC)/86400000.0/LUNATION)%1;
        return phase<0?phase+1:phase;
    }

    private Bitmap makeTexture() {
        final int n=320;
        Bitmap result=Bitmap.createBitmap(n,n,Bitmap.Config.ARGB_8888);
        int[] pixels=new int[n*n];
        Random rand=new Random(4382811);
        float[] craters=new float[55*4];
        for(int i=0;i<55;i++) {
            craters[i*4]=rand.nextFloat()*2-1;
            craters[i*4+1]=rand.nextFloat()*2-1;
            craters[i*4+2]=0.016f+rand.nextFloat()*.095f;
            craters[i*4+3]=.10f+rand.nextFloat()*.21f;
        }
        for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
            float xx=(2f*x+1-n)/n,yy=(2f*y+1-n)/n;
            float rr=xx*xx+yy*yy;
            if(rr>=.992f) { pixels[y*n+x]=Color.TRANSPARENT;continue; }
            float pseudo=(float)(Math.sin(xx*39+Math.sin(yy*18)*5)*Math.cos(yy*35+xx*7)*.085
                +Math.sin(xx*11+yy*31)*.06+Math.cos(xx*21-yy*16)*.035);
            float maria=(float)(Math.sin(xx*5.4+yy*2.2)*Math.cos(yy*6.7-xx*1.1));
            float val=163+pseudo*160-(maria>.22?28:0);
            for(int i=0;i<55;i++) {
                float cx=craters[i*4],cy=craters[i*4+1],rad=craters[i*4+2];
                float dist=(float)Math.hypot(xx-cx,yy-cy)/rad;
                if(dist<.72f) val-=20*craters[i*4+3]*3f;
                if(dist>=.75f&&dist<1.08f) val+=23*craters[i*4+3]*3f;
            }
            float limb=(float)Math.sqrt(Math.max(0,1-rr));
            val*=.52f+.48f*limb;
            int v=Math.max(25,Math.min(225,Math.round(val)));
            pixels[y*n+x]=Color.rgb(v,Math.min(255,v+1),Math.min(255,v+3));
        }
        result.setPixels(pixels,0,n,0,0,n,n);
        return result;
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(lunarSurface==null) lunarSurface=makeTexture();
        float radius=Math.min(getWidth()*.44f,getHeight()*.47f);
        if(radius<2) return;
        float cx=getWidth()*.5f,cy=getHeight()*.5f;
        RectF disk=new RectF(cx-radius,cy-radius,cx+radius,cy+radius);
        Path circle=new Path();
        circle.addOval(disk,Path.Direction.CW);
        canvas.save();canvas.clipPath(circle);

        // The night side must remain visibly spherical, never a blue/grey panel.
        paint.setStyle(Paint.Style.FILL);
        paint.setColorFilter(new LightingColorFilter(0xff151719,0));
        canvas.drawBitmap(lunarSurface,null,disk,paint);
        paint.setColorFilter(null);

        double phase=moonPhase(time);
        // waxing => right-hand illuminated, waning => left-hand illuminated
        boolean waxing=phase<.5;
        float terminator=(float)Math.cos(2*Math.PI*phase);
        Path lit=new Path();
        int steps=120;
        for(int i=0;i<=steps;i++) {
            float v=-1f+2f*i/steps;
            float edge=(float)Math.sqrt(Math.max(0,1-v*v));
            float boundary=cx+(waxing?terminator:-terminator)*edge*radius;
            float y=cy+v*radius;
            if(i==0) lit.moveTo(boundary,y);else lit.lineTo(boundary,y);
        }
        for(int i=steps;i>=0;i--) {
            float v=-1f+2f*i/steps;
            float edge=(float)Math.sqrt(Math.max(0,1-v*v));
            lit.lineTo(cx+(waxing?edge:-edge)*radius,cy+v*radius);
        }
        lit.close();
        canvas.save();canvas.clipPath(lit);
        paint.setColorFilter(null);
        canvas.drawBitmap(lunarSurface,null,disk,paint);
        canvas.restore();
        canvas.restore();
    }
    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(lunarSurface!=null){ lunarSurface.recycle();lunarSurface=null; }
    }
}
