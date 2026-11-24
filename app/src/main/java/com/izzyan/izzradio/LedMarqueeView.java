package com.izzyan.izzradio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * V1.45: exact image-measured golden amber 7x9 LED alphabet, preserving seamless scrolling.
 * The existing Home banner dimensions and blue housing are never changed.
 * Character LEDs are fixed cached columns; only their on-screen positions move.
 */
final class LedMarqueeView extends View {
    private static final int ROWS=LedMatrixFont.HEIGHT;
    private static final int DARK=0xff171715;
    private static final int BACKGROUND=0xff111110;

    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private LedBannerState state=LedBannerState.ready();
    private LedMatrixMessage matrix=LedMatrixMessage.from(state);
    private float pitch=1f;
    private float scrollPx=0f;
    private long previousFrameMs=0L;
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
        matrix=LedMatrixMessage.from(state); // Only on station/status/song changes.
        setContentDescription("Radio LED display: "+state.station+" - "+state.detail);
        scrollPx=0f;
        previousFrameMs=0L;
        invalidate(); // New message appears immediately, no initial hold.
    }

    void setActive(boolean enabled) {
        if(active==enabled) return;
        active=enabled;
        previousFrameMs=0L; // Ignore offscreen time; resume from the same LED position.
        if(active) invalidate();
    }

    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) {
        super.onSizeChanged(w,h,oldW,oldH);
        // Measured reference nine-dot-high glyphs fit WITHIN the unchanged 42dp banner.
        pitch=Math.max(1f,(h-dp(8))/(ROWS+1f));
        scrollPx=0f;
        previousFrameMs=0L;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width=getWidth(), height=getHeight();
        if(width<10 || height<10 || matrix.columns.length==0) return;
        float radius=Math.min(dp(12),height*.25f);
        float left=dp(8), right=width-dp(8);
        float top=(height-ROWS*pitch)*.5f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(BACKGROUND);
        canvas.drawRoundRect(dp(1),dp(1),width-dp(1),height-dp(1),radius,radius,paint);
        paint.setColor(0xff183440);
        canvas.drawRoundRect(dp(1),dp(1),width-dp(1),dp(3),radius,radius,paint);

        canvas.save();
        canvas.clipRect(left,dp(4),right,height-dp(4));
        paint.setColor(DARK);
        for(float x=left+pitch*.5f;x<right;x+=pitch) {
            for(int row=0;row<ROWS;row++) {
                canvas.drawCircle(x,top+(row+.5f)*pitch,pitch*.18f,paint);
            }
        }

        // The same loop applies to long AND short text, including READY and SELAWAT.
        // A cached period includes a short inter-message gap and repeats indefinitely.
        float period=matrix.columns.length*pitch;
        float wrapped=LedSeamlessLoop.wrap(scrollPx,period);
        int firstColumn=(int)Math.floor(wrapped/pitch);
        float phase=wrapped-firstColumn*pitch;
        int visibleColumns=(int)Math.ceil((right-left)/pitch)+3;
        final float litRadius=pitch*.28f;
        final float glowRadius=pitch*.43f;
        for(int visualCol=-1;visualCol<visibleColumns;visualCol++) {
            float x=left+(visualCol+.5f)*pitch-phase;
            if(x+glowRadius<left || x-glowRadius>right) continue;
            // Modulo indexing is continuous across every repeat boundary.
            int index=LedSeamlessLoop.columnIndex(firstColumn,visualCol,matrix.columns.length);
            int bits=matrix.columns[index];
            if(bits==0) continue;
            int colour=matrix.colours[index];
            for(int row=0;row<ROWS;row++) {
                if((bits&(1<<row))==0) continue;
                float y=top+(row+.5f)*pitch;
                paint.setColor((colour&0x00ffffff)|0x33000000);
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

        // Real elapsed time preserves speed regardless of frame rate.
        // Do not animate while app is hidden or the Screensaver is shown.
        if(active && getWindowVisibility()==VISIBLE && isShown()) {
            long now=SystemClock.uptimeMillis();
            if(previousFrameMs>0 && now>previousFrameMs) {
                scrollPx=LedSeamlessLoop.wrap(
                    scrollPx+dp(25f)*(now-previousFrameMs)/1000f,period);
            }
            previousFrameMs=now;
            postInvalidateDelayed(33L);
        } else {
            previousFrameMs=0L;
        }
    }

    @Override protected void onDetachedFromWindow() {
        active=false;
        previousFrameMs=0L;
        super.onDetachedFromWindow();
    }
}
