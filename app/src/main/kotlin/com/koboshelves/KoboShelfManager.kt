package com.koboshelves

import android.database.sqlite.SQLiteDatabase
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class ShelfResult(
    val shelfName: String,
    val bookCount: Int,
)

object KoboShelfManager {

    private val ROOT_FOLDERS = listOf("Manga", "Books")

    private val TIMESTAMP_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    fun nowIso(): String = TIMESTAMP_FORMAT.format(Instant.now())

    fun extractSubfolder(contentId: String): String? {
        for (folder in ROOT_FOLDERS) {
            val match = Regex("/$folder/([^/]+)/").find(contentId)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    fun buildShelfMap(db: SQLiteDatabase): Map<String, List<String>> {
        val likeClauses = ROOT_FOLDERS.joinToString(" OR ") { "ContentID LIKE '%/$it/%/%'" }
        val cursor = db.rawQuery(
            "SELECT ContentID FROM content WHERE ContentType = '6' AND ($likeClauses)",
            null,
        )

        val shelfMap = mutableMapOf<String, MutableList<String>>()
        cursor.use {
            while (it.moveToNext()) {
                val contentId = it.getString(0)
                val subfolder = extractSubfolder(contentId)
                if (subfolder != null) {
                    shelfMap.getOrPut(subfolder) { mutableListOf() }.add(contentId)
                }
            }
        }
        return shelfMap
    }

    fun createShelves(db: SQLiteDatabase, shelfMap: Map<String, List<String>>): List<ShelfResult> {
        val ts = nowIso()
        val results = mutableListOf<ShelfResult>()

        for ((shelfName, contentIds) in shelfMap.toSortedMap()) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO Shelf
                    (CreationDate, Id, InternalName, LastModified, Name, Type,
                     _IsDeleted, _IsVisible, _IsSynced, _SyncTime, LastAccessed)
                VALUES (?, ?, ?, ?, ?, NULL, 'false', 'true', 'false', NULL, ?)
                """.trimIndent(),
                arrayOf(ts, shelfName, shelfName, ts, shelfName, ts),
            )

            for (cid in contentIds) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO ShelfContent
                        (ShelfName, ContentId, DateModified, _IsDeleted, _IsSynced)
                    VALUES (?, ?, ?, 'false', 'false')
                    """.trimIndent(),
                    arrayOf(shelfName, cid, ts),
                )
            }

            results.add(ShelfResult(shelfName, contentIds.size))
        }

        return results
    }
}
