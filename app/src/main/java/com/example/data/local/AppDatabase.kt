package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class, TagEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isProtected INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN protectedPassword TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_updatedAt ON notes (updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_isTrashed ON notes (isTrashed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_isArchived ON notes (isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_isPinned ON notes (isPinned)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_reminderAt ON notes (reminderAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_trashedAt ON notes (trashedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_type ON notes (type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_colorHex ON notes (colorHex)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags (name)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mynotes_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
