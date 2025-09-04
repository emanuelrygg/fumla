package se.lublin.mumla.app;

import se.lublin.mumla.service.MumlaService;

public class PttToggleLaunchActivity extends android.app.Activity {
    @Override protected void onCreate(android.os.Bundle b) {
        super.onCreate(b);
        MumlaService.instance.onTalkKeyDown();
        finish(); overridePendingTransition(0,0);
    }
}