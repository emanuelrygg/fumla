package se.lublin.mumla.service;


import static android.content.ContentValues.TAG;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import se.lublin.mumla.R;
import androidx.core.app.NotificationCompat;

public class PTTForegroundService extends Service {

    private static final String CHANNEL_ID = "PTTServiceChannel";

    @Override
    public void onCreate() {

        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Push-To-Talk")
                .setContentText("Listening for Push-To-Talk from media buttons")
                .setSmallIcon(R.drawable.ic_talking_off)
                .build();

        Log.i("PTTForegroundService", "Preparing to call startForeground");
        startForeground(1, notification);
        Log.i("PTTForegroundService", "startForeground completed");

        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Received null Intent or Action; ignoring start command.");
            return START_STICKY; // Or STOP_SELF depending on desired behavior
        }

        String action = intent.getAction();
        if (MumlaService.instance != null && action != null) {
            // Determine source
            String source = intent.getStringExtra("source");
            if (source == null) source = "media"; // default to media if not set

            MumlaService.instance.setLastPTTSource(source);
            if (MumlaService.instance != null) {
                if ("com.morlunk.mumbleclient.ACTION_PTT_DOWN".equals(action)) {
                    MumlaService.instance.onTalkKeyDown();
                } else if ("com.morlunk.mumbleclient.ACTION_PTT_UP".equals(action)) {
                    MumlaService.instance.onTalkKeyUp();
                }
            }
        }

        return flags;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "PTT Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
