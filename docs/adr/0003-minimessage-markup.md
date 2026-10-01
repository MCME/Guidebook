# Area text is MiniMessage, converted once from legacy markup

Titles, Subtitles and Descriptions are stored and typed as MiniMessage. Each Area file still in legacy markup (`§` codes typed as `#`, `#RRGGBB`, `[Click="…"]`, `[Hover="…"]`, `\n` line markers) is converted when it loads, and then rewritten with a marker so it's converted only once. The Guidebook folder is copied to a timestamped backup before any conversion.

The conversion parses the legacy text with PluginUtils' own parser and turns the result into an Adventure component, then serialises it with MiniMessage. This keeps exactly what players saw, including click and hover actions. `LegacyComponentSerializer` and EnhancedLegacyText only understand colour codes, so they would have left `[Click]`/`[Hover]` as literal text.

## Considered Options

- **Keep legacy markup and only render it with Adventure:** no migration, but `#` can't be typed as a character, hex colours can't be entered in the dialog, and staff can't use the Adventure WebUI to preview.
- **Convert offline before deploy:** no legacy code ships, but an old file restored later would show raw `§` codes and `[Click=` tags.

## Consequences

One isolated class still imports PluginUtils' message parser. PluginUtils stays a dependency for regions anyway, and the class can be deleted in a later release. Converted text keeps the old quirks: hover text keeps PluginUtils' hard line breaks, and text that the old parser misread as a hex colour stays misread. Reverting to a pre-MiniMessage build of the plugin means restoring the backup, which loses any edits made since.

MiniMessage's strict mode isn't used. It rejects unclosed tags, which is how staff, the WebUI and MiniMessage's own serialiser all write text.
