// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import com.ilyskyo.blancall.data.model.PracticeRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ArticleMemoryReadsTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val zone = ZoneId.of("UTC")
    private val content = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。"
    private val article = Article(id = 1L, title = "article", content = content)

    @Test
    fun `LEGACY 口径忽略句级状态`() {
        val picks = SentenceSelector.candidates(article)
        val legacy = state(stability = 20.0, due = now + 20 * day)
        val sentence = state(stability = 10.0, due = now - day)

        val summary = ArticleMemoryReads.views(
            articles = listOf(article),
            legacyStates = mapOf(1L to legacy),
            sentenceStates = mapOf(picks[0].key to sentence),
            now = now,
            model = ArticleMemoryReads.Model.LEGACY,
        ).single()

        assertEquals(ArticleMemory.ArticleState.LEGACY, summary.state)
        assertEquals(ArticleMemory.CurveSource.LEGACY_ARTICLE, summary.source)
        assertEquals(0, summary.learned)
        assertFalse(summary.legacyDue)
    }

    @Test
    fun `PROJECTION 口径优先句级状态`() {
        val picks = SentenceSelector.candidates(article)
        val legacy = state(stability = 20.0, due = now + 20 * day)
        val sentence = state(stability = 10.0, due = now - day)

        val summary = ArticleMemoryReads.views(
            articles = listOf(article),
            legacyStates = mapOf(1L to legacy),
            sentenceStates = mapOf(picks[0].key to sentence),
            now = now,
            model = ArticleMemoryReads.Model.SENTENCE_PROJECTION,
        ).single()

        assertEquals(ArticleMemory.CurveSource.SENTENCE_AGGREGATE, summary.source)
        assertEquals(ArticleMemory.ArticleState.DUE, summary.state)
        assertEquals(1, summary.learned)
        assertEquals(1, summary.dueCount)
    }

    @Test
    fun `只有 records 的文章两种口径都回退为旧口径而不是全新`() {
        val records = listOf(
            record(timestamp = now - 10 * day, correct = 7),
            record(timestamp = now - 5 * day, correct = 9),
        )
        val expectedDue = EbbinghausScheduler.nextReviewAt(ReviewTemplate.STANDARD, records, zone)
        assertNotNull(expectedDue)

        for (model in ArticleMemoryReads.Model.entries) {
            val summary = ArticleMemoryReads.views(
                articles = listOf(article),
                legacyStates = emptyMap(),
                sentenceStates = emptyMap(),
                recordsByArticle = mapOf(1L to records),
                now = now,
                zone = zone,
                model = model,
            ).single()

            assertEquals("$model 不应把存量文章当全新", ArticleMemory.ArticleState.LEGACY, summary.state)
            assertEquals(ArticleMemory.CurveSource.LEGACY_ARTICLE, summary.source)
            assertEquals(expectedDue, summary.earliestDue)
            assertNotNull(summary.averageRetention)
        }
    }

    @Test
    fun `加入计划但没练时两种口径都保留加入时刻`() {
        val enrolledAt = now - day
        for (model in ArticleMemoryReads.Model.entries) {
            val summary = ArticleMemoryReads.views(
                articles = listOf(article),
                legacyStates = emptyMap(),
                sentenceStates = emptyMap(),
                enrolledAt = mapOf(1L to enrolledAt),
                now = now,
                model = model,
            ).single()

            assertEquals(ArticleMemory.ArticleState.NEW, summary.state)
            assertEquals(enrolledAt, summary.earliestDue)
            assertEquals(enrolledAt, summary.enrolledAt)
        }
    }

    @Test
    fun `没有任何状态也没有记录时是全新`() {
        val summary = ArticleMemoryReads.views(
            articles = listOf(article),
            legacyStates = emptyMap(),
            sentenceStates = emptyMap(),
            now = now,
            model = ArticleMemoryReads.Model.SENTENCE_PROJECTION,
        ).single()

        assertEquals(ArticleMemory.ArticleState.NEW, summary.state)
        assertEquals(ArticleMemory.CurveSource.NONE, summary.source)
    }

    @Test
    fun `暂停在两种口径下都保留`() {
        val picks = SentenceSelector.candidates(article)
        val sentence = state(stability = 10.0, due = now - day)

        for (model in ArticleMemoryReads.Model.entries) {
            val summary = ArticleMemoryReads.views(
                articles = listOf(article),
                legacyStates = emptyMap(),
                sentenceStates = mapOf(picks[0].key to sentence),
                pausedArticleIds = setOf(1L),
                now = now,
                model = model,
            ).single()

            assertEquals(ArticleMemory.ArticleState.PAUSED, summary.state)
        }
    }

    @Test
    fun `到期文章排在前面`() {
        val due = Article(id = 1L, title = "A", content = content)
        val fresh = Article(id = 2L, title = "B", content = content)
        val duePicks = SentenceSelector.candidates(due)
        val summary = ArticleMemoryReads.views(
            articles = listOf(fresh, due),
            legacyStates = emptyMap(),
            sentenceStates = mapOf(duePicks[0].key to state(stability = 10.0, due = now - day)),
            now = now,
            model = ArticleMemoryReads.Model.SENTENCE_PROJECTION,
        )

        assertEquals(listOf(1L, 2L), summary.map { it.articleId })
        assertTrue(summary.first().dueCount > 0)
    }

    private fun record(timestamp: Long, correct: Int) = PracticeRecord(
        articleId = 1L,
        mode = "SENTENCE",
        totalBlanks = 10,
        correctCount = correct,
        timestamp = timestamp,
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