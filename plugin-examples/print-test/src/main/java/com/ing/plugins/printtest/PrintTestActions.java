package com.ing.plugins.printtest;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;

public class PrintTestActions {

    private final CommandPluginApi api;

    public PrintTestActions(CommandPluginApi api) {
        this.api = api;
    }

    @Action(
        object = ObjectType.GENERAL,
        desc = "Prints the test data to the console and report log for debugging",
        input = InputType.YES
    )
    public void printTest() {
        String data = api.getData();
        if (data == null || data.trim().isEmpty()) {
            data = "(no input data)";
        }
        String message = "Print Test: " + data;
        System.out.println(message);
        api.getReport().updateTestLog(
            "printTest",
            message,
            Status.PASS
        );
    }
}
