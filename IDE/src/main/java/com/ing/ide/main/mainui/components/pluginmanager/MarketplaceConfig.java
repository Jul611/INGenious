package com.ing.ide.main.mainui.components.pluginmanager;

/**
 * Where the marketplace's registry repo lives -- shared by both Plugins and Reusable
 * Components, since they're published into the same repo (different sub-paths, different
 * registry files: {@code registry.json} for plugins, {@code reusable-components.json} for
 * Reusable Components). The Azure Artifacts feed values are plugin-only -- Reusable Components
 * are GitHub-repo-only, no build, no feed.
 * <p>
 * Every value here is a fixed constant, not a per-user setting -- ordinary users never see or
 * touch any of this; {@code PluginRegistrySettingsDialog} only asks them for their own ADO PAT
 * (which is itself plugin-only, for install; Reusable Components never need it). None of it is
 * ever a credential either way -- auth is handled entirely by {@code git}/{@code gh}/{@code mvn}
 * themselves.
 */
public final class MarketplaceConfig {
    private static final String REGISTRY_REPO = "ing-tech-hub/p33148-ingenious-marketplace";
    private static final String REGISTRY_BRANCH = "registry-branch";
    private static final String REGISTRY_PATH = "registry.json";
    private static final String REUSABLE_COMPONENTS_REGISTRY_PATH = "reusable-components.json";
    private static final String REUSABLE_COMPONENTS_SUB_PATH = "reusable-components";
    private static final String MAVEN_GROUP_ID = "com.ing.plugins";
    private static final String ADO_ORGANIZATION = "INGCDaaS";
    private static final String ADO_PROJECT = "IngOne";
    private static final String ADO_FEED_NAME = "p33148-marketplace-feed";
    private static final String ADO_FEED_SERVER_ID = "p33148-marketplace-feed";

    /** Returns the configured {@code owner/repo} slug for the registry repo, or {@code null} if unset. */
    public String getRegistryRepo() {
        return blankToNull(REGISTRY_REPO);
    }

    /** Branch of the registry repo that everything (plugins and Reusable Components) is submitted against. */
    public String getRegistryBranch() {
        return REGISTRY_BRANCH;
    }

    /** Path to registry.json (the plugin registry) within the registry repo. */
    public String getRegistryPath() {
        return REGISTRY_PATH;
    }

    /** Path to the Reusable Components registry file, in the same repo/branch as {@link #getRegistryPath()}. */
    public String getReusableComponentsRegistryPath() {
        return REUSABLE_COMPONENTS_REGISTRY_PATH;
    }

    /** Sub-path a submitted Reusable Component's files land under, parallel to {@code "plugins"} for plugins. */
    public String getReusableComponentsSubPath() {
        return REUSABLE_COMPONENTS_SUB_PATH;
    }

    /** Required Maven {@code groupId} namespace every plugin's pom.xml must use. */
    public String getMavenGroupId() {
        return MAVEN_GROUP_ID;
    }

    /** Azure DevOps organization the Artifacts feed lives in. */
    public String getAdoOrganization() {
        return ADO_ORGANIZATION;
    }

    /** Azure DevOps project the Artifacts feed is scoped to, or blank if the feed is org-scoped. */
    public String getAdoProject() {
        return ADO_PROJECT;
    }

    /** The real Azure Artifacts feed name -- what {@code MavenAuthenticate@0}'s {@code artifactsFeeds} input authenticates against in publish-pipeline.yml. */
    public String getAdoFeedName() {
        return ADO_FEED_NAME;
    }

    /**
     * Purely local {@code <server>} id used to match the generated shim pom's
     * {@code <repository><id>} against a {@code <server><id>} in the user's own
     * {@code ~/.m2/settings.xml} -- not required to equal the real feed name (see
     * {@link #getAdoFeedName()}), though publish-pipeline.yml's own value is kept identical
     * to it by convention so there's one string to remember instead of two.
     */
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
