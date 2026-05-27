package com.example.aidiary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DiaryEntry::class, DiaryAnswer::class, MealRecord::class],
    version = 1,
    exportSchema = false,
)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var instance: DiaryDatabase? = null

        fun get(context: Context): DiaryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "offline-diary.db",
                ).build().also { instance = it }
            }
    }
}
