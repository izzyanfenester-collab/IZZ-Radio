package com.izzyan.izzradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Handle actual Android reboots and optional OEM Quick Boot startup signals.
 * A sleep-to-wake cycle does not necessarily emit BOOT_COMPLETED; PlaybackService
 * handles SCREEN_OFF -> SCREEN_ON while its process is kept alive by the head unit.
 */
public final class BootReceiver extends BroadcastReceiver {
    private static final String QUICKBOOT_ACTION = "android.intent.action.QUICKBOOT_POWERON";
    private static final String HTC_QUICKBOOT_ACTION = "com.htc.intent.action.QUICKBOOT_POWERON";

    @Override public void onReceive(Context context, Intent intent) {
        if(intent==null) return;
        String action=intent.getAction();
        boolean startup=Intent.ACTION_BOOT_COMPLETED.equals(action)
                || QUICKBOOT_ACTION.equals(action)
                || HTC_QUICKBOOT_ACTION.equals(action);
        if(!startup) return;

        android.content.SharedPreferences prefs=context.getSharedPreferences("izz_radio_prefs",Context.MODE_PRIVATE);
        if(!prefs.getBoolean("auto_launch_boot",false)) return;

        // Some firmware sends BOOT_COMPLETED and QUICKBOOT_POWERON together.
        // De-duplicate without tying anything to the wall-clock date.
        long elapsed=android.os.SystemClock.elapsedRealtime();
        long previous=prefs.getLong("last_boot_launch_elapsed_ms",-1L);
        if(previous>=0 && elapsed>=previous && elapsed-previous<30000L) return;

        Intent launch=context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if(launch==null) return;
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra("auto_resume_last",true);

        try {
            context.startActivity(launch);
            prefs.edit().putLong("last_boot_launch_elapsed_ms",elapsed).apply();
        } catch(Exception ignored) {
            // Android 10+ and OEM firmware can prohibit background activity launches.
            // The head unit's built-in Auto Start feature may be required.
        }
    }
}
