# RuneLite & Jagex Rules

## Jagex Third-Party Client Guidelines

Source: https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1

Key restrictions for plugin development:

- **No automation**: Plugins must not automate gameplay actions (clicking, movement, combat, skilling).
- **No unfair advantage in PvP**: Do not provide combat-altering features in PvP scenarios beyond what the vanilla client offers.
- **No menu entry swaps in dangerous areas**: Be cautious with menu entry modifications, especially in Wilderness/PvP zones.
- **No deobfuscation or reverse engineering** of the game client beyond what RuneLite's public API provides.

## RuneLite Plugin Hub Rules

Source: https://github.com/runelite/plugin-hub

- Plugins must not use reflection, JNI/JNA, `Unsafe`, external processes, dynamic code loading, or runtime code generation.
- Plugins must not create fresh `Gson` instances — use `@Inject Gson`.
- Plugins must not interact with the game in ways that provide unfair advantages.
- All dependencies must be available from Maven Central or the RuneLite repository.

## RuneLite Rejected or Rolled Back Features

Source: https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features

Always check this list before implementing features that may have been previously rejected.
