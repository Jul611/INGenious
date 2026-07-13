# INGenious — CLI Enhancements & Validation Improvements

## Table of Contents

1. [CLI override coverage audit + full CLI usagee-docs
2. [CLI overrides — implementation of all missing prefixes + typed flags + `config prefixes` help](#2-cli-overridese` — quality dashboard with per-test-case & pereusable-kind-classification
4. [`project upgrade` — 4-step interactive modernisationwizard
5. [`validate` Test-set coverage — correct scoring at test-case granularity](#5CLI dispatcher — `object` / `testset` / `data` now route to picocli](#6-source CLI reference documentnt

---

## 1. CLI override coverage audit + full CLI usage docs

### Goal
Produce a single source of truth for CLI-configurable settings, override coverage, and command usage.

### Deliverable
New repository document: **`CLI_Override_Plan_And_Usage.md`**

#### Part A — Existing override architecture
All `-setEnv "<prefix>.<key>=<value>"` processing routes through:

- `ProjectRunner.overrideWithEnv()`
- `Engine/src/main/java/com/ing/engine/execution/run/ProjectRunner.java`

Existing prefixes documented:

- `exe`
- `run`
- `user`
- `tm`
- `driver`
- `capability.<browser>`
- `db.<db>`
- `context.<ctx>`
- `api.<api>`
- `kafkaSSl`

#### Part B — Override coverage matrix
Audit of all Settings and Archetype Configuration areas.

Previously missing override support identified for:

1. LambdaTest Grid Capabilities
2. Manage Browsers
3. Manage Devices / Mobile
4. Azure DevOps TestPlan Modules
5. Azure DevOps module-specific settings

Includes:

- Proposed namespaces
- Dispatcher integration points
- Typed CLI flag designs
- Validation and regression-test plans
- `config prefixes` helper proposal
- `kafkaSSl` → `kafkaSsl` standardisation plan

#### Part C — CLI usage reference
Complete command and flag documentation covering:

- Global options
- Modern Picocli commands
- Legacy CLI compatibility
- Override syntax
- Recipes
- Exit codes

### Breaking changes
None.

---

## 2. CLI overrides — implementation of all missing prefixes + typed flags + `config prefixes` help

### Goal
Provide CLI override support for every configuration area using a consistent model.

### What changed

#### New override prefixes

`ProjectRunner.overrideWithEnv()` was refactored and extended with support for:

- `lambdatest.<key>`
- `browser.<browser>.<key>`
- `browserArg.<browser>.<n>`
- `device.<name>.<key>`
- `emulator.<name>.<key>` (alias)
- `tmModule.<module>.<key>`
- `kafkaSsl.<key>` (canonical alias)

A central:

```java
PREFIX_CATALOGUE
```

maps all supported prefixes and powers CLI help output.

Additional improvements:

- Safer key parsing
- Automatic capability creation
- Browser capability initialisation without null-pointer risks

#### Supporting Datalib additions

Added helper methods:

- `Capabilities.getOrCreateCapabiltiesFor(...)`
- `Devices.getOrCreateDevice(...)`
- `TestMgmtModule.setOption(...)`

#### Typed Picocli override flags

New reusable mixin:

```java
OverrideOptions
```

Provides repeatable flags such as:

- `--set-env`
- `--driver`
- `--user`
- `--tm`
- `--capability`
- `--browser-set`
- `--browser-arg`
- `--db`
- `--context`
- `--api`
- `--kafka-ssl`
- `--lambdatest-cap`
- `--device`
- `--tm-module`

All generate standard in-memory environment overrides.

#### Run command integration

The mixin is now available across:

- `run testcase`
- `run testset`
- `run tags`
- `run rerun`

ensuring a single implementation path for all runtime overrides.

#### New help command

```bash
ingenious config prefixes
```

Lists all supported namespaces directly from `PREFIX_CATALOGUE`.

### Override precedence

```text
Project Files
      ↓
app.* Environment Variables
      ↓
-setEnv / Typed Override Flags
```

Overrides remain runtime-only and never modify project files.

### Verification

```bash
ingenious config prefixes
ingenious run testcase --help
ingenious run testset --help
ingenious run tags --help
ingenious run rerun --help
```

### Breaking changes
None.

---

## 3. `project validate` — quality dashboard with per-test-case & per-reusable Kind classification

### Feature

New command:

```bash
ingenious project validate <project>
```

Provides:

- Overall project health score
- A–F grade
- Six quality dimensions
- Test-case quality table
- Reusable-component quality table
- Kind classification

### Kind classification

Kinds include:

- `UI`
- `API`
- `Mobile`
- `DB`
- `Kafka`
- Combined archetypes (e.g. `UI + API`)
- `Unknown`

Classification is determined from:

- Step actions
- Built-in objects
- Object Repository references

### Implementation highlights

#### TestCaseQuality

Tracks:

- Web steps
- API steps
- Mobile steps
- DB steps
- Kafka steps
- Reuse metrics
- Parameterisation metrics

#### OR indexing

A cached OR lookup index scans:

- Web OR
- Mobile OR
- Structured Data OR
- SAP OR
- Shared repositories

Index includes:

- Page names
- Object-group names
- Object names

#### Reusable propagation

Reusable archetypes are merged into parent test-case metrics so Kind reflects the entire execution flow.

### Validation output

Example:

```text
Per-Reusable-Component Quality
──────────────────────────────
StepDefinitions/User fills up personal ... UI
StepDefinitions/submits relevant question UI
```

### Breaking changes
None.

---

## 4. `project upgrade` — 4-step interactive modernisation wizard

### Feature

New command:

```bash
ingenious project upgrade <project>
```

Interactive upgrade workflow with:

- `--yes`
- `--dry-run`
- `--keep-backup`

### Upgrade steps

#### 1. XML Object Repository → YAML

Converts legacy repositories using:

```java
ObjectRepository.saveAsYaml()
```

Original files are archived automatically.

#### 2. CSV Test Cases → YAML

Uses:

```java
ProjectMigrator.migrate(...)
```

Reports:

- Converted
- Skipped
- Conflicts
- Errors

#### 3. Legacy file cleanup

Removes deprecated files such as:

- `IOR.object`
- `ReusableComponent.xml`
- `ReusableComponent.xml.bak`
- `ProjectXMLOR`
- `SharedXMLOR`

#### 4. Reusable relocation

Moves reusable YAML files from:

```text
TestPlan/
```

to:

```text
ReusableComponents/
```

while preserving structure.

### Additional capabilities

- Project auto-discovery
- Preview mode
- Idempotent execution
- Interactive confirmation prompts

### Examples

```bash
ingenious project upgrade ING-Public-Web --dry-run

ingenious project upgrade ING-Public-Web --keep-backup

ingenious project upgrade ING-Public-Web -y
```

### Breaking changes
None.

---

## 5. `validate` Test-set coverage — correct scoring at test-case granularity

### Problem 1

Projects with complete test-set coverage were capped below 100%.

#### Root cause

Test-set execution rows were lazily loaded.

#### Fix

Validation now calls:

```java
ts.loadTestSetTableModel()
```

before evaluating coverage.

---

### Problem 2

Coverage was calculated at scenario level rather than test-case level.

#### Previous logic

```text
Covered Scenarios / Total Scenarios
```

#### New logic

```text
Covered Test Cases / Total Test Cases
```

Coverage now tracks:

```java
scenario/testcase
```

keys.

### Updated scoring

```java
private static int scoreTestSets(
        int totalTestSets,
        int totalTestCases,
        Set<String> testCasesInTestSets)
```

Produces partial-credit behaviour.

### Examples

Partial coverage:

```text
75/100
```

Full coverage:

```text
100/100
```

### Additional diagnostics

Uncovered test cases are reported:

```text
⚠ Test case not in any test set:
Contact Us/User should be able to send a question
```

### Breaking changes

Coverage scores now reflect real test-case-level coverage.

---

## 6. CLI dispatcher — `object` / `testset` / `data` now route to picocli

### Symptom

Commands such as:

```bash
ingenious object list -p <path>
ingenious testset list -p <path>
ingenious data list -p <path>
```

incorrectly routed to the legacy CLI.

### Root cause

The Picocli dispatcher whitelist omitted:

- `object`
- `objects`
- `or`
- `testset`
- `data`

### Fix

Added missing command families to:

```java
Control.isNewCLICommand(...)
```

### Result

The following command groups now correctly use Picocli:

- object
- objects
- or
- testset
- data

### Breaking changes
None.

---

## 7. Single-source CLI reference document

### Deliverable

New repository document:

```text
INGenious_CLI_Reference.md
```

### Contents

1. Global flags
2. Project commands
3. Scenario commands
4. Test Case commands
5. Test Set commands
6. Object commands
7. Data commands
8. Action commands
9. Run commands
10. Report commands
11. Config commands
12. Server commands
13. Shell / REPL
14. Legacy CLI mappings
15. Quick recipes

### Additional coverage

- Validation scoring details
- Kind classification legend
- Override prefix references
- Representative command outputs
- Modern vs legacy command comparisons

### Purpose

Acts as the primary day-to-day CLI command guide, complementing:

```text
CLI_Override_Plan_And_Usage.md
```

which remains focused on override mechanisms and integration details.

### Breaking changes
None.