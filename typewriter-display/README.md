# Guidebook's Typewriter display

The Typewriter display types each [Guidebook](../README.md) Welcome into a framed box over the
chat. The Area's name sits in the top border, the Description is typed out on Pages of 8 lines,
and the bottom border says which key turns the Page or closes the box. The Title types in on
screen first. The box stays until the player closes it.

It's a [Typewriter](https://typewritermc.com) extension, released separately from Guidebook on
its own `typewriter-v*` tags ([ADR 0008](../docs/adr/0008-welcomes-shown-through-a-display.md)).
Guidebook works without it: whenever it can't show a Welcome (it isn't installed, Typewriter isn't
ready for that player, the player is in another Typewriter dialogue), that Welcome is shown in chat.

## What it needs

- **Guidebook**, at least the version the release notes name as the lowest Guidebook version
- **Typewriter `0.9.0-beta-177`**, exactly. Typewriter only loads an extension built for its
  running build, so each Typewriter update needs a new release of this extension. The release
  notes name the Typewriter build each one is for
- **BasicExtension**, the build made for that Typewriter build, in `plugins/Typewriter/extensions/`
- **PacketEvents**, which Typewriter needs on MC 26.2

## Installing

From the [GitHub release](https://github.com/MCME/Guidebook/releases) (the `typewriter-v*` ones):

1. Put `typewriter-display-<version>.jar` in `plugins/Typewriter/extensions/`
2. On first install, put `guidebook.json` (the Typewriter page) in `plugins/Typewriter/pages/`
3. Choose it in `plugins/Guidebook/config.yml`:

   ```yaml
   display: typewriter
   ```

4. Set Typewriter's confirmation key to F, in `plugins/Typewriter/config.yml`:

   ```yaml
   confirmationKey: SWAP_HANDS
   ```

   Typewriter's default is sneak, which players press all the time while moving, so they'd skip
   and close boxes without reading them. The footer names whichever key is set.
5. Restart the server

**Restart after any change**: installing, upgrading, swapping the Typewriter page or changing
`display`. If the extension can't run (no Typewriter page, a duplicated entry, a Guidebook older
than it needs), the server log says why in one line, and Welcomes are shown in chat.

### Upgrading

Replace the jar and restart. **Keep your Typewriter page** unless the release notes say to replace
it: they do only when a release adds or renames a setting. Replacing `guidebook.json` resets every
setting below to its default, losing staff's panel edits.

### Reloading

Reloading Typewriter keeps the Typewriter display working. **Reloading Guidebook alone** (with a
plugin manager) leaves it unregistered, so Welcomes are shown in chat, with a warning in the log,
until Typewriter reloads or the server restarts. MCME restarts rather than reloading single
plugins, so this isn't handled. `/guidebook reload` doesn't count: it only rereads Areas.

## Settings

Staff change these in Typewriter's web panel, on the Guidebook Welcome entry in the `guidebook`
Typewriter page. They apply to every Area.

| Setting                                                         | Default                                |
|-----------------------------------------------------------------|----------------------------------------|
| Typing speed (`typingSpeed`)                                    | 20ms per character, spaces not counted |
| Per-Page typing cap (`pageTypingCap`)                           | 4s                                     |
| Header colour (`headerColor`)                                   | `#e3c77d`                              |
| Title: ticks per character (`titleTicksPerCharacter`)           | 2                                      |
| Title: pause before the Subtitle (`titleSubtitlePauseTicks`)    | 6 ticks                                |
| Description delay after the Subtitle (`descriptionDelayTicks`)  | 24 ticks                               |

The box's size and line width aren't settings: they match default chat.

### Why the entry lives in a Typewriter page

The Guidebook Welcome entry could be built in code, but that relies on how Typewriter looks entries
up today, which Typewriter doesn't support, and it would take the settings out of the panel. So the
entry ships in a Typewriter page, with an id that never changes between releases.

## Known limits

- **The box fits default chat settings only.** A player who changed chat width, height, text size
  or line spacing sees the box cut off or padded. The server can't see those settings.
- **The box's blank grey shape lingers briefly after it closes**, because a server can't delete
  chat messages.
- **Every Typewriter update needs a rebuilt extension**, since it only loads on the Typewriter
  build it names.
- **A Page holds roughly 400 characters.** Longer Descriptions carry on to more Pages.

## Developing

It's a Kotlin Gradle subproject of Guidebook. From the repository root:

```sh
./gradlew :typewriter-display:build   # compile, test, and write the jar to typewriter-display/build/libs/
./gradlew runServer                   # a test server with Typewriter and this extension installed
```

### Releasing

1. Set `version` in [`build.gradle.kts`](build.gradle.kts), and date its section in
   [`CHANGELOG.md`](CHANGELOG.md) (`## [0.1.0] - 2026-11-21`), filling in the template there
2. Merge into `master`, tag the merge commit `typewriter-v<version>` and push the tag

CI checks that the tag matches the jar's version and that the notes name the Typewriter build and
the lowest Guidebook version built into the jar, and whether to replace the Typewriter page. It then creates
the GitHub release with the jar, its SHA-256 and `guidebook.json`. Nothing is published to Maven.
