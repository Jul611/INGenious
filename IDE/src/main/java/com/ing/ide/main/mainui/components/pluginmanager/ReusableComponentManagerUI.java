package com.ing.ide.main.mainui.components.pluginmanager;

import com.ing.ide.main.utils.table.TableColor;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Top-level window for the Reusable Components marketplace -- Browse and Publish tabs, same
 * interaction shape as {@link PluginManagerBrowseUI}/{@link PluginManagerPublishUI} but far
 * simpler fields, since a Reusable Component is just data (no build, no manifest, no version).
 * Reached either standalone (Browse, to install something published) or pre-selected on a
 * specific local component (Publish, from {@code SharedReusablePopupMenu}'s "Publish to
 * Marketplace...").
 */
public class ReusableComponentManagerUI extends JPanel {
    private static final Logger LOG = Logger.getLogger(ReusableComponentManagerUI.class.getName());

    private final ReusableComponentService service = new ReusableComponentService();
    private final ReusableComponentCliService cliService = new ReusableComponentCliService();
    private final BrowsePanel browsePanel;
    private final PublishPanel publishPanel;
    private final JTabbedPane tabs;

    public ReusableComponentManagerUI() {
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        browsePanel = new BrowsePanel();
        publishPanel = new PublishPanel();
        tabs.addTab("Browse", browsePanel);
        tabs.addTab("Publish", publishPanel);
        add(tabs, BorderLayout.CENTER);
        browsePanel.loadData();
    }

    /** Opens straight to Publish with the given local Shared Reusable Component pre-selected. */
    public void selectForPublish(String componentName) {
        tabs.setSelectedComponent(publishPanel);
        publishPanel.preselect(componentName);
    }

    public void refreshBrowse() {
        browsePanel.loadData();
    }

    /** Opens this UI in a standalone, non-modal dialog on the Browse tab. */
    public static void openBrowseDialog(Component parent) {
        open(parent, null);
    }

    /** Opens this UI in a standalone, non-modal dialog, pre-selected for publishing one local component. */
    public static void openPublishDialog(Component parent, String componentName) {
        open(parent, componentName);
    }

    private static void open(Component parent, String preselectedComponentName) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(
            owner,
            "Reusable Components Marketplace",
            Dialog.ModalityType.MODELESS
        );
        ReusableComponentManagerUI ui = new ReusableComponentManagerUI();
        if (preselectedComponentName != null) {
            ui.selectForPublish(preselectedComponentName);
        }
        dialog.setContentPane(ui);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() != null ? cur.getMessage() : cur.toString();
    }

    // ─── Browse ─────────────────────────────────────────────────────

    private class BrowsePanel extends JPanel {
        private final JPanel cardsContainer;
        private final JLabel statusLabel;

        BrowsePanel() {
            setLayout(new BorderLayout());
            cardsContainer = new JPanel();
            cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
            cardsContainer.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            JScrollPane scrollPane = new JScrollPane(cardsContainer);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            add(scrollPane, BorderLayout.CENTER);

            statusLabel = new JLabel("Loading...");
            statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
            add(statusLabel, BorderLayout.SOUTH);

            JButton refreshButton = new JButton("Refresh");
            refreshButton.addActionListener(e -> loadData());
            JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            top.add(refreshButton);
            add(top, BorderLayout.NORTH);
        }

        void loadData() {
            statusLabel.setText("Loading...");
            SwingWorker<List<ReusableComponentEntry>, Void> worker = new SwingWorker<List<ReusableComponentEntry>, Void>() {

                @Override
                protected List<ReusableComponentEntry> doInBackground() {
                    return service.fetchRegistry();
                }

                @Override
                protected void done() {
                    try {
                        populate(get());
                    } catch (Exception e) {
                        statusLabel.setText("Failed to load: " + rootMessage(e));
                    }
                }
            };
            worker.execute();
        }

        private void populate(List<ReusableComponentEntry> entries) {
            cardsContainer.removeAll();
            if (entries == null || entries.isEmpty()) {
                statusLabel.setText("No reusable components published yet.");
            } else {
                for (ReusableComponentEntry entry : entries) {
                    cardsContainer.add(new ComponentCard(entry));
                }
                statusLabel.setText(entries.size() + " component(s) available");
            }
            cardsContainer.revalidate();
            cardsContainer.repaint();
        }

        private class ComponentCard extends JPanel {

            ComponentCard(ReusableComponentEntry entry) {
                setLayout(new BorderLayout(10, 4));
                setBorder(
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                    )
                );

                JPanel textColumn = new JPanel();
                textColumn.setOpaque(false);
                textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));

                JLabel nameLabel = new JLabel(entry.getDisplayName());
                nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
                textColumn.add(nameLabel);

                String author = entry.getAuthor() != null && !entry.getAuthor().isEmpty()
                    ? entry.getAuthor()
                    : "Unknown author";
                JLabel metaLabel = new JLabel(author);
                metaLabel.setFont(metaLabel.getFont().deriveFont(11f));
                textColumn.add(metaLabel);

                if (entry.getDescription() != null && !entry.getDescription().isEmpty()) {
                    JTextArea description = new JTextArea(entry.getDescription());
                    description.setEditable(false);
                    description.setLineWrap(true);
                    description.setWrapStyleWord(true);
                    description.setOpaque(false);
                    description.setFont(description.getFont().deriveFont(12f));
                    textColumn.add(Box.createVerticalStrut(6));
                    textColumn.add(description);
                }

                if (entry.getTags() != null && !entry.getTags().isEmpty()) {
                    JLabel tagsLabel = new JLabel(
                        "<html><small>" + String.join(", ", entry.getTags()) + "</small></html>"
                    );
                    textColumn.add(Box.createVerticalStrut(4));
                    textColumn.add(tagsLabel);
                }

                add(textColumn, BorderLayout.CENTER);

                JButton installButton = new JButton("Install");
                installButton.setBackground(TableColor.ING_PURPLE);
                installButton.setForeground(Color.WHITE);
                installButton.setOpaque(true);
                installButton.setBorderPainted(false);
                installButton.addActionListener(e -> install(entry, installButton));
                add(installButton, BorderLayout.EAST);
            }
        }

        private void install(ReusableComponentEntry entry, JButton button) {
            button.setEnabled(false);
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {
                    service.installComponent(entry, s -> {});
                    return null;
                }

                @Override
                protected void done() {
                    button.setEnabled(true);
                    try {
                        get();
                        JOptionPane.showMessageDialog(
                            BrowsePanel.this,
                            "Installed \"" +
                            entry.getDisplayName() +
                            "\" into your Shared Reusable Components.",
                            "Installed",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Install failed", e);
                        JOptionPane.showMessageDialog(
                            BrowsePanel.this,
                            "Install failed:\n" + rootMessage(e),
                            "Install Failed",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };
            worker.execute();
        }
    }

    // ─── Publish ────────────────────────────────────────────────────

    private class PublishPanel extends JPanel {
        private final JComboBox<String> componentPicker;
        private final JTextField displayNameField;
        private final JTextArea descriptionArea;
        private final JTextField authorField;
        private final JTextField authorEmailField;
        private final JTextField tagsField;
        private final JButton submitButton;
        private final JTextArea progressArea;

        PublishPanel() {
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

        void preselect(String componentName) {
            refreshLocalComponents();
            componentPicker.setSelectedItem(componentName);
            onComponentSelected();
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
                            PublishPanel.this,
                            "Pull request opened:\n" +
                            result.url +
                            "\n\nOpen it in your browser now?",
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
                            PublishPanel.this,
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
    }
}
