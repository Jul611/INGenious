package com.ing.datalib.component;

/**
 * Marker class to identify a test case as a shared reusable component.
 * <p>
 * In INGenious v3.0's filesystem-based architecture, shared reusable test cases
 * are stored in the {@code SharedReusableComponents} folder at the workspace root
 * rather than within individual projects.
 * </p>
 */
public class SharedReusable {

    private String executableType = "Executable";

    private String group;

    public String getExecutableType() {
        return executableType;
    }

    public void setExecutableType(String executableType) {
        this.executableType = executableType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

}
