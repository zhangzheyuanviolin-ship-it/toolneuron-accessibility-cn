package com.dark.tool_neuron.i18n

import java.util.Locale

private val zhMap = mapOf(
    "Action icon" to "操作图标",
    "Back" to "返回",
    "Close" to "关闭",
    "Open" to "打开",
    "Cancel" to "取消",
    "Confirm" to "确认",
    "Done" to "完成",
    "Retry" to "重试",
    "Save" to "保存",
    "Save Changes" to "保存更改",
    "Delete" to "删除",
    "Delete Model" to "删除模型",
    "Delete Everything" to "删除全部数据",
    "Clear All" to "清空全部",
    "Clear Stale" to "清理过期记忆",
    "Continue" to "继续",
    "Skip" to "跳过",
    "Search" to "搜索",
    "Refresh" to "刷新",
    "Loading..." to "加载中...",
    "Running..." to "运行中...",
    "Error" to "错误",
    "Success" to "成功",
    "Settings" to "设置",
    "General" to "通用",
    "System" to "系统",
    "Store" to "商店",
    "Installed" to "已安装",
    "Model Store" to "模型商店",
    "Model load failed" to "模型加载失败",
    "Load" to "加载",
    "Unload" to "卸载",
    "Download" to "下载",
    "Upload" to "上传",
    "Importing RAG package..." to "正在导入 RAG 包...",
    "RAG Manager" to "RAG 管理器",
    "Load Previous Model?" to "加载上一个模型？",
    "Search models..." to "搜索模型...",
    "Search memories..." to "搜索记忆...",
    "Search nodes..." to "搜索节点...",
    "Advanced Filters" to "高级筛选",
    "Clear All Memories?" to "清空所有记忆？",
    "Clear Stale Memories?" to "清理过期记忆？",
    "Go Back" to "返回",
    "Unlock" to "解锁",
    "Enter Password" to "输入密码",
    "Password" to "密码",
    "Show password" to "显示密码",
    "Hide password" to "隐藏密码",
    "Image Tools" to "图像工具",
    "Model Picker" to "模型选择器",
    "Processing Model...." to "正在处理模型....",
    "Restore from Backup" to "从备份恢复",
    "Select Backup File" to "选择备份文件",
    "Create Backup" to "创建备份",
    "Delete All Data" to "删除全部数据",
    "Type DELETE" to "输入 DELETE",
    "Repository Path" to "仓库路径",
    "Add Repository" to "添加仓库",
    "Edit Repository" to "编辑仓库",
    "Add User" to "添加用户",
    "Name" to "名称",
    "Description" to "描述",
    "Domain" to "领域",
    "Tags" to "标签",
    "Read" to "读取",
    "Admin" to "管理员",
    "Initialize" to "初始化",
    "Load Model" to "加载模型",
    "Chat" to "聊天",
    "Image" to "图像",
    "Memory" to "记忆",
    "Tools" to "工具",
    "Status" to "状态",
    "Send" to "发送",
    "Menu" to "菜单",
    "Open sidebar" to "打开侧边栏",
    "Open settings" to "打开设置",
    "Open model store" to "打开模型商店",
    "Import local model" to "从本地导入模型",
    "More options" to "更多选项",
    "Select model" to "选择模型",
    "Toggle web search" to "切换网络搜索",
    "Toggle thinking mode" to "切换思考模式",
    "Send message" to "发送消息",
    "Stop generation" to "停止生成",
    "All" to "全部",
    "CPU" to "CPU",
    "NPU" to "NPU",
    "Name (A-Z)" to "名称（A-Z）",
    "Name" to "名称",
    "Size" to "大小",
    "Recently Added" to "最近添加",
    "GGUF" to "GGUF",
    "Stable Diffusion" to "稳定扩散",
    "TTS" to "语音合成"
    ,"Web Search" to "网络搜索"
    ,"Plugins" to "插件"
    ,"Manage" to "管理"
    ,"Configure" to "配置"
    ,"Manage RAG" to "管理知识库"
    ,"Configure plugins" to "配置插件"
    ,"No models installed. Download one from the store or load a local GGUF file." to "未安装模型。请从商店下载，或从本地加载 GGUF 文件。"
    ,"Say Anything…" to "输入你想说的话…"
    ,"Describe the image you want…" to "描述你想生成的图片…"
    ,"Switch to text chat" to "切换到文本聊天"
    ,"Load a text model to enable chat" to "请先加载文本模型以启用聊天"
    ,"Switch to image generation" to "切换到图像生成"
    ,"Load an image model to enable" to "请先加载图像模型以启用"
    ,"Welcome to ToolNeuron" to "欢迎使用 ToolNeuron"
    ,"Your privacy-first AI assistant.\nEverything runs on your device — no cloud, no telemetry." to "你的隐私优先 AI 助手。\n所有处理均在本机完成，不上传云端，不做遥测。"
    ,"Your privacy-first AI assistant.\nEverything runs on your device - no cloud, no telemetry." to "你的隐私优先 AI 助手。\n所有处理均在本机完成，不上传云端，不做遥测。"
    ,"Terms & Conditions" to "条款与条件"
    ,"Please read carefully before using ToolNeuron" to "使用 ToolNeuron 前请仔细阅读"
    ,"I Accept" to "我同意"
    ,"Scroll to Continue" to "请滑到底部后继续"
    ,"Accept Terms" to "同意条款"
    ,"You can Minimize the app, Will Notify You" to "你可以最小化应用，下载完成后会通知你"
    ,"Welcome User" to "欢迎使用"
    ,"Choose Your Setup!" to "请选择初始化方案"
    ,"Text Generation" to "文本生成"
    ,"Text + Speech" to "文本 + 语音"
    ,"Image Generation" to "图像生成"
    ,"Power Mode" to "性能模式"
    ,"Set up later in Store" to "稍后在商店中设置"
    ,"Optimize Performance" to "优化性能"
    ,"Choose how your device runs AI models" to "选择设备运行 AI 模型的方式"
    ,"Maximum speed, higher battery usage" to "最高速度，耗电更高"
    ,"Good speed with reasonable battery life" to "速度与续航平衡"
    ,"Slower but saves battery" to "速度较慢但更省电"
    ,"Performance" to "高性能"
    ,"Balanced" to "均衡"
    ,"Power Saver" to "省电"
    ,"Selected" to "已选中"
    ,"Downloading" to "下载中"
    ,"Restoring..." to "恢复中..."
    ,"Restore complete!" to "恢复完成！"
    ,"Enter your backup password, then select the backup file." to "请输入备份密码，然后选择备份文件。"
    ,"Backup Password" to "备份密码"
    ,"Notification permission granted" to "通知权限已授予"
    ,"Notification permission denied" to "通知权限被拒绝"
    ,"Model Picker" to "模型选择器"
    ,"GGUF Model" to "GGUF 模型"
    ,"Pick a .gguf model file for text generation. The file will be accessed via a secure file descriptor - no broad storage permission needed." to "选择用于文本生成的 .gguf 模型文件。应用将通过安全文件句柄访问文件，无需授予广泛存储权限。"
    ,"Pick Model File" to "选择模型文件"
    ,"Close search" to "关闭搜索"
    ,"Show NSFW Content" to "显示 NSFW 内容"
    ,"Execution" to "执行目标"
    ,"Error loading models" to "加载模型失败"
    ,"No models found" to "未找到模型"
    ,"View models" to "查看模型"
    ,"No installed models" to "暂无已安装模型"
    ,"Active" to "已启用"
    ,"Details" to "详情"
    ,"Open vault manager" to "打开仓库管理"
    ,"Create new chat" to "新建聊天"
    ,"Delete chat" to "删除聊天"
    ,"No chats yet" to "还没有聊天记录"
    ,"Tap + to start a new conversation" to "点击 + 开始新的对话"
    ,"Loading chats..." to "正在加载聊天..."
    ,"Chats" to "聊天列表"
    ,"Download model" to "下载模型"
    ,"You previously had a model loaded. Would you like to load it again?" to "你之前加载过模型，是否再次加载？"
    ,"None loaded" to "未加载"
)

private val zhTokens = listOf(
    "Installed (" to "已安装（",
    "Loaded (" to "已加载（",
    "Loading..." to "加载中...",
    "Error" to "错误",
    "Success" to "成功",
    "Cancel" to "取消",
    "Confirm" to "确认",
    "Continue" to "继续",
    "Retry" to "重试",
    "Settings" to "设置",
    "Download" to "下载",
    "Upload" to "上传",
    "Model" to "模型",
    "Memory" to "记忆",
    "Search" to "搜索",
    "Delete" to "删除",
    "Close" to "关闭",
    "Open" to "打开",
    "Back" to "返回"
    ,"Web Search" to "网络搜索"
    ,"Plugin" to "插件"
    ,"Plugins" to "插件"
    ,"tools" to "工具"
    ,"model" to "模型"
    ,"models" to "模型"
    ,"Selected" to "已选中"
    ,"Downloading" to "下载中"
    ,"Restore" to "恢复"
    ,"Model Picker" to "模型选择器"
    ,"Chats" to "聊天列表"
    ,"msgs" to "条消息"
)

fun tn(text: String): String {
    val isZh = Locale.getDefault().toLanguageTag().lowercase(Locale.ROOT).startsWith("zh")
    if (!isZh) return text
    zhMap[text]?.let { return it }
    var translated = text
    for ((en, zh) in zhTokens) {
        translated = translated.replace(en, zh)
    }
    translated = translated.replace(" installed", " 已安装")
    translated = translated.replace(" loaded", " 已加载")
    translated = translated.replace("(", "（").replace(")", "）")
    return translated
}
