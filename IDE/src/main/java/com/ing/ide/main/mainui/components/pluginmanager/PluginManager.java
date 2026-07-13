package com.ing.ide.main.mainui.components.pluginmanager;

import com.ing.ide.main.mainui.AppMainFrame;
import java.awt.BorderLayout;
import javax.swing.*;

/**
 * Main Plugin Manager tab component.
 * Contains a JTabbedPane with Browse (Marketplace) and Installed tabs.
 * <p>
 * For the PoC, this is self-contained and reads from a local registry.json.
 * In production, it would fetch from a remote GitHub/Azure DevOps URL.
 * </p>
 */
public class PluginManager extends JPanel {
    private final AppMainFrame mainFrame;
    private final PluginManagerService service;
    private final PluginManagerBrowseUI browseUI;
    private final PluginManagerInstalledUI installedUI;
    private final JLabel statusLabel;
    private final JButton installFromFileButton;

    /**
     * Creates the Plugin Manager tab.
     *
     * @param mainFrame the parent AppMainFrame (can be null for standalone testing)
     */
    public PluginManager(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.service = new PluginManagerService();

        setLayout(new BorderLayout());

        // Tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Browse tab
        this.browseUI = new PluginManagerBrowseUI(service, this::refreshInstalled);
        tabbedPane.addTab("Browse", browseUI);

        // Installed tab
        this.installedUI = new PluginManagerInstalledUI(service, this::refreshInstalled);
        tabbedPane.addTab("Installed", installedUI);

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Plugin Manager v1.0");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // Install from File button (fallback for offline/demo)
        installFromFileButton = new JButton("Import from File...");
        installFromFileButton.setToolTipText("Install a plugin from a local JAR file");
        installFromFileButton.addActionListener(e -> importFromFile());
        statusPanel.add(installFromFileButton, BorderLayout.EAST);

        add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Called when this slide becomes visible. Refreshes the data.
     */
    public void load() {
        browseUI.loadData();
        installedUI.loadData();
        statusLabel.setText(
            "Plugin Manager v1.0 | Plugins directory: " + PluginManagerService.getPluginsDirectory()
        );
    }

    /**
     * Refreshes the installed plugins list (e.g., after install/uninstall).
     */
    private void refreshInstalled() {
        installedUI.loadData();
    }

    /**
     * Opens a file chooser to import a plugin from a local JAR file.
     * This is the offline fallback (Backup #1 from the architecture plan).
     */
    private void importFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Plugin JAR File");
        fileChooser.setFileFilter(
            new javax.swing.filechooser.FileNameExtensionFilter("JAR files (*.jar)", "jar")
        );

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            String pluginName = selectedFile.getName();
            if (pluginName.endsWith(".jar")) {
                pluginName = pluginName.substring(0, pluginName.length() - 4);
            }

            try {
                // Create plugin directory
                java.io.File pluginDir = new java.io.File(
                    PluginManagerService.getPluginsDirectory(),
                    pluginName
                );
                if (!pluginDir.exists()) {
                    pluginDir.mkdirs();
                }

                // Copy JAR
                java.nio.file.Files.copy(
                    selectedFile.toPath(),
                    new java.io.File(pluginDir, selectedFile.getName()).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );

                // Create basic .plugininfo
                java.util.Properties info = new java.util.Properties();
                info.setProperty("name", pluginName);
                info.setProperty("displayName", pluginName);
                info.setProperty("version", "Imported");
                info.setProperty("description", "Manually imported plugin");
                try (
                    java.io.OutputStream os = new java.io.FileOutputStream(
                        new java.io.File(pluginDir, ".plugininfo")
                    )
                ) {
                    info.store(os, "Plugin info");
                }

                JOptionPane.showMessageDialog(
                    this,
                    "Plugin \"" +
                    pluginName +
                    "\" imported successfully.\n" +
                    "Please restart INGenious for the changes to take effect.",
                    "Plugin Imported",
                    JOptionPane.INFORMATION_MESSAGE
                );

                refreshInstalled();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to import plugin: " + e.getMessage(),
                    "Import Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
