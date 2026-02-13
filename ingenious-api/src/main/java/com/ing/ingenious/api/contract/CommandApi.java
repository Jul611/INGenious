package com.ing.ingenious.api.contract;

import java.io.File;
import java.util.Properties;
import java.util.Stack;

//import com.microsoft.playwright.Locator;
//import com.microsoft.playwright.Page;
//import com.microsoft.playwright.Playwright;
//import com.microsoft.playwright.BrowserContext;

public interface CommandApi {
    // Variable getters (for Command class fields)
//    Page getPage();
//    Playwright getPlaywright();
//    BrowserContext getBrowserContext();
//    
//    com.ing.engine.drivers.AutomationObject getAObject();
//    com.ing.engine.drivers.MobileObject getMObject();
//    com.ing.engine.drivers.PlaywrightDriverCreation getDriver();
//    
//    String getData();
//    String getObjectName();
//    Locator getLocator();
//    com.ing.datalib.or.common.ObjectGroup<com.ing.datalib.or.image.ImageORObject> getImageObjectGroup();
//    String getDescription();
//    String getCondition();
//    String getInput();
//    String getAction();
   TestCaseReportApi getReport();
//    String getReference();
//    com.ing.engine.execution.data.UserDataAccess getUserData();
//    org.openqa.selenium.WebDriver getMDriver();
//    org.openqa.selenium.WebElement getElement();
//    com.ing.engine.drivers.MobileObject getMObjectField();
//    String getKey();
    // com.ing.engine.core.CommandControl getCommander();

    // Method signatures
    void addVar(String key, String val);
    String getRuntimeVar(String key);
    String getVar(String key);
    void addGlobalVar(String key, String val);
    String getUserDefinedData(String key);
    String getDatasheet(String key);
    Properties getDataBaseData(String val);
    File getDBFile(String val);
    Stack<?> getRunTimeElement();
    void executeMethod(String Action);
//    void executeMethod(Locator Locator, String Action, String Input);
    void executeMethod(String Action, String Input);
//    void executeMethod(Locator Locator, String Action);
    Object getDriverControl();
    Object getMobileDriverControl();
    Boolean isDriverAlive();
    String getCurrentBrowserName();
    Object getCommander();
    void executeTestCase(String scenarioName, String testCaseName, int subIteration);
    void executeTestCase(String scenarioName, String testCaseName);
    boolean browserAction();
    String resolveAllRuntimeVars(String str);
    String Endpoint();
    String ResponseCode();
    String ResponseMessage();
    String ResponseBody();
    Object Connection();
    String HttpAgent();
}
