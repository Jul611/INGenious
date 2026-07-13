package com.ing.ide.main.mainui.components.pluginmanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Data model for a locally installed plugin, tracked via a .plugininfo file.
 */
public class PluginInstalledEntry {
    private String name;
    private String displayName;
    private String version;
    private String description;
    private String author;
    private File pluginFolder;
    private List<String> actions;

    public PluginInstalledEntry() {
        this.actions = new ArrayList<>();
    }

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public File getPluginFolder() {
        return pluginFolder;
    }

    public void setPluginFolder(File pluginFolder) {
        this.pluginFolder = pluginFolder;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        return displayName != null ? displayName : name;
    }
}
