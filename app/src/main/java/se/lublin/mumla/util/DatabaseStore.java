package se.lublin.mumla.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import se.lublin.humla.model.Server;
import se.lublin.mumla.db.DatabaseCertificate;
import se.lublin.mumla.db.MumlaDatabase;

/**
 * SharedPreferences-backed implementation of MumlaDatabase.
 * - Servers, per-server lists, and certificate metadata are stored as JSON (Gson).
 * - Certificate blobs (PKCS12) are stored as Base64 strings.
 * - Ordering and duplicates for lists are preserved.
 *
 * NOTE: For secrets (e.g., passwords inside Server), consider EncryptedSharedPreferences
 * if you need at-rest encryption.
 */
public class DatabaseStore implements MumlaDatabase {

    private static final String PREFS = "mumla_database_store";

    private static final Gson GSON = new Gson();

    // Global keys
    private static final String KEY_SERVERS         = "servers_json";          // List<Server>
    private static final String KEY_CERT_META       = "cert_meta_json";        // List<CertMeta>
    private static final String KEY_CERT_ID_COUNTER = "cert_id_counter";       // long

    // Per-item prefixes
    private static final String K_COMMENT_PREFIX = "comment_seen:";            // + hash -> Base64(commentHash)
    private static final String K_PINNED_PREFIX  = "pinned_channels:";         // + serverId -> List<Integer>
    private static final String K_TOKENS_PREFIX  = "access_tokens:";           // + serverId -> List<String>
    private static final String K_MUTED_PREFIX   = "muted_users:";             // + serverId -> List<Integer>
    private static final String K_IGNORED_PREFIX = "ignored_users:";           // + serverId -> List<Integer>
    private static final String K_CERT_BLOB_PRE  = "cert_blob:";               // + certId  -> Base64(PKCS12)

    private final SharedPreferences prefs;

    // Gson types
    private static final Type LIST_SERVER = new TypeToken<List<Server>>(){}.getType();
    private static final Type LIST_INT    = new TypeToken<List<Integer>>(){}.getType();
    private static final Type LIST_STR    = new TypeToken<List<String>>(){}.getType();
    private static final Type LIST_CERTM  = new TypeToken<List<CertMeta>>(){}.getType();

    public DatabaseStore(@NonNull Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override public void open() { /* no-op */ }
    @Override public void close() { /* no-op */ }

    /* ------------------------- Servers ------------------------- */

    @Override
    public List<Server> getServers() {
        String json = prefs.getString(KEY_SERVERS, "[]");
        List<Server> list = GSON.fromJson(json, LIST_SERVER);
        return (list != null) ? list : new ArrayList<>();
    }

    @Override
    public void addServer(Server server) {
        List<Server> list = getServers();
        list.add(server);
        saveServers(list);
    }

    @Override
    public void updateServer(Server server) {
        List<Server> list = getServers();
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == server.getId()) {
                list.set(i, server);
                replaced = true;
                break;
            }
        }
        if (!replaced) list.add(server);
        saveServers(list);
    }

    @Override
    public void removeServer(Server server) {
        List<Server> list = getServers();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            list.removeIf(s -> s.getId() == server.getId());
        }
        saveServers(list);

        long sid = server.getId();
        prefs.edit()
                .remove(key(K_PINNED_PREFIX, sid))
                .remove(key(K_TOKENS_PREFIX, sid))
                .remove(key(K_MUTED_PREFIX, sid))
                .remove(key(K_IGNORED_PREFIX, sid))
                .apply();
    }

    private void saveServers(List<Server> servers) {
        prefs.edit().putString(KEY_SERVERS, GSON.toJson(servers, LIST_SERVER)).apply();
    }

    /* ------------------------- Comments ------------------------- */

    @Override
    public boolean isCommentSeen(String hash, byte[] commentHash) {
        String stored = prefs.getString(K_COMMENT_PREFIX + hash, null);
        return stored != null && stored.equals(toB64(commentHash));
    }

    @Override
    public void markCommentSeen(String hash, byte[] commentHash) {
        prefs.edit().putString(K_COMMENT_PREFIX + hash, toB64(commentHash)).apply();
    }

    /* ------------------------- Pinned channels ------------------------- */

    @Override
    public List<Integer> getPinnedChannels(long serverId) {
        return getIntList(key(K_PINNED_PREFIX, serverId));
    }

    @Override
    public void addPinnedChannel(long serverId, int channelId) {
        List<Integer> list = getPinnedChannels(serverId);
        if (!list.contains(channelId)) list.add(channelId);
        putIntList(key(K_PINNED_PREFIX, serverId), list);
    }

    @Override
    public void removePinnedChannel(long serverId, int channelId) {
        List<Integer> list = getPinnedChannels(serverId);
        list.remove((Integer) channelId);
        putIntList(key(K_PINNED_PREFIX, serverId), list);
    }

    @Override
    public boolean isChannelPinned(long serverId, int channelId) {
        return getPinnedChannels(serverId).contains(channelId);
    }

    /* ------------------------- Access tokens ------------------------- */

    @Override
    public List<String> getAccessTokens(long serverId) {
        return getStringList(key(K_TOKENS_PREFIX, serverId));
    }

    @Override
    public void addAccessToken(long serverId, String token) {
        List<String> list = getAccessTokens(serverId);
        if (!list.contains(token)) list.add(token);
        putStringList(key(K_TOKENS_PREFIX, serverId), list);
    }

    @Override
    public void removeAccessToken(long serverId, String token) {
        List<String> list = getAccessTokens(serverId);
        list.remove(token);
        putStringList(key(K_TOKENS_PREFIX, serverId), list);
    }

    /* ------------------------- Local muted users ------------------------- */

    @Override
    public List<Integer> getLocalMutedUsers(long serverId) {
        return getIntList(key(K_MUTED_PREFIX, serverId));
    }

    @Override
    public void addLocalMutedUser(long serverId, int userId) {
        List<Integer> list = getLocalMutedUsers(serverId);
        if (!list.contains(userId)) list.add(userId);
        putIntList(key(K_MUTED_PREFIX, serverId), list);
    }

    @Override
    public void removeLocalMutedUser(long serverId, int userId) {
        List<Integer> list = getLocalMutedUsers(serverId);
        list.remove((Integer) userId);
        putIntList(key(K_MUTED_PREFIX, serverId), list);
    }

    /* ------------------------- Local ignored users ------------------------- */

    @Override
    public List<Integer> getLocalIgnoredUsers(long serverId) {
        return getIntList(key(K_IGNORED_PREFIX, serverId));
    }

    @Override
    public void addLocalIgnoredUser(long serverId, int userId) {
        List<Integer> list = getLocalIgnoredUsers(serverId);
        if (!list.contains(userId)) list.add(userId);
        putIntList(key(K_IGNORED_PREFIX, serverId), list);
    }

    @Override
    public void removeLocalIgnoredUser(long serverId, int userId) {
        List<Integer> list = getLocalIgnoredUsers(serverId);
        list.remove((Integer) userId);
        putIntList(key(K_IGNORED_PREFIX, serverId), list);
    }

    /* ------------------------- Certificates ------------------------- */

    // Simple metadata we persist alongside the blob
    private static final class CertMeta {
        long id;
        String name;
        CertMeta(long id, String name) { this.id = id; this.name = name; }
    }

    @Override
    public DatabaseCertificate addCertificate(String name, byte[] certificate) {
        long id = prefs.getLong(KEY_CERT_ID_COUNTER, 1L);
        prefs.edit().putLong(KEY_CERT_ID_COUNTER, id + 1L).apply();

        // Store blob as Base64
        prefs.edit().putString(K_CERT_BLOB_PRE + id, toB64(certificate)).apply();

        // Append meta
        List<CertMeta> metas = getCertMetas();
        metas.add(new CertMeta(id, name));
        prefs.edit().putString(KEY_CERT_META, GSON.toJson(metas, LIST_CERTM)).apply();

        // Use your concrete DatabaseCertificate class (protected ctor; same package)
        return new DatabaseCertificate(id, name);
    }

    @Override
    public List<DatabaseCertificate> getCertificates() {
        List<CertMeta> metas = getCertMetas();
        List<DatabaseCertificate> out = new ArrayList<>(metas.size());
        for (CertMeta m : metas) {
            out.add(new DatabaseCertificate(m.id, m.name));
        }
        return out;
    }

    @Override
    public byte[] getCertificateData(long id) {
        String b64 = prefs.getString(K_CERT_BLOB_PRE + id, null);
        return (b64 != null) ? Base64.decode(b64, Base64.NO_WRAP) : null;
    }

    @Override
    public void removeCertificate(long id) {
        // Remove blob
        prefs.edit().remove(K_CERT_BLOB_PRE + id).apply();

        // Remove meta
        List<CertMeta> metas = getCertMetas();
        for (int i = 0; i < metas.size(); i++) {
            if (metas.get(i).id == id) { metas.remove(i); break; }
        }
        prefs.edit().putString(KEY_CERT_META, GSON.toJson(metas, LIST_CERTM)).apply();
    }

    private List<CertMeta> getCertMetas() {
        String json = prefs.getString(KEY_CERT_META, "[]");
        List<CertMeta> metas = GSON.fromJson(json, LIST_CERTM);
        return (metas != null) ? metas : new ArrayList<>();
    }

    /* ------------------------- Helpers ------------------------- */

    private static String key(String prefix, long serverId) {
        return prefix + serverId;
    }

    private static String toB64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private List<Integer> getIntList(String key) {
        String json = prefs.getString(key, "[]");
        List<Integer> list = GSON.fromJson(json, LIST_INT);
        return (list != null) ? list : new ArrayList<>();
    }

    private void putIntList(String key, List<Integer> list) {
        prefs.edit().putString(key, GSON.toJson(list, LIST_INT)).apply();
    }

    private List<String> getStringList(String key) {
        String json = prefs.getString(key, "[]");
        List<String> list = GSON.fromJson(json, LIST_STR);
        return (list != null) ? list : new ArrayList<>();
    }

    private void putStringList(String key, List<String> list) {
        prefs.edit().putString(key, GSON.toJson(list, LIST_STR)).apply();
    }
}
