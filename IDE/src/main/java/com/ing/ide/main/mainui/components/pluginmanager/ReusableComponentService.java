package com.ing.ide.main.mainui.components.pluginmanager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.component.Project;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestration for Reusable Component publish/browse/install -- mirrors {@link
 * PluginManagerService}'s shape, not its content. No build, no Maven coordinates, no version:
 * a Reusable Component is just the YAML files a Shared scenario already has on disk, plus
 * whatever descriptive metadata a contributor fills in fresh (there's no existing description/
 * author field on a Scenario/TestCase to reuse).
 */
public class ReusableComponentService {
    private static final Logger LOG = Logger.getLogger(ReusableComponentService.class.getName());
    private static final String SORT_ORDER_FILE = ".sort_order";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ReusableComponentCliService cliService = new ReusableComponentCliService();

    public List<ReusableComponentEntry> fetchRegistry() {
        try {
            String json = cliService.fetchRegistryJsonRaw(s -> {});
            Map<String, Object> root = mapper.readValue(
                json,
                new TypeReference<Map<String, Object>>() {}
            );
            Object componentsObj = root.get("components");
            if (componentsObj instanceof List) {
                return mapper.convertValue(
                    componentsObj,
                    new TypeReference<List<ReusableComponentEntry>>() {}
                );
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Reusable component registry fetch failed: {0}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /** Local Shared Reusable Component folder names, for the Publish tab's picker -- always already local, nothing to clone. */
    public List<String> listLocalSharedComponents() {
        File sharedRoot = new File(Project.getSharedReusableComponentsPath());
        File[] dirs = sharedRoot.listFiles(File::isDirectory);
        if (dirs == null) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (File dir : dirs) names.add(dir.getName());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /**
     * Copies a local Shared Reusable Component's files (flat -- one YAML per TestCase, no
     * sub-folders) into a fresh staging directory, skipping the machine-local {@code
     * .sort_order} file, and writes a {@code .component.json} sidecar with the given metadata.
     * Stamps {@code dateAdded} if not already set. The caller is responsible for deleting the
     * returned directory once submission completes.
     */
    public File stageSubmission(File scenarioDir, ReusableComponentEntry entry) throws IOException {
        if (entry.getDateAdded() == null || entry.getDateAdded().isEmpty()) {
            entry.setDateAdded(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        }
        File stagingDir = Files.createTempDirectory("ingenious-component-stage-").toFile();
        File[] files = scenarioDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && !SORT_ORDER_FILE.equals(f.getName())) {
                    Files.copy(
                        f.toPath(),
                        new File(stagingDir, f.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }
        }
        mapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(new File(stagingDir, ".component.json"), entry);
        return stagingDir;
    }

    /**
     * Computes the updated reusable-components.json content after adding-or-replacing one
     * entry (matched by name) -- written to a temp file the caller submits alongside the
     * component's own files in the same PR, since there's no merge-time bot pipeline here to
     * patch the registry afterward the way publish-pipeline.yml does for plugins.
     */
    public File buildUpdatedRegistry(ReusableComponentEntry entry) throws IOException {
        List<ReusableComponentEntry> updated = new ArrayList<>();
        for (ReusableComponentEntry existing : fetchRegistry()) {
            if (!existing.getName().equals(entry.getName())) updated.add(existing);
        }
        updated.add(entry);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("components", updated);
        File file = Files.createTempFile("reusable-components-", ".json").toFile();
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        return file;
    }

    /**
     * Copies a published component's files from the registry repo into the local Shared
     * folder -- no Maven artifact to resolve, since a Reusable Component is never built; this
     * just lists and fetches whatever files it actually has.
     */
    public void installComponent(ReusableComponentEntry entry, Consumer<String> progress)
        throws IOException {
        String listingJson = cliService.listComponentFilesRaw(entry.getName(), progress);
        List<Map<String, Object>> files = mapper.readValue(
            listingJson,
            new TypeReference<List<Map<String, Object>>>() {}
        );
        File destDir = new File(Project.getSharedReusableComponentsPath(), entry.getName());
        destDir.mkdirs();
        for (Map<String, Object> file : files) {
            if (!"file".equals(file.get("type"))) continue;
            String name = (String) file.get("name");
            String content = cliService.fetchComponentFileRaw(entry.getName(), name, progress);
            Files.write(new File(destDir, name).toPath(), content.getBytes("UTF-8"));
        }
    }
}
