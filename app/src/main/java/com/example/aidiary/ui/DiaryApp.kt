package com.example.aidiary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidiary.data.DiaryEntry
import com.example.aidiary.data.dailyQuestions
import com.example.aidiary.data.modelCatalog

private enum class AppTab(val title: String) {
    Today("今天"),
    History("历史"),
    Settings("设置"),
}

@Composable
fun DiaryApp(viewModel: DiaryViewModel) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(AppTab.Today) }
    val context = LocalContext.current
    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importModel(context, uri)
    }

    Scaffold(
        containerColor = Color(0xFFF6F8FB),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == AppTab.Today,
                    onClick = { tab = AppTab.Today },
                    icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                    label = { Text("今天") },
                )
                NavigationBarItem(
                    selected = tab == AppTab.History,
                    onClick = { tab = AppTab.History },
                    icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                    label = { Text("历史") },
                )
                NavigationBarItem(
                    selected = tab == AppTab.Settings,
                    onClick = { tab = AppTab.Settings },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("设置") },
                )
            }
        },
    ) { padding ->
        when (tab) {
            AppTab.Today -> TodayScreen(state, viewModel, Modifier.padding(padding))
            AppTab.History -> HistoryScreen(state.history, Modifier.padding(padding))
            AppTab.Settings -> SettingsScreen(
                state = state,
                viewModel = viewModel,
                onImportModel = { modelPicker.launch(arrayOf("*/*")) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun TodayScreen(
    state: DiaryUiState,
    viewModel: DiaryViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            TodayHeader(state)
            if (state.isGenerating) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        items(dailyQuestions) { question ->
            QuestionCard(
                title = question.title,
                placeholder = question.placeholder,
                value = state.answers[question.id].orEmpty(),
                skipped = question.id in state.skipped,
                onValueChange = { viewModel.updateAnswer(question.id, it) },
                onSkip = { viewModel.skipQuestion(question.id) },
            )
        }

        item {
            MealCard(state, viewModel)
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("中文日记草稿", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = state.generatedText,
                        onValueChange = viewModel::updateGeneratedText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        placeholder = { Text("生成后可以继续编辑，也可以直接手写。") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = viewModel::generateDiary,
                            enabled = !state.isGenerating,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.EditNote, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("整理成日记")
                        }
                        OutlinedButton(
                            onClick = viewModel::saveDiary,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("保存")
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun TodayHeader(state: DiaryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "离线日记",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(state.date, fontSize = 14.sp, color = Color(0xFF4B5563))
                    Text(state.status, fontSize = 15.sp, color = Color(0xFF1F2937))
                }
                Text(
                    text = if (state.modelAvailable) "模型已配置" else "离线草稿模式",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    title: String,
    placeholder: String,
    value: String,
    skipped: Boolean,
    onValueChange: (String) -> Unit,
    onSkip: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                FilterChip(
                    selected = skipped,
                    onClick = onSkip,
                    label = { Text(if (skipped) "已跳过" else "跳过") },
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text(placeholder) },
            )
        }
    }
}

@Composable
private fun MealCard(state: DiaryUiState, viewModel: DiaryViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row {
                Icon(Icons.Outlined.Restaurant, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("三餐记录", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            }
            MealField("早餐吃了什么？", state.breakfast) { viewModel.updateMeal(MealKind.Breakfast, it) }
            MealField("午餐吃了什么？", state.lunch) { viewModel.updateMeal(MealKind.Lunch, it) }
            MealField("晚餐吃了什么？", state.dinner) { viewModel.updateMeal(MealKind.Dinner, it) }
        }
    }
}

@Composable
private fun MealField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
private fun HistoryScreen(entries: List<DiaryEntry>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("历史日记", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        if (entries.isEmpty()) {
            item { Text("还没有保存的日记。") }
        } else {
            items(entries) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(entry.date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (entry.mood.isNotBlank()) {
                            Text("心情：${entry.mood}", color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            entry.finalText,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: DiaryUiState,
    viewModel: DiaryViewModel,
    onImportModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("设置", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        item {
            ModelCenter(state, viewModel)
        }
        item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("本地模型路径", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.modelAvailable) "模型路径已配置，可用于本地 LiteRT-LM 推理。" else "未配置模型时，应用会使用可编辑的本地草稿。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onImportModel,
                    enabled = !state.isImportingModel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isImportingModel) "正在导入模型……" else "导入 .litertlm 模型文件")
                }
                OutlinedTextField(
                    value = state.settings.modelPath,
                    onValueChange = viewModel::updateModelPath,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模型文件路径") },
                    placeholder = { Text("/sdcard/Download/gemma-3n-e2b.litertlm") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.settings.writingStyle,
                    onValueChange = viewModel::updateWritingStyle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("日记风格") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.settings.reminderTime,
                    onValueChange = viewModel::updateReminderTime,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("每日提醒时间") },
                    placeholder = { Text("21:30") },
                    singleLine = true,
                )
            }
        }
        }
        item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("本地数据", style = MaterialTheme.typography.titleMedium)
                    Text("清空日记、问答和三餐记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = viewModel::deleteAllLocalData) {
                    Icon(Icons.Outlined.Delete, contentDescription = "清空本地数据")
                }
            }
        }
        }
        item {
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ModelCenter(state: DiaryUiState, viewModel: DiaryViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("模型中心", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "推荐先下载 E2B LiteRT-LM。下载完成后，把文件放到手机 Download 目录，再把路径填到下面。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        modelCatalog.forEach { option ->
            ModelOptionCard(
                optionName = option.name,
                badge = option.badge,
                sizeLabel = option.sizeLabel,
                runtime = option.runtime,
                note = option.note,
                url = option.downloadUrl,
                selected = state.settings.selectedModelId == option.id,
                onSelect = { viewModel.updateSelectedModelId(option.id) },
            )
        }
    }
}

@Composable
private fun ModelOptionCard(
    optionName: String,
    badge: String,
    sizeLabel: String,
    runtime: String,
    note: String,
    url: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFEAF2FF) else Color.White,
        ),
        onClick = onSelect,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(optionName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("$sizeLabel · $runtime", fontSize = 13.sp, color = Color(0xFF4B5563))
                }
                FilterChip(
                    selected = selected,
                    onClick = onSelect,
                    label = { Text(if (selected) "已选" else badge) },
                )
            }
            Text(note, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { uriHandler.openUri(url) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("下载页")
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(url)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("复制链接")
                }
            }
        }
    }
}
