package com.niranjan.englisharticle.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MeaningEntity::class,
        RecentArticleEntity::class,
        SavedWordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EnglishArticleDatabase : RoomDatabase() {
    abstract fun meaningDao(): MeaningDao

    abstract fun recentArticleDao(): RecentArticleDao

    abstract fun savedWordDao(): SavedWordDao

    companion object {
        @Volatile
        private var instance: EnglishArticleDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_words (
                        savedKey TEXT NOT NULL PRIMARY KEY,
                        word TEXT NOT NULL,
                        sentence TEXT NOT NULL,
                        lookupMode TEXT NOT NULL,
                        articleTitle TEXT NOT NULL,
                        meaningKannada TEXT NOT NULL,
                        simpleEnglish TEXT NOT NULL,
                        partOfSpeech TEXT NOT NULL,
                        explanationKannada TEXT NOT NULL,
                        exampleEnglish TEXT NOT NULL,
                        exampleKannada TEXT NOT NULL,
                        savedAtMillis INTEGER NOT NULL,
                        practiceAttempts INTEGER NOT NULL,
                        correctAttempts INTEGER NOT NULL,
                        lastPracticedAtMillis INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recent_articles ADD COLUMN idiomaticPhrasesJson TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        fun getInstance(context: Context): EnglishArticleDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EnglishArticleDatabase::class.java,
                    "english_article.db"
                )
                    .addMigrations(migration1To2, migration2To3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
