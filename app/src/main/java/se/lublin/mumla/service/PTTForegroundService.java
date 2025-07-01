package se.lublin.mumla.service;


import static android.content.ContentValues.TAG;

import android.app.PendingIntent;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;

import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import se.lublin.humla.HumlaService;
import se.lublin.humla.audio.inputmode.ToggleInputMode;
import se.lublin.mumla.PTTReceiver;
import se.lublin.mumla.R;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class PTTForegroundService extends Service {

    private static final String CHANNEL_ID = "PTTServiceChannel";

    @Override
    public void onCreate() {

        super.onCreate();
//        initMediaSession();


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

    public MediaSessionCompat mediaSession;

    private void initMediaSession() {
        if (mediaSession != null) return;

        mediaSession = new MediaSessionCompat(this, "PlumbleMediaSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);


        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE)
                .build());
        mediaSession.setActive(true);


        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);


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
