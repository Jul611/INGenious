# Shared Reusable Components Migration and Enhancement Plan

## Executive Summary
Migrate the Shared Reusable Components POC from INGenious v2.3 (branch: task/8431075-shared-reusable) to v3.0 with enhancements aligned to the modern filesystem-based architecture.

## Background

### Current State (v3.0)
- **ReusableComponents**: Directory-based approach for project-level reusables
- **Location**: `<Project>/ReusableComponents/`
- **Scope**: Within a single project only
- **Structure**: Similar to TestPlan with scenario folders containing test case CSVs

### POC State (v2.3)
- **SharedReusables**: XML-based approach for cross-project reusables
- **Location**: `<user.dir>/Shared/SharedReusableComponents/`
- **Configuration**: `SharedReusableComponents.xml` for metadata
- **Scope**: Shared across all projects

### Goal (v3.0 Enhanced)
Implement shared reusable components that:
1. Can be used across multiple projects
2. Follow the filesystem-based architecture (no XML metadata)
3. Support both project-level and shared-level reusables
4. Provide clear UI distinction between local and shared reusables
5. Handle promotion/demotion between TestCase → Reusable → Shared Reusable

## Architecture Design

### Directory Structure
```
<Workspace Root>/
├── Projects/
│   ├── Project1/
│   │   ├── TestPlan/
│   │   │   └── Scenario1/
│   │   │       └── TestCase1.csv
│   │   └── ReusableComponents/
│   │       └── Scenario2/
│   │           └── TestCase2.csv (Project-level reusable)
│   └── Project2/
│       ├── TestPlan/
│       └── ReusableComponents/
└── SharedReusableComponents/
    └── Scenario3/
        └── TestCase3.csv (Shared reusable - available to all projects)
```

### Data Model Changes

#### 1. Scenario.java
- Add new source type: `SHARED_REUSABLE_COMPONENTS`
- Update `Source` enum:
  ```java
  public enum Source {
      TEST_PLAN,
      REUSABLE_COMPONENTS,
      SHARED_REUSABLE_COMPONENTS
  }
  ```
- Update `getLocation()` to handle shared path

#### 2. Project.java
- Add field: `private final List<Scenario> sharedReusableScenarios = new ArrayList<>()`
- Add constant: `public static final String SHARED_REUSABLE_DIR = "SharedReusableComponents"`
- Add method: `public String getSharedReusableComponentsPath()`
- Add method: `private Boolean loadScenariosFromSharedReusableComponents()`
- Add method: `public List<Scenario> getSharedReusableScenarios()`
- Add method: `public Scenario getSharedReusableScenarioByName(String name)`
- Add method: `public Scenario addSharedReusableScenario(String scenarioName)`
- Update `getAllScenarios()` to include shared reusables
- Add methods for promoting/demoting test cases:
  - `moveTestCaseToSharedReusable(TestCase testCase)`
  - `moveTestCaseFromSharedReusable(TestCase testCase)`

#### 3. TestCase.java
- Keep existing `isReusable()` method
- Add method: `public boolean isSharedReusable()`
- Add method: `public String getSharedReusableLocation()`
- Update `loadSteps()` to check shared location if not found locally
- Add toggle methods for shared reusable status

#### 4. FilePath.java (Engine)
- Add constant: `private final static String SHARED_REUSABLE = "SharedReusableComponents"`
- Add method: `public static String getSharedReusableComponentsPath()`

### IDE Changes

#### 1. TestDesign.java
- Add field: `private final SharedReusableTree sharedReusableTree`
- Initialize in constructor
- Add getter: `getSharedReusableTree()`
- Update `load()` to load shared reusable tree
- Update `save()` to save shared reusable tree

#### 2. TestDesignUI.java
- Add panel for Shared Reusables (third panel in split)
- Update layout to accommodate three trees:
  - TestPlan (top)
  - Reusable Components (middle)
  - Shared Reusable Components (bottom)
- Or use tabbed interface for Reusable vs Shared Reusable

#### 3. SharedReusableTree.java (New)
- Extend `ProjectTree`
- Use `SharedReusableTreeModel`
- Support CRUD operations on shared reusables
- Support drag-and-drop from/to other trees
- Context menu for promote/demote operations

#### 4. SharedReusableTreeModel.java (New)
- Extend `ProjectTreeModel`
- Load scenarios from shared location
- Support grouping (similar to v2.3 POC groups/folders)
- Save/load structure (filesystem-based, no XML)

#### 5. Tree Actions/Context Menus
- **TestPlan TestCase**: Right-click → "Make As Reusable" or "Make As Shared Reusable"
- **Reusable TestCase**: Right-click → "Make As TestCase" or "Make As Shared Reusable"
- **Shared Reusable TestCase**: Right-click → "Make As Reusable" or "Make As TestCase"

### Engine Changes

#### 1. TestCaseRunner.java
- Update reusable step resolution to check:
  1. Project-level reusable scenarios
  2. Shared reusable scenarios
- Add logging to indicate when shared reusables are executed

#### 2. Control.java
- Load shared reusable components at startup
- Make shared reusables available to all test executions

### Migration Strategy

#### Auto-Migration from v2.3 POC
If `SharedReusableComponents.xml` exists:
1. Parse XML to extract scenario/test case structure
2. Move CSV files to new filesystem structure
3. Remove grouping metadata (use flat structure or folder-based grouping)
4. Rename XML to `.bak` for reference

#### Manual Migration Support
Provide utility to:
1. Promote existing reusable components to shared
2. Import shared reusables from another workspace
3. Export shared reusables for distribution

### Validation and Constraints

#### Naming Uniqueness
- Shared reusable scenario + test case combinations must be globally unique across the workspace
- Enforce at creation time
- Validate on project load

#### Loading Priority
When resolving `Execute Scenario:TestCase`:
1. Check project-level reusable scenarios first
2. Then check shared reusable scenarios
3. Throw clear error if not found in either

#### Versioning
- Consider adding version metadata for shared reusables (future enhancement)
- For now, rely on filesystem and version control

### User Interface Enhancements

#### Panel Layout Option 1: Triple Split
```
┌─────────────────────────────────┐
│ TestPlan                        │
├─────────────────────────────────┤
│ Reusable Components             │
├─────────────────────────────────┤
│ Shared Reusable Components      │
└─────────────────────────────────┘
```

#### Panel Layout Option 2: Tabbed
```
┌─────────────────────────────────┐
│ TestPlan                        │
├─────────────────────────────────┤
│ [Reusable] [Shared Reusable]    │
│ ┌─────────────────────────────┐ │
│ │ Reusable Components         │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

**Recommendation**: Triple Split with collapsible panels for better visibility

#### Visual Indicators
- Different icons for shared reusables vs project reusables
- Tooltip showing source location (project vs shared)
- Color coding in test step table when using shared reusables

### Impact Analysis

#### Features Impacted
1. **Loading Projects**: Must load shared reusables from workspace root
2. **Creating New Project**: Ensure shared reusables are accessible
3. **Loops in Shared Reusables**: Support data-driven execution
4. **Using Shared Reusables in Test Datasheet**: Enable data parameterization
5. **BDD/Gherkin**: Support shared step definitions
6. **Object Repository**: Consider shared OR (future enhancement)
7. **Test Execution**: Resolve and execute shared reusable steps
8. **Reporting**: Indicate when shared reusables are used
9. **Refactoring**: Update shared reusables when renaming objects/pages
10. **Search/Find Usages**: Include shared reusables in impact analysis

### Testing Plan

#### Unit Tests
- Test shared scenario loading
- Test promote/demote operations
- Test uniqueness validation
- Test execution resolution

#### Integration Tests
- Test cross-project shared reusable usage
- Test mixed project + shared reusable execution
- Test migration from v2.3 POC

#### UI Tests
- Test tree operations (add, rename, delete)
- Test drag-and-drop between trees
- Test context menus

### Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Naming collisions | High | Enforce global uniqueness with validation |
| Performance with many shared reusables | Medium | Lazy loading, caching |
| Concurrent access to shared files | Medium | File locking, clear ownership model |
| Migration data loss | High | Backup strategy, validation, rollback support |
| Breaking existing reusable references | High | Maintain backward compatibility, phased migration |

### Implementation Phases

#### Phase 1: Core Data Model (Week 1)
- [ ] Update Scenario.java with SHARED_REUSABLE_COMPONENTS source
- [ ] Update Project.java with shared reusable support
- [ ] Update TestCase.java with shared reusable methods
- [ ] Update FilePath.java in Engine

#### Phase 2: IDE Components (Week 2)
- [ ] Implement SharedReusableTree.java
- [ ] Implement SharedReusableTreeModel.java
- [ ] Update TestDesign.java integration
- [ ] Update TestDesignUI.java layout
- [ ] Add context menu actions

#### Phase 3: Engine Integration (Week 3)
- [ ] Update TestCaseRunner.java for shared resolution
- [ ] Update Control.java for shared loading
- [ ] Add execution logging

#### Phase 4: Migration & Utilities (Week 4)
- [ ] Implement v2.3 POC migration
- [ ] Add promote/demote utilities
- [ ] Add validation on load

#### Phase 5: Testing & Documentation (Week 5)
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update user documentation
- [ ] Create tutorial/example

### Success Criteria
1. ✅ Shared reusables can be created and used across projects
2. ✅ UI clearly distinguishes between project and shared reusables
3. ✅ Promotion/demotion works seamlessly
4. ✅ No XML files required (filesystem-based only)
5. ✅ Backward compatible with v2.3 projects (migration supported)
6. ✅ All impacted features identified and tested
7. ✅ Performance acceptable with 100+ shared reusables

### Future Enhancements
1. Shared Object Repository
2. Version control integration for shared components
3. Import/Export shared reusable libraries
4. Dependency tracking and impact analysis
5. Shared reusable marketplace/library
6. Permission-based access control for shared reusables
7. Shared test data components

---

## References
- POC Branch: `task/8431075-shared-reusable`
- Target Branch: `task/shared-rc-merge3.0`
- Related Plan: `REUSABLE_COMPONENTS_IMPLEMENTATION_PLAN.md`
