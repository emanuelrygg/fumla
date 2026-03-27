package se.lublin.mumla.service;

import static android.os.Build.VERSION_CODES.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import se.lublin.mumla.app.MumlaActivity;

public class PttService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        Log.d("PTT", "PttService action=" + action);

        // Must post a notification immediately (within ~5s) or Android kills it
        startForeground(1, buildNotification());

        if ("android.intent.action.PTT.down".equals(action)) {
          //  startTx();
        } else if ("android.intent.action.PTT.up".equals(action)) {
           // stopTx();
        }
        return START_STICKY;
    }
    private Notification buildNotification() {

        Intent openAppIntent = new Intent(this, MumlaActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "ww")
                        .setContentTitle("Mumla PTT")
                        .setContentText("Ready")
                //        .setSmallIcon(R.drawable.ic_stat_ptt) // must exist
                        .setContentIntent(contentIntent)
                        .setOngoing(true)
                        .setSilent(true)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}