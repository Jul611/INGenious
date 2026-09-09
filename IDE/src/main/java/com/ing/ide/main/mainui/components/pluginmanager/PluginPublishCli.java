package com.ing.ide.main.mainui.components.pluginmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Headless equivalent of the Publish tab, for contributors who'd rather submit
 * from a terminal/CI step than the Swing UI. Runs the exact same sequence
 * {@link PluginManagerPublishUI#submitForReview} does — build, extract
 * manifest/actions, version-gate, stage, open the PR — against
 * {@link PluginManagerService} / {@link MarketplaceCliService} directly,
 * so nothing about the submission logic itself is reimplemented here.
 *
 * Unlike the Publish tab, source is always a local folder (no repo+branch
 * clone step) — the assumption is this runs from inside, or a CI checkout
 * of, the contributor's own already-cloned plugin repo. Metadata that the
 * Swing UI collects via form fields is instead read from a {@code
 * .submission.json} already sitting in that folder — the same file the
 * contributor pipeline template already requires, so there's nothing new to
 * author. {@code name}, {@code version}, {@code actions} and {@code
 * entryClasses} are always derived fresh from the build, overriding
 * whatever (if anything) is already in that file, same as
 * publish-pipeline.yml's own registry-patch step does.
 *
 * Usage: {@code mvn -pl IDE -am compile exec:java
 * -Dexec.mainClass=com.ing.ide.main.mainui.components.pluginmanager.PluginPublishCli
 * -Dexec.args="path/to/plugin"}
 */
public final class PluginPublishCli {

    private PluginPublishCli() {}

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: PluginPublishCli <path-to-plugin-folder>");
            System.exit(1);
            return;
        }

        try {
            run(new File(args[0]));
        } catch (Exception ex) {
            System.err.println("Publish failed: " + rootMessage(ex));
            System.exit(1);
        }
    }

    private static void run(File pluginDir) throws IOException {
        if (!pluginDir.isDirectory()) {
            throw new IOException("Not a directory: " + pluginDir.getAbsolutePath());
        }
        File pom = new File(pluginDir, "pom.xml");
        File submissionFile = new File(pluginDir, ".submission.json");
        if (!pom.exists()) throw new IOException(
            "Missing pom.xml in " + pluginDir.getAbsolutePath()
        );
        if (!submissionFile.exists()) {
            throw new IOException(
                "Missing .submission.json in " +
                pluginDir.getAbsolutePath() +
                " -- same file the contributor pipeline template requires; author it the same way."
            );
        }

        // Description file: no naming requirement, just exactly one *.md, same as the
        // Publish tab's loadReadmeFromSource() -- staging keeps whatever it's called.
        File[] mdFiles = pluginDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".md"));
        if (mdFiles == null || mdFiles.length == 0) {
            throw new IOException("No .md file found in " + pluginDir.getAbsolutePath());
        }
        if (mdFiles.length > 1) {
            throw new IOException(
                "Multiple .md files found in " +
                pluginDir.getAbsolutePath() +
                " -- keep exactly one so it's unambiguous which is the description."
            );
        }
        File readmeFile = mdFiles[0];

        String readmeContent = new String(Files.readAllBytes(readmeFile.toPath()), "UTF-8");
        int wordCount = readmeContent.trim().isEmpty()
            ? 0
            : readmeContent.trim().split("\\s+").length;
        if (wordCount < 50) {
            throw new IOException(
                readmeFile.getName() + " must be at least 50 words, found " + wordCount + "."
            );
        }

        ObjectMapper mapper = new ObjectMapper();
        PluginRegistryEntry entry = mapper.readValue(submissionFile, PluginRegistryEntry.class);

        Consumer<String> progress = System.out::println;
        PluginManagerService service = new PluginManagerService();
        MarketplaceCliService cliService = new MarketplaceCliService();

        File jar = cliService.buildLocally(pluginDir, progress);

        String requiredGroupId = new MarketplaceConfig().getMavenGroupId();
        if (requiredGroupId != null && !requiredGroupId.trim().isEmpty()) {
            String actualGroupId = cliService.readPomGroupId(pluginDir);
            if (!requiredGroupId.trim().equals(actualGroupId)) {
                throw new IOException(
                    "pom.xml groupId '" +
                    actualGroupId +
                    "' must be '" +
                    requiredGroupId.trim() +
                    "'."
                );
            }
        }

        String manifestName = null, manifestVersion = null, entryClasses = null;
        try (JarFile jf = new JarFile(jar)) {
            Manifest mf = jf.getManifest();
            if (mf != null) {
                Attributes attrs = mf.getMainAttributes();
                entryClasses = attrs.getValue("pluginEntryClasses");
                manifestName = attrs.getValue("Plugin-Name");
                manifestVersion = attrs.getValue("Plugin-Version");
            }
        }
        if (entryClasses == null || entryClasses.trim().isEmpty()) {
            throw new IOException(
                "Built jar is missing 'pluginEntryClasses' in META-INF/MANIFEST.MF -- required for the engine to load the plugin."
            );
        }
        if (manifestVersion == null || manifestVersion.trim().isEmpty()) {
            throw new IOException("Built jar's manifest has no Plugin-Version.");
        }

        String displayName = entry.getDisplayName() != null &&
            !entry.getDisplayName().trim().isEmpty()
            ? entry.getDisplayName().trim()
            : (manifestName != null ? manifestName : pluginDir.getName());
        String pluginId = displayName.toLowerCase().replaceAll("[^a-z0-9-]", "-");

        entry.setName(pluginId);
        entry.setDisplayName(displayName);
        entry.setVersion(manifestVersion.trim());
        entry.setEntryClasses(entryClasses);
        List<PluginRegistryEntry.ActionInfo> extractedActions = service.extractActionsFromJar(
            jar,
            entryClasses
        );
        entry.setActions(
            extractedActions != null
                ? extractedActions
                    .stream()
                    .map(PluginRegistryEntry.ActionInfo::getName)
                    .collect(Collectors.toList())
                : Collections.emptyList()
        );
        if (entry.getReleaseNotes() == null) entry.setReleaseNotes("");

        progress.accept("Checking published version...");
        service.checkVersionIsNewer(entry.getName(), entry.getVersion());

        progress.accept("Staging submission files...");
        File stagingDir = service.stagePluginSubmission(pluginDir, entry);
        MarketplaceCliService.PrResult result;
        try {
            result =
                cliService.submitPlugin(
                    stagingDir,
                    entry.getName(),
                    entry.getVersion(),
                    "Add plugin: " + entry.getDisplayName() + " v" + entry.getVersion(),
                    buildPrBody(entry),
                    progress
                );
        } finally {
            deleteRecursive(stagingDir);
        }

        System.out.println("Pull request opened: " + result.url);
        try {
            service.publishPlugin(jar, entry);
        } catch (Exception localEx) {
            System.err.println(
                "Warning: local staging after submission failed: " + rootMessage(localEx)
            );
        }
    }

    private static String buildPrBody(PluginRegistryEntry entry) {
        return (
            "**Plugin:** " +
            entry.getDisplayName() +
            "\n**Version:** " +
            entry.getVersion() +
            "\n**Author:** " +
            entry.getAuthor() +
            "\n\n" +
            (entry.getDescription() == null ? "" : entry.getDescription()) +
            "\n\nSubmitted via PluginPublishCli."
        );
    }

    private static void deleteRecursive(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursive(child);
        }
        dir.delete();
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }
}
