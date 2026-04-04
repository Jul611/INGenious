---
name: ingenious-plugin-creation
description: 'Expert guidance for creating INGenious Playwright Framework plugins. USE FOR: creating new plugins, fixing plugin errors, configuring maven POMs, implementing action methods, working with Playwright objects, troubleshooting classloader issues, version compatibility problems, extending the framework with custom actions, converting customizations into plugins. INCLUDES: complete templates, architecture patterns, dependency management, best practices. INTEGRATES WITH: ingenious-customization-detection skill for extracting customizations as plugins.'
argument-hint: 'Describe the plugin to create or issue to fix'
allowed-tools: shell
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

**Validation Checklist:**

After user confirms both directories:

**Source Code Directory:**
- ✅ Path exists or can be created
- ✅ Write permissions available
- ✅ No plugin name conflicts
- ⚠️ If exists: Prompt for different name/location or confirm overwrite

**INGenious Installation (if auto-deploy):**
- ✅ Valid INGenious structure (Engine/, Configuration/ present)
- ✅ plugins/ directory accessible
- ✅ Write permissions available
- ⚠️ If invalid: Request correct path or skip auto-deployment

**Agent Implementation:** Use `list_dir` and file operations to verify. Display clear warnings and options if validation fails.

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

**For complete POM template** with all plugins (maven-dependency-plugin, maven-jar-plugin, maven-antrun-plugin), see [reference/pom-complete-template.xml](reference/pom-complete-template.xml)

### 2. Build Configuration Summary

Required Maven plugins:
- **maven-dependency-plugin**: Copy dependencies to lib/
- **maven-jar-plugin**: Declare entry classes in manifest
- **maven-antrun-plugin**: Auto-deploy to INGenious (optional)

See complete configuration in [reference/pom-complete-template.xml](reference/pom-complete-template.xml)

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

See the complete constructor pattern in section 3 above (line 586). **DO NOT simplify or omit any initialization steps.**

**Key Requirements:**
- Initialize ALL fields from API contract
- Store API instance
- Cast Playwright/Appium objects once
- Never skip fields even if unused initially

This ensures plugin extensibility and prevents runtime errors.

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
```

**For complete API reference** including Browser (Playwright), Mobile (Appium), Webservice, Database methods, usage examples, and common patterns, see [reference/api-methods-quick-ref.md](reference/api-methods-quick-ref.md)

## Common Patterns

Load specific pattern examples as needed from `examples/` directory:

**Available Patterns:**
- **Element Interaction:** [examples/pattern-element-interaction.java](examples/pattern-element-interaction.java) - Click with highlighting
- **Variable Storage:** [examples/pattern-variable-storage.java](examples/pattern-variable-storage.java) - Store text in variables
- **Timeout Handling:** [examples/pattern-timeout-handling.java](examples/pattern-timeout-handling.java) - Optional timeout with navigation
- **Webservice Request:** [examples/pattern-webservice-request.java](examples/pattern-webservice-request.java) - POST request handling
- **Mobile Interaction:** [examples/pattern-mobile-interaction.java](examples/pattern-mobile-interaction.java) - Tap with validation

**Agent Loading Instructions:** Load pattern files only when user requests specific functionality. Each pattern is self-contained with complete working code.

## Troubleshooting Guide

Common errors with quick solutions. Load detailed guides from `troubleshooting/` for specific issues:

| Error | Symptom | Quick Fix | Details |
|-------|---------|-----------|---------|  
| ClassCastException | Cannot cast Playwright.Page | Use `provided` scope | [Guide](troubleshooting/classcastexception.md) |
| UnsupportedClassVersionError | Compiled by newer Java | Set Java 17 in POM | [Guide](troubleshooting/unsupported-class-version.md) |
| NoSuchMethodError | Method not found at runtime | Match Playwright 1.50.0 | [Guide](troubleshooting/nosuchmethoderror.md) |
| Duplicate Actions | Action name already exists | Rename with unique suffix | [Guide](troubleshooting/duplicate-actions.md) |
| Invalid Manifest | manifest format error | Single-line entry classes | [Guide](troubleshooting/manifest-errors.md) |

**Agent Loading:** Load specific troubleshooting guide only when user reports matching error.

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

See the canonical constructor pattern in Quick Start section (line 586).

**Required initialization sequence:**
1. Store API contract: `this.gen = gen;`
2. Initialize ALL test data fields (Data, Action, Input, Condition, ObjectName)
3. Initialize Report API
4. Initialize userData (if available)
5. Cast Playwright/Appium objects once

**Why this matters:**
- Future-proofs plugin for extensions
- Prevents NullPointerException
- Maintains framework consistency
- Enables all API features

**Anti-patterns to avoid:**

```java
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

## Example: Complete Plugin Templates

Load production-ready templates as needed from `templates/` directory:

| Template | Use Case | Features | File |
|----------|----------|----------|------|
| **Browser** | Playwright web automation | Navigation, assertions, variable storage, highlighting | [templates/browser-plugin-template.java](templates/browser-plugin-template.java) |
| **Database** | SQL operations | Query execution, variable substitution, result extraction | [templates/database-plugin-template.java](templates/database-plugin-template.java) |
| **General** | Utility operations | Text validation, custom assertions | [templates/general-plugin-template.java](templates/general-plugin-template.java) |
| **Mobile** | Appium mobile testing | Tap, scroll (Android/iOS), element validation | [templates/mobile-plugin-template.java](templates/mobile-plugin-template.java) |
| **Webservice** | REST API testing | GET/POST/PUT, JSON parsing, headers, assertions | [templates/webservice-plugin-template.java](templates/webservice-plugin-template.java) |

**Agent Loading Instructions:** Load specific template only when user requests code for that plugin type. Each template includes complete constructor pattern, action examples, and helper methods.

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
