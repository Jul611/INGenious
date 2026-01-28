
package com.ing.engine.support.methodInf;

import java.util.Set;
import java.util.HashSet;

public class ObjectType {
    public static final String BROWSER = "Browser";
    public static final String WEB = "Web";
    public static final String MOBILE = "Mobile";
    public static final String IMAGE = "Image";
    public static final String PLAYWRIGHT = "Playwright";
    public static final String APP = "App";
    public static final String DATABASE = "Database";
    public static final String PROTRACTORJS = "ProtractorJS";
    public static final String ANY = "Any";
    public static final String WEBSERVICE = "Webservice";
    public static final String FILE = "File";
    public static final String KAFKA = "Kafka";
    public static final String QUEUE = "Queue";
    public static final String DATA = "Data";
    public static final String GENERAL = "General";
    public static final String STRINGOPERATIONS = "String Operations";

    public static final Set<String> initialObjectTypes = new HashSet<String>() {{
        add(BROWSER);
        add(WEB);
        add(MOBILE);
        add(IMAGE);
        add(PLAYWRIGHT);
        add(APP);
        add(DATABASE);
        add(PROTRACTORJS);
        add(ANY);
        add(WEBSERVICE);
        add(FILE);
        add(KAFKA);
        add(QUEUE);
        add(DATA);
        add(GENERAL);
        add(STRINGOPERATIONS);
        }};

    public static Set<String> getObjectTypes() {
        return new HashSet<>(initialObjectTypes);
    }
    
}
