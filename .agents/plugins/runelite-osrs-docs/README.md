# RuneLite API & OSRS Wiki MCP Server

An MCP (Model Context Protocol) server designed for RuneLite plugin development that integrates:
1. **RuneLite API Javadocs** (`https://static.runelite.net/runelite-api/apidocs/`)
2. **Old School RuneScape Wiki** (`https://oldschool.runescape.wiki/`)

## Features & Exposed Tools

| Tool Name | Description |
| :--- | :--- |
| `search_runelite_javadocs` | Search classes, interfaces, enums, methods, fields, events, constants, and packages across RuneLite API javadocs. |
| `get_runelite_javadoc` | Retrieve full class/interface documentation including method signatures, parameters, return types, field constants, and doc comments. |
| `search_osrs_wiki` | Search the OSRS Wiki for monsters, items, mechanics, drop tables, player slang aliases, quests, and formulas. |
| `get_osrs_wiki_page` | Fetch clean article extracts, monster stats, combat mechanics, or specific sections from the OSRS Wiki. |
| `get_osrs_wiki_sections` | List table of contents and section anchors for an OSRS Wiki page. |

## Quick Test

```bash
cd .agents/plugins/runelite-osrs-docs
npm test
```

## Running the Server

```bash
node src/index.js
```
The server runs over standard I/O using the standard JSON-RPC 2.0 MCP protocol.
