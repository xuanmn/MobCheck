# ID Verification & Boss Mechanics Rules

When adding new monster/boss support to `mob_check`:

1. **Verify Combat Mechanics**:
   - Confirm whether the attack uses a projectile (travels over ticks based on distance) or an animation (instant hit / fixed tick delay).
   - Use `get_osrs_wiki_monster_stats` and `get_osrs_wiki_page` to check attack speed and telegraph styles.

2. **Cross-Reference IDs**:
   - Cross-reference projectile IDs and animation IDs against official RuneLite constants using `get_runelite_constants` or `search_runelite_source`.
   - Search Plugin Hub prior art using `search_plugin_hub_source` to confirm how top combat plugins track the boss.

3. **Check for Ambiguities & Generic IDs**:
   - If a projectile ID is low or generic (e.g. standard arrows, basic elemental spells), ensure it is added to a region-gated map like `COLOSSEUM_ONLY_PROJECTILE_STYLES` instead of global tables.

4. **Code Quality**:
   - Add new IDs to `MobCheckPlugin.ProjectileID` or `MobCheckPlugin.AnimationID`.
   - Maintain Java 11 compatibility.
   - Add unit test coverage in `MobCheckPluginUnitTest.java` for new boss encounters.
