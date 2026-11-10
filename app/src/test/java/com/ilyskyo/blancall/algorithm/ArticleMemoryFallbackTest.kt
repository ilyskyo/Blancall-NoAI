// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import com.ilyskyo.blancall.data.model.PracticeRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ArticleMemoryFallbackTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val zone = ZoneId.of("UTC")
    private val content = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。"

    @Test
    fun `有记录无状态时用旧到期口径回退`() {
        val records = listOf(
            record(timestamp = now - 10 * day, correct = 7),
            record(timestamp = now - 5 * day, correct = 9),
        )
        val expectedDue = EbbinghausScheduler.nextReviewAt(ReviewTemplate.STANDARD, records, zone)
        assertNotNull(expectedDue)

        val fallback = ArticleMemoryFallback.fromRecords(records, ReviewTemplate.STANDARD, zone)

        assertNotNull(fallback)
        assertEquals(expectedDue, fallback!!.due)
        val initial = ForgettingPredictor.initialStateFromRecords(records)!!
        assertEquals(
            FsrsEngine.retentionRate(initial, now).toFloat(),
            FsrsEngine.retentionRate(fallback, now).toFloat(),
            1e-6f,
        )
    }

    @Test
    fun `没有记录时没有回退`() {
        assertNull(ArticleMemoryFallback.fromRecords(emptyList(), ReviewTemplate.STANDARD, zone))
    }

    @Test
    fun `回退状态进入篇级投影时不会变成全新`() {
        val records = listOf(record(timestamp = now - 5 * day, correct = 9))
        val fallback = ArticleMemoryFallback.fromRecords(records, ReviewTemplate.STANDARD, zone)!!
        val article = Article(id = 1L, title = "article", content = content)

        val summary = ArticleMemory.summary(
            article = article,
            sentenceStates = emptyMap(),
            legacyState = fallback,
            now = now,
        )

        assertEquals(ArticleMemory.ArticleState.LEGACY, summary.state)
        assertEquals(ArticleMemory.CurveSource.LEGACY_ARTICLE, summary.source)
        assertEquals(0, summary.newCount)
        assertEquals(fallback.due, summary.earliestDue)
        assertNotNull(summary.averageRetention)
    }

    @Test
    fun `模板轮次跑完后回退为已完成而不是到期`() {
        val records = (0..ReviewTemplate.STANDARD.intervals.size).map { i ->
            record(timestamp = now - (20L - i) * day, correct = 9)
        }
        assertNull(EbbinghausScheduler.nextReviewAt(ReviewTemplate.STANDARD, records, zone))

        val fallback = ArticleMemoryFallback.fromRecords(records, ReviewTemplate.STANDARD, zone)!!
        val article = Article(id = 1L, title = "article", content = content)
        val summary = ArticleMemory.summary(article, emptyMap(), legacyState = fallback, now = now)

        assertEquals(ArticleMemory.ArticleState.LEGACY, summary.state)
        assertEquals(0L, fallback.due)
        assertFalse(summary.legacyDue)
        assertTrue(summary.averageRetention != null)
    }

    private fun record(timestamp: Long, correct: Int) = PracticeRecord(
        articleId = 1L,
        mode = "SENTENCE",
        totalBlanks = 10,
        correctCount = correct,
        timestamp = timestamp,
    )
}