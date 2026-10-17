# Guidebook

<p align="center">
  <img src="docs/images/title_example.png" width="494" alt="A player seeing a title when entering an area">
</p>

Guidebook is a Paper plugin built for MCME. It welcomes players to areas that they walk into -
an on-screen title and a description in chat

## Using it

**Players** can toggle Welcomes with `/guidebook off`, and `/guidebook on`.

**Staff** (`guidebook.staff`, ops by default) create and edit Areas in dialogs, which need a
1.21.6 or newer client. `/guidebook help` lists every command you can run.

To **create** an Area:

1. Make a cuboid or polygon WorldEdit selection
2. Run `/guidebook create <world>-<project>-<place>`
    - A dialog checks the placement, and then you can enter the Area's text
    - Your location becomes the warp point

To **edit** an Area's text, stand in it and run `/guidebook edit`. To **reshape or move** it, run
`/guidebook select <area>`, adjust the WorldEdit selection, then run `/guidebook move <area>`.

Area text is [MiniMessage](https://docs.advntr.dev/minimessage/format.html), a handful of custom
tags have been created for uniformity. The Edit dialog lists them (hover over "Guidebook tags")

When *Dynmap* is running, every Area is drawn on the web map, so staff can review Areas quicker
than they could in-game

## Installing

Build the jar (see below) and put it in `plugins/`. WorldEdit is required. Dynmap and
Multiverse-Core are supported if they're present, and PluginUtils is only needed to convert Area
files from Guidebook 1.x. Each Area is saved as `plugins/Guidebook/<world>/<area>.yml`. If you
edit one by hand, run `/guidebook reload`.

## Developing

```sh
./gradlew build       # compile, test, and write the jar to build/libs/
./gradlew runServer   # start a Paper test server in run/ with WorldEdit and PluginUtils
```

The Paper and Java versions are set in [`gradle/libs.versions.toml`](gradle/libs.versions.toml)
and [`build.gradle.kts`](build.gradle.kts). The first build installs a pre-commit hook that
formats staged Java files with Spotless.

Before changing anything, read [`GLOSSARY.md`](GLOSSARY.md) for what the domain terms mean, and
[`docs/adr/`](docs/adr) for why things are the way they are.
