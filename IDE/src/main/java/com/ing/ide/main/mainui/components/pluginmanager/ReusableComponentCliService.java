package com.ing.ide.main.mainui.components.pluginmanager;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Thin wrapper over {@link PluginRegistryCliService}'s generic submission/read mechanics for
 * Reusable Components -- no Maven, no ADO feed, nothing plugin-specific. Everything here just
 * calls into the same git/gh process-shelling {@link PluginRegistryCliService} already does for
 * plugins, pointed at a different sub-path and registry file in the same repo.
 */
public class ReusableComponentCliService {
    private final PluginRegistryCliService cliService = new PluginRegistryCliService();
    private final PluginRegistryConfig config = new PluginRegistryConfig();

    /** Reads reusable-components.json from the registry repo, the same way plugins read registry.json. */
    public String fetchRegistryJsonRaw(Consumer<String> progress) throws IOException {
        return cliService.fetchFileFromRegistry(
            config.getReusableComponentsRegistryPath(),
            progress
        );
    }

    /**
     * Forks-or-branches, copies {@code stagedContentDir}'s contents into
     * {@code reusable-components/{name}/} on a new branch, also commits {@code
     * updatedRegistryFile} as the new {@code reusable-components.json} in the same PR (no
     * merge-time bot pipeline exists for this artifact type to patch the registry afterward,
     * unlike plugins), and opens a pull request. Same underlying mechanism as {@code
     * PluginRegistryCliService.submitPlugin()}, just without a version in the branch name/
     * commit message, since Reusable Components don't have one.
     */
    public PluginRegistryCliService.PrResult submitComponent(
        File stagedContentDir,
        File updatedRegistryFile,
        String name,
        String prTitle,
        String prBody,
        Consumer<String> progress
    )
        throws IOException {
        return cliService.submitToRegistry(
            stagedContentDir,
            config.getReusableComponentsSubPath() + "/" + name,
            Map.of(config.getReusableComponentsRegistryPath(), updatedRegistryFile),
            "reusable-component/" + name,
            "Add reusable component: " + name,
            prTitle,
            prBody,
            progress
        );
    }

    /** Lists the files under a published component's directory in the registry repo. */
    public String listComponentFilesRaw(String name, Consumer<String> progress) throws IOException {
        return cliService.listRegistryDirectoryRaw(
            config.getReusableComponentsSubPath() + "/" + name,
            progress
        );
    }

    /** Fetches one file's raw content from a published component's directory. */
    public String fetchComponentFileRaw(String name, String fileName, Consumer<String> progress)
        throws IOException {
        return cliService.fetchFileFromRegistry(
            config.getReusableComponentsSubPath() + "/" + name + "/" + fileName,
            progress
        );
    }

    public PluginRegistryCliService.ToolStatus checkGitInstalled() {
        return cliService.checkGitInstalled();
    }

    public PluginRegistryCliService.ToolStatus checkGhInstalled() {
        return cliService.checkGhInstalled();
    }

    public PluginRegistryCliService.AuthStatus checkGhAuthStatus() {
        return cliService.checkGhAuthStatus();
    }
}
