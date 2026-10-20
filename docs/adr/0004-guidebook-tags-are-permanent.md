# Guidebook's own MiniMessage tags are permanent

Area text can use a small set of Guidebook tags on top of MiniMessage's standard ones: `<guide>`, `<date>`, `<term:'meaning'>`, `<wiki:page>`, `<warp:name>` and `<bullet>`. They make common styling look the same in every Area, and each one's look can be changed in one place (`AreaText`). The Legacy conversion writes the old `§3Guide: ` opening as `<guide>`, so every Area stores the house opening the same way.

Text is stored as MiniMessage and parsed leniently, so a tag that no longer exists is shown to players as literal text. Once stored text uses a Guidebook tag, removing or renaming it breaks every Area that uses it, and there's no list of which Areas those are. A tag can still change how it looks.

## Considered Options

- **Expand the tags when saving and store plain MiniMessage:** tags could be removed freely, but changing how one looks would no longer reach existing Areas, which is the reason to have them, and staff would see the expanded text when they edit.
- **Only a written convention (explicit colour tags):** nothing to keep, but every Area drifts in colour and wording, and links to Tolkien Gateway or `/warp` take a long click and hover tag each.

## Consequences

The tag set grows only when staff ask for a tag, and a new tag mustn't use a standard MiniMessage tag's name. Retiring a tag means first rewriting every Area that uses it. `<warp>` assumes players' warp command is `/warp <name>`. If that changes, the tag changes with it.
