# Plugin Marketplace — Next Steps

> Pick up where you left off. Current branch: `initiative-repo`.

---

## State of Play

| Area | Status | Notes |
|---|---|---|
| Browse tab | ✅ Done | Fetches from GitHub (Contents API), shows table, Install button |
| Installed tab | ✅ Done | Lists local plugins from `plugins/`, Uninstall button |
| Publish tab | ✅ Done | **Submission wizard**: select JAR, write README, conflict check, export for PR |
| Conflict detection | ✅ Done | Scans actions before install + before export |
| Version check | ✅ Done | minEngineVersion vs About.getBuildVersion() |
| Engine PluginLoader | ✅ Done | Scans `plugins/` and `{appRoot}/plugins/` |
| PluginMetadata | ✅ Done | Reads JAR manifest + .plugininfo |
| Toolbar + slide | ✅ Done | "Plugins" button in FXToolBar, registered in AppMainFrame |
| Sample plugins | ✅ Done | helloworld, screenshots, calc-math (3 examples) |
| **Private registry repo** | ✅ Done | `Jul611/ingenious-plugins-trial` — private repo with GitHub Releases |
| **Registry fetch** | ✅ Done | GitHub Contents API (`api.github.com`) — live data, no CDN cache |
| **Release downloads** | ✅ Done | Resolved via `/releases/tags/{tag}` → `/releases/assets/{id}` with `Accept: octet-stream` |
| **Auth (PAT)** | ✅ Done | `pluginRegistryToken` in `Resources/Configuration/app.settings` (survives builds); env var `INGENIOUS_PLUGIN_TOKEN` supported |
| **Placeholder JAR bug** | ✅ **FIXED** | `downloadFile()` throws `IOException` on non-200 — no more empty zips |
| **Configurable registry URL** | ✅ Done | Env var `INGENIOUS_PLUGIN_REGISTRY_URL` or hardcoded default |
| **CI Pipeline** | ✅ Done | `.github/workflows/publish-plugin.yml` — validates + auto-creates releases on merge |
| **Publishing (PR + CI)** | ✅ Done | `prepareSubmission()` exports JAR+README+registry, author opens PR, CI creates release |
| **README required** | ✅ Done | Publish tab has built-in README editor (min 50 words), pipeline enforces it |
| **Dead code removed** | ✅ Done | `publishPluginToGitHub()`, `pushToGithub()`, `getExistingSha()` removed |

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
  └── prepareSubmission()
        └── Exports: plugins/<name>/<jar> + README.md + updated registry.json

GitHub Actions (on merge to main)
  │
  ├── Validate:
  │     ├── registry.json is valid JSON
  │     ├── No duplicate action names
  │     ├── All downloadUrls resolve
  │     ├── Every JAR has pluginEntryClasses in manifest
  │     ├── README.md exists (min 50 words)
  │     └── Trivy security scan (HIGH/CRITICAL)
  │
  └── Publish:
        ├── gh release create <plugin>-v<version> <jar>
        └── Updates registry.json downloadUrl to release asset URL
```

**Install location:** `Dist/release/plugins/<name>/` (relative to IDE working directory)

**Token source (priority order):**
1. `INGENIOUS_PLUGIN_TOKEN` env var (production)
2. `pluginRegistryToken` in `Configuration/app.settings` (MVP)
3. Null — unauthenticated (works for public repos)

**Registry URL (priority order):**
1. `INGENIOUS_PLUGIN_REGISTRY_URL` env var
2. Hardcoded default: `api.github.com/repos/Jul611/ingenious-plugins-trial/contents/registry.json`

---

## Publishing Workflow (for authors)

### 1. Build your plugin JAR
```
mvn package -pl plugin-examples/your-plugin
```

### 2. Use the IDE Publish tab
- Select the JAR → auto-extracts manifest metadata + @Action methods
- Fill in registry metadata (name, version, description, etc.)
- Write README (template provided, min 50 words)
- Click "Export Submission for PR"

### 3. Submit via GitHub PR
```
git clone https://github.com/Jul611/ingenious-plugins-trial.git
# Copy the exported plugins/ and registry.json into the cloned repo
git checkout -b add-your-plugin
git add .
git commit -m "Add your-plugin v1.0.0"
git push -u origin add-your-plugin
# Open PR on GitHub
```

### 4. CI runs automatically
- Validates JAR manifest, README, action conflicts, security scan
- On merge to main: creates GitHub Release, uploads JAR, updates registry.json

---

## Pipeline Security Checks

### Tier 1: Blocking (must pass)
| Check | How |
|---|---|
| `registry.json` is valid JSON | `python3 -c "json.load()"` |
| No duplicate `@Action` names | Parse all actions across registry |
| Every JAR has `pluginEntryClasses` | `unzip -p *.jar META-INF/MANIFEST.MF \| grep` |
| `README.md` ≥ 50 words | `wc -w` |
| Trivy security scan | `aquasecurity/trivy-action` (HIGH, CRITICAL) |

### Tier 2: Warning (flags for review)
| Check | How |
|---|---|
| Suspicious imports (`Runtime.exec`, `ProcessBuilder`, `ClassLoader`) | `unzip -l *.jar \| grep` |
| All `downloadUrl` fields resolve | `curl -I` head request |

### Tier 3: Future
| Check | When |
|---|---|
| Code signing verification | Phase 2 |
| Sandbox analysis (monitor syscalls) | Phase 3 |
| VirusTotal hash submission | Could add now (free API) |

---

## Remaining by Week

### Week 4: Engine Integration & Testing (current week)

| Task | Status |
|---|---|
| Test: install plugin → restart → actions appear in Step Builder | ❌ |
| Test: uninstall → restart → actions removed | ❌ |
| Test: conflict detection during install | ❌ |
| Test: version incompatibility warnings | ❌ |
| Test: GitHub API fetch → fallback to local when offline | ❌ |
| Test: publish flow end-to-end (export → PR → CI → install) | ❌ |

### Week 5: Documentation & Polish

| Task | Status |
|---|---|
| Plugin author README in template repo | ❌ |
| Internal team docs: how to install, how IT bundles | ❌ |
| Move `.github/workflows/publish-plugin.yml` to plugins repo (it's in main repo now for reference) | ❌ |
| UI polish (error messages, loading states, edge cases) | ❌ |
| Loading spinner during fetch/install | ❌ |
| Final `mvn compile -pl IDE -am` green build | ✅ Done |

### Week 6: Stakeholder Demo & Handover

| Task | Status |
|---|---|
| Finalize proposal with screenshots | ❌ |
| Slide deck (5-10 slides) | ❌ |
| Stakeholder demo | ❌ |
| Handover doc | ❌ |
| Clean branches, remove dead code, final commit | ❌ |

### Backlog (from plan.md Week 1-3)

| Task | Priority | Notes |
|---|---|---|
| ~~Create `Jul611/ingenious-plugins` separate repo~~ | ~~Medium~~ ✅ Done | `ingenious-plugins-trial` |
| ~~Switch registry fetch to GitHub Contents API~~ | ~~Medium~~ ✅ Done | With base64 decode + auth |
| ~~Make registry URL configurable~~ | ~~Medium~~ ✅ Done | Via env var |
| ~~Publish tab rewrite as submission wizard~~ | ~~Medium~~ ✅ Done | README editor, conflict check, export for PR |
| ~~Remove dead publishing code~~ | ~~Medium~~ ✅ Done | `publishPluginToGitHub()`, `pushToGithub()`, `getExistingSha()` gone |
| ~~Create CI pipeline for validation + publishing~~ | ~~Medium~~ ✅ Done | `.github/workflows/publish-plugin.yml` |
| Create GitHub template repo `ingenious-plugin-template` | Low — Week 2 scope |
| Write `CONTRIBUTING.md` for plugins repo | Low — Week 2 scope |
| Move pipeline to plugins repo | Medium — Pipeline currently in main repo for reference |
| Add `readmeUrl` to `registry.json` schema | Medium — Data model has it, registry entries need the field |
| **PAT → OAuth device flow** | **Future** | Replace shared PAT with per-user GitHub OAuth |

---

## How to Test End-to-End

```bash
# 1. Build the IDE
mvn compile -pl IDE -am

# 2. Ensure PAT is set (survives builds — in Resources/Configuration/)
#    Check: Resources/Configuration/app.settings → pluginRegistryToken=<pat>

# 3. Launch from Dist/release/
#    Run: IDE/src/main/java/com/ing/ide/main/Main.java

# 4. Click "Plugins" in toolbar

# 5. Browse tab loads plugins from PRIVATE GitHub repo (authenticated)
#    → Shows "Hello World Demo" and "Screenshot Tools"

# 6. Click Install on "Hello World Demo"
#    → Downloads from GitHub Release API → saves to plugins/demo-helloworld/
#    → Verify: Dist/release/plugins/demo-helloworld/demo-helloworld-1.0.0.jar exists

# 7. Switch to Installed tab
#    → Shows the plugin with version and actions

# 8. Restart the IDE
#    → Open Step Builder → action picker should show plugin actions

# 9. Uninstall
#    → Plugin disappears from Installed tab
#    → Restart → actions should be gone
```

## Key Files to Know

| File | Path | Notes |
|---|---|---|
| PluginManagerService | `IDE/.../pluginmanager/PluginManagerService.java` | Core: registry fetch, install, auth, release resolution, submission export |
| PluginManagerBrowseUI | `IDE/.../pluginmanager/PluginManagerBrowseUI.java` | Browse tab UI |
| PluginManagerInstalledUI | `IDE/.../pluginmanager/PluginManagerInstalledUI.java` | Installed tab UI |
| PluginManagerPublishUI | `IDE/.../pluginmanager/PluginManagerPublishUI.java` | Submission wizard with README editor |
| PluginManager | `IDE/.../pluginmanager/PluginManager.java` | Tab container |
| PluginLoader | `Engine/.../plugin/loader/PluginLoader.java` | Scans `plugins/` on startup |
| PluginRegistryEntry | `IDE/.../pluginmanager/PluginRegistryEntry.java` | Data model (includes `readmeUrl`) |
| AppSettings | `IDE/.../settings/AppSettings.java` | `pluginRegistryToken` setting |
| Registry (source) | `Resources/Configuration/app.settings` | PAT lives here, copied to Dist on build |
| Registry JSON | `Dist/release/plugins/registry.json` | Local fallback catalog |
| CI Pipeline | `.github/workflows/publish-plugin.yml` | Validation + auto-publish (needs to be moved to plugins repo) |
| Private repo | `Jul611/ingenious-plugins-trial` | Registry + GitHub Releases |

## Files Changed in This Session

| File | Change |
|---|---|
| `.gitignore` | Added `Configuration/app.settings`, `Resources/Configuration/app.settings` |
| `IDE/.../settings/AppSettings.java` | Added `PLUGIN_REGISTRY_TOKEN` enum |
| `IDE/.../pluginmanager/PluginManagerService.java` | Token loading, auth headers, Contents API fetch with base64 decode, `resolveReleaseAssetUrl()`, manual redirect handling, placeholder fix, install path → `plugins/`, `prepareSubmission()`, `getPluginsRepoUrl()`, removed dead publishing code |
| `IDE/.../pluginmanager/PluginManagerPublishUI.java` | Complete rewrite: submission wizard with 4-step flow, README editor, conflict check, export for PR |
| `IDE/.../pluginmanager/PluginRegistryEntry.java` | Added `readmeUrl` field + getter/setter |
| `Resources/plugins/registry.json` | `downloadUrl` → GitHub Release asset URLs |
| `Dist/release/plugins/registry.json` | Same update |
| `Resources/Configuration/app.settings` | **New** — PAT stored here (survives builds, gitignored) |
| `Resources/plugins/demo-helloworld/*.jar` | Deleted — served from releases |
| `Resources/plugins/screenshots/*.jar` | Deleted — served from releases |
| `.github/workflows/publish-plugin.yml` | **New** — CI pipeline for validation + auto-publish |
