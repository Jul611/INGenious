package com.ing.engine.drivers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Automatically detects installed browser executable paths on the local system.
 * Supports Chrome and Firefox across Windows, macOS, and Linux.
 */
public class BrowserPathDetector {

    public enum BrowserType {
        CHROME("chrome"),
        FIREFOX("firefox");

        private final String name;

        BrowserType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Detects the executable path for a given browser type.
     * @param browserType The type of browser to detect
     * @return The path to the browser executable, or null if not found
     */
    public static String detectBrowserPath(BrowserType browserType) {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return detectWindowsBrowser(browserType);
        } else if (os.contains("mac")) {
            return detectMacBrowser(browserType);
        } else {
            return detectLinuxBrowser(browserType);
        }
    }

    private static String detectWindowsBrowser(BrowserType browserType) {
        List<String> possiblePaths = new ArrayList<>();

        switch (browserType) {
            case CHROME:
                possiblePaths.add("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
                possiblePaths.add(
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
                );
                break;
            case FIREFOX:
                possiblePaths.add("C:\\Program Files\\Mozilla Firefox\\firefox.exe");
                possiblePaths.add("C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe");
                break;
        }

        return findFirstExistingPath(possiblePaths);
    }

    private static String detectMacBrowser(BrowserType browserType) {
        List<String> possiblePaths = new ArrayList<>();

        switch (browserType) {
            case CHROME:
                possiblePaths.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
                break;
            case FIREFOX:
                possiblePaths.add("/Applications/Firefox.app/Contents/MacOS/firefox");
                break;
        }

        return findFirstExistingPath(possiblePaths);
    }

    private static String detectLinuxBrowser(BrowserType browserType) {
        List<String> possiblePaths = new ArrayList<>();

        switch (browserType) {
            case CHROME:
                possiblePaths.add("/usr/bin/google-chrome");
                possiblePaths.add("/snap/bin/google-chrome");
                break;
            case FIREFOX:
                possiblePaths.add("/usr/bin/firefox");
                possiblePaths.add("/snap/bin/firefox");
                break;
        }

        return findFirstExistingPath(possiblePaths);
    }

    private static String findFirstExistingPath(List<String> paths) {
        for (String path : paths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }
        return null;
    }
}
