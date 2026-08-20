# Project Rules & Reference Documentation

## RuneLite API Documentation & Tools
- Official RuneLite API Javadocs: https://static.runelite.net/runelite-api/apidocs/
- Use the `runelite-osrs-docs` MCP tools (`search_runelite_javadocs`, `get_runelite_javadoc`, `search_runelite_source`, `get_runelite_constants`, `search_plugin_hub_source`, `search_osrs_wiki`, `get_osrs_wiki_page`, `get_osrs_wiki_monster_stats`) for fast, accurate API lookups, source cross-referencing, and monster mechanics.

## Architecture Guidelines for Mob Check
1. **Per-Tick Cache**:
   - The attack list is rebuilt once per tick in `onGameTick()` via `buildActiveAttacks()` and cached in `cachedAttacks`.
   - Overlays (`MobCheckOverlay`, `MobCheckPrayerWidgetOverlay`, `MobCheckWorldOverlay`) must only read from `getActiveAttacks()` / `getPriorityAttack()` to avoid mid-frame drift.
2. **Projectile Tracking & Initial Ticks**:
   - `projectileInitialTicks` caches the first observed tick count keyed by `System.identityHashCode(projectile)` to drive the visual progress ring. Prune stale keys on each tick.
   - Generic projectiles (e.g. arrows `15`, spells `160`) must be region-gated to prevent false positives across the game.
3. **Melee Attacks**:
   - Tracked in `npcMeleeAttacks` keyed by `NPC.getIndex()`.
   - Decremented in `onGameTick()` and kept visible until `ticks < 0` so the `0t` impact tick remains visible to the player.
4. **World Highlights & True Tile**:
   - Server-side true tiles must be calculated using `LocalPoint.fromWorld(client, npc.getWorldLocation())`.
   - Multi-tile NPCs must use `Perspective.getCanvasTileAreaPoly(client, lp, npcSize)` with `npc.getComposition().getSize()`.

## ID Verification Workflow
Before adding any new Boss or Monster:
1. Search the OSRS Wiki page (`search_osrs_wiki`, `get_osrs_wiki_page`, `get_osrs_wiki_monster_stats`) to determine attack speeds, combat styles, and projectile mechanics.
2. Check RuneLite constants (`get_runelite_constants` for `AnimationID`, `GraphicID`, `SpotanimID`, `NpcID`).
3. Search RuneLite core and Plugin Hub plugins (`search_runelite_source`, `search_plugin_hub_source`) to verify exact projectile and animation IDs used by existing high-level plugins (e.g., inferno, colosseum, gauntlet, hydra).
4. Never guess IDs or hardcode magic numbers directly in logic — always declare them in `ProjectileID` / `AnimationID` constant classes.
