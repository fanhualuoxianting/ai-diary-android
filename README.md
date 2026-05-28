# 离线日记 Android

一个离线优先的中文 AI 日记助手原型。应用用卡片问题引导用户记录今天做了什么、心情如何，以及早餐、午餐、晚餐；随后通过本地模型接口整理为可编辑中文日记。

## 当前能力

- Kotlin + Jetpack Compose 原生 Android 应用。
- Room 本地保存日记、问答和三餐记录。
- DataStore 保存模型路径和写作风格。
- WorkManager 每日中文提醒。
- `LocalLlmEngine` 抽象和 `GemmaLocalEngine` LiteRT-LM 本地推理实现。
- 未配置模型或推理失败时，仍可生成本地可编辑草稿并保存。

## Gemma 接入点

`app/src/main/java/com/example/aidiary/llm/LocalLlmEngine.kt` 中的 `GemmaLocalEngine` 使用 `com.google.ai.edge.litertlm:litertlm-android` 运行 `.litertlm` 模型。当前 prompt 要求不虚构事件、食物或情绪。

## 推荐模型

首选下载 Gemma 3n E2B LiteRT-LM：

- 推荐：<https://huggingface.co/google/gemma-3n-E2B-it-litert-lm>
- 更强但更大：<https://huggingface.co/google/gemma-3n-E4B-it-litert-lm>
- 轻量实验：<https://huggingface.co/google/gemma-3-270m>
- Gemma 4 文档入口：<https://ai.google.dev/gemma/docs/core>

Hugging Face 上的 Google 模型通常需要登录并接受模型许可后才能下载。下载完成后，在 app 设置页点“导入 .litertlm 模型文件”，选择下载好的模型；app 会复制到自己的私有目录并保存可读路径。不要只手动填写 `/sdcard/Download/...`，Android 13+ 通常不允许普通 app 直接读取这个目录。

## 构建

用 Android Studio 打开 `ai-diary-android`，同步 Gradle 后运行：

```powershell
.\gradlew test
.\gradlew assembleDebug
```

如果要真正启用 Gemma，需要把可在 Android 端运行的移动模型文件放到手机本地，并在设置页填写模型路径，再把 `GemmaLocalEngine` 内部替换为实际推理调用。
