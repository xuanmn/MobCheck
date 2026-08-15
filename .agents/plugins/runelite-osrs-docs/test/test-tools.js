import { searchRuneliteJavadocs, getRuneliteJavadoc } from "../src/runelite-docs.js";
import { searchOsrsWiki, getOsrsWikiPage, getOsrsWikiSections } from "../src/osrs-wiki.js";

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
  console.log("Sample markdown snippet:\n" + javadoc.markdown.substring(0, 400) + "...\n");

  console.log("=== Testing OSRS Wiki Search ===");
  const searchWiki = await searchOsrsWiki("Abyssal demon", { limit: 3 });
  console.log(`Found ${searchWiki.count} results for 'Abyssal demon':`);
  for (const r of searchWiki.results) {
    console.log(`  - ${r.title} -> ${r.url}`);
  }

  console.log("\n=== Testing OSRS Wiki Sections ===");
  const sections = await getOsrsWikiSections("Abyssal demon");
  console.log(`Page: ${sections.title}, Sections count: ${sections.sections.length}`);
  console.log(`Sample sections:`, sections.sections.slice(0, 5).map(s => s.name));

  console.log("\n=== Testing OSRS Wiki Page Fetch ===");
  const page = await getOsrsWikiPage("Abyssal demon");
  console.log(`Title: ${page.title}`);
  console.log("Extract snippet:\n" + page.extract.substring(0, 300) + "...\n");

  console.log("ALL TESTS PASSED SUCCESSFULLY! ✅");
}

runTests().catch(err => {
  console.error("Test failed:", err);
  process.exit(1);
});
