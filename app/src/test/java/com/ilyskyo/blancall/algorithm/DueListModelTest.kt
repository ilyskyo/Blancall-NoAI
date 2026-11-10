// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DueListModelTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `句级到期进入清单`() {
        val items = DueListModel.dueItems(
            listOf(summary(state = ArticleMemory.ArticleState.DUE, due = now - day, learned = 1)),
            now,
        )

        assertEquals(1, items.size)
        assertEquals(now - day, items[0].due)
        assertTrue(items[0].practiced)
    }

    @Test
    fun `旧篇级卡到期进入清单`() {
        val items = DueListModel.dueItems(
            listOf(
                summary(
                    state = ArticleMemory.ArticleState.LEGACY,
                    source = ArticleMemory.CurveSource.LEGACY_ARTICLE,
                    due = now - 2 * day,
                    legacyDue = true,
                )
            ),
            now,
        )

        assertEquals(1, items.size)
        assertTrue(items[0].practiced)
    }

    @Test
    fun `旧篇级卡未到期不进入清单`() {
        val items = DueListModel.dueItems(
            listOf(
                summary(
                    state = ArticleMemory.ArticleState.LEGACY,
                    source = ArticleMemory.CurveSource.LEGACY_ARTICLE,
                    due = now + day,
                )
            ),
            now,
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `加入计划未练按加入时刻进入清单`() {
        val items = DueListModel.dueItems(
            listOf(
                summary(
                    state = ArticleMemory.ArticleState.NEW,
                    source = ArticleMemory.CurveSource.NONE,
                    due = now - day,
                    enrolledAt = now - day,
                )
            ),
            now,
        )

        assertEquals(1, items.size)
        assertFalse(items[0].practiced)
    }

    @Test
    fun `全新文章不进入清单`() {
        val items = DueListModel.dueItems(
            listOf(summary(state = ArticleMemory.ArticleState.NEW)),
            now,
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `暂停不进入清单`() {
        val items = DueListModel.dueItems(
            listOf(
                summary(
                    state = ArticleMemory.ArticleState.PAUSED,
                    due = now - day,
                    legacyDue = true,
                    learned = 1,
                )
            ),
            now,
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `未到期不进入清单`() {
        val items = DueListModel.dueItems(
            listOf(summary(state = ArticleMemory.ArticleState.LEARNING, due = now + day, learned = 1)),
            now,
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `due 为零不进入清单`() {
        val items = DueListModel.dueItems(
            listOf(
                summary(
                    state = ArticleMemory.ArticleState.LEGACY,
                    source = ArticleMemory.CurveSource.LEGACY_ARTICLE,
                    due = 0L,
                )
            ),
            now,
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `按到期时间升序`() {
        val items = DueListModel.dueItems(
            listOf(
                summary(articleId = 1L, state = ArticleMemory.ArticleState.DUE, due = now - day, learned = 1),
                summary(articleId = 2L, state = ArticleMemory.ArticleState.DUE, due = now - 3 * day, learned = 1),
            ),
            now,
        )

        assertEquals(listOf(2L, 1L), items.map { it.articleId })
    }

    private fun summary(
        articleId: Long = 1L,
        state: ArticleMemory.ArticleState,
        source: ArticleMemory.CurveSource = ArticleMemory.CurveSource.SENTENCE_AGGREGATE,
        due: Long = 0L,
        legacyDue: Boolean = false,
        enrolledAt: Long = 0L,
        learned: Int = 0,
    ) = ArticleMemory.Summary(
        articleId = articleId,
        title = "article-$articleId",
        eligible = 2,
        learned = learned,
        newCount = 0,
        dueCount = if (state == ArticleMemory.ArticleState.DUE) 1 else 0,
        earliestDue = due,
        averageRetention = null,
        weakestRetention = null,
        lastReview = 0L,
        state = state,
        source = source,
        legacyDue = legacyDue,
        enrolledAt = enrolledAt,
    )
}