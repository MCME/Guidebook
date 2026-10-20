# Changelog

All notable changes to Guidebook are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions before 2.0.1 have no entries;
see the GitHub history.

## [2.0.1] - Unreleased

Guidebook for Paper 26.2. Staff edit Areas in dialogs, Area text is MiniMessage, and every Area is
drawn on the web map.

### Upgrading from 1.x

- **This is a one-way upgrade.** On its first start, Guidebook converts every Area file that still
  uses the 1.x markup to MiniMessage and rewrites it. Before converting anything it copies
  `plugins/Guidebook` to `plugins/Guidebook-legacy-backup-<timestamp>`. Going back to 1.x means
  restoring that backup and losing every edit made since.
- Needs Paper 26.2 and Java 25. WorldEdit is now required.
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
