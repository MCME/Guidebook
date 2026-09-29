# Improving the `/guidebook` command setup with StrokkCommands 2.3.0

Researched 2026-09-29. Scope: whether `/guidebook`, declared with StrokkCommands 2.3.0 (ADR 0001), can be improved using StrokkCommands features. It covers:
1. records versus classes;
2. subcommands (nested and external classes), and how permissions and requirements are inherited;
3. suggestions and custom arguments, including whether providers must still be static;
4. other 2.3.0 features the repo doesn't use.

Method: I read the repo's command package (`src/main/java/com/mcmiddleearth/guidebook/command/*.java`) and the generated `build/generated/sources/annotationProcessor/java/main/com/mcmiddleearth/guidebook/command/GuidebookRootBrigadier.java`. I checked those against these first-party sources:
- every page in the sidebar of https://commands.strokkur.net/ (last updated 2026-09-25);
- the 2.3.0 annotation source jars in the Gradle cache;
- the GitHub releases, commits and open issues of https://github.com/Strokkur424/StrokkCommands. The task brief says `Strokkur24/`, which is a 404; the repo lives under `Strokkur424/`.

Then I **prototyped every uncertain claim**. I compiled throwaway `@Command` classes with the real 2.3.0 processor, in a scratch copy of the repo (`./gradlew compileJava --offline`), and read the generated `*Brigadier.java`. Section 7 lists those experiments. The only repo file changed is this note.

Jar paths below are abbreviated. The prefix is `~/.gradle/caches/modules-2/files-2.1/net.strokkur.commands/`:
- `common.jar` = `annotations-common/2.3.0/7d12fb88…/annotations-common-2.3.0-sources.jar`
- `paper.jar` = `annotations-paper/2.3.0/a7e1946b…/annotations-paper-2.3.0-sources.jar`
- `permission.jar` = `annotations-common-permission/2.3.0/ac5383dc…/annotations-common-permission-2.3.0-sources.jar`
- `processor-common-2.3.0.jar` and `processor-paper-2.3.0.jar` (no sources jar is published; I used stack traces and generated output).

---

## TL;DR

- **2.3.0 is the latest release.** It was published 2026-09-01. `master` has had only a FUNDING file and a `jap-util` bump since then (last commit 2026-09-21). Nothing below needs an upgrade, and no upgrade is available.
- **Records are supported, but they don't fit this command.** A `record`, whether it's the command itself or a nested `@Subcommand`, turns its components into arguments that all its `@Executes` methods share. That would be ideal for `size <area> radius|corners|height`. However, **`@CustomArg` and custom-suggestion annotations don't work on record components in 2.3.0**, and I verified this. `@CustomArg(AreaArgument.class) InfoArea area` fails the build with `Unknown parameter type`. A `@CustomSuggestion` marker on a component either crashes the processor or is silently dropped. Every shared argument we have is an `<area>`, so records buy nothing today.
- **External subcommand classes work and are the best structural improvement.** `@Subcommand("size") @Permission(STAFF) final GuidebookSize size;` in `GuidebookRoot` lets `GuidebookSize` own its `@Executes` methods, which already sit next to their logic. It can also own its own `@DefaultExecutes`. A deeper `@DefaultExecutes` overrides the root's for that subtree, so bare or incomplete `size …` could show `size`'s help without the root having to parse `args`. I verified this.
- **Permission hazard (verified).** A method-level or class-level `@Permission` attaches to the node where its `@Executes`/`@Subcommand` path ends. So an empty path, such as `@Executes void toggle(@Literal({"enable","disable"}) …)` or `@Subcommand class Staff`, puts `guidebook.staff` **on the root node**. That silently locks `guidebook.user` players out of `on`, `off` and `help`. Never give `@Permission` to anything whose path starts at the root.
- **Optional arguments (new in 2.3.0)** can merge `help` and `help <command>` into one method, and turn the 4 `list` methods into 2. The generated tree keeps the page-before-filter branch order. `set … [sphere <radius>]` is possible but no better.
- **Suggestion and requirement providers must still be static in 2.3.0.** The docs say so ("This requirement will be lifted in a future release"). An instance provider gets only a compiler `Note: … is not static. Is this a mistake?` and its suggestions are **silently dropped** (verified). Our `@CustomSuggestion` markers with static methods are the idiomatic 2.3.0 pattern, so keep them.
- **`@CustomArg` still needs a no-arg constructor.** The generated code does `new AreaArgument()`, and [issue #51](https://github.com/Strokkur424/StrokkCommands/issues/51) (open) proposes provider methods. Our two argument types already fit.
- **Smaller wins:**
  - `@Literal({"watch","unwatch"}) String action` merges two `dev` methods and removes two `@SuppressWarnings`.
  - Repeatable `@Executes` (2.2.0) suits Phase 2, where `edit`, `title` and `description` all open the same dialog.
  - Constructor injection works (`register(commands, plugin)`), but it can't reach providers or `AreaArgument`.
- **Ranked recommendation** (§6):
  1. Adopt the `@Permission` path rule.
  2. Adopt `Optional` for `help` and `list`.
  3. After Phase 2, split `GuidebookRoot` into external subcommand classes, starting with `size` and `dev`.
  4. Merge `dev watch|unwatch` with a multi-literal.
  5. Keep the suggestion markers and `@CustomArg` as they are.
  6. Skip records, `@UseInjection`, `@Aliases`, `@RequiresOP` and executor wrappers for now.

---

## Decisions (2026-09-29)

Reviewed with the maintainer. Where these differ from §6, these win.

- **Permission path rule: adopted.** It's recorded in ADR 0001's Consequences, and a comment on `GuidebookRoot` comes via ticket 12. There's no automated test, because the only test setup is plain JUnit and building the tree may need a server. Instead, any ticket that touches `@Permission` re-runs the *Help and permission hiding* section of ticket 08's smoke checklist.
- **`Optional` for `help`/`list`, and the `dev watch|unwatch` merge: adopted** in ticket 12.
- **Repeatable `@Executes` for `edit|title|description`: adopted** in ticket 09.
- **Splitting `GuidebookRoot` into subcommand classes: rejected, now and after Phase 2** (this reverses §6 item 4). Per-class `@DefaultExecutes` only replaces the root's `args` loop if *every* command with arguments gets its own class, including one-method commands like `show` and `warp`. A partial split gives two ways to declare a command and removes nothing. The flat root also mirrors `GuidebookHelp`'s table line for line.
- **The remaining `@Literal` suppressions: kept per-parameter.** Neither the name-as-literal form nor a class-level `@SuppressWarnings` is used.
- **`set <name>` stays a `@CustomSuggestion` marker**, not an `AreaNameArgument`.
- **Records:** not used. The maintainer will file an upstream issue about `@CustomArg` and suggestion markers on record components (P1 and P2).

---

## 1. Records vs classes

### 1.1 What 2.3.0 supports

- A `record` can be the `@Command` class, or a nested `@Subcommand`. Its **components become arguments** that are parsed before any of its `@Executes` methods, which read them as fields. Sources:
  - https://commands.strokkur.net/common/records/ ("you can offload common arguments to the components of the record");
  - https://commands.strokkur.net/common/subcommands/, section "Inner records";
  - `@Literal`, `@IntArg`, `@LongArg`, `@FloatArg`, `@DoubleArg` and `@StringArg` all declare `@Target({PARAMETER, RECORD_COMPONENT})` (`common.jar`, `net/strokkur/commands/Literal.java` and `net/strokkur/commands/arguments/*Arg.java`).
- The processor constructs **a new record per execution**. In prototype P1 the generated code is `new ProtoNested.Size(StringArgumentType.getString(ctx, "name")).radius(...)`. A record can't hold state between runs, which is good for Phase 2's rule that no per-invocation state lives in command objects.
- `@Permission` on a nested record and the root's `@DefaultExecutes` both carry into the record's subtree correctly (P1).

### 1.2 Why they don't fit Guidebook in 2.3.0 (verified)

Our only argument shared across several methods is `<area>`, which is `@CustomArg(AreaArgument.class) InfoArea`.

- `@CustomArg` is `@Target(ElementType.PARAMETER)` only (`paper.jar`, `net/strokkur/commands/paper/arguments/CustomArg.java`). On a record component, javac propagates it to the canonical constructor parameter, not to the component, and the processor reads the component. **Prototype P1** fails with:
  ```
  error: java.lang.IllegalStateException: Unknown parameter type: com.mcmiddleearth.guidebook.data.InfoArea
    at net.strokkur.commands.internal.paper.PaperPrototypeNodeBuilder.convertUnparsedParameter(PaperPrototypeNodeBuilder.java:125)
  ```
- Custom-suggestion markers such as `@GuidebookSet.AreaNameSuggestions` on a record component (P2):
  - If the marker's `@Target` includes `RECORD_COMPONENT` (the default when there's no `@Target`), the build aborts with `IllegalArgumentException: Cannot convert element of type: RECORD_COMPONENT` at `net.strokkur.jap.source.SourceMapUtil.parseElement`, called from `StrokkCommandsProcessor.fillRegistry`.
  - If it's restricted to `PARAMETER`, the build succeeds but the argument gets **no `.suggests(...)`**, silently.
- Plain types and the built-in `@…Arg` annotations do work on components (P1, P2). For example, `record A(@IntArg(min = 1) int n)` generates `IntegerArgumentType.integer(1)`.
- The v2.2.0 release notes mention fixing "annotations applied to record components [that] did not correctly apply due to malformed targets" (https://github.com/Strokkur424/StrokkCommands/releases/tag/v2.2.0). The fix doesn't extend to `@CustomArg` (a Paper annotation without `RECORD_COMPONENT`) or to user marker annotations. I found no open issue for this.

**What it would look like if it worked** (not possible in 2.3.0):

```java
// before (GuidebookRoot, 4 methods, 3 @SuppressWarnings)
@Executes("size") @Permission(STAFF)
void sizeRadius(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area,
        @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("radius") String radiusForm,
        @IntArg(min = 1) int radius) { … }

// after (hypothetical: fails in 2.3.0 with "Unknown parameter type: InfoArea")
@Subcommand("size") @Permission(STAFF)
record Size(@CustomArg(AreaArgument.class) InfoArea area) {
    @Executes void show(CommandSender sender) { GuidebookSize.show(sender, area); }
    @Executes("radius") void radius(CommandSender sender, @IntArg(min = 1) int r) { … }
    @Executes("corners") void corners(CommandSender sender, BlockPosition p1, BlockPosition p2) { … }
    @Executes("height") void height(CommandSender sender, @IntArg int minY, @IntArg int maxY) { … }
}
```

- **Buys (if fixed upstream):** the `<area>` argument is declared once per command. `radius`, `corners` and `height` move into `@Executes("…")` paths, which removes the `@Literal` parameters and their `@SuppressWarnings` noise. The executors are grouped too.
- **Cost:** not possible today. The workaround is to type the component as a plain `String` and resolve it in the method. That would lose `AreaArgument`'s parse-time error and its Title/Shape tooltips, which regresses spec user stories 3 and 5. **Not recommended.** Consider filing an upstream issue ("allow `@CustomArg` and custom suggestions on record components").

---

## 2. Subcommands: nested, external, shared paths, inherited permissions

### 2.1 What exists in 2.3.0

- **Shared literal paths merge.** Several `@Executes("size")` methods become one `size` literal node. This is what we do today; see the generated `GuidebookRootBrigadier.java`, which has one `Commands.literal("size")` with four branches. Source: https://commands.strokkur.net/common/executors/, "Paths can merge!".
- **Nested classes and records** use `@Subcommand("lit")` on a nested type, with or without `static`. The generated code builds inner classes as `instance.new Dev()`, which I verified in P5. Source: https://commands.strokkur.net/common/subcommands/, and `common.jar` `Subcommand.java` (`@Target({TYPE, FIELD})`, repeatable since 2.2.0 via `container/ManySubcommands.java`).
- **External subcommands ("prototypes")** are a field of any class that has `@Executes` methods but no `@Command`, annotated `@Subcommand("lit")`. An uninitialised field defaults to `new X()`. It can instead be initialised inline or, if `final`, in the constructor (v2.0.0 release notes). `@Permission` and `@RequiresOP` go on the field or on the class. Prototypes support "the full feature set" and can nest. Source: https://commands.strokkur.net/common/external-subcommands/.
- **Permissions**
  - `@Permission` targets `TYPE`, `METHOD` and `FIELD` (`permission.jar`, `net/strokkur/commands/permission/Permission.java`).
  - Requirements on the same node **stack with AND** (https://commands.strokkur.net/common/requirements/, "Requirement stacking").
  - When a node's children have different permissions, the processor adds an **OR** requirement to the parent, so nobody sees a command they can't use (https://commands.strokkur.net/paper/permissions/, "Merging requirements"). This is why our root has `hasPermission("guidebook.staff") || hasPermission("guidebook.user")`.
  - When all the children share one permission, it's hoisted to the parent (P6: a root whose only children were `edit`, `title` and `description`, all `STAFF`, got `.requires(… "guidebook.staff")` on the root).

### 2.2 The permission-placement hazard (verified, important)

A method-level or class-level `@Permission` attaches to **the node where the `@Executes`/`@Subcommand` path ends**, not to the first literal of the method's parameters. Two of my prototypes put `guidebook.staff` on the **root**:

```java
// P3: literal as the first *parameter*, @Executes path empty
@Executes @Permission(STAFF)
void toggle(CommandSender s, @Literal({"enable", "disable"}) String action, @CustomArg(AreaArgument.class) InfoArea area)
// P7: grouping staff commands in an empty-path subcommand class
@Subcommand @Permission(STAFF) class Staff { @Executes("reload") … @Executes("list") … }
```

Generated root, in both cases: `.requires(source -> source.getSender().hasPermission("guidebook.staff"))`. The `enable`/`disable` (or `reload`/`list`) literals get **no** `requires`. The `on` and `help` literals keep `guidebook.user`, but they're unreachable for a user-only player, because the root rejects them first. Removing `toggle` restored the `staff || user` root (P3b).

**Rule for this repo:** every `@Permission`-carrying `@Executes` or `@Subcommand` must have a non-empty path whose first literal is the command name (`@Executes("enable")`, `@Subcommand("size")`). Never attach `@Permission` to the `@Command` class itself either: requirements stack, so it would gate `on`, `off` and `help` too. It's worth one line in `CONTEXT.md` or ADR 0001.

### 2.3 Candidate: split `GuidebookRoot` into external subcommand classes

**Before** (today): `GuidebookRoot` has 4 `size` methods, each forwarding to static helpers in `GuidebookSize`.

**After** (compiles; P3 and P4 generate `instance.size.radius(...)` under a `size` literal with its own `requires`):

```java
// GuidebookRoot.java
@Command("guidebook")
@Description("manage guidebook areas")
class GuidebookRoot {
    @Subcommand("size") @Permission(STAFF) GuidebookSize size;   // = new GuidebookSize()
    @Subcommand("dev")  @Permission(STAFF) GuidebookDev dev;
    …
}

// GuidebookSize.java: no @Command; needs a non-private no-arg (or injected) constructor
final class GuidebookSize {
    @DefaultExecutes                       // bare or incomplete "size …" shows size's own help
    void help(CommandSender sender) { GuidebookHelp.sendOne(sender, "size"); }

    @Executes
    void show(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) { … }

    @Executes
    void radius(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area,
            @Literal String radius, @IntArg(min = 1) int r) { … }
    …
}
```

- **Buys:**
  - `GuidebookRoot` shrinks from about 330 lines to a table of fields.
  - Each command's declaration sits in the file that already holds its logic (`GuidebookSize`, `GuidebookDev`, `GuidebookList`, `GuidebookSet` and so on), so the thin forwarding methods disappear.
  - A per-class `@DefaultExecutes` **takes precedence over the root's for its subtree**. P4 showed `size`, `size <area> radius` and so on calling `instance.size.sizeHelp(...)`. `common.jar` `DefaultExecutes.java` says "the **deeper one** in the tree takes precedence". So the root's `args`-scanning loop (`GuidebookRoot#help`, including the `execute … run` workaround) could shrink to "show all".
  - The external field also carries the permission once instead of on every method.
- **Cost / risk:**
  - Churn across about 10 files. The static-utility classes (`private GuidebookSize() {}`) become instantiable.
  - The `@Literal` and `@SuppressWarnings` noise **remains**, because the literal still follows `<area>` (records would fix that, but see §1.2).
  - **Phase 2 reshapes `title`, `description`, `delete` and `set`** (tickets 09 and 10): `edit` is added, `description … getbook|save` is removed, and confirmations move to dialogs. So split only the stable commands (`size`, `dev`, `list`, `help`, `rename`) first, and do the rest after Phase 2.
  - A per-class `@DefaultExecutes` duplicates a one-liner per class.
  - **Eject-ability (ADR 0001) is unaffected.** The generated code is still plain Brigadier plus field access (`instance.size.radius(…)`).
- **Alternative, cheaper:** nested `@Subcommand("size") class Size { … }` inside `GuidebookRoot`. The generated code is `instance.new Size()` (P5). This groups methods and permissions but keeps one big file, so it buys less.

---

## 3. Suggestions and arguments

### 3.1 Are our `@CustomSuggestion` markers idiomatic? Yes.

The documented pattern is exactly what `GuidebookSet.AreaNameSuggestions` and `GuidebookHelp.CommandSuggestions` do:
1. declare a marker annotation meta-annotated with `@CustomSuggestion`;
2. annotate a static provider with it;
3. put it on the parameter.

Sources: https://commands.strokkur.net/common/suggestions/ and `common.jar` `CustomSuggestion.java`. The generated code is a method reference (`.suggests(GuidebookSet::suggest)`), which is ideal for eject-ability. `@Retention(SOURCE)` is optional (docs).

`CustomSuggestion.java` accepts four provider shapes. All but the first must be "**statically accessible**":
1. a class implementing `SuggestionProvider<S>` with a no-arg constructor;
2. a static method `(CommandContext<S>, SuggestionsBuilder)`, which is what we use;
3. a static method returning a `SuggestionProvider<S>`;
4. an initialised static field.

**Are providers still static-only in 2.3.0? Yes (verified, P5).**
- An instance method annotated with a marker compiles, but the processor prints `Note: This method matches the @ProtoMisc.InstanceSugg provider method, but is not static. Is this a mistake?`, and the argument is generated **without** `.suggests(...)`.
- The docs still say "This requirement will be lifted in a future release" (the suggestions and requirements pages).
- The v2.2.0 notes say instanced wrappers "will be reintroduced in a later release, alongside instanced suggestions/requirements". Note that `common.jar` `CustomExecutorWrapper.java` still says wrappers "may be non-static", which is stale per v2.2.0.
- ADR 0001's sentence ("Suggestion and requirement providers must be static until StrokkCommands lifts that restriction") remains accurate.

**Optional alternative for `set <name>`:** replace the marker and static method with a third argument type:

```java
// before
void set(…, @GuidebookSet.AreaNameSuggestions @StringArg String name)
// after
void set(…, @CustomArg(AreaNameArgument.class) String name)
final class AreaNameArgument implements CustomArgumentType.Converted<String, String> {
    public String convert(String name) { return name; }               // accepts new names
    public ArgumentType<String> getNativeType() { return StringArgumentType.word(); }
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> c, SuggestionsBuilder b) {
        return AreaArgument.suggestAreaNames(b);
    }
}
```

- **Buys:** symmetry with `AreaArgument` and `NewAreaNameArgument`, one fewer annotation type, and no static-provider constraint (`listSuggestions` is an instance method of the argument).
- **Cost:** a class instead of 6 lines. It's a matter of taste, so it's low priority. Keep `CommandSuggestions`: it reads the sender, which is fine in a static provider via `ctx.getSource()`.

### 3.2 `@CustomArg(AreaArgument.class)` and `@CustomArg(NewAreaNameArgument.class)`

- This is the documented way to plug in a Paper `CustomArgumentType` (`paper.jar` `CustomArg.java`: "The value **must** implement `CustomArgumentType`").
- The generated code is `Commands.argument("area", new AreaArgument())` plus `ctx.getArgument("area", InfoArea.class)`, so a **no-arg constructor is required** and no provider hook exists. [Issue #51](https://github.com/Strokkur424/StrokkCommands/issues/51) ("A better way to use custom argument types", open since 2026-08-31) proposes `@CustomArgFor(Type.class)` provider methods, and would allow bare `InfoArea area` parameters. Ours read the static `PluginData`, so no change is needed.
- `@CustomArg` can't be shortened with a meta-annotation, because its `@Target` is `PARAMETER` only. The 2.0 "inherited annotations" feature covers only `@Executes`, `@DefaultExecutes` and `@Subcommand` ([PR #38](https://github.com/Strokkur424/StrokkCommands/pull/38)).
- The **enum argument support** new in 2.3.0 (release notes) doesn't apply: we have no enum-typed arguments.

### 3.3 The `@Literal` + `@SuppressWarnings({"unused","SameParameterValue"})` noise

There are 10 sites today. Phase 2 removes 2 (`description … getbook|save`), leaving 8: `set … sphere`, `size … radius|corners|height`, and `dev console|level|watch|unwatch`. The options in 2.3.0 are:

| Option | Works in 2.3.0? | Effect |
|---|---|---|
| Records (§1) | **No** (custom arg on component) | Would remove the size/set sites |
| Multi-value `@Literal({"watch","unwatch"}) String action` whose value **is used** | Yes (P5: generates two literals, passing `"watch"`/`"unwatch"`) | Removes 2 `dev` sites, merges 2 methods |
| `@Literal String radius` (the name is the literal, `common.jar` `Literal.java`) | Yes | Shorter, but still unused, so the suppression stays |
| `@Literal Optional<String> sphere` (2.3.0 optionals) | Yes | Merges `set`/`setSphere`, but the param is still unused (§4.1) |
| One class-level `@SuppressWarnings({"unused","SameParameterValue"})` | Yes (IDE inspections only; javac doesn't warn) | Removes all per-parameter noise; blunt |

Before and after for `dev`:

```java
// before: 2 methods, 2 suppressions
@Executes("dev") @Permission(STAFF)
void devWatch(CommandSender s, @Executor Player p, @SuppressWarnings({…}) @Literal("watch") String watch) { DevUtil.add(p); … }
@Executes("dev") @Permission(STAFF)
void devUnwatch(CommandSender s, @Executor Player p, @SuppressWarnings({…}) @Literal("unwatch") String unwatch) { DevUtil.remove(p); … }
// after
@Executes("dev") @Permission(STAFF)
void devWatch(CommandSender s, @Executor Player p, @Literal({"watch", "unwatch"}) String action) {
    if (action.equals("watch")) DevUtil.add(p); else DevUtil.remove(p);
    GuidebookDev.showState(s);
}
```

Do **not** use this trick for `enable|disable`. Their literal would be the first parameter, so the `@Executes` path would be empty and the permission would land on the root (§2.2).

---

## 4. Other 2.3.0 features

### 4.1 Optional arguments (new in 2.3.0)

`Optional<T>`, `OptionalInt` and similar parameters become trailing optional arguments, with `Optional.empty()` passed when they're missing. Required arguments may follow optional ones if they're nullable (boxed or `@Nullable`), in which case `null` is passed. Source: https://github.com/Strokkur424/StrokkCommands/releases/tag/v2.3.0 ([PR #49](https://github.com/Strokkur424/StrokkCommands/pull/49)). **Not yet on the docs site.**

```java
// before: helpAll + helpOne, list + listPage + listFiltered + listFilteredPage (6 methods)
// after (P3 generates the same tree as today; the "page" branch is still registered ahead of "filter")
@Executes("help") @Permission(USER)
void help(CommandSender sender, @GuidebookHelp.CommandSuggestions Optional<String> command) {
    command.ifPresentOrElse(c -> GuidebookHelp.sendOne(sender, c), () -> GuidebookHelp.sendAll(sender));
}
@Executes("list") @Permission(STAFF)
void list(CommandSender sender, OptionalInt page) { GuidebookList.send(sender, "", page.orElse(1)); }
@Executes("list") @Permission(STAFF)
void listFiltered(CommandSender sender, @StringArg String filter, OptionalInt page) { … page.orElse(1) … }
```

- **Buys:** 6 methods become 3, with an identical Brigadier tree. The custom suggestion on an `Optional<String>` parameter still works (P3: `.suggests(GuidebookHelp::suggest)`).
- **Cost:** small. Every prefix of an optional chain becomes **executable**, so it no longer falls through to `@DefaultExecutes`. That's desired for `help` and `list`, but it's why `set` shouldn't use it:
  - `set(…, String name, @Literal Optional<String> sphere, OptionalInt radius)` makes `set <name> sphere` (with no radius) execute (P6).
  - `@IntArg(min = 1) int radius` after the optional literal generates `null` for an `int`, which is a **compile error** (P6).
  - Only `@Nullable Integer radius` works. It's correct, but it's no clearer than the current two methods.

### 4.2 Repeatable `@Executes` (2.2.0) — useful in Phase 2

`@Executes` is `@Repeatable(ManyExecutes.class)` (`common.jar` `Executes.java`; v2.2.0 release notes). One method can serve several literals. P6 generated `edit`, `title` and `description` literals, each calling `instance.edit(...)`:

```java
@Executes("edit") @Executes("title") @Executes("description") @Permission(STAFF)
void edit(@Executor Player player, @CustomArg(AreaArgument.class) InfoArea area) { EditDialog.open(player, area); }
```

- **Buys:** one method for ticket 09's "`title <area>` and `description <area>` open the same dialog".
- **Cost:** none. `@Executes` can also be a meta-annotation (PR #38), but that's not needed.

### 4.3 Constructor injection

Constructor parameters are added to the generated `create`/`register` methods (https://commands.strokkur.net/common/dependency-injection/). P3 generated `register(Commands commands, JavaPlugin plugin)` and `new ProtoExternal(plugin)`. `final` external-subcommand fields can be initialised in that constructor (P3, and the v2.0.0 notes).

```java
// before
LifecycleEvents.COMMANDS, event -> GuidebookRootBrigadier.register(event.registrar())
// after
LifecycleEvents.COMMANDS, event -> GuidebookRootBrigadier.register(event.registrar(), plugin)
```

- **Buys:** executors could use an injected `GuidebookPlugin` instead of `GuidebookPlugin.getPluginInstance()` (today only `GuidebookSet.confirmMove`, which ticket 10 deletes).
- **Cost:** it **can't reach** suggestion providers (static, §3.1) or `AreaArgument`/`NewAreaNameArgument` (no-arg `new`, §3.2), and those read the static `PluginData` anyway. So the static store stays, and DI would be half-applied. [Issue #53](https://github.com/Strokkur424/StrokkCommands/issues/53) (open) notes that `@Nullable` on constructor parameters isn't carried over. **Defer** until instanced providers ship.
- `@UseInjection` (2.1.0; `common.jar` `UseInjection.java`) is for Guice-style frameworks (a jakarta `@Inject`). It's irrelevant here.

### 4.4 `@Executor` and special parameters

- Since 2.0.0, `CommandSender`, `CommandSourceStack`, `CommandContext` and executor parameters may appear **anywhere, or be omitted** (v2.0.0 notes). P6 compiled `@Executor Player player` as the first parameter. The `paper.jar` `Executor.java` Javadoc ("can only be applied to the second parameter") is stale.
- **Tidy-up:** drop the unused `CommandSender sender` from `set`/`setSphere` (the sender is never read). Keep it where a message goes to the sender. Negligible value, so do it only in passing.
- The player-only failure message, `This command requires a Player executor!`, is hard-coded in the generated code (see `GuidebookRootBrigadier.java`). No annotation customises it. A static `@CustomExecutorWrapper` could catch and rethrow it, but that's hacky, so leave it.

### 4.5 Features to skip, with reasons

| Feature | Source | Why not |
|---|---|---|
| `@Aliases` | `common.jar` `Aliases.java`; https://commands.strokkur.net/paper/first-command/ | The spec: "No aliases are added" |
| `@RequiresOP` | `paper.jar` `RequiresOP.java` | The docs recommend `@Permission` instead (https://commands.strokkur.net/paper/permissions/); we have two permission nodes |
| `@CustomRequirement` | https://commands.strokkur.net/common/requirements/ | Our requirements are plain permissions. It can't take annotation values yet ([issue #55](https://github.com/Strokkur424/StrokkCommands/issues/55), open). A "has a WorldEdit selection" requirement would *hide* `set`, which is worse than the current error |
| `@CustomExecutorWrapper` | `common.jar` `CustomExecutorWrapper.java`; static-only since 2.2.0 | Could centralise the repeated `IOException` → log + error message, but the executors don't throw. Low value, and more magic to eject |
| `@DefaultToExecutor`, enum args (2.3.0) | `paper.jar` `DefaultToExecutor.java`; v2.3.0 notes | No player-target or enum arguments in `/guidebook` |
| `create(String name)` overload (2.3.0) | Generated `GuidebookRootBrigadier.java` | Only needed to register under another name |

---

## 5. How each candidate interacts with Phase 2 and ADR 0001

- **Phase 2 (tickets 09 and 10)** rewrites the executors for `title`, `description`, `delete` and `set`, and adds `edit`. Anything that moves those four now will be touched again. `size`, `dev`, `list`, `help`, `rename`, `warp`, `details`, `show`, `enable`, `disable`, `on`, `off` and `reload` are stable.
- **Eject-ability.** Every recommended option still generates plain Brigadier with method references, `new X()`, field access (`instance.size…`) or `Optional.of/empty`. Nothing adds a runtime dependency. Records (if they worked) generate `new Record(args…).method(…)`, which is also ejectable.
- **Static-provider constraint.** It's unchanged by every option. §3.1's `AreaNameArgument` alternative sidesteps it for `set`.

---

## 6. Ranked recommendations

1. **Adopt the permission-placement rule now (no code change).** Every `@Permission` needs a non-empty `@Executes("<cmd>")` or `@Subcommand("<cmd>")` path, and there must be no class-level `@Permission` on `GuidebookRoot`. Otherwise `guidebook.staff` lands on the root and user-only players lose `on`, `off` and `help` without any error (§2.2, verified). Record it in ADR 0001 or `CONTEXT.md`. Cost: none.
2. **Use `Optional` for `help` and `list` (2.3.0; low churn).** 6 methods become 3 with an identical tree (§4.1). Don't use it for `set`. Cost: tiny, and no Phase 2 conflict.
3. **Merge `dev watch|unwatch` with `@Literal({"watch","unwatch"})` (low churn).** It removes 2 suppressions (§3.3). Not for `enable|disable` (§2.2).
4. **After Phase 2, split `GuidebookRoot` into external subcommand fields (medium churn).** Start with `size` and `dev`, each owning its `@Executes` and its own `@DefaultExecutes` (§2.3). It shrinks the root, and the per-class `@DefaultExecutes` could replace the root's `args` parsing. Wait for Phase 2 before doing `set`, `title`, `description` and `delete`, and use repeatable `@Executes` for `edit|title|description` (§4.2).
5. **Keep the `@CustomSuggestion` markers and `@CustomArg` types as they are.** They're the idiomatic 2.3.0 pattern, and providers must stay static (§3.1, verified). Optionally, turn `set`'s marker into an `AreaNameArgument` for symmetry (low priority).
6. **Don't adopt, for now:**
   - **records**: blocked by `@CustomArg` and custom suggestions on components (§1.2, verified). Revisit if upstream fixes it; consider filing an issue.
   - **constructor DI**: it can't reach providers or argument types until instanced providers ship.
   - `@UseInjection`, `@Aliases`, `@RequiresOP`, `@CustomRequirement`, executor wrappers (§4.5).
   - a blanket class-level `@SuppressWarnings`: acceptable if the noise bothers you, but it hides real unused parameters.

---

## 7. Prototype log (scratch copy, real 2.3.0 processor)

All in a scratch copy of the repo (`…/scratchpad/proto`), built with `./gradlew compileJava --offline`, and checked by reading `build/generated/sources/annotationProcessor/.../*Brigadier.java`. The baseline repo compiled unchanged first.

| # | Declaration | Result |
|---|---|---|
| P1 | `@Subcommand("size") @Permission(STAFF) record Size(@CustomArg(AreaArgument.class) InfoArea area)` | **Build error** `Unknown parameter type: …InfoArea` (`PaperPrototypeNodeBuilder.convertUnparsedParameter`) |
| P1b | the same with `record Size(String name)` | OK: `new ProtoNested.Size(getString(ctx,"name")).radius(…)`; `size` gets `requires(staff)`; the root `@DefaultExecutes` fills the incomplete nodes |
| P2 | a record component with a `@CustomSuggestion` marker (no `@Target`, or `RECORD_COMPONENT`) | **Processor crash** `Cannot convert element of type: RECORD_COMPONENT` (`SourceMapUtil.parseElement` ← `StrokkCommandsProcessor.fillRegistry`) |
| P2b | the same marker with `@Target({PARAMETER, METHOD})` | Compiles; **no `.suggests`** generated |
| P2c | `record A(@IntArg(min = 1) int n)` | OK: `IntegerArgumentType.integer(1)` |
| P3 | External `@Subcommand("size") @Permission(STAFF) final ProtoSizeSub size` set in the constructor; `ProtoExternal(JavaPlugin)`; `Optional<String>` help; `OptionalInt` list; empty-path `toggle` with `@Literal({"enable","disable"})` + `@Permission(STAFF)` | Compiles. `register(Commands, JavaPlugin)`. `instance.size.radius(…)`. The optionals are correct. **Root `requires(staff)` only**, with no requires on `list`/`size`/`enable`/`disable` |
| P3b | P3 without `toggle` | Root `requires(staff \|\| user)`; `list` and `size` each `requires(staff)` |
| P4 | `@DefaultExecutes void sizeHelp(CommandSender)` inside the external `ProtoSizeSub` | `size` and `size <area> radius` call `instance.size.sizeHelp(…)`, overriding the root's |
| P5 | Instance (non-static) `@CustomSuggestion` provider; non-static inner `@Subcommand("dev") @Permission(STAFF) class Dev` with `@Literal({"watch","unwatch"})`; `@Aliases("gb")` | Note "…is not static. Is this a mistake?" and **no `.suggests`**; `instance.new Dev()`; `watch`/`unwatch` literals; `ALIASES = List.of("gb")` |
| P6 | `set(@Executor Player, name, @Literal Optional<String> sphere, OptionalInt radius)`; then with `int radius`; then with `@Nullable Integer radius`; repeatable `@Executes("edit") @Executes("title") @Executes("description")` | `set <name> sphere` became executable; `int` → **compile error** (`null` → `int`); `@Nullable Integer` OK; repeatable produces 3 literals → one method |
| P7 | `@Subcommand @Permission(STAFF) class Staff { @Executes("reload") … @Executes("list") … }` beside `@Executes("on") @Permission(USER)` | **Root `requires(staff)`**; `reload`/`list` have no requires |

## Open questions / unverified

- **Nothing was run on a server.** The permission-hazard conclusion rests on reading the generated `.requires(...)` calls, which is conclusive for Brigadier. Still, smoke-test any restructuring with a `guidebook.user`-only account in `runServer`.
- I didn't decompile the processor. The behaviour is inferred from generated output and stack traces, which is enough for 2.3.0 but won't predict future versions.
- It's unknown whether a future release will make `@CustomArg` and custom suggestions work on record components. No open issue tracks it, and 2.3.0 is the latest release as of 2026-09-29.
- Whether `@Permission` works as a meta-annotation (its `TYPE` target technically allows it on annotation types) was not tested. Nothing here needs it.
