package com.ing.ide.main.mainui.components.pluginmanager;

import com.ing.ide.settings.AppSettings;
import com.ing.ide.settings.AppSettings.APP_SETTINGS;

/**
 * Typed wrapper over {@link AppSettings} for everything the Plugin Marketplace
 * needs to know about *where* the registry repo and Azure Artifacts feed live.
 * <p>
 * Every value here is environment-specific (a personal account today, the
 * company org once GitHub migration lands) and none of it is ever a
 * credential — auth is handled entirely by {@code git}/{@code gh}/{@code mvn}
 * themselves. Swapping environments should only ever mean changing these
 * values, never touching code.
 */
public final class PluginRegistryConfig {

    /** Returns the configured {@code owner/repo} slug for the registry repo, or {@code null} if unset. */
    public String getRegistryRepo() {
        return blankToNull(AppSettings.get(APP_SETTINGS.PLUGIN_REGISTRY_REPO.getKey()));
    }

    public void setRegistryRepo(String value) {
        AppSettings.set(
            APP_SETTINGS.PLUGIN_REGISTRY_REPO.getKey(),
            value == null ? "" : value.trim()
        );
        AppSettings.store("Plugin registry repo updated");
    }

    public String getRegistryBranch() {
        return AppSettings.get(APP_SETTINGS.PLUGIN_REGISTRY_BRANCH.getKey());
    }

    public void setRegistryBranch(String value) {
        AppSettings.set(
            APP_SETTINGS.PLUGIN_REGISTRY_BRANCH.getKey(),
            value == null ? "" : value.trim()
        );
        AppSettings.store("Plugin registry branch updated");
    }

    public String getRegistryPath() {
        return AppSettings.get(APP_SETTINGS.PLUGIN_REGISTRY_PATH.getKey());
    }

    public void setRegistryPath(String value) {
        AppSettings.set(
            APP_SETTINGS.PLUGIN_REGISTRY_PATH.getKey(),
            value == null ? "" : value.trim()
        );
        AppSettings.store("Plugin registry path updated");
    }

    public String getMavenGroupId() {
        return AppSettings.get(APP_SETTINGS.PLUGIN_MAVEN_GROUP_ID.getKey());
    }

    public void setMavenGroupId(String value) {
        AppSettings.set(
            APP_SETTINGS.PLUGIN_MAVEN_GROUP_ID.getKey(),
            value == null ? "" : value.trim()
        );
        AppSettings.store("Plugin Maven groupId updated");
    }

    public String getAdoOrganization() {
        return AppSettings.get(APP_SETTINGS.ADO_ORGANIZATION.getKey());
    }

    public void setAdoOrganization(String value) {
        AppSettings.set(APP_SETTINGS.ADO_ORGANIZATION.getKey(), value == null ? "" : value.trim());
        AppSettings.store("ADO organization updated");
    }

    public String getAdoProject() {
        return AppSettings.get(APP_SETTINGS.ADO_PROJECT.getKey());
    }

    public void setAdoProject(String value) {
        AppSettings.set(APP_SETTINGS.ADO_PROJECT.getKey(), value == null ? "" : value.trim());
        AppSettings.store("ADO project updated");
    }

    public String getAdoFeedName() {
        return AppSettings.get(APP_SETTINGS.ADO_FEED_NAME.getKey());
    }

    public void setAdoFeedName(String value) {
        AppSettings.set(APP_SETTINGS.ADO_FEED_NAME.getKey(), value == null ? "" : value.trim());
        AppSettings.store("ADO feed name updated");
    }

    public String getAdoFeedServerId() {
        return AppSettings.get(APP_SETTINGS.ADO_FEED_SERVER_ID.getKey());
    }

    public void setAdoFeedServerId(String value) {
        AppSettings.set(
            APP_SETTINGS.ADO_FEED_SERVER_ID.getKey(),
            value == null ? "" : value.trim()
        );
        AppSettings.store("ADO feed server id updated");
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
