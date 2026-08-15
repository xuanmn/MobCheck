# RuneLite Plugin Testing & Verification

## Unit Testing

- **JUnit & Mockito**: Use JUnit 4/5 and Mockito to mock RuneLite API interfaces (`Client`, `Player`, `NPC`, `Projectile`, `OverlayManager`).
- **Reflection Injection**: Inject `@Inject` dependencies in test setup using reflection where Guice container is not launched.
- **Gradle Task**: Execute automated tests with `./gradlew test`.

## Manual Live-Client Debugging

- **Dev Launcher**: Launch the test client runner (e.g. `MobCheckPluginTest`) in developer mode to inspect live overlay behavior.
- **RuneLite Developer Tools**: Enable built-in RuneLite Developer Tools to inspect widget structures, NPC IDs, projectile IDs, and animation IDs in real-time.

## Preflight & Plugin Hub CI

- Ensure `./gradlew build` and `./gradlew test` pass cleanly without errors.
- Confirm `runelite-plugin.properties` has matching `displayName`, `author`, `description`, `tags`, and entrypoint class `plugins`.
