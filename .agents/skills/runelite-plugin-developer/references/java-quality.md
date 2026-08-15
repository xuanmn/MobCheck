# Java 11 Code Quality & Performance

## Language & Compatibility Rules

- **Java 11 Target**: Ensure all code compiles with `--release 11`. Do not use records, text blocks, sealed classes, pattern matching, or post-Java 11 APIs.
- **Concurrency & Threading**:
  - Distinguish between Client Thread and Event Dispatch Thread (EDT).
  - Use `clientThread.invoke()` or `clientThread.invokeLater()` when modifying game client structures from external threads.
- **Render Hot Paths**:
  - Keep overlay `render()` lightweight. Avoid allocation (`new` calls, collection instantiations) inside frame rendering loops.
- **Logging Best Practices**:
  - Use `@Slf4j` or injected `Logger`.
  - Use `log.debug()` for diagnostic info and `log.info()` sparingly for startup/shutdown or critical state changes.
- **Collection & Memory Management**:
  - Clear transient tracking maps/lists in `shutDown()` and `startUp()`.
  - Use primitive specialization or fixed bounds where applicable to prevent unbounded memory growth.
