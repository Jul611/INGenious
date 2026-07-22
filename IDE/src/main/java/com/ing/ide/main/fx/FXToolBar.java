package com.ing.ide.main.fx;

import com.ing.ide.main.Main;
import com.ing.ide.main.mainui.AppActionListener;
import com.ing.ide.main.mainui.components.pluginmanager.PluginManagerService;
import com.ing.ide.main.mainui.components.pluginmanager.UserConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javax.swing.SwingUtilities;

/**
 * JavaFX-based ToolBar wrapped in a JFXPanel for embedding in Swing.
 * Provides CSS-styled modern toolbar buttons with the same actions as AppToolBar.
 * Icons are rendered via Ikonli web font icons (INGIcons).
 */
public class FXToolBar extends JFXPanel {
    private static final Logger LOG = Logger.getLogger(FXToolBar.class.getName());

    private final AppActionListener actionListener;
    private ToggleButton autoSaveToggle;
    private ToggleButton darkModeToggle;
    private javafx.scene.control.ToolBar toolBar;

    public FXToolBar(AppActionListener actionListener) {
        this.actionListener = actionListener;
        CountDownLatch sceneReady = new CountDownLatch(1);
        Platform.runLater(
            () -> {
                initFX();
                sceneReady.countDown();
            }
        );
        try {
            sceneReady.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void initFX() {
        toolBar = new javafx.scene.control.ToolBar();

        toolBar
            .getItems()
            .addAll(
                createButton("New Project", "NewProject"),
                createButton("Open Project", "OpenProject"),
                new Separator(),
                createButton("Save Project", "SaveProject"),
                new Separator(),
                createAutoSaveSection(),
                new Separator(),
                createButton("Settings", "RunSettings"),
                createButton("Archetype Configurations", "BrowserConfiguration"),
                new Separator(),
                createAPITesterButton(),
                createPluginManagerButton(),
                createSpacer(),
                createProfileButton()
            );

        VBox root = new VBox(toolBar);
        root.getStyleClass().add("light-theme");

        Scene scene = new Scene(root);
        FXTheme.registerScene(scene);
        setScene(scene);
    }

    private Button createButton(String action, String iconName) {
        Button btn = new Button();
        btn.setTooltip(new Tooltip(action));

        org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fxColored(iconName, 18);
        if (icon != null) {
            btn.setGraphic(icon);
        } else {
            btn.setText(action);
        }

        btn.setOnAction(e -> fireSwingAction(action));
        return btn;
    }

    private Button createAPITesterButton() {
        Button btn = new Button("Workbench");
        btn.getStyleClass().add("workbench-btn");
        btn.setTooltip(new Tooltip("Open API Testing Console - Test REST APIs like Postman"));

        org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fx(
            "APITester",
            16,
            javafx.scene.paint.Color.BLACK
        );
        if (icon != null) {
            btn.setGraphic(icon);
        }

        btn.setOnAction(e -> fireSwingAction("API Workbench"));
        return btn;
    }

    private Button createPluginManagerButton() {
        Button btn = new Button("Plugins");
        btn.getStyleClass().add("workbench-btn");
        btn.setTooltip(new Tooltip("Open Plugin Manager - Browse and install plugins"));

        org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fx(
            "PluginManager",
            16,
            javafx.scene.paint.Color.BLACK
        );
        if (icon != null) {
            btn.setGraphic(icon);
        }

        btn.setOnAction(e -> fireSwingAction("Plugin Manager"));
        return btn;
    }

    private HBox createAutoSaveSection() {
        Label label = new Label("Auto Save");
        label.getStyleClass().add("auto-save-label");

        autoSaveToggle = new ToggleButton("OFF");
        autoSaveToggle.setOnAction(
            e -> {
                if (autoSaveToggle.isSelected()) {
                    autoSaveToggle.setText("ON");
                } else {
                    autoSaveToggle.setText("OFF");
                }
                fireSwingAction("Auto Save");
            }
        );

        HBox section = new HBox(6, label, autoSaveToggle);
        section.setAlignment(Pos.CENTER);
        return section;
    }

    private HBox createDarkModeToggle() {
        org.kordamp.ikonli.javafx.FontIcon sunIcon = new org.kordamp.ikonli.javafx.FontIcon(
            "fas-sun"
        );
        sunIcon.setIconSize(14);
        sunIcon.setIconColor(javafx.scene.paint.Color.web("#FF6200"));

        org.kordamp.ikonli.javafx.FontIcon moonIcon = new org.kordamp.ikonli.javafx.FontIcon(
            "fas-moon"
        );
        moonIcon.setIconSize(14);
        moonIcon.setIconColor(javafx.scene.paint.Color.web("#7724FF"));

        darkModeToggle = new ToggleButton();
        darkModeToggle.getStyleClass().add("dark-mode-pill");
        darkModeToggle.setSelected(Main.isDarkMode());
        updateDarkModeToggle();

        darkModeToggle.setOnAction(
            e -> {
                fireSwingAction("Dark Mode");
                Platform.runLater(
                    () -> {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {}
                        Platform.runLater(this::updateDarkModeToggle);
                    }
                );
            }
        );

        HBox section = new HBox(6, darkModeToggle);
        section.setAlignment(Pos.CENTER);
        return section;
    }

    private void updateDarkModeToggle() {
        boolean isDark = Main.isDarkMode();
        if (isDark) {
            org.kordamp.ikonli.javafx.FontIcon moonIcon = new org.kordamp.ikonli.javafx.FontIcon(
                "fas-moon"
            );
            moonIcon.setIconSize(14);
            moonIcon.setIconColor(javafx.scene.paint.Color.web("#89D6FD"));
            darkModeToggle.setGraphic(moonIcon);
            darkModeToggle.setText("Dark");
            darkModeToggle.setSelected(true);
        } else {
            org.kordamp.ikonli.javafx.FontIcon sunIcon = new org.kordamp.ikonli.javafx.FontIcon(
                "fas-sun"
            );
            sunIcon.setIconSize(14);
            sunIcon.setIconColor(javafx.scene.paint.Color.web("#FF6200"));
            darkModeToggle.setGraphic(sunIcon);
            darkModeToggle.setText("Light");
            darkModeToggle.setSelected(false);
        }
    }

    private Pane createSpacer() {
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private Button createProfileButton() {
        Button btn = new Button();
        btn.setTooltip(new Tooltip("User Profile — set PCode, user code, and publishing PAT"));

        org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fx("UserProfile", 16, Color.BLACK);
        if (icon != null) {
            btn.setGraphic(icon);
        } else {
            btn.setText("Profile");
        }

        btn.setOnAction(e -> showProfileDialog());
        return btn;
    }

    private void showProfileDialog() {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("User Profile");
        dialog.setHeaderText("Enter your personal details and publishing PAT");

        javafx.scene.control.TextField pcodeField = new javafx.scene.control.TextField(
            UserConfig.getPcode()
        );
        pcodeField.setPromptText("PCode");
        javafx.scene.control.TextField userCodeField = new javafx.scene.control.TextField(
            UserConfig.getUserCode()
        );
        userCodeField.setPromptText("User Code");
        javafx.scene.control.PasswordField patField = new javafx.scene.control.PasswordField();
        patField.setText(UserConfig.getPublishPat());

        // PAT help label — works both as info text and clickable link
        Hyperlink patHelpLink = new Hyperlink("How to create a PAT?");
        patHelpLink.setOnAction(
            e -> {
                try {
                    java
                        .awt.Desktop.getDesktop()
                        .browse(
                            java.net.URI.create("https://github.com/settings/tokens?type=beta")
                        );
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Could not open PAT help URL", ex);
                }
            }
        );
        patHelpLink.setTooltip(
            new Tooltip(
                "Open GitHub settings to create a fine-grained PAT with " +
                "'Contents: Read and Write' access on the plugins repo."
            )
        );

        Label patInstructions = new Label(
            "Required: Contents: Read & Write access on " +
            PluginManagerService.getPluginsRepoUrl() +
            "\nYou must be a collaborator on the plugins repo."
        );
        patInstructions.setWrapText(true);
        patInstructions.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.add(new Label("PCode:"), 0, 0);
        grid.add(pcodeField, 1, 0);
        grid.add(new Label("User Code:"), 0, 1);
        grid.add(userCodeField, 1, 1);
        grid.add(new Label("Publishing PAT:"), 0, 2);
        grid.add(patField, 1, 2);
        grid.add(patHelpLink, 1, 3);
        grid.add(patInstructions, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog
            .getDialogPane()
            .getButtonTypes()
            .addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        javafx.stage.Stage stage = (javafx.stage.Stage) dialog
            .getDialogPane()
            .getScene()
            .getWindow();
        FXTheme.registerScene(dialog.getDialogPane().getScene());

        dialog.setResultConverter(bt -> bt);
        dialog
            .showAndWait()
            .ifPresent(
                result -> {
                    if (result == javafx.scene.control.ButtonType.OK) {
                        UserConfig.setPcode(pcodeField.getText().trim());
                        UserConfig.setUserCode(userCodeField.getText().trim());
                        UserConfig.setPublishPat(patField.getText().trim());
                    }
                }
            );
    }

    private void fireSwingAction(String command) {
        SwingUtilities.invokeLater(
            () -> {
                java.awt.event.ActionEvent swingEvent = new java.awt.event.ActionEvent(
                    this,
                    java.awt.event.ActionEvent.ACTION_PERFORMED,
                    command
                );
                actionListener.actionPerformed(swingEvent);
            }
        );
    }

    public ToggleButton getAutoSaveToggle() {
        return autoSaveToggle;
    }
}
