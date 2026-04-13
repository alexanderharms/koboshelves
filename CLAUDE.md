# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Single-script CLI tool that creates shelves on a Kobo e-reader by writing directly to its SQLite database (`KoboReader.sqlite`). It scans the `content` table for books under `Manga/` and `Books/` subfolders, then inserts corresponding `Shelf` and `ShelfContent` rows so the Kobo UI groups books by subfolder.

## Usage

```bash
python kobo_shelves.py --db /path/to/KoboReader.sqlite
```

## Commands

```bash
ruff check . --fix   # lint
ruff format .        # format
python -m pytest     # tests (when they exist)
```

## Architecture

Everything lives in `kobo_shelves.py`:

- `build_shelf_map()` — queries `content` table, groups ContentIDs by subfolder name
- `create_shelves()` — inserts into `Shelf` and `ShelfContent` tables (uses `INSERT OR IGNORE` for idempotency)
- `backup_database()` — copies the DB to a timestamped `.bak` before any writes
- `main()` — CLI entry point via `argparse`

The database schema is Kobo's own (`Shelf`, `ShelfContent`, `content` tables). `ROOT_FOLDERS` constant controls which top-level folders are scanned.
