package com.ing.plugins.helloworld;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;

public class HelloWorldActions {

    private final CommandPluginApi api;

    public HelloWorldActions(CommandPluginApi api) {
        this.api = api;
    }

    @Action(
        object = ObjectType.GENERAL,
        desc = "Logs a 'Hello, World!' message to verify the plugin is working",
        input = InputType.NO
    )
    public void sayHelloWorld() {
        System.out.println("Hello, World! from HelloWorld plugin v1.0.0");
        api.getReport().updateTestLog(
            "sayHelloWorld",
            "Hello, World! Plugin is working correctly.",
            Status.PASS
        );
    }

    @Action(
        object = ObjectType.GENERAL,
        desc = "Logs a custom greeting from the [<Data>] field",
        input = InputType.YES
    )
    public void sayCustomGreeting() {
        String greeting = api.getData();
        if (greeting == null || greeting.isEmpty()) {
            greeting = "Hello! (no message provided)";
        }
        System.out.println("Custom greeting: " + greeting);
        api.getReport().updateTestLog(
            "sayCustomGreeting",
            "Custom greeting logged: " + greeting,
            Status.PASS
        );
    }
}
