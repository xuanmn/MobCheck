#!/usr/bin/env node

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

import { searchRuneliteJavadocs, getRuneliteJavadoc } from "./runelite-docs.js";
import { searchOsrsWiki, getOsrsWikiPage, getOsrsWikiSections, getOsrsWikiMonsterStats } from "./osrs-wiki.js";
import { searchRuneliteSource, getRuneliteConstants, searchPluginHubSource } from "./runelite-source.js";

const server = new Server(
  {
    name: "runelite-osrs-docs",
    version: "1.1.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// Define available tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "search_runelite_javadocs",
        description: "Search RuneLite API Javadocs (https://static.runelite.net/runelite-api/apidocs/) for classes, interfaces, enums, methods, fields, events, constants, and packages.",
        inputSchema: {
          type: "object",
          properties: {
            query: {
              type: "string",
              description: "Search term (e.g. 'NPC', 'Client', 'getNpcs', 'ActorDeath', 'WorldType', 'MenuAction')",
            },
            category: {
              type: "string",
              enum: ["all", "types", "members", "packages"],
              description: "Filter by category: 'types' (classes/interfaces), 'members' (methods/fields), 'packages', or 'all'. Default is 'all'.",
            },
            limit: {
              type: "number",
              description: "Maximum number of results to return (default 20, max 50).",
            },
          },
          required: ["query"],
        },
      },
      {
        name: "get_runelite_javadoc",
        description: "Fetch and format complete RuneLite Javadoc documentation for a class, interface, or enum (including method signatures, parameters, return types, fields, and doc comments).",
        inputSchema: {
          type: "object",
          properties: {
            target: {
              type: "string",
              description: "Class name (e.g. 'NPC', 'Client', 'net.runelite.api.Actor', 'events.HitsplatApplied') or full URL/path.",
            },
          },
          required: ["target"],
        },
      },
      {
        name: "search_runelite_source",
        description: "Search RuneLite source code on GitHub (runelite/runelite) for usages, constant definitions, event handlers, and official plugin implementations.",
        inputSchema: {
          type: "object",
          properties: {
            query: {
              type: "string",
              description: "Code search term or expression (e.g. 'ProjectileID.MANTICORE', 'AnimationID.JAL_IMKOT', 'onProjectileMoved')",
            },
            repo: {
              type: "string",
              description: "Target GitHub repository (default 'runelite/runelite').",
            },
            path: {
              type: "string",
              description: "Optional file path filter (e.g. 'runelite-client/src/main/java/net/runelite/client/plugins')",
            },
            language: {
              type: "string",
              description: "Programming language filter (default 'java').",
            },
            limit: {
              type: "number",
              description: "Maximum number of results (default 15).",
            },
          },
          required: ["query"],
        },
      },
      {
        name: "get_runelite_constants",
        description: "Fetch official RuneLite constant definitions (AnimationID, ProjectileID, NpcID, ItemID, ObjectID, GraphicID, etc.) with optional keyword filtering.",
        inputSchema: {
          type: "object",
          properties: {
            constantsFile: {
              type: "string",
              description: "Constant file name (e.g. 'AnimationID', 'ProjectileID', 'NpcID', 'ItemID', 'gameval.AnimationID', 'gameval.ProjectileID'). Leave empty to list available files.",
            },
            filter: {
              type: "string",
              description: "Optional keyword to filter constants by name/comment (e.g. 'manticore', 'jad', 'inferno', 'zulrah').",
            },
            branch: {
              type: "string",
              description: "Git branch to fetch from (default 'master').",
            },
          },
        },
      },
      {
        name: "search_plugin_hub_source",
        description: "Search Plugin Hub open-source plugins for prior art, combat mechanics implementations, and example plugin architectures.",
        inputSchema: {
          type: "object",
          properties: {
            query: {
              type: "string",
              description: "Search term across Plugin Hub plugins (e.g. 'Inferno', 'Colosseum', 'TrueTileOverlay', 'PrayerHelper')",
            },
            limit: {
              type: "number",
              description: "Maximum number of results (default 10).",
            },
          },
          required: ["query"],
        },
      },
      {
        name: "search_osrs_wiki",
        description: "Search the Old School RuneScape Wiki (https://oldschool.runescape.wiki/) for articles, monsters, items, mechanics, quests, player slang, and formulas.",
        inputSchema: {
          type: "object",
          properties: {
            query: {
              type: "string",
              description: "Search query (e.g. 'Abyssal demon', 'Superior Slayer Monsters', 'Tick manipulation', 'Unstable Orb')",
            },
            limit: {
              type: "number",
              description: "Maximum number of search results (default 10).",
            },
          },
          required: ["query"],
        },
      },
      {
        name: "get_osrs_wiki_page",
        description: "Fetch article content, combat mechanics, monster stats, drop tables, or section text from the Old School RuneScape Wiki.",
        inputSchema: {
          type: "object",
          properties: {
            titleOrUrl: {
              type: "string",
              description: "Wiki page title or URL (e.g. 'Abyssal demon', 'Damage per second/Melee', 'https://oldschool.runescape.wiki/w/Vorkath')",
            },
            section: {
              type: "string",
              description: "Optional section title (e.g. 'Combat', 'Drops', 'Strategy') or numerical section index.",
            },
          },
          required: ["titleOrUrl"],
        },
      },
      {
        name: "get_osrs_wiki_sections",
        description: "List the table of contents and section headings for an Old School RuneScape Wiki page.",
        inputSchema: {
          type: "object",
          properties: {
            titleOrUrl: {
              type: "string",
              description: "Wiki page title or URL (e.g. 'Abyssal demon', 'Tombs of Amascut')",
            },
          },
          required: ["titleOrUrl"],
        },
      },
      {
        name: "get_osrs_wiki_monster_stats",
        description: "Fetch structured monster combat statistics (combat level, hitpoints, attack speed, max hit, attack styles, weakness, projectile/animation IDs) from the OSRS Wiki.",
        inputSchema: {
          type: "object",
          properties: {
            monsterName: {
              type: "string",
              description: "Monster/boss name or wiki URL (e.g. 'JalTok-Jad', 'Phantom Muspah', 'Tormented Demon', 'Vorkath')",
            },
          },
          required: ["monsterName"],
        },
      },
    ],
  };
});

// Handle tool executions
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case "search_runelite_javadocs": {
        const query = args?.query || "";
        const category = args?.category || "all";
        const limit = typeof args?.limit === "number" ? args.limit : 20;
        const result = await searchRuneliteJavadocs(query, { category, limit });
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "get_runelite_javadoc": {
        const target = args?.target || "";
        const doc = await getRuneliteJavadoc(target);
        return {
          content: [
            {
              type: "text",
              text: doc.markdown,
            },
          ],
        };
      }

      case "search_runelite_source": {
        const query = args?.query || "";
        const repo = args?.repo || "runelite/runelite";
        const path = args?.path || "";
        const language = args?.language || "java";
        const limit = typeof args?.limit === "number" ? args.limit : 15;
        const result = await searchRuneliteSource(query, { repo, path, language, limit });
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "get_runelite_constants": {
        const constantsFile = args?.constantsFile || "";
        const filter = args?.filter || "";
        const branch = args?.branch || "master";
        const result = await getRuneliteConstants(constantsFile, { filter, branch });
        return {
          content: [
            {
              type: "text",
              text: result.content || JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "search_plugin_hub_source": {
        const query = args?.query || "";
        const limit = typeof args?.limit === "number" ? args.limit : 10;
        const result = await searchPluginHubSource(query, { limit });
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "search_osrs_wiki": {
        const query = args?.query || "";
        const limit = typeof args?.limit === "number" ? args.limit : 10;
        const result = await searchOsrsWiki(query, { limit });
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "get_osrs_wiki_page": {
        const titleOrUrl = args?.titleOrUrl || "";
        const section = args?.section || null;
        const page = await getOsrsWikiPage(titleOrUrl, { section });
        return {
          content: [
            {
              type: "text",
              text: page.markdown || page.content || JSON.stringify(page, null, 2),
            },
          ],
        };
      }

      case "get_osrs_wiki_sections": {
        const titleOrUrl = args?.titleOrUrl || "";
        const result = await getOsrsWikiSections(titleOrUrl);
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "get_osrs_wiki_monster_stats": {
        const monsterName = args?.monsterName || "";
        const result = await getOsrsWikiMonsterStats(monsterName);
        return {
          content: [
            {
              type: "text",
              text: result.markdown || JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      default:
        throw new Error(`Unknown tool name: ${name}`);
    }
  } catch (err) {
    return {
      isError: true,
      content: [
        {
          type: "text",
          text: `Error executing ${name}: ${err.message}`,
        },
      ],
    };
  }
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("RuneLite & OSRS Wiki MCP Server running on stdio");
}

main().catch((err) => {
  console.error("Fatal server error:", err);
  process.exit(1);
});
