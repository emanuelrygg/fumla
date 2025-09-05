/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.lublin.mumla.service;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.session.MediaSession;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import se.lublin.mumla.R;
import se.lublin.mumla.app.MumlaActivity;

public class MediaButtonService extends Service {

    public static MediaSession mMediaSession;

    private static final String NOTIF_CHANNEL_SHARED = "voice_foreground";
    @Override
    public void onCreate() {
        super.onCreate();

        Notification notification = createServiceNotification("Mediasession", "Listening to media input");
        startForeground(99, notification);

        ensureMediaSession(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Received null Intent or Action; ignoring start command.");
            return START_STICKY; // Or STOP_SELF depending on desired behavior
        }

        return flags;
    }

    public Notification createServiceNotification(String title, String text) {
        return new NotificationCompat.Builder(this, NOTIF_CHANNEL_SHARED)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_mumla) // replace with your app’s status icon
                .setOngoing(true)
                .setForegroundServiceBehavior(
                        NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
                )
                .build();
    }


    public static boolean keyaction(KeyEvent e)
    {
        if (e.getAction() == KeyEvent.ACTION_DOWN)
        {
            MumlaActivity.toogleformediasession = true;
            if (MumlaService.instance != null) {
                MumlaService.instance.onTalkKeyDown();
            }
            MumlaActivity.toogleformediasession = false;
        }
        else if (e.getAction() == KeyEvent.ACTION_UP)
        {
            MumlaActivity.toogleformediasession = true;
            if (MumlaService.instance != null) {
                MumlaService.instance.onTalkKeyUp();
            }
            MumlaActivity.toogleformediasession = false;
        }
        return true;
    }

    @SuppressLint("SuspiciousIndentation")
    public void ensureMediaSession(Context context) {
        if (mMediaSession == null) {
            mMediaSession = new MediaSession(context, "MumlaSession");
            mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
//            mMediaSession.setPlaybackToLocal(attrs);
            mMediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                    KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event == null)
                        return super.onMediaButtonEvent(mediaButtonIntent);
                    keyaction(event);
                    return super.onMediaButtonEvent(mediaButtonIntent);
                }
            });
        }
        mMediaSession.setActive(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
