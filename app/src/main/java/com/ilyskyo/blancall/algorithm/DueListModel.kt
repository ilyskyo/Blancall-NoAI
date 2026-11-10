// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

/**
 * 「待复习」清单的纯逻辑：从篇级投影里挑出今天该出现在清单里的文章。
 *
 * 只负责「是否到期、到期时间、是否练过」；页面分组（逾期/今天）仍由 UI 做。
 * `LEGACY` 口径下与旧 DueListScreen 的内联逻辑一致：
 * - 练过的看 FSRS due；只有旧记录的按模板兜底；加入计划未练的看到期=加入时刻；
 * - 暂停不出现；未到期不出现；模板跑完（due=0）不出现。
 */
object DueListModel {

    data class Item(
        val articleId: Long,
        val due: Long,
        val practiced: Boolean,
    )

    fun dueItems(
        summaries: List<ArticleMemory.Summary>,
        now: Long,
    ): List<Item> = summaries.mapNotNull { summary ->
        if (summary.state == ArticleMemory.ArticleState.PAUSED) return@mapNotNull null
        val due = when {
            summary.state == ArticleMemory.ArticleState.DUE || summary.legacyDue ->
                summary.earliestDue

            summary.state == ArticleMemory.ArticleState.NEW && summary.enrolledAt > 0L ->
                summary.earliestDue

            else -> return@mapNotNull null
        }
        if (due <= 0L || due > now) return@mapNotNull null
        Item(
            articleId = summary.articleId,
            due = due,
            practiced = summary.source != ArticleMemory.CurveSource.NONE || summary.learned > 0,
        )
    }.sortedBy { it.due }
}