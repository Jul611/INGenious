package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.ide.main.utils.tree.CommonNode;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;

/**
 * Root node for shared reusable components tree.
 */
public class SharedReusableNode extends CommonNode {

    private Project project;

    public void setProject(Project project) {
        this.project = project;
        removeAllChildren();
        loadScenarios();
    }

    public void loadScenarios() {
        if (project != null) {
            for (Scenario scenario : project.getSharedReusableScenarios()) {
                add(new ScenarioNode(scenario));
            }
        }
    }

    public void load() {
        loadScenarios();
    }

    public GroupNode getGroupByName(String groupName) {
        for (GroupNode group : GroupNode.toList(children())) {
            if (group.toString().equals(groupName)) {
                return group;
            }
        }
        return null;
    }

}
