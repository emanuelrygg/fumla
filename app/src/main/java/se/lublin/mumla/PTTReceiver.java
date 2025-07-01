package se.lublin.mumla;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.session.MediaSession;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.core.content.ContextCompat;

import se.lublin.mumla.service.MumlaService;
import se.lublin.mumla.service.PTTForegroundService;

public class PTTReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event == null) return;

        Log.i("PTTReceiver", "Received action: " + action);

        if ("com.sonim.intent.action.PTT_KEY_DOWN".equals(action)) {
            MumlaService.instance.onTalkKeyDown();
        } else if ("com.sonim.intent.action.PTT_KEY_UP".equals(action)) {
            MumlaService.instance.onTalkKeyUp();
        }
    }

}
