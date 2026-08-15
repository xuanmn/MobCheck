/**
 * RuneLite Javadocs search and doc fetcher.
 */

const BASE_URL = "https://static.runelite.net/runelite-api/apidocs/";

let typeIndex = null;
let memberIndex = null;
let packageIndex = null;
let lastIndexFetch = 0;
const INDEX_TTL = 1000 * 60 * 60; // 1 hour

async function fetchIndex(filename, varName) {
  const res = await fetch(`${BASE_URL}${filename}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch ${filename}: HTTP ${res.status}`);
  }
  const text = await res.text();
  const match = text.match(new RegExp(`${varName}\\s*=\\s*(\\[[\\s\\S]*\\])`));
  if (!match) {
    throw new Error(`Failed to parse ${filename}`);
  }
  return JSON.parse(match[1]);
}

export async function ensureIndices() {
  const now = Date.now();
  if (typeIndex && memberIndex && packageIndex && (now - lastIndexFetch < INDEX_TTL)) {
    return;
  }

  const [types, members, packages] = await Promise.all([
    fetchIndex("type-search-index.js", "typeSearchIndex").catch(() => []),
    fetchIndex("member-search-index.js", "memberSearchIndex").catch(() => []),
    fetchIndex("package-search-index.js", "packageSearchIndex").catch(() => [])
  ]);

  typeIndex = types.filter(t => t.p || t.l);
  memberIndex = members.filter(m => m.p || m.c || m.l);
  packageIndex = packages.filter(p => p.l);
  lastIndexFetch = now;
}

export async function searchRuneliteJavadocs(query, { category = "all", limit = 20 } = {}) {
  await ensureIndices();
  const q = (query || "").trim().toLowerCase();
  if (!q) {
    return { results: [], count: 0, query };
  }

  const results = [];

  // 1. Search Types (classes, interfaces, enums)
  if (category === "all" || category === "types" || category === "classes") {
    for (const item of typeIndex) {
      if (!item.l) continue;
      const label = item.l;
      const pkg = item.p || "";
      const fullName = pkg ? `${pkg}.${label}` : label;
      const lower = label.toLowerCase();
      const lowerFull = fullName.toLowerCase();

      if (lower.includes(q) || lowerFull.includes(q)) {
        let score = 10;
        if (lower === q) score = 100;
        else if (lower.startsWith(q)) score = 50;
        else if (lowerFull.includes(q)) score = 20;

        const path = item.url || (pkg ? `${pkg.replace(/\./g, "/")}/${label}.html` : `${label}.html`);
        results.push({
          category: "type",
          name: label,
          package: pkg,
          fullName,
          url: `${BASE_URL}${path}`,
          path,
          score
        });
      }
    }
  }

  // 2. Search Members (methods, fields, constants)
  if (category === "all" || category === "members" || category === "methods" || category === "fields") {
    for (const item of memberIndex) {
      if (!item.l) continue;
      const member = item.l;
      const cls = item.c || "";
      const pkg = item.p || "";
      const fullName = `${pkg ? pkg + "." : ""}${cls ? cls + "#" : ""}${member}`;
      const lower = member.toLowerCase();
      const lowerFull = fullName.toLowerCase();

      if (lower.includes(q) || lowerFull.includes(q)) {
        let score = 5;
        if (lower === q) score = 40;
        else if (lower.startsWith(q)) score = 25;
        else if (lowerFull.includes(q)) score = 15;

        const classPath = pkg ? `${pkg.replace(/\./g, "/")}/${cls}.html` : `${cls}.html`;
        const path = item.url || `${classPath}#${member}`;
        results.push({
          category: "member",
          name: member,
          className: cls,
          package: pkg,
          fullName,
          url: `${BASE_URL}${path}`,
          path,
          score
        });
      }
    }
  }

  // 3. Search Packages
  if (category === "all" || category === "packages") {
    for (const item of packageIndex) {
      if (!item.l) continue;
      const pkg = item.l;
      const lower = pkg.toLowerCase();

      if (lower.includes(q)) {
        let score = 2;
        if (lower === q) score = 30;
        else if (lower.startsWith(q)) score = 15;

        const path = item.url || `${pkg.replace(/\./g, "/")}/package-summary.html`;
        results.push({
          category: "package",
          name: pkg,
          fullName: pkg,
          url: `${BASE_URL}${path}`,
          path,
          score
        });
      }
    }
  }

  // Sort by relevance score desc
  results.sort((a, b) => b.score - a.score || a.name.length - b.name.length);

  const truncated = results.slice(0, Math.min(limit, 50));
  return {
    query,
    count: results.length,
    results: truncated
  };
}

function cleanHtmlText(html) {
  if (!html) return "";
  return html
    .replace(/<a\s+[^>]*href="([^"]*)"[^>]*>([\s\S]*?)<\/a>/gi, "[$2]($1)")
    .replace(/<code[^>]*>([\s\S]*?)<\/code>/gi, "`$1`")
    .replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;/g, " ")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&amp;/g, "&")
    .replace(/&quot;/g, "\"")
    .replace(/&#39;/g, "'")
    .replace(/\s+/g, " ")
    .trim();
}

export async function getRuneliteJavadoc(target) {
  if (!target) {
    throw new Error("Target class name or path is required.");
  }

  let cleanedTarget = target.trim();
  if (cleanedTarget.startsWith("https://static.runelite.net/runelite-api/apidocs/")) {
    cleanedTarget = cleanedTarget.replace("https://static.runelite.net/runelite-api/apidocs/", "");
  }

  // If simple name like "NPC" or "Client" or "net.runelite.api.NPC", resolve path
  let path = cleanedTarget;
  if (!path.endsWith(".html")) {
    await ensureIndices();
    const exact = typeIndex.find(t => t.l.toLowerCase() === cleanedTarget.toLowerCase() || 
      `${t.p}.${t.l}`.toLowerCase() === cleanedTarget.toLowerCase());
    
    if (exact) {
      path = exact.p ? `${exact.p.replace(/\./g, "/")}/${exact.l}.html` : `${exact.l}.html`;
    } else {
      // Try replacing dots with slashes
      path = `${cleanedTarget.replace(/\./g, "/")}.html`;
    }
  }

  const url = `${BASE_URL}${path}`;
  const res = await fetch(url);
  if (!res.ok) {
    // If not found, try searching
    const searchRes = await searchRuneliteJavadocs(cleanedTarget, { category: "types", limit: 5 });
    let errorMsg = `Javadoc not found at ${url} (HTTP ${res.status}).`;
    if (searchRes.results.length > 0) {
      errorMsg += `\nDid you mean one of these?\n` + searchRes.results.map(r => `- ${r.fullName} (${r.url})`).join("\n");
    }
    throw new Error(errorMsg);
  }

  const html = await res.text();

  // Parse HTML
  let title = "";
  const titleMatch = html.match(/<title>([\s\S]*?)<\/title>/i);
  if (titleMatch) {
    title = cleanHtmlText(titleMatch[1]);
  }

  let pkg = "";
  const pkgMatch = html.match(/<div class="subTitle"><span class="packageLabelInType">Package<\/span>&nbsp;([\s\S]*?)<\/div>/i)
    || html.match(/<div class="header">\s*<div class="subTitle">([\s\S]*?)<\/div>/i);
  if (pkgMatch) {
    pkg = cleanHtmlText(pkgMatch[1]);
  }

  let typeSignature = "";
  const sigMatch = html.match(/<pre>([\s\S]*?)<\/pre>/i);
  if (sigMatch) {
    typeSignature = cleanHtmlText(sigMatch[1]);
  }

  let classDesc = "";
  const descMatch = html.match(/<div class="description">\s*<ul class="blockList">\s*<li class="blockList">([\s\S]*?)<\/li>\s*<\/ul>\s*<\/div>/i);
  if (descMatch) {
    classDesc = cleanHtmlText(descMatch[1]);
  }

  // Parse Method Details
  const methodDetails = [];
  const methodRegex = /<a id="([^"]+)">\s*<!--   -->\s*<\/a>\s*<ul class="blockList">\s*<li class="blockList">\s*<h4>([^<]+)<\/h4>\s*<pre class="methodSignature">([\s\S]*?)<\/pre>([\s\S]*?)<\/li>\s*<\/ul>/gi;
  let match;
  while ((match = methodRegex.exec(html)) !== null) {
    const id = match[1];
    const name = cleanHtmlText(match[2]);
    const signature = cleanHtmlText(match[3]);
    const docBlock = match[4];

    let desc = "";
    const blockMatch = docBlock.match(/<div class="block">([\s\S]*?)<\/div>/i);
    if (blockMatch) {
      desc = cleanHtmlText(blockMatch[1]);
    }

    let returnVal = "";
    const returnMatch = docBlock.match(/<dt><span class="returnLabel">Returns:<\/span><\/dt>\s*<dd>([\s\S]*?)<\/dd>/i);
    if (returnMatch) {
      returnVal = cleanHtmlText(returnMatch[1]);
    }

    const params = [];
    const paramRegex = /<dt><span class="paramLabel">Parameters:<\/span><\/dt>\s*<dd><code>([^<]+)<\/code>\s*-\s*([\s\S]*?)<\/dd>/gi;
    let pMatch;
    while ((pMatch = paramRegex.exec(docBlock)) !== null) {
      params.push({ name: cleanHtmlText(pMatch[1]), description: cleanHtmlText(pMatch[2]) });
    }

    methodDetails.push({
      id,
      name,
      signature,
      description: desc,
      returns: returnVal,
      params
    });
  }

  // Parse Field Details
  const fieldDetails = [];
  const fieldRegex = /<a id="([^"]+)">\s*<!--   -->\s*<\/a>\s*<ul class="blockList(?:Last)?">\s*<li class="blockList">\s*<h4>([^<]+)<\/h4>\s*<pre>([\s\S]*?)<\/pre>([\s\S]*?)<\/li>\s*<\/ul>/gi;
  let fMatch;
  while ((fMatch = fieldRegex.exec(html)) !== null) {
    const id = fMatch[1];
    const name = cleanHtmlText(fMatch[2]);
    const signature = cleanHtmlText(fMatch[3]);
    const docBlock = fMatch[4];
    let desc = "";
    const blockMatch = docBlock.match(/<div class="block">([\s\S]*?)<\/div>/i);
    if (blockMatch) {
      desc = cleanHtmlText(blockMatch[1]);
    }
    fieldDetails.push({
      id,
      name,
      signature,
      description: desc
    });
  }

  // Format Markdown output
  let markdown = `# ${title}\n\n`;
  markdown += `**Source**: [${url}](${url})\n\n`;
  if (pkg) markdown += `**Package**: \`${pkg}\`\n\n`;
  if (typeSignature) {
    markdown += `\`\`\`java\n${typeSignature}\n\`\`\`\n\n`;
  }
  if (classDesc) {
    markdown += `### Overview\n${classDesc}\n\n`;
  }

  if (fieldDetails.length > 0) {
    markdown += `### Fields & Constants (${fieldDetails.length})\n\n`;
    for (const f of fieldDetails) {
      markdown += `- \`${f.signature || f.name}\``;
      if (f.description) markdown += `: ${f.description}`;
      markdown += `\n`;
    }
    markdown += `\n`;
  }

  if (methodDetails.length > 0) {
    markdown += `### Methods (${methodDetails.length})\n\n`;
    for (const m of methodDetails) {
      markdown += `#### \`${m.signature || m.name}\`\n`;
      if (m.description) markdown += `${m.description}\n\n`;
      if (m.params.length > 0) {
        markdown += `**Parameters:**\n`;
        for (const p of m.params) {
          markdown += `- \`${p.name}\`: ${p.description}\n`;
        }
        markdown += `\n`;
      }
      if (m.returns) {
        markdown += `**Returns:** ${m.returns}\n\n`;
      }
    }
  }

  return {
    title,
    package: pkg,
    url,
    path,
    typeSignature,
    methodCount: methodDetails.length,
    fieldCount: fieldDetails.length,
    markdown
  };
}
