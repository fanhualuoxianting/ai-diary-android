package com.example.aidiary

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.example.aidiary.data.DiaryDatabase
import com.example.aidiary.data.DiaryRepository
import com.example.aidiary.data.SettingsRepository
import com.example.aidiary.llm.GemmaLocalEngine
import com.example.aidiary.reminder.DiaryReminderWorker
import com.example.aidiary.ui.DiaryApp
import com.example.aidiary.ui.DiaryViewModel
import com.example.aidiary.ui.DiaryViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        scheduleReminder()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = DiaryDatabase.get(this)
        val diaryRepository = DiaryRepository(database.diaryDao())
        val settingsRepository = SettingsRepository(applicationContext)
        val engine = GemmaLocalEngine { settingsRepository.settingsSnapshot().modelPath }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1F6FEB),
                    secondary = Color(0xFF2EA043),
                    tertiary = Color(0xFFD29922),
                    background = Color(0xFFF8FAFC),
                    surface = Color.White,
                ),
            ) {
                val viewModel: DiaryViewModel = viewModel(
                    factory = DiaryViewModelFactory(diaryRepository, settingsRepository, engine),
                )
                DiaryApp(viewModel)
                LaunchedEffect(Unit) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun scheduleReminder() {
        lifecycleScope.launch {
            val settingsRepository = SettingsRepository(applicationContext)
            DiaryReminderWorker.schedule(
                context = this@MainActivity,
                reminderTime = settingsRepository.settingsSnapshot().reminderTime,
            )
        }
    }
}
