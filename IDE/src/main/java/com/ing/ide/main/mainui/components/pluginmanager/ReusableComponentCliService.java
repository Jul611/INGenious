package com.ing.ide.main.mainui.components.pluginmanager;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Thin wrapper over {@link MarketplaceCliService}'s generic submission/read mechanics for
 * Reusable Components -- no Maven, no ADO feed, nothing plugin-specific. Everything here just
 * calls into the same git/gh process-shelling {@link MarketplaceCliService} already does for
 * plugins, pointed at a different sub-path and registry file in the same repo.
 */
public class ReusableComponentCliService {
    private final MarketplaceCliService cliService = new MarketplaceCliService();
    private final MarketplaceConfig config = new MarketplaceConfig();

    /**
     * Read-only shallow clone of a contributor-supplied repo+branch -- identical to what
     * {@code MarketplaceCliService.cloneContributorSource()} already does for plugins.
     * Nothing here is plugin-specific, so this is a straight passthrough, not a reimplementation.
     */
    public File cloneSource(String repoUrl, String branch, Consumer<String> progress)
        throws IOException {
        return cliService.cloneContributorSource(repoUrl, branch, progress);
    }

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
     * MarketplaceCliService.submitPlugin()}, just without a version in the branch name/
     * commit message, since Reusable Components don't have one.
     */
    public MarketplaceCliService.PrResult submitComponent(
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

    public MarketplaceCliService.ToolStatus checkGitInstalled() {
        return cliService.checkGitInstalled();
    }

    public MarketplaceCliService.ToolStatus checkGhInstalled() {
        return cliService.checkGhInstalled();
    }

    public MarketplaceCliService.AuthStatus checkGhAuthStatus() {
        return cliService.checkGhAuthStatus();
    }
}
