package com.ing.ide.main.mainui.components.pluginmanager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Browse tab for the Plugin Manager marketplace.
 * Shows available plugins from the registry with Install buttons.
 */
public class PluginManagerBrowseUI extends JPanel {
    private static final Logger LOG = Logger.getLogger(PluginManagerBrowseUI.class.getName());
    private static final String[] COLUMNS = { "", "Plugin", "Author", "Version", "Actions", "" };
    private static final int[] COL_WIDTHS = { 30, 220, 140, 70, 80, 100 };

    private final PluginManagerService service;
    private final Runnable onInstallCallback;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<PluginRegistryEntry> plugins;
    private JLabel statusLabel;
    private JTextField searchField;

    public PluginManagerBrowseUI(PluginManagerService service, Runnable onInstallCallback) {
        this.service = service;
        this.onInstallCallback = onInstallCallback;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel("Plugin Marketplace");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Search field
        searchField = new JTextField();
        searchField.setToolTipText("Search plugins by name, author, or description");
        searchField.putClientProperty("JTextField.placeholderText", "Search plugins...");
        searchField.addActionListener(e -> filterTable());
        headerPanel.add(searchField, BorderLayout.CENTER);

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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Column widths
        for (int i = 0; i < COL_WIDTHS.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(COL_WIDTHS[i]);
            if (i == 0 || i == 5) {
                table.getColumnModel().getColumn(i).setMaxWidth(COL_WIDTHS[i]);
            }
        }

        // Featured icon renderer
        table
            .getColumnModel()
            .getColumn(0)
            .setCellRenderer(
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                        JTable table,
                        Object value,
                        boolean isSelected,
                        boolean hasFocus,
                        int row,
                        int column
                    ) {
                        JLabel label = new JLabel();
                        if (Boolean.TRUE.equals(value)) {
                            label.setText("\u2B50"); // star emoji for featured
                        }
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        return label;
                    }
                }
            );

        // Install button column
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Action column - center text
        table
            .getColumnModel()
            .getColumn(4)
            .setCellRenderer(
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                        JTable table,
                        Object value,
                        boolean isSelected,
                        boolean hasFocus,
                        int row,
                        int column
                    ) {
                        JLabel label = (JLabel) super.getTableCellRendererComponent(
                            table,
                            value,
                            isSelected,
                            hasFocus,
                            row,
                            column
                        );
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        return label;
                    }
                }
            );

        // Sort by featured first, then name
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Loading plugins...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                plugins = service.fetchRegistry();
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
        if (plugins == null || plugins.isEmpty()) {
            statusLabel.setText("No plugins found in registry.");
            return;
        }

        // Sort: featured first, then by name
        plugins.sort(
            (a, b) -> {
                if (a.isFeatured() != b.isFeatured()) {
                    return a.isFeatured() ? -1 : 1;
                }
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            }
        );

        for (PluginRegistryEntry plugin : plugins) {
            String actionCount = String.valueOf(plugin.getActionCount());
            tableModel.addRow(
                new Object[] {
                    plugin.isFeatured(),
                    plugin.getDisplayName(),
                    plugin.getAuthor(),
                    plugin.getVersion(),
                    actionCount,
                    "Install"
                }
            );
        }
        statusLabel.setText(plugins.size() + " plugin(s) available");
    }

    private void filterTable() {
        String query = searchField.getText().trim().toLowerCase();
        TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
        if (query.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(
                new RowFilter<Object, Object>() {

                    @Override
                    public boolean include(Entry<?, ?> entry) {
                        String name = entry.getStringValue(1).toLowerCase();
                        String author = entry.getStringValue(2).toLowerCase();
                        return name.contains(query) || author.contains(query);
                    }
                }
            );
        }
    }

    private void installPlugin(int modelRow) {
        if (plugins == null || modelRow < 0 || modelRow >= plugins.size()) return;
        PluginRegistryEntry plugin = plugins.get(modelRow);
        try {
            service.downloadPlugin(plugin);
            JOptionPane.showMessageDialog(
                this,
                "Plugin \"" +
                plugin.getDisplayName() +
                "\" installed successfully.\n" +
                "Please restart INGenious for the changes to take effect.",
                "Plugin Installed",
                JOptionPane.INFORMATION_MESSAGE
            );
            if (onInstallCallback != null) {
                onInstallCallback.run();
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to install plugin", e);
            JOptionPane.showMessageDialog(
                this,
                "Failed to install plugin: " + e.getMessage(),
                "Install Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Button renderer for the Install column
    static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);
            setText("Install");
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
            setText("Install");
            return this;
        }
    }

    // Button editor for the Install column
    class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("Install");
            button.addActionListener(
                (ActionEvent e) -> {
                    int modelRow = table.convertRowIndexToModel(currentRow);
                    installPlugin(modelRow);
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
            return "Install";
        }
    }
}
