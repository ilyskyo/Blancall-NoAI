// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.ui.statistics

import com.ilyskyo.blancall.algorithm.ArticleMemoryReads
import com.ilyskyo.blancall.algorithm.FsrsEngine
import com.ilyskyo.blancall.algorithm.MemoryModelConfig
import com.ilyskyo.blancall.algorithm.SentenceSelector
import com.ilyskyo.blancall.data.model.Article
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyFlowMemoryModelTest {

    private val day = 24L * 60 * 60 * 1000
    private val article = Article(
        id = 1L,
        title = "article",
        content = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。",
    )

    @After
    fun resetModel() {
        MemoryModelConfig.model = ArticleMemoryReads.Model.LEGACY
    }

    @Test
    fun `LEGACY 时句级状态不生成任务`() {
        val now = System.currentTimeMillis()
        val pick = SentenceSelector.candidates(article).first()
        MemoryModelConfig.model = ArticleMemoryReads.Model.LEGACY

        val tasks = buildTasks(
            articles = listOf(article),
            states = emptyMap(),
            pauses = emptyMap(),
            sentenceStates = mapOf(pick.key to state(now)),
            now = now,
        )

        assertTrue(tasks.isEmpty())
    }

    @Test
    fun `PROJECTION 时句级到期生成任务`() {
        val now = System.currentTimeMillis()
        val pick = SentenceSelector.candidates(article).first()
        MemoryModelConfig.model = ArticleMemoryReads.Model.SENTENCE_PROJECTION

        val tasks = buildTasks(
            articles = listOf(article),
            states = emptyMap(),
            pauses = emptyMap(),
            sentenceStates = mapOf(pick.key to state(now)),
            now = now,
        )

        assertEquals(1, tasks.size)
        assertEquals(now - day, tasks[0].due)
        assertEquals(1, tasks[0].sentenceDue)
        assertEquals(1, tasks[0].sentenceLearned)
    }

    @Test
    fun `投影口径停止写篇级卡而 LEGACY 保留写入`() {
        MemoryModelConfig.model = ArticleMemoryReads.Model.LEGACY
        assertTrue(MemoryModelConfig.writesLegacyArticleStates())
        MemoryModelConfig.model = ArticleMemoryReads.Model.SENTENCE_PROJECTION
        assertTrue(!MemoryModelConfig.writesLegacyArticleStates())
    }

    private fun state(now: Long) = FsrsEngine.CardState(
        difficulty = 5.0,
        stability = 10.0,
        due = now - day,
        lastReview = now - 5 * day,
        reviewCount = 1,
    )
}