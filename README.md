# Mob Check Plugin

A dynamic PvM Priority Prayer Helper plugin for the [RuneLite](https://runelite.net/) client, specialized for high-stakes encounters like **The Inferno** and **Fortis Colosseum**. It tracks and displays the exact ticks remaining before incoming NPC projectile attacks and melee attacks hit the player, helping you time protection prayers and 1-tick prayer flicking accurately.

---

## Features

- **Overhead Countdown & Progress Arc**: Renders the style-specific protection prayer icon (Magic, Range, Melee), visual countdown progress arc, and remaining ticks directly above your character. Color codes based on protection status and turns urgent red within the warning threshold.
- **Prayer Book Widget Highlights**: Draws a glowing box outline directly around the required protection prayer on your Prayer Tab interface:
  - 🟢 **Green Border**: Correct protection prayer is currently active.
  - 🔴 **Pulsing Red Border**: Unprotected / wrong prayer active with an attack landing in $\le 1$ tick.
- **On-Widget Tick Countdown**: Displays the exact tick count directly over the prayer button in your Prayer Tab for fast, focused clicking.
- **Screen Danger Vignette Flash**: Flashes a red danger border around your game screen when an attack hits in $\le 1$ tick and your active prayer is wrong.
- **Attacking NPC Outlines & True Tiles**: Highlights the model hull and ground true tile of whichever monster is currently attacking you, color-coded by incoming combat style (Cyan for Magic, Green for Range, Orange/Red for Melee).
- **Distinct Audio Alerts & Emergency Cues**:
  - Configurable sound effect per attack style (Magic, Range, Melee).
  - Emergency buzzer/audio alert when unprotected against an imminent attack.
- **Sidebar Info Box & Manticore Combo Sequence**: Displays active upcoming threats sorted by impact tick, including multi-hit rapid combo sequences (like Colosseum Manticore 3-hit cycles).
- **Specialized Endgame Coverage**:
  - **The Inferno**:
    - **Jal-Ak (Blobs)**: Magic (1380) and Ranged (1378) projectiles, plus melee animation (7582).
    - **Jal-AkRek (Mini-Blobs)**: Magic (1381) and Ranged (1379) projectiles.
    - **Jal-MejRah (Bats)**: Ranged projectile (1382) and attack animation (7578).
    - **Jal-Xil (Ranger)**: Ranged projectile (1376) and melee punch animation (7604).
    - **Jal-Zek (Mage)**: Magic projectile (1374) and melee punch animation (7612).
    - **Jal-ImKot (Meleer)**: Melee attack animation (7597).
    - **JalTok-Jad**: Magic projectile (448), Ranged projectile (449), and Melee attack animation (7590).
  - **Fortis Colosseum**:
    - **Manticore**: 3-hit rapid attack sequence — Magic projectile (2687) and Ranged projectile (2688).
    - **Serpent Shaman**: Magic projectile (2685).
    - **Javelinic Colossus**: Heavy Ranged javelin projectile (2686).
    - **Jaguar Warrior**: Fast Melee attack animation (10871).
    - **Minotaur**: Melee attack animation (10872).
    - **Fremennik Warband**: Berserker melee animation (10873), Archer projectile (15), Seer magic projectile (160).
    - **Shockwave Colossus**: Melee shockwave animation (10874).
    - **Sol Heredit**: Melee swing animations (10875, 10876, 10877) and special projectile (2689).
  - **Additional PvM Support**: Zulrah, Vorkath, Cerberus, Alchemical Hydra, Hunllef (The Gauntlet), Demonic Gorillas, and standard slayer monsters.

---

## Configuration Options

Inside the RuneLite Configuration Panel under **Mob Check**, you can customize:

| Section | Setting | Default | Description |
| :--- | :--- | :--- | :--- |
| **Overhead & HUD** | `Show Overhead Icon` | `true` | Renders prayer icon and tick countdown above character. |
| | `Show Tick Progress Arc` | `true` | Draws circular countdown progress ring around overhead icon. |
| | `Warning Threshold` | `1` | Tick threshold at which indicators turn red for immediate urgency. |
| **Prayer Book Highlights** | `Highlight Prayer Button` | `true` | Highlights the required prayer directly in your Prayer Tab. |
| | `Show Ticks on Prayer Button` | `true` | Renders tick countdown number directly on the prayer button. |
| | `Flash Wrong Prayer` | `true` | Pulses bright red on the prayer button when unprotected at 1 tick. |
| **Danger & Warnings** | `Flash Screen on Wrong Prayer` | `true` | Flashes red screen border if unprotected on an imminent hit. |
| | `Danger Flash Color` | `Red (Alpha 70)` | Color and opacity of the emergency screen flash. |
| **Audio Alerts** | `Play Sound Alert` | `true` | Plays audio cue on priority prayer changes. |
| | `Magic Sound ID` | `2266` | Sound effect ID for incoming Magic attacks. |
| | `Range Sound ID` | `2266` | Sound effect ID for incoming Range attacks. |
| | `Melee Sound ID` | `2266` | Sound effect ID for incoming Melee attacks. |
| | `Wrong Prayer Emergency Sound` | `true` | Plays emergency sound if unprotected when an attack is 1 tick away. |
| | `Wrong Prayer Sound ID` | `2277` | Sound effect ID for wrong prayer emergency warning. |
| **NPC Threat & World** | `Highlight Attacking NPC` | `true` | Outlines the model/hull of the monster currently attacking you. |
| | `Highlight NPC True Tile` | `true` | Draws the ground true tile of the attacking NPC. |
| **Info Box & Sequences** | `Show Info Box` | `true` | Displays the upcoming threat queue panel. |
| | `Show Manticore/Combo Sequence` | `true` | Displays rapid combo sequences (e.g. Manticore 3-hit combos) in order. |
| | `Track Unknown Projectiles` | `false` | Fallback to Pray Magic for unrecognized projectiles targeting the player. |

---

## Building and Development

### Prerequisites

- **Java 11 through Java 23** (supported by Gradle 8.10).
- If your default system JDK is newer, set `JAVA_HOME` to a compatible JDK (such as JDK 11 or 17):
  ```bash
  export JAVA_HOME=/path/to/compatible/jdk
  ```

### Build Commands

- **Compile and Test**:
  ```bash
  ./gradlew test
  ```
- **Build Plugin JAR**:
  ```bash
  ./gradlew build
  ```
- **Build Shaded / Standalone JAR**:
  ```bash
  ./gradlew shadowJar
  ```
- **Run Local Development Client**:
  Launch RuneLite in developer mode with the plugin preloaded:
  ```bash
  ./gradlew run
  ```
  *(Or execute `MobCheckPluginTest.java` directly from your IDE).*

---

## Developer Tooling & MCP Server

This repository includes a built-in [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server plugin (`.agents/plugins/runelite-osrs-docs/`) that connects AI agents and developer tooling directly to:
1. **[RuneLite API Javadocs](https://static.runelite.net/runelite-api/apidocs/)**
2. **[Old School RuneScape Wiki](https://oldschool.runescape.wiki/)**

### Available MCP Tools

| Tool | Purpose |
| :--- | :--- |
| `search_runelite_javadocs` | Indexed search for classes, interfaces, enums, methods, fields, events, constants, and packages. |
| `get_runelite_javadoc` | Retrieve full Javadoc documentation with method signatures, return types, parameters, and comments. |
| `search_osrs_wiki` | Search the OSRS Wiki for monsters, items, mechanics, drop tables, player slang aliases, quests, and formulas. |
| `get_osrs_wiki_page` | Fetch page extracts, combat mechanics, monster stats, or specific section text. |
| `get_osrs_wiki_sections` | List table of contents and section headings for any wiki page. |

### Running & Testing MCP Tools

```bash
cd .agents/plugins/runelite-osrs-docs
npm test
```