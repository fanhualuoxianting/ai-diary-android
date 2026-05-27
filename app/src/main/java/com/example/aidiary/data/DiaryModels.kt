package com.example.aidiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey val date: String,
    val aiDraft: String,
    val finalText: String,
    val mood: String,
    val tags: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "diary_answers", primaryKeys = ["date", "questionId"])
data class DiaryAnswer(
    val date: String,
    val questionId: String,
    val answer: String,
    val skipped: Boolean,
)

@Entity(tableName = "meal_records")
data class MealRecord(
    @PrimaryKey val date: String,
    val breakfast: String,
    val lunch: String,
    val dinner: String,
)

data class DiaryQuestion(
    val id: String,
    val title: String,
    val placeholder: String,
)

data class DiaryDraft(
    val date: String,
    val answers: List<DiaryAnswer>,
    val mealRecord: MealRecord,
) {
    val mood: String
        get() = answers.firstOrNull { it.questionId == QuestionIds.Mood }?.answer.orEmpty()
}

object QuestionIds {
    const val MainEvent = "main_event"
    const val Scene = "scene"
    const val Mood = "mood"
    const val ThanksReflectionTomorrow = "thanks_reflection_tomorrow"
}

val dailyQuestions = listOf(
    DiaryQuestion(
        id = QuestionIds.MainEvent,
        title = "今天最主要做了什么？",
        placeholder = "例如：完成了一个项目、见了朋友、在家休息……",
    ),
    DiaryQuestion(
        id = QuestionIds.Scene,
        title = "有没有一个具体场景值得记录？",
        placeholder = "例如：路上的一段对话、一个小变化、某个瞬间……",
    ),
    DiaryQuestion(
        id = QuestionIds.Mood,
        title = "今天心情怎么样？",
        placeholder = "例如：平静、开心、有点累、焦虑但还好……",
    ),
    DiaryQuestion(
        id = QuestionIds.ThanksReflectionTomorrow,
        title = "有没有想感谢、反思或明天继续的事？",
        placeholder = "写一句也可以，留空也没关系。",
    ),
)
