package se.lublin.mumla.service;// package se.lublin.mumla.accessibility;  // <-- sett korrekt package

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

// IMPORTER din MediaButtonService
// import se.lublin.mumla.service.MediaButtonService; // <-- juster sti hvis nødvendig

public class PttKeyAccessibilityService extends AccessibilityService {

    private static final String TAG = "PttKeyA11y";

    // XR21-observasjoner fra din logcat
    private static final int PTT_KEYCODE = 400;
    private static final int PTT_SCANCODE = 752;

    private PowerManager.WakeLock pttWl;
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        pttWl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mumla:ptt");
        pttWl.setReferenceCounted(false);
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        final boolean isPtt = (event.getKeyCode() == 400) || (event.getScanCode() == 752);
        if (!isPtt) return false;

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            // Keep CPU on while user holds the PTT
            if (pttWl != null && !pttWl.isHeld()) pttWl.acquire(60_000); // safety timeout 60s
            MumlaService.instance.onTalkKeyDown();
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            MumlaService.instance.onTalkKeyUp();
            if (pttWl != null && pttWl.isHeld()) pttWl.release();
            return true;
        }
        return false;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) { /* ikke brukt */ }
    @Override public void onInterrupt() { /* ikke brukt */ }
}
