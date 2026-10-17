# Commands are declared with StrokkCommands

`/guidebook` moves from a hand-rolled `TabExecutor` to Paper's Brigadier API. We declare the commands with StrokkCommands annotations instead of writing Brigadier trees by hand, because there are 17 subcommands and most follow the same `<area>`-plus-literals pattern. StrokkCommands is an annotation processor: it generates plain Brigadier source at compile time, uses no reflection, and adds nothing at runtime. Custom argument types such as `AreaArgument` plug in through `@CustomArg`.

## Considered Options

- **Hand-written Brigadier:** no dependency, but much more verbose.
- **Cloud (Incendo):** a larger runtime library that has to be shaded into the jar.

## Consequences

The library has a single maintainer, and v2 introduced breaking changes. If it's abandoned or blocks an upgrade, the fallback is to copy the generated `*Brigadier.java` files from the build's generated-sources folder into `src` and remove the processor. Suggestion and requirement providers must be static until StrokkCommands lifts that restriction.

A `@Permission` attaches to the node where its `@Executes` or `@Subcommand` path ends. If the path is empty (for example, the literal is the method's first parameter, or it's an unnamed `@Subcommand` class), the permission lands on the `/guidebook` root. That silently locks `guidebook.user` players out of `on`, `off` and `help`. The same happens with a `@Permission` on the `@Command` class. So every `@Permission` goes on an `@Executes("<command>")` or `@Subcommand("<command>")` with a non-empty path. See `docs/research/strokkcommands-command-setup.md` §2.2.
