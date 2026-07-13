package com.ing.ide.main.mainui.components.pluginmanager;

import java.util.List;

/**
 * Data model for a plugin entry in the marketplace registry.json.
 */
public class PluginRegistryEntry {
    private String name;
    private String displayName;
    private String description;
    private String author;
    private String authorEmail;
    private String version;
    private String minEngineVersion;
    private String maxEngineVersion;
    private String downloadUrl;
    private List<String> libUrls;
    private List<String> objectTypes;
    private int actionCount;
    private List<String> actions;
    private String homepageUrl;
    private String license;
    private String releaseNotes;
    private String dateAdded;
    private boolean featured;
    private String githubRepo;

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMinEngineVersion() {
        return minEngineVersion;
    }

    public void setMinEngineVersion(String minEngineVersion) {
        this.minEngineVersion = minEngineVersion;
    }

    public String getMaxEngineVersion() {
        return maxEngineVersion;
    }

    public void setMaxEngineVersion(String maxEngineVersion) {
        this.maxEngineVersion = maxEngineVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public List<String> getLibUrls() {
        return libUrls;
    }

    public void setLibUrls(List<String> libUrls) {
        this.libUrls = libUrls;
    }

    public List<String> getObjectTypes() {
        return objectTypes;
    }

    public void setObjectTypes(List<String> objectTypes) {
        this.objectTypes = objectTypes;
    }

    public int getActionCount() {
        return actionCount;
    }

    public void setActionCount(int actionCount) {
        this.actionCount = actionCount;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(String releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public String getGithubRepo() {
        return githubRepo;
    }

    public void setGithubRepo(String githubRepo) {
        this.githubRepo = githubRepo;
    }

    @Override
    public String toString() {
        return displayName != null ? displayName : name;
    }
}
