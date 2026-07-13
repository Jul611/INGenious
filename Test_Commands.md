# CLI Enhancements — Test Commands

Run **everything from `Dist/release/`**.

---

## 2. CLI Overrides — `config prefixes` + typed flags

### `config prefixes`

```bash
ingenious config prefixes
```

**Verify**: Lists all 16 prefix namespaces. Each entry has `prefix` + description.

---

### Typed override flags on `run` subcommands

```bash
ingenious run testcase --help
ingenious run testset --help
ingenious run tags --help
ingenious run rerun --help
```

**Verify**: Each shows all 14 typed override flags: `--set-env`, `--driver`, `--user`, `--tm`, `--capability`, `--db`, `--context`, `--api`, `--kafka-ssl`, `--lambdatest-cap`, `--browser-arg`, `--browser-set`, `--device`, `--tm-module`.

---

## 3. `project validate` — Quality Dashboard

```bash
ingenious project validate CLIDemo
ingenious project validate Tutorial
```

**Verify**:
- **Inventory** — scenarios, test cases, reusable scenarios/components, releases, test sets, tagged count
- **Formats** — OR format (YAML/XML/Mixed), test-case format (YAML/CSV/Mixed)
- **Scores** — 6 dimension bars (OR modernisation, TC modernisation, Modularity, Data parameterisation, Test-set coverage, Tag adoption) each with `XX/100`
- **Overall** — Grade A–F with score bar
- **Per-Test-Case Quality table** — columns: Scenario/Test Case, Kind, Steps, Reusable%, Param%, Tagged
- **Per-Reusable-Component Quality table** — columns: Scenario/Reusable, Kind, Steps, Param%, Calls reuse
- **Kind column** — shows `UI`, `API`, `Mobile`, `DB`, `Kafka`, combined (`UI + API`), or `Unknown`

Flags:
```bash
ingenious project validate CLIDemo --no-detail    # summary only, no TC tables
ingenious project validate CLIDemo --strict        # warnings → exit code 1
```

---

## 4. `project upgrade` — 4-Step Interactive Wizard

```bash
ingenious project upgrade CLIDemo --dry-run        # preview only, no changes
```

**Verify**: Pre-flight scan shows findings (Legacy OR XML, CSV test cases, Deprecated files, Mislocated reusables). Each step prompts `[y/N]`.

```bash
ingenious project upgrade CLIDemo -y               # unattended, accepts all defaults
```

**Verify 4 steps execute**:
1. OR XML → YAML conversion
2. CSV test cases → YAML conversion
3. Deprecated file cleanup (`ReusableComponent.xml.bak`, `ProjectXMLOR/`, `SharedXMLOR/`)
4. Mislocated reusables moved from `TestPlan/` → `ReusableComponents/`

```bash
ingenious project upgrade CLIDemo -y --keep-backup  # keep CSV originals under .migration-backup/
```

---

## 5. Test-Set Coverage — Correct Scoring

```bash
ingenious project validate CLIDemo
```

**Verify**:
- Test-set coverage score reflects **test-case granularity** (not scenario-level)
- Uncovered test cases listed under **Warnings**: `⚠ Test case not in any test set: Scenario/TestCase`

CLIDemo has **6 test cases** across 2 scenarios (`APIBasics`, `DataDriven`). Both test sets (`Smoke`, `Regression`) combined cover all 6 → score should be `100/100` for full coverage.

### To see partial credit

Create an uncovered test case, then re-validate:
```bash
mkdir -p Projects/CLIDemo/TestPlan/Orphan

# Paste the following YAML into Projects/CLIDemo/TestPlan/Orphan/Unlinked.yaml:
# schemaVersion: 1
# name: Unlinked
# scenario: Orphan
# steps: []

ingenious project validate CLIDemo
```

**Verify now**:
- **Test-set coverage** drops to partial credit (e.g. `86/100` — 6 out of 7 TCs covered)
- **Warning** lists: `⚠ Test case not in any test set: Orphan/Unlinked`

---

## 6. CLI Dispatcher — `object` / `testset` / `data` routing

```bash
ingenious object list --help
ingenious objects --help
ingenious or --help
ingenious testset list --help
ingenious data list --help
```

**Verify**: All show **Picocli output** (coloured text, usage synopsis, option/subcommand listing).

| Output style | Means |
|---|---|
| Coloured error/usage with option tables | ✅ Picocli — dispatcher **working** |
| `"xxx" not Implemented` (plain text, no colour) | ❌ Legacy CLI — dispatcher **broken** |

The five command words (`object`, `objects`, `or`, `testset`, `data`) now route to Picocli instead of falling through to the legacy Apache Commons CLI parser.
