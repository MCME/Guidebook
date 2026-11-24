# The Web map API is vendor-neutral and knows nothing about Areas

Guidebook draws every Area on the server's Web map (Dynmap today) so staff can scan them. It does this through its own small API rather than calling Dynmap directly: layers of markers, where a marker is an id, a world, a plain-text label, an HTML popup, a style, and an outline that is either a circle or a polygon with an optional Y range. Circles and polygons are what Dynmap, BlueMap, squaremap and Pl3xMap all share, so swapping maps means writing one adapter. The API deliberately has no notion of an Area, because it is meant to move into a shared MCME library (likely PluginUtils) for other plugins to use. Until then it lives in its own package that imports nothing from Guidebook, and Guidebook translates an Area into a marker on its side of the line.

## Considered Options

- **Call Dynmap directly:** least code, but every later map change rewrites Guidebook.
- **An API that speaks in Areas** (`show(area)`): smaller for Guidebook, but useless to any other plugin, and each adapter would repeat the Shape-to-outline work.

## Consequences

Popups are HTML fragments styled inline, and adapters pass them through untouched. All four maps take HTML popups, but only LiveAtlas (MCME's Dynmap front end) has been checked to show them as written; another map may clean some of it away. Callers escape their own text. (Popups were plain text at first; they became HTML so staff could proofread Area text in its colours.) BlueMap has no true circle, so its adapter would approximate spheres as polygons.
