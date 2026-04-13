"""Create Kobo shelves from Manga/ and Books/ subfolders."""

import argparse
import re
import shutil
import sqlite3
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

ROOT_FOLDERS = ("Manga", "Books")


def now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def backup_database(db_path: Path) -> Path:
    """Copy the database to a timestamped backup file."""
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup = db_path.with_suffix(f".{ts}.bak")
    shutil.copy2(db_path, backup)
    return backup


def extract_subfolder(content_id: str) -> str | None:
    """Return the immediate subfolder name under Manga/ or Books/, or None."""
    for folder in ROOT_FOLDERS:
        match = re.search(rf"/{folder}/([^/]+)/", content_id)
        if match:
            return match.group(1)
    return None


def build_shelf_map(conn: sqlite3.Connection) -> dict[str, list[str]]:
    """Query books and group ContentIDs by subfolder name."""
    like_clauses = " OR ".join(f"ContentID LIKE '%/{f}/%/%'" for f in ROOT_FOLDERS)
    rows = conn.execute(
        f"SELECT ContentID FROM content WHERE ContentType = '6' AND ({like_clauses})"  # noqa: S608
    ).fetchall()

    shelf_map: dict[str, list[str]] = defaultdict(list)
    for (content_id,) in rows:
        subfolder = extract_subfolder(content_id)
        if subfolder:
            shelf_map[subfolder].append(content_id)
    return dict(shelf_map)


def create_shelves(conn: sqlite3.Connection, shelf_map: dict[str, list[str]]) -> None:
    """Insert shelves and shelf-content links."""
    ts = now_iso()

    for shelf_name, content_ids in sorted(shelf_map.items()):
        conn.execute(
            """
            INSERT OR IGNORE INTO Shelf
                (CreationDate, Id, InternalName, LastModified, Name, Type,
                 _IsDeleted, _IsVisible, _IsSynced, _SyncTime, LastAccessed)
            VALUES (?, ?, ?, ?, ?, NULL, 'false', 'true', 'false', NULL, ?)
            """,
            (ts, shelf_name, shelf_name, ts, shelf_name, ts),
        )

        for cid in content_ids:
            conn.execute(
                """
                INSERT OR IGNORE INTO ShelfContent
                    (ShelfName, ContentId, DateModified, _IsDeleted, _IsSynced)
                VALUES (?, ?, ?, 'false', 'false')
                """,
                (shelf_name, cid, ts),
            )

        print(f"  {shelf_name}: {len(content_ids)} books")


def main() -> None:
    parser = argparse.ArgumentParser(description="Create Kobo shelves from folder structure.")
    parser.add_argument("--db", default="KoboReader.sqlite", help="Path to KoboReader.sqlite")
    args = parser.parse_args()

    db_path = Path(args.db)
    if not db_path.exists():
        parser.error(f"Database not found: {db_path}")

    backup = backup_database(db_path)
    print(f"Backup: {backup}")

    conn = sqlite3.connect(db_path)
    try:
        shelf_map = build_shelf_map(conn)
        if not shelf_map:
            print("No subfolders found under Manga/ or Books/.")
            return

        print(f"Found {len(shelf_map)} subfolders:")
        create_shelves(conn, shelf_map)
        conn.commit()
        print("Done.")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
