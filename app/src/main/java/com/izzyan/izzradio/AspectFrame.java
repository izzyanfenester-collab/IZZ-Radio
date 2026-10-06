package com.izzyan.izzradio;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
/** Measures the content itself at 16:9; no nonuniform scaling. */
final class AspectFrame extends FrameLayout {
    AspectFrame(Context context) { super(context); setBackgroundColor(0xff000000); }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);
        int cw=Math.min(w,(int)(h*16f/9f)),ch=Math.round(cw*9f/16f);
        for(int i=0;i<getChildCount();i++) getChildAt(i).measure(MeasureSpec.makeMeasureSpec(cw,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(ch,MeasureSpec.EXACTLY));
        setMeasuredDimension(w,h);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b) {
        for(int i=0;i<getChildCount();i++) { View v=getChildAt(i); int x=(getWidth()-v.getMeasuredWidth())/2,y=(getHeight()-v.getMeasuredHeight())/2; v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight()); }
    }
}
