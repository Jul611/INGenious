# Code Changes Summary

## Overview
**Date**: February 12, 2026  
**Total Files Changed**: 9 files  
**Primary Focus**: Integration of Lambda Test cloud execution support with configuration UI and capabilities management

---

## Detailed Changes

### 1. ProjectSettings.java
**Location**: `Datalib/src/main/java/com/ing/datalib/settings/ProjectSettings.java`

- Added `LambdaTestCaps` field declaration
- Initialized `lambdaTestCaps` in constructor
- Added `lambdaTestCaps.setLocation()` call in `resetLocation()` method
- Added `getLambdaTestCaps()` getter method
- Added `lambdaTestCaps.save()` in `save()` method

**Purpose**: Enable project-level Lambda test capabilities configuration

---

### 2. RunSettings.java
**Location**: `Datalib/src/main/java/com/ing/datalib/settings/RunSettings.java`

**Change**:
```
getRemoteGridURL() default value changed from:
  "http://localhost:4444/wd/hub"
to:
  "wss://cdp.lambdatest.com"
```

**Purpose**: Update default remote grid URL to point to Lambda Test cloud platform

---

### 3. Webservice.java
**Location**: `Engine/src/main/java/com/ing/engine/commands/webservice/Webservice.java`

**Change**: Added HTTP redirect handling
```java
httpClient.put(key, httpClientBuilder.get(key).followRedirects(HttpClient.Redirect.ALWAYS).build());
```

**Purpose**: Enable automatic following of HTTP redirects in web service requests

---

### 4. Control.java
**Location**: `Engine/src/main/java/com/ing/engine/core/Control.java`

**Changes**:
- Added import: `java.time.Instant`
- Added static field: `String executionStartTime`
- Initialized `executionStartTime = String.valueOf(Instant.now())` in `initRun()` method

**Purpose**: Track execution start timestamp for Lambda test build identification

---

### 5. Task.java
**Location**: `Engine/src/main/java/com/ing/engine/core/Task.java`

**Changes**:
- Added imports:
  - `com.microsoft.playwright.Browser`
  - `com.microsoft.playwright.BrowserContext`
  - `java.io.UnsupportedEncodingException`
  
- Updated `run()` method:
  - Added Lambda test status updates based on test result
  - Sets "passed" or "failed" status via `setLambdaStatus()`
  
- Updated `closePlaywrightDriver()`:
  - Added check for grid execution: `!Control.exe.getExecSettings().getRunSettings().isGridExecution()`
  
- Updated method signatures:
  - `launchPlaywright()` now throws `UnsupportedEncodingException`
  
- Added new method: `setLambdaStatus(String status, String remark)`
  - Sends Lambda test status updates via JavaScript execution

**Purpose**: Enable Lambda test integration for remote execution and test status reporting

---

### 6. PlaywrightDriverCreation.java
**Location**: `Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverCreation.java`

**Changes**:
- Added import: `java.io.UnsupportedEncodingException`
- Updated method signatures to throw `UnsupportedEncodingException`:
  - `launchDriver(RunContext context)`
  - `launchDriver(String browser)`
  - `RestartBrowser()`
  - `StartBrowser(String b)`
  
- Modified `StopBrowser()` method:
  - Added proper browser instance retrieval before closing
  - Ensures correct cleanup order: page → context → browser

**Purpose**: Properly handle encoding in driver initialization and cleanup for cloud execution

---

### 7. PlaywrightDriverFactory.java
**Location**: `Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverFactory.java`

**Changes**:
- Added imports:
  - `com.google.gson.JsonObject`
  - `java.io.UnsupportedEncodingException`
  - `java.net.URLEncoder`
  - `java.util.HashMap`
  - `java.util.Map`
  
- Modified `createPlaywright()`:
  - Now supports environment variable configuration
  - Creates Playwright with custom environment settings
  
- Updated `createContext()` signature:
  - Now throws `UnsupportedEncodingException`
  
- Added grid execution support:
  - Constructs Lambda Test CDP URL with encoded capabilities
  - Format: `wss://cdp.lambdatest.com/playwright?capabilities=<encoded_json>`
  
- Added new method: `lambdaTestCapabilities(RunContext context, List<String> caps)`
  - Builds JSON capabilities object for Lambda test
  - Includes: browser, version, platform, test name, build, user, access key
  - Supports: video, console logging, network logging, resolution, visual testing, tunneling, geolocation
  
- Added helper method: `getLambdaTestCap(String property)`
  - Retrieves Lambda test capabilities from project settings

**Capability Properties**:
- `browserName`: Playwright browser type (pw-chromium, pw-firefox, pw-webkit, Chrome, Edge)
- `browserVersion`: Browser version or "latest"
- `platform`: OS platform mapping
- `name`: Test case name
- `build`: Build identifier (auto-generated from scenario + execution timestamp)
- `user`: Lambda test username
- `accessKey`: Lambda test access key
- `video`: Video recording enabled
- `console`: Console log capture
- `network`: Network logging
- `resolution`: Screen resolution
- `visual`: Visual testing
- `tunnel`: Tunnel support
- `tunnelName`: Specific tunnel name
- `geoLocation`: Geolocation for testing
- `idleTimeout`: Timeout in seconds
- `useSpecificBundleVersion`: Use specific bundle version

**Purpose**: Enable cloud-based browser execution on Lambda Test platform with full capability configuration

---

### 8. LambdaTestCaps.java (NEW FILE)
**Location**: `Datalib/src/main/java/com/ing/datalib/settings/LambdaTestCaps.java`

**Content**: New class extending `AbstractPropSettings`

**Default Properties**:
```
- build: "" (empty, auto-populated)
- user: "" (Lambda test username)
- accessKey: "" (Lambda test access key)
- video: "true"
- console: "true"
- network: "true"
- resolution: "1920x1080"
- visual: "true"
- tunnel: "false"
- tunnelName: ""
- geoLocation: ""
- idleTimeout: "300"
- useSpecificBundleVersion: "false"
```

**Purpose**: Centralized management of Lambda test capabilities as project settings

---

### 9. INGeniousSettings.java
**Location**: `IDE/src/main/java/com/ing/ide/main/settings/INGeniousSettings.java`

**Changes**:
- Added field: `private XTablePanel lambdatestCapsPanel`
- Added tab in settings UI: "LambdaTest Capabilities"
- Added method: `loadLambdaTestCapabilities()`
  - Loads Lambda test capabilities from project settings into UI table
  
- Added method: `saveLambdaTestCaps()`
  - Saves Lambda test capabilities from UI table to project settings
  - Includes encryption handling
  
- Updated `loadSettings(ExecutionSettings execSettings)`:
  - Added `loadLambdaTestCapabilities()` call
  
- Updated `loadAll()`:
  - Added `loadLambdaTestCapabilities()` call
  
- Updated `saveAll()`:
  - Added `saveLambdaTestCaps()` call

**Purpose**: Provide UI interface for viewing and editing Lambda test capabilities in IDE settings

---

## Technology Stack Impact

### New Dependencies/Technologies
- **Google Gson**: JSON serialization/deserialization for Lambda test capabilities
- **Lambda Test CDP**: Cloud execution platform via WebSocket protocol (wss://)
- **Java URL Encoding**: For capability encoding in CDP URLs

### Modified Execution Flows
1. **Grid Execution**: When enabled, creates remote browser context via Lambda Test CDP
2. **Test Status Reporting**: Automatically reports pass/fail status to Lambda test
3. **Build Tracking**: Uses execution start timestamp to identify test builds

---

## Configuration Notes

### Lambda Test Setup Requirements
Users must configure the following in LambdaTest Capabilities settings:
- `user`: Lambda test username
- `accessKey`: Lambda test access key
- `build`: Build name (optional, auto-generated if empty)

### Execution Modes
- **Local Execution**: Uses direct Playwright browser launch (existing behavior)
- **Grid Execution**: Uses Lambda Test CDP remote browser execution (new)

### Remote Grid URL
Default format: `wss://cdp.lambdatest.com/playwright?capabilities=<encoded_json>`

---

## Files Summary Table

| File | Type | Changes |
|------|------|---------|
| ProjectSettings.java | Modified | Added LambdaTestCaps integration |
| RunSettings.java | Modified | Updated default remote URL |
| Webservice.java | Modified | Added redirect handling |
| Control.java | Modified | Added execution timestamp tracking |
| Task.java | Modified | Added Lambda status reporting |
| PlaywrightDriverCreation.java | Modified | Updated exception handling |
| PlaywrightDriverFactory.java | Modified | Added Lambda test CDP support |
| LambdaTestCaps.java | **NEW** | Lambda test configuration class |
| INGeniousSettings.java | Modified | Added Lambda test settings UI |

