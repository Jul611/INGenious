# Plugin Marketplace — Current Progress

> Last updated: 2026-07-21

---

## What's Built

### 1. Registry Fetching (Browse Tab)
- Fetches plugin catalog from private repo (`Jul611/ingenious-plugins-trial`) via GitHub Contents API
- Uses the user's publishing PAT from Profile dialog (single PAT for read + write)
- No local fallback — if remote fetch fails, Browse tab shows empty list
- ✅ Done

### 2. Plugin Installation (Install & Installed Tabs)
- Downloads JARs from GitHub Release assets (authentication via user's PAT)
- Installs to `Dist/release/plugins/<name>/`
- Resolves private repo release URLs via GitHub API (`/releases/assets/{id}` with `Accept: application/octet-stream`)
- Creates `.plugininfo` metadata file on install
- Uninstall removes plugin directory
- ✅ Done

### 3. Profile / User Config
- Profile icon button in toolbar (top-right)
- Dialog with **PCode**, **User Code**, and **Publishing PAT** fields
- Saved to `Configuration/user-config.json` (gitignored, plain text — no encryption)
- "How to create a PAT?" link opens `https://github.com/settings/tokens?type=beta` in browser
- Instructions label: "Required: Contents: Read & Write access on [repo URL] — You must be a collaborator"
- ✅ Profile dialog UI done
- ✅ PAT stored in plain text (no encryption fragility)
- ✅ PAT read fresh on every request (no stale cache)

### 4. Direct GitHub Publishing (Publish Tab)
- **"Publish Directly to Registry"** button creates a GitHub Release in one click:
  - Creates release with tag `{name}-v{version}`
  - Uploads JAR as release asset
  - Updates `registry.json` via Contents API
- **"Export Submission for PR"** button exports JAR + README + registry.json for manual PR workflow
- Conflict detection: checks for duplicate action names against live registry before publishing
- PAT check: warns if no PAT configured, offers to open Profile dialog
- Progress dialog with indeterminate progress bar during API calls
- Background execution via `SwingWorker` (UI stays responsive)
- ✅ Done

### 5. Data Models
- `PluginRegistryEntry` — includes `readmeUrl`, `entryClasses` fields
- `PluginInstalledEntry` — tracks local installed plugins from `.plugininfo`
- `UserConfig` — per-user settings (PCode, user code, publishing PAT)
- ✅ Done

### 6. Security & CI Pipeline
- `.github/workflows/publish-plugin.yml` — validates plugins on PR, creates releases on merge
- Checks: JSON validity, duplicate action names, manifest presence, README quality (50+ words), suspicious imports, Trivy scan
- ⚠️ Pipeline YAML exists but needs to be moved to the plugins repo

### 7. Engine Plugin Loading
- `PluginLoader` scans `plugins/` directory on startup
- `PluginMetadata` reads JAR manifest + `.plugininfo`
- Version compatibility check (`minEngineVersion` vs `About.getBuildVersion()`)
- ✅ Done

### 8. Sample Plugins
- `helloworld` — `sayHelloWorld`, `sayCustomGreeting` actions
- `screenshots` — `takeFullPageScreenshot`, `captureElementScreenshot`, `logPageInfo` actions
- `calc-math` — math operations
- ✅ Done

---

## Token Flow

Single PAT, one source, used for everything:

```
User sets PAT in Profile dialog
        │
        ▼
  Configuration/user-config.json (gitignored, plain text)
        │
        ▼
  UserConfig.getPublishPat()
        │
        ├── getRegistryToken() → used for registry fetch & download (Browse/Install tabs)
        └── publishDirectly()  → used for release creation + registry update (Publish tab)
```

No env var fallback, no `app.settings` lookup, no encryption. If no PAT is configured, the private repo returns 401 and features requiring auth will fail gracefully.

---

## Architecture (Current)

```
IDE PluginManagerService
  │
  ├── fetchRegistry()
  │     └── GET api.github.com/repos/.../contents/registry.json
  │           (Authorization: token <pat>, base64-decoded)
  │
  ├── downloadPlugin()
  │     ├── resolveReleaseAssetUrl()  ← github.com/releases/download → API asset endpoint
  │     └── downloadFile()            ← GET /repos/.../releases/assets/{id}
  │           (Authorization + Accept: application/octet-stream, manual redirects)
  │
  ├── prepareSubmission()            ← Export for PR
  │     └── Exports: plugins/<name>/<jar> + README.md + updated registry.json
  │
  └── publishDirectly()              ← Direct publish
        ├── POST /repos/.../releases  (creates release)
        ├── POST /releases/assets     (uploads JAR)
        └── PUT /contents/registry.json (updates registry)
```

---

## Files You Should Know

| File | Purpose | Status |
|---|---|---|
| `IDE/.../pluginmanager/PluginManagerService.java` | Core: fetch, install, resolve releases, publish directly | ✅ Done |
| `IDE/.../pluginmanager/PluginManagerPublishUI.java` | Publish tab UI with direct publish + export for PR | ✅ Done |
| `IDE/.../pluginmanager/PluginManagerBrowseUI.java` | Browse tab UI | ✅ Done |
| `IDE/.../pluginmanager/PluginManagerInstalledUI.java` | Installed tab UI | ✅ Done |
| `IDE/.../pluginmanager/PluginManager.java` | Tab container | ✅ Done |
| `IDE/.../pluginmanager/UserConfig.java` | PCode, user code, PAT (plain text, gitignored) | ✅ Done |
| `IDE/.../pluginmanager/PluginRegistryEntry.java` | Data model with `readmeUrl` field | ✅ Done |
| `IDE/.../fx/FXToolBar.java` | Profile button + PAT help link in toolbar | ✅ Done |
| `IDE/.../settings/AppSettings.java` | App settings (PLUGIN_REGISTRY_TOKEN removed) | ✅ Done |
| `Engine/.../plugin/loader/PluginLoader.java` | Scans `plugins/` on IDE startup | ✅ Done |
| `Configuration/user-config.json` | User settings + PAT (gitignored) | ✅ Exists |
| `.github/workflows/publish-plugin.yml` | CI pipeline for validation + auto-publish | ⏳ Move to plugins repo |

## Quick Links

| File | Path |
|---|---|
| PluginManagerService | `IDE/src/main/java/com/ing/ide/main/mainui/components/pluginmanager/PluginManagerService.java` |
| PluginManagerPublishUI | `IDE/src/main/java/com/ing/ide/main/mainui/components/pluginmanager/PluginManagerPublishUI.java` |
| UserConfig | `IDE/src/main/java/com/ing/ide/main/mainui/components/pluginmanager/UserConfig.java` |
| PluginRegistryEntry | `IDE/src/main/java/com/ing/ide/main/mainui/components/pluginmanager/PluginRegistryEntry.java` |
| FXToolBar | `IDE/src/main/java/com/ing/ide/main/fx/FXToolBar.java` |
| AppSettings | `IDE/src/main/java/com/ing/ide/settings/AppSettings.java` |
| CI Pipeline | `.github/workflows/publish-plugin.yml` |

---

## Publishing Flow

### Option A: Direct Publish (one-click)
1. Build your plugin JAR: `mvn package -pl plugin-examples/your-plugin`
2. Open Plugin Manager → Publish tab
3. Browse to select the JAR
4. Review auto-extracted metadata, fill in details, write README
5. Click **"Publish Directly to Registry"**
6. IDE creates a GitHub Release, uploads the JAR, and updates registry.json

### Option B: Export for PR (manual review)
1-4. Same as above
5. Click **"Export Submission for PR"**
6. Clone `Jul611/ingenious-plugins-trial`, copy exported files, commit, push, open PR
7. CI validates and creates the release on merge

---

## How to Test

```bash
# 1. Build the IDE
mvn compile -pl IDE -am -DskipTests
Copy-Item IDE/target/ingenious-ide-3.0.0.jar Dist/release/lib/ -Force

# 2. Launch
Dist\release\ingenious.bat

# 3. Set PAT
#    Profile icon (top-right) → paste GitHub PAT with Contents: Read & Write access
#    on Jul611/ingenious-plugins-trial → OK

# 4. Browse plugins
#    Plugins button → Browse tab → should load from GitHub

# 5. Test publish
#    Publish tab → Browse → select plugin-examples/helloworld/target/demo-helloworld-1.0.0.jar
#    Change Version to 1.0.1 → Publish Directly
```

## What's Not Built / Future

| Feature | Status |
|---|---|
| GitHub OAuth device flow (replace PAT) | ⏳ Future |
| Move CI pipeline to plugins repo | ⏳ Pending |
| Plugin template repo / scaffolder | ⏳ Pending |
| Plugin dependency resolution | ⏳ Future |
| **Source-based publishing** — submit `pom.xml` + `.java` instead of pre-built JAR; CI compiles and creates the release. Enables code review, reproducible builds, no opaque binaries in git. Publish tab becomes a project scaffolder. | ⏳ Flagged for later revisit |
| Download analytics | ⏳ Future |
