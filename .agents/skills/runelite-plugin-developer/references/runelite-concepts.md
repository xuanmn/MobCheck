# RuneLite Concepts

## Architecture Overview

```
┌─────────────────────────────────────────┐
│           Jagex Game Server             │
│  (authoritative game state, tick-based) │
└─────────────┬───────────────────────────┘
              │ network (game protocol)
┌─────────────▼───────────────────────────┐
│         OSRS Game Client (vanilla)      │
│  - Renders game world                   │
│  - Processes user input                 │
│  - Manages local game state cache       │
└─────────────┬───────────────────────────┘
              │ hooks / mixins
┌─────────────▼───────────────────────────┐
│         RuneLite Client Framework       │
│  - Event bus (Subscribe annotations)    │
│  - Plugin lifecycle (startUp/shutDown)  │
│  - Overlay system (Graphics2D drawing)  │
│  - Config system (persistent settings)  │
│  - Guice dependency injection           │
└─────────────┬───────────────────────────┘
              │ @Inject / @Subscribe
┌─────────────▼───────────────────────────┐
│         Your Plugin                     │
│  - Listens to events                    │
│  - Reads game state via Client API      │
│  - Renders overlays                     │
│  - Provides config UI                   │
└─────────────────────────────────────────┘
```

## Game Ticks

- OSRS runs on a 600ms tick cycle (approximately).
- Most game state changes happen on tick boundaries.
- `GameTick` event fires once per game tick.
- `ClientTick` fires more frequently (per client frame).
- Projectile cycles: 1 game tick = 30 client cycles.

## What Plugins Can Do

- **Observe**: Read game state (NPCs, players, objects, projectiles, animations, widgets, vars).
- **Draw**: Render overlays on the game world or UI panels.
- **React**: Respond to events (animation changes, projectile spawns, game ticks).
- **Configure**: Provide user-facing settings via `@ConfigItem`.
- **Notify**: Play sounds, show notifications, display info boxes.

## What Plugins Cannot Do

- **Automate**: Send inputs, click, move, or interact on behalf of the player.
- **Modify game state**: Change server-side data.
- **Intercept network**: Read or modify game protocol packets directly.
