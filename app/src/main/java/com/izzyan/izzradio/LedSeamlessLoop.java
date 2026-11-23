package com.izzyan.izzradio;

/** Pure wrap math shared by the real LED renderer and JVM unit tests. */
final class LedSeamlessLoop {
    private LedSeamlessLoop() {}

    static float wrap(float offset, float period) {
        if(!(period>0f) || !Float.isFinite(period)) return 0f;
        float result=offset%period;
        if(result<0f) result+=period;
        return result;
    }

    static int columnIndex(int offset, int viewportColumn, int count) {
        if(count<=0) throw new IllegalArgumentException("Empty LED message");
        return Math.floorMod(offset+viewportColumn,count);
    }
}
