# Hello World Demo Plugin

A minimal INGenious plugin demonstrating the plugin API contract end to end. It exists mainly as a
reference/test fixture for the Plugin Marketplace — showing the smallest shape a real plugin needs
(a `pom.xml` depending on `ingenious-api` at `provided` scope, an actions class, and the metadata
files the submission flow expects) rather than providing any real test-automation functionality.

## Actions

- **sayHelloWorld** — logs a fixed "Hello, World!" message and marks the step as passed. Useful as
  a smoke test that the plugin jar built correctly and loads inside the engine.
- **sayCustomGreeting** — logs a custom greeting read from the step's `[<Data>]` field, falling back
  to a default message if none is provided.

## Requirements

Depends on `com.ing:ingenious-api:3.0` at `provided` scope and targets INGenious engine version
3.0.0 or later.
