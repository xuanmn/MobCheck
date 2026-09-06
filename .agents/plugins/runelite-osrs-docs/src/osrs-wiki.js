/**
 * OSRS Wiki API search and documentation fetcher.
 */

const WIKI_API_URL = "https://oldschool.runescape.wiki/api.php";
const WIKI_BASE_URL = "https://oldschool.runescape.wiki/w/";
const USER_AGENT = "runelite-osrs-docs-mcp/1.0 (RuneLite Plugin Assistant)";

function cleanWikitext(wikitext) {
  if (!wikitext) return "";
  return wikitext
    .replace(/\{\{[^}]*\}\}/g, "") // Remove template macros
    .replace(/\[\[(?:[^|\]]*\|)?([^\]]+)\]\]/g, "$1") // Simplify wiki links [[Target|Text]] -> Text
    .replace(/'''([^']+)'''/g, "**$1**") // Bold
    .replace(/''([^']+)''/g, "*$1*") // Italic
    .replace(/==+\s*([^=]+)\s*==+/g, "\n### $1\n") // Headings
    .replace(/<[^>]+>/g, "") // Strip HTML tags
    .replace(/&nbsp;/g, " ")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&amp;/g, "&")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function cleanHtmlToMarkdown(html) {
  if (!html) return "";
  return html
    .replace(/<a\s+[^>]*href="\/w\/([^"]*)"[^>]*>([\s\S]*?)<\/a>/gi, "[$2](https://oldschool.runescape.wiki/w/$1)")
    .replace(/<a\s+[^>]*href="([^"]*)"[^>]*>([\s\S]*?)<\/a>/gi, "[$2]($1)")
    .replace(/<h([1-6])[^>]*>([\s\S]*?)<\/h\1>/gi, (match, level, content) => {
      const hashes = "#".repeat(Math.min(6, parseInt(level) + 1));
      return `\n\n${hashes} ${content.replace(/<[^>]+>/g, "").trim()}\n\n`;
    })
    .replace(/<li[^>]*>([\s\S]*?)<\/li>/gi, "- $1\n")
    .replace(/<th[^>]*>([\s\S]*?)<\/th>/gi, " **$1** |")
    .replace(/<td[^>]*>([\s\S]*?)<\/td>/gi, " $1 |")
    .replace(/<tr[^>]*>([\s\S]*?)<\/tr>/gi, "$1\n")
    .replace(/<code[^>]*>([\s\S]*?)<\/code>/gi, "`$1`")
    .replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;/g, " ")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&amp;/g, "&")
    .replace(/&quot;/g, "\"")
    .replace(/\n\s*\n\s*\n/g, "\n\n")
    .trim();
}

export async function searchOsrsWiki(query, { limit = 10 } = {}) {
  const q = (query || "").trim();
  if (!q) {
    return { results: [], count: 0, query };
  }

  // 1. Run OpenSearch
  const opensearchUrl = `${WIKI_API_URL}?action=opensearch&search=${encodeURIComponent(q)}&limit=${limit}&format=json`;
  // 2. Run text search with snippets
  const textSearchUrl = `${WIKI_API_URL}?action=query&list=search&srsearch=${encodeURIComponent(q)}&srlimit=${limit}&format=json`;

  const [openRes, textRes] = await Promise.all([
    fetch(opensearchUrl, { headers: { "User-Agent": USER_AGENT } }).then(r => r.json()).catch(() => [[], [], [], []]),
    fetch(textSearchUrl, { headers: { "User-Agent": USER_AGENT } }).then(r => r.json()).catch(() => ({ query: { search: [] } }))
  ]);

  const titles = openRes[1] || [];
  const urls = openRes[3] || [];
  const textMatches = textRes.query?.search || [];

  const resultsMap = new Map();

  // Add opensearch titles
  titles.forEach((title, i) => {
    resultsMap.set(title, {
      title,
      url: urls[i] || `${WIKI_BASE_URL}${encodeURIComponent(title.replace(/\s+/g, "_"))}`,
      snippet: ""
    });
  });

  // Merge full text search snippets
  for (const m of textMatches) {
    const existing = resultsMap.get(m.title) || {
      title: m.title,
      url: `${WIKI_BASE_URL}${encodeURIComponent(m.title.replace(/\s+/g, "_"))}`,
      snippet: ""
    };
    if (m.snippet) {
      existing.snippet = m.snippet.replace(/<span class="searchmatch">([^<]+)<\/span>/g, "**$1**").replace(/<[^>]+>/g, "");
    }
    resultsMap.set(m.title, existing);
  }

  const results = Array.from(resultsMap.values()).slice(0, limit);
  return {
    query: q,
    count: results.length,
    results
  };
}

export async function getOsrsWikiSections(titleOrUrl) {
  let title = titleOrUrl.trim();
  if (title.startsWith("https://oldschool.runescape.wiki/w/")) {
    title = decodeURIComponent(title.replace("https://oldschool.runescape.wiki/w/", "").replace(/_/g, " "));
  }

  const url = `${WIKI_API_URL}?action=parse&page=${encodeURIComponent(title)}&prop=sections&redirects=1&format=json`;
  const res = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
  if (!res.ok) {
    throw new Error(`Failed to fetch wiki sections: HTTP ${res.status}`);
  }
  const data = await res.json();
  if (data.error) {
    throw new Error(`Wiki API error: ${data.error.info || JSON.stringify(data.error)}`);
  }

  const sections = data.parse?.sections || [];
  return {
    title: data.parse?.title || title,
    url: `${WIKI_BASE_URL}${encodeURIComponent((data.parse?.title || title).replace(/\s+/g, "_"))}`,
    sections: sections.map(s => ({
      index: s.index,
      level: s.level,
      name: s.line.replace(/<[^>]+>/g, ""),
      anchor: s.anchor
    }))
  };
}

export async function getOsrsWikiPage(titleOrUrl, { section = null, format = "summary" } = {}) {
  let title = titleOrUrl.trim();
  if (title.startsWith("https://oldschool.runescape.wiki/w/")) {
    title = decodeURIComponent(title.replace("https://oldschool.runescape.wiki/w/", "").replace(/_/g, " "));
  }

  // If a specific section is requested by index or name
  if (section !== null && section !== undefined && section !== "") {
    let sectionIndex = section;

    // If section name given instead of number, resolve section index
    if (isNaN(section)) {
      const secData = await getOsrsWikiSections(title);
      const match = secData.sections.find(s => s.name.toLowerCase() === String(section).toLowerCase() || s.anchor.toLowerCase() === String(section).toLowerCase());
      if (match) {
        sectionIndex = match.index;
      }
    }

    const parseUrl = `${WIKI_API_URL}?action=parse&page=${encodeURIComponent(title)}&section=${sectionIndex}&prop=text|wikitext|headhtml&redirects=1&format=json`;
    const res = await fetch(parseUrl, { headers: { "User-Agent": USER_AGENT } });
    const data = await res.json();

    if (data.error) {
      throw new Error(`Failed to fetch section '${section}': ${data.error.info}`);
    }

    const parsedTitle = data.parse?.title || title;
    const wikitext = data.parse?.wikitext?.["*"] || "";
    const htmlText = data.parse?.text?.["*"] || "";
    const canonicalUrl = `${WIKI_BASE_URL}${encodeURIComponent(parsedTitle.replace(/\s+/g, "_"))}`;

    return {
      title: parsedTitle,
      url: canonicalUrl,
      section: sectionIndex,
      content: cleanWikitext(wikitext) || cleanHtmlToMarkdown(htmlText)
    };
  }

  // Otherwise fetch page extract & infobox overview
  const extractUrl = `${WIKI_API_URL}?action=query&prop=extracts|info&inprop=url&explaintext=1&titles=${encodeURIComponent(title)}&redirects=1&format=json`;
  const res = await fetch(extractUrl, { headers: { "User-Agent": USER_AGENT } });
  const data = await res.json();

  if (data.error) {
    throw new Error(`Wiki API error: ${data.error.info}`);
  }

  const pages = data.query?.pages || {};
  const pageId = Object.keys(pages)[0];
  if (!pageId || pageId === "-1") {
    // Try search to offer suggestions
    const searchRes = await searchOsrsWiki(title, { limit: 5 });
    let errorMsg = `Wiki page '${title}' not found.`;
    if (searchRes.results.length > 0) {
      errorMsg += `\nDid you mean:\n` + searchRes.results.map(r => `- [${r.title}](${r.url})`).join("\n");
    }
    throw new Error(errorMsg);
  }

  const page = pages[pageId];
  const canonicalTitle = page.title;
  const canonicalUrl = page.fullurl || `${WIKI_BASE_URL}${encodeURIComponent(canonicalTitle.replace(/\s+/g, "_"))}`;
  const extract = page.extract || "";

  let markdown = `# ${canonicalTitle}\n\n`;
  markdown += `**Source**: [${canonicalUrl}](${canonicalUrl})\n\n`;
  markdown += `${extract}\n\n`;

  return {
    title: canonicalTitle,
    url: canonicalUrl,
    pageId: page.pageid,
    extract,
    markdown
  };
}

/**
 * Fetch structured monster/NPC combat stats from the OSRS Wiki Infobox Monster template.
 * Parses wikitext to extract: combat level, hitpoints, attack style, attack speed,
 * max hit, aggressive, poisonous, immune to poison/venom, attack/strength/defence/
 * magic/ranged levels, and any listed projectile/animation IDs from the page content.
 */
export async function getOsrsWikiMonsterStats(monsterName) {
  if (!monsterName) {
    throw new Error("Monster name is required.");
  }

  let title = monsterName.trim();
  if (title.startsWith("https://oldschool.runescape.wiki/w/")) {
    title = decodeURIComponent(title.replace("https://oldschool.runescape.wiki/w/", "").replace(/_/g, " "));
  }

  // Fetch the raw wikitext to parse the Infobox Monster template
  const url = `${WIKI_API_URL}?action=parse&page=${encodeURIComponent(title)}&prop=wikitext&redirects=1&format=json`;
  const res = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
  if (!res.ok) {
    throw new Error(`Failed to fetch wiki page: HTTP ${res.status}`);
  }
  const data = await res.json();
  if (data.error) {
    // Try searching for suggestions
    const searchRes = await searchOsrsWiki(title, { limit: 5 });
    let errorMsg = `Wiki page '${title}' not found.`;
    if (searchRes.results.length > 0) {
      errorMsg += `\nDid you mean:\n` + searchRes.results.map(r => `- [${r.title}](${r.url})`).join("\n");
    }
    throw new Error(errorMsg);
  }

  const wikitext = data.parse?.wikitext?.["*"] || "";
  const parsedTitle = data.parse?.title || title;
  const canonicalUrl = `${WIKI_BASE_URL}${encodeURIComponent(parsedTitle.replace(/\s+/g, "_"))}`;

  // Isolate Infobox Monster template block(s) to avoid capturing fields from pet or drop infoboxes
  const infoboxMatches = wikitext.match(/\{\{Infobox Monster[\s\S]*?\n\}\}/gi) || [];
  const infoboxText = infoboxMatches.join("\n") || wikitext;

  // Parse all Infobox Monster template parameters including versioned attributes
  const paramRegex = /\|\s*([a-zA-Z0-9_ ]+?)\s*=\s*([^|\n{}]+)/gi;
  const rawParams = {};
  let pMatch;
  while ((pMatch = paramRegex.exec(infoboxText)) !== null) {
    const k = pMatch[1].trim().toLowerCase().replace(/\s+/g, "_");
    const v = pMatch[2]
      .replace(/\[\[(?:[^|\]]*\|)?([^\]]+)\]\]/g, "$1")
      .replace(/\{\{[^}]*\}\}/g, "")
      .replace(/<[^>]+>/g, "")
      .trim();
    if (v && v !== "N/A" && v !== "No") {
      rawParams[k] = v;
    }
  }

  // Detect if monster has multiple versions/forms/phases (version1, version2, etc.)
  const versionKeys = Object.keys(rawParams).filter(k => /^version\d+$/.test(k));
  let maxVersionIndex = 0;
  if (versionKeys.length > 0) {
    maxVersionIndex = Math.max(...versionKeys.map(k => parseInt(k.replace("version", ""))));
  } else {
    // Check if attack_speed1 or combat1 exists even if version1 is not explicitly named
    const indexedKeys = Object.keys(rawParams).filter(k => /^(?:attack_speed|attack_style|max_hit|combat|id|npcid|hitpoints)\d+$/.test(k));
    if (indexedKeys.length > 0) {
      maxVersionIndex = Math.max(...indexedKeys.map(k => parseInt(k.match(/\d+$/)[0])));
    }
  }

  const versions = [];
  if (maxVersionIndex > 0) {
    for (let i = 1; i <= maxVersionIndex; i++) {
      const vName = rawParams[`version${i}`] || `Form ${i}`;
      const vCombat = rawParams[`combat${i}`] || rawParams["combat"] || "N/A";
      const vStyle = rawParams[`attack_style${i}`] || rawParams[`attackstyle${i}`] || rawParams["attack_style"] || rawParams["attackstyle"] || "N/A";
      const vSpeed = rawParams[`attack_speed${i}`] || rawParams[`attspeed${i}`] || rawParams["attack_speed"] || rawParams["attspeed"] || "N/A";
      const vMaxHit = rawParams[`max_hit${i}`] || rawParams[`maxhit${i}`] || rawParams["max_hit"] || rawParams["maxhit"] || "N/A";
      const vHp = rawParams[`hitpoints${i}`] || rawParams["hitpoints"] || "N/A";
      const vId = rawParams[`id${i}`] || rawParams[`npcid${i}`] || rawParams["id"] || rawParams["npcid"] || "N/A";
      const vSize = rawParams[`size${i}`] || rawParams["size"] || "N/A";

      versions.push({
        index: i,
        name: vName,
        combat: vCombat,
        attackStyle: vStyle,
        attackSpeed: vSpeed,
        maxHit: vMaxHit,
        hitpoints: vHp,
        npcId: vId,
        size: vSize,
      });
    }
  }

  // Primary stats (from first version or base fields)
  const primaryVersion = versions[0];
  const stats = {
    name: rawParams["name"] || parsedTitle,
    combat: primaryVersion?.combat !== "N/A" ? primaryVersion?.combat : (rawParams["combat"] || "N/A"),
    hitpoints: primaryVersion?.hitpoints !== "N/A" ? primaryVersion?.hitpoints : (rawParams["hitpoints"] || "N/A"),
    attack_style: primaryVersion?.attackStyle !== "N/A" ? primaryVersion?.attackStyle : (rawParams["attack_style"] || rawParams["attackstyle"] || "N/A"),
    attack_speed: primaryVersion?.attackSpeed !== "N/A" ? primaryVersion?.attackSpeed : (rawParams["attack_speed"] || rawParams["attspeed"] || "N/A"),
    max_hit: primaryVersion?.maxHit !== "N/A" ? primaryVersion?.maxHit : (rawParams["max_hit"] || rawParams["maxhit"] || "N/A"),
    size: primaryVersion?.size !== "N/A" ? primaryVersion?.size : (rawParams["size"] || "N/A"),
    id: primaryVersion?.npcId !== "N/A" ? primaryVersion?.npcId : (rawParams["id"] || rawParams["npcid"] || "N/A"),
    aggressive: rawParams["aggressive"] || "N/A",
    poisonous: rawParams["poisonous"] || "N/A",
    immunepoison: rawParams["immunepoison"] || "N/A",
    immunevenom: rawParams["immunevenom"] || "N/A",
    slaylvl: rawParams["slaylvl"] || "N/A",
    slayxp: rawParams["slayxp"] || "N/A",
    examine: rawParams["examine"] || "N/A",
  };

  // Extract referenced IDs
  const idReferences = {};
  const projMatches = [...wikitext.matchAll(/projectile\s*(?:id|ID)?\s*[:=]?\s*(\d{2,5})/gi)];
  if (projMatches.length > 0) {
    idReferences.projectileIds = [...new Set(projMatches.map(m => parseInt(m[1])))];
  }
  const animMatches = [...wikitext.matchAll(/animation\s*(?:id|ID)?\s*[:=]?\s*(\d{3,5})/gi)];
  if (animMatches.length > 0) {
    idReferences.animationIds = [...new Set(animMatches.map(m => parseInt(m[1])))];
  }
  const graphicMatches = [...wikitext.matchAll(/(?:graphic|spotanim|spot animation)\s*(?:id|ID)?\s*[:=]?\s*(\d{2,5})/gi)];
  if (graphicMatches.length > 0) {
    idReferences.graphicIds = [...new Set(graphicMatches.map(m => parseInt(m[1])))];
  }

  // Collect all NPC IDs
  const allNpcIds = [];
  if (rawParams["id"]) allNpcIds.push(...rawParams["id"].split(",").map(s => s.trim()));
  if (rawParams["npcid"]) allNpcIds.push(...rawParams["npcid"].split(",").map(s => s.trim()));
  for (let i = 1; i <= maxVersionIndex; i++) {
    if (rawParams[`id${i}`]) allNpcIds.push(...rawParams[`id${i}`].split(",").map(s => s.trim()));
    if (rawParams[`npcid${i}`]) allNpcIds.push(...rawParams[`npcid${i}`].split(",").map(s => s.trim()));
  }
  const parsedNpcIds = [...new Set(allNpcIds.filter(id => /^\d+$/.test(id)).map(id => parseInt(id)))];
  if (parsedNpcIds.length > 0) {
    idReferences.npcIds = parsedNpcIds;
  }

  // Build markdown output
  let markdown = `# ${parsedTitle} — Monster Stats\n\n`;
  markdown += `**Source**: [${canonicalUrl}](${canonicalUrl})\n\n`;

  // Overview Table
  markdown += `## Overview\n\n`;
  markdown += `| Stat | Value |\n`;
  markdown += `| :--- | :--- |\n`;
  markdown += `| Combat Level | ${stats.combat} |\n`;
  markdown += `| Hitpoints | ${stats.hitpoints} |\n`;
  markdown += `| Attack Style | ${stats.attack_style} |\n`;
  markdown += `| Attack Speed | ${stats.attack_speed} ticks |\n`;
  markdown += `| Max Hit | ${stats.max_hit} |\n`;
  markdown += `| Size | ${stats.size} |\n`;
  if (stats.id !== "N/A") markdown += `| NPC ID(s) | ${stats.id} |\n`;
  if (stats.aggressive !== "N/A") markdown += `| Aggressive | ${stats.aggressive} |\n`;
  if (stats.poisonous !== "N/A") markdown += `| Poisonous | ${stats.poisonous} |\n`;
  if (stats.immunepoison !== "N/A") markdown += `| Poison Immune | ${stats.immunepoison} |\n`;
  if (stats.immunevenom !== "N/A") markdown += `| Venom Immune | ${stats.immunevenom} |\n`;
  markdown += `\n`;

  // If multiple forms/versions exist, render detailed table
  if (versions.length > 1) {
    markdown += `## Forms / Phases (${versions.length})\n\n`;
    markdown += `| Form / Version | Combat | HP | Attack Style | Attack Speed (ticks) | Max Hit | NPC ID(s) |\n`;
    markdown += `| :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n`;
    for (const v of versions) {
      markdown += `| ${v.name} | ${v.combat} | ${v.hitpoints} | ${v.attackStyle} | ${v.attackSpeed} | ${v.maxHit} | ${v.npcId} |\n`;
    }
    markdown += `\n`;
  }

  // Referenced IDs
  if (Object.keys(idReferences).length > 0) {
    markdown += `## Referenced IDs\n\n`;
    if (idReferences.npcIds && idReferences.npcIds.length > 0) {
      markdown += `**NPC IDs**: \`${idReferences.npcIds.join(", ")}\`\n\n`;
    }
    if (idReferences.projectileIds && idReferences.projectileIds.length > 0) {
      markdown += `**Projectile IDs**: \`${idReferences.projectileIds.join(", ")}\`\n\n`;
    }
    if (idReferences.animationIds && idReferences.animationIds.length > 0) {
      markdown += `**Animation IDs**: \`${idReferences.animationIds.join(", ")}\`\n\n`;
    }
    if (idReferences.graphicIds && idReferences.graphicIds.length > 0) {
      markdown += `**Graphic / SpotAnim IDs**: \`${idReferences.graphicIds.join(", ")}\`\n\n`;
    }
  }

  return {
    title: parsedTitle,
    url: canonicalUrl,
    stats,
    versionCount: Math.max(1, versions.length),
    versions,
    idReferences,
    markdown,
  };
}
