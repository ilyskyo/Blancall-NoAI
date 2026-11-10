// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceSelectorDueTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val article = Article(
        id = 1L,
        title = "article",
        content = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。",
    )

    @Test
    fun `只返回到期的句子索引`() {
        val sentences = SentenceSplitter.split(article.content)
        val states = mapOf(
            SentenceSelector.sentenceKey(1L, sentences[0]) to state(now - day),
            SentenceSelector.sentenceKey(1L, sentences[1]) to state(now + day),
        )

        assertEquals(setOf(0), SentenceSelector.dueSentenceIndices(article, states, now))
    }

    @Test
    fun `没有状态返回空集`() {
        assertTrue(SentenceSelector.dueSentenceIndices(article, emptyMap(), now).isEmpty())
    }

    @Test
    fun `due 为零的句子不会被当成到期`() {
        val sentences = SentenceSplitter.split(article.content)
        val finished = state(due = 0L)
        val states = mapOf(SentenceSelector.sentenceKey(1L, sentences[0]) to finished)

        assertTrue(SentenceSelector.dueSentenceIndices(article, states, now).isEmpty())
    }

    @Test
    fun `短句只要有状态也按全文下标返回`() {
        val short = Article(id = 2L, title = "short", content = "春。夜来风雨声花落知多少。")
        val sentences = SentenceSplitter.split(short.content)
        val states = mapOf(SentenceSelector.sentenceKey(2L, sentences[0]) to state(now - day))

        assertEquals(setOf(0), SentenceSelector.dueSentenceIndices(short, states, now))
    }

    private fun state(due: Long) = FsrsEngine.CardState(
        difficulty = 5.0,
        stability = 10.0,
        due = due,
        lastReview = now - 5 * day,
        reviewCount = 1,
    )
}