
package com.ing.engine.support.methodInf;


import com.ing.datalib.component.TestStep;
import com.ing.engine.constants.FilePath;
import com.ing.engine.support.AnnontationUtil;
import com.ing.engine.support.reflect.Discovery;
import com.ing.engine.support.reflect.MethodExecutor;
import com.ing.engine.support.ObjectTypeUtil;
import com.ing.exceptions.DuplicateMethodException;
import eu.infomas.annotation.AnnotationDetector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages discovery and lookup of methods annotated with {@link Action} in the application and plugins.
 * <p>
 * This class scans the main packages for actions and plugin JARs for methods annotated with {@code @Action},
 * builds a map of method names to their {@code Action} metadata, and provides lookup utilities for
 * descriptions and method lists by object type.
 */
public class MethodInfoManager {
    
    /**
     * Map of method names to their associated {@link Action} annotation metadata.
     */
    public static Map<String, Action> methodInfoMap = new HashMap<>();
    private static boolean isDuplicateMethodDetected = false;
    private static Map<String, Set<String>> objectTypeMethodMap = new HashMap<>();
    
    private static final AnnotationDetector.MethodReporter METHOD_REPORTER = new AnnotationDetector.MethodReporter() {
        
        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Annotation>[] annotations() {
            return new Class[]{Action.class};
        }
        
        @Override
        public void reportMethodAnnotation(Class<? extends Annotation> annotation,
                String className, String methodName) {
            loadMethodAndRegisterType(className, methodName);
        }
        
    };
    
    private static final AnnotationDetector ANNOTATION_DETECTOR = new AnnotationDetector(METHOD_REPORTER);
    
    /**
     * Loads all methods annotated with {@link Action} from the main application and plugin JARs.
     * <p>
     * This method clears the current method info map, initializes method executors, and scans
     * both the action packages and all plugin JARs for annotated methods.
     */
    public static void load() throws DuplicateMethodException {
        MethodExecutor.init();
        methodInfoMap.clear();
        AnnontationUtil.detect(ANNOTATION_DETECTOR, "com.ing.engine.commands");

        File basePluginDir = new File(FilePath.getAppRoot() + "/plugins");
        String[] jarPaths = getPluginJarPaths(basePluginDir);
        AnnontationUtil.detectFromPluginPaths(ANNOTATION_DETECTOR, jarPaths);

        if(isDuplicateMethodDetected){
            System.out.println("Duplicate method names detected in the loaded actions. Please resolve the conflicts.");
            throw new DuplicateMethodException("Duplicate method names detected in the loaded actions. Please resolve the conflicts.");
        }
    }
    
    /**
     * Loads the method with the given class and method name, stores its {@link Action} annotation in the map,
     * and registers the object type in {@link ObjectTypeUtil}.
     *
     * @param className the fully qualified class name
     * @param methodName the method name to load
     */
    private static void loadMethodAndRegisterType(String className, String methodName) {
        try {
            Method method = getClass(className).getMethod(methodName);
            Action mInfo = method.getAnnotation(Action.class);
            ObjectTypeUtil.registerObjectTypefromPlugin(mInfo.object());
            if (isDuplicateMethodForObjectType(methodName, mInfo.object())) {
                String originalObject = methodInfoMap.get(methodName).object();
                String currentLocation = getPluginFolderName(method);

                System.out.println("Duplicate action '" + methodName + "' for object type '" + mInfo.object() + "' detected:\n" +
                   "  - Duplicate found in: " + currentLocation + "\n" +
                   "  - Class: " + className);
                isDuplicateMethodDetected = true; // Set flag and return early
                return; // Don't register the duplicate
            }
            methodInfoMap.put(methodName, mInfo);
            registerMethodToObjectType(methodName, mInfo.object());
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(MethodInfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Attempts to retrieve a {@link Class} object for the specified class name.
     * <p>
     * This method first tries to obtain the class using {@link Discovery#getClassByName(String)}.
     * If unsuccessful, it falls back to using {@link Class#forName(String)}.
     * If the class cannot be found, it logs the exception and returns {@code null}.
     * </p>
     *
     * @param className the fully qualified name of the desired class
     * @return the {@link Class} object representing the class, or {@code null} if not found
     */
    private static Class<?> getClass(String className) {
        try {
            Class<?> class_ = Discovery.getClassByName(className);
            if (class_ != null) {
                return class_;
            }
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MethodInfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * Retrieves a sorted list of method names associated with the specified {@link ObjectType}
     * and any additional {@link ObjectType}s provided.
     *
     * <p>
     * The method iterates through the {@code methodInfoMap} and collects the names of methods
     * whose associated object type matches the given {@code type} or any of the {@code others}.
     * The resulting list of method names is sorted in natural order before being returned.
     * </p>
     *
     * @param type   the primary {@link ObjectType} to filter methods by
     * @param others additional {@link ObjectType}s to include in the filter (optional)
     * @return a sorted {@link List} of method names matching the specified object types
     */
    public static List<String> getMethodListFor(String type, String... others) {
        List<String> methodList = new ArrayList<>();
        for (Map.Entry<String, Action> entry : methodInfoMap.entrySet()) {
            String methodName = entry.getKey();
            Action mInfo = entry.getValue();
            if (mInfo.object().equals(type)
                    || (others != null
                    && Arrays.asList(others).contains(mInfo.object()))) {
                methodList.add(methodName);
            }
        }
        Collections.sort(methodList);
        return methodList;
    }
    
    /**
     * Returns the description for the given action method, if available.
     *
     * @param action the method name
     * @return the description from the {@link Action} annotation, or an empty string if not found
     */
    public static String getDescriptionFor(String action) {
        if (methodInfoMap.containsKey(action)) {
            return methodInfoMap.get(action).desc();
        }
        return "";
    }
    
    /**
     * Returns the resolved description for a test step, replacing placeholders with actual values.
     *
     * @param step the test step
     * @return the resolved description string
     */
    public static String getResolvedDescriptionFor(TestStep step) {
        return getDescriptionFor(step.getAction())
                .replace("[<Object>]", step.getObject())
                .replace("[<Object2>]", step.getCondition())
                .replace("[<Data>]", step.getInput());
    }

    /**
     * Returns an array of absolute paths to all plugin JAR files found in subdirectories of the given base plugin directory.
     *
     * @param basePluginDir the root plugin directory
     * @return array of absolute paths to plugin JAR files, or empty array if none found
     */
    private static String[] getPluginJarPaths(File basePluginDir) {
        if (basePluginDir == null || !basePluginDir.isDirectory()) {
            return new String[0];
        }
        File[] pluginDirs = basePluginDir.listFiles(File::isDirectory);
        if (pluginDirs == null) {
            return new String[0];
        }
        return Arrays.stream(pluginDirs) // Stream each plugin directory
            .flatMap(dir -> Arrays.stream( // For each directory, stream its files
                dir.listFiles(file ->      // Only include files that:
                    file.isFile() &&       // - are regular files
                    file.getName().endsWith(".jar") // - have a .jar extension
                )
            ))
            .map(File::getAbsolutePath)    // Map each File to its absolute path
            .toArray(String[]::new);       // Collect results into a String array
    }
    
    /**
     * Registers a method name to its associated object type.
     * <p>
     * This method maintains a mapping of object types to their registered method names,
     * allowing efficient lookup and duplicate detection.
     * </p>
     *
     * @param methodName the name of the method to register
     * @param objectType the object type associated with the method
     */
    private static void registerMethodToObjectType(String methodName, String objectType) {
        objectTypeMethodMap
            .computeIfAbsent(objectType, k -> new HashSet<>())
            .add(methodName);
    }

    /**
     * Checks if a method is already registered for the specified object type (duplicate detection).
     * <p>
     * This method is used for duplicate detection to ensure that each method name
     * is unique within its object type context.
     * </p>
     *
     * @param methodName the name of the method to check
     * @param objectType the object type to check against
     * @return {@code true} if the method is already registered for this object type (duplicate), {@code false} otherwise
     */
    private static boolean isDuplicateMethodForObjectType(String methodName, String objectType) {
        return objectTypeMethodMap.containsKey(objectType) &&
               objectTypeMethodMap.get(objectType).contains(methodName);
    }

    /**
     * Extracts the plugin folder name from the method's class location.
     * <p>
     * For plugin classes, returns the name of the folder containing the plugin JAR.
     * For core application classes, returns "core".
     * </p>
     *
     * @param method the method to get the plugin folder for
     * @return the plugin folder name or "core" for application classes
     */
    private static String getPluginFolderName(Method method) {
        try {
            java.net.URL location = method.getDeclaringClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            if (location != null) {
                String path = location.getPath();
                // Extract plugin folder name from path like: /path/to/plugins/plugin-name/plugin.jar
                if (path.contains("/plugins/")) {
                    int pluginsIndex = path.indexOf("/plugins/");
                    String afterPlugins = path.substring(pluginsIndex + "/plugins/".length());
                    int nextSlash = afterPlugins.indexOf("/");
                    if (nextSlash > 0) {
                        return afterPlugins.substring(0, nextSlash);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return default
        }
        return "core";
    }
    
}
