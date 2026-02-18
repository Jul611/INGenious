package com.ing.ingenious.api.contract.data;

/**
 * Interface for user data access operations
 */
public interface UserDataAccessApi {
    String getCurrentScenario();

    String getCurrentTestCase();

    String getScenario();

    String getTestCase();

    String getIteration();

    String getTestCaseSubIteration();

    String getSubIteration();

    int getSubIterationAsNumber();

    String getGlobalData(String globalDataID, String columnName);

    void putGlobalData(String globalDataID, String columnName, String value);

    String getData(String Sheet, String Column);

    String getData(String Sheet, String Column, String Iteration, String SubIteration);

    String getData(String sheet, String column, String scenario, String testcase, String iteration, String subiteration);

    void putData(String sheet, String column, String value);

    void putData(String sheet, String column, String value, String iteration, String subIteration);

    void putData(String sheet, String column, String value, String scenario, String testcase, String iteration, String subIteration);

    TestDataViewApi getTestData(String sheetName);
}