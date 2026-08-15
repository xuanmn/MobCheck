# Plugin Architecture & Design Patterns

## Core Architecture Guidelines

- **Lifecycle Management**: Implement initialization in `startUp()` and cleanup in `shutDown()`. Ensure overlays are registered/unregistered cleanly with `OverlayManager`, maps and collections are cleared, and state is reset.
- **Config Management**: Use RuneLite's `@ConfigGroup` and `@ConfigItem` interface pattern. Keep key names stable and use sensible default methods.
- **Overlays**: Use `PanelComponent`, `InfoBoxComponent`, or custom `Overlay` implementations. Keep calculations inside `render()` to a minimum; pre-calculate or cache values during game events/ticks.
- **Event Handling**: Use `@Subscribe` on handler methods. Filter early (e.g. check for `client.getLocalPlayer() != null` or correct actor targets) to avoid unnecessary processing.
- **I/O & Web Requests**: Use injected `@Inject OkHttpClient` and `@Inject Gson` for HTTP calls. Never make blocking network or file I/O calls directly on the client thread.
