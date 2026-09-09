package com.ing.ide.main.mainui.components.pluginmanager;

import com.ing.ide.main.ui.About;
import com.ing.ide.main.utils.table.TableColor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.html.HTMLDocument;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Browse tab for the marketplace -- plugins and Reusable Components together in one
 * expandable card list, filterable by type (each has a completely different install
 * mechanism -- Maven artifact resolution vs. a direct file copy -- so they stay two card
 * shapes even though they share one list and one search box). Includes pre-install conflict
 * detection and version compatibility checks for plugins.
 */
public class PluginManagerBrowseUI extends JPanel {
    private static final Logger LOG = Logger.getLogger(PluginManagerBrowseUI.class.getName());
    private static final String FILTER_ALL = "All";
    private static final String FILTER_PLUGINS = "Plugins";
    private static final String FILTER_COMPONENTS = "Reusable Components";

    private final PluginManagerService service;
    private final ReusableComponentService reusableComponentService;
    private final Runnable onInstallCallback;
    private List<PluginRegistryEntry> plugins;
    private List<ReusableComponentEntry> components;
    private final List<PluginCard> pluginCardList = new ArrayList<>();
    private final List<ReusableComponentCard> componentCardList = new ArrayList<>();
    private JPanel cardsContainer;
    private JLabel statusLabel;
    private JTextField searchField;
    private JComboBox<String> typeFilter;

    public PluginManagerBrowseUI(
        PluginManagerService service,
        ReusableComponentService reusableComponentService,
        Runnable onInstallCallback
    ) {
        this.service = service;
        this.reusableComponentService = reusableComponentService;
        this.onInstallCallback = onInstallCallback;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));

        JLabel titleLabel = new JLabel("Marketplace");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or author...");
        searchField
            .getDocument()
            .addDocumentListener(
                new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        filterCards();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        filterCards();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        filterCards();
                    }
                }
            );
        headerPanel.add(searchField, BorderLayout.CENTER);

        JPanel eastControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        typeFilter =
            new JComboBox<>(new String[] { FILTER_ALL, FILTER_PLUGINS, FILTER_COMPONENTS });
        typeFilter.addActionListener(e -> filterCards());
        eastControls.add(typeFilter);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadData());
        eastControls.add(refreshButton);
        headerPanel.add(eastControls, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Card list
        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setBackground(UIManager.getColor("Panel.background"));
        cardsContainer.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UIManager.getColor("Panel.background"));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Loading...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        Color disabledFg = UIManager.getColor("Label.disabledForeground");
        if (disabledFg != null) statusLabel.setForeground(disabledFg);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void loadData() {
        statusLabel.setText("Loading...");
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                plugins = service.fetchRegistry();
                components = reusableComponentService.fetchRegistry();
                return null;
            }

            @Override
            protected void done() {
                populateCards();
            }
        };
        worker.execute();
    }

    /** Just enough of a common shape to sort plugins and Reusable Components into one list together. */
    private static final class BrowseItem {
        final String displayName;
        final boolean featured;
        final Object entry;

        BrowseItem(String displayName, boolean featured, Object entry) {
            this.displayName = displayName;
            this.featured = featured;
            this.entry = entry;
        }
    }

    private void populateCards() {
        cardsContainer.removeAll();
        pluginCardList.clear();
        componentCardList.clear();

        List<BrowseItem> items = new ArrayList<>();
        if (plugins != null) {
            for (PluginRegistryEntry p : plugins) {
                items.add(new BrowseItem(p.getDisplayName(), p.isFeatured(), p));
            }
        }
        if (components != null) {
            for (ReusableComponentEntry c : components) {
                items.add(new BrowseItem(c.getDisplayName(), c.isFeatured(), c));
            }
        }

        if (items.isEmpty()) {
            statusLabel.setText(
                "Nothing published yet. If this is unexpected, run 'gh auth status' -- " +
                "if that's fine too, the marketplace may be misconfigured; contact your administrator."
            );
            cardsContainer.revalidate();
            cardsContainer.repaint();
            return;
        }

        items.sort(
            (a, b) -> {
                if (a.featured != b.featured) return a.featured ? -1 : 1;
                return a.displayName.compareToIgnoreCase(b.displayName);
            }
        );

        for (BrowseItem item : items) {
            if (item.entry instanceof PluginRegistryEntry) {
                PluginCard card = new PluginCard((PluginRegistryEntry) item.entry);
                pluginCardList.add(card);
                cardsContainer.add(card);
            } else {
                ReusableComponentCard card = new ReusableComponentCard(
                    (ReusableComponentEntry) item.entry
                );
                componentCardList.add(card);
                cardsContainer.add(card);
            }
        }
        statusLabel.setText(items.size() + " item(s) available");
        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private void filterCards() {
        String query = searchField.getText().trim().toLowerCase();
        String type = (String) typeFilter.getSelectedItem();
        boolean showPlugins = !FILTER_COMPONENTS.equals(type);
        boolean showComponents = !FILTER_PLUGINS.equals(type);

        for (PluginCard card : pluginCardList) {
            boolean matches =
                showPlugins &&
                (
                    query.isEmpty() ||
                    card.plugin.getDisplayName().toLowerCase().contains(query) ||
                    (
                        card.plugin.getAuthor() != null &&
                        card.plugin.getAuthor().toLowerCase().contains(query)
                    )
                );
            card.setVisible(matches);
        }
        for (ReusableComponentCard card : componentCardList) {
            boolean matches =
                showComponents &&
                (
                    query.isEmpty() ||
                    card.component.getDisplayName().toLowerCase().contains(query) ||
                    (
                        card.component.getAuthor() != null &&
                        card.component.getAuthor().toLowerCase().contains(query)
                    )
                );
            card.setVisible(matches);
        }
        cardsContainer.revalidate();
        cardsContainer.repaint();
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

    private void installPlugin(PluginRegistryEntry plugin) {
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

    private void installComponent(ReusableComponentEntry entry, JButton button) {
        button.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                reusableComponentService.installComponent(entry, s -> {});
                return null;
            }

            @Override
            protected void done() {
                button.setEnabled(true);
                try {
                    get();
                    JOptionPane.showMessageDialog(
                        PluginManagerBrowseUI.this,
                        "Installed \"" +
                        entry.getDisplayName() +
                        "\" into your Shared Reusable Components.",
                        "Installed",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Component install failed", e);
                    JOptionPane.showMessageDialog(
                        PluginManagerBrowseUI.this,
                        "Install failed:\n" + rootMessage(e),
                        "Install Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
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

    /** One plugin's row in the list: a collapsed summary, expandable to show more detail + README. */
    private class PluginCard extends JPanel {
        private final PluginRegistryEntry plugin;
        private final JLabel chevron;
        private final JPanel detailPanel;
        private final JEditorPane readmeArea;
        private boolean expanded = false;
        private boolean readmeRequested = false;

        PluginCard(PluginRegistryEntry plugin) {
            this.plugin = plugin;
            setLayout(new BorderLayout());
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBackground(UIManager.getColor("Panel.background"));
            Color border = UIManager.getColor("Component.borderColor");
            setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(
                        0,
                        0,
                        1,
                        0,
                        border != null ? border : Color.GRAY
                    ),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
                )
            );

            chevron = new JLabel("▸"); // ▸
            Color disabledFg = UIManager.getColor("Label.disabledForeground");
            if (disabledFg != null) chevron.setForeground(disabledFg);
            chevron.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

            add(buildSummaryRow(), BorderLayout.NORTH);

            detailPanel = buildDetailPanel();
            readmeArea = new JEditorPane();
            readmeArea.setContentType("text/html");
            readmeArea.setEditable(false);
            readmeArea.setOpaque(false);
            styleReadmeArea();

            detailPanel.setVisible(false);
            add(detailPanel, BorderLayout.CENTER);

            MouseAdapter toggle = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    setExpanded(!expanded);
                }
            };
            addMouseListener(toggle);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        // Stretch to the container's width but never grow taller than the
        // content actually needs -- recomputed on every layout pass, since
        // expanding/collapsing changes the preferred height.
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        private JLabel buildTypeBadge(String text, Color fg) {
            JLabel badge = new JLabel(text);
            badge.setFont(badge.getFont().deriveFont(Font.BOLD, 10f));
            badge.setForeground(fg);
            badge.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
            return badge;
        }

        private JPanel buildSummaryRow() {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);

            JPanel titleColumn = new JPanel();
            titleColumn.setOpaque(false);
            titleColumn.setLayout(new BoxLayout(titleColumn, BoxLayout.Y_AXIS));

            JPanel titleLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titleLine.setOpaque(false);
            titleLine.add(chevron);
            titleLine.add(buildTypeBadge("PLUGIN", TableColor.ING_PURPLE));
            if (plugin.isFeatured()) {
                JLabel star = new JLabel("★ ");
                star.setForeground(TableColor.ING_ORANGE);
                titleLine.add(star);
            }
            JLabel nameLabel = new JLabel(plugin.getDisplayName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
            titleLine.add(nameLabel);
            titleColumn.add(titleLine);

            String author = plugin.getAuthor() != null && !plugin.getAuthor().isEmpty()
                ? plugin.getAuthor()
                : "Unknown author";
            int actionCount = plugin.getActionCount();
            JLabel metaLabel = new JLabel(
                author +
                "  ·  v" +
                plugin.getVersion() +
                "  ·  " +
                actionCount +
                (actionCount == 1 ? " action" : " actions")
            );
            metaLabel.setFont(metaLabel.getFont().deriveFont(11f));
            Color disabledFg = UIManager.getColor("Label.disabledForeground");
            if (disabledFg != null) metaLabel.setForeground(disabledFg);
            metaLabel.setBorder(BorderFactory.createEmptyBorder(2, 16, 0, 0));
            titleColumn.add(metaLabel);

            row.add(titleColumn, BorderLayout.CENTER);
            row.add(buildInstallButton(), BorderLayout.EAST);
            return row;
        }

        private JButton buildInstallButton() {
            JButton installButton = new JButton("Install");
            installButton.setBackground(TableColor.ING_PURPLE);
            installButton.setForeground(Color.WHITE);
            installButton.setFocusPainted(false);
            installButton.setOpaque(true);
            installButton.setBorderPainted(false);
            installButton.addActionListener(e -> installPlugin(plugin));
            return installButton;
        }

        private JPanel buildDetailPanel() {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 16, 0, 0));

            if (plugin.getDescription() != null && !plugin.getDescription().isEmpty()) {
                JTextArea description = new JTextArea(plugin.getDescription());
                description.setEditable(false);
                description.setLineWrap(true);
                description.setWrapStyleWord(true);
                description.setOpaque(false);
                description.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(description);
                panel.add(Box.createVerticalStrut(8));
            }

            StringBuilder meta = new StringBuilder();
            if (plugin.getMinEngineVersion() != null && !plugin.getMinEngineVersion().isEmpty()) {
                meta.append("Min engine: ").append(plugin.getMinEngineVersion()).append("   ");
            }
            if (plugin.getMaxEngineVersion() != null && !plugin.getMaxEngineVersion().isEmpty()) {
                meta.append("Max engine: ").append(plugin.getMaxEngineVersion());
            }
            if (meta.length() > 0) {
                JLabel metaDetail = new JLabel(meta.toString().trim());
                metaDetail.setFont(metaDetail.getFont().deriveFont(11f));
                metaDetail.setAlignmentX(Component.LEFT_ALIGNMENT);
                Color disabledFg = UIManager.getColor("Label.disabledForeground");
                if (disabledFg != null) metaDetail.setForeground(disabledFg);
                panel.add(metaDetail);
                panel.add(Box.createVerticalStrut(8));
            }

            if (plugin.getActions() != null && !plugin.getActions().isEmpty()) {
                JLabel actionsHeader = new JLabel("Actions:");
                actionsHeader.setFont(actionsHeader.getFont().deriveFont(Font.BOLD, 11f));
                actionsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(actionsHeader);
                JLabel actionsList = new JLabel(
                    "<html>" + String.join(", ", plugin.getActions()) + "</html>"
                );
                actionsList.setFont(actionsList.getFont().deriveFont(11f));
                actionsList.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(actionsList);
                panel.add(Box.createVerticalStrut(8));
            }

            JLabel readmeHeader = new JLabel("README:");
            readmeHeader.setFont(readmeHeader.getFont().deriveFont(Font.BOLD, 11f));
            readmeHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(readmeHeader);

            return panel;
        }

        private void setExpanded(boolean expand) {
            expanded = expand;
            chevron.setText(expanded ? "▾" : "▸"); // ▾ : ▸
            detailPanel.setVisible(expanded);
            if (expanded && !readmeRequested) {
                readmeRequested = true;
                loadReadmeAsync();
            }
            revalidate();
            repaint();
            cardsContainer.revalidate();
            cardsContainer.repaint();
        }

        /** Keeps the HTML view's font/color matching the current Swing theme, since JEditorPane doesn't pick that up on its own. */
        private void styleReadmeArea() {
            Font base = readmeArea.getFont();
            Color fg = UIManager.getColor("Label.foreground");
            HTMLDocument doc = (HTMLDocument) readmeArea.getDocument();
            doc
                .getStyleSheet()
                .addRule(
                    "body { font-family: " +
                    base.getFamily() +
                    "; font-size: " +
                    base.getSize() +
                    "pt; color: " +
                    (fg != null ? String.format("#%06x", fg.getRGB() & 0xFFFFFF) : "inherit") +
                    "; } p { margin: 4px 0; } h1, h2, h3 { margin: 8px 0 4px; }"
                );
        }

        private void loadReadmeAsync() {
            readmeArea.setText("Loading README...");
            detailPanel.add(readmeArea);
            SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

                @Override
                protected String doInBackground() {
                    return service.fetchReadme(plugin);
                }

                @Override
                protected void done() {
                    try {
                        String readme = get();
                        if (readme != null && !readme.isEmpty()) {
                            Parser parser = Parser.builder().build();
                            HtmlRenderer renderer = HtmlRenderer.builder().build();
                            readmeArea.setText(renderer.render(parser.parse(readme)));
                            styleReadmeArea();
                        } else {
                            readmeArea.setText("No README available for this plugin.");
                        }
                    } catch (Exception e) {
                        readmeArea.setText("Couldn't load README: " + rootMessage(e));
                    }
                    detailPanel.revalidate();
                    detailPanel.repaint();
                    revalidate();
                    repaint();
                    cardsContainer.revalidate();
                    cardsContainer.repaint();
                }
            };
            worker.execute();
        }
    }

    /** One Reusable Component's row -- simpler than a plugin card, no build/manifest/README fetch, just the metadata already in the registry. */
    private class ReusableComponentCard extends JPanel {
        private final ReusableComponentEntry component;

        ReusableComponentCard(ReusableComponentEntry component) {
            this.component = component;
            setLayout(new BorderLayout(10, 4));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBackground(UIManager.getColor("Panel.background"));
            Color border = UIManager.getColor("Component.borderColor");
            setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(
                        0,
                        0,
                        1,
                        0,
                        border != null ? border : Color.GRAY
                    ),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
                )
            );

            JPanel textColumn = new JPanel();
            textColumn.setOpaque(false);
            textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));

            JPanel titleLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titleLine.setOpaque(false);
            JLabel badge = new JLabel("REUSABLE COMPONENT");
            badge.setFont(badge.getFont().deriveFont(Font.BOLD, 10f));
            badge.setForeground(TableColor.ING_ORANGE);
            badge.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
            titleLine.add(badge);
            if (component.isFeatured()) {
                JLabel star = new JLabel("★ ");
                star.setForeground(TableColor.ING_ORANGE);
                titleLine.add(star);
            }
            JLabel nameLabel = new JLabel(component.getDisplayName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
            titleLine.add(nameLabel);
            textColumn.add(titleLine);

            String author = component.getAuthor() != null && !component.getAuthor().isEmpty()
                ? component.getAuthor()
                : "Unknown author";
            JLabel metaLabel = new JLabel(author);
            metaLabel.setFont(metaLabel.getFont().deriveFont(11f));
            Color disabledFg = UIManager.getColor("Label.disabledForeground");
            if (disabledFg != null) metaLabel.setForeground(disabledFg);
            metaLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            textColumn.add(metaLabel);

            if (component.getDescription() != null && !component.getDescription().isEmpty()) {
                JTextArea description = new JTextArea(component.getDescription());
                description.setEditable(false);
                description.setLineWrap(true);
                description.setWrapStyleWord(true);
                description.setOpaque(false);
                description.setFont(description.getFont().deriveFont(12f));
                description.setAlignmentX(Component.LEFT_ALIGNMENT);
                textColumn.add(Box.createVerticalStrut(6));
                textColumn.add(description);
            }

            if (component.getTags() != null && !component.getTags().isEmpty()) {
                JLabel tagsLabel = new JLabel(
                    "<html><small>" + String.join(", ", component.getTags()) + "</small></html>"
                );
                textColumn.add(Box.createVerticalStrut(4));
                textColumn.add(tagsLabel);
            }

            add(textColumn, BorderLayout.CENTER);

            JButton installButton = new JButton("Install");
            installButton.setBackground(TableColor.ING_PURPLE);
            installButton.setForeground(Color.WHITE);
            installButton.setFocusPainted(false);
            installButton.setOpaque(true);
            installButton.setBorderPainted(false);
            installButton.addActionListener(e -> installComponent(component, installButton));
            add(installButton, BorderLayout.EAST);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
}
