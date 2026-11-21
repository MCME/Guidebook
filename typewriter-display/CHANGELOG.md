# Changelog

All notable changes to the Typewriter display are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Guidebook's own changes are in
[the root changelog](../CHANGELOG.md).

Each version's section becomes its GitHub release's notes, which are all a server admin installs
from. It opens with these lines, the first three of which CI checks before releasing:

<!--
## [x.y.z] - YYYY-MM-DD

- **Typewriter build:** `0.9.0-beta-177`
- **Lowest Guidebook version:** `x.y.z`
- **Typewriter page:** keep yours | replace `guidebook.json` (staff's panel edits are lost) | new: copy `guidebook.json` into `plugins/Typewriter/pages/`
- **Typewriter config:** set `confirmationKey: SWAP_HANDS` in `plugins/Typewriter/config.yml`, so F turns and closes the box

### Added / Changed / Fixed
-->

- **Typewriter build:** the exact build the jar loads on (`typewriter` in
  [`libs.versions.toml`](../gradle/libs.versions.toml)). CI refuses a release whose notes name
  another.
- **Lowest Guidebook version:** the oldest Guidebook it works with (`lowestGuidebookVersion` in
  [`build.gradle.kts`](build.gradle.kts), built into the jar). CI refuses a release whose notes
  name another.
- **Typewriter page:** `replace` only when the release adds or renames a setting on the Guidebook
  Welcome entry, because replacing it loses staff's panel edits. Otherwise `keep`, or `new` on the
  first release.
- **Typewriter config:** in every release, as every install needs it: Typewriter's default
  confirmation key is sneak, which players press while moving, closing boxes unread.

## [Unreleased]

- **Typewriter build:** `0.9.0-beta-177`
- **Lowest Guidebook version:** the first Guidebook release with Displays, after 2.0.1
- **Typewriter page:** new: copy `guidebook.json` into `plugins/Typewriter/pages/`
- **Typewriter config:** set `confirmationKey: SWAP_HANDS` in `plugins/Typewriter/config.yml`, so F turns and closes the box

### Added

- The Typewriter display: each Welcome typed into a framed box over the chat, in Pages the player
  turns and closes with a key. Choose it with `display: typewriter` in Guidebook's `config.yml`.
