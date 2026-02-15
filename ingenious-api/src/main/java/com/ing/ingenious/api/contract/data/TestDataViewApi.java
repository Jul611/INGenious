package com.ing.ingenious.api.contract.data;

import java.util.List;
import java.util.Set;

public interface TestDataViewApi {
    List columns();
    List records();
    int index(String field);
    boolean canUpdate(String field);
    List<String> addRecord(String scenario, String testcase, String iteration, String subIteration);

    void clear();
    void put(String key, List records);
    void add(String key, List<String> records);
    List get(String key);
    List get();
    String getField(String key, String field);
    String getField(String field);
    boolean update(String field, String value);
    List<String> getFields(String key, String field);
    List<String> getFields(String field);
    Set<String> getIterations();
    Set<String> getSubIterations();

    TestDataViewApi withScenarioOrGID(String scnOrgid);
    TestDataViewApi withTestcase(String scn, String tc);
    TestDataViewApi withIter(String scn, String tc, String iter);
    TestDataViewApi withIter(String scn, String tc, String iter, Boolean addIfNotPresent);
    TestDataViewApi withSubIter(String scn, String tc, String iter, String subIter);
    TestDataViewApi withSubIter(String scn, String tc, String iter, String subIter, Boolean addIfNotPresent);
}
