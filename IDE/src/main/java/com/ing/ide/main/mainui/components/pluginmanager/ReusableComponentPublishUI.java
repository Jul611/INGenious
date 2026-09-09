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
 * version). Step 1 is just a dropdown of local Shared Reusable Components, no clone/fetch,
 * since the source is always already local. Used two places: mounted directly as one option
 * in the Marketplace window's combined Publish tab, and opened standalone via {@link
 * #openInDialog} from {@code SharedReusablePopupMenu}'s "Publish to Marketplace..." when a
 * specific component is already known.
 */
public class ReusableComponentPublishUI extends JPanel {
    private static final Logger LOG = Logger.getLogger(ReusableComponentPublishUI.class.getName());

    private final ReusableComponentService service = new ReusableComponentService();
    private final ReusableComponentCliService cliService = new ReusableComponentCliService();

    private final JComboBox<String> componentPicker;
    private final JTextField displayNameField;
    private final JTextArea descriptionArea;
    private final JTextField authorField;
    private final JTextField authorEmailField;
    private final JTextField tagsField;
    private final JButton submitButton;
    private final JTextArea progressArea;

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
        add(new JLabel("Step 1: Pick a local Shared Reusable Component to publish"), gbc);
        row++;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Component:"), gbc);
        componentPicker = new JComboBox<>();
        refreshLocalComponents();
        componentPicker.addActionListener(e -> onComponentSelected());
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(componentPicker, gbc);
        gbc.weightx = 0;
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(new JLabel("Step 2: Describe it"), gbc);
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
        componentPicker.setSelectedItem(componentName);
        onComponentSelected();
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
        dialog.setSize(600, 600);
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
        componentPicker.removeAllItems();
        for (String name : service.listLocalSharedComponents()) {
            componentPicker.addItem(name);
        }
    }

    private void onComponentSelected() {
        String name = (String) componentPicker.getSelectedItem();
        if (name != null && displayNameField.getText().trim().isEmpty()) {
            displayNameField.setText(name);
        }
    }

    private void submit(java.awt.event.ActionEvent e) {
        String componentName = (String) componentPicker.getSelectedItem();
        if (componentName == null) {
            JOptionPane.showMessageDialog(this, "Pick a component first.");
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
        String slug = componentName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
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

        File scenarioDir = new File(
            com.ing.datalib.component.Project.getSharedReusableComponentsPath(),
            componentName
        );

        progressArea.setText("");
        submitButton.setEnabled(false);
        SwingWorker<PluginRegistryCliService.PrResult, String> worker = new SwingWorker<PluginRegistryCliService.PrResult, String>() {
            File stagingDir;
            File registryFile;

            @Override
            protected PluginRegistryCliService.PrResult doInBackground() throws Exception {
                publish("Staging submission files...");
                stagingDir = service.stageSubmission(scenarioDir, entry);
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
                    PluginRegistryCliService.PrResult result = get();
                    progressArea.append("Pull request opened: " + result.url + "\n");
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
