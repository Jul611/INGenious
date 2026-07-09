# Plugin Marketplace — Implementation Plan

## Overview

Two-month project to build a plugin marketplace for the INGenious IDE, backed by Azure DevOps.

## Architecture Components

```
┌─────────────────────────────────────────────────┐
│                 INGenious IDE                    │
│  ┌───────────────────────────────────────────┐  │
│  │  PluginManager.java (New Slide/Section)    │  │
│  │  ┌──────────────┐  ┌──────────────────┐   │  │
│  │  │ Browse Tab    │  │ Installed Tab    │   │  │
│  │  │ (Marketplace) │  │ (Manage Plugins) │   │  │
│  │  └──────┬───────┘  └────────┬─────────┘   │  │
│  │         │                    │              │  │
│  │  ┌──────┴────────────────────┴──────────┐  │  │
│  │  │ PluginManagerService.java            │  │  │
│  │  │  - fetchRegistry()                   │  │  │
│  │  │  - installPlugin(name, version)      │  │  │
│  │  │  - uninstallPlugin(name)             │  │  │
│  │  │  - getInstalledPlugins()             │  │  │
│  │  │  - checkForUpdates()                 │  │  │
│  │  └──────────────────┬───────────────────┘  │  │
│  └─────────────────────┼──────────────────────┘  │
│                        │ HTTPS (PAT auth)         │
└────────────────────────┼──────────────────────────┘
                         │
┌────────────────────────┼──────────────────────────┐
│  Azure DevOps          │                          │
│  ┌─────────────────────┴──────────────────────┐   │
│  │  Git Repo: ingenious-plugins               │   │
│  │  ├── registry.json                        │   │
│  │  ├── plugins/                             │   │
│  │  │   ├── ocr-reader/                      │   │
│  │  │   │   ├── 1.0.0/                      │   │
│  │  │   │   │   ├── ocr-reader.jar           │   │
│  │  │   │   │   └── lib/                    │   │
│  │  │   │   └── 1.1.0/                      │   │
│  │  │   └── sap-connector/                   │   │
│  │  └── docs/                                │   │
│  └─────────────────────────────────────────────┘  │
│                                                   │
│  ┌──────────────────────────────┐                  │
│  │  PR Pipeline:                │                  │
│  │  - Validate manifest         │                  │
│  │  - Check duplicate actions   │                  │
│  │  - Generate registry entry   │                  │
│  └──────────────────────────────┘                  │
└────────────────────────────────────────────────────┘
```

## registry.json Schema

```json
{
  "version": 1,
  "plugins": [
    {
      "name": "ocr-reader",
      "displayName": "OCR Reader",
      "description": "Extract text from images using Tesseract OCR",
      "author": "Team Alpha",
      "authorEmail": "team.alpha@company.com",
      "version": "1.1.0",
      "minEngineVersion": "3.1.0",
      "maxEngineVersion": "",
      "downloadUrl": "https://dev.azure.com/.../_apis/git/repositories/ingenious-plugins/items?path=/plugins/ocr-reader/1.1.0/ocr-reader.jar",
      "libUrls": [
        "https://dev.azure.com/.../items?path=/plugins/ocr-reader/1.1.0/lib/tesseract-wrapper.jar"
      ],
      "objectTypes": ["Browser", "General"],
      "actionCount": 3,
      "actions": ["readImageText", "captureScreenshotRegion", "compareImageText"],
      "homepageUrl": "https://dev.azure.com/.../ingenious-plugins",
      "license": "MIT",
      "releaseNotes": "Added support for multi-language OCR"
    }
  ]
}
```

## File Structure to Create

```
IDE/src/main/java/com/ing/ide/main/mainui/components/pluginmanager/
├── PluginManager.java                   # Main tab component (JPanel)
├── PluginManagerBrowseUI.java           # Browse marketplace tab
├── PluginManagerInstalledUI.java        # Manage installed plugins tab
├── PluginManagerService.java            # HTTP + filesystem operations
├── PluginRegistryEntry.java             # Data model for registry entry
├── PluginInstalledEntry.java            # Data model for installed plugin
└── PluginTableCellRenderer.java         # Custom table cell styling

ENGINE — minor changes:
Engine/src/main/java/com/ing/engine/plugin/loader/
├── PluginMetadata.java                  # NEW: reads metadata from installed plugin JAR
```

## Implementation Tasks (8 weeks)

### Week 1-2: Azure DevOps Repo Setup

1. Create `ingenious-plugins` Git repo
2. Create initial `registry.json` with schema
3. Create PR pipeline YAML:
   ```yaml
   trigger: none
   pr:
     branches:
       include:
         - main
   
   pool:
     vmImage: 'ubuntu-latest'
   
   steps:
   - task: PowerShell@2
     displayName: 'Validate Plugin PR'
     inputs:
       targetType: 'inline'
       script: |
         # Check registry.json is valid JSON
         # Check all referenced JARs exist
         # Check each JAR has 'pluginEntryClasses' in manifest
         # Check no duplicate plugin names
         # Check no duplicate action names across all plugins
         # (Detailed validation script to be written)
   ```
4. Upload a sample plugin JAR for testing

### Week 3-5: IDE Plugin Manager UI

1. Create `PluginRegistryEntry.java` — data model matching JSON schema
2. Create `PluginInstalledEntry.java` — tracks installed plugin metadata
3. Create `PluginManagerService.java`:
   - `fetchRegistry()` — HTTP GET registry.json from Azure DevOps raw endpoint
   - `installPlugin(PluginRegistryEntry)` — download JAR + libs → extract to `Resources/plugins/<name>/`
   - `uninstallPlugin(String name)` — delete plugin folder
   - `getInstalledPlugins()` — scan `Resources/plugins/` for installed plugins
   - `checkForUpdates()` — compare installed versions against registry
4. Create `PluginManagerBrowseUI.java` — JTable with columns: Name, Description, Author, Version, Actions + Install button
5. Create `PluginManagerInstalledUI.java` — JTable with: Name, Version, Actions count + Uninstall/Update buttons
6. Create `PluginManager.java` — JPanel with JTabbedPane (Browse | Installed)

### Week 6-7: Integration & Polish

1. Register PluginManager as a slide in `AppMainFrame.java`:
   - Similar to how APITester is registered: `slideShow.addSlide("PluginManager", pluginManager)`
   - Add navigation in `SimpleDock.java` or `FXToolBar.java`
2. Add toolbar button: `Plugin Manager`
3. Add action handler in `AppActionListener.java`:
   ```java
   case "Plugin Manager":
       sMainFrame.showPluginManager();
       break;
   ```
4. Pre-install conflict detection:
   - Before installing, scan new plugin's `@Action` methods
   - Compare against already-installed + core actions
   - Show warning dialog if conflicts found
5. Version compatibility check:
   - Compare `minEngineVersion` against `About.getBuildVersion()`
   - Show warning if incompatible
6. Restart banner after install/uninstall

### Week 8: Testing & Documentation

1. End-to-end test: install a plugin → restart → actions appear in step builder
2. Test uninstall → restart → actions removed
3. Test duplicate detection
4. Test version incompatibility warnings
5. Write internal team docs
6. Write Azure DevOps pipeline docs for plugin authors

## Key Integration Points

### Slide Registration (AppMainFrame.java)
Add roughly 10 lines to `AppMainFrame` constructor:
```java
private PluginManager pluginManager;  // new field

// In constructor, after apiTester initialization:
pluginManager = new PluginManager(this);
progressed(55);

// In init(), alongside other slides:
slideShow.addSlide("PluginManager", pluginManager);

// New method:
public void showPluginManager() {
    getGlassPane().setVisible(false);
    slideShow.showSlide("PluginManager");
    if (fxStatusBar != null) fxStatusBar.setCurrentView("Plugin Manager");
}
```

### Action Listener (AppActionListener.java)
Add one case:
```java
case "Plugin Manager":
    sMainFrame.showPluginManager();
    break;
```

### FXToolBar
Add one button:
```java
createButton("Plugin Manager", "PluginManager"),
```

## Authentication — No PAT Required

Since this is an internal company deployment, there's a **much better approach** than asking every user for a PAT:

### Option A: Public / Allow-list (Recommended)

Make the `ingenious-plugins` Git repo **public (within the org)** or grant **read access** to a specific Azure DevOps group that all INGenious users are members of. The Git REST API for public repos requires **no authentication** for read operations:

```
GET https://dev.azure.com/{org}/{project}/_apis/git/repositories/ingenious-plugins/items?path=/registry.json
```

**Zero credential management.** If the repo can be made browseable to all INGenious users, this is the simplest and most secure path.

### Option B: Encrypted PAT in AppSettings

If anonymous read isn't possible, use the **existing AES encryption** already in the framework (`Encryption.java`):

1. Admin generates a **single read-only PAT** for the registry repo (scoped to `Code (Read)` only)
2. A setup script encrypts it using the same AES/GCM mechanism the framework already uses for passwords
3. The encrypted value is stored in `Configuration/app.settings` under a `pluginRegistryToken` key
4. At runtime, the IDE decrypts it in memory for the HTTP request — never stored in plain text

This is identical to how the framework already handles encrypted proxy passwords and test data. The PAT exists only in memory after decryption.

### Option C: Azure CLI / Device Code Flow (Future)

The IDE could launch `az devops` CLI login or use the device authorization flow. But this adds complexity and a dependency on the Azure CLI being installed — not recommended for the MVP.

### Recommended: Option A

Make the plugin registry repo browsable to the user base. No tokens, no encryption, no setup for individual users. The only risk is if someone puts sensitive data in the registry — but it's just a metadata file + JARs, so there's nothing sensitive to leak.

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| `dev.azure.com` blocked on some machines | Add proxy settings support; fall back to manual download mode |
| Plugin breaks engine | Class loader isolation already handles this (child-first) |
| Duplicate action names | Pre-install scan detects conflicts; warn with option to cancel |
| JAR contains malicious code | No runtime sandbox (beyond scope); rely on PR pipeline + code review |
| 2 months insufficient | Scope cut: skip auto-update, skip rating/reviews, skip search — just browse + install |
