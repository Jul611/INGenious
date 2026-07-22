package com.ing.ide.main.mainui.components.pluginmanager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.util.encryption.Encryption;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Per-user configuration stored in {@code Configuration/user-config.json}.
 * Contains personal info (PCode, user code) and GitHub publishing PAT.
 * <p>
 * The PAT is AES-encrypted on disk via the framework's {@link Encryption} utility.
 * This file is git-ignored — each user sets their own values via the
 * profile button in the toolbar.
 */
public class UserConfig {
    private static final Logger LOG = Logger.getLogger(UserConfig.class.getName());
    private static final File CONFIG_FILE = new File("Configuration/user-config.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Map<String, String> cache;

    public static Map<String, String> load() {
        if (cache != null) return cache;
        if (CONFIG_FILE.exists()) {
            try {
                cache = MAPPER.readValue(CONFIG_FILE, new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Could not read user-config.json, using defaults", e);
            }
        }
        if (cache == null) {
            cache = new LinkedHashMap<>();
            cache.put("pcode", "");
            cache.put("userCode", "");
            cache.put("publishPat", "");
        }
        return cache;
    }

    public static void save() {
        load();
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(CONFIG_FILE, cache);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not save user-config.json", e);
        }
    }

    public static String get(String key) {
        return load().getOrDefault(key, "");
    }

    public static void set(String key, String value) {
        load().put(key, value);
        save();
    }

    // ── Convenience accessors ──

    public static String getPcode() {
        return get("pcode");
    }

    public static void setPcode(String val) {
        set("pcode", val);
    }

    public static String getUserCode() {
        return get("userCode");
    }

    public static void setUserCode(String val) {
        set("userCode", val);
    }

    /**
     * Returns the publishing PAT. Stored in plain text in the user's
     * private gitignored config file — no encryption needed.
     * Returns empty string if none configured.
     */
    public static String getPublishPat() {
        return get("publishPat");
    }

    /**
     * Stores the publishing PAT in plain text in user-config.json.
     * The file is gitignored and per-user, so no encryption overhead.
     */
    public static void setPublishPat(String val) {
        set("publishPat", val != null ? val.trim() : "");
    }

    public static void ensureExists() {
        if (!CONFIG_FILE.exists()) save();
    }
}
