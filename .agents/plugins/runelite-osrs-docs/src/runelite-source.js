/**
 * RuneLite GitHub source search and constants fetcher.
 * Provides tools to search RuneLite core source code and fetch constant files
 * (AnimationID, GraphicID, NpcID, ItemID, etc.) from the runelite/runelite repo.
 */

const GITHUB_API_URL = "https://api.github.com";
const RUNELITE_REPO = "runelite/runelite";
const USER_AGENT = "runelite-osrs-docs-mcp/1.0 (RuneLite Plugin Assistant)";

// Well-known constant file paths with fallback resolution
const CONSTANT_FILES = {
  AnimationID: [
    "runelite-api/src/main/java/net/runelite/api/gameval/AnimationID.java",
    "runelite-api/src/main/java/net/runelite/api/AnimationID.java",
  ],
  GraphicID: [
    "runelite-api/src/main/java/net/runelite/api/GraphicID.java",
    "runelite-api/src/main/java/net/runelite/api/gameval/GraphicID.java",
    "runelite-api/src/main/java/net/runelite/api/gameval/SpotanimID.java",
  ],
  SpotanimID: [
    "runelite-api/src/main/java/net/runelite/api/gameval/SpotanimID.java",
    "runelite-api/src/main/java/net/runelite/api/GraphicID.java",
  ],
  NpcID: [
    "runelite-api/src/main/java/net/runelite/api/gameval/NpcID.java",
    "runelite-api/src/main/java/net/runelite/api/NpcID.java",
  ],
  ItemID: [
    "runelite-api/src/main/java/net/runelite/api/gameval/ItemID.java",
    "runelite-api/src/main/java/net/runelite/api/ItemID.java",
  ],
  ObjectID: [
    "runelite-api/src/main/java/net/runelite/api/gameval/ObjectID.java",
    "runelite-api/src/main/java/net/runelite/api/ObjectID.java",
  ],
  SpriteID: [
    "runelite-api/src/main/java/net/runelite/api/gameval/SpriteID.java",
    "runelite-api/src/main/java/net/runelite/api/SpriteID.java",
  ],
  SoundEffectID: [
    "runelite-api/src/main/java/net/runelite/api/SoundEffectID.java",
  ],
  VarPlayer: [
    "runelite-api/src/main/java/net/runelite/api/gameval/VarPlayerID.java",
    "runelite-api/src/main/java/net/runelite/api/VarPlayer.java",
  ],
  Varbits: [
    "runelite-api/src/main/java/net/runelite/api/gameval/VarbitID.java",
    "runelite-api/src/main/java/net/runelite/api/Varbits.java",
  ],
  VarClientID: [
    "runelite-api/src/main/java/net/runelite/api/gameval/VarClientID.java",
    "runelite-api/src/main/java/net/runelite/api/VarClientInt.java",
  ],
};

/**
 * Search RuneLite source code on GitHub using the code search API.
 */
export async function searchRuneliteSource(query, { repo = RUNELITE_REPO, path = "", language = "java", limit = 15 } = {}) {
  const q = (query || "").trim();
  if (!q) {
    return { results: [], count: 0, query };
  }

  let searchQuery = `${q} repo:${repo}`;
  if (language) {
    searchQuery += ` language:${language}`;
  }
  if (path) {
    searchQuery += ` path:${path}`;
  }

  const url = `${GITHUB_API_URL}/search/code?q=${encodeURIComponent(searchQuery)}&per_page=${Math.min(limit, 30)}`;

  const headers = {
    "User-Agent": USER_AGENT,
    "Accept": "application/vnd.github.v3.text-match+json",
  };

  // Use GITHUB_TOKEN if available for higher rate limits
  const token = process.env.GITHUB_TOKEN;
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(url, { headers });
  if (!res.ok) {
    if (res.status === 403) {
      throw new Error(`GitHub API rate limit exceeded. Set GITHUB_TOKEN env var for higher limits. (HTTP ${res.status})`);
    }
    throw new Error(`GitHub code search failed: HTTP ${res.status}`);
  }

  const data = await res.json();
  const items = data.items || [];

  const results = items.map(item => {
    const result = {
      name: item.name,
      path: item.path,
      repository: item.repository?.full_name || repo,
      url: item.html_url,
      score: item.score || 0,
    };

    // Include text match fragments if available
    if (item.text_matches && item.text_matches.length > 0) {
      result.matches = item.text_matches.map(tm => ({
        fragment: tm.fragment,
      })).slice(0, 3); // Limit to 3 fragments per file
    }

    return result;
  });

  return {
    query: q,
    repo,
    totalCount: data.total_count || 0,
    count: results.length,
    results,
  };
}

/**
 * Fetch a specific RuneLite constants file and optionally filter to matching lines.
 */
export async function getRuneliteConstants(constantsFile, { filter = "", branch = "master" } = {}) {
  if (!constantsFile) {
    // Return available constant file names
    return {
      availableFiles: Object.keys(CONSTANT_FILES),
      hint: "Pass one of these names to fetch the file, e.g. 'AnimationID', 'GraphicID', 'NpcID', 'ItemID'",
    };
  }

  let filePaths = CONSTANT_FILES[constantsFile];
  if (!filePaths) {
    // Try to find a case-insensitive or partial match
    const available = Object.keys(CONSTANT_FILES);
    const lower = constantsFile.toLowerCase().replace(/^(gameval\.)/, "");
    const matchKey = available.find(k => k.toLowerCase() === lower || k.toLowerCase().includes(lower));
    if (matchKey) {
      filePaths = CONSTANT_FILES[matchKey];
    } else {
      throw new Error(
        `Unknown constants file '${constantsFile}'.\n` +
        `Available: ${available.join(", ")}`
      );
    }
  }

  let content = null;
  let resolvedPath = null;

  for (const filePath of filePaths) {
    const url = `https://raw.githubusercontent.com/${RUNELITE_REPO}/${branch}/${filePath}`;
    const res = await fetch(url, {
      headers: { "User-Agent": USER_AGENT },
    });

    if (res.ok) {
      content = await res.text();
      resolvedPath = filePath;
      break;
    }
  }

  if (!content) {
    throw new Error(`Failed to fetch constants for '${constantsFile}'. Attempted paths: ${filePaths.join(", ")}`);
  }

  // If a filter is provided, extract only matching constant declarations
  if (filter) {
    const filterLower = filter.toLowerCase();
    const lines = content.split("\n");
    const matchedLines = [];
    let currentComment = [];

    for (const line of lines) {
      const trimmed = line.trim();

      // Track comments that precede constants
      if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
        currentComment.push(line);
        continue;
      }

      // Check if this line contains a constant matching the filter
      if (trimmed.toLowerCase().includes(filterLower) &&
          (trimmed.includes("static final") || trimmed.includes("="))) {
        // Include preceding comment
        if (currentComment.length > 0) {
          matchedLines.push(...currentComment);
        }
        matchedLines.push(line);
        currentComment = [];
      } else {
        currentComment = [];
      }
    }

    if (matchedLines.length === 0) {
      return {
        file: constantsFile,
        path: resolvedPath,
        filter,
        matchCount: 0,
        content: `No constants matching '${filter}' found in ${constantsFile}.`,
        url: `https://github.com/${RUNELITE_REPO}/blob/${branch}/${resolvedPath}`,
      };
    }

    return {
      file: constantsFile,
      path: resolvedPath,
      filter,
      matchCount: matchedLines.length,
      content: matchedLines.join("\n"),
      url: `https://github.com/${RUNELITE_REPO}/blob/${branch}/${resolvedPath}`,
    };
  }

  // Return full file content (can be large)
  // Truncate if too big for context
  const MAX_CHARS = 60000;
  const truncated = content.length > MAX_CHARS;
  const outputContent = truncated ? content.substring(0, MAX_CHARS) + "\n\n// ... truncated ..." : content;

  return {
    file: constantsFile,
    path: resolvedPath,
    url: `https://github.com/${RUNELITE_REPO}/blob/${branch}/${resolvedPath}`,
    charCount: content.length,
    truncated,
    content: outputContent,
  };
}

/**
 * Search for a specific plugin in RuneLite core or Plugin Hub repos.
 */
export async function searchPluginHubSource(query, { limit = 10 } = {}) {
  const q = (query || "").trim();
  if (!q) {
    return { results: [], count: 0, query };
  }

  // Search both runelite core plugins and plugin-hub
  const repos = [RUNELITE_REPO, "runelite/plugin-hub"];

  const allResults = [];

  for (const repo of repos) {
    try {
      const result = await searchRuneliteSource(q, { repo, limit: Math.ceil(limit / 2) });
      allResults.push(...result.results.map(r => ({ ...r, source: repo })));
    } catch {
      // Skip failed repos (rate limiting etc)
    }
  }

  return {
    query: q,
    count: allResults.length,
    results: allResults.slice(0, limit),
  };
}
