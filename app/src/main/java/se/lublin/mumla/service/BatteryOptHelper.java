package se.lublin.mumla.service;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

public final class BatteryOptHelper {

    private BatteryOptHelper() {}

    /** Call once (e.g., after onboarding). Safe no-op on < API 23 or if already exempt. */
    public static void promptIgnoreBatteryOptimizations(Activity activity) {
      //  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        String pkg = activity.getPackageName();
        if (pm != null && !pm.isIgnoringBatteryOptimizations(pkg)) {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + pkg));
                activity.startActivity(i);
            } catch (ActivityNotFoundException e) {
                // Fallback: open the system list so user can whitelist manually
                Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                activity.startActivity(i);
            }
        }
    }
}
