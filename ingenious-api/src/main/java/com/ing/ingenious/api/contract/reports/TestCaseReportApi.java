package com.ing.ingenious.api.contract.reports;

import com.ing.ingenious.api.status.Status;
import java.io.File;
import java.util.List;

public interface TestCaseReportApi {
//    void createReport(RunContextApi runContext, String runTime);
    void updateTestLog(String stepName, String stepDescription, Status state);
    void updateTestLog(String stepName, String stepDescription, Status state, String optionalLink);
    void updateTestLog(String stepName, String stepDescription, Status state, List<String> optional);
    void updateTestLog(String stepName, String stepDescription, Status state, String optionalLink, List<String> optional);
    Object finalizeReport();
    void startIteration(int iteration);
    void startComponent(String component, String desc);
    void endComponent(String component);
    void endIteration(int iteration);
    Object getPlaywrightDriver();
    Object getWebDriver();
    int getIter();
    Object getData();
    File getFile();
    String getScreenShotName();
    String getNewScreenShotName();
    String getWebserviceResponseFileName();
    String getWebserviceRequestFileName();
    String getPdfResultName();
    String getLogFileName();
    File getReportLoc();
    Object getStep();
    String getTestCaseName();
    String getScenarioName();
    Object getCurrentStatus();
    Boolean isStepPassed();
    int getStepCount();
    // void register(Object testCaseHandler);
    // void register(Object testCaseHandler, boolean primaryHandler);
    static int getTestCaseNumber() { throw new UnsupportedOperationException(); }
}
