package com.ing.ide.main.mainui.components.pluginmanager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core service for plugin registry fetching, installation, and management.
 * Registry reads and pull-request submission go through
 * {@link MarketplaceCliService} (git/gh/mvn), never a stored token —
 * this class owns file-tree assembly and local bookkeeping only.
 */
public class PluginManagerService {
    private static final Logger LOG = Logger.getLogger(PluginManagerService.class.getName());
    private static final String PLUGINS_DIR = "plugins";
    private static final String PLUGIN_INFO_FILE = ".plugininfo";

    private final ObjectMapper mapper = new ObjectMapper();
    private final MarketplaceCliService cliService = new MarketplaceCliService();

    // ─── Registry fetching ────────────────────────────────────────────

    public List<PluginRegistryEntry> fetchRegistry() {
        List<PluginRegistryEntry> remote = tryFetchRemote();
        if (remote != null && !remote.isEmpty()) {
            LOG.info("Loaded registry from GitHub");
            return remote;
        }
        LOG.warning(
            "Remote registry fetch failed — check gh auth status and the configured registry repo"
        );
        return Collections.emptyList();
    }

    /**
     * Best-effort client-side warning: throws if {@code newVersion} isn't
     * strictly newer than whatever's already published for a plugin named
     * {@code name}. This is advisory only, not the real gate — it fails
     * open (returns quietly) if the registry can't be fetched, and a
     * submission can always reach the registry repo by some route other
     * than this dialog. publish-pipeline.yml enforces the same rule
     * server-side; that's the check that actually can't be skipped.
     */
    public void checkVersionIsNewer(String name, String newVersion) throws IOException {
        PluginRegistryEntry existing = fetchRegistry()
            .stream()
            .filter(p -> name.equals(p.getName()))
            .findFirst()
            .orElse(null);
        if (existing == null) return;
        if (compareVersions(newVersion, existing.getVersion()) <= 0) {
            throw new IOException(
                "Version " +
                newVersion +
                " is not newer than the currently published " +
                existing.getVersion() +
                " for \"" +
                name +
                "\" — bump the version in your pom.xml."
            );
        }
    }

    /** Same numeric dotted-version comparison approach as MarketplaceBrowseUI's engine-version check. */
    private int compareVersions(String a, String b) {
        return Integer.compare(parseVersion(a), parseVersion(b));
    }

    private int parseVersion(String version) {
        if (version == null || version.isEmpty()) return 0;
        String[] parts = version.split("\\.");
        int result = 0;
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try {
                result = result * 1000 + Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return result;
    }

    /**
     * Loads a plugin's README from the registry repo, or {@code null} if it
     * doesn't have one. Best-effort -- returns null on failure too, since this
     * is for optional detail display, not something worth interrupting the
     * user over.
     */
    public String fetchReadme(PluginRegistryEntry entry) {
        try {
            return cliService.fetchPluginReadmeRaw(
                entry.getName(),
                entry.getReadmeFileName(),
                s -> {}
            );
        } catch (Exception e) {
            LOG.log(
                Level.FINE,
                "README fetch failed for {0}: {1}",
                new Object[] { entry.getName(), e.getMessage() }
            );
            return null;
        }
    }

    private List<PluginRegistryEntry> tryFetchRemote() {
        try {
            String contentJson = cliService.fetchRegistryJsonRaw(s -> {});
            Map<String, Object> root = mapper.readValue(
                contentJson,
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
            LOG.log(Level.WARNING, "Remote registry fetch failed: {0}", e.getMessage());
        }
        return null;
    }

    // ─── Plugin download & install ────────────────────────────────────

    /**
     * Resolves the plugin's jar and transitive runtime dependencies from the
     * configured Azure Artifacts feed via Maven and writes a local
     * {@code .plugininfo} for the Installed tab.
     */
    public boolean downloadPlugin(PluginRegistryEntry entry, Consumer<String> progress)
        throws IOException {
        File pluginDir = new File(PLUGINS_DIR, entry.getName());
        if (!pluginDir.exists()) pluginDir.mkdirs();

        String groupId = entry.getMavenGroupId();
        String artifactId = entry.getMavenArtifactId();
        if (groupId == null || groupId.isEmpty() || artifactId == null || artifactId.isEmpty()) {
            throw new IOException(
                "\"" +
                entry.getDisplayName() +
                "\" doesn't have Maven coordinates yet — it may still be waiting on its publish " +
                "pipeline to finish building. Try refreshing the registry in a bit."
            );
        }

        File jar = cliService.resolvePluginArtifact(
            groupId,
            artifactId,
            entry.getVersion(),
            pluginDir,
            progress
        );
        LOG.info("Installed " + jar.getName());
        cliService.resolvePluginDependencies(
            groupId,
            artifactId,
            entry.getVersion(),
            new File(pluginDir, "lib"),
            progress
        );

        Properties info = new Properties();
        info.setProperty("name", entry.getName());
        info.setProperty("displayName", entry.getDisplayName());
        info.setProperty("version", entry.getVersion());
        info.setProperty(
            "description",
            entry.getDescription() != null ? entry.getDescription() : ""
        );
        info.setProperty("author", entry.getAuthor() != null ? entry.getAuthor() : "");
        if (entry.getActions() != null) info.setProperty(
            "actions",
            String.join(",", entry.getActions())
        );
        try (OutputStream os = new FileOutputStream(new File(pluginDir, PLUGIN_INFO_FILE))) {
            info.store(os, "Plugin info");
        }
        return true;
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
        return entry.getName();
    }

    // ─── Submission staging (for PR-based publishing) ──────────────────

    /**
     * Copies the contributor's plugin source project into a fresh staging
     * directory alongside a {@code .submission.json} sidecar — everything
     * {@link #entryToMap} already knows, minus what CI derives itself (name
     * from the directory it lands in, version from the built POM, Maven
     * coordinates/dateAdded/featured from the merge pipeline). The
     * description (*.md) file is whatever {@code sourceProjectDir} already
     * has, copied as-is under its original name -- nothing renames it to
     * README.md, so a contributor's own filename choice survives all the
     * way into the registry repo. {@link MarketplaceCliService#submitPlugin}
     * copies this directory's contents into a PR branch; the caller is
     * responsible for deleting it once the submission completes.
     */
    public File stagePluginSubmission(File sourceProjectDir, PluginRegistryEntry entry)
        throws IOException {
        File stagingDir = Files.createTempDirectory("ingenious-plugin-stage-").toFile();
        copyDirectory(
            sourceProjectDir,
            stagingDir,
            Set.of("target", ".git", ".idea", "node_modules")
        );
        Map<String, Object> submission = entryToSubmissionMap(entry);
        mapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(new File(stagingDir, ".submission.json"), submission);
        return stagingDir;
    }

    private Map<String, Object> entryToSubmissionMap(PluginRegistryEntry entry) {
        Map<String, Object> m = entryToMap(entry);
        m.remove("name");
        m.remove("downloadUrl");
        m.remove("dateAdded");
        m.remove("featured");
        return m;
    }

    private void copyDirectory(File srcDir, File destDir, Set<String> excludeDirNames)
        throws IOException {
        java.nio.file.Path srcPath = srcDir.toPath();
        java.nio.file.Path destPath = destDir.toPath();
        try (java.util.stream.Stream<java.nio.file.Path> stream = Files.walk(srcPath)) {
            List<java.nio.file.Path> paths = stream.collect(java.util.stream.Collectors.toList());
            for (java.nio.file.Path path : paths) {
                java.nio.file.Path relative = srcPath.relativize(path);
                boolean excluded = false;
                for (java.nio.file.Path part : relative) {
                    if (excludeDirNames.contains(part.toString())) {
                        excluded = true;
                        break;
                    }
                }
                if (excluded) continue;
                java.nio.file.Path target = destPath.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
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
