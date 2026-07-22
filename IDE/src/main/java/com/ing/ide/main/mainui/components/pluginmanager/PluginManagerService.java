package com.ing.ide.main.mainui.components.pluginmanager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.ide.settings.AppSettings;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
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
    private static final String PLUGINS_DIR = "plugins";
    private static final String REGISTRY_FILE = "plugins/registry.json";
    private static final String PLUGIN_INFO_FILE = ".plugininfo";

    // GitHub Contents API URL for the private plugins registry repo.
    // Falls back to env var INGENIOUS_PLUGIN_REGISTRY_URL, then app.settings, then this default.
    private static final String DEFAULT_REGISTRY_URL =
        "https://api.github.com/repos/Jul611/ingenious-plugins-trial/contents/registry.json";

    private final ObjectMapper mapper = new ObjectMapper();

    // ─── Token loading ────────────────────────────────────────────────

    /**
     * Returns the GitHub PAT for authenticating to the private plugin registry.
     * Reads directly from UserConfig every time (no stale cache).
     */
    public static String getRegistryToken() {
        try {
            String token = UserConfig.getPublishPat();
            if (token != null && !token.isEmpty()) {
                return token;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not read publishPat from UserConfig", e);
        }
        return null;
    }

    /**
     * Returns the effective remote registry URL.
     * Checks env var FIRST, then the hardcoded default.
     */
    private static String getRemoteRegistryUrl() {
        String envUrl = System.getenv("INGENIOUS_PLUGIN_REGISTRY_URL");
        if (envUrl != null && !envUrl.isEmpty()) {
            return envUrl;
        }
        return DEFAULT_REGISTRY_URL;
    }

    /**
     * Adds the Authorization header and User-Agent to a connection
     * if a token is available.
     */
    private static void addAuthHeader(HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", "INGenious-PluginManager/1.0");
        String token = getRegistryToken();
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "token " + token);
        }
    }

    // ─── Registry fetching ────────────────────────────────────────────

    public List<PluginRegistryEntry> fetchRegistry() {
        List<PluginRegistryEntry> remote = tryFetchRemote();
        if (remote != null && !remote.isEmpty()) {
            LOG.info("Loaded registry from GitHub");
            return remote;
        }
        LOG.warning("Remote registry fetch failed — no PAT configured or network error");
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
            URL url = new URL(getRemoteRegistryUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            addAuthHeader(conn);
            if (conn.getResponseCode() == 200) {
                // GitHub Contents API returns JSON with base64-encoded content
                Map<String, Object> apiResponse = mapper.readValue(
                    conn.getInputStream(),
                    new TypeReference<Map<String, Object>>() {}
                );
                String contentJson;
                Object contentField = apiResponse.get("content");
                Object encodingField = apiResponse.get("encoding");
                if ("base64".equals(encodingField) && contentField instanceof String) {
                    // Decode base64 content from GitHub Contents API
                    String b64 = ((String) contentField).replaceAll("\\s", "");
                    contentJson = new String(Base64.getDecoder().decode(b64), "UTF-8");
                } else {
                    // Raw response (e.g. from raw.githubusercontent.com or custom CDN)
                    contentJson = mapper.writeValueAsString(apiResponse);
                }

                Map<String, Object> root = mapper.readValue(
                    contentJson,
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
            LOG.log(Level.WARNING, "Remote registry fetch failed: {0}", e.getMessage());
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

    // ─── Plugin download & install ────────────────────────────────────

    public boolean downloadPlugin(PluginRegistryEntry entry) throws IOException {
        File pluginDir = new File(PLUGINS_DIR, entry.getName());
        if (!pluginDir.exists()) pluginDir.mkdirs();
        String urlStr = entry.getDownloadUrl();
        // Extract the jar filename from the original downloadUrl BEFORE
        // resolving to the API endpoint (which uses numeric asset IDs).
        String jarName = entry
            .getDownloadUrl()
            .substring(entry.getDownloadUrl().lastIndexOf('/') + 1);
        // Resolve GitHub release URLs to their raw asset download endpoint.
        // The github.com/releases/download/... URL pattern does not work
        // with Authorization headers on private repos (returns 404).
        // We must use the API: /repos/{owner}/{repo}/releases/assets/{id}
        if (urlStr.contains("github.com") && urlStr.contains("/releases/download/")) {
            urlStr = resolveReleaseAssetUrl(urlStr);
        }
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

    /**
     * Converts a github.com/releases/download/... URL to the GitHub API
     * asset download URL. On private repos, the release page URL returns 404
     * when used with an Authorization header. The API endpoint works instead.
     */
    private String resolveReleaseAssetUrl(String releaseUrl) throws IOException {
        // URL: https://github.com/OWNER/REPO/releases/download/TAG/FILE.jar
        // We need to find the asset ID via GET /repos/.../releases/tags/TAG
        // then use GET /repos/.../releases/assets/{id} with Accept: application/octet-stream
        String[] parts = releaseUrl.split("/releases/download/", 2);
        if (parts.length != 2) {
            throw new IOException("Unable to parse release URL: " + releaseUrl);
        }
        String ownerRepoPart = parts[0];
        // ownerRepoPart: https://github.com/Jul611/ingenious-plugins-trial
        String tagAndFile = parts[1]; // demo-helloworld-v1.0.0/demo-helloworld-1.0.0.jar
        int slashIdx = tagAndFile.indexOf('/');
        if (slashIdx < 0) {
            throw new IOException("Unable to parse release tag from URL: " + releaseUrl);
        }
        String tag = tagAndFile.substring(0, slashIdx);

        // Extract owner and repo from the prefix URL
        String prefixPath = ownerRepoPart.substring("https://github.com/".length());
        // e.g. "Jul611/ingenious-plugins-trial"
        String[] ownerRepo = prefixPath.split("/", 2);
        if (ownerRepo.length != 2) {
            throw new IOException("Unable to parse owner/repo from URL: " + releaseUrl);
        }
        String owner = ownerRepo[0];
        String repo = ownerRepo[1];

        // Step 1: Get release by tag
        String apiUrl = GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/releases/tags/" + tag;
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        addAuthHeader(conn);

        if (conn.getResponseCode() != 200) {
            String err = "";
            try (InputStream es = conn.getErrorStream()) {
                if (es != null) err = new String(es.readAllBytes(), "UTF-8");
            } catch (Exception ignored) {}
            throw new IOException(
                "Failed to resolve release tag " +
                tag +
                ": HTTP " +
                conn.getResponseCode() +
                " " +
                err
            );
        }

        Map<String, Object> release = mapper.readValue(
            conn.getInputStream(),
            new TypeReference<Map<String, Object>>() {}
        );
        Object assetsObj = release.get("assets");
        if (!(assetsObj instanceof List)) {
            throw new IOException("No assets found in release " + tag);
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assets = (List<Map<String, Object>>) assetsObj;
        if (assets.isEmpty()) {
            throw new IOException("Release " + tag + " has no assets");
        }

        // Use the first asset (there should be exactly one per release)
        int assetId = ((Number) assets.get(0).get("id")).intValue();

        // Return the API download URL
        // This URL requires the Accept: application/octet-stream header
        return GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/releases/assets/" + assetId;
    }

    /**
     * Downloads a file from the given URL to the target file.
     * Throws IOException if the download fails — no placeholders.
     */
    private void downloadFile(String urlStr, File target) throws IOException {
        if (urlStr.startsWith("file://")) {
            Files.copy(
                Paths.get(urlStr.substring(7)),
                target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            return;
        }

        String currentUrl = urlStr;
        boolean authRequired = true;
        int maxRedirects = 5;

        for (int i = 0; i < maxRedirects; i++) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(false);
            // Only send auth headers to GitHub — S3 redirect URLs are pre-signed
            // and reject the Authorization header.
            if (authRequired && currentUrl.contains("github")) {
                addAuthHeader(conn);
                // GitHub API asset endpoint requires this to return binary content
                if (currentUrl.contains("/releases/assets/")) {
                    conn.setRequestProperty("Accept", "application/octet-stream");
                }
            } else {
                conn.setRequestProperty("User-Agent", "INGenious-PluginManager/1.0");
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                try (
                    InputStream is = conn.getInputStream();
                    ReadableByteChannel rbc = Channels.newChannel(is);
                    FileOutputStream fos = new FileOutputStream(target)
                ) {
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }
                return;
            }

            // Follow redirect — GitHub release URLs redirect to a pre-signed S3 URL
            // that does not need (and may reject) the Authorization header.
            if (responseCode == 301 || responseCode == 302 || responseCode == 307) {
                String location = conn.getHeaderField("Location");
                if (location == null) {
                    throw new IOException("Redirect with no Location header for " + currentUrl);
                }
                currentUrl = location;
                authRequired = false;
                continue;
            }

            // Reached here = not a redirect, not 200 = real error
            String errorBody = "";
            try (InputStream es = conn.getErrorStream()) {
                if (es != null) errorBody = new String(es.readAllBytes(), "UTF-8");
            } catch (Exception ignored) {}
            throw new IOException(
                "Download failed: HTTP " +
                responseCode +
                " fetching " +
                currentUrl +
                (errorBody.isEmpty() ? "" : " \u2014 " + errorBody)
            );
        }

        throw new IOException("Too many redirects downloading " + urlStr);
    }

    // ─── Installed plugin management ──────────────────────────────────

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

    private static final String GITHUB_API_BASE = "https://api.github.com";

    // ─── Local Plugin Publishing (staging for local testing) ─────────

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

    // ─── Submission Export (for PR-based publishing) ──────────────────

    private static final String PLUGINS_REPO = "Jul611/ingenious-plugins-trial";

    /**
     * Exports a plugin submission to the given directory, producing the
     * structure expected by the plugins repo for a PR submission:
     * <pre>
     *   plugins/{pluginName}/
     *     \u2514\u2500\u2500 {jarFile}.jar
     *   registry.json (updated with new entry, downloadUrl placeholder)
     * </pre>
     */
    public File prepareSubmission(
        File jarFile,
        PluginRegistryEntry entry,
        String readmeContent,
        File outputDir
    )
        throws IOException {
        if (!outputDir.exists()) outputDir.mkdirs();

        // Copy JAR
        File pluginsDir = new File(outputDir, "plugins/" + entry.getName());
        pluginsDir.mkdirs();
        Files.copy(
            jarFile.toPath(),
            new File(pluginsDir, jarFile.getName()).toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );

        // Write README.md
        Files.write(new File(pluginsDir, "README.md").toPath(), readmeContent.getBytes("UTF-8"));

        // Write updated registry.json with placeholder downloadUrl
        entry.setDownloadUrl(""); // CI fills this in after release creation
        File regFile = new File(REGISTRY_FILE);
        Map<String, Object> registry;
        if (regFile.exists()) {
            registry = mapper.readValue(regFile, new TypeReference<Map<String, Object>>() {});
        } else {
            registry = new LinkedHashMap<>(Map.of("version", 1, "plugins", new ArrayList<>()));
        }
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
        File outRegFile = new File(outputDir, "registry.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(outRegFile, registry);

        return outputDir;
    }

    /**
     * Returns the PR creation URL for manual submission.
     * Author clones the repo, copies the exported files in, commits, and opens a PR.
     */
    public static String getPluginsRepoUrl() {
        return "https://github.com/" + PLUGINS_REPO;
    }

    public static String getPluginsRepoPrUrl() {
        return getPluginsRepoUrl() + "/compare/main...?expand=1";
    }

    // ─── Direct GitHub publish ────────────────────────────────────────

    /**
     * Publishes a plugin directly to the plugins repo via the GitHub API.
     * Creates a GitHub Release, uploads the JAR as an asset, and updates
     * registry.json with the download URL.
     * Uses the user's personal PAT from UserConfig (write access required).
     *
     * @param jarFile the built plugin JAR
     * @param entry   the registry entry with metadata
     * @param userPat the user's GitHub PAT with repo write access
     * @return the download URL of the published release JAR
     */
    public String publishDirectly(File jarFile, PluginRegistryEntry entry, String userPat)
        throws IOException {
        String owner = "Jul611";
        String repo = "ingenious-plugins-trial";
        String tagName = entry.getName() + "-v" + entry.getVersion();
        String jarFileName = jarFile.getName();

        // 1. Create release
        Map<String, Object> releaseBody = new LinkedHashMap<>();
        releaseBody.put("tag_name", tagName);
        releaseBody.put("name", entry.getDisplayName() + " v" + entry.getVersion());
        releaseBody.put("body", entry.getReleaseNotes() != null ? entry.getReleaseNotes() : "");
        releaseBody.put("draft", false);
        releaseBody.put("prerelease", false);

        String releaseUrl = GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/releases";
        Map<String, Object> release = apiPost(releaseUrl, releaseBody, userPat);
        Object uploadUrlObj = release.get("upload_url");
        String uploadUrlTemplate = (String) uploadUrlObj;
        String uploadUrl = uploadUrlTemplate.replace("{?name,label}", "?name=" + jarFileName);

        // 2. Upload JAR as release asset
        URL uploadEndpoint = new URL(uploadUrl);
        HttpURLConnection uploadConn = (HttpURLConnection) uploadEndpoint.openConnection();
        uploadConn.setRequestMethod("POST");
        uploadConn.setRequestProperty("Authorization", "token " + userPat);
        uploadConn.setRequestProperty("Content-Type", "application/java-archive");
        uploadConn.setRequestProperty("User-Agent", "INGenious-PluginManager/1.0");
        uploadConn.setDoOutput(true);
        uploadConn.setConnectTimeout(30000);
        uploadConn.setReadTimeout(30000);
        try (
            FileInputStream fis = new FileInputStream(jarFile);
            OutputStream os = uploadConn.getOutputStream()
        ) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) >= 0) {
                os.write(buffer, 0, read);
            }
        }
        int uploadCode = uploadConn.getResponseCode();
        if (uploadCode != 201) {
            String err = "";
            try (InputStream es = uploadConn.getErrorStream()) {
                if (es != null) err = new String(es.readAllBytes(), "UTF-8");
            } catch (Exception ignored) {}
            throw new IOException("Upload failed: HTTP " + uploadCode + " " + err);
        }
        String downloadUrl =
            "https://github.com/" +
            owner +
            "/" +
            repo +
            "/releases/download/" +
            tagName +
            "/" +
            jarFileName;
        LOG.info("Release created, JAR uploaded: " + downloadUrl);

        // 3. Update registry.json via Contents API
        entry.setDownloadUrl(downloadUrl);
        String regContentsUrl =
            GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/contents/registry.json";

        HttpURLConnection getConn = (HttpURLConnection) new URL(regContentsUrl).openConnection();
        getConn.setRequestProperty("Authorization", "token " + userPat);
        getConn.setRequestProperty("User-Agent", "INGenious-PluginManager/1.0");
        String existingSha = null;
        String existingContent = null;
        if (getConn.getResponseCode() == 200) {
            Map<String, Object> existing = mapper.readValue(
                getConn.getInputStream(),
                new TypeReference<Map<String, Object>>() {}
            );
            existingSha = (String) existing.get("sha");
            String b64 = ((String) existing.get("content")).replaceAll("\\s", "");
            existingContent = new String(Base64.getDecoder().decode(b64), "UTF-8");
        }

        Map<String, Object> registry;
        if (existingContent != null) {
            registry =
                mapper.readValue(existingContent, new TypeReference<Map<String, Object>>() {});
        } else {
            registry = new LinkedHashMap<>(Map.of("version", 1, "plugins", new ArrayList<>()));
        }
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
        String newContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(registry);
        String newContentB64 = Base64.getEncoder().encodeToString(newContent.getBytes("UTF-8"));

        Map<String, Object> putBody = new LinkedHashMap<>();
        putBody.put("message", "Add " + entry.getName() + " v" + entry.getVersion());
        putBody.put("content", newContentB64);
        if (existingSha != null) putBody.put("sha", existingSha);
        putBody.put("branch", "main");

        apiPut(regContentsUrl, putBody, userPat);
        LOG.info("registry.json updated on GitHub");

        return downloadUrl;
    }

    /**
     * Simple HTTP POST returning parsed JSON.
     */
    private Map<String, Object> apiPost(String urlStr, Map<String, Object> body, String token)
        throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "INGenious-PluginManager/1.0");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        try (OutputStream os = conn.getOutputStream()) {
            mapper.writeValue(os, body);
        }
        int code = conn.getResponseCode();
        if (code == 201 || code == 200) {
            return mapper.readValue(
                conn.getInputStream(),
                new TypeReference<Map<String, Object>>() {}
            );
        }
        String err = "";
        try (InputStream es = conn.getErrorStream()) {
            if (es != null) err = new String(es.readAllBytes(), "UTF-8");
        } catch (Exception ignored) {}
        throw new IOException("API POST " + code + " for " + urlStr + " \u2014 " + err);
    }

    /**
     * Simple HTTP PUT returning parsed JSON.
     */
    private Map<String, Object> apiPut(String urlStr, Map<String, Object> body, String token)
        throws IOException {
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
        int code = conn.getResponseCode();
        if (code == 201 || code == 200) {
            return mapper.readValue(
                conn.getInputStream(),
                new TypeReference<Map<String, Object>>() {}
            );
        }
        String err = "";
        try (InputStream es = conn.getErrorStream()) {
            if (es != null) err = new String(es.readAllBytes(), "UTF-8");
        } catch (Exception ignored) {}
        throw new IOException("API PUT " + code + " for " + urlStr + " \u2014 " + err);
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
