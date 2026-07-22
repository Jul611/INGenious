package com.ing.ide.main.mainui.components.pluginmanager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * "Publish" tab for the Plugin Manager.
 * Plugin authors select a built JAR, review metadata, select a README.md,
 * and publish directly to the GitHub registry.
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
    private JLabel readmeFileLabel;

    private JTextField pluginNameField;
    private JTextField descriptionArea;
    private JTextField authorField;
    private JTextField authorEmailField;
    private JTextField versionField;
    private JTextField minEngineField;
    private JTextField maxEngineField;
    private JButton publishDirectButton;

    private File selectedJar;
    private File selectedReadme;
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

        // Step 1: Select JAR
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        JLabel step1 = new JLabel("Step 1: Select a built plugin JAR");
        step1.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(step1, gbc);
        row++;

        resetLabelGbc(gbc, row);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(this::browseJar);
        mainPanel.add(browseButton, gbc);
        resetValueGbc(gbc, row);
        fileLabel = new JLabel("No file selected");
        fileLabel.setForeground(Color.GRAY);
        mainPanel.add(fileLabel, gbc);
        row++;

        separator(mainPanel, gbc, row++);

        // Manifest review (auto-populated)
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        JLabel manifestHeader = new JLabel("Manifest metadata (auto-extracted)");
        manifestHeader.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(manifestHeader, gbc);
        row++;

        row =
            addReadonlyField(
                mainPanel,
                gbc,
                row,
                "Entry Classes:",
                manifestEntryClassesLabel = new JLabel("-")
            );
        row =
            addReadonlyField(
                mainPanel,
                gbc,
                row,
                "Plugin Name:",
                manifestNameLabel = new JLabel("-")
            );
        row =
            addReadonlyField(
                mainPanel,
                gbc,
                row,
                "Version:",
                manifestVersionLabel = new JLabel("-")
            );
        row =
            addReadonlyField(mainPanel, gbc, row, "Author:", manifestAuthorLabel = new JLabel("-"));
        row =
            addReadonlyField(
                mainPanel,
                gbc,
                row,
                "Actions found:",
                manifestActionsLabel = new JLabel("-")
            );

        separator(mainPanel, gbc, row++);

        // Step 2: Registry details
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        JLabel step2 = new JLabel("Step 2: Fill in registry details");
        step2.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(step2, gbc);
        row++;

        pluginNameField = new JTextField(30);
        row = addField(mainPanel, gbc, row, "Plugin Name:", pluginNameField);

        descriptionArea = new JTextField(30);
        row = addField(mainPanel, gbc, row, "Description:", descriptionArea);

        authorField = new JTextField(30);
        row = addField(mainPanel, gbc, row, "Author:", authorField);

        authorEmailField = new JTextField(30);
        row = addField(mainPanel, gbc, row, "Author Email:", authorEmailField);

        versionField = new JTextField(10);
        row = addField(mainPanel, gbc, row, "Version:", versionField);

        minEngineField = new JTextField(10);
        row = addField(mainPanel, gbc, row, "Min Engine:", minEngineField);

        maxEngineField = new JTextField(10);
        row = addField(mainPanel, gbc, row, "Max Engine (blank = latest):", maxEngineField);

        separator(mainPanel, gbc, row++);

        // Step 3: Select README.md
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        JLabel step3 = new JLabel("Step 3: Select a README.md file");
        step3.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(step3, gbc);
        row++;

        resetLabelGbc(gbc, row);
        JButton browseReadmeButton = new JButton("Browse...");
        browseReadmeButton.addActionListener(this::browseReadme);
        mainPanel.add(browseReadmeButton, gbc);
        resetValueGbc(gbc, row);
        readmeFileLabel = new JLabel("No file selected");
        readmeFileLabel.setForeground(Color.GRAY);
        mainPanel.add(readmeFileLabel, gbc);
        row++;

        separator(mainPanel, gbc, row++);

        // Publish button
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        publishDirectButton = new JButton("Publish Directly to Registry");
        publishDirectButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        publishDirectButton.setEnabled(false);
        publishDirectButton.addActionListener(this::publishDirectly);
        mainPanel.add(publishDirectButton, gbc);

        // Instructions at the bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JTextArea instructions = new JTextArea(
            "Publishes a new plugin release to GitHub.\n" +
            "Creates a release, uploads the JAR, and updates the registry.\n" +
            "Requires a PAT with write access, configured in Profile."
        );
        instructions.setEditable(false);
        instructions.setBackground(getBackground());
        instructions.setFont(new Font("SansSerif", Font.PLAIN, 11));
        instructions.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        bottomPanel.add(instructions, BorderLayout.CENTER);

        JPanel outerPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        outerPanel.add(scrollPane, BorderLayout.CENTER);
        outerPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(outerPanel, BorderLayout.CENTER);
    }

    private int addField(
        JPanel panel,
        GridBagConstraints gbc,
        int row,
        String label,
        JTextField field
    ) {
        resetLabelGbc(gbc, row);
        panel.add(new JLabel(label), gbc);
        resetValueGbc(gbc, row);
        panel.add(field, gbc);
        return row + 1;
    }

    private int addReadonlyField(
        JPanel panel,
        GridBagConstraints gbc,
        int row,
        String label,
        JLabel value
    ) {
        resetLabelGbc(gbc, row);
        panel.add(new JLabel(label), gbc);
        resetValueGbc(gbc, row);
        value.setForeground(Color.GRAY);
        panel.add(value, gbc);
        return row + 1;
    }

    private void separator(JPanel panel, GridBagConstraints gbc, int row) {
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 4, 12, 4);
        panel.add(new JSeparator(), gbc);
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
        checkReady();
    }

    private void browseReadme(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select README.md");
        fc.setFileFilter(
            new javax.swing.filechooser.FileNameExtensionFilter("Markdown files (*.md)", "md")
        );
        int result = fc.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        selectedReadme = fc.getSelectedFile();
        readmeFileLabel.setText(selectedReadme.getAbsolutePath());
        readmeFileLabel.setForeground(Color.BLACK);
        checkReady();
    }

    private void checkReady() {
        publishDirectButton.setEnabled(
            selectedJar != null &&
            selectedReadme != null &&
            extractedEntryClasses != null &&
            !extractedEntryClasses.trim().isEmpty()
        );
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
            entryClasses != null ? entryClasses : "NOT FOUND \u2014 required!"
        );

        String jarName = selectedJar.getName();
        if (jarName.endsWith(".jar")) jarName = jarName.substring(0, jarName.length() - 4);
        if (manifestName != null) {
            manifestNameLabel.setText(manifestName);
            pluginNameField.setText(manifestName);
        } else {
            manifestNameLabel.setText("(not set in manifest)");
            String raw = jarName.replaceAll("-\\d+\\.\\d+\\.\\d+.*", "").replace("-", " ");
            pluginNameField.setText(toTitleCase(raw));
        }
        manifestVersionLabel.setText(manifestVersion != null ? manifestVersion : "(not set)");
        manifestAuthorLabel.setText(manifestAuthor != null ? manifestAuthor : "(not set)");
        if (extractedActions != null && !extractedActions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (PluginRegistryEntry.ActionInfo ai : extractedActions) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ai.getName());
            }
            manifestActionsLabel.setText(sb.toString());
        } else {
            manifestActionsLabel.setText("No @Action methods found");
        }

        versionField.setText(manifestVersion != null ? manifestVersion : "1.0.0");
        authorField.setText(manifestAuthor != null ? manifestAuthor : "");
        authorEmailField.setText("");
        minEngineField.setText("3.0.0");
        maxEngineField.setText("");
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
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private void publishDirectly(ActionEvent e) {
        if (selectedJar == null) return;

        // Validate required fields
        String pluginName = pluginNameField.getText().trim();
        if (pluginName.isEmpty()) {
            showError("Plugin Name is required.");
            return;
        }
        String version = versionField.getText().trim();
        if (version.isEmpty()) {
            showError("Version is required.");
            return;
        }
        if (extractedEntryClasses == null || extractedEntryClasses.trim().isEmpty()) {
            showError(
                "The JAR is missing 'pluginEntryClasses' in MANIFEST.MF.\nThis is required for the engine to load the plugin."
            );
            return;
        }

        // Validate README
        String readmeContent = "";
        if (selectedReadme != null && selectedReadme.exists()) {
            try {
                readmeContent = new String(Files.readAllBytes(selectedReadme.toPath()), "UTF-8");
            } catch (IOException ex) {
                showError("Failed to read README file: " + ex.getMessage());
                return;
            }
        }
        int wordCount = readmeContent.trim().split("\\s+").length;
        if (wordCount < 50) {
            showError("README must be at least 50 words. Currently: " + wordCount + " words.");
            return;
        }

        // Check PAT is configured
        String pat = UserConfig.getPublishPat();
        if (pat == null || pat.trim().isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "You haven't configured a GitHub Publishing PAT in your Profile.\n\n" +
                "Click Help to learn how, or set it via Profile (toolbar icon).\n\nOpen Profile now?",
                "PAT Required",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                firePropertyChange("openProfile", null, true);
            }
            return;
        }

        // Conflict check
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
            String pluginId = pluginName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
            for (PluginRegistryEntry regEntry : existingRegistry) {
                if (regEntry.getName().equals(pluginId)) continue;
                for (String existingAction : regEntry.getActions()) {
                    for (String newAction : newActionNames) {
                        if (existingAction.equals(newAction)) {
                            conflictMsg
                                .append("  \u2022 '")
                                .append(newAction)
                                .append("' already used by '")
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
                "Action conflicts detected:\n" + conflictMsg + "\nContinue anyway?",
                "Conflict Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) return;
        }

        // Build registry entry
        PluginRegistryEntry entry = new PluginRegistryEntry();
        entry.setName(pluginName.toLowerCase().replaceAll("[^a-z0-9-]", "-"));
        entry.setDisplayName(pluginName);
        entry.setDescription(descriptionArea.getText().trim());
        entry.setAuthor(authorField.getText().trim());
        entry.setAuthorEmail(authorEmailField.getText().trim());
        entry.setVersion(version);
        entry.setMinEngineVersion(minEngineField.getText().trim());
        entry.setMaxEngineVersion(maxEngineField.getText().trim());
        entry.setObjectTypes(java.util.Collections.singletonList("General"));
        entry.setLicense("MIT");
        entry.setReleaseNotes("");
        if (extractedActions != null && !extractedActions.isEmpty()) {
            entry.setActions(
                extractedActions
                    .stream()
                    .map(PluginRegistryEntry.ActionInfo::getName)
                    .collect(java.util.stream.Collectors.toList())
            );
        }
        entry.setEntryClasses(extractedEntryClasses);

        // Show progress dialog
        JDialog progressDialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Publishing Plugin...",
            Dialog.ModalityType.MODELESS
        );
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        progressPanel.add(
            new JLabel("Creating GitHub release and uploading JAR..."),
            BorderLayout.NORTH
        );
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressDialog.add(progressPanel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setVisible(true);

        // Run publish in background
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                return service.publishDirectly(selectedJar, entry, pat);
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    String downloadUrl = get();
                    try {
                        service.publishPlugin(selectedJar, entry);
                    } catch (Exception localEx) {
                        LOG.log(Level.WARNING, "Local staging failed after publish", localEx);
                    }
                    JOptionPane.showMessageDialog(
                        PluginManagerPublishUI.this,
                        "Plugin published successfully!\n\nRelease created and JAR uploaded.\nDownload URL: " +
                        downloadUrl +
                        "\n\nThe registry has been updated on GitHub.",
                        "Publish Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    publishDirectButton.setEnabled(false);
                    fileLabel.setText("No file selected");
                    fileLabel.setForeground(Color.GRAY);
                    readmeFileLabel.setText("No file selected");
                    readmeFileLabel.setForeground(Color.GRAY);
                    selectedJar = null;
                    selectedReadme = null;
                    extractedActions = null;
                    extractedEntryClasses = null;
                    if (onPublishCallback != null) onPublishCallback.run();
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Direct publish failed", ex);
                    showError("Publish failed: " + ex.getMessage());
                }
                publishDirectButton.setEnabled(true);
            }
        };
        publishDirectButton.setEnabled(false);
        worker.execute();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
}
