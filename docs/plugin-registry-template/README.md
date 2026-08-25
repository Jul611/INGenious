# Plugin Registry Repo — Setup

This is a template for the **separate** GitHub repo that hosts Plugin
Marketplace contributions (fork+PR, human review) and triggers the Azure
Artifacts publish pipeline. It doesn't live inside the INGenious app repo at
runtime — copy this structure into its own repo.

## Layout

```
your-plugin-registry-repo/
├── registry.json                    # seed as {"version":1,"plugins":[]}
├── plugins/                         # one folder per submitted plugin
├── ci/
│   └── vendor/
│       └── ingenious-api-3.0.jar    # see ci/vendor/README.md
└── .github/
    └── workflows/
        ├── validate.yml             # PR-time: no secrets, safe against forks
        └── publish.yml              # merge-time: builds, deploys, patches registry
```

## One-time setup

1. Build `ingenious-api` from the INGenious repo and vendor it:
   ```bash
   mvn -q -f ingenious-api/pom.xml clean package -DskipTests
   ```
   Copy the resulting jar to `ci/vendor/ingenious-api-3.0.jar` in the new repo.
2. Create the repo (`gh repo create <name> --private`) and push this
   skeleton, seeding `registry.json` as `{"version":1,"plugins":[]}`.
3. Branch protection on `main`: require a pull request, require the
   `validate` status check. Leave required-approvals at 0 if you want the
   bot's own registry-patch PR (opened by `publish.yml`) to auto-merge
   without a second manual click — every *contributor* submission should
   still require real review regardless of this setting; only the bot's
   trivial JSON-patch PR is a review-free-merge candidate. If your org
   mandates at least one approval with no exceptions, that PR just becomes a
   one-click manual merge instead — not a blocker, just not fully automatic.
4. Add repo Variables (Settings → Secrets and variables → Actions →
   Variables): `PLUGIN_MAVEN_GROUP_ID`, `ADO_ORGANIZATION`, `ADO_PROJECT`,
   `ADO_FEED_NAME`, `ADO_FEED_SERVER_ID`.
5. Add one repo **secret**: `ADO_PAT`, scoped to Packaging (Read & Write),
   tied to whichever ADO account is doing the publishing for now (a
   dedicated service account is the eventual target — a personal PAT is
   fine to unblock testing).
6. In INGenious's Plugin Manager (the "Registry Settings..." button), point
   the 8 config values at this repo and your Azure Artifacts feed. For local
   installs, generate a personal ADO PAT scoped to **Packaging: Read** and
   paste it into the PAT field in that same dialog — the app writes it into
   your real `~/.m2/settings.xml` for you (preserving anything else already
   there), so nobody has to hand-edit that file. The field is write-only and
   never redisplays the saved value.

Everything above is configuration, not code — this same layout and these
same two workflows work unchanged whether they're pointed at a personal
GitHub account and a personal ADO feed, or the real company org and feed.
Swapping environments later should only ever mean redoing this checklist
with new values, never editing the workflow files or the INGenious source.
