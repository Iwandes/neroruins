# Data shards and the archive

Data shards contain a single log. Use one to read its title, speaker and text
and add the log to your personal archive. A newly read shard is consumed in
survival unless `archive.keepShards=true`; creative players retain it. A
duplicate shard is retained, and a shard without a valid log reports that it is
unreadable. Prototype bunker chests contain Earth shards, but the bunker can
only be placed with commands and does not yet generate naturally.

Use `/neroruins codex` to open your archive as a book. It shows chapter progress,
your saved logs and completed chapter entries. You can also use
`/neroruins codex <log ID>` to reread a log in chat. After all six logs of a
chapter have been read, `/neroruins codex chapter <chapter>` shows its entry in
chat.
The book screen requires NeroRuins on your client; otherwise the command gives
an explanation and the server-side export remains available.

The archive stores the reader's UUID, log ID and first-read time in the world
save and its recovery backup. It does not store a name or location.
`/neroruins privacy export` writes your archive, read times and any vault-claim records to the world
folder; the server operator can access the file. `/neroland data eraseme`
requests erasure across participating Neroland mods, including the archive and
its export file. See [Privacy](../PRIVACY.md) for retention details.

## Log terminals

Log terminals project a rotating shard and present a log without consuming an item. Choose **Archive this log** to save it to your own archive; the terminal shows when that log is already archived. A terminal without a valid log shows an unreadable state. The current bunker prototype includes a terminal in its archive-room variant; it is command-placeable and does not yet generate naturally.

[Home](Home.md)

