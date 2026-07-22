# Print Test Plugin

## Description

A simple debugging plugin that prints whatever test data is passed to it. Useful for verifying that plugins are loaded correctly and that the test execution pipeline is working as expected.

## Actions

- `printTest` — Prints the test data from the `<Data>` field to both the console and the test report log. Accepts any text input.

## Prerequisites

- INGenious 3.0.0 or later
- The plugin must be installed in the `plugins/` directory of your INGenious installation

## Usage

1. Install the plugin via the Plugin Manager (Browse tab → Install)
2. Open the Step Builder in the IDE
3. Create a new test step
4. Select the **Object Type**: `General`
5. Select the **Action**: `printTest`
6. Enter any text in the **Data** field
7. Run the test case

The message "Print Test: [your text]" will appear in the console and in the test report.

## Example

| Field | Value |
|---|---|
| Object Type | General |
| Action | printTest |
| Data | Hello from Print Test plugin! |

## Author

INGenious Plugins Team  
plugins@company.com
