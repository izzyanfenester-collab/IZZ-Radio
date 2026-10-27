package com.izzyan.izzradio;
final class ScreensaverPreference {
    static final String DIGITAL="digital";
    static final String AQUARIUM="aquarium";
    static String normalize(String name) {
        return AQUARIUM.equals(name)?AQUARIUM:DIGITAL;
    }
}
