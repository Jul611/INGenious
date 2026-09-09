package com.ing.ide.main.mainui.components.pluginmanager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Data model for a Reusable Component entry in the marketplace's
 * reusable-components.json. Deliberately much smaller than {@link PluginRegistryEntry} --
 * no build, no Maven coordinates, no version: a Reusable Component is just data
 * (test-case/scenario YAML), and resubmitting the same name is treated as an update,
 * gated by review each time, rather than needing a version-newer check.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReusableComponentEntry {
    private String name;
    private String displayName;
    private String description;
    private String author;
    private String authorEmail;
    private List<String> tags;
    private String dateAdded;
    private boolean featured;

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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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

    @Override
    public String toString() {
        return displayName != null ? displayName : name;
    }
}
