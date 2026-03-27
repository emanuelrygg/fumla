package se.lublin.mumla.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


import se.lublin.mumla.app.MumlaActivity;
import se.lublin.mumla.R;

/**
 * AutoconnectService
 *
 * Kjøres f.eks. fra BootReceiver etter BOOT_COMPLETED.
 * Oppgave:
 *  - slå opp sist brukte server
 *  - lage en notification med PendingIntent som åpner MumlaActivity
 *    med info om at vi skal forsøke autoconnect.
 *
 * Viktig: Denne servicen er IKKE foreground/microphone.
 * Den bare poster en notifikasjon og terminerer seg selv.
 */
public class AutoconnectService extends Service {

    private static final String TAG = "Autoconnect";

    // Må være unik per app. Tilpass etter behov.
    private static final String CHANNEL_ID_AUTOCONNECT = "mumla_autoconnect";
    private static final int NOTIFICATION_ID_AUTOCONNECT = 1;

    // Preferences-nøkler – tilpass til dine faktiske nøkler.
    private static final String PREF_KEY_LAST_SERVER_ID = "last_server_id";
    private static final String PREF_KEY_LAST_SERVER_NAME = "last_server_name";

    // Intent-action som MumlaActivity kan sjekke.
    public static final String ACTION_AUTOCONNECT_FROM_NOTIFICATION =
            "se.lublin.mumla.action.AUTOCONNECT_FROM_NOTIFICATION";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        new Thread(() -> {
            try {
                handleAutoconnectNotification(this);
            } catch (Exception e) {
                Log.e(TAG, "handleAutoconnectNotification failed", e);
            } finally {
                stopSelf(startId);
            }
        }).start();

        return START_REDELIVER_INTENT;
    }

    private void handleAutoconnectNotification(Context context) {
   //     SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

      //  long lastServerId = prefs.getLong(PREF_KEY_LAST_SERVER_ID, -1L);
        //if (lastServerId == -1L) {
        //    Log.d(TAG, "No last_server_id stored; skipping autoconnect notification");
        //    return;
        //}

     //   String lastServerName = prefs.getString(PREF_KEY_LAST_SERVER_NAME, "saved server");

        // Intent som åpner MumlaActivity når brukeren trykker på notification.
        Intent activityIntent = new Intent(context, MumlaActivity.class);
        activityIntent.setAction(ACTION_AUTOCONNECT_FROM_NOTIFICATION);
 //       activityIntent.putExtra("server_id", lastServerId);
        // Viktig fra Service/Receiver:
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_AUTOCONNECT)
                .setSmallIcon(R.drawable.ic_stat_notify) // sørg for at denne finnes
                .setContentTitle("Mumla klar til å koble til")
                .setContentText("Trykk for å koble til server")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_AUTOCONNECT, notification);

        Log.d(TAG, "Posted autoconnect notification");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID_AUTOCONNECT,
                    "Mumla autoconnect",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("Notification for reconnecting to last Mumla server");

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(ch);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Ingen binding – ren startService-service.
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
