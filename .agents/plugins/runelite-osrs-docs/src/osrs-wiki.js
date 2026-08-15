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
