# Plugin Marketplace — Architecture & Implementation Plan

> **Status:** PoC phase — Browse, Install, and Publish tabs working in IDE
> **Timeline:** 8 weeks total, 6 weeks done
> **Constraint:** No budget, no licenses, no IT approval needed

---

## Architecture Overview

```
┌──────────────────────────────────────────────────┐
│                 INGenious IDE                     │
│                                                   │
│  ┌──────────────────────────────────────────┐     │
│  │         Plugin Manager (3 tabs)           │     │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐  │     │
│  │  │ Browse    │ │ Installed│ │ Publish   │  │     │
│  │  │ (Market)  │ │ (Manage) │ │ (Author)  │  │     │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘  │     │
│  │       │             │            │         │     │
│  │  ┌────┴─────────────┴────────────┴──┐     │     │
│  │  │     PluginManagerService         │     │     │
│  │  │  - fetchRegistry()              │     │     │
│  │  │  - downloadPlugin()             │     │     │
│  │  │  - publishPlugin()              │     │     │
│  │  │  - extractActionsFromJar()      │     │     │
│  │  └────────────┬────────────────────┘     │     │
│  └───────────────┼──────────────────────────┘     │
│                  │                                 │
│                  │  HTTPS (no auth for reads)      │
└──────────────────┼─────────────────────────────────┘
                   │
┌──────────────────┼─────────────────────────────────┐
│  GitHub          │                                   │
│                   │                                   │
│  repo: Jul611/ingenious-plugins (SEPARATE REPO)      │
│  ┌───────────────┴────────────────────────────────┐  │
│  │  registry.json (catalog of all plugins)         │  │
│  │  plugins/                                       │  │
│  │   ├── demo-helloworld/                          │  │
│  │   │   └── demo-helloworld-1.0.0.jar             │  │
│  │   └── calc-math/                                │  │
│  │       └── calc-math-1.0.0.jar                   │  │
│  └────────────────────────────────────────────────┘  │
│                                                       │
│  API: api.github.com (live data, no cache)            │
│  Raw: raw.githubusercontent.com (CDN, for JARs)       │
└───────────────────────────────────────────────────────┘
```

---

## Component Decisions

| Component | What we use | Justification |
|---|---|---|
| **Registry storage** | GitHub repo `ingenious-plugins` | Separate from main INGenious repo — clean permissions, no repo bloat, independent release cycle |
| **Plugin catalog** | `registry.json` in repo root | Simple JSON, versionable, human-reviewable in PRs, no database needed |
| **Fetching registry** | **GitHub Contents API** (`api.github.com/repos/.../contents/registry.json`) | Returns live data instantly — no 5-minute CDN cache like `raw.githubusercontent.com` |
| **JAR downloads** | GitHub raw URL (`raw.githubusercontent.com`) | CDN-backed fast downloads, free, no auth for public repos |
| **IDE registry URL** | Configurable from `Configuration/app.settings` | Company can change URL without rebuilding IDE |
| **Browse tab** | JTable in Swing | Already built, matches existing IDE UI patterns |
| **Installed tab** | JTable + filesystem scan | Already built, reads `Resources/plugins/` directories |
| **Publish tab** | Select JAR → read manifest → extract @Action methods → stage to `Resources/plugins/` + update `registry.json` | Already built. **No GitHub PAT in IDE** — user runs `git add / commit / push` manually |
| **Engine plugin loading** | `PluginLoader` scans `Resources/plugins/` at startup | Already built — loads JARs via child-first `PluginClassLoader` |
| **Conflict detection** | In-IDE: compare new actions against already-installed + registry | Already built — warns before install |
| **Version compatibility** | `About.getBuildVersion()` vs `minEngineVersion` / `maxEngineVersion` | Already built — warns before install |
| **Plugin sandbox** | `PluginClassLoader` child-first loading with parent-first for engine/API/JDK | Already built — plugins cannot override engine classes |

---

## What We Are NOT Using

| Technology | Reason cut |
|---|---|
| Cloudflare Worker | Not enough time (2 months left), not needed for MVP |
| Database / KV store | No ratings/reviews in MVP — static JSON is sufficient |
| Any paid service | No budget, no license pull |
| GitHub PAT in IDE | Fragile — user commits/pushes manually instead |
| Azure DevOps | Company is migrating to GitHub — build for the target |

---

## Data Flow

### Fetching plugin list (Browse tab)

```
IDE startup / user clicks Refresh
       │
       ▼
PluginManagerService.fetchRegistry()
       │
       ├── Try GitHub Contents API (live, no cache)
       │   GET https://api.github.com/repos/Jul611/ingenious-plugins/contents/registry.json?ref=main
       │   Returns: file content (base64) + sha
       │   Decode → parse JSON → merge with local entries
       │
       └── On failure: fall back to local Resources/plugins/registry.json
                          (shipped with build or staged from previous publish)
       │
       ▼
   Returns List<PluginRegistryEntry>
       │
       ▼
   PluginManagerBrowseUI populates JTable
```

### Installing a plugin

```
User clicks "Install" on a row
       │
       ▼
   Pre-install checks:
   ├── Version compatibility? (minEngineVersion ≤ engine version ≤ maxEngineVersion)
   ├── Action conflicts? (any @Action name already registered?)
       │
       ▼
   PluginManagerService.downloadPlugin(entry)
       │
       ├── Download JAR from raw.githubusercontent.com
       ├── Download lib JARs from raw.githubusercontent.com
       ├── Write .plugininfo metadata to Resources/plugins/<name>/
       │
       ▼
   Show restart prompt
```

### Publishing a plugin (author workflow)

```
Plugin author builds JAR: mvn clean package
       │
       ▼
   Open INGenious → Plugins → Publish tab
       │
       ├── Browse → select .jar file
       ├── Manifest auto-read: Plugin-Name, Version, Author, pluginEntryClasses
       ├── @Action methods extracted via reflection
       ├── Form pre-filled — author edits if needed
       │
       ▼
   Click "Publish to Registry"
       │
       ├── Copies .jar → Resources/plugins/<name>/
       ├── Copies lib/*.jar → Resources/plugins/<name>/lib/
       ├── Writes .plugininfo metadata
       ├── Upserts Resources/plugins/registry.json
       │
       ▼
   IDE shows: "Plugin staged. Commit and push to publish remotely:"
       git add Resources/plugins/<name>/
       git add Resources/plugins/registry.json
       git commit -m "Add <name> plugin v<version>"
       git push
```

### Loading plugins at engine startup

```
INGenious starts
       │
       ▼
   PluginLoader.loadAllPluginsEntryClasses()
       │
       ├── Scan Resources/plugins/ for directories
       ├── For each directory: find *.jar files
       ├── Read pluginEntryClasses from JAR manifest
       ├── Create PluginClassLoader (child-first, parent-first for engine/API)
       ├── Load each entry class
       │
       ▼
   @Action methods registered in MethodInfoManager
       │
       ▼
   Actions appear in Step Builder
```

---

## Registry Schema (`registry.json`)

Stays in the new `ingenious-plugins` repo root:

```json
{
  "version": 1,
  "plugins": [
    {
      "name": "demo-helloworld",
      "displayName": "Hello World Demo",
      "description": "A demo plugin that adds Hello World and custom greeting actions",
      "author": "INGenious Plugins Team",
      "authorEmail": "plugins@company.com",
      "version": "1.0.0",
      "minEngineVersion": "3.0.0",
      "maxEngineVersion": "",
      "downloadUrl": "https://raw.githubusercontent.com/Jul611/ingenious-plugins/main/plugins/demo-helloworld/demo-helloworld-1.0.0.jar",
      "libUrls": [],
      "objectTypes": ["General"],
      "actionCount": 2,
      "actions": ["sayHelloWorld", "sayCustomGreeting"],
      "homepageUrl": "",
      "license": "MIT",
      "releaseNotes": "Initial release",
      "dateAdded": "2026-07-13",
      "featured": true,
      "githubRepo": ""
    }
  ]
}
```

---

## Plugin JAR Manifest Requirements

Plugin authors must add to their `pom.xml`'s `maven-jar-plugin`:

```xml
<manifestEntries>
    <Plugin-Name>Calculator Math</Plugin-Name>
    <Plugin-Version>1.0.0</Plugin-Version>
    <Plugin-Description>Adds and multiplies numbers</Plugin-Description>
    <Plugin-Author>Your Name</Plugin-Author>
    <Plugin-Author-Email>you@company.com</Plugin-Author-Email>
    <Plugin-Min-Engine-Version>3.0.0</Plugin-Min-Engine-Version>
    <pluginEntryClasses>com.ing.plugins.calc.CalcActions</pluginEntryClasses>
</manifestEntries>
```

The Publish tab reads these automatically.

---

## API Reference

### GitHub Contents API (for fetching registry.json)

```
GET https://api.github.com/repos/Jul611/ingenious-plugins/contents/registry.json

Response:
{
  "name": "registry.json",
  "path": "registry.json",
  "sha": "abc123...",
  "content": "<base64-encoded file content>",
  "encoding": "base64"
}
```

Decode `content` from base64 → JSON → parse.

### GitHub Raw URL (for downloading JARs)

```
GET https://raw.githubusercontent.com/Jul611/ingenious-plugins/main/plugins/<name>/<jar-name>.jar
```

No auth needed for public repos. CDN-cached (acceptable for JAR downloads).

---

## Remaining Implementation Tasks

### 1. Create `ingenious-plugins` repo
- Create `Jul611/ingenious-plugins` on GitHub
- Move `Resources/plugins/registry.json` to repo root
- Move plugin directories to `plugins/` subdirectory
- Add `CONTRIBUTING.md`
- Time: 30 minutes

### 2. Update fetch URL to use Contents API
- Change `REMOTE_REGISTRY_URL` from `raw.githubusercontent.com` to `api.github.com/repos/.../contents/registry.json?ref=main`
- Add base64 decoding for API response
- Time: 30 minutes

### 3. Make registry URL configurable from app.settings
- Read `plugin.registry.url` from `Configuration/app.settings`
- Fall back to hardcoded default if not set
- Time: 30 minutes

### 4. Simplify Publish tab — remove GitHub PAT
- Remove `githubTokenField` and associated logic
- After local publish succeeds, show dialog with git commands user should run
- Time: 20 minutes

### 5. Clean up `PluginManagerService`
- Remove `publishPluginToGitHub()` method
- Remove `pushToGithub()` and `getExistingSha()` (no longer needed)
- Time: 10 minutes

---

## File Inventory

### New files to create

| File | Purpose |
|---|---|
| `docs/plugin-marketplace-plan.md` | This document — architecture plan |
| `scripts/publish_plugin.py` | CLI tool for CI/automated publishing (optional) |

### Existing files (no changes needed)

| File | Purpose |
|---|---|
| `PluginManagerBrowseUI.java` | Browse tab — reads registry, shows table, install button |
| `PluginManagerInstalledUI.java` | Installed tab — lists plugins, uninstall button |
| `PluginManager.java` | Container with JTabbedPane (Browse, Installed, Publish) |
| `PluginRegistryEntry.java` | Data model for registry.json entries |
| `PluginInstalledEntry.java` | Data model for locally installed plugins |
| `PluginTableCellRenderer.java` | Custom table rendering (if created) |
| `PluginMetadata.java` (Engine) | Reads plugin info from JAR manifests |
| `PluginLoader.java` (Engine) | Scans `Resources/plugins/` for plugin JARs |
| `PluginClassLoader.java` (Engine) | Child-first classloader for plugin isolation |
| `AppMainFrame.java` | Slide registration for PluginManager |
| `AppActionListener.java` | "Plugin Manager" action handler |
| `FXToolBar.java` | Toolbar button to open Plugin Manager |
| `INGIcons.java` | Puzzle icon for Plugin Manager |
| `Resources/plugins/registry.json` | Local copy of the registry (fallback + staging) |

### Files to modify

| File | Change |
|---|---|
| `PluginManagerService.java` | Change fetch URL to GitHub API, remove GitHub push methods |
| `PluginManagerPublishUI.java` | Remove PAT field, show git instructions after publish |

---

## Timeline (Remaining 2 Weeks)

| Day | Task |
|---|---|
| **Day 1** | Create `ingenious-plugins` repo, move files |
| **Day 1** | Update `REMOTE_REGISTRY_URL` to GitHub Contents API |
| **Day 1** | Make URL configurable from `app.settings` |
| **Day 2** | Simplify Publish tab — remove PAT, show git commands |
| **Day 2** | Build + test full flow end-to-end |
| **Day 3** | Write plugin template repo + author docs |
| **Day 4** | Polish, fix any remaining bugs |
| **Day 5** | Present demo to stakeholders |
| **Week 2** | Handover documentation, internship wrap-up |

---

## How to Demo

1. Open INGenious → click **Plugins** toolbar button
2. **Browse** tab shows 2 plugins from remote registry
3. Switch to **Installed** tab (shows any locally installed)
4. Switch to **Publish** tab → **Browse...** → select `plugin-examples/calc-math/target/calc-math-1.0.0.jar`
5. Form auto-fills: name "Calculator Math", version "1.0.0", actions "addNumbers, multiplyNumbers"
6. Click **Publish to Registry**
7. Dialog shows: *"Plugin staged. Run these commands to push to GitHub:"* with `git add`, `git commit`, `git push`
8. Switch to **Browse** → **Refresh** — calculator-math appears

## Company IT Restriction Mitigations

> Real constraints from corporate laptops: blocked websites, restricted downloads, no admin rights.

### What's blocked in most companies

| Restriction | Impact on plugin marketplace | Our mitigation |
|---|---|---|
| `raw.githubusercontent.com` blocked | IDE can't download JARs from raw URLs | **Already handled:** `PluginManagerService.downloadPlugin()` already has a `file://` URL path. Users can download JARs manually from a permitted source (internal share, email, portal) and use "Import from File" |
| `api.github.com` blocked | IDE can't fetch registry | **Fallback to local:** `fetchRegistry()` already falls back to `Resources/plugins/registry.json` shipped with the build. IT can bundle approved plugins in the distribution |
| `maven.org` / `mvnrepository.com` blocked | Plugin authors can't build JARs | **Use internal mirror:** Company Maven proxy (Nexus/Artifactory) handles this. Plugin template repo uses the company's internal Maven repo URL |
| No internet access at all | Entire marketplace unreachable | **Bundled plugins only:** IT ships pre-approved plugin JARs inside the INGenious installer zip. Plugin Manager shows only the Installed tab. Users get the plugins IT has approved |
| USB / file transfer blocked | Can't import JAR from USB | **Internal network share:** Plugin Manager supports a configurable `file://` path for a network share. IT publishes plugins to `\\companyfs\share\ingenious-plugins\` and the IDE reads from there |
| Git push blocked from corporate laptops | Authors can't push plugin changes | **CI/CD pipeline:** Author pushes via a build server (Jenkins/GitHub Actions), not from their locked-down laptop. The publish script is run in CI where it has network access |

### The "Import from File" button (already built)

The Plugin Manager already has an **Import from File** button in the bottom bar. This is the primary fallback when network is restricted:

1. IT or a teammate downloads the plugin JAR on an unrestricted machine
2. Transfers it via approved channel (email, Teams, internal wiki, network share)
3. User clicks **Import from File** → selects the JAR → IDE copies it to `Resources/plugins/`
4. Plugin appears in Installed tab after restart

This works regardless of network restrictions. No GitHub, no API, no PAT needed.

### The network share option (already designed)

The architecture doc (Backup Plan #4) describes a configurable local/network path:

```properties
# In Configuration/app.settings
plugin.registry.url=file:///Z:/shared/ingenious-plugins/registry.json
```

The same `PluginManagerService` code handles `file://` URLs identically to `https://` URLs — it just copies files. Changing the URL in settings is all IT needs to do.

### The bundled plugins option (already designed)

If ALL remote access is blocked, plugins ship inside the INGenious distribution zip:

```
ingenious-v3.1.0.zip
├── Resources/
│   ├── plugins/
│   │   ├── registry.json          ← Pre-populated with IT-approved plugins
│   │   ├── ocr-reader/
│   │   │   └── ocr-reader-1.0.0.jar
│   │   └── sap-connector/
│   │       └── sap-connector-2.1.0.jar
│   └── ...
```

The IDE uses the local `fetchRegistryLocal()` path. Browse tab works the same. All plugin data comes from the shipped files.

### What to tell stakeholders

> "The plugin marketplace works **with or without internet**. If GitHub is accessible, the IDE fetches and downloads plugins live. If not, plugins are bundled with the installer or loaded from a network share. The same UI, the same code — the only difference is where the files come from."

This is actually a strength: you can demo it with internet, deploy it without.

---

## Future Enhancements (No-Restrictions Wishlist)

> What this project would look like if we had budget, time, and organizational buy-in.

### 1. Cloudflare Worker API Backend
Replace the static `registry.json` with a proper API layer:
- `GET /plugins` — returns plugin list with live download counts
- `POST /plugins` — publish a plugin (multipart: metadata + JAR)
- `GET /plugins/{name}/stats` — download count, average rating, version history
- `POST /plugins/{name}/rate` — submit a 1-5 star rating

**Free tier handles thousands of requests/month. ~200 lines of JS.**

### 2. Plugin Sandbox (Security)
Currently plugins have full JVM access — same as the engine. With restrictions:
- **Java Security Manager** — restrict file system, network, and class loading permissions per plugin
- **Code signing** — plugin JARs signed by trusted author keys, IDE verifies before install
- **Dependency scanning** — PR pipeline runs `trivy` or `snyk` on plugin dependencies

### 3. Proper Backend (Web App + Database)
If the community takes off:
- **Next.js / FastAPI** web app with user accounts (OAuth via GitHub/Google)
- **PostgreSQL** — ratings, reviews, download history, per-user favorites
- **Plugin analytics dashboard** — "how many people use my plugin?"
- **Search + filtering** — full-text search, sort by downloads/rating/date

### 4. Automated Testing in CI
- PR pipeline runs plugin tests against the engine
- Compatibility matrix: which engine versions does this plugin work with?
- Visual diff of new @Action methods against the registry

### 5. In-IDE Plugin Creation Wizard
- Right-click → "New Plugin" → generates Maven project + sample `@Action` class
- One-click build + publish from the IDE
- No separate terminal needed

### 6. Auto-Update
- Background thread checks for updates every 24 hours
- Badge on Installed tab: "3 updates available"
- One-click update: download new JAR, replace, prompt restart

### 7. Plugin Dependency Resolution
- `registry.json` supports `dependsOn` field
- Installing a plugin auto-installs its dependencies
- Version range resolution (semver)

### 8. Enterprise Features
- **Private registry** — company-hosted plugin repo behind VPN
- **Approval workflow** — plugins must be approved by admin before appearing in Browse
- **Audit log** — who installed what, when
- **Bundled distribution** — IT pre-selects plugins, ships them inside the INGenious installer

### What Would Change With Budget

| Restriction | Today's workaround | With budget |
|---|---|---|
| No server | Static JSON in GitHub repo | Cloudflare Worker + R2 or full backend |
| No database | Registry is flat file | PostgreSQL / PlanetScale |
| No CDN | GitHub raw URL (free) | Cloudflare CDN or custom domain |
| No auth system | Git commit access = publishing rights | OAuth (GitHub/Google/SSO) |
| No devops time | Manual PRs for plugin submission | GitHub Actions + auto-deploy |
| No UI designer | Raw Swing JTables | Modern web-based marketplace |
| No security audit | Relies on code review | Java Security Manager + code signing |
