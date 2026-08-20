import { searchRuneliteJavadocs, getRuneliteJavadoc } from "../src/runelite-docs.js";
import { searchOsrsWiki, getOsrsWikiPage, getOsrsWikiSections, getOsrsWikiMonsterStats } from "../src/osrs-wiki.js";
import { getRuneliteConstants, searchRuneliteSource } from "../src/runelite-source.js";

async function runTests() {
  console.log("=== Testing RuneLite Javadoc Search ===");
  const searchRL = await searchRuneliteJavadocs("NPC", { limit: 5 });
  console.log(`Found ${searchRL.count} results for 'NPC'. Top matches:`);
  for (const r of searchRL.results) {
    console.log(`  - [${r.category}] ${r.fullName} (${r.url})`);
  }

  console.log("\n=== Testing RuneLite Javadoc Fetch ===");
  const javadoc = await getRuneliteJavadoc("net.runelite.api.NPC");
  console.log(`Title: ${javadoc.title}`);
  console.log(`Package: ${javadoc.package}`);
  console.log(`Methods parsed: ${javadoc.methodCount}`);

  console.log("\n=== Testing RuneLite Constants Fetch (Filtered) ===");
  const constants = await getRuneliteConstants("AnimationID", { filter: "jad" });
  console.log(`File: ${constants.file}, Matches: ${constants.matchCount}`);
  console.log("Sample constants content:\n" + constants.content);

  console.log("\n=== Testing OSRS Wiki Search ===");
  const searchWiki = await searchOsrsWiki("Abyssal demon", { limit: 3 });
  console.log(`Found ${searchWiki.count} results for 'Abyssal demon':`);
  for (const r of searchWiki.results) {
    console.log(`  - ${r.title} -> ${r.url}`);
  }

  console.log("\n=== Testing OSRS Wiki Monster Stats ===");
  const monsterStats = await getOsrsWikiMonsterStats("Phantom Muspah");
  console.log(`Title: ${monsterStats.title}`);
  console.log(`Combat level: ${monsterStats.stats?.combat || 'N/A'}`);
  console.log(`Hitpoints: ${monsterStats.stats?.hitpoints || 'N/A'}`);
  console.log(`Attack style: ${monsterStats.stats?.attack_style || 'N/A'}`);
  console.log(`Attack speed: ${monsterStats.stats?.attack_speed || monsterStats.stats?.attspeed || 'N/A'}`);

  console.log("\n=== Testing OSRS Wiki Page Fetch ===");
  const page = await getOsrsWikiPage("Abyssal demon");
  console.log(`Title: ${page.title}`);
  console.log("Extract snippet:\n" + page.extract.substring(0, 200) + "...\n");

  console.log("ALL TESTS PASSED SUCCESSFULLY! ✅");
}

runTests().catch(err => {
  console.error("Test failed:", err);
  process.exit(1);
});
