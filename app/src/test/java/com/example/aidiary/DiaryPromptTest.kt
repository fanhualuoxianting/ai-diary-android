package com.example.aidiary

import com.example.aidiary.data.DiaryAnswer
import com.example.aidiary.data.DiaryDraft
import com.example.aidiary.data.MealRecord
import com.example.aidiary.data.QuestionIds
import com.example.aidiary.llm.buildChineseDiaryPrompt
import com.example.aidiary.llm.toManualFallback
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryPromptTest {
    @Test
    fun promptKeepsAnsweredMealsAndDoesNotInventSkippedMeals() {
        val draft = DiaryDraft(
            date = "2026-05-27",
            answers = listOf(
                DiaryAnswer("2026-05-27", QuestionIds.MainEvent, "写了安卓日记应用", false),
                DiaryAnswer("2026-05-27", QuestionIds.Mood, "踏实", false),
            ),
            mealRecord = MealRecord(
                date = "2026-05-27",
                breakfast = "豆浆和包子",
                lunch = "",
                dinner = "",
            ),
        )

        val prompt = buildChineseDiaryPrompt(draft, "标准")

        assertTrue(prompt.contains("早餐：豆浆和包子"))
        assertFalse(prompt.contains("午餐："))
        assertFalse(prompt.contains("晚餐："))
        assertTrue(prompt.contains("不要虚构"))
    }

    @Test
    fun fallbackDraftUsesOnlyProvidedFacts() {
        val draft = DiaryDraft(
            date = "2026-05-27",
            answers = listOf(
                DiaryAnswer("2026-05-27", QuestionIds.MainEvent, "整理房间", false),
                DiaryAnswer("2026-05-27", QuestionIds.Scene, "", true),
            ),
            mealRecord = MealRecord("2026-05-27", "鸡蛋", "", "面条"),
        )

        val fallback = draft.toManualFallback()

        assertTrue(fallback.contains("整理房间"))
        assertTrue(fallback.contains("早餐吃了鸡蛋"))
        assertTrue(fallback.contains("晚餐吃了面条"))
        assertFalse(fallback.contains("午餐吃了"))
    }
}
