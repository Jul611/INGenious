package com.ing.ingenious.api.contract;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

public interface GeneralDbApi extends CommandApi {
    // Getters for fields
    Connection getDbconnection();
    Statement getStatement();
    ResultSet getResult();
    ResultSetMetaData getResultData();
    List<String> getColNames();
    String getData();
    String getAction();
    String getInput();


    // Method signatures
    Properties getDBDetails(String dbName);
    int getColumnIndex(String columnName);
    boolean verifyDbConnection(String dbName) throws ClassNotFoundException, java.sql.SQLException;
    void executeSelect() throws java.sql.SQLException;
    boolean executeDML() throws java.sql.SQLException;
    boolean closeConnection() throws java.sql.SQLException;
    boolean assertDB(String columnName, String condition);
    void storeValue(String input, String condition, boolean isGlobal);

}

