package com.ing.ide.main.mainui.components.pluginmanager;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Installed tab for the Plugin Manager.
 * Shows locally installed plugins with Uninstall buttons.
 * Uses MouseListener for button clicks (same reliable pattern as MarketplaceBrowseUI).
 */
public class PluginManagerInstalledUI extends JPanel {
    private static final String[] COLUMNS = { "Plugin", "Version", "Actions", "" };
    private static final int[] COL_WIDTHS = { 200, 100, 100, 120 };
    private static final int UNINSTALL_COL = 3;

    private final PluginManagerService service;
    private final ReusableComponentService reusableComponentService;
    private final Runnable onUninstallCallback;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<PluginInstalledEntry> installedPlugins;
    private JLabel pluginCountLabel;
    private JLabel componentCountLabel;

    public PluginManagerInstalledUI(
        PluginManagerService service,
        ReusableComponentService reusableComponentService,
        Runnable onUninstallCallback
    ) {
        this.service = service;
        this.reusableComponentService = reusableComponentService;
        this.onUninstallCallback = onUninstallCallback;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel("Installed");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton checkUpdatesButton = new JButton("Check for Updates");
        checkUpdatesButton.setToolTipText("Compare installed versions against the registry");
        checkUpdatesButton.addActionListener(e -> checkForUpdates());
        headerPanel.add(checkUpdatesButton, BorderLayout.EAST);

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
            if (i == UNINSTALL_COL) {
                table.getColumnModel().getColumn(i).setMaxWidth(COL_WIDTHS[i]);
            }
        }

        // Action count - centered
        table
            .getColumnModel()
            .getColumn(2)
            .setCellRenderer(
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                        JTable t,
                        Object v,
                        boolean isSel,
                        boolean hasFocus,
                        int row,
                        int col
                    ) {
                        JLabel l = (JLabel) super.getTableCellRendererComponent(
                            t,
                            v,
                            isSel,
                            hasFocus,
                            row,
                            col
                        );
                        l.setHorizontalAlignment(SwingConstants.CENTER);
                        return l;
                    }
                }
            );

        // Uninstall button column - render as button
        table.getColumnModel().getColumn(UNINSTALL_COL).setCellRenderer(new UninstallRenderer());

        // Mouse listener for Uninstall column clicks
        table.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    int col = table.columnAtPoint(e.getPoint());
                    int row = table.rowAtPoint(e.getPoint());
                    if (col == UNINSTALL_COL && row >= 0) {
                        int modelRow = table.convertRowIndexToModel(row);
                        uninstallPlugin(modelRow);
                    }
                }
            }
        );

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Status bar -- two separate counts, nothing else (no paths, no other detail)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        pluginCountLabel = new JLabel("0 plugin(s) installed");
        componentCountLabel = new JLabel("0 reusable component(s)");
        statusPanel.add(pluginCountLabel);
        statusPanel.add(componentCountLabel);
        add(statusPanel, BorderLayout.SOUTH);
    }

    public void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private int componentCount;

            @Override
            protected Void doInBackground() {
                installedPlugins = service.getInstalledPlugins();
                componentCount = reusableComponentService.listLocalSharedComponents().size();
                return null;
            }

            @Override
            protected void done() {
                populateTable();
                componentCountLabel.setText(componentCount + " reusable component(s)");
            }
        };
        worker.execute();
    }

    private void populateTable() {
        tableModel.setRowCount(0);
        if (installedPlugins == null || installedPlugins.isEmpty()) {
            pluginCountLabel.setText("0 plugin(s) installed");
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
        pluginCountLabel.setText(installedPlugins.size() + " plugin(s) installed");
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
            JOptionPane.showMessageDialog(
                this,
                "Plugin \"" +
                plugin.getDisplayName() +
                "\" uninstalled successfully.\n" +
                "Restart INGenious for the changes to take effect.",
                "Plugin Uninstalled",
                JOptionPane.INFORMATION_MESSAGE
            );
            loadData();
            if (onUninstallCallback != null) {
                onUninstallCallback.run();
            }
        }
    }

    private void checkForUpdates() {
        List<PluginInstalledEntry> installed = installedPlugins;
        if (installed == null || installed.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No plugins installed to check updates for.",
                "Check for Updates",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        SwingWorker<List<PluginRegistryEntry>, Void> worker = new SwingWorker<List<PluginRegistryEntry>, Void>() {

            @Override
            protected List<PluginRegistryEntry> doInBackground() {
                return service.checkForUpdates(installed);
            }

            @Override
            protected void done() {
                try {
                    List<PluginRegistryEntry> updates = get();
                    if (updates == null || updates.isEmpty()) {
                        JOptionPane.showMessageDialog(
                            PluginManagerInstalledUI.this,
                            "All installed plugins are up to date.",
                            "Check for Updates",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        StringBuilder sb = new StringBuilder("Updates available for:\n");
                        for (PluginRegistryEntry u : updates) {
                            sb
                                .append("  - ")
                                .append(u.getDisplayName())
                                .append(" (")
                                .append(u.getVersion())
                                .append(")\n");
                        }
                        JOptionPane.showMessageDialog(
                            PluginManagerInstalledUI.this,
                            sb.toString(),
                            "Updates Available",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        PluginManagerInstalledUI.this,
                        "Failed to check for updates: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    // Renders the Uninstall cell as a clickable button
    static class UninstallRenderer extends JButton implements javax.swing.table.TableCellRenderer {

        public UninstallRenderer() {
            setOpaque(true);
            setText("Uninstall");
            setBackground(new Color(200, 50, 50));
            setForeground(Color.WHITE);
        }

        @Override
        public Component getTableCellRendererComponent(
            JTable t,
            Object v,
            boolean isSel,
            boolean hasFocus,
            int row,
            int col
        ) {
            return this;
        }
    }
}
