
package com.ing.engine.support.methodInf;

import java.util.Set;
import java.util.HashSet;

public class ObjectType {
    public static final String BROWSER = "BROWSER";
    public static final String WEB = "WEB";
    public static final String MOBILE = "MOBILE";
    public static final String IMAGE = "IMAGE";
    public static final String PLAYWRIGHT = "PLAYWRIGHT";
    public static final String APP = "APP";
    public static final String DATABASE = "DATABASE";
    public static final String PROTRACTORJS = "PROTRACTORJS";
    public static final String ANY = "ANY";
    public static final String WEBSERVICE = "WEBSERVICE";
    public static final String FILE = "FILE";
    public static final String KAFKA = "KAFKA";
    public static final String QUEUE = "QUEUE";
    public static final String DATA = "DATA";
    public static final String GENERAL = "GENERAL";
    public static final String STRINGOPERATIONS = "STRINGOPERATIONS";

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
