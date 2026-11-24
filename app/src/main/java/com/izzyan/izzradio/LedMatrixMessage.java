package com.izzyan.izzradio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Precomputed LED columns and colours; immutable between metadata updates. */
final class LedMatrixMessage {
    static final int CYAN=0xffffbd37;
    static final int GOLD=0xffffbd37;
    static final int RED=0xffffbd37;
    static final int GREEN=0xffffbd37;
    static final int WHITE=0xffffbd37;

    final int[] columns; // Each int contains nine vertical LED bits.
    final int[] colours;

    private LedMatrixMessage(int[] columns,int[] colours) {
        this.columns=columns;
        this.colours=colours;
    }

    static LedMatrixMessage from(LedBannerState state) {
        Builder builder=new Builder();
        // V1.45: all station/song/status text uses the same reference Golden Amber.
        int stationColour=GOLD, detailsColour=GOLD;
        if(state.mode==LedBannerState.SELAWAT) stationColour=detailsColour=GREEN;
        else if(state.mode==LedBannerState.ERROR) stationColour=detailsColour=RED;
        else if(state.mode==LedBannerState.WAITING) stationColour=detailsColour=GOLD;
        else if(state.mode==LedBannerState.READY) stationColour=detailsColour=WHITE;
        builder.append(state.station,stationColour);
        builder.append("  -  ",detailsColour);
        builder.append(state.detail,detailsColour);
        if(state.live && !"LIVE RADIO".equalsIgnoreCase(state.detail.trim())) {
            builder.append("   LIVE",RED);
        }
        return builder.finish();
    }

    private static final class Builder {
        final List<Integer> bits=new ArrayList<>();
        final List<Integer> colours=new ArrayList<>();

        void append(String input,int colour) {
            String text=LedMatrixFont.normalize(input);
            for(int i=0;i<text.length();i++) {
                char ch=text.charAt(i);
                if(ch==' ') {
                    for(int j=0;j<LedMatrixFont.WORD_GAP;j++) add(0,colour);
                    continue;
                }
                int[] rows=LedMatrixFont.rows(ch);
                for(int col=0;col<LedMatrixFont.WIDTH;col++) {
                    int value=0;
                    for(int row=0;row<LedMatrixFont.HEIGHT;row++) {
                        if((rows[row]&(1<<(LedMatrixFont.WIDTH-1-col)))!=0) value|=1<<row;
                    }
                    add(value,colour);
                }
                for(int j=0;j<LedMatrixFont.CHARACTER_GAP;j++) add(0,colour);
            }
        }

        void add(int value,int colour) {
            bits.add(value);
            colours.add(colour);
        }
        LedMatrixMessage finish() {
            // Small, repeatable separator is part of the cached cycle itself.
            // This ensures the next copy starts immediately after the previous message.
            for(int gap=0;gap<8;gap++) add(0,GOLD);
            int[] columns=new int[bits.size()],rgb=new int[colours.size()];
            for(int i=0;i<columns.length;i++) { columns[i]=bits.get(i); rgb[i]=colours.get(i); }
            return new LedMatrixMessage(columns,rgb);
        }
    }
}
