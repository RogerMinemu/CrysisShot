# CrysisShot Plugin - Testing Development Roadmap

## 1. Overview of Testing Strategy

This document outlines the testing strategy for the CrysisShot plugin. The goal is to ensure code quality, prevent regressions, and facilitate ongoing development. We will employ a combination of:

1.  **Unit Tests:** To test individual components (classes and methods) in isolation. This will be the primary focus for automated testing.
2.  **Integration Tests:** To test the interaction between different components of the plugin.
3.  **Functional/End-to-End (E2E) Tests (Agent-Guided):** To simulate player/admin actions on a server, largely guided by the AI agent and executed manually by the developer.

## 2. Phase 1: Unit Testing Setup & Core Logic

### 2.1. Setup Testing Environment
1.  **Add Dependencies:**
    *   Ensure JUnit 5 (Jupiter) is in `pom.xml` (usually `junit-jupiter-api` and `junit-jupiter-engine` in `test` scope).
    *   Add Mockito Core (`mockito-core`) to `pom.xml` for mocking dependencies (especially Bukkit API classes).
    *   (Optional) Add MockBukkit if deeper Bukkit simulation is needed for specific unit/integration tests later.
2.  **Verify Directory Structure:**
    *   Ensure the standard Maven test directory `src/test/java/com/crysisshot/...` exists.

### 2.2. Target Non-Bukkit Dependent Classes for Initial Unit Tests

For these classes, we can write pure JUnit tests with minimal to no mocking of Bukkit APIs.

1.  **`ConfigManager.java`**
    *   Test loading of default `config.yml`.
    *   Test retrieval of various configuration values (strings, integers, booleans, lists).
    *   Test handling of missing configuration values (default fallbacks).
    *   Test configuration reloading.
2.  **`MessageManager.java`**
    *   Test loading of default `en.yml` locale.
    *   Test retrieval of simple messages.
    *   Test retrieval of messages with placeholders.
    *   Test language switching for a player (if state is managed internally or mockable).
    *   Test fallback to default language if a message key is missing in a specific locale.
3.  **`Arena.java` (Non-Bukkit specific logic)**
    *   Test getters and setters for properties like `name`, `minPlayers`, `maxPlayers`, `theme`.
    *   Test state transitions (`setState`, `getState`).
    *   Test validation logic if any is purely internal to the class (e.g., `isValid()` if it doesn't rely heavily on Bukkit locations immediately).
4.  **`ScoreCalculator.java` (if logic is sufficiently decoupled)**
    *   Test base kill score.
    *   Test combo bonus calculations for different kill streak levels.
    *   Test any other special scoring rules.
5.  **`Arena.Theme` (Enum)**
    *   Test `getDisplayName()` and `getDescription()` methods.

## 3. Phase 2: Unit Testing Bukkit-Dependent Logic (with Mockito)

For classes interacting more directly with Bukkit APIs, we'll use Mockito to mock those dependencies.

1.  **`ArenaManager.java`**
    *   Mock `CrysisShot` plugin instance, `ConfigManager`, `FileConfiguration` (for `arenas.yml`).
    *   Test `loadArenas()`:
        *   Loading from an empty `arenas.yml`.
        *   Loading valid arena configurations.
        *   Handling malformed arena configurations.
    *   Test `saveArenas()`:
        *   Saving arenas to `arenas.yml`.
    *   Test `addArena()`, `removeArena()`, `getArena()`.
    *   Test `validateArena()`:
        *   Mock `Location`, `World` as needed.
        *   Test scenarios for missing lobby, spawns, bounds, etc.
2.  **`ArenaSetupManager.java`**
    *   Mock `Player`, `CrysisShot` plugin, `ArenaManager`, `MessageManager`.
    *   Test `startSetup()`:
        *   Starting a new arena setup.
        *   Attempting to start setup when already in a session.
    *   Test `endSetup()`:
        *   Saving a valid setup.
        *   Cancelling a setup.
    *   Test `handleSetupCommand()` for various sub-commands:
        *   `setLobby`, `setSpectator`, `addSpawn`, `removeSpawn`, `setBounds`, `setTheme`, `setPlayers`.
        *   Mock player's location and other necessary states.
        *   Verify interactions with the `ArenaSetupSession` and `ArenaManager`.
    *   Test `validateCurrentSetup()` logic.
3.  **`ArenaThemeManager.java`**
    *   Mock `CrysisShot` plugin, `Arena`.
    *   Test `initializeThemeConfigurations()`: Verify that theme configurations are loaded correctly.
    *   Test `getThemeConfiguration()` for different themes.
    *   Test `getBuildingGuidelines()` for various themes.
    *   Unit testing `startThemeEffects` and `stopThemeEffects` is challenging due to `BukkitScheduler`. We can test that the correct parameters would be passed to the scheduler, or that tasks are correctly added/removed from `activeEffects`, but not the effects themselves.
4.  **`CrysisShotCommand.java` (Focus on command logic, not execution outcome)**
    *   Mock `CommandSender`, `Player`, `CrysisShot` plugin, and all relevant managers (`GameManager`, `MessageManager`, `ArenaSetupManager`, `ArenaThemeManager`).
    *   For each subcommand (e.g., `/cs admin setup start <name>`, `/cs admin theme preview <theme>`):
        *   Verify permission checks.
        *   Verify argument parsing and validation.
        *   Verify that the correct methods on manager classes are called with the correct arguments.
        *   Verify that appropriate messages are sent via `MessageManager`.
    *   Test `onTabComplete()` logic for various argument lengths and commands.

## 4. Phase 3: Integration Testing

This phase will focus on testing interactions between multiple components. MockBukkit might be introduced here for more realistic server environment simulation if pure mocking becomes too cumbersome.

1.  **Arena Creation & Setup Flow:**
    *   Simulate a sequence of `/cs admin setup ...` commands.
    *   Verify the `Arena` object is correctly configured in `ArenaManager`.
    *   Verify `arenas.yml` is updated correctly upon saving.
2.  **Theme Application Flow:**
    *   Simulate `/cs admin setup theme <theme_name>` during setup.
    *   Simulate `/cs admin theme preview <theme_name>`.
    *   Verify the `Arena` object reflects the chosen theme and that `ArenaThemeManager` provides correct data.
3.  **Configuration Reload Flow:**
    *   Modify a mock `config.yml` or `arenas.yml`.
    *   Simulate `/cs admin reload`.
    *   Verify that `ConfigManager` and `ArenaManager` reflect the reloaded configurations.

## 5. Phase 4: Functional/End-to-End (E2E) Testing (Agent-Guided)

This will continue as per our current workflow, where the AI agent (GitHub Copilot) generates test scenarios and command sequences, and the developer executes them on a live or test server.

**Key Scenarios to Cover (Iteratively):**
*   Full arena setup from scratch for each available theme.
*   Arena theme preview and guidelines command for all themes.
*   Arena theme effects start/stop for different arenas.
*   Basic player commands (join, leave, queue - once implemented).
*   Admin commands (reload, version, list arenas).
*   Language switching and message verification.
*   Error handling for invalid commands and inputs.

## 6. Workflow for AI-Driven Test Implementation

1.  **Select Target:** AI agent (GitHub Copilot) will pick a class or method from the roadmap above (e.g., "Unit test `ConfigManager.getString()`").
2.  **Clarify Requirements:** If needed, Copilot will ask for clarification on specific behaviors or edge cases.
3.  **Generate Test Code:** Copilot will generate the JUnit test case(s) for the selected target.
4.  **Developer Review:** The developer reviews the generated test code.
5.  **Execution:** Developer (or Copilot via tool if available and safe) runs `mvn test`.
6.  **Analysis & Iteration:**
    *   If tests pass, proceed to the next target.
    *   If tests fail, Copilot will help debug the test or the source code.
7.  **Commit:** Once tests for a logical unit are passing, commit them with a message like `test: add unit tests for ConfigManager` or `test: cover theme preview command logic`.

This document will be updated as the testing progresses and new areas for testing are identified.
