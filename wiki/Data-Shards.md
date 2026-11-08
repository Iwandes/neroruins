# Data shards and the archive

The story of NeroRuins is told in data logs: nine chapters, six logs each, placed through the ruins in
story order, with each chapter's final entries in its deepest rooms.

## Data shards

A data shard holds one log. Use it to read the title, speaker and text and add the log to your personal
archive. In survival a newly read shard is consumed unless the server sets `archive.keepShards=true`;
creative players keep it. A shard you have already read is kept, and a shard without a valid log reports
that it is unreadable. Shards turn up in ruin chests.

## Log terminals

Log terminals stand in ruins and show a rotating shard above them. Use one to read its log without
consuming anything, and choose **Archive this log** to save it. The terminal shows when you already have
that log.

## The Codex

- `/neroruins codex` opens your archive as a book: chapter progress, your saved logs and completed chapters.
- `/neroruins codex <log ID>` rereads a saved log in chat.
- When all six logs of a chapter are archived, `/neroruins codex chapter <chapter>` shows its entry.

The book needs NeroRuins on your client; otherwise the command explains why, and the export below still
works.

## Privacy

The archive stores your UUID, the log ID and when you first read it — no name or position. Export it with
`/neroruins privacy export`; erase it with Core's `/neroland data eraseme`. See [Privacy](Privacy.md).

[Home](Home.md)
