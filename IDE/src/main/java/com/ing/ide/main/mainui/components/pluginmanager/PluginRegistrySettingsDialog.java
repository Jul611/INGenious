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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Modal dialog for the one thing a Plugin Marketplace user actually has to
 * provide: their own ADO PAT. There's exactly one real marketplace, so where
 * the registry repo and Azure Artifacts feed live ({@link PluginRegistryConfig})
 * is a fixed constant, not something shown or configured here. The PAT field
 * is write-only, writing straight into the user's real ~/.m2/settings.xml
 * (via PluginRegistryCliService.saveAdoCredential) so nobody has to hand-edit
 * that file, and it's never redisplayed once saved.
 */
public final class PluginRegistrySettingsDialog {

    private PluginRegistrySettingsDialog() {}

    public static void show(Component parent, PluginRegistryConfig config) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(
            owner,
            "ADO Credential Setup",
            JDialog.ModalityType.APPLICATION_MODAL
        );

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        JPasswordField adoPatField = new JPasswordField(28);
        row =
            addField(
                form,
                c,
                row,
                "ADO PAT (Packaging: Read) -- leave blank to keep current:",
                adoPatField
            );

        JLabel patHint = new JLabel(
            "<html><small>Pasting a PAT here writes it into ~/.m2/settings.xml for you --" +
            " nothing to hand-edit.</small></html>"
        );
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        form.add(patHint, c);
        c.gridwidth = 1;

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
        JButton saveButton = new JButton("Save PAT");
        JButton closeButton = new JButton("Close");

        recheckButton.addActionListener(
            e -> runStatusCheck(statusLabel, recheckButton, config.getAdoFeedServerId())
        );
        saveButton.addActionListener(
            e -> {
                char[] patChars = adoPatField.getPassword();
                String pat = new String(patChars).trim();
                java.util.Arrays.fill(patChars, ' ');
                if (!pat.isEmpty()) {
                    try {
                        new PluginRegistryCliService()
                        .saveAdoCredential(config.getAdoFeedServerId(), pat);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                            dialog,
                            "Could not update ~/.m2/settings.xml:\n" + ex.getMessage(),
                            "ADO Credential Not Saved",
                            JOptionPane.WARNING_MESSAGE
                        );
                    }
                }
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
        JPasswordField field
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
                            : "gh: not signed in -- run 'gh auth login --web'<br>"
                    );
                }
                html.append(
                    adoConfigured
                        ? "ADO credential: configured in ~/.m2/settings.xml"
                        : "ADO credential: not set yet -- paste a PAT above and Save"
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
