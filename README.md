# Mob Check Plugin

[![RuneLite Compatible](https://img.shields.io/badge/RuneLite-Plugin-orange.svg?style=flat-square)](https://runelite.net/)
[![Java](https://img.shields.io/badge/Java-11%2B-blue.svg?style=flat-square)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.10-02303a.svg?style=flat-square)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-BSD--2--Clause-green.svg?style=flat-square)](LICENSE)

**Mob Check** is an intelligent, high-performance PvM Priority Prayer Helper plugin for the [RuneLite](https://runelite.net/) client. Built specifically for high-intensity PvM encounters—such as **The Inferno**, **Fortis Colosseum**, **Tormented Demons**, **Phantom Muspah**, and **Dagannoth Kings**—Mob Check calculates and displays the exact server ticks remaining before incoming NPC attacks hit the player, helping you execute flawless prayer timing and 1-tick prayer flicks with complete confidence.

---

## Key Features

### 1. Overhead Countdown & Progress Arc
- **Visual Overhead Indicator**: Displays the required protection prayer icon ([Magic](https://oldschool.runescape.wiki/w/Protect_from_Magic), [Missiles / Range](https://oldschool.runescape.wiki/w/Protect_from_Missiles), [Melee](https://oldschool.runescape.wiki/w/Protect_from_Melee)) and remaining tick count directly above your character.
- **Tick Progress Arc**: Renders a smooth circular countdown progress ring around the overhead prayer sprite showing the travel progression of incoming projectiles from launch to impact.
- **Dynamic Urgency Color-Coding**:
  - 🟢 **Green**: Correct protection prayer is active.
  - ⚪ **White / Style Color**: Safe countdown period ($> 1\text{t}$).
  - 🔴 **Red**: Urgent impact threshold ($\le 1\text{t}$) when prayer is not active.

### 2. Interactive Prayer Tab Widget Highlights
- **Active Prayer Status Border**:
  - 🟢 **Solid Green Outline**: Correct protection prayer is currently active.
  - 🔴 **Pulsing Red Border & Flash**: Unprotected or wrong prayer active with an attack landing in $\le 1$ tick.
  - 🔵 **Style-Coded Outline**: Highlights the required prayer when safe ($> 1\text{t}$).
- **On-Widget Tick Counter**: Renders the exact tick countdown directly over the corresponding prayer button in your Prayer Tab for fast, focused clicking without looking away from your inventory/prayers.

### 3. Screen Danger Vignette Flash
- Flashes a high-visibility red danger border across the game screen when an attack is $\le 1$ tick away and the player is not protected, giving you an instantaneous visual reflex cue.

### 4. Attacking NPC Outlines & Ground True Tiles
- **Model Hull Outline**: Highlights the 3D model convex hull of whichever monster is actively attacking you, color-coded by attack style (Cyan for Magic, Green for Range, Orange for Melee).
- **Server-Side Ground True Tile**: Highlights the actual server tile location using `npc.getWorldLocation()`. Supports multi-tile boss scaling (e.g., $3\times3$ bosses like Sol Heredit, General Graardor, Dagannoth Rex).
- **On-NPC Tick Timer**: Displays the attack countdown number above the attacking NPC's head.

### 5. Configurable Audio Alerts & Emergency Cues
- **Style-Specific Sounds**: Plays customizable sound effect IDs when the priority prayer style changes (Magic, Range, Melee).
- **Emergency Wrong Prayer Buzzer**: Plays an emergency warning sound (Default ID: `2277`) when an attack is landing in $\le 1$ tick without the correct protection active.

### 6. Top-Threat Queue & Manticore Combo Sequence Panel
- **Sidebar Info Box**: Displays up to 4 upcoming attacks ordered by impact tick, showing prayer sprites and remaining ticks.
- **Manticore 3-Hit Combo Visualizer**: Explicitly highlights rapid multi-hit combo cycles during Fortis Colosseum encounters.

---

## Supported Bosses & Monsters

| Encounter / Category | Monster | Attack Styles & Mechanics Tracked | Projectile / Anim IDs |
| :--- | :--- | :--- | :--- |
| **The Inferno** | **Jal-Ak (Blob)** | Magic projectile, Ranged projectile, Melee bite | Proj: `1380` (M), `1378` (R)<br>Anim: `7582` (Melee) |
| | **Jal-AkRek (Mini-Blobs)** | Magic and Ranged spit projectiles | Proj: `1381` (M), `1379` (R) |
| | **Jal-MejRah (Bat)** | Ranged projectile & attack animation | Proj: `1382` (R)<br>Anim: `7578` (Melee) |
| | **Jal-Xil (Ranger)** | Ranged boulder projectile & melee punch | Proj: `1376` (R)<br>Anim: `7604` (Melee) |
| | **Jal-Zek (Mage)** | Magic blast projectile & melee punch | Proj: `1374` (M)<br>Anim: `7612` (Melee) |
| | **Jal-ImKot (Meleer)** | Melee dig & swing animation | Anim: `7597` (Melee) |
| | **JalTok-Jad** | Magic fireball, Ranged boulder, Melee slam | Proj: `448` (M), `449` (R)<br>Anim: `7590` (Melee) |
| **Fortis Colosseum** | **Manticore** | 3-hit rapid sequence (Magic & Ranged projectiles) | Proj: `2687` (M), `2688` (R) |
| | **Serpent Shaman** | Magic venom projectile | Proj: `2685` (M) |
| | **Javelinic Colossus** | Heavy Ranged javelin projectile | Proj: `2686` (R) |
| | **Jaguar Warrior** | Fast Melee blade attack | Anim: `10871` (Melee) |
| | **Minotaur** | Melee club smash | Anim: `10872` (Melee) |
| | **Fremennik Warband** | Archer projectile, Seer spell, Berserker axe *(Region-gated)* | Proj: `15` (R), `160` (M)<br>Anim: `10873` (Melee) |
| | **Shockwave Colossus** | Melee shockwave slam | Anim: `10874` (Melee) |
| | **Sol Heredit** | Sun projectile, Melee attacks, sweeps, and slams | Proj: `2689` (M)<br>Anim: `10875`, `10876`, `10877` |
| **Tormented Demons** | **Tormented Demon** | Magic skull, Ranged spike, Melee claw attack | Proj: `1885` (M), `1884` (R)<br>Anim: `10922` (Melee) |
| **Phantom Muspah** | **Phantom Muspah** | Corruption Magic wave, Ranged spikes, Melee swipes | Proj: `2329` (M), `2330` (R)<br>Anim: `9920`, `9922` (Melee) |
| **Dagannoth Kings** | **Dagannoth Prime** | Water Wave magic projectile *(Region-gated to DK Lair)* | Proj: `162` (M) |
| | **Dagannoth Supreme** | Spine throw ranged projectiles | Proj: `475` (R), `476` (R) |
| | **Dagannoth Rex** | Melee claw swipes | Anim: `2853`, `2851` (Melee) |
| **Additional Bosses** | **Zulrah** | Magic venom spit & Ranged dart projectiles | Proj: `1044` (M), `1046` (R) |
| | **Vorkath** | Dragonfire Magic & Ranged dragonfire blast | Proj: `1481` (M), `1483` (R) |
| | **Cerberus** | Magic fire, Ranged blast, Melee triple attack | Proj: `1243` (M), `1244` (R)<br>Anim: `4492` (Melee) |
| | **Alchemical Hydra** | Lightning Magic & Poison Ranged projectiles | Proj: `1663` (M), `1662` (R) |
| | **Hunllef (Gauntlet)** | Magic orb & Ranged crystal projectiles | Proj: `1707` (M), `1708` (R) |
| | **Demonic Gorillas** | Boulder / orb projectiles & Melee sweep | Proj: `1302` (M), `1304` (R)<br>Anim: `7226` (Melee) |
| | **God Wars Dungeon** | Zilyana (Magic `1220`, Melee 2t `6964`), Graardor (Melee 6t `7060`), K'ril (Magic `1211`, Melee 6t `6948`) | Proj: `1220`, `1211`<br>Anim: `6964`, `7060`, `6948` |
| **Slayer Monsters** | **Abyssal Demons & Bloodvelds** | Melee attack animations | Anim: `2309`, `1552` |

---

## Configuration Reference

Inside the RuneLite Configuration Panel under **Mob Check**, the following options can be customized:

| Section | Setting | Default | Description |
| :--- | :--- | :--- | :--- |
| **Overhead & HUD** | `Show Overhead Icon` | `true` | Renders the required prayer icon and tick countdown directly above your player character. |
| | `Show Tick Progress Arc` | `true` | Draws a smooth circular countdown progress ring around the overhead prayer sprite. |
| | `Warning Threshold` | `1` | Remaining tick count at which visual elements turn red for immediate urgency. |
| **Prayer Book Highlights** | `Highlight Prayer Button` | `true` | Highlights the required protection prayer directly in the Prayer Tab interface. |
| | `Show Ticks on Prayer Button` | `true` | Displays remaining ticks directly over the prayer button in the Prayer Tab. |
| | `Flash Wrong Prayer` | `true` | Pulses a bright red border & fill over the required prayer button when unprotected at $\le 1$ tick. |
| **Danger & Warnings** | `Flash Screen on Wrong Prayer` | `true` | Flashes a red screen border if unprotected when an incoming attack lands in $\le 1$ tick. |
| | `Danger Flash Color` | `Red (Alpha 70)` | Color and transparency for the emergency screen flash border. |
| **Audio Alerts** | `Play Sound Alert` | `true` | Plays an audio cue when the priority prayer style switches. |
| | `Magic Sound ID` | `2266` | Sound effect ID played when Magic protection is required. |
| | `Range Sound ID` | `2266` | Sound effect ID played when Range protection is required. |
| | `Melee Sound ID` | `2266` | Sound effect ID played when Melee protection is required. |
| | `Wrong Prayer Emergency Sound` | `true` | Plays an emergency sound warning if unprotected when an attack is 1 tick away. |
| | `Wrong Prayer Sound ID` | `2277` | Sound effect ID used for the wrong prayer emergency warning. |
| **NPC Threat & World** | `Highlight Attacking NPC` | `true` | Outlines the 3D model convex hull of the monster currently attacking you. |
| | `Highlight NPC True Tile` | `true` | Draws the ground true tile of the attacking NPC (supporting multi-tile sizes). |
| **Info Box & Sequences** | `Show Info Box` | `true` | Displays the upcoming threat queue in a side overlay panel. |
| | `Show Manticore/Combo Sequence` | `true` | Highlights rapid combo sequences (e.g. Manticore 3-hit cycles) in order. |
| | `Track Unknown Projectiles` | `false` | Fallback mode to Pray Magic for unrecognized projectiles targeting the player. |

---

## Technical Architecture & Design

1. **Per-Tick Cache Synchronization**:
   - The attack list is rebuilt once per game tick inside `onGameTick()` via `buildActiveAttacks()` and cached in `cachedAttacks`.
   - All visual overlays (`MobCheckOverlay`, `MobCheckPrayerWidgetOverlay`, `MobCheckWorldOverlay`) consume this cached snapshot, guaranteeing zero mid-frame tearing or coordinate drift across rendering layers.
2. **Deterministic Projectile Cycle Conversion**:
   - Projectile flight duration is converted from client engine cycles into game ticks using `(remainingCycles + 29) / 30`.
   - The initial tick count is cached on first sight keyed by `System.identityHashCode(projectile)` to drive the overhead progress arc accurately.
3. **Region-Gated Projectile Verification**:
   - Generic engine projectile IDs (e.g. standard arrows `15` and elemental spells `160` in the Colosseum, or Water Wave `162` in Dagannoth Kings Lair) are strictly gated by player map region (`client.getMapRegions()`) to prevent false positives across the wider game world.
4. **Melee Attack Lifecycle**:
   - Tracked via `AnimationChanged` keyed by `NPC.getIndex()`.
   - Decremented on each game tick and preserved until `ticks < 0` so the $0\text{t}$ impact tick remains visible to the player during the decisive tick.
5. **Server-Side True Tile Calculation**:
   - Calculated with `LocalPoint.fromWorld(client, npc.getWorldLocation())` rather than visual interpolated coordinates. Multi-tile NPCs are rendered via `Perspective.getCanvasTileAreaPoly(client, lp, npcSize)` using `npc.getComposition().getSize()`.

---

## Building & Development

### Prerequisites
- **Java 11 through Java 23** (Gradle 8.10 wrapper included).
- Make sure `JAVA_HOME` points to a compatible JDK (JDK 11 or JDK 17 recommended):
  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 11)  # macOS
  # or export JAVA_HOME=/path/to/jdk-11
  ```

### Build & Run Commands

| Command | Description |
| :--- | :--- |
| `./gradlew test` | Runs the full unit test suite (35+ unit tests covering all bosses, overlays, and edge cases). |
| `./gradlew build` | Compiles the plugin classes and generates the distribution archive. |
| `./gradlew shadowJar` | Builds a standalone shaded JAR including all runtime dependencies. |
| `./gradlew run` | Launches a live development RuneLite client with the Mob Check plugin pre-injected. |

---

## Developer Tooling & MCP Server

This repository includes a built-in [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server (`.agents/plugins/runelite-osrs-docs/`) that provides LLM assistants and developer tooling with live indexed access to:
- **[RuneLite API Javadocs](https://static.runelite.net/runelite-api/apidocs/)**: Classes, interfaces, methods, fields, events, constants, and widget hierarchies.
- **[Old School RuneScape Wiki](https://oldschool.runescape.wiki/)**: Monster mechanics, combat stats, projectile IDs, animation IDs, and tick timings.

### Running MCP Tests
```bash
cd .agents/plugins/runelite-osrs-docs
npm test
```

---

## License

This project is open-source software licensed under the **BSD 2-Clause License**. See [LICENSE](LICENSE) for details.