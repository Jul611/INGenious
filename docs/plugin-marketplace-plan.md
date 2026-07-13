# Plugin Marketplace — Architecture & Implementation Plan

> **Timeline:** 6 weeks
> **Constraint:** No budget, no licenses, no IT approval needed

---

## What We Already Have (Week 0 — starting point)

| Feature | Status |
|---|---|
| Browse tab — fetches registry from GitHub, shows plugin table with Install buttons | ✅ Done |
| Installed tab — lists local plugins, Uninstall button, Check for Updates | ✅ Done |
| Publish tab — select JAR, auto-read manifest, extract @Action methods, edit metadata, stage locally | ✅ Done |
| Conflict detection — warns if actions collide with installed plugins | ✅ Done |
| Version compatibility check — minEngineVersion vs engine version | ✅ Done |
| Engine PluginLoader — scans `Resources/plugins/` and `{appRoot}/plugins/` for JARs | ✅ Done |
| PluginMetadata — reads `Plugin-Name`, `Plugin-Version`, `Plugin-Author` from JAR manifests | ✅ Done |
| PluginClassLoader — child-first loading with parent-first for engine/API/JDK classes | ✅ Done |
| Sample plugins — helloworld, screenshots, calc-math (3 examples) | ✅ Done |
| Integration — toolbar button, slide registration, action listener | ✅ Done |

---

## Architecture

```
┌──────────────────────────────────────────────────┐
│                 INGenious IDE                     │
│                                                   │
│  ┌──────────────────────────────────────────┐     │
│  │         Plugin Manager (3 tabs)           │     │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐  │     │
│  │  │ Browse    │ │ Installed│ │ Publish   │  │     │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘  │     │
│  │       │             │            │         │     │
│  │  ┌────┴─────────────┴────────────┴──┐     │     │
│  │  │     PluginManagerService         │     │     │
│  │  └────────────┬────────────────────┘     │     │
│  └───────────────┼──────────────────────────┘     │
│                  │ HTTPS / file://                │
└──────────────────┼────────────────────────────────┘
                   │
┌──────────────────┼────────────────────────────────┐
│  GitHub repo: ingenious-plugins                   │
│  ├── registry.json (plugin catalog)               │
│  └── plugins/<name>/<name>.jar                    │
│                                                    │
│  Fetch: api.github.com (live, no cache)            │
│  Download: raw.githubusercontent.com (CDN)         │
│  Offline: file://share or bundled in installer     │
└────────────────────────────────────────────────────┘
```

---

## 6-Week Plan

### Week 1: Registry Infrastructure

| Day | Task | Deliverable |
|---|---|---|
| Mon | Create `Jul611/ingenious-plugins` repo on GitHub | Repo exists |
| Mon | Move `Resources/plugins/registry.json` → repo root, plugins/ → repo `plugins/` dir | Registry lives in new repo |
| Tue | Change `REMOTE_REGISTRY_URL` from raw to GitHub Contents API | Live data, no 5-min cache |
| Tue | Add base64 decoding for API response | Fetch works end-to-end |
| Wed | Make registry URL configurable from `Configuration/app.settings` | IT can change URL without rebuild |
| Wed | Simplify Publish tab — remove PAT field, show git commands after publish | Cleaner author workflow |
| Thu | Remove `publishPluginToGitHub()` / `pushToGithub()` / `getExistingSha()` from service | Dead code gone |
| Fri | Build + test full flow end-to-end | Everything compiles + works |

### Week 2: Plugin Author Experience

| Day | Task | Deliverable |
|---|---|---|
| Mon | Create GitHub template repo `ingenious-plugin-template` | One-click "Use this template" |
| Mon | Template has: sample `@Action` class, correct `pom.xml` with manifest entries, `lib/ingenious-api-3.0.jar` | Plugin author starts in 5 minutes |
| Tue | Write `CONTRIBUTING.md` for `ingenious-plugins` repo | Clear submission guide |
| Tue | Write `scripts/publish_plugin.py` CLI tool (optional, for CI) | Automates JAR → registry |
| Wed | Test: build a fresh plugin from template → publish in IDE → commit → Browse shows it | Full author flow validated |
| Thu | Polish Publish tab UI based on feedback | UX cleanup |
| Fri | Buffer / catch-up day | — |

### Week 3: GitHub Actions & CI (Pipeline runs on plugin upload)

**Two pipeline triggers:**
1. **On Pull Request** (pre-merge validation) — runs when someone submits a new plugin
2. **On Push to `main`** (post-merge) — runs after merge to update any derived artifacts

| Day | Task | Deliverable |
|---|---|---|
| Mon | Create `.github/workflows/validate-plugin.yml` — runs on PR and push to main | Pipeline exists |
| Mon | **PR check: registry.json is valid JSON** | Prevents broken registry |
| Mon | **PR check: every referenced JAR exists** (downloadUrl points to a real file in `plugins/`) | Prevents 404s in IDE |
| Tue | **PR check: every JAR manifest has `pluginEntryClasses`** | Prevents unloadable plugins |
| Tue | **PR check: no duplicate plugin names** | Prevents name collisions |
| Tue | **PR check: no duplicate action names across all plugins** | Prevents @Action conflicts |
| Wed | **PR check: `plugins/` directory structure matches registry.json** | Ensures consistency |
| Wed | Add `trivy` (free) security scan on the JAR | Supply chain vulnerability check |
| Thu | **Push-to-main trigger: re-generate `registry.json` with sorted entries** | Auto-formats the registry |
| Thu | Push a plugin PR end-to-end (branch → PR → merge → IDE fetches updated registry) | Demo-ready pipeline |
| Fri | Buffer / catch-up day | — |

### Pipeline YAML structure (to be written)

```yaml
name: Validate Plugin Submission
on:
  pull_request:
    paths:
      - 'plugins/**'
      - 'registry.json'
  push:
    branches: [main]
    paths:
      - 'plugins/**'
      - 'registry.json'

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Validate registry.json
        run: python3 -c "import json; json.load(open('registry.json'))"
      
      - name: Check JAR files exist
        run: |
          python3 scripts/publish_plugin.py --ci --check-only
      
      - name: Check plugin manifest entries
        run: |
          python3 scripts/check_plugin_manifest.py plugins/**/*.jar
      
      - name: Check for duplicate actions
        run: |
          python3 scripts/check_duplicate_actions.py registry.json
      
      - name: Security scan (Trivy)
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: 'plugins/'
      
      - name: Sort registry.json (on push to main)
        if: github.event_name == 'push'
        run: python3 scripts/sort_registry.py registry.json
      
      - name: Commit sorted registry (on push to main)
        if: github.event_name == 'push'
        run: |
          git config user.name "ingenious-bot"
          git config user.email "bot@ingenious.dev"
          git add registry.json
          git diff --quiet && git diff --staged --quiet || git commit -m "Auto-format registry.json"
          git push
```

### Week 4: Engine Integration & Testing

| Day | Task | Deliverable |
|---|---|---|
| Mon | Test: install plugin → restart → actions appear in Step Builder | Core integration verified |
| Mon | Test: uninstall → restart → actions removed | Clean removal verified |
| Tue | Test: conflict detection during install | Safety verified |
| Tue | Test: version incompatibility warnings | Compatibility verified |
| Wed | Test: GitHub API fetch → fallback to local when offline | Network resilience verified |
| Wed | Test: file:// registry URL (network share scenario) | Offline mode verified |
| Thu | Test: publish → Browse shows plugin (local + remote merge) | Publish flow verified |
| Thu | Bug fixes from testing | — |
| Fri | Buffer / bug-fix day | — |

### Week 5: Documentation & Polish

| Day | Task | Deliverable |
|---|---|---|
| Mon | Plugin author documentation: README in template repo | External authors can use it |
| Mon | Internal team docs: how to install plugins, how IT can bundle | Internal adoption |
| Tue | Update `Resources/plugins/registry.json` with final schema + sample data | Shipping configuration |
| Tue | Update `Dist/release/plugins/` to match | Release package is correct |
| Wed | UI polish pass on all 3 tabs (error messages, loading states, edge cases) | Polished demo |
| Wed | Add loading spinner / status during fetch and install | UX improvement |
| Thu | Final build + compilation check (`mvn compile -pl IDE -am`) | Green build |
| Fri | Prep demo environment, rehearse walkthrough | Ready to present |

### Week 6: Stakeholder Demo & Handover

| Day | Task | Deliverable |
|---|---|---|
| Mon | Finalize proposal document with screenshots | Presentation-ready |
| Mon | Prepare slide deck (5-10 slides) | Visual walkthrough |
| Tue | Stakeholder demo — 15 minutes | Buy-in |
| Wed | Write handover document: what was built, architecture decisions, known issues, next steps | Next person can run with it |
| Thu | Clean up branches, add inline code comments, remove dead code | Codebase is clean |
| Fri | Last day — answer questions, hand over access, push final commit | Internship complete |

---

## Component Decisions

| Component | What we use | Why |
|---|---|---|
| **Registry storage** | GitHub repo `ingenious-plugins` | Separate from main INGenious repo |
| **Plugin catalog** | `registry.json` (JSON file) | No database needed |
| **Fetching registry** | GitHub Contents API (`api.github.com`) | Live data, no CDN cache |
| **JAR downloads** | GitHub raw URL (`raw.githubusercontent.com`) | CDN-backed, free |
| **IDE registry URL** | Configurable from `app.settings` | IT changes URL without rebuild |
| **Plugins in IDE** | Browse / Installed / Publish tabs in Swing | Already built |
| **Publish workflow** | Stage locally → user runs `git add/commit/push` | No PAT in IDE |
| **Engine loading** | `PluginLoader` scans `Resources/plugins/` | Already built |
| **Plugin sandbox** | `PluginClassLoader` (child-first, parent-first for engine) | Already built |
| **Conflict detection** | In-IDE scan of registry + installed actions | Already built |
| **Version check** | `About.getBuildVersion()` vs `minEngineVersion` | Already built |
| **Offline fallback** | `file://` URL or bundled `Resources/plugins/` | Already supported |

---

## What We Are NOT Using

| Technology | Reason |
|---|---|
| Cloudflare Worker | Not needed for MVP, no time |
| Database | Static JSON is sufficient |
| Any paid service | No budget, no licenses |
| GitHub PAT in IDE | Fragile — user commits manually |
| Azure DevOps | Company moving to GitHub |

---

## Company Environment Assessment

> Tested: `git push` / `git pull` to GitHub fork works from the company laptop.
> Assumed working: `api.github.com` and `raw.githubusercontent.com` (same HTTPS/auth stack).

### Primary flow (expected to work)

| Component | URL | Auth |
|---|---|---|
| Fetch registry | `api.github.com/repos/Jul611/ingenious-plugins/contents/registry.json` | None needed (public repo) |
| Download JARs | `raw.githubusercontent.com/Jul611/ingenious-plugins/main/plugins/...` | None needed (public repo) |
| Publish (stage + push) | `git push` to fork | SSH or HTTPS credential (already works) |

### Fallbacks (stay in code as insurance, no extra work)

| Scenario | Fallback |
|---|---|
| `api.github.com` unreachable | Fall back to local `Resources/plugins/registry.json` |
| `raw.githubusercontent.com` blocked | "Import from File" button or `file://` URL |
| No internet at all | Bundle approved plugins in installer zip |
| Can't `git push` from laptop | CI/CD pipeline handles the push |

> "The plugin marketplace works with or without internet. Same UI, same code — only the file source changes. But in practice, if you can push to GitHub from your laptop, everything else will work too."

---

## Future Enhancements (Wishlist)

- Cloudflare Worker backend for ratings + download counts
- Java Security Manager sandboxing
- Web app with PostgreSQL for community features
- In-IDE plugin creation wizard
- Auto-update background checker
- Private registry + approval workflow

---

## Summary

**6 weeks. One person. Zero budget.**

| Week | Focus | Key deliverables |
|---|---|---|
| 1 | Registry infrastructure | Separate plugins repo, GitHub API fetch, configurable URL |
| 2 | Plugin author experience | Template repo, contributing guide, publish CLI |
| 3 | GitHub Actions CI | PR validation pipeline, security scanning |
| 4 | Engine integration testing | End-to-end: install → restart → actions appear |
| 5 | Documentation & polish | Author docs, team docs, UI cleanup |
| 6 | Stakeholder demo & handover | Presentation, handover doc, final commit |
