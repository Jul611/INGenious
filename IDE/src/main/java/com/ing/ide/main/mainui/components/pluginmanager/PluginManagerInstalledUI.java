package com.ing.ide.main.mainui.components.pluginmanager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Installed tab for the Plugin Manager.
 * Shows locally installed plugins with Uninstall buttons.
 */
public class PluginManagerInstalledUI extends JPanel {
    private static final String[] COLUMNS = { "Plugin", "Version", "Actions", "" };
    private static final int[] COL_WIDTHS = { 250, 100, 100, 120 };

    private final PluginManagerService service;
    private final Runnable onUninstallCallback;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<PluginInstalledEntry> installedPlugins;
    private JLabel statusLabel;

    public PluginManagerInstalledUI(PluginManagerService service, Runnable onUninstallCallback) {
        this.service = service;
        this.onUninstallCallback = onUninstallCallback;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel("Installed Plugins");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadData());
        headerPanel.add(refreshButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Table
        tableModel =
            new DefaultTableModel(COLUMNS, 0) {

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

        table = new JTable(tableModel);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        for (int i = 0; i < COL_WIDTHS.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(COL_WIDTHS[i]);
            if (i == 3) {
                table.getColumnModel().getColumn(i).setMaxWidth(COL_WIDTHS[i]);
            }
        }

        // Uninstall button column
        table.getColumnModel().getColumn(3).setCellRenderer(new UninstallButtonRenderer());
        table
            .getColumnModel()
            .getColumn(3)
            .setCellEditor(new UninstallButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("No plugins installed.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                installedPlugins = service.getInstalledPlugins();
                return null;
            }

            @Override
            protected void done() {
                populateTable();
            }
        };
        worker.execute();
    }

    private void populateTable() {
        tableModel.setRowCount(0);
        if (installedPlugins == null || installedPlugins.isEmpty()) {
            statusLabel.setText("No plugins installed.");
            return;
        }

        for (PluginInstalledEntry plugin : installedPlugins) {
            String actions = plugin.getActions() != null
                ? String.valueOf(plugin.getActions().size())
                : "0";
            tableModel.addRow(
                new Object[] { plugin.getDisplayName(), plugin.getVersion(), actions, "Uninstall" }
            );
        }
        statusLabel.setText(installedPlugins.size() + " plugin(s) installed");
    }

    private void uninstallPlugin(int modelRow) {
        if (installedPlugins == null || modelRow < 0 || modelRow >= installedPlugins.size()) {
            return;
        }
        PluginInstalledEntry plugin = installedPlugins.get(modelRow);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to uninstall \"" +
            plugin.getDisplayName() +
            "\"?\n" +
            "The plugin folder will be deleted.",
            "Uninstall Plugin",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            service.uninstallPlugin(plugin.getName());
            loadData();
            if (onUninstallCallback != null) {
                onUninstallCallback.run();
            }
        }
    }

    // Button renderer
    static class UninstallButtonRenderer
        extends JButton
        implements javax.swing.table.TableCellRenderer {

        public UninstallButtonRenderer() {
            setOpaque(true);
            setText("Uninstall");
        }

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            setText("Uninstall");
            return this;
        }
    }

    // Button editor
    class UninstallButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private int currentRow;

        public UninstallButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("Uninstall");
            button.addActionListener(
                (ActionEvent e) -> {
                    int modelRow = table.convertRowIndexToModel(currentRow);
                    uninstallPlugin(modelRow);
                }
            );
        }

        @Override
        public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column
        ) {
            currentRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Uninstall";
        }
    }
}
