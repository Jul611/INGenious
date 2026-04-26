
package com.ing.engine.execution.run;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.execution.data.DataProcessor;
import com.ing.engine.execution.data.Parameter;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.execution.exception.UnKnownError;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.support.Status;
import com.ing.engine.support.Step;
import com.ing.engine.support.reflect.MethodExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.lang.String.format;
import java.util.ArrayList;
import java.util.List;

public class TestStepRunner {

    private static final Logger LOG = Logger.getLogger(TestStepRunner.class.getName());

    private final TestStep testStep;
    private final Parameter parameter;
    private Step step;

    public TestStepRunner(TestStep testStep, Parameter parameter) {
        this.parameter = parameter;
        this.testStep = testStep;
    }

    public TestStepRunner() {
        this.parameter = null;
        this.testStep = null;
    }

    public void run(TestCaseRunner context) throws DataNotFoundException, DriverClosedException {
        if (this.parameter != null && this.testStep != null) {
            if (context.executor().isDebugExe()) {
                checkForDebug();
            }
            step = new Step(testStep, context);
            context.getReport().updateStepDetails(step);
            switch (getStep().getObject()) {
                case "Execute":
                    execute(context);
                    break;
                default:
                    executeStep(context);
                    break;
            }
        } else {
            throw new RuntimeException("Not enough data to run a step");
        }
    }

    private void checkForDebug() {
        SystemDefaults.nextStepflag.set(true);
        SystemDefaults.pauseExecution.set(getStep().hasBreakPoint()
                || SystemDefaults.pauseExecution.get());
        while (SystemDefaults.pauseExecution.get() && SystemDefaults.nextStepflag.get()
                && !SystemDefaults.stopExecution.get()) {
            SystemDefaults.pollWait();
        }
    }

    private int getSubIterationFromInput(TestCaseRunner context) {
        if (!getStep().getInput().isEmpty()) {
            try {
                return Integer.valueOf(DataProcessor.resolve(getStep().getInput(), context,
                        String.valueOf(parameter.getSubIteration())));
            } catch (Exception ex) {
                System.err.println("Unable to resolve subIteration for reusable!!");
                LOG.log(Level.WARNING, ex.getMessage(), ex);
                return 1;
            }
        }
        return parameter.getSubIteration();
    }

    private TestStep getStep() {
        return testStep;
    }

    /**
     * parse the Execute action to reusable testcase and executes in the current
     * testcase context.
     * 
     * Supported formats:
     * 1. Execute Scenario:TestCase (default: check project first, then shared)
     * 2. Execute Project:Scenario:TestCase (explicit project reusables)
     * 3. Execute Shared:Scenario:TestCase (explicit shared reusables)
     *
     * @param context - current testcase context to run the reusable
     * @throws DataNotFoundException, ForcedException
     */
    private void execute(TestCaseRunner context) throws DataNotFoundException, ForcedException {
        if (getStep().isReusableStep()) {
            String action = getStep().getAction();
            String reference = getStep().getReference();
            
            // Parse the action to extract source, scenario, and testcase
            ReusableReference ref = parseReusableReference(action, reference);
            
            // Resolve the reusable test case based on source
            TestCase stc = resolveReusableTestCase(context, ref);
            
            if (stc != null) {
                stc.setParentTestCase(context.getTestCase());
                executeTestCase(context, stc);
                return;
            } else {
                throw new ForcedException(buildReusableNotFoundMessage(ref));
            }
        }
        throw new ForcedException(
                format("invalid reusable [%s], expected format [Scenario:TestCase] or [Source:Scenario:TestCase]",
                        getStep().getAction()));
    }

    /**
     * Parses the reusable reference from action and reference column.
     */
    private ReusableReference parseReusableReference(String action, String reference) {
        ReusableReference ref = new ReusableReference();
        
        // Check reference column for source specification
        if (reference != null && !reference.trim().isEmpty()) {
            String refLower = reference.trim().toLowerCase();
            if (refLower.equals("project")) {
                ref.source = ReusableSource.PROJECT;
            } else if (refLower.equals("shared")) {
                ref.source = ReusableSource.SHARED;
            }
        }
        
        // Parse action column
        String[] parts = action.split(":");
        
        if (parts.length == 2) {
            // Format: Scenario:TestCase
            ref.scenario = parts[0].trim();
            ref.testCase = parts[1].trim();
        } else if (parts.length == 3) {
            // Format: Source:Scenario:TestCase (legacy support or explicit)
            String sourcePart = parts[0].trim().toLowerCase();
            if (sourcePart.equals("project")) {
                ref.source = ReusableSource.PROJECT;
            } else if (sourcePart.equals("shared")) {
                ref.source = ReusableSource.SHARED;
            }
            ref.scenario = parts[1].trim();
            ref.testCase = parts[2].trim();
        } else {
            ref.scenario = parts[0].trim();
            ref.testCase = parts.length > 1 ? parts[1].trim() : "";
        }
        
        // Default to AUTO if no source specified
        if (ref.source == null) {
            ref.source = ReusableSource.AUTO;
        }
        
        return ref;
    }

    /**
     * Resolves the reusable test case based on source specification.
     */
    private TestCase resolveReusableTestCase(TestCaseRunner context, ReusableReference ref) {
        switch (ref.source) {
            case PROJECT:
                return findInProjectReusables(context, ref.scenario, ref.testCase);
                
            case SHARED:
                return findInSharedReusables(context, ref.scenario, ref.testCase);
                
            case AUTO:
            default:
                // Default behavior: check project first, then shared
                TestCase tc = findInProjectReusables(context, ref.scenario, ref.testCase);
                if (tc == null) {
                    tc = findInSharedReusables(context, ref.scenario, ref.testCase);
                }
                return tc;
        }
    }

    /**
     * Finds reusable in project-level reusable components.
     */
    private TestCase findInProjectReusables(TestCaseRunner context, String scenario, String testCase) {
        Scenario scn = context.project().getReusableScenarioByName(scenario);
        if (scn != null) {
            return scn.getTestCaseByName(testCase);
        }
        return null;
    }

    /**
     * Finds reusable in shared reusable components.
     */
    private TestCase findInSharedReusables(TestCaseRunner context, String scenario, String testCase) {
        Scenario scn = context.project().getSharedReusableScenarioByName(scenario);
        if (scn != null) {
            return scn.getTestCaseByName(testCase);
        }
        return null;
    }

    /**
     * Builds an appropriate error message for missing reusables.
     */
    private String buildReusableNotFoundMessage(ReusableReference ref) {
        StringBuilder msg = new StringBuilder();
        msg.append(format("Reusable test case not found: '%s:%s'\n", ref.scenario, ref.testCase));
        
        switch (ref.source) {
            case PROJECT:
                msg.append("Searched in: Project reusable components only");
                break;
            case SHARED:
                msg.append("Searched in: Shared reusable components only");
                break;
            case AUTO:
                msg.append("Searched in:\n");
                msg.append("  - Project reusable components\n");
                msg.append("  - Shared reusable components");
                break;
        }
        
        return msg.toString();
    }

    /**
     * Enum for reusable source specification.
     */
    private enum ReusableSource {
        PROJECT,  // Project-level reusables only
        SHARED,   // Shared reusables only
        AUTO      // Check project first, then shared
    }

    /**
     * Container for parsed reusable reference.
     */
    private static class ReusableReference {
        ReusableSource source;
        String scenario;
        String testCase;
    }

    private void executeTestCase(TestCaseRunner context, TestCase stc) throws DataNotFoundException {
        try {
            parameter.setSubIteration(getSubIterationFromInput(context));
            context.getReport().startComponent(getStep().getAction(), getStep().getDescription());
            new TestCaseRunner(context, stc, parameter).run();
        } finally {
            context.getReport().endComponent(getStep().getAction());
        }
    }

    private void executeStep(TestCaseRunner context) throws DataNotFoundException, DriverClosedException {
        try {
            Annotation ann = new Annotation(context.getControl());
            ann.beforeStepExecution();
            executeStep(context, step, parameter);
            ann.afterStepExecution();
        } catch (DataNotFoundException | DriverClosedException
                | ForcedException | ElementException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new UnKnownError(ex);
        }
    }

    private void executeStep(TestCaseRunner context, Step step, Parameter parameter)
            throws Throwable {
        step.printStep();
        if (step.ObjectName.equals("String Operations")) {
            List<String> concatList = context.getControl().smartCommaSplitter(getStep().getInput());
            List<String> result = new ArrayList();
            for (String part : concatList) {
                if (part.matches("%.*%")) 
                    result.add("'"+context.getControl().getVar(part)+"'");
                else if (part.matches("^\\{.*:.*\\}")) 
                    result.add("'"+context.getControl().getDatasheet(part)+"'");
                else if (part.matches("\".*\"")) 
                    result.add("'"+part.substring( 1, part.length() - 1 )+"'");
            }
            step.Data = String.join(",", result);
            context.getControl().sync(step);
        }
        else{
            context.getControl().sync(step, String.valueOf(parameter.getSubIteration()));
        }
        executeAction(context, step.Action);
    }

    public void executeAction(TestCaseRunner context, String action) throws Throwable {
        if (!MethodExecutor.executeMethod(action, context.getControl())) {
            System.out.println("[ERROR][Could not find Action:" + action + "]");
            context.getReport().updateTestLog(action, "[Could not find Action]",
                    Status.DEBUG);
        }
    }

}
