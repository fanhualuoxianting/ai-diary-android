package com.example.aidiary.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DiaryRepository(private val dao: DiaryDao) {
    fun observeEntries(): Flow<List<DiaryEntry>> = dao.observeEntries()

    fun observeEntry(date: String): Flow<DiaryEntry?> = dao.observeEntry(date)

    suspend fun loadDraft(date: String = LocalDate.now().toString()): DiaryDraft {
        val answers = dao.getAnswers(date)
        val meals = dao.getMealRecord(date) ?: MealRecord(date, "", "", "")
        return DiaryDraft(date = date, answers = answers, mealRecord = meals)
    }

    suspend fun saveDraft(draft: DiaryDraft) {
        dao.upsertAnswers(draft.answers)
        dao.upsertMealRecord(draft.mealRecord)
    }

    suspend fun saveEntry(date: String, aiDraft: String, finalText: String, mood: String) {
        val now = System.currentTimeMillis()
        val existing = dao.getEntry(date)
        dao.upsertEntry(
            DiaryEntry(
                date = date,
                aiDraft = aiDraft,
                finalText = finalText,
                mood = mood,
                tags = existing?.tags.orEmpty(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    suspend fun deleteAllLocalData() {
        dao.deleteEntries()
        dao.deleteAnswers()
        dao.deleteMeals()
    }
}
