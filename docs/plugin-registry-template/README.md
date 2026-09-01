# Plugin Registry Repo — Setup

This is a template for the **separate** GitHub repo that hosts Plugin
Marketplace contributions (fork+PR, human review) and triggers the Azure
Artifacts publish pipeline. It doesn't live inside the INGenious app repo at
runtime — copy this structure into its own repo.

**GitHub Actions is not available for this org** — hosted runners are
disabled, and installing the Azure Pipelines GitHub App yourself is also not
allowed (that's an org-admin action, done centrally). So CI for this repo
runs on **Azure Pipelines** instead, triggered by its native GitHub PR/push
integration. The `.github/workflows/` files below are kept for reference /
for anyone whose org *can* use Actions — the pipelines actually in use are
under `ado-pipelines/`.

## Layout

```
your-plugin-registry-repo/
├── registry.json                    # seed as {"version":1,"plugins":[]}
├── plugins/                         # one folder per submitted plugin
├── ci/
│   └── vendor/
│       └── ingenious-api-3.0.jar    # see ci/vendor/README.md
├── ado-pipelines/                   # ← what's actually wired up (see below)
│   ├── validate-pipeline.yml        # PR-time: no ADO/GitHub secrets, safe against forks
│   ├── publish-pipeline.yml         # merge-time: builds, deploys, patches registry
│   └── cleanup-stale-branches-pipeline.yml  # scheduled: deletes rejected submissions' branches
└── .github/
    └── workflows/                   # reference only — GH Actions isn't usable here
        ├── validate.yml
        └── publish.yml
```

## One-time setup

1. Build `ingenious-api` from the INGenious repo and vendor it:
   ```bash
   mvn -q -f ingenious-api/pom.xml clean package -DskipTests
   ```
   Copy the resulting jar to `ci/vendor/ingenious-api-3.0.jar` in the new repo.
2. Create the repo (`gh repo create <name> --private`) and push this
   skeleton, seeding `registry.json` as `{"version":1,"plugins":[]}`.
3. **Connect the repo to Azure Pipelines**: repos created within the company
   get the org's GitHub App connection automatically — check Project Settings
   → Service connections for it. A personal/workaround repo you created
   yourself won't have it; standing GitHub PATs aren't allowed as a substitute
   here, so a repo without the App connection can't run `publish-pipeline.yml`
   or `cleanup-stale-branches-pipeline.yml` for real (only `validate-pipeline.yml`,
   which needs no GitHub write access at all, works either way).
4. Create three pipelines in Azure DevOps pointing at `ado-pipelines/validate-pipeline.yml`,
   `ado-pipelines/publish-pipeline.yml`, and `ado-pipelines/cleanup-stale-branches-pipeline.yml`
   respectively. Fill in the `TODO`s in each file: registry branch name, agent
   pool if not using Microsoft-hosted agents, `GITHUB_REPO_SLUG`/`ADO_FEED_URL`
   pipeline variables (shared across the two files — a variable group is worth
   setting up rather than entering them twice), and each file's
   `create-github-app-token@1` step needs `githubAppConnection` set to the
   actual name of the connection from step 3, plus `owner`/`repositories` to
   scope the generated token to just this one repo. That task exchanges the
   App connection for a short-lived installation token — see the comment at
   the top of `publish-pipeline.yml` for why a standing PAT isn't used instead.
   The cleanup pipeline only deletes a submission branch once its PR has been
   closed-without-merging for `CLEANUP_GRACE_DAYS` (7 by default) — it never
   touches open PRs or fork-submitted branches (those live on the contributor's
   own fork, not this repo). Merged PRs' branches are handled separately by
   this repo's own "Automatically delete head branches" setting (step 5 below).
5. Turn on Settings → General → Pull Requests → "Automatically delete head
   branches" — free cleanup for merged submissions' branches, complementing
   the cleanup pipeline from step 4 (which only handles the rejected case).
   Then set up branch protection/ruleset on your registry branch (not `main`,
   unless that's genuinely your default branch — see CLAUDE.md): require a pull
   request, target only that one branch (not "All branches" — the IDE's
   own `plugin/**` submission branches and the bot's `bot/registry-update-**`
   branches need to stay unprotected so those flows can push/force-push
   freely). Once you've set that up, check "Require status checks to pass
   before merging" and add `validate` (or whatever name Azure Pipelines
   reports it under on your PRs — check an actual PR's checks list to get
   the exact name) as a required check. This is now safe to require
   unconditionally: `validate-pipeline.yml` deliberately isn't path-filtered
   to `plugins/**` anymore, specifically so it still runs (and passes
   through cleanly) on PRs that don't touch that path — like the bot's own
   registry-patch PR — instead of never firing and leaving a required check
   stuck forever.
6. In INGenious's Plugin Manager (the "Registry Settings..." button), point
   the 8 config values at this repo and your Azure Artifacts feed. For local
   installs, generate a personal ADO PAT scoped to **Packaging: Read** and
   paste it into the PAT field in that same dialog — the app writes it into
   your real `~/.m2/settings.xml` for you (preserving anything else already
   there), so nobody has to hand-edit that file. The field is write-only and
   never redisplays the saved value. Note this is a **separate** config
   surface from the Azure Pipelines variables in step 4 — nothing you enter
   here reaches ADO or GitHub's side of things; each needs to be set once,
   independently.

Everything above is configuration, not code — this same layout and these
same pipelines work unchanged whether they're pointed at a personal GitHub
account and a personal ADO feed, or the real company org and feed. Swapping
environments later should only ever mean redoing this checklist with new
values, never editing the pipeline files or the INGenious source.
