# Vendored `ingenious-api`

This directory holds a prebuilt `ingenious-api-3.0.jar`, checked in on
purpose — the one deliberate exception to "never commit built JARs" in this
project.

## Why this one is different

The PR-time `validate.yml` workflow builds a contributor's plugin from
source before a human has reviewed it, so it has to run with **no
repository secrets** — GitHub's default behavior for `pull_request`-triggered
workflows already enforces this for forked PRs, and that's exactly the
safety property the PR-time/merge-time split exists to protect. `ingenious-api`
isn't published anywhere remote today, so resolving it from an authenticated
source (the Azure Artifacts feed) isn't an option in that workflow without
either giving untrusted PR builds secrets access, or standing up a second,
separately public feed just for this one artifact.

A vendored jar sidesteps the problem entirely: `mvn install:install-file`
needs no network access and no credentials, so it works identically whether
or not the PR came from a fork. It's a reasonable one-off because it's a
single, low-frequency-change SDK pin — not the many-versions-of-many-plugins
churn that "don't commit built JARs" is actually about (that rule targets
the plugin *distribution* mechanism, i.e. the actual product, not a stable
build-time SDK dependency).

## Refreshing it

When `ingenious-api`'s version changes:

```bash
mvn -q -f ingenious-api/pom.xml clean package -DskipTests
cp ingenious-api/target/ingenious-api-<version>.jar ci/vendor/
```

Then update the `-Dversion=` value in both `validate.yml` and `publish.yml`'s
"Seed local repo with ingenious-api" step to match, and bump
`PluginRegistryConfig`'s expectations if the groupId/artifactId ever change
too.
