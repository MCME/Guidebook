# Guidebook

Guidebook tells players about the places they walk into on MCME. Staff define named regions of the world, and each one welcomes a player who enters it.

## Language

**Guidebook**:
The plugin and the system as a whole. It never refers to one region.
_Avoid_: "a guidebook" meaning one area

**Area**:
A named region of a world with its own Title, Subtitle and Description. Player-facing text may call it a "Guidebook area".
_Avoid_: guidebook, info area, region (when you mean the whole thing rather than its shape)

**Area name**:
The identifier of an Area. It is unique ignoring case and may contain only letters, digits and `_ - . +`. By convention it is written `<world>-<project>-<place>`, and each hyphen-separated segment can be searched on its own.

**Shape**:
The geometry of an Area: a sphere, a cuboid, or a prism (a polygon extruded between two heights).
_Avoid_: size (that is the command which changes a Shape's dimensions)

**Title**:
The short name of an Area, shown at the centre of the screen when a player enters it (if Show title is on). It is written in MiniMessage, and its length limit counts only the visible text.

**Show title**:
Whether an Area's Title and Subtitle appear on screen when a player enters it.

**Boss bar**:
An optional bar at the top of the screen that shows the Area's Title for as long as the player stays inside the Area.
_Avoid_: scoreboard

**Subtitle**:
The line shown under the Title on entry. It is written in MiniMessage, and its length limit counts only the visible text.

**Description**:
The body text of an Area, sent to the player in chat when they enter it. It is written in MiniMessage. By convention it opens with the `<guide>` tag (`Guide:` in dark aqua followed by white text), and a new Area starts with that opening.

**Legacy markup**:
How Titles, Subtitles and Descriptions were written before MiniMessage: `§` colour codes (typed as `#`), `#RRGGBB` hex colours, and `[Click="…"]`/`[Hover="…"]` tags. Each Area is converted from it to MiniMessage once, keeping exactly what players saw.
_Avoid_: old format, PluginUtils markup

**Guidebook tags**:
MiniMessage tags of Guidebook's own, usable in a Title, Subtitle or Description alongside the standard ones. They are permanent once stored text uses them (ADR 0004).

| Tag                | Renders as                                                                                     |
|--------------------|------------------------------------------------------------------------------------------------|
| `<guide>`          | `Guide: ` in dark aqua, then the text after it in white (the house opening)                    |
| `<date>…</date>`   | The text in yellow                                                                             |
| `<term:'meaning'>` | The text in gold and underlined, with the meaning in grey on hover                             |
| `<wiki:page>`      | The text in aqua and underlined, opening the page on Tolkien Gateway                           |
| `<warp:name>`      | The text in green and underlined, running `/warp <name>`; the name is letters, digits, single spaces and `' _ - .` |
| `<bullet>`         | A grey `▸ ` list marker                                                                        |

**Welcome**:
What a player receives when they enter an Area: the Title, then the Description. Welcomes have a cooldown per player per Area.

**Enabled / Disabled**:
Whether an Area welcomes anyone. This is set by staff for each Area.

**Opted out**:
A player who has chosen, for themselves, not to receive any Welcomes.
_Avoid_: excluded, off
