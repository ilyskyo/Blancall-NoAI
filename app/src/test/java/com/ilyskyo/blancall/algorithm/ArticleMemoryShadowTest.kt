// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleMemoryShadowTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val twoSentences = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。"

    @Test
    fun `旧卡未到期而句级投影到期时给出差异`() {
        val article = article(1L)
        val picks = SentenceSelector.candidates(article)
        val legacy = state(stability = 20.0, due = now + 20 * day)
        val sentence = state(stability = 10.0, due = now - day)

        val diffs = ArticleMemoryShadow.compare(
            articles = listOf(article),
            legacyStates = mapOf(1L to legacy),
            sentenceStates = mapOf(picks[0].key to sentence),
            now = now,
        )

        val diff = diffs.single()
        assertEquals(ArticleMemoryShadow.OldStatus.LEARNING, diff.oldStatus)
        assertEquals(ArticleMemory.ArticleState.DUE, diff.newState)
        assertTrue(diff.statusChanged)
        assertTrue(diff.dueChanged)
        assertFalse(diff.oldDueNow)
        assertTrue(diff.newDueNow)
    }

    @Test
    fun `旧卡到期而句级投影未到期时给出差异`() {
        val article = article(1L)
        val picks = SentenceSelector.candidates(article)
        val legacy = state(stability = 10.0, due = now - day)
        val sentence = state(stability = 30.0, due = now + 30 * day)

        val diffs = ArticleMemoryShadow.compare(
            articles = listOf(article),
            legacyStates = mapOf(1L to legacy),
            sentenceStates = mapOf(picks[0].key to sentence),
            now = now,
        )

        val diff = diffs.single()
        assertEquals(ArticleMemoryShadow.OldStatus.DUE, diff.oldStatus)
        assertEquals(ArticleMemory.ArticleState.LEARNING, diff.newState)
        assertTrue(diff.statusChanged)
        assertTrue(diff.oldDueNow)
        assertFalse(diff.newDueNow)
    }

    @Test
    fun `没有句级状态的文章不参与投影差异`() {
        val article = article(1L)
        val legacy = state(stability = 10.0, due = now - day)

        val diffs = ArticleMemoryShadow.compare(
            articles = listOf(article),
            legacyStates = mapOf(1L to legacy),
            sentenceStates = emptyMap(),
            now = now,
        )

        assertTrue(diffs.isEmpty())
    }

    @Test
    fun `没有旧卡但有句级状态时也给出差异`() {
        val article = article(1L)
        val picks = SentenceSelector.candidates(article)
        val sentence = state(stability = 20.0, due = now + 20 * day)

        val diffs = ArticleMemoryShadow.compare(
            articles = listOf(article),
            legacyStates = emptyMap(),
            sentenceStates = mapOf(picks[0].key to sentence),
            now = now,
        )

        val diff = diffs.single()
        assertEquals(ArticleMemoryShadow.OldStatus.NONE, diff.oldStatus)
        assertEquals(ArticleMemory.ArticleState.LEARNING, diff.newState)
        assertTrue(diff.statusChanged)
        assertTrue(diff.dueChanged)
    }

    @Test
    fun `暂停时仍按句级到期数判断新到期`() {
        val article = article(1L)
        val picks = SentenceSelector.candidates(article)
        val sent = state(stability = 10.0, due = now - day)

        val diffs = ArticleMemoryShadow.compare(
            articles = listOf(article),
            legacyStates = emptyMap(),
            sentenceStates = mapOf(picks[0].key to sent),
            pausedArticleIds = setOf(1L),
            now = now,
        )

        val diff = diffs.single()
        assertEquals(ArticleMemory.ArticleState.PAUSED, diff.newState)
        assertTrue(diff.newDueNow)
    }

    @Test
    fun `报告包含篇名与覆盖率`() {
        val article = article(1L)
        val picks = SentenceSelector.candidates(article)
        val diffs = ArticleMemoryShadow.compare(
            articles = listOf(article),
            legacyStates = emptyMap(),
            sentenceStates = mapOf(picks[0].key to state(stability = 10.0, due = now - day)),
            now = now,
        )

        val report = ArticleMemoryShadow.report(diffs)
        assertTrue(report.contains(article.title))
        assertTrue(report.contains("learned=1/2"))
    }

    private fun article(id: Long) = Article(
        id = id,
        title = "article-$id",
        content = twoSentences,
    )

    private fun state(
        stability: Double,
        due: Long,
        lastReview: Long = now - 5 * day,
        reviewCount: Int = 1,
    ) = FsrsEngine.CardState(
        difficulty = 5.0,
        stability = stability,
        due = due,
        lastReview = lastReview,
        reviewCount = reviewCount,
    )
}