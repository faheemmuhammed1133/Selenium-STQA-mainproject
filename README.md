# n8n Selenium Test Suite

Automated UI test suite for an **n8n** instance, built with **Selenium WebDriver**, **TestNG**, and **Maven**. The suite covers login (valid/invalid), workflow loading, node issue detection, chat-based workflow execution, and execution history verification.

---

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Running the Tests](#running-the-tests)
- [Test Cases](#test-cases)
- [Screenshots](#screenshots)
- [Architecture Notes](#architecture-notes)
- [Troubleshooting](#troubleshooting)

---

## Overview

This project drives a Chrome browser against a live n8n deployment to verify core user flows:

1. Authentication (success and failure paths)
2. Workflow canvas loading
3. Detection of node configuration issues
4. Triggering a workflow via the built-in chat trigger and observing failure behavior
5. Verifying execution history reflects an error state

Each test automatically captures a screenshot at the end of its run for evidence/debugging, saved under `screenshots/`.

## Project Structure

```
.
├── src/
│   └── test/
│       └── java/
│           ├── BaseTest.java   # WebDriver lifecycle (setup/teardown)
│           └── N8nTest.java    # Test cases (TC01–TC06)
├── screenshots/                # Generated at runtime, one PNG per test
├── pom.xml                     # Maven dependencies & TestNG runner config
└── README.md
```

### `BaseTest.java`
Base class extended by all test classes. Responsibilities:

- Reads runtime configuration from JVM system properties: `n8nBaseUrl`, `n8nUserEmail`, `n8nUserPassword`, `n8nWorkflowId`
- `@BeforeMethod setup()` — instantiates `ChromeDriver`, maximizes the window, sets a 10s implicit wait, and creates a `WebDriverWait` with a 15s timeout
- `@AfterMethod tearDown()` — quits the driver after each test

### `N8nTest.java`
Contains the test cases plus two helpers:

- `login(user, pass)` — navigates to `/signin`, fills email/password fields, clicks the "Sign in" button
- `capture(name)` — takes a full-page screenshot and saves it to `screenshots/<name>.png`

## Prerequisites

| Requirement | Notes |
|---|---|
| Java JDK | 11+ recommended |
| Maven | 3.6+ |
| Google Chrome | Installed and on `PATH` |
| ChromeDriver | Managed automatically if using Selenium Manager (Selenium 4.6+); otherwise install matching version manually |
| Running n8n instance | Reachable URL with a valid user account and at least one workflow containing an "AI Agent" node and a chat trigger |

## Configuration

Tests read configuration exclusively from **JVM system properties** — no hardcoded values, no `.env` file. Required properties:

| Property | Description | Example |
|---|---|---|
| `n8nBaseUrl` | Base URL of the n8n instance (no trailing slash) | `https://n8n.example.com` |
| `n8nUserEmail` | Valid login email for TC01, TC03–TC06 | `qa@example.com` |
| `n8nUserPassword` | Valid login password | `********` |
| `n8nWorkflowId` | ID of the workflow to open in TC03–TC05 | `abc123XYZ` |

> **Note:** TC02 (invalid login) uses hardcoded bogus credentials and does not depend on these properties.

## Running the Tests

Pass configuration via `-D` flags with Maven Surefire:

```bash
mvn clean test \
  -DsuiteXmlFile=testng.xml \
  -Dn8nBaseUrl="https://n8n.example.com" \
  -Dn8nUserEmail="qa@example.com" \
  -Dn8nUserPassword="YourPassword123!" \
  -Dn8nWorkflowId="abc123XYZ"
```

If your `pom.xml` already binds TestNG to the `test` phase by default, you can omit `-DsuiteXmlFile`. To run a single test class:

```bash
mvn -Dtest=N8nTest test
```

To run a single test method:

```bash
mvn -Dtest=N8nTest#TC01_LoginSuccess test
```

## Test Cases

Tests run in priority order (1 → 6). Several tests depend on a successful login, so earlier failures may cascade.

| # | Test Method | Priority | Purpose |
|---|---|---|---|
| TC01 | `TC01_LoginSuccess` | 1 | Logs in with valid credentials; asserts redirect to `/home` or `/workflows` |
| TC02 | `TC02_InvalidLogin` | 2 | Logs in with deliberately invalid credentials; asserts the "Wrong username or password" toast appears |
| TC03 | `TC03_VerifyWorkflowLoads` | 3 | Opens the target workflow by ID; asserts an "AI Agent" node is visible on the canvas |
| TC04 | `TC04_VerifyNodeIssues` | 4 | Opens the workflow; asserts at least one node-issue indicator (`[data-test-id='node-issues']`) is present |
| TC05 | `TC05_ExecuteWorkflowFailure` | 5 | Opens the chat panel, sends "hi", and asserts an error notification (`.el-notification--error`) appears |
| TC06 | `TC06_VerifyExecutionHistory` | 6 | Navigates to `/home/executions`; asserts the most recent execution row shows status "Error" |

Each test ends with `capture("<TestName>")`, writing a screenshot regardless of pass/fail (the capture call only runs on success paths currently, since it executes after the assertion).

## Screenshots

Screenshots are written to `screenshots/<TestCaseName>.png` relative to the working directory the tests are run from. This folder is created automatically by `FileUtils.copyFile`'s target resolution as long as the parent `screenshots/` directory exists — ensure it exists or is created by your CI step:

```bash
mkdir -p screenshots
```

Consider adding `screenshots/` to `.gitignore` if these are run artifacts rather than tracked assets:

```gitignore
screenshots/
target/
*.class
```

## Architecture Notes

- **Locator strategy:** Mostly CSS selectors with `data-test-id` attributes (stable, n8n-specific) and a few XPath text-matching selectors for dynamic toast/label text. Prefer keeping `data-test-id` selectors as primary where available, since text-based XPath is more brittle to copy/UI changes.
- **Waits:** Implicit wait (10s) is set globally in `BaseTest`; explicit `WebDriverWait` (15s) is used per-condition in test methods. Mixing implicit and explicit waits is generally discouraged in Selenium best practice, but is preserved here as-is from the original implementation.
- **No Page Object Model (POM) yet:** Locators and actions currently live directly in the test class. For a growing suite, consider extracting a `LoginPage`, `WorkflowPage`, and `ExecutionsPage` class to reduce duplication and improve maintainability.
- **Test independence:** Tests are not fully isolated — TC03–TC06 each re-perform login. This trades some runtime for resilience against TestNG execution order changes.

## Troubleshooting

| Issue | Likely Cause | Fix |
|---|---|---|
| `SessionNotCreatedException` | ChromeDriver/Chrome version mismatch | Update Chrome or let Selenium Manager auto-resolve (Selenium 4.6+) |
| `NoSuchElementException` on login fields | Page not fully loaded or selector changed in n8n UI | Increase wait or verify `n8nBaseUrl` is reachable |
| TC02 fails to find error toast | n8n error message text changed | Update the XPath text match in `TC02_InvalidLogin` |
| TC04 fails with 0 issues found | Workflow has no intentional misconfiguration | Ensure the target workflow (`n8nWorkflowId`) is set up to produce a node issue |
| Screenshots not saved | `screenshots/` directory missing | Run `mkdir -p screenshots` before test execution |

---

## License

Add your license of choice here (e.g., MIT).

## Contributing

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/my-test`)
3. Commit your changes
4. Open a pull request