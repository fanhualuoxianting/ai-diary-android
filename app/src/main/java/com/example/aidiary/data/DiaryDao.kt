package com.example.aidiary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun observeEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    fun observeEntry(date: String): Flow<DiaryEntry?>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    suspend fun getEntry(date: String): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_answers WHERE date = :date")
    suspend fun getAnswers(date: String): List<DiaryAnswer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnswers(answers: List<DiaryAnswer>)

    @Query("SELECT * FROM meal_records WHERE date = :date LIMIT 1")
    suspend fun getMealRecord(date: String): MealRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealRecord(record: MealRecord)

    @Query("DELETE FROM diary_entries")
    suspend fun deleteEntries()

    @Query("DELETE FROM diary_answers")
    suspend fun deleteAnswers()

    @Query("DELETE FROM meal_records")
    suspend fun deleteMeals()
}
