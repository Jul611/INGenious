package com.ing.ide.main.mainui.components.pluginmanager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Modal dialog for pointing the Plugin Marketplace at a registry repo and
 * Azure Artifacts feed. This is the one place all of that lives — swapping
 * from a personal setup to the real company org/feed later means changing
 * the values here, not touching code.
 */
public final class PluginRegistrySettingsDialog {

    private PluginRegistrySettingsDialog() {}

    public static void show(Component parent, PluginRegistryConfig config) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(
            owner,
            "Plugin Registry Settings",
            JDialog.ModalityType.APPLICATION_MODAL
        );

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        JLabel repoHeader = new JLabel("GitHub registry repo");
        repoHeader.setFont(repoHeader.getFont().deriveFont(Font.BOLD));
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        form.add(repoHeader, c);
        c.gridwidth = 1;

        JTextField repoField = new JTextField(config.getRegistryRepo(), 28);
        row = addField(form, c, row, "Repo (owner/repo):", repoField);
        JTextField branchField = new JTextField(config.getRegistryBranch(), 28);
        row = addField(form, c, row, "Branch:", branchField);
        JTextField pathField = new JTextField(config.getRegistryPath(), 28);
        row = addField(form, c, row, "registry.json path:", pathField);
        JTextField groupIdField = new JTextField(config.getMavenGroupId(), 28);
        row = addField(form, c, row, "Plugin Maven groupId:", groupIdField);

        JLabel adoHeader = new JLabel("Azure DevOps Artifacts feed");
        adoHeader.setFont(adoHeader.getFont().deriveFont(Font.BOLD));
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.insets = new Insets(14, 4, 4, 4);
        form.add(adoHeader, c);
        c.gridwidth = 1;
        c.insets = new Insets(4, 4, 4, 4);

        JTextField adoOrgField = new JTextField(config.getAdoOrganization(), 28);
        row = addField(form, c, row, "Organization:", adoOrgField);
        JTextField adoProjectField = new JTextField(config.getAdoProject(), 28);
        row = addField(form, c, row, "Project (blank if org-scoped):", adoProjectField);
        JTextField adoFeedField = new JTextField(config.getAdoFeedName(), 28);
        row = addField(form, c, row, "Feed name:", adoFeedField);
        JTextField adoServerIdField = new JTextField(config.getAdoFeedServerId(), 28);
        row =
            addField(
                form,
                c,
                row,
                "Maven <server> id (must match ~/.m2/settings.xml):",
                adoServerIdField
            );

        JLabel statusHeader = new JLabel("Local machine status");
        statusHeader.setFont(statusHeader.getFont().deriveFont(Font.BOLD));
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.insets = new Insets(14, 4, 4, 4);
        form.add(statusHeader, c);
        c.insets = new Insets(4, 4, 4, 4);

        JLabel statusLabel = new JLabel("Checking git, gh, and mvn...");
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        form.add(statusLabel, c);

        JButton recheckButton = new JButton("Re-check");
        JButton saveButton = new JButton("Save");
        JButton closeButton = new JButton("Close");

        recheckButton.addActionListener(
            e -> runStatusCheck(statusLabel, recheckButton, adoServerIdField.getText().trim())
        );
        saveButton.addActionListener(
            e -> {
                config.setRegistryRepo(repoField.getText().trim());
                config.setRegistryBranch(branchField.getText().trim());
                config.setRegistryPath(pathField.getText().trim());
                config.setMavenGroupId(groupIdField.getText().trim());
                config.setAdoOrganization(adoOrgField.getText().trim());
                config.setAdoProject(adoProjectField.getText().trim());
                config.setAdoFeedName(adoFeedField.getText().trim());
                config.setAdoFeedServerId(adoServerIdField.getText().trim());
                dialog.dispose();
            }
        );
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel();
        buttons.add(recheckButton);
        buttons.add(saveButton);
        buttons.add(closeButton);

        JPanel content = new JPanel(new BorderLayout());
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);

        runStatusCheck(statusLabel, recheckButton, config.getAdoFeedServerId());
        dialog.setVisible(true);
    }

    private static int addField(
        JPanel panel,
        GridBagConstraints c,
        int row,
        String label,
        JTextField field
    ) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
        return row + 1;
    }

    private static void runStatusCheck(
        JLabel statusLabel,
        JButton recheckButton,
        String adoServerId
    ) {
        recheckButton.setEnabled(false);
        statusLabel.setText("Checking git, gh, and mvn...");

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() {
                PluginRegistryCliService cli = new PluginRegistryCliService();
                PluginRegistryCliService.ToolStatus git = cli.checkGitInstalled();
                PluginRegistryCliService.ToolStatus gh = cli.checkGhInstalled();
                PluginRegistryCliService.ToolStatus mvn = cli.checkMvnInstalled();
                PluginRegistryCliService.AuthStatus auth = gh.installed
                    ? cli.checkGhAuthStatus()
                    : null;
                boolean adoConfigured = cli.isAdoMavenServerConfigured(adoServerId);

                StringBuilder html = new StringBuilder("<html>");
                html.append(toolLine("git", git));
                html.append(toolLine("gh", gh));
                html.append(toolLine("mvn", mvn));
                if (auth != null) {
                    html.append(
                        auth.authenticated
                            ? "gh: signed in as " + auth.username + "<br>"
                            : "gh: not signed in — run 'gh auth login --web'<br>"
                    );
                }
                html.append(
                    adoConfigured
                        ? "~/.m2/settings.xml: has a matching &lt;server&gt; entry"
                        : "~/.m2/settings.xml: no matching &lt;server&gt; entry yet — needed to install/publish"
                );
                html.append("</html>");
                return html.toString();
            }

            @Override
            protected void done() {
                try {
                    statusLabel.setText(get());
                } catch (Exception e) {
                    statusLabel.setText("Status check failed: " + e.getMessage());
                }
                recheckButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    private static String toolLine(String name, PluginRegistryCliService.ToolStatus status) {
        return status.installed
            ? name + ": " + status.version + "<br>"
            : name + ": not found on PATH<br>";
    }
}
