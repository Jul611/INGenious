package com.ing.plugins.calc;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;

public class CalcActions {

    private final CommandPluginApi api;

    public CalcActions(CommandPluginApi api) {
        this.api = api;
    }

    @Action(
        object = ObjectType.GENERAL,
        desc = "Adds two numbers from [<Data>] and logs the result",
        input = InputType.YES
    )
    public void addNumbers() {
        String data = api.getData();
        int result = 0;
        try {
            String[] parts = data.split("[+,]");
            int a = Integer.parseInt(parts[0].trim());
            int b = Integer.parseInt(parts[1].trim());
            result = a + b;
        } catch (Exception e) {
            api.getReport().updateTestLog("addNumbers",
                "Invalid input. Use format: 5+3", Status.FAIL);
            return;
        }
        api.getReport().updateTestLog("addNumbers",
            "Result: " + result, Status.PASS);
    }

    @Action(
        object = ObjectType.GENERAL,
        desc = "Multiplies two numbers from [<Data>] and logs the result",
        input = InputType.YES
    )
    public void multiplyNumbers() {
        String data = api.getData();
        int result = 0;
        try {
            String[] parts = data.split("[*,]");
            int a = Integer.parseInt(parts[0].trim());
            int b = Integer.parseInt(parts[1].trim());
            result = a * b;
        } catch (Exception e) {
            api.getReport().updateTestLog("multiplyNumbers",
                "Invalid input. Use format: 5*3", Status.FAIL);
            return;
        }
        api.getReport().updateTestLog("multiplyNumbers",
            "Result: " + result, Status.PASS);
    }
}
