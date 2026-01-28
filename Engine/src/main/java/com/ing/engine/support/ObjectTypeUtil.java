package com.ing.engine.support;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.ing.engine.support.methodInf.ObjectType;

/**
 * Utility class for managing and querying object types at runtime.
 */
public final class ObjectTypeUtil {
    // private static final Set<String> objectTypes = new HashSet<>();
    private static final Set<String> pluginObjectTypes = new HashSet<>();
    private static final List<String> objectTypesforIDE = new ArrayList<>();

    private ObjectTypeUtil() {}

    // static {
    //     objectTypes.addAll(ObjectType.initialObjectTypes);
    // }

    /**
     * Registers a new object type.
     *
     * @param type the object type to register
     */
    // public static void registerType(String type) {
    //     if (type != null && !type.isEmpty()) {
    //         objectTypes.add(type);
    //         pluginObjectTypes.add(type);
    //         System.out.println("Registered new object type: " + type);
    //     }
    // }
    public static void registerObjectTypefromPlugin(String type) {
        if (type != null && !type.isEmpty() && !ObjectType.initialObjectTypes.contains(type)) {
            pluginObjectTypes.add(type);
            System.out.println("Registered new object type: " + type);
        }
    }

    /**
     * Checks if the given type is registered.
     *
     * @param type the object type to check
     * @return true if registered, false otherwise
     */
    public static boolean isKnownType(String type) {
        return objectTypesforIDE.contains(type);
    }

    // /**
    //  * Returns an unmodifiable set of all registered object types.
    //  *
    //  * @return set of object types
    //  */
    // public static Set<String> getAllTypes() {
    //     return Collections.unmodifiableSet(objectTypes);
    // }

    public static List<String> getAllTypesForIDE() {
        // objectTypesforIDE.clear();
        objectTypesforIDE.add(ObjectType.BROWSER);
        objectTypesforIDE.add(ObjectType.MOBILE);
        objectTypesforIDE.add(ObjectType.WEBSERVICE);
        objectTypesforIDE.add(ObjectType.DATABASE);
        objectTypesforIDE.add(ObjectType.KAFKA);
        objectTypesforIDE.add(ObjectType.QUEUE);
        objectTypesforIDE.add("Synthetic Data");
        objectTypesforIDE.add(ObjectType.FILE);
        objectTypesforIDE.add(ObjectType.GENERAL);
        objectTypesforIDE.add("EXECUTE");
        objectTypesforIDE.add(ObjectType.STRINGOPERATIONS);
        objectTypesforIDE.addAll(pluginObjectTypes);
        return Collections.unmodifiableList(objectTypesforIDE);
    }
}
