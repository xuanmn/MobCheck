# Mob Check Plugin

A dynamic PvM Priority Prayer Helper plugin for the [RuneLite](https://runelite.net/) client, specialized for high-stakes encounters like **The Inferno** and **Fortis Colosseum**. It tracks and displays the exact ticks remaining before incoming NPC projectile attacks and melee attacks hit the player, helping you time protection prayers and 1-tick prayer flicking accurately.

---

## Features

- **Overhead Countdown Overlay**: Renders the style-specific protection prayer icon (Magic, Range, Melee) and remaining tick countdown directly above the player's character. Turns red when within the warning threshold.
- **Sidebar Info Box**: Displays a clean panel showing the sequence of upcoming active threats, attack styles, and ticks remaining.
- **Multi-Threat Priority Queue**: Automatically sorts all concurrent incoming threats by arrival tick so you can execute tick-perfect prayer flicks (e.g. Manticore 3-hit sequences or alternating Mage/Ranger waves).
- **Audio Alerts**: Plays a configurable sound effect when the priority prayer style switches, keeping you alerted during chaotic wave solves.
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

| Setting | Default | Description |
| :--- | :--- | :--- |
| **Show Overhead** | `true` | Renders the prayer icon and ticks remaining above your character. |
| **Show Info Box** | `true` | Displays the sidebar info panel with the upcoming attack queue. |
| **Play Sound Alert** | `true` | Plays an audio cue when the priority prayer style switches. |
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