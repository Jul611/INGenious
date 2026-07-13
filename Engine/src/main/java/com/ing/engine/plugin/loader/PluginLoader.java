package com.ing.engine.plugin.loader;

import com.ing.engine.constants.FilePath;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PluginLoader is responsible for discovering and loading plugin entry classes
 * from plugin JARs located in the application's plugin directory. It uses a child-first class loader
 * strategy to ensure plugin classes and their dependencies are loaded in isolation from the main application.
 * <p>
 * Scans both {@code Resources/plugins/} (IDE-side installed plugins) and
 * {@code {appRoot}/plugins/} (legacy/engine-side) directories.
 */
public class PluginLoader {
    private static final Logger LOG = Logger.getLogger(PluginLoader.class.getName());

    private static final String RESOURCES_PLUGINS_DIR = "Resources/plugins";

    /**
     * Loads all plugin entry classes from the plugins directory.
     * <p>
     * This method scans each plugin folder in both {@code Resources/plugins/}
     * and {@code {appRoot}/plugins/}, collects all plugin JARs and their dependencies,
     * and loads the classes specified as entry points in the JAR manifest (pluginEntryClasses attribute).
     *
     * @return a list of loaded plugin entry classes
     * @throws IllegalArgumentException if neither plugin directory exists
     */
    public static List<Class<?>> loadAllPluginsEntryClasses() {
        List<Class<?>> classes = new ArrayList<>();

        // Scan Resources/plugins/ first (IDE-side installed plugins)
        File resourcesDir = new File(RESOURCES_PLUGINS_DIR);
        if (resourcesDir.exists() && resourcesDir.isDirectory()) {
            LOG.info("Scanning plugins from: " + resourcesDir.getAbsolutePath());
            loadPluginsFromDirectory(resourcesDir, classes);
        }

        // Also scan legacy appRoot/plugins/ (engine-side)
        File baseDir = new File(FilePath.getAppRoot() + "/plugins");
        if (baseDir.exists() && baseDir.isDirectory() && !baseDir.equals(resourcesDir)) {
            LOG.info("Scanning plugins from: " + baseDir.getAbsolutePath());
            loadPluginsFromDirectory(baseDir, classes);
        }

        if (classes.isEmpty()) {
            LOG.warning("No plugin entry classes found in any plugin directory");
        }

        return classes;
    }

    /**
     * Scans a single plugin directory for plugin JARs and loads entry classes.
     */
    private static void loadPluginsFromDirectory(File baseDir, List<Class<?>> classes) {
        // Iterate over each plugin folder
        File[] pluginDirs = baseDir.listFiles(File::isDirectory);
        if (pluginDirs == null) return;

        for (File pluginFolder : pluginDirs) {
            if (pluginFolder.getName().startsWith(".")) continue; // skip hidden

            // Find all plugin JARs (any *.jar in the plugin folder, not in lib)
            File[] jarFiles = pluginFolder.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                LOG.fine("No plugin JAR found in: " + pluginFolder.getAbsolutePath());
                continue;
            }
            File libDir = new File(pluginFolder, "lib"); // Dependencies folder

            // Collect all JARs for this plugin (all found jars)
            List<URL> jarUrls;
            try {
                jarUrls = collectPluginJarsUrls(libDir, jarFiles);
                // Create child-first loader for this plugin
                ClassLoader pluginClassLoader = new PluginClassLoader(
                    jarUrls.toArray(new URL[0]),
                    PluginLoader.class.getClassLoader()
                );

                // get the entry classes for each plugin JAR
                for (File pluginJar : jarFiles) {
                    List<String> entryClasses;
                    try {
                        entryClasses = getEntryClasses(pluginJar);
                        for (String entryClass : entryClasses) {
                            classes.add(pluginClassLoader.loadClass(entryClass));
                        }
                    } catch (Exception ex) {
                        LOG.log(
                            Level.WARNING,
                            "Error loading entry classes from: " + pluginJar.getName(),
                            ex
                        );
                    }
                }
            } catch (Exception ex) {
                LOG.log(
                    Level.SEVERE,
                    "Error processing plugin folder: " + pluginFolder.getName(),
                    ex
                );
            }
        }
    }

    // Accepts one or more plugin JARs, plus an optional libDir for dependencies
    /**
     * Collects URLs for the given plugin JARs and their dependencies in the optional lib directory.
     *
     * @param libDir the directory containing dependency JARs (may be null)
     * @param pluginJars one or more plugin JAR files
     * @return a list of URLs for all plugin and dependency JARs
     * @throws Exception if a JAR file is missing or cannot be converted to a URL
     */
    private static List<URL> collectPluginJarsUrls(File libDir, File... pluginJars)
        throws Exception {
        List<URL> urls = new ArrayList<>();

        // Add all provided plugin JARs
        for (File pluginJar : pluginJars) {
            if (pluginJar.exists()) {
                urls.add(pluginJar.toURI().toURL());
            } else {
                throw new IllegalArgumentException(
                    "Plugin JAR not found: " + pluginJar.getAbsolutePath()
                );
            }
        }

        // Add all dependency JARs from lib directory
        if (libDir != null && libDir.exists() && libDir.isDirectory()) {
            File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    urls.add(jar.toURI().toURL());
                }
            }
        }
        return urls;
    }

    /**
     * Reads the pluginEntryClasses attribute from the manifest of the given plugin JAR.
     *
     * @param pluginJar the plugin JAR file
     * @return a list of entry class names specified in the manifest
     * @throws Exception if the manifest is missing or the attribute is not found
     */
    private static List<String> getEntryClasses(File pluginJar) throws Exception {
        try (JarFile jar = new JarFile(pluginJar)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                String entries = attrs.getValue("pluginEntryClasses");
                if (entries != null) {
                    return Arrays.asList(entries.split(","));
                }
            }
        }
        throw new IllegalStateException("No pluginEntryClasses attribute found in manifest");
    }
}
