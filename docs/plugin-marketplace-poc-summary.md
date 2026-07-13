# Plugin Marketplace PoC — Summary

## What Was Built

### Plugin Manager UI (6 new files in `IDE/`)

| File | Purpose |
|---|---|
| `PluginManager.java` | Main tab with Browse + Installed tabs, Import from File button |
| `PluginManagerBrowseUI.java` | Marketplace table showing registry plugins with Install button, **conflict detection**, **version compatibility check** |
| `PluginManagerInstalledUI.java` | Table of installed plugins with Uninstall button (MouseListener pattern, reliable) |
| `PluginManagerService.java` | Fetches registry (GitHub first, local fallback), downloads JARs, lists/uninstalls |
| `PluginRegistryEntry.java` | Data model for registry.json entries |
| `PluginInstalledEntry.java` | Data model for locally installed plugins |

### Integration Points (4 modified files)

| File | Change |
|---|---|
| `AppMainFrame.java` | Added `pluginManager` slide + `showPluginManager()` method registered as "PluginManager" |
| `AppActionListener.java` | Added "Plugin Manager" case that calls `showPluginManager()` |
| `INGIcons.java` | Registered puzzle icon `getPluginManagerIcon` via `getIngIcon` |
| `FXToolBar.java` | Added "Plugins" button to the toolbar |

### Engine Plugin Loading (2 new + 1 modified file)

| File | Change |
|---|---|
| `PluginLoader.java` | **Fixed** — now scans both `Resources/plugins/` and `{appRoot}/plugins/` |
| `PluginMetadata.java` | **New** — reads plugin metadata from JAR manifests (Plugin-Name, Plugin-Version, etc.) and .plugininfo files |

### Pre-Install Safety Checks

- **Version compatibility**: checks `minEngineVersion`/`maxEngineVersion` against `About.getBuildVersion()` before install
- **Conflict detection**: scans already-installed plugins for duplicate `@Action` method names before install
- **Restart prompt**: user can restart immediately after install/uninstall

### Registry

- `Resources/plugins/registry.json` — 2 plugins listed
- `Dist/release/plugins/registry.json` — in sync
- Service (`PluginManagerService`) tries `REMOTE_REGISTRY_URL` first, falls back to local file on failure
- Remote URL: `https://raw.githubusercontent.com/Jul611/INGenious/initiative-repo/Resources/plugins/registry.json`

### Sample Plugins (in `plugin-examples/`)

| Plugin | Object Types | Actions | JAR location |
|---|---|---|---|
| `helloworld/` | General | `sayHelloWorld`, `sayCustomGreeting` | `Resources/plugins/demo-helloworld/` |
| `screenshots/` | Playwright, General | `takeFullPageScreenshot`, `captureElementScreenshot`, `logPageInfo` | `Resources/plugins/screenshots/` |

Each has:
- A `pom.xml` with `maven-jar-plugin` set to add `pluginEntryClasses` manifest attribute
- An `src/main/java/` class with `@Action` methods and a `CommandPluginApi` constructor
- A `lib/` folder with `ingenious-api-3.0.jar` for compilation

### Build JARs

Both built JARs are in `Resources/plugins/` — served via `raw.githubusercontent.com` URLs pointing to the `initiative-repo` branch.

## Known Issues

- **Install/Uninstall buttons** — both tabs now use the reliable MouseListener pattern (installed tab was fixed from old ButtonEditor pattern)
- **Dist/release/plugins/** — synchronized with Resources/plugins/

## Remaining Work for Week 8

1. **Engine PluginLoader integration** — the `MethodInfoManager.load()` already scans `{appRoot}/plugins/` for `@Action` annotations. The `PluginLoader` now also scans `Resources/plugins/`. Still needs to verify end-to-end: install plugin → restart → actions appear in step builder
2. **Test suite** — unit tests for conflict detection, version parsing, and install/uninstall flows
3. **PR pipeline** — GitHub Actions YAML for plugin registry validation
4. **Plugin author documentation** — CONTRIBUTING.md for the plugins repo

## How to Test

1. Build: `mvn compile -pl IDE -am`
2. Launch INGenious from `IDE/src/main/java/com/ing/ide/main/Main.java`
3. Click **Plugins** button in the toolbar
4. Browse tab loads — shows plugin list
5. Click Install on a row — runs conflict + version checks, then downloads
6. Switch to Installed tab — see the plugin
7. Uninstall button works reliably

## How to Write a Plugin

1. Copy `plugin-examples/helloworld/` as a template
2. Write a class with `@Action` methods. Constructor must take `CommandPluginApi api`
3. Set `pluginEntryClasses` in `pom.xml`'s `maven-jar-plugin` config to your fully-qualified class name
4. Build: `mvn clean package`
5. Either add to registry.json + upload JAR to GitHub, or distribute the JAR directly
