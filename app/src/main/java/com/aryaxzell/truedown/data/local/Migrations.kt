package com.aryaxzell.truedown.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: Tambah index pada posts.createdAt untuk mempercepat query
 * ORDER BY createdAt DESC (PERF-06). Tidak ada perubahan kolom/tabel.
 *
 * Nama index WAJIB "index_posts_createdAt" — ini format default yang
 * digenerate Room dari @Entity(indices = [Index(value = ["createdAt"])])
 * tanpa custom name. Room memvalidasi schema live terhadap schema yang
 * diharapkan dari entity setelah migration jalan; kalau nama index tidak
 * cocok, Room akan anggap migration gagal walau SQL-nya sukses.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_createdAt` ON `posts` (`createdAt`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `update_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `versionName` TEXT NOT NULL,
                `versionCode` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `notes` TEXT NOT NULL
            )
        """.trimIndent())
    }
}
