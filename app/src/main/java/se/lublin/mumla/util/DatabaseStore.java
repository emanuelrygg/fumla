package se.lublin.mumla.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import androidx.annotation.NonNull;

public final class AutoReconnectStore {
    private static final String PREFS = "mumla_auto_reconnect";

    private static final String K_GLOBAL_ENABLED = "global_enabled";
    private static final String K_GLOBAL_FIXED_DELAY_MS = "global_fixed_delay_ms";

    private static final String K_ATTEMPTS_PREFIX = "attempts:";                 // +serverId
    private static final String K_LAST_FAILURE_ELAPSED_PREFIX = "last_fail_e:";  // +serverId
    private static final String K_NEXT_RECONNECT_AT_UTC_PREFIX = "next_at_utc:"; // +serverId

    private static final long DEFAULT_FIXED_DELAY_MS = 5_000L;

    private final SharedPreferences prefs;

    public AutoReconnectStore(@NonNull Context ctx) {
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- global config ----
    public void setEnabled(boolean enabled) { prefs.edit().putBoolean(K_GLOBAL_ENABLED, enabled).apply(); }
    public boolean isEnabled() { return prefs.getBoolean(K_GLOBAL_ENABLED, true); }

    public void setFixedDelayMs(long ms) { prefs.edit().putLong(K_GLOBAL_FIXED_DELAY_MS, ms).apply(); }
    public long getFixedDelayMs() { return prefs.getLong(K_GLOBAL_FIXED_DELAY_MS, DEFAULT_FIXED_DELAY_MS); }

    // ---- per-server runtime state ----
    public int incrementAttempt(long serverId) {
        String k = K_ATTEMPTS_PREFIX + serverId;
        int next = prefs.getInt(k, 0) + 1;
        prefs.edit().putInt(k, next).apply();
        return next;
    }
    public void resetAttempt(long serverId) { prefs.edit().putInt(K_ATTEMPTS_PREFIX + serverId, 0).apply(); }
    public int getAttempt(long serverId) { return prefs.getInt(K_ATTEMPTS_PREFIX + serverId, 0); }

    public void recordFailureNow(long serverId) {
        prefs.edit().putLong(K_LAST_FAILURE_ELAPSED_PREFIX + serverId, SystemClock.elapsedRealtime()).apply();
    }
    public long getLastFailureElapsed(long serverId) {
        return prefs.getLong(K_LAST_FAILURE_ELAPSED_PREFIX + serverId, -1L);
    }

    public void setNextReconnectAtUtc(long serverId, long utcMillis) {
        prefs.edit().putLong(K_NEXT_RECONNECT_AT_UTC_PREFIX + serverId, utcMillis).apply();
    }
    public long getNextReconnectAtUtc(long serverId) {
        return prefs.getLong(K_NEXT_RECONNECT_AT_UTC_PREFIX + serverId, -1L);
    }

    // convenience
    public void clearServerState(long serverId) {
        prefs.edit()
                .remove(K_ATTEMPTS_PREFIX + serverId)
                .remove(K_LAST_FAILURE_ELAPSED_PREFIX + serverId)
                .remove(K_NEXT_RECONNECT_AT_UTC_PREFIX + serverId)
                .apply();
    }
}
