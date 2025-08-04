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
import android.app.Service;
import android.content.Intent;
import android.media.session.MediaSession;
import android.os.IBinder;
import android.util.Log;
public class MediaButtonService extends Service {


    private static final String CHANNEL_ID = "PTTServiceChannel";

    public static MediaSession mMediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Received null Intent or Action; ignoring start command.");
            return START_STICKY; // Or STOP_SELF depending on desired behavior
        }

        return flags;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
