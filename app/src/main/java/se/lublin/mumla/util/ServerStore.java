package se.lublin.mumla.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import se.lublin.humla.model.Server;

/**
 * SharedPreferences-backed persistence for Server objects.
 * - Preserves order and duplicates via JSON (Gson).
 * - Auto-assigns a unique id if server.getId() == -1 on upsert.
 * - Stores/loads a "selected" server id for convenience.
 *
 * NOTE: Passwords are stored in plaintext here. For production, consider
 * android.security.EncryptedSharedPreferences if you need at-rest protection.
 */
public final class ServerStore {

    private static final String PREFS = "mumla_server_store";
    private static final String KEY_SERVERS_JSON       = "servers_json";         // List<Server> as JSON
    private static final String KEY_SELECTED_SERVER_ID = "selected_server_id";   // long
    private static final String KEY_ID_COUNTER         = "server_id_counter";    // long

    private static final Gson GSON = new Gson();
    private static final Type LIST_SERVER = new TypeToken<List<Server>>(){}.getType();

    private final SharedPreferences prefs;

    public ServerStore(@NonNull Context ctx) {
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /* -------------------- CRUD (List<Server>) -------------------- */

    /** Returns all stored servers (empty list if none). */
    @NonNull
    public List<Server> getServers() {
        String json = prefs.getString(KEY_SERVERS_JSON, "[]");
        List<Server> list = GSON.fromJson(json, LIST_SERVER);
        return (list != null) ? list : new ArrayList<>();
    }

    /** Overwrites the entire list. */
    public void setServers(@NonNull List<Server> servers) {
        prefs.edit().putString(KEY_SERVERS_JSON, GSON.toJson(servers, LIST_SERVER)).apply();
    }

    /**
     * Insert or replace by id. If server.getId() == -1, a new unique id is assigned
     * and written back into the instance before storing.
     *
     * @return the id that the server was stored under (new or existing).
     */
    public long upsertServer(@NonNull Server server) {
        List<Server> list = getServers();

        long id = server.getId();
        if (id == -1L) {
            id = nextId();
            server.setId(id);
        }

        int idx = indexOfServerById(list, id);
        if (idx >= 0) {
            list.set(idx, server);
        } else {
            list.add(server);
        }
        setServers(list);
        return id;
    }

    /** Remove a server by id (no-op if not found). */
    public void removeServer(long serverId) {
        List<Server> list = getServers();
        int idx = indexOfServerById(list, serverId);
        if (idx >= 0) {
            list.remove(idx);
            setServers(list);
        }
        if (getSelectedServerId() == serverId) clearSelectedServerId();
    }

    /* -------------------- Selection helpers -------------------- */

    public void setSelectedServerId(long serverId) {
        prefs.edit().putLong(KEY_SELECTED_SERVER_ID, serverId).apply();
    }

    public long getSelectedServerId() {
        return prefs.getLong(KEY_SELECTED_SERVER_ID, -1L);
    }

    public void clearSelectedServerId() {
        prefs.edit().remove(KEY_SELECTED_SERVER_ID).apply();
    }

    /** Convenience: returns the selected Server object, or null if none/missing. */
    @Nullable
    public Server getSelectedServer() {
        long id = getSelectedServerId();
        if (id == -1L) return null;
        List<Server> list = getServers();
        int idx = indexOfServerById(list, id);
        return (idx >= 0) ? list.get(idx) : null;
    }

    /* -------------------- Internal helpers -------------------- */

    private int indexOfServerById(@NonNull List<Server> list, long id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == id) return i;
        }
        return -1;
    }

    /** Monotonic id generator persisted in prefs. */
    private long nextId() {
        long next = prefs.getLong(KEY_ID_COUNTER, 1L);
        prefs.edit().putLong(KEY_ID_COUNTER, next + 1L).apply();
        return next;
    }
}
