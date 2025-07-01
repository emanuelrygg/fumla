package se.lublin.mumla;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import se.lublin.mumla.service.MumlaService;

public class PTTReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event == null) return;

        Log.i("PTTReceiver", "Received action: " + action);

        try {
            if ("com.sonim.intent.action.PTT_KEY_DOWN".equals(action)) {
                MumlaService.instance.onTalkKeyDown();
            } else if ("com.sonim.intent.action.PTT_KEY_UP".equals(action)) {
                MumlaService.instance.onTalkKeyUp();
            }
        } catch (NullPointerException e) {
            Log.w("PTTReceiver", "MumlaService.instance was null — ignoring " + action);
        }
    }


}
