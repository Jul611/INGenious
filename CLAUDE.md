# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

INGenious is a Java/JavaFX no-code/low-code test automation IDE (Playwright-Java, Appium, JDBC, JMS/Kafka, SAP), built by ING Bank and open-sourced under MIT (`ing-bank/INGenious`). Full user docs: https://ing-bank.github.io/ingenious-doc/.

## Build & test

This is a multi-module Maven reactor. **`ingenious-api` is not a reactor module** — it's a standalone project that other modules (and all plugins) depend on with `scope=provided`, and must be built and installed to the local `.m2` cache *before* the main reactor:

```bash
mvn clean install --file ingenious-api/pom.xml
mvn clean install --file pom.xml
```

To iterate on a single module without a full reactor build (much faster), build it plus only its required upstream modules:

```bash
mvn -pl IDE -am compile -DskipTests
```

Other useful commands:
- `mvn test` — runs the Surefire test suite (JaCoCo coverage is wired into the same lifecycle).
- `mvn dependency-check:check --file pom.xml` — OWASP dependency vulnerability scan (CI runs this on JDK 11, specifically; the main build runs on JDK 17).
- `mvn versions:display-dependency-updates` — check for outdated dependency versions (`versions-maven-plugin` is available).

**Auto-formatting gotcha**: `prettier-maven-plugin` is bound to the `validate` phase with the `write` goal (see root `pom.xml`), which means **any `mvn` command that reaches `validate` — including a plain `compile` — silently reformats every Java file it touches in place.** If you write/edit a file and then run a Maven command, expect the file's exact byte content (indentation, line-wrapping of long expressions/string concatenations) to change afterward. This breaks exact-string-match tools (e.g. an `Edit`-style diff) if you re-match against what you originally wrote rather than the post-format state — re-read the file (or use line-number/regex-based replacement) after the first `mvn` invocation touches it, not before.

## Module structure

- **Common** — shared build-lifecycle helpers/config for the reactor.
- **Datalib** — backend data source access, data retrieval/transformation.
- **Engine** — core business logic and test-execution workflows; also owns plugin *loading* (`Engine/src/main/java/com/ing/engine/plugin/loader/`).
- **IDE** — the Swing/JavaFX desktop IDE itself; almost all user-facing feature code lives here, under `IDE/src/main/java/com/ing/ide/`.
- **StoryWriter** — BDD scenario/feature authoring.
- **TestData - Csv** — test data / test plan orchestration.
- **Dist** — assembles the final packaged distribution (zip) from the other modules' outputs.
- **ingenious-api** (root-level, non-reactor) — the public contract plugins compile against.

## Code conventions

4-space indent, ~120-char lines, braces always (even single-statement blocks), `PascalCase`/`camelCase`/`UPPER_SNAKE_CASE`. Prefer collection interfaces (`List`, `Map`) over concrete types, Stream API for filtering/mapping, `final` where applicable. INGenious action-naming conventions: storage actions are `store<Data>In<Target>` (e.g. `storeDBValueInDataSheet`), assertions are `assert<Object><Condition>` (e.g. `assertResponseBodyContains`).

The stated convention is no wildcard imports — in practice, existing files (e.g. the whole `pluginmanager` package) use `import java.awt.*;`/`import java.util.*;` throughout. Match the surrounding file's existing import style rather than enforcing the stated rule against it.

## Plugin Marketplace (in progress)

Lives under `IDE/src/main/java/com/ing/ide/main/mainui/components/pluginmanager/`. Lets plugin authors submit a plugin via fork+PR against a separate GitHub registry repo, and lets any user browse/install published plugins — all without ever storing a GitHub PAT: `git`/`gh` manage their own credentials (HTTPS+Git Credential Manager, `gh auth login`), and the IDE never touches either directly for auth.

**Flow**: contributor points Publish at their plugin's Maven source project → IDE builds it locally to extract manifest metadata → on submit, forks-or-branches the registry repo (based on actual write-access via `gh api .../permissions.push`, not on repo ownership) → opens a PR. A human reviews and merges. CI (not yet wired to a real repo — see below) then builds the real artifact, `mvn deploy`s it to an Azure Artifacts Maven feed, and patches the registry via its own auto-merged PR. Install resolves the jar + transitive deps from that feed via Maven.

**Key files**:
- `PluginRegistryCliService` — all `git`/`gh`/`mvn` process-shelling: fork/branch resolution, PR submission, registry reads, Maven artifact/dependency resolution, and `saveAdoCredential()` (writes a user-pasted ADO PAT into their real `~/.m2/settings.xml`, preserving anything else already there).
- `PluginRegistryConfig` / `AppSettings` — 8 externalized config values (registry repo/branch/path, Maven groupId, ADO org/project/feed/server-id), all blank by default — swapping environments (personal test setup → real company repo/feed) is meant to be a config-only change, never a code change.
- `PluginRegistrySettingsDialog` — the config UI, plus a write-only ADO PAT field and a live git/gh/mvn/ADO-credential status panel.
- `PluginManagerService` / `PluginManagerPublishUI` / `PluginManagerBrowseUI` — orchestration and the Publish/Browse tab UIs.
- `docs/plugin-registry-template/` — `validate.yml` (PR-time, no secrets, safe against untrusted fork PRs) and `publish.yml` (merge-time, builds + deploys to ADO + patches registry.json) as a copy-paste template for whatever repo ends up being the real registry, plus its own setup README. `ingenious-api` is deliberately *vendored* as a jar in this template (`ci/vendor/`) rather than resolved from ADO, specifically because `validate.yml` must stay credential-free to safely build fork-PR'd code.

**Current state**: publish (opens a real PR), browse (reads a real `registry.json`), and install (resolves a real jar from ADO Artifacts) are all built and manually smoke-tested end to end, against a personal/workaround GitHub fork and a real company ADO feed — the actual company registry repo doesn't exist yet (GitHub migration still in progress).

**Next task — the CI pipelines**: `validate.yml`/`publish.yml` exist only as templates; they aren't wired into a running repo yet. Open decision before that happens: keep `publish.yml`'s ADO deploy step on GitHub Actions with a manually-owned, write-scoped `ADO_PAT` secret (simple, single CI system) — or split the merge-time build+deploy out to an Azure Pipeline that reuses the team's already-working `MavenAuthenticate@0` + `$(System.AccessToken)` pattern from their existing INGenious-build pipeline (no secret to own/rotate at all, but splits CI across GitHub Actions for PR validation and Azure Pipelines for the deploy step). Neither is implemented yet.
