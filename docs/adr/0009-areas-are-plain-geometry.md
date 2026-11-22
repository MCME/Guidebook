# Areas are plain geometry, and a prism's inside is WorldEdit's rule

An Area holds its Shape as an `AreaGeometry`, the name of its world and its Warp point, all plain values. It doesn't hold a Bukkit `World` or a PluginUtils `Region`. Bukkit worlds are looked up by name only at the edges that need a live one: teleporting to a Warp point, and asking whether a world is loaded before drawing its Areas on the Web map. "Is this point inside a prism" is answered by WorldEdit's static `Polygonal2DRegion.contains`, for Welcomes, the Overlap check and the Placement check alike, so a prism Area is exactly the blocks staff selected.

## Considered Options

- **Keep PluginUtils' `Region` on the Area:** MCME's own library, but a Region needs a loaded world from the moment it's built. Tests then can't build real Areas, an Area in an unloaded world is half an object, and the Placement check needed its own copy of the prism rule. Its prism rule also disagrees with WorldEdit on sloped edges and ignores the world for near and corners.
- **Add a world-free rule to PluginUtils and call it from Guidebook:** no copy, but every fix would wait on a PluginUtils release and a raised lowest version.
- **Keep a copy of PluginUtils' prism rule in Guidebook:** no release needed, but it keeps two copies of a rule known to be wrong at sloped edges.

## Consequences

Guidebook depends on WorldEdit for containment, which it already requires (`depend: [WorldEdit, PluginUtils]`). PluginUtils stays a dependency for Legacy markup conversion only. Reopen this if PluginUtils gains a world-free inside rule that agrees with WorldEdit and Guidebook would rather share it. Before putting a `World` or `Location` back on the Area, check that tests can still build a real Area from plain numbers.
