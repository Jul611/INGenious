# Plugin Marketplace — Implementation Status Slides

> **Target audience:** Stakeholders / team
> **Duration:** 10 minutes (10 slides)

---

## Slide 1: Title Slide

**Text:**
```
Plugin Marketplace for INGenious IDE
Implementation Status — July 2026

Browse · Install · Publish · Manage
```

**Speaker notes:**
"The plugin marketplace is now built and functional. Here's what was implemented and how it works."

---

## Slide 2: What's Implemented

**Text:**
```
✅ Browse tab — fetches plugin catalog from GitHub
✅ Install tab — one-click download and installation
✅ Installed tab — manage installed plugins
✅ Publish tab — two ways to publish:
   • Direct: one-click release to GitHub
   • Export: files ready for a PR
✅ Profile dialog — single PAT for read + write
✅ Engine loader — loads plugins on startup
✅ 3 sample plugins — helloworld, screenshots, calc-math
```

**Visual idea:** 8 checkmark items in two columns.

---

## Slide 3: Browse & Install Flow

**Text:**
```
Browse tab:
• Fetches registry.json from private GitHub repo
• Uses GitHub Contents API (live data)
• Shows plugin name, version, actions, author
• "Install" button downloads the JAR

Install tab:
• Downloads from GitHub Release assets
• Private repo: resolves via API with PAT auth
• Creates .plugininfo metadata file
• Uninstall removes plugin directory
```

**Speaker notes:**
"Users browse available plugins fetched live from GitHub. Click Install to download the JAR into the plugins directory. Uninstall removes it cleanly."

**Visual idea:** Screenshot of Browse tab showing plugin table.

---

## Slide 4: Publish — Direct (New)

**Text:**
```
"Publish Directly to Registry" creates a release in one click:

1. Select a built JAR
2. Auto-extracts manifest metadata + @Action methods
3. Fill in registry details (name, version, description)
4. Write README (min 50 words)
5. Click "Publish Directly"

Behind the scenes:
• POST /repos/.../releases — creates a GitHub Release
• POST /releases/assets — uploads the JAR
• PUT /contents/registry.json — updates the catalog
```

**Speaker notes:**
"The new direct publish flow is the main addition. Users can publish a plugin from the IDE in one click — no git commands, no PRs. The IDE creates a GitHub Release, uploads the JAR, and updates the registry automatically."

**Visual idea:** Flow diagram showing JAR → IDE → GitHub Release + registry.json.

---

## Slide 5: Publish — Export for PR

**Text:**
```
"Export Submission for PR" prepares files for manual review:

1-4. Same as Direct Publish
5. Click "Export Submission for PR"
6. Exports: plugins/<name>/<jar> + README.md + registry.json
7. Clone repo → copy files → commit → push → open PR
8. CI validates and creates the release on merge

Use case: when you want code review before publishing
```

**Speaker notes:**
"For teams that want code review before anything goes live, the export flow is the right choice. It produces the same output as the direct flow but goes through a PR with automated validation."

---

## Slide 6: Profile & Authentication

**Text:**
```
Single PAT for everything:

Profile dialog:
┌─────────────────────────────────┐
│ PCode:    [______________]      │
│ User Code:[______________]      │
│ Publishing PAT: [______]        │
│ How to create a PAT? ────────── │
│ Required: Contents: Read & Write │
│ You must be a collaborator       │
└─────────────────────────────────┘

PAT used for:
• Reading the registry (Browse tab)
• Downloading JARs (Install tab)
• Publishing releases (Publish tab)
```

**Speaker notes:**
"Users configure their GitHub PAT once in the profile dialog. The same token handles both reading the registry and publishing plugins. It's stored in plain text in a gitignored file — no encryption overhead, no key mismatch issues."

**Visual idea:** Mockup of the profile dialog window.

---

## Slide 7: Security & Validation

**Text:**
```
Pre-install checks:
• Duplicate action name detection
• Engine version compatibility (minEngineVersion)

Publish-time checks:
• PAT validation before publishing
• Conflict detection against live registry
• README word count (min 50)
• Required manifest fields (pluginEntryClasses)

GitHub CI pipeline (on PR/merge):
• JSON validity, duplicate actions
• Manifest presence, README quality
• Suspicious import scanning
• Trivy security scan (HIGH/CRITICAL)
```

**Speaker notes:**
"Multiple safety layers: the IDE checks before publishing, the CI pipeline checks before merging. Duplicate action names are caught early. Security scanning runs on every PR."

**Visual idea:** Three-layer shield: IDE checks → PR checks → merge checks.

---

## Slide 8: Token Flow Architecture

**Text:**
```
User sets PAT in Profile dialog
        │
        ▼
  Configuration/user-config.json
  (gitignored, per-user, plain text)
        │
        ▼
  UserConfig.getPublishPat()
        │
        ├── getRegistryToken()
        │   → Registry fetch & download
        │
        └── publishDirectly()
            → Release creation + registry update

No env vars, no app.settings, no encryption.
```

**Speaker notes:**
"The token architecture is dead simple. One PAT, configured once in the UI, saved to a gitignored local file. Read fresh on every request — no stale cache bugs. The same PAT we used to encrypt and fail to decrypt is now stored in plain text and works every time."

---

## Slide 9: Demo Walkthrough

**Text:**
```
1. Launch IDE → Dist\release\ingenious.bat
2. Set PAT → Profile icon → paste token → OK
3. Browse → Plugins → Browse tab → plugins from GitHub
4. Publish → Publish tab → Browse for JAR
   → plugin-examples/helloworld/target/demo-helloworld-1.0.0.jar
5. Bump version → change 1.0.0 → 1.0.1
6. Publish Directly → progress dialog → success
7. Check GitHub → new release + updated registry.json
```

**Speaker notes:**
"Let me walk through the full flow. Build the IDE, set a PAT, browse available plugins, and publish a new version in under a minute."

**Visual idea:** Minimal slide — the live demo does the work.

---

## Slide 10: What's Next

**Text:**
```
Built (ship ready):
• Browse, Install, Installed, Publish tabs
• Direct publish + Export for PR
• Profile/PAT management
• Engine plugin loading
• 3 sample plugins

Future:
• GitHub OAuth (no manual PAT)
• Plugin template repo / scaffolder
• Auto-updates for installed plugins
• Source-based publishing
• Download analytics
```

**Speaker notes:**
"What's built is ready to ship. The future work is about making it even easier — OAuth so users don't need to create tokens manually, a plugin template repo so authors can start in 5 minutes, auto-updates so users always have the latest version."

**Visual idea:** Two columns: green checkmarks for built, grey dimmed for future.
