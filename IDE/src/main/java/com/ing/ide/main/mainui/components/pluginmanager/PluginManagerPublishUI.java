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
 * "Publish" tab for the Plugin Manager. Plugin authors point at their plugin's
 * Maven source project, review metadata extracted from a local build, select
 * a README.md, and submit — this builds the jar locally to extract metadata,
 * then opens a pull request against the registry repo via git/gh. Nothing is
 * merged directly: a human reviewer approves, then CI builds the real
 * artifact, publishes it to Azure Artifacts, and updates the registry.
 */
public class PluginManagerPublishUI extends JPanel {
    private static final Logger LOG = Logger.getLogger(PluginManagerPublishUI.class.getName());

    private final PluginManagerService service;
    private final PluginRegistryCliService cliService = new PluginRegistryCliService();
    private final Runnable onPublishCallback;

    private JLabel fileLabel;
    private JLabel manifestEntryClassesLabel;
    private JLabel manifestNameLabel;
    private JLabel manifestVersionLabel;
    private JLabel manifestAuthorLabel;
    private JLabel manifestActionsLabel;
    private JLabel readmeFileLabel;
    private JTextArea progressArea;

    private JTextField pluginNameField;
    private JTextField descriptionArea;
    private JTextField authorField;
    private JTextField authorEmailField;
    private JTextField versionField;
    private JTextField minEngineField;
    private JTextField maxEngineField;
    private JButton submitButton;

    private File selectedSourceDir;
    private File builtJar;
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

        // Step 1: Select plugin source
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        JLabel step1 = new JLabel(
            "Step 1: Select your plugin's source project (folder with pom.xml)"
        );
        step1.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(step1, gbc);
        row++;

        resetLabelGbc(gbc, row);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(this::browseSourceDir);
        mainPanel.add(browseButton, gbc);
        resetValueGbc(gbc, row);
        fileLabel = new JLabel("No folder selected");
        fileLabel.setForeground(Color.GRAY);
        mainPanel.add(fileLabel, gbc);
        row++;

        separator(mainPanel, gbc, row++);

        // Manifest review (auto-populated from a local build)
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        JLabel manifestHeader = new JLabel("Manifest metadata (from a local build)");
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

        // Submit button
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        submitButton = new JButton("Submit for Review");
        submitButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitButton.setEnabled(false);
        submitButton.addActionListener(this::submitForReview);
        mainPanel.add(submitButton, gbc);
        row++;

        // Progress narration
        resetLabelGbc(gbc, row);
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        progressArea = new JTextArea(6, 40);
        progressArea.setEditable(false);
        progressArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane progressScroll = new JScrollPane(progressArea);
        progressScroll.setBorder(BorderFactory.createTitledBorder("Progress"));
        mainPanel.add(progressScroll, gbc);

        // Instructions at the bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JTextArea instructions = new JTextArea(
            "Builds your plugin from source and opens a pull request against the registry repo.\n" +
            "A reviewer approves and merges it; CI then builds the real artifact, publishes it to\n" +
            "Azure Artifacts, and updates the registry automatically. No PAT needed — this uses your\n" +
            "existing git/gh sign-in (run 'gh auth login --web' once if you haven't already)."
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

    private void browseSourceDir(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Plugin Source Project");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fc.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File dir = fc.getSelectedFile();
        if (!new File(dir, "pom.xml").exists()) {
            showError(
                "No pom.xml found in " +
                dir.getAbsolutePath() +
                ".\nSelect the root folder of a Maven plugin project."
            );
            return;
        }
        selectedSourceDir = dir;
        builtJar = null;
        fileLabel.setText(selectedSourceDir.getAbsolutePath());
        fileLabel.setForeground(Color.BLACK);
        buildAndPopulateMetadata();
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
        submitButton.setEnabled(
            selectedSourceDir != null &&
            builtJar != null &&
            selectedReadme != null &&
            extractedEntryClasses != null &&
            !extractedEntryClasses.trim().isEmpty()
        );
    }

    private void buildAndPopulateMetadata() {
        if (selectedSourceDir == null) return;
        manifestEntryClassesLabel.setText("Building...");
        manifestNameLabel.setText("-");
        manifestVersionLabel.setText("-");
        manifestAuthorLabel.setText("-");
        manifestActionsLabel.setText("-");
        submitButton.setEnabled(false);

        SwingWorker<File, Void> worker = new SwingWorker<File, Void>() {

            @Override
            protected File doInBackground() throws Exception {
                return cliService.buildLocally(selectedSourceDir, s -> {});
            }

            @Override
            protected void done() {
                try {
                    builtJar = get();
                    populateMetadataFromJar(builtJar);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Local build failed", ex);
                    manifestEntryClassesLabel.setText("BUILD FAILED — see error");
                    showError("Build failed:\n" + rootMessage(ex));
                }
                checkReady();
            }
        };
        worker.execute();
    }

    private void populateMetadataFromJar(File jar) {
        String entryClasses = null, manifestName = null, manifestVersion = null, manifestAuthor =
            null;
        try (JarFile jf = new JarFile(jar)) {
            Manifest mf = jf.getManifest();
            if (mf != null) {
                Attributes attrs = mf.getMainAttributes();
                entryClasses = attrs.getValue("pluginEntryClasses");
                manifestName = attrs.getValue("Plugin-Name");
                manifestVersion = attrs.getValue("Plugin-Version");
                manifestAuthor = attrs.getValue("Plugin-Author");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to read manifest from {0}", jar.getName());
        }
        extractedEntryClasses = entryClasses;
        if (entryClasses != null) {
            extractedActions = service.extractActionsFromJar(jar, entryClasses);
        } else {
            extractedActions = null;
        }
        manifestEntryClassesLabel.setText(
            entryClasses != null ? entryClasses : "NOT FOUND — required!"
        );

        String jarName = jar.getName();
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

    private void submitForReview(ActionEvent e) {
        if (selectedSourceDir == null || builtJar == null) return;

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
                "The build is missing 'pluginEntryClasses' in MANIFEST.MF.\nThis is required for the engine to load the plugin."
            );
            return;
        }

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
        String pluginId = pluginName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        if (newActionNames.length > 0) {
            for (PluginRegistryEntry regEntry : existingRegistry) {
                if (regEntry.getName().equals(pluginId)) continue;
                for (String existingAction : regEntry.getActions()) {
                    for (String newAction : newActionNames) {
                        if (existingAction.equals(newAction)) {
                            conflictMsg
                                .append("  • '")
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
        entry.setName(pluginId);
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

        progressArea.setText("");
        submitButton.setEnabled(false);
        String readmeContentFinal = readmeContent;

        SwingWorker<PluginRegistryCliService.PrResult, String> worker = new SwingWorker<PluginRegistryCliService.PrResult, String>() {
            File stagingDir;

            @Override
            protected PluginRegistryCliService.PrResult doInBackground() throws Exception {
                publish("Staging submission files...");
                stagingDir =
                    service.stagePluginSubmission(selectedSourceDir, entry, readmeContentFinal);
                try {
                    return cliService.submitPlugin(
                        stagingDir,
                        entry.getName(),
                        entry.getVersion(),
                        "Add plugin: " + entry.getDisplayName() + " v" + entry.getVersion(),
                        buildPrBody(entry),
                        this::publish
                    );
                } finally {
                    deleteQuietly(stagingDir);
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) {
                    progressArea.append(line + "\n");
                }
            }

            @Override
            protected void done() {
                try {
                    PluginRegistryCliService.PrResult result = get();
                    progressArea.append("Pull request opened: " + result.url + "\n");
                    try {
                        service.publishPlugin(builtJar, entry);
                    } catch (Exception localEx) {
                        LOG.log(Level.WARNING, "Local staging failed after submission", localEx);
                    }
                    int choice = JOptionPane.showConfirmDialog(
                        PluginManagerPublishUI.this,
                        "Pull request opened:\n" + result.url + "\n\nOpen it in your browser now?",
                        "Submitted for Review",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        try {
                            java.awt.Desktop.getDesktop().browse(java.net.URI.create(result.url));
                        } catch (Exception ex) {
                            LOG.log(Level.WARNING, "Could not open PR URL", ex);
                        }
                    }
                    resetForm();
                    if (onPublishCallback != null) onPublishCallback.run();
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Submission failed", ex);
                    showError("Submission failed: " + rootMessage(ex));
                }
                submitButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    private String buildPrBody(PluginRegistryEntry entry) {
        return (
            "**Plugin:** " +
            entry.getDisplayName() +
            "\n**Version:** " +
            entry.getVersion() +
            "\n**Author:** " +
            entry.getAuthor() +
            "\n\n" +
            (entry.getDescription() == null ? "" : entry.getDescription()) +
            "\n\nSubmitted from INGenious's Plugin Manager."
        );
    }

    private void resetForm() {
        submitButton.setEnabled(false);
        fileLabel.setText("No folder selected");
        fileLabel.setForeground(Color.GRAY);
        readmeFileLabel.setText("No file selected");
        readmeFileLabel.setForeground(Color.GRAY);
        selectedSourceDir = null;
        builtJar = null;
        selectedReadme = null;
        extractedActions = null;
        extractedEntryClasses = null;
        manifestEntryClassesLabel.setText("-");
        manifestNameLabel.setText("-");
        manifestVersionLabel.setText("-");
        manifestAuthorLabel.setText("-");
        manifestActionsLabel.setText("-");
    }

    private static void deleteQuietly(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File c : children) deleteQuietly(c);
        }
        file.delete();
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() != null ? cur.getMessage() : cur.toString();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
}
