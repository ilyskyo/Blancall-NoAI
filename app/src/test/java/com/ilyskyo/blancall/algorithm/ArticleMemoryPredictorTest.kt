// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleMemoryPredictorTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val article = Article(
        id = 1L,
        title = "article",
        content = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。",
    )

    @Test
    fun `句级到期生成预测与聚合曲线`() {
        val pick = SentenceSelector.candidates(article).first()
        val prediction = ArticleMemoryPredictor.predict(
            articles = listOf(article),
            allRecords = emptyList(),
            sentenceStates = mapOf(pick.key to state(now - day)),
            now = now,
        ).single()

        assertEquals(ForgettingPredictor.Urgency.TODAY, prediction.urgency)
        assertEquals(now - day, prediction.reviewDate)
        assertEquals(1, prediction.practiceCount)
        assertEquals(10f, prediction.memoryStrength, 1e-5f)
        assertTrue(prediction.retentionRate > 0f)
        assertEquals(ArticleMemory.DEFAULT_CURVE_DAYS, prediction.decayCurve.size)
        assertFalse(prediction.paused)
    }

    @Test
    fun `没有状态的文章是未开始`() {
        val prediction = ArticleMemoryPredictor.predict(
            articles = listOf(article),
            allRecords = emptyList(),
            now = now,
        ).single()

        assertEquals(ForgettingPredictor.Urgency.NEW, prediction.urgency)
        assertEquals(0L, prediction.reviewDate)
        assertTrue(prediction.decayCurve.isEmpty())
    }

    @Test
    fun `旧篇级状态仍能出预测`() {
        val prediction = ArticleMemoryPredictor.predict(
            articles = listOf(article),
            allRecords = emptyList(),
            legacyStates = mapOf(1L to state(now + 20 * day)),
            now = now,
        ).single()

        assertEquals(ForgettingPredictor.Urgency.LATER, prediction.urgency)
        assertEquals(now + 20 * day, prediction.reviewDate)
        assertEquals(1, prediction.practiceCount)
        assertEquals(ArticleMemory.DEFAULT_CURVE_DAYS, prediction.decayCurve.size)
    }

    @Test
    fun `暂停标记保留`() {
        val pick = SentenceSelector.candidates(article).first()
        val prediction = ArticleMemoryPredictor.predict(
            articles = listOf(article),
            allRecords = emptyList(),
            sentenceStates = mapOf(pick.key to state(now - day)),
            pausedIds = setOf(1L),
            now = now,
        ).single()

        assertTrue(prediction.paused)
    }

    private fun state(due: Long) = FsrsEngine.CardState(
        difficulty = 5.0,
        stability = 10.0,
        due = due,
        lastReview = now - 5 * day,
        reviewCount = 1,
    )
}