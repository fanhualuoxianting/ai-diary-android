package com.example.aidiary.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aidiary.data.DiaryAnswer
import com.example.aidiary.data.DiaryDraft
import com.example.aidiary.data.DiaryEntry
import com.example.aidiary.data.DiaryRepository
import com.example.aidiary.data.MealRecord
import com.example.aidiary.data.SettingsRepository
import com.example.aidiary.data.UserSettings
import com.example.aidiary.data.dailyQuestions
import com.example.aidiary.llm.GenerateResult
import com.example.aidiary.llm.LocalLlmEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class DiaryUiState(
    val date: String = LocalDate.now().toString(),
    val answers: Map<String, String> = emptyMap(),
    val skipped: Set<String> = emptySet(),
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val generatedText: String = "",
    val currentEntry: DiaryEntry? = null,
    val history: List<DiaryEntry> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val modelAvailable: Boolean = false,
    val status: String = "填写几句，今晚就能留下今天。",
    val isGenerating: Boolean = false,
    val isImportingModel: Boolean = false,
)

class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val settingsRepository: SettingsRepository,
    private val llmEngine: LocalLlmEngine,
) : ViewModel() {
    private val localState = MutableStateFlow(DiaryUiState())
    private val history = diaryRepository.observeEntries()
    private val currentEntry = diaryRepository.observeEntry(LocalDate.now().toString())

    val uiState: StateFlow<DiaryUiState> = combine(
        localState,
        history,
        currentEntry,
        settingsRepository.settings,
    ) { state, entries, entry, settings ->
        state.copy(history = entries, currentEntry = entry, settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    init {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val draft = diaryRepository.loadDraft(today)
            val entry = diaryRepository.observeEntry(today)
            localState.value = localState.value.copy(
                date = today,
                answers = draft.answers.associate { it.questionId to it.answer },
                skipped = draft.answers.filter { it.skipped }.map { it.questionId }.toSet(),
                breakfast = draft.mealRecord.breakfast,
                lunch = draft.mealRecord.lunch,
                dinner = draft.mealRecord.dinner,
                modelAvailable = llmEngine.isAvailable(),
            )
            entry.collect { saved ->
                if (saved != null && localState.value.generatedText.isBlank()) {
                    localState.value = localState.value.copy(generatedText = saved.finalText)
                }
            }
        }
    }

    fun updateAnswer(questionId: String, value: String) {
        localState.value = localState.value.copy(
            answers = localState.value.answers + (questionId to value),
            skipped = localState.value.skipped - questionId,
        )
    }

    fun skipQuestion(questionId: String) {
        localState.value = localState.value.copy(
            answers = localState.value.answers + (questionId to ""),
            skipped = localState.value.skipped + questionId,
        )
    }

    fun updateMeal(kind: MealKind, value: String) {
        localState.value = when (kind) {
            MealKind.Breakfast -> localState.value.copy(breakfast = value)
            MealKind.Lunch -> localState.value.copy(lunch = value)
            MealKind.Dinner -> localState.value.copy(dinner = value)
        }
    }

    fun updateGeneratedText(value: String) {
        localState.value = localState.value.copy(generatedText = value)
    }

    fun generateDiary() {
        viewModelScope.launch {
            localState.value = localState.value.copy(isGenerating = true, status = "正在整理今天的记录……")
            val draft = currentDraft()
            diaryRepository.saveDraft(draft)
            when (val result = llmEngine.generateDiary(draft, uiState.value.settings.writingStyle)) {
                is GenerateResult.Success -> localState.value = localState.value.copy(
                    generatedText = result.text,
                    status = "已生成日记，可以继续编辑后保存。",
                    isGenerating = false,
                    modelAvailable = true,
                )

                is GenerateResult.Unavailable -> localState.value = localState.value.copy(
                    generatedText = result.fallbackText,
                    status = result.reason,
                    isGenerating = false,
                    modelAvailable = false,
                )
            }
        }
    }

    fun saveDiary() {
        viewModelScope.launch {
            val draft = currentDraft()
            diaryRepository.saveDraft(draft)
            diaryRepository.saveEntry(
                date = draft.date,
                aiDraft = localState.value.generatedText,
                finalText = localState.value.generatedText,
                mood = draft.mood,
            )
            localState.value = localState.value.copy(status = "已保存在本机。")
        }
    }

    fun updateModelPath(value: String) {
        viewModelScope.launch {
            settingsRepository.updateModelPath(value)
            localState.value = localState.value.copy(modelAvailable = llmEngine.isAvailable())
        }
    }

    fun importModel(context: Context, uri: Uri) {
        viewModelScope.launch {
            localState.value = localState.value.copy(
                isImportingModel = true,
                status = "正在导入模型文件，请保持应用打开……",
            )
            val result = runCatching {
                val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
                val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                } ?: "gemma-model.litertlm"
                val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val target = File(modelsDir, safeName)
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "无法打开模型文件" }
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                target.absolutePath
            }
            result.fold(
                onSuccess = { path ->
                    settingsRepository.updateModelPath(path)
                    localState.value = localState.value.copy(
                        isImportingModel = false,
                        modelAvailable = llmEngine.isAvailable(),
                        status = "模型已导入本机，可用于离线生成。",
                    )
                },
                onFailure = {
                    localState.value = localState.value.copy(
                        isImportingModel = false,
                        status = "模型导入失败：${it.message ?: it::class.java.simpleName}",
                    )
                },
            )
        }
    }

    fun updateWritingStyle(value: String) {
        viewModelScope.launch { settingsRepository.updateWritingStyle(value) }
    }

    fun updateReminderTime(value: String) {
        viewModelScope.launch { settingsRepository.updateReminderTime(value) }
    }

    fun updateSelectedModelId(value: String) {
        viewModelScope.launch { settingsRepository.updateSelectedModelId(value) }
    }

    fun deleteAllLocalData() {
        viewModelScope.launch {
            diaryRepository.deleteAllLocalData()
            settingsRepository.clear()
            localState.value = DiaryUiState(status = "本地数据已清空。")
        }
    }

    private fun currentDraft(): DiaryDraft {
        val state = localState.value
        val answers = dailyQuestions.map { question ->
            DiaryAnswer(
                date = state.date,
                questionId = question.id,
                answer = state.answers[question.id].orEmpty(),
                skipped = question.id in state.skipped,
            )
        }
        return DiaryDraft(
            date = state.date,
            answers = answers,
            mealRecord = MealRecord(
                date = state.date,
                breakfast = state.breakfast,
                lunch = state.lunch,
                dinner = state.dinner,
            ),
        )
    }
}

enum class MealKind { Breakfast, Lunch, Dinner }

class DiaryViewModelFactory(
    private val diaryRepository: DiaryRepository,
    private val settingsRepository: SettingsRepository,
    private val llmEngine: LocalLlmEngine,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiaryViewModel(diaryRepository, settingsRepository, llmEngine) as T
    }
}
