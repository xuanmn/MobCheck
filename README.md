# Mob Check Plugin

A dynamic PvM Priority Prayer Helper plugin for the [RuneLite](https://runelite.net/) client. It calculates and displays the exact ticks remaining before incoming NPC projectile attacks and immediate melee attacks hit the player, helping you time protection prayers and prayer flicking accurately.

---

## Features

- **Overhead Countdown Overlay**: Renders the style-specific protection prayer icon (Magic, Range, Melee) and remaining tick countdown directly above the player's character.
- **Sidebar Info Box**: Displays a clean panel showing the current priority threat, incoming attack style, and ticks remaining.
- **Dynamic Threat Priority Engine**: Compares all concurrent threats (incoming projectiles and immediate melee animations) to surface the attack landing first.
- **Audio Alerts**: Plays a configurable sound effect when the priority prayer style switches, keeping you alerted during chaotic fights.
- **Broad Boss & Encounter Coverage**:
  - **Inferno**: Jal-Zek (Magic), Jal-Xil (Range), and JalTok-Jad (Magic & Range).
  - **Fortis Colosseum**: Serpent Shaman, Javelinic Colossus, Manticore (tri-attack sequence), Jaguar Warrior, Minotaur, Shockwave Colossus, Fremennik Warband, and Sol Heredit.
  - **Zulrah**: Magic and Range attack phases, plus Snakeling projectiles.
  - **Vorkath**: Standard Magic, Range, and Dragonfire projectiles.
  - **Cerberus**: Melee animations, Magic, and Range ghost/standard attacks.
  - **Alchemical Hydra**: Magic and Range style transitions.
  - **The Gauntlet / Corrupted Hunllef**: Magic and Range projectile cycles.
  - **Demonic Gorillas**: Style transition detection.
  - **God Wars Dungeon**: Commander Zilyana, General Graardor, and K'ril Tsutsaroth.
  - **General Combat**: Standard spell and projectile tracking for generic NPCs and slayer monsters (Abyssal demons, Bloodvelds, etc.).

---

## Configuration Options

Inside the RuneLite Configuration Panel under **Mob Check**, you can customize:

| Setting | Default | Description |
| :--- | :--- | :--- |
| **Show Overhead** | `true` | Renders the prayer icon and ticks remaining above your character. |
| **Show Info Box** | `true` | Displays the sidebar info panel. |
| **Play Sound Alert** | `false` | Plays an audio cue when the priority prayer style switches. |
| **Sound Effect ID** | `2266` | Sound effect ID to play on style switch (default: Grand Exchange plop). |
| **Warning Threshold** | `1` | Tick threshold at which the indicator turns red for immediate urgency. |
| **Track Unknown Projectiles** | `false` | Fallback protection prayer for unrecognized projectiles targeting the player. |

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