// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleMemoryTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val twoSentences = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。"
    private val fourSentences =
        "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。床前明月光疑是地上霜。举头望明月低头思故乡。"

    @Test
    fun `全新文章没有句级状态`() {
        val article = article(1L, twoSentences)

        val summary = ArticleMemory.summary(article, emptyMap(), now = now)

        assertEquals(2, summary.eligible)
        assertEquals(0, summary.learned)
        assertEquals(2, summary.newCount)
        assertEquals(0, summary.dueCount)
        assertEquals(ArticleMemory.ArticleState.NEW, summary.state)
        assertEquals(ArticleMemory.CurveSource.NONE, summary.source)
        assertNull(summary.averageRetention)
        assertNull(summary.weakestRetention)
        assertEquals(0L, summary.earliestDue)
        assertFalse(summary.legacyDue)
    }

    @Test
    fun `部分句子有状态时是学习中`() {
        val article = article(1L, twoSentences)
        val picks = picks(article)
        val state = state(stability = 10.0, due = now + 10 * day)

        val summary = ArticleMemory.summary(article, mapOf(picks[0].key to state), now = now)

        assertEquals(1, summary.learned)
        assertEquals(1, summary.newCount)
        assertEquals(0, summary.dueCount)
        assertEquals(ArticleMemory.ArticleState.LEARNING, summary.state)
        assertEquals(ArticleMemory.CurveSource.SENTENCE_AGGREGATE, summary.source)
        assertClose(FsrsEngine.retentionRate(state, now).toFloat(), summary.averageRetention)
        assertClose(FsrsEngine.retentionRate(state, now).toFloat(), summary.weakestRetention)
    }

    @Test
    fun `全部句子有状态且无到期时是已掌握`() {
        val article = article(1L, twoSentences)
        val picks = picks(article)
        val states = picks.associate { it.key to state(stability = 20.0, due = now + 20 * day) }

        val summary = ArticleMemory.summary(article, states, now = now)

        assertEquals(2, summary.learned)
        assertEquals(0, summary.newCount)
        assertEquals(ArticleMemory.ArticleState.CAUGHT_UP, summary.state)
    }

    @Test
    fun `任一已学句到期就是篇级到期`() {
        val article = article(1L, twoSentences)
        val picks = picks(article)
        val due = now - day
        val states = mapOf(
            picks[0].key to state(stability = 10.0, due = due),
            picks[1].key to state(stability = 10.0, due = now + 10 * day),
        )

        val summary = ArticleMemory.summary(article, states, now = now)

        assertEquals(1, summary.dueCount)
        assertEquals(due, summary.earliestDue)
        assertEquals(ArticleMemory.ArticleState.DUE, summary.state)
        assertFalse(summary.legacyDue)
    }

    @Test
    fun `暂停仍保留到期句数`() {
        val article = article(1L, twoSentences)
        val picks = picks(article)
        val states = mapOf(picks[0].key to state(stability = 10.0, due = now - day))

        val summary = ArticleMemory.summary(article, states, paused = true, now = now)

        assertEquals(ArticleMemory.ArticleState.PAUSED, summary.state)
        assertEquals(1, summary.dueCount)
    }

    @Test
    fun `旧篇级状态回退且标出旧口径`() {
        val article = article(1L, twoSentences)
        val legacy = state(stability = 12.0, due = now - day, lastReview = now - 5 * day)

        val summary = ArticleMemory.summary(article, emptyMap(), legacyState = legacy, now = now)

        assertEquals(ArticleMemory.ArticleState.LEGACY, summary.state)
        assertEquals(ArticleMemory.CurveSource.LEGACY_ARTICLE, summary.source)
        assertTrue(summary.legacyDue)
        assertEquals(0, summary.newCount)
        assertEquals(legacy.due, summary.earliestDue)
        assertEquals(legacy.lastReview, summary.lastReview)
        assertClose(FsrsEngine.retentionRate(legacy, now).toFloat(), summary.averageRetention)
        assertClose(FsrsEngine.retentionRate(legacy, now).toFloat(), summary.weakestRetention)
    }

    @Test
    fun `出现第一个句级状态后切到句级投影`() {
        val article = article(1L, twoSentences)
        val picks = picks(article)
        val legacy = state(stability = 12.0, due = now - day, lastReview = now - 5 * day)
        val sentence = state(stability = 30.0, due = now + 30 * day)

        val summary = ArticleMemory.summary(
            article,
            mapOf(picks[0].key to sentence),
            legacyState = legacy,
            now = now,
        )

        assertEquals(ArticleMemory.CurveSource.SENTENCE_AGGREGATE, summary.source)
        assertEquals(ArticleMemory.ArticleState.LEARNING, summary.state)
        assertEquals(1, summary.learned)
        assertEquals(1, summary.newCount)
        assertFalse(summary.legacyDue)
        assertClose(FsrsEngine.retentionRate(sentence, now).toFloat(), summary.averageRetention)
    }

    @Test
    fun `加入计划但没练时保留加入时刻`() {
        val article = article(1L, twoSentences)
        val enrolledAt = now - day

        val summary = ArticleMemory.summary(
            article = article,
            sentenceStates = emptyMap(),
            enrolledAt = enrolledAt,
            now = now,
        )

        assertEquals(ArticleMemory.ArticleState.NEW, summary.state)
        assertEquals(ArticleMemory.CurveSource.NONE, summary.source)
        assertEquals(enrolledAt, summary.earliestDue)
        assertEquals(enrolledAt, summary.enrolledAt)
        assertEquals(2, summary.newCount)
    }

    @Test
    fun `已有句级状态时不求值 records 回退`() {
        val article = article(1L, twoSentences)
        val pick = picks(article).first()
        var calls = 0

        val summary = ArticleMemory.summary(
            article = article,
            sentenceStates = mapOf(pick.key to state(stability = 10.0, due = now + day)),
            now = now,
            legacyFallback = {
                calls++
                state(stability = 30.0, due = now + 30 * day)
            },
        )

        assertEquals(1, summary.learned)
        assertEquals(0, calls)
    }

    @Test
    fun `没有合格句的文章不参与曲线也不报错`() {
        val article = article(1L, "春。")

        val summary = ArticleMemory.summary(article, emptyMap(), now = now)

        assertEquals(0, summary.eligible)
        assertEquals(0, summary.learned)
        assertEquals(0, summary.newCount)
        assertEquals(ArticleMemory.ArticleState.NEW, summary.state)
        assertNull(summary.averageRetention)
    }

    @Test
    fun `平均留存按非标点字符数加权`() {
        val article = article(1L, "Good morning everyone. Hi there friend.")
        val picks = picks(article)
        assertEquals(2, picks.size)
        val first = state(stability = 5.0, due = now + 10 * day, lastReview = now - 5 * day)
        val second = state(stability = 50.0, due = now + 10 * day, lastReview = now - 5 * day)
        val states = mapOf(picks[0].key to first, picks[1].key to second)

        val summary = ArticleMemory.summary(article, states, now = now)

        val w1 = picks[0].text.count { it.isLetterOrDigit() }.coerceAtLeast(1)
        val w2 = picks[1].text.count { it.isLetterOrDigit() }.coerceAtLeast(1)
        val r1 = FsrsEngine.retentionRate(first, now).toFloat()
        val r2 = FsrsEngine.retentionRate(second, now).toFloat()
        val expected = (r1 * w1 + r2 * w2) / (w1 + w2)
        assertClose(expected, summary.averageRetention)
        assertClose(minOf(r1, r2), summary.weakestRetention)
    }

    @Test
    fun `列表排序到期优先且按最早到期与到期句数`() {
        val dueTwo = article(1L, twoSentences, "A 两句子到期")
        val picksTwo = picks(dueTwo)
        val dueTwoStates = picksTwo.associate {
            it.key to state(stability = 10.0, due = now - 2 * day)
        }

        val dueOne = article(2L, twoSentences, "B 一句子到期")
        val picksOne = picks(dueOne)
        val dueOneStates = mapOf(picksOne[0].key to state(stability = 10.0, due = now - day))

        val learning = article(3L, twoSentences, "C 学习中")
        val learningStates = mapOf(
            picks(learning)[0].key to state(stability = 10.0, due = now + 5 * day)
        )

        val fresh = article(4L, twoSentences, "D 全新")

        val summaries = ArticleMemory.summaries(
            articles = listOf(fresh, learning, dueOne, dueTwo),
            sentenceStates = dueTwoStates + dueOneStates + learningStates,
            now = now,
        )

        assertEquals(listOf(1L, 2L, 3L, 4L), summaries.map { it.articleId })
    }

    @Test
    fun `旧口径到期排在有句级状态的到期之后但仍排在学习中之前`() {
        val legacyArticle = article(1L, twoSentences, "A 旧口径到期")
        val legacy = state(stability = 10.0, due = now - day, lastReview = now - 5 * day)

        val dueArticle = article(2L, twoSentences, "B 句级到期")
        val dueState = mapOf(picks(dueArticle)[0].key to state(stability = 10.0, due = now - day))

        val learning = article(3L, twoSentences, "C 学习中")
        val learningState = mapOf(
            picks(learning)[0].key to state(stability = 10.0, due = now + 5 * day)
        )

        val summaries = ArticleMemory.summaries(
            articles = listOf(learning, legacyArticle, dueArticle),
            sentenceStates = dueState + learningState,
            legacyStates = mapOf(legacyArticle.id to legacy),
            now = now,
        )

        assertEquals(listOf(2L, 1L, 3L), summaries.map { it.articleId })
    }

    private fun article(id: Long, content: String, title: String = "article-$id") = Article(
        id = id,
        title = title,
        content = content,
    )

    private fun picks(article: Article) = SentenceSelector.candidates(article)

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

    private fun assertClose(expected: Float, actual: Float?, delta: Float = 1e-5f) {
        assertNotNull("值为 null", actual)
        assertEquals(expected, actual!!, delta)
    }
}