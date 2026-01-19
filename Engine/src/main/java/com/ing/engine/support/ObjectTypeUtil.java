package com.ing.engine.support;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for managing and querying object types at runtime.
 */
public final class ObjectTypeUtil {
    private static final Set<String> objectTypes = new HashSet<>();

    private ObjectTypeUtil() {}

    /**
     * Registers a new object type.
     *
     * @param type the object type to register
     */
    public static void registerType(String type) {
        if (type != null && !type.isEmpty()) {
            objectTypes.add(type);
        }
    }

    /**
     * Checks if the given type is registered.
     *
     * @param type the object type to check
     * @return true if registered, false otherwise
     */
    public static boolean isKnownType(String type) {
        return objectTypes.contains(type);
    }

    /**
     * Returns an unmodifiable set of all registered object types.
     *
     * @return set of object types
     */
    public static Set<String> getAllTypes() {
        return Collections.unmodifiableSet(objectTypes);
    }
}
