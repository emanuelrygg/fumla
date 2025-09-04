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

import static se.lublin.mumla.Settings.PREF_PUSH_KEY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import se.lublin.mumla.service.MumlaService;

public class HardwareButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event == null) return;

        Log.i("PTTReceiver", "Received action: " + action);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String pttkey = preferences.getString("ptt_settings", null);
    //    if ((int)(boolean) pttkey.equals(event.getKeyCode()))
    //    {
            try
            {
                if ("com.sonim.intent.action.PTT_KEY_DOWN".equals(action)) {
                    MumlaService.instance.onTalkKeyDown();
                } else if ("com.sonim.intent.action.PTT_KEY_UP".equals(action)) {
                    MumlaService.instance.onTalkKeyUp();
                }
            }
            catch (NullPointerException e)
            {
                Log.w("PTTReceiver", "MumlaService.instance was null — ignoring " + action);
            }
   //     }

    }


}
