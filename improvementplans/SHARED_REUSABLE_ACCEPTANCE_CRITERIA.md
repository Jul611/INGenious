# Shared Reusable Components - Acceptance Criteria

## Scenario 1: Reference Resolution with Source Specification

### Requirement
Users should be able to refer to the correct reusable and test case (both project and shared). The source should appear in the reference column.

### Execute Step Formats

#### Format 1: Default (No Source Specified)
```
Execute Scenario:TestCase
```
**Behavior**: 
- First check project-level reusable components
- If not found, check shared reusable components
- If still not found, throw error

#### Format 2: Explicit Project Source
```
Execute Project:Scenario:TestCase
```
**Behavior**: 
- Only check project-level reusable components
- Throw error if not found

#### Format 3: Explicit Shared Source
```
Execute Shared:Scenario:TestCase
```
**Behavior**: 
- Only check shared reusable components
- Throw error if not found

### Test Cases

#### TC1: Reusables from Project Only
**Given**: 
- Project has reusable: `Login:ValidLogin`
- Shared does not have this reusable

**Test Steps**:
| Object | Action | Input | Reference |
|--------|--------|-------|-----------|
| Execute | Login:ValidLogin | | Project |
| Execute | Login:ValidLogin | | |

**Expected**: Both execute successfully from project reusables

#### TC2: Reusables from Shared Only
**Given**: 
- Shared has reusable: `Common:Navigation`
- Project does not have this reusable

**Test Steps**:
| Object | Action | Input | Reference |
|--------|--------|-------|-----------|
| Execute | Common:Navigation | | Shared |
| Execute | Common:Navigation | | |

**Expected**: Both execute successfully from shared reusables

#### TC3: Mixed - Both Project and Shared
**Given**: 
- Project has reusable: `Login:ValidLogin`
- Shared has reusable: `Common:Navigation`
- Both have reusable: `Search:BasicSearch` (different implementations)

**Test Steps**:
| Object | Action | Input | Reference |
|--------|--------|-------|-----------|
| Execute | Login:ValidLogin | | Project |
| Execute | Common:Navigation | | Shared |
| Execute | Search:BasicSearch | | Project |
| Execute | Search:BasicSearch | | Shared |
| Execute | Search:BasicSearch | | |

**Expected**: 
- First 4 execute from specified source
- Last one executes from project (default priority)

#### TC4: Same Names in Project and Shared
**Given**: 
- Project has: `Login:ValidLogin` (version 1)
- Shared has: `Login:ValidLogin` (version 2)

**Test Steps**:
| Object | Action | Input | Reference |
|--------|--------|-------|-----------|
| Execute | Login:ValidLogin | | |
| Execute | Login:ValidLogin | | Project |
| Execute | Login:ValidLogin | | Shared |

**Expected**: 
- First executes version 1 (project priority)
- Second executes version 1 (explicit)
- Third executes version 2 (explicit)

---

## Scenario 2: Validation - Invalid Characters

### Requirement
Error for special characters (comma, dot, etc) in scenario and test case names.

### Invalid Characters
- `,` (comma) - conflicts with CSV format
- `.` (dot) - conflicts with file extensions
- `/` (forward slash) - conflicts with file paths
- `\` (backslash) - conflicts with file paths
- `:` (colon) - conflicts with Execute format
- `*` (asterisk) - conflicts with wildcards
- `?` (question mark) - conflicts with wildcards
- `"` (quote) - conflicts with CSV format
- `<` `>` (angle brackets) - conflicts with file systems
- `|` (pipe) - conflicts with file systems

### Valid Characters
- Letters: `a-z`, `A-Z`
- Numbers: `0-9`
- Underscore: `_`
- Hyphen: `-`
- Space: ` `

### Test Cases

#### TC1: Create Scenario with Invalid Name
**Action**: Create scenario named `Test,Scenario`

**Expected**: Error message: "Scenario name cannot contain special characters: , . / \ : * ? \" < > |"

#### TC2: Create TestCase with Invalid Name
**Action**: Create test case named `Test.Case`

**Expected**: Error message: "TestCase name cannot contain special characters: , . / \ : * ? \" < > |"

#### TC3: Rename to Invalid Name
**Action**: Rename existing scenario to `Test:Scenario`

**Expected**: Error message and rename cancelled

---

## Scenario 3: Promote to Shared Reusable

### Requirement
Users should be able to make the project reusable scenario and test case as shared reusable.

### Use Cases

#### UC1: Promote to New Shared Scenario
**Given**: 
- Project has: `ReusableComponents/Login/ValidLogin.csv`
- Shared does not have `Login` scenario

**Action**: Right-click `Login:ValidLogin` → "Make As Shared Reusable"

**Expected**:
- File copied to: `SharedReusableComponents/Login/ValidLogin.csv`
- Original remains in project
- Shared reusable tree refreshed
- Success notification shown

#### UC2: Promote to Existing Shared Scenario
**Given**: 
- Project has: `ReusableComponents/Login/AdminLogin.csv`
- Shared already has: `SharedReusableComponents/Login/` (with other test cases)

**Action**: Right-click `Login:AdminLogin` → "Make As Shared Reusable"

**Expected**:
- File copied to: `SharedReusableComponents/Login/AdminLogin.csv`
- Merged into existing scenario folder
- Original remains in project
- Success notification shown

#### UC3: Conflict - TestCase Already Exists in Shared
**Given**: 
- Project has: `ReusableComponents/Login/ValidLogin.csv`
- Shared has: `SharedReusableComponents/Login/ValidLogin.csv`

**Action**: Right-click `Login:ValidLogin` → "Make As Shared Reusable"

**Expected**:
- Confirmation dialog: "Shared reusable 'Login:ValidLogin' already exists. Overwrite?"
- If Yes: Overwrite
- If No: Cancel operation

#### UC4: Promote Entire Scenario
**Given**: 
- Project has: `ReusableComponents/Login/` with 5 test cases

**Action**: Right-click `Login` scenario → "Make As Shared Reusable"

**Expected**:
- All test cases copied to shared
- Existing shared scenario merged (not replaced)
- Conflict resolution for each duplicate
- Summary shown: "Promoted 5 test cases to shared (3 new, 2 skipped)"

---

## Scenario 4: Missing Test Datasheet Highlighting

### Requirement
Missing test datasheet should be highlighted in red.

### Implementation Details

#### UI Indicators
- **Test Step Row**: If `Input` column references `@Sheet.Column` and `Sheet` doesn't exist
  - Background color: Light red (#FFCCCC)
  - Tooltip: "Test data sheet 'Sheet' not found"
  
- **Test Case Tree**: If test case has missing data references
  - Icon overlay: Red warning icon
  - Tooltip: "Contains missing test data references"

### Test Cases

#### TC1: Valid Data Reference
**Given**: Test data sheet `Users` exists

**Test Step**:
| Object | Action | Input |
|--------|--------|-------|
| txtUsername | input | @Users.username |

**Expected**: Normal rendering (no red highlight)

#### TC2: Missing Data Sheet
**Given**: Test data sheet `InvalidSheet` does NOT exist

**Test Step**:
| Object | Action | Input |
|--------|--------|-------|
| txtUsername | input | @InvalidSheet.username |

**Expected**: Row highlighted in red with tooltip

#### TC3: Missing Column (Valid Sheet)
**Given**: Test data sheet `Users` exists but has no column `invalidColumn`

**Test Step**:
| Object | Action | Input |
|--------|--------|-------|
| txtUsername | input | @Users.invalidColumn |

**Expected**: Row highlighted in orange (warning) with tooltip: "Column 'invalidColumn' not found in 'Users'"

#### TC4: Mixed References
**Test Steps**:
| Object | Action | Input |
|--------|--------|-------|
| txtUsername | input | @Users.username |
| txtPassword | input | @InvalidSheet.password |
| btnLogin | click | |

**Expected**: 
- Row 1: Normal
- Row 2: Red highlight
- Row 3: Normal

---

## Implementation Checklist

### Data Layer (Datalib)
- [x] Add `Source.SHARED_REUSABLE_COMPONENTS` to Scenario enum
- [x] Add `getSharedReusableComponentsPath()` to Project
- [x] Add `loadScenariosFromSharedReusableComponents()` to Project
- [x] Add `moveTestCaseToSharedReusable()` to Project
- [ ] Add name validation utility for scenarios/test cases
- [ ] Add conflict detection for promote operations

### Engine Layer
- [x] Add `getSharedReusableComponentsPath()` to FilePath
- [ ] Update TestStepRunner to parse source prefix (Project/Shared)
- [ ] Update reusable resolution logic with source awareness
- [ ] Add logging for source resolution

### IDE Layer
- [ ] Implement SharedReusableTree
- [ ] Implement SharedReusableTreeModel
- [ ] Add "Make As Shared Reusable" context menu
- [ ] Add "Copy to Shared Reusable" action
- [ ] Add source column display in test step table
- [ ] Add validation for invalid characters in names
- [ ] Implement red highlighting for missing test data
- [ ] Add tooltips for missing data references

### Testing
- [ ] Unit tests for source resolution logic
- [ ] Integration tests for promote operations
- [ ] UI tests for red highlighting
- [ ] End-to-end tests for all scenarios

---

## Error Messages

### Missing Reusable
```
Reusable test case not found: 'Scenario:TestCase'
Searched in:
- Project reusable components
- Shared reusable components
```

### Missing Reusable (Explicit Source)
```
Reusable test case not found in [Project/Shared]: 'Scenario:TestCase'
```

### Invalid Name
```
Invalid name: 'Test,Case'
Names cannot contain special characters: , . / \ : * ? " < > |
Use letters, numbers, spaces, underscores, and hyphens only.
```

### Promote Conflict
```
Shared reusable 'Scenario:TestCase' already exists.
Do you want to overwrite it?
[Yes] [No] [Compare]
```
