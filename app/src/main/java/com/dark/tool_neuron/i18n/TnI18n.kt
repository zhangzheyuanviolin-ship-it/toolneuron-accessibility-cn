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
