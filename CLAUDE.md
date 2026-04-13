# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Creates shelves on a Kobo e-reader by writing directly to its SQLite database (`KoboReader.sqlite`). Scans the `content` table for books under `Manga/` and `Books/` subfolders, then inserts corresponding `Shelf` and `ShelfContent` rows so the Kobo UI groups books by subfolder.

Two implementations exist:
- **Python CLI** (`kobo_shelves.py`) — original script, run with `python kobo_shelves.py --db <path>`
- **Android app** (`app/`) — Kotlin + Jetpack Compose, picks file via SAF, same shelf logic

## Commands

```bash
# Python
ruff check . --fix   # lint
ruff format .        # format
python -m pytest     # tests (when they exist)

# Android
./gradlew assembleDebug   # build APK (requires openjdk-17)
```

## Architecture

### Python (`kobo_shelves.py`)

Single file: `build_shelf_map()` → `create_shelves()` → commit. `backup_database()` copies the DB before writes. `ROOT_FOLDERS` controls which top-level folders are scanned.

### Android (`app/`)

Single-activity Compose app, no Room (direct `SQLiteDatabase` access).

- `KoboShelfManager.kt` — pure port of Python logic: `extractSubfolder()`, `buildShelfMap()`, `createShelves()`
- `MainViewModel.kt` — orchestrates SAF file copy-in → backup to `filesDir/backups/` → process → write-back via `contentResolver.openOutputStream(uri, "wt")`
- `ui/MainScreen.kt` — single Compose screen with idle/processing/done/error states
- `MainActivity.kt` — registers `ActivityResultContracts.OpenDocument()`, wires to ViewModel

The database schema is Kobo's own (`Shelf`, `ShelfContent`, `content` tables). All inserts use `INSERT OR IGNORE` for idempotency.
