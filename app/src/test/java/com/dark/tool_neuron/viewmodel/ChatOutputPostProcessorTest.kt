package com.dark.tool_neuron.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatOutputPostProcessorTest {
    @Test
    fun removesDuplicateTextSeparatedByThinkingCloseTag() {
        val answer = "我在黑暗里摸索琴弦，指尖的触感像是未知的星尘。第一次拉弦，声音在空荡的房间里回荡，像是从深渊里传来的呼喊。每一次滑音，都是在黑暗中寻找光明的痕迹。在这段旅程里，我用声音确认自己的方向，也用节奏保存内心的秩序。"
        val input = "$answer</think>$answer"

        assertEquals(answer, ChatOutputPostProcessor.cleanupFinalText(input))
    }

    @Test
    fun removesWholeAnswerRepeatedTwice() {
        val answer = "这是一个完整回答，用来模拟模型把同一段内容输出两遍的情况。它有足够长度，可以被相似度检测识别，但不会误伤短句。这里继续补充一些自然语言内容，让整段答案超过检测阈值，并保持语义连续。最终用户只应该看到一份完整回答，而不是两份重复内容。"
        val input = "$answer\n\n$answer"

        assertEquals(answer, ChatOutputPostProcessor.cleanupFinalText(input))
    }

    @Test
    fun keepsDifferentContent() {
        val input = "第一段说明问题背景和原因。\n\n第二段给出不同的后续行动和风险判断。"

        assertEquals(input, ChatOutputPostProcessor.cleanupFinalText(input))
        assertFalse(ChatOutputPostProcessor.cleanupFinalText(input).isBlank())
    }
}
