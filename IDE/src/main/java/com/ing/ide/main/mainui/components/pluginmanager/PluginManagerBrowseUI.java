package com.ing.ide.main.mainui.components.pluginmanager;

import com.ing.ide.main.ui.About;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
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
 * Includes pre-install conflict detection and version compatibility checks.
 */
public class PluginManagerBrowseUI extends JPanel {
    private static final Logger LOG = Logger.getLogger(PluginManagerBrowseUI.class.getName());
    private static final int INSTALL_COL = 5;

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

        searchField = new JTextField();
        searchField.setToolTipText("Search plugins by name or author");
        searchField.addActionListener(e -> filterTable());
        headerPanel.add(searchField, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadData());
        headerPanel.add(refreshButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Table
        tableModel =
            new DefaultTableModel(
                new String[] { "", "Plugin", "Author", "Version", "Actions", "Install" },
                0
            ) {

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // no inline editing; we handle clicks via mouse listener
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
        int[] widths = { 30, 220, 140, 70, 80, 100 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            if (i == 0 || i == 5) {
                table.getColumnModel().getColumn(i).setMaxWidth(widths[i]);
            }
        }

        // Featured star renderer
        table
            .getColumnModel()
            .getColumn(0)
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
                        JLabel l = new JLabel();
                        if (Boolean.TRUE.equals(v)) l.setText("\u2605");
                        l.setHorizontalAlignment(SwingConstants.CENTER);
                        return l;
                    }
                }
            );

        // Action count - centered
        table
            .getColumnModel()
            .getColumn(4)
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

        // Install column - render as button-like
        table.getColumnModel().getColumn(INSTALL_COL).setCellRenderer(new InstallRenderer());

        // Mouse listener for Install column clicks
        table.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    int col = table.columnAtPoint(e.getPoint());
                    int row = table.rowAtPoint(e.getPoint());
                    if (col == INSTALL_COL && row >= 0) {
                        int modelRow = table.convertRowIndexToModel(row);
                        installPlugin(modelRow);
                    }
                }
            }
        );

        // Sorter
        table.setRowSorter(new TableRowSorter<>(tableModel));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Loading plugins...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void loadData() {
        statusLabel.setText("Loading plugins...");
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
            statusLabel.setText(
                "No plugins found. If this is unexpected, run 'gh auth status' and check the " +
                "registry repo in Registry Settings."
            );
            return;
        }

        plugins.sort(
            (a, b) -> {
                if (a.isFeatured() != b.isFeatured()) {
                    return a.isFeatured() ? -1 : 1;
                }
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            }
        );

        for (PluginRegistryEntry plugin : plugins) {
            tableModel.addRow(
                new Object[] {
                    plugin.isFeatured(),
                    plugin.getDisplayName(),
                    plugin.getAuthor(),
                    plugin.getVersion(),
                    String.valueOf(plugin.getActionCount()),
                    "Install"
                }
            );
        }
        statusLabel.setText(plugins.size() + " plugin(s) available");
    }

    private void filterTable() {
        String query = searchField.getText().trim().toLowerCase();
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
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

    /**
     * Checks version compatibility before installing.
     * Returns a warning message if incompatible, null if ok.
     */
    private String checkVersionCompatibility(PluginRegistryEntry plugin) {
        String engineVersion = About.getBuildVersion();
        String minEngine = plugin.getMinEngineVersion();
        String maxEngine = plugin.getMaxEngineVersion();

        if (minEngine != null && !minEngine.isEmpty()) {
            try {
                int minVer = parseVersion(minEngine);
                int engVer = parseVersion(engineVersion);
                if (engVer < minVer) {
                    return (
                        "This plugin requires engine version " +
                        minEngine +
                        " or newer.\nCurrent engine version: " +
                        engineVersion +
                        "\n\nThe plugin may not work correctly."
                    );
                }
            } catch (NumberFormatException e) {
                // Version format not comparable, skip check
            }
        }

        if (maxEngine != null && !maxEngine.isEmpty()) {
            try {
                int maxVer = parseVersion(maxEngine);
                int engVer = parseVersion(engineVersion);
                if (engVer > maxVer) {
                    return (
                        "This plugin is designed for engine version " +
                        maxEngine +
                        " or older.\nCurrent engine version: " +
                        engineVersion +
                        "\n\nThe plugin may not work correctly."
                    );
                }
            } catch (NumberFormatException e) {
                // Version format not comparable, skip check
            }
        }

        return null;
    }

    /**
     * Simple version parser that converts "3.1.0" to an integer for comparison.
     */
    private int parseVersion(String version) {
        if (version == null || version.isEmpty() || version.equals("dev")) return 0;
        String[] parts = version.split("\\.");
        int result = 0;
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try {
                result = result * 1000 + Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return result;
    }

    /**
     * Checks if the plugin's actions conflict with already-installed plugins.
     * Returns a list of conflicting action names, empty if none.
     */
    private List<String> findConflictingActions(PluginRegistryEntry plugin) {
        List<String> conflicts = new ArrayList<>();
        List<PluginInstalledEntry> installed = service.getInstalledPlugins();

        if (plugin.getActions() == null || installed == null || installed.isEmpty()) {
            return conflicts;
        }

        Set<String> existingActions = new HashSet<>();
        for (PluginInstalledEntry installedPlugin : installed) {
            if (installedPlugin.getActions() != null) {
                existingActions.addAll(installedPlugin.getActions());
            }
        }

        for (String action : plugin.getActions()) {
            if (existingActions.contains(action)) {
                conflicts.add(action);
            }
        }

        return conflicts;
    }

    private void installPlugin(int modelRow) {
        if (plugins == null || modelRow < 0 || modelRow >= plugins.size()) return;
        PluginRegistryEntry plugin = plugins.get(modelRow);

        // --- Pre-install checks ---

        // 1. Version compatibility check
        String versionWarning = checkVersionCompatibility(plugin);
        if (versionWarning != null) {
            int choice = JOptionPane.showConfirmDialog(
                this,
                versionWarning + "\n\nDo you want to continue installing?",
                "Version Compatibility Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // 2. Conflict detection
        List<String> conflicts = findConflictingActions(plugin);
        if (!conflicts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The following actions already exist from other installed plugins:\n\n");
            for (String c : conflicts) {
                sb.append("  - ").append(c).append("\n");
            }
            sb.append("\nInstalling this plugin may cause duplicate action errors.\n");
            sb.append("Do you want to continue anyway?");

            int choice = JOptionPane.showConfirmDialog(
                this,
                sb.toString(),
                "Action Conflict Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // 3. Proceed with install
        String previousStatus = statusLabel.getText();
        statusLabel.setText("Installing " + plugin.getDisplayName() + "...");
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground() throws Exception {
                service.downloadPlugin(plugin, this::publish);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    int choice = JOptionPane.showConfirmDialog(
                        PluginManagerBrowseUI.this,
                        "Plugin \"" +
                        plugin.getDisplayName() +
                        "\" installed successfully.\n" +
                        "Restart INGenious now for the changes to take effect?",
                        "Plugin Installed",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        // Trigger restart via frame
                        Container parent = getTopLevelAncestor();
                        if (parent instanceof com.ing.ide.main.mainui.AppMainFrame) {
                            ((com.ing.ide.main.mainui.AppMainFrame) parent).restart();
                        }
                    }
                    if (onInstallCallback != null) {
                        onInstallCallback.run();
                    }
                    statusLabel.setText(previousStatus);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to install plugin", e);
                    JOptionPane.showMessageDialog(
                        PluginManagerBrowseUI.this,
                        "Failed to install plugin: " + rootMessage(e),
                        "Install Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText(previousStatus);
                }
            }
        };
        worker.execute();
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() != null ? cur.getMessage() : cur.toString();
    }

    // Renders the Install cell as a clickable button
    static class InstallRenderer extends JButton implements javax.swing.table.TableCellRenderer {

        public InstallRenderer() {
            setOpaque(true);
            setText("Install");
            setBackground(new Color(0, 120, 215));
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
