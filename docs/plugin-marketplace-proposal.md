# Plugin Marketplace for INGenious IDE
### Proposal — Turn INGenious into a platform

---

## The Problem

Today, adding new automation capabilities to INGenious means:
- Forking the main repo
- Editing `@Action` classes in the Engine codebase
- Rebuilding the entire IDE
- Distributing a new installer

**Result:** One team's useful OCR step builder action is invisible to everyone else. Custom capabilities stay siloed. The community can contribute code but not easily share it.

---

## The Vision

A **plugin marketplace** built right into the IDE:

- **Browse** available plugins from a GitHub-hosted registry
- **Install** plugins with one click — no code, no rebuild
- **Publish** your own plugins — select a JAR, auto-extract metadata, push to GitHub
- **Manage** installed plugins — uninstall, check for updates

Anyone who can write a Java class can ship a plugin. No fork needed.

---

## How It Works (Architecture)

```
┌─────────────────────────────────────┐
│         INGenious IDE                │
│  ┌───────────────────────────────┐  │
│  │    Plugin Manager (3 tabs)     │  │
│  │  ┌─────────┐ ┌────────┐ ┌───┐ │  │
│  │  │ Browse  │ │Installed│ │Pub│ │  │
│  │  │(Market) │ │(Manage) │ │ish│ │  │
│  │  └────┬────┘ └───┬────┘ └─┬─┘ │  │
│  │       │           │        │    │  │
│  │  ┌────┴───────────┴────────┘   │  │
│  │  │   PluginManagerService       │  │
│  │  │  - fetchRegistry()           │  │
│  │  │  - installPlugin()           │  │
│  │  │  - publishPluginToGitHub()   │  │
│  │  └───────────┬─────────────────┘  │
│  └──────────────┼────────────────────┘
│                 │ HTTPS
└─────────────────┼──────────────────┘
                  │
┌─────────────────┼──────────────────┐
│  GitHub         │                  │
│  (Jul611/       │                  │
│   INGenious)    │                  │
│  ┌──────────────┴───────────────┐  │
│  │ Resources/plugins/           │  │
│  │  ├── registry.json           │  │
│  │  ├── demo-helloworld/        │  │
│  │  │   ├── demo-helloworld.jar │  │
│  │  │   └── .plugininfo         │  │
│  │  └── calc-math/              │  │
│  │      ├── calc-math.jar       │  │
│  │      └── .plugininfo         │  │
│  └──────────────────────────────┘  │
└────────────────────────────────────┘
```

---

## What's Built: PoC Demo

### Plugin Manager (Browse | Installed | Publish)

| Tab | What it does | Status |
|---|---|---|
| **Browse** | Fetches registry from GitHub raw, shows table with name/author/version/actions, Install button | ✅ Working |
| **Installed** | Lists locally installed plugins, Uninstall button, Check for Updates | ✅ Working |
| **Publish** | Select a JAR → auto-extracts manifest + @Action methods → fill/edit metadata → publish locally + optionally push to GitHub via PAT | ✅ Working |

### Safety checks
- **Conflict detection** — warns if a new plugin's `@Action` names collide with existing installed plugins
- **Version compatibility** — checks `minEngineVersion`/`maxEngineVersion` against engine build version
- **Duplicate action detection** — prevents registering the same action name for the same object type

### Engine loading
- `PluginLoader` scans both `Resources/plugins/` and `{appRoot}/plugins/` directories
- `PluginClassLoader` uses child-first loading with parent-first isolation for engine + API classes
- `PluginMetadata` reads `Plugin-Name`, `Plugin-Version`, `Plugin-Author`, etc. from JAR manifests

---

## Demo Walkthrough (5 minutes)

### 1. Open Plugin Manager
Click **Plugins** button in the toolbar → three tabs: Browse, Installed, Publish

### 2. Browse marketplace
Shows 2 demo plugins: "Hello World Demo" and "Screenshot Tools" — with featured stars, author, version, action counts, Install buttons

### 3. Publish a new plugin
- Switch to Publish tab
- Browse → select `calc-math/target/calc-math-1.0.0.jar`
- Form auto-fills: name "Calculator Math", version "1.0.0", author "Demo Author", actions "addNumbers, multiplyNumbers"
- Click **Publish to Registry** (local)
- Switch to Browse → click Refresh → "Calculator Math" appears with `addNumbers`, `multiplyNumbers`

### 4. (Optional) Push to GitHub
Enter a GitHub PAT → same button pushes `registry.json`, JAR, and `.plugininfo` to `initiative-repo` branch

---

## How to Write a Plugin

1. Create a Maven project, depend on `ingenious-api:3.0`
2. Write a class with `@Action` methods:
```java
@Action(object = ObjectType.GENERAL, desc = "Logs a greeting", input = InputType.YES)
public void sayHello() {
    String name = api.getData();
    api.getReport().updateTestLog("sayHello", "Hello " + name, Status.PASS);
}
```
3. Configure `pluginEntryClasses` in `pom.xml`'s `maven-jar-plugin`
4. Build → `mvn clean package`
5. Publish from the IDE: Plugins → Publish → select JAR → Publish

---

## What This Unlocks

### For plugin authors
- **Zero friction shipping** — build a JAR, publish from the IDE, done
- **No fork needed** — plugin lives in its own repo with its own release cycle
- **Versioned independently** — your plugin updates don't wait for the main INGenious release

### For the organization
- **Share internal capabilities** — deploy plugins to the team without rebuilding the IDE
- **Open-source community** — external contributors ship plugins without touching the core
- **Adoption driver** — a marketplace makes INGenious a platform, not just a tool

### For testers
- **One-click install** — no config, no classpath, no rebuild
- **Isolated** — plugins load in their own class loader, can't break the engine
- **Easy uninstall** — remove a plugin, restart, gone

---

## What a Full Production Version Needs

| Feature | Current PoC | Production target |
|---|---|---|
| Registry backend | Static `registry.json` on GitHub raw | Cloudflare Worker + KV for ratings, download counts |
| Auth | Optional GitHub PAT | OAuth / org SSO |
| Plugin sandbox | Child-first classloader | Optional security manager |
| CI pipeline | Manual PR validation | GitHub Actions auto-validate + publish |
| Ratings / reviews | Not supported | Cloudflare Worker with KV store |
| Auto-update | Manual "Check for Updates" | Background version check + one-click update |
| Documentation | README + example plugins | Plugin author guide + template repo |

---

## Timeline Estimate

| Phase | Duration | Deliverables |
|---|---|---|
| **Phase 1** (done) | 6 weeks | PoC: Browse, Install, Publish tabs + Engine loader |
| **Phase 2** | 2-3 weeks | GitHub Actions PR pipeline, plugin template repo, author docs |
| **Phase 3** | 2 weeks | Cloudflare Worker backend (ratings, download counts) |
| **Phase 4** | 2 weeks | Auto-update, conflict resolution, UI polish |
| **Phase 5** | Ongoing | Company GitHub migration, private registry support |

---

## Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Malicious plugin code | PR pipeline review, optionally code signing (Phase 3) |
| Duplicate action names break steps | Pre-install conflict detection with warning dialog |
| GitHub API rate limiting | Local-first merge: remote data cached, local entries always visible |
| No sandbox for plugins | Child-first classloader isolates plugin classes from engine |
| Plugin not appearing after install | Merge strategy: remote + local entries always combined |

---

## Summary

The Plugin Marketplace turns INGenious from a test automation tool into a **test automation platform**.

- **Users** get new capabilities without upgrading the IDE
- **Developers** ship plugins without forking the repo
- **The organization** benefits from shared, reusable automation

**Demo ready.** Ask for a walkthrough.
