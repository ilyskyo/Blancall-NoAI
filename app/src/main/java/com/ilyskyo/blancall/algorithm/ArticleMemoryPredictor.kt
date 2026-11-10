// Copyright (c) 2026 ilyskyo
// SPDX-License-Identifier: MIT

package com.ilyskyo.blancall.algorithm

import com.ilyskyo.blancall.data.model.Article
import com.ilyskyo.blancall.data.model.PracticeRecord
import kotlin.math.ceil

/**
 * 投影口径的「遗忘曲线预测」：用句级投影 + 聚合曲线生成现有
 * [ForgettingPredictor.Prediction]，供 Overview 预测卡直接消费。
 *
 * 只在 `MemoryModelConfig.model == SENTENCE_PROJECTION` 时被调用；
 * LEGACY 继续走 [ForgettingPredictor.predict]，保持旧口径。
 */
object ArticleMemoryPredictor {

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    fun predict(
        articles: List<Article>,
        allRecords: List<PracticeRecord>,
        template: ReviewTemplate = ReviewTemplate.STANDARD,
        legacyStates: Map<Long, FsrsEngine.CardState> = emptyMap(),
        sentenceStates: Map<String, FsrsEngine.CardState> = emptyMap(),
        pausedIds: Set<Long> = emptySet(),
        enrolledAt: Map<Long, Long> = emptyMap(),
        now: Long = System.currentTimeMillis(),
        target: Float = FsrsEngine.targetRetention().toFloat(),
    ): List<ForgettingPredictor.Prediction> {
        val recordsByArticle = allRecords.filter { it.articleId > 0L }.groupBy { it.articleId }
        val summaries = ArticleMemoryReads.views(
            articles = articles,
            legacyStates = legacyStates,
            sentenceStates = sentenceStates,
            recordsByArticle = recordsByArticle,
            template = template,
            pausedArticleIds = pausedIds,
            enrolledAt = enrolledAt,
            now = now,
            model = ArticleMemoryReads.Model.SENTENCE_PROJECTION,
        ).associateBy { it.articleId }

        return articles.mapNotNull { article ->
            val summary = summaries[article.id] ?: return@mapNotNull null
            val records = recordsByArticle[article.id].orEmpty()
            // 有句级状态时曲线只用句子，records 反推没有意义；只在回退分支才构造。
            val fallback = if (summary.learned == 0) {
                legacyStates[article.id] ?: ArticleMemoryFallback.fromRecords(records, template)
            } else {
                null
            }
            val curve = ArticleMemory.curve(
                article = article,
                sentenceStates = sentenceStates,
                legacyState = fallback,
                now = now,
                days = ArticleMemory.DEFAULT_CURVE_DAYS,
                target = target,
            )
            val due = summary.earliestDue
            val daysLeft = if (due > 0L) {
                ceil((due - now).toDouble() / MILLIS_PER_DAY).toInt()
            } else {
                Int.MAX_VALUE
            }
            val urgency = when {
                // 没有句级/旧状态（含只有 enrolled、没有练习）→ 未开始，和旧口径一致。
                summary.learned == 0 && summary.source == ArticleMemory.CurveSource.NONE ->
                    ForgettingPredictor.Urgency.NEW

                due <= 0L -> if (summary.practiceCount > 0) {
                    ForgettingPredictor.Urgency.MASTERED
                } else {
                    ForgettingPredictor.Urgency.NEW
                }

                else -> ForgettingPredictor.urgencyOf(due - now, daysLeft)
            }
            ForgettingPredictor.Prediction(
                articleId = article.id,
                title = article.title,
                urgency = urgency,
                reviewDate = if (urgency == ForgettingPredictor.Urgency.NEW ||
                    urgency == ForgettingPredictor.Urgency.MASTERED
                ) 0L else due,
                daysLeft = if (urgency == ForgettingPredictor.Urgency.NEW ||
                    urgency == ForgettingPredictor.Urgency.MASTERED
                ) Int.MAX_VALUE else daysLeft,
                practiceCount = summary.practiceCount,
                lastAccuracy = ForgettingPredictor.weightedAccuracy(records),
                retentionRate = summary.averageRetention ?: 0f,
                memoryStrength = summary.averageStability ?: 0f,
                decayCurve = curve.mean,
                paused = summary.state == ArticleMemory.ArticleState.PAUSED,
                weakestCurve = curve.weakest,
            )
        }.sortedWith(
            compareBy<ForgettingPredictor.Prediction> { it.urgency.ordinal }
                .thenBy { it.daysLeft }
                .thenBy { it.articleId }
        )
    }
}