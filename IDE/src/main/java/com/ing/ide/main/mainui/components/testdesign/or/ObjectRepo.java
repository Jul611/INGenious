
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.ide.main.fx.FXPanelHeader;
import com.ing.ide.main.fx.INGIcons;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.or.structureddata.StructuredDataORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.web.WebORPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.Highlighter.Highlight;

/**
 * Main UI container for managing the Object Repository within Test Design.
 * <p>
 * The {@code ObjectRepo} panel provides a unified interface for switching between
 * Web and Mobile Object Repository views. It embeds both {@link WebORPanel} and
 * {@link MobileORPanel} inside a card-based layout and exposes high-level actions
 * such as loading repository data, adjusting UI layout, and navigating directly to
 * specific OR objects.
 * </p>
 *
 * <p>
 * A toggle-based toolbar allows the user to switch between repository types, and
 * the component ensures the correct panel is shown and updated when selections occur.
 * This class acts as the entry point for OR maintenance within the Test Design module.
 * </p>
 */
public class ObjectRepo extends JPanel implements ItemListener {

    private final TestDesign testDesign;

    private final SwitchToolBar switchToolBar;

    private final JPanel repositoryPanel;

    private final WebORPanel webORPanel;

    private final MobileORPanel mobileORPanel;

    private final StructuredDataORPanel structuredDataORPanel;

    FXPanelHeader header = new FXPanelHeader("Object Repository");

    public ObjectRepo(TestDesign testDesign) {
        this.testDesign = testDesign;
        switchToolBar = new SwitchToolBar();
        repositoryPanel = new JPanel();
        webORPanel = new WebORPanel(testDesign);
        mobileORPanel = new MobileORPanel(testDesign);
        structuredDataORPanel = new StructuredDataORPanel(testDesign);
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        // Create header panel with FXPanelHeader + SwitchToolBar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        header = new FXPanelHeader("Object Repository");
        headerPanel.add(header, BorderLayout.NORTH);
        headerPanel.add(switchToolBar, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);
        add(repositoryPanel, BorderLayout.CENTER);
        initRepos();
    }

    private void initRepos() {
        repositoryPanel.setLayout(new CardLayout());
        repositoryPanel.setOpaque(false);
        repositoryPanel.add(webORPanel, "Web");
        repositoryPanel.add(mobileORPanel, "Mobile");
        repositoryPanel.add(structuredDataORPanel, "API");
        switchToolBar.bgroup.getElements().nextElement().setSelected(true);
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getStateChange() == ItemEvent.SELECTED) {
            CardLayout layout = (CardLayout) repositoryPanel.getLayout();
            String command = ((JToggleButton) ie.getSource()).getActionCommand();
            layout.show(repositoryPanel, command);
            // Call adjustUI after panel becomes visible to fix split pane divider
            SwingUtilities.invokeLater(() -> {
                switch (command) {
                    case "Web":
                        webORPanel.adjustUI();
                        break;
                    case "Mobile":
                        mobileORPanel.adjustUI();
                        break;
                    case "StructuredData":
                        structuredDataORPanel.adjustUI();
                        break;
                }
            });
        }
    }

    public void load() {
        webORPanel.load();
        mobileORPanel.load();
        structuredDataORPanel.load();
    }

    public void adjustUI() {
        webORPanel.adjustUI();
        mobileORPanel.adjustUI();
        structuredDataORPanel.adjustUI();
    }

    public WebORPanel getWebORPanel() {
        return webORPanel;
    }

    public MobileORPanel getMobileORPanel() {
        return mobileORPanel;
    }

    public StructuredDataORPanel getStructuredDataOR() {
        return structuredDataORPanel;
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        if (webORPanel.navigateToObject(objectName, pageName)) {
            switchToolBar.webButton.setSelected(true);
            return true;
        } else if (mobileORPanel.navigateToObject(objectName, pageName)) {
            switchToolBar.mobileButton.setSelected(true);
            return true;
        } else if (structuredDataORPanel.navigateToObject(objectName, pageName)) {
            switchToolBar.structuredDataButton.setSelected(true);
            return true;
        }
        return false;
    }

    class SwitchToolBar extends JToolBar {

        private ButtonGroup bgroup;

        private JToggleButton webButton;
        //private JToggleButton imageButton;
        private JToggleButton mobileButton;
        private JToggleButton structuredDataButton;

        public SwitchToolBar() {
            init();
        }

        private void init() {
            setFloatable(false);
            setOpaque(false);
            bgroup = new ButtonGroup();
            
            add(new javax.swing.Box.Filler(new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 32767)));
            add(new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767)));
            add(webButton = create("Web", "or.Web"));
            //add(imageButton = create("Image"));
            add(mobileButton = create("Mobile", "or.Mobile"));
            add(structuredDataButton = create("Structured Data", "or.StructuredData"));
        }

        private JToggleButton create(String text, String iconKey) {
            JToggleButton toggleButton = new JToggleButton();
            toggleButton.setIcon(INGIcons.swingColored(iconKey, 18));
            toggleButton.setToolTipText(text + " Object Repository");
            toggleButton.setActionCommand(text);
            toggleButton.addItemListener(ObjectRepo.this);
            // Remove hover/focus background effect
            toggleButton.setContentAreaFilled(false);
            toggleButton.setFocusPainted(false);
            toggleButton.setBorderPainted(false);
            toggleButton.addItemListener(e -> {
                if (toggleButton.isSelected()) {
                    toggleButton.setIcon(INGIcons.swingColored(iconKey+".selected", 18));
                } else {
                    toggleButton.setIcon(INGIcons.swingColored(iconKey, 18));
                }
            });
            bgroup.add(toggleButton);
            return toggleButton;
        }
    }
}