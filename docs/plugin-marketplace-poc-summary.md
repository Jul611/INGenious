# Plugin Marketplace PoC — Summary

## What Was Built

### Plugin Manager UI (6 new files in `IDE/`)

| File | Purpose |
|---|---|
| `PluginManager.java` | Main tab with Browse + Installed tabs, Import from File button |
| `PluginManagerBrowseUI.java` | Marketplace table showing registry plugins with Install button |
| `PluginManagerInstalledUI.java` | Table of installed plugins with Uninstall button |
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

### Registry

- `Resources/plugins/registry.json` — 2 plugins listed
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

## Known Issue: Install Button Not Working

The `PluginManagerBrowseUI.java` was rewritten to use a `MouseListener` on the table instead of a `CellEditor`, which should be more reliable. The code:

```java
table.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        int col = table.columnAtPoint(e.getPoint());
        int row = table.rowAtPoint(e.getPoint());
        if (col == INSTALL_COL && row >= 0) {
            int modelRow = table.convertRowIndexToModel(row);
            installPlugin(modelRow);
        }
    }
});
```

The `PluginManagerInstalledUI.java` still uses the old `ButtonEditor` pattern — may have the same issue.

**To debug**: add `System.out.println("DEBUG click at col=" + col + " row=" + row)` inside the mouse listener.

## How to Test

1. Build: `mvn compile -pl IDE -am`
2. Launch INGenious from `IDE/src/main/java/com/ing/ide/main/Main.java` (?)
3. Click **Plugins** button in the toolbar
4. Browse tab loads — shows plugin list
5. Click Install on a row — calls `service.downloadPlugin()`
6. Switch to Installed tab — see the plugin

## What Still Needs Work

1. **Install click reliability** — debug if still not working
2. **Also fix Uninstall button** in `PluginManagerInstalledUI.java` (same rewrite needed)
3. **Engine PluginLoader integration** — the `Engine` needs to scan `Resources/plugins/` for installed plugin JARs and load `@Action` classes via the `pluginEntryClasses` manifest. Currently the Plugin Manager just writes files to disk
4. **registry.json duplicates** — both `Resources/plugins/` and `Dist/release/plugins/` have copies. Keep in sync
5. **Branch** — all work is on `initiative-repo` branch

## How to Write a Plugin

1. Copy `plugin-examples/helloworld/` as a template
2. Write a class with `@Action` methods. Constructor must take `CommandPluginApi api`
3. Set `pluginEntryClasses` in `pom.xml`'s `maven-jar-plugin` config to your fully-qualified class name
4. Build: `mvn clean package`
5. Either add to registry.json + upload JAR to GitHub, or distribute the JAR directly
