---
name: runelite-plugin-developer
description: Build, modify, review, test, package, research, debug, troubleshoot, or prepare CI/preflight checks for Java 11 RuneLite external plugins for Old School RuneScape. Use for tasks involving OSRS/RuneLite mental models, OSRS Wiki mechanics research, RuneLite core and Plugin Hub prior-art search, finding examples in existing plugins, RuneLite API/Javadocs, net.runelite.api game state, gameval IDs/constants, Client APIs, events, widgets, item containers, menus, overlays, Swing plugin panels, @PluginDescriptor, config, Java 11 code quality, RuneLite performance/concurrency/logging/test review, passive event capture/replay fixture testing, manual live-client debugging, Plugin Hub compliance, Plugin Hub packager/preflight CI, Jagex/RuneLite feature restrictions, Gradle test/run workflows, resource loading, HTTP/JSON/file I/O, or manual in-game validation.
---

# RuneLite Plugin Developer

Use this skill to make RuneLite external plugin changes that are maintainable, Java 11 compatible, and acceptable for Plugin Hub review. Treat target-repository instructions such as `AGENTS.md` as authoritative when they are stricter or more specific.

## Workflow

1. Inspect the repository before editing:
   - Run `git status --short` and preserve unrelated local changes.
   - Read Gradle files, `runelite-plugin.properties`, the plugin entrypoint, config interfaces, overlays, panels, and nearby tests.
   - Identify Java version, dependency versions, test task, and run task from the target repo instead of assuming defaults.
2. Search RuneLite core plugins and Plugin Hub for prior art when starting a new plugin, adding a sizable feature, touching an unfamiliar RuneLite API, or looking for examples. Read [references/plugin-hub-research.md](references/plugin-hub-research.md) to check for duplicate/overlapping plugins and find comparable source code.
3. Check compliance before implementing features that touch combat, PvP, menu entries, interfaces, input, player data, HTTP, persistence, external processes, or runtime loading. Read [references/runelite-rules.md](references/runelite-rules.md) first for these areas.
4. Read [references/runelite-concepts.md](references/runelite-concepts.md) when a feature depends on how OSRS, Jagex servers, the RuneLite client, plugins, and users interact, or when the request is ambiguous about what a plugin can observe, draw, change, or automate. Use the OSRS Wiki for game-mechanics context, terminology, content pages, and player-facing behavior before designing mechanics-heavy features.
   - When a user uses ambiguous player slang, search the OSRS Wiki and inspect redirects/aliases, not just article text. Wiki redirects can map community terms to canonical mechanics; for example, `Cum` redirects to `Unstable Orb`, the Akkha final-phase orb in Tombs of Amascut.
5. Before implementing game-state, UI, menu, item, scene, var, coordinate, or cache behavior, choose the narrowest RuneLite API surface that models it. Read [references/runelite-api.md](references/runelite-api.md) to map the task to `net.runelite.api`, events, widgets, coordinates, `gameval` constants, and common `net.runelite.client` services.
6. Follow existing architecture. Keep RuneLite boundary code in plugin/event/UI classes and move business logic into small testable services or models when practical. Read [references/plugin-patterns.md](references/plugin-patterns.md) when changing lifecycle, config, overlays, panels, event handling, HTTP/JSON/file I/O, or resources.
7. Read [references/java-quality.md](references/java-quality.md) when changing or reviewing core Java logic, concurrency/threading, render or tick hot paths, logging, resource handling, nullability, collections, or test quality.
8. Keep Java 11 compatibility. Do not introduce records, text blocks, switch expressions, pattern matching, sealed classes, or APIs added after Java 11.
9. Add or update focused tests where automated verification is possible. Read [references/testing.md](references/testing.md) before changing behavior or test infrastructure; for behavior that depends on live in-game event sequences, prefer passive capture/replay fixtures combined with unit tests over live-client automation.
10. For live-client bugs or ambiguous game interactions, use the manual live debugging loop in [references/testing.md](references/testing.md). Before asking the user to reproduce the issue, write an ASCII or Mermaid interaction diagram that includes OSRS/Jagex, RuneLite, the plugin, user actions, observed events, plugin state, and UI/output.
11. Run the repo's automated check, usually `./gradlew test`; if local instructions require a specific JDK or command, use that.
12. When preparing a GitHub or Plugin Hub PR, estimate changed-line count with generated files excluded where practical and encourage a reviewable scope. Use `<=1000` changed lines as small, `1001-3000` as medium, and `>3000` as large. For medium PRs, suggest whether a clean split is available; for large PRs, actively recommend splitting into smaller preparatory, mechanical, behavior, and test PRs unless the change is inherently atomic.
13. When preparing a Plugin Hub submission or release, offer an optional Plugin Hub preflight pipeline based on the upstream packager workflow. Read [references/testing.md](references/testing.md) for the limits of packager CI and RuneLite GitHub App checks.
14. Never automate RuneScape gameplay or interact with RuneScape through browser/computer-use tools. A clean JVM start is not a passing in-game test.

## Implementation Guardrails

- Prefer event-driven tracking via RuneLite events over scanning the whole scene every tick or frame.
- Prefer official RuneLite API models and events over parsing UI text, hard-coded widget trees, reflection, or raw IDs.
- Prefer existing widgets before overlays. If the game interface already has a text widget, changing `Widget#setText` is cleaner than drawing on top. For the Wilderness loot chest, `WildyLootChest.TABS` direct children mapped as `0..4` tab backgrounds, `5..9` key images, `10..14` tab text, so only `10..14` should be touched for tab values/text hiding.
- Do not recurse widget trees blindly. Some RuneLite widget groups have nested/dynamic/static children that look tempting but behave badly when modified. For known interfaces, inspect and use the direct child layer that actually owns the target components.
- Widget sprite overrides are not the same as interface editing. `client.getWidgetSpriteOverrides()` and `client.getSpriteOverrides()` can cooperate with Resource Packs, but broad sprite overrides are risky because sprites may be shared globally. Prefer packed widget-id overrides only when a live component explicitly needs sprite replacement.
- Apply [references/java-quality.md](references/java-quality.md) for Java review decisions, especially client-thread/EDT boundaries, hot render paths, logging, mutable state, and test quality.
- Keep overlay `render()` work minimal; precompute state outside per-frame drawing when possible.
- Use RuneLite APIs and constants instead of magic numbers where available, especially `net.runelite.api.gameval` IDs, component IDs, var IDs, DB table IDs, sprite IDs, item IDs, object IDs, and NPC IDs.
- Use injected services: `@Inject OkHttpClient`, `@Inject Gson`, `@Inject ConfigManager`, `ClientThread`, toolbar/overlay managers, and domain controllers already present in the project.
- Use `LinkBrowser` for URLs, `RuneLite.RUNELITE_DIR` for plugin-owned files under `.runelite`, and `getResourceAsStream` for packaged resources.
- Do not use reflection, JNI/JNA, `Unsafe`, external processes, dynamic code loading, runtime code generation, or Java serialization in hub-bound plugin code.
- Keep config group/key names stable. If renaming is unavoidable, implement migration so users do not silently lose settings.
- Avoid noisy production logging: use `log.debug()` for diagnostics and reserve `log.info()` for startup/shutdown or infrequent events.
- Before copying substantial code or structure from another plugin, confirm the source is under a permissive license compatible with RuneLite Plugin Hub expectations, such as BSD-2, and ask whether the user should make a PR to the original repository instead. If copying a whole class, preserve existing copyright/license notices and attribute the original developer in that class. If copying multiple whole classes, also document the copied code and attribution in a third-party notice, copyright, or license file.
- Before adding or touching many Java classes in a project without a recorded preference, ask once whether the user wants copyright/license notice blocks in each source class. Persist the answer in project instructions, preferably `AGENTS.md` when the project uses it, or otherwise in a small repo-local note such as `.agents/copyright-notices.md`; apply the recorded preference consistently and do not re-ask for that project.

## Versioning Note

- For Plugin Hub projects using `build=standard`, the Plugin Hub packager replaces the plugin's local `build.gradle` and `settings.gradle` during packaging. Do not assume `project.version` in `build.gradle` is what the Hub displays.
- Prefer an explicit `version=` field in `runelite-plugin.properties` for the Plugin Hub display version when the project uses `build=standard`.
- If the repository also declares `version = '...'` in `build.gradle`, keep it in sync with `runelite-plugin.properties` for local builds and artifacts, but treat `runelite-plugin.properties` as the Hub-facing source.
- When preparing a release, inspect the target repository's existing versioning convention before changing numbers. If the release type is ambiguous, ask whether the change should be patch, minor, or major instead of inventing project policy.

## Common Mistakes

- Assuming a pull request, tag, or GitHub release in the plugin's own repository updates the RuneLite Plugin Hub. Existing Hub plugins are updated by changing the pinned `commit=` in `runelite/plugin-hub`.
- Pinning a short SHA in a Plugin Hub manifest. Use the full 40-character commit hash.
- Treating the RuneLite Plugin Hub human-review gate as a build failure. Distinguish the actual build check from maintainer-review routing.
- Creating fresh `Gson` instances in production plugin code. Use injected `Gson`, or pass the injected instance into helpers; fresh `new Gson()` or `new GsonBuilder().create()` can be rejected by Plugin Hub tooling.
- Relying only on `build.gradle` for the displayed Plugin Hub version when `runelite-plugin.properties` uses `build=standard`.
- Searching only OSRS Wiki article bodies for player slang. Check search results and redirects so slang aliases resolve to the canonical page before choosing IDs or behavior.
- Forgetting Java 11 compatibility by adding records, text blocks, switch expressions, pattern matching, sealed classes, or post-Java-11 APIs.
- Broadly editing widget trees, sprite overrides, menu entries, or hot render paths without first narrowing the RuneLite API surface and checking Plugin Hub/Jagex rules.

## Completion Checklist

When finishing a RuneLite plugin task:

- Summarize changed files and behavior.
- Report the approximate PR size bucket when the work is headed for RuneLite or Plugin Hub review, and recommend a smaller split for large diffs or obvious mixed-scope medium diffs.
- Report automated checks run and any failures or skipped checks.
- Offer to launch the development client by running `./gradlew run` from the plugin root.
- Tell the user to follow RuneLite's "Using Jagex Accounts" instructions when logging into the development client: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
- Tell the user exactly what to test in game: the golden path, the behavior changed, and edge cases worth exercising.
- Wait for the user to confirm in-game behavior before treating gameplay-facing work as fully validated.

## Primary Sources

- RuneLite Developer Guide: https://github.com/runelite/runelite/wiki/Developer-Guide
- RuneLite API Javadocs: https://static.runelite.net/runelite-api/apidocs/index.html
- OSRS Wiki: https://oldschool.runescape.wiki/
- RuneLite Plugin Hub README: https://github.com/runelite/plugin-hub
- RuneLite example-plugin AGENTS.md: https://github.com/runelite/example-plugin/blob/master/AGENTS.md
- RuneLite Rejected or Rolled Back Features: https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features
- Jagex Third-Party Client Guidelines: https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1
