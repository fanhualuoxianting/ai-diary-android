package com.example.aidiary.llm

import com.example.aidiary.data.DiaryDraft
import com.example.aidiary.data.QuestionIds
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface LocalLlmEngine {
    suspend fun isAvailable(): Boolean
    suspend fun generateDiary(draft: DiaryDraft, writingStyle: String): GenerateResult
}

sealed interface GenerateResult {
    data class Success(val text: String) : GenerateResult
    data class Unavailable(val fallbackText: String, val reason: String) : GenerateResult
}

class GemmaLocalEngine(
    private val modelPathProvider: suspend () -> String,
) : LocalLlmEngine {
    override suspend fun isAvailable(): Boolean {
        val path = modelPathProvider()
        return path.isNotBlank() && File(path).canRead()
    }

    override suspend fun generateDiary(draft: DiaryDraft, writingStyle: String): GenerateResult {
        val modelPath = modelPathProvider()
        if (modelPath.isBlank()) {
            return GenerateResult.Unavailable(
                fallbackText = draft.toManualFallback(),
                reason = "本地 Gemma 模型尚未配置，已生成可手动编辑的草稿。",
            )
        }
        if (!File(modelPath).canRead()) {
            return GenerateResult.Unavailable(
                fallbackText = draft.toManualFallback(),
                reason = "模型文件不可读。请在设置里导入 .litertlm 文件，或确认路径属于本应用可访问目录。",
            )
        }

        val prompt = buildChineseDiaryPrompt(draft, writingStyle)
        return runCatching {
            withContext(Dispatchers.Default) {
                Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        maxNumTokens = 1024,
                    ),
                ).use { engine ->
                    engine.initialize()
                    engine.createConversation(
                        ConversationConfig(
                            samplerConfig = SamplerConfig(
                                topK = 40,
                                topP = 0.95,
                                temperature = 0.7,
                            ),
                        ),
                    ).use { conversation ->
                        conversation.sendMessage(prompt).toPlainText().ifBlank {
                            draft.toManualFallback()
                        }
                    }
                }
            }
        }.fold(
            onSuccess = { GenerateResult.Success(it) },
            onFailure = {
                GenerateResult.Unavailable(
                    fallbackText = draft.toManualFallback(),
                    reason = "本地模型推理失败：${it.message ?: it::class.java.simpleName}",
                )
            },
        )
    }
}

private fun com.google.ai.edge.litertlm.Message.toPlainText(): String =
    contents.contents.joinToString(separator = "") { content ->
        when (content) {
            is Content.Text -> content.text
            else -> ""
        }
    }.trim()

fun buildChineseDiaryPrompt(draft: DiaryDraft, writingStyle: String): String {
    val answerLines = draft.answers
        .filterNot { it.skipped || it.answer.isBlank() }
        .joinToString("\n") { answer ->
            val label = when (answer.questionId) {
                QuestionIds.MainEvent -> "今天最主要做了什么"
                QuestionIds.Scene -> "值得记录的具体场景"
                QuestionIds.Mood -> "今天心情"
                QuestionIds.ThanksReflectionTomorrow -> "感谢、反思或明天继续的事"
                else -> answer.questionId
            }
            "- $label：${answer.answer}"
        }

    val mealLines = listOf(
        "早餐" to draft.mealRecord.breakfast,
        "午餐" to draft.mealRecord.lunch,
        "晚餐" to draft.mealRecord.dinner,
    ).filter { it.second.isNotBlank() }
        .joinToString("\n") { "- ${it.first}：${it.second}" }

    return """
        你是一个离线运行的中文日记助手。请根据用户提供的信息，写一篇第一人称中文日记。

        严格要求：
        1. 只能根据用户填写的事实写，不要虚构事件、人物、地点、食物或情绪。
        2. 如果某个餐次为空，不要编造吃了什么，也不要强调“没填写”。
        3. 语气自然、克制、像真实个人日记，不要写成总结报告。
        4. 风格：$writingStyle。
        5. 长度约 300 到 600 字。

        日期：${draft.date}

        用户回答：
        ${answerLines.ifBlank { "无" }}

        三餐记录：
        ${mealLines.ifBlank { "无" }}
    """.trimIndent()
}

fun DiaryDraft.toManualFallback(): String {
    val parts = mutableListOf<String>()
    answers.firstOrNull { it.questionId == QuestionIds.MainEvent && it.answer.isNotBlank() }
        ?.let { parts += "今天主要做了${it.answer}。" }
    answers.firstOrNull { it.questionId == QuestionIds.Scene && it.answer.isNotBlank() }
        ?.let { parts += "有一个值得记录的场景：${it.answer}。" }
    answers.firstOrNull { it.questionId == QuestionIds.Mood && it.answer.isNotBlank() }
        ?.let { parts += "今天的心情是${it.answer}。" }

    val meals = listOf(
        "早餐吃了${mealRecord.breakfast}" to mealRecord.breakfast,
        "午餐吃了${mealRecord.lunch}" to mealRecord.lunch,
        "晚餐吃了${mealRecord.dinner}" to mealRecord.dinner,
    ).filter { it.second.isNotBlank() }.joinToString("，") { it.first }
    if (meals.isNotBlank()) parts += "$meals。"

    answers.firstOrNull {
        it.questionId == QuestionIds.ThanksReflectionTomorrow && it.answer.isNotBlank()
    }?.let { parts += it.answer }

    return parts.joinToString("\n\n").ifBlank { "今天先写下这些。可以直接在这里补充正文。" }
}
