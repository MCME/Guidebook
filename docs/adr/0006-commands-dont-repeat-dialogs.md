# Commands don't repeat what a dialog does, and Areas are placed through a Placement dialog

Once Areas were edited in Dialogs (ADR 0002), several commands did what a dialog already did, and `/guidebook` had grown to 16 subcommands. We removed `enable` and `disable` (the Edit dialog sets Enabled) and `show` (saving the Edit dialog plays the Welcome). `details` became `select`, whose one job is to set the staff member's WorldEdit selection to an Area's Shape. `set`, which both created and moved an Area depending on whether the name existed, split into `create` (free names only) and `move` (existing Areas only), so a typo can no longer start a new Area. Both open a Placement dialog that checks the WorldEdit selection before anything is written: it blocks a selection in another world, and warns about a polygon one block tall, any side under 3 or over 1000 blocks, the staff member standing far from the selection, and Overlap with other Areas. `size` and its Shape-specific forms are removed: to reshape an Area, `select` it, adjust the selection in WorldEdit, then `move` it. Two commands stay even though they break this rule. `delete` pairs with `create`, and `list` can offer it as a button on each Area; the Edit dialog gains a Delete button too. `rename` stays out of the Edit dialog altogether: renaming is rare and shouldn't clutter it, and a command tab-completes the Area to rename and answers a bad or taken new name with an error straight away in chat, where a dialog input has no suggestions.

## Considered Options

- **Keep `size`:** four forms, one per Shape, that duplicate what WorldEdit already does better.
- **Keep `set` with a confirmation on move:** the confirmation existed only because the command couldn't tell a move from a typo.
- **Warp point at the centre of the selection:** the centre can be underground, in mid-air, or outside a concave polygon, so the Warp point stays where the staff member stands.

## Consequences

The console can no longer change Enabled, play a Welcome or read an Area's Shape. Staff on clients older than 1.21.6 (see ADR 0002) also lose Enabled, which they could previously change through `enable` and `disable`.
