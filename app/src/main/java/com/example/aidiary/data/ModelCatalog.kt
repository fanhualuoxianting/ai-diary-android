package com.example.aidiary.data

data class DiaryModelOption(
    val id: String,
    val name: String,
    val badge: String,
    val sizeLabel: String,
    val runtime: String,
    val source: String,
    val downloadUrl: String,
    val note: String,
)

object ModelIds {
    const val Gemma3nE2bLiteRt = "gemma_3n_e2b_litert_lm"
    const val Gemma3nE4bLiteRt = "gemma_3n_e4b_litert_lm"
    const val Gemma3270m = "gemma_3_270m"
    const val Gemma4Docs = "gemma_4_models"
}

val modelCatalog = listOf(
    DiaryModelOption(
        id = ModelIds.Gemma3nE2bLiteRt,
        name = "Gemma 3n E2B LiteRT-LM",
        badge = "推荐",
        sizeLabel = "中等，适合高端安卓手机",
        runtime = "LiteRT-LM / Android",
        source = "Hugging Face: google/gemma-3n-E2B-it-litert-lm",
        downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm",
        note = "首选。质量和体积比较均衡，官方模型页给了 Android 设备 LiteRT-LM 性能数据。",
    ),
    DiaryModelOption(
        id = ModelIds.Gemma3nE4bLiteRt,
        name = "Gemma 3n E4B LiteRT-LM",
        badge = "更强",
        sizeLabel = "更大，建议旗舰机",
        runtime = "LiteRT-LM / Android",
        source = "Hugging Face: google/gemma-3n-E4B-it-litert-lm",
        downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm",
        note = "生成质量更好，但下载和运行成本更高。手机内存不足时不建议首选。",
    ),
    DiaryModelOption(
        id = ModelIds.Gemma3270m,
        name = "Gemma 3 270M",
        badge = "轻量",
        sizeLabel = "小，适合快速实验",
        runtime = "LiteRT / 需转换或适配",
        source = "Hugging Face: google/gemma-3-270m",
        downloadUrl = "https://huggingface.co/google/gemma-3-270m",
        note = "非常轻，但更适合专项微调；直接写长日记的质量会弱一些。",
    ),
    DiaryModelOption(
        id = ModelIds.Gemma4Docs,
        name = "Gemma 4 E2B/E4B",
        badge = "后续",
        sizeLabel = "新一代移动模型",
        runtime = "等待移动端包确认",
        source = "Google AI for Developers",
        downloadUrl = "https://ai.google.dev/gemma/docs/core",
        note = "官方说明 E2B/E4B 面向移动和边缘设备；等 LiteRT-LM 移动包稳定后再接入。",
    ),
)
