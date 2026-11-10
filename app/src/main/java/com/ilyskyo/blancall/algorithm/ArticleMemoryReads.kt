// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import com.ilyskyo.blancall.data.model.PracticeRecord
import java.time.ZoneId

/**
 * Phase 4 的读路径适配器：同一批文章可以按 [Model.LEGACY]（旧篇级卡）或
 * [Model.SENTENCE_PROJECTION]（句级投影）出同一套 [ArticleMemory.Summary]。
 *
 * 当前 UI 尚未接这个入口；先把它做成纯函数，等真实数据影子清单确认后，
 * 只需把调用方的 model 从 LEGACY 切到 SENTENCE_PROJECTION，并保留 LEGACY 回滚。
 *
 * 旧数据回退优先级：`a:` 状态 > records 反推 > 无。
 * records 反推见 [ArticleMemoryFallback]（留存/稳定性与统计页同源，due 与旧调度同源）。
 */
object ArticleMemoryReads {

    enum class Model { LEGACY, SENTENCE_PROJECTION }

    fun views(
        articles: List<Article>,
        legacyStates: Map<Long, FsrsEngine.CardState>,
        sentenceStates: Map<String, FsrsEngine.CardState>,
        recordsByArticle: Map<Long, List<PracticeRecord>> = emptyMap(),
        template: ReviewTemplate = ReviewTemplate.STANDARD,
        pausedArticleIds: Set<Long> = emptySet(),
        enrolledAt: Map<Long, Long> = emptyMap(),
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        model: Model = Model.LEGACY,
    ): List<ArticleMemory.Summary> = articles
        .map { article ->
            val stored = legacyStates[article.id]
            ArticleMemory.summary(
                article = article,
                sentenceStates = if (model == Model.SENTENCE_PROJECTION) sentenceStates else emptyMap(),
                legacyState = stored,
                paused = article.id in pausedArticleIds,
                enrolledAt = enrolledAt[article.id] ?: 0L,
                now = now,
                // 只有「没有任何句级状态」时才需要反推 records；否则这一步纯属浪费。
                legacyFallback = if (stored == null) {
                    {
                        ArticleMemoryFallback.fromRecords(
                            records = recordsByArticle[article.id].orEmpty(),
                            template = template,
                            zone = zone,
                        )
                    }
                } else {
                    null
                },
            )
        }
        .sortedWith(
            compareBy<ArticleMemory.Summary> {
                if (it.state == ArticleMemory.ArticleState.DUE || it.legacyDue) 0 else 1
            }
                .thenBy { if (it.earliestDue > 0L) it.earliestDue else Long.MAX_VALUE }
                .thenByDescending { it.dueCount }
                .thenBy { it.title }
        )
}