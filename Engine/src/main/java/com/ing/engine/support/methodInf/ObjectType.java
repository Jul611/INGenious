
package com.ing.engine.support.methodInf;

import java.util.Set;
import java.util.HashSet;

/**
 * Defines standard object type constants used throughout the framework.
 * <p>
 * This class provides constant definitions for all built-in object types that the framework
 * supports, including Browser, Mobile, Database, Webservice, and various other automation
 * object types. These constants are used for method routing, validation, and IDE auto-suggest.
 * </p>
 *
 * @see com.ing.engine.support.ObjectTypeUtil
 * @see MethodInfoManager
 */
public class ObjectType {
    /** Browser automation object type */
    public static final String BROWSER = "Browser";
    /** Web element object type */
    public static final String WEB = "Web";
    /** Mobile automation object type */
    public static final String MOBILE = "Mobile";
    /** Image processing object type */
    public static final String IMAGE = "Image";
    /** Playwright browser automation object type */
    public static final String PLAYWRIGHT = "Playwright";
    /** Mobile application object type */
    public static final String APP = "App";
    /** Database operations object type */
    public static final String DATABASE = "Database";
    /** ProtractorJS automation object type */
    public static final String PROTRACTORJS = "ProtractorJS";
    /** Generic object type for universal actions */
    public static final String ANY = "Any";
    /** Web service operations object type */
    public static final String WEBSERVICE = "Webservice";
    /** File operations object type */
    public static final String FILE = "File";
    /** Kafka messaging object type */
    public static final String KAFKA = "Kafka";
    /** Queue messaging object type */
    public static final String QUEUE = "Queue";
    /** Synthetic data generation object type */
    public static final String DATA = "Data";
    /** General utility operations object type */
    public static final String GENERAL = "General";
    /** String manipulation operations object type */
    public static final String STRINGOPERATIONS = "String Operations";

    
}
