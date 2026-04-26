package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;

/**
 * Tree model for shared reusable components.
 * Shared reusables are stored at workspace root level.
 */
public class SharedReusableTreeModel extends ProjectTreeModel {

    Project project;

    public SharedReusableTreeModel() {
        super(new SharedReusableNode());
    }

    @Override
    public final void setProject(Project project) {
        this.project = project;
        getRoot().setProject(project);
    }

    @Override
    public SharedReusableNode getRoot() {
        return (SharedReusableNode) super.getRoot();
    }

    public ScenarioNode addScenario(Scenario scenario) {
        if (scenario != null) {
            ScenarioNode sNode = new ScenarioNode(scenario);
            insertNodeInto(sNode, getRoot(), getRoot().getChildCount());
            return sNode;
        }
        return null;
    }

    @Override
    public TestCaseNode addTestCase(TestCase testCase) {
        // Find the scenario node
        for (ScenarioNode scenarioNode : ScenarioNode.toList(getRoot().children())) {
            if (scenarioNode.getScenario().equals(testCase.getScenario())) {
                return addTestCase(scenarioNode, testCase);
            }
        }
        // If scenario not found, create it
        Scenario scenario = testCase.getScenario();
        ScenarioNode sNode = addScenario(scenario);
        if (sNode != null) {
            return addTestCase(sNode, testCase);
        }
        return null;
    }

    @Override
    public TestCaseNode addTestCase(ScenarioNode scNode, TestCase testCase) {
        return super.addTestCase(scNode, testCase);
    }

    @Override
    public void onScenarioRename(Scenario scenario) {
        for (ScenarioNode sNode : ScenarioNode.toList(getRoot().children())) {
            if (sNode.getScenario().equals(scenario)) {
                reload(sNode);
                break;
            }
        }
    }

    public void save() {
        // No-op: shared reusable components don't need XML persistence
        // They are inferred from the filesystem structure
    }

}
