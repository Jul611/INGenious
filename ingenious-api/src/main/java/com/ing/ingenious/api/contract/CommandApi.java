package com.ing.ingenious.api.contract;

import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import java.io.File;
import java.util.Properties;
import java.util.Stack;

//import com.microsoft.playwright.Locator;
//import com.microsoft.playwright.Page;
//import com.microsoft.playwright.Playwright;
//import com.microsoft.playwright.BrowserContext;

public interface CommandApi {
    // Variable getters (for Command class fields)
    // Data related getters
    UserDataAccessApi getUserData();


//    //Playwright / Browser related getters
   Object getPage(); // needs to be cast as Page from com.microsoft.playwright
   Object getPlaywright(); // needs to be cast as Playwright from com.microsoft.playwright
   Object getBrowserContext(); //needs to be cast as BrowserContext from com.microsoft.playwright
   Object getLocator(); // needs to be cast as Locator from com.microsoft.playwright
//    com.ing.engine.drivers.AutomationObject getAObject();
//    com.ing.engine.drivers.PlaywrightDriverCreation getDriver();


   //Annotation Input access 
   String getData();
   String getObjectName();
   String getDescription();
   String getCondition();
   String getInput();
   String getAction();
   String getReference();

   //Report
   TestCaseReportApi getReport();

    // Method signatures
    // Basic data access
    void addVar(String key, String val);
    String getRuntimeVar(String key);
    String getVar(String key);
    void addGlobalVar(String key, String val);
    String getUserDefinedData(String key);
    String getDatasheet(String key);

    //Playwright / Browser related getters
    Object getDriverControl(); 
    Boolean isDriverAlive();
    boolean browserAction();

    String resolveAllRuntimeVars(String str);
    String Endpoint();
    String ResponseCode();
    String ResponseMessage();
    String ResponseBody();
    Object Connection();
    String HttpAgent();

    //Mobile
    // Object getMobileDriverControl();
    //    com.ing.engine.drivers.MobileObject getMObject();
    // Object getCommander();
    //    org.openqa.selenium.WebDriver getMDriver();
//    org.openqa.selenium.WebElement getElement();
//    com.ing.engine.drivers.MobileObject getMObjectField();
}
