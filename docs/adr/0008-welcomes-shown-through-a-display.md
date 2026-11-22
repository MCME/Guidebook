# Welcomes are shown through a Display, and the Typewriter display ships separately

Guidebook's Watchman decides which players get a Welcome and hands each one to a Display, which shows it. The Chat display is built in. Others, starting with the Typewriter display, register with Guidebook by name, and the server's config picks one. If the chosen Display isn't registered or fails, the Welcome goes through chat. Guidebook knows *which* Display is in use and nothing about how it works: settings such as typing speed, lines per page or header colour belong to the Display, not to Guidebook's config or to an Area. The Typewriter display is a Kotlin Typewriter extension. It lives in this repo as its own Gradle subproject and jar, with its own version and `typewriter-v*` tags, because a Typewriter extension only loads on the exact Typewriter build it names. Guidebook's own jar contains no Typewriter or Kotlin code, so it keeps working whether Typewriter is installed or not.

## Considered Options

- **Cancel `GuidebookSendEvent` and show the Welcome from a listener** (what the prototype did): no seam in Guidebook, but the listener skips the cooldown, has to repeat the Opted out check, loses the Boss bar and reads Areas from Guidebook's internals.
- **Build the Typewriter display into Guidebook's jar:** one jar to ship, but Guidebook would need Kotlin and would be tied to one Typewriter build, so every Typewriter update would force a Guidebook release.
- **One version and tag for both jars:** simpler to release, but every Typewriter update would mean a Guidebook release with nothing changed.

## Consequences

`GuidebookSendEvent` goes back to being only a way for other plugins to cancel a Welcome. Displays never listen for it, and previews don't fire it. A setting that only one Display understands, if one is ever wanted per Area, has to be stored on the Area as data Guidebook keeps for that Display without reading it, not as a new Area field. Each Typewriter display release states the lowest Guidebook version it works with.
