// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import com.ilyskyo.blancall.data.model.PracticeRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 存量文章（没有 FSRS 状态）的兜底排期只能有一个口径。
 *
 * 统计页的 [ForgettingPredictor] 与首页/列表/小组件/通知都走
 * [EbbinghausScheduler.nextReviewAt]；同一天多次练习按一个日历天计一轮。
 */
class ReviewScheduleConsistencyTest {

    private val day = 24L * 60 * 60 * 1000L
    private val now = 1_800_000_000_000L
    private val article = Article(id = 7L, title = "岳阳楼记", content = "庆历四年春，谪守巴陵郡。")

    private fun record(daysAgo: Long, correct: Int = 4, blanks: Int = 5): PracticeRecord =
        PracticeRecord(
            articleId = article.id,
            mode = "SENTENCE",
            totalBlanks = blanks,
            correctCount = correct,
            timestamp = now - daysAgo * day,
        )

    @Test
    fun `同一天多次练习只算一轮`() {
        // 同一天 3 条 + 4 天前 1 条 = 2 个日历天，远未跑完 STANDARD 的 6 档。
        val records = listOf(
            record(0), record(0), record(0), record(4)
        )
        assertNotNull(
            "同一天重做不能把轮次刷到完成",
            EbbinghausScheduler.nextReviewAt(ReviewTemplate.STANDARD, records)
        )
    }

    @Test
    fun `统计页与调度器给出同一个到期日`() {
        val records = listOf(record(8), record(6), record(4))
        val prediction = ForgettingPredictor.predict(
            listOf(article), records, ReviewTemplate.STANDARD, emptyMap()
        ).single()
        val scheduled = EbbinghausScheduler.nextReviewAt(ReviewTemplate.STANDARD, records)
        assertEquals("统计页与首页/列表必须同一答案", scheduled, prediction.reviewDate)
    }
}
