# Area editing uses Dialogs only

Paper 26.2 deprecates the Conversation API for removal. Editing an Area (Title, Subtitle, Show title, Boss bar, Description, Enabled) and confirming destructive actions (delete, move) therefore move to Paper Dialogs. There is no fallback UI: the chat conversations, the chat line-editor for Descriptions, and the writable-book `getbook`/`save` route are all removed. Only staff edit Areas, so we accept that staff must use a client on 1.21.6 or newer. Keeping a second UI for older or ViaVersion clients isn't worth maintaining.

## Consequences

A staff member on an older client can still run every command, but cannot edit an Area's text or confirm a delete or move.
