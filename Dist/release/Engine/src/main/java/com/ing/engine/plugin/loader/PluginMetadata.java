package com.ing.engine.plugin.loader;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads plugin metadata from installed plugin JAR manifests and .plugininfo files.
 * <p>
 * This provides a way to inspect a plugin's name, version, description, author,
 * and entry classes — both from JAR manifest attributes (author-defined) and from
 * the .plugininfo file written by PluginManagerService during installation.
 * </p>
 */
public class PluginMetadata {
    private static final Logger LOG = Logger.getLogger(PluginMetadata.class.getName());

    // Manifest attribute keys that plugin authors can set
    public static final String MF_PLUGIN_NAME = "Plugin-Name";
    public static final String MF_PLUGIN_VERSION = "Plugin-Version";
    public static final String MF_PLUGIN_DESCRIPTION = "Plugin-Description";
    public static final String MF_PLUGIN_AUTHOR = "Plugin-Author";
    public static final String MF_PLUGIN_AUTHOR_EMAIL = "Plugin-Author-Email";
    public static final String MF_PLUGIN_MIN_ENGINE = "Plugin-Min-Engine-Version";
    public static final String MF_PLUGIN_ENTRY_CLASSES = "pluginEntryClasses";

    private final String name;
    private final String version;
    private final String description;
    private final String author;
    private final String authorEmail;
    private final String minEngineVersion;
    private final String entryClasses;
    private final String source;

    /**
     * Creates PluginMetadata by reading a JAR's manifest.
     */
    public PluginMetadata(File jarFile) {
        this.source = jarFile.getAbsolutePath();
        String n = null, v = null, d = null, a = null, ae = null, me = null, ec = null;
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                java.util.jar.Attributes attrs = manifest.getMainAttributes();
                n = attrs.getValue(MF_PLUGIN_NAME);
                v = attrs.getValue(MF_PLUGIN_VERSION);
                d = attrs.getValue(MF_PLUGIN_DESCRIPTION);
                a = attrs.getValue(MF_PLUGIN_AUTHOR);
                ae = attrs.getValue(MF_PLUGIN_AUTHOR_EMAIL);
                me = attrs.getValue(MF_PLUGIN_MIN_ENGINE);
                ec = attrs.getValue(MF_PLUGIN_ENTRY_CLASSES);
            }
            if (n == null) {
                // Fallback: derive name from JAR filename
                String jarName = jarFile.getName();
                if (jarName.endsWith(".jar")) {
                    n = jarName.substring(0, jarName.length() - 4);
                } else {
                    n = jarName;
                }
            }
        } catch (Exception e) {
            LOG.log(
                Level.WARNING,
                "Failed to read manifest from {0}: {1}",
                new Object[] { jarFile.getName(), e.getMessage() }
            );
            n = jarFile.getName();
            if (n.endsWith(".jar")) n = n.substring(0, n.length() - 4);
        }
        this.name = n;
        this.version = v != null ? v : "?";
        this.description = d != null ? d : "";
        this.author = a != null ? a : "";
        this.authorEmail = ae != null ? ae : "";
        this.minEngineVersion = me != null ? me : "";
        this.entryClasses = ec != null ? ec : "";
    }

    /**
     * Creates PluginMetadata from a .plugininfo properties file.
     */
    public PluginMetadata(File pluginDir, Properties info) {
        this.source = pluginDir.getAbsolutePath();
        this.name = info.getProperty("name", pluginDir.getName());
        this.version = info.getProperty("version", "?");
        this.description = info.getProperty("description", "");
        this.author = info.getProperty("author", "");
        this.authorEmail = info.getProperty("authorEmail", "");
        this.minEngineVersion = info.getProperty("minEngineVersion", "");
        this.entryClasses = info.getProperty("entryClasses", "");
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getMinEngineVersion() {
        return minEngineVersion;
    }

    public String getEntryClasses() {
        return entryClasses;
    }

    public String getSource() {
        return source;
    }

    /**
     * Extracts plugin metadata from the first JAR found in a plugin directory.
     * Falls back to reading .plugininfo if no JAR with manifest metadata is found.
     */
    public static PluginMetadata fromPluginDirectory(File pluginDir) {
        // First try reading from JAR manifests
        File[] jars = pluginDir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                try {
                    return new PluginMetadata(jar);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Could not read metadata from {0}", jar.getName());
                }
            }
        }

        // Fall back to .plugininfo
        File infoFile = new File(pluginDir, ".plugininfo");
        if (infoFile.exists()) {
            try {
                Properties info = new Properties();
                try (InputStream is = new java.io.FileInputStream(infoFile)) {
                    info.load(is);
                }
                return new PluginMetadata(pluginDir, info);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to read .plugininfo from {0}", pluginDir.getName());
            }
        }

        // Minimal fallback
        return new PluginMetadata(pluginDir, new Properties());
    }

    @Override
    public String toString() {
        return name + " " + version;
    }
}
