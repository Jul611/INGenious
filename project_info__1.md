To delete Chromium binaries so you can test the install:

**PowerShell (Windows):**
```powershell
Get-ChildItem "$env:USERPROFILE\AppData\Local\ms-playwright" -Directory -Name | Select-String "^chromium-"
```
That will show you the Chromium folder name (e.g. `chromium-1155`). Then delete it:

```powershell
Remove-Item "$env:USERPROFILE\AppData\Local\ms-playwright\chromium-*" -Recurse -Force
```

**Then to test the targeted install (only Chromium + WebKit, no Firefox):**

Via the Maven profile:
```powershell
mvn exec:java@install-browsers -f Engine/pom.xml
```

Or via the Node CLI (from the `Resources/` directory):
```powershell
cd Resources
npx playwright install chromium webkit
```

Both will download Chromium and WebKit but **not** Firefox. The `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` env var in `PlaywrightDriverFactory.java` also ensures the runtime won't auto-download anything.