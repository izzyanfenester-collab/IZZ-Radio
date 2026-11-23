package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * Premium, truly legible 7x9 LED marquee.
 * Never rasterize a TextView/font into sampled pixels: each glyph has explicit
 * on/off LEDs, so letters retain their shape while moving by fractional pixels.
 */
final class LedMarqueeView extends View {
    private static final int ROWS=LedMatrixFont.HEIGHT;
    private static final int DARK=0xff151515;
    private static final int BACKGROUND=0xff050505;
    private static final long INTRO_PAUSE_MS=1100L;
    private static final long REPEAT_PAUSE_MS=1300L;

    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private LedBannerState state=LedBannerState.ready();
    private LedMatrixMessage matrix=LedMatrixMessage.from(state);
    private float pitch=1f;
    private float scrollPx=0f;
    private long previousFrameMs=0L;
    private long pauseUntilMs=0L;
    private boolean active=true;

    LedMarqueeView(Context context) { super(context); initialize(); }
    LedMarqueeView(Context context,AttributeSet attrs) { super(context,attrs); initialize(); }

    private void initialize() {
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription("Radio LED display: IZZ RADIO - READY");
        setWillNotDraw(false);
    }

    private float dp(float value) {
        return value*getResources().getDisplayMetrics().density;
    }

    void setDisplay(LedBannerState next) {
        if(next==null || next.equals(state)) return;
        state=next;
        matrix=LedMatrixMessage.from(state); // Build fixed dot columns only on metadata changes.
        setContentDescription("Radio LED display: "+state.station+" - "+state.detail);
        resetMarquee(INTRO_PAUSE_MS);
        invalidate();
    }

    void setActive(boolean enabled) {
        if(active==enabled) return;
        active=enabled;
        previousFrameMs=0;
        if(active) invalidate();
    }

    private void resetMarquee(long holdMs) {
        scrollPx=0;
        previousFrameMs=0;
        pauseUntilMs=SystemClock.uptimeMillis()+holdMs;
    }

    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) {
        super.onSizeChanged(w,h,oldW,oldH);
        // Exactly nine LED rows, approximately 80% of the inner display height.
        pitch=Math.max(dp(2f),(h-dp(8))/(ROWS+1.0f));
        resetMarquee(INTRO_PAUSE_MS);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width=getWidth(),height=getHeight();
        if(width<10 || height<10) return;
        float radius=Math.min(dp(12),height*.25f);
        float left=dp(8),right=width-dp(8);
        float top=(height-ROWS*pitch)*.5f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(BACKGROUND);
        canvas.drawRoundRect(dp(1),dp(1),width-dp(1),height-dp(1),radius,radius,paint);
        paint.setColor(0xff183440);
        canvas.drawRoundRect(dp(1),dp(1),width-dp(1),dp(3),radius,radius,paint);

        canvas.save();
        canvas.clipRect(left,dp(4),right,height-dp(4));

        // Inactive dots are subdued and never interfere with the letters.
        paint.setColor(DARK);
        for(float x=left+pitch*.5f;x<right;x+=pitch) {
            for(int row=0;row<ROWS;row++)
                canvas.drawCircle(x,top+(row+.5f)*pitch,pitch*.16f,paint);
        }

        float availableWidth=right-left;
        float messageWidth=matrix.columns.length*pitch;
        boolean marquee=messageWidth>availableWidth;
        float origin=marquee?left+pitch:left+(availableWidth-messageWidth)*.5f;
        float offset=marquee?scrollPx:0f;
        float litRadius=pitch*.34f;
        float glowRadius=pitch*.42f;
        int first=Math.max(0,(int)Math.floor((left-origin+offset)/pitch)-1);
        int last=Math.min(matrix.columns.length,(int)Math.ceil((right-origin+offset)/pitch)+2);

        // Pixel-perfect glyph patterns: scroll only changes the x position, never the LED bits.
        for(int col=first;col<last;col++) {
            int bits=matrix.columns[col];
            if(bits==0) continue;
            float x=origin+(col+.5f)*pitch-offset;
            int colour=matrix.colours[col];
            for(int row=0;row<ROWS;row++) {
                if((bits&(1<<row))==0) continue;
                float y=top+(row+.5f)*pitch;
                paint.setColor((colour&0x00ffffff)|0x26000000);
                canvas.drawCircle(x,y,glowRadius,paint);
                paint.setColor(colour);
                canvas.drawCircle(x,y,litRadius,paint);
            }
        }
        canvas.restore();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.3f));
        paint.setColor(0xff2998ba);
        canvas.drawRoundRect(dp(.8f),dp(.8f),width-dp(.8f),height-dp(.8f),radius,radius,paint);
        paint.setStyle(Paint.Style.FILL);

        // Only animate while visible; no periodic work in the background or clock screen.
        if(marquee && active && getWindowVisibility()==VISIBLE && isShown()) {
            long now=SystemClock.uptimeMillis();
            if(previousFrameMs>0 && now>=pauseUntilMs) {
                long delta=Math.max(0L,Math.min(75L,now-previousFrameMs));
                scrollPx+=dp(22f)*delta/1000f;
                if(scrollPx>messageWidth+pitch*2f) resetMarquee(REPEAT_PAUSE_MS);
            }
            previousFrameMs=now;
            postInvalidateDelayed(33L); // 30 FPS is sufficient for a car head unit.
        } else {
            previousFrameMs=0;
        }
    }

    @Override protected void onDetachedFromWindow() {
        active=false;
        previousFrameMs=0;
        super.onDetachedFromWindow();
    }
}
