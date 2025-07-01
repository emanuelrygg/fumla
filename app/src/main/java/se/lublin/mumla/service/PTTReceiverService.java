package se.lublin.mumla.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;


import androidx.core.app.NotificationCompat;

import se.lublin.mumla.PTTReceiver;
import se.lublin.mumla.R;


public class PTTReceiverService extends Service {

    private PTTReceiver pttReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        // Register BroadcastReceiver dynamically
        pttReceiver = new PTTReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        registerReceiver(pttReceiver, filter);

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "PTTChannel")
                .setContentTitle("PTT Ready")
                .setContentText("Listening for hardware PTT")
                .setSmallIcon(R.drawable.ic_talking_off)
                .build();

        startForeground(1001, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(pttReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "PTTChannel",
                    "PTT Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
