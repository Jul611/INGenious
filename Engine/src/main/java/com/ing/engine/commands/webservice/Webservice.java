package com.ing.engine.commands.webservice;

import com.ing.datalib.settings.DriverProperties;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import com.jayway.jsonpath.*;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import java.io.StringReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

public class Webservice extends GeneralWebservice {

    public Webservice(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "PUT Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void putRestRequest() {
        try {
            createHttpRequest(RequestMethod.PUT);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "POST Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void postRestRequest() {
        try {
            createHttpRequest(RequestMethod.POST);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "POST SOAP Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void postSoapRequest() {
        try {
            createHttpRequest(RequestMethod.POST);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "PATCH Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void patchRestRequest() {
        try {
            createHttpRequest(RequestMethod.PATCH);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "GET Rest Request ", input = InputType.NO, condition = InputType.OPTIONAL)
    public void getRestRequest() {
        try {
            createHttpRequest(RequestMethod.GET);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "DELETE Rest Request ", input = InputType.NO)
    public void deleteRestRequest() {
        try {
            createHttpRequest(RequestMethod.DELETE);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "DELETE with Payload ", input = InputType.YES)
    public void deleteWithPayload() {
        try {
            createHttpRequest(RequestMethod.DELETEWITHPAYLOAD);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Code ", input = InputType.YES)
    public void assertResponseCode() {
        try {
            if (responsecodes.get(key).equals(Data)) {
                Report.updateTestLog(Action, "Status code is : " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Status code is : " + responsecodes.get(key) + " but should be " + Data,
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response code :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Body contains ", input = InputType.YES)
    public void assertResponsebodycontains() {
        try {
            if (responsebodies.get(key).contains(Data)) {
                Report.updateTestLog(Action, "Response body contains : " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Response body does not contain : " + Data, Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response body :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementEquals() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Condition;
            String value = JsonPath.read(response, jsonpath).toString();
            if (value.equals(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text is [" + value + "] but is expected to be [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementContains() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Condition;
            String value = JsonPath.read(response, jsonpath).toString();
            if (value.contains(Data)) {
                Report.updateTestLog(Action, "Element text contains [" + Data + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element In DataSheet ", input = InputType.YES, condition = InputType.YES)
    public void storeJSONelementInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String response = responsebodies.get(key);
                    String jsonpath = Condition;
                    String value = JsonPath.read(response, jsonpath).toString();
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(Action, "Element text [" + value + "] is stored in " + strObj, Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Store XML Element In DataSheet ", input = InputType.YES, condition = InputType.YES)
    public void storeXMLelementInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String xmlText = responsebodies.get(key);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder;
                    InputSource inputSource = new InputSource();
                    inputSource.setCharacterStream(new StringReader(xmlText));
                    dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(inputSource);
                    doc.getDocumentElement().normalize();
                    XPath xPath = XPathFactory.newInstance().newXPath();
                    String expression = Condition;
                    NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
                    Node nNode = nodeList.item(0);
                    String value = nNode.getNodeValue();
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(Action, "Element text [" + value + "] is stored in " + strObj, Status.DONE);
                } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                        | SAXException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing XML element in datasheet :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing XML element in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element", input = InputType.YES, condition = InputType.YES)
    public void storeJSONelement() {
        try {
            String variableName = Condition;
            String jsonpath = Data;
            if (variableName.matches("%.*%")) {
                addVar(variableName, JsonPath.read(responsebodies.get(key), jsonpath).toString());
                Report.updateTestLog(Action, "JSON element value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Store XML Element", input = InputType.YES, condition = InputType.YES)
    public void storeXMLelement() {
        try {
            String variableName = Condition;
            String expression = Data;
            if (variableName.matches("%.*%")) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;
                InputSource inputSource = new InputSource();
                inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
                dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(inputSource);
                doc.getDocumentElement().normalize();
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
                Node nNode = nodeList.item(0);
                String value = nNode.getNodeValue();
                addVar(variableName, value);
                Report.updateTestLog(Action, "XML element value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Store Response Message In DataSheet ", input = InputType.YES)
    public void storeResponseBodyInDataSheet() {
        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    userData.putData(sheetName, columnName, responsebodies.get(key));
                    Report.updateTestLog(Action, "Response body is stored in " + strObj, Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing text in datasheet :" + ex.getMessage(), Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing response body in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Assert XML Element Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertXMLelementEquals() {

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = Condition;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            if (value.equals(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] is not as expected", Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Assert XML Element Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertXMLelementContains() {

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = Condition;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            if (value.contains(Data)) {
                Report.updateTestLog(Action, "Element text contains [" + Data + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Set End Point ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void setEndPoint() {
        try {
            String apiConfigName = Condition;
            DriverProperties driverProperties = Control.getCurrentProject().getProjectSettings().getDriverSettings();
            if (apiConfigName.startsWith("#")) {
                apiConfigName = apiConfigName.replace("#", "");
            } else {
                apiConfigName = ""; //This means that the Condtion is not an API Config Alias
            }

            String configToLoad = driverProperties.doesAPIconfigExist(apiConfigName) ? apiConfigName : "default";
            driverProperties.setCurrLoadedAPIConfig(configToLoad);
            
            String resource = handlePayloadorEndpoint(Data);
            endPoints.put(key, resource);
            httpAgentCheck();
            OpenURLconnection();
            Report.updateTestLog(Action, "End point set : " + resource, Status.DONE);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error setting the end point :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Count ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementCount() {

        try {
            String response = responsebodies.get(key);
            int actualObjectCount = 0;
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response);
            try {
                Map<String, String> objectMap = JsonPath.read(json, Condition);
                actualObjectCount = objectMap.keySet().size();
            } catch (Exception ex) {
                try {
                    JSONArray objectMap = JsonPath.read(json, Condition);
                    actualObjectCount = objectMap.size();
                } catch (Exception ex1) {
                    try {
                        net.minidev.json.JSONArray objectMap = JsonPath.read(json, Condition);
                        actualObjectCount = objectMap.size();
                    } catch (Exception ex2) {
                        String objectMap = JsonPath.read(json, Condition);
                        actualObjectCount = 1;
                    }
                }
            }

            int expectedObjectCount = Integer.parseInt(Data);
            if (actualObjectCount == expectedObjectCount) {
                Report.updateTestLog(Action, "Element count [" + expectedObjectCount + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element count is [" + actualObjectCount + "] but is expected to be [" + expectedObjectCount + "]", Status.FAILNS);
            }

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element count in variable ", input = InputType.YES, condition = InputType.YES)
    public void storeJsonElementCount() {

        try {
            String variableName = Condition;
            Condition = Data;

            if (variableName.matches("%.*%")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    int actualObjectCountInteger = 1;                                                       //getJsonElementCount();
                    String actualObjectCount = Integer.toString(actualObjectCountInteger);
                    addVar(variableName, actualObjectCount);
                    Report.updateTestLog(Action, "Element count [" + actualObjectCount + "] is stored in " + variableName,
                            Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing JSON element in Variable :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given condition [" + Condition + "] format is invalid. It should be [%Var%]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element in Variable :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }

    }

    public int getJsonElementCount() throws org.json.simple.parser.ParseException {

        int actualObjectCount = 0;

        JSONParser parser = new JSONParser();

        JSONObject json = (JSONObject) parser.parse(responsebodies.get(key));

        try {
            Map<String, String> objectMap = JsonPath.read(json, Condition);
            actualObjectCount = objectMap.keySet().size();
        } catch (Exception ex) {
            try {
                JSONArray objectMap = JsonPath.read(json, Condition);
                actualObjectCount = objectMap.size();
            } catch (Exception ex1) {
                try {
                    net.minidev.json.JSONArray objectMap = JsonPath.read(json, Condition);
                    actualObjectCount = objectMap.size();
                } catch (Exception ex2) {
                    String objectMap = JsonPath.read(json, Condition);
                    actualObjectCount = 1;
                }
            }
        }
        return actualObjectCount;
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element count in Datasheet ", input = InputType.YES, condition = InputType.YES)
    public void storeJsonElementCountInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    int actualObjectCountInteger = 1;                                                                         //getJsonElementCount();
                    String actualObjectCount = Integer.toString(actualObjectCountInteger);
                    userData.putData(sheetName, columnName, actualObjectCount);
                    Report.updateTestLog(Action, "Element count [" + actualObjectCount + "] is stored in " + strObj,
                            Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }

    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Add Header ", input = InputType.YES)
    public void addHeader() {
        try {

            List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                    .getTestDataNames();
            for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
                if (Data.contains("{" + sheetlist.get(sheet) + ":")) {
                    com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject().getTestData()
                            .getTestDataByName(sheetlist.get(sheet));
                    List<String> columns = tdModel.getColumns();
                    for (int col = 0; col < columns.size(); col++) {
                        if (Data.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                            Data = Data.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                    userData.getData(sheetlist.get(sheet), columns.get(col)));
                        }
                    }
                }
            }

            Collection<Object> valuelist = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
                    .values();
            for (Object prop : valuelist) {
                if (Data.contains("{" + prop + "}")) {
                    Data = Data.replace("{" + prop + "}", prop.toString());
                }
            }

            Pattern pattern = Pattern.compile("%\\w+%");
            Matcher matcher = pattern.matcher(Data);

            while (matcher.find()) {
                String variable = matcher.group();
                if (getVar(variable) != null) {
                    Data = Data.replaceAll(variable, getVar(variable));
                } else {
                    Report.updateTestLog(Action, "Variable " + variable + " not found", Status.DEBUG);
                }
            }

            if (headers.containsKey(key)) {
                headers.get(key).add(Data);
            } else {
                ArrayList<String> toBeAdded = new ArrayList<String>();
                toBeAdded.add(Data);
                headers.put(key, toBeAdded);
            }
            
            Report.updateTestLog(Action, "Header added [" + Data + "]", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error adding Header :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Add Parameters ", input = InputType.YES)
    public void addURLParam() {

        try {
            if (urlParams.containsKey(key)) {
                urlParams.get(key).add(Data);
            } else {
                ArrayList<String> toBeAdded = new ArrayList<String>();
                toBeAdded.add(Data);
                urlParams.put(key, toBeAdded);
            }
            Report.updateTestLog(Action, "URl Param added " + Data, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error adding Header :" + "\n" + ex.getMessage(), Status.DEBUG);
        }

    }

    @Action(object = ObjectType.WEBSERVICE, desc = "Close the connection ", input = InputType.NO)
    public void closeConnection() {
        try {
            // httpConnections.get(key).disconnect();
            headers.remove(key);
            responsebodies.remove(key);
            basicAuthorization = "";
            responsecodes.remove(key);
            responsemessages.remove(key);
            endPoints.remove(key);
            Report.updateTestLog(Action, "Connection is closed", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error closing connection :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

}
