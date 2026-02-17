package com.ing.ingenious.api.contract;

import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.drivers.AutomationObjectApi;
import com.ing.ingenious.api.contract.drivers.PlaywrightDriverCreationApi;
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


    //Playwright / Browser related getters
    /**
     * Retrieves the current Playwright Page instance.
     * @return the Page object that needs to be cast as {@code com.microsoft.playwright.Page}
     */
    Object getPage();
    
    /**
     * Retrieves the Playwright instance.
     * @return the Playwright object that needs to be cast as {@code com.microsoft.playwright.Playwright}
     */
    Object getPlaywright();
    
    /**
     * Retrieves the current browser context.
     * @return the BrowserContext object that needs to be cast as {@code com.microsoft.playwright.BrowserContext}
     */
    Object getBrowserContext();
    
    /**
     * Retrieves the current Locator instance.
     * @return the Locator object that needs to be cast as {@code com.microsoft.playwright.Locator}
     */
    Object getLocator();
    
    /**
     * Retrieves the automation object API for interacting with web elements.
     * @return the AutomationObjectApi instance
     */
    AutomationObjectApi getAObject();
    
    /**
     * Retrieves the Playwright driver creation API.
     * @return the PlaywrightDriverCreationApi instance for driver management
     */
    PlaywrightDriverCreationApi getDriver();


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
