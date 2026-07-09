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

## Backup Plans (Contingency)

Each layer of the architecture has a fallback if the company environment blocks the primary approach.

### 1. If Azure DevOps HTTP is blocked

| Primary | Backup |
|---|---|
| IDE fetches registry + downloads JARs via HTTPS from Azure DevOps Git REST API | **Manual mode**: Plugin Manager shows an **"Import from File..."** button. User downloads the JAR + `lib/` folder from a browser, then uses the IDE dialog to select the files. The IDE just handles extraction to `Resources/plugins/<name>/`. |

The manual mode reuses all the same code (conflict detection, version check, extraction) — only the download step changes. It's a 50-line fallback.

### 2. If Git repo permissions are complex

| Primary | Backup |
|---|---|
| Dedicated `ingenious-plugins` Git repo with org-wide read access | **Reuse the main INGenious repo**: Put a `plugins/registry.json` and plugin JARs directly in the main `INGenious` GitHub repo under a `plugins/` directory at the root. The IDE fetches from GitHub raw content (same as the main repo access). No new repo to manage. |

But this muddies the main repo — plugin submissions would go through the main project's PR process.

### 3. If PAT / auth is required and encryption is blocked

| Primary | Backup |
|---|---|
| Encrypted PAT stored in `app.settings`, decrypted at runtime | **Env variable**: The PAT is read from an environment variable `INGENIOUS_PLUGIN_TOKEN` set by IT via group policy or login script. The IDE reads it directly with `System.getenv("INGENIOUS_PLUGIN_TOKEN")`. No storage, no encryption needed. |

### 4. If the entire HTTP approach is blocked

| Primary | Backup |
|---|---|
| In-IDE marketplace fetching from Azure DevOps | **Share drive / intranet**: Plugins are published to a network share (e.g. `\\companyfs\share\ingenious-plugins\`). The IDE scans a configurable local folder path instead of fetching via HTTP. Plugin Manager works the same — just pointed at a directory instead of a URL. |

This is almost zero implementation work — `PluginManagerService` takes a `sourceUrl` parameter, and it can be a `file:///` path just as easily as an `https://` URL.

### 5. If company policy blocks ALL remote connections

| Primary | Backup |
|---|---|
| Any online/network registry | **Bundled plugins**: Plugin JARs are shipped inside the INGenious distribution zip itself (in `Resources/plugins/`). The IDE still has the Plugin Manager UI but it only shows the Installed tab — no Browse tab. Users get the plugins IT has pre-approved and bundled. |

---

## Community Features: Downloads, Ratings, and What They Require

A static Git repo (`registry.json` + JARs) supports browsing and downloading, but **not** dynamic data like download counts or ratings. Those need a writable backend.

### What a static Git repo CAN do (minimal effort)

| Feature | How |
|---|---|
| Plugin listing | Parsed from `registry.json` |
| Download | IDE downloads JAR from raw.githubusercontent.com |
| GitHub stars | Via GitHub REST API (per-repo, not per-plugin) |
| Release downloads | Via GitHub Releases API (if plugins are published as releases) |

### What a static Git repo CANNOT do

| Feature | Why |
|---|---|
| Per-plugin download counter | No write endpoint to increment a counter |
| User ratings / reviews | No database, no auth, no write API |
| "Most popular" sorting | No data to sort by |
| "Recently updated" sorting | Only by checking the last commit that touched a plugin's metadata — hacky |

### GitHub Releases — What You Need to Know

GitHub Releases is a free feature on all GitHub plans. Here's what it costs, what the limits are, and whether it's the right choice for plugins.

#### Is it free?

| Plan | Release Storage | Bandwidth | API Rate Limit (unauthenticated) |
|---|---|---|---|
| **Public repo** (any plan) | ✅ Unlimited** | ✅ Unlimited** | 60 req/hour |
| **Free private repo** | 500 MB total | 1 GB/month | 60 req/hour |
| **Team private repo** | 2 GB total | 2 GB/month * | 5,000 req/hour |
| **Enterprise private repo** | 50 GB total | 50 GB/month * | 5,000 req/hour |

\* Bandwidth limit can be raised by contacting GitHub support.
\** Public repos have no hard cap but GitHub's abuse detection may apply.

**For an open-source plugin marketplace on a public repo: everything is free with no meaningful limits.** JAR files are small (tens of KB to a few MB each), so bandwidth is negligible.

#### The catches (read these before committing)

| Catch | Impact | Mitigation |
|---|---|---|
| **API rate limiting** | Unauthenticated requests to the Releases API are limited to **60 requests per hour**. If every IDE install fetches the release list every time the user opens the Plugin Manager, you'll hit this limit fast. | **Option A**: Have the IDE cache the release data for 5-10 minutes. **Option B**: Use an authenticated request with a read-only token (5,000 req/hour). **Option C**: Don't use the Releases API at all for listing — keep `registry.json` as the source of truth, and only use the Releases API to fetch download counts for individual plugins on-demand (e.g., when the user clicks on a plugin to see details). |
| **Download counts reset per release** | If you publish `ocr-reader v1.0.0` and it gets 500 downloads, then publish `v1.1.0`, the new release starts at 0. The total across all versions isn't automatically summed — you'd sum them client-side. | Easy fix: the IDE sums `assets[].download_count` across all releases for the same plugin name. |
| **No per-asset download tracking for ZIPs** | If you upload a single `ocr-reader.zip` containing both the plugin JAR and libs, GitHub counts downloads of the zip, not individual files inside it. | Upload each JAR as a separate asset, or just use the zip download count as the plugin download count. |
| **Release management overhead** | Each plugin version needs a Git tag + release created. The PR pipeline would automate this, but it's more ceremony than just committing a JAR to a folder. | This is fine — the pipeline creates the release. Plugin authors just submit a PR. |
| **No ratings / reviews** | Releases API doesn't support this (as discussed). | Redirect to GitHub Discussions. |

#### The verdict for your use case

| Consideration | Verdict |
|---|---|
| **Free?** | ✅ Yes, for public repos |
| **Download counts?** | ✅ Yes, from the API |
| **Easy to implement?** | ✅ Yes, ~10 lines of Java |
| **Rate limit a problem?** | ⚠️ Only if unauthenticated. Use a token or cache the data. |
| **Better than JARs in a Git repo?** | ✅ Yes — you get download numbers for free |

**Recommendation**: Keep `registry.json` as the plugin catalog (it's fast and simple). Add a `downloadUrl` that points to a GitHub Release asset. Fetch individual download counts from the Releases API on-demand (when the user views plugin details). This avoids rate limits entirely since you're making one API call at a time, not fetching the full release list.

#### Tier 2: Minimal Backend (Cloudflare Worker + KV)

Deploy a **single Cloudflare Worker** (free tier) with a KV store. It serves:
- `GET /registry` — returns the plugin list (can proxy the static JSON or enhance it with live data)
- `POST /plugins/{name}/download` — increments and returns the download count
- `GET /plugins/{name}/stats` — returns download count, average rating
- `POST /plugins/{name}/rate` — submits a 1-5 star rating (simple body, no auth needed or optional GitHub login)

Total code: ~200 lines of JavaScript. No server to manage. Free tier handles thousands of requests.

#### Tier 3: Full Platform (Web App + Database)

A proper web app (Next.js, Flask, etc.) with a database. This is what you'd build if the community takes off — but it's **months of work**, not weeks.

### How to Build a Community-Feeling Marketplace Without a Backend

You can make the marketplace feel interactive and community-driven using **only read-only data** from GitHub. Ratings and download counters require write endpoints, but there's a lot you can do without them.

#### Free features (zero backend, all read-only)

| Feature | How | Feels like |
|---|---|---|
| **GitHub Stars** | `GET /repos/author/plugin-repo` — returns star count. Display a star icon + count in the table. | "This is popular" |
| **Last updated** | From the `registry.json` entry or the plugin's repo commit date. Show "Updated 3 weeks ago". | "Actively maintained" |
| **Featured / curated** | A `"featured": true` flag in `registry.json` set by the maintainer. Filter or highlight. | "Staff picks" |
| **"What's New" button** | The `releaseNotes` field from the registry. Show in a popup. | "This plugin has a changelog" |
| **Report Issue / Contribute** | Two buttons linking to the plugin's GitHub issues page and source repo. Just URLs. | "Open source, transparent" |
| **README preview** | Fetch `raw.githubusercontent.com/author/repo/main/README.md` and render it in a side panel when the user selects a plugin. | "Full documentation at a glance" |
| **Compatibility badge** | `minEngineVersion` vs the user's engine version: green checkmark (compatible), yellow warning (untested on newer engine), red (too old). All static data. | "Works with my version" |
| **Plugin search** | Filter the in-memory `registry.json` by name, description, author, or object type. | "Found exactly what I need" |
| **Action preview** | Show the list of `actions` from the registry entry. The user sees "This plugin adds: `readImageText`, `captureScreenshotRegion`, `compareImageText`". | "I know what I'm getting" |
| **Sort by recently added** | `registry.json` has a `dateAdded` field. Client-side sort. | "New stuff is happening" |
| **"From the same author"** | Client-side filter: show other plugins with the same `author` field. | "This author makes good plugins" |
| **GitHub Discussions link** | A button that opens the plugin's GitHub Discussions page. | "There's a community around this" |
| **Install count from GitHub Releases** | If plugins are published as GitHub Releases: `GET /repos/ing-bank/ingenious-plugins/releases` returns `assets[].download_count`. | "10,000 downloads" |

#### The only things you can't do without a backend

| Feature | Why | Alternative |
|---|---|---|
| User ratings (1-5 stars) | Needs a database to store votes | Skip it. Redirect to GitHub Discussions for feedback instead. |
| User reviews / comments | Needs a database + auth | "Leave feedback on GitHub" button |
| "Most downloaded this week" | Needs time-series counter | "Most downloaded all time" via GitHub Releases API totals |
| Per-user "favorites" list | Needs auth + per-user storage | Local storage in the IDE (client-side bookmarking) |

#### The actual recommendation

| Phase | Community Features | Effort |
|---|---|---|
| **Week 3-5** (on day 1 of the Plugin Manager) | GitHub stars, last updated, featured flag, action preview, compatibility badge, plugin search, sort by date | **Zero backend work** — all client-side from registry.json + GitHub API |
| **Week 5-6** (8-10 extra lines of code) | Download counts via GitHub Releases API | **1-2 days** (switch to releasing plugins as GitHub Releases, fetch with a single extra API call) |
| **Post-launch** (if community demands it) | Cloudflare Worker for ratings (optional) | ~1 week |

**You can ship 90% of a community-feeling marketplace without writing a single line of backend code.** The IDE fetches data from GitHub (read-only APIs) and computes everything locally. Stars, compatibility, search, featured curation, action lists, and download counts make it feel alive. Skip ratings entirely — GitHub Discussions is a better home for that conversation anyway.

---

## Phased Rollout: Open-Source First, Then Company

Since the company is migrating to GitHub soon, there's a clean path:

### Phase 1 (Weeks 1-6) — Open-Source on GitHub

Build everything targeting the **open-source INGenious project**. The registry lives in a public GitHub repo (`ing-bank/ingenious-plugins`). The IDE fetches from `raw.githubusercontent.com`. No auth needed — public repos are freely readable.

This gives you:
- A working MVP you can ship to the open-source community
- No company-specific blockers to deal with (no Azure DevOps, no permissions, no PAT)
- Real usage feedback before the company migration
- Everything already works when the company moves to GitHub

### Phase 2 (After GitHub Migration — ~1 week of work)

Once the company is on GitHub:
1. Create a **private GitHub repo** inside the company org for the internal registry
2. Change the registry URL in the company's build config
3. Option A: make the private repo readable to all employees (best — no auth)
4. Option B: use a fine-grained GitHub access token (repo read-only) stored in env variable or encrypted settings

**The IDE codebase doesn't change** — it's the same `PluginManagerService` fetching JSON from GitHub. Only the URL differs.

### What to build in Phase 1

- Public GitHub repo: `ing-bank/ingenious-plugins` with `registry.json` + plugin folders
- GitHub Actions PR pipeline for validation (instead of Azure DevOps)
- Plugin Manager tab in the IDE (same design, targets `raw.githubusercontent.com`)
- Manual "Import from File" fallback (backup #1)
- Configurable registry URL from the start (so Phase 2 just changes the URL)

### What you skip until Phase 2

- Company auth (PAT, env vars, encryption) — not needed for public repos
- Azure DevOps pipeline — use GitHub Actions instead
- Network share fallback — not needed if GitHub is reachable

### Why this is the right call

| Concern | How this addresses it |
|---|---|
| Company environment restrictions | **Avoid them entirely** in Phase 1. By the time the company migrates, you know the approach works. |
| Open-source alignment | INGenious is MIT-licensed. The plugin marketplace ships as part of the open-source project from day one. |
| 2-month timeline | Phase 1 (open-source) fits easily in 8 weeks. Phase 2 (company) is a config change + one week of wrap-up. |
| GitHub migration | You're building for the target platform from the start. |

---

## Recommendation

| Decision | Recommendation |
|---|---|
| **Timeline** | Phase 1 (open-source): weeks 1-6. Phase 2 (company): after GitHub migration (~1 week). |
| **Backend** | Public GitHub repo `ing-bank/ingenious-plugins`. GitHub Actions for PR validation. |
| **Auth** | None needed (public repo). If company wants a private repo later, use fine-grained token. |
| **IDE registry URL** | Configurable in `Configuration/app.settings`. Default: `https://raw.githubusercontent.com/ing-bank/ingenious-plugins/main/registry.json`. |
| **Backups** | Manual Import from File if GitHub is unreachable. Bundled plugins if all remote is blocked. |

## Other Things to Consider

### Plugin Author Workflow

Plugin authors need a clear path from idea → published plugin. Consider:

- **Maven archetype / template repo**: A GitHub template repo (`ing-bank/ingenious-plugin-template`) with the boilerplate: a sample `@Action` class, a `pom.xml` with the right dependencies, and a build script that packages the JAR. Lowers the barrier significantly.
- **Contribution guide**: A `CONTRIBUTING.md` in the plugins repo explaining: how to structure a plugin, what the `pluginEntryClasses` manifest attribute should contain, how to test locally, and how to submit a PR.
- **Breaking changes policy**: When the engine API changes, old plugins may break. The `minEngineVersion` / `maxEngineVersion` fields handle this at the registry level, but you need a process: document API changes in the main INGenious changelog, and give plugin authors a deprecation window.

### Security

- **No runtime sandbox**: A plugin JAR has full access to the JVM — the same as the engine. There's no way to restrict what a plugin can do (no class-level permissions, no security manager by default). Mitigation: all plugins go through PR review, and the pipeline checks `manifest.mf` and scans for `@Action` duplicates but doesn't audit code.
- **Code signing (future)**: If security becomes a concern, plugin authors could sign their JARs. The IDE would verify the signature against a trusted keychain before installing. This is a Phase 2+ feature.
- **Supply chain risk**: Dependencies in a plugin's `lib/` folder aren't scanned for vulnerabilities. The PR pipeline could add a `trivy` or `snyk` scan step.

### Documentation Expectations

The `registry.json` has a `homepageUrl` field. Authors should link to a README in their own repo covering: what the plugin does, what actions it adds, any setup steps (e.g., "install Tesseract OCR first"), and example test steps.

### Plugin Removal / Deprecation

- Add a `"deprecated": true` flag to the registry schema. When set, the IDE shows a warning banner on the plugin's row but doesn't auto-uninstall — the user can still use it, but knows it's no longer maintained.
- For security issues: remove the entry from `registry.json` entirely. The IDE won't show it in Browse. Already-installed copies still work (no forced removal), but an update check won't find new versions.

### Plugin Metadata in the JAR Itself

Currently `PluginLoader` reads `pluginEntryClasses` from the JAR manifest. Consider adding more optional manifest attributes that the IDE could display:

```
Plugin-Name: OCR Reader
Plugin-Version: 1.1.0
Plugin-Description: Extract text from images using Tesseract OCR  
Plugin-Author: Team Alpha
Plugin-Min-Engine-Version: 3.1.0
```

These would be read by `PluginMetadata.java` and shown in the Installed tab — even if the plugin was installed manually (no registry entry). Currently, manually installed plugins have no metadata in the UI.

### Testing Strategy

| What to test | How |
|---|---|
| Registry fetch succeeds | Mock HTTP endpoint, verify PluginManagerService parses JSON correctly |
| Registry fetch fails (network down) | Verify graceful error message, Browse tab shows "Unable to load registry" |
| Install downloads + extracts JAR | Verify JAR and lib/ end up in correct `Resources/plugins/<name>/` path |
| Install with conflict | Mock conflicting action, verify warning dialog appears and install is blocked |
| Install with version incompatibility | Mock old engine version, verify warning dialog |
| Uninstall removes files | Verify plugin folder is deleted |
| Import from File | Same as install but with local file picker instead of HTTP download |
| Restart banner | Verify restart prompt appears after install/uninstall |

### What to NOT build in the first 2 months

- **User accounts / login** — adds auth complexity with zero community benefit early on
- **Automated plugin testing** — running a plugin's test suite against the engine on every PR is nice but complex
- **In-app plugin creation wizard** — a separate feature, not a marketplace feature
- **Dependency graph / transitive deps** — plugins with complex `lib/` trees could conflict. Handle it by convention (bundle what you need) for now
- **Plugin analytics dashboard** — "how many people use my plugin?" is a Phase 3 feature

---

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| `dev.azure.com` blocked on some machines | Fallback to network share or Import from File (Backup #1, #4) |
| Plugin breaks engine | Class loader isolation already handles this (child-first) |
| Duplicate action names | Pre-install scan detects conflicts; warn with option to cancel |
| JAR contains malicious code | No runtime sandbox (beyond scope); rely on PR pipeline + code review |
| 2 months insufficient | Scope cut: skip auto-update, skip rating/reviews, skip search — just browse + install |
| Company blocks all remote | Fallback to bundled plugins shipped with the distribution (Backup #5) |
