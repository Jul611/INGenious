package com.ing.ide.main.mainui.components.pluginmanager;

import com.ing.ide.util.SystemInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shells out to {@code git}, {@code gh}, and {@code mvn} to publish plugin
 * submissions as GitHub pull requests and to resolve published plugin
 * artifacts from the Azure Artifacts feed. GitHub credentials are never
 * read, stored, or handled here — git/gh manage their own. Maven-against-ADO
 * uses a PAT the user pastes once into Registry Settings; this class writes
 * it straight into the user's real {@code ~/.m2/settings.xml} (preserving
 * anything else already there) so nobody has to hand-edit that file.
 */
public class PluginRegistryCliService {
    private static final long TOOL_CHECK_TIMEOUT_MS = 10_000;
    private static final long DEFAULT_TIMEOUT_MS = 30_000;
    private static final long CLONE_TIMEOUT_MS = 60_000;
    private static final long MVN_TIMEOUT_MS = 300_000;

    /**
     * Hands gh's own git credential helper to a single process via git's
     * GIT_CONFIG_COUNT env-var config override — scoped to just that one
     * invocation, never written to any config file. The empty VALUE_0
     * resets any credential.helper already configured for this URL first,
     * since git normally chains multiple helper entries rather than
     * replacing them (same two-step pattern `gh auth setup-git` itself
     * writes into ~/.gitconfig, just not persisted here).
     * <p>
     * Applied to every network git operation in the submission flow
     * (clone, push, and gh pr create's own internal git use) so the whole
     * flow depends on exactly one already-verified credential — gh's own,
     * confirmed via {@code gh auth status} before anything else runs —
     * instead of also depending on git's separate credential helper (e.g.
     * Git Credential Manager) being configured and caching correctly on
     * the user's machine, which isn't something this app can guarantee.
     */
    private static final Map<String, String> GH_OWN_GIT_CREDENTIAL_ENV = Map.of(
        "GIT_CONFIG_COUNT",
        "2",
        "GIT_CONFIG_KEY_0",
        "credential.https://github.com.helper",
        "GIT_CONFIG_VALUE_0",
        "",
        "GIT_CONFIG_KEY_1",
        "credential.https://github.com.helper",
        "GIT_CONFIG_VALUE_1",
        "!gh auth git-credential"
    );

    // ─── Result / status types ──────────────────────────────────────

    public static final class ToolStatus {
        public boolean installed;
        public String version;
        public String detail;
    }

    public static final class AuthStatus {
        public boolean authenticated;
        public String username;
        public String detail;
    }

    public static final class PrResult {
        public String url;
        public int number;
    }

    private static final class WorkRepo {
        final String owner;
        final String repo;
        final boolean isFork;

        WorkRepo(String owner, String repo, boolean isFork) {
            this.owner = owner;
            this.repo = repo;
            this.isFork = isFork;
        }
    }

    private static final class ProcResult {
        int exitCode;
        String output;
    }

    /** Thrown when a shelled-out command fails; carries the exit code and captured output. */
    public static class CliException extends IOException {
        private final int exitCode;
        private final String capturedOutput;

        public CliException(String message, int exitCode, String capturedOutput) {
            super(message);
            this.exitCode = exitCode;
            this.capturedOutput = capturedOutput == null ? "" : capturedOutput;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getCapturedOutput() {
            return capturedOutput;
        }

        @Override
        public String getMessage() {
            String base = super.getMessage();
            if (capturedOutput.isEmpty()) {
                return base;
            }
            String trimmed = capturedOutput.trim();
            int max = 800;
            if (trimmed.length() > max) {
                trimmed = trimmed.substring(0, max) + "... (truncated)";
            }
            return base + "\n\n" + trimmed;
        }
    }

    // ─── Tool detection & auth ───────────────────────────────────────

    public ToolStatus checkGitInstalled() {
        return detectTool(List.of("git", "--version"));
    }

    public ToolStatus checkGhInstalled() {
        return detectTool(List.of("gh", "--version"));
    }

    public ToolStatus checkMvnInstalled() {
        return detectTool(List.of("mvn", "--version"));
    }

    private ToolStatus detectTool(List<String> versionCommand) {
        ToolStatus status = new ToolStatus();
        try {
            ProcResult result = run(versionCommand, null, TOOL_CHECK_TIMEOUT_MS);
            status.installed = result.exitCode == 0;
            status.version = firstLine(result.output);
            status.detail = status.installed ? "" : "Exit code " + result.exitCode;
        } catch (IOException e) {
            status.installed = false;
            status.detail = e.getMessage();
        }
        return status;
    }

    public AuthStatus checkGhAuthStatus() {
        AuthStatus status = new AuthStatus();
        try {
            ProcResult result = run(List.of("gh", "auth", "status"), null, TOOL_CHECK_TIMEOUT_MS);
            status.authenticated = result.exitCode == 0;
            if (status.authenticated) {
                try {
                    status.username = resolveAuthenticatedUser();
                } catch (CliException e) {
                    status.username = null;
                }
            } else {
                status.detail = "Run 'gh auth login --web' to sign in.";
            }
        } catch (IOException e) {
            status.authenticated = false;
            status.detail = e.getMessage();
        }
        return status;
    }

    /**
     * Checks whether {@code ~/.m2/settings.xml} has a {@code <server>} entry
     * matching the given id. A simple text heuristic rather than an XML
     * parse — good enough to tell the user "you haven't set this up yet."
     */
    public boolean isAdoMavenServerConfigured(String serverId) {
        if (serverId == null || serverId.isEmpty()) return false;
        File settingsFile = adoSettingsFile();
        if (!settingsFile.exists()) return false;
        try {
            String content = new String(
                Files.readAllBytes(settingsFile.toPath()),
                StandardCharsets.UTF_8
            );
            return content.contains("<id>" + serverId + "</id>");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Writes (or replaces) a <server> entry for the ADO feed in the user's
     * real ~/.m2/settings.xml, preserving anything else already in that
     * file -- this is what lets a contributor just paste a PAT into
     * Registry Settings instead of hand-editing XML themselves.
     */
    public void saveAdoCredential(String serverId, String pat) throws IOException {
        File settingsFile = adoSettingsFile();
        settingsFile.getParentFile().mkdirs();
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            org.w3c.dom.Document doc;
            if (settingsFile.exists()) {
                doc = dbf.newDocumentBuilder().parse(settingsFile);
            } else {
                doc = dbf.newDocumentBuilder().newDocument();
                doc.appendChild(doc.createElement("settings"));
            }
            org.w3c.dom.Element root = doc.getDocumentElement();
            org.w3c.dom.NodeList serversList = root.getElementsByTagName("servers");
            org.w3c.dom.Element serversEl;
            if (serversList.getLength() == 0) {
                serversEl = doc.createElement("servers");
                root.appendChild(serversEl);
            } else {
                serversEl = (org.w3c.dom.Element) serversList.item(0);
            }
            org.w3c.dom.NodeList serverNodes = serversEl.getElementsByTagName("server");
            for (int i = serverNodes.getLength() - 1; i >= 0; i--) {
                org.w3c.dom.Element serverEl = (org.w3c.dom.Element) serverNodes.item(i);
                org.w3c.dom.NodeList idNodes = serverEl.getElementsByTagName("id");
                if (idNodes.getLength() > 0 && serverId.equals(idNodes.item(0).getTextContent())) {
                    serversEl.removeChild(serverEl);
                }
            }
            org.w3c.dom.Element newServer = doc.createElement("server");
            org.w3c.dom.Element idEl = doc.createElement("id");
            idEl.setTextContent(serverId);
            org.w3c.dom.Element userEl = doc.createElement("username");
            userEl.setTextContent("ado");
            org.w3c.dom.Element passEl = doc.createElement("password");
            passEl.setTextContent(pat);
            newServer.appendChild(idEl);
            newServer.appendChild(userEl);
            newServer.appendChild(passEl);
            serversEl.appendChild(newServer);

            javax.xml.transform.Transformer transformer = javax
                .xml.transform.TransformerFactory.newInstance()
                .newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(
                new javax.xml.transform.dom.DOMSource(doc),
                new javax.xml.transform.stream.StreamResult(settingsFile)
            );
        } catch (Exception e) {
            throw new IOException(
                "Could not update " + settingsFile.getAbsolutePath() + ": " + e.getMessage(),
                e
            );
        }
    }

    private static File adoSettingsFile() {
        return new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
    }

    private String resolveAuthenticatedUser() throws IOException {
        ProcResult result = run(
            List.of("gh", "api", "user", "-q", ".login"),
            null,
            DEFAULT_TIMEOUT_MS
        );
        if (result.exitCode != 0) {
            throw new CliException(
                "Not signed in to GitHub CLI. Run 'gh auth login --web'.",
                result.exitCode,
                result.output
            );
        }
        return result.output.trim();
    }

    // ─── Fork-or-branch resolution ───────────────────────────────────

    /**
     * Decides whether to branch directly on the configured registry repo or
     * fork it first, based on whether the authenticated gh user actually has
     * push access — not on whether they happen to own the repo. This covers
     * both a personal-account owner and a company teammate with shared write
     * access to an org repo, with the same code path.
     */
    private WorkRepo resolveWorkRepo(String owner, String repo, Consumer<String> progress)
        throws IOException {
        progress.accept("Checking GitHub sign-in...");
        String authUser = resolveAuthenticatedUser();

        progress.accept("Checking access to " + owner + "/" + repo + "...");
        ProcResult perm = run(
            List.of("gh", "api", "repos/" + owner + "/" + repo, "-q", ".permissions.push"),
            null,
            DEFAULT_TIMEOUT_MS
        );
        if (perm.exitCode != 0) {
            throw new CliException(
                "Could not find or access " +
                owner +
                "/" +
                repo +
                " on GitHub as " +
                authUser +
                ". Check the registry repo configured in Registry Settings.",
                perm.exitCode,
                perm.output
            );
        }
        boolean hasWrite = "true".equals(perm.output.trim());
        if (hasWrite) {
            progress.accept(
                "You have write access to " + owner + "/" + repo + " — branching directly."
            );
            return new WorkRepo(owner, repo, false);
        }

        progress.accept("No direct write access — forking " + owner + "/" + repo + "...");
        // Idempotent: succeeds whether or not a fork already exists.
        run(
            List.of("gh", "repo", "fork", owner + "/" + repo, "--clone=false", "--remote=false"),
            null,
            DEFAULT_TIMEOUT_MS
        );
        return new WorkRepo(authUser, repo, true);
    }

    // ─── Publish (contributor submission) ────────────────────────────

    /**
     * Builds the contributor's plugin source with Maven, so the existing
     * jar-manifest metadata extraction in {@code PluginManagerPublishUI} can
     * run unchanged against a freshly-built jar instead of a hand-picked one.
     */
    public File buildLocally(File sourceProjectDir, Consumer<String> progress) throws IOException {
        File pom = new File(sourceProjectDir, "pom.xml");
        if (!pom.exists()) {
            throw new CliException(
                "No pom.xml found in " +
                sourceProjectDir.getAbsolutePath() +
                " — select the root folder of a Maven plugin project.",
                -1,
                ""
            );
        }
        progress.accept("Building " + sourceProjectDir.getName() + " (mvn clean package)...");
        ProcResult result = run(
            List.of("mvn", "-q", "-f", pom.getAbsolutePath(), "clean", "package"),
            sourceProjectDir,
            MVN_TIMEOUT_MS
        );
        if (result.exitCode != 0) {
            throw new CliException(
                "Build failed for " + sourceProjectDir.getName(),
                result.exitCode,
                result.output
            );
        }
        File targetDir = new File(sourceProjectDir, "target");
        File[] jars = targetDir.listFiles(
            (d, n) -> n.endsWith(".jar") && !n.endsWith("-sources.jar")
        );
        if (jars == null || jars.length == 0) {
            throw new CliException(
                "Build succeeded but no jar was found in " + targetDir.getAbsolutePath(),
                0,
                result.output
            );
        }
        progress.accept("Build succeeded: " + jars[0].getName());
        return jars[0];
    }

    /**
     * Forks-or-branches, copies {@code stagedContentDir}'s contents into
     * {@code plugins/{pluginName}/} on a new branch, commits, pushes, and
     * opens a pull request against the configured registry repo.
     */
    public PrResult submitPlugin(
        File stagedContentDir,
        String pluginName,
        String pluginVersion,
        String prTitle,
        String prBody,
        Consumer<String> progress
    )
        throws IOException {
        PluginRegistryConfig config = new PluginRegistryConfig();
        String registryRepo = config.getRegistryRepo();
        if (registryRepo == null) {
            throw new CliException(
                "No plugin registry repo configured. Set one in Registry Settings.",
                -1,
                ""
            );
        }
        String[] parts = registryRepo.split("/", 2);
        if (parts.length != 2) {
            throw new CliException(
                "Registry repo must be in 'owner/repo' form, got: " + registryRepo,
                -1,
                ""
            );
        }
        String owner = parts[0];
        String repo = parts[1];
        String branch = config.getRegistryBranch();

        WorkRepo work = resolveWorkRepo(owner, repo, progress);

        File cloneDir;
        try {
            cloneDir = Files.createTempDirectory("ingenious-plugin-clone-").toFile();
        } catch (IOException e) {
            throw new CliException(
                "Could not create a temp folder to work in: " + e.getMessage(),
                -1,
                ""
            );
        }

        boolean success = false;
        try {
            progress.accept("Cloning " + work.owner + "/" + work.repo + "...");
            cloneWithRetry(work, branch, cloneDir, progress);

            String branchName = "plugin/" + pluginName + "/v" + pluginVersion;
            progress.accept("Creating branch " + branchName + "...");
            runOrThrow(
                List.of("git", "checkout", "-b", branchName),
                cloneDir,
                "Failed to create branch"
            );

            File targetPluginDir = new File(cloneDir, "plugins" + File.separator + pluginName);
            progress.accept("Copying plugin files...");
            copyDirectoryContents(stagedContentDir, targetPluginDir);

            runOrThrow(
                List.of("git", "add", "plugins/" + pluginName),
                cloneDir,
                "Failed to stage files"
            );
            runOrThrow(
                List.of(
                    "git",
                    "-c",
                    "user.name=INGenious",
                    "-c",
                    "user.email=ingenious@local",
                    "commit",
                    "-m",
                    "Add " + pluginName + " v" + pluginVersion
                ),
                cloneDir,
                "Failed to commit"
            );

            progress.accept("Pushing branch...");
            runOrThrow(
                List.of("git", "push", "--force", "origin", "HEAD:" + branchName),
                cloneDir,
                "Failed to push branch",
                GH_OWN_GIT_CREDENTIAL_ENV
            );

            progress.accept("Opening pull request...");
            List<String> prCmd = new ArrayList<>();
            prCmd.add("gh");
            prCmd.add("pr");
            prCmd.add("create");
            prCmd.add("--repo");
            prCmd.add(owner + "/" + repo);
            prCmd.add("--base");
            prCmd.add(branch);
            prCmd.add("--head");
            prCmd.add(work.isFork ? work.owner + ":" + branchName : branchName);
            prCmd.add("--title");
            prCmd.add(prTitle);
            prCmd.add("--body");
            prCmd.add(prBody);
            ProcResult prResult = run(
                prCmd,
                cloneDir,
                DEFAULT_TIMEOUT_MS,
                GH_OWN_GIT_CREDENTIAL_ENV
            );
            if (prResult.exitCode != 0) {
                throw new CliException(
                    "Failed to open pull request",
                    prResult.exitCode,
                    prResult.output
                );
            }
            PrResult result = parsePrResult(prResult.output);
            success = true;
            return result;
        } finally {
            if (success) {
                deleteRecursive(cloneDir);
            } else {
                progress.accept(
                    "Left working files at " + cloneDir.getAbsolutePath() + " for troubleshooting."
                );
            }
        }
    }

    private void cloneWithRetry(
        WorkRepo work,
        String branch,
        File targetDir,
        Consumer<String> progress
    )
        throws CliException {
        String cloneUrl = "https://github.com/" + work.owner + "/" + work.repo + ".git";
        int attempts = work.isFork ? 3 : 1;
        CliException last = null;
        for (int i = 1; i <= attempts; i++) {
            if (i > 1) {
                deleteRecursive(targetDir);
                targetDir.mkdirs();
            }
            ProcResult result;
            try {
                result =
                    run(
                        List.of(
                            "git",
                            "clone",
                            "--depth",
                            "1",
                            "--branch",
                            branch,
                            "--single-branch",
                            cloneUrl,
                            targetDir.getAbsolutePath()
                        ),
                        null,
                        CLONE_TIMEOUT_MS,
                        GH_OWN_GIT_CREDENTIAL_ENV
                    );
            } catch (IOException e) {
                throw new CliException(
                    "git clone failed for " + cloneUrl + ": " + e.getMessage(),
                    -1,
                    ""
                );
            }
            if (result.exitCode == 0) return;
            last =
                new CliException(
                    "git clone failed for " + cloneUrl,
                    result.exitCode,
                    result.output
                );
            if (i < attempts) {
                progress.accept(
                    "Clone attempt " +
                    i +
                    " failed (a fresh fork can take a few seconds to become clonable) — retrying..."
                );
                sleep(2000);
            }
        }
        throw last;
    }

    private static PrResult parsePrResult(String output) throws CliException {
        Matcher m = Pattern
            .compile("https://github\\.com/[^/\\s]+/[^/\\s]+/pull/(\\d+)")
            .matcher(output);
        String lastUrl = null;
        int number = -1;
        while (m.find()) {
            lastUrl = m.group(0);
            number = Integer.parseInt(m.group(1));
        }
        if (lastUrl == null) {
            throw new CliException(
                "Could not determine the pull request URL from gh's output.",
                0,
                output
            );
        }
        PrResult result = new PrResult();
        result.url = lastUrl;
        result.number = number;
        return result;
    }

    // ─── Registry read (browse) ──────────────────────────────────────

    public String fetchRegistryJsonRaw(Consumer<String> progress) throws IOException {
        PluginRegistryConfig config = new PluginRegistryConfig();
        String registryRepo = config.getRegistryRepo();
        if (registryRepo == null) {
            throw new CliException(
                "No plugin registry repo configured. Set one in Registry Settings.",
                -1,
                ""
            );
        }
        progress.accept("Reading registry from " + registryRepo + "...");
        List<String> cmd = List.of(
            "gh",
            "api",
            "repos/" +
            registryRepo +
            "/contents/" +
            config.getRegistryPath() +
            "?ref=" +
            config.getRegistryBranch(),
            "-H",
            "Accept: application/vnd.github.raw"
        );
        ProcResult result = run(cmd, null, DEFAULT_TIMEOUT_MS);
        if (result.exitCode != 0) {
            throw new CliException(
                "Could not read the registry from " +
                registryRepo +
                ". Run 'gh auth status' to check your sign-in.",
                result.exitCode,
                result.output
            );
        }
        return result.output;
    }

    /**
     * Reads {@code plugins/<pluginName>/README.md} from the registry repo, the
     * same way {@link #fetchRegistryJsonRaw} reads registry.json. Returns
     * {@code null} (rather than throwing) when the file simply doesn't exist --
     * a missing README is an expected, non-error state for a plugin entry,
     * not something to interrupt the user over.
     */
    public String fetchPluginReadmeRaw(String pluginName, Consumer<String> progress)
        throws IOException {
        PluginRegistryConfig config = new PluginRegistryConfig();
        String registryRepo = config.getRegistryRepo();
        if (registryRepo == null) {
            throw new CliException(
                "No plugin registry repo configured. Set one in Registry Settings.",
                -1,
                ""
            );
        }
        progress.accept("Loading README for " + pluginName + "...");
        List<String> cmd = List.of(
            "gh",
            "api",
            "repos/" +
            registryRepo +
            "/contents/plugins/" +
            pluginName +
            "/README.md?ref=" +
            config.getRegistryBranch(),
            "-H",
            "Accept: application/vnd.github.raw"
        );
        ProcResult result = run(cmd, null, DEFAULT_TIMEOUT_MS);
        if (result.exitCode != 0) {
            if (result.output != null && result.output.contains("404")) {
                return null;
            }
            throw new CliException(
                "Could not read the README for " + pluginName + ".",
                result.exitCode,
                result.output
            );
        }
        return result.output;
    }

    // ─── Artifact resolution (install) ───────────────────────────────

    public File resolvePluginArtifact(
        String groupId,
        String artifactId,
        String version,
        File destDir,
        Consumer<String> progress
    )
        throws IOException {
        PluginRegistryConfig config = new PluginRegistryConfig();
        progress.accept(
            "Resolving " + groupId + ":" + artifactId + ":" + version + " from Azure Artifacts..."
        );
        if (!destDir.exists()) destDir.mkdirs();
        List<String> cmd = List.of(
            "mvn",
            "-q",
            "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy",
            "-Dartifact=" + groupId + ":" + artifactId + ":" + version,
            "-DoutputDirectory=" + destDir.getAbsolutePath(),
            "-DremoteRepositories=" +
            config.getAdoFeedServerId() +
            "::default::" +
            config.getAdoFeedMavenUrl()
        );
        ProcResult result = run(cmd, null, MVN_TIMEOUT_MS);
        if (result.exitCode != 0) {
            throw new CliException(
                "Failed to resolve " +
                artifactId +
                " " +
                version +
                " from the Azure Artifacts feed. Check that ~/.m2/settings.xml has a <server> " +
                "with id '" +
                config.getAdoFeedServerId() +
                "' and a valid, read-scoped ADO PAT -- set one via Registry Settings.",
                result.exitCode,
                result.output
            );
        }
        File jar = new File(destDir, artifactId + "-" + version + ".jar");
        if (!jar.exists()) {
            throw new CliException(
                "Maven reported success but the expected jar was not found at " +
                jar.getAbsolutePath(),
                0,
                result.output
            );
        }
        return jar;
    }

    /**
     * Resolves the plugin's transitive runtime dependencies (excluding
     * {@code provided}-scope, e.g. ingenious-api) into {@code libDestDir},
     * via a throwaway shim pom rather than re-declaring the plugin's own
     * dependency tree by hand.
     */
    public void resolvePluginDependencies(
        String groupId,
        String artifactId,
        String version,
        File libDestDir,
        Consumer<String> progress
    )
        throws IOException {
        PluginRegistryConfig config = new PluginRegistryConfig();
        progress.accept("Resolving dependencies for " + artifactId + "...");
        File shimDir = null;
        try {
            shimDir = Files.createTempDirectory("ingenious-plugin-shim-").toFile();
            File shimPom = new File(shimDir, "pom.xml");
            String pomXml =
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.ing.plugins.shim</groupId>\n" +
                "  <artifactId>dependency-resolver-shim</artifactId>\n" +
                "  <version>1.0.0</version>\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>" +
                groupId +
                "</groupId>\n" +
                "      <artifactId>" +
                artifactId +
                "</artifactId>\n" +
                "      <version>" +
                version +
                "</version>\n" +
                "    </dependency>\n" +
                "  </dependencies>\n" +
                "</project>\n";
            Files.write(shimPom.toPath(), pomXml.getBytes(StandardCharsets.UTF_8));
            if (!libDestDir.exists()) libDestDir.mkdirs();
            List<String> cmd = List.of(
                "mvn",
                "-q",
                "-f",
                shimPom.getAbsolutePath(),
                "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies",
                "-DexcludeScope=provided",
                "-DoutputDirectory=" + libDestDir.getAbsolutePath(),
                "-DremoteRepositories=" +
                config.getAdoFeedServerId() +
                "::default::" +
                config.getAdoFeedMavenUrl()
            );
            ProcResult result = run(cmd, null, MVN_TIMEOUT_MS);
            if (result.exitCode != 0) {
                throw new CliException(
                    "Failed to resolve dependencies for " + artifactId,
                    result.exitCode,
                    result.output
                );
            }
        } finally {
            deleteRecursive(shimDir);
        }
    }

    // ─── File helpers ─────────────────────────────────────────────────

    private static void copyDirectoryContents(File src, File dest) throws IOException {
        java.nio.file.Path srcPath = src.toPath();
        java.nio.file.Path destPath = dest.toPath();
        Files.createDirectories(destPath);
        try (java.util.stream.Stream<java.nio.file.Path> stream = Files.walk(srcPath)) {
            List<java.nio.file.Path> paths = stream.collect(java.util.stream.Collectors.toList());
            for (java.nio.file.Path path : paths) {
                java.nio.file.Path relative = srcPath.relativize(path);
                if (relative.toString().isEmpty()) continue;
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

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursive(child);
            }
        }
        file.delete();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String firstLine(String text) {
        if (text == null) return "";
        int idx = text.indexOf('\n');
        return (idx < 0 ? text : text.substring(0, idx)).trim();
    }

    // ─── Low-level process execution ─────────────────────────────────

    private void runOrThrow(List<String> command, File workingDir, String failureMessage)
        throws CliException {
        runOrThrow(command, workingDir, failureMessage, Map.of());
    }

    private void runOrThrow(
        List<String> command,
        File workingDir,
        String failureMessage,
        Map<String, String> extraEnv
    )
        throws CliException {
        try {
            ProcResult result = run(command, workingDir, DEFAULT_TIMEOUT_MS, extraEnv);
            if (result.exitCode != 0) {
                throw new CliException(failureMessage, result.exitCode, result.output);
            }
        } catch (IOException e) {
            if (e instanceof CliException) throw (CliException) e;
            throw new CliException(failureMessage + ": " + e.getMessage(), -1, "");
        }
    }

    /**
     * Runs a command, capturing combined stdout+stderr. On Windows, git/gh
     * are real executables Java can invoke bare, but mvn is typically
     * mvn.cmd, which ProcessBuilder will not resolve by bare name the way an
     * interactive shell does — so every command is uniformly routed through
     * {@code cmd /c} on Windows, mirroring the same fix already used in
     * AppMainFrame's own restart logic.
     */
    private ProcResult run(List<String> command, File workingDir, long timeoutMs)
        throws IOException {
        return run(command, workingDir, timeoutMs, Map.of());
    }

    /**
     * Same as {@link #run(List, File, long)}, but with extra environment
     * variables set for just this one process — never touching any actual
     * git config file. Used to hand gh's own git credential helper to a
     * single {@code gh pr create} invocation (see {@code GIT_CONFIG_COUNT}
     * env-based config override, supported by modern git) instead of
     * globally registering it via {@code gh auth setup-git}, which would
     * change the user's git behavior everywhere, not just for this app.
     */
    private ProcResult run(
        List<String> command,
        File workingDir,
        long timeoutMs,
        Map<String, String> extraEnv
    )
        throws IOException {
        List<String> full = withPlatformPrefix(command);
        ProcessBuilder pb = new ProcessBuilder(full);
        if (workingDir != null) pb.directory(workingDir);
        if (!extraEnv.isEmpty()) pb.environment().putAll(extraEnv);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(
            () -> {
                try (
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
                    )
                ) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        output.append(line).append('\n');
                    }
                } catch (IOException ignored) {
                    // process ended / stream closed
                }
            },
            "plugin-registry-cli-reader"
        );
        reader.setDaemon(true);
        reader.start();

        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted while running: " + String.join(" ", command));
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException(
                "Timed out after " + (timeoutMs / 1000) + "s running: " + String.join(" ", command)
            );
        }
        try {
            reader.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ProcResult result = new ProcResult();
        result.exitCode = process.exitValue();
        result.output = output.toString();
        return result;
    }

    private static List<String> withPlatformPrefix(List<String> command) {
        if (!SystemInfo.isWindows()) {
            return command;
        }
        List<String> full = new ArrayList<>();
        full.add("cmd");
        full.add("/c");
        full.addAll(command);
        return full;
    }
}
