package com.koboshelves

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class UiState(
    val status: Status = Status.Idle,
    val shelves: List<ShelfResult> = emptyList(),
    val totalBooks: Int = 0,
    val backupPath: String? = null,
    val error: String? = null,
)

enum class Status { Idle, Processing, Done, Error }

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun processDatabase(uri: Uri, context: Context) {
        _uiState.value = UiState(status = Status.Processing)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = doProcess(uri, context)
                _uiState.value = result
            } catch (e: Exception) {
                _uiState.value = UiState(
                    status = Status.Error,
                    error = e.message ?: "Unknown error",
                )
            }
        }
    }

    private fun doProcess(uri: Uri, context: Context): UiState {
        // Copy SAF file to cache
        val cachedDb = File(context.cacheDir, "KoboReader.sqlite")
        context.contentResolver.openInputStream(uri)?.use { input ->
            cachedDb.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Could not read the selected file")

        // Create backup before modifying
        val backupDir = File(context.filesDir, "backups")
        backupDir.mkdirs()
        val ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val backupFile = File(backupDir, "KoboReader_$ts.bak")
        cachedDb.copyTo(backupFile, overwrite = true)

        // Open cached copy and process
        val db = SQLiteDatabase.openDatabase(
            cachedDb.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )

        val shelves: List<ShelfResult>
        try {
            val shelfMap = KoboShelfManager.buildShelfMap(db)
            if (shelfMap.isEmpty()) {
                return UiState(
                    status = Status.Done,
                    shelves = emptyList(),
                    totalBooks = 0,
                    backupPath = backupFile.absolutePath,
                )
            }
            shelves = KoboShelfManager.createShelves(db, shelfMap)
            db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
        } finally {
            db.close()
        }

        // Write modified file back to original URI
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            cachedDb.inputStream().use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("Could not write back to the original file")

        return UiState(
            status = Status.Done,
            shelves = shelves,
            totalBooks = shelves.sumOf { it.bookCount },
            backupPath = backupFile.absolutePath,
        )
    }

    fun reset() {
        _uiState.value = UiState()
    }
}
