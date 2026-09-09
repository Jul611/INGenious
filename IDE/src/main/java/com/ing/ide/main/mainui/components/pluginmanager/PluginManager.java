package com.ing.ide.main.mainui.components.pluginmanager;

import com.ing.ide.main.mainui.AppMainFrame;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import javax.swing.*;

/**
 * Main Marketplace window -- Browse, Installed, and Publish tabs, covering both plugins and
 * Reusable Components. Browse and Publish host both artifact types (Browse merges both into
 * one filterable list; Publish swaps between two separate forms via a type selector);
 * Installed stays plugin-only, since a Reusable Component's installed state already lives in
 * Test Design's own Shared Reusable Components tree.
 */
public class PluginManager extends JPanel {
    private final AppMainFrame mainFrame;
    private final PluginManagerService service;
    private final ReusableComponentService reusableComponentService;
    private final PluginManagerBrowseUI browseUI;
    private final PluginManagerInstalledUI installedUI;
    private final PluginManagerPublishUI publishUI;
    private final ReusableComponentPublishUI reusableComponentPublishUI;
    private final JLabel statusLabel;
    private final JButton installFromFileButton;
    private final JButton registrySettingsButton;
    private final PluginRegistryConfig registryConfig = new PluginRegistryConfig();

    /**
     * Creates the Plugin Manager tab.
     *
     * @param mainFrame the parent AppMainFrame (can be null for standalone testing)
     */
    public PluginManager(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.service = new PluginManagerService();
        this.reusableComponentService = new ReusableComponentService();

        setLayout(new BorderLayout());

        // Tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Browse tab -- plugins and Reusable Components together, filterable by type
        this.browseUI =
            new PluginManagerBrowseUI(service, reusableComponentService, this::refreshInstalled);
        tabbedPane.addTab("Browse", browseUI);

        // Installed tab -- plugins only; a Reusable Component's "installed" state is
        // already visible in Test Design's own Shared Reusable Components tree
        this.installedUI = new PluginManagerInstalledUI(service, this::refreshInstalled);
        tabbedPane.addTab("Installed", installedUI);

        // Publish tab -- one tab, a type selector swaps which form is showing
        this.publishUI = new PluginManagerPublishUI(service, this::refreshInstalled);
        this.reusableComponentPublishUI = new ReusableComponentPublishUI();
        tabbedPane.addTab("Publish", buildCombinedPublishTab());

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

        registrySettingsButton = new JButton("ADO Credential Setup...");
        registrySettingsButton.setToolTipText(
            "Set your Azure DevOps PAT so installs can resolve plugins from the Artifacts feed"
        );
        registrySettingsButton.addActionListener(
            e -> PluginRegistrySettingsDialog.show(PluginManager.this, registryConfig)
        );

        JPanel eastButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 0));
        eastButtons.add(registrySettingsButton);
        eastButtons.add(installFromFileButton);
        statusPanel.add(eastButtons, BorderLayout.EAST);

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
     * One Publish tab, a type selector at top swaps which form is showing via CardLayout --
     * the two forms stay fully separate ({@link PluginManagerPublishUI} vs. {@link
     * ReusableComponentPublishUI}, no shared/conditional fields), only the container is
     * unified.
     */
    private JPanel buildCombinedPublishTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel selectorRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
        selectorRow.add(new JLabel("Publish a:"));
        JComboBox<String> typeSelector = new JComboBox<>(
            new String[] { "Plugin", "Reusable Component" }
        );
        selectorRow.add(typeSelector);
        panel.add(selectorRow, BorderLayout.NORTH);

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);
        cards.add(publishUI, "Plugin");
        cards.add(reusableComponentPublishUI, "Reusable Component");
        panel.add(cards, BorderLayout.CENTER);

        typeSelector.addActionListener(
            e -> cardLayout.show(cards, (String) typeSelector.getSelectedItem())
        );

        return panel;
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
