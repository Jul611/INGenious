---
name: ingenious-plugin-creation
description: 'Expert guidance for creating INGenious Playwright Framework plugins. USE FOR: creating new plugins, fixing plugin errors, configuring maven POMs, implementing action methods, working with Playwright objects, troubleshooting classloader issues, version compatibility problems, extending the framework with custom actions, converting customizations into plugins. INCLUDES: complete templates, architecture patterns, dependency management, best practices. INTEGRATES WITH: ingenious-customization-detection skill for extracting customizations as plugins.'
argument-hint: 'Describe the plugin to create or issue to fix'
---

# INGenious Plugin Creation Skill

## When to Use This Skill

Use this skill when you need to:
- Create a new plugin for the INGenious Playwright
- Fix plugin compilation or runtime errors
- Configure Maven POM for plugins
- Implement action methods with proper annotations
- Work with Playwright objects through the API
- Troubleshoot ClassCastException, NoSuchMethodError, or classloader issues
- Ensure version compatibility between plugin and framework
- Debug duplicate action names or manifest issues
- **Convert customizations into plugins** (see Skill Integration below)

## Key Principles

**🔴 MANDATORY: This is an INTERACTIVE workflow**

- **NEVER assume** where to create the plugin
- **ALWAYS confirm** the target directory with the user before creating files
- **STOP before creating** and present location options
- **Validate** the user's choice before proceeding
- **Allow custom paths** - don't force predefined locations

This ensures plugins are created in the correct location and prevents accidental file creation in wrong directories.

## Skill Integration: From Customizations to Plugins

**🔗 Works with:** `ingenious-customization-detection` skill

If you have customized INGenious framework code, you can:

1. **First:** Use `/ingenious-customization-detection` to analyze your customizations
2. **Then:** Use this skill to convert detected customizations into plugins

**Benefits of Converting Customizations to Plugins:**
- ✅ Easier to upgrade INGenious (no merge conflicts)
- ✅ Can be shared across multiple INGenious installations
- ✅ Isolated from core framework changes
- ✅ Easier to maintain and test independently
- ✅ Can be version-controlled separately

**Example Workflow:**
```
User: "Analyze my INGenious customizations"
→ Uses: ingenious-customization-detection skill
→ Output: Plugin specifications identified

User: "Create Plugin 1 from the analysis"
→ Uses: ingenious-plugin-creation skill (this skill)
→ Output: Fully functional plugin created
```

**When Receiving Plugin Specifications:**

This skill can accept structured input from the customization detection skill:
- Plugin name and type
- Action method specifications
- Source code snippets to convert
- Dependencies to include
- API contract to implement

## How to Ensure This Skill is Used

**For Creating New Plugins:**
This skill is automatically available when working in the plugin repository. To ensure it's used when creating new modules:

1. **Ask to create the new plugin** in the same conversation
2. **Reference the skill explicitly**: Say "Using the plugin skill, create..." or "@PLUGIN-CREATION-SKILL"

**For Editing Existing Plugins:**
The skill auto-activates when you open files in:
- Any directory containing `plugin` in the path
- Any Java file with `Plugin` in the name

**File Pattern Triggers:**
```
✅ Matches (skill will activate):
- browser-test-plugin/pom.xml
- mobile-test-plugin/src/main/java/.../*.java

```

## Plugin Architecture Overview

The INGenious Framework uses a sophisticated plugin architecture:

- **API Module** (`ingenious-api`): Stable interfaces between framework and plugins
- **Custom Classloaders**: Each plugin runs in isolated classloader environment
- **Type Erasure Pattern**: Playwright objects passed as `Object` type for version independence
- **Parent-First Delegation**: Critical packages (Playwright, API) loaded from parent classloader

This ensures plugins can be developed independently while maintaining compatibility.

## ⚠️ BEFORE YOU START: Confirm Directories

**🔴 CRITICAL REQUIREMENT: ALWAYS confirm BOTH directories before proceeding**

### Step 0: Determine Plugin Directories

**Action:** Ask the user TWO questions and wait for explicit confirmation for each.

#### Question 1: Where to Save Plugin Source Code?

**Purpose:** This is where the plugin's source code, POM, and development files will be created.

**Workflow:**

1. **Detect Available Options**
   - List workspace folders (development repositories)
   - Identify the Github-Plugins-sys-INGenious repository if present
   - Check for other potential plugin development locations
   
2. **Present Options to User**
   ```
   I'll create the [plugin-name] plugin for you.
   
   STEP 1/2: Where should I save the plugin SOURCE CODE?
   
   Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious/[plugin-name]/
     └─ Recommended for: Plugin development, version control, sharing
   
   Option 2: /Users/qs01nn/ing_code_repos/INGenious_project/my-plugins/[plugin-name]/
     └─ Recommended for: Personal plugin development
   
   Option 3: Specify a custom directory
   
   Where should I create the plugin source code?
   ```

3. **STOP and Wait for User Response**
   - Do NOT create any files without explicit confirmation
   - Do NOT assume a default location
   - Allow user to specify a custom path

4. **Validate User's Choice**
   - After user confirms, verify the path exists or can be created
   - Check write permissions
   - Store this as `SOURCE_CODE_DIR`

#### Question 2: Where is the INGenious Installation?

**Purpose:** This is where the compiled plugin will be deployed for testing/usage.

**Workflow:**

1. **Detect Available INGenious Installations**
   - Look for folders containing Configuration/, Engine/, Projects/
   - List all detected INGenious installations
   
2. **Present Options to User**
   ```
   STEP 2/2: Where is your INGenious installation (deployment target)?
   
   Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3/
     └─ Plugin will be deployed to: [path]/plugins/[plugin-name]/
   
   Option 2: /Users/qs01nn/Applications/INGenious/
     └─ Plugin will be deployed to: [path]/plugins/[plugin-name]/
   
   Option 3: Specify a custom INGenious installation path
   
   Option 4: Skip auto-deployment (I'll deploy manually)
   
   Where is your INGenious installation?
   ```

3. **STOP and Wait for User Response**
   - Allow user to specify INGenious installation path
   - Allow user to skip auto-deployment (Option 4)
   - If skipped, don't configure maven-antrun-plugin

4. **Validate INGenious Installation**
   - Verify it's a valid INGenious installation (has Configuration/, Engine/)
   - Check that plugins/ directory exists or can be created
   - Store this as `INGENIOUS_INSTALL_DIR`
   - Deployment path will be: `${INGENIOUS_INSTALL_DIR}/plugins/${plugin-name}/`

**User Confirmation Required:**

For **Question 1 (Source Code Location)**, user must explicitly:
- Select an option number (e.g., "Option 1" or "1")
- Provide an absolute path (e.g., "/path/to/my-plugins/")
- Confirm with "yes", "create it there", etc.

For **Question 2 (INGenious Installation)**, user must explicitly:
- Select an option number (e.g., "Option 1")
- Provide INGenious installation path (e.g., "/Applications/INGenious/")
- Choose "Option 4" to skip auto-deployment
- Confirm with "yes", "that's my installation", etc.

**Expected User Responses:**

Question 1 examples:
- "Option 1" or "The plugins repository"
- "/Users/qs01nn/my-custom-plugins/"
- "Create it in Github-Plugins-sys-INGenious"
- "Yes, that's fine"

Question 2 examples:
- "Option 1" or "Neil-ingenious-playwright-2.3"
- "/Users/qs01nn/Applications/INGenious/"
- "Skip auto-deployment" or "Option 4" or "I'll deploy manually"
- "That's my INGenious installation"

#### Summary After Confirmation

After both questions are answered, present a summary:

```
✓ Configuration Confirmed:

Source Code Location:
  └─ /Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious/custom-actions/

Deployment Target:
  └─ /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3/plugins/custom-actions/

When you run 'mvn package', the plugin will automatically deploy to the target location.

Proceeding with plugin creation...
```

Or if auto-deployment is skipped:

```
✓ Configuration Confirmed:

Source Code Location:
  └─ /Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious/custom-actions/

Deployment Target:
  └─ Manual deployment (maven-antrun-plugin not configured)

You'll need to manually copy the plugin after building.

Proceeding with plugin creation...
```

**Questions to Ask:**

Question 1 (Source Code):
- "Where would you like me to create the [plugin-name] plugin source code?"
- "Where should I save the plugin project files?"
- "Please specify the directory for plugin development"

Question 2 (Deployment):
- "Where is your INGenious installation?"
- "What's the path to the INGenious application where this plugin will be used?"
- "Should I configure auto-deployment? If yes, where is INGenious installed?"

**Directory Structure**

The two directories serve different purposes:

| Directory | Purpose | Contains | Example |
|-----------|---------|----------|---------|
| **Source Code Dir** | Development | pom.xml, src/, target/ (after build) | `/path/to/Github-Plugins-sys-INGenious/custom-actions/` |
| **Deployment Dir** | Runtime | plugin.jar, lib/ (dependencies) | `/path/to/INGenious/plugins/custom-actions/` |

**Directory Structure to Create in Source Code Dir:**

```
${SOURCE_CODE_DIR}/[plugin-name]/
├── pom.xml                          # Maven build configuration
└── src/
    └── main/
        └── java/
            └── com/
                └── [organization]/
                    └── plugin/
                        └── [PluginName].java    # Plugin entry class

After 'mvn package':
├── target/
│   ├── [plugin-name].jar            # Compiled plugin
│   └── lib/                         # Dependencies
│       └── [dependency].jar

If auto-deploy configured, also deployed to:
${INGENIOUS_INSTALL_DIR}/plugins/[plugin-name]/
├── [plugin-name].jar
└── lib/
    └── [dependency].jar
```

**Validation Checks:**

After confirming both directories, verify:

```bash
# Validation for Source Code Directory
SOURCE_CODE_DIR="[user-provided-path]"
PLUGIN_NAME="[plugin-name]"

# 1. Check if source code path exists or parent exists for creation
[ -d "$SOURCE_CODE_DIR" ] || [ -d "$(dirname "$SOURCE_CODE_DIR")" ]

# 2. Check write permissions
[ -w "$SOURCE_CODE_DIR" ] || [ -w "$(dirname "$SOURCE_CODE_DIR")" ]

# 3. Check if plugin already exists
if [ -d "$SOURCE_CODE_DIR/$PLUGIN_NAME" ]; then
    echo "⚠️ Warning: Plugin '$PLUGIN_NAME' already exists at this location"
    echo "Options:"
    echo "  1. Choose a different name"
    echo "  2. Choose a different location"
    echo "  3. Overwrite existing plugin (will backup first)"
    # STOP and wait for user choice
fi

# Validation for INGenious Installation Directory (if provided)
if [ "$DEPLOY_ENABLED" = "true" ]; then
    INGENIOUS_DIR="[user-provided-ingenious-path]"
    
    # 4. Verify it's a valid INGenious installation
    if [ ! -d "$INGENIOUS_DIR/Engine" ] || [ ! -d "$INGENIOUS_DIR/Configuration" ]; then
        echo "⚠️ Warning: '$INGENIOUS_DIR' doesn't appear to be a valid INGenious installation"
        echo "Expected to find: Engine/ and Configuration/ directories"
        echo "Options:"
        echo "  1. Provide a different path"
        echo "  2. Skip auto-deployment (configure manually later)"
        # STOP and wait for user choice
    fi
    
    # 5. Check if plugins directory exists or can be created
    DEPLOY_DIR="$INGENIOUS_DIR/plugins/$PLUGIN_NAME"
    if [ ! -d "$INGENIOUS_DIR/plugins" ]; then
        echo "Creating plugins directory: $INGENIOUS_DIR/plugins/"
        mkdir -p "$INGENIOUS_DIR/plugins"
    fi
    
    # 6. Check write permissions for deployment
    [ -w "$INGENIOUS_DIR/plugins" ] || {
        echo "⚠️ Warning: No write permission to $INGENIOUS_DIR/plugins/"
        echo "Auto-deployment may fail. Continue anyway? (yes/no)"
        # STOP and wait for user choice
    }
fi
```

**What NOT to do:**

- ❌ Do NOT assume workspace root is the source code location
- ❌ Do NOT assume deployment target from source code location
- ❌ Do NOT create files without confirming BOTH directories
- ❌ Do NOT skip asking about INGenious installation path
- ❌ Do NOT configure maven-antrun-plugin without deployment path
- ❌ Do NOT proceed if validation fails without user confirmation

**What TO do:**

- ✅ ALWAYS ask Question 1: Source code location
- ✅ ALWAYS ask Question 2: INGenious installation (deployment target)
- ✅ ALWAYS wait for explicit user confirmation for each question
- ✅ ALWAYS validate both paths after confirmation
- ✅ ALWAYS allow user to skip auto-deployment
- ✅ ALWAYS show summary of both directories before proceeding
- ✅ Use deployment path in maven-antrun-plugin configuration
- ✅ Create plugin structure ONLY after successful confirmation of both paths

**Example Interaction:**

```
User: "Create a browser plugin called custom-actions"

Agent: "I'll create a browser plugin named 'custom-actions' for you.

STEP 1/2: Where should I save the plugin SOURCE CODE?

Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious/custom-actions/
  └─ Recommended for plugin development and version control

Option 2: Specify custom directory

Which option?"

User: "Option 1"

Agent: "✓ Source code will be created at: 
/Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious/custom-actions/

STEP 2/2: Where is your INGenious installation (for auto-deployment)?

Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3/
  └─ Plugin will auto-deploy to: [path]/plugins/custom-actions/

Option 2: Specify custom INGenious path

Option 3: Skip auto-deployment (I'll deploy manually)

Which option?"

User: "Option 1"

Agent: "✓ Configuration Confirmed:

Source Code Location:
  └─ /Users/qs01nn/.../Github-Plugins-sys-INGenious/custom-actions/

Deployment Target:
  └─ /Users/qs01nn/.../Neil-ingenious-playwright-2.3/plugins/custom-actions/

Creating plugin structure...
✓ Created pom.xml with auto-deployment to Neil-ingenious-playwright-2.3
✓ Created src/main/java/com/ing/plugin/browser/CustomActionsPlugin.java
✓ Generated BrowserPluginApi template with @Action annotations

Plugin created successfully!

Next steps:
1. Build: cd custom-actions && mvn clean package
2. The plugin will automatically deploy to your INGenious installation
3. Launch INGenious and verify 'custom-actions' appears in the actions list
```

## Quick Start: Creating a Plugin

**⚠️ IMPORTANT:** Before following these steps, ensure you've completed **Step 0** above:
- ✅ Confirmed source code directory (where to create the plugin)
- ✅ Confirmed INGenious installation directory (where to deploy the plugin)

Once both directories are confirmed, proceed with creating the plugin files:

### 1. Maven POM Configuration

**Generate pom.xml with deployment path from Step 0**

The POM should be created at: `${SOURCE_CODE_DIR}/${plugin-name}/pom.xml`

**Critical Requirements:**
```xml
<properties>
    <!-- MUST match framework Java version -->
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>

<dependencies>
    <!-- REQUIRED: API with provided scope -->
    <dependency>
        <groupId>com.ing</groupId>
        <artifactId>ingenious-api</artifactId>
        <version>3.0</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- REQUIRED FOR BROWSER PLUGINS: Playwright with provided scope -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.50.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Why `provided` scope is critical:**
- Prevents ClassCastException by ensuring classes load from parent classloader
- Keeps plugin JAR small (~10KB vs ~10MB)
- Framework controls versions for compatibility

### 2. Build Configuration

```xml
<build>
    <plugins>
        <!-- Copy compile-scoped dependencies to lib folder -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
            <version>3.6.0</version>
            <executions>
                <execution>
                    <id>copy-dependencies</id>
                    <phase>package</phase>
                    <goals>
                        <goal>copy-dependencies</goal>
                    </goals>
                    <configuration>
                        <outputDirectory>${project.build.directory}/lib</outputDirectory>
                        <includeScope>compile</includeScope>
                        <excludeTransitive>false</excludeTransitive>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        
        <!-- Declare entry classes in JAR manifest -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <version>3.3.0</version>
            <configuration>
                <archive>
                    <manifestEntries>
                        <!-- Single line, comma-separated class names -->
                        <pluginEntryClasses>com.example.plugin.MyPlugin</pluginEntryClasses>
                        <Implementation-Version>${project.version}</Implementation-Version>
                    </manifestEntries>
                </archive>
            </configuration>
        </plugin>
        
        <!-- Optional: Auto-deploy to INGenious plugins directory -->
        <!-- NOTE: This is configured based on the deployment path from Step 0, Question 2 -->
        <!-- If user selected "Skip auto-deployment", omit this plugin entirely -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-antrun-plugin</artifactId>
            <version>3.1.0</version>
            <executions>
                <execution>
                    <id>copy-artifacts</id>
                    <phase>package</phase>
                    <configuration>
                        <target>
                            <!-- deploy.dir should be set to: ${INGENIOUS_INSTALL_DIR}/plugins/${project.artifactId} -->
                            <!-- Example: /Users/qs01nn/.../Neil-ingenious-playwright-2.3/plugins/custom-actions -->
                            <property name="deploy.dir" 
                                      value="/path/to/INGenious/plugins/${project.artifactId}"/>
                            <mkdir dir="${deploy.dir}"/>
                            <mkdir dir="${deploy.dir}/lib"/>
                            <copy file="${project.build.directory}/${project.build.finalName}.jar"
                                  tofile="${deploy.dir}/${project.artifactId}.jar"
                                  overwrite="true"/>
                            <copy todir="${deploy.dir}/lib" overwrite="true">
                                <fileset dir="${project.build.directory}/lib"/>
                            </copy>
                            
                            <!-- Optional: Display deployment confirmation -->
                            <echo message="Plugin deployed to: ${deploy.dir}"/>
                        </target>
                    </configuration>
                    <goals>
                        <goal>run</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 3. Plugin Entry Class Pattern

**⚠️ MANDATORY: Use the COMPLETE constructor pattern shown below - do not simplify**

**Browser Plugin Example:**
```java
package com.ing.plugin.browser;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.BrowserPluginApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.status.Status;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;

public class BrowserTestPlugin {
    
    BrowserPluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public String ObjectName;
    
    // Cast Playwright objects once in constructor
    public Page Page;
    public Locator Locator;

    public BrowserTestPlugin(BrowserPluginApi gen) {
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.ObjectName = gen.getObjectName();
        
        // Cast once for type safety and IDE autocomplete
        this.Page = (Page) gen.getPage();
        this.Locator = (Locator) gen.getLocator();
    }

    @Action(object = ObjectType.BROWSER, 
            desc = "Open the Url [<Data>] in the Browser", 
            input = InputType.YES)
    public void Open() {
        try {
            Page.navigate(Data);
            Report.updateTestLog(Action, "Opened " + Data, Status.DONE);
        } catch (PlaywrightException e) {
            Report.updateTestLog(Action, 
                "Error: " + e.getMessage(), 
                Status.FAIL);
        }
    }
}
```

**⚠️ CRITICAL REMINDER: Constructor Pattern**

The constructor shown above is the **COMPLETE pattern** - always copy/follow this exact initialization sequence:

1. Store API contract: `this.gen = gen;`
2. Initialize ALL test data fields: `Data`, `Action`, `Input`, `Condition`, `ObjectName`
3. Initialize `Report` API
4. Initialize `userData` (if applicable to the API type)
5. Cast and store Playwright/Appium objects

**DO NOT simplify or omit ANY field initialization**, even if your current actions don't use all fields. This ensures:
- Plugin remains extendable without constructor changes
- All framework features accessible for future enhancements
- Consistency with framework patterns
- No runtime NullPointerException errors

**🔴 CRITICAL: Complete Constructor Pattern Required**

**ALWAYS use the complete constructor pattern shown above. DO NOT simplify or omit any initialization steps.**

**Why this matters:**
- ✅ Ensures full API compatibility if plugin is extended later
- ✅ All framework features remain accessible for future enhancements
- ✅ Users can add new actions without rewriting initialization
- ✅ Prevents runtime errors from missing field initialization
- ✅ Maintains consistency across all plugins

**Complete initialization pattern (MANDATORY):**
```java
public BrowserTestPlugin(BrowserPluginApi gen) {
    // 1. Store the API contract instance
    this.gen = gen;
    
    // 2. Initialize ALL test data fields (even if not used immediately)
    this.Data = gen.getData();
    this.Action = gen.getAction();
    this.Input = gen.getInput();
    this.Condition = gen.getCondition();
    this.ObjectName = gen.getObjectName();
    
    // 3. Initialize Report API
    this.Report = gen.getReport();
    
    // 4. Initialize UserDataAccess API (if available)
    this.userData = gen.getUserData();  // For BrowserPluginApi
    
    // 5. Cast Playwright/Appium objects once (if applicable)
    this.Page = (Page) gen.getPage();
    this.Locator = (Locator) gen.getLocator();
}
```

**❌ DO NOT do this (incomplete pattern):**
```java
// Bad - missing fields, will break if plugin is extended
public BrowserTestPlugin(BrowserPluginApi gen) {
    this.gen = gen;
    this.Page = (Page) gen.getPage();  // Only initializes what's needed now
    // Missing: Data, Action, Input, Condition, Report, etc.
}
```

**Key Pattern Elements:**
1. **Constructor Injection**: Receive API contract via constructor
2. **Complete Initialization**: Initialize ALL available fields from the API contract
3. **Cast Once**: Cast Playwright/Appium objects in constructor, store as typed fields
4. **@Action Annotation**: Mark methods as plugin actions
5. **Error Handling**: Always catch exceptions and report via Report API
6. **Status Reporting**: Use `Report.updateTestLog()` for all outcomes

## Plugin API Contracts

Different plugin types use different API contracts:

| Plugin Type | API Contract | Constructor Parameter |
|-------------|--------------|----------------------|
| Browser | `BrowserPluginApi` | `BrowserPluginApi gen` |
| Mobile | `MobilePluginApi` | `MobilePluginApi gen` |
| Webservice | `WebservicePluginApi` | `WebservicePluginApi gen` |
| Database | `DatabasePluginApi` | `DatabasePluginApi gen` |
| General | `CommandPluginApi` | `CommandPluginApi gen` |

**Common API Methods:**
```java
// Test data
String data = gen.getData();
String action = gen.getAction();
String input = gen.getInput();
String condition = gen.getCondition();
String objectName = gen.getObjectName();

// Reporting
TestCaseReportApi report = gen.getReport();
report.updateTestLog(action, message, Status.DONE);

// Variable management
gen.addVar("%myVar%", "value");
String value = gen.getVar("%myVar%");

// Data sheet access (BrowserPluginApi)
UserDataAccessApi userData = gen.getUserData();
String value = userData.getData("SheetName", "ColumnName");
userData.putData("SheetName", "ColumnName", "Value");

// Playwright objects (BrowserPluginApi)
Page page = (Page) gen.getPage();
Locator locator = (Locator) gen.getLocator();
```

## Common Patterns

### Pattern 1: Element Interaction with Highlighting

```java
@Action(object = ObjectType.PLAYWRIGHT, 
        desc = "Click on [<Object>]")
public void Click() {
    try {
        highlightElement();
        Locator.click();
        Report.updateTestLog(Action, 
            "Clicked on '" + ObjectName + "'", 
            Status.DONE);
    } catch (PlaywrightException e) {
        Report.updateTestLog(Action, 
            "Element not found: " + e.getMessage(), 
            Status.FAIL);
    } finally {
        removeHighlightFromElement();
    }
}

private void highlightElement() {
    Locator.scrollIntoViewIfNeeded();
    Locator.evaluate("element => element.style.outline = '2px solid red'");
}

private void removeHighlightFromElement() {
    Locator.evaluate("element => element.style.outline = ''");
}
```

### Pattern 2: Variable Storage

```java
@Action(object = ObjectType.PLAYWRIGHT, 
        desc = "Store [<Object>] text in variable [<Data>]", 
        input = InputType.YES)
public void storeElementTextinVariable() {
    try {
        String text = Locator.textContent();
        String variableName = Data;
        
        if (!variableName.matches("%.*%")) {
            Report.updateTestLog(Action, 
                "Variable format incorrect. Expected: %variableName%", 
                Status.FAIL);
            return;
        }
        
        gen.addVar(variableName, text);
        Report.updateTestLog(Action, 
            "Stored text '" + text + "' in variable " + variableName, 
            Status.DONE);
    } catch (PlaywrightException e) {
        Report.updateTestLog(Action, 
            "Error: " + e.getMessage(), 
            Status.FAIL);
    }
}
```

### Pattern 3: Timeout Handling

```java
@Action(object = ObjectType.BROWSER, 
        desc = "Open URL [<Data>] with optional timeout [<Condition>]", 
        input = InputType.YES, 
        condition = InputType.OPTIONAL)
public void Open() {
    try {
        Page.NavigateOptions options = new Page.NavigateOptions();
        
        // Optional timeout from Condition field
        if (Condition != null && !Condition.isEmpty() && Condition.matches("[0-9]+")) {
            options.setTimeout(Double.parseDouble(Condition) * 1000);
        }
        
        Page.navigate(Data, options);
        Report.updateTestLog(Action, "Opened " + Data, Status.DONE);
        
    } catch (TimeoutError e) {
        if (Condition != null && !Condition.isEmpty()) {
            Report.updateTestLog(Action, 
                "Opened URL but cancelled after " + Condition + " seconds", 
                Status.DONE);
        } else {
            Report.updateTestLog(Action, "Page load timed out", Status.FAIL);
        }
    }
}
```

### Pattern 4: Webservice Request

```java
@Action(object = ObjectType.WEBSERVICE, 
        desc = "POST Rest Request", 
        input = InputType.YES)
public void postRestRequest() {
    try {
        gen.createHttpRequest(RequestMethod.POST);
        
        // Update local fields with response
        this.responseCode = gen.ResponseCode();
        this.responseBody = gen.ResponseBody();
        
        // createHttpRequest already logs, this is optional
        // Report.updateTestLog(Action, "POST successful", Status.DONE);
        
    } catch (Exception e) {
        Report.updateTestLog(Action,
            "Error: " + e.getMessage(),
            Status.FAIL);
    }
}
```

### Pattern 5: Mobile Element Interaction

```java
@Action(object = ObjectType.APP, desc = "Tap on [<Object>]")
public void Tap() {
    if (gen.elementEnabled()) {
        Element.click();
        Report.updateTestLog(Action, 
            "Tapped on " + ObjectName, 
            Status.DONE);
    } else {
        throw new ElementException(
            ExceptionType.Element_Not_Enabled, 
            ObjectName);
    }
}
```

## Troubleshooting Guide

### Error: ClassCastException

**Symptom:**
```
java.lang.ClassCastException: cannot cast com.microsoft.playwright.Page to com.microsoft.playwright.Page
```

**Causes & Solutions:**
1. **Wrong scope**: Playwright dependency must be `<scope>provided</scope>`
2. **Missing dependency**: Add Playwright with `provided` scope to POM
3. **Version mismatch**: Use exact version 1.50.0 to match framework

### Error: UnsupportedClassVersionError

**Symptom:**
```
java.lang.UnsupportedClassVersionError: has been compiled by a more recent version
```

**Solution:**
```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

### Error: NoSuchMethodError

**Symptom:**
```
java.lang.NoSuchMethodError: com.microsoft.playwright.Page.someMethod()
```

**Cause**: Using Playwright API not available in framework's version (1.50.0)

**Solution**: Update plugin's Playwright version to match:
```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.50.0</version>
    <scope>provided</scope>
</dependency>
```

### Error: Duplicate Action Names

**Symptom:**
```
Duplicate action 'Click' for object type 'PLAYWRIGHT' detected
```

**Solution**: Ensure unique action method names within each object type. Rename your action:
```java
// Before (conflicts with core)
public void Click() { }

// After (unique name)
public void ClickWithDelay() { }
```

### Error: Invalid Manifest Format

**Symptom:**
```
invalid manifest format (line 12)
```

**Cause**: Multi-line `pluginEntryClasses` in manifest

**Solution**: Keep entry classes on single line:
```xml
<!-- ✅ Correct - single line -->
<pluginEntryClasses>com.ing.plugin.A,com.ing.plugin.B</pluginEntryClasses>

<!-- ❌ Wrong - multi-line -->
<pluginEntryClasses>
    com.ing.plugin.A,
    com.ing.plugin.B
</pluginEntryClasses>
```

## Version Compatibility

### Current Framework Versions
- **Java**: 17
- **Playwright**: 1.50.0
- **API**: 3.0

### Compatibility Rules

| Requirement | Value | Why |
|-------------|-------|-----|
| Java Compiler | ≤ 17 | Framework JVM runs Java 17 |
| Playwright | 1.50.0 | Must match framework exactly |
| API Version | 3.0 | Current framework API |

**Java Compatibility Matrix:**

| Plugin Java | Framework Java | Result |
|-------------|----------------|--------|
| 17 | 17 | ✅ Recommended |
| 11 | 17 | ✅ Works |
| 21 | 17 | ❌ UnsupportedClassVersionError |

**Playwright Compatibility Matrix:**

| Plugin Version | Framework Version | Result |
|----------------|-------------------|--------|
| 1.50.0 | 1.50.0 | ✅ Recommended |
| 1.55.0 | 1.50.0 | ❌ NoSuchMethodError at runtime |
| 1.40.0 | 1.50.0 | ✅ Works (limited to 1.40.0 APIs) |

## Best Practices

### Constructor Pattern (CRITICAL)

**⚠️ Always use the complete constructor pattern - never simplify**

```java
// ✅ CORRECT - Complete initialization (use this pattern)
public BrowserTestPlugin(BrowserPluginApi gen) {
    this.gen = gen;
    this.Data = gen.getData();
    this.Action = gen.getAction();
    this.Input = gen.getInput();
    this.Condition = gen.getCondition();
    this.Report = gen.getReport();
    this.ObjectName = gen.getObjectName();
    this.userData = gen.getUserData();  // For BrowserPluginApi
    
    // Cast Playwright objects
    this.Page = (Page) gen.getPage();
    this.Locator = (Locator) gen.getLocator();
}

// ❌ WRONG - Incomplete initialization (will break if extended)
public BrowserTestPlugin(BrowserPluginApi gen) {
    this.gen = gen;
    this.Page = (Page) gen.getPage();  // Missing other fields
}

// ❌ WRONG - Selective initialization (fragile)
public BrowserTestPlugin(BrowserPluginApi gen) {
    this.gen = gen;
    this.Data = gen.getData();  // Only what you need now
    this.Page = (Page) gen.getPage();  // Missing: Action, Input, Condition, Report, etc.
}
```

**Why complete initialization matters:**
- Future-proofs the plugin for additional actions
- Prevents NullPointerException when accessing fields
- Maintains consistency with framework patterns
- Makes plugin extendable without constructor changes
- All API features remain accessible

### Action Naming Conventions

**Storage Actions:**
```java
// Format: store<Data>In<Destination>
storeDBValueInDataSheet()
storeResultInVariable()
storeValueInGlobalVariable()
```

**Assertion Actions:**
```java
// Format: assert<Object><Condition>
assertResponseBodyContains()
assertXMLElementEquals()
assertElementIsVisible()
```

**General Actions:**
```java
// Use descriptive verb-noun format
clickAndWait()
fillFormField()
scrollToElement()
```

### Object Type Naming

```java
// ✅ Good - descriptive nouns
@Action(object = "Webservice", ...)
@Action(object = "Database", ...)
@Action(object = "Text Assertions", ...)

// ❌ Avoid - vague or abbreviated
@Action(object = "WS", ...)
@Action(object = "Test", ...)
```

### Error Handling

```java
// ✅ Always catch and report
@Action(...)
public void myAction() {
    try {
        // action logic
        Report.updateTestLog(Action, "Success", Status.DONE);
    } catch (SpecificException e) {
        Report.updateTestLog(Action, 
            "Specific error: " + e.getMessage(), 
            Status.FAIL);
    } catch (Exception e) {
        Logger.getLogger(getClass().getName()).log(Level.OFF, null, e);
        Report.updateTestLog(Action, 
            "Unexpected error: " + e.getMessage(), 
            Status.FAIL);
    }
}
```

### Null Safety

```java
// ✅ Check before use
Page page = (Page) gen.getPage();
if (page == null) {
    Report.updateTestLog(Action, "Page not available", Status.FAIL);
    return;
}
page.navigate(Data);
```

### Playwright Locator Best Practices

When working with Playwright elements in browser plugins:

```java
// ✅ Prioritize user-facing attributes (auto-waiting, retry-ability)
Page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("submit"));

// ✅ Chain locators to narrow scope
Locator product = Page.getByRole(AriaRole.LISTITEM)
    .filter(new Locator.FilterOptions().setHasText("Product 2"));

// ✅ Filter by text or another locator
Locator row = Page.getByRole(AriaRole.ROW)
    .filter(new Locator.FilterOptions().setHasText("John"));
row.getByRole(AriaRole.BUTTON).click();

// ❌ Avoid brittle CSS/XPath selectors when possible
Page.locator("#submit-btn-12345"); // ID may change
```

**Key Benefits:**
- **Auto-waiting**: Playwright checks visibility and enabled state
- **Retry-ability**: Automatically retries actionability checks
- **Resilient**: Less brittle than CSS/XPath selectors

## Build and Deploy

```bash
# Build plugin
mvn clean install package

# Plugin structure created:
target/
  ├── my-plugin.jar          # Plugin JAR
  └── lib/                   # Dependencies
      └── gson-2.10.1.jar
      
# If using maven-antrun-plugin, files auto-copy to:
/path/to/INGenious/plugins/my-plugin/
  ├── my-plugin.jar
  └── lib/
      └── gson-2.10.1.jar
```

## Testing Your Plugin

1. **Build**: `mvn clean install package`
2. **Deploy**: Copy JAR and lib to `INGenious/plugins/my-plugin/`
3. **Launch**: Start INGenious Playwright Studio
4. **Verify**: Your actions appear in the Object type dropdown
5. **Test**: Create test case using your plugin actions

## Example: Complete Plugin Template

See the full templates in `how-to-create-plugin.md`:
- Browser Plugin Template (lines 838-1055)
- Database Plugin Template (lines 1056-1232)
- General Plugin Template (lines 1234-1341)
- Mobile Plugin Template (lines 1343-1465)
- Webservice Plugin Template (lines 1467-1796)

Each template provides production-ready code with proper error handling, reporting, and best practices.

## Quick Reference: Status Values

```java
Status.PASS     // Action passed (with screenshot)
Status.FAIL     // Action failed (with screenshot)
Status.DONE     // Action completed
Status.PASSNS   // Pass without screenshot
Status.FAILNS   // Fail without screenshot
Status.DEBUG    // Debug message
Status.SKIP     // Action skipped
```

## When to Use Which API Contract

- **BrowserPluginApi**: Web browser automation with Playwright
- **MobilePluginApi**: Mobile app testing with Appium
- **WebservicePluginApi**: REST API and web service testing
- **DatabasePluginApi**: Database SQL operations
- **CommandPluginApi**: General purpose utilities and operations

## Additional Resources

- Complete POM template with all plugins configured
- Working plugin examples in the repository
- Full documentation in `how-to-create-plugin.md`
- Plugin samples: browser-test-plugin, mobile-test-plugin, webservice-test-plugin

---

**Critical Reminders:**

1. **Constructor Pattern**: Always use the COMPLETE constructor pattern - initialize ALL fields from the API contract (Data, Action, Input, Condition, Report, ObjectName, userData, etc.). DO NOT simplify even if some fields aren't used immediately. This ensures plugin extensibility and compatibility.

2. **Dependency Scope**: Always use `provided` scope for `ingenious-api` and `playwright` dependencies. This is critical to avoid ClassCastException and version conflicts.

3. **Java Version**: Plugin must be compiled with Java 17 or lower to match the framework's Java version.
