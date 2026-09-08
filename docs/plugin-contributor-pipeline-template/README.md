# Plugin Contributor Pipeline Template — Setup

This is what a plugin author copies into **their own** repo and **their own**
Azure DevOps project to build and validate their plugin *before* submitting
it to the marketplace. It's separate from `docs/plugin-registry-template/`,
which is the registry repo itself (`registry.json` + the marketplace's own
CI) — contributors don't need access to that repo at all to use this.

## What this is (and isn't)

This pipeline builds the plugin and runs the same checks the marketplace's
own `validate-pipeline.yml` runs — required files present, README length,
`groupId` namespace, build succeeds, manifest has `pluginEntryClasses` — on
the contributor's own infrastructure. The point is to catch obviously-broken
submissions early, before they ever cost the marketplace a CI run or a
reviewer's attention.

It does **not** publish anywhere. Once a contributor is happy with their own
green run here, they submit through INGenious's Plugin Manager Publish tab,
pointing it at this same repo+branch. That triggers the marketplace's own,
completely independent `validate-pipeline.yml` against the exact same
source, and only after a human reviews and merges does `publish-pipeline.yml`
(marketplace-owned, not this one) actually deploy to the Azure Artifacts
feed. Passing this pipeline is a courtesy pre-check, not a trust boundary —
the marketplace can't rely solely on a contributor's own self-reported pass
before it takes the action of publishing under its own authority, so it
always re-validates and re-builds regardless of what happened here.

## What to hand a contributor

1. `azure-pipelines.yml` from this folder. It builds and installs
   `ingenious-api` itself, straight from `github.com/ing-bank/INGenious`
   (public, MIT-licensed) — nothing to build or hand over separately.
2. Confirmation of the required Maven `groupId` namespace (`com.ing.plugins`
   unless that's changed) and, if their plugin's `pom.xml` isn't at the repo
   root, the right `PLUGIN_PATH` value to fill in.
3. Their plugin needs a `.submission.json` at its root (or wherever
   `PLUGIN_PATH` points) alongside `pom.xml` and `README.md` — this is the
   metadata INGenious's Publish tab and the registry submission flow expect.

No ADO credentials of any kind are needed for this template — it never
talks to the Azure Artifacts feed.
