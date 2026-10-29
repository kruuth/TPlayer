package com.iks.tplayer.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Non-destructive schema upgrades so library / root folder survive app updates.
 *
 * History:
 *  1 → base: tracks, album_arts, source_folders
 *  2 → source_folders.isCloud
 *  3 → playlists + playlist_entries
 */
object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Cloud sources flag (Settings → Cloud)
            try {
                db.execSQL(
                    "ALTER TABLE source_folders ADD COLUMN isCloud INTEGER NOT NULL DEFAULT 0"
                )
            } catch (_: Exception) {
                // Column may already exist on some devices
            }
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS playlists (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS playlist_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    playlistId INTEGER NOT NULL,
                    trackId INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_playlist_entries_playlistId ON playlist_entries(playlistId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_playlist_entries_trackId ON playlist_entries(trackId)"
            )
        }
    }

    /** Safety net: ensure v3 objects exist when moving past incomplete upgrades. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema change — version bump only so installs that used destructive
            // v3 can move forward under migration control.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS playlists (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS playlist_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    playlistId INTEGER NOT NULL,
                    trackId INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_playlist_entries_playlistId ON playlist_entries(playlistId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_playlist_entries_trackId ON playlist_entries(trackId)"
            )
            // Ensure isCloud exists for DBs that skipped 1→2 somehow
            try {
                db.execSQL(
                    "ALTER TABLE source_folders ADD COLUMN isCloud INTEGER NOT NULL DEFAULT 0"
                )
            } catch (_: Exception) {
            }
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
