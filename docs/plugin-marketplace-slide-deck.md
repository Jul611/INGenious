# Plugin Marketplace — PowerPoint Slide Guide

> **Target audience:** Stakeholders / decision-makers
> **Duration:** 15 minutes (12 slides)
> **Goal:** Get approval to continue building the plugin marketplace

---

## Slide 1: Title Slide

**Text:**
```
Plugin Marketplace for INGenious IDE
Turning INGenious from a tool into a platform

Julian | Intern Project | July 2026
```

**Speaker notes:**
"Hi everyone. Today I'm presenting the plugin marketplace — a feature that lets anyone extend INGenious without forking the codebase. I'll show you what it does, why it matters, and how we can build it in 6 weeks with zero budget."

---

## Slide 2: The Problem

**Text:**
```
Today, adding new capabilities to INGenious means:
• Forking the main repo
• Editing @Action classes in the Engine
• Rebuilding the entire IDE
• Distributing a new installer

Result: Custom capabilities stay siloed.
One team's useful feature is invisible to everyone else.
```

**Speaker notes:**
"Right now, if a team builds a new test step — say, an OCR reader or a SAP connector — there's no easy way to share it. You'd have to fork the repo, edit engine code, rebuild, and distribute a new installer. That's not scalable."

**Visual idea:** Simple icon showing a broken chain or isolated islands.

---

## Slide 3: The Vision

**Text:**
```
A plugin marketplace built right into the IDE
• Browse — discover plugins from a GitHub registry
• Install — one click, no code, no rebuild
• Publish — select a JAR, auto-extract metadata, done
• Manage — uninstall, check for updates

Anyone who can write a Java class can ship a plugin.
```

**Speaker notes:**
"Imagine opening INGenious, clicking 'Plugins', seeing a marketplace of available extensions, and installing one with a single click. No config files, no classpath changes, no rebuild. That's what we're building."

**Visual idea:** Screenshot mockup of a Plugin Manager window with Browse/Installed/Publish tabs.

---

## Slide 4: Architecture (Simple)

**Text:**
```
┌─────────────────────┐     GitHub repo: ingenious-plugins
│   INGenious IDE      │     ┌──────────────────────┐
│   ┌───────────────┐  │     │  registry.json        │
│   │ Plugin Manager │──┼─────┤  plugins/hello-world │
│   │ Browse         │  │     │  plugins/calc-math   │
│   │ Installed      │  │     └──────────────────────┘
│   │ Publish        │  │
│   └───────────────┘  │     All free, no server needed
└─────────────────────┘
```

**Speaker notes:**
"The architecture is intentionally simple. We use a public GitHub repo as the registry — a single JSON file lists all available plugins. The IDE fetches this file directly from GitHub's API. No databases, no servers, no licenses to buy."

**Visual idea:** Keep it clean — two boxes with arrows.

---

## Slide 5: Demonstration

**Text:**
```
Live Demo (5 minutes)

1. Open Plugins → Browse tab shows available plugins
2. Switch to Installed → see what's already installed
3. Switch to Publish → select a JAR → auto-fills metadata
4. Click Publish → plugin staged locally
5. Browse tab refreshes → new plugin appears
```

**Speaker notes:**
"Let me show you this working. I'll walk through the full flow: browsing the marketplace, publishing a new plugin from a built JAR, and seeing it appear in the list."

**Visual idea:** This slide is minimal — the demo does the talking.

---

## Slide 6: How Plugins Work

**Text:**
```
A plugin is just a JAR with:
• A class with @Action methods
• Plugin metadata in the manifest

Example:
@Action(object = "General", desc = "Adds two numbers")
public void addNumbers() {
    int result = Integer.parseInt(api.getData());
    api.getReport().updateTestLog("addNumbers", 
        "Result: " + result, Status.PASS);
}
```

**Speaker notes:**
"Writing a plugin is straightforward. You create a Java class, annotate methods with @Action, and set a few manifest entries in your pom.xml. Build a JAR, publish it from the IDE, and you're done. The template repo makes this even easier — we'll have a 'Use this template' button so you can start in 5 minutes."

**Visual idea:** Code snippet with syntax highlighting.

---

## Slide 7: Safety & Security

**Text:**
```
Built-in protections:
• Conflict detection — warns if action names collide
• Version compatibility — checks engine version
• ClassLoader isolation — plugins can't break the engine
• PR pipeline — validates every submission
• Security scanning — Trivy scans JARs for vulnerabilities

Plus: all plugins go through code review via GitHub PRs.
```

**Speaker notes:**
"Safety was a priority from the start. Before installing, the IDE checks for duplicate action names and engine version compatibility. At runtime, each plugin loads in its own classloader so it can't interfere with the engine or other plugins. And every plugin submission goes through a GitHub PR with automated validation and security scanning."

**Visual idea:** Shield icons next to each bullet point.

---

## Slide 8: Offline & Enterprise

**Text:**
```
Works with or without internet:
• Online: fetches from GitHub API live
• Offline: bundled plugins in the installer
• Restricted: read from a network share (file://)
• No GitHub: "Import from File" button

IT can configure the registry URL in app.settings.
No server to deploy. No licenses needed.
```

**Speaker notes:**
"The marketplace works in any environment. If GitHub is accessible, plugins load live. If not, IT can bundle approved plugins in the installer or host them on a network share. The same code handles all scenarios — only the URL changes."

**Visual idea:** Three columns showing Online / Offline / Restricted with checkmarks.

---

## Slide 9: Project Plan

**Text:**
```
6 weeks, one person, zero budget

Week 1: Registry setup — separate repo, API fetch
Week 2: Author experience — template repo, docs
Week 3: CI pipeline — PR validation, security scan
Week 4: Testing — end-to-end integration
Week 5: Polish — docs, UI cleanup
Week 6: Demo & handover
```

**Speaker notes:**
"We have 6 weeks to ship this. Week 1 sets up the registry infrastructure. Week 2 makes it easy for plugin authors. Week 3 adds automated validation. Week 4 is testing. Week 5 is documentation and polish. Week 6 is the stakeholder demo and handover."

**Visual idea:** Timeline graphic with 6 blocks.

---

## Slide 10: What This Enables

**Text:**
```
For the organization:
• Share automation capabilities across teams
• No more forking the main repo for every feature
• Community contributions without core access
• Adoption driver — INGenious becomes a platform

For testers:
• One-click install of new capabilities
• No config, no rebuild, no IT ticket
• Simple uninstall if something doesn't work
```

**Speaker notes:**
"This unlocks a lot. Teams can share their internal automation tools. The open-source community can contribute plugins without touching the core code. And for testers, getting new capabilities becomes as simple as clicking Install."

**Visual idea:** Three personas (org, developer, tester) with their benefits.

---

## Slide 11: What We're NOT Building (Yet)

**Text:**
```
Future ideas (if this takes off):
• Cloudflare Worker — for ratings and download counts
• Web marketplace — browse plugins from a browser
• Auto-updates — background version checking
• Plugin sandbox — Java Security Manager
• Plugin creation wizard — right-click → new plugin

But none of these are needed for the MVP.
```

**Speaker notes:**
"If the marketplace gets traction, there's a lot more we can do — ratings, a web storefront, auto-updates. But those are future phases. What we have right now — browse, install, publish, manage — is already a complete feature worth shipping."

**Visual idea:** Dimmed/greyed icons for future features, bright for MVP features.

---

## Slide 12: Summary & Ask

**Text:**
```
The plugin marketplace:
• Already 60% built (Browse, Install, Publish tabs working)
• Zero infrastructure — just a GitHub repo
• Zero licenses — all free and open-source
• Works online or offline
• 6 weeks to completion

I'm asking for approval to continue building this
as my remaining intern project.
```

**Speaker notes:**
"To summarize: the core is already built and demoable. The remaining work is infrastructure — a separate plugins repo, a CI pipeline, documentation, and testing. No budget needed, no licenses, no servers. I have 6 weeks left in my internship and I believe we can ship this."

**Visual idea:** Clean closing slide with the key message.

---

## Bonus: Q&A Prep

**Anticipated questions and answers:**

| Question | Answer |
|---|---|
| "What about malicious plugins?" | All plugins go through GitHub PR review. The pipeline checks for vulnerabilities. At runtime, plugins are sandboxed in their own classloader. |
| "How do we control which plugins users can install?" | IT sets the registry URL in app.settings. For restricted environments, plugins are bundled in the installer. |
| "Who maintains the plugin repo?" | The repo is public, so anyone can submit a PR. The INGenious maintainers review and merge. |
| "What if GitHub is blocked?" | The "Import from File" button works without any network. Or IT can configure a file:// path to a network share. |
| "Can plugins break the engine?" | No — each plugin loads in its own ClassLoader with child-first isolation. Engine classes cannot be overridden by plugins. |
| "How long until it's ready?" | 6 weeks. The Browse, Install, and Publish tabs already work. The remaining work is infrastructure and polish. |
