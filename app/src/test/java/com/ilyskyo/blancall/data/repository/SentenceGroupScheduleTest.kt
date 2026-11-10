// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.data.repository

import com.ilyskyo.blancall.algorithm.FsrsEngine
import com.ilyskyo.blancall.algorithm.SentenceSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 文章组操作（暂停 / 恢复 / 改期）必须同时作用于句级状态，
 * 否则投影口径下篇级换了日期、句子仍按旧日期到期。
 */
class SentenceGroupScheduleTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_700_000_000_000L
    private val articleId = 501L

    private fun newStore(): FsrsStateStore {
        val dir = Files.createTempDirectory("blancall-sentence-group").toFile()
        val store = FsrsStateStore.createForTest(File(dir, "fsrs_state.json").absolutePath)
        runBlocking { store.awaitLoaded() }
        return store
    }

    private fun state(due: Long, reviewCount: Int = 2) = FsrsEngine.CardState(
        difficulty = 4.0,
        stability = 3.0,
        due = due,
        lastReview = now - day,
        reviewCount = reviewCount,
    )

    private fun key(index: Int) = SentenceSelector.sentenceKey(articleId, "第 $index 句内容足够长。")

    @Test
    fun `恢复时句级 due 顺延整个暂停时长`() {
        val store = newStore()
        store.save(articleId, state(now + 2 * day))
        val first = key(1)
        val second = key(2)
        store.saveSentences(
            mapOf(
                first to state(now + 2 * day),
                second to state(now + 5 * day),
            )
        )

        assertTrue(store.pauseArticle(articleId, now))
        assertTrue(store.resumeArticle(articleId, now + 3 * day))

        assertEquals(now + 5 * day, store.getSentence(first)?.due)
        assertEquals(now + 8 * day, store.getSentence(second)?.due)
    }

    @Test
    fun `暂停中改期让整组句子落到同一日期`() {
        val store = newStore()
        store.save(articleId, state(now + 2 * day))
        val first = key(1)
        val second = key(2)
        store.saveSentences(
            mapOf(
                first to state(now + 2 * day),
                second to state(now + 5 * day),
            )
        )
        store.pauseArticle(articleId, now)
        val target = now + 10 * day

        assertTrue(store.rescheduleArticle(articleId, target, now + day))
        store.resumeArticle(articleId, now + 2 * day)

        assertEquals(target, store.getSentence(first)?.due)
        assertEquals(target, store.getSentence(second)?.due)
    }

    @Test
    fun `未暂停改期同步改写句级 due`() {
        val store = newStore()
        store.save(articleId, state(now + 2 * day))
        val sentence = key(1)
        store.saveSentence(sentence, state(now + 3 * day))
        val target = now + 9 * day

        assertTrue(store.rescheduleArticle(articleId, target, now))

        assertEquals(target, store.get(articleId)?.due)
        assertEquals(target, store.getSentence(sentence)?.due)
    }

    @Test
    fun `只有句级状态的投影文章也能暂停改期`() {
        val store = newStore()
        val sentence = key(1)
        val other = key(2)
        store.saveSentences(
            mapOf(
                sentence to state(now + 2 * day),
                other to state(now + 4 * day),
            )
        )

        assertTrue("没有篇级卡但有句子状态，应算计划成员", store.isInPlan(articleId))
        assertTrue(store.pauseArticle(articleId, now))
        val target = now + 8 * day
        assertTrue(store.rescheduleArticle(articleId, target, now + day))
        assertTrue(store.resumeArticle(articleId, now + 2 * day))

        assertEquals(target, store.getSentence(sentence)?.due)
        assertEquals(target, store.getSentence(other)?.due)
    }

    @Test
    fun `暂停中练过再改期仍让整组落到新日期`() {
        val store = newStore()
        store.save(articleId, state(now + 2 * day))
        val sentence = key(1)
        store.saveSentence(sentence, state(now + 2 * day))
        store.pauseArticle(articleId, now)

        // 暂停期间手动练了这一句：due 被练习页推进到 now + 6d
        store.updateSentenceWithPrevious(sentence) { previous ->
            previous.copy(due = now + 6 * day, lastReview = now + day, reviewCount = 3)
        }
        val target = now + 12 * day
        store.rescheduleArticle(articleId, target, now + 2 * day)
        store.resumeArticle(articleId, now + 3 * day)

        assertEquals(target, store.get(articleId)?.due)
        assertEquals(target, store.getSentence(sentence)?.due)
    }

    @Test
    fun `撤销恢复会把句级 due 一起还原`() {
        val store = newStore()
        store.save(articleId, state(now + 2 * day))
        val sentence = key(1)
        store.saveSentence(sentence, state(now + 2 * day))
        store.pauseArticle(articleId, now)
        val snapshot = store.snapshotArticles(listOf(articleId))

        store.resumeArticle(articleId, now + 3 * day)
        assertEquals(now + 5 * day, store.getSentence(sentence)?.due)

        assertTrue(store.restoreArticles(snapshot))
        assertEquals("撤销要回到暂停时的原 due", now + 2 * day, store.getSentence(sentence)?.due)
    }

    @Test
    fun `批量更新在一次锁内返回真实前后态`() {
        val store = newStore()
        val first = key(1)
        val second = key(2)
        store.saveSentences(
            mapOf(
                first to state(now + day, reviewCount = 1),
                second to state(now + day, reviewCount = 2),
            )
        )
        val before = store.revision.value

        val transitions = store.updateSentences(
            mapOf(
                first to { previous: FsrsEngine.CardState ->
                    previous.copy(reviewCount = previous.reviewCount + 1)
                },
                second to { previous: FsrsEngine.CardState ->
                    previous.copy(reviewCount = previous.reviewCount + 1)
                },
            )
        ).associateBy { it.key }

        assertEquals(1, transitions.getValue(first).previous.reviewCount)
        assertEquals(2, transitions.getValue(first).next.reviewCount)
        assertEquals(2, transitions.getValue(second).previous.reviewCount)
        assertEquals(3, store.getSentence(second)?.reviewCount)
        assertEquals("批量更新只通知一次", before + 1, store.revision.value)
    }

    @Test
    fun `并发批量更新不会丢计数`() = runBlocking {
        val store = newStore()
        val sentence = key(1)
        store.saveSentence(sentence, state(now + day, reviewCount = 1))

        coroutineScope {
            repeat(20) {
                launch(Dispatchers.Default) {
                    store.updateSentences(
                        mapOf(
                            sentence to { previous: FsrsEngine.CardState ->
                                previous.copy(reviewCount = previous.reviewCount + 1)
                            }
                        )
                    )
                }
            }
        }

        assertEquals(21, store.getSentence(sentence)?.reviewCount)
    }
}
