# Guidebook's Typewriter display

A [Typewriter](https://typewritermc.com) extension that types each [Guidebook](../README.md)
Welcome into a framed box over the chat: the Title in the top border ("Guide" for an Area with no
Title), the Description on Pages of 8 lines, and the key that turns the Page or closes the box in
the bottom border. It's its own jar, released on `typewriter-v*` tags ([ADR 0008](../docs/adr/0008-welcomes-shown-through-a-display.md)).
Whenever it can't show a Welcome, that Welcome uses the Chat display instead.

## Trying it in this repo

```sh
./gradlew runServer                        # a test server in run/ with Typewriter and this extension
./gradlew runServer -PwithoutTypewriter    # the same, without Typewriter
```

`runServer` installs Typewriter, its dependencies, a fresh build of this extension and its
Typewriter page. After the first start, set `display: typewriter` in
`run/plugins/Guidebook/config.yml` and restart. The log then says "The Typewriter display is
registered with Guidebook".

- **Web panel:** off in a fresh `run/`. Set `enabled: true` and `panel: enabled: true` in
  `run/plugins/Typewriter/config.yml` for `/tw connect`.
- **Typewriter page:** only copied when missing, to keep panel edits. After changing
  `guidebook.json` in the repo, delete the copy in `run/plugins/Typewriter/pages/`.

## Installing on a server

Needs Guidebook, PacketEvents, and the exact Typewriter build the release notes name (Typewriter
only loads an extension built for its running build), with BasicExtension from that same Modrinth
release in `plugins/Typewriter/extensions/`.

1. Put the jar from the [GitHub release](https://github.com/MCME/Guidebook/releases) in
   `plugins/Typewriter/extensions/`
2. On first install, put `guidebook.json` in `plugins/Typewriter/pages/`
3. Set `display: typewriter` in `plugins/Guidebook/config.yml`, and restart

The footer names Typewriter's `confirmationKey`, whichever it is. Restart after any change. If the
extension can't run, the log says why in one line and staff are told when they join.

**Upgrading:** replace the jar. Keep your `guidebook.json` unless the release notes say to replace
it, since replacing it loses staff's panel edits. **Reloading** Typewriter is fine; reloading
Guidebook alone leaves Welcomes on the Chat display until Typewriter reloads.

## Settings

On the Guidebook Welcome entry, in Typewriter's web panel. They apply to every Area.

| Setting                                    | Default                                |
|--------------------------------------------|----------------------------------------|
| `typingSpeed`                              | 20ms per character, spaces not counted |
| `pageTypingCap`                            | 4s per Page                            |
| `headerColor`                              | `#e3c77d`                              |
| `titleTicksPerCharacter`                   | 2                                      |
| `titleSubtitlePauseTicks`                  | 6 ticks                                |
| `descriptionDelayTicks` (after Subtitle)   | 24 ticks                               |

The entry ships in a Typewriter page rather than being built in code, so its settings stay in the
panel; there must be exactly one, or there's no telling whose settings apply.

## Known limits

- The box fits default chat settings only; the server can't see a player's chat width or scale.
- Its blank shape lingers briefly after closing, as a server can't delete chat.
- Every Typewriter update needs a rebuilt extension.
- A Page holds roughly 400 characters.

## Releasing

Set `version` in [`build.gradle.kts`](build.gradle.kts), date its section in
[`CHANGELOG.md`](CHANGELOG.md) using the template there, merge into `master`, then tag the merge
commit `typewriter-v<version>` and push the tag. CI checks the notes against the jar and creates
the GitHub release with the jar, its SHA-256 and `guidebook.json`.
