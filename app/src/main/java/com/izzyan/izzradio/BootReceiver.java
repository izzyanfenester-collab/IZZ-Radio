package com.izzyan.izzradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        boolean enabled = context
                .getSharedPreferences("izz_radio_prefs", Context.MODE_PRIVATE)
                .getBoolean("auto_launch_boot", false);
        if (!enabled) return;

        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) return;

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(launch);
        } catch (Exception ignored) {
            // Some Android builds restrict background activity launches.
            // Manual launch remains available and no relaunch loop is used.
        }
    }
}
