package com.izzyan.izzradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        android.content.SharedPreferences prefs=context.getSharedPreferences("izz_radio_prefs",Context.MODE_PRIVATE);
        if (!prefs.getBoolean("auto_launch_boot", false)) return;

        Intent launch=context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) return;

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launch.putExtra("auto_resume_last",true);
        try {
            context.startActivity(launch);
        } catch (Exception ignored) {
            // Some Android builds restrict UI auto-launch after boot.
        }
    }
}
