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
The short name of an Area, shown at the centre of the screen when a player enters it (if Show title is on).

**Show title**:
Whether an Area's Title and Subtitle appear on screen when a player enters it.

**Boss bar**:
An optional bar at the top of the screen that shows the Area's Title for as long as the player stays inside the Area.
_Avoid_: scoreboard

**Subtitle**:
The line shown under the Title on entry.

**Description**:
The body text of an Area, sent to the player in chat when they enter it. It is written in PluginUtils message markup.

**Welcome**:
What a player receives when they enter an Area: the Title, then the Description. Welcomes have a cooldown per player per Area.

**Enabled / Disabled**:
Whether an Area welcomes anyone. This is set by staff for each Area.

**Opted out**:
A player who has chosen, for themselves, not to receive any Welcomes.
_Avoid_: excluded, off
