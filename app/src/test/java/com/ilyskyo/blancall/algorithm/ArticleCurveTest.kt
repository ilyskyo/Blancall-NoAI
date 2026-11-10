// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleCurveTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val fourSentences =
        "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。床前明月光疑是地上霜。举头望明月低头思故乡。"

    @Test
    fun `没有句级状态也没有旧状态时曲线为空`() {
        val curve = ArticleMemory.curve(article(fourSentences), emptyMap(), now = now)

        assertTrue(curve.mean.isEmpty())
        assertTrue(curve.weakest.isEmpty())
        assertNull(curve.p25)
        assertNull(curve.p75)
        assertEquals(ArticleMemory.CurveSource.NONE, curve.source)
    }

    @Test
    fun `旧篇级状态画一条单线并标记来源`() {
        val legacy = state(stability = 12.0, due = now - day, lastReview = now - 5 * day)

        val curve = ArticleMemory.curve(
            article = article(fourSentences),
            sentenceStates = emptyMap(),
            legacyState = legacy,
            now = now,
            target = 0.9f,
        )

        assertEquals(ArticleMemory.CurveSource.LEGACY_ARTICLE, curve.source)
        assertEquals(ArticleMemory.DEFAULT_CURVE_DAYS, curve.mean.size)
        assertEquals(curve.mean, curve.weakest)
        assertNull(curve.p25)
        assertNull(curve.p75)
        assertEquals(0.9f, curve.target, 0f)
        assertEquals(
            FsrsEngine.retentionRate(legacy, now).toFloat(),
            curve.mean[0],
            1e-5f,
        )
        assertEquals(
            FsrsEngine.retentionRate(legacy, now + 10 * day).toFloat(),
            curve.mean[10],
            1e-5f,
        )
    }

    @Test
    fun `聚合曲线只算已学句且新句不进曲线`() {
        val article = article(fourSentences)
        val picks = SentenceSelector.candidates(article)
        val learned = state(stability = 20.0, due = now + 20 * day)
        val curve = ArticleMemory.curve(
            article = article,
            sentenceStates = mapOf(picks[0].key to learned),
            now = now,
        )

        assertEquals(ArticleMemory.CurveSource.SENTENCE_AGGREGATE, curve.source)
        assertEquals(ArticleMemory.DEFAULT_CURVE_DAYS, curve.mean.size)
        assertEquals(
            FsrsEngine.retentionRate(learned, now).toFloat(),
            curve.mean[0],
            1e-5f,
        )
        assertEquals(curve.mean, curve.weakest)
        assertNull(curve.p25)
        assertNull(curve.p75)
    }

    @Test
    fun `四句以上才给分位带`() {
        val article = article(fourSentences)
        val picks = SentenceSelector.candidates(article)
        assertEquals(4, picks.size)
        val states = picks.mapIndexed { index, pick ->
            pick.key to state(
                stability = 5.0 + index * 10.0,
                due = now + 10 * day,
                lastReview = now - 5 * day,
            )
        }.toMap()

        val curve = ArticleMemory.curve(article, states, now = now)

        assertNotNull(curve.p25)
        assertNotNull(curve.p75)
        assertEquals(ArticleMemory.DEFAULT_CURVE_DAYS, curve.p25!!.size)
        assertEquals(ArticleMemory.DEFAULT_CURVE_DAYS, curve.p75!!.size)
        for (i in curve.mean.indices) {
            assertTrue("p25 应不高于 p75：day $i", curve.p25!![i] <= curve.p75!![i])
            assertTrue("最弱句应不高于均值：day $i", curve.weakest[i] <= curve.mean[i] + 1e-6f)
        }
    }

    @Test
    fun `曲线天数可注入`() {
        val article = article(fourSentences)
        val picks = SentenceSelector.candidates(article)
        val curve = ArticleMemory.curve(
            article = article,
            sentenceStates = mapOf(picks[0].key to state(stability = 20.0, due = now + day)),
            now = now,
            days = 5,
        )

        assertEquals(5, curve.mean.size)
        assertEquals(5, curve.weakest.size)
    }

    private fun article(content: String) = Article(
        id = 1L,
        title = "article",
        content = content,
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