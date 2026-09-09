package com.ing.ide.main.mainui.components.pluginmanager;

/**
 * Where the Plugin Marketplace's registry repo and Azure Artifacts feed live.
 * <p>
 * There's exactly one real marketplace, so every value here is a fixed constant,
 * not a per-user setting -- ordinary users never see or touch any of this;
 * {@code PluginRegistrySettingsDialog} only asks them for their own ADO PAT.
 * None of it is ever a credential either way -- auth is handled entirely by
 * {@code git}/{@code gh}/{@code mvn} themselves.
 * <p>
 * {@code REGISTRY_REPO}, {@code ADO_ORGANIZATION}, {@code ADO_PROJECT}, and
 * {@code ADO_FEED_NAME} below are still placeholders -- fill in the real
 * values for the marketplace's actual repo/org/project/feed before shipping.
 * The other four are already the real, established values used throughout
 * this project's pipelines and examples.
 */
public final class PluginRegistryConfig {
    private static final String REGISTRY_REPO = "ing-tech-hub/p33148-ingenious-marketplace";
    private static final String REGISTRY_BRANCH = "registry-branch";
    private static final String REGISTRY_PATH = "registry.json";
    private static final String MAVEN_GROUP_ID = "com.ing.plugins";
    private static final String ADO_ORGANIZATION = "INGCDaaS";
    private static final String ADO_PROJECT = "IngOne";
    private static final String ADO_FEED_NAME = "p33148-marketplace-feed";
    private static final String ADO_FEED_SERVER_ID = "p33148-marketplace-feed";

    /** Returns the configured {@code owner/repo} slug for the registry repo, or {@code null} if unset. */
    public String getRegistryRepo() {
        return blankToNull(REGISTRY_REPO);
    }

    public String getRegistryBranch() {
        return REGISTRY_BRANCH;
    }

    public String getRegistryPath() {
        return REGISTRY_PATH;
    }

    public String getMavenGroupId() {
        return MAVEN_GROUP_ID;
    }

    public String getAdoOrganization() {
        return ADO_ORGANIZATION;
    }

    public String getAdoProject() {
        return ADO_PROJECT;
    }

    public String getAdoFeedName() {
        return ADO_FEED_NAME;
    }

    public String getAdoFeedServerId() {
        return ADO_FEED_SERVER_ID;
    }

    /**
     * Assembles the Azure Artifacts Maven feed URL. This is the only place in
     * the codebase an ADO feed URL is built from its parts.
     */
    public String getAdoFeedMavenUrl() {
        String org = getAdoOrganization();
        String project = getAdoProject();
        String feed = getAdoFeedName();
        String base = "https://pkgs.dev.azure.com/" + (org == null ? "" : org);
        String withProject = (project == null || project.isEmpty()) ? base : base + "/" + project;
        return withProject + "/_packaging/" + (feed == null ? "" : feed) + "/maven/v1";
    }

    /** True once repo, org, and feed are all set — the minimum needed to do anything. */
    public boolean isConfigured() {
        return (
            getRegistryRepo() != null &&
            getAdoOrganization() != null &&
            !getAdoOrganization().isEmpty() &&
            getAdoFeedName() != null &&
            !getAdoFeedName().isEmpty()
        );
    }

    private static String blankToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}
