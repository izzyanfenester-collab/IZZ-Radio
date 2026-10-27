package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Base64;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Lightweight real-time tropical aquarium, not a rendered 4K/60fps video.
 * The supplied aquarium image is processed into one offline WebP atlas.
 * The top 384x216 pixels are the reef, the lower strip holds separate fish sprites.
 * Nine fish move independently; the remaining reef inhabitants remain in the background.
 */
final class AquariumScreensaverView extends View {
    private static final int W=256,H=144,ATLAS_Y=148;
    private static final long LOOP_MS=60000L;
    private static final int[][] FISH={
        // sprite crop x,y,w,h ; original x,y in the 384x216 illustration
        {1,148,20,19,0,12}, {24,148,25,17,37,14},
        {52,148,30,24,106,4}, {85,148,25,18,175,7},
        {113,148,22,22,218,7}, {138,148,31,20,140,23},
        {172,148,29,19,194,40}, {204,148,24,16,144,49}
    };
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect from=new Rect();
    private final RectF to=new RectF();
    private Bitmap atlas;
    private boolean active;

    AquariumScreensaverView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        try (InputStream in=context.getAssets().open("aquarium_atlas.webp.b64")) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buf=new byte[4096];int count;
            while ((count=in.read(buf))!=-1) out.write(buf,0,count);
            byte[] image=Base64.decode(out.toByteArray(),Base64.DEFAULT);
            atlas=BitmapFactory.decodeByteArray(image,0,image.length);
        } catch (Exception ignored) {
            atlas=null; // render a graceful fallback instead of crashing
        }
    }
    void setActive(boolean value) {
        active=value;
        if(active)postInvalidateOnAnimation();
        else invalidate();
    }
    private static float wave(double phase){ return (float)Math.sin(phase*2*Math.PI); }
    private void drawBubble(Canvas c,float x,float y,float r,int alpha) {
        paint.setColor(Color.argb(alpha,164,222,240));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.75f);
        c.drawCircle(x,y,r,paint);
        paint.setColor(Color.argb(alpha/3,235,255,255));
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(x-r*.25f,y-r*.25f,r*.3f,paint);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int w=getWidth(),h=getHeight();
        if(w<=0||h<=0)return;
        canvas.drawColor(Color.BLACK);
        float scale=Math.min(w/(float)W,h/(float)H);
        canvas.save();
        canvas.translate((w-scale*W)/2f,(h-scale*H)/2f);
        canvas.scale(scale,scale);
        if(atlas==null) {
            paint.setColor(0xff103342);
            paint.setTextSize(12);
            canvas.drawText("Aquarium artwork unavailable",80,108,paint);
            canvas.restore();
            return;
        }
        double t=(SystemClock.uptimeMillis()%LOOP_MS)/(double)LOOP_MS;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        from.set(0,0,W,H);to.set(0,0,W,H);
        canvas.drawBitmap(atlas,from,to,paint);

        // Calm synchronized waves; all motion returns to the same state at 60s.
        for(int i=0;i<FISH.length;i++){
            int[] b=FISH[i];
            double phase=t*(i%3+1) + i*.137;
            float dx=7f*wave(phase)*((i%2==0)?1f:-1f);
            float dy=3.2f*wave(t*(i%4+1)+i*.19);
            float cx=b[4]+b[2]*.5f+dx,cy=b[5]+b[3]*.5f+dy;
            canvas.save();
            canvas.rotate(2.5f*wave(t*(i%5+1)+i*.21),cx,cy);
            // A tiny lateral scale simulates swimming without distorting species.
            float flex=1f+.023f*wave(t*(4+i%4)+i*.13);
            canvas.scale(flex,1,cx,cy);
            from.set(b[0],b[1],b[0]+b[2],b[1]+b[3]);
            to.set(b[4]+dx,b[5]+dy,b[4]+dx+b[2],b[5]+dy+b[3]);
            paint.setAlpha(255);
            canvas.drawBitmap(atlas,from,to,paint);
            canvas.restore();
        }

        // The original central bubble column remains; these add actual upward motion.
        for(int i=0;i<92;i++){
            float x=96f+((i*23)%20)+3f*wave(t*2+i*.137);
            float period=144f;
            float advance=(float)(t*period*((i%3)+1));
            float y=143f-((i*19f+advance)%period);
            float radius=.3f+(i%4)*.18f;
            drawBubble(canvas,x,y,radius,28+(i%5)*15);
        }
        // Subtle moving underwater light on corals. No panning, zoom or text.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.5f);
        paint.setColor(0x1647d3ff);
        for(int i=0;i<8;i++){
            float x=10+i*32+3*wave(t+i*.1);
            canvas.drawArc(x,84,x+25,139,192,38,false,paint);
        }
        canvas.restore();
        if(active && getWindowVisibility()==VISIBLE)postInvalidateDelayed(16L); // best-effort up to ~60fps
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if(visibility==VISIBLE && active) postInvalidateOnAnimation();
    }
    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        active=false;
        if(atlas!=null) {
            atlas.recycle();
            atlas=null;
        }
    }
}
