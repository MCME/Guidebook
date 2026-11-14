# An Area holding another is drawn as an outline, and every change redraws every Area

On LiveAtlas, a click or hover goes to the shape drawn last, and Guidebook can't choose that order: LiveAtlas draws a marker set's polygons before its circles and otherwise follows Dynmap's internal map order, and a marker set's priority only stacks icons, not shapes. So a big Area could cover the Areas inside it and leave them unclickable. LiveAtlas draws an area or circle with no fill as a bare line that only catches clicks on itself, so an Area whose footprint box (seen from above, heights ignored) holds another Area's box in the same world is drawn with no fill and a thicker line, and the Areas inside it can be clicked anywhere. Of two Areas with the same box, the first by name gives up its fill, so the other stays clickable. Since one Area's change can decide whether another holds it, every create, delete, rename, move and save redraws every Area, and the Dynmap adapter skips any marker equal to the one it last drew, so only markers that actually changed reach the map.

## Considered Options

- **Draw small Areas last:** the order isn't Guidebook's to choose, and toggling the layer re-adds its shapes anyway.
- **One marker set per nesting depth:** marker set priority doesn't stack shapes in LiveAtlas, and staff would have a checkbox per depth.
- **Outline any Area that overlaps a smaller one:** partly overlapping Areas each have a part only they cover, so both are already clickable; this would only take away a fill.
- **Exact containment instead of boxes:** more work for little gain, since a loose box only ever costs an Area its fill.

## Consequences

An outlined Area can only be hovered or clicked on its line. Stock Dynmap's own web page still draws an Area with no fill as a polygon that catches clicks, so there the Areas inside stay covered, though the outlined Area is drawn without a fill.
