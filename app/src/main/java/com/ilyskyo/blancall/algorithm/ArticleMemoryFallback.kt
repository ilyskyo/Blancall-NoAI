// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.PracticeRecord
import java.time.ZoneId

/**
 * 存量文章的旧口径回退：有练习记录、但没有 `a:` / `s:` 状态。
 *
 * 这类文章在旧模型里由模板间隔调度；直接交给句级投影会变成 NEW，等于把学过的东西说成没学过。
 * 这里把两条旧口径合成为一张只读 CardState：
 * - 留存/难度/稳定性来自 [ForgettingPredictor.initialStateFromRecords]（与统计页同一模型）；
 * - 到期时间来自 [EbbinghausScheduler.nextReviewAt]（与首页/列表/提醒同一出口）。
 *
 * 结果只用于投影展示，不落盘；用户真练一次后，句级/篇级真实状态会接管。
 */
object ArticleMemoryFallback {

    fun fromRecords(
        records: List<PracticeRecord>,
        template: ReviewTemplate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): FsrsEngine.CardState? {
        if (records.isEmpty()) return null
        val state = ForgettingPredictor.initialStateFromRecords(records) ?: return null
        val due = EbbinghausScheduler.nextReviewAt(template, records, zone) ?: 0L
        return state.copy(due = due)
    }
}