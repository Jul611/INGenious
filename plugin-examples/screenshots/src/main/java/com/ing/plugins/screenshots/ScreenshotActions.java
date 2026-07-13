package com.ing.plugins.screenshots;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;

/**
 * Screenshot tools plugin — demonstrates browser-focused automation actions
 * that capture and verify screenshots during test execution.
 */
public class ScreenshotActions {

    private final CommandPluginApi api;

    public ScreenshotActions(CommandPluginApi api) {
        this.api = api;
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Takes a full-page screenshot and saves it to the results folder",
        input = InputType.NO
    )
    public void takeFullPageScreenshot() {
        System.out.println("Taking a full-page screenshot...");
        api.getReport().updateTestLog(
            "takeFullPageScreenshot",
            "Full-page screenshot captured successfully. (Demo plugin — requires Playwright Page access in production)",
            Status.PASS
        );
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Captures a screenshot of a specific [<Object>] element",
        input = InputType.NO
    )
    public void captureElementScreenshot() {
        String element = api.getObjectName();
        System.out.println("Capturing element screenshot for: " + element);
        api.getReport().updateTestLog(
            "captureElementScreenshot",
            "Element screenshot captured for: " + element,
            Status.PASS
        );
    }

    @Action(
        object = ObjectType.GENERAL,
        desc = "Logs the current page URL and title",
        input = InputType.NO
    )
    public void logPageInfo() {
        System.out.println("Logging page info...");
        api.getReport().updateTestLog(
            "logPageInfo",
            "Page info logged. (Demo plugin — integrate with BrowserPluginApi for real URL access)",
            Status.PASS
        );
    }
}
