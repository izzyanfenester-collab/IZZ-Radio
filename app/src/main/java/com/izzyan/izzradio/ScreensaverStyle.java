package com.izzyan.izzradio;

/** Stable persisted identifiers for the two independent screensaver layouts. */
final class ScreensaverStyle {
    static final String DIGITAL="digital";
    static final String VINTAGE="vintage";

    private ScreensaverStyle(){}

    static String normalize(String selected){
        return VINTAGE.equals(selected)?VINTAGE:DIGITAL;
    }
}
