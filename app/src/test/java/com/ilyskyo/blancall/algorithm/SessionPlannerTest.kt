// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlannerTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val two = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。"
    private val three = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。床前明月光疑是地上霜。"
    private val four = "春眠不觉晓处处闻啼鸟。夜来风雨声花落知多少。床前明月光疑是地上霜。举头望明月低头思故乡。"

    @Test
    fun `到期句优先于新句`() {
        val a = article(1L, two)
        val b = article(2L, two)
        val ap = picks(a)
        val bp = picks(b)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a, b),
                sentenceStates = mapOf(
                    ap[0].key to dueState(now - day),
                    ap[1].key to futureState(),
                ),
                budgetMinutes = 2.0,
                now = now,
            )
        )

        assertEquals(2, plan.selectedKeys.size)
        assertEquals(
            listOf(SessionPlanner.Kind.DUE, SessionPlanner.Kind.NEW),
            plan.articlePlans.flatMap { it.candidates }.map { it.kind },
        )
        assertEquals(0, plan.dueRemaining)
        assertEquals(1, plan.newRemaining)
    }

    @Test
    fun `最逾期的先选`() {
        val a = article(1L, two)
        val p = picks(a)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a),
                sentenceStates = mapOf(
                    p[0].key to dueState(now - day),
                    p[1].key to dueState(now - 2 * day),
                ),
                budgetMinutes = 2.0,
                now = now,
            )
        )

        assertEquals(listOf(p[1].key, p[0].key), plan.selectedKeys.toList())
    }

    @Test
    fun `预算用尽时不选低优先级`() {
        val a = article(1L, four)
        val p = picks(a)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a),
                sentenceStates = mapOf(
                    p[0].key to dueState(now - day),
                    p[1].key to dueState(now - 2 * day),
                    p[2].key to dueState(now - 3 * day),
                ),
                budgetMinutes = 2.0,
                now = now,
            )
        )

        assertEquals(2, plan.selectedKeys.size)
        assertEquals(1, plan.dueRemaining)
        assertEquals(1, plan.newRemaining)
        assertTrue(plan.articlePlans.flatMap { it.candidates }.all { it.kind == SessionPlanner.Kind.DUE })
    }

    @Test
    fun `新句按限量选择`() {
        val a = article(1L, four)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a),
                sentenceStates = emptyMap(),
                budgetMinutes = 10.0,
                newSentenceLimit = 2,
                now = now,
            )
        )

        assertEquals(2, plan.selectedKeys.size)
        assertEquals(2, plan.newRemaining)
    }

    @Test
    fun `暂停文章不参与`() {
        val a = article(1L, two)
        val p = picks(a)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a),
                sentenceStates = mapOf(p[0].key to dueState(now - day)),
                excludedArticleIds = setOf(1L),
                budgetMinutes = 10.0,
                now = now,
            )
        )

        assertTrue(plan.selectedKeys.isEmpty())
        assertEquals(0, plan.dueTotal)
    }

    @Test
    fun `弱句在到期之后新句之前`() {
        val a = article(1L, three)
        val p = picks(a)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a),
                sentenceStates = mapOf(
                    p[0].key to dueState(now - day),
                    p[1].key to futureState(),
                ),
                weakKeys = setOf(p[1].key),
                budgetMinutes = 3.0,
                now = now,
            )
        )

        assertEquals(
            listOf(SessionPlanner.Kind.DUE, SessionPlanner.Kind.WEAK, SessionPlanner.Kind.NEW),
            plan.articlePlans.flatMap { it.candidates }.map { it.kind },
        )
    }

    @Test
    fun `错题排在最后`() {
        val a = article(1L, three)
        val p = picks(a)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a),
                sentenceStates = mapOf(
                    p[0].key to dueState(now - day),
                    p[1].key to futureState(),
                ),
                mistakeKeys = setOf(p[1].key),
                budgetMinutes = 3.0,
                now = now,
            )
        )

        assertEquals(
            listOf(SessionPlanner.Kind.DUE, SessionPlanner.Kind.NEW, SessionPlanner.Kind.MISTAKE),
            plan.articlePlans.flatMap { it.candidates }.map { it.kind },
        )
    }

    @Test
    fun `覆盖率按篇统计`() {
        val a = article(1L, two)
        val p = picks(a)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a),
                sentenceStates = mapOf(p[0].key to dueState(now - day)),
                budgetMinutes = 2.0,
                now = now,
            )
        )

        val coverage = plan.coverageByArticle.getValue(1L)
        assertEquals(2, coverage.eligible)
        assertEquals(1, coverage.dueBefore)
        assertEquals(1, coverage.selectedDue)
        assertEquals(1, coverage.selectedNew)
    }

    @Test
    fun `计划不修改输入状态`() {
        val a = article(1L, two)
        val p = picks(a)
        val states = mapOf(p[0].key to dueState(now - day))
        val before = states.mapValues { it.value.copy() }

        SessionPlanner.plan(
            SessionPlanner.Input(articles = listOf(a), sentenceStates = states, now = now)
        )

        assertEquals(before, states)
    }

    @Test
    fun `孤儿弱句键被忽略`() {
        val a = article(1L, two)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a),
                sentenceStates = emptyMap(),
                weakKeys = setOf("s:99:orphan"),
                budgetMinutes = 1.0,
                now = now,
            )
        )

        assertEquals(1, plan.selectedKeys.size)
        assertEquals(SessionPlanner.Kind.NEW, plan.articlePlans.single().candidates.single().kind)
    }

    @Test
    fun `到期句因预算放不下时不选新句`() {
        val a = article(1L, two)
        val b = article(2L, two)
        val ap = picks(a)
        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a, b),
                sentenceStates = mapOf(ap[0].key to dueState(now - day)),
                budgetMinutes = 2.0,
                now = now,
                minutesPerSentence = { text -> if (text == ap[0].text) 5.0 else 1.0 },
            )
        )

        assertEquals(0, plan.selectedKeys.size)
        assertEquals(1, plan.dueRemaining)
        assertEquals(3, plan.newRemaining)
    }

    @Test
    fun `三十句文章十句到期只推进到期句`() {
        // 句文本插序号保证 30 个互不相同的 s: 键（同文会撞 hash）
        val base = two.substringBefore("。")
        val content = (1..30).joinToString("") { "$base$it。" }
        val a = article(1L, content)
        val p = picks(a)
        assertEquals(30, p.size)
        val dueKeys = p.take(10).map { it.key }.toSet()
        val states = p.take(10).associate { it.key to dueState(now - day) }
        val before = states.mapValues { it.value.copy() }

        val plan = SessionPlanner.plan(
            SessionPlanner.Input(
                articles = listOf(a),
                sentenceStates = states,
                budgetMinutes = 10.0,
                now = now,
            )
        )

        assertEquals(dueKeys, plan.selectedKeys.toSet())
        assertEquals(0, plan.dueRemaining)
        assertEquals(20, plan.newRemaining)
        val coverage = plan.coverageByArticle.getValue(1L)
        assertEquals(30, coverage.eligible)
        assertEquals(10, coverage.dueBefore)
        assertEquals(10, coverage.selectedDue)
        assertEquals(0, coverage.selectedNew)
        // 未评估（未选）句的状态不被计划触碰
        assertEquals(before, states)
    }

    private fun article(id: Long, content: String) = Article(id = id, title = "article-$id", content = content)

    private fun picks(article: Article) = SentenceSelector.candidates(article)

    private fun dueState(due: Long) = FsrsEngine.CardState(
        difficulty = 5.0,
        stability = 10.0,
        due = due,
        lastReview = now - 5 * day,
        reviewCount = 1,
    )

    private fun futureState() = FsrsEngine.CardState(
        difficulty = 5.0,
        stability = 30.0,
        due = now + 30 * day,
        lastReview = now - 5 * day,
        reviewCount = 1,
    )
}