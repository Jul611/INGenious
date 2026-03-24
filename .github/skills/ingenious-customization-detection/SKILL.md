---
name: ingenious-customization-detection
description: 'Detect and analyze customizations made to INGenious framework by users. USE FOR: detecting code modifications, identifying custom enhancements, comparing user version against official release, analyzing build vs source code copies, generating customization reports, identifying version differences, tracking user modifications, extracting customizations as plugins. INCLUDES: version detection, repository comparison, git diff analysis, customization reporting with functional grouping, plugin candidate identification. OUTPUTS: structured plugin specifications for use with ingenious-plugin-creation skill.'
argument-hint: 'Describe which INGenious installation to analyze or confirm the root folder'
---

# INGenious Customization Detection Skill

## When to Use This Skill

Use this skill when you need to:
- Detect customizations or modifications made to INGenious framework
- Compare a user's INGenious installation against the official release
- Identify version and type of INGenious installation (build vs source)
- Generate reports of enhancements grouped by functionality
- Analyze differences between user code and official release
- Track custom modifications across modules (Engine, Datalib, Common, etc.)
- Understand what changes were made to the framework

## Key Principles

**🔴 MANDATORY: This is an INTERACTIVE workflow**

- **NEVER assume** which folder to analyze
- **ALWAYS confirm** with the user before proceeding
- **STOP at Step 1** until user explicitly confirms the target installation
- **Present clear options** when multiple installations are detected
- **Validate** user's choice before moving forward

Each step requires validation before proceeding to the next step. This ensures accuracy and prevents analyzing the wrong codebase.

## Decision Flow

This skill follows a systematic workflow:

```
1. Identify Repository → 2. Detect Version → 3. Determine Copy Type → 
4. Download Official Release → 5. Compare & Detect Changes → 6. Generate Report →
7. Identify Plugin Candidates (Optional)
```

**Skill Integration:** This skill can invoke the `ingenious-plugin-creation` skill to help convert customizations into plugins.

## Workflow Steps

### Step 1: Identify INGenious Repository

**CRITICAL REQUIREMENT: ALWAYS verify with user before proceeding to Step 2**

**Action:** Present options and wait for explicit user confirmation.

**Workflow:**

1. **Detect Available Options**
   - List all workspace folders
   - Identify folders with INGenious markers
   
2. **Present Options to User**
   ```
   I've detected the following potential INGenious installations:
   
   Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3
   Option 2: /Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious
   
   Which installation would you like to analyze for customizations?
   (You can also specify a different path)
   ```

3. **STOP and Wait for User Response**
   - Do NOT proceed to Step 2 without explicit confirmation
   - Do NOT assume which folder to use
   - Allow user to provide alternative path

4. **Validate User's Choice**
   - After user confirms, verify the path contains INGenious markers
   - If validation fails, ask user to confirm again or provide correct path

**User Confirmation Required:**
User must explicitly:
- Select an option number (e.g., "Option 1" or "1")
- Provide an absolute path (e.g., "/path/to/ingenious")
- Confirm with "yes", "correct", "that's right", etc.

**Questions to ask:**
- "Which INGenious installation would you like to analyze?"
- "I see multiple options - which one should I analyze?"
- "Please confirm the root folder path of your INGenious installation"
- "Is [detected path] the correct installation to analyze?"

**Expected User Responses:**
- "Option 1"
- "Use Neil-ingenious-playwright-2.3"
- "/Users/qs01nn/my-custom-ingenious/"
- "Yes, that's correct"
- "The first one"

**Validation:**
Check for INGenious markers:
```bash
# Look for these key indicators:
- Configuration/ directory
- Engine/ directory
- Projects/ directory
- Run.bat or Run.command files
- Configuration/conf.js or XPLOR_SETTINGS.json
```

**Code Pattern:**
```python
# Validate INGenious root after user confirms
indicators = [
    "Configuration/conf.js",
    "Configuration/XPLOR_SETTINGS.json", 
    "Engine/pom.xml",
    "Projects/"
]

# Check if at least 2 indicators exist
found = sum(1 for indicator in indicators if os.path.exists(os.path.join(user_path, indicator)))
if found < 2:
    print(f"⚠️ Warning: Only {found} INGenious markers found in {user_path}")
    print("Please confirm this is the correct path, or provide an alternative.")
    # STOP and wait for user confirmation again
```

**What NOT to do:**
- ❌ Do NOT assume workspace folder is the target
- ❌ Do NOT proceed automatically without confirmation
- ❌ Do NOT guess which folder to use if multiple exist
- ❌ Do NOT skip validation step

**What TO do:**
- ✅ ALWAYS present options clearly
- ✅ ALWAYS wait for explicit user confirmation
- ✅ ALWAYS validate the confirmed path
- ✅ ALWAYS allow user to provide alternative path
- ✅ Proceed to Step 2 ONLY after successful confirmation and validation

### Step 2: Detect Version

**Action:** Identify the INGenious version by examining code artifacts.

**Version Detection Strategy:**

1. **Check pom.xml files** for version tags:
   ```bash
   grep -r "<version>" Engine/pom.xml
   ```

2. **Check manifest files** in JAR files:
   ```bash
   unzip -p Engine/target/*.jar META-INF/MANIFEST.MF | grep Implementation-Version
   ```

3. **Check property files**:
   - Configuration/package.properties
   - Configuration/Global\ Settings.Properties

4. **Check source code files** for version constants:
   ```bash
   grep -r "VERSION\s*=\s*" Engine/src/ --include="*.java"
   ```

5. **Check Git tags** (if repository is git-enabled):
   ```bash
   git describe --tags --always
   ```

**Common Version Patterns:**
- `2.3`, `2.3.0`, `2.3.1`
- `v2.3`, `v2.3.0`
- `release-2.3`

**Code Pattern:**
```bash
# Multi-strategy version detection
version=$(grep -m 1 "<version>" Engine/pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
if [ -z "$version" ]; then
    version=$(grep -r "VERSION" Configuration/*.properties | head -1)
fi
```

### Step 3: Determine Copy Type

**Action:** Determine if user has build copy or source code copy.

**Detection Logic:**

| Copy Type | Indicators | Modules Present |
|-----------|-----------|-----------------|
| **Source Code Copy** | Has multiple Maven modules | Datalib/, Common/, Engine/, IDE/, StoryWriter/ |
| **Build Copy** | Only has Engine module | Engine/ (source code only) |

**Check Strategy:**
```bash
# Look for module directories
if [ -d "Datalib" ] && [ -d "Common" ] && [ -d "IDE" ]; then
    echo "SOURCE_CODE_COPY"
elif [ -d "Engine/src" ] && [ ! -d "Datalib" ]; then
    echo "BUILD_COPY"
else
    echo "UNKNOWN"
fi
```

**Directories to Check:**
- `Datalib/` - Data library module
- `Common/` - Common utilities module  
- `Engine/` - Core engine module
- `IDE/` - IDE integration module
- `ingenious-api/` - API module
- `StoryWriter/` - Story writer module
- `TestData - Csv/` - CSV test data module

**Source Code Copy Structure:**
```
INGenious/
├── Common/
│   ├── pom.xml
│   └── src/
├── Datalib/
│   ├── pom.xml
│   └── src/
├── Engine/
│   ├── pom.xml
│   └── src/
├── IDE/
│   ├── pom.xml
│   └── src/
├── ingenious-api/
│   ├── pom.xml
│   └── src/
├── StoryWriter/
│   ├── pom.xml
│   └── src/
└── pom.xml (parent)
```

**Build Copy Structure:**
```
INGenious/
├── Configuration/
├── Engine/
│   ├── pom.xml
│   └── src/
├── lib/
├── Projects/
└── Tools/
```

### Step 4: Download Official INGenious Release

**Action:** Clone the official INGenious repository and checkout the matching version.

**Prerequisites:**
- Git must be installed
- Internet connection available
- Sufficient disk space (~100-500MB)

**Download Strategy:**

```bash
# Step 4a: Create temporary directory
TEMP_DIR=$(mktemp -d -t ingenious-official-XXXXXX)
cd "$TEMP_DIR"

# Step 4b: Clone official repository
git clone https://github.com/ing-bank/INGenious.git official-ingenious

# Step 4c: Navigate to repository
cd official-ingenious

# Step 4d: List available tags
git tag -l

# Step 4e: Checkout matching version
# Try different tag patterns
VERSION="2.3"  # From Step 2
git checkout "v${VERSION}" 2>/dev/null || \
git checkout "${VERSION}" 2>/dev/null || \
git checkout "release-${VERSION}" 2>/dev/null || \
git checkout "Release-${VERSION}" 2>/dev/null

# Step 4f: Verify checkout
git describe --tags --exact-match 2>/dev/null
```

**Error Handling:**

If exact version tag is not found:
1. List all tags and find closest match
2. Ask user which tag to use for comparison
3. Option: Use main/master branch with warning about potential differences

**Alternative: Download Release Asset**
```bash
# If tagged release has assets
RELEASE_URL="https://github.com/ing-bank/INGenious/archive/refs/tags/v${VERSION}.zip"
curl -L "$RELEASE_URL" -o ingenious-official.zip
unzip ingenious-official.zip
```

### Step 5: Compare and Detect Changes

**Action:** Systematically compare user's copy against official release.

**Comparison Strategy:**

#### 5A. For Source Code Copy

Compare all modules:
```bash
MODULES=(
    "Common"
    "Datalib" 
    "Engine"
    "IDE"
    "ingenious-api"
    "StoryWriter"
    "TestData - Csv"
)

for module in "${MODULES[@]}"; do
    if [ -d "$USER_PATH/$module" ] && [ -d "$OFFICIAL_PATH/$module" ]; then
        echo "Comparing $module..."
        # Use diff or git diff
        diff -rq "$USER_PATH/$module/src" "$OFFICIAL_PATH/$module/src" > changes_${module}.txt
    fi
done
```

#### 5B. For Build Copy

Compare only Engine module:
```bash
diff -rq "$USER_PATH/Engine/src" "$OFFICIAL_PATH/Engine/src" > changes_Engine.txt
```

**Detailed Diff Strategy:**

```bash
# For each module, generate detailed diff
diff -Naur \
    --exclude='target' \
    --exclude='*.class' \
    --exclude='.git' \
    --exclude='*.iml' \
    --exclude='.idea' \
    "$OFFICIAL_PATH/$module" \
    "$USER_PATH/$module" \
    > detailed_diff_${module}.patch

# Count changes
ADDED=$(grep -c "^+" detailed_diff_${module}.patch)
REMOVED=$(grep -c "^-" detailed_diff_${module}.patch)
FILES_CHANGED=$(grep -c "^diff" detailed_diff_${module}.patch)
```

**What to Compare:**

1. **Source Code Files** (*.java):
   - New files added
   - Modified files
   - Deleted files
   - Line-by-line changes

2. **Configuration Files**:
   - pom.xml modifications (dependencies, plugins)
   - properties files
   - XML configuration files

3. **Resource Files**:
   - Templates
   - Static resources
   - Property bundles

4. **Build Scripts**:
   - Maven configurations
   - Build plugins
   - Deployment scripts

**Exclude from Comparison:**
- `target/` directories (build outputs)
- `.git/` directories
- IDE files (`.idea/`, `*.iml`)
- Log files
- Generated files
- User-specific paths

**Using Git Diff (Recommended):**

```bash
# Copy official version to temp location
cp -r "$OFFICIAL_PATH/$module" /tmp/official_module

# Initialize git in user's module (if not already)
cd "$USER_PATH/$module"
git init
git add -A
git commit -m "User's current state"

# Replace with official and check diff
rm -rf src/
cp -r /tmp/official_module/src .
git add -A
git diff --cached > ../customizations_${module}.diff

# Restore user's version
git reset --hard HEAD
```

### Step 6: Generate Customization Report

**Action:** Create a comprehensive report of all customizations.

**Report Structure:**

```markdown
# INGenious Customization Report

## Summary
- **Analyzed Installation:** [path]
- **Version Detected:** [version]
- **Copy Type:** [Source Code / Build]
- **Official Version Compared:** [tag]
- **Analysis Date:** [date]

## Overview Statistics
- **Modules Analyzed:** [count]
- **Files Modified:** [count]
- **Files Added:** [count]
- **Files Deleted:** [count]
- **Total Lines Changed:** [+lines / -lines]

## Detailed Changes by Module

### Module: [Module Name]
**Files Changed:** [count]

#### Customization Category: [Category Name]
**Intent:** [Inferred purpose]
**Impact:** [High/Medium/Low]

**Files Affected:**
- [file path] (+X lines, -Y lines)
- [file path] (+X lines, -Y lines)

**Description:**
[Detailed description of changes]

**Code Sample:**
```java
[Representative code snippet]
```

---

### Module: [Next Module]
[repeat structure]

## Customization Categories

Based on analysis, customizations are grouped into:

### 1. Feature Enhancements
- New functionality added
- Extended capabilities
- Custom actions or commands

### 2. Bug Fixes & Patches
- Error handling improvements
- Null pointer fixes
- Exception handling

### 3. Configuration Changes
- POM modifications
- Dependency updates
- Build configuration

### 4. Integration Customizations
- External tool integrations
- API modifications
- Plugin system changes

### 5. Performance Optimizations
- Code optimizations
- Caching improvements
- Resource management

### 6. UI/Reporting Changes
- Report template modifications
- Dashboard customizations
- Log formatting

### 7. Framework Modifications
- Core engine changes
- API contract modifications
- Architecture changes

## Risk Assessment

| Change Type | Risk Level | Reason |
|-------------|-----------|--------|
| [Type] | [High/Med/Low] | [Explanation] |

## Recommendations

1. **Upgrade Path:** [Suggestions for maintaining customizations during upgrades]
2. **Plugin Candidates:** [Changes that could be extracted to plugins]
3. **Contribution Opportunities:** [Changes that could be contributed upstream]

## Plugin Extraction Opportunities

### Recommended Plugins to Create

Based on the customizations detected, the following plugins are recommended:

#### Plugin 1: [Plugin Name]
**Type:** Browser/Mobile/Webservice/Database/General
**Priority:** High/Medium/Low
**Complexity:** Simple/Moderate/Complex

**Actions to Implement:**
1. `actionName1` - [Description]
2. `actionName2` - [Description]

**Required Dependencies:**
- [dependency-name] version [x.x.x]

**Affected Files:**
- [file path] - [lines modified]

**Code Snippet:**
```java
[Key customization code that would become plugin action]
```

**Next Steps:**
To create this plugin, use: `/ingenious-plugin-creation` and provide this specification.

---

### Plugin Specification Format (JSON)

For automated plugin creation, use this structured format:

```json
{
  "plugins": [
    {
      "name": "custom-browser-actions",
      "type": "browser",
      "priority": "high",
      "api_contract": "BrowserPluginApi",
      "actions": [
        {
          "method_name": "customClick",
          "description": "Click element with custom behavior",
          "object_type": "PLAYWRIGHT",
          "input_required": false,
          "source_file": "Engine/src/main/java/com/ing/engine/commands/WebCommands.java",
          "source_lines": "145-167",
          "code_snippet": "[extracted code]"
        }
      ],
      "dependencies": [
        {
          "groupId": "com.example",
          "artifactId": "custom-lib",
          "version": "1.0.0"
        }
      ]
    }
  ]
}
```

## Detailed Diff Files

Full diff files available at:
- [path to diff files]

```

**Categorization Logic:**

Analyze changes to infer intent:

```python
# Pattern matching for categorization
categories = {
    "Feature Enhancement": [
        "new class", "new method", "implements", "extends", 
        "Added functionality", "Enhanced"
    ],
    "Bug Fix": [
        "fix", "null check", "exception", "try-catch", "validate",
        "NullPointerException", "fixed"
    ],
    "Configuration": [
        "pom.xml", "dependency", "plugin", "version", "properties"
    ],
    "Integration": [
        "API", "REST", "external", "third-party", "integration"
    ],
    "Performance": [
        "optimize", "cache", "performance", "faster", "efficient"
    ],
    "UI/Reporting": [
        "report", "template", "HTML", "dashboard", "display", "UI"
    ],
    "Framework Core": [
        "engine", "core", "framework", "architecture", "Plugin"
    ]
}
```

**Impact Assessment:**

- **High Impact:** Changes to core engine, API contracts, plugin system
- **Medium Impact:** Feature additions, integration points, configuration
- **Low Impact:** Bug fixes, minor enhancements, formatting

**Report Generation Code Pattern:**

```bash
# Generate markdown report
cat > customization_report.md << 'EOF'
# INGenious Customization Report
...
EOF

# For each changed file, analyze and categorize
for file in $(git diff --name-only); do
    # Extract changes
    changes=$(git diff "$file")
    
    # Infer category (pattern matching)
    category=$(infer_category "$changes")
    
    # Add to report
    echo "### $file" >> customization_report.md
    echo "**Category:** $category" >> customization_report.md
    echo '```diff' >> customization_report.md
    echo "$changes" >> customization_report.md
    echo '```' >> customization_report.md
done
```

### Step 7: Identify Plugin Candidates (Optional)

**Action:** Analyze customizations and identify which ones should be extracted as plugins.

**When to Execute This Step:**
- After completing Step 6 (report generation)
- When user asks to "extract customizations to plugins"
- When user wants to upgrade INGenious and preserve customizations
- When customizations are significant enough to warrant plugin extraction

**Plugin Candidate Criteria:**

A customization is a good plugin candidate if it:

1. **Adds New Functionality** (not modifies existing core behavior)
   - New action methods
   - New commands
   - New integrations
   
2. **Is Self-Contained** (minimal dependencies on core framework)
   - Doesn't modify framework internal classes
   - Uses only public API methods
   - Can be isolated into separate class(es)

3. **Provides Reusable Actions**
   - Could be useful across multiple test scenarios
   - Implements generic functionality
   - Not project-specific hacks

4. **Is Located in Appropriate Modules**
   - Custom actions in Engine module → Plugin candidate
   - New commands in Engine/src/main/java/com/ing/engine/commands/ → High priority
   - Modified core classes → Not suitable (keep as customization)

**Analysis Strategy:**

```bash
# Step 7a: Identify new methods with @Action or @Command patterns
grep -r "public void\|public boolean\|public String" \
    --include="*.java" \
    $USER_PATH/Engine/src | \
    grep -v "^$OFFICIAL_PATH" > new_methods.txt

# Step 7b: Find new classes (entire files added)
find $USER_PATH/Engine/src -name "*.java" | while read file; do
    relative_path=${file#$USER_PATH/}
    if [ ! -f "$OFFICIAL_PATH/$relative_path" ]; then
        echo "NEW CLASS: $file"
    fi
done

# Step 7c: Analyze imports to determine plugin type
grep -h "^import" $new_class_files | sort | uniq
# - com.microsoft.playwright.* → Browser plugin
# - io.appium.* → Mobile plugin  
# - java.net.http.* → Webservice plugin
# - java.sql.* → Database plugin
```

**Plugin Type Detection:**

Based on imports and method signatures:

| Pattern Found | Plugin Type | API Contract |
|---------------|-------------|--------------|
| `import com.microsoft.playwright.*` | Browser | BrowserPluginApi |
| `import io.appium.*` | Mobile | MobilePluginApi |
| `import java.net.http.HttpClient` | Webservice | WebservicePluginApi |
| `import java.sql.*` | Database | DatabasePluginApi |
| No specific imports | General | CommandPluginApi |

**Action Method Detection:**

Look for methods that fit action patterns:

```bash
# Find methods that look like actions
grep -E "public (void|boolean|String) [a-zA-Z]+\(.*\)" \
    $new_methods_file | \
    grep -v "^(private|protected)" | \
    grep -v "^(get|set|is)" | \
    grep -v "^(equals|hashCode|toString)"
```

**Plugin Specification Generator:**

```python
# Generate plugin specification from detected customizations

def generate_plugin_spec(customization_data):
    """
    Creates a structured plugin specification
    """
    plugin_spec = {
        "plugin_name": infer_plugin_name(customization_data),
        "plugin_type": detect_plugin_type(customization_data),
        "api_contract": get_api_contract(plugin_type),
        "actions": [],
        "dependencies": []
    }
    
    for method in customization_data['new_methods']:
        action = {
            "method_name": method['name'],
            "description": extract_javadoc(method),
            "object_type": infer_object_type(method),
            "input_required": has_data_parameter(method),
            "condition_optional": has_condition_parameter(method),
            "source_code": extract_method_body(method),
            "source_file": method['file_path'],
            "source_lines": f"{method['start_line']}-{method['end_line']}"
        }
        plugin_spec['actions'].append(action)
    
    # Detect dependencies
    for import_stmt in customization_data['imports']:
        if not is_standard_library(import_stmt):
            plugin_spec['dependencies'].append({
                "import": import_stmt,
                "maven_artifact": map_to_maven(import_stmt)
            })
    
    return plugin_spec
```

**Output Format for Plugin Creation:**

Generate a structured output that can be consumed by the `ingenious-plugin-creation` skill:

```markdown
## 🔌 Plugin Extraction Recommendations

Based on the customization analysis, **3 plugins** can be extracted:

---

### Plugin 1: Custom Browser Actions

**✅ High Priority** | **Plugin Type:** Browser | **Complexity:** Moderate

**Purpose:** Extends browser automation with custom interaction methods

**Actions Identified:**
1. `clickWithHighlight()` - Highlights element before clicking
   - Source: Engine/src/main/java/com/ing/engine/commands/WebCommands.java:145-167
   - Object Type: PLAYWRIGHT
   - Input Required: No
   
2. `hoverAndWait()` - Hover over element with configurable wait time
   - Source: Engine/src/main/java/com/ing/engine/commands/WebCommands.java:210-235
   - Object Type: PLAYWRIGHT
   - Input Required: Yes (wait time in Condition field)

**Dependencies Required:**
- None (uses only Playwright API)

**API Contract:** `BrowserPluginApi`

**Estimated Effort:** 2-3 hours

**Code Ready:** Yes - can be extracted directly

**Next Step:**
```
Use the ingenious-plugin-creation skill with this specification:
"Create a browser plugin named 'custom-browser-actions' with the following actions:
1. clickWithHighlight - highlights element before clicking
2. hoverAndWait - hover with configurable wait time"
```

---

### Plugin 2: Database Validation

**✅ Medium Priority** | **Plugin Type:** Database | **Complexity:** Simple

[Similar structure...]

---

### Plugin 3: API Testing Extensions

**⚠️ Low Priority** | **Plugin Type:** Webservice | **Complexity:** Complex

[Similar structure...]

---

## 📋 Plugin Specifications (Machine-Readable)

Saved to: `plugin_specifications.json`

Use this file with automated plugin generation workflows.

---

## ⚙️ How to Proceed

**Option 1: Create plugins one at a time**
Ask: "Create Plugin 1" or "Create the Custom Browser Actions plugin"

**Option 2: Create all recommended plugins**
Ask: "Create all recommended plugins from the customization analysis"

**Option 3: Review a specific plugin in detail**
Ask: "Show me the full specification for Plugin 2"

**Option 4: Ask the plugin creation skill directly**
Type: `/ingenious-plugin-creation` and provide the specification above
```

**Cross-Skill Workflow:**

When user requests plugin creation after detection:

1. ✅ **Detection Skill** outputs plugin specifications
2. ✅ **User confirms** which plugins to create
3. ✅ **Detection Skill invokes** ingenious-plugin-creation skill with:
   - Plugin name
   - Plugin type
   - Action specifications
   - Source code snippets
   - Dependencies

**Code Pattern for Invocation:**

```markdown
After generating plugin specifications, if user asks to create plugins:

1. Ask: "I've identified 3 plugin candidates. Would you like me to create:
   - Plugin 1: Custom Browser Actions (High Priority)
   - Plugin 2: Database Validation (Medium Priority)  
   - Plugin 3: API Testing Extensions (Low Priority)
   
   Which plugin(s) should I create? (Type 1, 2, 3, or 'all')"

2. When user selects, invoke the plugin creation skill:
   
   Load: /ingenious-plugin-creation skill
   
   Provide:
   - Plugin type (browser/mobile/webservice/database/general)
   - Action specifications from Step 7 analysis
   - Source code extracted from customizations
   - Dependencies detected
   
3. The plugin creation skill will:
   - Generate POM file
   - Create plugin entry class
   - Implement action methods
   - Configure dependencies
   - Set up build configuration
```

**Validation Before Plugin Creation:**

Before creating a plugin, verify:

```bash
# Check if customization is truly isolated
grep -r "import com.ing.engine.core" $customization_file
# If found: ⚠️ Warning - depends on engine internals

# Check if it modifies existing methods
if git diff shows modified existing methods:
    # ⚠️ Not suitable for plugin - keep as customization
else:
    # ✅ Good plugin candidate - new functionality only
fi

# Check complexity
LOC=$(wc -l < $customization_file)
if [ $LOC -gt 500 ]; then
    echo "⚠️ Complex customization - may need multiple plugins"
fi
```

## Implementation Tools

### Recommended Tool Sequence

1. **run_in_terminal** - Execute git commands, diff operations
2. **read_file** - Read configuration files for version detection
3. **grep_search** - Search for version strings, patterns
4. **create_file** - Generate customization report
5. **file_search** - Find specific files across modules

### Code Patterns for Common Operations

**Version Detection:**
```bash
# Multi-file search for version
grep -h -r "VERSION\|version\|Version" \
    --include="*.xml" \
    --include="*.properties" \
    --include="*.java" \
    Configuration/ Engine/ | \
    grep -o "[0-9]\+\.[0-9]\+\(\.[0-9]\+\)\?" | \
    sort -V | uniq
```

**Module Detection:**
```bash
# Check for source code modules
MODULES_FOUND=()
for module in Common Datalib Engine IDE StoryWriter; do
    [ -d "$module/src" ] && MODULES_FOUND+=("$module")
done

if [ ${#MODULES_FOUND[@]} -gt 1 ]; then
    echo "SOURCE_CODE_COPY"
else
    echo "BUILD_COPY"
fi
```

**Diff with Exclusions:**
```bash
# Compare with exclusions
diff -Nur \
    -x 'target' \
    -x '*.class' \
    -x '.git' \
    -x '.idea' \
    -x '*.iml' \
    -x 'node_modules' \
    -x '*.log' \
    "$OFFICIAL" "$USER" > changes.diff
```

## Error Handling & Edge Cases

### Common Issues

**Issue 1: Version Tag Not Found**
- **Symptom:** `git checkout` fails with "pathspec did not match"
- **Solution:** List all tags, find closest match, ask user
```bash
git tag -l | grep -i "$VERSION"
```

**Issue 2: No Git Available**
- **Symptom:** `git: command not found`
- **Solution:** Fall back to ZIP download or ask user to install git

**Issue 3: Large Diff Output**
- **Symptom:** Diff files are too large (>100MB)
- **Solution:** Summarize changes, provide file list only
```bash
diff -qr "$OFFICIAL" "$USER" > file_list.txt
```

**Issue 4: Permission Denied**
- **Symptom:** Cannot access directories or files
- **Solution:** Check permissions, ask user to run with appropriate access

**Issue 5: Module Structure Mismatch**
- **Symptom:** Official version has different module layout
- **Solution:** Document structural differences separately

### Validation Checks

Before each step:
```bash
# Check disk space
df -h . | awk 'NR==2 {print $4}'

# Verify path exists
[ -d "$USER_PATH" ] || { echo "Path not found"; exit 1; }

# Verify git is available
command -v git >/dev/null 2>&1 || { echo "Git not installed"; exit 1; }

# Check internet connectivity (for clone)
ping -c 1 github.com >/dev/null 2>&1 || { echo "No internet"; exit 1; }
```

## Best Practices

1. **Always Confirm Paths**: Ask user to verify the INGenious root directory
2. **Save Official Clone**: Keep the official repository for future comparisons
3. **Use Absolute Paths**: Avoid relative path issues
4. **Generate Timestamped Reports**: Include date in report filename
5. **Preserve Context**: Include surrounding code in diffs for clarity
6. **Group Related Changes**: Combine related modifications in report
7. **Provide Examples**: Include code snippets for key customizations
8. **Document Intent**: Try to infer why changes were made
9. **Risk Assessment**: Identify changes that may impact upgrades
10. **Actionable Recommendations**: Suggest next steps for user

## Output Artifacts

After completing the analysis, generate:

1. **Main Report** (`customization_report.md`)
   - Summary statistics
   - Categorized changes
   - Risk assessment
   - Recommendations

2. **Detailed Diffs** (per module)
   - `diff_Engine.patch`
   - `diff_Datalib.patch`
   - etc.

3. **File Lists**
   - `files_added.txt`
   - `files_modified.txt`
   - `files_deleted.txt`

4. **Statistics Summary** (`stats.json`)
```json
{
  "version": "2.3",
  "copy_type": "source",
  "modules_analyzed": 7,
  "files_changed": 45,
  "lines_added": 2340,
  "lines_removed": 456,
  "categories": {
    "feature_enhancement": 12,
    "bug_fix": 8,
    "configuration": 15,
    "framework_core": 10
  }
}
```

## Example Usage

**User Request:** "Analyze my INGenious installation for customizations"

**Agent Response - Step 1 (STOP and Wait for Confirmation):**
```
I'll help you detect customizations in your INGenious installation.

I've detected the following potential INGenious installations in your workspace:

Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3
  └─ Contains: Configuration/, Engine/, Projects/
  
Option 2: /Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious
  └─ Contains: Common/, Datalib/, Engine/, IDE/

Which installation would you like to analyze for customizations?
(Or provide a different path if needed)
```

**User:** "Option 1" or "Neil-ingenious-playwright-2.3" or "The first one"

**Agent Response - Validation:**
```
✓ Confirmed: Analyzing /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3

Proceeding with Step 2: Detecting version...
```

**Agent Response - Step 2:**
```
Found version 2.3 in Engine/pom.xml
```

**Agent Response - Step 3:**
```
This is a BUILD COPY (Engine module only - no Datalib or Common modules found)
```

**Agent Response - Step 4:**
```
Downloading official INGenious v2.3 from GitHub...
✓ Successfully cloned and checked out tag v2.3
```

**Agent Response - Step 5:**
```
Comparing Engine module against official v2.3...
Analyzing differences...
```

**Agent Response - Step 6:**
```
Analysis complete! 

Summary:
- Files Modified: 23
- Lines Added: 456
- Lines Removed: 89
- Customization Categories: 3

Report saved to: customization_report.md

Key Findings:
1. Feature Enhancements (12 files) - Custom browser actions added
2. Bug Fixes (8 files) - Null pointer exception handling
3. Configuration (3 files) - Modified POM dependencies

Would you like me to:
1. Open the detailed report?
2. Identify plugin extraction opportunities?
```

**User:** "Identify plugin opportunities" or "Can these be converted to plugins?"

**Agent Response - Step 7:**
```
Analyzing customizations for plugin extraction...

🔌 Plugin Extraction Analysis Complete!

I've identified 2 plugins that can be extracted from your customizations:

---

### Plugin 1: Custom Browser Actions ✅ High Priority
**Type:** Browser Plugin | **Complexity:** Moderate

**Actions Identified:**
1. clickWithHighlight() - Highlights element before clicking
2. hoverAndWait() - Hover with configurable wait time
3. scrollToBottom() - Scroll page to bottom with animation

**Source Files:**
- Engine/src/main/java/com/ing/engine/commands/WebCommands.java (lines 145-235)

**Dependencies:** None (uses only Playwright API)
**Estimated Effort:** 2-3 hours

---

### Plugin 2: Excel Data Helpers ✅ Medium Priority  
**Type:** General Plugin | **Complexity:** Simple

**Actions Identified:**
1. readExcelColumnValues() - Read all values from a column
2. compareExcelSheets() - Compare two Excel sheets

**Source Files:**
- Engine/src/main/java/com/ing/engine/utils/ExcelHelper.java (lines 89-156)

**Dependencies:** 
- Apache POI 5.2.3

**Estimated Effort:** 1-2 hours

---

Would you like me to create these plugins? Reply with:
- "Create Plugin 1" for Custom Browser Actions
- "Create Plugin 2" for Excel Data Helpers  
- "Create both plugins"
```

**User:** "Create Plugin 1"

**Agent (Invoking Plugin Creation Skill):**
```
Loading ingenious-plugin-creation skill...

Creating Custom Browser Actions plugin...

✓ Created project structure: custom-browser-actions/
✓ Generated pom.xml with BrowserPluginApi dependency
✓ Created entry class: CustomBrowserActionsPlugin.java
✓ Implemented 3 actions with @Action annotations
✓ Configured maven-jar-plugin with manifest entry
✓ Set up maven-dependency-plugin for lib/ folder

Plugin created successfully at:
/Users/qs01nn/ing_code_repos/INGenious_project/custom-browser-actions/

Next steps:
1. Build: cd custom-browser-actions && mvn clean package
2. Deploy: Copy target/*.jar and target/lib/ to INGenious/plugins/custom-browser-actions/
3. Test: Launch INGenious and verify actions appear in Object dropdown

Would you like me to build the plugin now?
```

## Example Usage (Complete End-to-End Workflow)

**Scenario:** User has customized INGenious and wants to upgrade to a new version

**User:** "I need to upgrade INGenious but I have customizations. How can I preserve them?"

**Agent:**
```
I'll help you analyze your customizations and convert them to plugins so they're preserved during the upgrade.

Step 1: Let me detect your customizations first.
Which INGenious installation should I analyze?

Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3
Option 2: [another path]
```

**User:** "Option 1"

**Agent:** [Runs Steps 1-6 as shown above, generating customization report]

**Agent:** [Automatically proceeds to Step 7]
```
I've analyzed your customizations and identified 2 that can be converted to plugins.
This will make upgrading much safer - your customizations will remain separate from the core framework.

Plugin 1: Custom Browser Actions (3 actions identified)
Plugin 2: Excel Data Helpers (2 actions identified)

Should I create these plugins so you can upgrade safely?
```

**User:** "Yes, create both"

**Agent:** [Invokes plugin creation skill twice]
```
Creating Plugin 1: Custom Browser Actions...
✓ Plugin 1 created successfully

Creating Plugin 2: Excel Data Helpers...  
✓ Plugin 2 created successfully

Both plugins are ready! Here's your upgrade path:

1. ✅ Your customizations are now in plugins:
   - custom-browser-actions/
   - excel-data-helpers/

2. Next: Upgrade INGenious to the new version
   - Download new version
   - Replace the Engine/ folder
   - Keep your plugins/ folder

3. Deploy your plugins to the new installation
   - Copy plugin folders to new INGenious/plugins/

Your customizations will work with the new version! 🎉

Would you like me to build these plugins now?
```

## Additional Considerations

### For Large Codebases
- Use summary mode for initial analysis
- Offer detailed mode for specific modules
- Implement progress indicators

### For Multiple Versions
- Support comparing against multiple official versions
- Show evolution of customizations over time

### For CI/CD Integration
- Provide JSON/XML output formats
- Exit codes for automation
- Machine-readable reports

### For Team Collaboration
- Export changes as patches
- Document migration strategies
- Identify shared customizations

## Troubleshooting Guide

| Problem | Possible Cause | Solution |
|---------|---------------|----------|
| Cannot detect version | Version not in standard location | Manual version input |
| Module directory missing | Incomplete installation | Verify installation integrity |
| Diff shows too many changes | Comparing different major versions | Confirm version match |
| Git clone fails | Network/firewall issues | Try ZIP download alternative |
| Out of disk space | Large official clone | Clean up or use different temp directory |

## Maintenance Notes

- Update official repository URL if it changes
- Keep list of common version patterns up to date
- Refine categorization patterns based on actual usage
- Add new module types as framework evolves

---

**Version:** 1.0  
**Last Updated:** March 2026  
**Maintainer:** INGenious Team
