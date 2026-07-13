package com.ing.ide.main.mainui.components.pluginmanager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * "Publish" tab for the Plugin Manager.
 * <p>
 * Allows plugin authors to select a built JAR, review auto-extracted metadata,
 * fill in additional fields, and publish it to the local registry.
 * </p>
 */
public class PluginManagerPublishUI extends JPanel {
    private static final Logger LOG = Logger.getLogger(PluginManagerPublishUI.class.getName());

    private final PluginManagerService service;
    private final Runnable onPublishCallback;

    private JLabel fileLabel;
    private JLabel manifestEntryClassesLabel;
    private JLabel manifestNameLabel;
    private JLabel manifestVersionLabel;
    private JLabel manifestAuthorLabel;
    private JLabel manifestActionsLabel;

    private JTextField displayNameField;
    private JTextField descriptionArea;
    private JTextField authorField;
    private JTextField authorEmailField;
    private JTextField versionField;
    private JTextField minEngineField;
    private JTextField maxEngineField;
    private JTextField objectTypesField;
    private JTextField licenseField;
    private JTextField releaseNotesField;
    private JPasswordField githubTokenField;
    private JButton publishButton;

    private File selectedJar;
    private String extractedEntryClasses;
    private List<PluginRegistryEntry.ActionInfo> extractedActions;

    public PluginManagerPublishUI(PluginManagerService service, Runnable onPublishCallback) {
        this.service = service;
        this.onPublishCallback = onPublishCallback;
        setLayout(new BorderLayout());
        initUI();
    }

    private void resetLabelGbc(GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);
    }

    private void resetValueGbc(GridBagConstraints gbc, int row) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        int row = 0;

        // Step 1
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        JLabel step1 = new JLabel("Step 1: Select a built plugin JAR");
        step1.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(step1, gbc);
        row++;

        resetLabelGbc(gbc, row);
        gbc.gridwidth = 1;
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(this::browseJar);
        mainPanel.add(browseButton, gbc);
        resetValueGbc(gbc, row);
        fileLabel = new JLabel("No file selected");
        fileLabel.setForeground(Color.GRAY);
        mainPanel.add(fileLabel, gbc);
        row++;

        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 4, 12, 4);
        mainPanel.add(new JSeparator(), gbc);
        row++;

        // Step 2
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        JLabel step2 = new JLabel("Step 2: Review & Edit Metadata");
        step2.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(step2, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Entry Classes:"), gbc);
        resetValueGbc(gbc, row);
        manifestEntryClassesLabel = new JLabel("-");
        manifestEntryClassesLabel.setForeground(Color.GRAY);
        mainPanel.add(manifestEntryClassesLabel, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Plugin Name (from manifest):"), gbc);
        resetValueGbc(gbc, row);
        manifestNameLabel = new JLabel("-");
        manifestNameLabel.setForeground(Color.GRAY);
        mainPanel.add(manifestNameLabel, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Version (from manifest):"), gbc);
        resetValueGbc(gbc, row);
        manifestVersionLabel = new JLabel("-");
        manifestVersionLabel.setForeground(Color.GRAY);
        mainPanel.add(manifestVersionLabel, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Author (from manifest):"), gbc);
        resetValueGbc(gbc, row);
        manifestAuthorLabel = new JLabel("-");
        manifestAuthorLabel.setForeground(Color.GRAY);
        mainPanel.add(manifestAuthorLabel, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Actions found:"), gbc);
        resetValueGbc(gbc, row);
        manifestActionsLabel = new JLabel("-");
        manifestActionsLabel.setForeground(Color.GRAY);
        mainPanel.add(manifestActionsLabel, gbc);
        row++;

        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 4, 12, 4);
        mainPanel.add(new JSeparator(), gbc);
        row++;

        // Step 3
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        JLabel step3 = new JLabel("Step 3: Fill in registry details");
        step3.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(step3, gbc);
        row++;

        // Display Name
        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Display Name:"), gbc);
        displayNameField = new JTextField(30);
        resetValueGbc(gbc, row);
        mainPanel.add(displayNameField, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Description:"), gbc);
        descriptionArea = new JTextField(30);
        resetValueGbc(gbc, row);
        mainPanel.add(descriptionArea, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Author:"), gbc);
        authorField = new JTextField(30);
        resetValueGbc(gbc, row);
        mainPanel.add(authorField, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Author Email:"), gbc);
        authorEmailField = new JTextField(30);
        resetValueGbc(gbc, row);
        mainPanel.add(authorEmailField, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Version:"), gbc);
        versionField = new JTextField(10);
        resetValueGbc(gbc, row);
        mainPanel.add(versionField, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Min Engine:"), gbc);
        minEngineField = new JTextField(10);
        resetValueGbc(gbc, row);
        mainPanel.add(minEngineField, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Max Engine (blank = latest):"), gbc);
        maxEngineField = new JTextField(10);
        resetValueGbc(gbc, row);
        mainPanel.add(maxEngineField, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Object Types (comma-sep):"), gbc);
        objectTypesField = new JTextField(20);
        resetValueGbc(gbc, row);
        mainPanel.add(objectTypesField, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("License:"), gbc);
        licenseField = new JTextField(15);
        resetValueGbc(gbc, row);
        mainPanel.add(licenseField, gbc);
        row++;

        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Release Notes:"), gbc);
        releaseNotesField = new JTextField(30);
        resetValueGbc(gbc, row);
        mainPanel.add(releaseNotesField, gbc);
        row++;

        // GitHub PAT
        resetLabelGbc(gbc, row);
        mainPanel.add(new JLabel("Auth Token (GitHub PAT):"), gbc);
        githubTokenField = new JPasswordField(40);
        githubTokenField.setToolTipText(
            "GitHub Personal Access Token with repo scope. Leave blank for local-only publish."
        );
        resetValueGbc(gbc, row);
        mainPanel.add(githubTokenField, gbc);
        row++;

        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 4, 12, 4);
        mainPanel.add(new JSeparator(), gbc);
        row++;

        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        publishButton = new JButton("Publish to Registry (Local + GitHub)");
        publishButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        publishButton.setEnabled(false);
        publishButton.addActionListener(this::publishPlugin);
        mainPanel.add(publishButton, gbc);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JLabel footer = new JLabel("Select a built plugin JAR to get started.");
        footer.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(footer, BorderLayout.SOUTH);
    }

    private void browseJar(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Plugin JAR");
        fc.setFileFilter(
            new javax.swing.filechooser.FileNameExtensionFilter("JAR files (*.jar)", "jar")
        );
        int result = fc.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        selectedJar = fc.getSelectedFile();
        fileLabel.setText(selectedJar.getAbsolutePath());
        fileLabel.setForeground(Color.BLACK);
        extractAndPopulateMetadata();
    }

    private void extractAndPopulateMetadata() {
        if (selectedJar == null) return;
        String entryClasses = null, manifestName = null, manifestVersion = null, manifestAuthor =
            null;
        try (JarFile jar = new JarFile(selectedJar)) {
            Manifest mf = jar.getManifest();
            if (mf != null) {
                Attributes attrs = mf.getMainAttributes();
                entryClasses = attrs.getValue("pluginEntryClasses");
                manifestName = attrs.getValue("Plugin-Name");
                manifestVersion = attrs.getValue("Plugin-Version");
                manifestAuthor = attrs.getValue("Plugin-Author");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to read manifest from {0}", selectedJar.getName());
        }
        extractedEntryClasses = entryClasses;
        if (entryClasses != null) {
            extractedActions = service.extractActionsFromJar(selectedJar, entryClasses);
        } else {
            extractedActions = null;
        }
        manifestEntryClassesLabel.setText(
            entryClasses != null ? entryClasses : "NOT FOUND in MANIFEST.MF"
        );
        manifestNameLabel.setText(manifestName != null ? manifestName : "(not set in manifest)");
        manifestVersionLabel.setText(
            manifestVersion != null ? manifestVersion : "(not set in manifest)"
        );
        manifestAuthorLabel.setText(
            manifestAuthor != null ? manifestAuthor : "(not set in manifest)"
        );
        if (extractedActions != null && !extractedActions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (PluginRegistryEntry.ActionInfo ai : extractedActions) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ai.getName());
            }
            manifestActionsLabel.setText(sb.toString());
        } else {
            manifestActionsLabel.setText("No @Action methods found (or no pluginEntryClasses)");
        }
        String jarName = selectedJar.getName();
        if (jarName.endsWith(".jar")) jarName = jarName.substring(0, jarName.length() - 4);
        if (manifestName != null) {
            displayNameField.setText(manifestName);
        } else {
            String raw = jarName.replaceAll("-\\d+\\.\\d+\\.\\d+.*", "").replace("-", " ");
            raw = toTitleCase(raw);
            displayNameField.setText(raw);
        }
        versionField.setText(manifestVersion != null ? manifestVersion : "1.0.0");
        authorField.setText(manifestAuthor != null ? manifestAuthor : "");
        authorEmailField.setText("");
        minEngineField.setText("3.0.0");
        maxEngineField.setText("");
        objectTypesField.setText("General");
        licenseField.setText("MIT");
        releaseNotesField.setText("Initial release");
        publishButton.setEnabled(true);
    }

    private static String toTitleCase(String s) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : s.toCharArray()) {
            if (c == ' ') {
                nextUpper = true;
                sb.append(c);
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void publishPlugin(ActionEvent e) {
        if (selectedJar == null) return;
        String displayName = displayNameField.getText().trim();
        if (displayName.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Display Name is required.",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        String version = versionField.getText().trim();
        if (version.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Version is required.",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Conflict check against registry
        List<PluginRegistryEntry> existingRegistry = service.fetchRegistry();
        String[] newActionNames = extractedActions != null
            ? extractedActions
                .stream()
                .map(PluginRegistryEntry.ActionInfo::getName)
                .toArray(String[]::new)
            : new String[0];
        boolean hasConflict = false;
        StringBuilder conflictMsg = new StringBuilder();
        if (newActionNames.length > 0) {
            for (PluginRegistryEntry regEntry : existingRegistry) {
                if (
                    regEntry
                        .getName()
                        .equals(displayName.toLowerCase().replaceAll("[^a-z0-9-]", "-"))
                ) continue;
                for (String existingAction : regEntry.getActions()) {
                    for (String newAction : newActionNames) {
                        if (existingAction.equals(newAction)) {
                            conflictMsg
                                .append("  - '")
                                .append(newAction)
                                .append("' already in '")
                                .append(regEntry.getDisplayName())
                                .append("'\n");
                            hasConflict = true;
                        }
                    }
                }
            }
        }
        if (hasConflict) {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Action conflicts detected:\n" +
                conflictMsg +
                "\nPublishing anyway may cause duplicate action errors.\nContinue?",
                "Conflict Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) return;
        }

        PluginRegistryEntry entry = new PluginRegistryEntry();
        entry.setName(displayName.toLowerCase().replaceAll("[^a-z0-9-]", "-"));
        entry.setDisplayName(displayName);
        entry.setDescription(descriptionArea.getText().trim());
        entry.setAuthor(authorField.getText().trim());
        entry.setAuthorEmail(authorEmailField.getText().trim());
        entry.setVersion(version);
        entry.setMinEngineVersion(minEngineField.getText().trim());
        entry.setMaxEngineVersion(maxEngineField.getText().trim());
        String otRaw = objectTypesField.getText().trim();
        if (!otRaw.isEmpty()) entry.setObjectTypes(
            java.util.Arrays.asList(otRaw.split("\\s*,\\s*"))
        );
        entry.setLicense(licenseField.getText().trim());
        entry.setReleaseNotes(releaseNotesField.getText().trim());
        if (extractedActions != null && !extractedActions.isEmpty()) {
            entry.setActions(
                extractedActions
                    .stream()
                    .map(PluginRegistryEntry.ActionInfo::getName)
                    .collect(java.util.stream.Collectors.toList())
            );
        } else {
            String manualActions = JOptionPane.showInputDialog(
                this,
                "No @Action methods detected.\nEnter action names (comma-separated) or leave blank:",
                "Manual Actions",
                JOptionPane.QUESTION_MESSAGE
            );
            if (manualActions != null && !manualActions.trim().isEmpty()) {
                entry.setActions(java.util.Arrays.asList(manualActions.split("\\s*,\\s*")));
            }
        }
        entry.setEntryClasses(extractedEntryClasses);

        try {
            String token = new String(githubTokenField.getPassword());
            boolean remotePublish = token != null && !token.trim().isEmpty();
            if (remotePublish) {
                service.publishPluginToGitHub(selectedJar, entry, token.trim());
            } else {
                service.publishPlugin(selectedJar, entry);
            }
            String msg =
                "Plugin \"" +
                displayName +
                "\" published.\n" +
                "   Local: Resources/plugins/" +
                entry.getName() +
                "/\n";
            if (remotePublish) {
                msg +=
                    "   GitHub: pushed to " +
                    service.getRegistryFilePath() +
                    "\n" +
                    "   Branch: initiative-repo\n";
            } else {
                msg += "   Registry: " + PluginManagerService.getRegistryFilePath() + "\n";
            }
            msg += "\nSwitch to Browse tab and click Refresh to see it.";
            JOptionPane.showMessageDialog(
                this,
                msg,
                "Publish Successful",
                JOptionPane.INFORMATION_MESSAGE
            );
            publishButton.setEnabled(false);
            fileLabel.setText("No file selected");
            fileLabel.setForeground(Color.GRAY);
            selectedJar = null;
            extractedActions = null;
            extractedEntryClasses = null;
            if (onPublishCallback != null) onPublishCallback.run();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to publish plugin", ex);
            JOptionPane.showMessageDialog(
                this,
                "Failed to publish plugin: " + ex.getMessage(),
                "Publish Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
