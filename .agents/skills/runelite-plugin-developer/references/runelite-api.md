# RuneLite API Reference

## Primary API Javadocs

https://static.runelite.net/runelite-api/apidocs/index.html

## Key Interfaces

### Client (`net.runelite.api.Client`)
- `getLocalPlayer()` — the logged-in player
- `getProjectiles()` — active projectile deque
- `getNpcs()` — all loaded NPCs
- `playSoundEffect(int id)` — play a game sound

### Actor (`net.runelite.api.Actor`)
- Parent of `Player` and `NPC`
- `getAnimation()` — current animation ID
- `getInteracting()` — who this actor is targeting
- `getCanvasImageLocation(BufferedImage, int)` — screen position for overlays

### NPC (`net.runelite.api.NPC`)
- `getIndex()` — unique NPC index in the scene
- `getName()` — NPC display name
- `getId()` — NPC definition ID

### Projectile (`net.runelite.api.Projectile`)
- `getId()` — projectile graphic/animation ID
- `getInteracting()` — target actor
- `getRemainingCycles()` — client cycles until impact (÷30 for ticks)

## Key Events (`net.runelite.api.events`)

| Event | Fires When |
|---|---|
| `GameTick` | Each game tick (~600ms) |
| `AnimationChanged` | An actor starts a new animation |
| `ProjectileSpawned` | A new projectile appears |
| `NpcSpawned` / `NpcDespawned` | NPC enters/exits the scene |
| `GameStateChanged` | Login state transitions |

## Gameval Constants (`net.runelite.api.gameval`)

Prefer `gameval` IDs over raw integer magic numbers when available:
- NPC IDs, Item IDs, Object IDs
- Animation IDs, Projectile IDs
- Component IDs, Var IDs

## Client Services (`net.runelite.client`)

| Service | Usage |
|---|---|
| `SpriteManager` | Load game sprites by ID |
| `OverlayManager` | Register/unregister overlays |
| `ConfigManager` | Read/write plugin config |
| `ClientThread` | Execute on the client thread |
| `Notifier` | System notifications |
