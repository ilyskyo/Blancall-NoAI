// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceClozeFocusTest {

    private val content = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。床前明月光疑是地上霜。"

    @Test
    fun `只挖指定句子`() {
        val result = BlancallGenerator.generateSentenceCloze(
            content = content,
            focusSentenceIndices = setOf(1),
        )

        assertEquals(3, result.sentences.size)
        assertTrue(result.blanks.isNotEmpty())
        assertTrue(result.blanks.all { it.sentenceIndex == 1 })
    }

    @Test
    fun `多个目标句每个至少一个空`() {
        val result = BlancallGenerator.generateSentenceCloze(
            content = content,
            count = 1,
            focusSentenceIndices = setOf(0, 2),
        )

        val indices = result.blanks.map { it.sentenceIndex }.toSet()
        assertTrue(indices.contains(0))
        assertTrue(indices.contains(2))
        assertTrue(indices.none { it == 1 })
    }

    @Test
    fun `目标句下标越界时忽略`() {
        val result = BlancallGenerator.generateSentenceCloze(
            content = content,
            focusSentenceIndices = setOf(99),
        )

        // 全部越界 → 按无 focus 处理，仍能生成正文与若干空，不崩。
        assertEquals(3, result.sentences.size)
        assertTrue(result.blanks.isNotEmpty())
    }
}