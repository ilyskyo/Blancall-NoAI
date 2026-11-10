// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article

/**
 * 影子投影差异：只比较「有句级状态」的文章，旧 `a:` 卡 vs 新句级投影。
 *
 * 用途是切换前的只读体检，不参与调度、不写盘、不显示给用户：
 * - 旧卡说还早、新投影说到期（或反之）的篇；
 * - 到期日变化；
 * - 每篇的句级覆盖率（learned/eligible）。
 *
 * 没有句级状态的文章两边都走旧 `a:` 回退，不属于差异，直接跳过。
 */
object ArticleMemoryShadow {

    data class Diff(
        val articleId: Long,
        val title: String,
        val oldStatus: OldStatus,
        val newState: ArticleMemory.ArticleState,
        val oldDue: Long,
        val newDue: Long,
        val oldDueNow: Boolean,
        val newDueNow: Boolean,
        val learned: Int,
        val eligible: Int,
        val newCount: Int,
    ) {
        val statusChanged: Boolean get() = oldStatus != oldStatusOf(newState)
        val dueChanged: Boolean get() = oldDue != newDue
    }

    enum class OldStatus { NONE, LEARNING, DUE }

    fun compare(
        articles: List<Article>,
        legacyStates: Map<Long, FsrsEngine.CardState>,
        sentenceStates: Map<String, FsrsEngine.CardState>,
        pausedArticleIds: Set<Long> = emptySet(),
        now: Long = System.currentTimeMillis(),
    ): List<Diff> = articles.mapNotNull { article ->
        val legacy = legacyStates[article.id]
        val summary = ArticleMemory.summary(
            article = article,
            sentenceStates = sentenceStates,
            legacyState = legacy,
            paused = article.id in pausedArticleIds,
            now = now,
        )
        // 没有句级状态：新旧都走旧卡回退/全新，不是投影差异。
        if (summary.learned == 0) return@mapNotNull null

        val oldStatus = when {
            legacy == null || legacy.reviewCount <= 0 -> OldStatus.NONE
            legacy.due > 0L && FsrsEngine.isDue(legacy, now) -> OldStatus.DUE
            else -> OldStatus.LEARNING
        }
        val oldDue = legacy?.due ?: 0L
        val oldDueNow = oldStatus == OldStatus.DUE
        Diff(
            articleId = article.id,
            title = article.title,
            oldStatus = oldStatus,
            newState = summary.state,
            oldDue = oldDue,
            newDue = summary.earliestDue,
            oldDueNow = oldDueNow,
            // 暂停态的 state 是 PAUSED，但句级 dueCount 仍应显示；影子差异按「有多少句到期」判断。
            newDueNow = summary.dueCount > 0,
            learned = summary.learned,
            eligible = summary.eligible,
            newCount = summary.newCount,
        )
    }.sortedBy { it.articleId }

    private fun oldStatusOf(state: ArticleMemory.ArticleState): OldStatus = when (state) {
        ArticleMemory.ArticleState.DUE -> OldStatus.DUE
        ArticleMemory.ArticleState.LEARNING,
        ArticleMemory.ArticleState.CAUGHT_UP,
        ArticleMemory.ArticleState.LEGACY,
        -> OldStatus.LEARNING

        ArticleMemory.ArticleState.NEW,
        ArticleMemory.ArticleState.PAUSED,
        -> OldStatus.NONE
    }

    /** 生成人类可读清单；不依赖 Android，可直接在 JVM/探针里打印。 */
    fun report(diffs: List<Diff>, now: Long = System.currentTimeMillis()): String = buildString {
        appendLine("memory-shadow: ${diffs.size} articles with sentence states")
        for (d in diffs) {
            val dueFlag = when {
                d.oldDueNow && !d.newDueNow -> "old=DUE/new=NOT_DUE"
                !d.oldDueNow && d.newDueNow -> "old=NOT_DUE/new=DUE"
                else -> ""
            }
            val dueText = if (d.dueChanged) {
                "due=${d.oldDue}->${d.newDue} ${dueFlag}"
            } else {
                "due=${d.newDue}"
            }
            appendLine(
                "  ${d.articleId} ${d.title} | ${d.oldStatus}->${d.newState} | " +
                    "$dueText | learned=${d.learned}/${d.eligible} new=${d.newCount}"
            )
        }
        appendLine("now=$now")
    }
}