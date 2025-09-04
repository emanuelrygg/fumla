package se.lublin.mumla;


import android.app.ForegroundServiceStartNotAllowedException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import se.lublin.mumla.service.MumlaService;     // use your actual package
import se.lublin.mumla.util.DatabaseStore;

public class BootReceiver extends BroadcastReceiver {
    public static final String ACTION_AUTOCONNECT = "android.intent.action.LOCKED_BOOT_COMPLETED";

    // BootReceiver.java
    @Override
    public void onReceive(Context context, Intent intent) {
        //if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        //SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
       // boolean autoReconnect = sp.getBoolean("autoReconnect", false); // or your own key
       // if (!autoReconnect) return;
        Log.d("Autoconnect", "Receives in bootreceiver");

        Intent svc = new Intent(context, MumlaService.class)
                .setAction(ACTION_AUTOCONNECT);
        ContextCompat.startForegroundService(context, svc);

    }

}
