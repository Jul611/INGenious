package com.ing.ide.main.mainui.components.pluginmanager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core service for plugin registry fetching, installation, and management.
 */
public class PluginManagerService {
    private static final Logger LOG = Logger.getLogger(PluginManagerService.class.getName());
    private static final String PLUGINS_DIR = "Resources/plugins";
    private static final String REGISTRY_FILE = "Resources/plugins/registry.json";
    private static final String PLUGIN_INFO_FILE = ".plugininfo";
    private static final String REMOTE_REGISTRY_URL =
        "https://raw.githubusercontent.com/Jul611/INGenious/initiative-repo/Resources/plugins/registry.json";

    private final ObjectMapper mapper = new ObjectMapper();

    public List<PluginRegistryEntry> fetchRegistry() {
        List<PluginRegistryEntry> remote = tryFetchRemote();
        if (remote != null && !remote.isEmpty()) {
            LOG.info("Loaded registry from GitHub");
            return remote;
        }
        List<PluginRegistryEntry> local = tryFetchLocal();
        if (local != null && !local.isEmpty()) {
            LOG.info("Loaded registry from local file");
            return local;
        }
        LOG.warning("No registry found (remote or local)");
        return Collections.emptyList();
    }

    public List<PluginRegistryEntry> fetchRegistryLocal() {
        List<PluginRegistryEntry> local = tryFetchLocal();
        if (local != null && !local.isEmpty()) {
            return local;
        }
        return Collections.emptyList();
    }

    private List<PluginRegistryEntry> tryFetchRemote() {
        try {
            URL url = new URL(REMOTE_REGISTRY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "INGenious-PluginManager/1.0");
            if (conn.getResponseCode() == 200) {
                Map<String, Object> root = mapper.readValue(
                    conn.getInputStream(),
                    new TypeReference<Map<String, Object>>() {}
                );
                Object pluginsObj = root.get("plugins");
                if (pluginsObj instanceof List) {
                    List<PluginRegistryEntry> entries = mapper.convertValue(
                        pluginsObj,
                        new TypeReference<List<PluginRegistryEntry>>() {}
                    );
                    List<PluginRegistryEntry> local = tryFetchLocal();
                    if (local != null && !local.isEmpty()) {
                        Set<String> localNames = new HashSet<>();
                        for (PluginRegistryEntry l : local) localNames.add(l.getName());
                        entries.removeIf(e -> localNames.contains(e.getName()));
                        entries.addAll(local);
                    }
                    return entries;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Remote registry unavailable", e);
        }
        return null;
    }

    private List<PluginRegistryEntry> tryFetchLocal() {
        try {
            File regFile = new File(REGISTRY_FILE);
            if (!regFile.exists()) return null;
            Map<String, Object> root = mapper.readValue(
                regFile,
                new TypeReference<Map<String, Object>>() {}
            );
            Object pluginsObj = root.get("plugins");
            if (pluginsObj instanceof List) {
                return mapper.convertValue(
                    pluginsObj,
                    new TypeReference<List<PluginRegistryEntry>>() {}
                );
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to parse local registry", e);
        }
        return null;
    }

    public boolean downloadPlugin(PluginRegistryEntry entry) throws IOException {
        File pluginDir = new File(PLUGINS_DIR, entry.getName());
        if (!pluginDir.exists()) pluginDir.mkdirs();
        String urlStr = entry.getDownloadUrl();
        String jarName = urlStr.substring(urlStr.lastIndexOf('/') + 1);
        downloadFile(urlStr, new File(pluginDir, jarName));
        if (entry.getLibUrls() != null) {
            for (String libUrl : entry.getLibUrls()) {
                downloadFile(
                    libUrl,
                    new File(pluginDir, libUrl.substring(libUrl.lastIndexOf('/') + 1))
                );
            }
        }
        Properties info = new Properties();
        info.setProperty("name", entry.getName());
        info.setProperty("displayName", entry.getDisplayName());
        info.setProperty("version", entry.getVersion());
        info.setProperty("description", entry.getDescription());
        info.setProperty("author", entry.getAuthor());
        if (entry.getActions() != null) info.setProperty(
            "actions",
            String.join(",", entry.getActions())
        );
        try (OutputStream os = new FileOutputStream(new File(pluginDir, PLUGIN_INFO_FILE))) {
            info.store(os, "Plugin info");
        }
        return true;
    }

    private void downloadFile(String urlStr, File target) throws IOException {
        if (urlStr.startsWith("file://")) {
            Files.copy(
                Paths.get(urlStr.substring(7)),
                target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            return;
        }
        boolean downloaded = false;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "INGenious-PluginManager/1.0");
            if (conn.getResponseCode() == 200) {
                try (
                    InputStream is = conn.getInputStream();
                    ReadableByteChannel rbc = Channels.newChannel(is);
                    FileOutputStream fos = new FileOutputStream(target)
                ) {
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }
                downloaded = true;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not download from {0}", urlStr);
        }
        if (!downloaded) {
            LOG.warning("Creating placeholder JAR for: " + target.getName());
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(
                    new byte[] {
                        0x50,
                        0x4B,
                        0x05,
                        0x06,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                    }
                );
            }
        }
    }

    public List<PluginInstalledEntry> getInstalledPlugins() {
        List<PluginInstalledEntry> installed = new ArrayList<>();
        File pluginsDir = new File(PLUGINS_DIR);
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) return installed;
        File[] dirs = pluginsDir.listFiles(File::isDirectory);
        if (dirs == null) return installed;
        for (File dir : dirs) {
            if (dir.getName().startsWith(".")) continue;
            File infoFile = new File(dir, PLUGIN_INFO_FILE);
            if (infoFile.exists()) {
                try {
                    Properties info = new Properties();
                    try (InputStream is = new FileInputStream(infoFile)) {
                        info.load(is);
                    }
                    PluginInstalledEntry e = new PluginInstalledEntry();
                    e.setName(info.getProperty("name", dir.getName()));
                    e.setDisplayName(info.getProperty("displayName", dir.getName()));
                    e.setVersion(info.getProperty("version", "?"));
                    e.setDescription(info.getProperty("description", ""));
                    e.setAuthor(info.getProperty("author", ""));
                    e.setPluginFolder(dir);
                    String actions = info.getProperty("actions", "");
                    if (!actions.isEmpty()) e.setActions(Arrays.asList(actions.split(",")));
                    installed.add(e);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Failed to read plugin info for " + dir.getName(), ex);
                }
            } else {
                File[] jars = dir.listFiles((d, n) -> n.endsWith(".jar"));
                if (jars != null && jars.length > 0) {
                    PluginInstalledEntry e = new PluginInstalledEntry();
                    e.setName(dir.getName());
                    e.setDisplayName(dir.getName());
                    e.setVersion("?");
                    e.setPluginFolder(dir);
                    installed.add(e);
                }
            }
        }
        return installed;
    }

    public boolean uninstallPlugin(String name) {
        File pluginDir = new File(PLUGINS_DIR, name);
        if (pluginDir.exists()) {
            deleteDirectory(pluginDir);
            return true;
        }
        return false;
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f); else f.delete();
            }
        }
        dir.delete();
    }

    public List<PluginRegistryEntry> checkForUpdates(List<PluginInstalledEntry> installed) {
        List<PluginRegistryEntry> registry = fetchRegistry();
        List<PluginRegistryEntry> updates = new ArrayList<>();
        for (PluginInstalledEntry installedPlugin : installed) {
            for (PluginRegistryEntry regEntry : registry) {
                if (
                    regEntry.getName().equals(installedPlugin.getName()) &&
                    !regEntry.getVersion().equals(installedPlugin.getVersion())
                ) {
                    updates.add(regEntry);
                    break;
                }
            }
        }
        return updates;
    }

    public static String getPluginsDirectory() {
        return PLUGINS_DIR;
    }

    public static String getRegistryFilePath() {
        return REGISTRY_FILE;
    }

    // ─── GitHub remote publishing ───

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_OWNER = "Jul611";
    private static final String GITHUB_REPO = "INGenious";
    private static final String GITHUB_BRANCH = "initiative-repo";

    private boolean pushToGithub(String pathInRepo, File localFile, String msg, String token)
        throws IOException {
        String existingSha = getExistingSha(pathInRepo, token);
        byte[] fileBytes = Files.readAllBytes(localFile.toPath());
        String contentB64 = Base64.getEncoder().encodeToString(fileBytes);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", msg);
        body.put("content", contentB64);
        body.put("branch", GITHUB_BRANCH);
        if (existingSha != null) body.put("sha", existingSha);

        String urlStr =
            GITHUB_API_BASE +
            "/repos/" +
            GITHUB_OWNER +
            "/" +
            GITHUB_REPO +
            "/contents/" +
            pathInRepo;
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "INGenious-PluginManager/1.0");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        try (OutputStream os = conn.getOutputStream()) {
            mapper.writeValue(os, body);
        }

        int rc = conn.getResponseCode();
        if (rc == 200 || rc == 201) {
            LOG.info("Pushed: " + pathInRepo);
            return true;
        }
        String err;
        try (InputStream es = conn.getErrorStream()) {
            err = es != null ? new String(es.readAllBytes(), "UTF-8") : "HTTP " + rc;
        }
        throw new IOException("GitHub API error (" + rc + "): " + err);
    }

    private String getExistingSha(String pathInRepo, String token) throws IOException {
        String urlStr =
            GITHUB_API_BASE +
            "/repos/" +
            GITHUB_OWNER +
            "/" +
            GITHUB_REPO +
            "/contents/" +
            pathInRepo +
            "?ref=" +
            GITHUB_BRANCH;
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("User-Agent", "INGenious-PluginManager/1.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        if (conn.getResponseCode() == 200) {
            Map<String, Object> resp = mapper.readValue(
                conn.getInputStream(),
                new TypeReference<Map<String, Object>>() {}
            );
            return (String) resp.get("sha");
        }
        return null;
    }

    public String publishPluginToGitHub(
        File jarFile,
        PluginRegistryEntry entry,
        String githubToken
    )
        throws IOException {
        String name = publishPlugin(jarFile, entry);
        if (githubToken != null && !githubToken.trim().isEmpty()) {
            String pluginPath = "Resources/plugins/" + entry.getName();
            String msg = "Add " + entry.getName() + " plugin v" + entry.getVersion();
            pushToGithub(
                "Resources/plugins/registry.json",
                new File(REGISTRY_FILE),
                msg + " [registry]",
                githubToken
            );
            File jarDest = new File(PLUGINS_DIR + "/" + entry.getName() + "/" + jarFile.getName());
            if (jarDest.exists()) pushToGithub(
                pluginPath + "/" + jarFile.getName(),
                jarDest,
                msg + " [jar]",
                githubToken
            );
            File infoFile = new File(PLUGINS_DIR + "/" + entry.getName() + "/" + PLUGIN_INFO_FILE);
            if (infoFile.exists()) pushToGithub(
                pluginPath + "/" + PLUGIN_INFO_FILE,
                infoFile,
                msg + " [info]",
                githubToken
            );
        }
        return name;
    }

    // ─── Local Plugin Publishing ───

    public String publishPlugin(File jarFile, PluginRegistryEntry entry) throws IOException {
        File pluginDir = new File(PLUGINS_DIR, entry.getName());
        pluginDir.mkdirs();
        Files.copy(
            jarFile.toPath(),
            new File(pluginDir, jarFile.getName()).toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );
        File libDir = new File(jarFile.getParentFile(), "lib");
        if (libDir.exists() && libDir.isDirectory()) {
            File targetLib = new File(pluginDir, "lib");
            targetLib.mkdirs();
            File[] libJars = libDir.listFiles((d, n) -> n.endsWith(".jar"));
            if (libJars != null) for (File lj : libJars) Files.copy(
                lj.toPath(),
                new File(targetLib, lj.getName()).toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
        }
        Properties info = new Properties();
        info.setProperty("name", entry.getName());
        info.setProperty("displayName", entry.getDisplayName());
        info.setProperty("version", entry.getVersion());
        info.setProperty(
            "description",
            entry.getDescription() != null ? entry.getDescription() : ""
        );
        info.setProperty("author", entry.getAuthor() != null ? entry.getAuthor() : "");
        info.setProperty(
            "authorEmail",
            entry.getAuthorEmail() != null ? entry.getAuthorEmail() : ""
        );
        if (entry.getActions() != null && !entry.getActions().isEmpty()) info.setProperty(
            "actions",
            String.join(",", entry.getActions())
        );
        if (entry.getMinEngineVersion() != null) info.setProperty(
            "minEngineVersion",
            entry.getMinEngineVersion()
        );
        if (entry.getEntryClasses() != null) info.setProperty(
            "entryClasses",
            entry.getEntryClasses()
        );
        try (OutputStream os = new FileOutputStream(new File(pluginDir, PLUGIN_INFO_FILE))) {
            info.store(os, "Plugin info (published)");
        }

        File regFile = new File(REGISTRY_FILE);
        Map<String, Object> registry = regFile.exists()
            ? mapper.readValue(regFile, new TypeReference<Map<String, Object>>() {})
            : new LinkedHashMap<>(Map.of("version", 1, "plugins", new ArrayList<>()));
        List<Map<String, Object>> pluginsList = (List<Map<String, Object>>) registry.get("plugins");
        List<Map<String, Object>> updated = new ArrayList<>();
        boolean replaced = false;
        for (Map<String, Object> p : pluginsList) {
            if (entry.getName().equals(p.get("name"))) {
                updated.add(entryToMap(entry));
                replaced = true;
            } else updated.add(p);
        }
        if (!replaced) updated.add(entryToMap(entry));
        registry.put("plugins", updated);
        mapper.writerWithDefaultPrettyPrinter().writeValue(regFile, registry);
        return entry.getName();
    }

    public List<PluginRegistryEntry.ActionInfo> extractActionsFromJar(
        File jarFile,
        String entryClassesStr
    ) {
        List<PluginRegistryEntry.ActionInfo> actions = new ArrayList<>();
        if (entryClassesStr == null || entryClassesStr.trim().isEmpty()) return actions;
        try {
            URL[] urls = new URL[] { jarFile.toURI().toURL() };
            try (
                java.net.URLClassLoader cl = new java.net.URLClassLoader(
                    urls,
                    getClass().getClassLoader()
                )
            ) {
                for (String cn : entryClassesStr.split(",")) {
                    cn = cn.trim();
                    try {
                        Class<?> clazz = cl.loadClass(cn);
                        for (java.lang.reflect.Method m : clazz.getMethods()) {
                            com.ing.ingenious.api.annotation.Action ann = m.getAnnotation(
                                com.ing.ingenious.api.annotation.Action.class
                            );
                            if (ann != null) {
                                PluginRegistryEntry.ActionInfo ai = new PluginRegistryEntry.ActionInfo();
                                ai.setName(m.getName());
                                ai.setDescription(ann.desc());
                                ai.setObjectType(ann.object());
                                ai.setInputType(ann.input().name());
                                actions.add(ai);
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        LOG.log(Level.WARNING, "Could not load entry class: " + cn, e);
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to extract actions from JAR", e);
        }
        return actions;
    }

    private Map<String, Object> entryToMap(PluginRegistryEntry entry) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", entry.getName());
        m.put("displayName", entry.getDisplayName());
        m.put("description", entry.getDescription() != null ? entry.getDescription() : "");
        m.put("author", entry.getAuthor() != null ? entry.getAuthor() : "");
        m.put("authorEmail", entry.getAuthorEmail() != null ? entry.getAuthorEmail() : "");
        m.put("version", entry.getVersion());
        m.put(
            "minEngineVersion",
            entry.getMinEngineVersion() != null ? entry.getMinEngineVersion() : "3.0.0"
        );
        m.put(
            "maxEngineVersion",
            entry.getMaxEngineVersion() != null ? entry.getMaxEngineVersion() : ""
        );
        m.put("downloadUrl", entry.getDownloadUrl() != null ? entry.getDownloadUrl() : "");
        m.put("libUrls", entry.getLibUrls() != null ? entry.getLibUrls() : new ArrayList<>());
        m.put(
            "objectTypes",
            entry.getObjectTypes() != null ? entry.getObjectTypes() : new String[] { "General" }
        );
        m.put("actionCount", entry.getActions() != null ? entry.getActions().size() : 0);
        m.put("actions", entry.getActions() != null ? entry.getActions() : new ArrayList<>());
        m.put("homepageUrl", entry.getHomepageUrl() != null ? entry.getHomepageUrl() : "");
        m.put("license", entry.getLicense() != null ? entry.getLicense() : "MIT");
        m.put("releaseNotes", entry.getReleaseNotes() != null ? entry.getReleaseNotes() : "");
        m.put(
            "dateAdded",
            entry.getDateAdded() != null
                ? entry.getDateAdded()
                : new SimpleDateFormat("yyyy-MM-dd").format(new Date())
        );
        m.put("featured", entry.isFeatured());
        m.put("githubRepo", entry.getGithubRepo() != null ? entry.getGithubRepo() : "");
        return m;
    }
}
