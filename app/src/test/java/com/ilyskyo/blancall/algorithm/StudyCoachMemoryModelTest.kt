// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyCoachMemoryModelTest {

    private val day = 24L * 60 * 60 * 1000
    private val content = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。"
    private val article = Article(id = 1L, title = "article", content = content)

    @After
    fun resetModel() {
        MemoryModelConfig.model = ArticleMemoryReads.Model.LEGACY
    }

    @Test
    fun `LEGACY 不看句级状态`() {
        val now = System.currentTimeMillis()
        val pick = SentenceSelector.candidates(article).first()
        val source = StudyCoach.Source(
            articles = listOf(article),
            sentenceStates = mapOf(
                pick.key to FsrsEngine.CardState(
                    difficulty = 5.0,
                    stability = 10.0,
                    due = now - day,
                    lastReview = now - 5 * day,
                    reviewCount = 1,
                )
            ),
        )

        MemoryModelConfig.model = ArticleMemoryReads.Model.LEGACY
        val snapshot = StudyCoach.snapshot(StudyCoach.prepare(source), now)

        assertEquals(0, snapshot.dueArticleCount)
    }

    @Test
    fun `PROJECTION 让句级到期进入今日计划`() {
        val now = System.currentTimeMillis()
        val pick = SentenceSelector.candidates(article).first()
        val source = StudyCoach.Source(
            articles = listOf(article),
            sentenceStates = mapOf(
                pick.key to FsrsEngine.CardState(
                    difficulty = 5.0,
                    stability = 10.0,
                    due = now - day,
                    lastReview = now - 5 * day,
                    reviewCount = 1,
                )
            ),
        )

        MemoryModelConfig.model = ArticleMemoryReads.Model.SENTENCE_PROJECTION
        val snapshot = StudyCoach.snapshot(StudyCoach.prepare(source), now)

        assertEquals(1, snapshot.dueArticleCount)
        assertEquals(1, snapshot.sentenceTotalDue)
    }
}