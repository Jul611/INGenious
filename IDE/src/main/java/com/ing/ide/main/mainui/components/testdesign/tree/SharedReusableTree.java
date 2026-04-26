package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.util.Validator;
import com.ing.ide.main.mainui.components.testdesign.tree.model.SharedReusableTreeModel;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.util.Notification;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

/**
 * Tree component for managing shared reusable components.
 * Shared reusables are stored at workspace root level and available to all projects.
 */
public class SharedReusableTree extends ProjectTree {

    private static final Logger LOGGER = Logger.getLogger(SharedReusableTree.class.getName());

    public SharedReusableTree(TestDesign testDesign) {
        super(testDesign);
    }

    @Override
    protected SharedReusableTreeModel getNewTreeModel() {
        return new SharedReusableTreeModel();
    }

    @Override
    SharedReusablePopupMenu getNewPopupMenu() {
        return new SharedReusablePopupMenu();
    }

    @Override
    public SharedReusableTreeModel getTreeModel() {
        return (SharedReusableTreeModel) super.getTreeModel();
    }

    @Override
    public void loadTableModelForSelection() {
        Object selected = getSelectedTestCase();
        if (selected != null) {
            super.loadTableModelForSelection();
        }
    }

    @Override
    protected void togglePopupMenu(Object selected) {
        if (isRootSelected()) {
            ((SharedReusablePopupMenu) popupMenu).forRoot();
        } else {
            super.togglePopupMenu(selected);
        }
    }

    @Override
    protected void onNewAction() {
        if (isRootSelected()) {
            addSharedReusableScenario();
        } else if (getSelectedScenarioNodeSafe() != null) {
            addSharedReusableTestCase();
        } else {
            super.onNewAction();
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Add Scenario":
                addSharedReusableScenario();
                break;
            case "Add TestCase":
                addSharedReusableTestCase();
                break;
            case "Copy to Project Reusable":
                copyToProjectReusable();
                break;
            default:
                super.actionPerformed(ae);
        }
    }

    @Override
    protected Boolean checkAndRename() {
        String name = getTree().getCellEditor().getCellEditorValue().toString().trim();
        
        // Validate name using same validator as ReusableTree
        if (!Validator.isValidName(name)) {
            Notification.show("Invalid name. Use valid characters only.");
            return false;
        }
        
        return super.checkAndRename();
    }

    @Override
    void renameScenario(Scenario scenario) {
        // Update references in project tree if needed
        getTestDesign().getProjectTree().getTreeModel().onScenarioRename(scenario);
    }

    @Override
    void makeAsReusableRTestCase(TestCase testCase) {
        // Move from shared to project reusable
        if (copyTestCaseToProjectReusable(testCase, false)) {
            // Optionally remove from shared after copying
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Test case copied to project reusables.\nRemove from shared reusables?",
                    "Remove from Shared",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                deleteTestCaseFile(testCase);
                load();
            }
            getTestDesign().getReusableTree().load();
        }
    }

    private void addSharedReusableScenario() {
        String name = fetchNewScenarioName("NewSharedScenario");
        
        // Validate name
        if (!Validator.isValidName(name)) {
            Notification.show("Invalid scenario name. Use valid characters only.");
            return;
        }
        
        Scenario scenario = getProject().addSharedReusableScenario(name);
        if (scenario != null) {
            ScenarioNode scNode = getTreeModel().addScenario(scenario);
            if (scNode != null) {
                selectAndScrollTo(new TreePath(scNode.getPath()));
            }
        } else {
            Notification.show("Scenario " + name + " already exists");
        }
    }

    private void addSharedReusableTestCase() {
        ScenarioNode scNode = getSelectedScenarioNodeSafe();
        if (scNode != null) {
            String name = fetchNewTestCaseName();
            
            // Validate name
            if (!Validator.isValidName(name)) {
                Notification.show("Invalid test case name. Use valid characters only.");
                return;
            }
            
            TestCase testCase = scNode.getScenario().addTestCase(name);
            if (testCase != null) {
                TestCaseNode tcNode = getTreeModel().addTestCase(scNode, testCase);
                if (tcNode != null) {
                    selectAndScrollTo(new TreePath(tcNode.getPath()));
                }
            } else {
                Notification.show("TestCase " + name + " already exists in this scenario");
            }
        }
    }

    private void copyToProjectReusable() {
        List<TestCaseNode> nodes = getSelectedTestCaseNodes();
        if (!nodes.isEmpty()) {
            copyTestCaseToProjectReusable(nodes.get(0).getTestCase(), true);
        }
    }

    private boolean copyTestCaseToProjectReusable(TestCase testCase, boolean showNotification) {
        try {
            String scenarioName = testCase.getScenario().getName();
            String testCaseName = testCase.getName();
            
            // Get or create scenario in project reusables
            Scenario targetScenario = getProject().getReusableScenarioByName(scenarioName);
            if (targetScenario == null) {
                targetScenario = getProject().addReusableScenario(scenarioName);
            }
            
            // Check if test case already exists
            if (targetScenario.getTestCaseByName(testCaseName) != null) {
                int confirm = JOptionPane.showConfirmDialog(null,
                        "Test case '" + scenarioName + ":" + testCaseName + 
                        "' already exists in project reusables.\nOverwrite?",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
            
            // Copy the file
            File sourceFile = new File(testCase.getLocation());
            File targetDir = new File(targetScenario.getLocation());
            targetDir.mkdirs();
            File targetFile = new File(targetDir, testCaseName + ".csv");
            
            Files.copy(sourceFile.toPath(), targetFile.toPath(), 
                    StandardCopyOption.REPLACE_EXISTING);
            
            if (showNotification) {
                Notification.show("Copied to project reusables: " + scenarioName + ":" + testCaseName);
            }
            
            // Reload project tree
            getProject().reload();
            getTestDesign().getReusableTree().load();
            
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to copy test case to project reusable", ex);
            if (showNotification) {
                Notification.show("Error copying test case: " + ex.getMessage());
            }
            return false;
        }
    }

    private void deleteTestCaseFile(TestCase testCase) {
        File file = new File(testCase.getLocation());
        if (file.exists()) {
            file.delete();
        }
    }

    private String fetchNewScenarioName(String prefix) {
        String newName = prefix;
        for (int i = 0;; i++) {
            boolean exists = false;
            for (Scenario scenario : getProject().getSharedReusableScenarios()) {
                if (scenario.getName().equals(newName)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                break;
            }
            newName = prefix + i;
        }
        return newName;
    }

    private String fetchNewTestCaseName() {
        String newName = "NewTestCase";
        ScenarioNode scNode = getSelectedScenarioNodeSafe();
        if (scNode != null) {
            for (int i = 0;; i++) {
                if (scNode.getScenario().getTestCaseByName(newName) == null) {
                    break;
                }
                newName = "NewTestCase" + i;
            }
        }
        return newName;
    }

    private Boolean isRootSelected() {
        TreePath path = getTree().getSelectionPath();
        if (path != null) {
            return path.getLastPathComponent().equals(getTreeModel().getRoot());
        }
        return false;
    }

    private ScenarioNode getSelectedScenarioNodeSafe() {
        List<ScenarioNode> nodes = getSelectedScenarioNodes();
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.get(0);
    }

    public void save() {
        getTreeModel().save();
    }

    class SharedReusablePopupMenu extends ProjectPopupMenu {

        JMenuItem addScenario;
        JMenuItem addTestCase;
        JMenuItem copyToProject;

        public SharedReusablePopupMenu() {
            initMenu();
        }

        private void initMenu() {
            removeAll();
            add(addScenario = create("Add Scenario", Keystroke.NEW));
            add(addTestCase = create("Add TestCase", Keystroke.NEW));
            addSeparator();
            add(copyToProject = create("Copy to Project Reusable", null));
            addSeparator();
            super.init();
            toggleReusable.setText("Copy to Project Reusable");
            toggleReusable.setVisible(false);
        }

        @Override
        protected void forTestCase() {
            super.forTestCase();
            addScenario.setEnabled(false);
            addTestCase.setEnabled(false);
            copyToProject.setEnabled(true);
        }

        @Override
        protected void forScenario() {
            super.forScenario();
            addScenario.setEnabled(false);
            addTestCase.setEnabled(true);
            copyToProject.setEnabled(false);
        }

        @Override
        protected void forTestPlan() {
            super.forTestPlan();
            addScenario.setEnabled(false);
            addTestCase.setEnabled(false);
            copyToProject.setEnabled(false);
        }

        protected void forRoot() {
            super.forTestPlan();
            addScenario.setEnabled(true);
            addTestCase.setEnabled(false);
            copyToProject.setEnabled(false);
        }
    }

}
