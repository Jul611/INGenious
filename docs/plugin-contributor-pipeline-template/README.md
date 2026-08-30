# Plugin Contributor Pipeline Template — Setup

This is what a plugin author copies into **their own** repo and **their own**
Azure DevOps project to build, validate, and publish their plugin into the
shared ADO Artifacts feed. It's separate from `docs/plugin-registry-template/`,
which is the registry repo itself (`registry.json` + the marketplace's own
CI) — contributors don't need access to that repo at all under this model.

Publishing to the feed is the contributor's part. It is **not** the same as
being listed in the marketplace: a separate, marketplace-owned pipeline
watches the feed for new versions and runs its own validation and security
checks before anything gets added to `registry.json`. That pipeline belongs
to the marketplace team, not the contributor, and isn't covered by this
template.

## What to hand a contributor

1. `azure-pipelines.yml` from this folder.
2. A copy of `ingenious-api-3.0.jar` to place at `ci/vendor/ingenious-api-3.0.jar`
   in their repo (build it yourself: `mvn -q -f ingenious-api/pom.xml clean package -DskipTests`,
   the jar lands at `ingenious-api/target/ingenious-api-3.0.jar`). It isn't
   published anywhere authenticated today, so this has to be vendored rather
   than resolved as a normal Maven dependency.
3. The real values for the `TODO`s in the YAML: the ADO feed URL, and
   confirmation of the required Maven `groupId` namespace (`com.ing.plugins`
   unless that's changed).
4. A scoped ADO PAT — **Packaging: Read & Write** on the feed only — for
   them to add as a **secret** pipeline variable named `ADO_PUBLISH_PAT` in
   their own project (Pipelines → Edit → Variables → "Keep this value
   secret"). This is a different PAT/scope than the Read-only one INGenious's
   own Registry Settings writes locally for installs — don't reuse either
   PAT for the other purpose.
5. Their plugin needs a `.submission.json` at its root (or wherever
   `PLUGIN_PATH` points) alongside `pom.xml` and `README.md` — same shape
   as what `PluginRegistryCliService`/the old fork+PR flow expected. This is
   the metadata the marketplace's internal pipeline will eventually read to
   populate `registry.json`.

## Open question this doesn't answer yet

Where the marketplace's own internal pipeline picks this up from — i.e.
what triggers it off a new feed version, how it fetches `.submission.json`
now that it's not arriving via a PR into the registry repo, and whether the
old fork+PR-to-registry-repo flow (`submitPlugin()` in
`PluginRegistryCliService`) still has a role for anything. Not addressed
here — this file only covers the contributor's own build-validate-publish
step.
