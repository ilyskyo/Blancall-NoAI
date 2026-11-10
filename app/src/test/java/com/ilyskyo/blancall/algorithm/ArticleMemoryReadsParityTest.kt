// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import com.ilyskyo.blancall.data.model.PracticeRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4 接线前的对拍：`ArticleMemoryReads.Model.LEGACY` 必须与
 * `EbbinghausScheduler.getReviewStatus` 对同一份旧数据给出同一结论。
 */
class ArticleMemoryReadsParityTest {

    private val now = System.currentTimeMillis()
    private val day = 24L * 60 * 60 * 1000
    private val article = Article(
        id = 1L,
        title = "article",
        content = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。",
    )

    @Test
    fun `没有状态没有记录也没有加入计划`() {
        assertParity(state = null, records = emptyList(), inPlan = false)
    }

    @Test
    fun `加入计划但没练`() {
        assertParity(state = null, records = emptyList(), inPlan = true)
    }

    @Test
    fun `篇级状态到期`() {
        assertParity(state = state(due = now - day), records = emptyList(), inPlan = false)
    }

    @Test
    fun `篇级状态未到期`() {
        assertParity(state = state(due = now + day), records = emptyList(), inPlan = false)
    }

    @Test
    fun `只有练习记录`() {
        val records = listOf(
            record(now - 10 * day, correct = 7),
            record(now - 5 * day, correct = 9),
        )
        assertParity(state = null, records = records, inPlan = false)
    }

    @Test
    fun `模板跑完视为已完成`() {
        val records = (0..ReviewTemplate.STANDARD.intervals.size).map { i ->
            record(now - (20L - i) * day, correct = 9)
        }
        val old = EbbinghausScheduler.getReviewStatus(null, records, ReviewTemplate.STANDARD, paused = false, inPlan = false)
        assertEquals(EbbinghausScheduler.ReviewStatus.COMPLETED, old)

        val summary = summary(state = null, records = records, inPlan = false)
        assertEquals(ArticleMemory.ArticleState.LEGACY, summary.state)
        assertFalse(summary.legacyDue)
        assertEquals(0L, summary.earliestDue)
    }

    @Test
    fun `暂停优先于到期`() {
        assertParity(state = state(due = now - day), records = emptyList(), inPlan = true, paused = true)
    }

    private fun assertParity(
        state: FsrsEngine.CardState?,
        records: List<PracticeRecord>,
        inPlan: Boolean,
        paused: Boolean = false,
    ) {
        val old = EbbinghausScheduler.getReviewStatus(
            fsrsState = state,
            records = records,
            template = ReviewTemplate.STANDARD,
            paused = paused,
            inPlan = inPlan,
        )
        val summary = summary(state, records, inPlan, paused)
        when (old) {
            EbbinghausScheduler.ReviewStatus.NOT_STARTED -> {
                assertEquals(ArticleMemory.ArticleState.NEW, summary.state)
                assertEquals(0L, summary.enrolledAt)
                assertEquals(0L, summary.earliestDue)
            }

            EbbinghausScheduler.ReviewStatus.PLANNED -> {
                assertEquals(ArticleMemory.ArticleState.NEW, summary.state)
                assertTrue(summary.enrolledAt > 0L)
                assertEquals(summary.enrolledAt, summary.earliestDue)
            }

            EbbinghausScheduler.ReviewStatus.DUE -> {
                assertEquals(ArticleMemory.ArticleState.LEGACY, summary.state)
                assertTrue(summary.legacyDue)
            }

            is EbbinghausScheduler.ReviewStatus.PENDING -> {
                assertEquals(ArticleMemory.ArticleState.LEGACY, summary.state)
                assertFalse(summary.legacyDue)
                assertTrue(summary.earliestDue > now)
            }

            EbbinghausScheduler.ReviewStatus.PAUSED -> {
                assertEquals(ArticleMemory.ArticleState.PAUSED, summary.state)
            }

            EbbinghausScheduler.ReviewStatus.COMPLETED -> {
                assertEquals(ArticleMemory.ArticleState.LEGACY, summary.state)
                assertFalse(summary.legacyDue)
            }
        }
    }

    private fun summary(
        state: FsrsEngine.CardState?,
        records: List<PracticeRecord>,
        inPlan: Boolean,
        paused: Boolean = false,
    ): ArticleMemory.Summary = ArticleMemoryReads.views(
        articles = listOf(article),
        legacyStates = state?.let { mapOf(1L to it) } ?: emptyMap(),
        sentenceStates = emptyMap(),
        recordsByArticle = mapOf(1L to records),
        template = ReviewTemplate.STANDARD,
        pausedArticleIds = if (paused) setOf(1L) else emptySet(),
        enrolledAt = if (inPlan) mapOf(1L to now - day) else emptyMap(),
        now = now,
        model = ArticleMemoryReads.Model.LEGACY,
    ).single()

    private fun record(timestamp: Long, correct: Int) = PracticeRecord(
        articleId = 1L,
        mode = "SENTENCE",
        totalBlanks = 10,
        correctCount = correct,
        timestamp = timestamp,
    )

    private fun state(due: Long) = FsrsEngine.CardState(
        difficulty = 5.0,
        stability = 10.0,
        due = due,
        lastReview = now - 5 * day,
        reviewCount = 1,
    )
}