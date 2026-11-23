package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import java.util.Locale;

/**
 * Hardware-accelerated LED dot-matrix message board.
 * Text is rasterized once when it changes, then sampled into cached LED dots.
 * No per-dot Views, extra network requests, or bitmap allocation per animation frame.
 */
final class LedMarqueeView extends View {
    private static final int ROWS=9;
    private static final int CYAN=0xff00c8ff;
    private static final int GOLD=0xffffd447;
    private static final int RED=0xffff4545;
    private static final int GREEN=0xff47e9a2;
    private static final int WHITE=0xffe5f6ff;
    private static final int DARK=0xff151515;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private LedBannerState state=LedBannerState.ready();
    private boolean[][] pixels=new boolean[0][0];
    private int[] colours=new int[0];
    private float pitch=1, textWidth=0, scroll=0;
    private long lastFrameMs=0;
    private boolean active=true;

    LedMarqueeView(Context context) { super(context); init(); }
    LedMarqueeView(Context context, AttributeSet attrs) { super(context,attrs); init(); }

    private void init() {
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription("Radio LED display. IZZ RADIO - READY");
        glyphPaint.setTypeface(Typeface.create(Typeface.MONOSPACE,Typeface.BOLD));
        setWillNotDraw(false);
    }

    void setDisplay(LedBannerState next) {
        if(next==null || next.equals(state)) return;
        state=next;
        setContentDescription("Radio LED display: "+state.station+" - "+state.detail);
        scroll=0;
        lastFrameMs=0;
        rebuildPixels();
        invalidate();
    }

    void setActive(boolean value) {
        if(active==value) return;
        active=value;
        lastFrameMs=0;
        if(active) invalidate();
    }

    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) {
        super.onSizeChanged(w,h,oldW,oldH);
        scroll=0;
        lastFrameMs=0;
        rebuildPixels();
    }

    private float dp(float v) { return v*getResources().getDisplayMetrics().density; }

    private void rebuildPixels() {
        if(getHeight()<12) return;
        pitch=Math.max(2f,(getHeight()-dp(8))/(ROWS+1));
        final String first=state.station.toUpperCase(Locale.ROOT);
        final String second=state.detail.toUpperCase(Locale.ROOT);
        final boolean explicitLive=state.live && !second.equals("LIVE RADIO");
        final String lead=first+"  -  ";
        final String body=lead+second+(explicitLive?"   ● LIVE":"");
        glyphPaint.setTextSize(pitch*8.9f);
        float firstWidth=glyphPaint.measureText(first);
        float songWidth=glyphPaint.measureText(lead+second);
        float completeWidth=glyphPaint.measureText(body)+pitch*3;
        int bitmapWidth=(int)Math.min(16000,Math.ceil(completeWidth));
        int bitmapHeight=Math.max(1,(int)Math.ceil(pitch*ROWS));
        if(bitmapWidth<=0) return;

        Bitmap mask=Bitmap.createBitmap(bitmapWidth,bitmapHeight,Bitmap.Config.ARGB_8888);
        try {
            Canvas offscreen=new Canvas(mask);
            glyphPaint.setColor(Color.WHITE);
            offscreen.drawText(body,pitch*1.5f,pitch*8.0f,glyphPaint);
            int cols=(int)Math.ceil(bitmapWidth/pitch);
            boolean[][] nextPixels=new boolean[cols][ROWS];
            int[] nextColours=new int[cols];
            for(int c=0;c<cols;c++) {
                int sampleX=Math.min(bitmapWidth-1,(int)((c+0.5f)*pitch));
                if(state.mode==LedBannerState.SELAWAT) nextColours[c]=GREEN;
                else if(state.mode==LedBannerState.ERROR) nextColours[c]=RED;
                else if(state.mode==LedBannerState.READY) nextColours[c]=WHITE;
                else if(state.mode==LedBannerState.WAITING) nextColours[c]=GOLD;
                else if(sampleX>pitch*1.5f+songWidth) nextColours[c]=RED;
                else if(sampleX>pitch*1.5f+firstWidth) nextColours[c]=GOLD;
                else nextColours[c]=CYAN;
                for(int row=0;row<ROWS;row++) {
                    int sampleY=Math.min(bitmapHeight-1,(int)((row+0.5f)*pitch));
                    nextPixels[c][row]=Color.alpha(mask.getPixel(sampleX,sampleY))>=75;
                }
            }
            pixels=nextPixels;
            colours=nextColours;
            textWidth=cols*pitch;
        } finally {
            mask.recycle();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight();
        if(w<=1 || h<=1) return;
        float r=Math.min(dp(12),h*.25f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff050505);
        canvas.drawRoundRect(dp(1),dp(1),w-dp(1),h-dp(1),r,r,paint);
        paint.setColor(0xff183440);
        canvas.drawRoundRect(dp(1),dp(1),w-dp(1),dp(3),r,r,paint);

        float left=dp(8),right=w-dp(8);
        float top=(h-ROWS*pitch)*.5f;
        canvas.save();
        canvas.clipRect(left,dp(4),right,h-dp(4));
        paint.setColor(DARK);
        float bgRadius=pitch*.23f;
        for(float x=left+pitch*.5f;x<right;x+=pitch) {
            for(int row=0;row<ROWS;row++)
                canvas.drawCircle(x,top+(row+.5f)*pitch,bgRadius,paint);
        }

        float available=right-left;
        boolean marquee=textWidth>available;
        float origin=marquee?left+pitch*2: left+Math.max(0,(available-textWidth)*.5f);
        float shown=marquee?scroll:0;
        float litRadius=pitch*.35f;
        float glowRadius=pitch*.47f;
        int start=Math.max(0,(int)Math.floor((left-origin+shown)/pitch)-1);
        int end=Math.min(pixels.length,(int)Math.ceil((right-origin+shown)/pitch)+2);
        for(int col=start;col<end;col++) {
            float x=origin+col*pitch+pitch*.5f-shown;
            int color=colours[col];
            for(int row=0;row<ROWS;row++) {
                if(!pixels[col][row]) continue;
                float y=top+(row+.5f)*pitch;
                paint.setColor((color & 0x00ffffff)|0x44000000);
                canvas.drawCircle(x,y,glowRadius,paint);
                paint.setColor(color);
                canvas.drawCircle(x,y,litRadius,paint);
            }
        }
        canvas.restore();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.3f));
        paint.setColor(0xff2998ba);
        canvas.drawRoundRect(dp(.8f),dp(.8f),w-dp(.8f),h-dp(.8f),r,r,paint);
        paint.setStyle(Paint.Style.FILL);

        // Only animate while displayed. Keep background audio independent.
        if(marquee && active && getWindowVisibility()==VISIBLE && isShown()) {
            long now=SystemClock.uptimeMillis();
            if(lastFrameMs!=0) {
                long dt=Math.min(50,Math.max(0,now-lastFrameMs));
                scroll+=dp(30)*dt/1000f;
                if(scroll>textWidth+pitch*5) { scroll=0; lastFrameMs=0; }
            }
            lastFrameMs=now;
            postInvalidateOnAnimation();
        } else lastFrameMs=0;
    }

    @Override protected void onDetachedFromWindow() {
        active=false; lastFrameMs=0;
        super.onDetachedFromWindow();
    }
}
