package com.ing.ide.main.mainui.components.pluginmanager;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Publish tab/panel for Reusable Components -- far simpler than {@link
 * PluginManagerPublishUI}, since a Reusable Component is just data (no build, no manifest, no
 * version), but the same repo+branch-link-primary/local-secondary source input, mirroring the
 * Plugin Marketplace's Publish tab exactly: a component doesn't have to already be sitting in
 * this machine's Shared Reusable Components folder, it can come from any repo+branch too.
 * Used two places: mounted directly as one option in the Marketplace window's combined Publish
 * tab, and opened standalone via {@link #openInDialog} from {@code SharedReusablePopupMenu}'s
 * "Publish to Marketplace..." when a specific local component is already known.
 */
public class ReusableComponentPublishUI extends JPanel {
    private static final Logger LOG = Logger.getLogger(ReusableComponentPublishUI.class.getName());

    private final ReusableComponentService service = new ReusableComponentService();
    private final ReusableComponentCliService cliService = new ReusableComponentCliService();

    private JTextField repoUrlField;
    private JTextField repoBranchField;
    private JTextField repoPathField;
    private JButton fetchButton;
    private JLabel sourceLabel;
    private JComboBox<String> localComponentPicker;
    private File selectedSourceDir;
    private File clonedSourceDir;

    private JTextField displayNameField;
    private JTextArea descriptionArea;
    private JTextField authorField;
    private JTextField authorEmailField;
    private JTextField tagsField;
    private JButton submitButton;
    private JTextArea progressArea;

    public ReusableComponentPublishUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel step1 = new JLabel(
            "Step 1: Point at the component's source (a repo you've already pushed it to)"
        );
        step1.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(step1, gbc);
        row++;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Repo URL:"), gbc);
        repoUrlField = new JTextField();
        repoUrlField.putClientProperty(
            "JTextField.placeholderText",
            "https://github.com/owner/repo.git"
        );
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(repoUrlField, gbc);
        gbc.weightx = 0;
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Branch:"), gbc);
        JPanel branchRow = new JPanel(new BorderLayout(8, 0));
        repoBranchField = new JTextField("main", 12);
        branchRow.add(repoBranchField, BorderLayout.WEST);
        fetchButton = new JButton("Fetch");
        fetchButton.addActionListener(this::fetchFromRepo);
        branchRow.add(fetchButton, BorderLayout.EAST);
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(branchRow, gbc);
        gbc.weightx = 0;
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Path in repo (optional):"), gbc);
        repoPathField = new JTextField();
        repoPathField.putClientProperty(
            "JTextField.placeholderText",
            "leave blank if the component's YAML is at the repo root"
        );
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(repoPathField, gbc);
        gbc.weightx = 0;
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel orLabel = new JLabel(
            "— or, pick one already in this machine's Shared Reusable Components —"
        );
        orLabel.setFont(orLabel.getFont().deriveFont(Font.ITALIC, 11f));
        Color mutedFg = UIManager.getColor("Label.disabledForeground");
        if (mutedFg != null) orLabel.setForeground(mutedFg);
        add(orLabel, gbc);
        row++;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Local component:"), gbc);
        localComponentPicker = new JComboBox<>();
        refreshLocalComponents();
        localComponentPicker.addActionListener(e -> onLocalComponentSelected());
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(localComponentPicker, gbc);
        gbc.weightx = 0;
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Source:"), gbc);
        sourceLabel = new JLabel("No source selected");
        sourceLabel.setForeground(Color.GRAY);
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(sourceLabel, gbc);
        gbc.weightx = 0;
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel step2 = new JLabel("Step 2: Describe it");
        step2.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(step2, gbc);
        row++;
        gbc.gridwidth = 1;

        displayNameField = new JTextField(30);
        row = addField(gbc, row, "Name:", displayNameField);

        authorField = new JTextField(30);
        row = addField(gbc, row, "Author:", authorField);

        authorEmailField = new JTextField(30);
        row = addField(gbc, row, "Author Email:", authorEmailField);

        tagsField = new JTextField(30);
        tagsField.putClientProperty(
            "JTextField.placeholderText",
            "comma-separated, e.g. login, api, retry"
        );
        row = addField(gbc, row, "Tags:", tagsField);

        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Description:"), gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 0.4;
        gbc.fill = GridBagConstraints.BOTH;
        descriptionArea = new JTextArea(5, 40);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        add(new JScrollPane(descriptionArea), gbc);
        row++;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        submitButton = new JButton("Submit for Review");
        submitButton.setFont(submitButton.getFont().deriveFont(Font.BOLD, 14f));
        submitButton.addActionListener(this::submit);
        add(submitButton, gbc);
        row++;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 0.6;
        gbc.fill = GridBagConstraints.BOTH;
        progressArea = new JTextArea(6, 40);
        progressArea.setEditable(false);
        progressArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane progressScroll = new JScrollPane(progressArea);
        progressScroll.setBorder(BorderFactory.createTitledBorder("Progress"));
        add(progressScroll, gbc);
    }

    /** Pre-selects a specific local component -- used by the context-menu launch path, which already knows what to publish. */
    public void preselect(String componentName) {
        refreshLocalComponents();
        localComponentPicker.setSelectedItem(componentName);
        onLocalComponentSelected();
    }

    /** Opens this panel in a standalone, non-modal dialog, pre-selected on one local component. */
    public static void openInDialog(Component parent, String componentName) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(
            owner,
            "Publish Reusable Component",
            Dialog.ModalityType.MODELESS
        );
        ReusableComponentPublishUI ui = new ReusableComponentPublishUI();
        ui.preselect(componentName);
        dialog.setContentPane(ui);
        dialog.setSize(650, 700);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private int addField(GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(field, gbc);
        gbc.weightx = 0;
        return row + 1;
    }

    private void refreshLocalComponents() {
        localComponentPicker.removeAllItems();
        for (String name : service.listLocalSharedComponents()) {
            localComponentPicker.addItem(name);
        }
    }

    private void onLocalComponentSelected() {
        String name = (String) localComponentPicker.getSelectedItem();
        if (name == null) return;
        cleanupClonedSource();
        selectedSourceDir =
            new File(com.ing.datalib.component.Project.getSharedReusableComponentsPath(), name);
        sourceLabel.setText(selectedSourceDir.getAbsolutePath());
        sourceLabel.setForeground(Color.BLACK);
        if (displayNameField.getText().trim().isEmpty()) {
            displayNameField.setText(name);
        }
    }

    private void fetchFromRepo(java.awt.event.ActionEvent e) {
        String repoUrl = repoUrlField.getText().trim();
        if (repoUrl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a repo URL first.");
            return;
        }
        String branch = repoBranchField.getText().trim();
        if (branch.isEmpty()) branch = "main";
        final String branchFinal = branch;
        final String subPath = repoPathField
            .getText()
            .trim()
            .replaceAll("^/+", "")
            .replaceAll("/+$", "");
        final String locationDesc =
            repoUrl + " @ " + branchFinal + (subPath.isEmpty() ? "" : " (" + subPath + ")");

        cleanupClonedSource();
        fetchButton.setEnabled(false);
        sourceLabel.setText("Cloning " + locationDesc + "...");
        sourceLabel.setForeground(Color.GRAY);

        SwingWorker<File, Void> worker = new SwingWorker<File, Void>() {

            @Override
            protected File doInBackground() throws Exception {
                return cliService.cloneSource(repoUrl, branchFinal, s -> {});
            }

            @Override
            protected void done() {
                fetchButton.setEnabled(true);
                File cloned = null;
                try {
                    cloned = get();
                    File componentRoot = subPath.isEmpty() ? cloned : new File(cloned, subPath);
                    if (
                        !subPath.isEmpty() &&
                        !componentRoot
                            .getCanonicalPath()
                            .startsWith(cloned.getCanonicalPath() + File.separator)
                    ) {
                        deleteQuietly(cloned);
                        sourceLabel.setText("No source selected");
                        sourceLabel.setForeground(Color.GRAY);
                        JOptionPane.showMessageDialog(
                            ReusableComponentPublishUI.this,
                            "Path in repo can't point outside the repo: " + subPath
                        );
                        return;
                    }
                    File[] yamlFiles = componentRoot.listFiles(
                        (d, n) ->
                            n.toLowerCase().endsWith(".yaml") || n.toLowerCase().endsWith(".yml")
                    );
                    if (yamlFiles == null || yamlFiles.length == 0) {
                        deleteQuietly(cloned);
                        sourceLabel.setText("No source selected");
                        sourceLabel.setForeground(Color.GRAY);
                        JOptionPane.showMessageDialog(
                            ReusableComponentPublishUI.this,
                            "No .yaml/.yml file found at " +
                            (
                                subPath.isEmpty()
                                    ? "the root of " + repoUrl
                                    : "'" + subPath + "' in " + repoUrl
                            ) +
                            " (branch " +
                            branchFinal +
                            ")."
                        );
                        return;
                    }
                    clonedSourceDir = cloned;
                    selectedSourceDir = componentRoot;
                    localComponentPicker.setSelectedItem(null);
                    sourceLabel.setText(locationDesc);
                    sourceLabel.setForeground(Color.BLACK);
                    if (displayNameField.getText().trim().isEmpty() && !subPath.isEmpty()) {
                        String[] parts = subPath.split("/");
                        displayNameField.setText(parts[parts.length - 1]);
                    }
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Clone failed", ex);
                    sourceLabel.setText("No source selected");
                    sourceLabel.setForeground(Color.GRAY);
                    JOptionPane.showMessageDialog(
                        ReusableComponentPublishUI.this,
                        "Could not fetch that repo:\n" + rootMessage(ex)
                    );
                }
            }
        };
        worker.execute();
    }

    /** Deletes the temp clone from fetchFromRepo(), if any -- never touches a locally-picked Shared component. */
    private void cleanupClonedSource() {
        if (clonedSourceDir != null) {
            deleteQuietly(clonedSourceDir);
            clonedSourceDir = null;
        }
    }

    private void submit(java.awt.event.ActionEvent e) {
        if (selectedSourceDir == null) {
            JOptionPane.showMessageDialog(this, "Pick or fetch a source first.");
            return;
        }
        String displayName = displayNameField.getText().trim();
        if (displayName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.");
            return;
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Description is required.");
            return;
        }

        ReusableComponentEntry entry = new ReusableComponentEntry();
        String slug = displayName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        entry.setName(slug);
        entry.setDisplayName(displayName);
        entry.setDescription(descriptionArea.getText().trim());
        entry.setAuthor(authorField.getText().trim());
        entry.setAuthorEmail(authorEmailField.getText().trim());
        List<String> tags = new ArrayList<>();
        for (String tag : tagsField.getText().split(",")) {
            if (!tag.trim().isEmpty()) tags.add(tag.trim());
        }
        entry.setTags(tags);

        File sourceDirFinal = selectedSourceDir;
        progressArea.setText("");
        submitButton.setEnabled(false);
        SwingWorker<MarketplaceCliService.PrResult, String> worker = new SwingWorker<MarketplaceCliService.PrResult, String>() {
            File stagingDir;
            File registryFile;

            @Override
            protected MarketplaceCliService.PrResult doInBackground() throws Exception {
                publish("Staging submission files...");
                stagingDir = service.stageSubmission(sourceDirFinal, entry);
                publish("Computing updated registry...");
                registryFile = service.buildUpdatedRegistry(entry);
                try {
                    return cliService.submitComponent(
                        stagingDir,
                        registryFile,
                        entry.getName(),
                        "Add reusable component: " + entry.getDisplayName(),
                        "**Component:** " +
                        entry.getDisplayName() +
                        "\n**Author:** " +
                        entry.getAuthor() +
                        "\n\n" +
                        entry.getDescription() +
                        "\n\nSubmitted from INGenious's Reusable Components Marketplace.",
                        this::publish
                    );
                } finally {
                    deleteQuietly(stagingDir);
                    deleteQuietly(registryFile);
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) progressArea.append(line + "\n");
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                try {
                    MarketplaceCliService.PrResult result = get();
                    progressArea.append("Pull request opened: " + result.url + "\n");
                    cleanupClonedSource();
                    int choice = JOptionPane.showConfirmDialog(
                        ReusableComponentPublishUI.this,
                        "Pull request opened:\n" + result.url + "\n\nOpen it in your browser now?",
                        "Submitted for Review",
                        JOptionPane.YES_NO_OPTION
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        try {
                            Desktop.getDesktop().browse(java.net.URI.create(result.url));
                        } catch (Exception ex) {
                            LOG.log(Level.WARNING, "Could not open PR URL", ex);
                        }
                    }
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Submission failed", ex);
                    progressArea.append("Failed: " + rootMessage(ex) + "\n");
                    JOptionPane.showMessageDialog(
                        ReusableComponentPublishUI.this,
                        "Submission failed:\n" + rootMessage(ex),
                        "Submission Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void deleteQuietly(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File c : children) deleteQuietly(c);
        }
        file.delete();
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() != null ? cur.getMessage() : cur.toString();
    }
}
