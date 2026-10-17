# Upgrading Guidebook from Paper 1.19.2 to Paper 26.2

Researched 2026-09-27. Scope: (1) whether ProtocolLib / WorldEdit / FAWE support Paper 26.2 on Java 25, and (2) which Paper API changes between 1.19.2 and 26.2 affect **this** repo.

Method: I read the repo (`pom.xml`, `plugin.yml`, all imports and call sites), then checked each item against first-party sources: the PaperMC downloads API, the Maven repo, docs and news; Paper's GitHub; EngineHub, IntellectualSites and dmulloy2 on GitHub; Mojang's changelog. I also downloaded `paper-api-26.2.build.129-stable.jar` (plus its sources jar) and compiled the unmodified `src/main/java` against it with JDK 25 (`javac --release 25 -Xlint:deprecation,removal`), using WorldEdit 7.4.5 and the local PluginUtils 1.9.1 jar on the classpath. That compile happened in a scratch directory. No repo files other than this note were changed.

---

## TL;DR

- **Paper 26.2 is stable and needs Java 25.** Latest build is 129 (2026-09-23). API coordinate: `io.papermc.paper:paper-api:26.2.build.129-stable`, from `https://repo.papermc.io/repository/maven-public/`. The `-R0.1-SNAPSHOT` version style ended with 26.1.
- **Guidebook's own code compiles against Paper API 26.2 with zero errors.** There are 98 warnings. 78 of them are the Conversation API, which is now `@Deprecated(forRemoval = true)`. The other 20 cover `ChatColor`, `BookMeta#addPage/getPage(String)`, `JavaPlugin#getDescription()` and `Server#broadcastMessage`. None of these break today.
- **Build-level changes are required:**
  - Run the build on a JDK 25. The 26.2 API and WorldEdit 7.4.x jars are class-file version 69, and javac 21 refuses them (I tested this).
  - Bump `paper-api` to the new coordinate and add the PaperMC repo. Today `paper-api` only resolves because the EngineHub repo happens to proxy it.
  - Bump WorldEdit 7.3.0 to 7.4.4 or later (7.4.5 is current).
  - Drop or replace the ProtocolLib dependency. Its old repo URL now returns 404, and **Guidebook never imports ProtocolLib** anyway.
- **WorldEdit supports 26.2 in 7.4.4 and later** (7.4.4 changelog: "Updated to 26.2"). 7.4.3 added 26.1 and Java 25. WorldEdit is still a normal `plugin.yml` plugin named `WorldEdit`.
- **FAWE supports 26.2 in 2.15.3 and later** (2.15.4 is current). It declares `provides: [WorldEdit]`.
- **ProtocolLib supports 26.2 only in dev builds.** The latest release, 5.4.0 (2025-08-07), covers 1.21.4 to 1.21.8. 26.2 support landed on master on 2026-06-20 (commit `2d3b3e3`) and ships in the rolling `dev-build` GitHub release. The dev build's main `ProtocolLib.jar` is now a Paper plugin (`paper-plugin.yml`, `api-version: "26.2"`), and Spigot gets `ProtocolLib-Spigot.jar`. Maven coordinates moved to `net.dmulloy2:ProtocolLib` on Maven Central, where only up to 5.4.0 is published. This is irrelevant to Guidebook unless something else on the server needs ProtocolLib.
- **The biggest real risk is transitive: PluginUtils 1.9.1.** It sends `tellraw` with JSON keys from before 1.21.5 (`clickEvent`, `hoverEvent`, `contents`, `value`). Mojang renamed those in 1.21.5, so Guidebook's clickable and hover chat lines (`/guidebook list`, `details`, and the description editor) will probably lose their click and hover behaviour. The PluginUtils jar also contains NMS code built against 1.21.4. A 26.2-compatible PluginUtils is needed. That is outside this repo.

---

## 1. Repo inventory (what Guidebook actually uses)

### Build (`pom.xml`)

| Item | Current value | Where |
|---|---|---|
| Compiler | `maven-compiler-plugin` 3.10.1, `<source>17</source><target>17</target>`. The stale `maven.compiler.source/target` properties are `1.7` but are overridden. | `pom.xml:11-12`, `pom.xml:18-23` |
| Repositories | `dmulloy2-repo` `https://repo.dmulloy2.net/content/groups/public/`, `sk89q-repo` `https://maven.enginehub.org/repo/`. **No PaperMC repo.** | `pom.xml:37-46` |
| Paper API | `io.papermc.paper:paper-api:1.19.2-R0.1-SNAPSHOT` (provided). The local `~/.m2/.../_remote.repositories` shows it was resolved from `sk89q-repo`. | `pom.xml:48-53` |
| Guava | `com.google.guava:guava:31.1-jre` (provided). No `com.google` imports in `src`. | `pom.xml:54-59` |
| ProtocolLib | `com.comphenix.protocol:ProtocolLib:4.8.0` (provided). **No `com.comphenix` imports anywhere in `src`.** | `pom.xml:60-65` |
| PluginUtils | `com.mcmiddleearth:PluginUtils:1.9.1` (provided) | `pom.xml:66-71` |
| WorldEdit | `com.sk89q.worldedit:worldedit-core` and `worldedit-bukkit` 7.3.0 (provided), excluding `org.bukkit:bukkit`, `org.spigot:spigot`, `org.bstats:bstats-bukkit` | `pom.xml:72-97` |
| paperweight / NMS | None. No `net.minecraft` or `org.bukkit.craftbukkit` usage in `src`. | n/a |

### `plugin.yml` (`src/main/resources/plugin.yml`)

- `api-version: 1.19` (line 7), `load: POSTWORLD` (line 5).
- `softdepend: [Multiverse-Core, worldedit-core, worldedit-bukkit, PluginUtils, bukkit, ProtocolLib, guava]` (line 6). `worldedit-core`, `worldedit-bukkit`, `bukkit` and `guava` are not plugin names. WorldEdit's plugin name is `WorldEdit` (see §2.1), so this list does not order Guidebook after WorldEdit.

### API surface used (from grepping imports and call sites)

- **Bukkit core:** `Bukkit`, `Location`, `World`, `Material.WRITABLE_BOOK/WRITTEN_BOOK`, `OfflinePlayer`, `Player`, `util.Vector`, `ItemStack`, `inventory.meta.BookMeta`, `configuration.*` (YAML), `plugin.java.JavaPlugin`, `PluginDescriptionFile`, `scheduler.BukkitRunnable`, `scheduleSyncRepeatingTask`, `command.*` (`TabExecutor`), `event.*` (custom `GuidebookSendEvent` with `Cancellable`, `HandlerList`, and Paper's `Event#callEvent()` at `data/InfoArea.java:162`), `PlayerQuitEvent`.
- **Boss bars:** `Server#createBossBar(String, BarColor, BarStyle)` at `data/InfoArea.java:83`, `BossBar#setTitle(String)` at `data/InfoArea.java:274`.
- **Legacy chat:** `org.bukkit.ChatColor` (16 sites), `CommandSender#sendMessage(String)`, `Server#broadcastMessage` (`util/MessageUtil_invalid.java:60`, a class that nothing references).
- **Conversation API:** `org.bukkit.conversations.*` across 15 files in `conversation/`.
- **WorldEdit** (only `command/GuidebookDetails.java`): `WorldEdit.getInstance().getSessionManager()`, `BukkitAdapter.adapt(Player)`, `LocalSession#setRegionSelector`, `CuboidRegionSelector`, `SphereRegionSelector`, `Polygonal2DRegionSelector`, `BlockVector2/3`.
- **PluginUtils** (MCME library): `FancyMessage`, `MessageUtil`, `MessageType`, `FancyMessageConfigUtil`, `TitleUtil`, `FileUtil`, `NumericUtil`, `region.*`.
- **Not used:** `io.papermc.*`, `com.destroystokyo.*`, `net.kyori.*` (Adventure), `com.comphenix.*`, NMS/CraftBukkit.

---

## 2. Question 1: dependency support for Paper 26.2 on Java 25

### 2.0 Paper 26.2 baseline

- The PaperMC downloads API reports version `26.2` with `support.status = SUPPORTED` and `java.version.minimum = 25`. The latest build is **129**, channel `STABLE`, built 2026-09-23. Source: https://fill.papermc.io/v3/projects/paper/versions/26.2 and https://fill.papermc.io/v3/projects/paper/versions/26.2/builds (queried 2026-09-27).
- Maven: `paper-api` versions `26.2.build.125-stable` through `26.2.build.129-stable` exist. Source: https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml (lastUpdated 2026-09-26).
- The project-setup docs give the repo `https://repo.papermc.io/repository/maven-public/`, the dependency `io.papermc.paper:paper-api:26.2.build.+` (Gradle), and a Java 25 toolchain. For Maven they say to use a range `[26.2.build,)` or a pinned build such as `26.2.build.129-stable`, and that before 26.1 the format was `{VERSION}-R0.1-SNAPSHOT`. Source: https://docs.papermc.io/paper/dev/project-setup/
- The 26.1 announcement (2026-04-26) covers the new versioning format `<mcversion>.build.<build>-<status>`, the fact that Mojang no longer ships obfuscated server jars, and that Paper removed its internal remapper. It also marks `WorldInfo#getName` obsolete and describes the world storage move to `world/dimensions/...`. Source: https://papermc.io/news/26-1/
- The 26.2 announcement (2026-06-12) says Adventure 5 shipped with previously deprecated APIs removed, and `BookMeta` no longer extends Adventure's `Book`. It also covers the removal of deprecated ClickEvent and HoverEvent code, beds losing their PDC, and that worlds cannot be downgraded. Source: https://papermc.io/news/26-2/
- Paper hard-forked from Spigot on 2024-12-13. Source: https://papermc.io/news/the-future-of-paper-hard-fork

### 2.1 WorldEdit (EngineHub)

| Version | Tag date (commit) | Relevant changelog line |
|---|---|---|
| 7.3.0 (current in pom) | n/a | Predates 1.21.x. Does not support 26.x (the 26.x adapters were added in 7.4.3 and 7.4.4, below). |
| 7.3.19 | n/a | "Final release of the 7.3.x series in preparation for 7.4 release" |
| 7.4.3 | 2026-05-04 (`78babeb2c3`) | "Updated to 26.1(.1/.2) and Java 25" |
| **7.4.4** | 2026-07-04 (`f9e033fa48`) | **"Updated to 26.2"** |
| 7.4.5 (latest) | 2026-08-09 (`b8dc4c1d91`) | "[Bukkit] Replace PaperLib to improve compatibility with newer Minecraft versions" |

Source for all of the above: https://github.com/EngineHub/WorldEdit/blob/7.4.5/CHANGELOG.txt. Tag dates come from `gh api repos/EngineHub/WorldEdit/commits/<tag>`.

- The 7.4.5 Bukkit adapters are `1.21.4`, `1.21.5`, `1.21.6`, `1.21.9`, `1.21.11`, `26.1` and `26.2`. Source: https://github.com/EngineHub/WorldEdit/tree/7.4.5/worldedit-bukkit/adapters
- The 7.4.5 descriptor is still a classic `plugin.yml`: `name: WorldEdit`, `load: STARTUP`, `api-version: 1.21.4`, `folia-supported: true`. Source: https://github.com/EngineHub/WorldEdit/blob/7.4.5/worldedit-bukkit/src/main/resources/plugin.yml
- Maven coordinates are unchanged (`com.sk89q.worldedit:worldedit-core` and `worldedit-bukkit` `7.4.5` on `https://maven.enginehub.org/repo/`). Source: https://maven.enginehub.org/repo/com/sk89q/worldedit/worldedit-bukkit/maven-metadata.xml (lastUpdated 2026-09-27). The 7.4.5 `worldedit-bukkit` POM no longer pulls in `org.bukkit:bukkit` or `org.spigot:spigot` transitively, so the exclusions in `pom.xml:83-96` are now no-ops. Source: https://maven.enginehub.org/repo/com/sk89q/worldedit/worldedit-bukkit/7.4.5/worldedit-bukkit-7.4.5.pom
- **Caveat (verified locally):** the 7.4.5 `worldedit-core` and `worldedit-bukkit` jars are Java 25 bytecode (class major version 69), so Maven must run on a JDK 25 or newer.
- API compatibility (verified by compiling): every WorldEdit call in `command/GuidebookDetails.java:84-141` compiles unchanged against 7.4.5, with no deprecation warnings. The 7.4.0 notes deprecate some things "for removal in WE 8", but none of them are APIs this repo uses (per the compile).
- Paperweight/Mojang mappings: this concerns WorldEdit internally (7.3.19 added "a separate Gradle configuration for reobfuscation to better prepare for MC 26.1"). It has no effect on API consumers like Guidebook.

### 2.2 FastAsyncWorldEdit (only relevant if the server runs FAWE instead of WorldEdit)

- **2.15.3 (2026-07-14): "Introduce 26.2 support".** Source: https://github.com/IntellectualSites/FastAsyncWorldEdit/releases/tag/2.15.3
- 2.15.1 (2026-05-24): "Support Minecraft 26.1.2", "Build with Java 25", "Switch to Paperweight Userdev beta". Source: https://github.com/IntellectualSites/FastAsyncWorldEdit/releases/tag/2.15.1
- 2.15.2 (2026-06-04), under "Breaking changes": "Remove 1.20.x support". Source: https://github.com/IntellectualSites/FastAsyncWorldEdit/releases/tag/2.15.2
- 2.15.4 (2026-08-16, latest): "Use older Piston version for Java 21 compatibility". Source: https://github.com/IntellectualSites/FastAsyncWorldEdit/releases/tag/2.15.4
- The 2.15.4 adapters include `adapter-26.1` and `adapter-26.2`. The `plugin.yml` has `name: FastAsyncWorldEdit`, `provides: [ WorldEdit ]` and `api-version: 1.21`, and there is no `paper-plugin.yml`. Source: https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/2.15.4/worldedit-bukkit/src/main/resources/plugin.yml
- Guidebook compiles against the WorldEdit API (`com.sk89q.worldedit.*`), which FAWE implements. I have not checked binary compatibility of FAWE 2.15.x with the specific selector constructors used in `GuidebookDetails` (see Open questions).

### 2.3 ProtocolLib (dmulloy2)

- **Latest release, 5.4.0 (2025-08-07):** "Support added for Minecraft 1.21.4-1.21.8"; minimum Java 17; the project "will be published to Maven Central instead of repo.dmulloy2.net. The groupId has been changed to `net.dmulloy2`"; "ci.dmulloy2.net is also officially deprecated. Future dev builds will be hosted on GitHub." **This release does not claim 26.x support.** Source: https://github.com/dmulloy2/ProtocolLib/releases/tag/5.4.0
- **26.x support exists only in dev builds.** The rolling `dev-build` GitHub release has assets `ProtocolLib.jar` and `ProtocolLib-Spigot.jar`, last updated 2026-09-25. Source: https://github.com/dmulloy2/ProtocolLib/releases/tag/dev-build. Relevant master commits:
  - `22012d3` 2026-03-27 "Mark 26.1 as supported"; `f911ee9` "Bump version to 5.5.0-SNAPSHOT"
  - `2d3b3e3` 2026-06-20 "Update to Minecraft 26.2 (#3642)". Source: https://github.com/dmulloy2/ProtocolLib/commit/2d3b3e3363
  - `67ce937` 2026-08-03 "Create modern Paper plugin artifact (#3656)". Source: https://github.com/dmulloy2/ProtocolLib/commit/67ce937109
  - `98856a8` / `78b1016` / `fec45cf` (August 2026): restored the Java 17 bytecode target. Issue #3643 had reported the dev build requiring Java 25. Source: https://github.com/dmulloy2/ProtocolLib/issues/3643
  - `583353e` 2026-09-25 "Fix Minecraft 26.3 play packet mappings"
- **Caveat: `plugin.yml` vs `paper-plugin.yml`.** The README now says "`ProtocolLib.jar` is the primary Paper plugin. Spigot servers must use the compatibility artifact, `ProtocolLib-Spigot.jar`." The Paper artifact ships `paper-plugin.yml` with `api-version: "26.2"` and `load: STARTUP`. Sources: https://github.com/dmulloy2/ProtocolLib/blob/master/README.md and https://github.com/dmulloy2/ProtocolLib/blob/master/paper/src/main/resources/paper-plugin.yml. Paper's docs call Paper plugins experimental and say they use isolated classloaders. Source: https://docs.papermc.io/paper/dev/getting-started/paper-plugins/
- **Open 26.2 issues:** #3660 "ProtocolLib error on Player join Paper 26.2" (open since 2026-08-17): https://github.com/dmulloy2/ProtocolLib/issues/3660. #3669 (Leaf 26.2 with io_uring): https://github.com/dmulloy2/ProtocolLib/issues/3669
- **Maven:**
  - Maven Central `net.dmulloy2:ProtocolLib` lists only `5.1.0`, `5.3.0` and `5.4.0`. Source: https://repo1.maven.org/maven2/net/dmulloy2/ProtocolLib/maven-metadata.xml
  - The URL in `pom.xml:40`, `https://repo.dmulloy2.net/content/groups/public/`, **returns HTTP 404** (checked 2026-09-27). The build only works because 4.8.0 is cached in `~/.m2`.
  - `https://repo.dmulloy2.net/repository/public/` still answers, but it lists only `com.comphenix.protocol:ProtocolLib` `5.3.0` and `5.4.0-SNAPSHOT`.
  - I could not confirm any published Maven artifact carrying 26.2 support.
- **Relevance to Guidebook: none at code level.** `grep` finds no `com.comphenix` import or reference in `src/`. ProtocolLib appears only in `pom.xml:60-65` and the `softdepend` at `plugin.yml:6`. The PluginUtils 1.9.1 classes Guidebook uses have no `com/comphenix` references (checked with `javap`). PluginUtils itself soft-depends on ProtocolLib.

---

## 3. Question 2: Paper API changes between 1.19.2 and 26.2 that affect this repo

Rating legend: **breaking** (won't build or run without a change) / **deprecated-but-works** / **no impact**.

"Compile-verified" means the item was checked by compiling unmodified `src/main/java` against `paper-api-26.2.build.129-stable.jar` with JDK 25. Result: **0 errors, 98 warnings.**

### 3.1 Build-level

| # | Change | Repo location | Rating | Evidence |
|---|---|---|---|---|
| B1 | Java 25 minimum to **run** Paper 26.2 | n/a (server) | **breaking** (server JVM) | https://fill.papermc.io/v3/projects/paper/versions/26.2 (`java.version.minimum: 25`) |
| B2 | Paper API jar (and WorldEdit 7.4.x) is class-file version 69 (Java 25), so the **build JDK must be 25 or newer**. Guidebook's own bytecode could still target 17; javac 25 with `--release 17` compiled fine. The docs recommend a Java 25 toolchain. | `pom.xml:18-23` | **breaking** if Maven runs on JDK 17 or 21. Verified: JDK 21 javac fails with "class file has wrong version 69.0, should be 65.0". | Local test; https://docs.papermc.io/paper/dev/project-setup/ |
| B3 | Version string format changed from `1.19.2-R0.1-SNAPSHOT` to `26.2.build.<n>-stable` (a Maven range such as `[26.2.build,)` also works) | `pom.xml:51` | **breaking** (edit required) | https://docs.papermc.io/paper/dev/project-setup/ ; https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml |
| B4 | PaperMC repository `https://repo.papermc.io/repository/maven-public/` is not declared. `paper-api` currently resolves via EngineHub's repo, which also serves `26.2.build.129-stable` (HTTP 200 on 2026-09-27), but that is incidental. | `pom.xml:37-46` | deprecated-but-works (fragile) | https://docs.papermc.io/paper/dev/project-setup/ ; `~/.m2/.../paper-api/1.19.2-R0.1-SNAPSHOT/_remote.repositories` |
| B5 | `repo.dmulloy2.net/content/groups/public/` returns 404. ProtocolLib moved to `net.dmulloy2` on Maven Central. The dependency is unused by code. | `pom.xml:38-41`, `pom.xml:60-65` | **breaking** on a clean `~/.m2`. Fix: remove it. | §2.3 |
| B6 | WorldEdit 7.3.0 has no 26.x adapter. Use 7.4.4 or later (7.4.5 current). This is a runtime server plugin matter; the compile API is unchanged. | `pom.xml:72-97` | **breaking** at runtime (old WE won't load on 26.2). Compile-verified that 7.4.5 is source compatible. | §2.1 |
| B7 | No NMS use, so **no paperweight-userdev is needed**. Since 26.1, Paper no longer supports obfuscated plugins or remapping to Spigot mappings. | n/a | no impact | https://docs.papermc.io/paper/dev/userdev/ ; https://papermc.io/news/26-1/ |
| B8 | `plugin.yml` remains the standard format and `paper-plugin.yml` is optional and experimental. The docs list valid `api-version` values from `1.13` to `26.2`. `api-version: 1.19` remains valid; bumping to `'26.2'` is optional. | `plugin.yml:7` | no impact (optional bump) | https://docs.papermc.io/paper/dev/plugin-yml/ ; https://docs.papermc.io/paper/dev/getting-started/paper-plugins/ |
| B9 | Guava 31.1-jre is declared, but Paper 26.2 provides Guava 33.6.0-jre and the repo imports no Guava. | `pom.xml:54-59` | no impact (can be removed) | `paper-api-26.2.build.129-stable.pom` (https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/26.2.build.129-stable/paper-api-26.2.build.129-stable.pom) |
| B10 | `softdepend` lists non-plugin names (`worldedit-core`, `worldedit-bukkit`, `bukkit`, `guava`) and omits `WorldEdit`. Harmless at runtime because WE classes are only touched in a command, but the list is misleading. | `plugin.yml:6` | no impact (cleanup) | WE plugin name: https://github.com/EngineHub/WorldEdit/blob/7.4.5/worldedit-bukkit/src/main/resources/plugin.yml |

### 3.2 Source/API-level (all compile-verified)

| # | API | Repo location | 26.2 status | Rating | Evidence |
|---|---|---|---|---|---|
| A1 | `org.bukkit.conversations.*`: `ConversationFactory`, `Conversation`, `ConversationContext`, `ConversationAbandonedListener/Event`, `ConversationPrefix`, `Prompt`, `StringPrompt`, `BooleanPrompt`, `NumericPrompt`, `FixedSetPrompt`, `ValidatingPrompt`, `MessagePrompt` | All of `conversation/*.java` (78 warnings), e.g. `conversation/ConfirmationFactory.java:30-59`, `DescriptionEditFactory.java:43-71`, `TitleEditFactory.java:43-70`. Used from `command/GuidebookDescription.java` and `GuidebookTitle.java`. | `@Deprecated(forRemoval = true)`: "The conversation API has been deprecated for removal. This system does not support component based messages". Deprecated in Paper commit `810b3c6cd6` (2025-09-17). **Still present in 26.2.** | **deprecated-but-works** (highest future risk: it will be removed) | Paper sources jar `org/bukkit/conversations/ConversationFactory.java`; https://github.com/PaperMC/Paper/commit/810b3c6cd6 |
| A2 | `org.bukkit.ChatColor` | `command/GuidebookDetails.java:75,76,92,104,134`; `command/GuidebookList.java:51-53`; `conversation/ConfirmationPrefix.java:31`; `conversation/DescriptionEditEnterSubcommandPrompt.java:68`; `util/DevUtil.java:36`; `util/MessageUtil_invalid.java:37,45,53,60` | `@Deprecated // Paper` in favour of Adventure `NamedTextColor`. Not for removal. | deprecated-but-works | Paper sources jar `org/bukkit/ChatColor.java`; https://github.com/PaperMC/Paper/blob/26.2/paper-api/src/main/java/org/bukkit/ChatColor.java |
| A3 | `BookMeta#addPage(String...)`, `BookMeta#getPage(int)` | `data/InfoArea.java:236`, `data/InfoArea.java:245` | `@Deprecated` in favour of `addPages(Component...)` and `page(int)` | deprecated-but-works | Paper sources jar `org/bukkit/inventory/meta/BookMeta.java` |
| A4 | Casting a **`WRITABLE_BOOK`** meta to `BookMeta` | `data/InfoArea.java:233-234`; `command/GuidebookDescription.java:63-69` | A new `WritableBookMeta` supertype exists, and its javadoc says to use it for writable books. On Paper 26.2 the server class `CraftMetaBook implements BookMeta, WritableBookMeta`, and `CraftItemMetas` maps `WRITABLE_BOOK` to `ItemMetaData<BookMeta>`, so the cast still succeeds. `WRITTEN_BOOK` maps to `CraftMetaBookSigned implements BookMeta`. | deprecated-but-works (the cast is safe on 26.2; switching to `WritableBookMeta` is the forward-compatible choice) | https://github.com/PaperMC/Paper/blob/26.2/paper-server/src/main/java/org/bukkit/craftbukkit/inventory/CraftMetaBook.java ; https://github.com/PaperMC/Paper/blob/26.2/paper-server/src/main/java/org/bukkit/craftbukkit/inventory/CraftItemMetas.java ; Paper tag `26.2` = `e5fe71723e` |
| A5 | 26.2 change: "`BookMeta` no longer extends Adventure's `Book`" | n/a (repo never uses `net.kyori`) | Changed | no impact | https://papermc.io/news/26-2/ |
| A6 | Adventure 5 (`adventure-bom` 5.2.0) with deprecated APIs removed, and removed ClickEvent/HoverEvent deprecated code | n/a (no `net.kyori` imports in repo or in the PluginUtils classes used) | Changed | no impact on the repo (but see P1 below for chat components) | https://papermc.io/news/26-2/ ; API POM |
| A7 | `JavaPlugin#getDescription()` / `PluginDescriptionFile` | `command/GuidebookCommandExecutor.java:116` | `@Deprecated`: "No longer applicable to all types of plugins" (the replacement is `getPluginMeta()`) | deprecated-but-works | Paper sources jar `org/bukkit/plugin/java/JavaPlugin.java` |
| A8 | `Server#broadcastMessage(String)` | `util/MessageUtil_invalid.java:60` (dead code: nothing references the class) | `@Deprecated // Paper`, use `broadcast(Component)` | deprecated-but-works | Paper sources jar `org/bukkit/Server.java` |
| A9 | `World#getName()` / `Bukkit.getWorld(String)` (legacy world names). World names are persisted: `data/PluginData.java:169` builds a per-world data folder from `getWorld().getName()`, `data/CuboidInfoArea.java:78` calls `Bukkit.getWorld(data.getString("world"))`, and `command/GuidebookDetails.java:77` displays the name. PluginUtils' `Region` also stores and loads worlds by name. | as listed | `@ApiStatus.Obsolete`, "candidate for future deprecation. Prefer using `getKey()`". Not `@Deprecated`, so there is no compiler warning. | deprecated-but-works (but see Open questions on migrated world names) | Paper sources jar `org/bukkit/generator/WorldInfo.java`, `org/bukkit/Bukkit.java`; https://papermc.io/news/26-1/ |
| A10 | `Server#createBossBar(String, BarColor, BarStyle, BarFlag...)`, `BossBar#setTitle(String)`, `setProgress`, `addPlayer/removePlayer` | `data/InfoArea.java:83-84,175,220,274` | Present, not deprecated | no impact | compile |
| A11 | `Event#callEvent()` (a Paper addition) and the custom `Event` + `Cancellable` + static `HandlerList` pattern | `data/InfoArea.java:161-163`; `events/GuidebookSendEvent.java` | Present | no impact | compile |
| A12 | Scheduler (`scheduleSyncRepeatingTask`, `BukkitRunnable#runTaskLater`), commands (`getCommand().setExecutor`, `TabExecutor`, `Bukkit.getPluginCommand`), YAML config, `PlayerQuitEvent`, `Player#teleport`, `getInventory().getItemInMainHand()`, `Bukkit.getOfflinePlayer(UUID)`, `Location`/`Vector` | `GuidebookPlugin.java:49-60`; `data/InfoArea.java:215-228`; `command/*`; `data/PluginData.java`; `listener/PlayerListener.java:30-34`; `util/DevUtil.java:79` | Present, not deprecated | no impact | compile |
| A13 | 26.2 bed PDC removal, `MagmaCube`/`Slime` hierarchy, gamerule registry, `Vex#getSummoner`, and similar | n/a | Not used | no impact | https://papermc.io/news/26-2/ ; https://papermc.io/news/26-1/ |

### 3.3 Transitive: PluginUtils 1.9.1 (not Paper API, but it decides whether Guidebook works on 26.2)

| # | Finding | Repo location affected | Rating | Evidence |
|---|---|---|---|---|
| P1 | `FancyMessage` sends chat by dispatching `tellraw <player> <json>` from console. The payload uses the pre-1.21.5 keys `"clickEvent":{"action":..,"value":..}` and `"hoverEvent":{"action":"show_text","contents":[..]}` (constant pool strings in `FancyMessage.class`, via `MessageUtil.sendRawMessage`). Minecraft 1.21.5 made command text components SNBT, renamed `clickEvent` to `click_event` and `hoverEvent` to `hover_event`, renamed `show_text.contents` to `value`, and renamed `run_command.value` to `command`. | Every `FancyMessage#addFancy/addClickable/addTooltipped` call: `command/GuidebookDetails.java:69,75,104,134`, `command/GuidebookList.java:52`, `conversation/DescriptionEditEnterSubcommandPrompt.java`. Also all `FancyMessage#send` output. | **likely breaking (functional)**: click and hover probably stop working, or the `tellraw` may fail to parse. Not runtime-tested. | https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-5 (sections "Text Components in commands such as /tellraw ... are now specified with SNBT instead of JSON" and "The format of hover and click events has been updated"); `javap -v` on PluginUtils-1.9.1.jar |
| P2 | PluginUtils 1.9.1 contains NMS code (`com.mcmiddleearth.pluginutil.nms.*`, which references `net.minecraft.*` and `org.bukkit.craftbukkit.*`), compiled against a Paper **1.21.4** jar (its POM has a `systemPath` of `paper-1.21.4.jar`). Its `plugin.yml` has `api-version: 1.21` and `mappings-version: mojang`. The classes Guidebook calls do not touch `nms.*`. Whether the PluginUtils plugin itself enables cleanly on 26.2 is unverified. | Indirect (PluginUtils must be installed as a plugin) | unknown / likely needs a rebuild | `~/.m2/repository/com/mcmiddleearth/PluginUtils/1.9.1/PluginUtils-1.9.1.pom`; `javap` scan |
| P3 | `TitleUtil.showTitle` calls `Player#sendTitle(String,String,int,int,int)`. This is `@Deprecated // Paper - Adventure` but present in 26.2. All 47 `org/bukkit` method references in the PluginUtils classes Guidebook uses resolve against the 26.2 API. | `data/InfoArea.java:212` | deprecated-but-works | Paper sources jar `org/bukkit/entity/Player.java`; `javap` cross-check |

---

## 4. Suggested minimal change set (for whoever implements; not applied)

1. `pom.xml`:
   - Add `https://repo.papermc.io/repository/maven-public/`.
   - Set `paper-api` to `26.2.build.129-stable` (or the range `[26.2.build,)`).
   - Set WorldEdit to `7.4.5`, and drop the now-pointless exclusions.
   - Remove ProtocolLib, its repo, and Guava.
   - Set the compiler to `<release>25</release>`. The minimum is building on JDK 25 at any target.
   - Delete the stale `1.7` properties.
2. `plugin.yml`: optionally set `api-version: '26.2'`. Replace the bogus softdepends with `[Multiverse-Core, WorldEdit, PluginUtils]`.
3. Get or build a PluginUtils release with 1.21.5+ component syntax and 26.2 NMS (P1 and P2).
4. Later, not required for 26.2: migrate the Conversation API (A1) and legacy `ChatColor`/`BookMeta` strings (A2 and A3) to Adventure, and switch the writable-book cast to `WritableBookMeta` (A4).

---

## Open questions / unverified

- **Runtime not tested.** Nothing here was run on a 26.2 server. Compile success does not prove behaviour: P1 (tellraw), and the plugins actually enabling, need a smoke test.
- **World names after the 26.1 storage migration.** Paper migrates legacy CraftBukkit world folders (`paper-server/src/main/java/io/papermc/paper/world/migration/LegacyCraftBukkitWorldMigration.java` at tag `26.2`: https://github.com/PaperMC/Paper/blob/26.2/paper-server/src/main/java/io/papermc/paper/world/migration/LegacyCraftBukkitWorldMigration.java). I could not confirm from first-party docs that `World#getName()` returns the same legacy names (e.g. `world_nether`, or Multiverse world names) after migration. Guidebook's per-world data folders (`data/PluginData.java:169`) and saved `world:` keys (`data/CuboidInfoArea.java:78`) depend on that. Verify on a copy of the production world.
- **PluginUtils status.** I could not find the PluginUtils source repo or any release newer than 1.9.1 (the local `~/.m2` only has failed-download markers for 1.9.2). Whether a 26.x-compatible PluginUtils exists is unknown.
- **ProtocolLib Maven artifact with 26.2 support.** No 5.5.0 release exists. I could not confirm a published snapshot artifact on Maven Central or repo.dmulloy2.net; the README hints at JitPack snapshots, which I did not check. This does not matter to Guidebook, which does not use ProtocolLib.
- **Bukkit-plugin access to Paper-plugin ProtocolLib.** Paper's docs describe classloader isolation between Paper plugins but do not say whether a `plugin.yml` plugin can link against a Paper plugin's classes via `depend`. This is irrelevant to Guidebook today, but relevant to other MCME plugins that do use ProtocolLib.
- **FAWE binary compatibility** with the `SphereRegionSelector`, `CuboidRegionSelector` and `Polygonal2DRegionSelector` constructors used in `GuidebookDetails` was not checked. Only EngineHub WorldEdit 7.4.5 was compile-checked.
- **ProtocolLib 5.4.0 on 26.x:** the release notes claim support only up to 1.21.8. I infer, but did not test, that it will not work on 26.2.
- **Deprecation timeline:** I did not establish when `ChatColor`, `BookMeta` string methods or `getDescription()` were first deprecated relative to 1.19.2 (some were already deprecated then). Only the Conversation API deprecation date (2025-09-17) was confirmed.
