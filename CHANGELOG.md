# Changelog

All notable changes to Guidebook are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions before 2.0.1 have no entries;
see the GitHub history.

## [Unreleased]

### Added

- `display` in `config.yml` chooses how Welcomes are shown. `chat` (the default) is the Title then
  the Description in chat, as before. Another plugin can add a Display through
  `GuidebookPlugin#getDisplaySwitchboard()`. If the chosen one isn't registered or fails, Welcomes
  are shown in chat and the server log says why. A Display that can't show a Welcome for an
  expected reason throws `Display.Unavailable`, which sends it to chat without logging a failure.
- Staff (`guidebook.staff`) who join while the Display chosen in `config.yml` isn't running are
  told so, and that Welcomes are using the Chat display.
- The Edit dialog's Preview shows the Description as the server's Display would. A Display shows
  its own preview by overriding `Display#previewDescription`; by default it previews as chat.

### Changed

- An Area marker's popup on the Web map is a compact card: the Area name (marked if the Area is
  Disabled), the Title and Subtitle on one line, then the Description, all in the colours players see.
  Missing text shows as a dashed "no Title", "no Subtitle" or "no Description". The Shape is no longer
  listed.
- Re-Enabling an Area Welcomes the players standing in it.
- `/guidebook on` Welcomes you to the Areas you're standing in, instead of waiting for the next
  one you enter.
- `/guidebook reload` no longer undoes `/guidebook on` and `off` used since the server started.
- `GuidebookSendEvent` isn't fired for Opted out players.
- A polygon Area's corners are written to its Area file in the order staff selected them. The
  polygon is unchanged, and older Guidebook versions read it the same.

### Fixed

- An Edit dialog save whose Area file can't be written leaves the Area as it was, as the "NOT
  saved" message says. Before, the new Title, Subtitle, Description, Show title, Boss bar and
  Enabled were already live for players and on the Web map until the next `/guidebook reload`
  undid them.
- A player inside a polygon Area who warps to another world now leaves it, instead of keeping its
  Boss bar and missing the Welcome when they come back.
- Players are Welcomed into polygon Areas on exactly the blocks staff selected with WorldEdit.
  Before, sloped edges could start a Welcome a block early or late.
- An Area file holding no Shape Guidebook knows is now reported in the server log when loading,
  instead of being skipped silently.
- An Area whose world isn't loaded can now be saved (edited or renamed), instead of failing.
- `/guidebook warp` to an Area whose world isn't loaded says so, instead of failing.
- Right after `/guidebook create` or `/guidebook move`, an Area's Warp point, and a sphere's centre,
  sit on whole blocks, as they already did after a `/guidebook reload`.
- Moving an Area checks it for Overlap against every other Area, including one whose name differs
  from it only in case. Before, the Move dialog skipped both.

## [2.0.1] - 2026-10-20

Guidebook for Paper 26.2. Staff edit Areas in dialogs, Area text is MiniMessage, and every Area is
drawn on the web map.

### Upgrading from 1.x

- **This is a one-way upgrade.** On its first start, Guidebook converts every Area file that still
  uses the 1.x markup to MiniMessage and rewrites it. Before converting anything it copies
  `plugins/Guidebook` to `plugins/Guidebook-legacy-backup-<timestamp>`. Going back to 1.x means
  restoring that backup and losing every edit made since.
- Needs Paper 26.2 and Java 25. WorldEdit and PluginUtils are now required (`depend`), so
  Guidebook won't load on a server without them.
- Staff need a 1.21.6 or newer client to edit Areas and to confirm a delete or a move.
- `guidebook.staff` no longer grants `autoteleport.user`.

### Added

- `/guidebook create <area>` and `/guidebook move <area>` place an Area from your WorldEdit
  selection. A Placement dialog checks the placement before anything is saved.
- `/guidebook edit` opens the Edit dialog for the Area you stand in: title, subtitle, show title,
  boss bar, description, enabled, and delete.
- `/guidebook select <area>` sets your WorldEdit selection to the Area's shape.
- When Dynmap is running, every Area is drawn on the web map.
- `GuidebookSendEvent`, which other plugins can cancel to stop a Welcome.
- Releases are published to repo.mcmiddleearth.com as `com.mcmiddleearth:Guidebook`.

### Changed

- Commands are Brigadier commands, with tab completion for Area names.
- Area text is MiniMessage, with a few Guidebook tags. The Edit dialog lists them.
- Built with Gradle instead of Maven.

### Removed

- `/guidebook set` (use `create` or `move`), `title` and `description` (use `edit`), and
  `details`, `enable`, `disable`, `show` and `size`. The console can no longer enable or disable
  an Area.
