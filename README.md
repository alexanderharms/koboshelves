# KoboShelves

Personal tool that auto-creates shelves on a Kobo e-reader from its folder structure. It reads `KoboReader.sqlite`, finds books under `Manga/` and `Books/` subfolders, and inserts matching shelf entries so the Kobo UI groups them automatically.

**This is developed for personal use. No guarantees, no support.**

## How it works

1. Pick a `KoboReader.sqlite` file
2. A timestamped backup is created in the app's internal storage
3. Shelves are added to the `Shelf` and `ShelfContent` tables (idempotent — safe to run repeatedly)
4. The modified database is written back to the original file

## Safety

- **Copy the database file to a folder on your phone before using the app.** Do not operate directly on the file while the Kobo is connected.
- Backups are stored in the app's data directory and are **not automatically removed**. Clean them up periodically to avoid wasting storage.

## Building

Requires JDK 17 and the Android SDK.

```bash
./gradlew assembleDebug
```

The APK is output to `app/build/outputs/apk/debug/app-debug.apk`.
