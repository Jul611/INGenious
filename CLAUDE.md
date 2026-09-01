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
- `PluginRegistryCliService` — all `git`/`gh`/`mvn` process-shelling: fork/branch resolution, PR submission, registry reads, Maven artifact/dependency resolution, and `saveAdoCredential()` (writes a user-pasted ADO PAT into their real `~/.m2/settings.xml`, preserving anything else already there). `GH_OWN_GIT_CREDENTIAL_ENV` hands gh's own already-verified git credential helper to every network git op in the submission flow — clone, push, and `gh pr create`'s own internal git use — via a scoped `GIT_CONFIG_COUNT` env override (not a persisted git-config change), so the flow depends on exactly one credential source instead of also trusting git's separate credential helper (e.g. GCM) to be configured correctly on the user's machine; the older soft `gh auth setup-git` suggestion is gone now that this is unconditional. `submitPlugin()`'s branch push is `git push --force`, not a plain push — the per-submission branch name (`plugin/<name>/v<version>`) is deterministic, so resubmitting the same plugin+version (routine during manual testing) would otherwise fail as non-fast-forward against the leftover branch from the prior attempt; against a real shared registry with outside contributors, this would also silently clobber any manual follow-up commit someone pushed straight to that PR branch.
- `PluginRegistryConfig` / `AppSettings` — 8 externalized config values (registry repo/branch/path, Maven groupId, ADO org/project/feed/server-id), all blank by default — swapping environments (personal test setup → real company repo/feed) is meant to be a config-only change, never a code change.
- `PluginRegistrySettingsDialog` — the config UI, plus a write-only ADO PAT field and a live git/gh/mvn/ADO-credential status panel.
- `PluginManagerService` / `PluginManagerPublishUI` / `PluginManagerBrowseUI` — orchestration and the Publish/Browse tab UIs.
- `docs/plugin-registry-template/` — a copy-paste template for whatever repo ends up being the real registry, plus its own setup README. **GitHub Actions hosted runners are not available for this org** (confirmed via a hard error, not just a preference) and installing the Azure Pipelines GitHub App yourself isn't allowed either (that's an org-admin action, done automatically for repos created within the company) — so the `.github/workflows/validate.yml`/`publish.yml` pair is kept only as reference; the CI actually in use lives in `ado-pipelines/` (`validate-pipeline.yml`, `publish-pipeline.yml`, `cleanup-stale-branches-pipeline.yml`), triggered by Azure Pipelines' native GitHub PR/push integration instead. `ingenious-api` is deliberately *vendored* as a jar in this template (`ci/vendor/`) rather than resolved from ADO, specifically so the PR-time pipeline stays credential-free to safely build fork-PR'd code — that reasoning holds regardless of which CI system runs it.
- `docs/plugin-contributor-pipeline-template/` — a parked alternate design where each contributor runs their own build+validate+publish pipeline (using a template we'd hand out) and a separate marketplace-owned pipeline validates/security-scans the artifact only after it lands in the ADO feed, rather than the marketplace building contributor source itself. Deliberately not deleted — "PR-triggered is better for now" was the explicit call, but this is the fallback if the centralized fork+PR build ever becomes a bottleneck (e.g. many concurrent external contributors).

**Current state**: publish (opens a real PR), browse (reads a real `registry.json`), and install (resolves a real jar from ADO Artifacts) are all built and have been manually exercised end to end against a personal/workaround GitHub fork (a separate repo, content wiped, working off a `registry-branch` rather than its default branch) and a real company ADO feed — the actual company registry repo doesn't exist yet (GitHub migration still in progress). The CI pipeline piece is drafted (`ado-pipelines/`) but not yet exercised end to end (see below) — and per confirmation below, most of it structurally *can't* be, until the real repo exists.

**Known gotchas from manual testing**:
- **ADO Artifacts has no Maven-publish path via `az` CLI.** `az artifacts` only supports Universal Packages (a different, incompatible package type/protocol from what `resolvePluginArtifact()` resolves) — publishing a real Maven artifact only ever happens via `mvn deploy`, never `az`.
- **Two different ADO PAT scopes matter and are easy to conflate**: the one in `~/.m2/settings.xml` (managed by the app via `saveAdoCredential()`) is deliberately **Packaging: Read** only, for installs. A manual `mvn deploy` test needs a separate **Packaging: Read & Write** PAT under a different `<server>` id — trying to deploy with the read-only one fails with 401, by design, not a bug.
- **`-DaltDeploymentRepository=...` on the command line is fragile** across shells (line-continuation characters differ between bash/zsh and PowerShell; a dropped `-D` prefix manifests as a confusing "could not resolve plugin/artifact" error, not an auth error). For one-off manual deploys, temporarily adding `<repositories>`/`<distributionManagement>` to a *scratch copy* of the plugin's `pom.xml` sidesteps this — never commit that into `plugin-examples/helloworld/pom.xml` itself, since contributor plugin POMs are meant to stay infra-agnostic.

**CI pipelines — architecture settled, wiring still unverified**: GitHub Actions is unavailable outright (hosted runners disabled org-wide), so PR-time validate and merge-time publish both run on Azure Pipelines (`docs/plugin-registry-template/ado-pipelines/`), triggered via Azure Pipelines' own GitHub PR/push integration rather than `.github/workflows/`. The contributor-owns-their-pipeline alternative was considered and explicitly parked in favor of sticking with this centralized-build model for now (see `docs/plugin-contributor-pipeline-template/` above).

Two things changed from the original GitHub-Actions-only design, beyond just moving where the YAML runs:
- The merge-time deploy step uses `MavenAuthenticate@0` + `$(System.AccessToken)` for the ADO feed (reusing the team's existing INGenious-build pipeline pattern) — no `ADO_PAT` secret to own/rotate.
- All GitHub-side write operations — the registry-patch push+PR in `publish-pipeline.yml`, the PR-listing+branch-deletion in `cleanup-stale-branches-pipeline.yml` — go through the **`create-github-app-token@1`** task instead of a GitHub PAT, since a standing PAT for this isn't allowed. It exchanges the org's installed GitHub App connection for a short-lived (~1hr) installation token, consumed via `GH_TOKEN` for `gh` CLI calls.

**Confirmed working** against the `marketplace-registry` test branch (a temporarily-borrowed company-created repo, chosen specifically because it has the GitHub App connection — the personal/workaround fork doesn't): `docs/plugin-registry-template/ado-pipelines/diagnostic-github-app-token-test.yml`, a throwaway diagnostic pipeline, ran end to end — the token task resolved, the `githubAppConnection` connection resolved, and the resulting installation token has both read and write access (verified by creating and immediately deleting a real branch via the GitHub API). This settles what was previously the biggest open risk in this design. Still not reconciled: whether the task name that actually worked was `create-github-app-token@1` or `GitHubAppToken@1` — and the real `githubAppConnection`/`GITHUB_REPO_SLUG`/`ADO_FEED_URL` values still need copying from the diagnostic pipeline's config into the real `publish-pipeline.yml` and `cleanup-stale-branches-pipeline.yml` (which still have TODO placeholders for those). The diagnostic pipeline/file itself is disposable once that's done.

Confirmed, and it narrows what's actually testable right now: the GitHub App connection is enabled automatically on repos **created within the company**, not on personal ones. The current personal/workaround fork therefore can only ever prove out `validate-pipeline.yml` for real (it needs no GitHub write access at all) — `publish-pipeline.yml` and `cleanup-stale-branches-pipeline.yml` both need the real company registry repo to exist before they can be end-to-end tested.

Still unfilled placeholders in all three `ado-pipelines/*.yml` files: registry branch name, agent pool, `GITHUB_REPO_SLUG`/`ADO_FEED_URL` variables, and each file's `create-github-app-token@1` step's `githubAppConnection`/`owner`/`repositories` inputs. Also still unconfirmed: whether Azure Pipelines' fork-PR secret-withholding behavior actually matches GitHub Actions' — needed before treating `validate-pipeline.yml` as safe against untrusted fork PRs, which is the one pipeline actually reachable for testing against the current workaround repo.
